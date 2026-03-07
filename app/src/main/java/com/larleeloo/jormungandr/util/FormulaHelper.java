package com.larleeloo.jormungandr.util;

import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.PlayerStats;

public final class FormulaHelper {
    private FormulaHelper() {}

    public static int calculateMaxHp(int level, int constitution) {
        return Constants.STARTING_HP + (level - 1) * 5 + constitution * 3;
    }

    public static int calculateMaxMana(int level, int intelligence) {
        return Constants.STARTING_MANA + (level - 1) * 3 + intelligence * 4;
    }

    public static int calculateMaxStamina(int level, int dexterity) {
        return Constants.STARTING_STAMINA + (level - 1) * 3 + dexterity * 3;
    }

    public static int calculateAttackDamage(int weaponDamage, int strength, int level) {
        return weaponDamage + strength * 2 + (level - 1);
    }

    public static int calculateDefense(int armorDefense, int constitution, int dexterity) {
        return armorDefense + constitution + dexterity / 2;
    }

    public static int calculateXpToNext(int level) {
        return (int)(Constants.BASE_XP_TO_LEVEL * Math.pow(Constants.XP_SCALING_FACTOR, level - 1));
    }

    public static int calculateInventorySlots(int strength) {
        return Math.min(Constants.MAX_INVENTORY_SLOTS,
                Constants.MIN_INVENTORY_SLOTS + strength * 2);
    }

    public static int calculateEquipmentSlots(int level) {
        return Math.min(Constants.MAX_EQUIPMENT_SLOTS,
                Constants.MIN_EQUIPMENT_SLOTS + level * 2);
    }

    public static double calculateCreatureScaleFactor(int creatureLevel) {
        return 1.0 + Constants.DIFFICULTY_SCALE_PER_LEVEL * (creatureLevel - 1);
    }

    public static int calculateCreatureLevel(int playerLevel, int region) {
        return playerLevel + region;
    }

    public static double calculateEncounterChance(int roomsVisitedSinceHub, int zone) {
        return Constants.BASE_ENCOUNTER_CHANCE +
                roomsVisitedSinceHub * Constants.ENCOUNTER_CHANCE_PER_ROOM +
                zone * 0.02;
    }

    public static int calculateSellPrice(int buyPrice) {
        return Math.max(1, buyPrice / 3);
    }

    public static void recalculatePlayerStats(Player player) {
        PlayerStats stats = player.getStats();
        player.setMaxHp(calculateMaxHp(player.getLevel(), stats.getConstitution()));
        player.setMaxMana(calculateMaxMana(player.getLevel(), stats.getIntelligence()));
        player.setMaxStamina(calculateMaxStamina(player.getLevel(), stats.getDexterity()));
        player.setXpToNext(calculateXpToNext(player.getLevel()));
    }
}
