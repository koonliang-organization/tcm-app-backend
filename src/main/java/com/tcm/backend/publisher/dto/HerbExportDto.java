package com.tcm.backend.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HerbExportDto(
        @JsonProperty("id")
        Integer id,

        @JsonProperty("source_url")
        String sourceUrl,

        @JsonProperty("name_zh")
        String nameZh,

        @JsonProperty("name_pinyin")
        String namePinyin,

        @JsonProperty("desc_zh")
        String descZh,

        @JsonProperty("desc_en")
        String descEn,

        @JsonProperty("appearance")
        String appearance,

        @JsonProperty("property")
        String property,

        @JsonProperty("flavors")
        List<HerbFlavorExportDto> flavors,

        @JsonProperty("formulas")
        List<HerbFormulaExportDto> formulas,

        @JsonProperty("images")
        List<HerbImageExportDto> images,

        @JsonProperty("indications")
        List<HerbIndicationExportDto> indications,

        @JsonProperty("meridians")
        List<HerbMeridianExportDto> meridians
) {
}