package com.larleeloo.jormungandr.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authenticated WebSocket sessions by access code.
 *
 * Provides methods to:
 * - Register/unregister sessions after authentication
 * - Look up sessions by access code
 * - Send targeted messages to specific players
 * - Broadcast to all connected players or players in a room
 */
@Component
public class SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    /** Access code -> WebSocket session */
    private final Map<String, WebSocketSession> sessionsByCode = new ConcurrentHashMap<>();

    /** Session ID -> access code (reverse lookup for disconnect cleanup) */
    private final Map<String, String> codesBySessionId = new ConcurrentHashMap<>();

    /** Access code -> current room ID (for proximity/broadcast) */
    private final Map<String, String> roomByCode = new ConcurrentHashMap<>();

    public void register(String accessCode, WebSocketSession session) {
        // Close any existing session for this code (reconnect scenario)
        WebSocketSession existing = sessionsByCode.get(accessCode);
        if (existing != null && existing.isOpen() && !existing.getId().equals(session.getId())) {
            try {
                existing.close();
            } catch (IOException ignored) {}
        }

        sessionsByCode.put(accessCode, session);
        codesBySessionId.put(session.getId(), accessCode);
        log.info("Registered session for {}", accessCode);
    }

    public void unregister(WebSocketSession session) {
        String code = codesBySessionId.remove(session.getId());
        if (code != null) {
            sessionsByCode.remove(code);
            roomByCode.remove(code);
            log.info("Unregistered session for {}", code);
        }
    }

    public String getAccessCode(WebSocketSession session) {
        return codesBySessionId.get(session.getId());
    }

    public WebSocketSession getSession(String accessCode) {
        return sessionsByCode.get(accessCode);
    }

    public boolean isOnline(String accessCode) {
        WebSocketSession session = sessionsByCode.get(accessCode);
        return session != null && session.isOpen();
    }

    public void updateRoom(String accessCode, String roomId) {
        roomByCode.put(accessCode, roomId);
    }

    public String getRoom(String accessCode) {
        return roomByCode.get(accessCode);
    }

    public Collection<String> getOnlineCodes() {
        return sessionsByCode.keySet();
    }

    public Map<String, String> getAllPlayerRooms() {
        return Map.copyOf(roomByCode);
    }

    /**
     * Send a JSON message to a specific player by access code.
     */
    public void sendTo(String accessCode, String json) {
        WebSocketSession session = sessionsByCode.get(accessCode);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("Failed to send to {}: {}", accessCode, e.getMessage());
            }
        }
    }

    /**
     * Broadcast a JSON message to all players currently in a specific room.
     */
    public void broadcastToRoom(String roomId, String json) {
        roomByCode.forEach((code, room) -> {
            if (roomId.equals(room)) {
                sendTo(code, json);
            }
        });
    }

    /**
     * Broadcast a JSON message to all players in a room except the sender.
     */
    public void broadcastToRoomExcept(String roomId, String excludeCode, String json) {
        roomByCode.forEach((code, room) -> {
            if (roomId.equals(room) && !code.equals(excludeCode)) {
                sendTo(code, json);
            }
        });
    }

    /**
     * Broadcast a JSON message to all connected players.
     */
    public void broadcastAll(String json) {
        sessionsByCode.forEach((code, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("Broadcast failed for {}: {}", code, e.getMessage());
                }
            }
        });
    }
}
