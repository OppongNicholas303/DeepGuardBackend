package DeepQuard.DeepGuardBackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "breach_detections")
public class BreachDetection {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BreachMonitoringRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type", length = 50, nullable = false)
    private DetectionType detectionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20, nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "detected_content_hash", length = 64)
    private String detectedContentHash;

    @Column(name = "context_snippet", columnDefinition = "TEXT")
    private String contextSnippet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_info", columnDefinition = "jsonb")
    private Map<String, Object> sourceInfo;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_acknowledged")
    private Boolean isAcknowledged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    public enum DetectionType {
        PII, CREDENTIALS, API_KEY, CREDIT_CARD, SSN, EMAIL
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}