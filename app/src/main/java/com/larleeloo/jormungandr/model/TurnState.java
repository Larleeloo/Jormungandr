package com.larleeloo.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the turn queue for a room with co-located players.
 * When multiple players occupy the same room, they take turns
 * interacting with room objects to ensure fair resource sharing.
 */
public class TurnState {
    private String roomId;
    private List<String> queue;
    private int currentIndex;
    private long turnStartedAt;

    public TurnState() {
        this.queue = new ArrayList<>();
        this.currentIndex = 0;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public List<String> getQueue() { return queue; }
    public void setQueue(List<String> queue) { this.queue = queue; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }

    public long getTurnStartedAt() { return turnStartedAt; }
    public void setTurnStartedAt(long turnStartedAt) { this.turnStartedAt = turnStartedAt; }

    /**
     * Returns the access code of the player whose turn it currently is,
     * or null if the queue is empty.
     */
    public String getCurrentTurnHolder() {
        if (queue == null || queue.isEmpty()) return null;
        int idx = currentIndex % queue.size();
        return queue.get(idx);
    }

    /**
     * Check whether the given access code holds the current turn.
     */
    public boolean isPlayersTurn(String accessCode) {
        String holder = getCurrentTurnHolder();
        return holder != null && holder.equals(accessCode);
    }

    /**
     * Check whether the queue has more than one player (turn-based mode active).
     */
    public boolean isTurnBasedActive() {
        return queue != null && queue.size() > 1;
    }
}
