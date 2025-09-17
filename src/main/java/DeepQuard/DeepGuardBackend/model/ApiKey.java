package DeepQuard.DeepGuardBackend.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "key_name", length = 100, nullable = false)
    private String keyName;

    @Column(name = "api_key_hash", length = 255, nullable = false)
    private String apiKeyHash;

    @Type(JsonType.class)
    @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> permissions;

    @Column(name = "rate_limit")
    private Integer rateLimit = 1000;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "is_active")
    private Boolean isActive = true;

    
}