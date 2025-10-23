package DeepQuard.DeepGuardBackend.repository;

import DeepQuard.DeepGuardBackend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
