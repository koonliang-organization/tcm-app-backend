package com.tcm.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(
    @NotBlank(message = "Result is required")
    String result,

    @NotBlank(message = "Message is required")
    String message,

    TokenData data
) {
    public record TokenData(
        @NotBlank(message = "Access token is required")
        String accessToken,

        @NotBlank(message = "Refresh token is required")
        String refreshToken,

        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Email is required")
        String email,

        String fullName,

        @NotNull(message = "Roles are required")
        Set<String> roles
    ) {}

    public static AuthenticationResponse success(String message, TokenData data) {
        return new AuthenticationResponse("SUCCESS", message, data);
    }

    public static AuthenticationResponse error(String message) {
        return new AuthenticationResponse("ERROR", message, null);
    }
}