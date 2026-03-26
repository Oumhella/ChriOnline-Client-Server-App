package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ProduitDAO {

    public static List<Produit> findAll() {
        Map<Integer, Produit> produitsMap = new LinkedHashMap<>();
        // Note: Joining with product_formats to get basic format info for the list
        String sql = "SELECT p.id_produit AS pid, p.nom, p.description, p.date_ajout, p.id_categorie, " +
                "pf.id_product_formats, pf.prix, pf.stock, pf.stock_alerte, pf.image_url " +
                "FROM produit p LEFT JOIN product_formats pf ON p.id_produit = pf.id_produit " +
                "ORDER BY pid";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idProduit = rs.getInt("pid");

                Produit p = produitsMap.computeIfAbsent(idProduit, k -> {
                    Produit prod = new Produit();
                    prod.setIdProduit(idProduit);
                    try {
                        prod.setIdCategorie(rs.getInt("id_categorie"));
                        prod.setNom(rs.getString("nom"));
                        prod.setDescription(rs.getString("description"));
                        prod.setDateAjout(rs.getTimestamp("date_ajout"));
                    } catch (SQLException e) { e.printStackTrace(); }
                    prod.setFormats(new ArrayList<>());
                    return prod;
                });

                int idFmt = rs.getInt("id_product_formats");
                if (!rs.wasNull()) {
                    ProductFormat fmt = new ProductFormat();
                    fmt.setId(idFmt);
                    fmt.setPrix(rs.getDouble("prix"));
                    fmt.setStock(rs.getInt("stock"));
                    fmt.setStockAlerte(rs.getInt("stock_alerte"));
                    fmt.setImageUrl(rs.getString("image_url"));
                    p.getFormats().add(fmt);
                    
                    // Also update summary fields for the main table view
                    if (p.getPrix() == 0) p.setPrix(fmt.getPrix().floatValue());
                    if (p.getImageUrl() == null) p.setImageUrl(fmt.getImageUrl());
                }
            }

        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findAll : " + e.getMessage());
            e.printStackTrace();
        }

        return new ArrayList<>(produitsMap.values());
    }

    public static Produit findById(int id) {
        String sql = "SELECT " +
                "p.id_produit AS pid, p.nom AS produit_nom, p.description, p.date_ajout, " +
                "p.id_categorie, " +
                "pf.id_product_formats, pf.prix, pf.stock, pf.stock_alerte, pf.image_url AS f_image_url, " +
                "lv.id_labelValues, lv.valeur, " +
                "l.id_label, l.nom AS label_nom " +
                "FROM produit p " +
                "LEFT JOIN product_formats pf ON p.id_produit = pf.id_produit " +
                "LEFT JOIN product_formats_values pfv ON pf.id_product_formats = pfv.id_product_formats " +
                "LEFT JOIN label_values lv ON pfv.id_labelValues = lv.id_labelValues " +
                "LEFT JOIN label l ON lv.id_label = l.id_label " +
                "WHERE p.id_produit = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            Produit produit = null;
            Map<Integer, ProductFormat> formatsMap = new HashMap<>();

            while (rs.next()) {
                if (produit == null) {
                    produit = new Produit();
                    produit.setIdProduit(rs.getInt("pid"));
                    produit.setIdCategorie(rs.getInt("id_categorie"));
                    produit.setNom(rs.getString("produit_nom"));
                    produit.setDescription(rs.getString("description"));
                    produit.setDateAjout(rs.getTimestamp("date_ajout"));
                    
                    double firstPrix = rs.getDouble("prix");
                    if (!rs.wasNull()) {
                        produit.setPrix((float) firstPrix);
                    }
                    produit.setFormats(new ArrayList<>());
                    System.out.println("[ProduitDAO] findById: Produit " + id + " initialisé avec imageUrl=" + produit.getImageUrl());
                }

                int formatId = rs.getInt("id_product_formats");
                if (rs.wasNull()) continue;

                ProductFormat format = formatsMap.get(formatId);
                if (format == null) {
                    format = new ProductFormat();
                    format.setId(formatId);
                    format.setPrix(rs.getDouble("prix"));
                    format.setStock(rs.getInt("stock"));
                    format.setStockAlerte(rs.getInt("stock_alerte"));
                    format.setImageUrl(rs.getString("f_image_url"));
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

    public static int insert(Produit p) {
        System.out.println("[ProduitDAO] Tentative d'insertion du produit : " + p.getNom());
        String sql = "INSERT INTO produit (id_categorie, nom, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            try {
                ps.setInt(1, p.getIdCategorie());
                ps.setString(2, p.getNom());
                ps.setString(3, p.getDescription());
                System.out.println("[ProduitDAO] Execution SQL produit... category=" + p.getIdCategorie());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int idProduit = rs.getInt(1);
                    p.setIdProduit(idProduit);
                    System.out.println("[ProduitDAO] Produit inséré ID=" + idProduit + ". Insertion des " + p.getFormats().size() + " formats...");
                    for (ProductFormat f : p.getFormats()) {
                        insertFormat(conn, idProduit, f);
                    }
                    conn.commit();
                    System.out.println("[ProduitDAO] Transaction validée (Commit).");
                    return idProduit;
                }
                System.err.println("[ProduitDAO] Échec récupération clé générée produit.");
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("[ProduitDAO] ERREUR SQL dans la transaction : " + e.getMessage());
                e.printStackTrace();
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur insert complete : " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private static void insertFormat(Connection conn, int idProduit, ProductFormat f) throws SQLException {
        System.out.println("[ProduitDAO] -> Insertion format : prix=" + f.getPrix());
        String sql = "INSERT INTO product_formats (id_produit, prix, stock, stock_alerte, image_url) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idProduit);
            ps.setDouble(2, f.getPrix() != null ? f.getPrix() : 0.0);
            ps.setInt(3, f.getStock());
            ps.setInt(4, f.getStockAlerte());
            ps.setString(5, f.getImageUrl());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int idFormat = rs.getInt(1);
                f.setId(idFormat);
                System.out.println("[ProduitDAO] -> Format inséré ID=" + idFormat + ". Valeurs variants : " + 
                        (f.getLabelValues() != null ? f.getLabelValues().size() : 0));
                
                if (f.getLabelValues() != null) {
                    for (LabelValue lv : f.getLabelValues()) {
                        insertFormatValue(conn, idFormat, lv.getId());
                    }
                }
            }
        }
    }

    private static void insertFormatValue(Connection conn, int idFormat, int idLabelValue) throws SQLException {
        System.out.println("[ProduitDAO] --> Liaison format_value: idFormat=" + idFormat + ", idValue=" + idLabelValue);
        String sql = "INSERT INTO product_formats_values (id_product_formats, id_labelValues) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idFormat);
            ps.setInt(2, idLabelValue);
            ps.executeUpdate();
        }
    }

    public static boolean update(Produit p) {
        System.out.println("[ProduitDAO] Mise à jour du produit : " + p.getNom() + " (ID=" + p.getIdProduit() + ")");
        String sql = "UPDATE produit SET id_categorie = ?, nom = ?, description = ? WHERE id_produit = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                ps.setInt(1, p.getIdCategorie());
                ps.setString(2, p.getNom());
                ps.setString(3, p.getDescription());
                ps.setInt(4, p.getIdProduit());
                int affected = ps.executeUpdate();
                System.out.println("[ProduitDAO] Update produit principal: " + affected + " lignes modifiées.");
                // --- Gestion Chirurgicale des Formats ---
                List<Integer> incomingIds = new ArrayList<>();
                for (ProductFormat f : p.getFormats()) {
                    if (f.getId() > 0) {
                        System.out.println("[ProduitDAO] -> Update format existant ID=" + f.getId());
                        updateFormat(conn, f);
                        incomingIds.add(f.getId());
                    } else {
                        System.out.println("[ProduitDAO] -> Insert nouveau format...");
                        insertFormat(conn, p.getIdProduit(), f);
                        incomingIds.add(f.getId());
                    }
                }
                
                // Supprimer les anciens formats non présents dans la nouvelle liste
                try {
                    deleteObsoleteFormats(conn, p.getIdProduit(), incomingIds);
                } catch (SQLException e) {
                    System.err.println("[ProduitDAO] WARNING: Certains formats n'ont pu être supprimés (liés à des commandes) : " + e.getMessage());
                    // On continue, c'est acceptable d'avoir des "zombies" si liés à l'historique
                }

                conn.commit();
                System.out.println("[ProduitDAO] Mise à jour terminée.");
                return true;
            } catch (SQLException e) {
                System.err.println("[ProduitDAO] Erreur transactionnelle Update : " + e.getMessage());
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur update complete : " + e.getMessage());
        }
        return false;
    }

    private static void updateFormat(Connection conn, ProductFormat f) throws SQLException {
        System.out.println("[ProduitDAO] -> Mise à jour format ID=" + f.getId());
        String sql = "UPDATE product_formats SET prix = ?, stock = ?, stock_alerte = ?, image_url = ? WHERE id_product_formats = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, f.getPrix() != null ? f.getPrix() : 0.0);
            ps.setInt(2, f.getStock());
            ps.setInt(3, f.getStockAlerte());
            ps.setString(4, f.getImageUrl());
            ps.setInt(5, f.getId());
            ps.executeUpdate();
            
            // Variantes : On vide et on re-remplit pour cet ID de format
            deleteFormatValues(conn, f.getId());
            if (f.getLabelValues() != null) {
                for (LabelValue lv : f.getLabelValues()) {
                    insertFormatValue(conn, f.getId(), lv.getId());
                }
            }
        }
    }

    private static void deleteObsoleteFormats(Connection conn, int idProduit, List<Integer> keptIds) throws SQLException {
        if (keptIds.isEmpty()) {
            deleteFormats(conn, idProduit);
            return;
        }
        String placeholders = keptIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM product_formats WHERE id_produit = ? AND id_product_formats NOT IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            for (int i = 0; i < keptIds.size(); i++) ps.setInt(i + 2, keptIds.get(i));
            ps.executeUpdate();
        }
    }

    private static void deleteFormatValues(Connection conn, int idFormat) throws SQLException {
        String sql = "DELETE FROM product_formats_values WHERE id_product_formats = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idFormat);
            ps.executeUpdate();
        }
    }

    private static void deleteFormats(Connection conn, int idProduit) throws SQLException {
        // Cascade delete in MySQL should handle values, but let's be explicit if needed
        String sql = "DELETE FROM product_formats WHERE id_produit = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            ps.executeUpdate();
        }
    }

    public static boolean delete(int idProduit) {
        String sql = "DELETE FROM produit WHERE id_produit = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur delete : " + e.getMessage());
        }
        return false;
    }

    public static List<Categorie> findAllCategories() {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT id_categorie, id_parent, nom, description FROM categorie";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id_categorie"));
                c.setIdParent(rs.getInt("id_parent"));
                c.setNom(rs.getString("nom"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findAllCategories : " + e.getMessage());
        }
        return list;
    }

    public static int insertCategorie(Categorie c) {
        String sql = "INSERT INTO categorie (id_parent, nom, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (c.getIdParent() > 0) ps.setInt(1, c.getIdParent());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, c.getNom());
            ps.setString(3, c.getDescription());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur insertCategorie : " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean updateCategorie(Categorie c) {
        String sql = "UPDATE categorie SET id_parent = ?, nom = ?, description = ? WHERE id_categorie = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (c.getIdParent() > 0) ps.setInt(1, c.getIdParent());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, c.getNom());
            ps.setString(3, c.getDescription());
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur updateCategorie : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteCategorie(int id) {
        String sql = "DELETE FROM categorie WHERE id_categorie = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur deleteCategorie : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static List<Label> findLabelsByCategorie(int idCat) {
        List<Label> list = new ArrayList<>();
        // Note: some labels might be global? Or all linked to cat? 
        // For now, let's allow finding by category.
        String sql = "SELECT id_label, nom, id_categorie FROM label WHERE id_categorie = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Label l = new Label();
                l.setId(rs.getInt("id_label"));
                l.setNom(rs.getString("nom"));
                l.setIdCategorie(rs.getInt("id_categorie"));
                list.add(l);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findLabelsByCategorie : " + e.getMessage());
        }
        return list;
    }

    public static int insertLabel(Label l) {
        String sql = "INSERT INTO label (nom, id_categorie) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, l.getNom());
            ps.setInt(2, l.getIdCategorie());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static boolean updateLabel(Label l) {
        String sql = "UPDATE label SET nom = ?, id_categorie = ? WHERE id_label = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, l.getNom());
            ps.setInt(2, l.getIdCategorie());
            ps.setInt(3, l.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean deleteLabel(int id) {
        String sql = "DELETE FROM label WHERE id_label = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static int insertLabelValue(LabelValue lv) {
        String sql = "INSERT INTO label_values (id_label, valeur) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lv.getLabel().getId());
            ps.setString(2, lv.getValeur());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static boolean deleteLabelValue(int id) {
        String sql = "DELETE FROM label_values WHERE id_labelValues = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<LabelValue> findLabelValuesByLabel(int idLabel) {
        List<LabelValue> list = new ArrayList<>();
        String sql = "SELECT lv.id_labelValues, lv.valeur, l.id_label, l.nom " +
                "FROM label_values lv JOIN label l ON lv.id_label = l.id_label " +
                "WHERE lv.id_label = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLabel);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LabelValue lv = new LabelValue();
                lv.setId(rs.getInt("id_labelValues"));
                lv.setValeur(rs.getString("valeur"));
                
                Label l = new Label();
                l.setId(rs.getInt("id_label"));
                l.setNom(rs.getString("nom"));
                lv.setLabel(l);
                
                list.add(lv);
            }
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur findLabelValuesByLabel : " + e.getMessage());
        }
        return list;
    }
}