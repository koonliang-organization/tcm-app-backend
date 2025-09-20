package com.tcm.backend.repository;

import com.tcm.backend.domain.HerbMeridian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerbMeridianRepository extends JpaRepository<HerbMeridian, Integer> {
    
    List<HerbMeridian> findByHerbId(Integer herbId);
    
    @Query("SELECT hm FROM HerbMeridian hm WHERE hm.herb.id = :herbId AND hm.value = :value")
    HerbMeridian findByHerbIdAndValue(@Param("herbId") Integer herbId, @Param("value") String value);
    
    void deleteByHerbId(Integer herbId);
}