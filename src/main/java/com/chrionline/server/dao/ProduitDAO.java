package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.models.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitDAO {

    public static List<Produit> findAll() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Produit p = new Produit();
                p.setIdProduit(rs.getInt("id_produit"));
                p.setIdCategorie(rs.getInt("id_categorie"));
                p.setNom(rs.getString("nom"));
                p.setDescription(rs.getString("description"));
                p.setPrix(rs.getBigDecimal("prix"));
                p.setStock(rs.getInt("stock"));
                p.setStockAlerte(rs.getInt("stock_alerte"));
                p.setImageUrl(rs.getString("image_url"));
                p.setDateAjout(rs.getTimestamp("date_ajout"));
                produits.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findAll : " + e.getMessage());
        }
        return produits;
    }
}
