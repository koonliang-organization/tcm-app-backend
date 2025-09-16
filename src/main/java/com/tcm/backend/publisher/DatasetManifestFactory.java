package com.tcm.backend.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.domain.PublishRelease;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatasetManifestFactory {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public String createManifest(PublishRelease release, long sizeBytes, String checksum, String url, String minAppVersion) {
        Instant createdAt = Instant.now(clock);
        Map<String, Object> manifest = Map.of(
                "version", release.getVersionName(),
                "created_at", createdAt.toString(),
                "size_bytes", sizeBytes,
                "checksum_sha256", checksum,
                "url", url,
                "min_app_version", minAppVersion
        );
        try {
            return objectMapper.writeValueAsString(manifest);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise manifest", e);
        }
    }
}
