package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HerbFlavorDto(
        Integer id,
        @NotBlank @Size(max = 128) String value
) {
    public HerbFlavorDto {
        // Validation handled by annotations
    }
}