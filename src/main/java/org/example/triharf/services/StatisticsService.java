package org.example.triharf.services;

import org.example.triharf.models.ResultatPartie;
import org.example.triharf.models.Partie;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * StatisticsService.java
 * Gère toutes les requêtes statistiques depuis la base de données
 */
public class StatisticsService {

    private String connectionString = "jdbc:mysql://localhost:3306/baccalaureat_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private String username = "root";
    private String password = "";

    /**
     * Statistiques globales d'un joueur
     */
    public Map<String, Object> getGlobalStats(String joueur) {
        Map<String, Object> stats = new HashMap<>();

        String query = """
            SELECT 
                COUNT(*) as totalParties,
                AVG(scoreTotal) as scoreMoyen,
                MAX(scoreTotal) as meilleurScore,
                AVG(duree) as dureeMoyenne
            FROM parties
            WHERE joueur = ?
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("totalParties", rs.getInt("totalParties"));
                stats.put("scoreMoyen", rs.getDouble("scoreMoyen"));
                stats.put("meilleurScore", rs.getInt("meilleurScore"));
                stats.put("dureeMoyenne", rs.getDouble("dureeMoyenne"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getGlobalStats: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Statistiques par mode de jeu
     */
    public Map<String, Map<String, Object>> getStatsByMode(String joueur) {
        Map<String, Map<String, Object>> statsByMode = new HashMap<>();

        String query = """
            SELECT 
                mode,
                COUNT(*) as parties,
                AVG(scoreTotal) as scoreMoyen,
                MAX(scoreTotal) as meilleurScore
            FROM parties
            WHERE joueur = ?
            GROUP BY mode
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String mode = rs.getString("mode");
                Map<String, Object> modeStats = new HashMap<>();
                modeStats.put("parties", rs.getInt("parties"));
                modeStats.put("scoreMoyen", rs.getDouble("scoreMoyen"));
                modeStats.put("meilleurScore", rs.getInt("meilleurScore"));

                statsByMode.put(mode, modeStats);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getStatsByMode: " + e.getMessage());
        }

        return statsByMode;
    }

    /**
     * Statistiques par catégorie
     */
    public Map<String, Map<String, Object>> getStatsByCategorie(String joueur) {
        Map<String, Map<String, Object>> statsByCategorie = new HashMap<>();

        String query = """
            SELECT 
                c.nom,
                COUNT(rp.id) as tentatives,
                SUM(CASE WHEN rp.valide = 1 THEN 1 ELSE 0 END) as reussies,
                AVG(rp.points) as pointsMoyens
            FROM categories c
            LEFT JOIN resultats_partie rp ON c.id = rp.categorie_id
            LEFT JOIN parties p ON rp.partie_id = p.id
            WHERE p.joueur = ? OR p.joueur IS NULL
            GROUP BY c.id, c.nom
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String categorie = rs.getString("nom");
                Map<String, Object> catStats = new HashMap<>();
                catStats.put("tentatives", rs.getInt("tentatives"));
                catStats.put("reussies", rs.getInt("reussies"));
                catStats.put("pointsMoyens", rs.getDouble("pointsMoyens"));

                // Calculer le taux de réussite
                int tentatives = rs.getInt("tentatives");
                int reussies = rs.getInt("reussies");
                double tauxReussite = tentatives > 0 ? (reussies * 100.0 / tentatives) : 0;
                catStats.put("tauxReussite", tauxReussite);

                statsByCategorie.put(categorie, catStats);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getStatsByCategorie: " + e.getMessage());
        }

        return statsByCategorie;
    }

    /**
     * Historique des 10 dernières parties
     */
    public List<Map<String, Object>> getRecentParties(String joueur, int limit) {
        List<Map<String, Object>> parties = new ArrayList<>();

        String query = """
            SELECT 
                id, dateJeu, mode, scoreTotal, duree
            FROM parties
            WHERE joueur = ?
            ORDER BY dateJeu DESC
            LIMIT ?
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> partie = new HashMap<>();
                partie.put("id", rs.getInt("id"));
                partie.put("date", rs.getDate("dateJeu"));
                partie.put("mode", rs.getString("mode"));
                partie.put("score", rs.getInt("scoreTotal"));
                partie.put("duree", rs.getInt("duree"));

                parties.add(partie);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getRecentParties: " + e.getMessage());
        }

        return parties;
    }

    /**
     * Tendance du score (derniers 7 jours)
     */
    public Map<LocalDate, Double> getScoreTrend(String joueur) {
        Map<LocalDate, Double> trend = new LinkedHashMap<>();

        String query = """
            SELECT 
                DATE(dateJeu) as date,
                AVG(scoreTotal) as scoreMoyen
            FROM parties
            WHERE joueur = ? AND dateJeu >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY DATE(dateJeu)
            ORDER BY date
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("date").toLocalDate();
                double scoreMoyen = rs.getDouble("scoreMoyen");
                trend.put(date, scoreMoyen);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getScoreTrend: " + e.getMessage());
        }

        return trend;
    }

    /**
     * Classement global (Top 10)
     */
    public List<Map<String, Object>> getGlobalRanking() {
        List<Map<String, Object>> ranking = new ArrayList<>();

        String query = """
            SELECT 
                joueur,
                COUNT(*) as parties,
                MAX(scoreTotal) as meilleurScore,
                AVG(scoreTotal) as scoreMoyen
            FROM parties
            GROUP BY joueur
            ORDER BY meilleurScore DESC
            LIMIT 10
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(query);
            int rank = 1;

            while (rs.next()) {
                Map<String, Object> player = new HashMap<>();
                player.put("rank", rank++);
                player.put("joueur", rs.getString("joueur"));
                player.put("parties", rs.getInt("parties"));
                player.put("meilleurScore", rs.getInt("meilleurScore"));
                player.put("scoreMoyen", rs.getDouble("scoreMoyen"));

                ranking.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getGlobalRanking: " + e.getMessage());
        }

        return ranking;
    }

    /**
     * Taux de réussite global
     */
    public double getGlobalSuccessRate(String joueur) {
        String query = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN valide = 1 THEN 1 ELSE 0 END) as reussies
            FROM resultats_partie rp
            JOIN parties p ON rp.partie_id = p.id
            WHERE p.joueur = ?
        """;

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, joueur);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int reussies = rs.getInt("reussies");
                return total > 0 ? (reussies * 100.0 / total) : 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur StatisticsService.getGlobalSuccessRate: " + e.getMessage());
        }

        return 0;
    }
}