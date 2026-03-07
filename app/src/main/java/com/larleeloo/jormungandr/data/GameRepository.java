package com.larleeloo.jormungandr.data;

import android.content.Context;

import com.larleeloo.jormungandr.engine.RoomGenerator;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
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

        return currentRoom;
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

    public Room navigateToRoom(String roomId) {
        Room room = loadOrGenerateRoom(roomId);

        if (currentPlayer != null) {
            currentPlayer.setCurrentRoomId(roomId);
            currentPlayer.setCurrentRegion(RoomIdHelper.getRegion(roomId));
            currentPlayer.discoverRoom(roomId, RoomIdHelper.getRegion(roomId));
            currentPlayer.setRoomsVisitedSinceHub(
                    currentPlayer.getRoomsVisitedSinceHub() + 1);

            if (RoomIdHelper.isHub(roomId)) {
                currentPlayer.setRoomsVisitedSinceHub(0);
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
