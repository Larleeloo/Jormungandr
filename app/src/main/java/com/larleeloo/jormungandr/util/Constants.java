package com.larleeloo.jormungandr.util;

public final class Constants {
    private Constants() {}

    // Game version
    public static final String GAME_VERSION = "1.0";

    // Inventory
    public static final int MIN_INVENTORY_SLOTS = 8;
    public static final int MAX_INVENTORY_SLOTS = 48;
    public static final int MAX_STACK_SIZE = 16;

    // Equipment
    public static final int MIN_EQUIPMENT_SLOTS = 8;
    public static final int MAX_EQUIPMENT_SLOTS = 48;

    // World
    public static final int NUM_REGIONS = 8;
    public static final int ROOMS_PER_REGION = 10000;

    // Grid maze topology (100x100 per region)
    public static final int GRID_SIZE = 100;
    public static final double MAZE_EXTRA_CONNECTION_CHANCE = 0.08;

    // Room generation
    public static final double CREATURE_DEN_CHANCE = 0.20;
    public static final double EMPTY_ROOM_CHANCE = 0.80;
    public static final int MIN_CHESTS_PER_ROOM = 0;
    public static final int MAX_CHESTS_PER_ROOM = 3;
    public static final int MIN_ITEMS_PER_CHEST = 1;
    public static final int MAX_ITEMS_PER_CHEST = 5;

    // Stamina costs
    public static final int STAMINA_COST_MOVE = 1;
    public static final int STAMINA_COST_SWING = 2;
    public static final int STAMINA_COST_SHOOT = 1;
    public static final int STAMINA_COST_BLOCK = 1;
    public static final int STAMINA_COST_THROW = 1;
    public static final int STAMINA_REGEN_PER_ROOM = 1;

    // Combat
    public static final double DIFFICULTY_SCALE_PER_LEVEL = 0.15;

    // Player defaults
    public static final int STARTING_HP = 20;
    public static final int STARTING_MANA = 10;
    public static final int STARTING_STAMINA = 10;
    public static final int STAT_POINTS_PER_LEVEL = 3;
    public static final int BASE_XP_TO_LEVEL = 100;
    public static final double XP_SCALING_FACTOR = 1.5;

    // Player starting location
    public static final int HUB_REGION = 0;
    public static final String HUB_ROOM_ID = "r0_00000";

    // Cloud sync - set APPS_SCRIPT_URL after deploying your Apps Script
    // See cloud/apps-script/Code.gs for deployment instructions.
    // Steps: 1) Deploy Code.gs as Web App in Google Apps Script
    //        2) Create 3 Drive folders (players, rooms, notes)
    //        3) Set folder IDs in Code.gs
    //        4) Paste the deployed Web App URL here
    public static final String APPS_SCRIPT_URL = "";
    public static final String PLAYER_FOLDER_ID = "12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv";
    public static final String ROOM_FOLDER_ID = "1lVx_0npSW4JVaiSr98VQyOpRjxMZ-pfg";
    public static final String NOTES_FOLDER_ID = "16iJKA0ch5gUDL6yJ_zVa0c4xMBPrrmLG";

    // Access codes (25 initial testing codes)
    public static final int MAX_ACCESS_CODES = 25;

    // Region colors (ARGB)
    public static final int COLOR_RED_DUNGEON = 0xFF8B0000;
    public static final int COLOR_VOLCANIC = 0xFFFF4500;
    public static final int COLOR_MEADOW = 0xFFFFD700;
    public static final int COLOR_FOREST = 0xFF228B22;
    public static final int COLOR_OCEAN = 0xFF1E90FF;
    public static final int COLOR_CASTLE = 0xFFFF69B4;
    public static final int COLOR_ICE_CAVE = 0xFF87CEEB;
    public static final int COLOR_VOID = 0xFF4B0082;
    public static final int COLOR_HUB = 0xFFDEB887;

    // Door dimensions (relative to canvas)
    public static final float DOOR_WIDTH = 0.12f;
    public static final float DOOR_HEIGHT = 0.25f;

    // Monster encounter scaling
    public static final double BASE_ENCOUNTER_CHANCE = 0.05;
    public static final double ENCOUNTER_CHANCE_PER_ROOM = 0.005;

    // Waypoint room legacy ID (kept for save compat)
    public static final int WAYPOINT_ROOM_ID = 500;

    // World seed for deterministic generation
    public static final long WORLD_SEED = 0x4A6F726D756E4CL;
}
