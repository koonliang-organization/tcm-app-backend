package com.tcm.backend.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CompositeDatasetExportService implements DatasetExportService {

    private final JsonDatasetExportService jsonDatasetExportService;
    private final SqliteDatasetExportService sqliteDatasetExportService;

    @Override
    public ExportResult exportDataset() {
        log.info("Starting composite dataset export (JSON + SQLite)");

        try (ByteArrayOutputStream compositeStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(compositeStream)) {

            // Export and add JSON dataset
            log.info("Exporting JSON dataset for composite export");
            ExportResult jsonResult = jsonDatasetExportService.exportDataset();
            addToZip(zipOutputStream, "json-dataset.zip", jsonResult.datasetStream());

            // Export and add SQLite dataset
            log.info("Exporting SQLite dataset for composite export");
            ExportResult sqliteResult = sqliteDatasetExportService.exportDataset();
            addToZip(zipOutputStream, "sqlite-dataset.db", sqliteResult.datasetStream());

            // Add composite manifest
            addCompositeManifest(zipOutputStream, jsonResult, sqliteResult);

            // Add comprehensive README
            addCompositeReadme(zipOutputStream);

            zipOutputStream.finish();
            byte[] compositeBytes = compositeStream.toByteArray();

            log.info("Created composite dataset with {} bytes (JSON: {} bytes, SQLite: {} bytes)",
                    compositeBytes.length, jsonResult.sizeBytes(), sqliteResult.sizeBytes());

            InputStream datasetStream = new ByteArrayInputStream(compositeBytes);
            return new ExportResult(datasetStream, compositeBytes.length);

        } catch (Exception e) {
            log.error("Failed to create composite dataset export", e);
            throw new IllegalStateException("Failed to create composite dataset export", e);
        }
    }

    private void addToZip(ZipOutputStream zipOutputStream, String entryName, InputStream inputStream)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        inputStream.transferTo(zipOutputStream);
        zipOutputStream.closeEntry();
        inputStream.close();
    }

    private void addCompositeManifest(ZipOutputStream zipOutputStream, ExportResult jsonResult, ExportResult sqliteResult)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));

        String manifest = String.format("""
                {
                    "export_type": "composite",
                    "export_timestamp": %d,
                    "formats": {
                        "json": {
                            "filename": "json-dataset.zip",
                            "size_bytes": %d,
                            "description": "Complete dataset in JSON format with ZIP compression"
                        },
                        "sqlite": {
                            "filename": "sqlite-dataset.db",
                            "size_bytes": %d,
                            "description": "SQLite database with FTS support for offline querying"
                        }
                    },
                    "total_size_bytes": %d,
                    "recommended_usage": {
                        "json": "Web applications, API consumption, data analysis",
                        "sqlite": "Mobile applications, offline access, full-text search"
                    }
                }
                """,
                System.currentTimeMillis(),
                jsonResult.sizeBytes(),
                sqliteResult.sizeBytes(),
                jsonResult.sizeBytes() + sqliteResult.sizeBytes()
        );

        zipOutputStream.write(manifest.getBytes());
        zipOutputStream.closeEntry();
    }

    private void addCompositeReadme(ZipOutputStream zipOutputStream) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry("README.md"));

        String readme = """
                # TCM Herbs Dataset (Composite Format)

                This dataset contains Traditional Chinese Medicine (TCM) herbs data in multiple formats to support different use cases.

                ## Contents

                ### Files Included
                - `json-dataset.zip` - Complete dataset in JSON format
                - `sqlite-dataset.db` - SQLite database with full-text search support
                - `manifest.json` - Metadata about both formats
                - `README.md` - This documentation file

                ### Format Comparison

                | Format | Best For | Features | Size |
                |--------|----------|----------|------|
                | JSON | Web apps, APIs, analysis | Human readable, widely supported | Larger |
                | SQLite | Mobile apps, offline use | FTS search, relational queries, compact | Smaller |

                ## Data Structure

                Each herb record includes:
                - **Basic Information**: Chinese name, Pinyin, descriptions in Chinese/English
                - **Properties**: Traditional properties and characteristics
                - **Flavors**: Taste characteristics and classifications
                - **Formulas**: Associated traditional formulations
                - **Images**: Visual representations with binary data
                - **Indications**: Medical uses and therapeutic applications
                - **Meridians**: Related meridian channels in TCM theory

                ## Usage Examples

                ### JSON Format
                ```javascript
                // Load and parse JSON dataset
                const dataset = JSON.parse(fs.readFileSync('json-dataset.zip'));
                const herbs = dataset.herbs;
                const metadata = dataset.metadata;
                ```

                ### SQLite Format
                ```sql
                -- Full-text search for herbs
                SELECT * FROM herbs_fts WHERE herbs_fts MATCH '当归';

                -- Get herb with all related data
                SELECT h.*,
                       GROUP_CONCAT(DISTINCT hf.value) as flavors,
                       GROUP_CONCAT(DISTINCT hi.value) as indications
                FROM herbs h
                LEFT JOIN herb_flavors hf ON h.id = hf.herb_id
                LEFT JOIN herb_indications hi ON h.id = hi.herb_id
                WHERE h.id = 1
                GROUP BY h.id;
                ```

                ## Integration Notes

                - Both formats contain identical data, choose based on your application needs
                - SQLite format includes optimized indexes for common query patterns
                - JSON format preserves exact structure for easy API consumption
                - Both formats include comprehensive metadata for validation

                ## Technical Specifications

                - Character encoding: UTF-8
                - SQLite version: 3.45+
                - JSON schema: TCM Herbs v1.0
                - Compression: ZIP for JSON, native SQLite compression for database

                ---

                Generated by TCM App Backend Dataset Publisher
                For technical support and documentation: [Repository Link]
                """;

        zipOutputStream.write(readme.getBytes());
        zipOutputStream.closeEntry();
    }
}