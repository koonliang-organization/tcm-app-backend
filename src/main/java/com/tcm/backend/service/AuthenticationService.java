package com.tcm.backend.service;

import com.tcm.backend.domain.AdminUser;
import com.tcm.backend.domain.UserSession;
import com.tcm.backend.domain.SecurityAuditLog;
import com.tcm.backend.dto.AuthenticationRequest;
import com.tcm.backend.dto.AuthenticationResponse;
import com.tcm.backend.dto.RefreshTokenRequest;
import com.tcm.backend.repository.AdminUserRepository;
import com.tcm.backend.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AdminUserRepository adminUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenService jwtTokenService;
    private final SecurityAuditService securityAuditService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.lockout.duration-minutes:30}")
    private int lockoutDurationMinutes;

    public record LoginResult(
            String accessToken,
            String refreshToken,
            AdminUser adminUser,
            UserSession session
    ) {}

    @Transactional
    public LoginResult authenticate(AuthenticationRequest request, String ipAddress, String userAgent) {
        try {
            // Find user by email
            Optional<AdminUser> optionalUser = adminUserRepository.findByEmail(request.email());
            if (optionalUser.isEmpty()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGIN_FAILURE,
                        null,
                        ipAddress,
                        userAgent,
                        false,
                        "User not found: " + request.email()
                );
                throw new BadCredentialsException("Invalid credentials");
            }

            AdminUser user = optionalUser.get();

            // Check if account is enabled
            if (!user.getIsEnabled()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGIN_FAILURE,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Account disabled"
                );
                throw new DisabledException("Account is disabled");
            }

            // Check if account is locked
            if (user.getIsLocked()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGIN_FAILURE,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Account locked"
                );
                throw new LockedException("Account is locked due to too many failed login attempts");
            }

            // Check password expiration
            if (user.isPasswordExpired()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGIN_FAILURE,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Password expired"
                );
                throw new BadCredentialsException("Password has expired");
            }

            // Verify password
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                // Increment failed login attempts
                user.incrementFailedLoginAttempts(lockoutDurationMinutes);
                adminUserRepository.save(user);

                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGIN_FAILURE,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Invalid password. Attempts: " + user.getFailedLoginAttempts()
                );

                if (user.getIsLocked()) {
                    securityAuditService.logEvent(
                            SecurityAuditLog.EventType.ACCOUNT_LOCKED,
                            user,
                            ipAddress,
                            userAgent,
                            true,
                            "Account locked after " + user.getFailedLoginAttempts() + " failed attempts"
                    );
                    throw new LockedException("Account has been locked due to too many failed login attempts");
                }

                throw new BadCredentialsException("Invalid credentials");
            }

            // Generate tokens
            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user);

            // Create session
            UserSession session = new UserSession(
                    user,
                    hashToken(refreshToken),
                    ipAddress,
                    userAgent,
                    Instant.now().plus(jwtTokenService.getRefreshTokenExpirationDays(), ChronoUnit.DAYS)
            );
            session = userSessionRepository.save(session);

            // Update user login info
            user.recordSuccessfulLogin();
            adminUserRepository.save(user);

            // Log successful login
            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.LOGIN_SUCCESS,
                    user,
                    ipAddress,
                    userAgent,
                    true,
                    "Successful login"
            );

            return new LoginResult(accessToken, refreshToken, user, session);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.LOGIN_FAILURE,
                    null,
                    ipAddress,
                    userAgent,
                    false,
                    "Unexpected error: " + e.getMessage()
            );
            throw new BadCredentialsException("Authentication failed");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        try {
            // Validate refresh token
            if (!jwtTokenService.canRefresh(request.refreshToken())) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            String userId = jwtTokenService.getUserIdFromToken(request.refreshToken());
            if (userId == null) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            // Find user
            Optional<AdminUser> optionalUser = adminUserRepository.findByIdAndIsEnabledTrue(userId);
            if (optionalUser.isEmpty()) {
                throw new BadCredentialsException("User not found or disabled");
            }

            AdminUser user = optionalUser.get();

            // Find session by refresh token hash
            String tokenHash = hashToken(request.refreshToken());
            Optional<UserSession> optionalSession = userSessionRepository.findByRefreshTokenHashAndIsActiveTrue(tokenHash);
            if (optionalSession.isEmpty()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.TOKEN_REFRESH,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Invalid refresh token session"
                );
                throw new BadCredentialsException("Invalid refresh token session");
            }

            UserSession session = optionalSession.get();
            if (!session.isValid()) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.TOKEN_REFRESH,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Expired refresh token session"
                );
                throw new BadCredentialsException("Refresh token session expired");
            }

            // Generate new access token
            String newAccessToken = jwtTokenService.generateAccessToken(user);

            // Log successful token refresh
            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.TOKEN_REFRESH,
                    user,
                    ipAddress,
                    userAgent,
                    true,
                    "Token refreshed successfully"
            );

            return new AuthenticationResponse(
                    "SUCCESS",
                    "Token refreshed successfully",
                    new AuthenticationResponse.TokenData(
                            newAccessToken,
                            request.refreshToken(), // Keep the same refresh token
                            user.getId(),
                            user.getEmail(),
                            user.getFullName(),
                            user.getRoleNames()
                    )
            );

        } catch (Exception e) {
            log.error("Error refreshing token", e);
            throw new BadCredentialsException("Token refresh failed");
        }
    }

    @Transactional
    public void logout(String refreshToken, String ipAddress, String userAgent) {
        try {
            if (refreshToken != null && jwtTokenService.isTokenValid(refreshToken)) {
                String userId = jwtTokenService.getUserIdFromToken(refreshToken);
                if (userId != null) {
                    AdminUser user = adminUserRepository.findById(userId).orElse(null);

                    // Invalidate session
                    String tokenHash = hashToken(refreshToken);
                    userSessionRepository.findByRefreshTokenHashAndIsActiveTrue(tokenHash)
                            .ifPresent(session -> {
                                session.invalidate();
                                userSessionRepository.save(session);
                            });

                    // Log logout
                    securityAuditService.logEvent(
                            SecurityAuditLog.EventType.LOGOUT,
                            user,
                            ipAddress,
                            userAgent,
                            true,
                            "User logged out"
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error during logout", e);
        }
    }

    @Transactional
    public void logoutAllSessions(String userId, String ipAddress, String userAgent) {
        try {
            AdminUser user = adminUserRepository.findById(userId).orElse(null);
            if (user != null) {
                // Deactivate all sessions for user
                userSessionRepository.deactivateAllSessionsForUserId(userId);

                // Log logout all
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.LOGOUT,
                        user,
                        ipAddress,
                        userAgent,
                        true,
                        "All sessions logged out"
                );
            }
        } catch (Exception e) {
            log.error("Error logging out all sessions", e);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public boolean isValidSession(String refreshToken) {
        if (refreshToken == null || !jwtTokenService.isTokenValid(refreshToken)) {
            return false;
        }

        String tokenHash = hashToken(refreshToken);
        return userSessionRepository.findByRefreshTokenHashAndIsActiveTrue(tokenHash)
                .map(UserSession::isValid)
                .orElse(false);
    }
}