package com.tcm.backend.service.impl;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import com.tcm.backend.dto.PublishReleaseDto;
import com.tcm.backend.mapper.PublishReleaseMapper;
import com.tcm.backend.repository.PublishReleaseRepository;
import com.tcm.backend.service.PublishReleaseService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PublishReleaseServiceImpl implements PublishReleaseService {

    @Autowired
    private PublishReleaseRepository publishReleaseRepository;

    @Autowired
    private PublishReleaseMapper publishReleaseMapper;

    @Override
    @Transactional
    public PublishReleaseDto createDraft(String versionName) {
        PublishRelease release = new PublishRelease();
        release.setVersionName(versionName);
        release.setStatus(ReleaseStatus.DRAFT);
        PublishRelease saved = publishReleaseRepository.save(release);
        return publishReleaseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PublishReleaseDto markReadyForReview(String releaseId) {
        PublishRelease release = publishReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found"));
        release.setStatus(ReleaseStatus.READY_FOR_REVIEW);
        PublishRelease saved = publishReleaseRepository.save(release);
        return publishReleaseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PublishReleaseDto approveRelease(String releaseId, String approverId) {
        PublishRelease release = publishReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found"));
        release.setStatus(ReleaseStatus.APPROVED);
        release.setApprovedBy(approverId);
        release.setApprovedAt(Instant.now());
        PublishRelease saved = publishReleaseRepository.save(release);
        return publishReleaseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public Page<PublishReleaseDto> listReleases(Pageable pageable) {
        return publishReleaseRepository.findAll(pageable).map(publishReleaseMapper::toDto);
    }

    @Override
    @Transactional
    public PublishReleaseDto getLatestApproved() {
        PublishRelease release = publishReleaseRepository
                .findFirstByStatusOrderByCreatedAtDesc(ReleaseStatus.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("No approved releases"));
        return publishReleaseMapper.toDto(release);
    }
}
