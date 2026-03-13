package com.larleeloo.jormungandr.data;

import android.content.Context;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.engine.RoomGenerator;
import com.larleeloo.jormungandr.engine.RoomNode;
import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.FormulaHelper;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.Map;

/**
 * Singleton coordinating all game data access.
 *
 * Room loading priority:
 *   1. Check Drive cloud storage for existing room data
 *   2. Check local cache
 *   3. Generate room content based on the room's position in the pre-built WorldMesh
 *
 * The WorldMesh (a pre-generated branching linked list of all 80,000 rooms) defines
 * which rooms connect to which. Room content (creatures, loot, traps) is generated
 * on demand when a room is first visited and no cloud data exists for it.
 */
public class GameRepository {
    private static GameRepository instance;

    private final LocalCache localCache;
    private final ItemRegistry itemRegistry;
    private final CreatureRegistry creatureRegistry;
    private final RoomFileManager roomFileManager;
    private final PlayerFileManager playerFileManager;
    private final RoomGenerator roomGenerator;
    private final AppsScriptClient cloudClient;

    private Player currentPlayer;
    private Room currentRoom;

    private GameRepository(Context context) {
        this.localCache = new LocalCache(context);
        this.itemRegistry = new ItemRegistry();
        this.creatureRegistry = new CreatureRegistry();
        this.roomFileManager = new RoomFileManager(localCache);
        this.playerFileManager = new PlayerFileManager(localCache);
        this.cloudClient = new AppsScriptClient();

        // Load registries
        itemRegistry.load(context);
        creatureRegistry.load(context);

        this.roomGenerator = new RoomGenerator(itemRegistry, creatureRegistry);

        // Try loading mesh from reference file (lazy mode) before falling back to full build
        WorldMesh.initFromReference(context);
    }

