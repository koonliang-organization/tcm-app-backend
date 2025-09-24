package com.tcm.backend.repository;

import com.tcm.backend.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByName(String name);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByNameAndIsActiveTrue(String name);

    @Query("SELECT r FROM Role r WHERE r.isActive = true")
    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findAllActiveRoles();

    @Query("SELECT r FROM Role r WHERE r.name IN :roleNames AND r.isActive = true")
    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findByNameInAndIsActiveTrue(@Param("roleNames") Set<String> roleNames);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName AND r.isActive = true")
    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findByPermissionName(@Param("permissionName") String permissionName);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.resource = :resource AND p.action = :action AND r.isActive = true")
    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findByResourceAndAction(@Param("resource") String resource, @Param("action") String action);

    boolean existsByName(String name);

    @Query("SELECT COUNT(r) FROM Role r WHERE r.isActive = true")
    long countActiveRoles();
}