# Jörmungandr

A turn-based roguelike dungeon crawler for Android. Players explore a shared cloud-based world of 80,000+ procedurally generated rooms across 8 themed regions, battling creatures, collecting loot, and leaving notes for other adventurers.

## Current Version

**v1.1** (versionCode 2)

## Game Overview

- **8 regions**, each a 100x100 grid maze (10,000 rooms per region, 80,000 total)
- **1 central hub** with portal doors to discovered waypoints
- **Deterministic world generation** from a single seed (`WORLD_SEED`) — all players share the same maze layout
- **Cloud-backed persistence** — all player saves, room state, and notes live in Google Drive via Apps Script
- **25 testing access codes** (JORM-ALPHA-001 through JORM-ALPHA-025)
- **3 admin codes** (JORM-ALPHA-001, 002, 003) with full map view, room inspector, and reset capabilities

## Architecture

### World Generation

The world is built deterministically from `Constants.WORLD_SEED` by `WorldMesh`:

1. **Hub** (region 0): Single room with a NORTH door to region 1 entrance
2. **Per-region maze**: Iterative randomized DFS (recursive backtracker) carves a spanning tree on a 100x100 grid
3. **Extra connections**: 8% chance to punch additional passages, creating loops
4. **Portals**: 7 one-way portal doors per region, each targeting a different region's waypoint
5. **Waypoints**: 1 per region, placed via seeded LCG with minimum Manhattan distance 15 from entrance

Room IDs follow the format `r{region}_{5-digit-number}` (e.g., `r3_04250`). The room number encodes grid position: `number = row * 100 + col`.

Directions are cardinal: NORTH (row-1), SOUTH (row+1), WEST (col-1), EAST (col+1).

### Room Types & Generation

Rooms are generated on first visit by `RoomGenerator`:

| Room Type | Chance | Contents |
|-----------|--------|----------|
| Creature Den | 20% | Hostile creatures + loot chests (0-3 chests, 1-5 items each) |
| Empty Room | 80% | Safe passage, rare floor loot |
| Waypoint | 1 per region | NPC vendor, fast travel, notes |
| Hub | 1 total | Portals, shop, bank, trading post |

### Room Lifecycle

1. Player navigates to a room
2. `GameRepository.loadOrGenerateRoom()` checks cloud for existing room data
3. If not found, `RoomGenerator` procedurally generates content (chests, creatures, traps, items)
4. Room is saved to cloud on first generation
5. All subsequent changes (opened chests, killed creatures, picked up items) are persisted via async `CloudSyncManager`
6. Door connections always come from the pre-built `WorldMesh`, not from saved data

### Key Singletons

| Singleton | Purpose | Reset on update? |
|-----------|---------|:---:|
| `WorldMesh` | 80,000-room maze graph | Yes |
| `GameRepository` | Player/room state, registries, cloud sync | Yes |
| `GameAssetManager` | LruCache for sprite bitmaps | Yes |
| `SoundManager` | MediaPlayer + SoundPool | Yes |

