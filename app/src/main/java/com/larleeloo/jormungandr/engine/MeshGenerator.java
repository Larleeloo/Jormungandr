package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Door connection lookup backed by the pre-generated {@link WorldMesh}.
 *
 * Previously this class computed door connections on-the-fly using deterministic
 * hashing. It now delegates entirely to the WorldMesh linked list, which builds
 * all 80,000 room connections at startup. This makes the room layout inspectable
 * and ensures every room number references a real, connected node.
 */
public class MeshGenerator {

    private MeshGenerator() {}

    /**
     * Look up all door connections for a given room from the pre-built WorldMesh.
     * Returns a map of Direction -> target room ID.
     */
    public static Map<Direction, String> generateDoors(int region, int roomNumber) {
        WorldMesh mesh = WorldMesh.getInstance();
        String roomId = RoomIdHelper.makeRoomId(region, roomNumber);
        RoomNode node = mesh.getNode(roomId);

        if (node != null) {
            // Return a mutable copy so callers can modify (e.g. override BACK door)
            return new HashMap<>(node.getNeighbors());
        }

        // Fallback for rooms not in the mesh (shouldn't happen for valid IDs)
        return new HashMap<>();
    }

    /**
     * Estimate the branch depth of a room based on its position in the mesh.
     * Trunk rooms have depth 0. Branch rooms count hops back to the trunk.
     */
    static int estimateBranchDepth(int region, int roomNumber) {
        if (RoomIdHelper.isTrunkRoom(roomNumber)) return 0;

        WorldMesh mesh = WorldMesh.getInstance();
        String roomId = RoomIdHelper.makeRoomId(region, roomNumber);
        int depth = 0;
        int maxHops = 50; // safety limit

        while (depth < maxHops) {
            RoomNode node = mesh.getNode(roomId);
            if (node == null) break;

            String backTarget = node.getNeighbor(Direction.BACK);
            if (backTarget == null) break;

            depth++;
            int backNumber = RoomIdHelper.getRoomNumber(backTarget);
            if (RoomIdHelper.isTrunkRoom(backNumber)) break;

            roomId = backTarget;
        }

        return depth;
    }
}
