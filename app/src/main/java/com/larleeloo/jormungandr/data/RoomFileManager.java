package com.larleeloo.jormungandr.data;

import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.model.Room;

/**
 * Cloud-only room persistence. All reads and writes go directly to
 * Google Drive via the Apps Script backend. No local files are stored.
 */
public class RoomFileManager {
    private static final String TAG = "RoomFileManager";
    private final AppsScriptClient cloudClient;

    public RoomFileManager(AppsScriptClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public Room loadRoom(String roomId) {
        if (!cloudClient.isConfigured()) {
            Log.w(TAG, "Cloud not configured — cannot load room");
            return null;
        }
        try {
            SyncResult result = cloudClient.getRoom(roomId);
            if (result.isSuccess() && result.getData() != null) {
                return JsonHelper.fromJson(result.getData(), Room.class);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load room from cloud", e);
        }
        return null;
    }

    public void saveRoom(Room room) {
        if (!cloudClient.isConfigured()) {
            Log.w(TAG, "Cloud not configured — cannot save room");
            return;
        }
        try {
            String json = JsonHelper.toJson(room);
            SyncResult result = cloudClient.saveRoom(room.getRoomId(), json);
            if (!result.isSuccess()) {
                Log.w(TAG, "Failed to save room to cloud: " + result.getMessage());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to save room to cloud", e);
        }
    }

    public boolean roomExists(String roomId) {
        if (!cloudClient.isConfigured()) return false;
        try {
            SyncResult result = cloudClient.getRoom(roomId);
            return result.isSuccess() && result.getData() != null
                    && !result.getData().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
