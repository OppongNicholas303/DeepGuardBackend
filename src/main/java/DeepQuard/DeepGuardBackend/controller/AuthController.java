package DeepQuard.DeepGuardBackend.controller;


import DeepQuard.DeepGuardBackend.dto.request.*;
import DeepQuard.DeepGuardBackend.dto.response.JwtAuthenticationResponse;
import DeepQuard.DeepGuardBackend.service.auth.AuthService;
import DeepQuard.DeepGuardBackend.service.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Update last login
        authService.updateLastLogin(loginRequest.getEmail());

        JwtAuthenticationResponse response = new JwtAuthenticationResponse(jwt, refreshToken, "Bearer");

        return ResponseEntity.ok(new ApiResponse<>(true, response, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            authService.registerUser(signUpRequest);
            return ResponseEntity.ok(new ApiResponse<>(true, null,
                    "User registered successfully. Please check your email for verification."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        try {
            JwtAuthenticationResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(new ApiResponse<>(true, response, "Token refreshed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            authService.verifyEmail(request.getToken());
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Email verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(new ApiResponse<>(true, null,
                    "Password reset instructions sent to your email"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7); // Remove "Bearer " prefix
            authService.logout(jwt);
            return ResponseEntity.ok(new ApiResponse<>(true, null, "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }
}