package com.larleeloo.jormungandr.server.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists player save data as JSON blobs in SQLite.
 *
 * The server stores raw JSON exactly as the client sends it, matching
 * the Apps Script behavior. No server-side deserialization of player
 * model fields — the client owns the schema.
 */
@Component
public class PlayerStore {

    private static final Logger log = LoggerFactory.getLogger(PlayerStore.class);
    private final DatabaseManager db;

    public PlayerStore(DatabaseManager db) {
        this.db = db;
    }

    public String getPlayer(String accessCode) {
        String sql = "SELECT data FROM players WHERE access_code = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accessCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException e) {
            log.error("Failed to load player {}: {}", accessCode, e.getMessage());
        }
        return null;
    }

    public boolean savePlayer(String accessCode, String json) {
        String sql = """
                INSERT INTO players (access_code, data, updated_at)
                VALUES (?, ?, datetime('now'))
                ON CONFLICT(access_code) DO UPDATE SET
                    data = excluded.data,
                    updated_at = datetime('now')
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accessCode);
            ps.setString(2, json);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to save player {}: {}", accessCode, e.getMessage());
            return false;
        }
    }

    public boolean deleteAll() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM players")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete all players: {}", e.getMessage());
            return false;
        }
    }
}
