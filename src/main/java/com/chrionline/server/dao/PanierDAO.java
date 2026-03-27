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

    // â”€â”€â”€ RÃ©cupÃ©rer ou crÃ©er le panier actif
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            if (!keys.next())
                throw new SQLException("Ã‰chec crÃ©ation panier");

            Panier p = new Panier();
            p.setIdPanier(keys.getInt(1));
            p.setIdUtilisateur(idUtilisateur);
            p.setStatut("actif");
            p.setLignes(new ArrayList<>());
            System.out.println("[PanierDAO] Nouveau panier crÃ©Ã© â€” id=" + p.getIdPanier());
            return p;
        }
    }

    // â”€â”€â”€ Ajouter un produit
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Modifier la quantitÃ©
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Retirer un produit
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static Panier retirerProduit(int idUtilisateur, int idProductFormats) throws SQLException {
        return modifierQuantite(idUtilisateur, idProductFormats, 0);
    }

    // â”€â”€â”€ Vider le panier
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Valider le panier â†’ gÃ©nÃ¨re un rÃ©capitulatif
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

            // SANS MARQUER LE PANIER COMME VALIDE ICI, ça sera fait lors du paiement.
            conn.commit();
            System.out.println("[PanierDAO] Panier recapitulé (non validé) - ref provisoire=" + reference);
            return recap;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Confirme la commande après choix du paiement.
     * Crée la commande, les lignes, met à jour le stock, crée le paiement, génère
     * la facture,
     * et marque le panier actif comme 'valide'.
     */
    public static CommandeDTO confirmerCommande(int idUtilisateur, String methodePaiement, String nomCarte,
            String numeroCarte) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            Panier panier = getPanierActif(idUtilisateur);
            if (panier.getLignes().isEmpty()) {
                throw new SQLException("Le panier est vide.");
            }

            // --- 1. Generer reference definitive ---
            String reference = genererReference(conn);

            // --- 2. Inserer dans commande ---
            String sqlCmd = "INSERT INTO commande (idUtilisateur, reference, status, date_commande, date_modification) VALUES (?, ?, 'en_preparation', NOW(), NOW())";
            int idCommande;
            try (PreparedStatement ps = conn.prepareStatement(sqlCmd, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUtilisateur);
                ps.setString(2, reference);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next())
                    throw new SQLException("Echec creation commande");
                idCommande = keys.getInt(1);
            }

            // --- 3. Inserer les lignes de commande et MAJ stock ---
            String sqlLigne = "INSERT INTO ligne_commande (id_commande, id_product_formats, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
            String sqlStock = "UPDATE product_formats SET stock = stock - ? WHERE id_product_formats = ? AND stock >= ?";

            try (PreparedStatement psLigne = conn.prepareStatement(sqlLigne);
                    PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

                for (LignePanier ligne : panier.getLignes()) {
                    // Update stock
                    psStock.setInt(1, ligne.getQuantite());
                    psStock.setInt(2, ligne.getIdProductFormats());
                    psStock.setInt(3, ligne.getQuantite());
                    int updated = psStock.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Stock insuffisant pour le produit " + ligne.getNomProduit());
                    }

                    // Insert ligne
                    psLigne.setInt(1, idCommande);
                    psLigne.setInt(2, ligne.getIdProductFormats());
                    psLigne.setInt(3, ligne.getQuantite());
                    psLigne.setBigDecimal(4, ligne.getPrix());
                    psLigne.executeUpdate();
                }
            }

            // --- 4. Inserer le paiement et generer facture ---
            java.io.File dirFactures = new java.io.File("factures");
            if (!dirFactures.exists())
                dirFactures.mkdir();
            String cheminFacture = "factures/" + reference + ".pdf";
            genererFacturePDF(cheminFacture, reference, panier, methodePaiement, idUtilisateur, conn);

            String sqlPaiement = "INSERT INTO paiement (id_commande, methode_paiement, statut_paiement, montant, date_paiement, chemin_facture, nom_carte, numero_carte) VALUES (?, ?, 'paye', ?, NOW(), ?, ?, ?)";
            try (PreparedStatement psPaiement = conn.prepareStatement(sqlPaiement)) {
                psPaiement.setInt(1, idCommande);
                psPaiement.setString(2, methodePaiement);
                psPaiement.setBigDecimal(3, panier.getMontantTotal());
                psPaiement.setString(4, cheminFacture);
                psPaiement.setString(5, nomCarte);
                psPaiement.setString(6, numeroCarte);
                psPaiement.executeUpdate();
            }

            // --- 5. Marquer le panier comme valide ---
            String sqlValide = "UPDATE panier SET statut = 'valide' WHERE id_panier = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlValide)) {
                ps.setInt(1, panier.getIdPanier());
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[PanierDAO] Commande confirmee - ref=" + reference);

            // --- 6. Fetch user info for DTO ---
            String sqlUser = "SELECT nom, prenom FROM utilisateur WHERE idUtilisateur = ?";
            String nomUtilisateur = "Client";
            try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setInt(1, idUtilisateur);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    nomUtilisateur = rs.getString("prenom") + " " + rs.getString("nom");
                }
            }

            String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());
            CommandeDTO recap = new CommandeDTO(reference, nomUtilisateur, panier.getMontantTotal().doubleValue(),
                    "en_preparation", dateStr);
            recap.setReference(reference);

            java.util.List<LigneCommandeDTO> lignesRecap = new java.util.ArrayList<>();
            for (LignePanier ligne : panier.getLignes()) {
                LigneCommandeDTO l = new LigneCommandeDTO();
                l.setNomProduit(ligne.getNomProduit());
                l.setQuantite(ligne.getQuantite());
                l.setPrixUnitaire(ligne.getPrix().doubleValue());
                l.setSousTotal(ligne.getSousTotal().doubleValue());
                lignesRecap.add(l);
            }
            recap.setLignes(lignesRecap);

            // --- 7. Détecter les formats de produit dont le stock est passé sous le seuil d'alerte ---
            java.util.List<String> alertesStock = new java.util.ArrayList<>();
            String sqlAlerte = "SELECT p.nom AS nom_produit, pf.id_product_formats, pf.stock, pf.stock_alerte " +
                    "FROM product_formats pf " +
                    "JOIN produit p ON p.id_produit = pf.id_produit " +
                    "WHERE pf.id_product_formats = ? AND pf.stock < pf.stock_alerte";
            try (PreparedStatement psAlerte = conn.prepareStatement(sqlAlerte)) {
                for (LignePanier ligne : panier.getLignes()) {
                    psAlerte.setInt(1, ligne.getIdProductFormats());
                    ResultSet rsA = psAlerte.executeQuery();
                    if (rsA.next()) {
                        String nomProduit  = rsA.getString("nom_produit");
                        int stockRestant   = rsA.getInt("stock");
                        int stockAlerteSeuil = rsA.getInt("stock_alerte");
                        String msg = nomProduit + ":stock=" + stockRestant + ":seuil=" + stockAlerteSeuil;
                        alertesStock.add(msg);
                        System.out.println("[PanierDAO] ⚠ STOCK ALERTE — " + msg);
                    }
                }
            } catch (Exception e) {
                System.err.println("[PanierDAO] Erreur vérification alertes stock : " + e.getMessage());
            }
            recap.setAlertesStock(alertesStock);
            return recap;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // â”€â”€â”€ Utilitaires privÃ©s
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    /** Genere un fichier PDF de facture super professionnel en utilisant iText */
    private static void genererFacturePDF(String chemin, String reference, Panier panier, String methodePaiement,
            int idUtilisateur, Connection conn) {
        String nomClient = "Client " + idUtilisateur;
        try (PreparedStatement ps = conn
                .prepareStatement("SELECT nom, prenom FROM utilisateur WHERE idUtilisateur = ?")) {
            ps.setInt(1, idUtilisateur);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                nomClient = rs.getString("prenom") + " " + rs.getString("nom");
        } catch (Exception ignored) {
        }

        com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 40, 40, 50,
                50);
        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(chemin));
            document.open();

            // Polices
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                    28, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(62, 44, 30)); // BRUN
            com.itextpdf.text.Font subTitleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.NORMAL,
                    new com.itextpdf.text.BaseColor(154, 123, 101)); // BRUN_LIGHT
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                    11, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.DARK_GRAY);
            com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                    11, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.DARK_GRAY);

            com.itextpdf.text.Font tableHeaderFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD,
                    com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font tableRowFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL,
                    com.itextpdf.text.BaseColor.DARK_GRAY);

            com.itextpdf.text.Font totalTitleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(107, 158, 122)); // SAUGE_DARK
            com.itextpdf.text.Font totalValueFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(201, 107, 74)); // TERRACOTTA

            // Couleurs
            com.itextpdf.text.BaseColor headerColor = new com.itextpdf.text.BaseColor(107, 158, 122); // SAUGE_DARK
            com.itextpdf.text.BaseColor rowEvenColor = new com.itextpdf.text.BaseColor(253, 251, 247); // CREME
            com.itextpdf.text.BaseColor rowOddColor = new com.itextpdf.text.BaseColor(255, 255, 255); // BLANC
            com.itextpdf.text.BaseColor borderColor = new com.itextpdf.text.BaseColor(232, 224, 213); // BORDER

            // Header Principal (ChriOnline + Facture)
            com.itextpdf.text.pdf.PdfPTable headerTable = new com.itextpdf.text.pdf.PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.getDefaultCell().setBorder(com.itextpdf.text.Rectangle.NO_BORDER);

            com.itextpdf.text.pdf.PdfPCell leftHeaderCell = new com.itextpdf.text.pdf.PdfPCell();
            leftHeaderCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            leftHeaderCell.addElement(new com.itextpdf.text.Paragraph("ChriOnline", titleFont));
            leftHeaderCell.addElement(
                    new com.itextpdf.text.Paragraph("Vente de produits naturels & cosmétiques", subTitleFont));

            com.itextpdf.text.pdf.PdfPCell rightHeaderCell = new com.itextpdf.text.pdf.PdfPCell();
            rightHeaderCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            rightHeaderCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            com.itextpdf.text.Paragraph pFacture = new com.itextpdf.text.Paragraph("FACTURE",
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 24,
                            com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.LIGHT_GRAY));
            pFacture.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            rightHeaderCell.addElement(pFacture);

            headerTable.addCell(leftHeaderCell);
            headerTable.addCell(rightHeaderCell);
            headerTable.setSpacingAfter(40);
            document.add(headerTable);

            // Tracer une ligne de séparation
            com.itextpdf.text.pdf.draw.LineSeparator ls = new com.itextpdf.text.pdf.draw.LineSeparator();
            ls.setLineColor(borderColor);
            document.add(new com.itextpdf.text.Chunk(ls));
            document.add(new com.itextpdf.text.Paragraph(" ")); // Espace

            // Information Client et Commande
            com.itextpdf.text.pdf.PdfPTable infoTable = new com.itextpdf.text.pdf.PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.getDefaultCell().setBorder(com.itextpdf.text.Rectangle.NO_BORDER);

            String methodeStr = methodePaiement.equals("livraison") ? "À la livraison" : "Carte Bancaire";
            com.itextpdf.text.Paragraph clientInfo = new com.itextpdf.text.Paragraph();
            clientInfo.add(new com.itextpdf.text.Chunk("FACTURÉ À :\n", boldFont));
            clientInfo.add(new com.itextpdf.text.Chunk(nomClient + "\n", normalFont));
            clientInfo.add(new com.itextpdf.text.Chunk("\nMode de paiement :\n", boldFont));
            clientInfo.add(new com.itextpdf.text.Chunk(methodeStr, normalFont));

            String dateFormatted = new java.text.SimpleDateFormat("dd MMMM yyyy").format(new java.util.Date());
            com.itextpdf.text.Paragraph invoiceInfo = new com.itextpdf.text.Paragraph();
            invoiceInfo.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            invoiceInfo.add(new com.itextpdf.text.Chunk("RÉFÉRENCE :\n", boldFont));
            invoiceInfo.add(new com.itextpdf.text.Chunk(reference + "\n", normalFont));
            invoiceInfo.add(new com.itextpdf.text.Chunk("\nDATE DE FACTURATION :\n", boldFont));
            invoiceInfo.add(new com.itextpdf.text.Chunk(dateFormatted, normalFont));

            com.itextpdf.text.pdf.PdfPCell cClient = new com.itextpdf.text.pdf.PdfPCell(clientInfo);
            cClient.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);

            com.itextpdf.text.pdf.PdfPCell cInvoice = new com.itextpdf.text.pdf.PdfPCell(invoiceInfo);
            cInvoice.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            cInvoice.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

            infoTable.addCell(cClient);
            infoTable.addCell(cInvoice);
            infoTable.setSpacingAfter(40);
            document.add(infoTable);

            // Tableau des Articles
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3.5f, 1f, 1.5f, 1.5f });

            String[] headers = { "Désignation de l'article", "Quantité", "Prix Unitaire", "Total TTC" };
            for (String h : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(h, tableHeaderFont));
                cell.setBackgroundColor(headerColor);
                cell.setPadding(10);
                cell.setBorderColor(headerColor);
                table.addCell(cell);
            }

            boolean isEven = false;
            for (com.chrionline.shared.models.LignePanier l : panier.getLignes()) {
                com.itextpdf.text.BaseColor rowColor = isEven ? rowEvenColor : rowOddColor;

                com.itextpdf.text.pdf.PdfPCell c1 = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(l.getNomProduit(), tableRowFont));
                com.itextpdf.text.pdf.PdfPCell c2 = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(String.valueOf(l.getQuantite()), tableRowFont));
                com.itextpdf.text.pdf.PdfPCell c3 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(
                        String.format("%.2f MAD", l.getPrix().doubleValue()), tableRowFont));
                com.itextpdf.text.pdf.PdfPCell c4 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(
                        String.format("%.2f MAD", l.getSousTotal().doubleValue()), tableRowFont));

                c2.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                c3.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                c4.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

                com.itextpdf.text.pdf.PdfPCell[] cells = { c1, c2, c3, c4 };
                for (com.itextpdf.text.pdf.PdfPCell c : cells) {
                    c.setBorderColor(borderColor);
                    c.setBackgroundColor(rowColor);
                    c.setPadding(10);
                    table.addCell(c);
                }
                isEven = !isEven;
            }
            table.setSpacingAfter(20);
            document.add(table);

            // Zone du Total
            com.itextpdf.text.pdf.PdfPTable totalTable = new com.itextpdf.text.pdf.PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[] { 6f, 3f });
            totalTable.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(""))); // Cellule vide à
                                                                                                      // gauche

            com.itextpdf.text.pdf.PdfPCell totalCell = new com.itextpdf.text.pdf.PdfPCell();
            totalCell.setBorderColor(borderColor);
            totalCell.setBackgroundColor(rowEvenColor);
            totalCell.setPadding(15);

            com.itextpdf.text.Paragraph tTitle = new com.itextpdf.text.Paragraph("NET À PAYER", totalTitleFont);
            tTitle.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            com.itextpdf.text.Paragraph tValue = new com.itextpdf.text.Paragraph(
                    String.format("%.2f MAD", panier.getMontantTotal().doubleValue()), totalValueFont);
            tValue.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

            totalCell.addElement(tTitle);
            totalCell.addElement(tValue);
            totalTable.addCell(totalCell);
            document.add(totalTable);

            // Footer de la page
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                    "\n\nMerci pour votre confiance ! Pour toute question concernant cette facture,\n" +
                            "veuillez nous contacter via support@chrionline.com",
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                            com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.GRAY));
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            footer.setSpacingBefore(60);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            System.err.println("[PanierDAO] Erreur generation facture pdf iText : " + e.getMessage());
        }
    }
}
