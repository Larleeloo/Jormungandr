package com.larleeloo.jormungandr.data;

import android.content.Context;
import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.engine.RoomGenerator;
import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.FormulaHelper;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.Map;

/**
 * Singleton coordinating all game data access — fully cloud-backed.
 *
 * Room loading priority:
 *   1. Check Drive cloud storage for existing room data
 *   2. Generate room content based on the room's position in the pre-built WorldMesh
 *
 * No local files are stored. All persistence goes through the Apps Script backend
 * to Google Drive, eliminating stale-cache issues between versions.
 */
public class GameRepository {
    private static final String TAG = "GameRepository";
    private static GameRepository instance;

    private final ItemRegistry itemRegistry;
    private final CreatureRegistry creatureRegistry;
    private final RoomFileManager roomFileManager;
    private final PlayerFileManager playerFileManager;
    private final AppsScriptClient cloudClient;
    private final RoomGenerator roomGenerator;

    private Player currentPlayer;
    private Room currentRoom;

    private GameRepository(Context context) {
        this.cloudClient = new AppsScriptClient();
        this.itemRegistry = new ItemRegistry();
        this.creatureRegistry = new CreatureRegistry();
        this.roomFileManager = new RoomFileManager(cloudClient);
        this.playerFileManager = new PlayerFileManager(cloudClient);

        // Load registries from bundled assets (read-only, not save data)
        itemRegistry.load(context);
        creatureRegistry.load(context);

        this.roomGenerator = new RoomGenerator(itemRegistry, creatureRegistry);

        // Initialize mesh: try cloud reference, otherwise build in memory from seed
        WorldMesh.initFromCloud(cloudClient);
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
     * Load a room using the priority: cloud -> generate from WorldMesh.
     * Door connections always come from the pre-built WorldMesh linked list,
     * ensuring the room layout is consistent regardless of data source.
     */
    public Room loadOrGenerateRoom(String roomId) {
        int region = RoomIdHelper.getRegion(roomId);
        int roomNumber = RoomIdHelper.getRoomNumber(roomId);
        int playerLevel = currentPlayer != null ? currentPlayer.getLevel() : 1;

        // 1. Try fetching from Drive cloud storage
        Room cloudRoom = roomFileManager.loadRoom(roomId);
        if (cloudRoom != null) {
            applyMeshDoors(cloudRoom, region, roomNumber);
            currentRoom = cloudRoom;
            return currentRoom;
        }

        // 2. No cloud data — generate room content based on WorldMesh position
        if (region == 0) {
            currentRoom = roomGenerator.generateHubRoom();
        } else {
            currentRoom = roomGenerator.generateRoom(region, roomNumber, playerLevel);
        }

        if (currentPlayer != null) {
            currentRoom.setFirstVisitedBy(currentPlayer.getAccessCode());
        }

        // Save generated room to cloud
        roomFileManager.saveRoom(currentRoom);

        return currentRoom;
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

    public Room navigateToRoom(String roomId) {
        Room room = loadOrGenerateRoom(roomId);

        if (currentPlayer != null) {
            String oldRoomId = currentPlayer.getCurrentRoomId();

            if (oldRoomId != null && !oldRoomId.equals(roomId)) {
                currentPlayer.setPreviousRoomId(oldRoomId);
            }

            currentPlayer.setCurrentRoomId(roomId);
            currentPlayer.setCurrentRegion(RoomIdHelper.getRegion(roomId));
            currentPlayer.discoverRoom(roomId, RoomIdHelper.getRegion(roomId));
            currentPlayer.setRoomsVisitedSinceHub(
                    currentPlayer.getRoomsVisitedSinceHub() + 1);

            if (RoomIdHelper.isHub(roomId)) {
                currentPlayer.setRoomsVisitedSinceHub(0);
                currentPlayer.setStamina(currentPlayer.getMaxStamina());
            } else if (room != null && room.isWaypoint()) {
                currentPlayer.setStamina(currentPlayer.getMaxStamina());
            } else {
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
    public AppsScriptClient getCloudClient() { return cloudClient; }
}
