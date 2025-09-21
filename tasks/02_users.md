# Task 02: Users Feature & Access Control Implementation

## Overview
Implement a comprehensive user management and access control system for the TCM App Backend, supporting both authenticated admin users and anonymous guest users with appropriate security measures and role-based access control.

## Requirements Analysis
From requirements.md sections 7 & 8:
- Admin authentication with OAuth2/OIDC or username/password + MFA
- Role-based access control: ADMIN, EDITOR, PUBLISHER, VIEWER
- JWT access/refresh tokens with HTTP-only cookies
- Password policies (min 12 chars, complexity, rotation)
- Account lockout on repeated failures
- Audit logging for authentication events
- Public/mobile APIs with rate limiting
- Dataset download security with signed URLs

## Implementation Plan

### Phase 1: Database Schema & Core Entities

#### 1.1 Database Tables
```sql
-- Admin users table
CREATE TABLE admin_users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_enabled BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    last_login_at TIMESTAMP NULL,
    password_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by CHAR(36),
    updated_by CHAR(36),
    version INT DEFAULT 0
);

-- Roles table
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Permissions table
CREATE TABLE permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User roles junction table
CREATE TABLE admin_user_roles (
    admin_user_id CHAR(36) NOT NULL,
    role_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by CHAR(36),
    PRIMARY KEY (admin_user_id, role_id),
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Role permissions junction table
CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- User sessions for tracking active logins
CREATE TABLE user_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    admin_user_id CHAR(36) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id)
);

-- Audit logging for security events
CREATE TABLE security_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    admin_user_id CHAR(36),
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    details JSON,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_user_timestamp (admin_user_id, timestamp),
    FOREIGN KEY (admin_user_id) REFERENCES admin_users(id)
);

-- Guest sessions for tracking anonymous users (optional)
CREATE TABLE guest_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    ip_address VARCHAR(45),
    user_agent TEXT,
    first_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    request_count INT DEFAULT 1,
    INDEX idx_ip_address (ip_address)
);
```

#### 1.2 Initial Data Population
```sql
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
('PUBLISH_APPROVE', 'publish', 'approve', 'Approve content for publication'),
('PUBLISH_REJECT', 'publish', 'reject', 'Reject publication requests'),
('USER_MANAGE', 'user', 'manage', 'Manage admin users'),
('SYSTEM_ADMIN', 'system', 'admin', 'Full system administration');
```

### Phase 2: Domain Entities

#### 2.1 AdminUser Entity
- UUID primary key
- Email, password hash, personal info
- Account status fields (enabled, locked, failed attempts)
- Audit fields (created_at, updated_at, created_by, updated_by)
- Version field for optimistic locking
- Many-to-many relationship with roles

#### 2.2 Role & Permission Entities
- Role entity with name, description
- Permission entity with resource/action structure
- Many-to-many relationships between Role-Permission and User-Role

#### 2.3 Session Management Entities
- UserSession for active admin sessions
- GuestSession for anonymous user tracking
- SecurityAuditLog for compliance logging

### Phase 3: Security Configuration

#### 3.1 JWT Implementation
- JWT utility class for token generation/validation
- Access tokens (15 min expiry) + refresh tokens (7 days)
- Claims: user ID, roles, permissions
- HTTP-only cookies for web clients

#### 3.2 Authentication Filter Chain
- JWT authentication filter for /api/v1/**
- Anonymous access for /public/v1/**
- Rate limiting filter (different limits per user type)
- CORS configuration for admin web portal

#### 3.3 Password Security
- BCrypt encoder with strength 12
- Password policy validation (min 12 chars, complexity)
- Account lockout after 5 failed attempts
- Password expiration (90 days for admins)

### Phase 4: API Endpoints

#### 4.1 Public Endpoints (/public/v1/)
```
GET /public/v1/herbs?page=0&size=20&filter=
GET /public/v1/formulas?page=0&size=20&filter=
GET /public/v1/datasets/latest
```
- Anonymous access
- Rate limited: 100 requests/hour per IP
- Only published content visible
- Cached responses (5 minutes)

#### 4.2 Authentication Endpoints (/api/v1/auth/)
```
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
GET /api/v1/auth/me
POST /api/v1/auth/change-password
```

#### 4.3 User Management Endpoints (/api/v1/admin/)
```
GET /api/v1/admin/users
POST /api/v1/admin/users
GET /api/v1/admin/users/{id}
PUT /api/v1/admin/users/{id}
DELETE /api/v1/admin/users/{id}
PUT /api/v1/admin/users/{id}/roles
GET /api/v1/admin/audit-logs
```

### Phase 5: Service Layer Implementation

#### 5.1 Authentication Service
- Login/logout logic
- JWT token management
- Session tracking
- Failed login attempt handling

#### 5.2 User Management Service
- CRUD operations for admin users
- Role assignment/removal
- Password policy enforcement
- Account lifecycle management

#### 5.3 Authorization Service
- Permission checking
- Role-based access control
- Method-level security support

#### 5.4 Audit Service
- Security event logging
- Compliance reporting
- Failed authentication tracking

### Phase 6: Integration & Testing

#### 6.1 Service Integration
- Update existing HerbService with user context
- Filter content based on publication status and user roles
- Add audit logging to sensitive operations

#### 6.2 Testing Requirements
- Unit tests for all services (≥80% coverage)
- Integration tests for authentication flows
- Security tests for RBAC enforcement
- Performance tests for rate limiting
- End-to-end API tests

## Security Considerations

### Authentication Security
- Strong password policies enforced
- Account lockout protection against brute force
- JWT tokens with appropriate expiry times
- Secure cookie configuration (HttpOnly, Secure, SameSite)

### Authorization Security
- Principle of least privilege
- Role-based access control with granular permissions
- Method-level security annotations
- Input validation and sanitization

### Data Security
- Password hashing with BCrypt
- Sensitive data encryption at rest
- Audit logging for compliance
- Rate limiting to prevent abuse

## Performance Requirements
- Admin authentication: ≤200ms median latency
- Public API responses: ≤100ms with caching
- Rate limiting overhead: <10ms per request
- Database queries optimized with proper indexing

## Monitoring & Alerting
- Failed authentication attempt alerts
- Account lockout notifications
- High error rate alerts for public APIs
- JWT token validation failure tracking

## Acceptance Criteria
1. Admin users can authenticate with email/password
2. Role-based access control properly restricts functionality
3. Guest users can access public APIs with appropriate rate limits
4. All security events are properly logged for audit
5. Password policies are enforced and account lockout works
6. JWT tokens are properly issued and validated
7. Public APIs only expose published content
8. Performance requirements are met for all endpoints

## Implementation Order
1. Database schema and migration scripts
2. Domain entities and repositories
3. Core authentication service
4. JWT implementation and security config
5. Authentication controllers
6. User management controllers
7. RBAC implementation
8. Rate limiting and security measures
9. Integration with existing services
10. Testing and documentation