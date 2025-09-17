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
@Table(name = "deepfake_analyses")
public class DeepfakeAnalysis {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private AiModel model;

    @Column(name = "confidence_score", precision = 5, scale = 4, nullable = false)
    private BigDecimal confidenceScore;

    @Column(name = "is_deepfake", nullable = false)
    private Boolean isDeepfake;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_details", columnDefinition = "jsonb")
    private Map<String, Object> analysisDetails;

    @Column(name = "annotated_media_s3_key", length = 500)
    private String annotatedMediaS3Key;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}