package org.example.triharf.services;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcReportGenerator {
    private static final String URL = "jdbc:mysql://localhost:3306/baccalaureat_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public Map<String, Integer> getMotsParCategorie() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT c.nom, COUNT(m.id) as total " +
                "FROM categories c LEFT JOIN mots m ON c.id = m.categorie_id " +
                "GROUP BY c.id, c.nom";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                stats.put(rs.getString("nom"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public int getRareteScore(String texte, Long categorieId) {
        String sql = "SELECT nb_utilisations FROM mots " +
                "WHERE texte = ? AND categorie_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, texte);
            stmt.setLong(2, categorieId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int nbUtil = rs.getInt("nb_utilisations");
                return Math.max(10 - nbUtil, 1); // Plus rare = plus de points
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 5; // Score par défaut
    }
}