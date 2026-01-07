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

    public synchronized GameRoom createRoom(String roomId, int maxPlayers, Langue langue) {
        if (!rooms.containsKey(roomId)) {
            GameRoom room = new GameRoom(roomId, maxPlayers, langue);
            rooms.put(roomId, room);
            return room;
        }
        return null;
    }

    public synchronized boolean joinRoom(String clientId, String roomId, String pseudo) {
        GameRoom room = rooms.get(roomId);
        if (room == null || !room.addPlayer(clientId, pseudo)) {
            return false;
        }

        broadcastPlayerStatus(roomId);
        return true;
    }

    public synchronized void setPlayerReady(String clientId, String roomId, boolean ready) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.setPlayerReady(clientId, ready);
            broadcastPlayerStatus(roomId);

            // Auto-start if all ready
            if (room.allPlayersReady()) {
                startGame(roomId);
            }
        }
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

    private Character generateLetter() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return letters.charAt(new Random().nextInt(letters.length()));
    }

    public synchronized void leaveRoom(String clientId, String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.removePlayer(clientId);
            if (room.getPlayerIds().isEmpty()) {
                rooms.remove(roomId);
            } else {
                broadcastPlayerStatus(roomId);
            }
        }
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void broadcast(String roomId, NetworkMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.getPlayerIds().forEach(playerId -> {
                ClientHandler client = clients.get(playerId);
                if (client != null) client.sendMessage(message);
            });
        }
    }
}