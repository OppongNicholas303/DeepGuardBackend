package com.documentanalysis.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DetectionReport {
    private String analysisId;
    private String status;
    private Map<String, Object> document;
    private Map<String, Object> scores;
    private Boolean isManipulated;
    private String recommendation;
}