package com.tcm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HerbImageDto(
        Integer id,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 64) String mime,
        byte[] data
) {
    public HerbImageDto {
        // Validation handled by annotations
    }
}