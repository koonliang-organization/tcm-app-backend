package com.tcm.backend.repository;

import com.tcm.backend.domain.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AdminUser> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AdminUser> findByEmailAndIsEnabledTrue(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AdminUser> findByIdAndIsEnabledTrue(String id);

    boolean existsByEmail(String email);

    @Query("SELECT au FROM AdminUser au WHERE au.isEnabled = true AND au.isLocked = false")
    @EntityGraph(attributePaths = {"roles"})
    Page<AdminUser> findAllActiveUsers(Pageable pageable);

    @Query("SELECT au FROM AdminUser au WHERE au.isLocked = true")
    @EntityGraph(attributePaths = {"roles"})
    List<AdminUser> findAllLockedUsers();

    @Query("SELECT au FROM AdminUser au WHERE au.failedLoginAttempts >= :threshold")
    List<AdminUser> findUsersWithFailedLoginAttempts(@Param("threshold") int threshold);

    @Query("SELECT au FROM AdminUser au WHERE au.passwordExpiresAt < :now")
    List<AdminUser> findUsersWithExpiredPasswords(@Param("now") Instant now);

    @Query("SELECT au FROM AdminUser au WHERE au.passwordExpiresAt BETWEEN :now AND :warningTime")
    List<AdminUser> findUsersWithPasswordsExpiringSoon(@Param("now") Instant now,
                                                        @Param("warningTime") Instant warningTime);

    @Query("SELECT au FROM AdminUser au WHERE au.lastLoginAt < :cutoffTime OR au.lastLoginAt IS NULL")
    List<AdminUser> findInactiveUsers(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT au FROM AdminUser au JOIN au.roles r WHERE r.name = :roleName")
    @EntityGraph(attributePaths = {"roles"})
    List<AdminUser> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT au FROM AdminUser au JOIN au.roles r JOIN r.permissions p WHERE p.name = :permissionName")
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<AdminUser> findByPermissionName(@Param("permissionName") String permissionName);

    @Query("SELECT COUNT(au) FROM AdminUser au WHERE au.isEnabled = true")
    long countActiveUsers();

    @Query("SELECT COUNT(au) FROM AdminUser au WHERE au.isLocked = true")
    long countLockedUsers();

    @Query("SELECT COUNT(au) FROM AdminUser au WHERE au.lastLoginAt > :since")
    long countRecentlyActiveUsers(@Param("since") Instant since);
}