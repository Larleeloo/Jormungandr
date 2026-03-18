package com.larleeloo.jormungandr.model;

/**
 * A single timestamped action recorded during player co-location.
 * Stored per-room in the cloud so co-located players can see each other's actions.
 */
public class TimestampedAction {
    private String playerName;
    private String accessCode;
    private String actionText;
    private long timestamp;
    private String roomId;

    public TimestampedAction() {}

    public TimestampedAction(String playerName, String accessCode, String actionText, String roomId) {
        this.playerName = playerName;
        this.accessCode = accessCode;
        this.actionText = actionText;
        this.roomId = roomId;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
