package com.documentanalysis.service.auth;

import com.documentanalysis.dto.request.*;
import com.documentanalysis.dto.response.*;
import com.documentanalysis.model.User;
import com.documentanalysis.repository.UserRepository;
import com.documentanalysis.repository.UserSessionRepository;
import com.documentanalysis.security.JwtTokenProvider;
import com.documentanalysis.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final SecureRandom secureRandom = new SecureRandom();

    public void registerUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(User.Role.valueOf(signUpRequest.getRole().toUpperCase()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);

        userRepository.save(user);
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationTokenAndNotExpired(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpires(null);

        userRepository.save(user);
    }

    public JwtAuthenticationResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        UUID userId = UUID.fromString(tokenProvider.getUserIdFromToken(refreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return new JwtAuthenticationResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    public void updateLastLogin(String email) {
        userRepository.updateLastLoginByEmail(email, LocalDateTime.now());
        userRepository.updateFailedLoginAttempts(email, 0);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String resetToken = generateSecureToken();
        user.setResetToken(resetToken);
        user.setResetTokenExpires(LocalDateTime.now().plusHours(1));

        userRepository.save(user);
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetTokenAndNotExpired(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpires(null);

        userRepository.save(user);
        userSessionRepository.revokeAllUserTokens(user.getId());
    }

    public void logout(String token) {
        String tokenHash = DigestUtils.md5DigestAsHex(token.getBytes());
        userSessionRepository.revokeToken(tokenHash);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}