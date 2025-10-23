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
public class DeepfakeAnalysisResponse {
    private boolean success;
    private BigDecimal confidenceScore;
    private boolean isDeepfake;
    private String riskLevel;
    private List<String> detectedAnomalies;
    private Map<String, Object> technicalDetails;
    private String modelUsed;
    private long processingTimeMs;
    private String message;
}