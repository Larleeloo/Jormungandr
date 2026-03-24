package com.larleeloo.jormungandr.server.handler;

import com.google.gson.Gson;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.ActionStore;
import com.larleeloo.jormungandr.server.websocket.SessionRegistry;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles proximity detection and co-location action logging.
 *
 * Key improvement over Apps Script: Instead of polling, the server knows
 * every player's current room in real-time via the SessionRegistry. Proximity
 * checks are computed server-side with zero I/O — just a map scan.
 *
 * Mirrors the Apps Script handlers:
 *   handleGetNearbyPlayers, handleRecordAction, handleGetRecentActions, handleCleanupActions
 */
@Component
public class ProximityHandler {

    private final ActionStore actionStore;
    private final Gson gson = new Gson();

    public ProximityHandler(ActionStore actionStore) {
        this.actionStore = actionStore;
    }

    /**
     * Find players within Manhattan distance of the requesting player.
     *
     * Unlike Apps Script which reads every player file from Drive, the server
     * has all player locations in memory. This is O(n) where n = 25 max.
     */
    public ServerMessage getNearbyPlayers(ClientMessage msg, SessionRegistry registry) {
        String callerCode = normalize(msg.getCode());
        String callerRoom = msg.getRoomId();
        int range = msg.getRange();

        if (callerCode == null || callerRoom == null || callerRoom.isBlank()) {
            return ServerMessage.error("getNearbyPlayers", "Missing code or roomId.");
        }
        callerRoom = callerRoom.trim();

        // Update caller's room in the registry
        registry.updateRoom(callerCode, callerRoom);

        // Parse caller's grid position
        int[] callerPos = parseGridPosition(callerRoom);
        int callerRegion = parseRegion(callerRoom);

        List<Map<String, Object>> nearby = new ArrayList<>();

        for (Map.Entry<String, String> entry : registry.getAllPlayerRooms().entrySet()) {
            String otherCode = entry.getKey();
            String otherRoom = entry.getValue();

            if (otherCode.equals(callerCode)) continue;
            if (!registry.isOnline(otherCode)) continue;

            int otherRegion = parseRegion(otherRoom);
            if (otherRegion != callerRegion) continue;

            int[] otherPos = parseGridPosition(otherRoom);
            int distance = Math.abs(callerPos[0] - otherPos[0])
                         + Math.abs(callerPos[1] - otherPos[1]);

            if (distance <= range) {
                nearby.add(Map.of(
                        "accessCode", otherCode,
                        "roomId", otherRoom,
                        "distance", distance
                ));
            }
        }

        return ServerMessage.ok("getNearbyPlayers", "OK", gson.toJson(nearby));
    }

    public ServerMessage recordAction(ClientMessage msg, SessionRegistry registry) {
        String roomId = msg.getRoomId();
        String code = normalize(msg.getCode());
        String actionText = msg.getActionText();

        if (roomId == null || code == null || actionText == null) {
            return ServerMessage.error("recordAction", "Missing roomId, code, or actionText.");
        }

        boolean saved = actionStore.recordAction(roomId.trim(), code, actionText);

        // Push the action to all other players in the room
        if (saved) {
            String broadcast = gson.toJson(Map.of(
                    "type", "actionBroadcast",
                    "roomId", roomId.trim(),
                    "accessCode", code,
                    "actionText", actionText,
                    "timestamp", System.currentTimeMillis() / 1000L
            ));
            registry.broadcastToRoomExcept(roomId.trim(), code, broadcast);
        }

        return saved
                ? ServerMessage.ok("recordAction", "Action recorded.")
                : ServerMessage.error("recordAction", "Failed to record action.");
    }

    public ServerMessage getRecentActions(ClientMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("getRecentActions", "No roomId provided.");
        }
        String data = actionStore.getRecentActions(roomId.trim(), msg.getSince());
        return ServerMessage.ok("getRecentActions", "OK", data);
    }

    public ServerMessage cleanupActions(ClientMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("cleanupActions", "No roomId provided.");
        }
        actionStore.cleanupRoom(roomId.trim());
        return ServerMessage.ok("cleanupActions", "Cleanup complete.");
    }

    /** Called when a player disconnects. No persistent state to clean here. */
    public void handleDisconnect(String accessCode) {
        // Room location is cleaned up by SessionRegistry.unregister()
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) return null;
        return code.trim().toUpperCase();
    }

    /** Parse region from "r3_04250" -> 3 */
    private int parseRegion(String roomId) {
        try {
            int underscore = roomId.indexOf('_');
            if (underscore > 1 && roomId.charAt(0) == 'r') {
                return Integer.parseInt(roomId.substring(1, underscore));
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /** Parse grid position from "r3_04250" -> [42, 50] (row=42, col=50) */
    private int[] parseGridPosition(String roomId) {
        try {
            int underscore = roomId.indexOf('_');
            if (underscore >= 0) {
                int number = Integer.parseInt(roomId.substring(underscore + 1));
                return new int[]{number / 100, number % 100};
            }
        } catch (Exception ignored) {}
        return new int[]{0, 0};
    }
}
