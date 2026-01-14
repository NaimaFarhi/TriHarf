package org.example.triharf.network;

import org.example.triharf.enums.Langue;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.HashMap;

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

    private NetworkDiscovery discovery;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("Server started on port " + PORT);

        // Start UDP Discovery
        discovery = new NetworkDiscovery();
        discovery.startBroadcasting(PORT);

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
        if (discovery != null) {
            discovery.stopBroadcasting();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        threadPool.shutdown();
    }

    public synchronized void startGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.canStart()) {
            room.setGameStarted(true);

            Character letter = generateLetter();
            room.setCurrentLetter(letter);

            // Get player pseudos list
            List<String> playerPseudos = new ArrayList<>();
            for (String pid : room.getPlayerIds()) {
                playerPseudos.add(room.getPseudo(pid));
            }

            Map<String, Object> gameData = new HashMap<>();
            gameData.put("letter", letter.toString());
            gameData.put("duration", room.getRoundDuration());
            gameData.put("totalRounds", room.getTotalRounds());
            gameData.put("categories", room.getCategories());
            gameData.put("players", playerPseudos);
            gameData.put("gameMode", room.getGameMode());

            broadcast(roomId, new NetworkMessage(
                    NetworkMessage.Type.GAME_START,
                    "SERVER",
                    gameData));
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

        // Auto-ready the player
        room.setPlayerReady(clientId, true);

        // Send room info to the joining player (so they know maxPlayers, etc.)
        ClientHandler newClient = clients.get(clientId);
        if (newClient != null) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("maxPlayers", room.getMaxPlayers());
            roomInfo.put("roomId", room.getRoomId());
            newClient.sendMessage(new NetworkMessage(
                    NetworkMessage.Type.ROOM_INFO,
                    "SERVER",
                    roomInfo));
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
                playerStatusList);
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
                    playerStatusList);
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
                if (client != null)
                    client.sendMessage(message);
            });
        }
    }

    // Broadcast chat message to all players except the sender
    public void broadcastChat(String roomId, String senderClientId, NetworkMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.getPlayerIds().forEach(playerId -> {
                // Don't send back to the sender
                if (!playerId.equals(senderClientId)) {
                    ClientHandler client = clients.get(playerId);
                    if (client != null)
                        client.sendMessage(message);
                }
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
                    playerStatusList);
            broadcast(roomId, msg);
        }
    }

    // Handle player validation
    @SuppressWarnings("unchecked")
    public void handleValidation(String roomId, String senderClientId, NetworkMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room == null)
            return;

        Map<String, Object> validationData = (Map<String, Object>) message.getData();
        String playerPseudo = (String) validationData.get("player");
        Map<String, String> answers = (Map<String, String>) validationData.get("answers");

        // Store validation in room
        room.validatePlayer(playerPseudo, answers);
        System.out.println("✅ " + playerPseudo + " a validé ses réponses (" + room.getValidatedPlayers().size() + "/"
                + room.getPlayerIds().size() + ")");

        // Broadcast validation to other players (excluding sender)
        room.getPlayerIds().forEach(playerId -> {
            if (!playerId.equals(senderClientId)) {
                ClientHandler client = clients.get(playerId);
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        });

        // Check if all players have validated
        if (room.allPlayersValidated()) {
            System.out.println("🎉 Tous les joueurs ont validé - envoi ALL_VALIDATED");

            // Send ALL_VALIDATED with all answers to everyone
            NetworkMessage allValidatedMsg = new NetworkMessage(
                    NetworkMessage.Type.ALL_VALIDATED,
                    "SERVER",
                    room.getValidatedAnswers());
            broadcast(roomId, allValidatedMsg);
        }
    }
}
