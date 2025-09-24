package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "admin_users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser extends AbstractAuditableEntity implements UserDetails {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Column(name = "password_hash", nullable = false)
    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    private String passwordHash;

    @Column(name = "first_name")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Column(name = "last_name")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Min(value = 0, message = "Failed login attempts cannot be negative")
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private String createdBy;

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "admin_user_roles",
        joinColumns = @JoinColumn(name = "admin_user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "adminUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UserSession> sessions = new HashSet<>();

    public AdminUser(String email, String passwordHash, String firstName, String lastName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    public void addRole(Role role) {
        this.roles.add(role);
        role.getAdminUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getAdminUsers().remove(this);
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean hasPermission(String permissionName) {
        return roles.stream()
                .anyMatch(role -> role.hasPermission(permissionName));
    }

    public Set<String> getRoleNames() {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    public void incrementFailedLoginAttempts(int lockoutDurationMinutes) {
        this.failedLoginAttempts++;

        // Lock account after 5 failed attempts for specified duration
        if (this.failedLoginAttempts >= 5) {
            this.isLocked = true;
            this.lockedUntil = Instant.now().plusSeconds(lockoutDurationMinutes * 60L);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.resetFailedLoginAttempts();
        if (this.isLocked) {
            this.isLocked = false;
        }
    }

    public boolean isPasswordExpired() {
        return passwordExpiresAt != null && Instant.now().isAfter(passwordExpiresAt);
    }

    // UserDetails implementation for Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles as authorities
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));

            // Add permissions as authorities
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // We don't implement account expiration yet
    }

    @Override
    public boolean isAccountNonLocked() {
        if (!isLocked) {
            return true;
        }

        // Check if lockout period has expired
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            // Auto-unlock account and reset failed attempts
            this.isLocked = false;
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !isPasswordExpired();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (passwordExpiresAt == null) {
            // Set password expiration to 90 days from creation
            passwordExpiresAt = Instant.now().plusSeconds(90L * 24L * 60L * 60L);
        }
    }
}