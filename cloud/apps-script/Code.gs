/**
 * Jörmungandr - Google Apps Script Cloud Sync Web App
 *
 * This script acts as a REST API middleware between the Android game client
 * and Google Drive, enabling cloud save/load functionality.
 *
 * ========== DEPLOYMENT INSTRUCTIONS ==========
 *
 * 1. Go to https://script.google.com and create a new project.
 *    Name it "Jormungandr Cloud Sync".
 *
 * 2. Paste this entire file into Code.gs (replacing default content).
 *
 * 3. Update the CONFIGURATION section below:
 *    - PLAYER_FOLDER_ID: Google Drive folder ID for player save files.
 *    - ROOM_FOLDER_ID:   Google Drive folder ID for room data files.
 *    - NOTES_FOLDER_ID:  Google Drive folder ID for player notes.
 *    Create these 3 folders in your Google Drive and copy their IDs from the URL.
 *    (The folder ID is the long string after /folders/ in the Drive URL.)
 *
 * 4. Deploy as Web App:
 *    - Click "Deploy" → "New deployment"
 *    - Select type: "Web app"
 *    - Description: "Jormungandr Cloud Sync v1.0"
 *    - Execute as: "Me" (your Google account)
 *    - Who has access: "Anyone"
 *    - Click "Deploy"
 *
 * 5. Copy the Web App URL and paste it into the Android app's
 *    Constants.java file as APPS_SCRIPT_URL.
 *
 * 6. Grant permissions when prompted (the script needs Drive access).
 *
 * ========== API REFERENCE ==========
 *
 * All requests are POST with JSON body containing an "action" field.
 *
 * Actions:
 *   validateCode  { code }                    → { success, message }
 *   getPlayer     { code }                    → { success, data }
 *   savePlayer    { code, data }              → { success, message }
 *   getRoom       { roomId }                  → { success, data }
 *   saveRoom      { roomId, data }            → { success, message }
 *   getVersion    {}                           → { success, data }
 *   listRooms     { region }                  → { success, data }
 *   getNotes      { roomId }                  → { success, data }
 *   saveNote      { roomId, code, note }      → { success, message }
 *
 * =============================================
 */

// ========== CONFIGURATION ==========
var PLAYER_FOLDER_ID = "12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv";
var ROOM_FOLDER_ID   = "1lVx_0npSW4JVaiSr98VQyOpRjxMZ-pfg";
var NOTES_FOLDER_ID  = "16iJKA0ch5gUDL6yJ_zVa0c4xMBPrrmLG";
var TRADES_FOLDER_ID = ""; // Create a Drive folder for trade listings and set ID here
var ACTIONS_FOLDER_ID = ""; // Create a Drive folder for timestamped co-location actions
var GAME_VERSION     = "1.0";

// ---- Action data retention ----
// Actions are ephemeral co-location logs. Unlike notes or trades, they have no
// long-term value — they exist only so nearby players can see what happened in
// a room *right now*. Without a TTL, every room that ever had two players near
// each other would keep an actions file forever, and those files would never be
// cleaned up because no game event triggers their deletion. With 80,000+
// possible rooms and 25 players, the ACTIONS_FOLDER would grow unbounded.
//
// ACTION_TTL_SECONDS controls how long an individual action entry survives.
// Any action older than this is pruned on the next read or write to that room's
// file. If all entries in a file are expired, the file itself is deleted to
// reclaim the Drive storage slot entirely.
var ACTION_TTL_SECONDS = 3600; // 1 hour — actions older than this are pruned

// Turn-based co-location — when 2+ players occupy the same room, they take
// turns interacting with objects. If a player holds a turn without acting for
// this many seconds, the server auto-advances to the next player.
var TURN_TIMEOUT_SECONDS = 30;

// Maximum number of action files that can exist across all rooms. This is a
// safety net: even if many rooms accumulate action files simultaneously, the
// scheduled cleanup will trash the oldest files once this cap is exceeded.
// With 25 alpha testers spread across 80,000 rooms, hitting this limit would
// indicate a bug or abuse rather than normal gameplay.
var MAX_ACTION_FILES = 200;

// Valid access codes (JORM-ALPHA-001 through JORM-ALPHA-025)
var VALID_CODES = [];
for (var i = 1; i <= 25; i++) {
  VALID_CODES.push("JORM-ALPHA-" + ("00" + i).slice(-3));
}

// Admin access codes with reset privileges
var ADMIN_CODES = ["JORM-ALPHA-001", "JORM-ALPHA-002", "JORM-ALPHA-003"];

// ========== ENTRY POINT ==========

