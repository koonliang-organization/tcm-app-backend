package com.tcm.backend.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.backend.domain.Herb;
import com.tcm.backend.repository.HerbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class JsonDatasetExportService implements DatasetExportService {

    private final HerbRepository herbRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ExportResult exportDataset() {
        List<Herb> herbs = herbRepository.findAll();
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("herbs.json"));
            objectMapper.writeValue(zipOutputStream, herbs);
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            byte[] bytes = byteStream.toByteArray();
            InputStream datasetStream = new ByteArrayInputStream(bytes);
            return new ExportResult(datasetStream, bytes.length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export dataset", e);
        }
    }
}
