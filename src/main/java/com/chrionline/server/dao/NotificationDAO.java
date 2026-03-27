package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Gère le stockage et la récupération des notifications dans la base de données.
 */
public class NotificationDAO {

    /**
     * Sauvegarde une notification dans la table `notification`.
     * @param userId l'ID de l'utilisateur destinataire
     * @param message le contenu du message
     * @param type le type de notification (ex: commande, stock, alerte)
     * @return true si la sauvegarde a réussi
     */
    public static boolean save(int userId, String message, String type) {
        String sql = "INSERT INTO notification (idUtilisateur, message, type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, type);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Erreur save : " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère l'historique des notifications pour un utilisateur donné.
     * @param userId l'ID de l'utilisateur
     * @return une liste de maps contenant les infos des notifications
     */
    public static List<Map<String, Object>> findByUserId(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT idNotification, message, type, dateEnvoi FROM notification WHERE idUtilisateur = ? ORDER BY dateEnvoi DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> n = new HashMap<>();
                n.put("id", rs.getInt("idNotification"));
                n.put("message", rs.getString("message"));
                n.put("type", rs.getString("type"));
                n.put("date", rs.getTimestamp("dateEnvoi"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Erreur findByUserId : " + e.getMessage());
        }
        return list;
    }
}
