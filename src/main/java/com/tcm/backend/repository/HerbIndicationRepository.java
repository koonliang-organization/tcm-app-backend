package com.tcm.backend.repository;

import com.tcm.backend.domain.HerbIndication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerbIndicationRepository extends JpaRepository<HerbIndication, Integer> {
    
    List<HerbIndication> findByHerbId(Integer herbId);
    
    @Query("SELECT hi FROM HerbIndication hi WHERE hi.herb.id = :herbId AND hi.value = :value")
    HerbIndication findByHerbIdAndValue(@Param("herbId") Integer herbId, @Param("value") String value);
    
    void deleteByHerbId(Integer herbId);
}