package com.larleeloo.jormungandr.engine;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.CloudSyncManager;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.data.JsonHelper;
import com.larleeloo.jormungandr.model.NearbyPlayer;
import com.larleeloo.jormungandr.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls the cloud for other players near the local player's
 * current room. When players are co-located (same room or within
 * {@link Constants#PROXIMITY_RANGE}), listeners are notified so the UI can
 * show the proximity indicator bar and actions become timestamped.
 *
 * The poll interval speeds up when at least one player is co-located
 * ({@link Constants#PROXIMITY_POLL_ACTIVE_MS}) and slows back down when
 * the player is alone ({@link Constants#PROXIMITY_POLL_INTERVAL_MS}).
 *
 * <h3>Action file cleanup</h3>
 * Every co-location session writes action entries to a per-room JSON file on
 * Google Drive. These files must be cleaned up when the session ends, or they
 * will accumulate across all 80,000+ rooms. The game has no explicit "log out"
 * event — the only reliable signals are:
 * <ul>
 *   <li>{@link #stop()} — called when the player pauses the app or leaves
 *       the room view</li>
 *   <li>{@link #updateRoom(String)} — called when the player navigates to a
 *       new room (we clean up the old room)</li>
 * </ul>
 * At these points, we fire a lightweight cleanup request to the server so it
 * can prune expired entries and delete the file if it's fully stale. This is
 * complemented by server-side TTL pruning on every read/write and an hourly
 * scheduled sweep for files that no client ever touches again.
 */
public class ProximityManager {
    private static final String TAG = "ProximityManager";

    public interface ProximityListener {
        /** Called on the main thread whenever the nearby-player list changes. */
        void onNearbyPlayersChanged(List<NearbyPlayer> nearbyPlayers);
    }

    private final AppsScriptClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ProximityListener listener;
    private String currentAccessCode;
    private String currentRoomId;

    /**
     * Optional reference to CloudSyncManager for triggering action file
     * cleanup when polling stops. Nullable because ProximityManager can
     * function without it (cleanup just won't happen client-side).
     */
    private CloudSyncManager cloudSyncManager;

    /** Optional TurnManager to coordinate turn-based interactions during co-location. */
    private TurnManager turnManager;

    /** Immutable snapshot of the last poll result (main-thread safe). */
    private volatile List<NearbyPlayer> lastNearby = Collections.emptyList();

    private Runnable pollRunnable;

    public ProximityManager(AppsScriptClient client) {
        this.client = client;
    }

    /**
     * Set the CloudSyncManager used to trigger action cleanup on disconnect.
     * Must be called before {@link #start} for cleanup to work.
     */
    public void setCloudSyncManager(CloudSyncManager cloudSyncManager) {
        this.cloudSyncManager = cloudSyncManager;
    }

    public void setTurnManager(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    public TurnManager getTurnManager() {
        return turnManager;
    }

    public void setListener(ProximityListener listener) {
        this.listener = listener;
    }

    /** Start polling for the given player in the given room. */
    public void start(String accessCode, String roomId) {
        this.currentAccessCode = accessCode;
        this.currentRoomId = roomId;

        if (running.getAndSet(true)) {
            // Already running — just update the room and trigger an immediate poll
            schedulePoll(0);
            return;
        }
        schedulePoll(0);
    }

    /**
     * Stop polling (e.g., when the player pauses the app or leaves the room).
     *
     * CLEANUP: When polling stops, the player is effectively "disconnecting"
     * from the co-location system. Any action data for their current room
     * may now be stale (no one is watching anymore). We ask the server to
     * prune the action file for this room so it doesn't sit on Drive
     * indefinitely. This is fire-and-forget — if the request fails, the
     * server's hourly sweep or the next player's read/write will handle it.
     */
    public void stop() {
        running.set(false);
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
        }

        // Leave the turn queue if active
        if (turnManager != null) {
            turnManager.deactivate();
        }

        // Trigger cleanup for the room we were tracking. The player is leaving,
        // so any action entries older than ACTION_TTL_SECONDS can be pruned, and
        // if the file is entirely expired it will be deleted from Drive.
        requestActionCleanup(currentRoomId);
    }

    /**
     * Update the room being tracked (e.g., player navigated to a new room).
     *
     * CLEANUP: The player is leaving oldRoom and entering newRoom. The action
     * file for oldRoom may no longer have any active participants after this
     * player leaves, so we request a cleanup. This prevents action files from
     * accumulating as players explore — without this, every room a player
     * passes through during a session would retain its action file until the
     * hourly sweep.
     */
    public void updateRoom(String roomId) {
        String oldRoom = this.currentRoomId;
        this.currentRoomId = roomId;

        // Leave the turn queue for the old room
        if (turnManager != null && oldRoom != null && !oldRoom.equals(roomId)) {
            turnManager.updateRoom(roomId);
        }

        // Clean up the room we just left — its action file may now be stale
        // if we were the last player nearby.
        if (oldRoom != null && !oldRoom.equals(roomId)) {
            requestActionCleanup(oldRoom);
        }

        // Trigger immediate re-poll for the new room
        if (running.get()) {
            schedulePoll(0);
        }
    }

    /**
     * Fire-and-forget request to prune a room's action file on Drive.
     * Safe to call with null — will silently no-op.
     */
    private void requestActionCleanup(String roomId) {
        if (roomId == null || cloudSyncManager == null) return;
        cloudSyncManager.cleanupActions(roomId);
    }

    public List<NearbyPlayer> getLastNearby() {
        return lastNearby;
    }

    public boolean hasCoLocatedPlayers() {
        for (NearbyPlayer np : lastNearby) {
            if (np.isSameRoom()) return true;
        }
        return false;
    }

    public boolean hasNearbyPlayers() {
        return !lastNearby.isEmpty();
    }

    /** Record a timestamped action to the cloud (fire-and-forget). */
    public void recordAction(String roomId, String accessCode, String actionText) {
        executor.execute(() -> {
            try {
                client.recordAction(roomId, accessCode, actionText);
            } catch (Exception e) {
                Log.w(TAG, "Failed to record action", e);
            }
        });
    }

    // ---- internals ----

    private void schedulePoll(long delayMs) {
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
        }
        pollRunnable = this::doPoll;
        mainHandler.postDelayed(pollRunnable, delayMs);
    }

    private void doPoll() {
        if (!running.get()) return;

        final String code = currentAccessCode;
        final String room = currentRoomId;

        executor.execute(() -> {
            try {
                SyncResult result = client.getNearbyPlayers(code, room, Constants.PROXIMITY_RANGE);
                List<NearbyPlayer> parsed;
                if (result.isSuccess() && result.getData() != null) {
                    parsed = JsonHelper.listFromJson(result.getData(), NearbyPlayer.class);
                    if (parsed == null) parsed = new ArrayList<>();
                } else {
                    parsed = new ArrayList<>();
                }

                final List<NearbyPlayer> nearby = Collections.unmodifiableList(parsed);
                lastNearby = nearby;

                // Notify TurnManager of co-location changes
                if (turnManager != null) {
                    boolean coLocated = false;
                    for (NearbyPlayer np : nearby) {
                        if (np.isSameRoom()) { coLocated = true; break; }
                    }
                    turnManager.onCoLocationChanged(coLocated, room);
                }

                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onNearbyPlayersChanged(nearby);
                    }
                    // Schedule next poll with appropriate interval
                    if (running.get()) {
                        long interval = nearby.isEmpty()
                                ? Constants.PROXIMITY_POLL_INTERVAL_MS
                                : Constants.PROXIMITY_POLL_ACTIVE_MS;
                        schedulePoll(interval);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Proximity poll failed", e);
                if (running.get()) {
                    schedulePoll(Constants.PROXIMITY_POLL_INTERVAL_MS);
                }
            }
        });
    }

    public void shutdown() {
        stop();
        executor.shutdown();
        if (turnManager != null) {
            turnManager.shutdown();
        }
    }
}
