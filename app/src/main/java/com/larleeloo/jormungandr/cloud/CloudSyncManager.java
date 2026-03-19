package com.larleeloo.jormungandr.cloud;

import android.os.Handler;
import android.os.Looper;

import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.data.JsonHelper;
import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.PlayerNote;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.TradeListing;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import com.larleeloo.jormungandr.model.TimestampedAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates cloud sync at save points (waypoint visit, hub entry, app pause).
 * All game data lives in the cloud — this manager handles async operations
 * that don't go through GameRepository's synchronous path (e.g. merging notes,
 * pulling fresh data from cloud into the in-memory model).
 */
public class CloudSyncManager {

    public interface SyncCallback {
        void onSyncComplete(boolean success, String message);
    }

    private final AppsScriptClient client;
    private final ExecutorService executor;
    private final ExecutorService prefetchExecutor;
    private final Handler mainHandler;

    public CloudSyncManager() {
        this.client = new AppsScriptClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.prefetchExecutor = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Upload player data to Drive (async).
     */
    public void syncPlayerToCloud(Player player, SyncCallback callback) {
        executor.execute(() -> {
            String json = JsonHelper.toJson(player);
            SyncResult result = client.savePlayer(player.getAccessCode(), json);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Download player data from Drive (async) and set as current player in-memory.
     */
    public void syncPlayerFromCloud(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getPlayer(accessCode);
            if (result.isSuccess() && result.getData() != null) {
                Player cloudPlayer = JsonHelper.fromJson(result.getData(), Player.class);
                if (cloudPlayer != null) {
                    GameRepository repo = GameRepository.getInstance();
                    if (repo != null) {
                        repo.setCurrentPlayer(cloudPlayer);
                    }
                }
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Upload room data to Drive (async).
     */
    public void syncRoomToCloud(Room room, SyncCallback callback) {
        executor.execute(() -> {
            String json = JsonHelper.toJson(room);
            SyncResult result = client.saveRoom(room.getRoomId(), json);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Download room data from Drive (async). Replaces in-memory room content with
     * cloud data while preserving door connections from the pre-built WorldMesh.
     */
    public void syncRoomFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getRoom(roomId);
            if (result.isSuccess() && result.getData() != null) {
                Room cloudRoom = JsonHelper.fromJson(result.getData(), Room.class);
                GameRepository repo = GameRepository.getInstance();
                if (cloudRoom != null && repo != null) {
                    // Apply mesh doors so cloud data doesn't override the linked list layout
                    int region = RoomIdHelper.getRegion(roomId);
                    int roomNumber = RoomIdHelper.getRoomNumber(roomId);
                    Map<Direction, String> meshDoors = WorldMesh.getInstance()
                            .getNeighbors(region, roomNumber);
                    if (!meshDoors.isEmpty()) {
                        cloudRoom.getDoors().clear();
                        for (Map.Entry<Direction, String> entry : meshDoors.entrySet()) {
                            cloudRoom.addDoor(entry.getKey(), entry.getValue());
                        }
                    }

                    Room currentRoom = repo.getCurrentRoom();
                    if (currentRoom != null && currentRoom.getRoomId() != null
                            && currentRoom.getRoomId().equals(roomId)) {
                        currentRoom.setObjects(cloudRoom.getObjects());
                        currentRoom.setFirstVisitedBy(cloudRoom.getFirstVisitedBy());
                        currentRoom.setFirstVisitedAt(cloudRoom.getFirstVisitedAt());

                        // Merge notes (additive)
                        if (cloudRoom.getPlayerNotes() != null) {
                            for (PlayerNote cloudNote : cloudRoom.getPlayerNotes()) {
                                boolean exists = false;
                                for (PlayerNote localNote : currentRoom.getPlayerNotes()) {
                                    if (localNote.getText().equals(cloudNote.getText())
                                            && localNote.getPlayerName().equals(cloudNote.getPlayerName())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    currentRoom.getPlayerNotes().add(cloudNote);
                                }
                            }
                        }

                    }
                }
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Upload trade listings for a waypoint room to Drive (async).
     * Trade listings are stored in a separate file per room to prevent
     * overwrite conflicts when multiple players save the room.
     */
    public void syncTradesToCloud(String roomId, List<TradeListing> listings, SyncCallback callback) {
        executor.execute(() -> {
            String json = JsonHelper.toJson(listings);
            SyncResult result = client.saveTrades(roomId, json);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Download trade listings for a waypoint room from Drive (async)
     * and update the in-memory room.
     */
    public void syncTradesFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getTrades(roomId);
            if (result.isSuccess() && result.getData() != null) {
                GameRepository repo = GameRepository.getInstance();
                if (repo != null) {
                    Room currentRoom = repo.getCurrentRoom();
                    if (currentRoom != null && currentRoom.getRoomId() != null
                            && currentRoom.getRoomId().equals(roomId)) {
                        List<TradeListing> cloudTrades = JsonHelper.listFromJson(
                                result.getData(), TradeListing.class);
                        if (cloudTrades != null) {
                            currentRoom.setTradeListings(new ArrayList<>(cloudTrades));
                        }
                    }
                }
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Upload a single note for a room to Drive (async).
     */
    public void syncNoteToCloud(String roomId, String accessCode, String noteText, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.saveNote(roomId, accessCode, noteText);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Download notes for a room from Drive (async) and merge into in-memory room.
     */
    public void syncNotesFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getNotes(roomId);
            if (result.isSuccess() && result.getData() != null) {
                GameRepository repo = GameRepository.getInstance();
                if (repo != null) {
                    Room currentRoom = repo.getCurrentRoom();
                    if (currentRoom != null && currentRoom.getRoomId() != null
                            && currentRoom.getRoomId().equals(roomId)) {
                        List<PlayerNote> cloudNotes = JsonHelper.listFromJson(
                                result.getData(), PlayerNote.class);
                        if (cloudNotes != null) {
                            for (PlayerNote cloudNote : cloudNotes) {
                                boolean exists = false;
                                for (PlayerNote localNote : currentRoom.getPlayerNotes()) {
                                    if (localNote.getText().equals(cloudNote.getText())
                                            && localNote.getPlayerName().equals(cloudNote.getPlayerName())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    currentRoom.getPlayerNotes().add(cloudNote);
                                }
                            }
                        }
                    }
                }
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Validate an access code against the cloud (async).
     */
    public void validateAccessCode(String code, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.validateAccessCode(code);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Check game version against cloud (async).
     */
    public void checkVersion(SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getVersion();
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Full sync: upload player + current room.
     */
    public void fullSync(Player player, Room room, SyncCallback callback) {
        executor.execute(() -> {
            if (!client.isConfigured()) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncComplete(false,
                            "Cloud sync not configured. Set APPS_SCRIPT_URL in Constants.java"));
                }
                return;
            }

            SyncResult playerResult = client.savePlayer(player.getAccessCode(), JsonHelper.toJson(player));
            SyncResult roomResult = null;
            if (room != null) {
                roomResult = client.saveRoom(room.getRoomId(), JsonHelper.toJson(room));
            }

            boolean success = playerResult.isSuccess() && (roomResult == null || roomResult.isSuccess());
            String message;
            if (success) {
                message = "Synced";
            } else {
                StringBuilder sb = new StringBuilder("Sync failed: ");
                if (!playerResult.isSuccess()) {
                    sb.append("Player (").append(playerResult.getMessage()).append(")");
                    if (roomResult != null && !roomResult.isSuccess()) {
                        sb.append(", ");
                    }
                }
                if (roomResult != null && !roomResult.isSuccess()) {
                    sb.append("Room (").append(roomResult.getMessage()).append(")");
                }
                message = sb.toString();
            }

            if (callback != null) {
                final String finalMessage = message;
                mainHandler.post(() -> callback.onSyncComplete(success, finalMessage));
            }
        });
    }

    /**
     * Record a timestamped action to the cloud (async, fire-and-forget).
     * Used during co-location to log room interactions.
     */
    public void recordAction(String roomId, String accessCode, String actionText, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.recordAction(roomId, accessCode, actionText);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Fetch recent timestamped actions for a room (async).
     */
    public void getRecentActions(String roomId, long sinceEpochSeconds, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getRecentActions(roomId, sinceEpochSeconds);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    public void adminResetAllRooms(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.adminResetAllRooms(accessCode);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    public void adminResetAllNotes(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.adminResetAllNotes(accessCode);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    public void adminResetAllPlayers(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.adminResetAllPlayers(accessCode);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    public void adminResetAllTrades(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.adminResetAllTrades(accessCode);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    public void adminResetAllActions(String accessCode, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.adminResetAllActions(accessCode);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Run a task on the background executor where network I/O is permitted.
     */
    public void executeInBackground(Runnable task) {
        executor.execute(task);
    }

    /**
     * Run a low-priority prefetch task on a separate thread pool so it
     * doesn't block navigation or sync operations on the main executor.
     */
    public void executeInPrefetchPool(Runnable task) {
        prefetchExecutor.execute(task);
    }

    public void shutdown() {
        executor.shutdown();
        prefetchExecutor.shutdown();
    }
}
