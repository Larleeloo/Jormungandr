package com.larleeloo.jormungandr.server.model;

/**
 * Represents an outgoing message from the server to an Android client.
 *
 * Matches the existing SyncResult contract the client expects:
 *   {"type":"<responseType>","success":true|false,"message":"...","data":"..."}
 *
 * The "type" field echoes the request type so the client can match responses.
 * The "data" field carries JSON payloads (player data, room data, etc.)
 * as a raw JSON string, matching how the Apps Script backend returns data.
 */
public class ServerMessage {

    private String type;
    private boolean success;
    private String message;
    private String data;

    public ServerMessage() {}

    public ServerMessage(String type, boolean success, String message) {
        this.type = type;
        this.success = success;
        this.message = message;
    }

    public ServerMessage(String type, boolean success, String message, String data) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static ServerMessage ok(String type, String message) {
        return new ServerMessage(type, true, message);
    }

    public static ServerMessage ok(String type, String message, String data) {
        return new ServerMessage(type, true, message, data);
    }

    public static ServerMessage error(String type, String message) {
        return new ServerMessage(type, false, message);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
