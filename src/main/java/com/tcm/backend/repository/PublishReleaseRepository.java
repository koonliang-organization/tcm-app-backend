package com.tcm.backend.repository;

import com.tcm.backend.domain.PublishRelease;
import com.tcm.backend.domain.PublishRelease.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublishReleaseRepository extends JpaRepository<PublishRelease, String> {

    Optional<PublishRelease> findFirstByStatusOrderByCreatedAtDesc(ReleaseStatus status);
}
