package org.example.triharf.network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Client-side network handler for multiplayer games
 * Connects to GameServer and sends/receives messages
 */
public class GameClient {
    private String serverHost = "localhost";
    private int serverPort = 8888;

    private Socket socket;  // TCP connection to server
    private BufferedReader in;  // Read messages from server
    private PrintWriter out;  // Send messages to server
    private Thread listenerThread;  // Background thread for incoming messages
    private Consumer<NetworkMessage> messageHandler;  // Callback for received messages
    private boolean connected;

    public void setConnectionInfo(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    /**
     * Establish connection to server
     * Creates socket, opens streams, starts listener thread
     */
    public void connect() throws IOException {
        System.out.println("Connecting to " + serverHost + ":" + serverPort);
        socket = new Socket();
        // Timeout de 5 secondes pour la connexion
        socket.connect(new InetSocketAddress(serverHost, serverPort), 5000);
        
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        startListening();
    }

    /**
     * Start background thread to continuously read messages from server
     * Runs until disconnected or connection lost
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                // Continuously read JSON lines from server
                while (connected && (line = in.readLine()) != null) {
                    NetworkMessage message = NetworkMessage.fromJson(line);
                    // Notify registered handler (e.g., UI controller)
                    if (messageHandler != null) {
                        messageHandler.accept(message);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Connection lost: " + e.getMessage());
                }
            }
        });
        listenerThread.start();
    }

    /**
     * Send message to server
     * Converts to JSON and writes to output stream
     */
    public void sendMessage(NetworkMessage message) {
        if (connected) {
            out.println(message.toJson());
        }
    }

    /**
     * Close connection and stop listener thread
     */
    public void disconnect() throws IOException {
        connected = false;
        if (socket != null) socket.close();
        if (listenerThread != null) listenerThread.interrupt();
    }

    /**
     * Register callback for incoming messages
     * Example: client.setMessageHandler(msg -> System.out.println(msg))
     */
    public void setMessageHandler(Consumer<NetworkMessage> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return connected;
    }
}