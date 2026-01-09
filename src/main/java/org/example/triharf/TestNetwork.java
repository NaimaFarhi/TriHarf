package org.example.triharf;

import org.example.triharf.enums.Langue;
import org.example.triharf.network.*;

public class TestNetwork {
    public static void main(String[] args) throws Exception {
        // Start server in separate thread
        Thread serverThread = new Thread(() -> {
            try {
                GameServer server = new GameServer();
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        Thread.sleep(1000); // Wait for server to start

        // Create 2 clients
        GameClient client1 = new GameClient();
        GameClient client2 = new GameClient();

        // Set message handlers
        client1.setMessageHandler(msg ->
                System.out.println("Client1 received: " + msg.getType())
        );

        client2.setMessageHandler(msg ->
                System.out.println("Client2 received: " + msg.getType())
        );

        // Connect clients
        client1.connect();
        client2.connect();
        System.out.println("✓ Both clients connected");

        // Client1 joins room
        NetworkMessage joinMsg = new NetworkMessage(
                NetworkMessage.Type.JOIN_ROOM,
                "client1",
                "room123"
        );
        client1.sendMessage(joinMsg);

        Thread.sleep(500);

        // Client2 joins same room
        joinMsg = new NetworkMessage(
                NetworkMessage.Type.JOIN_ROOM,
                "client2",
                "room123"
        );
        client2.sendMessage(joinMsg);

        Thread.sleep(2000);

        System.out.println("✓ Test complete");
        client1.disconnect();
        client2.disconnect();
    }
}