package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HerbIndicationDto(
        Integer id,
        @NotBlank @Size(max = 255) String value
) {
    public HerbIndicationDto {
        // Validation handled by annotations
    }
}