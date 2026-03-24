package com.larleeloo.jormungandr.server.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persists room notes in SQLite.
 *
 * Notes are stored as a JSON array per room, matching the Apps Script behavior
 * where each room's notes file contains the full list of PlayerNote objects.
 */
@Component
public class NoteStore {

    private static final Logger log = LoggerFactory.getLogger(NoteStore.class);
    private static final Gson gson = new Gson();
    private static final Type NOTE_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private final DatabaseManager db;

    public NoteStore(DatabaseManager db) {
        this.db = db;
    }

    public String getNotes(String roomId) {
        String sql = "SELECT data FROM notes WHERE room_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException e) {
            log.error("Failed to load notes for {}: {}", roomId, e.getMessage());
        }
        return "[]";
    }

    /**
     * Append a note to a room's notes list. The note is a JSON object with
     * author, accessCode, timestamp, and text fields — constructed by the caller.
     */
    public boolean addNote(String roomId, String accessCode, String noteText) {
        // Load existing notes
        String existing = getNotes(roomId);
        List<Map<String, Object>> notes;
        try {
            notes = gson.fromJson(existing, NOTE_LIST_TYPE);
            if (notes == null) notes = new ArrayList<>();
        } catch (Exception e) {
            notes = new ArrayList<>();
        }

        // Add new note
        Map<String, Object> note = Map.of(
                "accessCode", accessCode,
                "text", noteText,
                "timestamp", System.currentTimeMillis() / 1000L
        );
        notes.add(note);

        String json = gson.toJson(notes);
        String sql = """
                INSERT INTO notes (room_id, data, updated_at)
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
            log.error("Failed to save note for {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public boolean deleteAll() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM notes")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Failed to delete all notes: {}", e.getMessage());
            return false;
        }
    }
}
