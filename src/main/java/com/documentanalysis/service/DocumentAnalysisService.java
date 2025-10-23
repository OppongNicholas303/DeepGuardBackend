package com.documentanalysis.service;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.model.ModuleScore;
import com.documentanalysis.model.GeminiAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

@Service
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    @Autowired
    private MetadataAnalysisService metadataAnalysisService;
    
    @Autowired
    private ForensicsAnalysisService forensicsAnalysisService;
    
    @Autowired
    private VisualAnomalyService visualAnomalyService;
    
    @Autowired
    private DeepfakeDetectionService deepfakeDetectionService;
    
    @Autowired
    private TextManipulationService textManipulationService;
    
    @Autowired
    private EnsembleScoringService ensembleScoringService;
    
    @Autowired
    private GeminiReasoningService geminiReasoningService;

    @Async
    public CompletableFuture<ModuleScore> analyzeMetadata(File file) {
        return CompletableFuture.completedFuture(metadataAnalysisService.analyze(file));
    }
    
    @Async
    public CompletableFuture<ModuleScore> analyzeForensics(File file) {
        return CompletableFuture.completedFuture(forensicsAnalysisService.analyze(file));
    }
    
    @Async
    public CompletableFuture<ModuleScore> analyzeVisualAnomalies(File file) {
        return CompletableFuture.completedFuture(visualAnomalyService.analyze(file));
    }
    
    @Async
    public CompletableFuture<ModuleScore> analyzeDeepfake(File file) {
        return CompletableFuture.completedFuture(deepfakeDetectionService.analyze(file));
    }
    
    @Async
    public CompletableFuture<ModuleScore> analyzeTextManipulation(File file) {
        return CompletableFuture.completedFuture(textManipulationService.analyze(file));
    }

    public AnalysisResult analyze(MultipartFile multipartFile) {
        File tempFile = null;
        
        try {
            tempFile = convertToFile(multipartFile);
            boolean isPdf = isPdfFile(multipartFile);
            
            log.info("Starting parallel analysis for file: {} (PDF: {})", multipartFile.getOriginalFilename(), isPdf);
            
            CompletableFuture<ModuleScore> metaFuture = analyzeMetadata(tempFile);
            CompletableFuture<ModuleScore> forensicsFuture = analyzeForensics(tempFile);
            CompletableFuture<ModuleScore> visualFuture = analyzeVisualAnomalies(tempFile);
            CompletableFuture<ModuleScore> deepfakeFuture = analyzeDeepfake(tempFile);
            CompletableFuture<ModuleScore> textFuture = analyzeTextManipulation(tempFile);
            
            CompletableFuture.allOf(metaFuture, forensicsFuture, visualFuture, deepfakeFuture, textFuture)
                    .orTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .join();
            
            ModuleScore metadataScore = metaFuture.get();
            ModuleScore forensicsScore = forensicsFuture.get();
            ModuleScore visualScore = visualFuture.get();
            ModuleScore deepfakeScore = deepfakeFuture.get();
            ModuleScore textScore = textFuture.get();
            
            log.info("All modules completed. Calculating ensemble score...");
            
            AnalysisResult result;
            if (isPdf && textScore.getScore() > 0) {
                // Use PDF-specific ensemble scoring with text manipulation
                result = ensembleScoringService.calculatePdfEnsembleScore(
                        metadataScore, forensicsScore, visualScore, textScore);
            } else if (isPdf) {
                // PDF without text manipulation analysis
                result = ensembleScoringService.calculateEnsembleScore(
                        metadataScore, forensicsScore, visualScore, deepfakeScore);
            } else {
                // Use image-specific ensemble scoring
                result = ensembleScoringService.calculateEnsembleScore(
                        metadataScore, forensicsScore, visualScore, deepfakeScore);
            }
            
            log.info("Sending findings to Gemini AI for reasoning...");
            GeminiAnalysis geminiAnalysis = geminiReasoningService.analyzeFindings(result);
            
            geminiAnalysis.setAreasOfConcern(extractAreasOfConcern(result));
            result.setGeminiAnalysis(geminiAnalysis);
            
            result.setFinalVerdict(determineFinalVerdict(result));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private boolean isPdfFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    private File convertToFile(MultipartFile multipartFile) throws IOException {
        Path tempPath = Files.createTempFile("analysis_", "_" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(tempPath.toFile());
        return tempPath.toFile();
    }

    private com.documentanalysis.model.FinalVerdict determineFinalVerdict(AnalysisResult result) {
        com.documentanalysis.model.FinalVerdict verdict = new com.documentanalysis.model.FinalVerdict();
        
        double finalScore = result.getEnsembleScore().getFinalScore();
        double forensicsScore = result.getModuleScores().getForensics().getScore();
        double geminiConfidence = result.getGeminiAnalysis().getGeminiConfidence();
        
        boolean copyMoveDetected = result.getModuleScores().getForensics().getFindings().stream()
            .anyMatch(finding -> finding.toLowerCase().contains("copy-move"));
        
        boolean hasHighSeverityRegions = result.getModuleScores().getForensics().getFlaggedRegions().stream()
            .anyMatch(region -> "high".equalsIgnoreCase(region.getSeverity())) ||
            result.getModuleScores().getVisualAnomalies().getFlaggedRegions().stream()
            .anyMatch(region -> "high".equalsIgnoreCase(region.getSeverity()));
        
        double combinedConfidence = (result.getEnsembleScore().getConfidence() + geminiConfidence) / 2.0;
        
        verdict.setManipulationConfidence(combinedConfidence);
        
        boolean isManipulated = finalScore >= 50.0 || forensicsScore > 50.0 || copyMoveDetected || hasHighSeverityRegions;
        verdict.setManipulated(isManipulated);
        
        if (finalScore >= 75 || copyMoveDetected || hasHighSeverityRegions) {
            verdict.setVerdict("HIGHLY LIKELY MANIPULATED");
        } else if (finalScore >= 50 || forensicsScore > 50) {
            verdict.setVerdict("LIKELY MANIPULATED");
        } else {
            verdict.setVerdict("UNLIKELY MANIPULATED");
        }
        
        verdict.setExplanation(String.format(
            "Analysis confidence: %.1f%%. Ensemble score: %.1f/100. %s",
            combinedConfidence * 100, finalScore,
            result.getGeminiAnalysis().getExecutiveSummary()
        ));
        
        return verdict;
    }
    
    private List<com.documentanalysis.model.AreaOfConcern> extractAreasOfConcern(AnalysisResult result) {
        List<com.documentanalysis.model.AreaOfConcern> areasOfConcern = new ArrayList<>();
        List<com.documentanalysis.model.FlaggedRegion> allRegions = new ArrayList<>();
        
        if (result.getModuleScores().getForensics().getFlaggedRegions() != null) {
            allRegions.addAll(result.getModuleScores().getForensics().getFlaggedRegions());
        }
        if (result.getModuleScores().getVisualAnomalies().getFlaggedRegions() != null) {
            allRegions.addAll(result.getModuleScores().getVisualAnomalies().getFlaggedRegions());
        }
        
        // Filter to only HIGH and MEDIUM severity regions, limit to 15
        allRegions.stream()
            .filter(region -> "high".equalsIgnoreCase(region.getSeverity()) || "medium".equalsIgnoreCase(region.getSeverity()))
            .sorted((r1, r2) -> {
                int severity1 = getSeverityWeight(r1.getSeverity());
                int severity2 = getSeverityWeight(r2.getSeverity());
                return Integer.compare(severity2, severity1);
            })
            .limit(15)
            .forEach(region -> {
                com.documentanalysis.model.AreaOfConcern area = new com.documentanalysis.model.AreaOfConcern();
                area.setArea(getAreaDescription(region.getType()));
                area.setSeverity(region.getSeverity().toUpperCase());
                area.setCoordinates(region.getCoordinates());
                area.setDetails(region.getDetails() != null ? region.getDetails() : getAreaDetails(region.getType(), region.getSeverity()));
                areasOfConcern.add(area);
            });
        
        return areasOfConcern;
    }
    
    private int getSeverityWeight(String severity) {
        if (severity == null) return 0;
        switch (severity.toLowerCase()) {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 0;
        }
    }
    
    private String getAreaDescription(String type) {
        switch (type) {
            case "copy_move_detected":
                return "Copy-move forgery in detected region";
            case "ela_anomaly":
                return "Error Level Analysis anomaly";
            case "shadow_inconsistency":
                return "Lighting and shadow direction mismatch";
            case "color_gradient_anomaly":
                return "Unnatural color gradient patterns";
            default:
                return "Suspicious region detected";
        }
    }
    
    private String getAreaDetails(String type, String severity) {
        switch (type) {
            case "copy_move_detected":
                return "Two regions match with 85% similarity, indicating content duplication";
            case "ela_anomaly":
                return "Compression inconsistencies suggest digital manipulation";
            case "shadow_inconsistency":
                return "Light source appears inconsistent with shadow positioning";
            case "color_gradient_anomaly":
                return "Color transitions appear artificially enhanced or modified";
            default:
                return "Technical analysis indicates potential manipulation";
        }
    }
}