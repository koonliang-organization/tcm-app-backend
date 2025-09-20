package com.tcm.backend.api;

import com.tcm.backend.dto.*;
import com.tcm.backend.service.HerbService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/herbs")
public class HerbController {

    @Autowired
    private HerbService herbService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HerbDto>>> listHerbs(Pageable pageable) {
        try {
            Page<HerbDto> herbs = herbService.listHerbs(pageable);
            return ResponseEntity.ok(ApiResponse.success("Herbs retrieved", herbs));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid sort parameter: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve herbs: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HerbDto>> getHerb(@PathVariable Integer id) {
        try {
            HerbDto herb = herbService.getHerb(id);
            return ResponseEntity.ok(ApiResponse.success("Herb retrieved", herb));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/by-source-url")
    public ResponseEntity<ApiResponse<HerbDto>> getHerbBySourceUrl(@RequestParam String sourceUrl) {
        try {
            HerbDto herb = herbService.getHerbBySourceUrl(sourceUrl);
            return ResponseEntity.ok(ApiResponse.success("Herb retrieved", herb));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<HerbDto>>> searchHerbs(@RequestParam String searchTerm) {
        try {
            List<HerbDto> herbs = herbService.searchHerbsByName(searchTerm);
            return ResponseEntity.ok(ApiResponse.success("Search results retrieved", herbs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Search failed"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HerbDto>> createHerb(@Valid @RequestBody HerbDto herbDto) {
        try {
            HerbDto created = herbService.createHerb(herbDto);
            return ResponseEntity.ok(ApiResponse.success("Herb created", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HerbDto>> updateHerb(@PathVariable Integer id,
                                                           @Valid @RequestBody HerbDto herbDto) {
        try {
            HerbDto updated = herbService.updateHerb(id, herbDto);
            return ResponseEntity.ok(ApiResponse.success("Herb updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHerb(@PathVariable Integer id) {
        try {
            herbService.deleteHerb(id);
            return ResponseEntity.ok(ApiResponse.success("Herb deleted", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Endpoints for managing herb attributes
    @PostMapping("/{herbId}/flavors")
    public ResponseEntity<ApiResponse<HerbFlavorDto>> addFlavorToHerb(@PathVariable Integer herbId,
                                                                      @Valid @RequestBody HerbFlavorDto flavorDto) {
        try {
            HerbFlavorDto created = herbService.addFlavorToHerb(herbId, flavorDto);
            return ResponseEntity.ok(ApiResponse.success("Flavor added to herb", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{herbId}/flavors/{flavorId}")
    public ResponseEntity<ApiResponse<Void>> removeFlavorFromHerb(@PathVariable Integer herbId,
                                                                  @PathVariable Integer flavorId) {
        try {
            herbService.removeFlavorFromHerb(herbId, flavorId);
            return ResponseEntity.ok(ApiResponse.success("Flavor removed from herb", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{herbId}/formulas")
    public ResponseEntity<ApiResponse<HerbFormulaDto>> addFormulaToHerb(@PathVariable Integer herbId,
                                                                        @Valid @RequestBody HerbFormulaDto formulaDto) {
        try {
            HerbFormulaDto created = herbService.addFormulaToHerb(herbId, formulaDto);
            return ResponseEntity.ok(ApiResponse.success("Formula added to herb", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{herbId}/formulas/{formulaId}")
    public ResponseEntity<ApiResponse<Void>> removeFormulaFromHerb(@PathVariable Integer herbId,
                                                                   @PathVariable Integer formulaId) {
        try {
            herbService.removeFormulaFromHerb(herbId, formulaId);
            return ResponseEntity.ok(ApiResponse.success("Formula removed from herb", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{herbId}/images")
    public ResponseEntity<ApiResponse<HerbImageDto>> addImageToHerb(@PathVariable Integer herbId,
                                                                    @Valid @RequestBody HerbImageDto imageDto) {
        try {
            HerbImageDto created = herbService.addImageToHerb(herbId, imageDto);
            return ResponseEntity.ok(ApiResponse.success("Image added to herb", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{herbId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> removeImageFromHerb(@PathVariable Integer herbId,
                                                                 @PathVariable Integer imageId) {
        try {
            herbService.removeImageFromHerb(herbId, imageId);
            return ResponseEntity.ok(ApiResponse.success("Image removed from herb", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{herbId}/indications")
    public ResponseEntity<ApiResponse<HerbIndicationDto>> addIndicationToHerb(@PathVariable Integer herbId,
                                                                              @Valid @RequestBody HerbIndicationDto indicationDto) {
        try {
            HerbIndicationDto created = herbService.addIndicationToHerb(herbId, indicationDto);
            return ResponseEntity.ok(ApiResponse.success("Indication added to herb", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{herbId}/indications/{indicationId}")
    public ResponseEntity<ApiResponse<Void>> removeIndicationFromHerb(@PathVariable Integer herbId,
                                                                      @PathVariable Integer indicationId) {
        try {
            herbService.removeIndicationFromHerb(herbId, indicationId);
            return ResponseEntity.ok(ApiResponse.success("Indication removed from herb", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{herbId}/meridians")
    public ResponseEntity<ApiResponse<HerbMeridianDto>> addMeridianToHerb(@PathVariable Integer herbId,
                                                                          @Valid @RequestBody HerbMeridianDto meridianDto) {
        try {
            HerbMeridianDto created = herbService.addMeridianToHerb(herbId, meridianDto);
            return ResponseEntity.ok(ApiResponse.success("Meridian added to herb", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{herbId}/meridians/{meridianId}")
    public ResponseEntity<ApiResponse<Void>> removeMeridianFromHerb(@PathVariable Integer herbId,
                                                                    @PathVariable Integer meridianId) {
        try {
            herbService.removeMeridianFromHerb(herbId, meridianId);
            return ResponseEntity.ok(ApiResponse.success("Meridian removed from herb", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
