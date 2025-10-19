package com.documentanalysis.model;

import lombok.Data;

@Data
public class FlaggedRegion {
    private String type;
    private Coordinates coordinates;
    private String severity;
    
    @Data
    public static class Coordinates {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
    }
}