# Plan: Restructure World Topology to Trunk-and-Branch (Vine) System

## Overview

Replace the current flat zone-based room topology (10,000 rooms per region across 5 concentric zones) with a tree/vine-like trunk-and-branch system. Each region has a main trunk (~200-300 rooms) with branches growing off it. Cross-region connections can link any two regions spontaneously.

## Current State

- **MeshGenerator**: Generates doors deterministically using zones 1-5, rooms 0-9999
- **Constants**: Defines ZONE_1_ROOMS=500, ZONE_2_ROOMS=1500, etc. totaling 10,000
- **RoomIdHelper**: Maps room numbers to zones, checks waypoints at room 500
- **Room model**: Stores region, zone, doors (Direction → targetRoomId)
- **RoomGenerator**: Creates rooms with loot/creatures based on zone difficulty
- **GameRepository**: Navigation with history stack, bidirectional door linking

## Implementation Steps

### Step 1: Update Constants.java

Replace zone-based constants with trunk-and-branch constants:
- `TRUNK_LENGTH = 250` (main trunk rooms per region)
- `MAX_BRANCH_DEPTH = 40` (max rooms deep a branch can go)
- `BRANCH_CHANCE = 0.35` (probability a trunk room spawns a branch)
- `SUB_BRANCH_CHANCE = 0.15` (probability a branch room spawns a sub-branch)
- `CROSS_REGION_CHANCE = 0.03` (3% chance any forward door connects to another region)
- `ROOMS_PER_REGION = 10000` (keep as upper bound for ID space)
- Remove ZONE_1_ROOMS through ZONE_5_ROOMS constants
- Remove NUM_ZONES
- Add `WAYPOINT_INTERVAL = 50` (waypoint every ~50 trunk rooms)

### Step 2: Introduce New Room ID Scheme in RoomIdHelper

Room IDs need to encode position in the tree:
- **Trunk rooms**: `r{region}_{trunkIndex}` — e.g. `r1_00042` is trunk room 42 of region 1
- **Branch rooms**: `r{region}_{parentTrunkIndex}b{branchId}_{depth}` — e.g. `r1_00042b1_003` is branch 1 off trunk 42, depth 3

Actually, to keep backward compatibility with the existing `r{region}_{number}` format and avoid breaking serialization, we'll use a simpler approach:

- Trunk rooms use IDs 0 to TRUNK_LENGTH-1 (0-249)
- Branch rooms use IDs starting at TRUNK_LENGTH (250+), assigned deterministically
- A room's "depth" (difficulty tier) is computed from its position in the tree, not from its raw ID

Add new helper methods:
- `isTrunkRoom(int roomNumber)` → roomNumber < TRUNK_LENGTH
- `getDifficulty(int region, int roomNumber)` → computed from tree depth (0.0 to 1.0)
- `isWaypoint(int roomNumber)` → trunk rooms at multiples of WAYPOINT_INTERVAL
- Remove `getZone()` method (replace with `getDifficulty()`)

### Step 3: Rewrite MeshGenerator for Trunk-and-Branch Topology

The core change. New `generateDoors()` logic:

**For trunk rooms** (roomNumber < TRUNK_LENGTH):
- BACK door → previous trunk room (roomNumber - 1), or hub if roomNumber == 0
- FORWARD door → next trunk room (roomNumber + 1), unless at end of trunk
- LEFT/RIGHT doors → branch entrances (deterministic based on seed)
  - Use seeded RNG to decide if this trunk room has a branch on LEFT, RIGHT, or both
  - Branch entrance targets use IDs in the 250+ range, computed deterministically

**For branch rooms** (roomNumber >= TRUNK_LENGTH):
- BACK door → parent room (either trunk room or previous branch room)
- FORWARD door → deeper branch room, if depth hasn't exceeded MAX_BRANCH_DEPTH
- LEFT/RIGHT → sub-branches (if sub-branch chance triggers)
- Cross-region door chance on any forward direction: 3% chance to connect to any region 1-8 (not just adjacent)

**Cross-region connections:**
- `getCrossRegionTarget()` updated: instead of only zone 5 and only ±1 region, allow from any branch room, pick any random region 1-8 (excluding current)
- Target room in destination region is a random trunk room

**Deterministic branch ID mapping:**
- Given a trunk room T and direction D, the branch entrance room number = `TRUNK_LENGTH + hash(region, T, D)`
- Given a branch room B, the next deeper room = `TRUNK_LENGTH + hash(region, B, "forward")`
- All hashes mod (ROOMS_PER_REGION - TRUNK_LENGTH) + TRUNK_LENGTH to stay in valid range

### Step 4: Update RoomGenerator for Difficulty-Based Content

Replace zone-based difficulty with tree-depth-based difficulty:
- `getDifficultyTier(int region, int roomNumber)` returns 1-5 based on depth:
  - Trunk rooms near start → tier 1
  - Trunk rooms near end → tier 3
  - Shallow branches → tier 2-3
  - Deep branches → tier 4-5
- Creature level scaling uses difficulty tier instead of zone
- Loot quality uses difficulty tier instead of zone
- Waypoints placed at trunk multiples of WAYPOINT_INTERVAL

### Step 5: Update GameRepository Navigation

- `ensureBidirectionalDoors()` works the same (generate target room, override its BACK door)
- Navigation history stack unchanged
- The BACK door logic for branch rooms needs to correctly point to parent

### Step 6: Update RoomCanvasView Waypoint Proximity

- Replace zone-based proximity with trunk-distance-based proximity
- For trunk rooms: distance = |current - nearest waypoint multiple|
- For branch rooms: distance = depth from trunk + trunk room's waypoint distance

### Step 7: Update HubFragment Region Selection

- No changes needed — hub buttons already navigate to `r{region}_00000` (trunk room 0)
- The 8-button layout remains the same

## Files Modified

| File | Change |
|------|--------|
| `util/Constants.java` | Replace zone constants with trunk/branch constants |
| `util/RoomIdHelper.java` | Replace `getZone()` with `getDifficulty()`, update `isWaypoint()` |
| `engine/MeshGenerator.java` | Complete rewrite of `generateDoors()` for tree topology |
| `engine/RoomGenerator.java` | Use difficulty tier instead of zone for content generation |
| `data/GameRepository.java` | Minor: update any zone references |
| `view/RoomCanvasView.java` | Update waypoint proximity indicator |
| `model/Room.java` | Keep zone field but populate with difficulty tier |

## Key Design Decisions

1. **Keep room ID format**: `r{region}_{number}` stays the same — no serialization changes
2. **Trunk = IDs 0-249, Branches = IDs 250+**: Simple split, deterministic mapping
3. **Difficulty from tree depth, not ID range**: Computed dynamically
4. **All regions cross-connect**: Any forward door has 3% chance to jump to any other region
5. **Waypoints on trunk only**: Every 50 trunk rooms (rooms 0, 50, 100, 150, 200)
