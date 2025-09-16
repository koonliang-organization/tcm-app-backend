package com.tcm.backend.service.impl;

import com.tcm.backend.domain.Herb;
import com.tcm.backend.dto.HerbDto;
import com.tcm.backend.mapper.HerbMapper;
import com.tcm.backend.repository.HerbRepository;
import com.tcm.backend.service.HerbService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HerbServiceImpl implements HerbService {

    @Autowired
    private HerbRepository herbRepository;

    @Autowired
    private HerbMapper herbMapper;

    @Override
    @Transactional
    public Page<HerbDto> listHerbs(Pageable pageable) {
        return herbRepository.findAll(pageable).map(herbMapper::toDto);
    }

    @Override
    @Transactional
    public HerbDto createHerb(HerbDto herbDto) {
        herbRepository.findByLatinNameIgnoreCase(herbDto.latinName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Herb with latin name already exists");
                });
        Herb herb = herbMapper.toEntity(herbDto);
        Herb saved = herbRepository.save(herb);
        return herbMapper.toDto(saved);
    }

    @Override
    @Transactional
    public HerbDto updateHerb(UUID herbId, HerbDto herbDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        herbMapper.updateEntityFromDto(herbDto, herb);
        Herb saved = herbRepository.save(herb);
        return herbMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteHerb(UUID herbId) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        herbRepository.delete(herb);
    }

    @Override
    @Transactional
    public HerbDto getHerb(UUID herbId) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        return herbMapper.toDto(herb);
    }
}
