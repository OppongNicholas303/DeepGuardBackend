package DeepQuard.DeepGuardBackend.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BreachAnalysisResponse {
    private boolean breachDetected;
    private String riskLevel;
    private BigDecimal confidence;
    private List<String> detectedTypes;
    private String snippet;
    private List<DetectedItem> detectedItems;
    private Map<String, Object> metadata;
    private String message;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetectedItem {
        private String type;
        private String value;
        private BigDecimal confidence;
        private String context;
    }
}