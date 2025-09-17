package DeepQuard.DeepGuardBackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
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
@Table(name = "notification_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "channel"})
})
public class NotificationSetting {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configuration;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "notification_types", joinColumns = @JoinColumn(name = "notification_setting_id"))
    @Column(name = "notification_type")
    private Set<NotificationType> notificationTypes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum NotificationChannel {
        EMAIL, SMS, WEBHOOK
    }

    public enum NotificationType {
        BREACH_ALERT, ANALYSIS_COMPLETE, SYSTEM_MAINTENANCE, SECURITY_ALERT
    }
}


