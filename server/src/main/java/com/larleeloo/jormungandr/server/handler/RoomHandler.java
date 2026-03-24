package com.larleeloo.jormungandr.server.handler;

import com.google.gson.Gson;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.RoomStore;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles room-related messages: get, save, list.
 *
 * Mirrors the Apps Script handlers:
 *   handleGetRoom, handleSaveRoom, handleListRooms
 */
@Component
public class RoomHandler {

    private final RoomStore store;
    private final Gson gson = new Gson();

    public RoomHandler(RoomStore store) {
        this.store = store;
    }

    public ServerMessage getRoom(ClientMessage msg) {
        String roomId = normalize(msg.getRoomId());
        if (roomId == null) {
            return ServerMessage.error("getRoom", "No roomId provided.");
        }
        String data = store.getRoom(roomId);
        if (data == null) {
            return ServerMessage.error("getRoom", "Room not found.");
        }
        return ServerMessage.ok("getRoom", "Room loaded.", data);
    }

    public ServerMessage saveRoom(ClientMessage msg) {
        String roomId = normalize(msg.getRoomId());
        String data = msg.getData();
        if (roomId == null || data == null || data.isBlank()) {
            return ServerMessage.error("saveRoom", "Missing roomId or data.");
        }
        boolean saved = store.saveRoom(roomId, data);
        return saved
                ? ServerMessage.ok("saveRoom", "Room saved.")
                : ServerMessage.error("saveRoom", "Failed to save room.");
    }

    public ServerMessage listRooms(ClientMessage msg) {
        int region = msg.getRegion();
        List<String> roomIds = store.listRooms(region);
        return ServerMessage.ok("listRooms", "OK", gson.toJson(roomIds));
    }

    private String normalize(String roomId) {
        if (roomId == null || roomId.isBlank()) return null;
        return roomId.trim();
    }
}
