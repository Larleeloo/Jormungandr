package com.larleeloo.jormungandr.server.websocket;

import com.larleeloo.jormungandr.server.handler.MessageRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Core WebSocket handler. Receives text messages from connected clients,
 * delegates to {@link MessageRouter} for processing, and manages session
 * lifecycle via {@link SessionRegistry}.
 *
 * Connection flow:
 * 1. Client connects to ws://<host>:8080/game
 * 2. Client sends: {"type":"authenticate","code":"JORM-ALPHA-001"}
 * 3. Server responds: {"type":"authenticated","success":true}
 * 4. All subsequent messages require authentication
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final MessageRouter router;
    private final SessionRegistry registry;

    public GameWebSocketHandler(MessageRouter router, SessionRegistry registry) {
        this.router = router;
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("New connection: {} from {}", session.getId(),
                session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String response = router.route(session, message.getPayload());
            if (response != null) {
                session.sendMessage(new TextMessage(response));
            }
        } catch (Exception e) {
            log.error("Error handling message from {}: {}", session.getId(), e.getMessage(), e);
            try {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"error\",\"message\":\"Internal server error\"}"));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String code = registry.getAccessCode(session);
        log.info("Connection closed: {} (code={}, status={})",
                session.getId(), code, status);
        registry.unregister(session);

        // Notify the router so handlers can clean up (turn queues, proximity, etc.)
        if (code != null) {
            router.handleDisconnect(code);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error for {}: {}", session.getId(), exception.getMessage());
    }
}
