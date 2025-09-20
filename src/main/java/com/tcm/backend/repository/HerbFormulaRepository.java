package com.tcm.backend.repository;

import com.tcm.backend.domain.HerbFormula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerbFormulaRepository extends JpaRepository<HerbFormula, Integer> {
    
    List<HerbFormula> findByHerbId(Integer herbId);
    
    @Query("SELECT hf FROM HerbFormula hf WHERE hf.herb.id = :herbId AND hf.value = :value")
    HerbFormula findByHerbIdAndValue(@Param("herbId") Integer herbId, @Param("value") String value);
    
    void deleteByHerbId(Integer herbId);
}