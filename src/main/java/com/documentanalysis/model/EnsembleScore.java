package com.documentanalysis.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class EnsembleScore {
    private Double finalScore;
    private String riskLevel;
    private Double confidence;
    private String interpretation;
    
    public EnsembleScore() {}
    
    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
}