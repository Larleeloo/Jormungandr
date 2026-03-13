package com.larleeloo.jormungandr.engine;

import android.util.Log;

import com.larleeloo.jormungandr.cloud.AppsScriptClient;
import com.larleeloo.jormungandr.cloud.SyncResult;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.util.Constants;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One-time tool to export the full WorldMesh as a single JSON reference file.
 * Generates the mesh in memory, serializes it to JSON, and uploads to Drive.
 * No local files are stored.
 */
public class MeshExporter {

    private static final String TAG = "MeshExporter";

    public interface ExportCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * Export the full WorldMesh to a JSON string.
     */
    public static String exportToJson() {
        WorldMesh mesh = WorldMesh.getInstance();

        try {
            JSONObject root = new JSONObject();
            root.put("seed", "0x" + Long.toHexString(Constants.WORLD_SEED));
            root.put("version", Constants.GAME_VERSION);
            root.put("totalRooms", mesh.getTotalRoomCount());

            JSONObject regions = new JSONObject();
            for (int region = 0; region <= Constants.NUM_REGIONS; region++) {
                regions.put(String.valueOf(region), new JSONObject());
            }

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
     * Export mesh and upload to Drive. Runs on a background thread.
     * No local file is saved.
     */
    public static void exportAndUpload(ExportCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String json = exportToJson();
            if (json == null) {
                postCallback(callback, false, "Failed to serialize mesh");
                return;
            }

            Log.i(TAG, "Mesh exported: " + json.length() + " bytes, " +
                    WorldMesh.getInstance().getTotalRoomCount() + " rooms");

            AppsScriptClient client = new AppsScriptClient();
            if (!client.isConfigured()) {
                postCallback(callback, false,
                        "Cloud not configured — set APPS_SCRIPT_URL to upload mesh reference.");
                return;
            }

            SyncResult result = client.saveMeshReference(json);
            if (result.isSuccess()) {
                postCallback(callback, true,
                        "Mesh reference uploaded to Drive (" + json.length() + " bytes)");
            } else {
                postCallback(callback, false,
                        "Drive upload failed: " + result.getMessage());
            }
        });
        executor.shutdown();
    }

    private static void postCallback(ExportCallback callback, boolean success, String message) {
        if (callback != null) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onComplete(success, message));
        }
    }
}
