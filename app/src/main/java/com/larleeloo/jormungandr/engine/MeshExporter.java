package com.larleeloo.jormungandr.engine;

import android.content.Context;
import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.data.LocalCache;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One-time tool to export the full WorldMesh as a single JSON reference file.
 * Generates the mesh in memory, serializes it to JSON, saves locally, and
 * uploads to Drive as a single file (world_mesh_reference.json).
 *
 * After this runs once, the mesh reference lives in Drive and can be downloaded
 * on future app starts instead of rebuilding 80,000 nodes in heap memory.
 */
public class MeshExporter {

    private static final String TAG = "MeshExporter";
    public static final String MESH_REFERENCE_FILE = "world_mesh_reference.json";
    private static final String LOCAL_PATH = "mesh/" + MESH_REFERENCE_FILE;

    public interface ExportCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * Export the full WorldMesh to a JSON string.
     * Structure:
     * {
     *   "seed": "0x4A6F726D756E4C",
     *   "version": "1.0",
     *   "totalRooms": 80001,
     *   "regions": {
     *     "0": { "r0_00000": { "FORWARD":"r1_00000", "LEFT":"r8_00000", "RIGHT":"r2_00000" } },
     *     "1": { "r1_00000": { ... }, ... },
     *     ...
     *   }
     * }
     */
    public static String exportToJson() {
        // Build the mesh in memory (deterministic from seed)
        WorldMesh mesh = WorldMesh.getInstance();

        try {
            JSONObject root = new JSONObject();
            root.put("seed", "0x" + Long.toHexString(Constants.WORLD_SEED));
            root.put("version", Constants.GAME_VERSION);
            root.put("totalRooms", mesh.getTotalRoomCount());

            // Group rooms by region
            JSONObject regions = new JSONObject();
            for (int region = 0; region <= Constants.NUM_REGIONS; region++) {
                regions.put(String.valueOf(region), new JSONObject());
            }

            // Iterate all nodes and serialize neighbors
            for (Map.Entry<String, RoomNode> entry : mesh.getAllNodes().entrySet()) {
                String roomId = entry.getKey();
                RoomNode node = entry.getValue();
                int region = node.getRegion();

                JSONObject neighbors = new JSONObject();
                for (Map.Entry<Direction, String> doorEntry : node.getNeighbors().entrySet()) {
                    neighbors.put(doorEntry.getKey().name(), doorEntry.getValue());
                }

                JSONObject regionObj = regions.getJSONObject(String.valueOf(region));
                regionObj.put(roomId, neighbors);
            }

            root.put("regions", regions);
            return root.toString();

        } catch (Exception e) {
            Log.e(TAG, "Failed to export mesh to JSON", e);
            return null;
        }
    }

    /**
     * Export mesh, save locally, and upload to Drive. Runs on a background thread.
     */
    public static void exportAndUpload(Context context, ExportCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String json = exportToJson();
            if (json == null) {
                postCallback(callback, false, "Failed to serialize mesh");
                return;
            }

            Log.i(TAG, "Mesh exported: " + json.length() + " bytes, " +
                    WorldMesh.getInstance().getTotalRoomCount() + " rooms");

            // Save locally
            LocalCache cache = new LocalCache(context);
            cache.saveGeneric(LOCAL_PATH, json);
            Log.i(TAG, "Mesh reference saved locally");

            // Upload to Drive as a single file
            AppsScriptClient client = new AppsScriptClient();
            if (!client.isConfigured()) {
                postCallback(callback, true,
                        "Mesh saved locally. Cloud not configured — set APPS_SCRIPT_URL to upload.");
                return;
            }

            SyncResult result = client.saveMeshReference(json);
            if (result.isSuccess()) {
                postCallback(callback, true,
                        "Mesh reference exported and uploaded to Drive (" + json.length() + " bytes)");
            } else {
                postCallback(callback, false,
                        "Mesh saved locally but Drive upload failed: " + result.getMessage());
            }
        });
        executor.shutdown();
    }

    /**
     * Check if a local mesh reference file already exists.
     */
    public static boolean localReferenceExists(Context context) {
        LocalCache cache = new LocalCache(context);
        String data = cache.loadGeneric(LOCAL_PATH);
        return data != null && !data.isEmpty();
    }

    private static void postCallback(ExportCallback callback, boolean success, String message) {
        if (callback != null) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onComplete(success, message));
        }
    }
}
