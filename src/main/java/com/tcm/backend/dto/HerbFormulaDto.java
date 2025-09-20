package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HerbFormulaDto(
        Integer id,
        @NotBlank @Size(max = 255) String value
) {
    public HerbFormulaDto {
        // Validation handled by annotations
    }
}