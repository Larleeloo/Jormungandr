package com.larleeloo.jormungandr.util;

public final class RoomIdHelper {
    private RoomIdHelper() {}

    public static String makeRoomId(int region, int number) {
        return String.format("r%d_%05d", region, number);
    }

    public static int getRegion(String roomId) {
        try {
            int underscoreIdx = roomId.indexOf('_');
            return Integer.parseInt(roomId.substring(1, underscoreIdx));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getRoomNumber(String roomId) {
        try {
            int underscoreIdx = roomId.indexOf('_');
            return Integer.parseInt(roomId.substring(underscoreIdx + 1));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns true if the room is on the main trunk (IDs 0 to TRUNK_LENGTH-1).
     */
    public static boolean isTrunkRoom(int roomNumber) {
        return roomNumber < Constants.TRUNK_LENGTH;
    }

    public static boolean isTrunkRoom(String roomId) {
        return isTrunkRoom(getRoomNumber(roomId));
    }

    /**
     * Compute a difficulty tier (1-5) based on position in the tree.
     * Trunk rooms scale from tier 1 (start) to tier 3 (end).
     * Branch rooms scale from tier 2 (shallow) to tier 5 (deep).
     * The zone field on Room is populated with this value for backward compat.
     */
    public static int getDifficultyTier(int region, int roomNumber) {
        if (isTrunkRoom(roomNumber)) {
            // Trunk: tier 1 for first third, tier 2 for middle, tier 3 for last third
            float progress = (float) roomNumber / Constants.TRUNK_LENGTH;
            if (progress < 0.33f) return 1;
            if (progress < 0.66f) return 2;
            return 3;
        }
        // Branch rooms: estimate depth from how far the ID is from BRANCH_ID_START
        // This is an approximation; actual depth is tracked by MeshGenerator
        // We use a hash-based approach to get a stable "depth" value
        SeededRandom depthRng = new SeededRandom(SeededRandom.hashSeed(region, roomNumber));
        int estimatedDepth = depthRng.nextIntRange(1, Constants.MAX_BRANCH_DEPTH);
        if (estimatedDepth <= 10) return 2;
        if (estimatedDepth <= 20) return 3;
        if (estimatedDepth <= 30) return 4;
        return 5;
    }

    /**
     * @deprecated Use getDifficultyTier instead. Kept for backward compat with Room.zone field.
     */
    public static int getZone(int roomNumber) {
        // Map to difficulty tier using a default region of 1
        return getDifficultyTier(1, roomNumber);
    }

    public static int getZone(String roomId) {
        return getDifficultyTier(getRegion(roomId), getRoomNumber(roomId));
    }

    /**
     * Returns true if this trunk room is a waypoint (multiples of WAYPOINT_INTERVAL).
     * Room 0 is NOT a waypoint (that's the region entrance from hub).
     */
    public static boolean isWaypoint(int roomNumber) {
        return isTrunkRoom(roomNumber) && roomNumber > 0
                && roomNumber % Constants.WAYPOINT_INTERVAL == 0;
    }

    public static boolean isWaypoint(String roomId) {
        return isWaypoint(getRoomNumber(roomId));
    }

    public static boolean isHub(String roomId) {
        return roomId != null && roomId.startsWith("r0_");
    }
}
