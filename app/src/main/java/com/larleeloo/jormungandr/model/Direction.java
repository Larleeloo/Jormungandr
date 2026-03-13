package com.larleeloo.jormungandr.model;

/**
 * Four cardinal directions used for room connections.
 * In the grid maze: FORWARD=North (row-1), BACK=South (row+1),
 * LEFT=West (col-1), RIGHT=East (col+1).
 */
public enum Direction {
    LEFT("Left", 0, -1),
    RIGHT("Right", 0, 1),
    FORWARD("Forward", -1, 0),
    BACK("Back", 1, 0);

    private final String displayName;
    private final int dRow;
    private final int dCol;

    Direction(String displayName, int dRow, int dCol) {
        this.displayName = displayName;
        this.dRow = dRow;
        this.dCol = dCol;
    }

    public String getDisplayName() { return displayName; }
    public int getDRow() { return dRow; }
    public int getDCol() { return dCol; }

    public Direction opposite() {
        switch (this) {
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            case FORWARD: return BACK;
            case BACK: return FORWARD;
            default: return BACK;
        }
    }
}
