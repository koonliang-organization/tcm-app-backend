package com.tcm.backend.service;

import com.tcm.backend.dto.PublishReleaseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PublishReleaseService {

    PublishReleaseDto createDraft(String versionName);

    PublishReleaseDto markReadyForReview(String releaseId);

    PublishReleaseDto approveRelease(String releaseId, String approverId);

    Page<PublishReleaseDto> listReleases(Pageable pageable);

    PublishReleaseDto getLatestApproved();
}
