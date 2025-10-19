package com.documentanalysis.model;

import lombok.Data;
import java.util.List;

@Data
public class ModuleScore {
    private Double score;
    private Double confidence;
    private List<String> findings;
    private List<FlaggedRegion> flaggedRegions;
    
    // Deepfake-specific fields
    private Boolean isDeepfake;
    private String modelName;
}