package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// AiModelRepository.java
@Repository
public interface AiModelRepository extends JpaRepository<AiModel, UUID> {
    List<AiModel> findByModelTypeAndIsActiveTrue(AiModel.ModelType modelType);
    Optional<AiModel> findByModelNameAndModelVersion(String modelName, String version);
}
