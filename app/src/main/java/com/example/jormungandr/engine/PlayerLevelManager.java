package com.example.jormungandr.engine;

import com.example.jormungandr.model.Player;
import com.example.jormungandr.model.PlayerStats;
import com.example.jormungandr.model.InventorySlot;
import com.example.jormungandr.util.Constants;
import com.example.jormungandr.util.FormulaHelper;

public class PlayerLevelManager {

    /**
     * Award XP to the player and handle level ups.
     * @return true if the player leveled up
     */
    public static boolean awardXp(Player player, int xp) {
        player.setXp(player.getXp() + xp);
        boolean leveledUp = false;

        while (player.getXp() >= player.getXpToNext()) {
            player.setXp(player.getXp() - player.getXpToNext());
            player.setLevel(player.getLevel() + 1);
            player.setXpToNext(FormulaHelper.calculateXpToNext(player.getLevel()));
            player.getStats().setAvailablePoints(
                    player.getStats().getAvailablePoints() + Constants.STAT_POINTS_PER_LEVEL);
            leveledUp = true;
        }

        if (leveledUp) {
            FormulaHelper.recalculatePlayerStats(player);
            // Heal to full on level up
            player.setHp(player.getMaxHp());
            player.setMana(player.getMaxMana());
            player.setStamina(player.getMaxStamina());
            // Expand inventory if needed
            expandInventory(player);
        }

        return leveledUp;
    }

    /**
     * Invest a stat point into a specific stat.
     * @return true if the point was invested successfully
     */
    public static boolean investStatPoint(Player player, String stat) {
        PlayerStats stats = player.getStats();
        if (stats.getAvailablePoints() <= 0) return false;

        switch (stat.toLowerCase()) {
            case "strength":
                stats.setStrength(stats.getStrength() + 1);
                break;
            case "constitution":
                stats.setConstitution(stats.getConstitution() + 1);
                break;
            case "intelligence":
                stats.setIntelligence(stats.getIntelligence() + 1);
                break;
            case "wisdom":
                stats.setWisdom(stats.getWisdom() + 1);
                break;
            case "charisma":
                stats.setCharisma(stats.getCharisma() + 1);
                break;
            case "dexterity":
                stats.setDexterity(stats.getDexterity() + 1);
                break;
            default:
                return false;
        }

        stats.setAvailablePoints(stats.getAvailablePoints() - 1);
        FormulaHelper.recalculatePlayerStats(player);
        expandInventory(player);
        return true;
    }

    private static void expandInventory(Player player) {
        int targetSlots = player.getMaxInventorySlots();
        while (player.getInventory().size() < targetSlots) {
            player.getInventory().add(new InventorySlot(null, 0, player.getInventory().size()));
        }
    }
}
