package com.larleeloo.jormungandr.server.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.larleeloo.jormungandr.server.config.ServerConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persists co-location action logs with TTL-based expiration.
 *
 * Actions are ephemeral "Player X opened chest" logs that only matter while
 * players are near each other. Entries older than ACTION_TTL_SECONDS are
 * pruned on read/write and by a scheduled cleanup task.
 */
@Component
public class ActionStore {

    private static final Logger log = LoggerFactory.getLogger(ActionStore.class);
    private static final Gson gson = new Gson();
    private static final Type ACTION_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private final DatabaseManager db;

    public ActionStore(DatabaseManager db) {
        this.db = db;
    }

    public boolean recordAction(String roomId, String accessCode, String actionText) {
        List<Map<String, Object>> actions = loadActions(roomId);
        long now = System.currentTimeMillis() / 1000L;

        // Prune expired
        long cutoff = now - ServerConstants.ACTION_TTL_SECONDS;
        actions.removeIf(a -> {
            Object ts = a.get("timestamp");
            return ts instanceof Number && ((Number) ts).longValue() < cutoff;
        });

        // Add new action
        actions.add(Map.of(
                "accessCode", accessCode,
                "actionText", actionText,
                "timestamp", now
        ));

        return saveActions(roomId, actions);
    }

    public String getRecentActions(String roomId, long sinceEpochSeconds) {
        List<Map<String, Object>> actions = loadActions(roomId);
        long cutoff = Math.max(sinceEpochSeconds,
                System.currentTimeMillis() / 1000L - ServerConstants.ACTION_TTL_SECONDS);

        List<Map<String, Object>> recent = actions.stream()
                .filter(a -> {
                    Object ts = a.get("timestamp");
                    return ts instanceof Number && ((Number) ts).longValue() >= cutoff;
                })
                .toList();

        return gson.toJson(recent);
    }

    public boolean cleanupRoom(String roomId) {
        List<Map<String, Object>> actions = loadActions(roomId);
        long cutoff = System.currentTimeMillis() / 1000L - ServerConstants.ACTION_TTL_SECONDS;

        actions.removeIf(a -> {
            Object ts = a.get("timestamp");
            return ts instanceof Number && ((Number) ts).longValue() < cutoff;
        });

        if (actions.isEmpty()) {
            return deleteRoom(roomId);
        }
        return saveActions(roomId, actions);
    }

    public boolean deleteAll() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM actions")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete all actions: {}", e.getMessage());
            return false;
        }
    }

    /** Scheduled cleanup — runs every 15 minutes to prune expired actions. */
    @Scheduled(fixedRate = 900_000)
    public void scheduledCleanup() {
        String sql = "SELECT room_id FROM actions";
        List<String> roomIds = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roomIds.add(rs.getString("room_id"));
            }
        } catch (SQLException e) {
            log.error("Cleanup query failed: {}", e.getMessage());
            return;
        }

        int cleaned = 0;
        for (String roomId : roomIds) {
            cleanupRoom(roomId);
            cleaned++;
        }
        if (cleaned > 0) {
            log.debug("Scheduled cleanup processed {} action rooms", cleaned);
        }
    }

    private List<Map<String, Object>> loadActions(String roomId) {
        String sql = "SELECT data FROM actions WHERE room_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String data = rs.getString("data");
                List<Map<String, Object>> list = gson.fromJson(data, ACTION_LIST_TYPE);
                return list != null ? new ArrayList<>(list) : new ArrayList<>();
            }
        } catch (SQLException e) {
            log.error("Failed to load actions for {}: {}", roomId, e.getMessage());
        }
        return new ArrayList<>();
    }

    private boolean saveActions(String roomId, List<Map<String, Object>> actions) {
        String sql = """
                INSERT INTO actions (room_id, data, updated_at)
                VALUES (?, ?, datetime('now'))
                ON CONFLICT(room_id) DO UPDATE SET
                    data = excluded.data,
                    updated_at = datetime('now')
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, gson.toJson(actions));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to save actions for {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    private boolean deleteRoom(String roomId) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "DELETE FROM actions WHERE room_id = ?")) {
            ps.setString(1, roomId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete actions for {}: {}", roomId, e.getMessage());
            return false;
        }
    }
}
