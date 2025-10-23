package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// MediaFileRepository.java
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByUserIdOrderByUploadTimestampDesc(UUID userId);
    Optional<MediaFile> findByFileHash(String fileHash);
}
