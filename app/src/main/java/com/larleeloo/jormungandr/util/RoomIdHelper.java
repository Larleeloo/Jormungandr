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

    public static int getZone(int roomNumber) {
        if (roomNumber < Constants.ZONE_1_ROOMS) return 1;
        if (roomNumber < Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS) return 2;
        if (roomNumber < Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS) return 3;
        if (roomNumber < Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS + Constants.ZONE_4_ROOMS) return 4;
        return 5;
    }

    public static int getZone(String roomId) {
        return getZone(getRoomNumber(roomId));
    }

    public static boolean isWaypoint(int roomNumber) {
        return roomNumber == Constants.WAYPOINT_ROOM_ID;
    }

    public static boolean isWaypoint(String roomId) {
        return isWaypoint(getRoomNumber(roomId));
    }

    public static boolean isHub(String roomId) {
        return roomId != null && roomId.startsWith("r0_");
    }
}
