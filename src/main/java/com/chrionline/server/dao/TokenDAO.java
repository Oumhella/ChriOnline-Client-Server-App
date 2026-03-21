package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import java.sql.*;
import java.util.UUID;

/**
 * Gère le stockage et la validation des tokens de sécurité (confirmation email, reset mdp).
 */
public class TokenDAO {

    /**
     * Génère et stocke un token pour l'utilisateur.
     * Supprime les anciens tokens du même type avant insertion.
     *
     * @param type "confirmation" (24h, OTP 8 chars) ou "reset_mdp" (1h, UUID complet)
     * @return le token généré (à envoyer par email)
     */
    public static String genererToken(int idUtilisateur, String type) throws SQLException {
        String token = "confirmation".equals(type) ? genererOTP() : UUID.randomUUID().toString();
        int heures   = "confirmation".equals(type) ? 24 : 1;

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Nettoyer les anciens tokens de ce type pour cet utilisateur
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM email_tokens WHERE idUtilisateur = ? AND type = ?")) {
                del.setInt(1, idUtilisateur);
                del.setString(2, type);
                del.executeUpdate();
            }

            // Insérer le nouveau token
            String sql = """
                INSERT INTO email_tokens (idUtilisateur, token, type, expiration)
                VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? HOUR))
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUtilisateur);
                ps.setString(2, token);
                ps.setString(3, type);
                ps.setInt(4, heures);
                ps.executeUpdate();
            }
        }
        return token;
    }

    /**
     * Valide le token et le marque comme utilisé (opération atomique).
     *
     * @return idUtilisateur si valide, -1 sinon (expiré, déjà utilisé, introuvable)
     */
    public static int consommerToken(String token, String type) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId = -1;

                String select = """
                    SELECT idUtilisateur FROM email_tokens
                    WHERE token = ? AND type = ? AND utilise = FALSE AND expiration > NOW()
                """;
                try (PreparedStatement ps = conn.prepareStatement(select)) {
                    ps.setString(1, token);
                    ps.setString(2, type);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) userId = rs.getInt("idUtilisateur");
                }

                if (userId != -1) {
                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE email_tokens SET utilise = TRUE WHERE token = ?")) {
                        upd.setString(1, token);
                        upd.executeUpdate();
                    }
                    conn.commit();
                } else {
                    conn.rollback();
                }
                return userId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * OTP lisible pour la confirmation (8 chars alphanumériques majuscules).
     */
    private static String genererOTP() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
