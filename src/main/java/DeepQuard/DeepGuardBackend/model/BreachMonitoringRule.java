package DeepQuard.DeepGuardBackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "breach_monitoring_rules")
public class BreachMonitoringRule {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rule_name", length = 100, nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 50, nullable = false)
    private RuleType ruleType;

    @Column(name = "pattern", columnDefinition = "TEXT", nullable = false)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", length = 20)
    private SensitivityLevel sensitivityLevel = SensitivityLevel.MEDIUM;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BreachDetection> detections;

    public enum RuleType {
        REGEX, ML_ANOMALY, KEYWORD
    }

    public enum SensitivityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}

