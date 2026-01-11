package org.example.triharf.network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class GameClient {
    private String serverHost;
    private int serverPort;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private Consumer<NetworkMessage> messageHandler;
    private boolean connected;

    public GameClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    public void connect() throws IOException {
        socket = new Socket(serverHost, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;
        startListening();
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    NetworkMessage message = NetworkMessage.fromJson(line);
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

    public void sendMessage(NetworkMessage message) {
        if (connected) {
            out.println(message.toJson());
        }
    }

    public void disconnect() throws IOException {
        connected = false;
        if (socket != null) socket.close();
        if (listenerThread != null) listenerThread.interrupt();
    }

    public void setMessageHandler(Consumer<NetworkMessage> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return connected;
    }
}