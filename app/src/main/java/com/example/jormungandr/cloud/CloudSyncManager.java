package com.example.jormungandr.cloud;

import android.os.Handler;
import android.os.Looper;

import com.example.jormungandr.data.GameRepository;
import com.example.jormungandr.data.JsonHelper;
import com.example.jormungandr.model.Player;
import com.example.jormungandr.model.Room;

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
     * Full sync at save point: upload player + current room.
     */
    public void fullSync(Player player, Room room, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult playerResult = client.savePlayer(player.getAccessCode(), JsonHelper.toJson(player));
            SyncResult roomResult = null;
            if (room != null) {
                roomResult = client.saveRoom(room.getRoomId(), JsonHelper.toJson(room));
            }
            boolean success = playerResult.isSuccess() && (roomResult == null || roomResult.isSuccess());
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(success,
                        success ? "Sync complete!" : "Sync failed"));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
