package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.server.session.Session;

import java.sql.*;

/**
 * Persiste les sessions TCP en base pour :
 *   - survie au redémarrage du serveur
 *   - audit des connexions actives
 */
public class SessionDAO {

    // ─── Sauvegarde ────────────────────────────────────────────────────────────

    public static void save(Session s) {
        String sql = """
            INSERT INTO sessions (session_id, user_id, ip_address, created_at, last_activity, expires_at, actif)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            ON DUPLICATE KEY UPDATE last_activity = VALUES(last_activity), actif = TRUE
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSessionId());
            ps.setInt(2, s.getUserId());
            ps.setString(3, s.getClientIp());
            ps.setTimestamp(4, new Timestamp(s.getCreatedAt()));
            ps.setTimestamp(5, new Timestamp(s.getLastActivity()));
            // expires_at = created_at + 24h
            ps.setTimestamp(6, new Timestamp(s.getCreatedAt() + 24L * 60 * 60 * 1000));
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SessionDAO] save: " + e.getMessage());
        }
    }

    // ─── Rafraîchissement ──────────────────────────────────────────────────────

    public static void refresh(String sessionId, long lastActivity) {
        String sql = "UPDATE sessions SET last_activity = ? WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(lastActivity));
            ps.setString(2, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SessionDAO] refresh: " + e.getMessage());
        }
    }

    // ─── Invalidation ──────────────────────────────────────────────────────────

    public static void invalidate(String sessionId) {
        String sql = "UPDATE sessions SET actif = FALSE WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SessionDAO] invalidate: " + e.getMessage());
        }
    }

    public static void invalidateAllByUserId(int userId) {
        String sql = "UPDATE sessions SET actif = FALSE WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SessionDAO] invalidateAllByUserId: " + e.getMessage());
        }
    }

    // ─── Comptage / recherche ──────────────────────────────────────────────────

    public static int countActiveByUserId(int userId) {
        String sql = """
            SELECT COUNT(*) FROM sessions
            WHERE user_id = ? AND actif = TRUE
              AND expires_at > NOW()
              AND last_activity > DATE_SUB(NOW(), INTERVAL 15 MINUTE)
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            System.err.println("[SessionDAO] countActiveByUserId: " + e.getMessage());
            return 0;
        }
    }

    public static String getOldestSessionIdByUserId(int userId) {
        String sql = """
            SELECT session_id FROM sessions
            WHERE user_id = ? AND actif = TRUE
            ORDER BY created_at ASC LIMIT 1
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("session_id") : null;
        } catch (Exception e) {
            System.err.println("[SessionDAO] getOldestSessionIdByUserId: " + e.getMessage());
            return null;
        }
    }

    // ─── Nettoyage ─────────────────────────────────────────────────────────────

    /** Appelé par le planificateur toutes les 5 minutes. */
    public static void cleanupExpired() {
        String sql = """
            UPDATE sessions SET actif = FALSE
            WHERE actif = TRUE AND (
                expires_at < NOW()
                OR last_activity < DATE_SUB(NOW(), INTERVAL 15 MINUTE)
            )
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            if (rows > 0) System.out.println("[SessionDAO] Cleanup BDD : " + rows + " session(s) expirée(s).");
        } catch (Exception e) {
            System.err.println("[SessionDAO] cleanupExpired: " + e.getMessage());
        }
    }
}
