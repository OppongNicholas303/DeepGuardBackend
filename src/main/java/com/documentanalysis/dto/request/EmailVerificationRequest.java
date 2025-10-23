package com.documentanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;

public class EmailVerificationRequest {
    @NotBlank(message = "Verification token is required")
    private String token;

    public EmailVerificationRequest() {}
    public EmailVerificationRequest(String token) { this.token = token; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}