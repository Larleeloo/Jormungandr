package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-generated branching linked list of all 80,000 rooms across 8 regions.
 * Built once at startup using the deterministic WORLD_SEED so every client
 * produces an identical room layout.
 *
 * The mesh is a graph of {@link RoomNode} objects where each node holds a
 * room number and references 1-4 neighboring rooms by ID. Room content
 * (creatures, loot, etc.) is NOT stored here — only the structural layout.
 *
 * Layout per region:
 *   - Trunk: rooms 0 through TRUNK_LENGTH-1 form a linear spine.
 *   - Branches: rooms BRANCH_ID_START+ grow off trunk rooms and sub-branch
 *     recursively up to MAX_BRANCH_DEPTH.
 *   - Cross-region links: small chance any branch leads to another region.
 *
 * When a player enters a room the game checks Drive cloud storage for room
 * data first. If none exists, data is generated based on the room's position
 * in this mesh.
 */
public class WorldMesh {

    private static WorldMesh instance;

    /** All room nodes keyed by room ID (e.g. "r1_00050"). */
    private final Map<String, RoomNode> nodes = new HashMap<>();

    /** Tracks the next branch room ID to allocate per region during build. */
    private int nextBranchId;

    private WorldMesh() {
        buildMesh();
    }

    public static synchronized WorldMesh getInstance() {
        if (instance == null) {
            instance = new WorldMesh();
        }
        return instance;
    }

    /**
     * Look up a room node by its ID. Returns null if the room is not part
     * of the pre-generated mesh (shouldn't happen for valid room IDs).
     */
    public RoomNode getNode(String roomId) {
        return nodes.get(roomId);
    }

    /**
     * Get the neighbor map for a room. Returns an empty map if the room
     * is not in the mesh.
     */
    public Map<Direction, String> getNeighbors(String roomId) {
        RoomNode node = nodes.get(roomId);
        if (node == null) return new HashMap<>();
        return node.getNeighbors();
    }

    /**
     * Get the neighbor map for a room by region and room number.
     */
    public Map<Direction, String> getNeighbors(int region, int roomNumber) {
        return getNeighbors(RoomIdHelper.makeRoomId(region, roomNumber));
    }

    /**
     * Check if a room exists in the pre-generated mesh.
     */
    public boolean hasRoom(String roomId) {
        return nodes.containsKey(roomId);
    }

    /**
     * Total number of rooms in the mesh.
     */
    public int getTotalRoomCount() {
        return nodes.size();
    }

    /**
     * Number of rooms in a specific region.
     */
    public int getRegionRoomCount(int region) {
        int count = 0;
        for (RoomNode node : nodes.values()) {
            if (node.getRegion() == region) count++;
        }
        return count;
    }

    // ---- Mesh construction ----

    private void buildMesh() {
        buildHub();
        for (int region = 1; region <= Constants.NUM_REGIONS; region++) {
            buildRegion(region);
        }
    }

    private void buildHub() {
        RoomNode hub = new RoomNode(0, 0);
        // Hub connects to the first trunk room of regions 1, 2, and 8
        hub.addNeighbor(Direction.FORWARD, RoomIdHelper.makeRoomId(1, 0));
        hub.addNeighbor(Direction.LEFT, RoomIdHelper.makeRoomId(8, 0));
        hub.addNeighbor(Direction.RIGHT, RoomIdHelper.makeRoomId(2, 0));
        putNode(hub);
    }

    private void buildRegion(int region) {
        // Seed RNG per region so each region's layout is deterministic and independent
        SeededRandom rng = new SeededRandom(Constants.WORLD_SEED ^ ((long) region << 48));
        nextBranchId = Constants.BRANCH_ID_START;

        // Build trunk (linear spine)
        for (int i = 0; i < Constants.TRUNK_LENGTH; i++) {
            buildTrunkRoom(region, i, rng);
        }

        // Second pass: fill remaining room slots with extra branches off trunk rooms
        // to ensure all regions approach ROOMS_PER_REGION connected rooms
        if (nextBranchId < Constants.ROOMS_PER_REGION) {
            fillRemainingRooms(region, rng);
        }
    }

    private void buildTrunkRoom(int region, int trunkIndex, SeededRandom rng) {
        RoomNode node = new RoomNode(region, trunkIndex);

        // BACK: previous trunk room, or hub if at room 0
        if (trunkIndex == 0) {
            node.addNeighbor(Direction.BACK, RoomIdHelper.makeRoomId(0, 0));
        } else {
            node.addNeighbor(Direction.BACK, RoomIdHelper.makeRoomId(region, trunkIndex - 1));
        }

        // FORWARD: next trunk room (unless end of trunk)
        if (trunkIndex < Constants.TRUNK_LENGTH - 1) {
            node.addNeighbor(Direction.FORWARD, RoomIdHelper.makeRoomId(region, trunkIndex + 1));
        }

        // LEFT branch
        if (rng.nextDouble() < Constants.BRANCH_CHANCE && nextBranchId < Constants.ROOMS_PER_REGION) {
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                node.addNeighbor(Direction.LEFT, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                int branchRoot = nextBranchId;
                node.addNeighbor(Direction.LEFT, RoomIdHelper.makeRoomId(region, branchRoot));
                buildBranch(region, branchRoot, trunkIndex, 1, rng);
            }
        }

        // RIGHT branch
        if (rng.nextDouble() < Constants.BRANCH_CHANCE && nextBranchId < Constants.ROOMS_PER_REGION) {
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                node.addNeighbor(Direction.RIGHT, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                int branchRoot = nextBranchId;
                node.addNeighbor(Direction.RIGHT, RoomIdHelper.makeRoomId(region, branchRoot));
                buildBranch(region, branchRoot, trunkIndex, 1, rng);
            }
        }

        putNode(node);
    }

