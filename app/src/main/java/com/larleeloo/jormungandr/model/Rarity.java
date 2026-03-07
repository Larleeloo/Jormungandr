package com.larleeloo.jormungandr.model;

public enum Rarity {
    COMMON("Common", 0xFFFFFFFF, 0.50),
    UNCOMMON("Uncommon", 0xFF00FF00, 0.25),
    RARE("Rare", 0xFF0088FF, 0.15),
    EPIC("Epic", 0xFFAA00FF, 0.08),
    LEGENDARY("Legendary", 0xFFFF8800, 0.015),
    MYTHIC("Mythic", 0xFF00FFFF, 0.005);

    private final String displayName;
    private final int glowColor;
    private final double dropRate;

    Rarity(String displayName, int glowColor, double dropRate) {
        this.displayName = displayName;
        this.glowColor = glowColor;
        this.dropRate = dropRate;
    }

    public String getDisplayName() { return displayName; }
    public int getGlowColor() { return glowColor; }
    public double getDropRate() { return dropRate; }

    public static Rarity rollRarity(double roll) {
        double cumulative = 0;
        for (Rarity r : values()) {
            cumulative += r.dropRate;
            if (roll < cumulative) return r;
        }
        return COMMON;
    }
}
