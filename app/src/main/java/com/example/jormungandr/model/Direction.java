package com.example.jormungandr.model;

public enum Direction {
    LEFT("Left"),
    RIGHT("Right"),
    FORWARD("Forward"),
    BACK("Back");

    private final String displayName;

    Direction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

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
