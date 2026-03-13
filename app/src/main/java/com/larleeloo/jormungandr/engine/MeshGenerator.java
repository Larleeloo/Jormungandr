package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Door connection lookup backed by the pre-generated {@link WorldMesh}.
 * The mesh defines a grid maze for each region; this class simply
 * looks up the connections for a given room.
 */
public class MeshGenerator {

    private MeshGenerator() {}

    /**
     * Look up all door connections for a given room from the pre-built WorldMesh.
     */
    public static Map<Direction, String> generateDoors(int region, int roomNumber) {
        WorldMesh mesh = WorldMesh.getInstance();
        String roomId = RoomIdHelper.makeRoomId(region, roomNumber);
        RoomNode node = mesh.getNode(roomId);

        if (node != null) {
            return new HashMap<>(node.getNeighbors());
        }
        return new HashMap<>();
    }
}
