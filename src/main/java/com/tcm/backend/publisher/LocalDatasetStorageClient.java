package com.tcm.backend.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalDatasetStorageClient implements DatasetStorageClient {

    @Value("${publisher.storage.local-directory:build/datasets}")
    private String storageDirectory;

    @Override
    public StorageResult storeDataset(String objectKey, InputStream datasetStream, long contentLength) {
        try {
            Path targetDir = Path.of(storageDirectory);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(objectKey);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInputStream = new DigestInputStream(datasetStream, digest);
                 OutputStream outputStream = Files.newOutputStream(targetPath)) {
                digestInputStream.transferTo(outputStream);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            log.info("Stored dataset {} ({} bytes) in {}", objectKey, contentLength, targetPath);
            return new StorageResult(targetPath.toAbsolutePath().toString(), checksum);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to store dataset", e);
        }
    }
}
