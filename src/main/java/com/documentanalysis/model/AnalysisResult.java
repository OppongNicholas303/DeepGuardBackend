package com.documentanalysis.model;

import lombok.Data;
import java.time.Instant;

@Data
public class AnalysisResult {
    private String status;
    private Instant timestamp;
    private Long processingTimeMs;
    
    private ModuleScores moduleScores;
    private EnsembleScore ensembleScore;
    private GeminiAnalysis geminiAnalysis;
    private FinalVerdict finalVerdict;
    
    @Data
    public static class ModuleScores {
        private ModuleScore metadata;
        private ModuleScore forensics;
        private ModuleScore visualAnomalies;
        private ModuleScore deepfakeDetection;
    }
}