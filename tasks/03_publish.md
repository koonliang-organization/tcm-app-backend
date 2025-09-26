# Task 03: Dataset Publishing Implementation

## Overview
Implement complete dataset publishing pipeline for herbs and related entities, based on requirements.md specifications for dataset export and distribution.

## Current State Analysis

### ✅ Already Implemented
- `PublishRelease` entity with status workflow (DRAFT → READY_FOR_REVIEW → APPROVED → FAILED)
- `PublishReleaseController` with admin endpoints (`/api/v1/publish/releases`)
- Basic `JsonDatasetExportService` (herbs only, basic ZIP creation)
- `LocalDatasetStorageClient` with SHA-256 checksums
- `DatasetManifestFactory` for manifest generation
- `DatasetPublisherServiceImpl` orchestration
- Basic publishing workflow (create → approve → publish)

### ❌ Missing/Incomplete
- Complete herb-related data export (only basic herbs, missing relationships)
- SQLite export capability
- Public API endpoints (`/public/v1/datasets/*`)
- N+1 query optimization with EntityGraph
- Export DTOs (currently exposing raw entities)
- Advanced dataset validation and integrity checks
- Signed URL generation for secure downloads
- Full-text search indexes in SQLite exports

## Implementation Plan

### Phase 1: Enhanced Dataset Export Services

#### 1.1 Improve JsonDatasetExportService
**Files to modify:**
- `src/main/java/com/tcm/backend/publisher/JsonDatasetExportService.java`
- `src/main/java/com/tcm/backend/repository/HerbRepository.java`

**Tasks:**
- Add `@EntityGraph` query method to load herbs with all relationships
- Create export DTOs for clean serialization:
  - `HerbExportDto` (remove audit fields, internal IDs)
  - `HerbFlavorExportDto`, `HerbFormulaExportDto`, etc.
- Include all herb-related entities in export
- Add comprehensive error handling and logging
- Generate export statistics (count of herbs, flavors, etc.)

#### 1.2 Create SQLiteDatasetExportService
**New files to create:**
- `src/main/java/com/tcm/backend/publisher/SqliteDatasetExportService.java`
- `src/main/java/com/tcm/backend/publisher/dto/HerbExportDto.java` (and related DTOs)

**Tasks:**
- Implement SQLite database generation using embedded SQLite
- Create proper schema with foreign key constraints
- Add FTS (Full-Text Search) indexes for herbs table
- Include metadata tables (version info, export timestamp)
- Optimize for mobile offline querying patterns

#### 1.3 Create CompositeDatasetExportService
**New file to create:**
- `src/main/java/com/tcm/backend/publisher/CompositeDatasetExportService.java`

**Tasks:**
- Combine JSON and SQLite exports in single ZIP
- Include manifest.json within archive
- Add dataset statistics file
- Implement export validation checks

### Phase 2: Public API Endpoints

#### 2.1 Create PublicDatasetController
**New file to create:**
- `src/main/java/com/tcm/backend/api/PublicDatasetController.java`

**Endpoints to implement:**
```
GET /public/v1/datasets/latest          # Return latest approved manifest
GET /public/v1/datasets/{version}       # Get specific version manifest
GET /public/v1/datasets/download/{version}  # Redirect to signed download URL
```

**Tasks:**
- Implement manifest retrieval from approved releases
- Add proper error handling (404 for missing versions)
- Integrate with storage client for download URLs
- Add rate limiting considerations

#### 2.2 Enhance HerbController for Public Access
**Files to modify:**
- `src/main/java/com/tcm/backend/api/HerbController.java` (or create public version)

**Tasks:**
- Add public endpoints under `/public/v1/herbs`
- Implement filtering and pagination for mobile clients
- Return clean DTOs without sensitive fields
- Add proper caching headers

### Phase 3: Repository and Query Optimization

