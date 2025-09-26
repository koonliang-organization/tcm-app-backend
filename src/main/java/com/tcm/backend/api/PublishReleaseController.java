package com.tcm.backend.api;

import com.tcm.backend.dto.ApiResponse;
import com.tcm.backend.dto.PublishReleaseDto;
import com.tcm.backend.service.DatasetPublisherService;
import com.tcm.backend.service.PublishReleaseService;
import com.tcm.backend.domain.AdminUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public ResponseEntity<ApiResponse<PublishReleaseDto>> markReady(@PathVariable String id) {
        PublishReleaseDto ready = publishReleaseService.markReadyForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Release marked ready", ready));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PUBLISH_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PublishReleaseDto>> approve(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AdminUser)) {
            throw new IllegalArgumentException("User not authenticated");
        }

        AdminUser currentUser = (AdminUser) authentication.getPrincipal();
        String approverId = currentUser.getId();

        PublishReleaseDto approved = publishReleaseService.approveRelease(id, approverId);
        return ResponseEntity.ok(ApiResponse.success("Release approved", approved));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PUBLISH_EXECUTE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable String id) {
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
