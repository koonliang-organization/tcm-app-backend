package com.tcm.backend.config.security;

import com.tcm.backend.domain.AdminUser;
import com.tcm.backend.repository.AdminUserRepository;
import com.tcm.backend.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && jwtTokenService.isTokenValid(jwt)) {
                String userId = jwtTokenService.getUserIdFromToken(jwt);
                String tokenType = jwtTokenService.getTokenType(jwt);

                // Only allow access tokens for authentication (not refresh tokens)
                if ("access".equals(tokenType) && userId != null) {
                    Optional<AdminUser> optionalUser = adminUserRepository.findByIdAndIsEnabledTrue(userId);

                    if (optionalUser.isPresent()) {
                        AdminUser user = optionalUser.get();

                        // Check if user account is still valid
                        if (user.isAccountNonLocked() && user.isCredentialsNonExpired()) {
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.debug("Set authentication for user: {} with roles: {}",
                                    user.getEmail(), user.getRoleNames());
                        } else {
                            log.warn("User account not valid: {} (locked: {}, credentials expired: {})",
                                    user.getEmail(), !user.isAccountNonLocked(), !user.isCredentialsNonExpired());
                        }
                    } else {
                        log.warn("User not found or disabled for token: {}", userId);
                    }
                } else if (tokenType != null && !"access".equals(tokenType)) {
                    log.warn("Invalid token type for authentication: {}", tokenType);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Try Authorization header first
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Try cookie for web clients
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Don't filter public endpoints
        if (path.startsWith("/public/")) {
            return true;
        }

        // Don't filter authentication endpoints
        if (path.startsWith("/api/v1/auth/login") ||
            path.startsWith("/api/v1/auth/refresh")) {
            return true;
        }

        // Don't filter actuator endpoints
        if (path.startsWith("/actuator/")) {
            return true;
        }

        // Don't filter OpenAPI/Swagger endpoints
        if (path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui")) {
            return true;
        }

        return false;
    }
}