    /**
     * Recursively build a branch chain. Each branch room gets a BACK door to its
     * parent, may continue FORWARD deeper, and may sub-branch LEFT/RIGHT.
     */
    private void buildBranch(int region, int roomNumber, int parentRoom, int depth, SeededRandom rng) {
        if (nextBranchId <= roomNumber) {
            nextBranchId = roomNumber + 1;
        }

        RoomNode node = new RoomNode(region, roomNumber);
        node.addNeighbor(Direction.BACK, RoomIdHelper.makeRoomId(region, parentRoom));

        // FORWARD: continue deeper
        if (depth < Constants.MAX_BRANCH_DEPTH && nextBranchId < Constants.ROOMS_PER_REGION) {
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                node.addNeighbor(Direction.FORWARD, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                int nextRoom = nextBranchId;
                nextBranchId++;
                node.addNeighbor(Direction.FORWARD, RoomIdHelper.makeRoomId(region, nextRoom));
                buildBranch(region, nextRoom, roomNumber, depth + 1, rng);
            }
        }

        // Sub-branch LEFT
        if (depth < Constants.MAX_BRANCH_DEPTH - 5
                && nextBranchId < Constants.ROOMS_PER_REGION
                && rng.nextDouble() < Constants.SUB_BRANCH_CHANCE) {
            int subBranch = nextBranchId;
            nextBranchId++;
            node.addNeighbor(Direction.LEFT, RoomIdHelper.makeRoomId(region, subBranch));
            buildBranch(region, subBranch, roomNumber, depth + 1, rng);
        }

        // Sub-branch RIGHT
        if (depth < Constants.MAX_BRANCH_DEPTH - 5
                && nextBranchId < Constants.ROOMS_PER_REGION
                && rng.nextDouble() < Constants.SUB_BRANCH_CHANCE) {
            int subBranch = nextBranchId;
            nextBranchId++;
            node.addNeighbor(Direction.RIGHT, RoomIdHelper.makeRoomId(region, subBranch));
            buildBranch(region, subBranch, roomNumber, depth + 1, rng);
        }

        putNode(node);
    }

    /**
     * After trunk + initial branches are built, fill remaining room slots with
     * additional branches off existing leaf nodes (rooms with fewer than 4 neighbors).
     * This ensures every region uses close to ROOMS_PER_REGION rooms and all are connected.
     */
    private void fillRemainingRooms(int region, SeededRandom rng) {
        // Collect existing branch nodes that can still grow (have < 4 neighbors)
        String regionPrefix = "r" + region + "_";
        java.util.List<RoomNode> growable = new java.util.ArrayList<>();
        for (RoomNode node : nodes.values()) {
            if (node.getRegion() == region
                    && node.getNeighborCount() < 4
                    && node.getRoomNumber() >= Constants.BRANCH_ID_START) {
                growable.add(node);
            }
        }

        int growIdx = 0;
        while (nextBranchId < Constants.ROOMS_PER_REGION && !growable.isEmpty()) {
            RoomNode parent = growable.get(growIdx % growable.size());
            growIdx++;

            // Find an open direction on the parent
            Direction openDir = null;
            for (Direction d : new Direction[]{Direction.FORWARD, Direction.LEFT, Direction.RIGHT}) {
                if (!parent.hasNeighbor(d)) {
                    openDir = d;
                    break;
                }
            }
            if (openDir == null) {
                // This node is full, remove it from growable
                growable.remove(parent);
                if (growable.isEmpty()) break;
                continue;
            }

            int newRoom = nextBranchId;
            nextBranchId++;

            parent.addNeighbor(openDir, RoomIdHelper.makeRoomId(region, newRoom));

            RoomNode newNode = new RoomNode(region, newRoom);
            newNode.addNeighbor(Direction.BACK, parent.getRoomId());
            putNode(newNode);
            growable.add(newNode);
        }
    }

    /**
     * Roll for a cross-region door. Returns target region (1-8) or -1 if none.
     */
    private int rollCrossRegion(int region, SeededRandom rng) {
        if (region == 0) return -1;
        if (rng.nextDouble() < Constants.CROSS_REGION_CHANCE) {
            int target;
            do {
                target = rng.nextIntRange(1, Constants.NUM_REGIONS);
            } while (target == region);
            return target;
        }
        return -1;
    }

    private void putNode(RoomNode node) {
        nodes.put(node.getRoomId(), node);
    }
}
