package com.tcm.backend.mapper;

import com.tcm.backend.domain.*;
import com.tcm.backend.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HerbMapper {

    public HerbDto toDto(Herb herb) {
        if (herb == null) {
            return null;
        }

        return new HerbDto(
                herb.getId(),
                herb.getSourceUrl(),
                herb.getNameZh(),
                herb.getNamePinyin(),
                herb.getDescZh(),
                herb.getDescEn(),
                herb.getAppearance(),
                herb.getProperty(),
                mapFlavorsToDto(herb.getFlavors()),
                mapFormulasToDto(herb.getFormulas()),
                mapImagesToDto(herb.getImages()),
                mapIndicationsToDto(herb.getIndications()),
                mapMeridiansToDto(herb.getMeridians())
        );
    }

    public void updateEntityFromDto(HerbDto dto, Herb herb) {
        herb.setSourceUrl(dto.sourceUrl());
        herb.setNameZh(dto.nameZh());
        herb.setNamePinyin(dto.namePinyin());
        herb.setDescZh(dto.descZh());
        herb.setDescEn(dto.descEn());
        herb.setAppearance(dto.appearance());
        herb.setProperty(dto.property());

        // Handle related entities - clear existing and add new ones
        updateFlavors(dto.flavors(), herb);
        updateFormulas(dto.formulas(), herb);
        updateImages(dto.images(), herb);
        updateIndications(dto.indications(), herb);
        updateMeridians(dto.meridians(), herb);
    }

    public Herb toEntity(HerbDto dto) {
        if (dto == null) {
            return null;
        }

        Herb herb = new Herb();
        herb.setSourceUrl(dto.sourceUrl());
        herb.setNameZh(dto.nameZh());
        herb.setNamePinyin(dto.namePinyin());
        herb.setDescZh(dto.descZh());
        herb.setDescEn(dto.descEn());
        herb.setAppearance(dto.appearance());
        herb.setProperty(dto.property());

        // Initialize collections
        herb.setFlavors(new ArrayList<>());
        herb.setFormulas(new ArrayList<>());
        herb.setImages(new ArrayList<>());
        herb.setIndications(new ArrayList<>());
        herb.setMeridians(new ArrayList<>());

        return herb;
    }

    private List<HerbFlavorDto> mapFlavorsToDto(List<HerbFlavor> flavors) {
        if (flavors == null) {
            return new ArrayList<>();
        }
        return flavors.stream()
                .map(flavor -> new HerbFlavorDto(flavor.getId(), flavor.getValue()))
                .collect(Collectors.toList());
    }

    private List<HerbFormulaDto> mapFormulasToDto(List<HerbFormula> formulas) {
        if (formulas == null) {
            return new ArrayList<>();
        }
        return formulas.stream()
                .map(formula -> new HerbFormulaDto(formula.getId(), formula.getValue()))
                .collect(Collectors.toList());
    }

    private List<HerbImageDto> mapImagesToDto(List<HerbImage> images) {
        if (images == null) {
            return new ArrayList<>();
        }
        return images.stream()
                .map(image -> new HerbImageDto(image.getId(), image.getFilename(), image.getMime(), image.getData()))
                .collect(Collectors.toList());
    }

    private List<HerbIndicationDto> mapIndicationsToDto(List<HerbIndication> indications) {
        if (indications == null) {
            return new ArrayList<>();
        }
        return indications.stream()
                .map(indication -> new HerbIndicationDto(indication.getId(), indication.getValue()))
                .collect(Collectors.toList());
    }

    private List<HerbMeridianDto> mapMeridiansToDto(List<HerbMeridian> meridians) {
        if (meridians == null) {
            return new ArrayList<>();
        }
        return meridians.stream()
                .map(meridian -> new HerbMeridianDto(meridian.getId(), meridian.getValue()))
                .collect(Collectors.toList());
    }

    private void updateFlavors(List<HerbFlavorDto> flavorDtos, Herb herb) {
        if (herb.getFlavors() == null) {
            herb.setFlavors(new ArrayList<>());
        }
        
        herb.getFlavors().clear();
        
        if (flavorDtos != null) {
            for (HerbFlavorDto dto : flavorDtos) {
                HerbFlavor flavor = new HerbFlavor();
                flavor.setHerb(herb);
                flavor.setValue(dto.value());
                herb.getFlavors().add(flavor);
            }
        }
    }

    private void updateFormulas(List<HerbFormulaDto> formulaDtos, Herb herb) {
        if (herb.getFormulas() == null) {
            herb.setFormulas(new ArrayList<>());
        }
        
        herb.getFormulas().clear();
        
        if (formulaDtos != null) {
            for (HerbFormulaDto dto : formulaDtos) {
                HerbFormula formula = new HerbFormula();
                formula.setHerb(herb);
                formula.setValue(dto.value());
                herb.getFormulas().add(formula);
            }
        }
    }

    private void updateImages(List<HerbImageDto> imageDtos, Herb herb) {
        if (herb.getImages() == null) {
            herb.setImages(new ArrayList<>());
        }
        
        herb.getImages().clear();
        
        if (imageDtos != null) {
            for (HerbImageDto dto : imageDtos) {
                HerbImage image = new HerbImage();
                image.setHerb(herb);
                image.setFilename(dto.filename());
                image.setMime(dto.mime());
                image.setData(dto.data());
                herb.getImages().add(image);
            }
        }
    }

    private void updateIndications(List<HerbIndicationDto> indicationDtos, Herb herb) {
        if (herb.getIndications() == null) {
            herb.setIndications(new ArrayList<>());
        }
        
        herb.getIndications().clear();
        
        if (indicationDtos != null) {
            for (HerbIndicationDto dto : indicationDtos) {
                HerbIndication indication = new HerbIndication();
                indication.setHerb(herb);
                indication.setValue(dto.value());
                herb.getIndications().add(indication);
            }
        }
    }

    private void updateMeridians(List<HerbMeridianDto> meridianDtos, Herb herb) {
        if (herb.getMeridians() == null) {
            herb.setMeridians(new ArrayList<>());
        }
        
        herb.getMeridians().clear();
        
        if (meridianDtos != null) {
            for (HerbMeridianDto dto : meridianDtos) {
                HerbMeridian meridian = new HerbMeridian();
                meridian.setHerb(herb);
                meridian.setValue(dto.value());
                herb.getMeridians().add(meridian);
            }
        }
    }
}
