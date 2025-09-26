package com.tcm.backend.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class CompositeDatasetExportServiceTest {

    @Mock
    private JsonDatasetExportService jsonDatasetExportService;

    @Mock
    private SqliteDatasetExportService sqliteDatasetExportService;

    @InjectMocks
    private CompositeDatasetExportService compositeDatasetExportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void exportDatasetCreatesCombinedZipArchive() throws Exception {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 9);
        DatasetExportService.ExportResult sqliteResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("sqlite data".getBytes()), 11);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset()).thenReturn(sqliteResult);

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(jsonResult.sizeBytes() + sqliteResult.sizeBytes());

        validateCompositeZipStructure(result.datasetStream());
    }

    @Test
    void exportDatasetCreatesCorrectManifest() throws Exception {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 100);
        DatasetExportService.ExportResult sqliteResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("sqlite data".getBytes()), 200);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset()).thenReturn(sqliteResult);

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        validateManifestContent(result.datasetStream(), jsonResult, sqliteResult);
    }

    @Test
    void exportDatasetCreatesComprehensiveReadme() throws Exception {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 100);
        DatasetExportService.ExportResult sqliteResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("sqlite data".getBytes()), 200);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset()).thenReturn(sqliteResult);

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        validateReadmeContent(result.datasetStream());
    }

    @Test
    void exportDatasetHandlesJsonServiceException() {
        when(jsonDatasetExportService.exportDataset())
            .thenThrow(new RuntimeException("JSON export failed"));

        assertThrows(IllegalStateException.class, () ->
            compositeDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetHandlesSqliteServiceException() {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 100);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset())
            .thenThrow(new RuntimeException("SQLite export failed"));

        assertThrows(IllegalStateException.class, () ->
            compositeDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetCalculatesCorrectSizes() throws Exception {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 150);
        DatasetExportService.ExportResult sqliteResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("sqlite data".getBytes()), 250);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset()).thenReturn(sqliteResult);

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        assertThat(result.sizeBytes()).isGreaterThan(400); // Should be larger due to ZIP overhead

        validateSizeCalculations(result.datasetStream(), jsonResult, sqliteResult);
    }

    @Test
    void exportDatasetIncludesAllRequiredFiles() throws Exception {
        DatasetExportService.ExportResult jsonResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("json data".getBytes()), 100);
        DatasetExportService.ExportResult sqliteResult = new DatasetExportService.ExportResult(
            new ByteArrayInputStream("sqlite data".getBytes()), 200);

        when(jsonDatasetExportService.exportDataset()).thenReturn(jsonResult);
        when(sqliteDatasetExportService.exportDataset()).thenReturn(sqliteResult);

        DatasetExportService.ExportResult result = compositeDatasetExportService.exportDataset();

        validateRequiredFiles(result.datasetStream());
    }

    private void validateCompositeZipStructure(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            boolean foundJsonDataset = false;
            boolean foundSqliteDataset = false;
            boolean foundManifest = false;
            boolean foundReadme = false;

            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                switch (entryName) {
                    case "json-dataset.zip":
                        foundJsonDataset = true;
                        break;
                    case "sqlite-dataset.db":
                        foundSqliteDataset = true;
                        break;
                    case "manifest.json":
                        foundManifest = true;
                        break;
                    case "README.md":
                        foundReadme = true;
                        break;
                }
                zipStream.closeEntry();
            }

            assertThat(foundJsonDataset).isTrue();
            assertThat(foundSqliteDataset).isTrue();
            assertThat(foundManifest).isTrue();
            assertThat(foundReadme).isTrue();
        }
    }

    private void validateManifestContent(InputStream datasetStream,
                                       DatasetExportService.ExportResult jsonResult,
                                       DatasetExportService.ExportResult sqliteResult) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] manifestBytes = zipStream.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(manifestBytes);

                    assertThat(manifest.get("export_type").asText()).isEqualTo("composite");
                    assertThat(manifest.has("export_timestamp")).isTrue();
                    assertThat(manifest.get("export_timestamp").asLong()).isGreaterThan(0);

                    JsonNode formats = manifest.get("formats");
                    assertThat(formats).isNotNull();

                    JsonNode json = formats.get("json");
                    assertThat(json.get("filename").asText()).isEqualTo("json-dataset.zip");
                    assertThat(json.get("size_bytes").asInt()).isEqualTo(jsonResult.sizeBytes());
                    assertThat(json.get("description").asText()).contains("JSON format");

                    JsonNode sqlite = formats.get("sqlite");
                    assertThat(sqlite.get("filename").asText()).isEqualTo("sqlite-dataset.db");
                    assertThat(sqlite.get("size_bytes").asInt()).isEqualTo(sqliteResult.sizeBytes());
                    assertThat(sqlite.get("description").asText()).contains("SQLite database");

                    long totalExpectedSize = (long) jsonResult.sizeBytes() + (long) sqliteResult.sizeBytes();
                    assertThat(manifest.get("total_size_bytes").asInt()).isEqualTo((int) totalExpectedSize);

                    JsonNode recommendedUsage = manifest.get("recommended_usage");
                    assertThat(recommendedUsage.get("json").asText()).contains("Web applications");
                    assertThat(recommendedUsage.get("sqlite").asText()).contains("Mobile applications");

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateReadmeContent(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("README.md".equals(entry.getName())) {
                    byte[] readmeBytes = zipStream.readAllBytes();
                    String readmeContent = new String(readmeBytes);

                    assertThat(readmeContent).contains("TCM Herbs Dataset (Composite Format)");
                    assertThat(readmeContent).contains("json-dataset.zip");
                    assertThat(readmeContent).contains("sqlite-dataset.db");
                    assertThat(readmeContent).contains("manifest.json");
                    assertThat(readmeContent).contains("README.md");

                    // Check format comparison table
                    assertThat(readmeContent).contains("| Format | Best For | Features | Size |");
                    assertThat(readmeContent).contains("JSON");
                    assertThat(readmeContent).contains("SQLite");

                    // Check usage examples
                    assertThat(readmeContent).contains("Usage Examples");
                    assertThat(readmeContent).contains("JSON Format");
                    assertThat(readmeContent).contains("SQLite Format");

                    // Check SQL examples
                    assertThat(readmeContent).contains("SELECT * FROM herbs_fts WHERE herbs_fts MATCH");
                    assertThat(readmeContent).contains("GROUP_CONCAT");

                    // Check technical specifications
                    assertThat(readmeContent).contains("Technical Specifications");
                    assertThat(readmeContent).contains("UTF-8");
                    assertThat(readmeContent).contains("SQLite version");

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateSizeCalculations(InputStream datasetStream,
                                        DatasetExportService.ExportResult jsonResult,
                                        DatasetExportService.ExportResult sqliteResult) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] manifestBytes = zipStream.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(manifestBytes);

                    JsonNode formats = manifest.get("formats");
                    int jsonSize = formats.get("json").get("size_bytes").asInt();
                    int sqliteSize = formats.get("sqlite").get("size_bytes").asInt();
                    int totalSize = manifest.get("total_size_bytes").asInt();

                    assertThat(jsonSize).isEqualTo(jsonResult.sizeBytes());
                    assertThat(sqliteSize).isEqualTo(sqliteResult.sizeBytes());
                    assertThat(totalSize).isEqualTo(jsonSize + sqliteSize);

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }

    private void validateRequiredFiles(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            int fileCount = 0;
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                fileCount++;
                zipStream.closeEntry();
            }

            assertThat(fileCount).isEqualTo(4); // json-dataset.zip, sqlite-dataset.db, manifest.json, README.md
        }
    }
}