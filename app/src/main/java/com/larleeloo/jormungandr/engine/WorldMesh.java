package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * World room layout graph for all rooms across 8 regions.
 *
 * Each region is a 100×100 grid (10,000 rooms) laid out as a maze.
 * The maze is generated using iterative randomized DFS (recursive backtracker)
 * which creates a perfect spanning tree, then extra connections are added
 * to create loops and alternate paths. Dead-end rooms may get portal doors
 * that connect to rooms in other regions.
 *
 * The hub (region 0, room 0) connects to the (0,0) entry room of regions 1, 2, and 8.
 *
 * Grid coordinates: room number = row * 100 + col.
 * Directions map to cardinal movement:
 *   FORWARD = North (row−1), BACK = South (row+1),
 *   LEFT = West (col−1), RIGHT = East (col+1).
 */
public class WorldMesh {

    private static final String TAG = "WorldMesh";

    private static WorldMesh instance;

    /** All room nodes keyed by room ID. Used in full-build mode. */
    private final Map<String, RoomNode> nodes = new HashMap<>();

    /** Parsed reference JSON — loaded lazily from cloud. */
    private JSONObject referenceJson;

    /** True if operating in lazy-reference mode. */
    private boolean lazyMode;

    /** Total room count from the reference file. */
    private int refTotalRooms;

    private WorldMesh() {}

    public static synchronized WorldMesh getInstance() {
        if (instance == null) {
            instance = new WorldMesh();
            instance.buildMesh();
        }
        return instance;
    }

    /**
     * Initialize from cloud reference. Falls back to full in-memory build.
     */
    public static synchronized boolean initFromCloud(AppsScriptClient client) {
        if (instance != null) return instance.lazyMode;

        instance = new WorldMesh();
        if (instance.loadReferenceFromCloud(client)) {
            Log.i(TAG, "Loaded mesh reference from cloud (lazy mode). Rooms: " + instance.refTotalRooms);
            return true;
        }

        Log.i(TAG, "No cloud mesh reference found. Building full grid maze in memory.");
        instance.buildMesh();
        return false;
    }

    private boolean loadReferenceFromCloud(AppsScriptClient client) {
        if (client == null || !client.isConfigured()) return false;
        try {
            SyncResult result = client.getMeshReference();
            if (result.isSuccess() && result.getData() != null) {
                String json = result.getData();
                referenceJson = new JSONObject(json);
                refTotalRooms = referenceJson.optInt("totalRooms", 0);
                lazyMode = true;
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch mesh reference from cloud", e);
        }
        return false;
    }

    // ---- Public accessors ----

    public RoomNode getNode(String roomId) {
        if (!lazyMode) return nodes.get(roomId);
        return buildNodeFromReference(roomId);
    }

    public Map<Direction, String> getNeighbors(String roomId) {
        if (!lazyMode) {
            RoomNode node = nodes.get(roomId);
            if (node == null) return new HashMap<>();
            return node.getNeighbors();
        }
        return getNeighborsFromReference(roomId);
    }

    public Map<Direction, String> getNeighbors(int region, int roomNumber) {
        return getNeighbors(RoomIdHelper.makeRoomId(region, roomNumber));
    }

    public boolean hasRoom(String roomId) {
        if (!lazyMode) return nodes.containsKey(roomId);
        return hasRoomInReference(roomId);
    }

    public int getTotalRoomCount() {
        if (lazyMode) return refTotalRooms;
        return nodes.size();
    }

    public int getRegionRoomCount(int region) {
        if (lazyMode) {
            try {
                JSONObject regions = referenceJson.getJSONObject("regions");
                JSONObject regionObj = regions.optJSONObject(String.valueOf(region));
                return regionObj != null ? regionObj.length() : 0;
            } catch (Exception e) { return 0; }
        }
        int count = 0;
        for (RoomNode node : nodes.values()) {
            if (node.getRegion() == region) count++;
        }
        return count;
    }

    public Map<String, RoomNode> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public boolean isLazyMode() { return lazyMode; }

    // ---- Lazy-reference lookups ----

    private Map<Direction, String> getNeighborsFromReference(String roomId) {
        try {
            int region = RoomIdHelper.getRegion(roomId);
            JSONObject regions = referenceJson.getJSONObject("regions");
            JSONObject regionObj = regions.optJSONObject(String.valueOf(region));
            if (regionObj == null) return new HashMap<>();
            JSONObject roomObj = regionObj.optJSONObject(roomId);
            if (roomObj == null) return new HashMap<>();

            Map<Direction, String> neighbors = new HashMap<>();
            Iterator<String> keys = roomObj.keys();
            while (keys.hasNext()) {
                String dirName = keys.next();
                try {
                    Direction dir = Direction.valueOf(dirName);
                    neighbors.put(dir, roomObj.getString(dirName));
                } catch (IllegalArgumentException ignored) {}
            }
            return neighbors;
        } catch (Exception e) { return new HashMap<>(); }
    }

    private boolean hasRoomInReference(String roomId) {
        try {
            int region = RoomIdHelper.getRegion(roomId);
            JSONObject regions = referenceJson.getJSONObject("regions");
            JSONObject regionObj = regions.optJSONObject(String.valueOf(region));
            return regionObj != null && regionObj.has(roomId);
        } catch (Exception e) { return false; }
    }

    private RoomNode buildNodeFromReference(String roomId) {
        Map<Direction, String> neighbors = getNeighborsFromReference(roomId);
        if (neighbors.isEmpty()) return null;
        int region = RoomIdHelper.getRegion(roomId);
        int roomNumber = RoomIdHelper.getRoomNumber(roomId);
        RoomNode node = new RoomNode(region, roomNumber);
        for (Map.Entry<Direction, String> entry : neighbors.entrySet()) {
            node.addNeighbor(entry.getKey(), entry.getValue());
        }
        return node;
    }

    // ---- Mesh construction ----

    private void buildMesh() {
        buildHub();
        for (int region = 1; region <= Constants.NUM_REGIONS; region++) {
            buildRegionMaze(region);
        }
    }

    private void buildHub() {
        RoomNode hub = new RoomNode(0, 0);
        // New players start with only one door: NORTH to region 1 entrance
        hub.addNeighbor(Direction.NORTH, RoomIdHelper.makeRoomId(1, 0));
        putNode(hub);
    }

    /**
     * Build a 100×100 grid maze for a single region using iterative
     * randomized DFS (recursive backtracker), then punch extra connections
     * and add portal doors to other regions.
     */
    private void buildRegionMaze(int region) {
        int size = Constants.GRID_SIZE;
        SeededRandom rng = new SeededRandom(Constants.WORLD_SEED ^ ((long) region << 48));

        // Create all room nodes for this region (empty, no connections yet)
        RoomNode[][] grid = new RoomNode[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int roomNumber = RoomIdHelper.toRoomNumber(r, c);
                grid[r][c] = new RoomNode(region, roomNumber);
            }
        }

        // Phase 1: Carve maze paths using iterative DFS
        boolean[][] visited = new boolean[size][size];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        visited[0][0] = true;

        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            int cr = cell[0], cc = cell[1];

            // Collect unvisited neighbors
            List<Direction> unvisitedDirs = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                int nr = cr + dir.getDRow();
                int nc = cc + dir.getDCol();
                if (nr >= 0 && nr < size && nc >= 0 && nc < size && !visited[nr][nc]) {
                    unvisitedDirs.add(dir);
                }
            }

