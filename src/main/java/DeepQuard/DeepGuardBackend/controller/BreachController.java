package DeepQuard.DeepGuardBackend.controller;

import DeepQuard.DeepGuardBackend.aop.RateLimited;
import DeepQuard.DeepGuardBackend.dto.response.ApiResponse;
import DeepQuard.DeepGuardBackend.model.BreachDetection;
import DeepQuard.DeepGuardBackend.model.User;
import DeepQuard.DeepGuardBackend.repository.BreachDetectionRepository;
import DeepQuard.DeepGuardBackend.repository.UserRepository;
import DeepQuard.DeepGuardBackend.service.auth.analyzerService.AnalyzerService;
import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/breach")
public class BreachController {

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private BreachDetectionRepository breachRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/scan")
    @RateLimited("api")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanContent(
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

            // Use Together AI for breach analysis
            analyzerService.analyzeForBreaches(content, user)
                .thenAccept(breachDetection -> {
                    if (breachDetection != null) {
                        // Breach detected and saved
                        System.out.println("Breach detected and saved: " + breachDetection.getId());
                    }
                });

            return ResponseEntity.ok(new ApiResponse<>(true, 
                Map.of("message", "Breach scan initiated", "status", "processing"), 
                "Scan started"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Scan failed: " + e.getMessage()));
        }
    }



    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<BreachDetection>>> getBreachResults(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            List<BreachDetection> breaches = breachRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, breaches, "Breach results retrieved"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving breach results: " + e.getMessage()));
        }
    }

    @GetMapping("/unacknowledged")
    public ResponseEntity<ApiResponse<List<BreachDetection>>> getUnacknowledgedBreaches(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            List<BreachDetection> breaches = breachRepository.findByUserIdAndIsAcknowledgedFalse(userPrincipal.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, breaches, "Unacknowledged breaches retrieved"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error retrieving unacknowledged breaches: " + e.getMessage()));
        }
    }

    @PostMapping("/acknowledge/{id}")
    public ResponseEntity<ApiResponse<String>> acknowledgeBreach(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            BreachDetection breach = breachRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Breach not found"));

            if (!breach.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "Unauthorized access to breach"));
            }

            breach.setIsAcknowledged(true);
            breach.setAcknowledgedBy(user);
            breach.setAcknowledgedAt(java.time.LocalDateTime.now());
            
            breachRepository.save(breach);

            return ResponseEntity.ok(new ApiResponse<>(true, "Breach acknowledged", "Success"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, null, "Error acknowledging breach: " + e.getMessage()));
        }
    }
}