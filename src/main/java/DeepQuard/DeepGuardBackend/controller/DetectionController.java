package DeepQuard.DeepGuardBackend.controller;

import DeepQuard.DeepGuardBackend.aop.RateLimited;
import DeepQuard.DeepGuardBackend.dto.response.ApiResponse;
import DeepQuard.DeepGuardBackend.model.DeepfakeAnalysis;
import DeepQuard.DeepGuardBackend.model.MediaFile;
import DeepQuard.DeepGuardBackend.model.User;
import DeepQuard.DeepGuardBackend.repository.DeepfakeAnalysisRepository;
import DeepQuard.DeepGuardBackend.repository.MediaFileRepository;
import DeepQuard.DeepGuardBackend.repository.UserRepository;
import DeepQuard.DeepGuardBackend.service.auth.analyzerService.AnalyzerService;
import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/detection")
public class DetectionController {

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private DeepfakeAnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/analyze-file")
    @RateLimited("upload")
    public ResponseEntity<ApiResponse<UUID>> analyzeFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
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

            // Start async analysis
            CompletableFuture<DeepfakeAnalysis> analysisResult = analyzerService.analyzeMedia(savedFile);

            return ResponseEntity.ok(new ApiResponse<>(true, savedFile.getId(), "Analysis started"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<ApiResponse<DeepfakeAnalysis>> getResult(@PathVariable UUID id) {
        try {
            DeepfakeAnalysis analysis = analysisRepository.findByMediaFileId(id)
                    .orElse(null);

            if (analysis == null) {
                return ResponseEntity.ok(new ApiResponse<>(true, null, "Analysis in progress"));
            }

            return ResponseEntity.ok(new ApiResponse<>(true, analysis, "Analysis completed"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving result: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<DeepfakeAnalysis>>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            List<DeepfakeAnalysis> history = analysisRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, history, "History retrieved"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving history: " + e.getMessage()));
        }
    }

    @GetMapping("/progress/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(@PathVariable UUID id) {
        try {
            Map<String, Object> progress = analyzerService.getAnalysisProgress(id);
            return ResponseEntity.ok(new ApiResponse<>(true, progress, "Progress retrieved"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving progress: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze-text")
    @RateLimited("api")
    public ResponseEntity<ApiResponse<UUID>> analyzeText(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "Content is required"));
            }

            // Start breach analysis
            analyzerService.analyzeForBreaches(content, user);

            return ResponseEntity.ok(new ApiResponse<>(true, UUID.randomUUID(), "Text analysis started"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Text analysis failed: " + e.getMessage()));
        }
    }
}