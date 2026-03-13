package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.LootEntry;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.data.CreatureRegistry;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.CreatureDef;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomGenerator {

    private final LootGenerator lootGenerator;
    private final ItemRegistry itemRegistry;
    private final CreatureRegistry creatureRegistry;

    public RoomGenerator(ItemRegistry itemRegistry, CreatureRegistry creatureRegistry) {
        this.itemRegistry = itemRegistry;
        this.creatureRegistry = creatureRegistry;
        this.lootGenerator = new LootGenerator(itemRegistry);
    }

    /**
     * Generate a complete room for a given region and room ID.
     */
    public Room generateRoom(int region, int roomNumber, int playerLevel) {
        String roomId = RoomIdHelper.makeRoomId(region, roomNumber);
        int difficultyTier = RoomIdHelper.getDifficultyTier(region, roomNumber);
        SeededRandom rng = new SeededRandom(SeededRandom.hashSeed(region, roomNumber));

        Room room = new Room(roomId, region, difficultyTier);
        room.setBiome(BiomeType.fromRegion(region).name().toLowerCase());

        // Set background
        room.setBackgroundId(generateBackgroundId(region, rng));

        // Check if waypoint
        room.setWaypoint(RoomIdHelper.isWaypoint(region, roomNumber));

        // Generate doors via MeshGenerator
        Map<Direction, String> doors = MeshGenerator.generateDoors(region, roomNumber);
        for (Map.Entry<Direction, String> entry : doors.entrySet()) {
            room.addDoor(entry.getKey(), entry.getValue());
        }

        // Determine room type: creature den or empty
        boolean isCreatureDen = !room.isWaypoint() && rng.nextDouble() < Constants.CREATURE_DEN_CHANCE;
        room.setCreatureDen(isCreatureDen);

        if (room.isWaypoint()) {
            generateWaypointRoom(room, rng);
        } else if (isCreatureDen) {
            generateCreatureDenRoom(room, rng, region, difficultyTier, playerLevel);
        } else {
            generateEmptyRoom(room, rng, region, difficultyTier);
        }

        room.setFirstVisitedAt(System.currentTimeMillis() / 1000);

        return room;
    }

    /**
     * Generate the hub room (region 0).
     */
    public Room generateHubRoom() {
        Room hub = new Room(Constants.HUB_ROOM_ID, 0, 0);
        hub.setBiome("hub");
        hub.setBackgroundId("hub_bg1");
        hub.setWaypoint(true);

        // Hub starts with a single NORTH door to region 1 entrance.
        // Additional portal doors are managed dynamically by HubFragment
        // based on the player's discovered waypoints.
        Map<Direction, String> doors = MeshGenerator.generateDoors(0, 0);
        for (Map.Entry<Direction, String> entry : doors.entrySet()) {
            hub.addDoor(entry.getKey(), entry.getValue());
        }

        // Decorations for the hub
        hub.getObjects().add(RoomObject.createDecoration("hub_shop", "shop_counter",
                0.1f, 0.5f, 0.2f, 0.15f));
        hub.getObjects().add(RoomObject.createDecoration("hub_storage", "storage_chest",
                0.7f, 0.5f, 0.15f, 0.12f));

        return hub;
    }

    private void generateEmptyRoom(Room room, SeededRandom rng, int region, int tier) {
        // Chests
        int numChests = rng.nextIntRange(Constants.MIN_CHESTS_PER_ROOM, Constants.MAX_CHESTS_PER_ROOM);
        for (int i = 0; i < numChests; i++) {
            List<InventorySlot> contents = lootGenerator.generateChestLoot(rng, region, tier);
            float x = rng.nextFloatRange(0.15f, 0.75f);
            float y = rng.nextFloatRange(0.5f, 0.75f);
            room.getObjects().add(RoomObject.createChest(
                    "chest_" + i, "wooden_chest", x, y, contents));
        }

        // Floor items
        List<InventorySlot> floorItems = lootGenerator.generateFloorItems(rng, tier);
        for (int i = 0; i < floorItems.size(); i++) {
            InventorySlot item = floorItems.get(i);
            float x = rng.nextFloatRange(0.1f, 0.8f);
            float y = rng.nextFloatRange(0.6f, 0.8f);
            room.getObjects().add(RoomObject.createFloorItem(
                    "floor_" + i, item.getItemId(), item.getQuantity(), x, y));
        }

        // Traps (small chance, higher tiers more likely)
        if (tier >= 2 && rng.nextDouble() < 0.15) {
            float x = rng.nextFloatRange(0.2f, 0.7f);
            float y = rng.nextFloatRange(0.5f, 0.75f);
            String trapType = rng.nextBoolean() ? "spike" : "poison_gas";
            int damage = rng.nextIntRange(3, 5 + tier * 2);
            room.getObjects().add(RoomObject.createTrap("trap_0", trapType, damage, x, y));
        }

        // Hidden items (only revealed by torch)
        if (rng.nextDouble() < 0.20) {
            List<InventorySlot> hiddenLoot = lootGenerator.generateChestLoot(rng, region, tier);
            for (int i = 0; i < hiddenLoot.size(); i++) {
                InventorySlot item = hiddenLoot.get(i);
                float hx = rng.nextFloatRange(0.1f, 0.8f);
                float hy = rng.nextFloatRange(0.4f, 0.7f);
                RoomObject hiddenItem = RoomObject.createFloorItem(
                        "hidden_" + i, item.getItemId(), item.getQuantity(), hx, hy);
                hiddenItem.setHidden(true);
                room.getObjects().add(hiddenItem);
            }
        }

        // Decorations
        addDecorations(room, rng);
    }

    private void generateCreatureDenRoom(Room room, SeededRandom rng, int region,
                                          int tier, int playerLevel) {
        // Main creature
        CreatureDef creatureDef = creatureRegistry.getRandomCreatureForRegion(rng, region);
        if (creatureDef != null) {
            int creatureLevel = playerLevel + region;
            double scaleFactor = 1.0 + Constants.DIFFICULTY_SCALE_PER_LEVEL * (creatureLevel - 1);
            int hp = (int)(creatureDef.getBaseHp() * scaleFactor);

            RoomObject creature = RoomObject.createCreature(
                    "creature_0", creatureDef.getCreatureId(), creatureLevel, hp,
                    creatureDef.getLootTable());
            room.getObjects().add(creature);
        }

        // Creature dens still have some loot
        if (rng.nextDouble() < 0.5) {
            List<InventorySlot> contents = lootGenerator.generateChestLoot(rng, region, tier);
            float x = rng.nextFloatRange(0.6f, 0.8f);
            float y = rng.nextFloatRange(0.55f, 0.7f);
            room.getObjects().add(RoomObject.createChest(
                    "chest_0", "bone_chest", x, y, contents));
        }
    }

    private void generateWaypointRoom(Room room, SeededRandom rng) {
        // Waypoint crystal decoration
        room.getObjects().add(RoomObject.createDecoration("waypoint_crystal", "crystal",
                0.4f, 0.3f, 0.2f, 0.25f));
        // Storage chest
        room.getObjects().add(RoomObject.createDecoration("waypoint_storage", "storage_chest",
                0.1f, 0.55f, 0.15f, 0.12f));
        // Trading post
        room.getObjects().add(RoomObject.createDecoration("waypoint_trade", "trade_post",
                0.7f, 0.55f, 0.15f, 0.12f));
    }

    private void addDecorations(Room room, SeededRandom rng) {
        if (rng.nextDouble() < 0.4) {
            float x = rng.nextFloatRange(0.05f, 0.85f);
            float y = rng.nextFloatRange(0.2f, 0.5f);
            String[] decors = {"torch", "pillar", "cobweb", "banner", "crack", "bones"};
            String decor = decors[rng.nextInt(decors.length)];
            room.getObjects().add(RoomObject.createDecoration(
                    "decor_0", decor, x, y, 0.06f, 0.08f));
        }
    }

    private String generateBackgroundId(int region, SeededRandom rng) {
        BiomeType biome = BiomeType.fromRegion(region);
        String baseName = biome.name().toLowerCase();
        int variant = rng.nextIntRange(1, 3); // up to 3 variants per biome
        return baseName + "_bg" + variant;
    }
}
