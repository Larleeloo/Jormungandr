package com.larleeloo.jormungandr.model;

/**
 * Lightweight snapshot of another player detected in proximity.
 * Returned by the cloud getNearbyPlayers API and consumed by ProximityManager.
 */
public class NearbyPlayer {
    private String accessCode;
    private String name;
    private String roomId;
    private int level;
    /** Manhattan distance from the local player. 0 = same room. */
    private int distance;

    public NearbyPlayer() {}

    public NearbyPlayer(String accessCode, String name, String roomId, int level, int distance) {
        this.accessCode = accessCode;
        this.name = name;
        this.roomId = roomId;
        this.level = level;
        this.distance = distance;
    }

    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }

    /** True when both players are in the exact same room. */
    public boolean isSameRoom() { return distance == 0; }
}
