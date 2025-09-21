package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "security_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLog {

    public enum EventType {
        LOGIN_SUCCESS("LOGIN_SUCCESS"),
        LOGIN_FAILURE("LOGIN_FAILURE"),
        LOGOUT("LOGOUT"),
        PASSWORD_CHANGE("PASSWORD_CHANGE"),
        ACCOUNT_LOCKED("ACCOUNT_LOCKED"),
        ACCOUNT_UNLOCKED("ACCOUNT_UNLOCKED"),
        ROLE_ASSIGNED("ROLE_ASSIGNED"),
        ROLE_REMOVED("ROLE_REMOVED"),
        USER_CREATED("USER_CREATED"),
        USER_UPDATED("USER_UPDATED"),
        USER_DELETED("USER_DELETED"),
        PERMISSION_DENIED("PERMISSION_DENIED"),
        TOKEN_REFRESH("TOKEN_REFRESH"),
        SESSION_EXPIRED("SESSION_EXPIRED");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    @NotNull(message = "Event type is required")
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Column(name = "ip_address")
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "details", columnDefinition = "JSON")
    private String details;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    public SecurityAuditLog(EventType eventType, AdminUser adminUser, String ipAddress,
                          String userAgent, Boolean success, String details) {
        this.eventType = eventType;
        this.adminUser = adminUser;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.details = details;
    }

    public SecurityAuditLog(EventType eventType, String adminUserEmail, String ipAddress,
                          Boolean success, String details) {
        this.eventType = eventType;
        // Note: adminUser will be null if user doesn't exist
        this.ipAddress = ipAddress;
        this.success = success;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = Instant.now();
    }

    @Override
    public String toString() {
        return "SecurityAuditLog{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", adminUserId=" + (adminUser != null ? adminUser.getId() : null) +
                ", ipAddress='" + ipAddress + '\'' +
                ", success=" + success +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAuditLog)) return false;
        SecurityAuditLog that = (SecurityAuditLog) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}