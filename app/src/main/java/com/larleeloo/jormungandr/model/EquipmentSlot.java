package com.larleeloo.jormungandr.model;

public class EquipmentSlot {
    private String itemId;
    private String equipSlot;

    public EquipmentSlot() {}

    public EquipmentSlot(String itemId, String equipSlot) {
        this.itemId = itemId;
        this.equipSlot = equipSlot;
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty();
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getEquipSlot() { return equipSlot; }
    public void setEquipSlot(String equipSlot) { this.equipSlot = equipSlot; }

    // Standard equipment slot names
    public static final String MAIN_HAND = "mainHand";
    public static final String OFF_HAND = "offHand";
    public static final String HEAD = "head";
    public static final String CHEST = "chest";
    public static final String LEGS = "legs";
    public static final String FEET = "feet";
    public static final String ACCESSORY_1 = "accessory1";
    public static final String ACCESSORY_2 = "accessory2";
}
