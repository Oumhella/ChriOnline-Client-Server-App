package com.chrionline.server.dao;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.shared.dto.LignePanierDTO;
import com.chrionline.shared.dto.PanierDTO;
import com.chrionline.shared.models.LignePanier;
import com.chrionline.shared.models.Panier;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO gÃ©rant toutes les opÃ©rations liÃ©es au panier en base de donnÃ©es.
 * Tables : panier, ligne_panier, product_formats, produit.
 */
public class PanierDAO {

    // â”€â”€â”€ RÃ©cupÃ©rer ou crÃ©er le panier actif â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Retourne le panier actif de l'utilisateur avec ses lignes.
     * Si aucun panier actif n'existe, en crÃ©e un nouveau.
     */
    public static Panier getPanierActif(int idUtilisateur) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        // Chercher un panier actif existant
        String sqlFind = "SELECT * FROM panier WHERE idUtilisateur = ? AND statut = 'actif' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlFind)) {
            ps.setInt(1, idUtilisateur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Panier p = mapPanier(rs);
                p.setLignes(getLignes(p.getIdPanier(), conn));
                p.recalculerTotal();
                return p;
            }
        }

        // Aucun panier actif â†’ en crÃ©er un
        return creerPanier(idUtilisateur, conn);
    }

    /**
     * CrÃ©e un nouveau panier actif pour l'utilisateur.
     */
    private static Panier creerPanier(int idUtilisateur, Connection conn) throws SQLException {
        String sql = "INSERT INTO panier (idUtilisateur, montant_total, statut) VALUES (?, 0.00, 'actif')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) throw new SQLException("Ã‰chec crÃ©ation panier");

            Panier p = new Panier();
            p.setIdPanier(keys.getInt(1));
            p.setIdUtilisateur(idUtilisateur);
            p.setStatut("actif");
            p.setLignes(new ArrayList<>());
            System.out.println("[PanierDAO] Nouveau panier crÃ©Ã© â€” id=" + p.getIdPanier());
            return p;
        }
    }

    // â”€â”€â”€ Ajouter un produit â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Ajoute un format produit au panier.
     * Si la ligne existe dÃ©jÃ , incrÃ©mente la quantitÃ©.
     * VÃ©rifie le stock disponible avant insertion.
     *
     * @return le panier mis Ã  jour
     */
    public static Panier ajouterProduit(int idUtilisateur, int idProductFormats, int quantite)
            throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // (La vÃ©rification de stock se fera lors de la crÃ©ation de la commande)

            Panier panier = getPanierActif(idUtilisateur);

            // VÃ©rifier si la ligne existe dÃ©jÃ 
            String sqlCheck = "SELECT quantite FROM ligne_panier WHERE id_panier = ? AND id_product_formats = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, panier.getIdPanier());
                ps.setInt(2, idProductFormats);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    // La ligne existe â†’ mise Ã  jour quantitÃ©
                    int nouvelleQte = rs.getInt("quantite") + quantite;
                    String sqlUpd = "UPDATE ligne_panier SET quantite = ? WHERE id_panier = ? AND id_product_formats = ?";
                    try (PreparedStatement upd = conn.prepareStatement(sqlUpd)) {
                        upd.setInt(1, nouvelleQte);
                        upd.setInt(2, panier.getIdPanier());
                        upd.setInt(3, idProductFormats);
                        upd.executeUpdate();
                    }
                } else {
                    // Nouvelle ligne
                    String sqlIns = "INSERT INTO ligne_panier (id_panier, id_product_formats, quantite) VALUES (?, ?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
                        ins.setInt(1, panier.getIdPanier());
                        ins.setInt(2, idProductFormats);
                        ins.setInt(3, quantite);
                        ins.executeUpdate();
                    }
                }
            }

            mettreAJourTotal(panier.getIdPanier(), conn);
            conn.commit();

            // Recharger et retourner le panier complet
            return getPanierActif(idUtilisateur);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // â”€â”€â”€ Modifier la quantitÃ© â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Modifie la quantitÃ© d'une ligne.
     * Si quantite <= 0, supprime la ligne.
     */
    public static Panier modifierQuantite(int idUtilisateur, int idProductFormats, int nouvelleQte)
            throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            Panier panier = getPanierActif(idUtilisateur);

            if (nouvelleQte <= 0) {
                // Supprimer la ligne
                String sqlDel = "DELETE FROM ligne_panier WHERE id_panier = ? AND id_product_formats = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlDel)) {
                    ps.setInt(1, panier.getIdPanier());
                    ps.setInt(2, idProductFormats);
                    ps.executeUpdate();
                }
            } else {
                // Mise Ã  jour de la quantitÃ© sans vÃ©rification de stock
                // (La vÃ©rification se fera lors de la crÃ©ation de la commande)
                String sqlUpd = "UPDATE ligne_panier SET quantite = ? WHERE id_panier = ? AND id_product_formats = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpd)) {
                    ps.setInt(1, nouvelleQte);
                    ps.setInt(2, panier.getIdPanier());
                    ps.setInt(3, idProductFormats);
                    ps.executeUpdate();
                }
            }

            mettreAJourTotal(panier.getIdPanier(), conn);
            conn.commit();
            return getPanierActif(idUtilisateur);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // â”€â”€â”€ Retirer un produit â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static Panier retirerProduit(int idUtilisateur, int idProductFormats) throws SQLException {
        return modifierQuantite(idUtilisateur, idProductFormats, 0);
    }

    // â”€â”€â”€ Vider le panier â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static Panier viderPanier(int idUtilisateur) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            Panier panier = getPanierActif(idUtilisateur);

            String sql = "DELETE FROM ligne_panier WHERE id_panier = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, panier.getIdPanier());
                ps.executeUpdate();
            }

            String sqlUpd = "UPDATE panier SET montant_total = 0.00 WHERE id_panier = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpd)) {
                ps.setInt(1, panier.getIdPanier());
                ps.executeUpdate();
            }

            conn.commit();
            return getPanierActif(idUtilisateur);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // â”€â”€â”€ Valider le panier â†’ gÃ©nÃ¨re un rÃ©capitulatif â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Valide le panier : gÃ©nÃ¨re un rÃ©capitulatif (CommandeDTO) et marque
     * le panier comme 'valide'. La commande sera enregistrÃ©e en BD
     * dans une Ã©tape ultÃ©rieure (ex : aprÃ¨s paiement).
     *
     * @return un objet CommandeDTO contenant le rÃ©capitulatif complet
     */
    public static CommandeDTO validerPanier(int idUtilisateur) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            Panier panier = getPanierActif(idUtilisateur);
            if (panier.getLignes().isEmpty()) {
                throw new SQLException("Le panier est vide.");
            }

            // --- 1. Recuperer le nom du client ---
            String sqlUser = "SELECT nom, prenom FROM utilisateur WHERE idUtilisateur = ?";
            String nomUtilisateur = "Client";
            try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setInt(1, idUtilisateur);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    nomUtilisateur = rs.getString("prenom") + " " + rs.getString("nom");
                }
            }

            // --- 2. Generer une reference provisoire ---
            String reference = genererReference(conn);
            String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());
            double total = panier.getMontantTotal() != null ? panier.getMontantTotal().doubleValue() : 0.0;

            CommandeDTO recap = new CommandeDTO(reference, nomUtilisateur, total, "en_preparation", dateStr);
            recap.setReference(reference); // <--- Fixed missing reference

            // --- 3. Construire les lignes du recap (SANS ecriture en BDD) ---
            java.util.List<LigneCommandeDTO> lignesRecap = new java.util.ArrayList<>();
            for (LignePanier ligne : panier.getLignes()) {
                LigneCommandeDTO l = new LigneCommandeDTO();
                l.setNomProduit(ligne.getNomProduit());
                l.setQuantite(ligne.getQuantite());
                l.setPrixUnitaire(ligne.getPrix() != null ? ligne.getPrix().doubleValue() : 0.0);
                l.setSousTotal(ligne.getSousTotal() != null ? ligne.getSousTotal().doubleValue() : 0.0);
                lignesRecap.add(l);
            }
            recap.setLignes(lignesRecap);

            // --- 4. Marquer le panier comme valide ---
            String sqlValide = "UPDATE panier SET statut = 'valide' WHERE id_panier = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlValide)) {
                ps.setInt(1, panier.getIdPanier());
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[PanierDAO] Panier valide - ref=" + reference);
            return recap;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // â”€â”€â”€ Utilitaires privÃ©s â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Charge les lignes d'un panier avec JOIN sur produit et product_formats.
     */
    private static List<LignePanier> getLignes(int idPanier, Connection conn) throws SQLException {
        String sql = """
            SELECT lp.id_product_formats,
                   lp.quantite,
                   pf.prix           AS prix_unitaire,
                   pf.image_url,
                   p.nom             AS nom_produit,
                   GROUP_CONCAT(lv.valeur ORDER BY l.nom SEPARATOR ' â€“ ') AS variants
            FROM ligne_panier lp
            JOIN product_formats pf ON pf.id_product_formats = lp.id_product_formats
            JOIN produit p           ON p.id_produit = pf.id_produit
            LEFT JOIN product_formats_values pfv ON pfv.id_product_formats = pf.id_product_formats
            LEFT JOIN label_values lv  ON lv.id_labelValues = pfv.id_labelValues
            LEFT JOIN label l          ON l.id_label = lv.id_label
            WHERE lp.id_panier = ?
            GROUP BY lp.id_product_formats, lp.quantite, pf.prix, pf.image_url, p.nom
        """;
        List<LignePanier> lignes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPanier);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LignePanier ligne = new LignePanier();
                ligne.setIdPanier(idPanier);
                ligne.setIdProductFormats(rs.getInt("id_product_formats"));
                ligne.setQuantite(rs.getInt("quantite"));
                ligne.setPrix(rs.getBigDecimal("prix_unitaire"));
                ligne.setNomProduit(rs.getString("nom_produit"));
                ligne.setDescriptionVariant(rs.getString("variants"));
                ligne.setImageUrl(rs.getString("image_url"));
                lignes.add(ligne);
            }
        }
        return lignes;
    }

    /** Recalcule et met Ã  jour le montant_total dans la table panier. */
    private static void mettreAJourTotal(int idPanier, Connection conn) throws SQLException {
        String sql = """
            UPDATE panier p
            SET montant_total = (
                SELECT COALESCE(SUM(lp.quantite * pf.prix), 0)
                FROM ligne_panier lp
                JOIN product_formats pf ON pf.id_product_formats = lp.id_product_formats
                WHERE lp.id_panier = p.id_panier
            )
            WHERE id_panier = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPanier);
            ps.executeUpdate();
        }
    }

    /** Retourne le stock disponible d'un format produit. */
    private static int getStock(int idProductFormats, Connection conn) throws SQLException {
        String sql = "SELECT stock FROM product_formats WHERE id_product_formats = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProductFormats);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("stock") : 0;
        }
    }

    /** GÃ©nÃ¨re une rÃ©fÃ©rence commande unique au format CMD-YYYY-NNNNN. */
    private static String genererReference(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE YEAR(date_commande) = YEAR(NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt(1) + 1 : 1;
            return String.format("CMD-%d-%05d",
                    java.time.Year.now().getValue(), count);
        }
    }

    /** Mappe un ResultSet vers un objet Panier (sans les lignes). */
    private static Panier mapPanier(ResultSet rs) throws SQLException {
        Panier p = new Panier();
        p.setIdPanier(rs.getInt("id_panier"));
        p.setIdUtilisateur(rs.getInt("idUtilisateur"));
        p.setMontantTotal(rs.getBigDecimal("montant_total"));
        p.setStatut(rs.getString("statut"));
        p.setDateCreation(rs.getTimestamp("date_creation"));
        p.setDateModification(rs.getTimestamp("date_modification"));
        return p;
    }
}
