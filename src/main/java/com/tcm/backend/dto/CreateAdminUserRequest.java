package com.tcm.backend.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record CreateAdminUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 255, message = "Password must be between 12 and 255 characters")
    String password,

    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    Set<String> roleNames
) {
    public CreateAdminUserRequest {
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
        if (roleNames != null) {
            roleNames = Set.copyOf(roleNames); // Make immutable
        }
    }
}