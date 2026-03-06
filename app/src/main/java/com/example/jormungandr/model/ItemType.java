package com.example.jormungandr.model;

public enum ItemType {
    WEAPON("Weapon"),
    ARMOR("Armor"),
    POTION("Potion"),
    SCROLL("Scroll"),
    MATERIAL("Material"),
    FOOD("Food"),
    KEY("Key"),
    TOOL("Tool"),
    ACCESSORY("Accessory"),
    MISC("Misc"),
    DYE("Dye"),
    CLOTHING("Clothing"),
    SHIELD("Shield"),
    AMMO("Ammo");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
