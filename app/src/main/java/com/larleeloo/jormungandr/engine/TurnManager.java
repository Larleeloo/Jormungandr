package com.larleeloo.jormungandr.engine;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.data.JsonHelper;
import com.larleeloo.jormungandr.model.TurnState;
import com.larleeloo.jormungandr.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the turn-based interaction queue when multiple players occupy
 * the same room. Polls the server for turn state and notifies listeners
 * when it's the local player's turn.
 *
 * <p>Turn-based mode activates automatically when ProximityManager detects
 * co-located players (same room). Each player takes turns performing
 * actions (opening chests, picking up items, fighting creatures). After
 * each action the turn advances to the next player. Players can always
 * leave the room regardless of whose turn it is.</p>
 *
 * <p>The server enforces a turn timeout ({@link Constants#TURN_TIMEOUT_SECONDS}).
 * If a player holds a turn without acting, the server auto-advances when
 * the next {@code getTurnState} request detects the timeout.</p>
 */
public class TurnManager {
    private static final String TAG = "TurnManager";
    private static final long POLL_INTERVAL_MS = 2000;

    public interface TurnListener {
        /** Called on the main thread when the turn state changes. */
        void onTurnStateChanged(TurnState state, boolean isMyTurn);
    }

    private final AppsScriptClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean polling = new AtomicBoolean(false);

    private TurnListener listener;
    private String accessCode;
    private String currentRoomId;
    private volatile TurnState lastState;
    private Runnable pollRunnable;

    public TurnManager(AppsScriptClient client) {
        this.client = client;
    }

    public void setListener(TurnListener listener) {
        this.listener = listener;
    }

    /**
     * Join the turn queue for a room and start polling for turn state.
     * Called when ProximityManager detects a co-located player.
     */
    public void activate(String accessCode, String roomId) {
        this.accessCode = accessCode;
        this.currentRoomId = roomId;

        executor.execute(() -> {
            try {
                SyncResult result = client.joinTurnQueue(roomId, accessCode);
                if (result.isSuccess() && result.getData() != null) {
                    TurnState state = JsonHelper.fromJson(result.getData(), TurnState.class);
                    lastState = state;
                    notifyListener(state);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to join turn queue", e);
            }
        });

        startPolling();
    }

    /**
     * Leave the turn queue and stop polling. Called when the player
     * leaves the room or when co-location ends.
     */
    public void deactivate() {
        stopPolling();

        final String room = currentRoomId;
        final String code = accessCode;
        if (room != null && code != null) {
            executor.execute(() -> {
                try {
                    client.leaveTurnQueue(room, code);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to leave turn queue", e);
                }
            });
        }

        lastState = null;
        currentRoomId = null;
    }

    /**
     * End the local player's turn after performing an action.
     * The server advances to the next player in the queue.
     */
    public void endMyTurn() {
        final String room = currentRoomId;
        final String code = accessCode;
        if (room == null || code == null) return;

        executor.execute(() -> {
            try {
                SyncResult result = client.endTurn(room, code);
                if (result.isSuccess() && result.getData() != null) {
                    TurnState state = JsonHelper.fromJson(result.getData(), TurnState.class);
                    lastState = state;
                    notifyListener(state);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to end turn", e);
            }
        });
    }

    /**
     * Returns true if it's the local player's turn, or if turn-based
     * mode is not active (solo play — always allowed).
     */
    public boolean isMyTurn() {
        TurnState state = lastState;
        if (state == null || !state.isTurnBasedActive()) return true;
        return state.isPlayersTurn(accessCode);
    }

    /**
     * Returns true if turn-based mode is currently active (2+ players).
     */
    public boolean isTurnBasedActive() {
        TurnState state = lastState;
        return state != null && state.isTurnBasedActive();
    }

    public TurnState getLastState() {
        return lastState;
    }

    /**
     * Update the room being tracked. Leaves the old queue and joins the new one.
     */
    public void updateRoom(String newRoomId) {
        if (newRoomId == null || newRoomId.equals(currentRoomId)) return;

        final String oldRoom = currentRoomId;
        final String code = accessCode;

        // Leave old queue
        if (oldRoom != null && code != null) {
            executor.execute(() -> {
                try {
                    client.leaveTurnQueue(oldRoom, code);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to leave old turn queue", e);
                }
            });
        }

        currentRoomId = newRoomId;
        lastState = null;
    }

    /**
     * Called by ProximityManager when co-location status changes.
     * If players are co-located, join the queue. If not, leave it.
     */
    public void onCoLocationChanged(boolean hasCoLocatedPlayers, String roomId) {
        if (hasCoLocatedPlayers) {
            if (accessCode != null) {
                // Only join/re-join if we're not already active for this room
                if (!polling.get() || !roomId.equals(currentRoomId)) {
                    activate(accessCode, roomId);
                }
            }
        } else {
            deactivate();
        }
    }

    // ---- Polling ----

    private void startPolling() {
        if (polling.getAndSet(true)) return;
        schedulePoll(POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        polling.set(false);
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
        }
    }

    private void schedulePoll(long delayMs) {
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
        }
        pollRunnable = this::doPoll;
        mainHandler.postDelayed(pollRunnable, delayMs);
    }

    private void doPoll() {
        if (!polling.get()) return;

        final String room = currentRoomId;
        if (room == null) return;

        executor.execute(() -> {
            try {
                SyncResult result = client.getTurnState(room);
                if (result.isSuccess() && result.getData() != null) {
                    TurnState state = JsonHelper.fromJson(result.getData(), TurnState.class);
                    lastState = state;
                    notifyListener(state);
                }
            } catch (Exception e) {
                Log.w(TAG, "Turn state poll failed", e);
            }

            if (polling.get()) {
                mainHandler.post(() -> schedulePoll(POLL_INTERVAL_MS));
            }
        });
    }

    private void notifyListener(TurnState state) {
        if (listener == null || state == null) return;
        boolean myTurn = state.isPlayersTurn(accessCode);
        mainHandler.post(() -> listener.onTurnStateChanged(state, myTurn));
    }

    public void shutdown() {
        deactivate();
        executor.shutdown();
    }
}
