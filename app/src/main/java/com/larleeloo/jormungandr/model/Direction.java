package com.larleeloo.jormungandr.model;

/**
 * Four cardinal directions used for room connections in the grid maze.
 * NORTH (row-1), SOUTH (row+1), WEST (col-1), EAST (col+1).
 */
public enum Direction {
    WEST("West", 0, -1),
    EAST("East", 0, 1),
    NORTH("North", -1, 0),
    SOUTH("South", 1, 0);

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
            case WEST: return EAST;
            case EAST: return WEST;
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            default: return SOUTH;
        }
    }
}
