package com.larleeloo.jormungandr.server.handler;

import com.google.gson.Gson;
import com.larleeloo.jormungandr.server.config.ServerConstants;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.websocket.SessionRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages turn-based co-location queues entirely in memory.
 *
 * When 2+ players are in the same room, they take turns interacting
 * with room objects (chests, creatures). The turn queue is a FIFO list
 * of access codes per room.
 *
 * Key improvement over Apps Script: Turn state changes are pushed to
 * all players in the room immediately via WebSocket broadcast, eliminating
 * the 2-second polling interval from the Android client.
 *
 * Mirrors the Apps Script handlers:
 *   handleJoinTurnQueue, handleEndTurn, handleLeaveTurnQueue, handleGetTurnState
 */
@Component
public class TurnHandler {

    private static final Logger log = LoggerFactory.getLogger(TurnHandler.class);
    private final Gson gson = new Gson();

    /**
     * In-memory turn state per room.
     * Key: roomId, Value: TurnQueue (list of codes + current index + timestamp)
     */
    private final Map<String, TurnQueue> turnQueues = new ConcurrentHashMap<>();

    public ServerMessage joinTurnQueue(ClientMessage msg, SessionRegistry registry) {
        String roomId = msg.getRoomId();
        String code = normalize(msg.getCode());
        if (roomId == null || code == null) {
            return ServerMessage.error("joinTurnQueue", "Missing roomId or code.");
        }
        roomId = roomId.trim();

        TurnQueue queue = turnQueues.computeIfAbsent(roomId, k -> new TurnQueue());
        synchronized (queue) {
            if (!queue.codes.contains(code)) {
                queue.codes.add(code);
                if (queue.codes.size() == 1) {
                    queue.currentIndex = 0;
                    queue.turnStartedAt = System.currentTimeMillis() / 1000L;
                }
            }
        }

        // Broadcast updated turn state to the room
        broadcastTurnState(roomId, registry);

        return ServerMessage.ok("joinTurnQueue", "Joined turn queue.", turnStateJson(roomId));
    }

    public ServerMessage endTurn(ClientMessage msg, SessionRegistry registry) {
        String roomId = msg.getRoomId();
        String code = normalize(msg.getCode());
        if (roomId == null || code == null) {
            return ServerMessage.error("endTurn", "Missing roomId or code.");
        }
        roomId = roomId.trim();

        TurnQueue queue = turnQueues.get(roomId);
        if (queue == null) {
            return ServerMessage.error("endTurn", "No turn queue for this room.");
        }

        synchronized (queue) {
            // Only the current turn holder can end their turn
            if (queue.currentIndex >= 0 && queue.currentIndex < queue.codes.size()) {
                String currentHolder = queue.codes.get(queue.currentIndex);
                if (currentHolder.equals(code)) {
                    advanceTurn(queue);
                }
            }
        }

        broadcastTurnState(roomId, registry);
        return ServerMessage.ok("endTurn", "Turn ended.", turnStateJson(roomId));
    }

    public ServerMessage leaveTurnQueue(ClientMessage msg, SessionRegistry registry) {
        String roomId = msg.getRoomId();
        String code = normalize(msg.getCode());
        if (roomId == null || code == null) {
            return ServerMessage.error("leaveTurnQueue", "Missing roomId or code.");
        }
        roomId = roomId.trim();

        removeFromQueue(roomId, code);
        broadcastTurnState(roomId, registry);

        return ServerMessage.ok("leaveTurnQueue", "Left turn queue.", turnStateJson(roomId));
    }

    public ServerMessage getTurnState(ClientMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("getTurnState", "No roomId provided.");
        }
        return ServerMessage.ok("getTurnState", "OK", turnStateJson(roomId.trim()));
    }

    /**
     * Called when a player disconnects. Remove them from all turn queues.
     */
    public void handleDisconnect(String accessCode) {
        turnQueues.forEach((roomId, queue) -> {
            synchronized (queue) {
                if (queue.codes.contains(accessCode)) {
                    removeFromQueue(roomId, accessCode);
                }
            }
        });
    }

    /** Scheduled task: auto-advance stale turns every 5 seconds. */
    @Scheduled(fixedRate = 5000)
    public void autoAdvanceStaleTurns() {
        long now = System.currentTimeMillis() / 1000L;
        turnQueues.forEach((roomId, queue) -> {
            synchronized (queue) {
                if (!queue.codes.isEmpty()
                        && queue.turnStartedAt > 0
                        && (now - queue.turnStartedAt) >= ServerConstants.TURN_TIMEOUT_SECONDS) {
                    log.debug("Auto-advancing stale turn in room {}", roomId);
                    advanceTurn(queue);
                }
            }
        });
    }

    private void advanceTurn(TurnQueue queue) {
        if (queue.codes.isEmpty()) return;
        queue.currentIndex = (queue.currentIndex + 1) % queue.codes.size();
        queue.turnStartedAt = System.currentTimeMillis() / 1000L;
    }

    private void removeFromQueue(String roomId, String code) {
        TurnQueue queue = turnQueues.get(roomId);
        if (queue == null) return;

        synchronized (queue) {
            int idx = queue.codes.indexOf(code);
            if (idx < 0) return;

            // Adjust currentIndex if removing before or at current position
            if (idx < queue.currentIndex) {
                queue.currentIndex--;
            } else if (idx == queue.currentIndex) {
                // Current player leaving — don't advance index, let it point to next
                // (which shifts into this position after removal)
            }

            queue.codes.remove(idx);

            if (queue.codes.isEmpty()) {
                turnQueues.remove(roomId);
            } else {
                // Wrap index if needed
                if (queue.currentIndex >= queue.codes.size()) {
                    queue.currentIndex = 0;
                }
                queue.turnStartedAt = System.currentTimeMillis() / 1000L;
            }
        }
    }

    private String turnStateJson(String roomId) {
        TurnQueue queue = turnQueues.get(roomId);
        if (queue == null || queue.codes.isEmpty()) {
            return gson.toJson(Map.of(
                    "roomId", roomId,
                    "queue", List.of(),
                    "currentIndex", -1,
                    "turnStartedAt", 0
            ));
        }
        synchronized (queue) {
            return gson.toJson(Map.of(
                    "roomId", roomId,
                    "queue", queue.codes,
                    "currentIndex", queue.currentIndex,
                    "turnStartedAt", queue.turnStartedAt
            ));
        }
    }

    private void broadcastTurnState(String roomId, SessionRegistry registry) {
        String state = turnStateJson(roomId);
        String broadcast = "{\"type\":\"turnStateChanged\"," +
                "\"roomId\":\"" + roomId + "\"," +
                "\"data\":" + state + "}";
        registry.broadcastToRoom(roomId, broadcast);
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) return null;
        return code.trim().toUpperCase();
    }

    /** In-memory turn queue state for a single room. */
    private static class TurnQueue {
        final List<String> codes = new ArrayList<>();
        int currentIndex = -1;
        long turnStartedAt = 0;
    }
}
