package com.larleeloo.jormungandr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {
    private String roomId;
    private int region;
    private int zone;
    private String biome;
    private String backgroundId;
    private boolean isWaypoint;
    private boolean isCreatureDen;
    private Map<String, String> doors; // Direction name -> target room ID
    private List<RoomObject> objects;
    private List<PlayerNote> playerNotes;
    private String firstVisitedBy;
    private long firstVisitedAt;

    public Room() {
        this.doors = new HashMap<>();
        this.objects = new ArrayList<>();
        this.playerNotes = new ArrayList<>();
    }

    public Room(String roomId, int region, int zone) {
        this();
        this.roomId = roomId;
        this.region = region;
        this.zone = zone;
        this.biome = BiomeType.fromRegion(region).name().toLowerCase();
    }

    public void addDoor(Direction direction, String targetRoomId) {
        doors.put(direction.name(), targetRoomId);
    }

    public String getDoorTarget(Direction direction) {
        return doors.get(direction.name());
    }

    public boolean hasDoor(Direction direction) {
        return doors.containsKey(direction.name());
    }

    public List<Direction> getAvailableDirections() {
        List<Direction> directions = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (hasDoor(d)) directions.add(d);
        }
        return directions;
    }

    public boolean hasLivingCreature() {
        for (RoomObject obj : objects) {
            if ("creature".equals(obj.getType()) && obj.isAlive()) return true;
        }
        return false;
    }

    public RoomObject getFirstLivingCreature() {
        for (RoomObject obj : objects) {
            if ("creature".equals(obj.getType()) && obj.isAlive()) return obj;
        }
        return null;
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public int getRegion() { return region; }
    public void setRegion(int region) { this.region = region; }
    public int getZone() { return zone; }
    public void setZone(int zone) { this.zone = zone; }
    public String getBiome() { return biome; }
    public void setBiome(String biome) { this.biome = biome; }
    public String getBackgroundId() { return backgroundId; }
    public void setBackgroundId(String backgroundId) { this.backgroundId = backgroundId; }
    public boolean isWaypoint() { return isWaypoint; }
    public void setWaypoint(boolean waypoint) { isWaypoint = waypoint; }
    public boolean isCreatureDen() { return isCreatureDen; }
    public void setCreatureDen(boolean creatureDen) { isCreatureDen = creatureDen; }
    public Map<String, String> getDoors() { return doors; }
    public void setDoors(Map<String, String> doors) { this.doors = doors; }
    public List<RoomObject> getObjects() { return objects; }
    public void setObjects(List<RoomObject> objects) { this.objects = objects; }
    public List<PlayerNote> getPlayerNotes() { return playerNotes; }
    public void setPlayerNotes(List<PlayerNote> playerNotes) { this.playerNotes = playerNotes; }
    public String getFirstVisitedBy() { return firstVisitedBy; }
    public void setFirstVisitedBy(String firstVisitedBy) { this.firstVisitedBy = firstVisitedBy; }
    public long getFirstVisitedAt() { return firstVisitedAt; }
    public void setFirstVisitedAt(long firstVisitedAt) { this.firstVisitedAt = firstVisitedAt; }
}
