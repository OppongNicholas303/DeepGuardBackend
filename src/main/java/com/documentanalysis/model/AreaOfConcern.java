package com.documentanalysis.model;

import lombok.Data;

@Data
public class AreaOfConcern {
    private String area;
    private String severity;
    private FlaggedRegion.Coordinates coordinates;
    private String details;
}