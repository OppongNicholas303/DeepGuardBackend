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

@Entity
@Table(name = "media_files")
@NamedEntityGraph(
        name = "MediaFile.withAnalyses",
        attributeNodes = @NamedAttributeNode("analyses")
)
@Getter
@Setter
@NoArgsConstructor
public class MediaFile {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "file_hash", length = 64, nullable = false)
    private String fileHash;

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;

    @Column(name = "s3_bucket", length = 100, nullable = false)
    private String s3Bucket;

    @CreationTimestamp
    @Column(name = "upload_timestamp")
    private LocalDateTime uploadTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_metadata", columnDefinition = "jsonb")
    private Map<String, Object> fileMetadata;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DeepfakeAnalysis> analyses;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AnalysisQueue> queueEntries;

    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}