package com.tcm.backend.config.security;

import com.tcm.backend.domain.AdminUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof AdminUser)) {
            return false;
        }

        AdminUser user = (AdminUser) authentication.getPrincipal();
        String permissionName = permission.toString().toUpperCase();

        log.debug("Evaluating permission: {} for user: {}", permissionName, user.getEmail());

        // Check if user has the specific permission
        return user.hasPermission(permissionName);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || permission == null || targetType == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof AdminUser)) {
            return false;
        }

        AdminUser user = (AdminUser) authentication.getPrincipal();
        String permissionName = permission.toString().toUpperCase();
        String resourceType = targetType.toLowerCase();

        log.debug("Evaluating permission: {} on resource: {} with ID: {} for user: {}",
                permissionName, resourceType, targetId, user.getEmail());

        // Check if user has the specific permission for the resource type
        String fullPermissionName = resourceType.toUpperCase() + "_" + permissionName;
        if (user.hasPermission(fullPermissionName)) {
            return true;
        }

        // Check if user has the generic permission
        if (user.hasPermission(permissionName)) {
            return true;
        }

        // Special case: Users can always view their own profile
        if ("user".equals(resourceType) && "READ".equals(permissionName.toUpperCase())) {
            return targetId != null && targetId.toString().equals(user.getId());
        }

        // Special case: Users can modify their own profile (limited fields)
        if ("user".equals(resourceType) && "WRITE".equals(permissionName.toUpperCase())) {
            return targetId != null && targetId.toString().equals(user.getId());
        }

        return false;
    }

    /**
     * Convenience method to check if user has permission on a specific resource
     */
    public boolean hasPermission(AdminUser user, String resource, String action) {
        if (user == null || resource == null || action == null) {
            return false;
        }

        String permissionName = resource.toUpperCase() + "_" + action.toUpperCase();
        return user.hasPermission(permissionName);
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(AdminUser user, String... permissions) {
        if (user == null || permissions == null) {
            return false;
        }

        for (String permission : permissions) {
            if (user.hasPermission(permission.toUpperCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(AdminUser user, String... permissions) {
        if (user == null || permissions == null) {
            return false;
        }

        for (String permission : permissions) {
            if (!user.hasPermission(permission.toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if user has role
     */
    public boolean hasRole(AdminUser user, String roleName) {
        if (user == null || roleName == null) {
            return false;
        }

        String fullRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return user.hasRole(fullRoleName);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(AdminUser user, String... roles) {
        if (user == null || roles == null) {
            return false;
        }

        for (String role : roles) {
            String fullRoleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            if (user.hasRole(fullRoleName)) {
                return true;
            }
        }

        return false;
    }
}