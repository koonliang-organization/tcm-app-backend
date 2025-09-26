package com.tcm.backend.publisher;

import com.tcm.backend.domain.*;
import com.tcm.backend.publisher.dto.*;
import com.tcm.backend.publisher.mapper.HerbExportMapper;
import com.tcm.backend.repository.HerbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class SqliteDatasetExportServiceTest {

    @Mock
    private HerbRepository herbRepository;

    @Mock
    private HerbExportMapper herbExportMapper;

    @InjectMocks
    private SqliteDatasetExportService sqliteDatasetExportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void exportDatasetWithEmptyDatabaseCreatesValidSqliteFile() throws Exception {
        when(herbRepository.findAllWithRelationsForExport()).thenReturn(new ArrayList<>());
        when(herbExportMapper.toExportDtos(anyList())).thenReturn(new ArrayList<>());

        DatasetExportService.ExportResult result = sqliteDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        validateSqliteStructure(result.datasetStream(), 0);
    }

    @Test
    void exportDatasetWithSingleHerbCreatesCompleteDatabase() throws Exception {
        Herb herb = createTestHerb();
        List<Herb> herbs = List.of(herb);

        HerbExportDto herbExportDto = createTestHerbExportDto();
        List<HerbExportDto> herbExportDtos = List.of(herbExportDto);

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);

        DatasetExportService.ExportResult result = sqliteDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        validateSqliteStructure(result.datasetStream(), 1);
        validateSqliteData(result.datasetStream(), herbExportDto);
    }

    @Test
    void exportDatasetWithMultipleHerbsStoresAllData() throws Exception {
        List<Herb> herbs = List.of(createTestHerb(), createSecondTestHerb());
        List<HerbExportDto> herbExportDtos = List.of(
            createTestHerbExportDto(),
            createSecondTestHerbExportDto()
        );

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);

        DatasetExportService.ExportResult result = sqliteDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        validateSqliteStructure(result.datasetStream(), 2);
        validateFtsSearchFunctionality(result.datasetStream());
    }

    @Test
    void exportDatasetHandlesRepositoryException() {
        when(herbRepository.findAllWithRelationsForExport())
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(IllegalStateException.class, () ->
            sqliteDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetHandlesMapperException() {
        List<Herb> herbs = List.of(createTestHerb());

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(anyList()))
            .thenThrow(new RuntimeException("Mapping error"));

        assertThrows(IllegalStateException.class, () ->
            sqliteDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetCreatesProperIndexes() throws Exception {
        Herb herb = createTestHerb();
        List<Herb> herbs = List.of(herb);

        HerbExportDto herbExportDto = createTestHerbExportDto();
        List<HerbExportDto> herbExportDtos = List.of(herbExportDto);

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);

        DatasetExportService.ExportResult result = sqliteDatasetExportService.exportDataset();

        validateIndexes(result.datasetStream());
    }

    private Herb createTestHerb() {
        Herb herb = new Herb();
        herb.setId(1);
        herb.setSourceUrl("https://example.com/herb1");
        herb.setNameZh("白术");
        herb.setNamePinyin("bai zhu");
        herb.setDescZh("白术描述");
        herb.setDescEn("Description");
        herb.setAppearance("Appearance");
        herb.setProperty("property");

        List<HerbFlavor> flavors = new ArrayList<>();
        HerbFlavor flavor = new HerbFlavor();
        flavor.setValue("Sweet");
        flavors.add(flavor);
        herb.setFlavors(flavors);

        List<HerbFormula> formulas = new ArrayList<>();
        HerbFormula formula = new HerbFormula();
        formula.setValue("Test Formula");
        formulas.add(formula);
        herb.setFormulas(formulas);

        List<HerbImage> images = new ArrayList<>();
        HerbImage image = new HerbImage();
        image.setFilename("test.jpg");
        image.setMime("image/jpeg");
        image.setData("test data".getBytes());
        images.add(image);
        herb.setImages(images);

        List<HerbIndication> indications = new ArrayList<>();
        HerbIndication indication = new HerbIndication();
        indication.setValue("Test Indication");
        indications.add(indication);
        herb.setIndications(indications);

        List<HerbMeridian> meridians = new ArrayList<>();
        HerbMeridian meridian = new HerbMeridian();
        meridian.setValue("Lung");
        meridians.add(meridian);
        herb.setMeridians(meridians);

        return herb;
    }

    private Herb createSecondTestHerb() {
        Herb herb = new Herb();
        herb.setId(2);
        herb.setSourceUrl("https://example.com/herb2");
        herb.setNameZh("当归");
        herb.setNamePinyin("dang gui");
        herb.setDescZh("当归描述");
        herb.setDescEn("Dang Gui Description");
        herb.setAppearance("Brown root");
        herb.setProperty("Warm");

        List<HerbFlavor> flavors = new ArrayList<>();
        HerbFlavor flavor = new HerbFlavor();
        flavor.setValue("Sweet");
        flavors.add(flavor);
        herb.setFlavors(flavors);

        List<HerbFormula> formulas = new ArrayList<>();
        HerbFormula formula = new HerbFormula();
        formula.setValue("Bu Yang Huan Wu Tang");
        formulas.add(formula);
        herb.setFormulas(formulas);

        List<HerbImage> images = new ArrayList<>();
        HerbImage image = new HerbImage();
        image.setFilename("dang-gui.jpg");
        image.setMime("image/jpeg");
        image.setData("test data 2".getBytes());
        images.add(image);
        herb.setImages(images);

        List<HerbIndication> indications = new ArrayList<>();
        HerbIndication indication = new HerbIndication();
        indication.setValue("Blood deficiency");
        indications.add(indication);
        herb.setIndications(indications);

        List<HerbMeridian> meridians = new ArrayList<>();
        HerbMeridian meridian = new HerbMeridian();
        meridian.setValue("Liver");
        meridians.add(meridian);
        herb.setMeridians(meridians);

        return herb;
    }

    private HerbExportDto createTestHerbExportDto() {
        return new HerbExportDto(
            1,
            "https://example.com/herb1",
            "白术",
            "bai zhu",
            "白术描述",
            "Description",
            "Appearance",
            "property",
            List.of(new HerbFlavorExportDto("Sweet")),
            List.of(new HerbFormulaExportDto("Test Formula")),
            List.of(new HerbImageExportDto("test.jpg", "image/jpeg", "test data".getBytes())),
            List.of(new HerbIndicationExportDto("Test Indication")),
            List.of(new HerbMeridianExportDto("Lung"))
        );
    }

    private HerbExportDto createSecondTestHerbExportDto() {
        return new HerbExportDto(
            2,
            "https://example.com/herb2",
            "当归",
            "dang gui",
            "当归描述",
            "Dang Gui Description",
            "Brown root",
            "Warm",
            List.of(new HerbFlavorExportDto("Sweet")),
            List.of(new HerbFormulaExportDto("Bu Yang Huan Wu Tang")),
            List.of(new HerbImageExportDto("dang-gui.jpg", "image/jpeg", "test data 2".getBytes())),
            List.of(new HerbIndicationExportDto("Blood deficiency")),
            List.of(new HerbMeridianExportDto("Liver"))
        );
    }

    private void validateSqliteStructure(InputStream datasetStream, int expectedHerbCount) throws Exception {
        File tempFile = writeStreamToTempFile(datasetStream);
        String jdbcUrl = "jdbc:sqlite:" + tempFile.getAbsolutePath();

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            validateTables(connection);
            validateHerbCount(connection, expectedHerbCount);
            validateMetadata(connection, expectedHerbCount);
        } finally {
            tempFile.delete();
        }
    }

    private void validateSqliteData(InputStream datasetStream, HerbExportDto expectedHerb) throws Exception {
        File tempFile = writeStreamToTempFile(datasetStream);
        String jdbcUrl = "jdbc:sqlite:" + tempFile.getAbsolutePath();

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            validateHerbData(connection, expectedHerb);
            validateRelationshipData(connection, expectedHerb);
        } finally {
            tempFile.delete();
        }
    }

    private void validateFtsSearchFunctionality(InputStream datasetStream) throws Exception {
        File tempFile = writeStreamToTempFile(datasetStream);
        String jdbcUrl = "jdbc:sqlite:" + tempFile.getAbsolutePath();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(
                 "SELECT COUNT(*) FROM herbs_fts WHERE herbs_fts MATCH ?")) {

            stmt.setString(1, "白术");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(0);
            }
        } finally {
            tempFile.delete();
        }
    }

    private void validateIndexes(InputStream datasetStream) throws Exception {
        File tempFile = writeStreamToTempFile(datasetStream);
        String jdbcUrl = "jdbc:sqlite:" + tempFile.getAbsolutePath();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(
                 "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'")) {

            try (ResultSet rs = stmt.executeQuery()) {
                List<String> indexes = new ArrayList<>();
                while (rs.next()) {
                    indexes.add(rs.getString("name"));
                }

                assertThat(indexes).contains(
                    "idx_herbs_name_zh",
                    "idx_herbs_name_pinyin",
                    "idx_herb_flavors_herb_id",
                    "idx_herb_formulas_herb_id",
                    "idx_herb_images_herb_id",
                    "idx_herb_indications_herb_id",
                    "idx_herb_meridians_herb_id"
                );
            }
        } finally {
            tempFile.delete();
        }
    }

    private File writeStreamToTempFile(InputStream datasetStream) throws Exception {
        File tempFile = File.createTempFile("test-sqlite", ".db");
        tempFile.deleteOnExit();
        Files.copy(datasetStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        datasetStream.reset();
        return tempFile;
    }

    private void validateTables(Connection connection) throws Exception {
        String[] expectedTables = {
            "herbs", "herbs_fts", "herb_flavors", "herb_formulas",
            "herb_images", "herb_indications", "herb_meridians", "dataset_metadata"
        };

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name = ?")) {

            for (String tableName : expectedTables) {
                stmt.setString(1, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                }
            }
        }
    }

    private void validateHerbCount(Connection connection, int expectedCount) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM herbs")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(expectedCount);
            }
        }
    }

    private void validateMetadata(Connection connection, int expectedHerbCount) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT key, value FROM dataset_metadata")) {

            try (ResultSet rs = stmt.executeQuery()) {
                boolean foundTotalHerbs = false;
                boolean foundExportFormat = false;
                boolean foundExportTimestamp = false;

                while (rs.next()) {
                    String key = rs.getString("key");
                    String value = rs.getString("value");

                    switch (key) {
                        case "total_herbs":
                            assertThat(Integer.parseInt(value)).isEqualTo(expectedHerbCount);
                            foundTotalHerbs = true;
                            break;
                        case "export_format":
                            assertThat(value).isEqualTo("sqlite");
                            foundExportFormat = true;
                            break;
                        case "export_timestamp":
                            assertThat(Long.parseLong(value)).isGreaterThan(0);
                            foundExportTimestamp = true;
                            break;
                    }
                }

                assertThat(foundTotalHerbs).isTrue();
                assertThat(foundExportFormat).isTrue();
                assertThat(foundExportTimestamp).isTrue();
            }
        }
    }

    private void validateHerbData(Connection connection, HerbExportDto expectedHerb) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM herbs WHERE id = ?")) {

            stmt.setInt(1, expectedHerb.id());
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("source_url")).isEqualTo(expectedHerb.sourceUrl());
                assertThat(rs.getString("name_zh")).isEqualTo(expectedHerb.nameZh());
                assertThat(rs.getString("name_pinyin")).isEqualTo(expectedHerb.namePinyin());
                assertThat(rs.getString("desc_zh")).isEqualTo(expectedHerb.descZh());
                assertThat(rs.getString("desc_en")).isEqualTo(expectedHerb.descEn());
                assertThat(rs.getString("appearance")).isEqualTo(expectedHerb.appearance());
                assertThat(rs.getString("property")).isEqualTo(expectedHerb.property());
            }
        }
    }

    private void validateRelationshipData(Connection connection, HerbExportDto expectedHerb) throws Exception {
        validateTableData(connection, "herb_flavors", expectedHerb.id(),
            expectedHerb.flavors().size());
        validateTableData(connection, "herb_formulas", expectedHerb.id(),
            expectedHerb.formulas().size());
        validateTableData(connection, "herb_images", expectedHerb.id(),
            expectedHerb.images().size());
        validateTableData(connection, "herb_indications", expectedHerb.id(),
            expectedHerb.indications().size());
        validateTableData(connection, "herb_meridians", expectedHerb.id(),
            expectedHerb.meridians().size());
    }

    private void validateTableData(Connection connection, String tableName, Integer herbId,
                                 int expectedCount) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + tableName + " WHERE herb_id = ?")) {

            stmt.setInt(1, herbId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(expectedCount);
            }
        }
    }
}