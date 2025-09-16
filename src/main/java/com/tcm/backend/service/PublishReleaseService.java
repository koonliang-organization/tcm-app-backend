package com.tcm.backend.service;

import com.tcm.backend.dto.PublishReleaseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PublishReleaseService {

    PublishReleaseDto createDraft(String versionName);

    PublishReleaseDto markReadyForReview(UUID releaseId);

    PublishReleaseDto approveRelease(UUID releaseId, UUID approverId);

    Page<PublishReleaseDto> listReleases(Pageable pageable);

    PublishReleaseDto getLatestApproved();
}
