package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HerbIndicationExportDto(
        @JsonProperty("value")
        String value
) {
}