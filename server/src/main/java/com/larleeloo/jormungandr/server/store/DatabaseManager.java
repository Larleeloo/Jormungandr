package com.larleeloo.jormungandr.server.store;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages a single SQLite database connection for the server.
 *
 * SQLite is ideal for this use case:
 * - 25 players max, very low write concurrency
 * - Single-file database, trivial to back up (scp the .db file)
 * - No external database service required
 * - Sub-millisecond reads for in-process queries
 *
 * The database file path is configurable via application.properties:
 *   jormungandr.db.path=./data/jormungandr.db
 */
@Component
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    @Value("${jormungandr.db.path:./data/jormungandr.db}")
    private String dbPath;

    private Connection connection;

    @PostConstruct
    public void init() throws SQLException, IOException {
        // Ensure parent directory exists
        java.io.File dbFile = new java.io.File(dbPath);
        if (dbFile.getParentFile() != null) {
            dbFile.getParentFile().mkdirs();
        }

        String url = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(url);

        // Enable WAL mode for better concurrent read performance
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        // Run schema initialization
        String schema = new String(
                new ClassPathResource("schema.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        try (Statement stmt = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }

        log.info("Database initialized at {}", dbPath);
    }

    public Connection getConnection() {
        return connection;
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Database connection closed");
            } catch (SQLException e) {
                log.warn("Error closing database: {}", e.getMessage());
            }
        }
    }
}
