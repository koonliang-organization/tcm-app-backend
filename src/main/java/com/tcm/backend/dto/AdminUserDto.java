package com.tcm.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserDto(
    @NotBlank(message = "User ID is required")
    String id,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    String firstName,

    String lastName,

    String fullName,

    @NotNull(message = "Enabled status is required")
    Boolean isEnabled,

    @NotNull(message = "Locked status is required")
    Boolean isLocked,

    Instant lastLoginAt,

    Instant passwordExpiresAt,

    @NotNull(message = "Roles are required")
    Set<String> roles,

    @NotNull(message = "Created at is required")
    Instant createdAt,

    @NotNull(message = "Updated at is required")
    Instant updatedAt
) {}