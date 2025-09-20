package com.tcm.backend.repository;

import com.tcm.backend.domain.HerbFlavor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerbFlavorRepository extends JpaRepository<HerbFlavor, Integer> {
    
    List<HerbFlavor> findByHerbId(Integer herbId);
    
    @Query("SELECT hf FROM HerbFlavor hf WHERE hf.herb.id = :herbId AND hf.value = :value")
    HerbFlavor findByHerbIdAndValue(@Param("herbId") Integer herbId, @Param("value") String value);
    
    void deleteByHerbId(Integer herbId);
}