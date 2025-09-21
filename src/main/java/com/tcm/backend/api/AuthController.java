package com.tcm.backend.api;

import com.tcm.backend.dto.*;
import com.tcm.backend.service.AuthenticationService;
import com.tcm.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.tcm.backend.domain.AdminUser;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AdminUserService adminUserService;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.lockout.duration-minutes:30}")
    private int lockoutDurationMinutes;

    @PostMapping("/login")
    @Operation(summary = "Authenticate admin user", description = "Login with email and password to get access and refresh tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AuthenticationResponse.TokenData>> login(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            var loginResult = authenticationService.authenticate(request, ipAddress, userAgent);

            // Set refresh token as HTTP-only cookie
            Cookie refreshTokenCookie = new Cookie("refresh_token", loginResult.refreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(cookieSecure);
            refreshTokenCookie.setPath("/api/v1/auth");
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            refreshTokenCookie.setAttribute("SameSite", "Strict");
            httpResponse.addCookie(refreshTokenCookie);

            AuthenticationResponse.TokenData tokenData = new AuthenticationResponse.TokenData(
                loginResult.accessToken(),
                loginResult.refreshToken(),
                loginResult.adminUser().getId(),
                loginResult.adminUser().getEmail(),
                loginResult.adminUser().getFullName(),
                loginResult.adminUser().getRoleNames()
            );

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Login successful",
                tokenData
            ));

        } catch (org.springframework.security.authentication.LockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "Invalid email or password", null)
            );
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred during login", null)
            );
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AuthenticationResponse response = authenticationService.refreshToken(request, ipAddress, userAgent);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Token refreshed successfully",
                response
            ));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "Invalid refresh token", null)
            );
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred during token refresh", null)
            );
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate the current session and refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Try to get refresh token from request body or cookie
            String refreshToken = null;
            if (request != null && request.refreshToken() != null) {
                refreshToken = request.refreshToken();
            } else if (httpRequest.getCookies() != null) {
                for (Cookie cookie : httpRequest.getCookies()) {
                    if ("refresh_token".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (refreshToken != null) {
                authenticationService.logout(refreshToken, ipAddress, userAgent);
            }

            // Clear refresh token cookie
            Cookie refreshTokenCookie = new Cookie("refresh_token", "");
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setPath("/api/v1/auth");
            refreshTokenCookie.setMaxAge(0); // Delete cookie
            httpResponse.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Logout successful",
                null
            ));

        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred during logout", null)
            );
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AdminUserDto>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof AdminUser) {
                AdminUser currentUser = (AdminUser) authentication.getPrincipal();

                AdminUserDto userDto = adminUserService.getUserById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                    "SUCCESS",
                    "User information retrieved successfully",
                    userDto
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "Not authenticated", null)
            );

        } catch (Exception e) {
            log.error("Get current user error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while retrieving user information", null)
            );
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the password for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request or validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof AdminUser) {
                AdminUser currentUser = (AdminUser) authentication.getPrincipal();
                String ipAddress = getClientIpAddress(httpRequest);
                String userAgent = httpRequest.getHeader("User-Agent");

                adminUserService.changePassword(currentUser.getId(), request, ipAddress, userAgent);

                return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                    "SUCCESS",
                    "Password changed successfully",
                    null
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "Not authenticated", null)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Change password error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while changing password", null)
            );
        }
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all sessions", description = "Invalidate all sessions for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logged out from all sessions successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> logoutAll(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof AdminUser) {
                AdminUser currentUser = (AdminUser) authentication.getPrincipal();
                String ipAddress = getClientIpAddress(httpRequest);
                String userAgent = httpRequest.getHeader("User-Agent");

                authenticationService.logoutAllSessions(currentUser.getId(), ipAddress, userAgent);

                // Clear refresh token cookie
                Cookie refreshTokenCookie = new Cookie("refresh_token", "");
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(true);
                refreshTokenCookie.setPath("/api/v1/auth");
                refreshTokenCookie.setMaxAge(0); // Delete cookie
                httpResponse.addCookie(refreshTokenCookie);

                return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                    "SUCCESS",
                    "Logged out from all sessions successfully",
                    null
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "Not authenticated", null)
            );

        } catch (Exception e) {
            log.error("Logout all sessions error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while logging out from all sessions", null)
            );
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}