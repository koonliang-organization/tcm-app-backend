package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record HerbDto(
        UUID id,
        @NotBlank @Size(max = 255) String latinName,
        @NotBlank @Size(max = 255) String pinyinName,
        @NotBlank @Size(max = 255) String chineseNameSimplified,
        @NotBlank @Size(max = 255) String chineseNameTraditional,
        @Size(max = 2000) String properties,
        @Size(max = 2000) String indications,
        @Size(max = 2000) String precautions
) {
    public HerbDto {
        // no extra validation beyond annotations for now
    }
}
