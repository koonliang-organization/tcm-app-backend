package com.tcm.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.dto.ApiResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter implements Filter {

    private final ObjectMapper objectMapper;

    @Value("${app.security.rate-limit.auth-requests-per-hour:100}")
    private int authRequestsPerHour;

    @Value("${app.security.rate-limit.guest-requests-per-hour:100}")
    private int guestRequestsPerHour;

    @Value("${app.security.rate-limit.authenticated-requests-per-hour:1000}")
    private int authenticatedRequestsPerHour;

    // Sliding window rate limiting using concurrent maps
    private final ConcurrentHashMap<String, SlidingWindow> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientKey = getClientKey(httpRequest);
        String path = httpRequest.getRequestURI();

        int requestLimit = determineRequestLimit(httpRequest, path);

        if (!isRequestAllowed(clientKey, requestLimit)) {
            handleRateLimitExceeded(httpResponse, requestLimit);
            return;
        }

        // Continue with the request
        chain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        // Try to get authenticated user ID first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            return "user:" + authentication.getName();
        }

        // Fall back to IP address for guest users
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
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

    private int determineRequestLimit(HttpServletRequest request, String path) {
        // Authentication endpoints have stricter limits
        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/refresh")) {
            return authRequestsPerHour;
        }

        // Public endpoints for guest users
        if (path.startsWith("/public/")) {
            return guestRequestsPerHour;
        }

        // Authenticated user endpoints
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            return authenticatedRequestsPerHour;
        }

        // Default to guest limit
        return guestRequestsPerHour;
    }

    private boolean isRequestAllowed(String clientKey, int requestLimit) {
        SlidingWindow window = rateLimitMap.computeIfAbsent(clientKey, k -> new SlidingWindow());
        return window.isRequestAllowed(requestLimit);
    }

    private void handleRateLimitExceeded(HttpServletResponse response, int requestLimit)
            throws IOException {

        log.warn("Rate limit exceeded. Limit: {} requests per hour", requestLimit);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestLimit));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("Retry-After", "3600"); // 1 hour in seconds

        ApiResponse<Object> apiResponse = new ApiResponse<>(
            "ERROR",
            String.format("Rate limit exceeded. Maximum %d requests per hour allowed.", requestLimit),
            null
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    /**
     * Sliding window rate limiter implementation
     */
    private static class SlidingWindow {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private static final long WINDOW_SIZE_MS = 60 * 60 * 1000; // 1 hour

        public synchronized boolean isRequestAllowed(int maxRequests) {
            long now = System.currentTimeMillis();
            long currentWindowStart = windowStart.get();

            // Reset window if it's been more than an hour
            if (now - currentWindowStart >= WINDOW_SIZE_MS) {
                windowStart.set(now);
                requestCount.set(0);
            }

            int currentCount = requestCount.get();
            if (currentCount >= maxRequests) {
                return false;
            }

            requestCount.incrementAndGet();
            return true;
        }
    }

    // Cleanup expired entries periodically (simplified version)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        rateLimitMap.entrySet().removeIf(entry -> {
            SlidingWindow window = entry.getValue();
            return now - window.windowStart.get() > 2 * 60 * 60 * 1000; // Clean up after 2 hours
        });
    }
}