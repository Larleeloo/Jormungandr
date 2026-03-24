package com.larleeloo.jormungandr.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server-side constants mirroring the Android client's Constants.java
 * and the Apps Script Code.gs configuration.
 */
public final class ServerConstants {
    private ServerConstants() {}

    public static final String GAME_VERSION = "1.1";

    // Access codes
    public static final int MAX_ACCESS_CODES = 25;
    public static final List<String> VALID_CODES = new ArrayList<>();
    public static final Set<String> ADMIN_CODES = Set.of(
            "JORM-ALPHA-001", "JORM-ALPHA-002", "JORM-ALPHA-003"
    );

    static {
        for (int i = 1; i <= MAX_ACCESS_CODES; i++) {
            VALID_CODES.add(String.format("JORM-ALPHA-%03d", i));
        }
    }

    // Proximity
    public static final int PROXIMITY_RANGE = 3;

    // Action TTL
    public static final long ACTION_TTL_SECONDS = 3600;

    // Turn system
    public static final int TURN_TIMEOUT_SECONDS = 30;
}
