package org.example.triharf.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkUtils {

    public record ConnectionInfo(String host, int port) {
    }

    private static final Pattern IP_PORT_PATTERN = Pattern.compile("^([^:]+)(:(\\d{1,5}))?$");

    public static ConnectionInfo parseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ConnectionInfo("localhost", 8888);
        }

        // Strip protocol/slash if present (simple cleanup)
        String cleanUrl = url.replace("tcp://", "").replace("http://", "").replace("https://", "");
        if (cleanUrl.endsWith("/"))
            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);

        Matcher matcher = IP_PORT_PATTERN.matcher(cleanUrl);
        if (matcher.find()) {
            String host = matcher.group(1);
            String portStr = matcher.group(3);
            int port = 8888; // Default
            if (portStr != null) {
                try {
                    port = Integer.parseInt(portStr);
                    if (port < 0 || port > 65535)
                        port = 8888;
                } catch (NumberFormatException e) {
                    // Ignore, use default
                }
            }
            return new ConnectionInfo(host, port);
        }

        // Fallback
        return new ConnectionInfo(cleanUrl, 8888);
    }

    public static boolean isValidIpPortFormat(String input) {
        if (input == null || input.isBlank())
            return false;
        // Simple regex check for something that looks like host:port or just host
        // We allows just IP too (port defaults to 8888)
        return IP_PORT_PATTERN.matcher(input).matches();
    }

    public static String getLocalIpAddress() {
        try (final java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            // Fallback to iteration if socket fails
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp())
                        continue;

                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        // Prefer IPv4 site local (private network)
                        if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            } catch (Exception ex) {
                /* ignore */ }
        }
        return "127.0.0.1";
    }
}
