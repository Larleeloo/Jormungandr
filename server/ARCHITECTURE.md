# Jörmungandr Multiplayer Server — Architecture & Setup

## Overview

A WebSocket-based game server that replaces the Google Apps Script + Google Drive
backend with real-time (<25ms) communication. Designed for 25 concurrent players
on a single micro VM instance.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Google Cloud E2-micro                       │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Spring Boot Application (port 8080)          │  │
│  │                                                           │  │
│  │  ┌─────────────┐    ┌──────────────┐   ┌──────────────┐  │  │
│  │  │  WebSocket   │───▶│   Message    │──▶│   Handler    │  │  │
│  │  │   Handler    │    │   Router     │   │   Layer      │  │  │
│  │  └──────┬───────┘    └──────────────┘   │              │  │  │
│  │         │                               │  Player      │  │  │
│  │  ┌──────▼───────┐                       │  Room        │  │  │
│  │  │   Session    │                       │  Note        │  │  │
│  │  │   Registry   │◀─── broadcast ────────│  Trade       │  │  │
│  │  │  (in-memory) │                       │  Proximity   │  │  │
│  │  └──────────────┘                       │  Turn        │  │  │
│  │                                         │  Admin       │  │  │
│  │                                         └──────┬───────┘  │  │
│  │                                                │          │  │
│  │                                         ┌──────▼───────┐  │  │
│  │                                         │   SQLite DB  │  │  │
│  │                                         │  (WAL mode)  │  │  │
│  │                                         │              │  │  │
│  │                                         │  players     │  │  │
│  │                                         │  rooms       │  │  │
│  │                                         │  notes       │  │  │
│  │                                         │  trades      │  │  │
│  │                                         │  actions     │  │  │
│  │                                         └──────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  data/jormungandr.db ← single file, trivial backup             │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component       | Choice                          | Why                                   |
|-----------------|---------------------------------|---------------------------------------|
| Language        | Java 21                         | Matches Android client                |
| Framework       | Spring Boot 3.2                 | WebSocket support, dependency injection, scheduling |
| WebSocket       | Spring WebSocket (JSR 356)      | Native Spring integration             |
| Database        | SQLite 3.45 (WAL mode)          | Zero config, single file, sub-ms reads |
| JSON            | GSON 2.11                       | Same library as Android client        |
| Build           | Gradle                          | Same build system as Android project  |

## Why This Architecture

### vs. Google Apps Script (current)

| Metric             | Apps Script           | WebSocket Server     |
|--------------------|-----------------------|----------------------|
| Latency            | 200-800ms per request | 5-15ms per message   |
| Proximity polling  | 5-10s intervals       | Instant push         |
| Turn state         | 2s polling            | Instant broadcast    |
| Concurrency        | 1 req at a time       | 25 concurrent WS     |
| Persistence        | Google Drive files    | SQLite (local)       |
| Cost               | Free (quota limited)  | ~$7/mo (free tier)   |

### Key Improvements

1. **Real-time push** — Turn state, action logs, and proximity events are
   broadcast to affected players instantly over the persistent WebSocket
   connection. No more polling.

2. **In-memory session tracking** — The server knows every player's current
   room at all times. Proximity checks are O(25) map scans with zero I/O,
   instead of reading every player file from Google Drive.

3. **Turn queue in memory** — Turn state lives in a ConcurrentHashMap.
   Join/leave/advance operations are sub-microsecond. Stale turns are
   auto-advanced by a scheduled task every 5 seconds.

4. **SQLite WAL mode** — Concurrent reads don't block writes. With 25 players,
   write contention is effectively zero. Sub-millisecond queries.

## Directory Structure

