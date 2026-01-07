package org.example.triharf.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.triharf.utils.NetworkUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service pour gérer le processus Ngrok localement.
 * Lance 'ngrok tcp <port>' et récupère l'URL publique via l'API locale.
 */
public class NgrokService {

    private Process ngrokProcess;
    private static final String API_URL = "http://127.0.0.1:4040/api/tunnels";
    private final Gson gson = new Gson();

    /**
     * Tente de démarrer Ngrok sur le port spécifié et retourne l'URL publique.
     * @param port Le port local (ex: 8888)
     * @return CompletableFuture contenant l'URL publique (ex: "0.tcp.ngrok.io:12345")
     */
    public CompletableFuture<String> startNgrok(int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Vérifier si ngrok tourne déjà ou le lancer
                if (isNgrokRunning()) {
                    System.out.println("⚠️ Ngrok semble déjà tourner. Tentative de récupération d'URL existante...");
                } else {
                    startNgrokProcess(port);
                    // Laisser le temps à Ngrok de s'initialiser
                    Thread.sleep(2000);
                }

                // 2. Récupérer l'URL via l'API
                // On fait quelques essais
                for (int i = 0; i < 5; i++) {
                    String url = fetchPublicUrl();
                    if (url != null && !url.isEmpty()) {
                        return url;
                    }
                    Thread.sleep(1000);
                }
                
                throw new RuntimeException("Délai d'attente dépassé pour l'URL Ngrok.");

            } catch (Exception e) {
                if (ngrokProcess != null) {
                    ngrokProcess.destroy();
                    ngrokProcess = null;
                }
                throw new RuntimeException("Erreur Ngrok: " + e.getMessage(), e);
            }
        });
    }

    private void startNgrokProcess(int port) throws IOException {
        System.out.println("🚀 Démarrage de Ngrok sur le port " + port + "...");
        ProcessBuilder pb = new ProcessBuilder("ngrok", "tcp", String.valueOf(port), "--log=stdout");
        pb.redirectErrorStream(true);
        this.ngrokProcess = pb.start();
        
        // Lire la sortie dans un thread séparé pour ne pas bloquer et pour debug
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ngrokProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // System.out.println("[Ngrok] " + line); // Uncomment for debug
                }
            } catch (IOException e) {
                // Ignore
            }
        }).start();
        
        // S'assurer de tuer ngrok à la fermeture de l'app
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopNgrok));
    }

    public void stopNgrok() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            System.out.println("🛑 Arrêt du processus Ngrok.");
            ngrokProcess.destroy();
            ngrokProcess = null;
        }
    }

    private boolean isNgrokRunning() {
        // Simple check: try to hit the API
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(500);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private String fetchPublicUrl() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            
            if (conn.getResponseCode() != 200) return null;

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            // Parsing JSON avec Gson
            JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
            JsonArray tunnels = json.getAsJsonArray("tunnels");
            
            if (tunnels.size() > 0) {
                // On prend le premier tunnel public
                for (JsonElement t : tunnels) {
                    JsonObject tunnel = t.getAsJsonObject();
                    String publicUrl = tunnel.get("public_url").getAsString();
                    if (publicUrl.startsWith("tcp://")) {
                        // On nettoie l'URL pour l'affichage (enlève tcp://)
                        return publicUrl.replace("tcp://", "");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur fetch API Ngrok: " + e.getMessage());
        }
        return null;
    }
}