    public static synchronized GameRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GameRepository(context.getApplicationContext());
        }
        return instance;
    }

    public static GameRepository getInstance() {
        return instance;
    }

    // ---- Player operations ----

    public Player createNewPlayer(String accessCode, String name) {
        Player player = new Player();
        player.setAccessCode(accessCode);
        player.setName(name);
        FormulaHelper.recalculatePlayerStats(player);
        currentPlayer = player;
        savePlayer();
        return player;
    }

    public Player loadPlayer(String accessCode) {
        currentPlayer = playerFileManager.loadPlayer(accessCode);
        return currentPlayer;
    }

    public void savePlayer() {
        if (currentPlayer != null) {
            playerFileManager.savePlayer(currentPlayer);
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    public boolean playerExists(String accessCode) {
        return playerFileManager.playerExists(accessCode);
    }

    // ---- Room operations ----

    /**
     * Load a room using the priority: cloud -> local cache -> generate from WorldMesh.
     * Door connections always come from the pre-built WorldMesh linked list,
     * ensuring the room layout is consistent regardless of data source.
     */
    public Room loadOrGenerateRoom(String roomId) {
        int region = RoomIdHelper.getRegion(roomId);
        int roomNumber = RoomIdHelper.getRoomNumber(roomId);
        int playerLevel = currentPlayer != null ? currentPlayer.getLevel() : 1;

        // 1. Try fetching from Drive cloud storage first
        Room cloudRoom = fetchRoomFromCloud(roomId);
        if (cloudRoom != null) {
            // Cloud data exists — apply mesh doors to ensure layout consistency
            applyMeshDoors(cloudRoom, region, roomNumber);
            roomFileManager.saveRoom(cloudRoom);
            currentRoom = cloudRoom;
            return currentRoom;
        }

        // 2. Try loading from local cache
        if (roomFileManager.roomExists(roomId)) {
            currentRoom = roomFileManager.loadRoom(roomId);
            // Re-apply mesh doors in case the local data has stale connections
            applyMeshDoors(currentRoom, region, roomNumber);
            return currentRoom;
        }

        // 3. No cloud or local data — generate room content based on WorldMesh position
        if (region == 0) {
            currentRoom = roomGenerator.generateHubRoom();
        } else {
            currentRoom = roomGenerator.generateRoom(region, roomNumber, playerLevel);
        }

        if (currentPlayer != null) {
            currentRoom.setFirstVisitedBy(currentPlayer.getAccessCode());
        }

        // Save generated room
        roomFileManager.saveRoom(currentRoom);

        return currentRoom;
    }

    /**
     * Attempt a synchronous cloud fetch for room data.
     * Returns null if cloud is not configured, the fetch fails, or no data exists.
     * Uses a short timeout to avoid blocking gameplay for too long.
     */
    private Room fetchRoomFromCloud(String roomId) {
        if (!cloudClient.isConfigured()) return null;

        try {
            SyncResult result = cloudClient.getRoom(roomId);
            if (result.isSuccess() && result.getData() != null) {
                Room room = JsonHelper.fromJson(result.getData(), Room.class);
                return room;
            }
        } catch (Exception e) {
            // Cloud fetch failed — fall through to local/generate
        }
        return null;
    }

    /**
     * Overwrite a room's doors with the connections defined in the WorldMesh.
     * This ensures the pre-generated linked list structure is always authoritative
     * for room layout, even when room content comes from cloud.
     */
    private void applyMeshDoors(Room room, int region, int roomNumber) {
        WorldMesh mesh = WorldMesh.getInstance();
        Map<Direction, String> meshDoors = mesh.getNeighbors(region, roomNumber);

        if (!meshDoors.isEmpty()) {
            room.getDoors().clear();
            for (Map.Entry<Direction, String> entry : meshDoors.entrySet()) {
                room.addDoor(entry.getKey(), entry.getValue());
            }
        }
    }

    public void saveCurrentRoom() {
        if (currentRoom != null) {
            roomFileManager.saveRoom(currentRoom);
        }
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    // ---- Navigation ----

    /**
     * Navigate using BACK door — pops from history instead of pushing.
     */
    public Room navigateBack() {
        if (currentPlayer == null) return null;
        String previousRoom = currentPlayer.popRoomFromHistory();
        if (previousRoom == null) {
            // Fallback: go to hub if history is empty
            previousRoom = Constants.HUB_ROOM_ID;
        }
        // Use internal navigate with isBackNavigation=true so we don't push onto history
        return navigateToRoomInternal(previousRoom, true);
    }

    public Room navigateToRoom(String roomId) {
        return navigateToRoomInternal(roomId, false);
    }

    private Room navigateToRoomInternal(String roomId, boolean isBackNavigation) {
        Room room = loadOrGenerateRoom(roomId);

        if (currentPlayer != null) {
            String oldRoomId = currentPlayer.getCurrentRoomId();

            // Only push to history when moving forward (not going back)
            if (!isBackNavigation && oldRoomId != null && !oldRoomId.equals(roomId)) {
                currentPlayer.pushRoomToHistory(oldRoomId);
            }

            // Update previousRoomId for legacy compatibility
            if (oldRoomId != null && !oldRoomId.equals(roomId)) {
                currentPlayer.setPreviousRoomId(oldRoomId);
            }

            // Set BACK door based on top of history stack
            if (room != null && !RoomIdHelper.isHub(roomId)) {
                java.util.List<String> history = currentPlayer.getRoomHistory();
                String backTarget = history.isEmpty() ? Constants.HUB_ROOM_ID
                        : history.get(history.size() - 1);
                room.getDoors().put("BACK", backTarget);
                roomFileManager.saveRoom(room);
            }

            currentPlayer.setCurrentRoomId(roomId);
            currentPlayer.setCurrentRegion(RoomIdHelper.getRegion(roomId));
            currentPlayer.discoverRoom(roomId, RoomIdHelper.getRegion(roomId));
            currentPlayer.setRoomsVisitedSinceHub(
                    currentPlayer.getRoomsVisitedSinceHub() + 1);

            // Stamina: consume on movement, regen at hub/waypoint
            if (RoomIdHelper.isHub(roomId)) {
                currentPlayer.setRoomsVisitedSinceHub(0);
                currentPlayer.getRoomHistory().clear(); // Clear history at hub
                currentPlayer.setStamina(currentPlayer.getMaxStamina()); // Full restore at hub
            } else if (room != null && room.isWaypoint()) {
                currentPlayer.setStamina(currentPlayer.getMaxStamina()); // Full restore at waypoint
            } else {
                // Consume stamina for movement, regen a little
                int newStamina = currentPlayer.getStamina() - Constants.STAMINA_COST_MOVE
                        + Constants.STAMINA_REGEN_PER_ROOM;
                currentPlayer.setStamina(Math.max(0,
                        Math.min(currentPlayer.getMaxStamina(), newStamina)));
            }

            savePlayer();
        }

        return room;
    }

    // ---- Registries ----

    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public CreatureRegistry getCreatureRegistry() { return creatureRegistry; }
    public RoomFileManager getRoomFileManager() { return roomFileManager; }
    public PlayerFileManager getPlayerFileManager() { return playerFileManager; }
    public LocalCache getLocalCache() { return localCache; }
}
