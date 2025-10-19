package com.documentanalysis.service;

import com.documentanalysis.model.AnalysisResult;
import com.documentanalysis.model.ModuleScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisService {

    private final MetadataAnalysisService metadataAnalysisService;
    private final ForensicsAnalysisService forensicsAnalysisService;
    private final VisualAnomalyService visualAnomalyService;
    private final DeepfakeDetectionService deepfakeDetectionService;
    private final EnsembleScoringService ensembleScoringService;
    private final GeminiReasoningService geminiReasoningService;

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

    public AnalysisResult analyze(MultipartFile multipartFile) {
        File tempFile = null;
        
        try {
            // Convert MultipartFile to File for processing
            tempFile = convertToFile(multipartFile);
            
            log.info("Starting parallel analysis for file: {}", multipartFile.getOriginalFilename());
            
            // Run all 4 modules in parallel
            CompletableFuture<ModuleScore> metaFuture = analyzeMetadata(tempFile);
            CompletableFuture<ModuleScore> forensicsFuture = analyzeForensics(tempFile);
            CompletableFuture<ModuleScore> visualFuture = analyzeVisualAnomalies(tempFile);
            CompletableFuture<ModuleScore> deepfakeFuture = analyzeDeepfake(tempFile);
            
            // Wait for all 4 to complete (with timeout)
            CompletableFuture.allOf(metaFuture, forensicsFuture, visualFuture, deepfakeFuture)
                    .orTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .join();
            
            // Get results
            ModuleScore metadataScore = metaFuture.get();
            ModuleScore forensicsScore = forensicsFuture.get();
            ModuleScore visualScore = visualFuture.get();
            ModuleScore deepfakeScore = deepfakeFuture.get();
            
            log.info("All modules completed. Calculating ensemble score...");
            
            // Calculate ensemble score
            AnalysisResult result = ensembleScoringService.calculateEnsembleScore(
                    metadataScore, forensicsScore, visualScore, deepfakeScore);
            
            // Get Gemini AI analysis
            log.info("Sending findings to Gemini AI for reasoning...");
            result.setGeminiAnalysis(geminiReasoningService.analyzeFindings(result));
            
            // Set final verdict based on Gemini + ensemble
            result.setFinalVerdict(determineFinalVerdict(result));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        } finally {
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File convertToFile(MultipartFile multipartFile) throws IOException {
        Path tempPath = Files.createTempFile("analysis_", "_" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(tempPath.toFile());
        return tempPath.toFile();
    }

    private com.documentanalysis.model.FinalVerdict determineFinalVerdict(AnalysisResult result) {
        com.documentanalysis.model.FinalVerdict verdict = new com.documentanalysis.model.FinalVerdict();
        
        double finalScore = result.getEnsembleScore().getFinalScore();
        double geminiConfidence = result.getGeminiAnalysis().getGeminiConfidence();
        
        // Combine ensemble score with Gemini confidence
        double combinedConfidence = (result.getEnsembleScore().getConfidence() + geminiConfidence) / 2.0;
        
        verdict.setManipulationConfidence(combinedConfidence);
        verdict.setManipulated(finalScore >= 50.0);
        
        if (finalScore >= 75) {
            verdict.setVerdict("HIGHLY LIKELY MANIPULATED");
        } else if (finalScore >= 50) {
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
}