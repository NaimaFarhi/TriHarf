package org.example.triharf.utils;

public class NetworkUtils {

    public record ConnectionInfo(String host, int port) {}

    public static ConnectionInfo parseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ConnectionInfo("localhost", 8888);
        }

        // Strip protocol if present
        String cleanUrl = url.replace("tcp://", "").replace("http://", "").replace("https://", "");
        
        // Handle IP:Port or Host:Port
        if (cleanUrl.contains(":")) {
            String[] parts = cleanUrl.split(":");
            if (parts.length == 2) {
                try {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    return new ConnectionInfo(host, port);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port format: " + cleanUrl);
                }
            }
        }

        // Fallback or default
        return new ConnectionInfo(cleanUrl, 8888);
    }
    
    public static String getLocalIpAddress() {
        try (final java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            // Fallback to iteration if socket fails
            try {
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;

                    java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            } catch (Exception ex) { /* ignore */ }
        }
        return "127.0.0.1";
    }
}
