package com.tcm.backend.repository;

import com.tcm.backend.domain.Herb;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HerbRepository extends JpaRepository<Herb, UUID> {

    @EntityGraph(attributePaths = {})
    Optional<Herb> findByLatinNameIgnoreCase(String latinName);
}
