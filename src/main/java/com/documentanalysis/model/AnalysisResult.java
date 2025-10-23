package com.documentanalysis.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_results")
public class AnalysisResult {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;
    @Column(name = "status")
    private String status;
    
    @Column(name = "timestamp")
    private Instant timestamp;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "module_scores", columnDefinition = "TEXT")
    private String moduleScoresJson;
    
    @Embedded
    private EnsembleScore ensembleScore;
    
    @Column(name = "gemini_analysis", columnDefinition = "TEXT")
    private String geminiAnalysisJson;
    
    @Embedded
    private FinalVerdict finalVerdict;
    
    @Transient
    private ModuleScores moduleScores;
    
    @Transient
    private GeminiAnalysis geminiAnalysis;
    
    public AnalysisResult() {}
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public ModuleScores getModuleScores() { return moduleScores; }
    public void setModuleScores(ModuleScores moduleScores) { this.moduleScores = moduleScores; }
    
    public String getModuleScoresJson() { return moduleScoresJson; }
    public void setModuleScoresJson(String moduleScoresJson) { this.moduleScoresJson = moduleScoresJson; }
    
    public EnsembleScore getEnsembleScore() { return ensembleScore; }
    public void setEnsembleScore(EnsembleScore ensembleScore) { this.ensembleScore = ensembleScore; }
    
    public GeminiAnalysis getGeminiAnalysis() { return geminiAnalysis; }
    public void setGeminiAnalysis(GeminiAnalysis geminiAnalysis) { this.geminiAnalysis = geminiAnalysis; }
    
    public String getGeminiAnalysisJson() { return geminiAnalysisJson; }
    public void setGeminiAnalysisJson(String geminiAnalysisJson) { this.geminiAnalysisJson = geminiAnalysisJson; }
    
    public FinalVerdict getFinalVerdict() { return finalVerdict; }
    public void setFinalVerdict(FinalVerdict finalVerdict) { this.finalVerdict = finalVerdict; }
    
    @Embeddable
    public static class ModuleScores {
        private ModuleScore metadata;
        private ModuleScore forensics;
        private ModuleScore visualAnomalies;
        private ModuleScore deepfakeDetection;
        private ModuleScore textManipulation; // Added for PDF support
        
        public ModuleScores() {}
        
        public ModuleScore getMetadata() { return metadata; }
        public void setMetadata(ModuleScore metadata) { this.metadata = metadata; }
        
        public ModuleScore getForensics() { return forensics; }
        public void setForensics(ModuleScore forensics) { this.forensics = forensics; }
        
        public ModuleScore getVisualAnomalies() { return visualAnomalies; }
        public void setVisualAnomalies(ModuleScore visualAnomalies) { this.visualAnomalies = visualAnomalies; }
        
        public ModuleScore getDeepfakeDetection() { return deepfakeDetection; }
        public void setDeepfakeDetection(ModuleScore deepfakeDetection) { this.deepfakeDetection = deepfakeDetection; }
        
        public ModuleScore getTextManipulation() { return textManipulation; }
        public void setTextManipulation(ModuleScore textManipulation) { this.textManipulation = textManipulation; }
    }
}