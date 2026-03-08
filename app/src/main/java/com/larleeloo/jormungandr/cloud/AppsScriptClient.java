package com.larleeloo.jormungandr.cloud;

import com.larleeloo.jormungandr.util.Constants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * HTTP client for communicating with the deployed Google Apps Script web app.
 * All requests are POST with JSON body containing an "action" field.
 */
public class AppsScriptClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 4;
    private static final int[] BACKOFF_MS = {2000, 4000, 8000, 16000};

    private final OkHttpClient client;
    private final String scriptUrl;

    public AppsScriptClient() {
        this.scriptUrl = Constants.APPS_SCRIPT_URL;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public SyncResult validateAccessCode(String code) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "validateCode");
            body.put("code", code);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult getPlayer(String accessCode) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "getPlayer");
            body.put("code", accessCode);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult savePlayer(String accessCode, String playerJson) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "savePlayer");
            body.put("code", accessCode);
            body.put("data", playerJson);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult getRoom(String roomId) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "getRoom");
            body.put("roomId", roomId);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult saveRoom(String roomId, String roomJson) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "saveRoom");
            body.put("roomId", roomId);
            body.put("data", roomJson);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult getVersion() {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "getVersion");
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public SyncResult getNotes(String roomId) {
        try {
            JSONObject body = new JSONObject();
            body.put("action", "getNotes");
            body.put("roomId", roomId);
            return execute(body);
        } catch (Exception e) {
            return new SyncResult(false, "Error: " + e.getMessage(), null);
        }
    }

    public boolean isConfigured() {
        return scriptUrl != null && !scriptUrl.isEmpty();
    }

    private SyncResult execute(JSONObject body) {
        if (!isConfigured()) {
            android.util.Log.w("CloudSync", "APPS_SCRIPT_URL is not configured. Cloud sync disabled.");
            return new SyncResult(false, "Cloud sync not configured. Set APPS_SCRIPT_URL in Constants.java", null);
        }

        String action = body.optString("action", "unknown");
        android.util.Log.d("CloudSync", "Executing action: " + action + " to URL: " + scriptUrl);

        RequestBody requestBody = RequestBody.create(body.toString(), JSON);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(scriptUrl)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JSONObject result = new JSONObject(responseBody);
                    boolean success = result.optBoolean("success", false);
                    String message = result.optString("message", "");
                    String data = result.optString("data", null);
                    android.util.Log.d("CloudSync", "Response for " + action + ": success=" + success + " msg=" + message);
                    return new SyncResult(success, message, data);
                }
                android.util.Log.w("CloudSync", "HTTP " + response.code() + " for " + action);

                // Network error - retry
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(BACKOFF_MS[attempt]);
                }
            } catch (IOException e) {
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(BACKOFF_MS[attempt]); } catch (InterruptedException ignored) {}
                } else {
                    return new SyncResult(false, "Network error: " + e.getMessage(), null);
                }
            } catch (Exception e) {
                return new SyncResult(false, "Error: " + e.getMessage(), null);
            }
        }

        return new SyncResult(false, "Max retries exceeded", null);
    }
}
