package com.tcm.backend.api;

import com.tcm.backend.dto.ApiResponse;
import com.tcm.backend.dto.HerbDto;
import com.tcm.backend.service.HerbService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/herbs")
public class HerbController {

    @Autowired
    private HerbService herbService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HerbDto>>> listHerbs(Pageable pageable) {
        Page<HerbDto> herbs = herbService.listHerbs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Herbs retrieved", herbs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HerbDto>> getHerb(@PathVariable UUID id) {
        HerbDto herb = herbService.getHerb(id);
        return ResponseEntity.ok(ApiResponse.success("Herb retrieved", herb));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HerbDto>> createHerb(@Valid @RequestBody HerbDto herbDto) {
        HerbDto created = herbService.createHerb(herbDto);
        return ResponseEntity.ok(ApiResponse.success("Herb created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HerbDto>> updateHerb(@PathVariable UUID id,
                                                           @Valid @RequestBody HerbDto herbDto) {
        HerbDto updated = herbService.updateHerb(id, herbDto);
        return ResponseEntity.ok(ApiResponse.success("Herb updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHerb(@PathVariable UUID id) {
        herbService.deleteHerb(id);
        return ResponseEntity.ok(ApiResponse.success("Herb deleted", null));
    }
}
