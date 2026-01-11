package org.example.triharf.network;

import org.example.triharf.enums.Langue;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private Map<String, GameRoom> rooms;
    private Map<String, ClientHandler> clients;
    private ExecutorService threadPool;
    private boolean running;

    public GameServer() {
        this.rooms = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("Server started on port " + PORT);

        while (running) {
            Socket clientSocket = serverSocket.accept();
            String clientId = UUID.randomUUID().toString();
            ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
            clients.put(clientId, handler);
            threadPool.execute(handler);
            System.out.println("Client connected: " + clientId);
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
        threadPool.shutdown();
    }

    public synchronized void startGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.canStart()) {
            room.setGameStarted(true);

            Character letter = generateLetter();
            room.setCurrentLetter(letter);

            Map<String, Object> gameData = Map.of(
                    "letter", letter.toString(),
                    "duration", 180
            );
            broadcast(roomId, new NetworkMessage(
                    NetworkMessage.Type.GAME_START,
                    "SERVER",
                    gameData
            ));
        }
    }

    private Character generateLetter() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return letters.charAt(new Random().nextInt(letters.length()));
    }


    // Create new game room
    public synchronized GameRoom createRoom(String roomId, int maxPlayers, Langue langue) {
        if (!rooms.containsKey(roomId)) {
            GameRoom room = new GameRoom(roomId, maxPlayers, langue);
            rooms.put(roomId, room);
            return room;
        }
        return null; // Room exists
    }

    // Player joins room
    public synchronized boolean joinRoom(String clientId, String roomId, String pseudo) {
        GameRoom room = rooms.get(roomId);
        if (room == null || !room.addPlayer(clientId, pseudo)) {
            return false;
        }

        // Notify all players in room with status list (using pseudos)
        List<String> playerStatusList = new ArrayList<>();
        for (String pid : room.getPlayerIds()) {
            String name = room.getPseudo(pid);
            String status = room.getReadyPlayers().contains(pid) ? "PREST" : "ATTENTE";
            playerStatusList.add(name + ":" + status);
        }

        NetworkMessage msg = new NetworkMessage(
                NetworkMessage.Type.PLAYER_JOINED,
                "SERVER",
                playerStatusList
        );
        broadcast(roomId, msg);
        return true;
    }

    public synchronized void setPlayerReady(String clientId, String roomId, boolean ready) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.setPlayerReady(clientId, ready);
            
            // Map player names to status
            List<String> playerStatusList = new ArrayList<>();
            for (String pid : room.getPlayerIds()) {
                String name = room.getPseudo(pid);
                String status = room.getReadyPlayers().contains(pid) ? "PREST" : "ATTENTE";
                playerStatusList.add(name + ":" + status);
            }

            NetworkMessage msg = new NetworkMessage(
                    NetworkMessage.Type.PLAYER_JOINED,
                    "SERVER",
                    playerStatusList
            );
            broadcast(roomId, msg);
        }
    }

    // Player leaves room
    public synchronized void leaveRoom(String clientId, String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.removePlayer(clientId);

            // Remove empty rooms
            if (room.getPlayerIds().isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    // Get room info
    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    // Send message to all players in room
    public void broadcast(String roomId, NetworkMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.getPlayerIds().forEach(playerId -> {
                ClientHandler client = clients.get(playerId);
                if (client != null) client.sendMessage(message);
            });
        }
    }

    // In ClientHandler cleanup or when client disconnects
    public void handleClientDisconnect(String clientId) {
        // Find which room this client was in
        for (GameRoom room : rooms.values()) {
            if (room.getPlayerIds().contains(clientId)) {
                leaveRoom(clientId, room.getRoomId());

                // Notify remaining players
                broadcastPlayerStatus(room.getRoomId());
                break;
            }
        }
        clients.remove(clientId);
    }

    private void broadcastPlayerStatus(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            List<String> playerStatusList = new ArrayList<>();
            for (String pid : room.getPlayerIds()) {
                String name = room.getPseudo(pid);
                String status = room.getReadyPlayers().contains(pid) ? "PREST" : "ATTENTE";
                playerStatusList.add(name + ":" + status);
            }

            NetworkMessage msg = new NetworkMessage(
                    NetworkMessage.Type.PLAYER_JOINED,
                    "SERVER",
                    playerStatusList
            );
            broadcast(roomId, msg);
        }
    }
}
