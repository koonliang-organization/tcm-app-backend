package com.tcm.backend.service.impl;

import com.tcm.backend.domain.*;
import com.tcm.backend.dto.*;
import com.tcm.backend.mapper.HerbMapper;
import com.tcm.backend.repository.*;
import com.tcm.backend.service.HerbService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HerbServiceImpl implements HerbService {

    private static final Logger logger = LoggerFactory.getLogger(HerbServiceImpl.class);

    @Autowired
    private HerbRepository herbRepository;

    @Autowired
    private HerbFlavorRepository herbFlavorRepository;

    @Autowired
    private HerbFormulaRepository herbFormulaRepository;

    @Autowired
    private HerbImageRepository herbImageRepository;

    @Autowired
    private HerbIndicationRepository herbIndicationRepository;

    @Autowired
    private HerbMeridianRepository herbMeridianRepository;

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
        herbRepository.findBySourceUrl(herbDto.sourceUrl())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Herb with source URL already exists");
                });
        Herb herb = herbMapper.toEntity(herbDto);
        Herb saved = herbRepository.save(herb);
        return herbMapper.toDto(saved);
    }

    @Override
    @Transactional
    public HerbDto updateHerb(Integer herbId, HerbDto herbDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        herbMapper.updateEntityFromDto(herbDto, herb);
        Herb saved = herbRepository.save(herb);
        return herbMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteHerb(Integer herbId) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        herbRepository.delete(herb);
    }

    @Override
    @Transactional
    public HerbDto getHerb(Integer herbId) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        return herbMapper.toDto(herb);
    }

    @Override
    @Transactional
    public HerbDto getHerbBySourceUrl(String sourceUrl) {
        Herb herb = herbRepository.findBySourceUrl(sourceUrl)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        return herbMapper.toDto(herb);
    }

    @Override
    @Transactional
    public List<HerbDto> searchHerbsByName(String searchTerm) {
        logger.info("Searching herbs by name with term: '{}'", searchTerm);
        try {
            List<Herb> herbs = herbRepository.findByNameContaining(searchTerm);
            logger.info("Repository search completed, found {} herbs", herbs.size());
            List<HerbDto> result = herbs.stream().map(herbMapper::toDto).toList();
            logger.info("Mapping to DTOs completed, returning {} herbs", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error in searchHerbsByName: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public HerbFlavorDto addFlavorToHerb(Integer herbId, HerbFlavorDto flavorDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        
        HerbFlavor existingFlavor = herbFlavorRepository.findByHerbIdAndValue(herbId, flavorDto.value());
        if (existingFlavor != null) {
            throw new IllegalArgumentException("Flavor already exists for this herb");
        }
        
        HerbFlavor flavor = new HerbFlavor();
        flavor.setHerb(herb);
        flavor.setValue(flavorDto.value());
        
        HerbFlavor saved = herbFlavorRepository.save(flavor);
        return new HerbFlavorDto(saved.getId(), saved.getValue());
    }

    @Override
    @Transactional
    public HerbFormulaDto addFormulaToHerb(Integer herbId, HerbFormulaDto formulaDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        
        HerbFormula existingFormula = herbFormulaRepository.findByHerbIdAndValue(herbId, formulaDto.value());
        if (existingFormula != null) {
            throw new IllegalArgumentException("Formula already exists for this herb");
        }
        
        HerbFormula formula = new HerbFormula();
        formula.setHerb(herb);
        formula.setValue(formulaDto.value());
        
        HerbFormula saved = herbFormulaRepository.save(formula);
        return new HerbFormulaDto(saved.getId(), saved.getValue());
    }

    @Override
    @Transactional
    public HerbImageDto addImageToHerb(Integer herbId, HerbImageDto imageDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        
        HerbImage existingImage = herbImageRepository.findByHerbIdAndFilename(herbId, imageDto.filename());
        if (existingImage != null) {
            throw new IllegalArgumentException("Image with this filename already exists for this herb");
        }
        
        HerbImage image = new HerbImage();
        image.setHerb(herb);
        image.setFilename(imageDto.filename());
        image.setMime(imageDto.mime());
        image.setData(imageDto.data());
        
        HerbImage saved = herbImageRepository.save(image);
        return new HerbImageDto(saved.getId(), saved.getFilename(), saved.getMime(), saved.getData());
    }

    @Override
    @Transactional
    public HerbIndicationDto addIndicationToHerb(Integer herbId, HerbIndicationDto indicationDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        
        HerbIndication existingIndication = herbIndicationRepository.findByHerbIdAndValue(herbId, indicationDto.value());
        if (existingIndication != null) {
            throw new IllegalArgumentException("Indication already exists for this herb");
        }
        
        HerbIndication indication = new HerbIndication();
        indication.setHerb(herb);
        indication.setValue(indicationDto.value());
        
        HerbIndication saved = herbIndicationRepository.save(indication);
        return new HerbIndicationDto(saved.getId(), saved.getValue());
    }

    @Override
    @Transactional
    public HerbMeridianDto addMeridianToHerb(Integer herbId, HerbMeridianDto meridianDto) {
        Herb herb = herbRepository.findById(herbId)
                .orElseThrow(() -> new IllegalArgumentException("Herb not found"));
        
        HerbMeridian existingMeridian = herbMeridianRepository.findByHerbIdAndValue(herbId, meridianDto.value());
        if (existingMeridian != null) {
            throw new IllegalArgumentException("Meridian already exists for this herb");
        }
        
        HerbMeridian meridian = new HerbMeridian();
        meridian.setHerb(herb);
        meridian.setValue(meridianDto.value());
        
        HerbMeridian saved = herbMeridianRepository.save(meridian);
        return new HerbMeridianDto(saved.getId(), saved.getValue());
    }

    @Override
    @Transactional
    public void removeFlavorFromHerb(Integer herbId, Integer flavorId) {
        HerbFlavor flavor = herbFlavorRepository.findById(flavorId)
                .orElseThrow(() -> new IllegalArgumentException("Flavor not found"));
        
        if (!flavor.getHerb().getId().equals(herbId)) {
            throw new IllegalArgumentException("Flavor does not belong to this herb");
        }
        
        herbFlavorRepository.delete(flavor);
    }

    @Override
    @Transactional
    public void removeFormulaFromHerb(Integer herbId, Integer formulaId) {
        HerbFormula formula = herbFormulaRepository.findById(formulaId)
                .orElseThrow(() -> new IllegalArgumentException("Formula not found"));
        
        if (!formula.getHerb().getId().equals(herbId)) {
            throw new IllegalArgumentException("Formula does not belong to this herb");
        }
        
        herbFormulaRepository.delete(formula);
    }

    @Override
    @Transactional
    public void removeImageFromHerb(Integer herbId, Integer imageId) {
        HerbImage image = herbImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        
        if (!image.getHerb().getId().equals(herbId)) {
            throw new IllegalArgumentException("Image does not belong to this herb");
        }
        
        herbImageRepository.delete(image);
    }

    @Override
    @Transactional
    public void removeIndicationFromHerb(Integer herbId, Integer indicationId) {
        HerbIndication indication = herbIndicationRepository.findById(indicationId)
                .orElseThrow(() -> new IllegalArgumentException("Indication not found"));
        
        if (!indication.getHerb().getId().equals(herbId)) {
            throw new IllegalArgumentException("Indication does not belong to this herb");
        }
        
        herbIndicationRepository.delete(indication);
    }

    @Override
    @Transactional
    public void removeMeridianFromHerb(Integer herbId, Integer meridianId) {
        HerbMeridian meridian = herbMeridianRepository.findById(meridianId)
                .orElseThrow(() -> new IllegalArgumentException("Meridian not found"));
        
        if (!meridian.getHerb().getId().equals(herbId)) {
            throw new IllegalArgumentException("Meridian does not belong to this herb");
        }
        
        herbMeridianRepository.delete(meridian);
    }
}
