package DeepQuard.DeepGuardBackend.dto.response;

import DeepQuard.DeepGuardBackend.model.DocumentAnalysis;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class DocumentAnalysisResponse {
    private UUID analysisId;
    private String status;
    private LocalDateTime uploadTimestamp;
    private String filename;
    private Map<String, Object> scores;
    private Boolean isManipulated;
    private DocumentAnalysis.RiskLevel riskLevel;
    private Long processingTimeMs;
    private Map<String, Object> forensicDetails;
}