#### 3.1 Add EntityGraph Support
**Files to modify:**
- `src/main/java/com/tcm/backend/repository/HerbRepository.java`

**Tasks:**
- Add `@EntityGraph(attributePaths = {"flavors", "formulas", "images", "indications", "meridians"})`
- Create bulk export query method
- Implement database advisory locking for export consistency

### Phase 4: Storage and Security Enhancements

#### 4.1 Enhance LocalDatasetStorageClient
**Files to modify:**
- `src/main/java/com/tcm/backend/publisher/LocalDatasetStorageClient.java`

**Tasks:**
- Add file versioning and cleanup of old releases
- Implement URL signing for secure downloads (if needed)
- Add atomic file operations
- Improve error handling and recovery

#### 4.2 Dataset Validation and Integrity
**New files to create:**
- `src/main/java/com/tcm/backend/publisher/DatasetValidator.java`

**Tasks:**
- Validate export completeness (all expected entities present)
- Verify data integrity (foreign key consistency)
- Add rollback mechanisms for failed exports
- Generate validation reports

### Phase 5: Configuration and Environment

#### 5.1 Application Configuration
**Files to modify:**
- `src/main/resources/application.yml`

**Configuration to add:**
```yaml
publisher:
  storage:
    local-directory: ${DATASET_STORAGE_DIR:build/datasets}
    retention-count: ${DATASET_RETENTION:10}
  export:
    batch-size: ${EXPORT_BATCH_SIZE:1000}
    include-sqlite: ${EXPORT_INCLUDE_SQLITE:true}
  min-app-version: ${MIN_APP_VERSION:1.0.0}
```

### Phase 6: Testing

#### 6.1 Unit Tests
**Test files to create/modify:**
- `src/test/java/com/tcm/backend/publisher/CompositeDatasetExportServiceTest.java`
- `src/test/java/com/tcm/backend/publisher/SqliteDatasetExportServiceTest.java`
- `src/test/java/com/tcm/backend/api/PublicDatasetControllerTest.java`

#### 6.2 Integration Tests
**Test files to create:**
- `src/test/java/com/tcm/backend/publisher/DatasetPublishingIntegrationTest.java`

**Test scenarios:**
- End-to-end publishing workflow
- Dataset download and validation
- Public API endpoint behavior
- Export data integrity and completeness

## Acceptance Criteria

### Must Have
- [ ] Complete herb dataset export including all relationships
- [ ] Both JSON and SQLite export formats
- [ ] Public API endpoints for dataset access
- [ ] Proper EntityGraph optimization (no N+1 queries)
- [ ] Export validation and integrity checks
- [ ] Comprehensive test coverage (>80%)

### Should Have
- [ ] Full-text search indexes in SQLite
- [ ] Dataset statistics and metadata
- [ ] Proper error handling and logging
- [ ] Configuration-driven export options

### Could Have
- [ ] Signed URLs for enhanced security
- [ ] Export progress tracking
- [ ] Dataset diff reporting between versions
- [ ] Automatic cleanup of old dataset files

## Dependencies
- Current herb domain model and relationships
- Existing publish release workflow
- Spring Data JPA with EntityGraph support
- SQLite JDBC driver (add to pom.xml if needed)
- Storage infrastructure (currently local filesystem)

## Risks and Considerations
1. **Performance**: Large datasets may cause memory issues - implement streaming where possible
2. **Data Consistency**: Use database locking during export to prevent inconsistent snapshots
3. **Storage Space**: Multiple dataset versions will consume disk space - implement retention policies
4. **Mobile Compatibility**: Ensure exported data structure matches mobile client expectations
5. **Security**: Public endpoints need rate limiting and proper access controls

## Implementation Order
1. Enhanced export services (JsonDatasetExportService improvements)
2. SQLite export capability
3. Composite export service
4. Public API endpoints
5. Repository optimizations
6. Storage enhancements
7. Testing and validation
8. Documentation and configuration