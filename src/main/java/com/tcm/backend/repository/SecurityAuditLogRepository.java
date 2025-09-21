package com.tcm.backend.repository;

import com.tcm.backend.domain.SecurityAuditLog;
import com.tcm.backend.domain.SecurityAuditLog.EventType;
import com.tcm.backend.domain.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    Page<SecurityAuditLog> findByEventTypeOrderByTimestampDesc(EventType eventType, Pageable pageable);

    Page<SecurityAuditLog> findByAdminUserOrderByTimestampDesc(AdminUser adminUser, Pageable pageable);

    Page<SecurityAuditLog> findByAdminUserIdOrderByTimestampDesc(String adminUserId, Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.timestamp BETWEEN :startTime AND :endTime ORDER BY sal.timestamp DESC")
    Page<SecurityAuditLog> findByTimestampBetween(@Param("startTime") Instant startTime,
                                                 @Param("endTime") Instant endTime,
                                                 Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.eventType = :eventType AND sal.timestamp BETWEEN :startTime AND :endTime ORDER BY sal.timestamp DESC")
    Page<SecurityAuditLog> findByEventTypeAndTimestampBetween(@Param("eventType") EventType eventType,
                                                             @Param("startTime") Instant startTime,
                                                             @Param("endTime") Instant endTime,
                                                             Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.adminUser.id = :adminUserId AND sal.timestamp BETWEEN :startTime AND :endTime ORDER BY sal.timestamp DESC")
    Page<SecurityAuditLog> findByAdminUserIdAndTimestampBetween(@Param("adminUserId") String adminUserId,
                                                               @Param("startTime") Instant startTime,
                                                               @Param("endTime") Instant endTime,
                                                               Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.success = :success ORDER BY sal.timestamp DESC")
    Page<SecurityAuditLog> findBySuccessOrderByTimestampDesc(@Param("success") Boolean success, Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.ipAddress = :ipAddress ORDER BY sal.timestamp DESC")
    Page<SecurityAuditLog> findByIpAddressOrderByTimestampDesc(@Param("ipAddress") String ipAddress, Pageable pageable);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.eventType IN :eventTypes AND sal.success = false AND sal.timestamp > :since ORDER BY sal.timestamp DESC")
    List<SecurityAuditLog> findRecentFailedEvents(@Param("eventTypes") List<EventType> eventTypes,
                                                 @Param("since") Instant since);

    @Query("SELECT sal FROM SecurityAuditLog sal WHERE sal.eventType = :eventType AND sal.ipAddress = :ipAddress AND sal.timestamp > :since")
    List<SecurityAuditLog> findRecentEventsByTypeAndIp(@Param("eventType") EventType eventType,
                                                       @Param("ipAddress") String ipAddress,
                                                       @Param("since") Instant since);

    @Query("SELECT COUNT(sal) FROM SecurityAuditLog sal WHERE sal.eventType = :eventType AND sal.timestamp > :since")
    long countByEventTypeAndTimestampAfter(@Param("eventType") EventType eventType,
                                          @Param("since") Instant since);

    @Query("SELECT COUNT(sal) FROM SecurityAuditLog sal WHERE sal.success = false AND sal.timestamp > :since")
    long countFailedEventsSince(@Param("since") Instant since);

    @Query("SELECT COUNT(sal) FROM SecurityAuditLog sal WHERE sal.eventType = :eventType AND sal.ipAddress = :ipAddress AND sal.timestamp > :since")
    long countByEventTypeAndIpAddressAndTimestampAfter(@Param("eventType") EventType eventType,
                                                       @Param("ipAddress") String ipAddress,
                                                       @Param("since") Instant since);

    @Query("SELECT sal.ipAddress, COUNT(sal) as count FROM SecurityAuditLog sal WHERE sal.eventType = 'LOGIN_FAILURE' AND sal.timestamp > :since GROUP BY sal.ipAddress HAVING COUNT(sal) > :threshold ORDER BY count DESC")
    List<Object[]> findSuspiciousIpAddresses(@Param("since") Instant since,
                                            @Param("threshold") long threshold);

    @Query("DELETE FROM SecurityAuditLog sal WHERE sal.timestamp < :cutoffTime")
    int deleteOldLogs(@Param("cutoffTime") Instant cutoffTime);
}