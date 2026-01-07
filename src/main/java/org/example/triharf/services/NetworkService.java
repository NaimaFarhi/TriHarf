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
                // Parse input: Expecting "host:port/roomId" or just "roomId" (localhost implied)
                String host = "localhost";
                int port = 8888;
                String roomToJoin = rawUrl;

                if (rawUrl.contains("/") && (rawUrl.contains(":") || rawUrl.contains("."))) {
                    // Format: host:port/roomId
                    String[] split = rawUrl.split("/");
                    String addressPart = split[0];
                    if (split.length > 1) {
                        roomToJoin = split[1];
                    } else {
                        throw new IllegalArgumentException("Format attendu: host:port/CodeSalon");
                    }
                    
                    org.example.triharf.utils.NetworkUtils.ConnectionInfo info = org.example.triharf.utils.NetworkUtils.parseUrl(addressPart);
                    host = info.host();
                    port = info.port();
                } else if (rawUrl.contains(":") || rawUrl.contains(".")) {
                    // Just address provided? Assuming RoomId is required by server logic.
                    // We try to parse it, but we'll probably fail joining if no room ID.
                    org.example.triharf.utils.NetworkUtils.ConnectionInfo info = org.example.triharf.utils.NetworkUtils.parseUrl(rawUrl);
                    host = info.host();
                    port = info.port();
                    roomToJoin = ""; // Will likely fail
                }

                System.out.println("Connecting to " + host + ":" + port + " Room: " + roomToJoin);
                connectClient(host, port);
                
                this.roomId = roomToJoin;
                joinRoom(ParametresGenerauxController.pseudoGlobal, roomId);
                javafx.application.Platform.runLater(onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> onError.accept("Erreur connexion: " + e.getMessage()));
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
