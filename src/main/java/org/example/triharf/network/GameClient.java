package org.example.triharf.network;

import org.example.triharf.utils.PropertiesManager;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Client-side network handler for multiplayer games
 * Connects to GameServer and sends/receives messages
 */
public class GameClient {
    private static final String SERVER_HOST = PropertiesManager.getProperty("server.host", "localhost");
    private static final int SERVER_PORT = PropertiesManager.getInt("server.port", 8888); // Server's local IP

    private Socket socket;  // TCP connection to server
    private BufferedReader in;  // Read messages from server
    private PrintWriter out;  // Send messages to server
    private Thread listenerThread;  // Background thread for incoming messages
    private Consumer<NetworkMessage> messageHandler;  // Callback for received messages
    private boolean connected;

    /**
     * Establish connection to server
     * Creates socket, opens streams, starts listener thread
     */
    public void connect() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
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