package com.larleeloo.jormungandr.server.handler;

import com.google.gson.Gson;
import com.larleeloo.jormungandr.server.config.ServerConstants;
import com.larleeloo.jormungandr.server.model.ClientMessage;
import com.larleeloo.jormungandr.server.model.ServerMessage;
import com.larleeloo.jormungandr.server.websocket.SessionRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Routes incoming WebSocket messages to the appropriate handler based
 * on the message "type" field. Mirrors the switch statement in Code.gs doPost().
 *
 * Authentication is enforced here: all messages except "authenticate" require
 * the session to be registered in the SessionRegistry.
 */
@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);
    private final Gson gson = new Gson();

    private final SessionRegistry registry;
    private final PlayerHandler playerHandler;
    private final RoomHandler roomHandler;
    private final NoteHandler noteHandler;
    private final TradeHandler tradeHandler;
    private final ProximityHandler proximityHandler;
    private final TurnHandler turnHandler;
    private final AdminHandler adminHandler;

    public MessageRouter(SessionRegistry registry,
                         PlayerHandler playerHandler,
                         RoomHandler roomHandler,
                         NoteHandler noteHandler,
                         TradeHandler tradeHandler,
                         ProximityHandler proximityHandler,
                         TurnHandler turnHandler,
                         AdminHandler adminHandler) {
        this.registry = registry;
        this.playerHandler = playerHandler;
        this.roomHandler = roomHandler;
        this.noteHandler = noteHandler;
        this.tradeHandler = tradeHandler;
        this.proximityHandler = proximityHandler;
        this.turnHandler = turnHandler;
        this.adminHandler = adminHandler;
    }

    /**
     * Parse and route a raw JSON message. Returns the JSON response string.
     */
    public String route(WebSocketSession session, String rawJson) {
        ClientMessage msg;
        try {
            msg = gson.fromJson(rawJson, ClientMessage.class);
        } catch (Exception e) {
            return toJson(ServerMessage.error("error", "Invalid JSON: " + e.getMessage()));
        }

        if (msg.getType() == null) {
            return toJson(ServerMessage.error("error", "Missing 'type' field"));
        }

        // Authentication gate
        if ("authenticate".equals(msg.getType())) {
            return toJson(handleAuthenticate(session, msg));
        }

        // All other messages require an authenticated session
        String callerCode = registry.getAccessCode(session);
        if (callerCode == null) {
            return toJson(ServerMessage.error(msg.getType(), "Not authenticated. Send 'authenticate' first."));
        }

        // Route to handler
        ServerMessage response = switch (msg.getType()) {
            // Player
            case "validateCode" -> playerHandler.validateCode(msg);
            case "getPlayer"    -> playerHandler.getPlayer(msg);
            case "savePlayer"   -> playerHandler.savePlayer(msg);

            // Room
            case "getRoom"   -> roomHandler.getRoom(msg);
            case "saveRoom"  -> roomHandler.saveRoom(msg);
            case "listRooms" -> roomHandler.listRooms(msg);

            // Notes
            case "getNotes" -> noteHandler.getNotes(msg);
            case "saveNote" -> noteHandler.saveNote(msg);

            // Trades
            case "getTrades"  -> tradeHandler.getTrades(msg);
            case "saveTrades" -> tradeHandler.saveTrades(msg);

            // Proximity & actions
            case "getNearbyPlayers" -> proximityHandler.getNearbyPlayers(msg, registry);
            case "recordAction"     -> proximityHandler.recordAction(msg, registry);
            case "getRecentActions" -> proximityHandler.getRecentActions(msg);
            case "cleanupActions"   -> proximityHandler.cleanupActions(msg);

            // Turn queue
            case "joinTurnQueue" -> turnHandler.joinTurnQueue(msg, registry);
            case "endTurn"       -> turnHandler.endTurn(msg, registry);
            case "leaveTurnQueue"-> turnHandler.leaveTurnQueue(msg, registry);
            case "getTurnState"  -> turnHandler.getTurnState(msg);

            // Admin
            case "adminResetAllRooms"   -> adminHandler.resetAllRooms(msg);
            case "adminResetAllNotes"   -> adminHandler.resetAllNotes(msg);
            case "adminResetAllPlayers" -> adminHandler.resetAllPlayers(msg);
            case "adminResetAllTrades"  -> adminHandler.resetAllTrades(msg);
            case "adminResetAllActions" -> adminHandler.resetAllActions(msg);

            // Version
            case "getVersion" -> ServerMessage.ok("getVersion", "OK", ServerConstants.GAME_VERSION);

            default -> ServerMessage.error(msg.getType(), "Unknown message type: " + msg.getType());
        };

        return toJson(response);
    }

    /**
     * Called when a client disconnects. Cleans up turn queues and proximity state.
     */
    public void handleDisconnect(String accessCode) {
        log.info("Cleaning up state for disconnected player: {}", accessCode);
        turnHandler.handleDisconnect(accessCode);
        proximityHandler.handleDisconnect(accessCode);
    }

    private ServerMessage handleAuthenticate(WebSocketSession session, ClientMessage msg) {
        String code = msg.getCode();
        if (code == null || code.isBlank()) {
            return ServerMessage.error("authenticated", "No access code provided.");
        }
        code = code.trim().toUpperCase();
        if (!ServerConstants.VALID_CODES.contains(code)) {
            return ServerMessage.error("authenticated", "Invalid access code.");
        }

        registry.register(code, session);
        return ServerMessage.ok("authenticated", "Welcome, " + code + "!");
    }

    private String toJson(ServerMessage msg) {
        return gson.toJson(msg);
    }
}
