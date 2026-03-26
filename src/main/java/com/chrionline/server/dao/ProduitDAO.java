package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.models.Label;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class ProduitDAO {

    public static List<Produit> findAll() {
        Map<Integer, Produit> produitsMap = new LinkedHashMap<>();
        String sql = "SELECT p.id_produit, p.nom, p.description, p.date_ajout, p.id_categorie, " +
                     "pf.id_product_formats, pf.prix, pf.image_url " +
                     "FROM produit p LEFT JOIN product_formats pf ON p.id_produit = pf.id_produit " +
                     "ORDER BY p.id_produit";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");

                Produit p = produitsMap.get(idProduit);
                if (p == null) {
                    p = new Produit();
                    p.setIdProduit(idProduit);
                    p.setIdCategorie(rs.getInt("id_categorie"));
                    p.setNom(rs.getString("nom"));
                    p.setDescription(rs.getString("description"));
                    p.setDateAjout(rs.getTimestamp("date_ajout"));
                    produitsMap.put(idProduit, p);
                }

                // Set imageUrl and prix from the first format that has them
                if (p.getImageUrl() == null) {
                    String imgUrl = rs.getString("image_url");
                    if (imgUrl != null) {
                        p.setImageUrl(imgUrl);
                    }
                }
                if (p.getPrix() == 0) {
                    double prix = rs.getDouble("prix");
                    if (!rs.wasNull()) {
                        p.setPrix((float) prix);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findAll : " + e.getMessage());
        }

        return new ArrayList<>(produitsMap.values());
    }

    public static Produit findById(int id) {

        String sql = "SELECT " +
                "p.id_produit, p.nom AS produit_nom, p.description, p.date_ajout, " +
                "p.id_categorie, " +
                "pf.id_product_formats, pf.prix, pf.stock, pf.stock_alerte, pf.image_url, " +
                "lv.id_labelValues, lv.valeur, " +
                "l.id_label, l.nom AS label_nom " +
                "FROM produit p " +
                "JOIN product_formats pf ON p.id_produit = pf.id_produit " +
                "JOIN product_formats_values pfv ON pf.id_product_formats = pfv.id_product_formats " +
                "JOIN label_values lv ON pfv.id_labelValues = lv.id_labelValues " +
                "JOIN label l ON lv.id_label = l.id_label " +
                "WHERE p.id_produit = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            Produit produit = null;

            // éviter doublons
            Map<Integer, ProductFormat> formatsMap = new HashMap<>();

            while (rs.next()) {

                //  Produit
                if (produit == null) {
                    produit = new Produit();
                    produit.setIdProduit(rs.getInt("id_produit"));
                    produit.setIdCategorie(rs.getInt("id_categorie"));
                    produit.setNom(rs.getString("produit_nom"));
                    produit.setDescription(rs.getString("description"));
                    produit.setDateAjout(rs.getTimestamp("date_ajout"));

                    // Set top-level imageUrl and prix from first row
                    produit.setImageUrl(rs.getString("image_url"));
                    double firstPrix = rs.getDouble("prix");
                    if (!rs.wasNull()) {
                        produit.setPrix((float) firstPrix);
                    }

                    produit.setFormats(new ArrayList<>());
                }

                //  Format
                int formatId = rs.getInt("id_product_formats");

                ProductFormat format = formatsMap.get(formatId);

                if (format == null) {
                    format = new ProductFormat();
                    format.setId(formatId);
                    format.setPrix(rs.getDouble("prix"));
                    format.setStock(rs.getInt("stock"));
                    format.setStockAlerte(rs.getInt("stock_alerte"));
                    format.setImageUrl(rs.getString("image_url"));

                    format.setLabelValues(new ArrayList<>());

                    formatsMap.put(formatId, format);
                    produit.getFormats().add(format);
                }

                //  LabelValue
                LabelValue lv = new LabelValue();
                lv.setId(rs.getInt("id_labelValues"));
                lv.setValeur(rs.getString("valeur"));

                //  Label
                Label label = new Label();
                label.setId(rs.getInt("id_label"));
                label.setNom(rs.getString("label_nom"));

                lv.setLabel(label);

                format.getLabelValues().add(lv);
            }

            return produit;

        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findById : " + e.getMessage());
        }

        return null;
    }
}
