package com.documentanalysis.util.security;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    // At least one uppercase, one lowercase, one digit, one special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{"
                    + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    private static final Pattern COMMON_PATTERNS = Pattern.compile(
            ".*(password|123456|qwerty|admin|letmein|welcome|monkey|dragon).*",
            Pattern.CASE_INSENSITIVE);

    public static ValidationResult validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }

        if (password.length() < MIN_LENGTH) {
            return new ValidationResult(false, "Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            return new ValidationResult(false, "Password must be less than " + MAX_LENGTH + " characters long");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false,
                    "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }

        if (COMMON_PATTERNS.matcher(password).matches()) {
            return new ValidationResult(false, "Password is too common. Please choose a stronger password");
        }

        return new ValidationResult(true, "Password is valid");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}