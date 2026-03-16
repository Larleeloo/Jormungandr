package com.larleeloo.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

public class RoomObject {
    private String type; // "chest", "creature", "trap", "item", "decoration"
    private String id;
    private String spriteId;
    private float x; // 0.0 - 1.0 relative to canvas
    private float y;
    private float width;
    private float height;

    // Chest fields
    private List<InventorySlot> inventory;
    private boolean opened;

    // Creature fields
    private String creatureDefId;
    private int level;
    private int hp;
    private int maxHp;
    private boolean alive;
    private List<LootEntry> lootTable;

    // Trap fields
    private String trapType;
    private int damage;
    private boolean triggered;
    private boolean hidden; // Hidden objects (traps) are not rendered until revealed

    // Item fields (loose items on the floor)
    private String itemId;
    private int quantity;

    public RoomObject() {
        this.inventory = new ArrayList<>();
        this.lootTable = new ArrayList<>();
        this.alive = true;
        this.opened = false;
        this.triggered = false;
    }

    // Static factory methods
    public static RoomObject createChest(String id, String spriteId, float x, float y,
                                          List<InventorySlot> contents) {
        RoomObject obj = new RoomObject();
        obj.type = "chest";
        obj.id = id;
        obj.spriteId = spriteId;
        obj.x = x;
        obj.y = y;
        obj.width = 0.10f;
        obj.height = 0.08f;
        obj.inventory = contents != null ? contents : new ArrayList<>();
        return obj;
    }

    public static RoomObject createCreature(String id, String creatureDefId, int level,
                                             int hp, List<LootEntry> lootTable) {
        RoomObject obj = new RoomObject();
        obj.type = "creature";
        obj.id = id;
        obj.creatureDefId = creatureDefId;
        obj.level = level;
        obj.hp = hp;
        obj.maxHp = hp;
        obj.lootTable = lootTable != null ? lootTable : new ArrayList<>();
        obj.x = 0.5f;
        obj.y = 0.3f;
        obj.width = 0.15f;
        obj.height = 0.20f;
        return obj;
    }

    public static RoomObject createTrap(String id, String trapType, int damage,
                                         float x, float y) {
        RoomObject obj = new RoomObject();
        obj.type = "trap";
        obj.id = id;
        obj.trapType = trapType;
        obj.damage = damage;
        obj.x = x;
        obj.y = y;
        obj.width = 0.06f;
        obj.height = 0.06f;
        obj.hidden = true; // Traps are hidden by default
        return obj;
    }

    public static RoomObject createFloorItem(String id, String itemId, int quantity,
                                              float x, float y) {
        RoomObject obj = new RoomObject();
        obj.type = "item";
        obj.id = id;
        obj.itemId = itemId;
        obj.quantity = quantity;
        obj.x = x;
        obj.y = y;
        obj.width = 0.06f;
        obj.height = 0.06f;
        return obj;
    }

    public static RoomObject createDecoration(String id, String spriteId,
                                               float x, float y, float w, float h) {
        RoomObject obj = new RoomObject();
        obj.type = "decoration";
        obj.id = id;
        obj.spriteId = spriteId;
        obj.x = x;
        obj.y = y;
        obj.width = w;
        obj.height = h;
        return obj;
    }

    public boolean isInteractable() {
        switch (type) {
            case "chest": return !opened;
            case "creature": return alive;
            case "trap": return !triggered;
            case "item": return quantity > 0;
            case "decoration":
                // Hub/waypoint furnishings are interactable
                if (spriteId != null) {
                    switch (spriteId) {
                        case "shop_counter":
                        case "storage_chest":
                        case "trade_post":
                        case "crystal":
                            return true;
                    }
                }
                return false;
            default: return false;
        }
    }

    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSpriteId() { return spriteId; }
    public void setSpriteId(String spriteId) { this.spriteId = spriteId; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public List<InventorySlot> getInventory() { return inventory; }
    public void setInventory(List<InventorySlot> inventory) { this.inventory = inventory; }
    public boolean isOpened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }
    public String getCreatureDefId() { return creatureDefId; }
    public void setCreatureDefId(String creatureDefId) { this.creatureDefId = creatureDefId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public List<LootEntry> getLootTable() { return lootTable; }
    public void setLootTable(List<LootEntry> lootTable) { this.lootTable = lootTable; }
    public String getTrapType() { return trapType; }
    public void setTrapType(String trapType) { this.trapType = trapType; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
