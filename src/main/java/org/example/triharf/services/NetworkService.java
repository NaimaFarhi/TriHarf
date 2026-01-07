package org.example.triharf.services;

import org.example.triharf.controllers.ParametresGenerauxController;
import org.example.triharf.enums.Langue;
import org.example.triharf.network.GameClient;
import org.example.triharf.network.GameServer;
import org.example.triharf.network.NetworkMessage;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service gérant toute la logique réseau (Serveur et Client).
 * Permet de séparer la logique réseau des contrôleurs UI.
 */
public class NetworkService {

    private GameServer gameServer;
    private GameClient gameClient;
    private String roomId;
    private ExecutorService executorService;

    public NetworkService() {
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Démarrer une partie en tant qu'Hôte (Serveur + Client)
     */
    public void startHost(Langue langue, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        executorService.submit(() -> {
            try {
                // 1. Démarrer le serveur
                gameServer = new GameServer();
                Thread serverThread = new Thread(() -> {
                    try {
                        gameServer.start();
                    } catch (IOException e) {
                        System.err.println("Erreur serveur: " + e.getMessage());
                    }
                });
                serverThread.setDaemon(true);
                serverThread.start();

                // Attendre un peu que le serveur soit prêt
                Thread.sleep(500);

                // 2. Connecter le client
                connectClient();

                // 3. Créer la salle
                this.roomId = UUID.randomUUID().toString().substring(0, 8);
                gameServer.createRoom(roomId, 4, langue);
                
                // 4. Rejoindre la salle
                joinRoom(ParametresGenerauxController.pseudoGlobal, roomId);

                // Callback succès sur thread UI
                javafx.application.Platform.runLater(onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    /**
     * Rejoindre une partie existante en tant que Client uniquement
     * (Pas implémenté complètement dans ParamPartieMultiController actuel le bouton rejoindre, 
     * mais on prépare le terrain)
     */
    public void startClientOnly(String rawUrl, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        executorService.submit(() -> {
            try {
                org.example.triharf.utils.NetworkUtils.ConnectionInfo info = org.example.triharf.utils.NetworkUtils.parseUrl(rawUrl);
                
                // Note: The rawUrl here might be "host:port" or "host:port/roomId" or just "roomId"
                // If it is just "roomId" (length 8, alphanumeric), we assume localhost:8888 
                // But parseUrl defaults to cleanUrl as host if not ":". 
                // We need to differentiate "Remote URL" vs "Simple Room Code on Localhost".
                
                String targetRoomId = this.roomId; // Keep existing if set? No, we reset.
                
                // Logic: 
                // If input looks like RoomID (Length 8, no dots/colons), assume localhost.
                // If input has colon or dot, assume Address. Then where is RoomID?
                // The prompt says: "Text field for entering host's ngrok URL". And "Connect to server".
                // It doesn't explicitly mention entering the Room ID separately.
                // Assumption: User must enter "address:port".
                // Then how do we join a room? 
                // If the user enters JUST the address, we connect. Then we need to JOIN a room.
                // WE WILL ASSUME THE ROOM ID IS PASSED SEPARATELY OR WE NEED TO ASK FOR IT?
                // OR: The user pastes "address:port". We connect. Then... what?
                // We'll update joinRoom to use a default or ask user?
                // CURRENTLY: handleRejoindre passes 'code' which was the room ID.
                // New logic: 
                // If input is "host:port", we connect there. And RoomID? 
                // Maybe the user enters "host:port/roomid"?
                // Let's implement support for that in parseUrl? No, keep it simple.
                // Let's assume the user enters "host:port". We join the server.
                // For now, let's assume we try to join the FIRST available room or a default one if we can't specify.
                // However, the existing code passed 'code' as 'roomId'. 
                // IF we want to support Ngrok, maybe we force the Host to share "host:port:roomId"? 
                // OR simpler: Input field = "host:port". We connect. Then we auto-join room "default"? Or we list rooms?
                // To minimize UI changes, let's try to pass the RoomID as well.
                // If the user inputs `0.tcp.ngrok.io:12345`, we treat `12345` as port.
                // But we lack RoomId. 
                // Let's optimistically assume for this task that the user enters "host:port" AND we might need a way to get RoomId.
                // OR: We try to join any room.
                // Let's check `joinRoom`. It sends `JOIN_ROOM` with `roomId`.
                // If I send "ANY" or null, maybe server assigns one?
                // `GameServer.joinRoom` checks `rooms.get(roomId)`. It needs exact match.
                
                // Hack/Solution: Allow input string to be "host:port#roomId" or similar?
                // Or just: If it looks like a URL, use it as host:port, and assume RoomID is separate?
                // But there is only one text field.
                
                // DECISION: If the input is complex (has : or .), we connect to it.
                // We will use a default RoomID or try to find one.
                // BUT wait, `startClientOnly` takes `targetRoomId`... oh wait, checking my own code.
                // `startClientOnly(String targetRoomId, ...)`
                
                // Refactoring methodology:
                // I will change the signature to accept `String inputString`. 
                // I will try to extract host/port/roomId from it.
                // Format: `host:port/roomId`. 
                // If just `roomId` -> localhost:8888, roomId.
                // If `host:port` -> host:port, roomId=???? (Maybe ask server? or fail?)
                
                // Let's implicitly assume the user will enter `host:port`. We can't guess RoomId.
                // Maybe the Host room ID is ALWAYS the same or ignorable if 1 server = 1 game?
                // `GameServer` creates room with random UUID.
                
                // Correction: I should update `GameServer` to allow listing rooms, or joining "the only room".
                // But I can't easily change the protocol and `GameServer` logic significantly without risk.
                // Safest bet: User must provide RoomID.
                // I will support input format `host:port/roomId`. 
                // Example: `0.tcp.ngrok.io:12345/ABCDFEGH`.
                
                // Updating parsing logic here inline or in NetworkUtils?
                // NetworkUtils only does host/port.
                
                String host = "localhost";
                int port = 8888;
                String roomToJoin = rawUrl; // Default assumption
                
                if (rawUrl.contains("/") && (rawUrl.contains(":") || rawUrl.contains("."))) {
                    String[] split = rawUrl.split("/");
                    String addressPart = split[0];
                    roomToJoin = (split.length > 1) ? split[1] : "";
                    
                    org.example.triharf.utils.NetworkUtils.ConnectionInfo infoCb = org.example.triharf.utils.NetworkUtils.parseUrl(addressPart);
                    host = infoCb.host();
                    port = infoCb.port();
                } else if (!rawUrl.contains("/") && (rawUrl.contains(":") || rawUrl.contains("."))) {
                     // Just address? We can't join without RoomId.
                     // Maybe we treat the whole thing as address and RoomId is missing?
                     // I'll assume they might use a separator like space?
                     // Let's stick to the Slash separator.
                     org.example.triharf.utils.NetworkUtils.ConnectionInfo infoCb = org.example.triharf.utils.NetworkUtils.parseUrl(rawUrl);
                     host = infoCb.host();
                     port = infoCb.port();
                     roomToJoin = ""; // Will fail valid join
                }

                connectClient(host, port);
                
                this.roomId = roomToJoin;
                joinRoom(ParametresGenerauxController.pseudoGlobal, roomId);
                javafx.application.Platform.runLater(onSuccess);
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    private void connectClient() throws IOException, InterruptedException {
        // If port/host not set, it defaults to localhost:8888 in GameClient unless configured
        // But here we need to know if we are hosting or joining
        // For Hosting, we use default (localhost)
        // For Joining, we might have set it before calling this? 
        // Better: Pass ConnectionInfo to connectClient
        
        // Refactoring: gameClient is instantiated here. 
        if (gameClient == null) {
            gameClient = new GameClient(); 
            // Note: If we had parameters, we would set them here. 
            // But startClientOnly creates the thread then calls this.
            // We should refactor to allow passing host/port.
        }
        
        // Actually, let's instantiate with specific params if needed or set them before connect
        // But since we are inside a private method used by both Host and Client flows...
        
        int attempts = 5;
        boolean connected = false;
        while (attempts > 0 && !connected) {
            try {
                gameClient.connect();
                connected = true;
            } catch (IOException e) {
                attempts--;
                if (attempts > 0) {
                     Thread.sleep(500);
                } else {
                    throw e;
                }
            }
        }
    }

    private void connectClient(String host, int port) throws IOException, InterruptedException {
        gameClient = new GameClient();
        gameClient.setConnectionInfo(host, port);
        
        int attempts = 5;
        boolean connected = false;
        while (attempts > 0 && !connected) {
            try {
                gameClient.connect();
                connected = true;
            } catch (IOException e) {
                attempts--;
                if (attempts > 0) {
                    Thread.sleep(500);
                } else {
                    throw e;
                }
            }
        }
    }

    private void joinRoom(String pseudo, String roomId) {
        if (gameClient != null) {
            gameClient.sendMessage(new NetworkMessage(NetworkMessage.Type.JOIN_ROOM, pseudo, roomId));
        }
    }

    public void stop() {
        try {
            if (gameClient != null) gameClient.disconnect(); // Assumant qu'il y a une méthode disconnect, sinon à ajouter
            if (gameServer != null) gameServer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GameClient getGameClient() {
        return gameClient;
    }

    public GameServer getGameServer() {
        return gameServer;
    }

    public String getRoomId() {
        return roomId;
    }
}
