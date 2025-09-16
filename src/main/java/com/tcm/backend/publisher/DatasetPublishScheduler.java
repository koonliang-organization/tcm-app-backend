package com.tcm.backend.publisher;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import com.tcm.backend.repository.PublishReleaseRepository;
import com.tcm.backend.service.DatasetPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetPublishScheduler {

    private final PublishReleaseRepository publishReleaseRepository;
    private final DatasetPublisherService datasetPublisherService;

    @Scheduled(cron = "0 */15 * * * *")
    public void publishScheduledReleases() {
        List<PublishRelease> releases = publishReleaseRepository.findAll().stream()
                .filter(release -> release.getStatus() == ReleaseStatus.APPROVED && release.getStorageUrl() == null)
                .toList();
        releases.forEach(release -> {
            try {
                datasetPublisherService.publishRelease(release.getId());
            } catch (Exception e) {
                log.error("Failed to publish release {}", release.getVersionName(), e);
            }
        });
    }
}
