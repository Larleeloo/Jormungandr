package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic maze generator using trunk-and-branch (vine) topology.
 *
 * Each region has a main trunk of TRUNK_LENGTH rooms (IDs 0 to TRUNK_LENGTH-1).
 * Branches grow off trunk rooms and can sub-branch recursively up to MAX_BRANCH_DEPTH.
 * Cross-region doors can spontaneously link any two regions.
 *
 * Branch room IDs start at BRANCH_ID_START (= TRUNK_LENGTH) and are computed
 * deterministically via hashing so any client produces identical topology.
 */
public class MeshGenerator {

    private MeshGenerator() {}

    /**
     * Compute all door connections for a given room.
     * Returns a map of Direction -> target room ID.
     */
    public static Map<Direction, String> generateDoors(int region, int roomNumber) {
        Map<Direction, String> doors = new HashMap<>();

        if (region == 0) {
            generateHubDoors(doors);
            return doors;
        }

        if (RoomIdHelper.isTrunkRoom(roomNumber)) {
            generateTrunkDoors(doors, region, roomNumber);
        } else {
            generateBranchDoors(doors, region, roomNumber);
        }

        return doors;
    }

    /**
     * Generate doors for a trunk room (main spine of the region).
     * Trunk rooms form a linear chain: 0 → 1 → 2 → ... → TRUNK_LENGTH-1
     * with branches growing off to the LEFT and/or RIGHT.
     */
    private static void generateTrunkDoors(Map<Direction, String> doors, int region, int roomNumber) {
        SeededRandom rng = new SeededRandom(SeededRandom.hashSeed(region, roomNumber));

        // BACK door: previous trunk room, or hub if at room 0
        if (roomNumber == 0) {
            doors.put(Direction.BACK, RoomIdHelper.makeRoomId(0, 0));
        } else {
            doors.put(Direction.BACK, RoomIdHelper.makeRoomId(region, roomNumber - 1));
        }

        // FORWARD door: next trunk room, unless at end of trunk
        if (roomNumber < Constants.TRUNK_LENGTH - 1) {
            doors.put(Direction.FORWARD, RoomIdHelper.makeRoomId(region, roomNumber + 1));
        }

        // LEFT branch: deterministic chance based on seed
        if (rng.nextDouble() < Constants.BRANCH_CHANCE) {
            int branchTarget = computeBranchEntrance(region, roomNumber, Direction.LEFT);
            // Check for cross-region: small chance the branch leads to another region
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                // Cross-region door: connect to a random trunk room in another region
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                doors.put(Direction.LEFT, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                doors.put(Direction.LEFT, RoomIdHelper.makeRoomId(region, branchTarget));
            }
        }

        // RIGHT branch: independent roll
        if (rng.nextDouble() < Constants.BRANCH_CHANCE) {
            int branchTarget = computeBranchEntrance(region, roomNumber, Direction.RIGHT);
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                doors.put(Direction.RIGHT, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                doors.put(Direction.RIGHT, RoomIdHelper.makeRoomId(region, branchTarget));
            }
        }
    }

    /**
     * Generate doors for a branch room (off the trunk or a deeper sub-branch).
     * Branch rooms can continue deeper (FORWARD), sub-branch (LEFT/RIGHT),
     * or connect cross-region.
     */
    private static void generateBranchDoors(Map<Direction, String> doors, int region, int roomNumber) {
        SeededRandom rng = new SeededRandom(SeededRandom.hashSeed(region, roomNumber));

        // Estimate depth from the seed (consistent per room)
        int depth = estimateBranchDepth(region, roomNumber);

        // BACK door: computed from parent hash (the room that generated this branch room)
        // The actual BACK target is overridden by GameRepository.ensureBidirectionalDoors
        // and navigateToRoomInternal, so we set a placeholder that will be corrected.
        int parentId = computeParentRoom(region, roomNumber);
        doors.put(Direction.BACK, RoomIdHelper.makeRoomId(region, parentId));

        // FORWARD door: continue deeper if not at max depth
        if (depth < Constants.MAX_BRANCH_DEPTH) {
            int crossRegion = rollCrossRegion(region, rng);
            if (crossRegion > 0) {
                int targetTrunk = rng.nextIntRange(0, Constants.TRUNK_LENGTH - 1);
                doors.put(Direction.FORWARD, RoomIdHelper.makeRoomId(crossRegion, targetTrunk));
            } else {
                int nextRoom = computeBranchNext(region, roomNumber);
                doors.put(Direction.FORWARD, RoomIdHelper.makeRoomId(region, nextRoom));
            }
        }

        // Sub-branches (LEFT/RIGHT) with lower probability, only if not too deep
        if (depth < Constants.MAX_BRANCH_DEPTH - 5 && rng.nextDouble() < Constants.SUB_BRANCH_CHANCE) {
            int subBranch = computeBranchEntrance(region, roomNumber, Direction.LEFT);
            doors.put(Direction.LEFT, RoomIdHelper.makeRoomId(region, subBranch));
        }
        if (depth < Constants.MAX_BRANCH_DEPTH - 5 && rng.nextDouble() < Constants.SUB_BRANCH_CHANCE) {
            int subBranch = computeBranchEntrance(region, roomNumber, Direction.RIGHT);
            doors.put(Direction.RIGHT, RoomIdHelper.makeRoomId(region, subBranch));
        }
    }

