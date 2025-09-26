package com.tcm.backend.publisher.mapper;

import com.tcm.backend.domain.*;
import com.tcm.backend.publisher.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HerbExportMapper {

    public HerbExportDto toExportDto(Herb herb) {
        if (herb == null) {
            return null;
        }

        return new HerbExportDto(
                herb.getId(),
                herb.getSourceUrl(),
                herb.getNameZh(),
                herb.getNamePinyin(),
                herb.getDescZh(),
                herb.getDescEn(),
                herb.getAppearance(),
                herb.getProperty(),
                toFlavorExportDtos(herb.getFlavors()),
                toFormulaExportDtos(herb.getFormulas()),
                toImageExportDtos(herb.getImages()),
                toIndicationExportDtos(herb.getIndications()),
                toMeridianExportDtos(herb.getMeridians())
        );
    }

    public List<HerbExportDto> toExportDtos(List<Herb> herbs) {
        if (herbs == null) {
            return null;
        }
        return herbs.stream()
                .map(this::toExportDto)
                .toList();
    }

    private List<HerbFlavorExportDto> toFlavorExportDtos(List<HerbFlavor> flavors) {
        if (flavors == null) {
            return null;
        }
        return flavors.stream()
                .map(flavor -> new HerbFlavorExportDto(flavor.getValue()))
                .toList();
    }

    private List<HerbFormulaExportDto> toFormulaExportDtos(List<HerbFormula> formulas) {
        if (formulas == null) {
            return null;
        }
        return formulas.stream()
                .map(formula -> new HerbFormulaExportDto(formula.getValue()))
                .toList();
    }

    private List<HerbImageExportDto> toImageExportDtos(List<HerbImage> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(image -> new HerbImageExportDto(
                        image.getFilename(),
                        image.getMime(),
                        image.getData()
                ))
                .toList();
    }

    private List<HerbIndicationExportDto> toIndicationExportDtos(List<HerbIndication> indications) {
        if (indications == null) {
            return null;
        }
        return indications.stream()
                .map(indication -> new HerbIndicationExportDto(indication.getValue()))
                .toList();
    }

    private List<HerbMeridianExportDto> toMeridianExportDtos(List<HerbMeridian> meridians) {
        if (meridians == null) {
            return null;
        }
        return meridians.stream()
                .map(meridian -> new HerbMeridianExportDto(meridian.getValue()))
                .toList();
    }
}