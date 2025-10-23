package com.documentanalysis.model;

import java.util.List;

public class ModuleScore {
    private Double score;
    private Double confidence;
    private List<String> findings;
    private List<FlaggedRegion> flaggedRegions;
    
    // Deepfake-specific fields
    private Boolean isDeepfake;
    private String modelName;
    
    public ModuleScore() {}
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public List<String> getFindings() { return findings; }
    public void setFindings(List<String> findings) { this.findings = findings; }
    
    public List<FlaggedRegion> getFlaggedRegions() { return flaggedRegions; }
    public void setFlaggedRegions(List<FlaggedRegion> flaggedRegions) { this.flaggedRegions = flaggedRegions; }
    
    public Boolean getIsDeepfake() { return isDeepfake; }
    public void setIsDeepfake(Boolean isDeepfake) { this.isDeepfake = isDeepfake; }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}