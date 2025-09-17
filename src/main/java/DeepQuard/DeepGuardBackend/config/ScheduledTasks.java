package DeepQuard.DeepGuardBackend.config;

import DeepQuard.DeepGuardBackend.service.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private AuthService authService;

    // Run every hour to cleanup expired tokens
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredTokens() {
        logger.debug("Running scheduled cleanup of expired tokens");
        try {
            authService.cleanupExpiredTokens();
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
        }
    }
}