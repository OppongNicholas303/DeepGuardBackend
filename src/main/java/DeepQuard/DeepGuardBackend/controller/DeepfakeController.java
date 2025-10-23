package DeepQuard.DeepGuardBackend.controller;

import DeepQuard.DeepGuardBackend.aop.RateLimited;
import DeepQuard.DeepGuardBackend.dto.response.ApiResponse;
import DeepQuard.DeepGuardBackend.model.DeepfakeAnalysis;
import DeepQuard.DeepGuardBackend.model.MediaFile;
import DeepQuard.DeepGuardBackend.model.User;
import DeepQuard.DeepGuardBackend.repository.DeepfakeAnalysisRepository;
import DeepQuard.DeepGuardBackend.repository.MediaFileRepository;
import DeepQuard.DeepGuardBackend.repository.UserRepository;
import DeepQuard.DeepGuardBackend.service.ai.TogetherAIService;
import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/deepfake")
public class DeepfakeController {

    private static final Logger logger = LoggerFactory.getLogger(DeepfakeController.class);

    @Autowired
    private TogetherAIService togetherAIService;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private DeepfakeAnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/analyze")
    @RateLimited("upload")
    public ResponseEntity<ApiResponse<UUID>> analyzeMedia(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // Validate file
            if (!isValidMediaFile(file)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "Invalid file format"));
            }

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create MediaFile record
            MediaFile mediaFile = new MediaFile();
            mediaFile.setUser(user);
            mediaFile.setOriginalFilename(file.getOriginalFilename());
            mediaFile.setFileSize(file.getSize());
            mediaFile.setContentType(file.getContentType());
            mediaFile.setFileHash(String.valueOf(file.hashCode()));
            mediaFile.setS3Key("temp-key-" + UUID.randomUUID());
            mediaFile.setS3Bucket("temp-bucket");
            mediaFile.setProcessingStatus(MediaFile.ProcessingStatus.PENDING);

            MediaFile savedFile = mediaFileRepository.save(mediaFile);

            // Start async analysis with Together AI
            CompletableFuture<Map<String, Object>> analysisResult = 
                togetherAIService.analyzeImageForDeepfake(file);

            // Process result asynchronously
            analysisResult.thenAccept(result -> {
                try {
                    DeepfakeAnalysis analysis = new DeepfakeAnalysis();
                    analysis.setMediaFile(savedFile);
                    analysis.setConfidenceScore((BigDecimal) result.get("confidence"));
                    analysis.setIsDeepfake((Boolean) result.get("isDeepfake"));
                    analysis.setAnalysisDetails(result);
                    analysis.setProcessingTimeMs(2000); // Estimate

                    analysisRepository.save(analysis);

                    // Update media file status
                    savedFile.setProcessingStatus(MediaFile.ProcessingStatus.COMPLETED);
                    mediaFileRepository.save(savedFile);

                } catch (Exception e) {
                    logger.error("Error saving analysis result: {}", e.getMessage(), e);
                    savedFile.setProcessingStatus(MediaFile.ProcessingStatus.FAILED);
                    mediaFileRepository.save(savedFile);
                }
            });

            return ResponseEntity.ok(new ApiResponse<>(true, savedFile.getId(), "Analysis started"));

        } catch (Exception e) {
            logger.error("Error starting deepfake analysis: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<ApiResponse<DeepfakeAnalysis>> getAnalysisResult(@PathVariable UUID id) {
        try {
            DeepfakeAnalysis analysis = analysisRepository.findByMediaFileId(id)
                    .orElse(null);

            if (analysis == null) {
                MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
                if (mediaFile == null) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<>(false, null, "Analysis not found"));
                }

                if (mediaFile.getProcessingStatus() == MediaFile.ProcessingStatus.PENDING) {
                    return ResponseEntity.ok(new ApiResponse<>(true, null, "Analysis in progress"));
                } else if (mediaFile.getProcessingStatus() == MediaFile.ProcessingStatus.FAILED) {
                    return ResponseEntity.ok(new ApiResponse<>(false, null, "Analysis failed"));
                }
            }

            return ResponseEntity.ok(new ApiResponse<>(true, analysis, "Analysis completed"));

        } catch (Exception e) {
            logger.error("Error retrieving analysis result: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving result: " + e.getMessage()));
        }
    }

    private boolean isValidMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.startsWith("image/") || 
               contentType.startsWith("video/") || 
               contentType.startsWith("audio/");
    }
}