package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic maze generator. Given a region and room number, computes which doors exist
 * and where they lead. Uses seeded RNG so any client produces identical topology.
 *
 * Rooms are organized in 5 concentric zones per region:
 * - Zone 1 (hub-adjacent): rooms 0-499, dense connections (3-4 doors)
 * - Zone 2: rooms 500-1999
 * - Zone 3: rooms 2000-3999
 * - Zone 4: rooms 4000-6499
 * - Zone 5 (deepest): rooms 6500-9999, sparse connections (1-2 doors)
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
            // Hub room - has 8 doors leading to region 1-8 first rooms
            generateHubDoors(doors);
            return doors;
        }

        SeededRandom rng = new SeededRandom(SeededRandom.hashSeed(region, roomNumber));
        int zone = RoomIdHelper.getZone(roomNumber);

        // Determine number of doors based on zone
        int minDoors, maxDoors;
        switch (zone) {
            case 1: minDoors = 2; maxDoors = 4; break;
            case 2: minDoors = 2; maxDoors = 3; break;
            case 3: minDoors = 2; maxDoors = 3; break;
            case 4: minDoors = 1; maxDoors = 3; break;
            case 5: minDoors = 1; maxDoors = 2; break;
            default: minDoors = 2; maxDoors = 3; break;
        }

        int numDoors = rng.nextIntRange(minDoors, maxDoors);

        // Always have a back door (except room 0 which goes to hub)
        if (roomNumber == 0) {
            doors.put(Direction.BACK, RoomIdHelper.makeRoomId(0, 0)); // Back to hub
        } else {
            // Connect back to a room in the same or lower zone
            int backTarget = computeBackTarget(region, roomNumber, rng);
            doors.put(Direction.BACK, RoomIdHelper.makeRoomId(region, backTarget));
        }

        // Generate forward/left/right doors to rooms in same or higher zone
        int remainingDoors = numDoors - 1; // -1 for the back door

        Direction[] forwardDirs = {Direction.FORWARD, Direction.LEFT, Direction.RIGHT};
        boolean[] assigned = new boolean[3];

        for (int i = 0; i < remainingDoors && i < 3; i++) {
            int dirIdx;
            if (i == 0) {
                dirIdx = 0; // Always try forward first
            } else {
                // Pick random remaining direction
                do {
                    dirIdx = rng.nextInt(3);
                } while (assigned[dirIdx]);
            }
            assigned[dirIdx] = true;

            int target = computeForwardTarget(region, roomNumber, zone, rng);
            doors.put(forwardDirs[dirIdx], RoomIdHelper.makeRoomId(region, target));
        }

        return doors;
    }

    private static void generateHubDoors(Map<Direction, String> doors) {
        // Hub has a special layout: forward leads to region 1, others fan out
        // We encode the 8 region entries into available directions plus room 0 of each region
        // For simplicity, forward = region 1, and the hub fragment handles the full 8-door UI
        doors.put(Direction.FORWARD, RoomIdHelper.makeRoomId(1, 0));
        doors.put(Direction.LEFT, RoomIdHelper.makeRoomId(8, 0));
        doors.put(Direction.RIGHT, RoomIdHelper.makeRoomId(2, 0));
    }

    private static int computeBackTarget(int region, int roomNumber, SeededRandom rng) {
        int currentZone = RoomIdHelper.getZone(roomNumber);

        if (currentZone == 1) {
            // Zone 1 connects back toward room 0 area
            return Math.max(0, roomNumber - rng.nextIntRange(1, 10));
        }

        // Go back toward lower-numbered rooms (closer to hub)
        int zoneStart = getZoneStart(currentZone);
        int prevZoneEnd = zoneStart - 1;
        int prevZoneStart = getZoneStart(currentZone - 1);

        // Pick a room in the previous zone
        return rng.nextIntRange(prevZoneStart, prevZoneEnd);
    }

    private static int computeForwardTarget(int region, int roomNumber, int zone, SeededRandom rng) {
        int nextZoneStart, nextZoneEnd;

        if (zone < 5) {
            // Move deeper into the next zone
            nextZoneStart = getZoneStart(zone + 1);
            nextZoneEnd = getZoneEnd(zone + 1);
        } else {
            // Zone 5: connect to other rooms within zone 5
            nextZoneStart = getZoneStart(5);
            nextZoneEnd = getZoneEnd(5);
        }

        // Sometimes connect within same zone
        if (rng.nextDouble() < 0.3) {
            int sameZoneStart = getZoneStart(zone);
            int sameZoneEnd = getZoneEnd(zone);
            int target;
            do {
                target = rng.nextIntRange(sameZoneStart, sameZoneEnd);
            } while (target == roomNumber);
            return target;
        }

        return rng.nextIntRange(nextZoneStart, nextZoneEnd);
    }

    private static int getZoneStart(int zone) {
        switch (zone) {
            case 1: return 0;
            case 2: return Constants.ZONE_1_ROOMS;
            case 3: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS;
            case 4: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS;
            case 5: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS + Constants.ZONE_4_ROOMS;
            default: return 0;
        }
    }

    private static int getZoneEnd(int zone) {
        switch (zone) {
            case 1: return Constants.ZONE_1_ROOMS - 1;
            case 2: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS - 1;
            case 3: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS - 1;
            case 4: return Constants.ZONE_1_ROOMS + Constants.ZONE_2_ROOMS + Constants.ZONE_3_ROOMS + Constants.ZONE_4_ROOMS - 1;
            case 5: return Constants.ROOMS_PER_REGION - 1;
            default: return Constants.ROOMS_PER_REGION - 1;
        }
    }
}
