package com.documentanalysis.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ModuleScore {
    private BigDecimal score;
    private Map<String, Object> details;
}