All singletons are invalidated automatically when `versionCode` changes (see [Releasing Updates](#releasing-updates)).

### Cloud Sync

All persistence goes through Google Apps Script to Google Drive. No local files are stored.

| Component | Purpose |
|-----------|---------|
| `AppsScriptClient` | HTTP client for the Apps Script REST API (OkHttp3, 30s timeouts, max 4 retries with exponential backoff) |
| `CloudSyncManager` | Async cloud operations on a background executor |
| `RoomFileManager` | Cloud CRUD for room JSON |
| `PlayerFileManager` | Cloud CRUD for player JSON |

Sync points:
- **Navigation**: Leaving room uploaded, entering room downloaded
- **In-room interactions**: Chest opens, item pickups, combat wins trigger async room upload
- **App pause**: Player + current room synced to cloud
- **Combat return**: Room + player synced after victory

### Project Structure

```
app/src/main/java/com/larleeloo/jormungandr/
  activity/       MainActivity (login), GameActivity (gameplay)
  adapter/        RecyclerView adapters (actions, inventory, notes, shop, trades)
  asset/          GameAssetManager (sprites), SoundManager (audio), SpriteLoader
  cloud/          AppsScriptClient, CloudSyncManager, AccessCodeValidator
  data/           GameRepository, ItemRegistry, CreatureRegistry, file managers
  engine/         WorldMesh, RoomGenerator, CombatEngine, LootGenerator, PlayerLevelManager
  fragment/       RoomFragment, HubFragment, MapFragment, CombatFragment,
                  InventoryFragment, CharacterFragment, ShopFragment,
                  TradingPostFragment, NoteFragment, TransferFragment,
                  AdminFragment, LoadingFragment
  model/          Player, Room, RoomObject, Direction, BiomeType, ItemDef,
                  CreatureDef, CombatCreature, BuffEffect, ActionType, Rarity,
                  ItemType, InventorySlot, EquipmentSlot, PlayerNote, TradeListing, LootEntry
  util/           Constants, RoomIdHelper, SeededRandom, FormulaHelper, ColorHelper
  view/           RoomCanvasView, GridMapCanvasView, CombatCanvasView,
                  PlaceholderRenderer, HpBarView, CharacterSilhouetteView

cloud/apps-script/
  Code.gs         Google Apps Script REST API (deploy as Web App)

app/src/main/assets/
  data/           items.json (277 items), creatures.json (45 creatures)
  backgrounds/    Room background images
  entities/       Creature sprites
  items/          Item sprite directories
  sounds/         Music and SFX
```

## Regions & Biomes

| # | Biome | Color | Enemy Level Offset |
|---|-------|-------|--------------------|
| 0 | Hub (Desert Marketplace) | Tan | — |
| 1 | Red Dungeon | Dark Red | +1 |
| 2 | Volcanic Waste | Orange | +2 |
| 3 | Meadow | Gold | +3 |
| 4 | Forest | Green | +4 |
| 5 | Ocean | Blue | +5 |
| 6 | Castle | Pink | +6 |
| 7 | Ice Cave | Light Blue | +7 |
| 8 | The Void | Indigo | +8 |

## Gameplay

### Navigation

- Tap cardinal doors (N/S/E/W) to move between rooms
- Each move costs 1 stamina
- Stamina regenerates by 1 per non-waypoint room entered
- Discovering a region's waypoint adds a portal to the hub for fast travel
- Portals are one-way doors to other regions' waypoints

### Combat System

Pokemon-style turn-based battles with action selection:

| Action | Type | Stamina Cost | Notes |
|--------|------|:---:|-------|
| Swing | Melee | 2 | Standard melee attack |
| Shoot | Ranged | 2 | Ranged attack (requires bow/crossbow) |
| Cast | Magic | 3 | Magic attack (uses mana) |
| Throw | Consumable | 1 | Throw a consumable item |
| Block | Defensive | 1 | Reduces incoming damage |

- **Exhaustion**: Running out of stamina reduces all damage dealt by 50%
- **Free buff/potion usage**: Before the main action each turn, players can use a buff or potion at no stamina cost
- **Creature AI**: Weighted random ability selection from creature's ability pool
- **Loot drops**: Creatures drop items based on their loot table and rarity weights

### Character Progression

- **Level-up**: Gain 3 stat points per level to allocate freely
- **XP scaling**: Base 100 XP for level 2, multiplied by 1.5x per subsequent level
- **Stats**:

| Stat | Effect |
|------|--------|
| Strength | Melee damage, inventory capacity (8-48 slots) |
| Dexterity | Ranged damage, dodge chance |
| Constitution | Max HP, max stamina, defense |
| Intelligence | Magic damage, max mana |

- **Starting stats**: 20 HP, 10 mana, 10 stamina
- **Creature difficulty**: Scales by 0.15 per player level + region level offset

### Inventory & Equipment

- **Inventory slots**: 8 base, scaling up to 48 with Strength
- **Stack size**: Max 16 items per slot
- **Equipment slots**: Weapons, armor (chest/legs/gauntlets/helmet/boots), accessories
- **Item interaction**: Equip, use, drop, or sell items from inventory

### Shop & Economy

- **Hub vendor**: Buy and sell items at waypoints and the hub
- **Gold**: Earned from creature drops and selling items
- **Buy/sell prices**: Defined per item in `items.json`

### Trading Post

- **Player-to-player trading**: List items for sale at a set gold price
- **Trade listings**: Other players can browse and accept trade offers
- **Managed via** `TradingPostFragment` and cloud-synced `TradeListing` objects

### Bank Storage

- **Secure storage**: Separate bank inventory accessible at the hub
- **Transfer items**: Move items between inventory and bank via `TransferFragment`
- **Persistence**: Bank contents saved with player data to cloud

### Notes System

- Leave messages for other players in waypoints, creature dens, and the hub
- Notes persist in cloud and are visible to all players who visit the room
- Each note records author access code, timestamp, and content

### Death & Respawn

- On death, respawn at hub with 50% HP
- Room state (opened chests, killed creatures) is preserved
- No item or gold loss on death

### Admin Features (JORM-ALPHA-001 to 003)

- **Filled map view**: All 10,000 rooms per region visible (undiscovered rooms dimmed)
- **Room inspector**: Tap any room to see ID, grid position, doors, portals, and contents
- **Travel anywhere**: "Travel Here" button in the inspector for direct teleportation
- **Reset controls**: Admin panel to wipe all rooms, notes, or player saves from cloud

## Items & Creatures

### Item Categories (277 total in `items.json`)

| Category | Examples | Count |
|----------|----------|:---:|
| Weapons | Swords, axes, bows, staffs, daggers, crossbows | 70+ |
| Armor | Chestplates, leggings, gauntlets, helmets, boots | 60+ |
| Clothing | Shirts, dresses, hats, shoes, robes | 80+ |
| Accessories | Rings, necklaces, bracelets | 30+ |
| Consumables | Health/mana/stamina potions, food | 20+ |
| Tools | Pickaxes, shovels, fishing rods | 15+ |
| Materials | Ores, gems, crafting components | 25+ |
| Misc | Keys, scrolls, collectibles | 40+ |

### Item Rarity Tiers

| Rarity | Color | Drop Rate |
|--------|-------|-----------|
| Common | White | 50% |
| Uncommon | Green | 25% |
| Rare | Blue | 15% |
| Epic | Purple | 8% |
| Legendary | Orange | 1.5% |
| Mythic | Cyan | 0.5% |

### Creatures (45 species in `creatures.json`)

Each region has its own themed creature set. Creatures have base stats, XP rewards, ability pools, and loot tables. Stats scale with player level via `DIFFICULTY_SCALE_PER_LEVEL` (0.15).

## Rendering & Assets

### Canvas Views

| View | Purpose |
|------|---------|
| `RoomCanvasView` | Draws current room: background, doors, objects, UI overlays |
| `GridMapCanvasView` | Region grid map with visited/undiscovered room visualization |
| `CombatCanvasView` | Combat scene with creature sprite and HP bars |

### Asset Management

- **`GameAssetManager`**: LRU bitmap cache singleton for sprites; auto-resets on version update
- **`SpriteLoader`**: Loads images from assets folders with fallback to `PlaceholderRenderer`
- **`PlaceholderRenderer`**: Procedurally generates colored shapes when sprite assets are unavailable
- **`SoundManager`**: MediaPlayer for background music + SoundPool for SFX; auto-resets on version update

## Setup

### Prerequisites

- Android Studio (Arctic Fox or later)
- JDK 21
- Android SDK 35 (compileSdk) with minSdk 24

### Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.appcompat | 1.7.0 | Android backward compatibility |
| androidx.constraintlayout | 2.2.0 | Flexible UI layouts |
| com.google.android.material | 1.12.0 | Material Design components |
| com.google.code.gson | 2.11.0 | JSON serialization/deserialization |
| com.squareup.okhttp3 | 4.12.0 | HTTP client for cloud API |
| junit | 4.13.2 | Unit testing (test only) |

### Cloud Sync Setup

1. Go to [Google Apps Script](https://script.google.com) and create a new project
2. Paste the contents of `cloud/apps-script/Code.gs`
3. Create 3 Google Drive folders for players, rooms, and notes
4. Update the folder IDs in `Code.gs`:
   ```javascript
   var PLAYER_FOLDER_ID = "your-player-folder-id";
   var ROOM_FOLDER_ID   = "your-room-folder-id";
   var NOTES_FOLDER_ID  = "your-notes-folder-id";
   ```
5. Deploy as Web App (Execute as: Me, Access: Anyone)
6. Copy the Web App URL into `Constants.java`:
   ```java
   public static final String APPS_SCRIPT_URL = "https://script.google.com/macros/s/YOUR_ID/exec";
   ```

### Building

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK (requires keystore.properties)
```

Output: `app/build/outputs/apk/*/jormungandr.apk`

## Releasing Updates

When releasing a new version, **always bump the `versionCode`** so existing installs automatically invalidate stale cached state (world mesh, registries, assets, sounds).

### Steps

1. **Bump `versionCode`** in `app/build.gradle`:
   ```groovy
   defaultConfig {
       versionCode 3          // Increment this
       versionName "1.2"      // Update display version
   }
   ```

2. **Update `GAME_VERSION`** in `Constants.java` to match:
   ```java
   public static final String GAME_VERSION = "1.2";
   ```

3. **Build and distribute** the APK

### What happens on update

`MainActivity.onCreate()` compares `BuildConfig.VERSION_CODE` to a SharedPreferences value. On mismatch:

1. `WorldMesh.reset()` — clears the 80,000-room graph so it rebuilds from seed
2. `GameRepository.reset()` — clears player/room state and shuts down the old sync manager
3. `GameAssetManager.reset()` — evicts all cached sprites
4. `SoundManager.reset()` — releases MediaPlayer and SoundPool resources
5. Stores the new versionCode in SharedPreferences

All singletons are then lazily re-initialized with the latest code on next access.

### When to bump versionCode

- Any change to world generation (WorldMesh, RoomGenerator, Constants affecting layout)
- Changes to item/creature registry data files
- Changes to room generation logic or seed
- Changes to serialization format (Player, Room models)
- Basically: **every release should bump versionCode**

## Apps Script API Reference

All requests are POST with JSON body containing an `action` field.

| Action | Parameters | Response |
|--------|-----------|----------|
| `validateCode` | `{ code }` | `{ success, message }` |
| `getPlayer` | `{ code }` | `{ success, data }` |
| `savePlayer` | `{ code, data }` | `{ success, message }` |
| `getRoom` | `{ roomId }` | `{ success, data }` |
| `saveRoom` | `{ roomId, data }` | `{ success, message }` |
| `getVersion` | `{}` | `{ success, data }` |
| `listRooms` | `{ region }` | `{ success, data }` |
| `getNotes` | `{ roomId }` | `{ success, data }` |
| `saveNote` | `{ roomId, code, note }` | `{ success, message }` |
| `getTrades` | `{ roomId }` | `{ success, data }` |
| `saveTrades` | `{ roomId, data }` | `{ success, message }` |
| `adminResetAllRooms` | `{ code }` | `{ success, message }` |
| `adminResetAllNotes` | `{ code }` | `{ success, message }` |
| `adminResetAllPlayers` | `{ code }` | `{ success, message }` |

## Key Constants

| Constant | Value | Notes |
|----------|-------|-------|
| `WORLD_SEED` | `0x4A6F726D756E4CL` | Deterministic world generation seed |
| `GRID_SIZE` | 100 | 100x100 rooms per region |
| `NUM_REGIONS` | 8 | Themed biome regions |
| `ROOMS_PER_REGION` | 10,000 | Total rooms per region |
| `MAZE_EXTRA_CONNECTION_CHANCE` | 0.08 | 8% chance for extra passages |
| `CREATURE_DEN_CHANCE` | 0.20 | 20% rooms are creature dens |
| `MAX_STACK_SIZE` | 16 | Items per inventory slot |
| `MIN_INVENTORY_SLOTS` | 8 | Starting inventory capacity |
| `MAX_INVENTORY_SLOTS` | 48 | Max inventory slots (Strength-scaled) |
| `STARTING_HP` | 20 | New player hit points |
| `STARTING_MANA` | 10 | New player mana |
| `STARTING_STAMINA` | 10 | New player stamina |
| `STAT_POINTS_PER_LEVEL` | 3 | Points to allocate per level |
| `BASE_XP_TO_LEVEL` | 100 | XP required for level 2 |
| `XP_SCALING_FACTOR` | 1.5 | XP growth multiplier per level |
| `DIFFICULTY_SCALE_PER_LEVEL` | 0.15 | Creature stat scaling per level |
| `STAMINA_COST_MOVE` | 1 | Movement stamina cost |
| `STAMINA_COST_SWING` | 2 | Melee attack stamina cost |
| `STAMINA_REGEN_PER_ROOM` | 1 | Stamina regen per non-waypoint room |

## Codebase Statistics

| Metric | Value |
|--------|-------|
| Java source files | 68 |
| Lines of Java code | ~9,570 |
| Item definitions | 277 |
| Creature definitions | 45 |
| UI Fragments | 12 |
| Model classes | 18 |
| Total game rooms | 80,001 (80,000 + hub) |
