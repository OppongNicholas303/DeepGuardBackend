package com.documentanalysis.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class FinalVerdict {
    private Boolean isManipulated;
    private Double manipulationConfidence;
    private String verdict;
    private String explanation;

    public FinalVerdict() {}

    public Boolean getIsManipulated() { return isManipulated; }
    public void setIsManipulated(Boolean isManipulated) { this.isManipulated = isManipulated; }
    public void setManipulated(Boolean manipulated) { this.isManipulated = manipulated; }
    
    public Double getManipulationConfidence() { return manipulationConfidence; }
    public void setManipulationConfidence(Double manipulationConfidence) { this.manipulationConfidence = manipulationConfidence; }
    
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}