```
server/
├── build.gradle                          # Spring Boot + SQLite + GSON
├── settings.gradle
├── ARCHITECTURE.md                       # This file
├── src/main/java/com/larleeloo/jormungandr/server/
│   ├── JormungandrServer.java            # @SpringBootApplication entry point
│   ├── config/
│   │   ├── ServerConstants.java          # Access codes, TTL, timeouts
│   │   └── WebSocketConfig.java          # Registers /game endpoint
│   ├── websocket/
│   │   ├── GameWebSocketHandler.java     # Connection lifecycle
│   │   └── SessionRegistry.java          # Player sessions + room tracking
│   ├── handler/
│   │   ├── MessageRouter.java            # JSON dispatch (switch on type)
│   │   ├── PlayerHandler.java            # validate, get, save
│   │   ├── RoomHandler.java              # get, save, list
│   │   ├── NoteHandler.java              # get, save
│   │   ├── TradeHandler.java             # get, save
│   │   ├── ProximityHandler.java         # nearby players, action logs
│   │   ├── TurnHandler.java              # join, end, leave, state
│   │   └── AdminHandler.java             # reset operations
│   ├── store/
│   │   ├── DatabaseManager.java          # SQLite connection + schema init
│   │   ├── PlayerStore.java              # CRUD for player JSON blobs
│   │   ├── RoomStore.java                # CRUD for room JSON blobs
│   │   ├── NoteStore.java                # CRUD for note arrays
│   │   ├── TradeStore.java               # CRUD for trade arrays
│   │   └── ActionStore.java              # CRUD + TTL pruning for actions
│   └── model/
│       ├── ClientMessage.java            # Incoming message shape
│       └── ServerMessage.java            # Outgoing message shape
└── src/main/resources/
    ├── application.properties            # Port, DB path, logging
    └── schema.sql                        # CREATE TABLE statements
```

## Message Protocol

All communication is JSON over WebSocket. The protocol mirrors the existing
Apps Script API so the Android client requires minimal changes.

### Authentication (must be first message)

```json
→ {"type":"authenticate","code":"JORM-ALPHA-001"}
← {"type":"authenticated","success":true,"message":"Welcome, JORM-ALPHA-001!"}
```

### Request/Response (same as Apps Script)

```json
→ {"type":"getPlayer","code":"JORM-ALPHA-001"}
← {"type":"getPlayer","success":true,"message":"Player loaded.","data":"{...}"}

→ {"type":"saveRoom","roomId":"r3_04250","data":"{...}"}
← {"type":"saveRoom","success":true,"message":"Room saved."}
```

### Server-Pushed Events (new — not in Apps Script)

```json
← {"type":"turnStateChanged","roomId":"r3_04250","data":{...}}
← {"type":"actionBroadcast","roomId":"r3_04250","accessCode":"JORM-ALPHA-005",
   "actionText":"Opened a chest","timestamp":1711234567}
```

### Full Message Type Reference

| Type                | Direction | Description                        |
|---------------------|-----------|------------------------------------|
| `authenticate`      | →         | Login with access code             |
| `authenticated`     | ←         | Auth result                        |
| `validateCode`      | → ←       | Check if code is valid             |
| `getPlayer`         | → ←       | Load player save                   |
| `savePlayer`        | → ←       | Upload player save                 |
| `getRoom`           | → ←       | Load room data                     |
| `saveRoom`          | → ←       | Upload room data                   |
| `listRooms`         | → ←       | List saved rooms in a region       |
| `getNotes`          | → ←       | Load notes for a room              |
| `saveNote`          | → ←       | Add a note to a room               |
| `getTrades`         | → ←       | Load trade listings                |
| `saveTrades`        | → ←       | Upload trade listings              |
| `getNearbyPlayers`  | → ←       | Get co-located players             |
| `recordAction`      | → ←       | Log a co-location action           |
| `getRecentActions`  | → ←       | Get recent action logs             |
| `cleanupActions`    | → ←       | Prune expired actions              |
| `joinTurnQueue`     | → ←       | Enter turn queue                   |
| `endTurn`           | → ←       | End current turn                   |
| `leaveTurnQueue`    | → ←       | Leave turn queue                   |
| `getTurnState`      | → ←       | Get current turn state             |
| `getVersion`        | → ←       | Get server version                 |
| `turnStateChanged`  | ←         | **Push**: turn queue updated       |
| `actionBroadcast`   | ←         | **Push**: action in your room      |
| `adminResetAll*`    | → ←       | Admin: wipe data tables            |

