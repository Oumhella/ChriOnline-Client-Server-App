package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserDAO {

    /**
     * Inscrit un nouvel utilisateur :
     * 1. INSERT dans `utilisateur`
     * 2. INSERT dans `client` (FK idUtilisateur)
     * Retourne true si succès.
     */
    public boolean inscrire(User user) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        // Vérifier si l'email existe déjà
        if (emailExiste(user.getEmail(), conn)) {
            return false; // email déjà pris
        }

        conn.setAutoCommit(false); // Transaction atomique
        try {
            // ── Étape 1 : INSERT utilisateur ──────────────────
            String sqlUser = "INSERT INTO utilisateur (nom, prenom, email, password) VALUES (?, ?, ?, ?)";
            PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, user.getNom());
            psUser.setString(2, user.getPrenom());
            psUser.setString(3, user.getEmail());
            psUser.setString(4, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            psUser.executeUpdate();

            // Récupérer l'idUtilisateur généré
            ResultSet keys = psUser.getGeneratedKeys();
            int idUtilisateur = -1;
            if (keys.next()) idUtilisateur = keys.getInt(1);

            // ── Étape 2 : INSERT client ───────────────────────
            String sqlClient = "INSERT INTO client (idUtilisateur) VALUES (?)";
            PreparedStatement psClient = conn.prepareStatement(sqlClient);
            psClient.setInt(1, idUtilisateur);
            psClient.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Vérifie si un email est déjà utilisé. */
    private boolean emailExiste(String email, Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM utilisateur WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }
}