package com.larleeloo.jormungandr.engine;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
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

    /** Immutable snapshot of the last poll result (main-thread safe). */
    private volatile List<NearbyPlayer> lastNearby = Collections.emptyList();

    private Runnable pollRunnable;

    public ProximityManager(AppsScriptClient client) {
        this.client = client;
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

    /** Stop polling (e.g., when the player leaves the room view). */
    public void stop() {
        running.set(false);
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
        }
    }

    /** Update the room being tracked (e.g., player navigated). */
    public void updateRoom(String roomId) {
        this.currentRoomId = roomId;
        // Trigger immediate re-poll
        if (running.get()) {
            schedulePoll(0);
        }
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
                List<NearbyPlayer> nearby;
                if (result.isSuccess() && result.getData() != null) {
                    nearby = JsonHelper.listFromJson(result.getData(), NearbyPlayer.class);
                    if (nearby == null) nearby = new ArrayList<>();
                } else {
                    nearby = new ArrayList<>();
                }

                lastNearby = Collections.unmodifiableList(nearby);

                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onNearbyPlayersChanged(lastNearby);
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
    }
}
