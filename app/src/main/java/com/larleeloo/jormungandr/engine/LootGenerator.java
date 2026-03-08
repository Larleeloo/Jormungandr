package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.LootEntry;
import com.larleeloo.jormungandr.model.Rarity;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;

public class LootGenerator {

    private final ItemRegistry itemRegistry;

    public LootGenerator(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * Generate loot for a chest based on zone depth.
     */
    public List<InventorySlot> generateChestLoot(SeededRandom rng, int region, int zone) {
        List<InventorySlot> loot = new ArrayList<>();
        int numItems = rng.nextIntRange(1, Math.min(5, zone + 1));

        for (int i = 0; i < numItems; i++) {
            Rarity rarity = rollRarity(rng, zone);
            ItemDef item = itemRegistry.getRandomItemByRarity(rng, rarity);
            if (item != null) {
                int quantity = calculateQuantity(rng, rarity, item.getMaxStack());
                loot.add(new InventorySlot(item.getItemId(), quantity, i));
            }
        }
        return loot;
    }

    /**
     * Generate loot dropped by a defeated creature.
     */
    public List<InventorySlot> generateCreatureLoot(SeededRandom rng, List<LootEntry> lootTable,
                                                     int creatureLevel) {
        List<InventorySlot> drops = new ArrayList<>();
        if (lootTable == null || lootTable.isEmpty()) return drops;

        // Roll for each entry in the loot table
        double totalWeight = 0;
        for (LootEntry entry : lootTable) totalWeight += entry.getWeight();

        int numDrops = rng.nextIntRange(1, 3);
        for (int i = 0; i < numDrops; i++) {
            double roll = rng.nextDouble() * totalWeight;
            double cumulative = 0;
            for (LootEntry entry : lootTable) {
                cumulative += entry.getWeight();
                if (roll < cumulative) {
                    int quantity = rng.nextIntRange(1, 3);
                    drops.add(new InventorySlot(entry.getItemId(), quantity, i));
                    break;
                }
            }
        }
        return drops;
    }

    /**
     * Generate random floor items for an empty room.
     */
    public List<InventorySlot> generateFloorItems(SeededRandom rng, int zone) {
        List<InventorySlot> items = new ArrayList<>();
        if (rng.nextDouble() > 0.3) return items; // 30% chance of floor items

        int numItems = rng.nextIntRange(1, 2);
        for (int i = 0; i < numItems; i++) {
            ItemDef item = itemRegistry.getRandomItemByRarity(rng, Rarity.COMMON);
            if (item != null) {
                items.add(new InventorySlot(item.getItemId(),
                        rng.nextIntRange(1, 3), i));
            }
        }
        return items;
    }

    private Rarity rollRarity(SeededRandom rng, int zone) {
        // Higher zones slightly boost rare drops
        double roll = rng.nextDouble();
        double zoneBonus = zone * 0.005; // Each zone adds 0.5% to rare+ chance

        if (roll < Rarity.MYTHIC.getDropRate() + zoneBonus * 0.05) return Rarity.MYTHIC;
        roll -= Rarity.MYTHIC.getDropRate();
        if (roll < Rarity.LEGENDARY.getDropRate() + zoneBonus * 0.1) return Rarity.LEGENDARY;
        roll -= Rarity.LEGENDARY.getDropRate();
        if (roll < Rarity.EPIC.getDropRate() + zoneBonus * 0.2) return Rarity.EPIC;
        roll -= Rarity.EPIC.getDropRate();
        if (roll < Rarity.RARE.getDropRate() + zoneBonus) return Rarity.RARE;
        roll -= Rarity.RARE.getDropRate();
        if (roll < Rarity.UNCOMMON.getDropRate()) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    private int calculateQuantity(SeededRandom rng, Rarity rarity, int maxStack) {
        int maxQty;
        switch (rarity) {
            case MYTHIC:
            case LEGENDARY: maxQty = 1; break;
            case EPIC: maxQty = Math.min(2, maxStack); break;
            case RARE: maxQty = Math.min(4, maxStack); break;
            case UNCOMMON: maxQty = Math.min(8, maxStack); break;
            default: maxQty = Math.min(16, maxStack); break;
        }
        return rng.nextIntRange(1, maxQty);
    }
}
