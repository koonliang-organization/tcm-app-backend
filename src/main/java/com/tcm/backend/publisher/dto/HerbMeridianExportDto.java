package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HerbMeridianExportDto(
        @JsonProperty("value")
        String value
) {
}