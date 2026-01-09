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

    private static final String DEFAULT_ROOM_ID = "LAN_GAME";

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
                // Use a fixed Room ID for LAN play simplicity
                this.roomId = DEFAULT_ROOM_ID;
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
     */
    public void startClientOnly(String rawUrl, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        executorService.submit(() -> {
            try {
                String host = "localhost";
                int port = 8888;
                String roomToJoin = DEFAULT_ROOM_ID; // Default to LAN room

                // Check if Room ID is provided (e.g. "1.2.3.4:8888/MyRoom")
                String addressPart = rawUrl;
                if (rawUrl.contains("/")) {
                    String[] split = rawUrl.split("/", 2);
                    addressPart = split[0];
                    if (split.length > 1 && !split[1].isBlank()) {
                        roomToJoin = split[1];
                    }
                }

                org.example.triharf.utils.NetworkUtils.ConnectionInfo info = org.example.triharf.utils.NetworkUtils
                        .parseUrl(addressPart);
                host = info.host();
                port = info.port();

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
        // If port/host not set, it defaults to localhost:8888 in GameClient unless
        // configured
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

        // Actually, let's instantiate with specific params if needed or set them before
        // connect
        // But since we are inside a private method used by both Host and Client
        // flows...

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
            if (gameClient != null)
                gameClient.disconnect(); // Assumant qu'il y a une méthode disconnect, sinon à ajouter
            if (gameServer != null)
                gameServer.stop();
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
