package com.tcm.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "publisher.export")
public class DatasetExportConfig {

    private String defaultService = "composite";
    private boolean enableJsonExport = true;
    private boolean enableSqliteExport = true;
    private boolean enableCompositeExport = true;
    private int batchSize = 1000;
}