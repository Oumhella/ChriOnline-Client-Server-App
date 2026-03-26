package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAO {

    /**
     * Gets the ID of the user's default wishlist, or creates it if it doesn't exist.
     */
    private static int getOrCreateWishlist(Connection conn, int userId) throws SQLException {
        // Find existing
        String sqlFind = "SELECT id_wishlist FROM wishlist WHERE idUtilisateur = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlFind)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_wishlist");
                }
            }
        }

        // Create new
        String sqlCreate = "INSERT INTO wishlist (idUtilisateur, nom) VALUES (?, 'Ma liste')";
        try (PreparedStatement ps = conn.prepareStatement(sqlCreate, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de créer la wishlist pour l'utilisateur " + userId);
    }

    /**
     * Adds a product to the user's wishlist if it's not already there.
     */
    public static void add(int userId, int produitId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int wishlistId = getOrCreateWishlist(conn, userId);

                // Check if already exists in wishlist_produit
                String checkSql = "SELECT 1 FROM wishlist_produit WHERE id_wishlist = ? AND id_produit = ?";
                boolean exists = false;
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, wishlistId);
                    checkPs.setInt(2, produitId);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        exists = rs.next();
                    }
                }

                if (!exists) {
                    // Insert into wishlist_produit
                    String insertSql = "INSERT INTO wishlist_produit (id_wishlist, id_produit) VALUES (?, ?)";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        insertPs.setInt(1, wishlistId);
                        insertPs.setInt(2, produitId);
                        insertPs.executeUpdate();
                    }

                    // Increment count
                    String updateSql = "UPDATE wishlist SET nombre_produits = nombre_produits + 1 WHERE id_wishlist = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setInt(1, wishlistId);
                        updatePs.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Removes a product from the user's wishlist.
     */
    public static void remove(int userId, int produitId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int wishlistId = getOrCreateWishlist(conn, userId);

                // Delete from wishlist_produit
                String deleteSql = "DELETE FROM wishlist_produit WHERE id_wishlist = ? AND id_produit = ?";
                int rowsAffected = 0;
                try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                    deletePs.setInt(1, wishlistId);
                    deletePs.setInt(2, produitId);
                    rowsAffected = deletePs.executeUpdate();
                }

                // Decrement count if a row was actually deleted
                if (rowsAffected > 0) {
                    String updateSql = "UPDATE wishlist SET nombre_produits = GREATEST(0, nombre_produits - 1) WHERE id_wishlist = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setInt(1, wishlistId);
                        updatePs.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Returns a list of product IDs in the user's wishlist.
     */
    public static List<Integer> findByUser(int userId) {
        List<Integer> produitIds = new ArrayList<>();
        String sql = "SELECT wp.id_produit " +
                     "FROM wishlist_produit wp " +
                     "JOIN wishlist w ON wp.id_wishlist = w.id_wishlist " +
                     "WHERE w.idUtilisateur = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitIds.add(rs.getInt("id_produit"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO] Erreur findByUser : " + e.getMessage());
        }
        return produitIds;
    }
}
