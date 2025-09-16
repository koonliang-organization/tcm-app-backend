package com.tcm.backend.publisher;

import java.io.InputStream;

public interface DatasetStorageClient {

    StorageResult storeDataset(String objectKey, InputStream datasetStream, long contentLength);

    record StorageResult(String url, String checksum) {
    }
}
