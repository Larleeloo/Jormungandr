package com.example.jormungandr.data;

import com.example.jormungandr.model.Player;

public class PlayerFileManager {
    private final LocalCache localCache;

    public PlayerFileManager(LocalCache localCache) {
        this.localCache = localCache;
    }

    public Player loadPlayer(String accessCode) {
        String json = localCache.loadPlayer(accessCode);
        if (json == null) return null;
        return JsonHelper.fromJson(json, Player.class);
    }

    public void savePlayer(Player player) {
        String json = JsonHelper.toJson(player);
        localCache.savePlayer(player.getAccessCode(), json);
    }

    public boolean playerExists(String accessCode) {
        return localCache.playerExists(accessCode);
    }
}
