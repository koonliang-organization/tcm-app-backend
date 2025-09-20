package com.tcm.backend.service;

import com.tcm.backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HerbService {

    Page<HerbDto> listHerbs(Pageable pageable);

    HerbDto createHerb(HerbDto herbDto);

    HerbDto updateHerb(Integer herbId, HerbDto herbDto);

    void deleteHerb(Integer herbId);

    HerbDto getHerb(Integer herbId);

    HerbDto getHerbBySourceUrl(String sourceUrl);

    List<HerbDto> searchHerbsByName(String searchTerm);

    // Methods for managing herb attributes
    HerbFlavorDto addFlavorToHerb(Integer herbId, HerbFlavorDto flavorDto);
    
    HerbFormulaDto addFormulaToHerb(Integer herbId, HerbFormulaDto formulaDto);
    
    HerbImageDto addImageToHerb(Integer herbId, HerbImageDto imageDto);
    
    HerbIndicationDto addIndicationToHerb(Integer herbId, HerbIndicationDto indicationDto);
    
    HerbMeridianDto addMeridianToHerb(Integer herbId, HerbMeridianDto meridianDto);

    void removeFlavorFromHerb(Integer herbId, Integer flavorId);
    
    void removeFormulaFromHerb(Integer herbId, Integer formulaId);
    
    void removeImageFromHerb(Integer herbId, Integer imageId);
    
    void removeIndicationFromHerb(Integer herbId, Integer indicationId);
    
    void removeMeridianFromHerb(Integer herbId, Integer meridianId);
}
