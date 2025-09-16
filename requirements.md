# TCM App – Backend Requirements

This document defines the functional scope, architecture, data contracts, and non-functional requirements for the TCM App backend service. The service powers the mobile client, the admin web portal, and the dataset publishing pipeline. It must expose a secure, well-documented REST API, persist canonical data in MySQL, and support generation of immutable read-only datasets consumed by offline-capable clients.

## 1) Scope & Principles

- Authoritative data store for Herbs, Formulas, Recipes, Ingredients, Tags, Media assets, and reference metadata.
- Manage admin workflows: authentication, role-based access, CRUD with validation, publishing approvals.
- Provide public/mobile-facing APIs with throttling and read-only access to published data.
- Generate versioned dataset snapshots for offline distribution (SQLite/JSON exports) with checksum and manifest metadata.
- Follow layered architecture (Controller → Service → Repository) with DTO boundaries; avoid leaking entities across layers.
- Adhere to SOLID/DRY/KISS/YAGNI, OWASP ASVS, and Spring Boot best practices.

Non-goals (MVP):
- Real-time collaboration or concurrent editing conflict resolution.
- Push notification orchestration.
- Advanced analytics pipeline (beyond basic usage metrics collection hooks).

## 2) System Context & Integrations

- **Clients**: Admin web portal (SPA), Mobile apps (iOS/Android) consuming published datasets and limited REST APIs.
- **Data Pipeline**: ETL job that exports approved content to SQLite/JSON and uploads to CDN/object store.
- **External Services**: Object storage (S3-compatible) for dataset binaries and media, SMTP provider for admin notifications, optional SSO IdP for admin login.
- **Environments**: Local development (Docker Compose), Staging, Production (Kubernetes or VM-based).

## 3) Technology Stack

- Java 17, Spring Boot 3, Maven.
- Spring Web MVC, Spring Data JPA, Spring Validation, Spring Security, Springdoc OpenAPI.
- MySQL 8 as primary database.
- JUnit 5 + Mockito for automated tests.
- Docker for containerisation; GitHub Actions (or equivalent) CI/CD pipeline.

## 4) High-Level Architecture

- **API Layer**: REST controllers returning `ResponseEntity<ApiResponse<T>>`, request validation with DTO records.
- **Service Layer**: `@Service` implementations encapsulating business logic, transaction boundaries, and orchestration of repositories/publishers.
- **Persistence Layer**: Spring Data JPA repositories for MySQL access, using JPQL or derived queries; employ EntityGraph to avoid N+1 queries where needed.
- **Integration Layer**: Storage clients for exporting datasets, email notifications, background job triggers.
- **Dataset Publisher**: Scheduled/async service that snapshots approved entities into versioned SQLite/JSON bundles, writes manifest, and uploads to configured storage.

## 5) Domain Model Overview (Canonical DB)

- `Herb`: id (UUID), latinName, pinyinName, chineseNameSimplified, chineseNameTraditional, properties, indications, precautions.
- `Formula`: id, name fields, description, usage, contraindications, references.
- `FormulaHerb`: join table with dosage metadata.
- `Ingredient`: id, category, description.
- `Tag`: id, type, label.
- `Alias`: id, entityType, entityId, localizedName, language.
- `MediaAsset`: id, url, checksum, mimeType, attribution.
- `UserNoteTemplate`/`ReferenceArticle` (optional) for curated educational content.
- `AdminUser`: id, email, passwordHash, roles, lastLoginAt.
- `PublishRelease`: id, version, status, createdBy, approvedBy, manifestJson, checksum, storageUrl, createdAt.

All IDs use UUIDv4. Entities store auditing columns (`created_at`, `updated_at`, `created_by`, `updated_by`). Soft delete via `deleted_at` where reversible deletes are required.

## 6) API Design Guidelines

- Base path: `/api/v1` for authenticated admin APIs; `/public/v1` for read-only endpoints consumed by mobile.
- Use DTO records for request/response payloads; map to entities within services.
- Enforce validation annotations (`@NotBlank`, `@Size`, `@Email`, etc.) with canonical constructors.
- Apply pagination (`page`, `size`, `sort`) and filtering via query params; default `size` ≤ 50.
- Support optimistic locking using `@Version` for mutable entities.
- Document APIs with OpenAPI 3.1 via Springdoc; publish JSON and interactive Swagger UI (admin only).
- Error handling through `GlobalExceptionHandler`, returning structured `ApiResponse` with error codes.
- Rate-limit public endpoints (Spring Cloud Gateway or Servlet filter) to prevent abuse.

## 7) Authentication & Authorization

- Admin auth: OAuth2/OIDC or username/password with MFA; issue JWT access/refresh tokens stored HTTP-only.
- Public/mobile auth: optionally issue API keys or signed URLs for dataset manifest; dataset downloads secured via signed CDN URLs.
- Role-based access control: `ROLE_ADMIN`, `ROLE_EDITOR`, `ROLE_PUBLISHER`, `ROLE_VIEWER` with granular permissions enforced at service layer.
- Password policies (min length 12, complexity, rotation), account lockout on repeated failures.
- Audit logging for authentication events, privilege changes, and publish actions.