function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents);
    var action = body.action;

    switch (action) {
      case "validateCode":
        return jsonResponse(handleValidateCode(body));
      case "getPlayer":
        return jsonResponse(handleGetPlayer(body));
      case "savePlayer":
        return jsonResponse(handleSavePlayer(body));
      case "getRoom":
        return jsonResponse(handleGetRoom(body));
      case "saveRoom":
        return jsonResponse(handleSaveRoom(body));
      case "getVersion":
        return jsonResponse(handleGetVersion());
      case "listRooms":
        return jsonResponse(handleListRooms(body));
      case "getNotes":
        return jsonResponse(handleGetNotes(body));
      case "saveNote":
        return jsonResponse(handleSaveNote(body));
      case "getNearbyPlayers":
        return jsonResponse(handleGetNearbyPlayers(body));
      case "recordAction":
        return jsonResponse(handleRecordAction(body));
      case "getRecentActions":
        return jsonResponse(handleGetRecentActions(body));
      case "getTrades":
        return jsonResponse(handleGetTrades(body));
      case "saveTrades":
        return jsonResponse(handleSaveTrades(body));
      case "adminResetAllRooms":
        return jsonResponse(handleAdminResetAllRooms(body));
      case "adminResetAllNotes":
        return jsonResponse(handleAdminResetAllNotes(body));
      case "adminResetAllPlayers":
        return jsonResponse(handleAdminResetAllPlayers(body));
      case "adminResetAllTrades":
        return jsonResponse(handleAdminResetAllTrades(body));
      case "adminResetAllActions":
        return jsonResponse(handleAdminResetAllActions(body));
      case "cleanupActions":
        return jsonResponse(handleCleanupActions(body));
      case "joinTurnQueue":
        return jsonResponse(handleJoinTurnQueue(body));
      case "endTurn":
        return jsonResponse(handleEndTurn(body));
      case "leaveTurnQueue":
        return jsonResponse(handleLeaveTurnQueue(body));
      case "getTurnState":
        return jsonResponse(handleGetTurnState(body));
      default:
        return jsonResponse({ success: false, message: "Unknown action: " + action });
    }
  } catch (err) {
    return jsonResponse({ success: false, message: "Server error: " + err.toString() });
  }
}

function doGet(e) {
  return jsonResponse({ success: true, message: "Jormungandr Cloud Sync API v" + GAME_VERSION });
}

// ========== ACTION HANDLERS ==========

function handleValidateCode(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!code) {
    return { success: false, message: "No access code provided." };
  }
  var valid = VALID_CODES.indexOf(code) !== -1;
  return { success: valid, message: valid ? "Access code valid." : "Invalid access code." };
}

function handleGetPlayer(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!code) {
    return { success: false, message: "No access code provided." };
  }
  var fileName = "player_" + code + ".json";
  var file = findFileInFolder(PLAYER_FOLDER_ID, fileName);
  if (!file) {
    return { success: false, message: "No save found for this code." };
  }
  var data = file.getBlob().getDataAsString();
  return { success: true, message: "Player loaded.", data: data };
}

function handleSavePlayer(body) {
  var code = (body.code || "").trim().toUpperCase();
  var data = body.data;
  if (!code || !data) {
    return { success: false, message: "Missing code or data." };
  }

  // Validate JSON
  try {
    JSON.parse(data);
  } catch (err) {
    return { success: false, message: "Invalid JSON data." };
  }

  var fileName = "player_" + code + ".json";
  var folder = DriveApp.getFolderById(PLAYER_FOLDER_ID);
  var existing = findFileInFolder(PLAYER_FOLDER_ID, fileName);

  if (existing) {
    existing.setContent(data);
  } else {
    folder.createFile(fileName, data, MimeType.PLAIN_TEXT);
  }

  return { success: true, message: "Player saved." };
}

function handleGetRoom(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }
  var fileName = roomId + ".json";
  var file = findFileInFolder(ROOM_FOLDER_ID, fileName);
  if (!file) {
    return { success: false, message: "Room not found." };
  }
  var data = file.getBlob().getDataAsString();
  return { success: true, message: "Room loaded.", data: data };
}

function handleSaveRoom(body) {
  var roomId = (body.roomId || "").trim();
  var data = body.data;
  if (!roomId || !data) {
    return { success: false, message: "Missing roomId or data." };
  }

  try {
    JSON.parse(data);
  } catch (err) {
    return { success: false, message: "Invalid JSON data." };
  }

  var fileName = roomId + ".json";
  var folder = DriveApp.getFolderById(ROOM_FOLDER_ID);
  var existing = findFileInFolder(ROOM_FOLDER_ID, fileName);

  if (existing) {
    existing.setContent(data);
  } else {
    folder.createFile(fileName, data, MimeType.PLAIN_TEXT);
  }

  return { success: true, message: "Room saved." };
}

