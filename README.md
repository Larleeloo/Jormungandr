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
| `AppsScriptClient` | HTTP client for the Apps Script REST API |
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
  adapter/        RecyclerView adapters (actions, inventory, notes, shop)
  asset/          GameAssetManager (sprites), SoundManager (audio)
  cloud/          AppsScriptClient, CloudSyncManager, AccessCodeValidator
  data/           GameRepository, ItemRegistry, CreatureRegistry, file managers
  engine/         WorldMesh, RoomGenerator, CombatEngine, LootGenerator
  fragment/       RoomFragment, HubFragment, MapFragment, CombatFragment,
                  InventoryFragment, CharacterFragment, AdminFragment, etc.
  model/          Player, Room, RoomObject, Direction, BiomeType, ItemDef, etc.
  util/           Constants, RoomIdHelper, SeededRandom, FormulaHelper
  view/           RoomCanvasView, GridMapCanvasView, CombatCanvasView, etc.

cloud/apps-script/
  Code.gs         Google Apps Script REST API (deploy as Web App)
```

## Regions & Biomes

| # | Biome | Color | Enemy Level Offset |
|---|-------|-------|--------------------|
| 1 | Red Dungeon | Dark Red | +1 |
| 2 | Volcanic Waste | Orange | +2 |
| 3 | Meadow | Gold | +3 |
| 4 | Forest | Green | +4 |
| 5 | Ocean | Blue | +5 |
| 6 | Castle | Pink | +6 |
| 7 | Ice Cave | Light Blue | +7 |
| 8 | Void | Indigo | +8 |

## Gameplay

- **Navigation**: Tap cardinal doors (N/S/E/W) to move between rooms
- **Combat**: Pokemon-style action selection (8-48 actions based on equipped items)
- **Inventory**: Stackable items (max 16 per slot), D&D-style stat investment
- **Waypoints**: Discovering a region's waypoint adds a portal to the hub
- **Portals**: One-way doors to other regions' waypoints
- **Notes**: Leave messages for other players in waypoints, creature dens, and the hub
- **Death**: Respawn at hub with half HP, room state preserved

### Admin Features (JORM-ALPHA-001 to 003)

- **Filled map view**: All 10,000 rooms per region visible (undiscovered rooms dimmed)
- **Room inspector**: Tap any room to see ID, grid position, doors, portals, and contents
- **Travel anywhere**: "Travel Here" button in the inspector for direct teleportation
- **Reset controls**: Admin panel to wipe all rooms, notes, or player saves from cloud

## Setup

### Prerequisites

- Android Studio (Arctic Fox or later)
- JDK 21
- Android SDK 35 (compileSdk) with minSdk 24

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
| `adminResetAllRooms` | `{ code }` | `{ success, message }` |
| `adminResetAllNotes` | `{ code }` | `{ success, message }` |
| `adminResetAllPlayers` | `{ code }` | `{ success, message }` |

## Item Rarity Tiers

| Rarity | Color | Drop Rate |
|--------|-------|-----------|
| Common | White | 50% |
| Uncommon | Green | 25% |
| Rare | Blue | 15% |
| Epic | Purple | 8% |
| Legendary | Orange | 1.5% |
| Mythic | Cyan | 0.5% |

## Key Constants

| Constant | Value | Notes |
|----------|-------|-------|
| `WORLD_SEED` | `0x4A6F726D756E4CL` | Deterministic world generation seed |
| `GRID_SIZE` | 100 | 100x100 rooms per region |
| `NUM_REGIONS` | 8 | Themed biome regions |
| `MAZE_EXTRA_CONNECTION_CHANCE` | 0.08 | 8% chance for extra passages |
| `CREATURE_DEN_CHANCE` | 0.20 | 20% rooms are creature dens |
| `MAX_STACK_SIZE` | 16 | Items per inventory slot |
| `STARTING_HP` | 20 | New player hit points |
| `DIFFICULTY_SCALE_PER_LEVEL` | 0.15 | Creature stat scaling per level |
