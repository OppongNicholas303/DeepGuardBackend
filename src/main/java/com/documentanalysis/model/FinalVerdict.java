package com.documentanalysis.model;

import lombok.Data;

@Data
public class FinalVerdict {
    private Boolean isManipulated;
    private Double manipulationConfidence;
    private String verdict;
    private String explanation;
}