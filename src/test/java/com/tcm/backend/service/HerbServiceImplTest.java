package com.tcm.backend.service;

import com.tcm.backend.domain.Herb;
import com.tcm.backend.dto.HerbDto;
import com.tcm.backend.mapper.HerbMapper;
import com.tcm.backend.repository.HerbRepository;
import com.tcm.backend.service.impl.HerbServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HerbServiceImplTest {

    @Mock
    private HerbRepository herbRepository;

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
        HerbDto request = new HerbDto(null, "Radix", "bai zhu", "白术", "白術", null, null, null);
        Herb entity = new Herb();
        Herb saved = new Herb();
        saved.setId(UUID.randomUUID());

        when(herbRepository.findByLatinNameIgnoreCase("Radix")).thenReturn(Optional.empty());
        when(herbMapper.toEntity(request)).thenReturn(entity);
        when(herbRepository.save(entity)).thenReturn(saved);
        when(herbMapper.toDto(saved)).thenReturn(new HerbDto(saved.getId(), "Radix", "bai zhu", "白术", "白術", null, null, null));

        HerbDto result = herbService.createHerb(request);

        assertThat(result.id()).isEqualTo(saved.getId());
        verify(herbRepository).save(entity);
    }

    @Test
    void listHerbsReturnsMappedDtos() {
        Herb herb = new Herb();
        herb.setId(UUID.randomUUID());
        Page<Herb> herbPage = new PageImpl<>(List.of(herb));
        when(herbRepository.findAll(PageRequest.of(0, 10))).thenReturn(herbPage);
        HerbDto dto = new HerbDto(herb.getId(), "Radix", "bai zhu", "白术", "白術", null, null, null);
        when(herbMapper.toDto(herb)).thenReturn(dto);

        Page<HerbDto> result = herbService.listHerbs(PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void deleteHerbThrowsWhenNotFound() {
        UUID herbId = UUID.randomUUID();
        when(herbRepository.findById(herbId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> herbService.deleteHerb(herbId));
    }
}
