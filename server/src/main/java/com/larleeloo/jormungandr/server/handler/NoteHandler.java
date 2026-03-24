package com.larleeloo.jormungandr.server.handler;

import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.NoteStore;

import org.springframework.stereotype.Component;

/**
 * Handles note-related messages: get notes for a room, save a new note.
 *
 * Mirrors the Apps Script handlers:
 *   handleGetNotes, handleSaveNote
 */
@Component
public class NoteHandler {

    private final NoteStore store;

    public NoteHandler(NoteStore store) {
        this.store = store;
    }

    public ServerMessage getNotes(ClientMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("getNotes", "No roomId provided.");
        }
        String data = store.getNotes(roomId.trim());
        return ServerMessage.ok("getNotes", "Notes loaded.", data);
    }

    public ServerMessage saveNote(ClientMessage msg) {
        String roomId = msg.getRoomId();
        String code = msg.getCode();
        String noteText = msg.getNote();

        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("saveNote", "No roomId provided.");
        }
        if (code == null || code.isBlank()) {
            return ServerMessage.error("saveNote", "No access code provided.");
        }
        if (noteText == null || noteText.isBlank()) {
            return ServerMessage.error("saveNote", "No note text provided.");
        }

        boolean saved = store.addNote(roomId.trim(), code.trim().toUpperCase(), noteText);
        return saved
                ? ServerMessage.ok("saveNote", "Note saved.")
                : ServerMessage.error("saveNote", "Failed to save note.");
    }
}
