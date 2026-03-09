package com.larleeloo.jormungandr.data;

import android.content.Context;

import com.larleeloo.jormungandr.engine.RoomGenerator;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.FormulaHelper;
import com.larleeloo.jormungandr.util.RoomIdHelper;

/**
 * Singleton coordinating all game data access. Local-first: gameplay reads/writes local JSON.
 * Cloud sync is triggered at save points (waypoints, hub, app pause).
 */
public class GameRepository {
    private static GameRepository instance;

    private final LocalCache localCache;
    private final ItemRegistry itemRegistry;
    private final CreatureRegistry creatureRegistry;
    private final RoomFileManager roomFileManager;
    private final PlayerFileManager playerFileManager;
    private final RoomGenerator roomGenerator;

    private Player currentPlayer;
    private Room currentRoom;

    private GameRepository(Context context) {
        this.localCache = new LocalCache(context);
        this.itemRegistry = new ItemRegistry();
        this.creatureRegistry = new CreatureRegistry();
        this.roomFileManager = new RoomFileManager(localCache);
        this.playerFileManager = new PlayerFileManager(localCache);

        // Load registries
        itemRegistry.load(context);
        creatureRegistry.load(context);

        this.roomGenerator = new RoomGenerator(itemRegistry, creatureRegistry);
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

    public Room loadOrGenerateRoom(String roomId) {
        // Try loading from local cache first
        if (roomFileManager.roomExists(roomId)) {
            currentRoom = roomFileManager.loadRoom(roomId);
            return currentRoom;
        }

        // Generate new room
        int region = RoomIdHelper.getRegion(roomId);
        int roomNumber = RoomIdHelper.getRoomNumber(roomId);
        int playerLevel = currentPlayer != null ? currentPlayer.getLevel() : 1;

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

        // Ensure bidirectional door links: for each door target, if that room
        // doesn't exist yet, generate it and ensure its BACK door points here.
        if (region != 0) {
            ensureBidirectionalDoors(currentRoom, playerLevel);
        }

        return currentRoom;
    }

    /**
     * For each door in the source room, check if the target room exists.
     * If not, generate it and override its BACK door to point back to the source room.
     */
    private void ensureBidirectionalDoors(Room sourceRoom, int playerLevel) {
        for (java.util.Map.Entry<String, String> entry : sourceRoom.getDoors().entrySet()) {
            String dirName = entry.getKey();
            String targetId = entry.getValue();
            if (targetId == null || dirName.equals("BACK")) continue;

            // If target room already exists, don't overwrite it
            if (roomFileManager.roomExists(targetId)) continue;

            int targetRegion = RoomIdHelper.getRegion(targetId);
            int targetNumber = RoomIdHelper.getRoomNumber(targetId);

            if (targetRegion == 0) continue; // Don't regenerate hub

            Room targetRoom = roomGenerator.generateRoom(targetRegion, targetNumber, playerLevel);
            // Override the BACK door to point back to source room
            targetRoom.getDoors().put("BACK", sourceRoom.getRoomId());
            roomFileManager.saveRoom(targetRoom);
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
