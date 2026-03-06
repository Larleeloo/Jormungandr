package com.example.jormungandr.model;

public class PlayerNote {
    private String playerName;
    private String text;
    private long timestamp;
    private String roomId;

    public PlayerNote() {}

    public PlayerNote(String playerName, String text, String roomId) {
        this.playerName = playerName;
        this.text = text;
        this.roomId = roomId;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
