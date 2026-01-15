package org.example.triharf.network;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkDiscovery {
    private static final int DISCOVERY_PORT = 8889;
    private static final String HEADER = "TRIHARF_SERVER";

    private AtomicBoolean broadcasting = new AtomicBoolean(false);
    private Thread broadcastThread;

    /**
     * Start broadcasting the server existence
     * 
     * @param gamePort The TCP port the game server is listening on
     */
    public void startBroadcasting(int gamePort) {
        if (broadcasting.get())
            return;
        broadcasting.set(true);

        broadcastThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);

                String message = HEADER + ":" + gamePort;
                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

                System.out.println("📢 Démarrage du broadcasting sur le port " + DISCOVERY_PORT);

                while (broadcasting.get()) {
                    try {
                        // Broadcast to 255.255.255.255
                        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress,
                                DISCOVERY_PORT);
                        socket.send(packet);

                        // Also try to broadcast to all interface broadcast addresses (more reliable on
                        // some networks)
                        broadcastToAllInterfaces(socket, buffer, DISCOVERY_PORT);

                        Thread.sleep(1000); // Broadcast every second
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.err.println("Erreur broadcast loop: " + e.getMessage());
                    }
                }
            } catch (SocketException e) {
                System.err.println("Erreur init broadcast: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        });
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    private void broadcastToAllInterfaces(DatagramSocket socket, byte[] buffer, int port) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp())
                    continue;

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, port);
                            socket.send(packet);
                        } catch (Exception e) {
                            // Ignore specific send errors
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore interface enumeration errors
        }
    }

    public void stopBroadcasting() {
        broadcasting.set(false);
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    /**
     * Listen for a server broadcast
     * 
     * @param timeoutMs How long to listen in milliseconds
     * @return The IP string of the server, or null if not found
     */
    public static String discoverServer(int timeoutMs) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setReuseAddress(true); // Allow multiple clients on same machine
            socket.setSoTimeout(timeoutMs);

            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    if (message.startsWith(HEADER)) {
                        String ip = packet.getAddress().getHostAddress();
                        // If it's localhost, it might return 127.0.0.1 or local network IP
                        System.out.println("🔭 Serveur découvert: " + ip + " (" + message + ")");
                        return ip;
                    }
                } catch (SocketTimeoutException e) {
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur découverte: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
        return null;
    }
}
