package com.larleeloo.jormungandr.server.handler;

import com.larleeloo.jormungandr.server.config.ServerConstants;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.store.ActionStore;
import com.larleeloo.jormungandr.server.store.NoteStore;
import com.larleeloo.jormungandr.server.store.PlayerStore;
import com.larleeloo.jormungandr.server.store.RoomStore;
import com.larleeloo.jormungandr.server.store.TradeStore;

import org.springframework.stereotype.Component;

/**
 * Handles admin reset operations. Only admin access codes (001-003)
 * can execute these.
 *
 * Mirrors the Apps Script admin handlers:
 *   handleAdminResetAllRooms, handleAdminResetAllNotes,
 *   handleAdminResetAllPlayers, handleAdminResetAllTrades,
 *   handleAdminResetAllActions
 */
@Component
public class AdminHandler {

    private final PlayerStore playerStore;
    private final RoomStore roomStore;
    private final NoteStore noteStore;
    private final TradeStore tradeStore;
    private final ActionStore actionStore;

    public AdminHandler(PlayerStore playerStore, RoomStore roomStore,
                        NoteStore noteStore, TradeStore tradeStore,
                        ActionStore actionStore) {
        this.playerStore = playerStore;
        this.roomStore = roomStore;
        this.noteStore = noteStore;
        this.tradeStore = tradeStore;
        this.actionStore = actionStore;
    }

    public ServerMessage resetAllRooms(ClientMessage msg) {
        if (!isAdmin(msg)) return unauthorized("adminResetAllRooms");
        return roomStore.deleteAll()
                ? ServerMessage.ok("adminResetAllRooms", "All rooms deleted.")
                : ServerMessage.error("adminResetAllRooms", "Failed to delete rooms.");
    }

    public ServerMessage resetAllNotes(ClientMessage msg) {
        if (!isAdmin(msg)) return unauthorized("adminResetAllNotes");
        return noteStore.deleteAll()
                ? ServerMessage.ok("adminResetAllNotes", "All notes deleted.")
                : ServerMessage.error("adminResetAllNotes", "Failed to delete notes.");
    }

    public ServerMessage resetAllPlayers(ClientMessage msg) {
        if (!isAdmin(msg)) return unauthorized("adminResetAllPlayers");
        return playerStore.deleteAll()
                ? ServerMessage.ok("adminResetAllPlayers", "All players deleted.")
                : ServerMessage.error("adminResetAllPlayers", "Failed to delete players.");
    }

    public ServerMessage resetAllTrades(ClientMessage msg) {
        if (!isAdmin(msg)) return unauthorized("adminResetAllTrades");
        return tradeStore.deleteAll()
                ? ServerMessage.ok("adminResetAllTrades", "All trades deleted.")
                : ServerMessage.error("adminResetAllTrades", "Failed to delete trades.");
    }

    public ServerMessage resetAllActions(ClientMessage msg) {
        if (!isAdmin(msg)) return unauthorized("adminResetAllActions");
        return actionStore.deleteAll()
                ? ServerMessage.ok("adminResetAllActions", "All actions deleted.")
                : ServerMessage.error("adminResetAllActions", "Failed to delete actions.");
    }

    private boolean isAdmin(ClientMessage msg) {
        String code = msg.getCode();
        if (code == null || code.isBlank()) return false;
        return ServerConstants.ADMIN_CODES.contains(code.trim().toUpperCase());
    }

    private ServerMessage unauthorized(String type) {
        return ServerMessage.error(type, "Admin access required.");
    }
}
