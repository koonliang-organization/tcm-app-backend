package com.tcm.backend.service;

import com.tcm.backend.domain.AdminUser;
import com.tcm.backend.domain.SecurityAuditLog;
import com.tcm.backend.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    @Async
    public void logEvent(SecurityAuditLog.EventType eventType, AdminUser adminUser,
                        String ipAddress, String userAgent, Boolean success, String details) {
        try {
            SecurityAuditLog auditLog = new SecurityAuditLog(
                    eventType, adminUser, ipAddress, userAgent, success, details
            );
            securityAuditLogRepository.save(auditLog);

            // Log to application logs as well
            if (success) {
                log.info("Security event: {} for user {} from IP {} - {}",
                        eventType,
                        adminUser != null ? adminUser.getEmail() : "unknown",
                        ipAddress,
                        details);
            } else {
                log.warn("Security event: {} for user {} from IP {} - {}",
                        eventType,
                        adminUser != null ? adminUser.getEmail() : "unknown",
                        ipAddress,
                        details);
            }
        } catch (Exception e) {
            log.error("Failed to log security audit event", e);
        }
    }

    public void logAuthenticationFailure(String email, String ipAddress, String userAgent, String reason) {
        logEvent(SecurityAuditLog.EventType.LOGIN_FAILURE, null, ipAddress, userAgent, false,
                "Failed login attempt for email: " + email + " - " + reason);
    }

    public void logPermissionDenied(AdminUser adminUser, String resource, String action,
                                  String ipAddress, String userAgent) {
        logEvent(SecurityAuditLog.EventType.PERMISSION_DENIED, adminUser, ipAddress, userAgent, false,
                "Permission denied for resource: " + resource + ", action: " + action);
    }

    public Page<SecurityAuditLog> getAuditLogs(Pageable pageable) {
        return securityAuditLogRepository.findAll(pageable);
    }

    public Page<SecurityAuditLog> getAuditLogsByEventType(SecurityAuditLog.EventType eventType, Pageable pageable) {
        return securityAuditLogRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
    }

    public Page<SecurityAuditLog> getAuditLogsByUser(String adminUserId, Pageable pageable) {
        return securityAuditLogRepository.findByAdminUserIdOrderByTimestampDesc(adminUserId, pageable);
    }

    public Page<SecurityAuditLog> getAuditLogsByDateRange(Instant startTime, Instant endTime, Pageable pageable) {
        return securityAuditLogRepository.findByTimestampBetween(startTime, endTime, pageable);
    }

    public Page<SecurityAuditLog> getFailedEvents(Pageable pageable) {
        return securityAuditLogRepository.findBySuccessOrderByTimestampDesc(false, pageable);
    }

    public Page<SecurityAuditLog> getAuditLogsByIpAddress(String ipAddress, Pageable pageable) {
        return securityAuditLogRepository.findByIpAddressOrderByTimestampDesc(ipAddress, pageable);
    }

    public List<SecurityAuditLog> getRecentFailedLogins(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return securityAuditLogRepository.findRecentEventsByTypeAndIp(
                SecurityAuditLog.EventType.LOGIN_FAILURE, null, since);
    }

    public long getFailedLoginCount(String ipAddress, int minutes) {
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        return securityAuditLogRepository.countByEventTypeAndIpAddressAndTimestampAfter(
                SecurityAuditLog.EventType.LOGIN_FAILURE, ipAddress, since);
    }

    public long getFailedEventCount(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return securityAuditLogRepository.countFailedEventsSince(since);
    }

    public List<Object[]> getSuspiciousIpAddresses(int hours, long threshold) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return securityAuditLogRepository.findSuspiciousIpAddresses(since, threshold);
    }

    public boolean isIpSuspicious(String ipAddress, int minutes, long threshold) {
        return getFailedLoginCount(ipAddress, minutes) >= threshold;
    }

    public void cleanupOldAuditLogs(int retentionDays) {
        try {
            Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            int deletedCount = securityAuditLogRepository.deleteOldLogs(cutoffTime);
            log.info("Cleaned up {} old audit log entries older than {} days", deletedCount, retentionDays);
        } catch (Exception e) {
            log.error("Failed to cleanup old audit logs", e);
        }
    }
}