function handleGetVersion() {
  return { success: true, message: "Version retrieved.", data: GAME_VERSION };
}

function handleListRooms(body) {
  var region = body.region;
  if (region === undefined || region === null) {
    return { success: false, message: "No region provided." };
  }
  var prefix = "r" + region + "_";
  var folder = DriveApp.getFolderById(ROOM_FOLDER_ID);
  var files = folder.getFiles();
  var roomIds = [];

  while (files.hasNext()) {
    var file = files.next();
    var name = file.getName();
    if (name.indexOf(prefix) === 0 && name.indexOf(".json") !== -1) {
      roomIds.push(name.replace(".json", ""));
    }
  }

  return { success: true, message: "Found " + roomIds.length + " rooms.", data: JSON.stringify(roomIds) };
}

function handleGetNotes(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }
  var fileName = "notes_" + roomId + ".json";
  var file = findFileInFolder(NOTES_FOLDER_ID, fileName);
  if (!file) {
    return { success: true, message: "No notes found.", data: "[]" };
  }
  var data = file.getBlob().getDataAsString();
  return { success: true, message: "Notes loaded.", data: data };
}

function handleSaveNote(body) {
  var roomId = (body.roomId || "").trim();
  var code = (body.code || "").trim().toUpperCase();
  var note = body.note;

  if (!roomId || !code || !note) {
    return { success: false, message: "Missing roomId, code, or note." };
  }

  var fileName = "notes_" + roomId + ".json";
  var folder = DriveApp.getFolderById(NOTES_FOLDER_ID);
  var existing = findFileInFolder(NOTES_FOLDER_ID, fileName);

  var notes = [];
  if (existing) {
    try {
      notes = JSON.parse(existing.getBlob().getDataAsString());
    } catch (err) {
      notes = [];
    }
  }

  // Load player name from player file
  var playerName = code;
  var playerFile = findFileInFolder(PLAYER_FOLDER_ID, "player_" + code + ".json");
  if (playerFile) {
    try {
      var playerData = JSON.parse(playerFile.getBlob().getDataAsString());
      playerName = playerData.name || code;
    } catch (err) {}
  }

  notes.push({
    playerName: playerName,
    text: note,
    timestamp: Math.floor(Date.now() / 1000),
    roomId: roomId
  });

  // Keep only latest 50 notes per room
  if (notes.length > 50) {
    notes = notes.slice(notes.length - 50);
  }

  var jsonData = JSON.stringify(notes);
  if (existing) {
    existing.setContent(jsonData);
  } else {
    folder.createFile(fileName, jsonData, MimeType.PLAIN_TEXT);
  }

  return { success: true, message: "Note saved." };
}

// ========== PROXIMITY / CO-LOCATION HANDLERS ==========

/**
 * Scan all player save files to find players near a given room.
 * Request: { code, roomId, range }
 * Returns an array of { accessCode, name, roomId, level, distance }.
 */
function handleGetNearbyPlayers(body) {
  var callerCode = (body.code || "").trim().toUpperCase();
  var callerRoomId = (body.roomId || "").trim();
  var range = body.range || 3;

  if (!callerCode || !callerRoomId) {
    return { success: false, message: "Missing code or roomId." };
  }

  var callerRegion = parseRegion(callerRoomId);
  var callerNumber = parseRoomNumber(callerRoomId);
  var callerRow = Math.floor(callerNumber / 100);
  var callerCol = callerNumber % 100;

  var folder = DriveApp.getFolderById(PLAYER_FOLDER_ID);
  var files = folder.getFiles();
  var nearby = [];

  while (files.hasNext()) {
    var file = files.next();
    try {
      var playerData = JSON.parse(file.getBlob().getDataAsString());
      var pCode = (playerData.accessCode || "").toUpperCase();
      // Skip self
      if (pCode === callerCode) continue;

      var pRoomId = playerData.currentRoomId || "";
      var pRegion = parseRegion(pRoomId);
      // Must be in the same region
      if (pRegion !== callerRegion) continue;

      var pNumber = parseRoomNumber(pRoomId);
      var pRow = Math.floor(pNumber / 100);
      var pCol = pNumber % 100;
      var dist = Math.abs(callerRow - pRow) + Math.abs(callerCol - pCol);

      if (dist <= range) {
        nearby.push({
          accessCode: pCode,
          name: playerData.name || pCode,
          roomId: pRoomId,
          level: playerData.level || 1,
          distance: dist
        });
      }
    } catch (err) {
      // Skip unparseable player files
    }
  }

  return { success: true, message: "Found " + nearby.length + " nearby.", data: JSON.stringify(nearby) };
}

