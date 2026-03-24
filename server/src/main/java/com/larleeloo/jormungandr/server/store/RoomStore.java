package com.larleeloo.jormungandr.server.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists room data as JSON blobs in SQLite.
 *
 * Rooms are keyed by room ID (e.g., "r3_04250").
 * The region column is extracted for efficient listing queries.
 */
@Component
public class RoomStore {

    private static final Logger log = LoggerFactory.getLogger(RoomStore.class);
    private final DatabaseManager db;

    public RoomStore(DatabaseManager db) {
        this.db = db;
    }

    public String getRoom(String roomId) {
        String sql = "SELECT data FROM rooms WHERE room_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException e) {
            log.error("Failed to load room {}: {}", roomId, e.getMessage());
        }
        return null;
    }

    public boolean saveRoom(String roomId, String json) {
        int region = parseRegion(roomId);
        String sql = """
                INSERT INTO rooms (room_id, region, data, updated_at)
                VALUES (?, ?, ?, datetime('now'))
                ON CONFLICT(room_id) DO UPDATE SET
                    data = excluded.data,
                    updated_at = datetime('now')
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, region);
            ps.setString(3, json);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to save room {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public List<String> listRooms(int region) {
        List<String> roomIds = new ArrayList<>();
        String sql = "SELECT room_id FROM rooms WHERE region = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, region);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roomIds.add(rs.getString("room_id"));
            }
        } catch (SQLException e) {
            log.error("Failed to list rooms for region {}: {}", region, e.getMessage());
        }
        return roomIds;
    }

    public boolean deleteAll() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM rooms")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete all rooms: {}", e.getMessage());
            return false;
        }
    }

    /** Extract region number from room ID like "r3_04250" -> 3 */
    private int parseRegion(String roomId) {
        try {
            if (roomId != null && roomId.startsWith("r")) {
                int underscoreIdx = roomId.indexOf('_');
                if (underscoreIdx > 1) {
                    return Integer.parseInt(roomId.substring(1, underscoreIdx));
                }
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }
}
