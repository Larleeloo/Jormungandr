package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
 * The hub (region 0, room 0) starts with a single NORTH door to region 1 entrance.
 *
 * Grid coordinates: room number = row * 100 + col.
 * Directions: NORTH (row-1), SOUTH (row+1), WEST (col-1), EAST (col+1).
 */
public class WorldMesh {

    private static WorldMesh instance;

    /** All room nodes keyed by room ID. */
    private final Map<String, RoomNode> nodes = new HashMap<>();

    private WorldMesh() {}

    public static synchronized WorldMesh getInstance() {
        if (instance == null) {
            instance = new WorldMesh();
            instance.buildMesh();
        }
        return instance;
    }

    /** Clear the cached instance so it rebuilds on next access. */
    public static synchronized void reset() {
        instance = null;
    }

    // ---- Public accessors ----

    public RoomNode getNode(String roomId) {
        return nodes.get(roomId);
    }

    public Map<Direction, String> getNeighbors(String roomId) {
        RoomNode node = nodes.get(roomId);
        if (node == null) return new HashMap<>();
        return node.getNeighbors();
    }

    public Map<Direction, String> getNeighbors(int region, int roomNumber) {
        return getNeighbors(RoomIdHelper.makeRoomId(region, roomNumber));
    }

    public boolean hasRoom(String roomId) {
        return nodes.containsKey(roomId);
    }

    public int getTotalRoomCount() {
        return nodes.size();
    }

    public int getRegionRoomCount(int region) {
        int count = 0;
        for (RoomNode node : nodes.values()) {
            if (node.getRegion() == region) count++;
        }
        return count;
    }

    public Map<String, RoomNode> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
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
