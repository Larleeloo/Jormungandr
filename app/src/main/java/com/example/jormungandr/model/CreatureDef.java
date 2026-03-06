package com.example.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

public class CreatureDef {
    private String creatureId;
    private String displayName;
    private int region;
    private int baseHp;
    private int baseAttack;
    private int baseDefense;
    private int xpReward;
    private String spriteId;
    private String spritePath;
    private String placeholderColor;
    private List<String> abilities;
    private List<LootEntry> lootTable;
    private String description;

    public CreatureDef() {
        this.abilities = new ArrayList<>();
        this.lootTable = new ArrayList<>();
    }

    public int getPlaceholderColorInt() {
        try {
            return (int) Long.parseLong(placeholderColor.replace("#", ""), 16) | 0xFF000000;
        } catch (Exception e) {
            return 0xFFCC0000;
        }
    }

    // Getters and setters
    public String getCreatureId() { return creatureId; }
    public void setCreatureId(String creatureId) { this.creatureId = creatureId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getRegion() { return region; }
    public void setRegion(int region) { this.region = region; }
    public int getBaseHp() { return baseHp; }
    public void setBaseHp(int baseHp) { this.baseHp = baseHp; }
    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = baseDefense; }
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    public String getSpriteId() { return spriteId; }
    public void setSpriteId(String spriteId) { this.spriteId = spriteId; }
    public String getSpritePath() { return spritePath; }
    public void setSpritePath(String spritePath) { this.spritePath = spritePath; }
    public String getPlaceholderColor() { return placeholderColor; }
    public void setPlaceholderColor(String placeholderColor) { this.placeholderColor = placeholderColor; }
    public List<String> getAbilities() { return abilities; }
    public void setAbilities(List<String> abilities) { this.abilities = abilities; }
    public List<LootEntry> getLootTable() { return lootTable; }
    public void setLootTable(List<LootEntry> lootTable) { this.lootTable = lootTable; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
