package com.tcm.backend.dto;

import jakarta.validation.constraints.*;

public record UpdateAdminUserRequest(
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    Boolean isEnabled,

    Boolean isLocked
) {
    public UpdateAdminUserRequest {
        // Compact constructor for validation and normalization
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
    }
}