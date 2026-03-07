package com.larleeloo.jormungandr.model;

public enum ActionType {
    SWING("Swing", "Swing your weapon at the enemy", true),
    THROW("Throw", "Throw the item at the enemy (consumes item)", true),
    BLOCK("Block", "Block incoming damage with equipped shield", true),
    SHOOT("Shoot", "Fire a ranged weapon (requires ammo)", true),
    CAST("Cast", "Cast a spell from a scroll or staff", true),
    USE("Use", "Use a consumable item (potions, food, buffs)", false),
    DROP("Drop", "Drop the item on the ground", false);

    private final String displayName;
    private final String description;
    private final boolean isCombatAction;

    ActionType(String displayName, String description, boolean isCombatAction) {
        this.displayName = displayName;
        this.description = description;
        this.isCombatAction = isCombatAction;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isCombatAction() { return isCombatAction; }
}
