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
}
