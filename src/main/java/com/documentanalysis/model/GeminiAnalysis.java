package com.documentanalysis.model;

import lombok.Data;
import java.util.List;

@Data
public class GeminiAnalysis {
    private String executiveSummary;
    private List<String> keyFindings;
    private String riskAssessment;
    private List<AreaOfConcern> areasOfConcern;
    private Double geminiConfidence;
    private List<String> recommendations;
}