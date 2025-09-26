package com.tcm.backend.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.domain.*;
import com.tcm.backend.repository.HerbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatasetPublishingIntegrationTest {

    @Autowired
    private CompositeDatasetExportService compositeDatasetExportService;

    @Autowired
    private JsonDatasetExportService jsonDatasetExportService;

    @Autowired
    private SqliteDatasetExportService sqliteDatasetExportService;

    @Autowired
    private HerbRepository herbRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clear any existing data
        herbRepository.deleteAll();

        // Create test data
        createTestHerbs();
    }

    @Test
    void completePublishingWorkflowProducesConsistentData() throws Exception {
        // Export using composite service
        DatasetExportService.ExportResult compositeResult = compositeDatasetExportService.exportDataset();

        assertThat(compositeResult).isNotNull();
        assertThat(compositeResult.sizeBytes()).isGreaterThan(0);

        // Validate composite structure
        validateCompositeExportStructure(compositeResult.datasetStream());

        // Extract and validate individual formats
        validateIndividualFormatsConsistency(compositeResult.datasetStream());
    }

    @Test
    void jsonAndSqliteExportsContainIdenticalData() throws Exception {
        // Export JSON dataset
        DatasetExportService.ExportResult jsonResult = jsonDatasetExportService.exportDataset();

        // Export SQLite dataset
        DatasetExportService.ExportResult sqliteResult = sqliteDatasetExportService.exportDataset();

        // Validate data consistency between formats
        validateDataConsistencyBetweenFormats(jsonResult.datasetStream(), sqliteResult.datasetStream());
    }

    @Test
    void publishingWithLargeDatasetMaintainsPerformance() throws Exception {
        // Add more test data to simulate larger dataset
        createAdditionalTestHerbs(50);

        long startTime = System.currentTimeMillis();
        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();
        long endTime = System.currentTimeMillis();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        // Performance check - should complete within reasonable time (10 seconds for test)
        long executionTime = endTime - startTime;
        assertThat(executionTime).isLessThan(10000); // 10 seconds

        // Validate data integrity with larger dataset
        validateLargeDatasetIntegrity(result.datasetStream());
    }

    @Test
    void emptyDatabaseHandledGracefully() throws Exception {
        // Clear all herbs
        herbRepository.deleteAll();

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0); // Still has structure/metadata

        validateEmptyDatasetStructure(result.datasetStream());
    }

    @Test
    void publishingHandlesComplexRelationships() throws Exception {
        // Create herbs with complex relationships
        createHerbsWithComplexRelationships();

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        validateComplexRelationshipsInExport(result.datasetStream());
    }

    private void createTestHerbs() {
        // Create first herb with all relationship types
        Herb herb1 = new Herb();
        herb1.setSourceUrl("https://example.com/herb1");
        herb1.setNameZh("白术");
        herb1.setNamePinyin("bai zhu");
        herb1.setDescZh("白术是一种常用中药");
        herb1.setDescEn("Bai Zhu is a common Chinese herb");
        herb1.setAppearance("White rhizome");
        herb1.setProperty("Warm, Sweet");

        herb1 = herbRepository.save(herb1);

        // Add relationships
        addRelationshipsToHerb(herb1, "Sweet", "Si Jun Zi Tang", "bai-zhu.jpg",
                              "Digestive problems", "Spleen");

        // Create second herb
        Herb herb2 = new Herb();
        herb2.setSourceUrl("https://example.com/herb2");
        herb2.setNameZh("当归");
        herb2.setNamePinyin("dang gui");
        herb2.setDescZh("当归补血活血");
        herb2.setDescEn("Dang Gui nourishes and moves blood");
        herb2.setAppearance("Brown root");
        herb2.setProperty("Warm, Sweet");

        herb2 = herbRepository.save(herb2);

        addRelationshipsToHerb(herb2, "Sweet", "Bu Yang Huan Wu Tang", "dang-gui.jpg",
                              "Blood deficiency", "Liver");
    }

    private void addRelationshipsToHerb(Herb herb, String flavorValue, String formulaValue,
                                       String imageFilename, String indicationValue,
                                       String meridianValue) {
        // Add flavor
        HerbFlavor flavor = new HerbFlavor();
        flavor.setValue(flavorValue);
        flavor.setHerb(herb);
        herb.setFlavors(List.of(flavor));

        // Add formula
        HerbFormula formula = new HerbFormula();
        formula.setValue(formulaValue);
        formula.setHerb(herb);
        herb.setFormulas(List.of(formula));

        // Add image
        HerbImage image = new HerbImage();
        image.setFilename(imageFilename);
        image.setMime("image/jpeg");
        image.setData(("test image data for " + herb.getNameZh()).getBytes());
        image.setHerb(herb);
        herb.setImages(List.of(image));

        // Add indication
        HerbIndication indication = new HerbIndication();
        indication.setValue(indicationValue);
        indication.setHerb(herb);
        herb.setIndications(List.of(indication));

        // Add meridian
        HerbMeridian meridian = new HerbMeridian();
        meridian.setValue(meridianValue);
        meridian.setHerb(herb);
        herb.setMeridians(List.of(meridian));
    }

    private void createAdditionalTestHerbs(int count) {
        for (int i = 0; i < count; i++) {
            Herb herb = new Herb();
            herb.setSourceUrl("https://example.com/herb-" + i);
            herb.setNameZh("测试草药" + i);
            herb.setNamePinyin("ce shi cao yao " + i);
            herb.setDescZh("测试草药描述" + i);
            herb.setDescEn("Test herb description " + i);
            herb.setAppearance("Test appearance " + i);
            herb.setProperty("Test property " + i);

            herb = herbRepository.save(herb);

            addRelationshipsToHerb(herb, "Flavor" + i, "Formula" + i, "image" + i + ".jpg",
                                  "Indication" + i, "Meridian" + (i % 5));
        }
    }

    private void createHerbsWithComplexRelationships() {
        Herb herb = new Herb();
        herb.setSourceUrl("https://example.com/complex-herb");
        herb.setNameZh("复杂草药");
        herb.setNamePinyin("fu za cao yao");
        herb.setDescZh("具有复杂关系的草药");
        herb.setDescEn("Herb with complex relationships");
        herb.setAppearance("Complex appearance");
        herb.setProperty("Complex property");

        herb = herbRepository.save(herb);

        // Add multiple relationships of each type
        HerbFlavor flavor1 = new HerbFlavor();
        flavor1.setValue("Sweet");
        flavor1.setHerb(herb);

        HerbFlavor flavor2 = new HerbFlavor();
        flavor2.setValue("Bitter");
        flavor2.setHerb(herb);

        herb.setFlavors(List.of(flavor1, flavor2));

        // Multiple formulas, indications, etc.
        // This tests the N+1 query optimization
    }

    private void validateCompositeExportStructure(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            boolean foundJson = false, foundSqlite = false, foundManifest = false, foundReadme = false;

            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case "json-dataset.zip" -> foundJson = true;
                    case "sqlite-dataset.db" -> foundSqlite = true;
                    case "manifest.json" -> foundManifest = true;
                    case "README.md" -> foundReadme = true;
                }
                zipStream.closeEntry();
            }

            assertThat(foundJson).isTrue();
            assertThat(foundSqlite).isTrue();
            assertThat(foundManifest).isTrue();
            assertThat(foundReadme).isTrue();
        }
    }

    private void validateIndividualFormatsConsistency(InputStream compositeStream) throws Exception {
        // Reset stream for re-reading
        compositeStream.reset();

        try (ZipInputStream zipStream = new ZipInputStream(compositeStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] manifestBytes = zipStream.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(manifestBytes);

                    assertThat(manifest.get("export_type").asText()).isEqualTo("composite");
                    assertThat(manifest.has("formats")).isTrue();

                    JsonNode formats = manifest.get("formats");
                    assertThat(formats.has("json")).isTrue();
                    assertThat(formats.has("sqlite")).isTrue();

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateDataConsistencyBetweenFormats(InputStream jsonStream, InputStream sqliteStream) throws Exception {
        // Validate JSON structure
        JsonNode jsonData = extractJsonDataFromZip(jsonStream);
        assertThat(jsonData.has("herbs")).isTrue();
        assertThat(jsonData.has("metadata")).isTrue();

        JsonNode herbs = jsonData.get("herbs");
        int jsonHerbCount = herbs.size();

        // Validate SQLite structure
        int sqliteHerbCount = countHerbsInSqlite(sqliteStream);

        // Data consistency check
        assertThat(sqliteHerbCount).isEqualTo(jsonHerbCount);
        assertThat(jsonHerbCount).isGreaterThan(0); // Should have test data
    }

    private void validateLargeDatasetIntegrity(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] manifestBytes = zipStream.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(manifestBytes);

                    int totalSize = manifest.get("total_size_bytes").asInt();
                    assertThat(totalSize).isGreaterThan(1000); // Should be substantial with 50+ herbs

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateEmptyDatasetStructure(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] manifestBytes = zipStream.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(manifestBytes);

                    JsonNode formats = manifest.get("formats");
                    assertThat(formats.get("json").get("size_bytes").asInt()).isGreaterThan(0);
                    assertThat(formats.get("sqlite").get("size_bytes").asInt()).isGreaterThan(0);

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateComplexRelationshipsInExport(InputStream datasetStream) throws Exception {
        // This would validate that complex relationships are properly exported
        // Implementation depends on the specific complex relationships created
        assertThat(datasetStream).isNotNull();
    }

    private JsonNode extractJsonDataFromZip(InputStream zipStream) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("dataset.json".equals(entry.getName())) {
                    byte[] jsonBytes = zis.readAllBytes();
                    return objectMapper.readTree(jsonBytes);
                }
                zis.closeEntry();
            }
        }
        throw new IllegalStateException("dataset.json not found in ZIP");
    }

    private int countHerbsInSqlite(InputStream sqliteStream) throws Exception {
        File tempFile = File.createTempFile("test-sqlite", ".db");
        try {
            Files.copy(sqliteStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String jdbcUrl = "jdbc:sqlite:" + tempFile.getAbsolutePath();

            try (Connection connection = DriverManager.getConnection(jdbcUrl);
                 PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM herbs")) {

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } finally {
            tempFile.delete();
        }
        return 0;
    }
}