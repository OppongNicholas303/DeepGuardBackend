package com.documentanalysis.model;

import java.util.Map;

public class AreaOfConcern {
    private String area;
    private String severity;
    private Map<String, Integer> coordinates;
    private String details;
    
    public AreaOfConcern() {}
    
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public Map<String, Integer> getCoordinates() { return coordinates; }
    public void setCoordinates(Map<String, Integer> coordinates) { this.coordinates = coordinates; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}