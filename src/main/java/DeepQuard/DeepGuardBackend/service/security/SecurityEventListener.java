package DeepQuard.DeepGuardBackend.service.security;


import DeepQuard.DeepGuardBackend.model.AuditLog;
import DeepQuard.DeepGuardBackend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SecurityEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventListener.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        logSecurityEvent(user.getId(), "LOGIN_SUCCESS", "User logged in successfully",
                ipAddress, userAgent, true, null);

        logger.info("Successful authentication for user: {} from IP: {}", user.getEmail(), ipAddress);
    }

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        logSecurityEvent(null, "LOGIN_FAILED",
                "Failed login attempt for: " + username + ". Reason: " + reason,
                ipAddress, userAgent, false, reason);

        logger.warn("Failed authentication for user: {} from IP: {}. Reason: {}",
                username, ipAddress, reason);
    }

    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        Authentication auth = event.getAuthentication().get();

        UUID userId = null;
        String username = "anonymous";

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal user = (UserPrincipal) auth.getPrincipal();
            userId = user.getId();
            username = user.getEmail();
        }

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        logSecurityEvent(userId, "ACCESS_DENIED",
                "Access denied for user: " + username,
                ipAddress, userAgent, false, "Insufficient permissions");

        logger.warn("Access denied for user: {} from IP: {}", username, ipAddress);
    }

    private void logSecurityEvent(UUID userId, String action, String message,
                                  String ipAddress, String userAgent, boolean success, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType("AUTHENTICATION");
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());

            // Add additional data
            if (message != null) {

                Map<String, Object> data = new HashMap<>();
                data.put("message", message);
                auditLog.setAdditionalData(data);
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
            // Don't fail the authentication flow if audit logging fails
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
