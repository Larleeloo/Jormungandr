package com.larleeloo.jormungandr.util;

/**
 * Helpers for room ID parsing and grid coordinate calculations.
 * Room IDs follow the format "r{region}_{5-digit-number}" (e.g. "r1_00050").
 * In a 100x100 grid, room number = row * GRID_SIZE + col.
 */
public final class RoomIdHelper {
    private RoomIdHelper() {}

    /**
     * Pre-computed single waypoint room number per region (lazily built).
     * Index 0 unused (hub). Indices 1-8 hold the waypoint for that region.
     * A value of -1 means not yet computed.
     */
    private static final int[] waypointRoomNumbers = new int[Constants.NUM_REGIONS + 1];
    static {
        java.util.Arrays.fill(waypointRoomNumbers, -1);
    }

    public static String makeRoomId(int region, int number) {
        return String.format("r%d_%05d", region, number);
    }

    public static String makeRoomId(int region, int row, int col) {
        return makeRoomId(region, row * Constants.GRID_SIZE + col);
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

    /** Grid row (0-99) from room number. */
    public static int getRow(int roomNumber) {
        return roomNumber / Constants.GRID_SIZE;
    }

    /** Grid column (0-99) from room number. */
    public static int getCol(int roomNumber) {
        return roomNumber % Constants.GRID_SIZE;
    }

    public static int getRow(String roomId) {
        return getRow(getRoomNumber(roomId));
    }

    public static int getCol(String roomId) {
        return getCol(getRoomNumber(roomId));
    }

    /** Room number from grid coordinates. */
    public static int toRoomNumber(int row, int col) {
        return row * Constants.GRID_SIZE + col;
    }

    /**
     * Difficulty tier (1-5) based on Manhattan distance from the grid origin (0,0).
     */
    public static int getDifficultyTier(int region, int roomNumber) {
        int row = getRow(roomNumber);
        int col = getCol(roomNumber);
        int dist = row + col;
        if (dist < 25) return 1;
        if (dist < 50) return 2;
        if (dist < 100) return 3;
        if (dist < 150) return 4;
        return 5;
    }

    /** @deprecated Use getDifficultyTier instead. */
    public static int getZone(int roomNumber) {
        return getDifficultyTier(1, roomNumber);
    }

    public static int getZone(String roomId) {
        return getDifficultyTier(getRegion(roomId), getRoomNumber(roomId));
    }

    /**
     * Each region has exactly one waypoint, placed at a seeded-random position.
     * The region entrance (0,0) is never the waypoint.
     */
    public static boolean isWaypoint(int roomNumber) {
        return false; // region-unaware overload — use the region-aware version
    }

    public static boolean isWaypoint(int region, int roomNumber) {
        if (region < 1 || region > Constants.NUM_REGIONS) return false;
        return roomNumber == getRegionWaypointNumber(region);
    }

    public static boolean isWaypoint(String roomId) {
        return isWaypoint(getRegion(roomId), getRoomNumber(roomId));
    }

    /**
     * Get the single waypoint room number for a region.
     * Deterministically placed using a seeded LCG, avoiding (0,0).
     */
    public static int getRegionWaypointNumber(int region) {
        if (region < 1 || region > Constants.NUM_REGIONS) return -1;
        if (waypointRoomNumbers[region] == -1) {
            waypointRoomNumbers[region] = computeWaypointNumber(region);
        }
        return waypointRoomNumbers[region];
    }

    /**
     * Get the single waypoint room ID for a region.
     */
    public static String getRegionWaypointId(int region) {
        int num = getRegionWaypointNumber(region);
        if (num < 0) return null;
        return makeRoomId(region, num);
    }

    /**
     * For backward compat with getWaypointSet() callers.
     * Returns a single-element array.
     */
    public static int[] getWaypointSet(int region) {
        int wp = getRegionWaypointNumber(region);
        if (wp < 0) return new int[0];
        return new int[]{wp};
    }

    /**
     * Compute the waypoint position for a region using seeded LCG.
     * The waypoint is placed at a random position NOT at (0,0) and NOT
     * too close to (0,0) — minimum Manhattan distance of 15 from the entrance.
     */
    private static int computeWaypointNumber(int region) {
        long seed = Constants.WORLD_SEED ^ ((long) region << 32) ^ WAYPOINT_SEED_MIX;
        int totalCells = Constants.GRID_SIZE * Constants.GRID_SIZE;

        for (int attempt = 0; attempt < 1000; attempt++) {
            seed = lcgNext(seed);
            int candidate = (int) ((seed >>> 16) % totalCells);
            if (candidate == 0) continue;
            int row = getRow(candidate);
            int col = getCol(candidate);
            // Ensure waypoint is not too close to the entrance
            if (row + col >= 15) return candidate;
        }
        // Fallback: center of grid
        return toRoomNumber(50, 50);
    }

    private static final long WAYPOINT_SEED_MIX = 0x574159504F494E54L;

    private static long lcgNext(long seed) {
        return seed * 6364136223846793005L + 1442695040888963407L;
    }

    public static boolean isHub(String roomId) {
        return roomId != null && roomId.startsWith("r0_");
    }
}
