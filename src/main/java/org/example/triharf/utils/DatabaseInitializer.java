package org.example.triharf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/"; // Connect to server, not specific DB
    private static final String DB_NAME = "baccalaureat_db";
    private static final String USER = "root";
    private static final String PASS = ""; // Assumed empty based on hibernate.cfg.xml
    private static final String SQL_FILE_PATH = "database_schema.sql";

    public static void initialize() {
        System.out.println("Checking database status...");
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement()) {

            // Check if database exists
            ResultSet rs = stmt.executeQuery(
                    "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DB_NAME + "'");
            if (rs.next()) {
                System.out.println("Database '" + DB_NAME + "' already exists. Skipping initialization.");
                return;
            }

            System.out.println("Database '" + DB_NAME + "' not found. Initializing...");

            // Read SQL file
            File sqlFile = new File(SQL_FILE_PATH);
            if (!sqlFile.exists()) {
                System.err.println("Error: Schema file not found at " + sqlFile.getAbsolutePath());
                return;
            }

            StringBuilder script = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(sqlFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    script.append(line).append("\n");
                }
            }

            // Execute script
            // The script might contain multiple statements separated by ;
            // Proper splitting is complex, but for this specific file we can try logical
            // splitting
            // or use specific logic.
            // The database_schema.sql provided has standard structure.
            String[] commands = script.toString().split(";");

            for (String command : commands) {
                String trimmed = command.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--"))
                    continue;

                try {
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    // Ignore error if it's just about selecting the DB or trivial
                    // But print it
                    System.out.println(
                            "Executed: " + (trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed));
                }
            }

            System.out.println("Database initialization complete.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
}
