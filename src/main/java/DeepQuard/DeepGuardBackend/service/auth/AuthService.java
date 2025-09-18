package DeepQuard.DeepGuardBackend.service.auth;

import DeepQuard.DeepGuardBackend.dto.request.*;
import DeepQuard.DeepGuardBackend.dto.response.*;
import DeepQuard.DeepGuardBackend.model.User;
import DeepQuard.DeepGuardBackend.model.UserSession;
import DeepQuard.DeepGuardBackend.repository.UserRepository;
import DeepQuard.DeepGuardBackend.repository.UserSessionRepository;
import DeepQuard.DeepGuardBackend.service.security.JwtTokenProvider;
import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import DeepQuard.DeepGuardBackend.service.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public void registerUser(SignUpRequest signUpRequest) {
        logger.info("Registering new user with email: {}", signUpRequest.getEmail());

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        String role = signUpRequest.getRole().toUpperCase();
        if (!role.equals("USER") && !role.equals("ADMIN") && !role.equals("ANALYST")) {
            role = "USER";
        }

        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(User.Role.valueOf(role));
        user.setStatus(User.UserStatus.valueOf("ACTIVE"));
        user.setEmailVerified(false);

        String verificationToken = generateSecureToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpires(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", savedUser.getEmail(), e);
        }

        logger.info("User registered successfully with ID: {}", savedUser.getId());
    }

    public void verifyEmail(String token) {
        logger.info("Verifying email with token");

        User user = userRepository.findByVerificationTokenAndNotExpired(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpires(null);

        userRepository.save(user);

        logger.info("Email verified successfully for user: {}", user.getEmail());
    }

    public JwtAuthenticationResponse refreshToken(String refreshToken) {
        logger.info("Refreshing token");

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new RuntimeException("Invalid token type");
        }

        UUID userId = UUID.fromString(tokenProvider.getUserIdFromToken(refreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        logger.info("User: {}", user.getStatus());
        if (!"ACTIVE".equals(user.getStatus().toString()) || !user.isEmailVerified()) {
            throw new RuntimeException("User account is not active");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        createUserSession(user, newAccessToken, null, null);

        logger.info("Token refreshed successfully for user: {}", user.getEmail());

        return new JwtAuthenticationResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    public void updateLastLogin(String email) {
        logger.debug("Updating last login for user: {}", email);
        userRepository.updateLastLoginByEmail(email, LocalDateTime.now());

        userRepository.updateFailedLoginAttempts(email, 0);
    }

    public void initiatePasswordReset(String email) {
        logger.info("Initiating password reset for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        String resetToken = generateSecureToken();
        user.setResetToken(resetToken);
        user.setResetTokenExpires(LocalDateTime.now().plusHours(1)); // 1 hour expiry

        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email");
        }

        logger.info("Password reset initiated successfully for user: {}", user.getEmail());
    }

    public void resetPassword(String token, String newPassword) {
        logger.info("Resetting password with token");

        User user = userRepository.findByResetTokenAndNotExpired(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpires(null);

        userRepository.save(user);

        userSessionRepository.revokeAllUserTokens(user.getId());

        logger.info("Password reset successfully for user: {}", user.getEmail());
    }

    public void logout(String token) {
        logger.info("Logging out user");

        try {

            String tokenHash = hashToken(token);

            int updated = userSessionRepository.revokeToken(tokenHash);

            if (updated == 0) {
                logger.warn("Token not found in session store, may have already been revoked");
            }

            logger.info("User logged out successfully");
        } catch (Exception e) {
            logger.error("Error during logout", e);
            throw new RuntimeException("Logout failed");
        }
    }

    public void handleFailedLogin(String email) {
        logger.warn("Failed login attempt for email: {}", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            userRepository.updateFailedLoginAttempts(email, attempts);

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
                userRepository.lockUser(email, lockUntil);
                logger.warn("User account locked due to too many failed attempts: {}", email);
            }
        });
    }

    private void createUserSession(User user, String token, String ipAddress, String userAgent) {
        try {
            UserSession session = new UserSession();
            session.setUser(user);
            session.setTokenHash(hashToken(token));
            session.setExpiresAt(LocalDateTime.now().plusMinutes(15)); // Match JWT expiry
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setIsRevoked(false);

            userSessionRepository.save(session);
        } catch (Exception e) {
            logger.error("Failed to create user session", e);
            // Don't fail the authentication if session creation fails
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes());
    }

    // Cleanup expired tokens (can be scheduled)
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = userSessionRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            logger.info("Cleaned up {} expired tokens", deleted);
        }
    }
}
