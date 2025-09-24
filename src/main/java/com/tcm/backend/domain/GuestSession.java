package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;

@Entity
@Table(name = "guest_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestSession {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "ip_address")
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "first_access_at", nullable = false, updatable = false)
    private Instant firstAccessAt;

    @Column(name = "last_access_at", nullable = false)
    private Instant lastAccessAt;

    @Column(name = "request_count", nullable = false)
    @Min(value = 1, message = "Request count must be at least 1")
    private Integer requestCount = 1;

    public GuestSession(String ipAddress, String userAgent) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public void recordRequest() {
        this.requestCount++;
        this.lastAccessAt = Instant.now();
    }

    public boolean isRecentlyActive(long maxInactiveMinutes) {
        Instant cutoff = Instant.now().minusSeconds(maxInactiveMinutes * 60);
        return lastAccessAt.isAfter(cutoff);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.firstAccessAt = now;
        this.lastAccessAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastAccessAt = Instant.now();
    }

    @Override
    public String toString() {
        return "GuestSession{" +
                "id='" + id + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", firstAccessAt=" + firstAccessAt +
                ", lastAccessAt=" + lastAccessAt +
                ", requestCount=" + requestCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuestSession)) return false;
        GuestSession that = (GuestSession) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}