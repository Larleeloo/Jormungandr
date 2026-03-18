package com.larleeloo.jormungandr.data;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.CloudSyncManager;
import com.larleeloo.jormungandr.engine.RoomGenerator;
import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.FormulaHelper;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import android.os.Handler;
import android.os.Looper;

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

    private static final int ROOM_CACHE_SIZE = 50;

    private final ItemRegistry itemRegistry;
    private final CreatureRegistry creatureRegistry;
    private final RoomFileManager roomFileManager;
    private final PlayerFileManager playerFileManager;
    private final AppsScriptClient cloudClient;
    private final CloudSyncManager cloudSyncManager;
    private final RoomGenerator roomGenerator;
    private final LruCache<String, Room> roomCache = new LruCache<>(ROOM_CACHE_SIZE);

    private Player currentPlayer;
    private Room currentRoom;

    private GameRepository(Context context) {
        this.cloudClient = new AppsScriptClient();
        this.cloudSyncManager = new CloudSyncManager();
        this.itemRegistry = new ItemRegistry();
        this.creatureRegistry = new CreatureRegistry();
        this.roomFileManager = new RoomFileManager(cloudClient);
        this.playerFileManager = new PlayerFileManager(cloudClient);

        // Load registries from bundled assets (read-only, not save data)
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

    /** Clear the cached instance so it reinitializes on next access. */
    public static synchronized void reset() {
        if (instance != null) {
            instance.cloudSyncManager.shutdown();
        }
        instance = null;
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
            // Async cloud upload to avoid NetworkOnMainThreadException
            cloudSyncManager.syncPlayerToCloud(currentPlayer, null);
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
            roomCache.put(roomId, cloudRoom);
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
        roomCache.put(roomId, currentRoom);

        return currentRoom;
    }

    /**
     * Return a cached room if available, or null if the room must be fetched
     * from the cloud. Does NOT set currentRoom.
     */
    public Room getCachedRoom(String roomId) {
        return roomCache.get(roomId);
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
            roomCache.put(currentRoom.getRoomId(), currentRoom);
            // Async cloud upload — the synchronous roomFileManager.saveRoom()
            // throws NetworkOnMainThreadException when called from UI thread,
            // so rely on the async path for actual persistence.
            cloudSyncManager.syncRoomToCloud(currentRoom, null);
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
                // Auto-link discovered waypoints to the hub
                if (!currentPlayer.getDiscoveredWaypoints().contains(roomId)) {
                    currentPlayer.getDiscoveredWaypoints().add(roomId);
                }
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

    // ---- Async room operations ----

    public interface RoomCallback {
        void onComplete(Room room);
    }

    /**
     * Load or generate a room on a background thread where network I/O is
     * permitted, then deliver the result on the main thread.
     */
    public void loadOrGenerateRoomAsync(String roomId, RoomCallback callback) {
        cloudSyncManager.executeInBackground(() -> {
            Room room = loadOrGenerateRoom(roomId);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(room));
            }
            // Prefetch adjacent rooms so the first navigation is fast
            prefetchAdjacentRooms(roomId);
        });
    }

    /**
     * Navigate using cache-first strategy. If the room is cached, applies
     * player state updates immediately and returns the cached room. A
     * background cloud sync refreshes the cache afterward.
     * If not cached, falls back to the full async network load.
     */
    public void navigateToRoomAsync(String roomId, RoomCallback callback) {
        Room cached = roomCache.get(roomId);
        if (cached != null) {
            // Use cached room — apply player state on main thread immediately
            currentRoom = cached;
            applyNavigationState(roomId, cached);
            if (callback != null) {
                callback.onComplete(cached);
            }
            // Refresh from cloud in background so cache stays fresh
            cloudSyncManager.executeInBackground(() -> {
                Room fresh = loadOrGenerateRoom(roomId);
                if (fresh != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        currentRoom = fresh;
                    });
                }
            });
            return;
        }

        // No cache hit — full network load
        cloudSyncManager.executeInBackground(() -> {
            Room room = navigateToRoom(roomId);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(room));
            }
            // Prefetch adjacent rooms
            prefetchAdjacentRooms(roomId);
        });
    }

    /**
     * Prefetch rooms adjacent to the given room by loading them into the cache
     * on the background executor. Only fetches rooms not already cached.
     */
    private void prefetchAdjacentRooms(String roomId) {
        WorldMesh mesh = WorldMesh.getInstance();
        Map<Direction, String> neighbors = mesh.getNeighbors(roomId);
        for (String neighborId : neighbors.values()) {
            if (roomCache.get(neighborId) == null) {
                cloudSyncManager.executeInPrefetchPool(() -> {
                    try {
                        loadOrGenerateRoom(neighborId);
                    } catch (Exception e) {
                        Log.w(TAG, "Prefetch failed for " + neighborId, e);
                    }
                });
            }
        }
    }

    /**
     * Apply player state changes for navigating to a room without triggering
     * a network load. Used by the cache-hit path of navigateToRoomAsync.
     */
    private void applyNavigationState(String roomId, Room room) {
        if (currentPlayer == null) return;

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
            if (!currentPlayer.getDiscoveredWaypoints().contains(roomId)) {
                currentPlayer.getDiscoveredWaypoints().add(roomId);
            }
        } else {
            int newStamina = currentPlayer.getStamina() - Constants.STAMINA_COST_MOVE
                    + Constants.STAMINA_REGEN_PER_ROOM;
            currentPlayer.setStamina(Math.max(0,
                    Math.min(currentPlayer.getMaxStamina(), newStamina)));
        }

        savePlayer();
    }

    // ---- Registries ----

    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public CreatureRegistry getCreatureRegistry() { return creatureRegistry; }
    public RoomFileManager getRoomFileManager() { return roomFileManager; }
    public PlayerFileManager getPlayerFileManager() { return playerFileManager; }
    public AppsScriptClient getCloudClient() { return cloudClient; }
    public CloudSyncManager getCloudSyncManager() { return cloudSyncManager; }
}
