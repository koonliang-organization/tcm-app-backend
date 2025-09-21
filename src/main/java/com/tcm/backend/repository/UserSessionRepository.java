package com.tcm.backend.repository;

import com.tcm.backend.domain.UserSession;
import com.tcm.backend.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByRefreshTokenHashAndIsActiveTrue(String refreshTokenHash);

    List<UserSession> findByAdminUserAndIsActiveTrue(AdminUser adminUser);

    List<UserSession> findByAdminUserIdAndIsActiveTrue(String adminUserId);

    @Query("SELECT s FROM UserSession s WHERE s.adminUser.id = :adminUserId AND s.isActive = true ORDER BY s.createdAt DESC")
    List<UserSession> findActiveSessionsByUserId(@Param("adminUserId") String adminUserId);

    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now")
    List<UserSession> findExpiredSessions(@Param("now") Instant now);

    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.expiresAt < :now")
    List<UserSession> findExpiredActiveSessions(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.adminUser = :adminUser")
    int deactivateAllSessionsForUser(@Param("adminUser") AdminUser adminUser);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.adminUser.id = :adminUserId")
    int deactivateAllSessionsForUserId(@Param("adminUserId") String adminUserId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now")
    int deactivateExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :cutoffTime")
    int deleteOldSessions(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.isActive = true")
    long countActiveSessions();

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.adminUser = :adminUser AND s.isActive = true")
    long countActiveSessionsForUser(@Param("adminUser") AdminUser adminUser);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.adminUser.id = :adminUserId AND s.isActive = true")
    long countActiveSessionsForUserId(@Param("adminUserId") String adminUserId);

    @Query("SELECT s FROM UserSession s WHERE s.ipAddress = :ipAddress AND s.isActive = true")
    List<UserSession> findActiveSessionsByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT s FROM UserSession s WHERE s.createdAt > :since ORDER BY s.createdAt DESC")
    List<UserSession> findRecentSessions(@Param("since") Instant since);
}