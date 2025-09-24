package com.tcm.backend.service;

import com.tcm.backend.domain.AdminUser;
import com.tcm.backend.domain.Permission;
import com.tcm.backend.domain.Role;
import com.tcm.backend.repository.AdminUserRepository;
import com.tcm.backend.repository.PermissionRepository;
import com.tcm.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeederService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;

    @Value("${app.seeding.enabled:true}")
    private boolean seedingEnabled;

    @Value("${app.seeding.admin.email:admin@tcmapp.com}")
    private String adminEmail;

    @Value("${app.seeding.admin.password:}")
    private String adminPassword;

    @Value("${app.seeding.admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.seeding.admin.last-name:Administrator}")
    private String adminLastName;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        if (!seedingEnabled) {
            log.info("Data seeding is disabled");
            return;
        }

        log.info("Starting data seeding process...");

        try {
            seedPermissions();
            seedRoles();
            seedAdminUser();
            log.info("Data seeding completed successfully");
        } catch (Exception e) {
            log.error("Error during data seeding", e);
            throw new RuntimeException("Failed to seed initial data", e);
        }
    }

    private void seedPermissions() {
        log.info("Seeding permissions...");

        List<Permission> permissions = Arrays.asList(
            // Herb management permissions
            new Permission("HERBS_READ", "herbs", "read", "Read herbs and view herb details"),
            new Permission("HERBS_WRITE", "herbs", "write", "Create and update herbs"),
            new Permission("HERBS_DELETE", "herbs", "delete", "Delete herbs"),
            new Permission("HERBS_MANAGE", "herbs", "manage", "Full herb management access"),

            // Formula management permissions
            new Permission("FORMULAS_READ", "formulas", "read", "Read formulas and view formula details"),
            new Permission("FORMULAS_WRITE", "formulas", "write", "Create and update formulas"),
            new Permission("FORMULAS_DELETE", "formulas", "delete", "Delete formulas"),
            new Permission("FORMULAS_MANAGE", "formulas", "manage", "Full formula management access"),

            // User management permissions
            new Permission("USERS_READ", "users", "read", "View user accounts and profiles"),
            new Permission("USERS_WRITE", "users", "write", "Create and update user accounts"),
            new Permission("USERS_DELETE", "users", "delete", "Delete user accounts"),
            new Permission("USERS_MANAGE", "users", "manage", "Full user management access"),

            // System administration permissions
            new Permission("SYSTEM_CONFIG", "system", "config", "Access system configuration"),
            new Permission("SYSTEM_LOGS", "system", "logs", "View system logs and audit trails"),
            new Permission("SYSTEM_MONITOR", "system", "monitor", "Monitor system health and performance"),
            new Permission("SYSTEM_BACKUP", "system", "backup", "Perform system backups"),

            // Publishing permissions
            new Permission("PUBLISH_READ", "publish", "read", "View publication status and releases"),
            new Permission("PUBLISH_WRITE", "publish", "write", "Create and manage publications"),
            new Permission("PUBLISH_EXECUTE", "publish", "execute", "Execute publication processes"),

            // Content moderation permissions
            new Permission("CONTENT_MODERATE", "content", "moderate", "Moderate user-generated content"),
            new Permission("CONTENT_REVIEW", "content", "review", "Review content before publication")
        );

        for (Permission permission : permissions) {
            Optional<Permission> existing = permissionRepository.findByName(permission.getName());
            if (existing.isEmpty()) {
                permissionRepository.save(permission);
                log.debug("Created permission: {}", permission.getName());
            }
        }

        log.info("Permissions seeding completed");
    }

    private void seedRoles() {
        log.info("Seeding roles...");

        // Create ROLE_VIEWER
        Role viewerRole = createOrUpdateRole("ROLE_VIEWER", "Viewer with read-only access to application data");
        addPermissionsToRole(viewerRole, Arrays.asList(
            "HERBS_READ", "FORMULAS_READ", "PUBLISH_READ"
        ));

        // Create ROLE_EDITOR
        Role editorRole = createOrUpdateRole("ROLE_EDITOR", "Editor with content management permissions");
        addPermissionsToRole(editorRole, Arrays.asList(
            "HERBS_READ", "HERBS_WRITE", "HERBS_DELETE",
            "FORMULAS_READ", "FORMULAS_WRITE", "FORMULAS_DELETE",
            "PUBLISH_READ", "PUBLISH_WRITE", "CONTENT_REVIEW"
        ));

        // Create ROLE_ADMIN
        Role adminRole = createOrUpdateRole("ROLE_ADMIN", "Administrator with user and content management access");
        addPermissionsToRole(adminRole, Arrays.asList(
            "HERBS_MANAGE", "FORMULAS_MANAGE",
            "USERS_READ", "USERS_WRITE",
            "PUBLISH_READ", "PUBLISH_WRITE", "PUBLISH_EXECUTE",
            "CONTENT_MODERATE", "CONTENT_REVIEW",
            "SYSTEM_MONITOR", "SYSTEM_LOGS"
        ));

        // Create ROLE_SUPER_ADMIN
        Role superAdminRole = createOrUpdateRole("ROLE_SUPER_ADMIN", "Super Administrator with full system access");
        List<Permission> allPermissions = permissionRepository.findAll();
        for (Permission permission : allPermissions) {
            superAdminRole.addPermission(permission);
        }
        roleRepository.save(superAdminRole);

        log.info("Roles seeding completed");
    }

    private Role createOrUpdateRole(String roleName, String description) {
        Optional<Role> existingRole = roleRepository.findByName(roleName);
        if (existingRole.isPresent()) {
            log.debug("Role already exists: {}", roleName);
            return existingRole.get();
        }

        Role role = new Role(roleName, description);
        role = roleRepository.save(role);
        log.debug("Created role: {}", roleName);
        return role;
    }

    private void addPermissionsToRole(Role role, List<String> permissionNames) {
        for (String permissionName : permissionNames) {
            Optional<Permission> permission = permissionRepository.findByName(permissionName);
            if (permission.isPresent()) {
                role.addPermission(permission.get());
            } else {
                log.warn("Permission not found: {}", permissionName);
            }
        }
        roleRepository.save(role);
    }

    private void seedAdminUser() {
        log.info("Seeding admin user...");

        Optional<AdminUser> existingAdmin = adminUserRepository.findByEmail(adminEmail);
        if (existingAdmin.isPresent()) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        String password = adminPassword;
        if (password == null || password.trim().isEmpty()) {
            password = passwordService.generateSecurePassword(16);
            log.warn("No admin password configured. Generated secure password: {}", password);
            log.warn("IMPORTANT: Save this password securely and change it after first login!");
        }

        // Validate the password
        /*PasswordService.PasswordValidationResult validation = passwordService.validatePassword(password);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Admin password does not meet security requirements: " + validation.errorMessage());
        }*/

        String hashedPassword = passwordService.hashPassword(password);

        AdminUser adminUser = new AdminUser(adminEmail, hashedPassword, adminFirstName, adminLastName);
        adminUser.setCreatedBy("SYSTEM");
        adminUser.setPasswordExpiresAt(passwordService.calculatePasswordExpirationDate());

        // Assign ROLE_SUPER_ADMIN
        Optional<Role> superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN");
        if (superAdminRole.isPresent()) {
            adminUser.addRole(superAdminRole.get());
        } else {
            throw new RuntimeException("ROLE_SUPER_ADMIN not found. Ensure roles are seeded before users.");
        }

        adminUserRepository.save(adminUser);

        log.info("Admin user created successfully: {}", adminEmail);
        log.info("Admin user has been assigned ROLE_SUPER_ADMIN with full system access");
    }
}