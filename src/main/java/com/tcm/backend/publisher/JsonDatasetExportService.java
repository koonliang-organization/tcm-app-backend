package com.tcm.backend.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.domain.Herb;
import com.tcm.backend.publisher.dto.HerbExportDto;
import com.tcm.backend.publisher.mapper.HerbExportMapper;
import com.tcm.backend.repository.HerbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonDatasetExportService implements DatasetExportService {

    private final HerbRepository herbRepository;
    private final HerbExportMapper herbExportMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ExportResult exportDataset() {
        log.info("Starting JSON dataset export");

        try {
            // First, fetch all herbs without relations
            List<Herb> herbs = herbRepository.findAllWithRelationsForExport();
            log.info("Loaded {} herbs for export", herbs.size());

            // If no herbs, return empty dataset
            if (herbs.isEmpty()) {
                Map<String, Object> emptyDataset = createDatasetStructure(List.of());
                return createZipFile(emptyDataset);
            }

            // Get herb IDs for fetching relations
            List<Integer> herbIds = herbs.stream()
                    .map(Herb::getId)
                    .toList();

            // Load relations separately to avoid MultipleBagFetchException
            loadHerbRelations(herbIds);

            List<HerbExportDto> herbExportDtos = herbExportMapper.toExportDtos(herbs);

            Map<String, Object> dataset = createDatasetStructure(herbExportDtos);

            return createZipFile(dataset);
        } catch (Exception e) {
            log.error("Failed to export JSON dataset", e);
            throw new IllegalStateException("Failed to export dataset", e);
        }
    }

    private void loadHerbRelations(List<Integer> herbIds) {
        log.debug("Loading relations for {} herbs", herbIds.size());

        // Load each relation type separately to avoid MultipleBagFetchException
        herbRepository.findByIdInWithFlavors(herbIds);
        herbRepository.findByIdInWithFormulas(herbIds);
        herbRepository.findByIdInWithImages(herbIds);
        herbRepository.findByIdInWithIndications(herbIds);
        herbRepository.findByIdInWithMeridians(herbIds);

        log.debug("Finished loading all relations");
    }

    private Map<String, Object> createDatasetStructure(List<HerbExportDto> herbs) {
        Map<String, Object> dataset = new HashMap<>();

        // Add herbs data
        dataset.put("herbs", herbs);

        // Add export metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("export_timestamp", System.currentTimeMillis());
        metadata.put("export_format", "json");
        metadata.put("total_herbs", herbs.size());

        // Calculate counts for related entities
        int totalFlavors = herbs.stream()
                .mapToInt(h -> h.flavors() != null ? h.flavors().size() : 0)
                .sum();
        int totalFormulas = herbs.stream()
                .mapToInt(h -> h.formulas() != null ? h.formulas().size() : 0)
                .sum();
        int totalImages = herbs.stream()
                .mapToInt(h -> h.images() != null ? h.images().size() : 0)
                .sum();
        int totalIndications = herbs.stream()
                .mapToInt(h -> h.indications() != null ? h.indications().size() : 0)
                .sum();
        int totalMeridians = herbs.stream()
                .mapToInt(h -> h.meridians() != null ? h.meridians().size() : 0)
                .sum();

        metadata.put("total_flavors", totalFlavors);
        metadata.put("total_formulas", totalFormulas);
        metadata.put("total_images", totalImages);
        metadata.put("total_indications", totalIndications);
        metadata.put("total_meridians", totalMeridians);

        dataset.put("metadata", metadata);

        log.info("Created dataset with {} herbs, {} flavors, {} formulas, {} images, {} indications, {} meridians",
                herbs.size(), totalFlavors, totalFormulas, totalImages, totalIndications, totalMeridians);

        return dataset;
    }

    private ExportResult createZipFile(Map<String, Object> dataset) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteStream)) {

            // Add main dataset file
            zipOutputStream.putNextEntry(new ZipEntry("dataset.json"));

            // Write JSON to a separate byte array first to avoid stream closing issues
            byte[] jsonBytes = objectMapper.writeValueAsBytes(dataset);
            zipOutputStream.write(jsonBytes);
            zipOutputStream.closeEntry();

            // Add readme file
            zipOutputStream.putNextEntry(new ZipEntry("README.txt"));
            String readme = createReadmeContent();
            zipOutputStream.write(readme.getBytes());
            zipOutputStream.closeEntry();

            zipOutputStream.finish();
            byte[] bytes = byteStream.toByteArray();

            log.info("Created JSON dataset ZIP file with {} bytes", bytes.length);

            InputStream datasetStream = new ByteArrayInputStream(bytes);
            return new ExportResult(datasetStream, bytes.length);
        }
    }

    private String createReadmeContent() {
        return """
                TCM Herbs Dataset (JSON Format)
                ===============================

                This dataset contains Traditional Chinese Medicine (TCM) herbs data in JSON format.

                Files:
                - dataset.json: Main dataset file containing herbs and metadata
                - README.txt: This file

                Structure:
                - herbs[]: Array of herb objects with complete information
                - metadata: Export statistics and timestamp information

                Each herb includes:
                - Basic information (names in Chinese and Pinyin, descriptions, properties)
                - Flavors: Taste characteristics
                - Formulas: Associated formulations
                - Images: Visual representations with binary data
                - Indications: Medical uses and applications
                - Meridians: Related meridian channels

                Generated by TCM App Backend Dataset Publisher
                """;
    }
}