            if (unvisitedDirs.isEmpty()) {
                stack.pop(); // Backtrack
            } else {
                // Pick a random unvisited neighbor
                Direction chosen = unvisitedDirs.get(rng.nextInt(unvisitedDirs.size()));
                int nr = cr + chosen.getDRow();
                int nc = cc + chosen.getDCol();

                // Carve passage: connect both directions
                grid[cr][cc].addNeighbor(chosen,
                        RoomIdHelper.makeRoomId(region, RoomIdHelper.toRoomNumber(nr, nc)));
                grid[nr][nc].addNeighbor(chosen.opposite(),
                        RoomIdHelper.makeRoomId(region, RoomIdHelper.toRoomNumber(cr, cc)));

                visited[nr][nc] = true;
                stack.push(new int[]{nr, nc});
            }
        }

        // Phase 2: Punch extra connections to create loops and alternate paths
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                for (Direction dir : Direction.values()) {
                    int nr = r + dir.getDRow();
                    int nc = c + dir.getDCol();
                    if (nr >= 0 && nr < size && nc >= 0 && nc < size
                            && !grid[r][c].hasNeighbor(dir)
                            && rng.nextDouble() < Constants.MAZE_EXTRA_CONNECTION_CHANCE) {
                        grid[r][c].addNeighbor(dir,
                                RoomIdHelper.makeRoomId(region, RoomIdHelper.toRoomNumber(nr, nc)));
                        grid[nr][nc].addNeighbor(dir.opposite(),
                                RoomIdHelper.makeRoomId(region, RoomIdHelper.toRoomNumber(r, c)));
                    }
                }
            }
        }

        // Phase 3: Place exactly 7 one-way portal doors — one for each other region.
        // Each portal targets the destination region's single waypoint room.
        // Portals are one-way: entering takes the player to the waypoint, but
        // the waypoint does NOT have a door back to the source portal room.
        int portalsPlaced = 0;
        for (int targetRegion = 1; targetRegion <= Constants.NUM_REGIONS; targetRegion++) {
            if (targetRegion == region) continue;

            String targetWaypointId = RoomIdHelper.getRegionWaypointId(targetRegion);
            if (targetWaypointId == null) continue;

            // Find a random room with an open direction for this portal
            boolean placed = false;
            for (int attempt = 0; attempt < 500 && !placed; attempt++) {
                int pr = rng.nextInt(size);
                int pc = rng.nextInt(size);
                // Don't place portals on the entrance or on this region's waypoint
                int roomNum = RoomIdHelper.toRoomNumber(pr, pc);
                if (roomNum == 0) continue;
                if (RoomIdHelper.isWaypoint(region, roomNum)) continue;

                // Find an open direction
                for (Direction dir : Direction.values()) {
                    if (!grid[pr][pc].hasNeighbor(dir)) {
                        grid[pr][pc].addNeighbor(dir, targetWaypointId);
                        placed = true;
                        portalsPlaced++;
                        break;
                    }
                }
            }
        }

        // Store all nodes
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                putNode(grid[r][c]);
            }
        }
    }

    private void putNode(RoomNode node) {
        nodes.put(node.getRoomId(), node);
    }
}
