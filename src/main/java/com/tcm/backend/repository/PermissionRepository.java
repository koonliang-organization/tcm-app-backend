package com.tcm.backend.repository;

import com.tcm.backend.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByName(String name);

    Optional<Permission> findByResourceAndAction(String resource, String action);

    List<Permission> findByResource(String resource);

    List<Permission> findByAction(String action);

    @Query("SELECT p FROM Permission p WHERE p.name IN :permissionNames")
    List<Permission> findByNameIn(@Param("permissionNames") Set<String> permissionNames);

    @Query("SELECT p FROM Permission p WHERE p.resource IN :resources")
    List<Permission> findByResourceIn(@Param("resources") Set<String> resources);

    @Query("SELECT DISTINCT p.resource FROM Permission p ORDER BY p.resource")
    List<String> findAllResources();

    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findAllActions();

    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName")
    List<Permission> findByRoleName(@Param("roleName") String roleName);

    boolean existsByName(String name);

    boolean existsByResourceAndAction(String resource, String action);

    @Query("SELECT COUNT(p) FROM Permission p")
    long countAllPermissions();
}