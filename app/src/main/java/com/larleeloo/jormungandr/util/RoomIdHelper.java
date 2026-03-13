package com.larleeloo.jormungandr.util;

/**
 * Helpers for room ID parsing and grid coordinate calculations.
 * Room IDs follow the format "r{region}_{5-digit-number}" (e.g. "r1_00050").
 * In a 100x100 grid, room number = row * GRID_SIZE + col.
 */
public final class RoomIdHelper {
    private RoomIdHelper() {}

    /**
     * Number of waypoints scattered per region.
     * They are placed at seeded-random positions, never at the region entrance (0,0).
     */
    private static final int WAYPOINTS_PER_REGION = 50;

    /**
     * Pre-computed waypoint room numbers per region (lazily built).
     * waypointSets[region] is a sorted array of room numbers that are waypoints.
     */
    private static final int[][] waypointSets = new int[Constants.NUM_REGIONS + 1][];

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
     * Closer to origin = easier, farther = harder.
     */
    public static int getDifficultyTier(int region, int roomNumber) {
        int row = getRow(roomNumber);
        int col = getCol(roomNumber);
        int dist = row + col; // Manhattan distance from (0,0)
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
     * Waypoints are placed at seeded-random positions within each region.
     * There are ~50 per region, scattered throughout the grid so that
     * players must explore to find them. The region entrance (0,0) is NOT
     * a waypoint.
     */
    public static boolean isWaypoint(int roomNumber) {
        // Hub room is always a waypoint
        return false; // region-unaware overload — use the region-aware version
    }

    public static boolean isWaypoint(int region, int roomNumber) {
        if (region <= 0) return false;
        if (region > Constants.NUM_REGIONS) return false;
        // Room (0,0) is the region entrance — never a waypoint
        if (roomNumber == 0) return false;

        int[] set = getWaypointSet(region);
        return java.util.Arrays.binarySearch(set, roomNumber) >= 0;
    }

    public static boolean isWaypoint(String roomId) {
        return isWaypoint(getRegion(roomId), getRoomNumber(roomId));
    }

    /**
     * Get the pre-computed waypoint room numbers for a region.
     */
    public static int[] getWaypointSet(int region) {
        if (region < 1 || region > Constants.NUM_REGIONS) return new int[0];
        if (waypointSets[region] == null) {
            waypointSets[region] = buildWaypointSet(region);
        }
        return waypointSets[region];
    }

    /**
     * Build waypoint positions deterministically using a seeded LCG.
     * Scatters WAYPOINTS_PER_REGION waypoints across the grid, avoiding (0,0)
     * and ensuring no duplicates.
     */
    private static int[] buildWaypointSet(int region) {
        // Simple seeded LCG for deterministic placement
        long seed = Constants.WORLD_SEED ^ ((long) region << 32) ^ WAYPOINT_SEED_MIX;

        java.util.Set<Integer> positions = new java.util.LinkedHashSet<>();
        int totalCells = Constants.GRID_SIZE * Constants.GRID_SIZE;
        int attempts = 0;

        while (positions.size() < WAYPOINTS_PER_REGION && attempts < WAYPOINTS_PER_REGION * 10) {
            seed = lcgNext(seed);
            int candidate = (int) ((seed >>> 16) % totalCells);
            // Skip the entrance room (0,0) = room number 0
            if (candidate == 0) { attempts++; continue; }
            // Ensure some minimum spacing (Manhattan distance >= 5 from all existing)
            int row = getRow(candidate);
            int col = getCol(candidate);
            boolean tooClose = false;
            for (int existing : positions) {
                int er = getRow(existing);
                int ec = getCol(existing);
                if (Math.abs(row - er) + Math.abs(col - ec) < 5) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                positions.add(candidate);
            }
            attempts++;
        }

        int[] result = new int[positions.size()];
        int i = 0;
        for (int p : positions) result[i++] = p;
        java.util.Arrays.sort(result);
        return result;
    }

    // Hex literal workaround: 0xWAYPOINT doesn't compile, use a constant
    private static final long WAYPOINT_SEED_MIX = 0x574159504F494E54L;

    private static long lcgNext(long seed) {
        return seed * 6364136223846793005L + 1442695040888963407L;
    }

    public static boolean isHub(String roomId) {
        return roomId != null && roomId.startsWith("r0_");
    }
}
