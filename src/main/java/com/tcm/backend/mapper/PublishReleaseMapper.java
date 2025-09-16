package com.tcm.backend.mapper;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.dto.PublishReleaseDto;
import org.springframework.stereotype.Component;

@Component
public class PublishReleaseMapper {

    public PublishReleaseDto toDto(PublishRelease release) {
        return new PublishReleaseDto(
                release.getId(),
                release.getVersionName(),
                release.getStatus(),
                release.getManifestJson(),
                release.getChecksum(),
                release.getStorageUrl(),
                release.getApprovedAt(),
                release.getApprovedBy()
        );
    }
}
