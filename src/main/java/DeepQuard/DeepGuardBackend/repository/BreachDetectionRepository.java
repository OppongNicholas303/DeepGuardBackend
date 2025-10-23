package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.BreachDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// BreachDetectionRepository.java
@Repository
public interface BreachDetectionRepository extends JpaRepository<BreachDetection, UUID> {
    List<BreachDetection> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<BreachDetection> findByUserIdAndIsAcknowledgedFalse(UUID userId);
}