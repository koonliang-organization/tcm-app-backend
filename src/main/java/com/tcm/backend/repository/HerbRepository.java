package com.tcm.backend.repository;

import com.tcm.backend.domain.Herb;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HerbRepository extends JpaRepository<Herb, Integer> {

    @EntityGraph(attributePaths = {"flavors", "formulas", "images", "indications", "meridians"})
    Optional<Herb> findById(Integer id);

    @EntityGraph(attributePaths = {"flavors", "formulas", "images", "indications", "meridians"})
    Optional<Herb> findBySourceUrl(String sourceUrl);

    @EntityGraph(attributePaths = {"flavors", "formulas", "images", "indications", "meridians"})
    Optional<Herb> findByNameZhIgnoreCase(String nameZh);

    @EntityGraph(attributePaths = {"flavors", "formulas", "images", "indications", "meridians"})
    Optional<Herb> findByNamePinyinIgnoreCase(String namePinyin);

    @Query("SELECT DISTINCT h FROM Herb h WHERE h.nameZh LIKE %:searchTerm% OR h.namePinyin LIKE %:searchTerm%")
    java.util.List<Herb> findByNameContaining(@Param("searchTerm") String searchTerm);
}
