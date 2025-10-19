package DeepQuard.DeepGuardBackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_analyses")
@Getter
@Setter
@NoArgsConstructor
public class DocumentAnalysis {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @Column(name = "metadata_score", precision = 5, scale = 2)
    private BigDecimal metadataScore;

    @Column(name = "forensics_score", precision = 5, scale = 2)
    private BigDecimal forensicsScore;

    @Column(name = "visual_score", precision = 5, scale = 2)
    private BigDecimal visualScore;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "is_manipulated")
    private Boolean isManipulated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forensic_details", columnDefinition = "jsonb")
    private Map<String, Object> forensicDetails;

    @CreationTimestamp
    @Column(name = "analysis_timestamp")
    private LocalDateTime analysisTimestamp;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}