package org.example.triharf.utils;

import java.io.*;
import java.util.regex.*;

public class NgrokManager {
    private Process ngrokProcess;
    private String publicUrl;
    private int publicPort;

    public void start(int localPort) throws IOException, InterruptedException {
        // Get ngrok.exe from resources
        String ngrokPath = getNgrokPath();

        // Start ngrok
        ProcessBuilder pb = new ProcessBuilder(ngrokPath, "tcp", String.valueOf(localPort));
        ngrokProcess = pb.start();

        // Wait for ngrok to start
        Thread.sleep(2000);

        // Get public URL from ngrok API
        fetchPublicUrl();
    }

    private void fetchPublicUrl() throws IOException {
        // Ngrok exposes local API at http://localhost:4040/api/tunnels
        java.net.URL url = new java.net.URL("http://localhost:4040/api/tunnels");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        // Parse JSON to extract public URL
        parseNgrokResponse(response.toString());
    }

    private void parseNgrokResponse(String json) {
        // Extract: "public_url":"tcp://0.tcp.ngrok.io:12345"
        Pattern pattern = Pattern.compile("\"public_url\":\"tcp://([^:]+):(\\d+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String host = matcher.group(1);
            int port = Integer.parseInt(matcher.group(2));
            this.publicUrl = host + ":" + port;
            this.publicPort = port;
        }
    }

    private String getNgrokPath() throws IOException {
        // Extract ngrok.exe from resources to temp folder
        InputStream in = getClass().getResourceAsStream("/ngrok/ngrok.exe");
        File tempFile = File.createTempFile("ngrok", ".exe");
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        return tempFile.getAbsolutePath();
    }

    public void stop() {
        if (ngrokProcess != null) {
            ngrokProcess.destroy();
        }
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}