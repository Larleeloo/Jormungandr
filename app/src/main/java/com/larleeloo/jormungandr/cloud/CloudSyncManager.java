package com.larleeloo.jormungandr.cloud;

import android.os.Handler;
import android.os.Looper;

import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.data.JsonHelper;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.PlayerNote;
import com.larleeloo.jormungandr.model.Room;

import java.util.List;

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
     * Download room data from Drive (async). Merges notes from cloud into local room.
     */
    public void syncRoomFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getRoom(roomId);
            if (result.isSuccess() && result.getData() != null) {
                Room cloudRoom = JsonHelper.fromJson(result.getData(), Room.class);
                GameRepository repo = GameRepository.getInstance();
                if (cloudRoom != null && repo != null) {
                    Room localRoom = repo.getCurrentRoom();
                    // Merge cloud notes into local room (notes are the main cross-device data)
                    if (localRoom != null && localRoom.getRoomId() != null
                            && localRoom.getRoomId().equals(roomId)
                            && cloudRoom.getPlayerNotes() != null) {
                        for (PlayerNote cloudNote : cloudRoom.getPlayerNotes()) {
                            boolean exists = false;
                            for (PlayerNote localNote : localRoom.getPlayerNotes()) {
                                if (localNote.getText().equals(cloudNote.getText())
                                        && localNote.getPlayerName().equals(cloudNote.getPlayerName())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                localRoom.getPlayerNotes().add(cloudNote);
                            }
                        }
                        repo.saveCurrentRoom();
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
     * Matches the Apps Script saveNote action: {roomId, code, note}.
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
     * @deprecated Use {@link #syncNoteToCloud} for individual notes instead.
     */
    public void syncNotesToCloud(String roomId, List<PlayerNote> notes, SyncCallback callback) {
        executor.execute(() -> {
            String json = JsonHelper.toJson(notes);
            SyncResult result = client.saveNotes(roomId, json);
            if (callback != null) {
                mainHandler.post(() -> callback.onSyncComplete(result.isSuccess(), result.getMessage()));
            }
        });
    }

    /**
     * Download notes for a room from Drive (async) and merge into local room.
     */
    public void syncNotesFromCloud(String roomId, SyncCallback callback) {
        executor.execute(() -> {
            SyncResult result = client.getNotes(roomId);
            if (result.isSuccess() && result.getData() != null) {
                GameRepository repo = GameRepository.getInstance();
                if (repo != null) {
                    Room localRoom = repo.getCurrentRoom();
                    if (localRoom != null && localRoom.getRoomId() != null
                            && localRoom.getRoomId().equals(roomId)) {
                        List<PlayerNote> cloudNotes = JsonHelper.listFromJson(
                                result.getData(), PlayerNote.class);
                        if (cloudNotes != null) {
                            for (PlayerNote cloudNote : cloudNotes) {
                                boolean exists = false;
                                for (PlayerNote localNote : localRoom.getPlayerNotes()) {
                                    if (localNote.getText().equals(cloudNote.getText())
                                            && localNote.getPlayerName().equals(cloudNote.getPlayerName())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    localRoom.getPlayerNotes().add(cloudNote);
                                }
                            }
                            repo.saveCurrentRoom();
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
