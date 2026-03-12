package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Lightweight node in the pre-generated world mesh.
 * Each node stores a room number and references to 1-4 neighboring rooms
 * by their room ID strings. The mesh is built once at startup and provides
 * a deterministic, inspectable room layout for all 80,000 rooms.
 */
public class RoomNode {
    private final int region;
    private final int roomNumber;
    private final String roomId;
    private final Map<Direction, String> neighbors;

    public RoomNode(int region, int roomNumber) {
        this.region = region;
        this.roomNumber = roomNumber;
        this.roomId = RoomIdHelper.makeRoomId(region, roomNumber);
        this.neighbors = new EnumMap<>(Direction.class);
    }

    public void addNeighbor(Direction direction, String targetRoomId) {
        neighbors.put(direction, targetRoomId);
    }

    public String getNeighbor(Direction direction) {
        return neighbors.get(direction);
    }

    public boolean hasNeighbor(Direction direction) {
        return neighbors.containsKey(direction);
    }

    public Map<Direction, String> getNeighbors() {
        return Collections.unmodifiableMap(neighbors);
    }

    public int getNeighborCount() {
        return neighbors.size();
    }

    public int getRegion() { return region; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomId() { return roomId; }
}