/**
 * Record a timestamped action for a room (used during co-location).
 * Request: { roomId, code, actionText }
 * Stored in ACTIONS_FOLDER as actions_{roomId}.json — an array of recent entries.
 *
 * STORAGE CLEANUP (why this matters):
 * Every recordAction call creates or appends to a per-room JSON file on Drive.
 * Without cleanup, these files accumulate indefinitely because:
 *   - Players leave rooms without any "disconnect" event
 *   - The game has 80,000+ rooms, each of which could generate a file
 *   - Proximity polling fires every 5-10 seconds, so action writes are frequent
 *   - Google Drive has storage quotas that would eventually be exhausted
 *
 * To prevent unbounded growth, we apply two cleanup strategies on every write:
 *   1. TTL pruning: discard entries older than ACTION_TTL_SECONDS
 *   2. Count cap: keep at most 30 entries per room (belt-and-suspenders)
 * If after pruning no entries remain, the file is deleted entirely rather than
 * left as an empty JSON array, so Drive doesn't accumulate thousands of
 * near-empty files across rooms that no one is visiting anymore.
 */
function handleRecordAction(body) {
  var roomId = (body.roomId || "").trim();
  var code = (body.code || "").trim().toUpperCase();
  var actionText = body.actionText || "";

  if (!roomId || !code || !actionText) {
    return { success: false, message: "Missing roomId, code, or actionText." };
  }

  // Resolve player name
  var playerName = code;
  var playerFile = findFileInFolder(PLAYER_FOLDER_ID, "player_" + code + ".json");
  if (playerFile) {
    try {
      var pd = JSON.parse(playerFile.getBlob().getDataAsString());
      playerName = pd.name || code;
    } catch (err) {}
  }

  var fileName = "actions_" + roomId + ".json";
  var folder = DriveApp.getFolderById(ACTIONS_FOLDER_ID);
  var existing = findFileInFolder(ACTIONS_FOLDER_ID, fileName);

  var actions = [];
  if (existing) {
    try { actions = JSON.parse(existing.getBlob().getDataAsString()); } catch (err) { actions = []; }
  }

  // Add the new action with a server-side timestamp so all entries use a
  // consistent clock (clients may have skewed system time).
  var now = Math.floor(Date.now() / 1000);
  actions.push({
    playerName: playerName,
    accessCode: code,
    actionText: actionText,
    timestamp: now,
    roomId: roomId
  });

  // --- TTL pruning ---
  // Remove entries older than ACTION_TTL_SECONDS. Actions are ephemeral
  // co-location data; once a player has left the area, their hour-old
  // "Opened a chest" log is no longer useful to anyone. Pruning on every
  // write keeps each file small and prevents stale data from accumulating
  // between admin resets.
  var cutoff = now - ACTION_TTL_SECONDS;
  actions = actions.filter(function(a) { return a.timestamp >= cutoff; });

  // --- Count cap ---
  // Even within the TTL window, cap at 30 entries per room to bound file
  // size. In a high-activity room with many co-located players, this
  // prevents a single file from growing too large for a single request.
  if (actions.length > 30) {
    actions = actions.slice(actions.length - 30);
  }

  // --- Empty file cleanup ---
  // If all entries were expired (shouldn't happen since we just added one,
  // but defensive), delete the file to avoid leaving empty stubs on Drive.
  if (actions.length === 0) {
    if (existing) {
      existing.setTrashed(true);
    }
    return { success: true, message: "Action recorded (file pruned)." };
  }

  var jsonData = JSON.stringify(actions);
  if (existing) {
    existing.setContent(jsonData);
  } else {
    folder.createFile(fileName, jsonData, MimeType.PLAIN_TEXT);
  }

  return { success: true, message: "Action recorded." };
}

/**
 * Get recent timestamped actions for a room.
 * Request: { roomId, since } — since is an epoch-second cutoff (optional).
 *
 * STORAGE CLEANUP (read-path pruning):
 * Reads are a natural opportunity to prune stale data. If a client fetches
 * actions and everything in the file is expired, we delete the file on the
 * spot rather than returning an empty array and leaving dead data on Drive.
 * This "lazy cleanup" approach catches files that stopped receiving writes
 * (because all players left) but were never explicitly cleaned up.
 */
