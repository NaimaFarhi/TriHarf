package org.example.triharf.utils;

import java.net.*;
import java.util.*;

public class NetworkUtils {

    /**
     * Get local IP address for hosting
     * Prioritizes Wi-Fi adapter for hotspot connectivity
     */
    public static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // Get IPv4 addresses only
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();

                        // Prioritize hotspot range (192.168.x.x)
                        if (ip.startsWith("192.168.")) {
                            return ip;
                        }
                    }
                }
            }

            // Fallback
            return InetAddress.getLocalHost().getHostAddress();

        } catch (Exception e) {
            System.err.println("Error getting IP: " + e.getMessage());
            return "127.0.0.1";
        }
    }

    /**
     * Validate IP:PORT format
     */
    public static boolean isValidIPPort(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String[] parts = input.split(":");
        if (parts.length != 2) {
            return false;
        }

        String ip = parts[0].trim();
        if (!isValidIP(ip)) {
            return false;
        }

        try {
            int port = Integer.parseInt(parts[1].trim());
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate IPv4 address format
     */
    private static boolean isValidIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parse host and port from IP:PORT string
     */
    public static String[] parseHostPort(String input) {
        if (!isValidIPPort(input)) {
            return null;
        }

        String[] parts = input.split(":");
        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    /**
     * Format IP:PORT for display
     */
    public static String formatIPPort(String ip, int port) {
        return ip + ":" + port;
    }
}