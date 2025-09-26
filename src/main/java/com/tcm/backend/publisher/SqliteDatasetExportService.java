package com.tcm.backend.publisher;

import com.tcm.backend.domain.Herb;
import com.tcm.backend.publisher.dto.HerbExportDto;
import com.tcm.backend.publisher.mapper.HerbExportMapper;
import com.tcm.backend.repository.HerbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqliteDatasetExportService implements DatasetExportService {

    private final HerbRepository herbRepository;
    private final HerbExportMapper herbExportMapper;

    @Override
    public ExportResult exportDataset() {
        log.info("Starting SQLite dataset export");

        try {
            // Create temporary SQLite file
            File tempDb = File.createTempFile("tcm-dataset", ".sqlite");
            tempDb.deleteOnExit();

            String jdbcUrl = "jdbc:sqlite:" + tempDb.getAbsolutePath();

            // Load herbs with relations
            List<Herb> herbs = herbRepository.findAllWithRelationsForExport();
            log.info("Loaded {} herbs with relations for SQLite export", herbs.size());

            List<HerbExportDto> herbExportDtos = herbExportMapper.toExportDtos(herbs);

            // Create and populate SQLite database
            try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                createTables(connection);
                populateData(connection, herbExportDtos);
                createIndexes(connection);
                createMetadata(connection, herbExportDtos);
            }

            // Read the SQLite file into byte array
            byte[] sqliteBytes = Files.readAllBytes(tempDb.toPath());

            log.info("Created SQLite dataset with {} bytes", sqliteBytes.length);

            InputStream datasetStream = new ByteArrayInputStream(sqliteBytes);
            return new ExportResult(datasetStream, sqliteBytes.length);

        } catch (Exception e) {
            log.error("Failed to export SQLite dataset", e);
            throw new IllegalStateException("Failed to export SQLite dataset", e);
        }
    }

    private void createTables(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Create herbs table with FTS support
            stmt.execute("""
                CREATE TABLE herbs (
                    id INTEGER PRIMARY KEY,
                    source_url TEXT NOT NULL UNIQUE,
                    name_zh TEXT,
                    name_pinyin TEXT,
                    desc_zh TEXT,
                    desc_en TEXT,
                    appearance TEXT,
                    property TEXT
                )
                """);

            // Create FTS virtual table for search
            stmt.execute("""
                CREATE VIRTUAL TABLE herbs_fts USING fts5(
                    name_zh,
                    name_pinyin,
                    desc_zh,
                    desc_en,
                    content='herbs',
                    content_rowid='id'
                )
                """);

            // Create related entity tables
            stmt.execute("""
                CREATE TABLE herb_flavors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    herb_id INTEGER NOT NULL,
                    value TEXT NOT NULL,
                    FOREIGN KEY (herb_id) REFERENCES herbs(id),
                    UNIQUE(herb_id, value)
                )
                """);

            stmt.execute("""
                CREATE TABLE herb_formulas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    herb_id INTEGER NOT NULL,
                    value TEXT NOT NULL,
                    FOREIGN KEY (herb_id) REFERENCES herbs(id),
                    UNIQUE(herb_id, value)
                )
                """);

            stmt.execute("""
                CREATE TABLE herb_images (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    herb_id INTEGER NOT NULL,
                    filename TEXT NOT NULL,
                    mime TEXT NOT NULL,
                    data BLOB NOT NULL,
                    FOREIGN KEY (herb_id) REFERENCES herbs(id),
                    UNIQUE(herb_id, filename)
                )
                """);

            stmt.execute("""
                CREATE TABLE herb_indications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    herb_id INTEGER NOT NULL,
                    value TEXT NOT NULL,
                    FOREIGN KEY (herb_id) REFERENCES herbs(id),
                    UNIQUE(herb_id, value)
                )
                """);

            stmt.execute("""
                CREATE TABLE herb_meridians (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    herb_id INTEGER NOT NULL,
                    value TEXT NOT NULL,
                    FOREIGN KEY (herb_id) REFERENCES herbs(id),
                    UNIQUE(herb_id, value)
                )
                """);

            log.info("Created SQLite tables with FTS support");
        }
    }

    private void populateData(Connection connection, List<HerbExportDto> herbs) throws Exception {
        connection.setAutoCommit(false);

        try {
            // Insert herbs
            String herbSql = """
                INSERT INTO herbs (id, source_url, name_zh, name_pinyin, desc_zh, desc_en, appearance, property)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement herbStmt = connection.prepareStatement(herbSql)) {
                for (HerbExportDto herb : herbs) {
                    herbStmt.setInt(1, herb.id());
                    herbStmt.setString(2, herb.sourceUrl());
                    herbStmt.setString(3, herb.nameZh());
                    herbStmt.setString(4, herb.namePinyin());
                    herbStmt.setString(5, herb.descZh());
                    herbStmt.setString(6, herb.descEn());
                    herbStmt.setString(7, herb.appearance());
                    herbStmt.setString(8, herb.property());
                    herbStmt.addBatch();
                }
                herbStmt.executeBatch();
            }

            // Populate FTS table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("INSERT INTO herbs_fts(herbs_fts) VALUES('rebuild')");
            }

            // Insert related entities
            insertRelatedEntities(connection, herbs);

            connection.commit();
            log.info("Populated SQLite database with {} herbs", herbs.size());

        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void insertRelatedEntities(Connection connection, List<HerbExportDto> herbs) throws Exception {
        // Insert flavors
        String flavorSql = "INSERT INTO herb_flavors (herb_id, value) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(flavorSql)) {
            for (HerbExportDto herb : herbs) {
                if (herb.flavors() != null) {
                    for (var flavor : herb.flavors()) {
                        stmt.setInt(1, herb.id());
                        stmt.setString(2, flavor.value());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }

        // Insert formulas
        String formulaSql = "INSERT INTO herb_formulas (herb_id, value) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(formulaSql)) {
            for (HerbExportDto herb : herbs) {
                if (herb.formulas() != null) {
                    for (var formula : herb.formulas()) {
                        stmt.setInt(1, herb.id());
                        stmt.setString(2, formula.value());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }

        // Insert images
        String imageSql = "INSERT INTO herb_images (herb_id, filename, mime, data) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(imageSql)) {
            for (HerbExportDto herb : herbs) {
                if (herb.images() != null) {
                    for (var image : herb.images()) {
                        stmt.setInt(1, herb.id());
                        stmt.setString(2, image.filename());
                        stmt.setString(3, image.mime());
                        stmt.setBytes(4, image.data());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }

        // Insert indications
        String indicationSql = "INSERT INTO herb_indications (herb_id, value) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(indicationSql)) {
            for (HerbExportDto herb : herbs) {
                if (herb.indications() != null) {
                    for (var indication : herb.indications()) {
                        stmt.setInt(1, herb.id());
                        stmt.setString(2, indication.value());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }

        // Insert meridians
        String meridianSql = "INSERT INTO herb_meridians (herb_id, value) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(meridianSql)) {
            for (HerbExportDto herb : herbs) {
                if (herb.meridians() != null) {
                    for (var meridian : herb.meridians()) {
                        stmt.setInt(1, herb.id());
                        stmt.setString(2, meridian.value());
                        stmt.addBatch();
                    }
                }
            }
            stmt.executeBatch();
        }
    }

    private void createIndexes(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Create indexes for better query performance
            stmt.execute("CREATE INDEX idx_herbs_name_zh ON herbs(name_zh)");
            stmt.execute("CREATE INDEX idx_herbs_name_pinyin ON herbs(name_pinyin)");
            stmt.execute("CREATE INDEX idx_herb_flavors_herb_id ON herb_flavors(herb_id)");
            stmt.execute("CREATE INDEX idx_herb_formulas_herb_id ON herb_formulas(herb_id)");
            stmt.execute("CREATE INDEX idx_herb_images_herb_id ON herb_images(herb_id)");
            stmt.execute("CREATE INDEX idx_herb_indications_herb_id ON herb_indications(herb_id)");
            stmt.execute("CREATE INDEX idx_herb_meridians_herb_id ON herb_meridians(herb_id)");

            log.info("Created SQLite indexes for performance optimization");
        }
    }

    private void createMetadata(Connection connection, List<HerbExportDto> herbs) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Create metadata table
            stmt.execute("""
                CREATE TABLE dataset_metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);

            // Insert metadata
            PreparedStatement metaStmt = connection.prepareStatement(
                "INSERT INTO dataset_metadata (key, value) VALUES (?, ?)");

            metaStmt.setString(1, "export_timestamp");
            metaStmt.setString(2, String.valueOf(System.currentTimeMillis()));
            metaStmt.addBatch();

            metaStmt.setString(1, "export_format");
            metaStmt.setString(2, "sqlite");
            metaStmt.addBatch();

            metaStmt.setString(1, "total_herbs");
            metaStmt.setString(2, String.valueOf(herbs.size()));
            metaStmt.addBatch();

            // Calculate counts
            int totalFlavors = herbs.stream()
                    .mapToInt(h -> h.flavors() != null ? h.flavors().size() : 0)
                    .sum();
            int totalFormulas = herbs.stream()
                    .mapToInt(h -> h.formulas() != null ? h.formulas().size() : 0)
                    .sum();
            int totalImages = herbs.stream()
                    .mapToInt(h -> h.images() != null ? h.images().size() : 0)
                    .sum();
            int totalIndications = herbs.stream()
                    .mapToInt(h -> h.indications() != null ? h.indications().size() : 0)
                    .sum();
            int totalMeridians = herbs.stream()
                    .mapToInt(h -> h.meridians() != null ? h.meridians().size() : 0)
                    .sum();

            metaStmt.setString(1, "total_flavors");
            metaStmt.setString(2, String.valueOf(totalFlavors));
            metaStmt.addBatch();

            metaStmt.setString(1, "total_formulas");
            metaStmt.setString(2, String.valueOf(totalFormulas));
            metaStmt.addBatch();

            metaStmt.setString(1, "total_images");
            metaStmt.setString(2, String.valueOf(totalImages));
            metaStmt.addBatch();

            metaStmt.setString(1, "total_indications");
            metaStmt.setString(2, String.valueOf(totalIndications));
            metaStmt.addBatch();

            metaStmt.setString(1, "total_meridians");
            metaStmt.setString(2, String.valueOf(totalMeridians));
            metaStmt.addBatch();

            metaStmt.executeBatch();

            log.info("Created SQLite metadata with statistics");
        }
    }
}