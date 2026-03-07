package com.larleeloo.jormungandr.model;

import com.larleeloo.jormungandr.util.Constants;

public enum BiomeType {
    HUB(0, "Desert Marketplace", Constants.COLOR_HUB),
    RED_DUNGEON(1, "Red Dungeon", Constants.COLOR_RED_DUNGEON),
    VOLCANIC_WASTE(2, "Volcanic Waste", Constants.COLOR_VOLCANIC),
    MEADOW(3, "Meadow", Constants.COLOR_MEADOW),
    FOREST(4, "Forest", Constants.COLOR_FOREST),
    OCEAN(5, "Ocean", Constants.COLOR_OCEAN),
    CASTLE(6, "Castle", Constants.COLOR_CASTLE),
    ICE_CAVE(7, "Ice Cave", Constants.COLOR_ICE_CAVE),
    VOID(8, "The Void", Constants.COLOR_VOID);

    private final int regionNumber;
    private final String displayName;
    private final int color;

    BiomeType(int regionNumber, String displayName, int color) {
        this.regionNumber = regionNumber;
        this.displayName = displayName;
        this.color = color;
    }

    public int getRegionNumber() { return regionNumber; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }

    public static BiomeType fromRegion(int region) {
        for (BiomeType biome : values()) {
            if (biome.regionNumber == region) return biome;
        }
        return HUB;
    }
}
