package com.tcm.backend.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 12, max = 255, message = "New password must be between 12 and 255 characters")
    String newPassword
) {
    public ChangePasswordRequest {
        // Compact constructor for validation
        if (currentPassword != null) {
            currentPassword = currentPassword.trim();
        }
        if (newPassword != null) {
            newPassword = newPassword.trim();
        }
    }
}