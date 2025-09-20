package com.tcm.backend.service;

import com.tcm.backend.domain.Herb;
import com.tcm.backend.dto.*;
import com.tcm.backend.mapper.HerbMapper;
import com.tcm.backend.repository.*;
import com.tcm.backend.service.impl.HerbServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HerbServiceImplTest {

    @Mock
    private HerbRepository herbRepository;

    @Mock
    private HerbFlavorRepository herbFlavorRepository;

    @Mock
    private HerbFormulaRepository herbFormulaRepository;

    @Mock
    private HerbImageRepository herbImageRepository;

    @Mock
    private HerbIndicationRepository herbIndicationRepository;

    @Mock
    private HerbMeridianRepository herbMeridianRepository;

    @Mock
    private HerbMapper herbMapper;

    @InjectMocks
    private HerbServiceImpl herbService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createHerbPersistsEntity() {
        HerbDto request = new HerbDto(null, "https://example.com/herb1", "白术", "bai zhu", 
                "白术描述", "Description", "Appearance", "property",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 
                new ArrayList<>(), new ArrayList<>());
        Herb entity = new Herb();
        Herb saved = new Herb();
        saved.setId(1);

        when(herbRepository.findBySourceUrl("https://example.com/herb1")).thenReturn(Optional.empty());
        when(herbMapper.toEntity(request)).thenReturn(entity);
        when(herbRepository.save(entity)).thenReturn(saved);
        when(herbMapper.toDto(saved)).thenReturn(new HerbDto(saved.getId(), "https://example.com/herb1", 
                "白术", "bai zhu", "白术描述", "Description", "Appearance", "property",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 
                new ArrayList<>(), new ArrayList<>()));

        HerbDto result = herbService.createHerb(request);

        assertThat(result.id()).isEqualTo(saved.getId());
        verify(herbRepository).save(entity);
    }

    @Test
    void listHerbsReturnsMappedDtos() {
        Herb herb = new Herb();
        herb.setId(1);
        Page<Herb> herbPage = new PageImpl<>(List.of(herb));
        when(herbRepository.findAll(PageRequest.of(0, 10))).thenReturn(herbPage);
        HerbDto dto = new HerbDto(herb.getId(), "https://example.com/herb1", "白术", "bai zhu", 
                "白术描述", "Description", "Appearance", "property",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 
                new ArrayList<>(), new ArrayList<>());
        when(herbMapper.toDto(herb)).thenReturn(dto);

        Page<HerbDto> result = herbService.listHerbs(PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void deleteHerbThrowsWhenNotFound() {
        Integer herbId = 1;
        when(herbRepository.findById(herbId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> herbService.deleteHerb(herbId));
    }
}
