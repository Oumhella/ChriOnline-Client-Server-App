package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import java.sql.*;

/**
 * Gère la liste noire des adresses IP (trop de tentatives de connexion échouées).
 */
public class SecurityBlacklistDAO {

    // ─── Ajout / mise à jour ───────────────────────────────────────────────────

    /**
     * Ajoute (ou renouvelle) une IP en liste noire.
     *
     * @param ip         adresse IP à bloquer
     * @param email      email tenté (pour audit)
     * @param raison     description de la cause
     * @param dureeHeures durée de blocage en heures (ex: 0 pour minutes → utilisez {@code addIpMinutes})
     */
    public static void addIp(String ip, String email, String raison, int dureeMinutes) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Désactiver les anciennes entrées actives pour cet IP
            try (PreparedStatement del = conn.prepareStatement(
                    "UPDATE security_blacklist SET actif = FALSE WHERE ip_address = ? AND actif = TRUE")) {
                del.setString(1, ip);
                del.executeUpdate();
            }
            // Insérer la nouvelle entrée
            String ins = """
                INSERT INTO security_blacklist (ip_address, email, raison, expire_le, actif)
                VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), TRUE)
            """;
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                ps.setString(1, ip);
                ps.setString(2, email);
                ps.setString(3, raison);
                ps.setInt(4, dureeMinutes);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[SecurityBlacklistDAO] addIp: " + e.getMessage());
        }
    }

    // ─── Vérification ──────────────────────────────────────────────────────────

    /** @return true si l'IP est actuellement en liste noire active. */
    public static boolean isIpBlacklisted(String ip) {
        if (ip == null || "unknown".equals(ip)) return false;
        String sql = """
            SELECT 1 FROM security_blacklist
            WHERE ip_address = ? AND actif = TRUE AND expire_le > NOW()
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            return ps.executeQuery().next();
        } catch (Exception e) {
            System.err.println("[SecurityBlacklistDAO] isIpBlacklisted: " + e.getMessage());
            return false;
        }
    }

    /** @return secondes restantes de blocage, 0 si non bloquée. */
    public static long getRemainingSeconds(String ip) {
        String sql = """
            SELECT TIMESTAMPDIFF(SECOND, NOW(), expire_le) AS remaining
            FROM security_blacklist
            WHERE ip_address = ? AND actif = TRUE AND expire_le > NOW()
            ORDER BY expire_le DESC LIMIT 1
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Math.max(0L, rs.getLong("remaining")) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // ─── Déverrouillage ────────────────────────────────────────────────────────

    /** Débloque une IP après un login réussi. */
    public static void unlockIp(String ip) {
        if (ip == null || "unknown".equals(ip)) return;
        String sql = "UPDATE security_blacklist SET actif = FALSE WHERE ip_address = ? AND actif = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SecurityBlacklistDAO] unlockIp: " + e.getMessage());
        }
    }

    /** Débloque toutes les IPs associées à l'email de l'utilisateur. Utile après un reset de mot de passe. */
    public static void unlockIpByUserId(int userId) {
        String sql = """
            UPDATE security_blacklist sb
            JOIN utilisateur u ON u.email = sb.email
            SET sb.actif = FALSE
            WHERE u.idUtilisateur = ? AND sb.actif = TRUE
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SecurityBlacklistDAO] unlockIpByUserId: " + e.getMessage());
        }
    }

    // ─── Nettoyage automatique ─────────────────────────────────────────────────

    /** Expire les entrées dont la durée est dépassée (appelé par le planificateur). */
    public static void cleanupExpired() {
        String sql = "UPDATE security_blacklist SET actif = FALSE WHERE actif = TRUE AND expire_le < NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("[SecurityBlacklistDAO] cleanupExpired: " + e.getMessage());
        }
    }
}
