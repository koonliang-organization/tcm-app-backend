package com.tcm.backend.repository;

import com.tcm.backend.domain.HerbImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerbImageRepository extends JpaRepository<HerbImage, Integer> {
    
    List<HerbImage> findByHerbId(Integer herbId);
    
    @Query("SELECT hi FROM HerbImage hi WHERE hi.herb.id = :herbId AND hi.filename = :filename")
    HerbImage findByHerbIdAndFilename(@Param("herbId") Integer herbId, @Param("filename") String filename);
    
    void deleteByHerbId(Integer herbId);
}