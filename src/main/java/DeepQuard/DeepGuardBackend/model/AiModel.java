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
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_models", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"model_name", "model_version"})
})
public class AiModel {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    @Column(name = "model_version", length = 20, nullable = false)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", length = 50, nullable = false)
    private ModelType modelType;

    @Column(name = "accuracy_score", precision = 5, scale = 4)
    private BigDecimal accuracyScore;

    @Column(name = "model_path", columnDefinition = "TEXT")
    private String modelPath;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DeepfakeAnalysis> analyses;

    public enum ModelType {
        IMAGE, VIDEO, AUDIO
    }
}
