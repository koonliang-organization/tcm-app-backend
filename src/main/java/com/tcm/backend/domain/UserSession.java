package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    @NotNull(message = "Admin user is required")
    private AdminUser adminUser;

    @Column(name = "refresh_token_hash", nullable = false)
    @NotBlank(message = "Refresh token hash is required")
    @Size(max = 255, message = "Refresh token hash must not exceed 255 characters")
    private String refreshTokenHash;

    @Column(name = "ip_address")
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public UserSession(AdminUser adminUser, String refreshTokenHash, String ipAddress,
                      String userAgent, Instant expiresAt) {
        this.adminUser = adminUser;
        this.refreshTokenHash = refreshTokenHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public void invalidate() {
        this.isActive = false;
    }

    public void refreshExpiration(Instant newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "id='" + id + '\'' +
                ", adminUserId=" + (adminUser != null ? adminUser.getId() : null) +
                ", ipAddress='" + ipAddress + '\'' +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSession)) return false;
        UserSession that = (UserSession) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}