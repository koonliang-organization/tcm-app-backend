package com.tcm.backend.publisher;

import java.io.InputStream;

public interface DatasetExportService {

    ExportResult exportDataset();

    record ExportResult(InputStream datasetStream, long sizeBytes) {
    }
}
