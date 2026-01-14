package org.example.triharf.network;

import com.google.gson.Gson;

public class NetworkMessage {
    public enum Type {
        JOIN_ROOM,
        PLAYER_JOINED,
        PLAYER_READY,
        GAME_START,
        SUBMIT_ANSWER,
        GAME_END,
        DISCONNECT,
        CHAT,
        PLAYER_ANSWERS,
        VALIDATE_ANSWERS,
        ALL_VALIDATED,
        NEXT_ROUND,
        SHOW_RESULTS,
        PLAYER_ELIMINATED,
        VALIDATION_RESULTS,
        ROOM_INFO,

        CHAOS_EVENT,
        GAME_ENDED_HOST_LEFT,
        PLAYER_LEFT
    }

    private Type type;
    private String senderId;
    private Object data;

    // Constructor, getters, setters
    public NetworkMessage(Type type, String senderId, Object data) {
        this.type = type;
        this.senderId = senderId;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static NetworkMessage fromJson(String json) {
        return new Gson().fromJson(json, NetworkMessage.class);
    }
}