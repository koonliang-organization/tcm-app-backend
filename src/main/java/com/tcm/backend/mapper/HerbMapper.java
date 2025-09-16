package com.tcm.backend.mapper;

import com.tcm.backend.domain.Herb;
import com.tcm.backend.dto.HerbDto;
import org.springframework.stereotype.Component;

@Component
public class HerbMapper {

    public HerbDto toDto(Herb herb) {
        return new HerbDto(
                herb.getId(),
                herb.getLatinName(),
                herb.getPinyinName(),
                herb.getChineseNameSimplified(),
                herb.getChineseNameTraditional(),
                herb.getProperties(),
                herb.getIndications(),
                herb.getPrecautions()
        );
    }

    public void updateEntityFromDto(HerbDto dto, Herb herb) {
        herb.setLatinName(dto.latinName());
        herb.setPinyinName(dto.pinyinName());
        herb.setChineseNameSimplified(dto.chineseNameSimplified());
        herb.setChineseNameTraditional(dto.chineseNameTraditional());
        herb.setProperties(dto.properties());
        herb.setIndications(dto.indications());
        herb.setPrecautions(dto.precautions());
    }

    public Herb toEntity(HerbDto dto) {
        Herb herb = new Herb();
        updateEntityFromDto(dto, herb);
        return herb;
    }
}
