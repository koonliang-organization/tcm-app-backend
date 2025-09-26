package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HerbImageExportDto(
        @JsonProperty("filename")
        String filename,

        @JsonProperty("mime")
        String mime,

        @JsonProperty("data")
        byte[] data
) {
}