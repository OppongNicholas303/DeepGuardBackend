package DeepQuard.DeepGuardBackend.service.auth.analyzerService;

import DeepQuard.DeepGuardBackend.model.*;
import DeepQuard.DeepGuardBackend.repository.*;
import DeepQuard.DeepGuardBackend.service.ai.TogetherAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class AnalyzerServiceImpl implements AnalyzerService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);
    
    @Autowired
    private DeepfakeAnalysisRepository analysisRepository;
    
    @Autowired
    private BreachDetectionRepository breachRepository;
    
    @Autowired
    private AiModelRepository aiModelRepository;
    
    @Autowired
    private TogetherAIService togetherAIService;
    
    @Async("aiProcessingExecutor")
    @Override
    public CompletableFuture<DeepfakeAnalysis> analyzeMedia(MediaFile mediaFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting analysis for media file: {}", mediaFile.getId());
                
                // Get active AI model
                AiModel model = aiModelRepository.findByModelTypeAndIsActiveTrue(
                    getModelTypeFromContentType(mediaFile.getContentType())
                ).stream().findFirst().orElse(null);
                
                if (model == null) {
                    throw new RuntimeException("No active AI model found");
                }
                
                // For now, simulate analysis since we need actual file content
                // In production, you'd load the file from S3 and pass to TogetherAI
                Map<String, Object> analysisDetails = performAIAnalysis(mediaFile);
                
                // Create analysis result
                DeepfakeAnalysis analysis = new DeepfakeAnalysis();
                analysis.setMediaFile(mediaFile);
                analysis.setModel(model);
                analysis.setConfidenceScore((BigDecimal) analysisDetails.get("confidence"));
                analysis.setIsDeepfake((Boolean) analysisDetails.get("isDeepfake"));
                analysis.setAnalysisDetails(analysisDetails);
                analysis.setProcessingTimeMs((Integer) analysisDetails.get("processingTime"));
                
                return analysisRepository.save(analysis);
                
            } catch (Exception e) {
                logger.error("Error analyzing media: {}", e.getMessage(), e);
                throw new RuntimeException("Analysis failed", e);
            }
        });
    }
    
    @Async("aiProcessingExecutor")
    @Override
    public CompletableFuture<BreachDetection> analyzeForBreaches(String content, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting breach analysis for user: {}", user.getId());
                
                // Use Together AI for breach detection
                Map<String, Object> breachResult = togetherAIService.analyzeTextForBreaches(content).get();
                
                if (!(Boolean) breachResult.get("breachDetected")) {
                    return null;
                }
                
                BreachDetection breach = new BreachDetection();
                breach.setUser(user);
                breach.setDetectionType(mapToDetectionType(breachResult));
                breach.setRiskLevel(mapToRiskLevel((String) breachResult.get("riskLevel")));
                breach.setConfidenceScore(new BigDecimal(breachResult.get("confidence").toString()));
                breach.setContextSnippet((String) breachResult.get("snippet"));
                breach.setSourceInfo(breachResult);
                breach.setDetectedContentHash(String.valueOf(content.hashCode()));
                // Set rule to null for now - can be enhanced later
                breach.setRule(null);
                
                return breachRepository.save(breach);
                
            } catch (Exception e) {
                logger.error("Error analyzing for breaches: {}", e.getMessage(), e);
                throw new RuntimeException("Breach analysis failed", e);
            }
        });
    }
    
    @Override
    public Map<String, Object> getAnalysisProgress(UUID analysisId) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("analysisId", analysisId);
        progress.put("status", "PROCESSING");
        progress.put("progress", 75);
        progress.put("estimatedTimeRemaining", 30);
        return progress;
    }
    
    private AiModel.ModelType getModelTypeFromContentType(String contentType) {
        if (contentType.startsWith("image/")) {
            return AiModel.ModelType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return AiModel.ModelType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return AiModel.ModelType.AUDIO;
        }
        return AiModel.ModelType.IMAGE; // Default
    }
    
    private Map<String, Object> performAIAnalysis(MediaFile mediaFile) {
        // Simulate AI processing - in production, load file and use TogetherAI
        Map<String, Object> result = new HashMap<>();
        result.put("confidence", new BigDecimal("0.85"));
        result.put("isDeepfake", false);
        result.put("processingTime", 2500);
        result.put("modelUsed", "together-ai-deepfake-detector");
        result.put("analysisTimestamp", LocalDateTime.now());
        result.put("anomalies", java.util.Arrays.asList("No significant anomalies detected"));
        return result;
    }
    
    private BreachDetection.DetectionType mapToDetectionType(Map<String, Object> breachResult) {
        Object detectedTypes = breachResult.get("detectedTypes");
        if (detectedTypes instanceof java.util.List) {
            java.util.List<?> types = (java.util.List<?>) detectedTypes;
            if (!types.isEmpty()) {
                String firstType = types.get(0).toString();
                try {
                    return BreachDetection.DetectionType.valueOf(firstType.toUpperCase());
                } catch (Exception e) {
                    return BreachDetection.DetectionType.PII;
                }
            }
        }
        return BreachDetection.DetectionType.PII; // Default
    }
    
    private BreachDetection.RiskLevel mapToRiskLevel(String riskLevel) {
        try {
            return BreachDetection.RiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (Exception e) {
            return BreachDetection.RiskLevel.MEDIUM; // Default
        }
    }
}