function handleGetRecentActions(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }

  var fileName = "actions_" + roomId + ".json";
  var file = findFileInFolder(ACTIONS_FOLDER_ID, fileName);
  if (!file) {
    return { success: true, message: "No actions.", data: "[]" };
  }

  var actions = [];
  try { actions = JSON.parse(file.getBlob().getDataAsString()); } catch (err) {}

  // --- TTL pruning on read ---
  // Always filter out expired entries, even if the caller didn't pass a
  // "since" parameter. This ensures clients never see stale ghost actions
  // from players who left hours ago, and keeps the stored file trim.
  var now = Math.floor(Date.now() / 1000);
  var ttlCutoff = now - ACTION_TTL_SECONDS;
  actions = actions.filter(function(a) { return a.timestamp >= ttlCutoff; });

  // Apply the caller's own "since" filter on top of the TTL filter.
  // The caller may want an even tighter window (e.g., "last 30 seconds").
  var since = body.since || 0;
  if (since > 0) {
    actions = actions.filter(function(a) { return a.timestamp >= since; });
  }

  // --- Lazy file deletion ---
  // If every entry in the file has expired, trash the file so it doesn't
  // sit on Drive as dead weight. This handles the common case where two
  // players were co-located, both left, and no further writes occurred to
  // trigger the write-path pruning in handleRecordAction.
  if (actions.length === 0) {
    file.setTrashed(true);
    return { success: true, message: "No actions.", data: "[]" };
  }

  // Write the pruned list back to Drive so subsequent reads (and the
  // scheduled sweep) see a smaller file. This is safe because we only
  // removed entries that were already past their TTL.
  file.setContent(JSON.stringify(actions));

  return { success: true, message: "Actions loaded.", data: JSON.stringify(actions) };
}

// Room-ID parsing helpers (server-side)
function parseRegion(roomId) {
  var idx = roomId.indexOf("_");
  if (idx < 0) return 0;
  return parseInt(roomId.substring(1, idx), 10) || 0;
}

function parseRoomNumber(roomId) {
  var idx = roomId.indexOf("_");
  if (idx < 0) return 0;
  return parseInt(roomId.substring(idx + 1), 10) || 0;
}

// ========== TURN-BASED CO-LOCATION HANDLERS ==========

/**
 * Add a player to the turn queue for a room.
 * If the queue doesn't exist, create it with this player first in line.
 * If the player is already in the queue, just return current state.
 * Request: { roomId, code }
 */
function handleJoinTurnQueue(body) {
  var roomId = (body.roomId || "").trim();
  var code = (body.code || "").trim().toUpperCase();
  if (!roomId || !code) {
    return { success: false, message: "Missing roomId or code." };
  }

  var state = loadTurnState(roomId);
  var now = Math.floor(Date.now() / 1000);

  // Auto-advance if current turn has timed out
  state = applyTurnTimeout(state, now);

  // Add to queue if not already present
  if (state.queue.indexOf(code) === -1) {
    state.queue.push(code);
    // If this is the first player, they get the turn
    if (state.queue.length === 1) {
      state.currentIndex = 0;
      state.turnStartedAt = now;
    }
  }

  saveTurnState(roomId, state);
  return { success: true, message: "Joined queue.", data: JSON.stringify(state) };
}

/**
 * End the current player's turn and advance to the next player.
 * Only the current turn holder can end their turn.
 * Request: { roomId, code }
 */
function handleEndTurn(body) {
  var roomId = (body.roomId || "").trim();
  var code = (body.code || "").trim().toUpperCase();
  if (!roomId || !code) {
    return { success: false, message: "Missing roomId or code." };
  }

  var state = loadTurnState(roomId);
  var now = Math.floor(Date.now() / 1000);

  // Auto-advance if timed out (someone else may have timed out)
  state = applyTurnTimeout(state, now);

  if (state.queue.length === 0) {
    return { success: true, message: "Queue empty.", data: JSON.stringify(state) };
  }

  var currentHolder = state.queue[state.currentIndex % state.queue.length];
  if (currentHolder !== code) {
    return { success: false, message: "Not your turn.", data: JSON.stringify(state) };
  }

  // Advance to next player
  state.currentIndex = (state.currentIndex + 1) % state.queue.length;
  state.turnStartedAt = now;

  saveTurnState(roomId, state);
  return { success: true, message: "Turn ended.", data: JSON.stringify(state) };
}

/**
 * Remove a player from the turn queue (they left the room or disconnected).
 * If the departing player held the current turn, advance to the next player.
 * If the queue becomes empty, delete the turn file.
 * Request: { roomId, code }
 */
