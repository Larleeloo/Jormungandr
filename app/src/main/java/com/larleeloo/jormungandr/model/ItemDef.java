package com.larleeloo.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

public class ItemDef {
    private String itemId;
    private String displayName;
    private String description;
    private String type; // Maps to ItemType enum name
    private String subType;
    private String rarity; // Maps to Rarity enum name
    private int maxStack;
    private int damage;
    private int defense;
    private int healAmount;
    private int manaRestore;
    private int staminaRestore;
    private String equipSlot;
    private String spriteId;
    private String spritePath;
    private String placeholderColor;
    private String placeholderShape;
    private List<String> actions;
    private List<Integer> debuffRegions;
    private int buyPrice;
    private int sellPrice;

    public ItemDef() {
        this.actions = new ArrayList<>();
        this.debuffRegions = new ArrayList<>();
        this.maxStack = 16;
    }

    public Rarity getRarityEnum() {
        try {
            return Rarity.valueOf(rarity.toUpperCase());
        } catch (Exception e) {
            return Rarity.COMMON;
        }
    }

    public ItemType getItemTypeEnum() {
        try {
            return ItemType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return ItemType.MISC;
        }
    }

    public int getPlaceholderColorInt() {
        try {
            return (int) Long.parseLong(placeholderColor.replace("#", ""), 16) | 0xFF000000;
        } catch (Exception e) {
            return 0xFF888888;
        }
    }

    public boolean isEquippable() {
        return equipSlot != null && !equipSlot.isEmpty();
    }

    public boolean isConsumable() {
        ItemType t = getItemTypeEnum();
        return t == ItemType.POTION || t == ItemType.FOOD || t == ItemType.SCROLL;
    }

    public boolean isUsable() {
        return actions != null && actions.contains("USE");
    }

    // Getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public int getMaxStack() { return maxStack; }
    public void setMaxStack(int maxStack) { this.maxStack = maxStack; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }
    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }
    public int getManaRestore() { return manaRestore; }
    public void setManaRestore(int manaRestore) { this.manaRestore = manaRestore; }
    public int getStaminaRestore() { return staminaRestore; }
    public void setStaminaRestore(int staminaRestore) { this.staminaRestore = staminaRestore; }
    public String getEquipSlot() { return equipSlot; }
    public void setEquipSlot(String equipSlot) { this.equipSlot = equipSlot; }
    public String getSpriteId() { return spriteId; }
    public void setSpriteId(String spriteId) { this.spriteId = spriteId; }
    public String getSpritePath() { return spritePath; }
    public void setSpritePath(String spritePath) { this.spritePath = spritePath; }
    public String getPlaceholderColor() { return placeholderColor; }
    public void setPlaceholderColor(String placeholderColor) { this.placeholderColor = placeholderColor; }
    public String getPlaceholderShape() { return placeholderShape; }
    public void setPlaceholderShape(String placeholderShape) { this.placeholderShape = placeholderShape; }
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public List<Integer> getDebuffRegions() { return debuffRegions; }
    public void setDebuffRegions(List<Integer> debuffRegions) { this.debuffRegions = debuffRegions; }
    public int getBuyPrice() { return buyPrice; }
    public void setBuyPrice(int buyPrice) { this.buyPrice = buyPrice; }
    public int getSellPrice() { return sellPrice; }
    public void setSellPrice(int sellPrice) { this.sellPrice = sellPrice; }
}