## 8) Dataset Publishing & Distribution

- Only `PublishRelease` records with status `APPROVED` can be exported.
- Snapshot process steps:
  1. Lock relevant tables using advisory lock to ensure consistency.
  2. Query data via repositories and map to export DTOs.
  3. Generate SQLite DB (FTS indexes, overlay metadata) and/or JSON archive.
  4. Compress to `.zip`, compute SHA-256 checksum, create manifest (`version`, `created_at`, `size_bytes`, `checksum`, `url`, `min_app_version`).
  5. Upload archive + manifest to object storage with immutable naming and signed CDN URLs.
- Retain last N releases, support rollback by flagging previous manifest as `latest`.
- Provide `/api/v1/publish/releases` for admin management and `/public/v1/datasets/latest` for clients.

## 9) Data Quality & Validation

- Enforce uniqueness on localized names and alias combinations.
- Validate reference relationships (e.g., `FormulaHerb` references existing herbs) with referential integrity.
- Implement content linting (e.g., dosage format) in service layer before saving.
- Prevent destructive edits to published versions; require draft copy for modifications.

## 10) Performance & Scalability Targets

- Admin CRUD endpoints: median latency ≤ 200ms under 100 RPS; P95 ≤ 500ms.
- Dataset export: complete within 5 minutes for 10k herbs/formulas combined; run asynchronously with status updates.
- Database: maintain indexes on frequently filtered columns (name, tags); plan for horizontal read replicas if API read volume grows.
- Caching: cache read-only public responses for 5 minutes; bust cache on publish.
- File uploads (media) streamed to storage; enforce max file size 20MB, image dimensions validated server-side.

## 11) Security, Privacy & Compliance

- Sanitize and validate all inputs; avoid string concatenated queries.
- Store secrets in environment variables or vault; never in source control.
- HTTPS enforced end-to-end; HSTS enabled on production ingress.
- Encrypt sensitive columns at rest where required (e.g., admin MFA secrets).
- Implement content moderation permissions; ensure licensing data recorded for assets.
- Log PII access with actor, timestamp, reason; ensure GDPR/CCPA compliance for user-generated contributions (if enabled later).

## 12) Observability & Operations

- Structured JSON logging with correlation IDs per request.
- Expose `/actuator` endpoints: health, info, metrics, prometheus, loggers.
- Metrics: request latency, error rates, DB connection pool usage, cache hit rate, dataset export duration.
- Alerting: high error rate, export failures, storage upload errors, DB replication lag.
- Daily backups of MySQL; verify restore process quarterly.
- Disaster recovery RTO ≤ 4 hours, RPO ≤ 15 minutes.

## 13) Testing & Quality Assurance

- Unit tests for services, validators, and utilities (≥80% branch coverage on business logic modules).
- Integration tests with Testcontainers for MySQL/Redis; cover core CRUD flows and dataset export pipeline.
- Contract tests for public API endpoints; ensure compatibility with mobile client schemas.
- Security testing: dependency scanning (OWASP Dependency-Check), SAST, and scheduled penetration tests before GA.
- Performance test suite (Gatling/JMeter) for critical endpoints and dataset export job.

## 14) Deployment & Release Management

- CI pipeline: build, unit/integration tests, static analysis (SpotBugs, Checkstyle), Docker image publish.
- CD pipeline: staging deploy requires manual approval; production deploy uses blue/green or rolling strategy with automated smoke tests.
- Feature toggles for risky changes; configuration externalized via Config Server or environment variables.
- Versioning: semantic version for API (`v1`), dataset releases follow `{major}.{minor}.{patch}`.

## 15) Acceptance Criteria (MVP)

- Admin user can authenticate, manage content entities, and submit/approve a publish request.
- Dataset export generates manifest + archive, uploads to storage, and mobile client can download verified release.
- Public endpoints expose published read-only data with correct filtering/pagination and enforcement of RBAC/rate limits.
- System recovers gracefully from failed export (rollback to last good release, alerts emitted).
- Automated test suite executes in CI with ≥90% pass rate and blocks deploy on failure.

## 16) Open Questions

- Finalize authentication strategy (in-house vs external IdP) and MFA implementation timeline.
- Determine scale requirements for public APIs (expected RPS) to size infrastructure and caching.
- Confirm legal/licensing obligations for distributing media assets offline.
- Clarify whether user-generated notes will sync to backend in future phases (impacts schema and privacy controls).
- Decide on long-term storage location and retention policy for historical dataset releases.

---

Decision summary:
- Backend acts as canonical source of truth, enforcing validations and RBAC before data is published.
- Dataset pipeline remains server-driven; mobile clients consume immutable snapshots plus limited REST APIs.
- Layered Spring Boot architecture with DTO boundaries, MySQL persistence, and automated CI/CD ensures maintainability and scalability.