function handleLeaveTurnQueue(body) {
  var roomId = (body.roomId || "").trim();
  var code = (body.code || "").trim().toUpperCase();
  if (!roomId || !code) {
    return { success: false, message: "Missing roomId or code." };
  }

  var state = loadTurnState(roomId);
  var now = Math.floor(Date.now() / 1000);

  var idx = state.queue.indexOf(code);
  if (idx === -1) {
    // Not in queue — nothing to do
    return { success: true, message: "Not in queue.", data: JSON.stringify(state) };
  }

  var wasCurrentTurn = (idx === state.currentIndex % state.queue.length);

  // Remove from queue
  state.queue.splice(idx, 1);

  if (state.queue.length === 0) {
    // Queue empty — delete the file
    deleteTurnState(roomId);
    state.currentIndex = 0;
    state.turnStartedAt = 0;
    return { success: true, message: "Left queue (now empty).", data: JSON.stringify(state) };
  }

  // Adjust currentIndex if needed
  if (idx < state.currentIndex) {
    state.currentIndex--;
  }
  state.currentIndex = state.currentIndex % state.queue.length;

  // If the leaving player held the turn, reset the turn timer
  if (wasCurrentTurn) {
    state.turnStartedAt = now;
  }

  saveTurnState(roomId, state);
  return { success: true, message: "Left queue.", data: JSON.stringify(state) };
}

/**
 * Get the current turn state for a room, auto-advancing any timed-out turns.
 * Request: { roomId }
 */
function handleGetTurnState(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }

  var state = loadTurnState(roomId);
  var now = Math.floor(Date.now() / 1000);

  var before = state.currentIndex;
  state = applyTurnTimeout(state, now);

  // Save if the timeout changed state
  if (state.currentIndex !== before && state.queue.length > 0) {
    saveTurnState(roomId, state);
  }

  return { success: true, message: "Turn state.", data: JSON.stringify(state) };
}

/**
 * Auto-advance the turn if the current holder has timed out.
 */
function applyTurnTimeout(state, now) {
  if (state.queue.length < 2) return state;
  if (state.turnStartedAt > 0 && (now - state.turnStartedAt) > TURN_TIMEOUT_SECONDS) {
    state.currentIndex = (state.currentIndex + 1) % state.queue.length;
    state.turnStartedAt = now;
  }
  return state;
}

function loadTurnState(roomId) {
  var fileName = "turns_" + roomId + ".json";
  var file = findFileInFolder(ACTIONS_FOLDER_ID, fileName);
  if (!file) {
    return { roomId: roomId, queue: [], currentIndex: 0, turnStartedAt: 0 };
  }
  try {
    var state = JSON.parse(file.getBlob().getDataAsString());
    state.roomId = roomId;
    if (!state.queue) state.queue = [];
    return state;
  } catch (err) {
    return { roomId: roomId, queue: [], currentIndex: 0, turnStartedAt: 0 };
  }
}

function saveTurnState(roomId, state) {
  var fileName = "turns_" + roomId + ".json";
  var folder = DriveApp.getFolderById(ACTIONS_FOLDER_ID);
  var existing = findFileInFolder(ACTIONS_FOLDER_ID, fileName);
  var jsonData = JSON.stringify(state);
  if (existing) {
    existing.setContent(jsonData);
  } else {
    folder.createFile(fileName, jsonData, MimeType.PLAIN_TEXT);
  }
}

function deleteTurnState(roomId) {
  var fileName = "turns_" + roomId + ".json";
  var file = findFileInFolder(ACTIONS_FOLDER_ID, fileName);
  if (file) {
    file.setTrashed(true);
  }
}

// ========== TRADE LISTING HANDLERS ==========

function handleGetTrades(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }
  var fileName = "trades_" + roomId + ".json";
  var file = findFileInFolder(TRADES_FOLDER_ID, fileName);
  if (!file) {
    return { success: true, message: "No trades found.", data: "[]" };
  }
  var data = file.getBlob().getDataAsString();
  return { success: true, message: "Trades loaded.", data: data };
}

function handleSaveTrades(body) {
  var roomId = (body.roomId || "").trim();
  var data = body.data;
  if (!roomId || !data) {
    return { success: false, message: "Missing roomId or data." };
  }

  try {
    JSON.parse(data);
  } catch (err) {
    return { success: false, message: "Invalid JSON data." };
  }

  var fileName = "trades_" + roomId + ".json";
  var folder = DriveApp.getFolderById(TRADES_FOLDER_ID);
  var existing = findFileInFolder(TRADES_FOLDER_ID, fileName);

  if (existing) {
    existing.setContent(data);
  } else {
    folder.createFile(fileName, data, MimeType.PLAIN_TEXT);
  }

  return { success: true, message: "Trades saved." };
}

