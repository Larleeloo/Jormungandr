package com.larleeloo.jormungandr.model;

import com.larleeloo.jormungandr.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String version;
    private String accessCode;
    private String name;
    private int level;
    private int xp;
    private int xpToNext;
    private int hp;
    private int maxHp;
    private int mana;
    private int maxMana;
    private int stamina;
    private int maxStamina;
    private int gold;
    private PlayerStats stats;
    private int currentRegion;
    private String currentRoomId;
    private List<InventorySlot> inventory;
    private List<EquipmentSlot> equipment;
    private List<InventorySlot> bankStorage;
    private Map<String, List<String>> discoveredRooms; // region -> list of room IDs
    private List<String> discoveredWaypoints;
    private List<String> achievements;
    private List<String> perks;
    private List<PlayerNote> notes;
    private int roomsVisitedSinceHub;
    private String previousRoomId;
    private List<String> roomHistory;

    public Player() {
        this.version = Constants.GAME_VERSION;
        this.level = 1;
        this.xp = 0;
        this.xpToNext = Constants.BASE_XP_TO_LEVEL;
        this.hp = Constants.STARTING_HP;
        this.maxHp = Constants.STARTING_HP;
        this.mana = Constants.STARTING_MANA;
        this.maxMana = Constants.STARTING_MANA;
        this.stamina = Constants.STARTING_STAMINA;
        this.maxStamina = Constants.STARTING_STAMINA;
        this.gold = 0;
        this.stats = new PlayerStats();
        this.currentRegion = Constants.HUB_REGION;
        this.currentRoomId = Constants.HUB_ROOM_ID;
        this.inventory = new ArrayList<>();
        this.equipment = new ArrayList<>();
        this.bankStorage = new ArrayList<>();
        this.discoveredRooms = new HashMap<>();
        this.discoveredWaypoints = new ArrayList<>();
        this.achievements = new ArrayList<>();
        this.perks = new ArrayList<>();
        this.notes = new ArrayList<>();
        this.roomsVisitedSinceHub = 0;
        this.roomHistory = new ArrayList<>();

        // Initialize inventory slots
        for (int i = 0; i < Constants.MIN_INVENTORY_SLOTS; i++) {
            inventory.add(new InventorySlot(null, 0, i));
        }
    }

    public int getMaxInventorySlots() {
        int base = Constants.MIN_INVENTORY_SLOTS + stats.getStrength() * 2;
        // Backpack bonus: check if any equipped item has subType "backpack"
        for (EquipmentSlot eq : equipment) {
            if (eq != null && !eq.isEmpty()) {
                String id = eq.getItemId();
                if (id != null && id.contains("backpack")) {
                    base += 8;
                    break;
                }
            }
        }
        return Math.min(Constants.MAX_INVENTORY_SLOTS, base);
    }

    public int getMaxEquipmentSlots() {
        return Math.min(Constants.MAX_EQUIPMENT_SLOTS,
                Constants.MIN_EQUIPMENT_SLOTS + level * 2);
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void discoverRoom(String roomId, int region) {
        String regionKey = String.valueOf(region);
        if (!discoveredRooms.containsKey(regionKey)) {
            discoveredRooms.put(regionKey, new ArrayList<>());
        }
        List<String> rooms = discoveredRooms.get(regionKey);
        if (!rooms.contains(roomId)) {
            rooms.add(roomId);
        }
    }

    public boolean addItemToInventory(String itemId, int quantity) {
        // Try to stack with existing
        for (InventorySlot slot : inventory) {
            if (itemId.equals(slot.getItemId()) && slot.getQuantity() < Constants.MAX_STACK_SIZE) {
                int canAdd = Math.min(quantity, Constants.MAX_STACK_SIZE - slot.getQuantity());
                slot.setQuantity(slot.getQuantity() + canAdd);
                quantity -= canAdd;
                if (quantity <= 0) return true;
            }
        }
        // Try empty slots
        for (InventorySlot slot : inventory) {
            if (slot.isEmpty()) {
                int canAdd = Math.min(quantity, Constants.MAX_STACK_SIZE);
                slot.setItemId(itemId);
                slot.setQuantity(canAdd);
                quantity -= canAdd;
                if (quantity <= 0) return true;
            }
        }
        return quantity <= 0;
    }

    public boolean removeItemFromInventory(String itemId, int quantity) {
        for (InventorySlot slot : inventory) {
            if (itemId.equals(slot.getItemId())) {
                if (slot.getQuantity() >= quantity) {
                    slot.setQuantity(slot.getQuantity() - quantity);
                    if (slot.getQuantity() <= 0) {
                        slot.setItemId(null);
                        slot.setQuantity(0);
                    }
                    return true;
                } else {
                    quantity -= slot.getQuantity();
                    slot.setItemId(null);
                    slot.setQuantity(0);
                }
            }
        }
        return quantity <= 0;
    }

    public void compactInventory() {
        List<InventorySlot> nonEmpty = new ArrayList<>();
        for (InventorySlot slot : inventory) {
            if (!slot.isEmpty()) {
                nonEmpty.add(new InventorySlot(slot.getItemId(), slot.getQuantity(), 0));
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (i < nonEmpty.size()) {
                inventory.get(i).setItemId(nonEmpty.get(i).getItemId());
                inventory.get(i).setQuantity(nonEmpty.get(i).getQuantity());
            } else {
                inventory.get(i).setItemId(null);
                inventory.get(i).setQuantity(0);
            }
            inventory.get(i).setSlot(i);
        }
    }

    /**
     * Sanitize inventory by clearing ghost slots: any slot with quantity <= 0
     * or a null/empty itemId is fully cleared to prevent ghost items.
     */
    public void sanitizeInventory() {
        for (InventorySlot slot : inventory) {
            if (slot.getQuantity() <= 0 || slot.getItemId() == null || slot.getItemId().isEmpty()) {
                slot.setItemId(null);
                slot.setQuantity(0);
            }
        }
    }

    /**
     * Swap two inventory slots by index. Used for drag-and-drop reordering.
     */
    public void swapInventorySlots(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= inventory.size() ||
            toIndex < 0 || toIndex >= inventory.size() || fromIndex == toIndex) {
            return;
        }
        InventorySlot from = inventory.get(fromIndex);
        InventorySlot to = inventory.get(toIndex);

        String tempId = from.getItemId();
        int tempQty = from.getQuantity();

        from.setItemId(to.getItemId());
        from.setQuantity(to.getQuantity());

        to.setItemId(tempId);
        to.setQuantity(tempQty);
    }

    public void ensureInventoryCapacity() {
        int max = getMaxInventorySlots();
        while (inventory.size() < max) {
            inventory.add(new InventorySlot(null, 0, inventory.size()));
        }
    }

    public int countItem(String itemId) {
        int count = 0;
        for (InventorySlot slot : inventory) {
            if (itemId.equals(slot.getItemId())) {
                count += slot.getQuantity();
            }
        }
        return count;
    }

    public EquipmentSlot getEquipped(String slotName) {
        for (EquipmentSlot eq : equipment) {
            if (slotName.equals(eq.getEquipSlot())) return eq;
        }
        return null;
    }

    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public int getXpToNext() { return xpToNext; }
    public void setXpToNext(int xpToNext) { this.xpToNext = xpToNext; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }
    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }
    public int getStamina() { return stamina; }
    public void setStamina(int stamina) { this.stamina = stamina; }
    public int getMaxStamina() { return maxStamina; }
    public void setMaxStamina(int maxStamina) { this.maxStamina = maxStamina; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public PlayerStats getStats() { return stats; }
    public void setStats(PlayerStats stats) { this.stats = stats; }
    public int getCurrentRegion() { return currentRegion; }
    public void setCurrentRegion(int currentRegion) { this.currentRegion = currentRegion; }
    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }
    public List<InventorySlot> getInventory() { return inventory; }
    public void setInventory(List<InventorySlot> inventory) { this.inventory = inventory; }
    public List<EquipmentSlot> getEquipment() { return equipment; }
    public void setEquipment(List<EquipmentSlot> equipment) { this.equipment = equipment; }
    public List<InventorySlot> getBankStorage() { return bankStorage; }
    public void setBankStorage(List<InventorySlot> bankStorage) { this.bankStorage = bankStorage; }
    public Map<String, List<String>> getDiscoveredRooms() { return discoveredRooms; }
    public void setDiscoveredRooms(Map<String, List<String>> discoveredRooms) { this.discoveredRooms = discoveredRooms; }
    public List<String> getDiscoveredWaypoints() { return discoveredWaypoints; }
    public void setDiscoveredWaypoints(List<String> discoveredWaypoints) { this.discoveredWaypoints = discoveredWaypoints; }
    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }
    public List<String> getPerks() { return perks; }
    public void setPerks(List<String> perks) { this.perks = perks; }
    public List<PlayerNote> getNotes() { return notes; }
    public void setNotes(List<PlayerNote> notes) { this.notes = notes; }
    public int getRoomsVisitedSinceHub() { return roomsVisitedSinceHub; }
    public void setRoomsVisitedSinceHub(int roomsVisitedSinceHub) { this.roomsVisitedSinceHub = roomsVisitedSinceHub; }
    public String getPreviousRoomId() { return previousRoomId; }
    public void setPreviousRoomId(String previousRoomId) { this.previousRoomId = previousRoomId; }
    public List<String> getRoomHistory() {
        if (roomHistory == null) roomHistory = new ArrayList<>();
        return roomHistory;
    }
    public void setRoomHistory(List<String> roomHistory) { this.roomHistory = roomHistory; }

    public void pushRoomToHistory(String roomId) {
        if (roomHistory == null) roomHistory = new ArrayList<>();
        // Don't push duplicates at the top
        if (!roomHistory.isEmpty() && roomHistory.get(roomHistory.size() - 1).equals(roomId)) return;
        roomHistory.add(roomId);
    }

    public String popRoomFromHistory() {
        if (roomHistory == null || roomHistory.isEmpty()) return null;
        return roomHistory.remove(roomHistory.size() - 1);
    }
}
