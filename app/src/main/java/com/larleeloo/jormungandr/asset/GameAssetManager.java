package com.larleeloo.jormungandr.asset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads sprites by ID from the assets/ folder.
 * Falls back to PlaceholderRenderer when the file is missing.
 * Uses LruCache sized to 1/8 available heap.
 */
public class GameAssetManager {
    private static GameAssetManager instance;
    private final Context context;
    private final LruCache<String, Bitmap> cache;
    /** Paths known to be missing — avoids repeated I/O for non-existent assets. */
    private final Set<String> missingPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private GameAssetManager(Context context) {
        this.context = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        this.cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static synchronized GameAssetManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAssetManager(context);
        }
        return instance;
    }

    /** Clear the cached instance and evict all cached sprites. */
    public static synchronized void reset() {
        if (instance != null) {
            instance.cache.evictAll();
            instance.missingPaths.clear();
        }
        instance = null;
    }

    /**
     * Load a sprite by its asset path. Returns null if not found (caller should use placeholder).
     */
    public Bitmap loadSprite(String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) return null;
        if (missingPaths.contains(assetPath)) return null;

        Bitmap cached = cache.get(assetPath);
        if (cached != null) return cached;

        try {
            InputStream is = context.getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap != null) {
                cache.put(assetPath, bitmap);
            } else {
                missingPaths.add(assetPath);
            }
            return bitmap;
        } catch (IOException e) {
            missingPaths.add(assetPath);
            return null;
        }
    }

    /**
     * Try to load a sprite by ID, checking multiple possible paths and extensions.
     */
    public Bitmap loadSpriteById(String spriteId, String categoryPath) {
        if (spriteId == null) return null;

        String[] extensions = {".png", ".gif", ".jpg", ".webp"};
        String[] basePaths = {
                categoryPath + "/" + spriteId,
                spriteId
        };

        for (String basePath : basePaths) {
            for (String ext : extensions) {
                Bitmap bmp = loadSprite(basePath + ext);
                if (bmp != null) return bmp;
            }
        }
        return null;
    }

    /**
     * Check if an asset exists without loading it.
     */
    public boolean assetExists(String assetPath) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void clearCache() {
        cache.evictAll();
    }
}
