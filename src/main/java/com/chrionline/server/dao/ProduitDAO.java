package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.models.Label;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;

import java.sql.*;
import java.util.*;

public class ProduitDAO {

    /**
     * Retourne tous les produits avec leurs formats (prix, stock, image).
     * Chaque produit a au moins un ProductFormat pour que l'ajout au panier fonctionne.
     */
    public static List<Produit> findAll() {
        List<Produit> produits = new ArrayList<>();

        String sql = """
            SELECT
                p.id_produit,
                p.id_categorie,
                p.nom             AS produit_nom,
                p.description,
                p.date_ajout,
                pf.id_product_formats,
                pf.prix,
                pf.stock,
                pf.stock_alerte,
                pf.image_url,
                lv.id_labelValues,
                lv.valeur,
                l.id_label,
                l.nom             AS label_nom
            FROM produit p
            JOIN product_formats pf
                ON pf.id_produit = p.id_produit
            LEFT JOIN product_formats_values pfv
                ON pfv.id_product_formats = pf.id_product_formats
            LEFT JOIN label_values lv
                ON lv.id_labelValues = pfv.id_labelValues
            LEFT JOIN label l
                ON l.id_label = lv.id_label
            ORDER BY p.id_produit, pf.id_product_formats
        """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Maps pour éviter les doublons
            Map<Integer, Produit>       produitsMap = new LinkedHashMap<>();
            Map<Integer, ProductFormat> formatsMap  = new HashMap<>();

            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");

                // ── Produit ──────────────────────────────────────
                Produit p = produitsMap.get(idProduit);
                if (p == null) {
                    p = new Produit();
                    p.setIdProduit(idProduit);
                    p.setIdCategorie(rs.getInt("id_categorie"));
                    p.setNom(rs.getString("produit_nom"));
                    p.setDescription(rs.getString("description"));
                    p.setDateAjout(rs.getTimestamp("date_ajout"));
                    p.setFormats(new ArrayList<>());
                    produitsMap.put(idProduit, p);
                }

                // ── Format ───────────────────────────────────────
                int idFormat = rs.getInt("id_product_formats");
                if (rs.wasNull()) continue; // pas de format du tout

                ProductFormat fmt = formatsMap.get(idFormat);
                if (fmt == null) {
                    fmt = new ProductFormat();
                    fmt.setId(idFormat);
                    fmt.setPrix(rs.getDouble("prix"));
                    fmt.setStock(rs.getInt("stock"));
                    fmt.setStockAlerte(rs.getInt("stock_alerte"));
                    fmt.setImageUrl(rs.getString("image_url"));
                    fmt.setLabelValues(new ArrayList<>());
                    formatsMap.put(idFormat, fmt);
                    p.getFormats().add(fmt);

                    // Le premier format définit le prix et l'image du produit
                    p.setPrix(fmt.getPrix().floatValue());
                    if (p.getImageUrl() == null && fmt.getImageUrl() != null) {
                        p.setImageUrl(fmt.getImageUrl());
                    }
                }

                // ── LabelValue (optionnel) ───────────────────────
                int idLV = rs.getInt("id_labelValues");
                if (!rs.wasNull()) {
                    LabelValue lv = new LabelValue();
                    lv.setId(idLV);
                    lv.setValeur(rs.getString("valeur"));

                    Label label = new Label();
                    label.setId(rs.getInt("id_label"));
                    label.setNom(rs.getString("label_nom"));
                    lv.setLabel(label);

                    fmt.getLabelValues().add(lv);
                }
            }

            produits.addAll(produitsMap.values());
            System.out.println("[ProduitDAO] findAll → " + produits.size() + " produit(s) chargé(s)");

        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findAll : " + e.getMessage());
        }

        return produits;
    }

    /**
     * Retourne un produit complet par son id (avec tous ses formats et labels).
     */
    public static Produit findById(int id) {
        String sql = """
            SELECT
                p.id_produit, p.nom AS produit_nom, p.description, p.date_ajout,
                p.id_categorie,
                pf.id_product_formats, pf.prix, pf.stock, pf.stock_alerte, pf.image_url,
                lv.id_labelValues, lv.valeur,
                l.id_label, l.nom AS label_nom
            FROM produit p
            JOIN product_formats pf ON p.id_produit = pf.id_produit
            LEFT JOIN product_formats_values pfv ON pf.id_product_formats = pfv.id_product_formats
            LEFT JOIN label_values lv ON pfv.id_labelValues = lv.id_labelValues
            LEFT JOIN label l ON lv.id_label = l.id_label
            WHERE p.id_produit = ?
        """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            Produit produit = null;
            Map<Integer, ProductFormat> formatsMap = new HashMap<>();

            while (rs.next()) {
                if (produit == null) {
                    produit = new Produit();
                    produit.setIdProduit(rs.getInt("id_produit"));
                    produit.setIdCategorie(rs.getInt("id_categorie"));
                    produit.setNom(rs.getString("produit_nom"));
                    produit.setDescription(rs.getString("description"));
                    produit.setDateAjout(rs.getTimestamp("date_ajout"));
                    produit.setFormats(new ArrayList<>());
                }

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

                int idLV = rs.getInt("id_labelValues");
                if (!rs.wasNull()) {
                    LabelValue lv = new LabelValue();
                    lv.setId(idLV);
                    lv.setValeur(rs.getString("valeur"));
                    Label label = new Label();
                    label.setId(rs.getInt("id_label"));
                    label.setNom(rs.getString("label_nom"));
                    lv.setLabel(label);
                    format.getLabelValues().add(lv);
                }
            }
            return produit;

        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findById : " + e.getMessage());
        }
        return null;
    }
}