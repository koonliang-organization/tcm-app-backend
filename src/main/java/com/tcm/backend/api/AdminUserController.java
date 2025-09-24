package com.tcm.backend.api;

import com.tcm.backend.dto.*;
import com.tcm.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Admin user management APIs")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get all admin users", description = "Retrieve a paginated list of all admin users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER') or hasAuthority('USER_READ')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Page<AdminUserDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {

        try {
            Page<AdminUserDto> users = adminUserService.getAllUsers(pageable);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Users retrieved successfully",
                users
            ));

        } catch (Exception e) {
            log.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while retrieving users", null)
            );
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active admin users", description = "Retrieve a paginated list of active admin users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER', 'EDITOR') or hasAuthority('USER_READ')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Page<AdminUserDto>>> getActiveUsers(
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {

        try {
            Page<AdminUserDto> users = adminUserService.getActiveUsers(pageable);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Active users retrieved successfully",
                users
            ));

        } catch (Exception e) {
            log.error("Error retrieving active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while retrieving active users", null)
            );
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admin user by ID", description = "Retrieve a specific admin user by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER') or hasAuthority('USER_READ') or #id == authentication.principal.id")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AdminUserDto>> getUserById(@PathVariable String id) {

        try {
            return adminUserService.getUserById(id)
                .map(user -> ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                    "SUCCESS",
                    "User retrieved successfully",
                    user
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", "User not found", null)
                ));

        } catch (Exception e) {
            log.error("Error retrieving user by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while retrieving user", null)
            );
        }
    }

    @PostMapping
    @Operation(summary = "Create new admin user", description = "Create a new admin user with specified roles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request or validation error"),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_WRITE')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AdminUserDto>> createUser(
            @Valid @RequestBody CreateAdminUserRequest request,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AdminUserDto createdUser = adminUserService.createUser(request, ipAddress, userAgent);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                new com.tcm.backend.dto.ApiResponse<>(
                    "SUCCESS",
                    "User created successfully",
                    createdUser
                )
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while creating user", null)
            );
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update admin user", description = "Update an existing admin user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request or validation error"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_WRITE') or (#id == authentication.principal.id and hasAuthority('USER_READ'))")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<AdminUserDto>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateAdminUserRequest request,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            AdminUserDto updatedUser = adminUserService.updateUser(id, request, ipAddress, userAgent);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "User updated successfully",
                updatedUser
            ));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error updating user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while updating user", null)
            );
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete admin user", description = "Delete an admin user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_DELETE')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> deleteUser(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            adminUserService.deleteUser(id, ipAddress, userAgent);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "User deleted successfully",
                null
            ));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error deleting user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while deleting user", null)
            );
        }
    }

    @PostMapping("/{id}/roles/{roleName}")
    @Operation(summary = "Assign role to user", description = "Assign a role to an admin user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User or role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> assignRole(
            @PathVariable String id,
            @PathVariable String roleName,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            adminUserService.assignRole(id, roleName, ipAddress, userAgent);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Role assigned successfully",
                null
            ));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error assigning role {} to user: {}", roleName, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while assigning role", null)
            );
        }
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @Operation(summary = "Remove role from user", description = "Remove a role from an admin user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role removed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User or role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<Void>> removeRole(
            @PathVariable String id,
            @PathVariable String roleName,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            adminUserService.removeRole(id, roleName, ipAddress, userAgent);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "Role removed successfully",
                null
            ));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
                );
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error removing role {} from user: {}", roleName, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while removing role", null)
            );
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get statistics about admin users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER') or hasAuthority('USER_READ')")
    public ResponseEntity<com.tcm.backend.dto.ApiResponse<UserStatsDto>> getUserStats() {

        try {
            long activeUsers = adminUserService.getActiveUserCount();
            long lockedUsers = adminUserService.getLockedUserCount();

            UserStatsDto stats = new UserStatsDto(activeUsers, lockedUsers);

            return ResponseEntity.ok(new com.tcm.backend.dto.ApiResponse<>(
                "SUCCESS",
                "User statistics retrieved successfully",
                stats
            ));

        } catch (Exception e) {
            log.error("Error retrieving user statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new com.tcm.backend.dto.ApiResponse<>("ERROR", "An error occurred while retrieving user statistics", null)
            );
        }
    }

    public record UserStatsDto(long activeUsers, long lockedUsers) {}

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}