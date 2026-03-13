package com.larleeloo.jormungandr.util;

/**
 * Helpers for room ID parsing and grid coordinate calculations.
 * Room IDs follow the format "r{region}_{5-digit-number}" (e.g. "r1_00050").
 * In a 100x100 grid, room number = row * GRID_SIZE + col.
 */
public final class RoomIdHelper {
    private RoomIdHelper() {}

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
     * Waypoints are rooms at grid positions where both row and col are
     * multiples of WAYPOINT_SPACING (e.g. (0,0), (0,10), (10,0), (10,10), ...).
     * Room (0,0) IS a waypoint (the region entrance).
     */
    public static boolean isWaypoint(int roomNumber) {
        int row = getRow(roomNumber);
        int col = getCol(roomNumber);
        return row % Constants.WAYPOINT_SPACING == 0
                && col % Constants.WAYPOINT_SPACING == 0;
    }

    public static boolean isWaypoint(String roomId) {
        return isWaypoint(getRoomNumber(roomId));
    }

    public static boolean isHub(String roomId) {
        return roomId != null && roomId.startsWith("r0_");
    }
}
