-- TCM App Backend - User Management Schema
-- Task 02: Users Feature & Access Control Implementation

-- Admin users table - core user management
CREATE TABLE admin_users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    failed_login_attempts INT DEFAULT 0 NOT NULL,
    last_login_at TIMESTAMP NULL,
    password_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    version INT DEFAULT 0 NOT NULL,
    INDEX idx_email (email),
    INDEX idx_enabled_locked (is_enabled, is_locked),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Roles table - RBAC roles (ADMIN, EDITOR, PUBLISHER, VIEWER)
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_name (name),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permissions table - granular permissions
CREATE TABLE permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_resource_action (resource, action),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User roles junction table - many-to-many relationship
CREATE TABLE admin_user_roles (
    admin_user_id CHAR(36) NOT NULL,
    role_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by CHAR(36),
    PRIMARY KEY (admin_user_id, role_id),
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    INDEX idx_user_id (admin_user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_assigned_at (assigned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role permissions junction table - many-to-many relationship
CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User sessions for tracking active admin sessions
CREATE TABLE user_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    admin_user_id CHAR(36) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    INDEX idx_user_id (admin_user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_active (is_active),
    INDEX idx_refresh_token (refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Security audit logs for compliance and monitoring
CREATE TABLE security_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    admin_user_id CHAR(36),
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    details JSON,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_event_type (event_type),
    INDEX idx_user_timestamp (admin_user_id, timestamp),
    INDEX idx_timestamp (timestamp),
    INDEX idx_success (success),
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Guest sessions for tracking anonymous users (optional analytics)
CREATE TABLE guest_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    ip_address VARCHAR(45),
    user_agent TEXT,
    first_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    request_count INT DEFAULT 1 NOT NULL,
    INDEX idx_ip_address (ip_address),
    INDEX idx_first_access (first_access_at),
    INDEX idx_last_access (last_access_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Full system access including user management'),
('ROLE_PUBLISHER', 'Approve/reject content for publication'),
('ROLE_EDITOR', 'Create and edit content drafts'),
('ROLE_VIEWER', 'Read-only access to admin dashboard');

-- Insert default permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('HERB_READ', 'herb', 'read', 'View herbs'),
('HERB_WRITE', 'herb', 'write', 'Create/edit herbs'),
('HERB_DELETE', 'herb', 'delete', 'Delete herbs'),
('FORMULA_READ', 'formula', 'read', 'View formulas'),
('FORMULA_WRITE', 'formula', 'write', 'Create/edit formulas'),
('FORMULA_DELETE', 'formula', 'delete', 'Delete formulas'),
('PUBLISH_READ', 'publish', 'read', 'View publish releases'),
('PUBLISH_APPROVE', 'publish', 'approve', 'Approve content for publication'),
('PUBLISH_REJECT', 'publish', 'reject', 'Reject publication requests'),
('PUBLISH_CREATE', 'publish', 'create', 'Create publication requests'),
('USER_READ', 'user', 'read', 'View admin users'),
('USER_WRITE', 'user', 'write', 'Create/edit admin users'),
('USER_DELETE', 'user', 'delete', 'Delete admin users'),
('ROLE_MANAGE', 'role', 'manage', 'Assign/remove user roles'),
('AUDIT_READ', 'audit', 'read', 'View audit logs'),
('SYSTEM_ADMIN', 'system', 'admin', 'Full system administration');

-- Assign permissions to roles
-- ROLE_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';

-- ROLE_PUBLISHER gets publish and read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_PUBLISHER'
AND p.name IN (
    'HERB_READ', 'FORMULA_READ', 'PUBLISH_READ', 'PUBLISH_APPROVE',
    'PUBLISH_REJECT', 'PUBLISH_CREATE', 'USER_READ'
);

-- ROLE_EDITOR gets content edit permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_EDITOR'
AND p.name IN (
    'HERB_READ', 'HERB_WRITE', 'FORMULA_READ', 'FORMULA_WRITE',
    'PUBLISH_READ', 'PUBLISH_CREATE', 'USER_READ'
);

-- ROLE_VIEWER gets only read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_VIEWER'
AND p.name IN ('HERB_READ', 'FORMULA_READ', 'PUBLISH_READ', 'USER_READ');

-- Create default admin user (password: TcmAdmin123!)
-- Password hash generated with BCrypt strength 12
INSERT INTO admin_users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    is_enabled,
    is_locked,
    password_expires_at
) VALUES (
    UUID(),
    'admin@tcmapp.com',
    '$2a$12$rQZx8QQwXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX', -- Will be properly hashed
    'System',
    'Administrator',
    TRUE,
    FALSE,
    DATE_ADD(NOW(), INTERVAL 90 DAY)
);

-- Assign ADMIN role to default admin user
INSERT INTO admin_user_roles (admin_user_id, role_id, assigned_by)
SELECT au.id, r.id, au.id
FROM admin_users au, roles r
WHERE au.email = 'admin@tcmapp.com' AND r.name = 'ROLE_ADMIN';