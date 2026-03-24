package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO pour les statistiques du dashboard admin.
 * Agrège les données depuis toutes les tables principales.
 */
public class DashboardDAO {

    /**
     * Retourne les KPIs principaux du dashboard.
     * @return Map contenant : totalClients, totalCommandes, chiffreAffaires,
     *         commandesEnAttente, stockAlerte, totalProduits
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

            // ── Total clients actifs ──────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM client WHERE statut_compte = 'actif'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalClients", rs.getInt(1));
            }

            // ── Total commandes ───────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM commande")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalCommandes", rs.getInt(1));
            }

            // ── Chiffre d'affaires (paiements validés) ────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(montant), 0) FROM paiement WHERE statut_paiement = 'valide'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("chiffreAffaires", rs.getBigDecimal(1));
            }

            // ── Commandes en attente ──────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM commande WHERE status = 'en_attente'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("commandesEnAttente", rs.getInt(1));
            }

            // ── Produits en alerte de stock ───────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM product_formats WHERE stock <= stock_alerte")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("stockAlerte", rs.getInt(1));
            }

            // ── Total produits ────────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM produit")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats.put("totalProduits", rs.getInt(1));
            }

            // ── Commandes récentes (5 dernières) ──────────────
            StringBuilder sb = new StringBuilder();
            String sqlRecentes = """
                SELECT c.reference, c.status, c.date_commande,
                       u.nom, u.prenom,
                       COALESCE(p.montant, 0) as montant
                FROM commande c
                JOIN utilisateur u ON u.idUtilisateur = c.idUtilisateur
                LEFT JOIN paiement p ON p.id_commande = c.id_commande
                ORDER BY c.date_commande DESC
                LIMIT 5
            """;
            java.util.List<Map<String, Object>> recentes = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlRecentes)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("reference",    rs.getString("reference"));
                    row.put("status",       rs.getString("status"));
                    row.put("dateCommande", rs.getTimestamp("date_commande"));
                    row.put("client",       rs.getString("prenom") + " " + rs.getString("nom"));
                    row.put("montant",      rs.getBigDecimal("montant"));
                    recentes.add(row);
                }
            }
            stats.put("commandesRecentes", recentes);

            // ── Inscriptions par mois (6 derniers mois) ───────
            String sqlInscrits = """
                SELECT DATE_FORMAT(date_inscription, '%Y-%m') AS mois,
                       COUNT(*) AS nb
                FROM client
                WHERE date_inscription >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                GROUP BY mois
                ORDER BY mois ASC
            """;
            java.util.List<Map<String, Object>> inscriptions = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlInscrits)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("mois", rs.getString("mois"));
                    row.put("nb",   rs.getInt("nb"));
                    inscriptions.add(row);
                }
            }
            stats.put("inscriptionsParMois", inscriptions);

        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Erreur getStats : " + e.getMessage());
        }
        return stats;
    }

    /**
     * Retourne la répartition des commandes par statut.
     */
    public static Map<String, Integer> getCommandesParStatut() {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as nb FROM commande GROUP BY status";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Erreur getCommandesParStatut : " + e.getMessage());
        }
        return result;
    }
}