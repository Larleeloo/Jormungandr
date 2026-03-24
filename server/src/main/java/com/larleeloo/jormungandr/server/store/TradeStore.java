package com.larleeloo.jormungandr.server.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists trade listings in SQLite.
 *
 * Trade data is stored as a raw JSON array per room (hub room only in practice),
 * matching the Apps Script behavior.
 */
@Component
public class TradeStore {

    private static final Logger log = LoggerFactory.getLogger(TradeStore.class);
    private final DatabaseManager db;

    public TradeStore(DatabaseManager db) {
        this.db = db;
    }

    public String getTrades(String roomId) {
        String sql = "SELECT data FROM trades WHERE room_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException e) {
            log.error("Failed to load trades for {}: {}", roomId, e.getMessage());
        }
        return "[]";
    }

    public boolean saveTrades(String roomId, String json) {
        String sql = """
                INSERT INTO trades (room_id, data, updated_at)
                VALUES (?, ?, datetime('now'))
                ON CONFLICT(room_id) DO UPDATE SET
                    data = excluded.data,
                    updated_at = datetime('now')
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, json);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to save trades for {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public boolean deleteAll() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM trades")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete all trades: {}", e.getMessage());
            return false;
        }
    }
}
