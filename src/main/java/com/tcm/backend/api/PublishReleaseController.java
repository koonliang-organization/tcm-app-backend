package com.tcm.backend.api;

import com.tcm.backend.dto.ApiResponse;
import com.tcm.backend.dto.PublishReleaseDto;
import com.tcm.backend.service.DatasetPublisherService;
import com.tcm.backend.service.PublishReleaseService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/publish/releases")
public class PublishReleaseController {

    @Autowired
    private PublishReleaseService publishReleaseService;

    @Autowired
    private DatasetPublisherService datasetPublisherService;

    @PostMapping
    public ResponseEntity<ApiResponse<PublishReleaseDto>> createDraft(@RequestBody Map<String, String> payload) {
        String versionName = payload.get("versionName");
        if (versionName == null || versionName.isBlank()) {
            throw new IllegalArgumentException("versionName is required");
        }
        PublishReleaseDto draft = publishReleaseService.createDraft(versionName);
        return ResponseEntity.ok(ApiResponse.success("Draft created", draft));
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<PublishReleaseDto>> markReady(@PathVariable UUID id) {
        PublishReleaseDto ready = publishReleaseService.markReadyForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Release marked ready", ready));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PublishReleaseDto>> approve(@PathVariable UUID id,
                                                                   @RequestBody Map<String, String> payload) {
        String approverId = payload.get("approverId");
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("approverId is required");
        }
        PublishReleaseDto approved = publishReleaseService.approveRelease(id, UUID.fromString(approverId));
        return ResponseEntity.ok(ApiResponse.success("Release approved", approved));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable UUID id) {
        datasetPublisherService.publishRelease(id);
        return ResponseEntity.ok(ApiResponse.success("Release published", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PublishReleaseDto>>> list(Pageable pageable) {
        Page<PublishReleaseDto> releases = publishReleaseService.listReleases(pageable);
        return ResponseEntity.ok(ApiResponse.success("Releases retrieved", releases));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<PublishReleaseDto>> latestApproved() {
        PublishReleaseDto release = publishReleaseService.getLatestApproved();
        return ResponseEntity.ok(ApiResponse.success("Latest release retrieved", release));
    }
}
