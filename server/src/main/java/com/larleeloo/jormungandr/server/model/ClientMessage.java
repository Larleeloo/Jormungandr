package com.larleeloo.jormungandr.server.model;

/**
 * Represents an incoming message from an Android client over WebSocket.
 *
 * All messages have a "type" field that determines how they're routed.
 * Additional fields are populated depending on the message type.
 *
 * Example messages:
 *   {"type":"authenticate","code":"JORM-ALPHA-001"}
 *   {"type":"getPlayer","code":"JORM-ALPHA-001"}
 *   {"type":"savePlayer","code":"JORM-ALPHA-001","data":"{...}"}
 *   {"type":"getRoom","roomId":"r3_04250"}
 *   {"type":"saveRoom","roomId":"r3_04250","data":"{...}"}
 *   {"type":"joinTurnQueue","roomId":"r3_04250","code":"JORM-ALPHA-001"}
 */
public class ClientMessage {

    private String type;
    private String code;
    private String roomId;
    private String data;
    private String note;
    private String actionText;
    private int range;
    private long since;
    private int region;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }

    public int getRange() { return range; }
    public void setRange(int range) { this.range = range; }

    public long getSince() { return since; }
    public void setSince(long since) { this.since = since; }

    public int getRegion() { return region; }
    public void setRegion(int region) { this.region = region; }
}
