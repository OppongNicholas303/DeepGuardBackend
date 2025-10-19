package com.documentanalysis.model;

import lombok.Data;

@Data
public class EnsembleScore {
    private Double finalScore;
    private String riskLevel;
    private Double confidence;
    private String interpretation;
}