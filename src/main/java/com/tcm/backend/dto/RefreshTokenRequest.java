package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {
    public RefreshTokenRequest {
        // Compact constructor for validation
        if (refreshToken != null) {
            refreshToken = refreshToken.trim();
        }
    }
}