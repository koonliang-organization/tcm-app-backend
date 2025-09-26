package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HerbFormulaExportDto(
        @JsonProperty("value")
        String value
) {
}