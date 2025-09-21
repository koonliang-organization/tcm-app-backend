package com.tcm.backend.repository;

import com.tcm.backend.domain.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSession, String> {

    Optional<GuestSession> findByIpAddress(String ipAddress);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.ipAddress = :ipAddress AND gs.lastAccessAt > :cutoffTime")
    Optional<GuestSession> findActiveByIpAddress(@Param("ipAddress") String ipAddress,
                                                @Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.lastAccessAt > :cutoffTime ORDER BY gs.lastAccessAt DESC")
    List<GuestSession> findRecentlyActive(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.lastAccessAt < :cutoffTime")
    List<GuestSession> findInactiveSessions(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.firstAccessAt BETWEEN :startTime AND :endTime ORDER BY gs.firstAccessAt DESC")
    List<GuestSession> findByFirstAccessTimeBetween(@Param("startTime") Instant startTime,
                                                   @Param("endTime") Instant endTime);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.requestCount > :threshold ORDER BY gs.requestCount DESC")
    List<GuestSession> findHighActivitySessions(@Param("threshold") int threshold);

    @Query("SELECT gs FROM GuestSession gs WHERE gs.ipAddress = :ipAddress ORDER BY gs.lastAccessAt DESC")
    List<GuestSession> findAllByIpAddress(@Param("ipAddress") String ipAddress);

    @Modifying
    @Query("DELETE FROM GuestSession gs WHERE gs.lastAccessAt < :cutoffTime")
    int deleteInactiveSessions(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT COUNT(gs) FROM GuestSession gs WHERE gs.lastAccessAt > :since")
    long countActiveSessionsSince(@Param("since") Instant since);

    @Query("SELECT COUNT(gs) FROM GuestSession gs WHERE gs.firstAccessAt > :since")
    long countNewSessionsSince(@Param("since") Instant since);

    @Query("SELECT SUM(gs.requestCount) FROM GuestSession gs WHERE gs.lastAccessAt > :since")
    long sumRequestCountSince(@Param("since") Instant since);

    @Query("SELECT gs.ipAddress, COUNT(gs) as sessionCount, SUM(gs.requestCount) as totalRequests FROM GuestSession gs WHERE gs.lastAccessAt > :since GROUP BY gs.ipAddress HAVING COUNT(gs) > :sessionThreshold OR SUM(gs.requestCount) > :requestThreshold ORDER BY totalRequests DESC")
    List<Object[]> findSuspiciousGuestActivity(@Param("since") Instant since,
                                              @Param("sessionThreshold") long sessionThreshold,
                                              @Param("requestThreshold") long requestThreshold);

    @Query("SELECT DATE(gs.firstAccessAt), COUNT(gs) FROM GuestSession gs WHERE gs.firstAccessAt > :since GROUP BY DATE(gs.firstAccessAt) ORDER BY DATE(gs.firstAccessAt)")
    List<Object[]> getDailyNewGuestStats(@Param("since") Instant since);
}