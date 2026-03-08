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
var GAME_VERSION     = "1.0";

// Valid access codes (JORM-ALPHA-001 through JORM-ALPHA-025)
var VALID_CODES = [];
for (var i = 1; i <= 25; i++) {
  VALID_CODES.push("JORM-ALPHA-" + ("00" + i).slice(-3));
}

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
