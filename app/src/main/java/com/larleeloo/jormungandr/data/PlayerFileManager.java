package com.larleeloo.jormungandr.data;

import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.model.Player;

/**
 * Cloud-only player persistence. All reads and writes go directly to
 * Google Drive via the Apps Script backend. No local files are stored.
 */
public class PlayerFileManager {
    private static final String TAG = "PlayerFileManager";
    private final AppsScriptClient cloudClient;

    public PlayerFileManager(AppsScriptClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public Player loadPlayer(String accessCode) {
        if (!cloudClient.isConfigured()) {
            Log.w(TAG, "Cloud not configured — cannot load player");
            return null;
        }
        try {
            SyncResult result = cloudClient.getPlayer(accessCode);
            if (result.isSuccess() && result.getData() != null) {
                return JsonHelper.fromJson(result.getData(), Player.class);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load player from cloud", e);
        }
        return null;
    }

    public void savePlayer(Player player) {
        if (!cloudClient.isConfigured()) {
            Log.w(TAG, "Cloud not configured — cannot save player");
            return;
        }
        try {
            String json = JsonHelper.toJson(player);
            SyncResult result = cloudClient.savePlayer(player.getAccessCode(), json);
            if (!result.isSuccess()) {
                Log.w(TAG, "Failed to save player to cloud: " + result.getMessage());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to save player to cloud", e);
        }
    }

    public boolean playerExists(String accessCode) {
        if (!cloudClient.isConfigured()) return false;
        try {
            SyncResult result = cloudClient.getPlayer(accessCode);
            return result.isSuccess() && result.getData() != null
                    && !result.getData().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
