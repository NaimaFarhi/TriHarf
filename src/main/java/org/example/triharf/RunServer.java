package org.example.triharf;

import org.example.triharf.network.GameServer;

public class RunServer {
    public static void main(String[] args) throws Exception {
        GameServer server = new GameServer();
        System.out.println("Starting server...");
        server.start();
    }
}