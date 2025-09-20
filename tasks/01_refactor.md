# Database Refactoring Plan: Implement Normalized Herb Schema

## Overview
Refactor the current flat Herb entity structure to match the normalized database schema defined in `tasks/tables.sql`, which includes separate tables for herb attributes (flavors, formulas, images, indications, meridians).

## Current vs Target Schema Analysis

**Current Structure:**
- Single `Herb` entity with flat properties stored as strings
- Uses UUID as primary key
- Simple structure with all data in one table

**Target Structure (from tables.sql):**
- Main `herbs` table with integer ID and basic info
- Separate related tables: `herb_flavors`, `herb_formulas`, `herb_images`, `herb_indications`, `herb_meridians`
- Foreign key relationships with CASCADE operations
- Normalized approach with proper relational design

## Implementation Plan

### Phase 1: New Entity Classes
1. **Update Herb entity** to match the SQL schema:
   - Change ID type from UUID to Integer 
   - Update column names to match SQL schema (name_zh, name_pinyin, desc_zh, desc_en, etc.)
   - Add JPA relationships to new entities

2. **Create new related entities:**
   - `HerbFlavor` entity
   - `HerbFormula` entity  
   - `HerbImage` entity
   - `HerbIndication` entity
   - `HerbMeridian` entity

### Phase 2: Repository Updates
1. **Create new repositories:**
   - `HerbFlavorRepository`
   - `HerbFormulaRepository`
   - `HerbImageRepository`
   - `HerbIndicationRepository`
   - `HerbMeridianRepository`

2. **Update HerbRepository** with new query methods for joined data

### Phase 3: DTO Refactoring
1. **Update HerbDto** to include collections of related data
2. **Create new DTOs:**
   - `HerbFlavorDto`
   - `HerbFormulaDto`
   - `HerbImageDto` 
   - `HerbIndicationDto`
   - `HerbMeridianDto`

### Phase 4: Service Layer Updates
1. **Update HerbService** interface with new methods
2. **Refactor HerbServiceImpl** to:
   - Handle CRUD operations for related entities
   - Implement proper transaction management
   - Add methods for managing herb relationships

### Phase 5: Controller and Mapper Updates
1. **Update HerbController** endpoints as needed
2. **Refactor HerbMapper** to handle entity-DTO conversions for complex structure
3. **Update API responses** to include relational data

## Files to Create/Modify

### New Entity Files:
- `src/main/java/com/tcm/backend/domain/HerbFlavor.java`
- `src/main/java/com/tcm/backend/domain/HerbFormula.java`
- `src/main/java/com/tcm/backend/domain/HerbImage.java`
- `src/main/java/com/tcm/backend/domain/HerbIndication.java`
- `src/main/java/com/tcm/backend/domain/HerbMeridian.java`

### New Repository Files:
- `src/main/java/com/tcm/backend/repository/HerbFlavorRepository.java`
- `src/main/java/com/tcm/backend/repository/HerbFormulaRepository.java`
- `src/main/java/com/tcm/backend/repository/HerbImageRepository.java`
- `src/main/java/com/tcm/backend/repository/HerbIndicationRepository.java`
- `src/main/java/com/tcm/backend/repository/HerbMeridianRepository.java`

### New DTO Files:
- `src/main/java/com/tcm/backend/dto/HerbFlavorDto.java`
- `src/main/java/com/tcm/backend/dto/HerbFormulaDto.java`
- `src/main/java/com/tcm/backend/dto/HerbImageDto.java`
- `src/main/java/com/tcm/backend/dto/HerbIndicationDto.java`
- `src/main/java/com/tcm/backend/dto/HerbMeridianDto.java`

### Files to Modify:
- `src/main/java/com/tcm/backend/domain/Herb.java`
- `src/main/java/com/tcm/backend/dto/HerbDto.java`
- `src/main/java/com/tcm/backend/repository/HerbRepository.java`
- `src/main/java/com/tcm/backend/service/HerbService.java`
- `src/main/java/com/tcm/backend/service/impl/HerbServiceImpl.java`
- `src/main/java/com/tcm/backend/mapper/HerbMapper.java`
- `src/main/java/com/tcm/backend/api/HerbController.java`

### Documentation:
- `tasks/01_refactor.md` (this plan document)

## Key Considerations
- Maintain backward compatibility where possible
- Use proper JPA relationships with CASCADE settings
- Implement @Transactional annotations for multi-table operations
- Follow existing project conventions for validation annotations
- Ensure proper error handling for relational constraints
- Consider performance implications of lazy vs eager loading

## Database Schema Mapping

### herbs table
```sql
CREATE TABLE `herbs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `source_url` varchar(512) NOT NULL,
  `name_zh` varchar(255) DEFAULT NULL,
  `name_pinyin` varchar(255) DEFAULT NULL,
  `desc_zh` longtext,
  `desc_en` longtext,
  `appearance` longtext,
  `property` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_source_url` (`source_url`)
);
```

### Related tables
- `herb_flavors`: Many-to-one relationship with herbs
- `herb_formulas`: Many-to-one relationship with herbs  
- `herb_images`: Many-to-one relationship with herbs
- `herb_indications`: Many-to-one relationship with herbs
- `herb_meridians`: Many-to-one relationship with herbs

All related tables have:
- Auto-increment integer ID
- Foreign key to herbs.id with CASCADE DELETE/UPDATE
- Unique constraint on (herb_id, value) pair
- Value field for the actual data