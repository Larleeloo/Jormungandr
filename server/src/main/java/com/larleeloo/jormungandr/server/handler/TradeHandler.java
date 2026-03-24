package com.larleeloo.jormungandr.server.handler;

import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.TradeStore;

import org.springframework.stereotype.Component;

/**
 * Handles trade-related messages: get and save trade listings.
 *
 * Mirrors the Apps Script handlers:
 *   handleGetTrades, handleSaveTrades
 */
@Component
public class TradeHandler {

    private final TradeStore store;

    public TradeHandler(TradeStore store) {
        this.store = store;
    }

    public ServerMessage getTrades(ClientMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return ServerMessage.error("getTrades", "No roomId provided.");
        }
        String data = store.getTrades(roomId.trim());
        return ServerMessage.ok("getTrades", "Trades loaded.", data);
    }

    public ServerMessage saveTrades(ClientMessage msg) {
        String roomId = msg.getRoomId();
        String data = msg.getData();
        if (roomId == null || roomId.isBlank() || data == null || data.isBlank()) {
            return ServerMessage.error("saveTrades", "Missing roomId or data.");
        }
        boolean saved = store.saveTrades(roomId.trim(), data);
        return saved
                ? ServerMessage.ok("saveTrades", "Trades saved.")
                : ServerMessage.error("saveTrades", "Failed to save trades.");
    }
}
