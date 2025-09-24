package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", unique = true, nullable = false)
    @NotBlank(message = "Permission name is required")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Permission name must contain only uppercase letters and underscores")
    private String name;

    @Column(name = "resource", nullable = false)
    @NotBlank(message = "Resource is required")
    @Size(max = 50, message = "Resource must not exceed 50 characters")
    @Pattern(regexp = "^[a-z_]+$", message = "Resource must contain only lowercase letters and underscores")
    private String resource;

    @Column(name = "action", nullable = false)
    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    @Pattern(regexp = "^[a-z_]+$", message = "Action must contain only lowercase letters and underscores")
    private String action;

    @Column(name = "description")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

    public Permission(String name, String resource, String action, String description) {
        this.name = name;
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    public Permission(String resource, String action) {
        this.resource = resource;
        this.action = action;
        this.name = generatePermissionName(resource, action);
    }

    private String generatePermissionName(String resource, String action) {
        return resource.toUpperCase() + "_" + action.toUpperCase();
    }

    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.name == null && this.resource != null && this.action != null) {
            this.name = generatePermissionName(this.resource, this.action);
        }
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        Permission permission = (Permission) o;
        return name != null && name.equals(permission.name);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}