// ========== ADMIN HANDLERS ==========

function validateAdminCode(code) {
  return ADMIN_CODES.indexOf((code || "").trim().toUpperCase()) !== -1;
}

function deleteAllFilesInFolder(folderId) {
  var folder = DriveApp.getFolderById(folderId);
  var files = folder.getFiles();
  var count = 0;
  while (files.hasNext()) {
    var file = files.next();
    file.setTrashed(true);
    count++;
  }
  return count;
}

function handleAdminResetAllRooms(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!validateAdminCode(code)) {
    return { success: false, message: "Unauthorized. Admin access required." };
  }
  var count = deleteAllFilesInFolder(ROOM_FOLDER_ID);
  return { success: true, message: "Reset complete. " + count + " room files deleted." };
}

function handleAdminResetAllNotes(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!validateAdminCode(code)) {
    return { success: false, message: "Unauthorized. Admin access required." };
  }
  var count = deleteAllFilesInFolder(NOTES_FOLDER_ID);
  return { success: true, message: "Reset complete. " + count + " note files deleted." };
}

function handleAdminResetAllPlayers(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!validateAdminCode(code)) {
    return { success: false, message: "Unauthorized. Admin access required." };
  }
  var count = deleteAllFilesInFolder(PLAYER_FOLDER_ID);
  return { success: true, message: "Reset complete. " + count + " player files deleted." };
}

/**
 * Client-triggered cleanup for a single room's action file.
 * Request: { roomId }
 *
 * WHY THIS EXISTS:
 * The client calls this when a player's ProximityManager stops polling — i.e.,
 * when the player pauses the app, navigates away, or loses connectivity.
 * At that point, the player is no longer co-located with anyone from their
 * perspective, so their room's action log can be pruned.
 *
 * This is a lightweight alternative to a full folder sweep: instead of
 * iterating every file in ACTIONS_FOLDER (which is expensive on Drive and
 * could hit Apps Script execution time limits), the client tells us exactly
 * which room to clean up. If the file is entirely expired, it gets deleted.
 * If it still has live entries, we just prune the stale ones and shrink it.
 *
 * This complements the two other cleanup paths:
 *   1. Write-path pruning in handleRecordAction (cleans on every write)
 *   2. Read-path pruning in handleGetRecentActions (cleans on every read)
 * Together, these three paths ensure action files don't survive long after
 * the co-location session that created them has ended.
 */
function handleCleanupActions(body) {
  var roomId = (body.roomId || "").trim();
  if (!roomId) {
    return { success: false, message: "No roomId provided." };
  }

  var fileName = "actions_" + roomId + ".json";
  var file = findFileInFolder(ACTIONS_FOLDER_ID, fileName);
  if (!file) {
    // Nothing to clean — file was already removed or never existed.
    return { success: true, message: "No action file to clean." };
  }

  var actions = [];
  try { actions = JSON.parse(file.getBlob().getDataAsString()); } catch (err) { actions = []; }

  // Prune entries older than the TTL
  var now = Math.floor(Date.now() / 1000);
  var cutoff = now - ACTION_TTL_SECONDS;
  actions = actions.filter(function(a) { return a.timestamp >= cutoff; });

  if (actions.length === 0) {
    // All entries expired — delete the file entirely to free the Drive slot.
    // This is the most common outcome: the player left, time passed, and
    // now there's nothing worth keeping.
    file.setTrashed(true);
    return { success: true, message: "Action file deleted (all entries expired)." };
  }

  // Some entries are still live (other players may still be nearby).
  // Write the pruned list back so the file is smaller for the next reader.
  file.setContent(JSON.stringify(actions));
  return { success: true, message: "Pruned to " + actions.length + " live entries." };
}

/**
 * Scheduled sweep of the entire ACTIONS_FOLDER.
 *
 * WHY THIS EXISTS:
 * The three request-driven cleanup paths (write, read, client disconnect)
 * only fire when a client makes a request for a specific room. If all
 * players go offline simultaneously (e.g., server maintenance, everyone
 * sleeps), no requests arrive and stale files sit on Drive indefinitely.
 *
 * This function is designed to be called by an Apps Script time-driven
 * trigger (e.g., every hour) to sweep the entire folder. It:
 *   1. Deletes files where ALL entries are expired (past ACTION_TTL_SECONDS)
 *   2. Prunes expired entries from files that still have live data
 *   3. Enforces MAX_ACTION_FILES as a hard cap — if somehow more files
 *      exist than expected, the oldest are trashed first
 *
 * TO SET UP THE TRIGGER:
 *   1. In the Apps Script editor, go to Triggers (clock icon)
 *   2. Click "Add Trigger"
 *   3. Function: sweepStaleActionFiles
 *   4. Event source: Time-driven
 *   5. Type: Hour timer → Every hour
 *   6. Click Save
 */
