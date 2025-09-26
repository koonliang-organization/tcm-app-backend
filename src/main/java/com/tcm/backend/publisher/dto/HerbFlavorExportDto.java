package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HerbFlavorExportDto(
        @JsonProperty("value")
        String value
) {
}