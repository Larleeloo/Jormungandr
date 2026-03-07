package com.larleeloo.jormungandr.data;

import com.larleeloo.jormungandr.model.Room;

public class RoomFileManager {
    private final LocalCache localCache;

    public RoomFileManager(LocalCache localCache) {
        this.localCache = localCache;
    }

    public Room loadRoom(String roomId) {
        String json = localCache.loadRoom(roomId);
        if (json == null) return null;
        return JsonHelper.fromJson(json, Room.class);
    }

    public void saveRoom(Room room) {
        String json = JsonHelper.toJson(room);
        localCache.saveRoom(room.getRoomId(), json);
    }

    public boolean roomExists(String roomId) {
        return localCache.roomExists(roomId);
    }
}
