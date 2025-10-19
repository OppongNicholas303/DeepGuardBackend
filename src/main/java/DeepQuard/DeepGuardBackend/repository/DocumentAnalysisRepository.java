package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.DocumentAnalysis;
import DeepQuard.DeepGuardBackend.model.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, UUID> {
    
    Optional<DocumentAnalysis> findByMediaFileId(UUID mediaFileId);
    
    List<DocumentAnalysis> findByRiskLevel(DocumentAnalysis.RiskLevel riskLevel);
    
    @Query("SELECT da FROM DocumentAnalysis da WHERE da.mediaFile.user.id = :userId ORDER BY da.analysisTimestamp DESC")
    Page<DocumentAnalysis> findByUserIdOrderByAnalysisTimestampDesc(UUID userId, Pageable pageable);
    
    @Query("SELECT da FROM DocumentAnalysis da WHERE da.analysisTimestamp BETWEEN :startDate AND :endDate")
    List<DocumentAnalysis> findByAnalysisTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(da) FROM DocumentAnalysis da WHERE da.isManipulated = true")
    Long countManipulatedDocuments();
}