    private static void generateHubDoors(Map<Direction, String> doors) {
        doors.put(Direction.FORWARD, RoomIdHelper.makeRoomId(1, 0));
        doors.put(Direction.LEFT, RoomIdHelper.makeRoomId(8, 0));
        doors.put(Direction.RIGHT, RoomIdHelper.makeRoomId(2, 0));
    }

    // ---- Deterministic ID computation ----

    /**
     * Compute the branch entrance room ID for a given parent room and direction.
     * Uses hashing to produce a stable ID in the branch range [BRANCH_ID_START, ROOMS_PER_REGION).
     */
    private static int computeBranchEntrance(int region, int parentRoomNumber, Direction dir) {
        long seed = Constants.WORLD_SEED ^ ((long) region << 40)
                ^ ((long) parentRoomNumber << 16) ^ (long) dir.ordinal();
        int branchRange = Constants.ROOMS_PER_REGION - Constants.BRANCH_ID_START;
        int offset = (int) ((Math.abs(seed * 2654435761L) >> 8) % branchRange);
        return Constants.BRANCH_ID_START + offset;
    }

    /**
     * Compute the next room deeper along a branch.
     */
    private static int computeBranchNext(int region, int currentRoomNumber) {
        long seed = Constants.WORLD_SEED ^ ((long) region << 40)
                ^ ((long) currentRoomNumber << 16) ^ 0xFACEL;
        int branchRange = Constants.ROOMS_PER_REGION - Constants.BRANCH_ID_START;
        int offset = (int) ((Math.abs(seed * 2654435761L) >> 8) % branchRange);
        int next = Constants.BRANCH_ID_START + offset;
        // Avoid self-loops
        if (next == currentRoomNumber) {
            next = Constants.BRANCH_ID_START + ((offset + 1) % branchRange);
        }
        return next;
    }

    /**
     * Compute a plausible parent room for a branch room.
     * This is a best-effort reverse lookup used for the initial BACK door.
     * The real BACK door is corrected by GameRepository when the room is first visited.
     */
    private static int computeParentRoom(int region, int branchRoomNumber) {
        long seed = Constants.WORLD_SEED ^ ((long) region << 40)
                ^ ((long) branchRoomNumber << 20) ^ 0xBAC0L;
        // Point back toward a trunk room as a reasonable default
        return (int) (Math.abs(seed) % Constants.TRUNK_LENGTH);
    }

    /**
     * Estimate the branch depth of a room (how many hops from the trunk).
     * Uses seeded RNG for consistency — the same room always gets the same depth.
     */
    static int estimateBranchDepth(int region, int roomNumber) {
        if (RoomIdHelper.isTrunkRoom(roomNumber)) return 0;
        SeededRandom depthRng = new SeededRandom(
                Constants.WORLD_SEED ^ ((long) region << 36) ^ ((long) roomNumber * 0x9E3779B9L));
        return depthRng.nextIntRange(1, Constants.MAX_BRANCH_DEPTH);
    }

    /**
     * Roll for a cross-region door. Returns target region (1-8) or -1 if none.
     * Any region can connect to any other region.
     */
    static int rollCrossRegion(int region, SeededRandom rng) {
        if (region == 0) return -1;
        if (rng.nextDouble() < Constants.CROSS_REGION_CHANCE) {
            // Pick any other region 1-8
            int target;
            do {
                target = rng.nextIntRange(1, Constants.NUM_REGIONS);
            } while (target == region);
            return target;
        }
        return -1;
    }
}
