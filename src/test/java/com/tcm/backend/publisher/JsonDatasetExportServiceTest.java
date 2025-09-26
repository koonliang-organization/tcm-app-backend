package com.tcm.backend.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.domain.*;
import com.tcm.backend.publisher.dto.*;
import com.tcm.backend.publisher.mapper.HerbExportMapper;
import com.tcm.backend.repository.HerbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class JsonDatasetExportServiceTest {

    @Mock
    private HerbRepository herbRepository;

    @Mock
    private HerbExportMapper herbExportMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonDatasetExportService jsonDatasetExportService;

    private final ObjectMapper realObjectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void exportDatasetWithEmptyDatabaseReturnsEmptyDataset() throws Exception {
        when(herbRepository.findAllWithRelationsForExport()).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        DatasetExportService.ExportResult result = jsonDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(500); // Should have ZIP structure with README
    }

    @Test
    void exportDatasetWithSingleHerbCreatesCompleteExport() throws Exception {
        Herb herb = createTestHerb();
        List<Herb> herbs = List.of(herb);

        HerbExportDto herbExportDto = createTestHerbExportDto();
        List<HerbExportDto> herbExportDtos = List.of(herbExportDto);

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);
        when(objectMapper.writeValueAsBytes(any())).thenAnswer(invocation ->
            realObjectMapper.writeValueAsBytes(invocation.getArgument(0)));

        DatasetExportService.ExportResult result = jsonDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        validateZipStructure(result.datasetStream());
    }

    @Test
    void exportDatasetWithMultipleHerbsCalculatesCorrectStatistics() throws Exception {
        List<Herb> herbs = List.of(createTestHerb(), createTestHerb());
        List<Integer> herbIds = List.of(1, 2);

        List<HerbExportDto> herbExportDtos = List.of(
            createTestHerbExportDto(),
            createTestHerbExportDto()
        );

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);
        when(objectMapper.writeValueAsBytes(any())).thenAnswer(invocation -> {
            Object data = invocation.getArgument(0);
            return realObjectMapper.writeValueAsBytes(data);
        });

        DatasetExportService.ExportResult result = jsonDatasetExportService.exportDataset();

        assertThat(result).isNotNull();
        assertThat(result.sizeBytes()).isGreaterThan(0);

        validateDatasetContent(result.datasetStream(), 2);
    }

    @Test
    void exportDatasetHandlesRepositoryException() {
        when(herbRepository.findAllWithRelationsForExport())
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(IllegalStateException.class, () ->
            jsonDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetHandlesMapperException() {
        List<Herb> herbs = List.of(createTestHerb());

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(anyList()))
            .thenThrow(new RuntimeException("Mapping error"));

        assertThrows(IllegalStateException.class, () ->
            jsonDatasetExportService.exportDataset());
    }

    @Test
    void exportDatasetHandlesSerializationException() throws Exception {
        List<Herb> herbs = List.of(createTestHerb());
        List<HerbExportDto> herbExportDtos = List.of(createTestHerbExportDto());

        when(herbRepository.findAllWithRelationsForExport()).thenReturn(herbs);
        when(herbExportMapper.toExportDtos(herbs)).thenReturn(herbExportDtos);
        when(objectMapper.writeValueAsBytes(any()))
            .thenThrow(new RuntimeException("Serialization error"));

        assertThrows(IllegalStateException.class, () ->
            jsonDatasetExportService.exportDataset());
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

    private void validateZipStructure(InputStream datasetStream) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            boolean foundDatasetJson = false;
            boolean foundReadme = false;

            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("dataset.json".equals(entry.getName())) {
                    foundDatasetJson = true;
                }
                if ("README.txt".equals(entry.getName())) {
                    foundReadme = true;
                }
                zipStream.closeEntry();
            }

            assertThat(foundDatasetJson).isTrue();
            assertThat(foundReadme).isTrue();
        }
    }

    private void validateDatasetContent(InputStream datasetStream, int expectedHerbCount) throws Exception {
        try (ZipInputStream zipStream = new ZipInputStream(datasetStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if ("dataset.json".equals(entry.getName())) {
                    byte[] jsonBytes = zipStream.readAllBytes();
                    JsonNode jsonNode = realObjectMapper.readTree(jsonBytes);

                    assertThat(jsonNode.has("herbs")).isTrue();
                    assertThat(jsonNode.has("metadata")).isTrue();

                    JsonNode herbs = jsonNode.get("herbs");
                    assertThat(herbs.size()).isEqualTo(expectedHerbCount);

                    JsonNode metadata = jsonNode.get("metadata");
                    assertThat(metadata.get("total_herbs").asInt()).isEqualTo(expectedHerbCount);
                    assertThat(metadata.has("export_timestamp")).isTrue();
                    assertThat(metadata.get("export_format").asText()).isEqualTo("json");

                    break;
                }
                zipStream.closeEntry();
            }
        }
    }
}