## Google Cloud E2 Setup Guide

### 1. Create the VM

```bash
gcloud compute instances create jormungandr-server \
    --zone=us-central1-a \
    --machine-type=e2-micro \
    --image-family=debian-12 \
    --image-project=debian-cloud \
    --boot-disk-size=10GB \
    --tags=game-server
```

### 2. Open the firewall

```bash
gcloud compute firewall-rules create allow-jormungandr \
    --allow=tcp:8080 \
    --target-tags=game-server \
    --description="Jormungandr WebSocket server"
```

### 3. Install Java 21

```bash
sudo apt update && sudo apt install -y openjdk-21-jre-headless
```

### 4. Build the server

From the repository root:

```bash
cd server
./gradlew bootJar
```

This produces `server/build/libs/jormungandr-server.jar`.

### 5. Deploy

```bash
# Copy the jar to the VM
gcloud compute scp server/build/libs/jormungandr-server.jar \
    jormungandr-server:~/jormungandr-server.jar

# SSH in and run
gcloud compute ssh jormungandr-server
mkdir -p data
java -jar jormungandr-server.jar
```

### 6. Run as a systemd service (production)

Create `/etc/systemd/system/jormungandr.service`:

```ini
[Unit]
Description=Jormungandr Game Server
After=network.target

[Service]
User=jormungandr
WorkingDirectory=/home/jormungandr
ExecStart=/usr/bin/java -jar jormungandr-server.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo useradd -m -s /bin/bash jormungandr
sudo cp jormungandr-server.jar /home/jormungandr/
sudo mkdir -p /home/jormungandr/data
sudo chown -R jormungandr:jormungandr /home/jormungandr
sudo systemctl enable jormungandr
sudo systemctl start jormungandr
```

### 7. Backup

The entire game state is a single SQLite file:

```bash
# One-liner backup
cp data/jormungandr.db data/jormungandr-$(date +%Y%m%d).db
```

## Android Client Migration

When ready to switch the Android client from Apps Script to WebSocket:

### What changes in the client

1. **Replace `AppsScriptClient`** with a new `WebSocketClient` that:
   - Opens a persistent WebSocket to `ws://<server-ip>:8080/game`
   - Sends JSON messages with `type` instead of `action`
   - Handles server-pushed events (`turnStateChanged`, `actionBroadcast`)

2. **Remove polling** from `ProximityManager` and `TurnManager`:
   - Turn state updates arrive via push
   - Action logs arrive via push
   - Proximity still needs periodic `getNearbyPlayers` calls (server computes
     from in-memory map, so these are <1ms)

3. **Update `Constants.java`**:
   - Replace `APPS_SCRIPT_URL` with `WEBSOCKET_URL = "ws://<ip>:8080/game"`
   - Polling intervals can be reduced or removed for pushed events

4. **Add reconnection logic**:
   - On WebSocket disconnect, reconnect with exponential backoff
   - Re-authenticate on reconnect
   - Re-fetch current room/turn state after reconnect

### What stays the same

- All model classes (Player, Room, ItemDef, etc.) — unchanged
- JSON serialization format — unchanged (GSON on both sides)
- Game logic (combat, world gen, loot) — unchanged
- All response payloads — same `{success, message, data}` shape

## Estimated Latency Budget

```
Client → Server:  2-8ms  (TCP within same continent)
Server processing: <1ms  (in-memory lookup or SQLite query)
Server → Client:  2-8ms  (TCP return)
                  ─────
Total:            5-17ms  (well under 25ms target)
```

## Cost

| Provider     | Instance   | Monthly Cost | Notes                      |
|--------------|------------|-------------|----------------------------|
| Google Cloud | e2-micro   | $0 (free tier) | 1 always-free instance  |
| AWS          | t4g.micro  | ~$6         | If GCP free tier expires   |

The free tier e2-micro (0.25 vCPU, 1GB RAM) is more than sufficient for
25 WebSocket connections with in-memory state and SQLite persistence.
