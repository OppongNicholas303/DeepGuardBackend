package com.documentanalysis.model;

import java.util.Map;

public class FlaggedRegion {
    private String type;
    private Map<String, Integer> coordinates;
    private String severity;
    private String details;
    
    public FlaggedRegion() {}
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, Integer> getCoordinates() { return coordinates; }
    public void setCoordinates(Map<String, Integer> coordinates) { this.coordinates = coordinates; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}