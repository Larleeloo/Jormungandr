package com.larleeloo.jormungandr.server.config;

import com.larleeloo.jormungandr.server.websocket.GameWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration. Registers the game handler at /game.
 *
 * Clients connect via: ws://<host>:8080/game
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameHandler;

    public WebSocketConfig(GameWebSocketHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameHandler, "/game")
                .setAllowedOrigins("*");
    }
}
