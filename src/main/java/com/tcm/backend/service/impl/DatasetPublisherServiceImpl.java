package com.tcm.backend.service.impl;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import com.tcm.backend.publisher.DatasetExportService;
import com.tcm.backend.publisher.DatasetManifestFactory;
import com.tcm.backend.publisher.DatasetStorageClient;
import com.tcm.backend.repository.PublishReleaseRepository;
import com.tcm.backend.service.DatasetPublisherService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class DatasetPublisherServiceImpl implements DatasetPublisherService {

    @Autowired
    private PublishReleaseRepository publishReleaseRepository;

    @Autowired
    private DatasetExportService datasetExportService;

    @Autowired
    private DatasetStorageClient datasetStorageClient;

    @Autowired
    private DatasetManifestFactory datasetManifestFactory;

    @Value("${publisher.min-app-version:1.0.0}")
    private String minAppVersion;

    @Override
    @Transactional
    public void publishRelease(UUID releaseId) {
        PublishRelease release = publishReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found"));
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            throw new IllegalStateException("Release must be approved before publishing");
        }

        DatasetExportService.ExportResult exportResult = datasetExportService.exportDataset();
        String objectKey = release.getVersionName() + ".zip";
        try (InputStream datasetStream = exportResult.datasetStream()) {
            DatasetStorageClient.StorageResult storageResult =
                    datasetStorageClient.storeDataset(objectKey, datasetStream, exportResult.sizeBytes());
            String manifestJson = datasetManifestFactory.createManifest(
                    release,
                    exportResult.sizeBytes(),
                    storageResult.checksum(),
                    storageResult.url(),
                    minAppVersion
            );
            release.setManifestJson(manifestJson);
            release.setChecksum(storageResult.checksum());
            release.setStorageUrl(storageResult.url());
            publishReleaseRepository.save(release);
            log.info("Published release {} with checksum {}", release.getVersionName(), storageResult.checksum());
        } catch (Exception e) {
            release.setStatus(ReleaseStatus.FAILED);
            publishReleaseRepository.save(release);
            throw new IllegalStateException("Failed to publish release", e);
        }
    }
}
