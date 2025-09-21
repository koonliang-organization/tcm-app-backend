package com.tcm.backend.service;

import com.tcm.backend.domain.AdminUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;
    private final String issuer;

    public JwtTokenService(
            @Value("${app.jwt.secret:tcm-app-super-secret-key-that-should-be-changed-in-production-and-be-at-least-256-bits}") String secret,
            @Value("${app.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${app.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays,
            @Value("${app.jwt.issuer:tcm-app-backend}") String issuer) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.issuer = issuer;
    }

    public String generateAccessToken(AdminUser adminUser) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        Set<String> roles = adminUser.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        Set<String> permissions = adminUser.getAllPermissions();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", adminUser.getId());
        claims.put("email", adminUser.getEmail());
        claims.put("name", adminUser.getFullName());
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(AdminUser adminUser) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", adminUser.getId());
        claims.put("email", adminUser.getEmail());
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        Claims claims = validateToken(token);
        return claims != null && !isTokenExpired(claims);
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? (String) claims.get("email") : null;
    }

    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null && claims.get("roles") != null) {
            return Set.copyOf((java.util.List<String>) claims.get("roles"));
        }
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null && claims.get("permissions") != null) {
            return Set.copyOf((java.util.List<String>) claims.get("permissions"));
        }
        return Set.of();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public Instant getExpirationTime(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getExpiration().toInstant() : null;
    }

    public boolean canRefresh(String refreshToken) {
        return isTokenValid(refreshToken) && isRefreshToken(refreshToken);
    }

    public long getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }
}