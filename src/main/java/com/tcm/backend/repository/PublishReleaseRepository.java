package com.tcm.backend.repository;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublishReleaseRepository extends JpaRepository<PublishRelease, UUID> {

    Optional<PublishRelease> findFirstByStatusOrderByCreatedAtDesc(ReleaseStatus status);
}
