package com.tcm.backend.dto;

import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record PublishReleaseDto(
        UUID id,
        @NotBlank @Size(max = 32) String versionName,
        @NotNull ReleaseStatus status,
        String manifestJson,
        String checksum,
        String storageUrl,
        Instant approvedAt,
        UUID approvedBy
) {
    public PublishReleaseDto {
        // canonical constructor for validation hooks if needed later
    }
}
