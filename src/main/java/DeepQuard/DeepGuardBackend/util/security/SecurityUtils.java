package DeepQuard.DeepGuardBackend.util.security;

import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }

        return null;
    }

    public static UUID getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUserEmail() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    public static boolean isCurrentUser(UUID userId) {
        UUID currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public static boolean canAccessResource(UUID resourceOwnerId) {
        // Admin can access any resource
        if (hasRole("ADMIN")) {
            return true;
        }

        // User can only access their own resources
        return isCurrentUser(resourceOwnerId);
    }
}
