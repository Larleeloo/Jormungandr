package com.larleeloo.jormungandr.server.handler;

import com.larleeloo.jormungandr.server.config.ServerConstants;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.PlayerStore;

import org.springframework.stereotype.Component;

/**
 * Handles player-related messages: validate, get, save.
 *
 * Mirrors the Apps Script handlers:
 *   handleValidateCode, handleGetPlayer, handleSavePlayer
 */
@Component
public class PlayerHandler {

    private final PlayerStore store;

    public PlayerHandler(PlayerStore store) {
        this.store = store;
    }

    public ServerMessage validateCode(ClientMessage msg) {
        String code = normalize(msg.getCode());
        if (code == null) {
            return ServerMessage.error("validateCode", "No access code provided.");
        }
        boolean valid = ServerConstants.VALID_CODES.contains(code);
        return valid
                ? ServerMessage.ok("validateCode", "Access code valid.")
                : ServerMessage.error("validateCode", "Invalid access code.");
    }

    public ServerMessage getPlayer(ClientMessage msg) {
        String code = normalize(msg.getCode());
        if (code == null) {
            return ServerMessage.error("getPlayer", "No access code provided.");
        }
        String data = store.getPlayer(code);
        if (data == null) {
            return ServerMessage.error("getPlayer", "No save found for this code.");
        }
        return ServerMessage.ok("getPlayer", "Player loaded.", data);
    }

    public ServerMessage savePlayer(ClientMessage msg) {
        String code = normalize(msg.getCode());
        String data = msg.getData();
        if (code == null || data == null || data.isBlank()) {
            return ServerMessage.error("savePlayer", "Missing code or data.");
        }
        boolean saved = store.savePlayer(code, data);
        return saved
                ? ServerMessage.ok("savePlayer", "Player saved.")
                : ServerMessage.error("savePlayer", "Failed to save player.");
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) return null;
        return code.trim().toUpperCase();
    }
}
