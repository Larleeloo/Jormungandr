package com.larleeloo.jormungandr.data;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LocalCache {
    private final File baseDir;

    public LocalCache(Context context) {
        this.baseDir = new File(context.getFilesDir(), "jormungandr");
        ensureDirectories();
    }

    private void ensureDirectories() {
        new File(baseDir, "rooms").mkdirs();
        new File(baseDir, "players").mkdirs();
    }

    public void saveRoom(String roomId, String json) {
        writeFile(new File(baseDir, "rooms/" + roomId + ".json"), json);
    }

    public String loadRoom(String roomId) {
        return readFile(new File(baseDir, "rooms/" + roomId + ".json"));
    }

    public boolean roomExists(String roomId) {
        return new File(baseDir, "rooms/" + roomId + ".json").exists();
    }

    public void savePlayer(String accessCode, String json) {
        writeFile(new File(baseDir, "players/player_" + accessCode + ".json"), json);
    }

    public String loadPlayer(String accessCode) {
        return readFile(new File(baseDir, "players/player_" + accessCode + ".json"));
    }

    public boolean playerExists(String accessCode) {
        return new File(baseDir, "players/player_" + accessCode + ".json").exists();
    }

    public void saveGeneric(String path, String json) {
        File file = new File(baseDir, path);
        file.getParentFile().mkdirs();
        writeFile(file, json);
    }

    public String loadGeneric(String path) {
        return readFile(new File(baseDir, path));
    }

    private void writeFile(File file, String content) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            android.util.Log.w("LocalCache", "Failed to write file: " + file.getName(), e);
        }
    }

    private String readFile(File file) {
        if (!file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            android.util.Log.w("LocalCache", "Failed to read file: " + file.getName(), e);
            return null;
        }
    }

    public File getBaseDir() {
        return baseDir;
    }
}
