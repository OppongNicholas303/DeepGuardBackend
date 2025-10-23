package com.documentanalysis.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ElementCollection;
import java.util.List;

@Embeddable
public class GeminiAnalysis {
    private String executiveSummary;
    private List<String> keyFindings;
    private String riskAssessment;
    private List<AreaOfConcern> areasOfConcern;
    private Double geminiConfidence;
    private List<String> recommendations;
    
    public GeminiAnalysis() {}
    
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    
    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    
    public List<AreaOfConcern> getAreasOfConcern() { return areasOfConcern; }
    public void setAreasOfConcern(List<AreaOfConcern> areasOfConcern) { this.areasOfConcern = areasOfConcern; }
    
    public Double getGeminiConfidence() { return geminiConfidence; }
    public void setGeminiConfidence(Double geminiConfidence) { this.geminiConfidence = geminiConfidence; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}