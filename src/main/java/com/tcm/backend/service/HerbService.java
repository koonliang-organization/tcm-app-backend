package com.tcm.backend.service;

import com.tcm.backend.dto.HerbDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HerbService {

    Page<HerbDto> listHerbs(Pageable pageable);

    HerbDto createHerb(HerbDto herbDto);

    HerbDto updateHerb(UUID herbId, HerbDto herbDto);

    void deleteHerb(UUID herbId);

    HerbDto getHerb(UUID herbId);
}
