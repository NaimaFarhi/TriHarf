package org.example.triharf.utils;

import java.io.*;
import java.util.regex.*;

public class NgrokManager {
    private Process ngrokProcess;
    private String publicUrl;
    private int publicPort;

    private StringBuilder logBuffer = new StringBuilder();

    public void start(int localPort) throws IOException, InterruptedException {
        String ngrokPath = getNgrokPath();

        ProcessBuilder pb = new ProcessBuilder(ngrokPath, "tcp", String.valueOf(localPort), "--log=stdout");
        pb.redirectErrorStream(true);
        ngrokProcess = pb.start();
        
        // Clear previous logs
        logBuffer.setLength(0);

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ngrokProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[NGROK] " + line);
                    synchronized (logBuffer) {
                        logBuffer.append(line).append("\n");
                    }
                }
            } catch (IOException e) { /* ignore */ }
        }).start();

        // Retry for up to 10 seconds
        int attempts = 0;
        while (attempts < 20) {
            if (!ngrokProcess.isAlive()) {
                String logs;
                synchronized (logBuffer) {
                     logs = logBuffer.toString();
                }
                if (logs.contains("ERR_NGROK_4018") || logs.contains("requires a verified account")) {
                    throw new IOException("NGROK_AUTH_REQUIRED");
                }
                throw new IOException("Ngrok process exited unexpectedly. Logs:\n" + logs);
            }
            
            try {
                Thread.sleep(500);
                fetchPublicUrl();
                if (publicUrl != null) return; 
            } catch (IOException e) {
                // API not ready yet
            }
            attempts++;
        }
        
        throw new IOException("Ngrok API not responding after 10 seconds.");
    }
    
    public void setAuthToken(String token) throws IOException, InterruptedException {
        String ngrokPath = getNgrokPath();
        ProcessBuilder pb = new ProcessBuilder(ngrokPath, "config", "add-authtoken", token);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to set authtoken. Exit code: " + exitCode);
        }
    }

    private void fetchPublicUrl() throws IOException {
        // Ngrok exposes local API at http://localhost:4040/api/tunnels
        java.net.URL url = new java.net.URL("http://localhost:4040/api/tunnels");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(500); // Fast fail
        conn.setReadTimeout(500);

        if (conn.getResponseCode() != 200) {
           throw new IOException("API responded with " + conn.getResponseCode());
        }

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