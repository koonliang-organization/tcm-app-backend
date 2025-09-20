package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HerbDto(
        Integer id,
        @NotBlank @Size(max = 512) String sourceUrl,
        @Size(max = 255) String nameZh,
        @Size(max = 255) String namePinyin,
        String descZh,
        String descEn,
        String appearance,
        @Size(max = 64) String property,
        List<HerbFlavorDto> flavors,
        List<HerbFormulaDto> formulas,
        List<HerbImageDto> images,
        List<HerbIndicationDto> indications,
        List<HerbMeridianDto> meridians
) {
    public HerbDto {
        // Validation handled by annotations
    }
}
