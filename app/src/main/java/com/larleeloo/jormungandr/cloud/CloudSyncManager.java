package com.larleeloo.jormungandr.cloud;

import android.os.Handler;
import android.os.Looper;

import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.data.JsonHelper;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates cloud sync at save points (waypoint visit, hub entry, app pause).
 * Local-first: gameplay never blocks on network. All sync is async.
 */
public class CloudSyncManager {

    public interface SyncCallback {
        void onSyncComplete(boolean success, String message);
    }

    private final AppsScriptClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public CloudSyncManager() {
        this.client = new AppsScriptClient();
        this.executor = Executors.newSingleThreadExecutor();
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
     * Download player data from Drive (async).
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
                        repo.savePlayer();
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
     * Download room data from Drive (async).
     */
    public void syncRoomFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getRoom(roomId);
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
     * Returns detailed error messages for UI feedback.
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

    public void shutdown() {
        executor.shutdown();
    }
}
