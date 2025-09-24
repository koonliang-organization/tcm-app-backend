package com.tcm.backend.service;

import com.tcm.backend.domain.AdminUser;
import com.tcm.backend.domain.Role;
import com.tcm.backend.domain.SecurityAuditLog;
import com.tcm.backend.dto.AdminUserDto;
import com.tcm.backend.dto.CreateAdminUserRequest;
import com.tcm.backend.dto.UpdateAdminUserRequest;
import com.tcm.backend.dto.ChangePasswordRequest;
import com.tcm.backend.repository.AdminUserRepository;
import com.tcm.backend.repository.RoleRepository;
import com.tcm.backend.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordService passwordService;
    private final SecurityAuditService securityAuditService;

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        return adminUserRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getActiveUsers(Pageable pageable) {
        return adminUserRepository.findAllActiveUsers(pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUserDto> getUserById(String id) {
        return adminUserRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUserDto> getUserByEmail(String email) {
        return adminUserRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    @Transactional
    public AdminUserDto createUser(CreateAdminUserRequest request, String ipAddress, String userAgent) {
        // Validate password
        var passwordValidation = passwordService.validatePassword(request.password());
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException("Password validation failed: " + passwordValidation.errorMessage());
        }

        // Check if email already exists
        if (adminUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email " + request.email() + " already exists");
        }

        // Create new user
        AdminUser user = new AdminUser();
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordService.hashPassword(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setIsEnabled(true);
        user.setIsLocked(false);
        user.setPasswordExpiresAt(passwordService.calculatePasswordExpirationDate());

        // Set audit fields
        String currentUserId = getCurrentUserId();
        user.setCreatedBy(currentUserId);
        user.setUpdatedBy(currentUserId);

        // Assign default role if specified
        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            Set<Role> roles = roleRepository.findByNameInAndIsActiveTrue(request.roleNames())
                    .stream().collect(Collectors.toSet());

            if (roles.size() != request.roleNames().size()) {
                throw new IllegalArgumentException("One or more specified roles not found or inactive");
            }

            roles.forEach(user::addRole);
        } else {
            // Assign default VIEWER role
            roleRepository.findByNameAndIsActiveTrue("ROLE_VIEWER")
                    .ifPresent(user::addRole);
        }

        user = adminUserRepository.save(user);

        // Log user creation
        securityAuditService.logEvent(
                SecurityAuditLog.EventType.USER_CREATED,
                getCurrentUser().orElse(null),
                ipAddress,
                userAgent,
                true,
                "Created user: " + user.getEmail() + " with roles: " + user.getRoleNames()
        );

        log.info("Created new admin user: {} by user: {}", user.getEmail(), currentUserId);

        return convertToDto(user);
    }

    @Transactional
    public AdminUserDto updateUser(String userId, UpdateAdminUserRequest request, String ipAddress, String userAgent) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String originalEmail = user.getEmail();
        boolean emailChanged = false;

        // Update basic fields
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            String newEmail = request.email().toLowerCase().trim();
            if (adminUserRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email already exists: " + newEmail);
            }
            user.setEmail(newEmail);
            emailChanged = true;
        }

        // Update enabled status
        if (request.isEnabled() != null) {
            user.setIsEnabled(request.isEnabled());
        }

        // Unlock account if requested
        if (Boolean.FALSE.equals(request.isLocked()) && user.getIsLocked()) {
            user.setIsLocked(false);
            user.resetFailedLoginAttempts();

            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.ACCOUNT_UNLOCKED,
                    getCurrentUser().orElse(null),
                    ipAddress,
                    userAgent,
                    true,
                    "Account unlocked for user: " + user.getEmail()
            );
        }

        // Set audit fields
        user.setUpdatedBy(getCurrentUserId());

        user = adminUserRepository.save(user);

        // Log user update
        securityAuditService.logEvent(
                SecurityAuditLog.EventType.USER_UPDATED,
                getCurrentUser().orElse(null),
                ipAddress,
                userAgent,
                true,
                "Updated user: " + originalEmail +
                (emailChanged ? " -> " + user.getEmail() : "") +
                " by: " + getCurrentUserId()
        );

        log.info("Updated admin user: {} by user: {}", user.getEmail(), getCurrentUserId());

        return convertToDto(user);
    }

    @Transactional
    public void deleteUser(String userId, String ipAddress, String userAgent) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        // Deactivate all sessions
        userSessionRepository.deactivateAllSessionsForUserId(userId);

        // Remove user (this will cascade to roles due to foreign key constraints)
        adminUserRepository.delete(user);

        // Log user deletion
        securityAuditService.logEvent(
                SecurityAuditLog.EventType.USER_DELETED,
                getCurrentUser().orElse(null),
                ipAddress,
                userAgent,
                true,
                "Deleted user: " + user.getEmail() + " by: " + currentUserId
        );

        log.info("Deleted admin user: {} by user: {}", user.getEmail(), currentUserId);
    }

    @Transactional
    public void assignRole(String userId, String roleName, String ipAddress, String userAgent) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role role = roleRepository.findByNameAndIsActiveTrue(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found or inactive: " + roleName));

        if (!user.hasRole(roleName)) {
            user.addRole(role);
            user.setUpdatedBy(getCurrentUserId());
            adminUserRepository.save(user);

            // Log role assignment
            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.ROLE_ASSIGNED,
                    getCurrentUser().orElse(null),
                    ipAddress,
                    userAgent,
                    true,
                    "Assigned role: " + roleName + " to user: " + user.getEmail()
            );

            log.info("Assigned role {} to user: {} by: {}", roleName, user.getEmail(), getCurrentUserId());
        }
    }

    @Transactional
    public void removeRole(String userId, String roleName, String ipAddress, String userAgent) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        if (user.hasRole(roleName)) {
            user.removeRole(role);
            user.setUpdatedBy(getCurrentUserId());
            adminUserRepository.save(user);

            // Log role removal
            securityAuditService.logEvent(
                    SecurityAuditLog.EventType.ROLE_REMOVED,
                    getCurrentUser().orElse(null),
                    ipAddress,
                    userAgent,
                    true,
                    "Removed role: " + roleName + " from user: " + user.getEmail()
            );

            log.info("Removed role {} from user: {} by: {}", roleName, user.getEmail(), getCurrentUserId());
        }
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request, String ipAddress, String userAgent) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify current password (if not admin changing another user's password)
        String currentUserId = getCurrentUserId();
        boolean isChangingOwnPassword = userId.equals(currentUserId);

        if (isChangingOwnPassword) {
            if (!passwordService.verifyPassword(request.currentPassword(), user.getPasswordHash())) {
                securityAuditService.logEvent(
                        SecurityAuditLog.EventType.PASSWORD_CHANGE,
                        user,
                        ipAddress,
                        userAgent,
                        false,
                        "Invalid current password provided"
                );
                throw new IllegalArgumentException("Current password is incorrect");
            }
        }

        // Validate new password
        var passwordValidation = passwordService.validatePassword(request.newPassword());
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException("Password validation failed: " + passwordValidation.errorMessage());
        }

        // Check if new password is different from current
        if (passwordService.verifyPassword(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordService.hashPassword(request.newPassword()));
        user.setPasswordExpiresAt(passwordService.calculatePasswordExpirationDate());
        user.setUpdatedBy(currentUserId);

        // If password was expired or user was locked, unlock them
        if (user.getIsLocked()) {
            user.setIsLocked(false);
            user.resetFailedLoginAttempts();
        }

        adminUserRepository.save(user);

        // Invalidate all existing sessions for this user (force re-login with new password)
        if (isChangingOwnPassword) {
            userSessionRepository.deactivateAllSessionsForUserId(userId);
        }

        // Log password change
        securityAuditService.logEvent(
                SecurityAuditLog.EventType.PASSWORD_CHANGE,
                user,
                ipAddress,
                userAgent,
                true,
                isChangingOwnPassword ? "User changed own password" : "Password changed by admin: " + currentUserId
        );

        log.info("Password changed for user: {} by: {}", user.getEmail(), currentUserId);
    }

    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return adminUserRepository.countActiveUsers();
    }

    @Transactional(readOnly = true)
    public long getLockedUserCount() {
        return adminUserRepository.countLockedUsers();
    }

    private AdminUserDto convertToDto(AdminUser user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getIsEnabled(),
                user.getIsLocked(),
                user.getLastLoginAt(),
                user.getPasswordExpiresAt(),
                user.getRoleNames(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String getCurrentUserId() {
        return getCurrentUser()
                .map(AdminUser::getId)
                .orElse("system");
    }

    private Optional<AdminUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUser) {
            return Optional.of((AdminUser) authentication.getPrincipal());
        }
        return Optional.empty();
    }
}