function sweepStaleActionFiles() {
  if (!ACTIONS_FOLDER_ID) return; // Folder not configured yet

  var folder = DriveApp.getFolderById(ACTIONS_FOLDER_ID);
  var files = folder.getFiles();
  var now = Math.floor(Date.now() / 1000);
  var cutoff = now - ACTION_TTL_SECONDS;

  var deleted = 0;
  var pruned = 0;
  var fileList = []; // Track files for MAX_ACTION_FILES enforcement

  while (files.hasNext()) {
    var file = files.next();
    var actions = [];
    try { actions = JSON.parse(file.getBlob().getDataAsString()); } catch (err) { actions = []; }

    // Filter out expired entries
    var live = actions.filter(function(a) { return a.timestamp >= cutoff; });

    if (live.length === 0) {
      // Every entry is expired — delete the entire file. This is the main
      // way we reclaim Drive storage from rooms that are no longer active.
      file.setTrashed(true);
      deleted++;
    } else {
      // Some entries are still live — write back the pruned version.
      if (live.length < actions.length) {
        file.setContent(JSON.stringify(live));
        pruned++;
      }
      // Track this file for the MAX_ACTION_FILES cap check below.
      // Use the newest entry's timestamp to decide which files to keep
      // if we need to enforce the cap.
      var newest = 0;
      for (var i = 0; i < live.length; i++) {
        if (live[i].timestamp > newest) newest = live[i].timestamp;
      }
      fileList.push({ file: file, newestTimestamp: newest });
    }
  }

  // --- MAX_ACTION_FILES enforcement ---
  // If we still have more files than the cap allows (shouldn't happen in
  // normal gameplay with 25 testers, but protects against bugs or abuse),
  // delete the oldest files first. "Oldest" = the file whose newest entry
  // has the smallest timestamp, meaning it's the least recently active room.
  if (fileList.length > MAX_ACTION_FILES) {
    fileList.sort(function(a, b) { return a.newestTimestamp - b.newestTimestamp; });
    var excess = fileList.length - MAX_ACTION_FILES;
    for (var j = 0; j < excess; j++) {
      fileList[j].file.setTrashed(true);
      deleted++;
    }
  }

  Logger.log("Action sweep complete: " + deleted + " files deleted, " + pruned + " files pruned.");

  // Also sweep stale turn files. A turn file is stale if its turnStartedAt
  // is older than 2x the timeout (no one has touched it in a while).
  var turnCutoff = now - (TURN_TIMEOUT_SECONDS * 2);
  var turnFiles = folder.getFiles();
  var turnDeleted = 0;
  while (turnFiles.hasNext()) {
    var tf = turnFiles.next();
    if (tf.getName().indexOf("turns_") !== 0) continue;
    try {
      var ts = JSON.parse(tf.getBlob().getDataAsString());
      if (!ts.queue || ts.queue.length === 0 || ts.turnStartedAt < turnCutoff) {
        tf.setTrashed(true);
        turnDeleted++;
      }
    } catch (err) {
      tf.setTrashed(true);
      turnDeleted++;
    }
  }
  if (turnDeleted > 0) {
    Logger.log("Turn sweep: " + turnDeleted + " stale turn files deleted.");
  }
}

function handleAdminResetAllTrades(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!validateAdminCode(code)) {
    return { success: false, message: "Unauthorized. Admin access required." };
  }
  var count = deleteAllFilesInFolder(TRADES_FOLDER_ID);
  return { success: true, message: "Reset complete. " + count + " trade files deleted." };
}

function handleAdminResetAllActions(body) {
  var code = (body.code || "").trim().toUpperCase();
  if (!validateAdminCode(code)) {
    return { success: false, message: "Unauthorized. Admin access required." };
  }
  var count = deleteAllFilesInFolder(ACTIONS_FOLDER_ID);
  return { success: true, message: "Reset complete. " + count + " action files deleted." };
}

// ========== UTILITY ==========

function findFileInFolder(folderId, fileName) {
  var folder = DriveApp.getFolderById(folderId);
  var files = folder.getFilesByName(fileName);
  if (files.hasNext()) {
    return files.next();
  }
  return null;
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
