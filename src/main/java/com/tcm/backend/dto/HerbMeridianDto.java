package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HerbMeridianDto(
        Integer id,
        @NotBlank @Size(max = 128) String value
) {
    public HerbMeridianDto {
        // Validation handled by annotations
    }
}