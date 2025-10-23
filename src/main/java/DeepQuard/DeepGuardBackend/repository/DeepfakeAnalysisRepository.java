package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.DeepfakeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeepfakeAnalysisRepository extends JpaRepository<DeepfakeAnalysis, UUID> {
    @Query("SELECT da FROM DeepfakeAnalysis da WHERE da.mediaFile.user.id = :userId ORDER BY da.createdAt DESC")
    List<DeepfakeAnalysis> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    Optional<DeepfakeAnalysis> findByMediaFileId(UUID mediaFileId);
}