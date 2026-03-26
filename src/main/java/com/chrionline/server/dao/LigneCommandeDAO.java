package com.chrionline.server.dao;

import com.chrionline.shared.models.LigneCommande;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LigneCommandeDAO {

    private Connection connection;

    public LigneCommandeDAO(Connection connection) {
        this.connection = connection;
    }

    // ───── Lister les lignes d'une commande ─────
    public List<LigneCommande> findByCommande(String idCommande) throws SQLException {
        List<LigneCommande> lignes = new ArrayList<>();

        // Utilise les vraies tables : ligne_commande, product_formats, produit
        String sql = "SELECT lc.id_commande, lc.id_product_formats, lc.quantite, lc.prix_unitaire, p.nom AS nom_produit " +
                "FROM ligne_commande lc " +
                "JOIN product_formats pf ON lc.id_product_formats = pf.id_product_formats " +
                "JOIN produit p ON pf.id_produit = p.id_produit " +
                "WHERE lc.id_commande = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, idCommande);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            LigneCommande ligne = new LigneCommande();
            // Dans ce nouveau schéma, il n'y a pas d'id_ligne => on utilise l'id du format comme idProduit
            ligne.setIdLigne(rs.getInt("id_product_formats"));
            ligne.setIdCommande(rs.getString("id_commande"));
            ligne.setIdProduit(rs.getInt("id_product_formats"));
            ligne.setNomProduit(rs.getString("nom_produit"));
            ligne.setQuantite(rs.getInt("quantite"));
            ligne.setPrixUnitaire(rs.getDouble("prix_unitaire"));
            lignes.add(ligne);
        }
        return lignes;
    }

    // ───── Restaurer le stock lors d'une annulation ─────
    public void restaurerStock(String idCommande) throws SQLException {
        String sql = "SELECT id_product_formats, quantite " +
                "FROM ligne_commande " +
                "WHERE id_commande = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, idCommande);
        ResultSet rs = ps.executeQuery();

        // Le stock est géré dans product_formats d'après la nouvelle BDD
        String updateStock = "UPDATE product_formats SET stock = stock + ? " +
                "WHERE id_product_formats = ?";

        while (rs.next()) {
            PreparedStatement ps2 = connection.prepareStatement(updateStock);
            ps2.setInt(1, rs.getInt("quantite"));
            ps2.setInt(2, rs.getInt("id_product_formats"));
            ps2.executeUpdate();
        }
    }
}