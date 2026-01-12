package org.example.triharf.network;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientId;
    private GameServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String currentRoomId;

    public ClientHandler(Socket socket, String clientId, GameServer server) throws IOException {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                NetworkMessage message = NetworkMessage.fromJson(line);
                handleMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + clientId);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case JOIN_ROOM -> {
                currentRoomId = (String) message.getData();
                String pseudo = message.getSenderId();
                server.joinRoom(clientId, currentRoomId, pseudo);
            }
            case PLAYER_READY -> {
                if (currentRoomId != null) {
                    server.setPlayerReady(clientId, currentRoomId, (Boolean) message.getData());
                }
            }
            case SUBMIT_ANSWER -> handleSubmitAnswer(message);
            case DISCONNECT -> {
                if (currentRoomId != null) {
                    server.leaveRoom(clientId, currentRoomId);
                }
            }
            case CHAT -> {
                // Broadcast chat message to all players in the room (except sender)
                if (currentRoomId != null) {
                    server.broadcastChat(currentRoomId, clientId, message);
                }
            }
        }
    }

    private void handleSubmitAnswer(NetworkMessage message) {
        if (currentRoomId == null) return;

        @SuppressWarnings("unchecked")
        Map<String, String> answerData = (Map<String, String>) message.getData();
    }

    public void sendMessage(NetworkMessage message) {
        out.println(message.toJson());
    }

    private void cleanup() {
        try {
            socket.close();
            server.handleClientDisconnect(clientId);
        } catch (IOException e) {}
    }

    public String getClientId() { return clientId; }
}