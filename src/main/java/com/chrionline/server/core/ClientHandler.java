package com.chrionline.server.core;


import com.chrionline.database.DatabaseConnection;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

/**
 * Gère la communication avec UN client connecté au serveur ChriOnline.
 *
 * Protocole : HashMap<String, Object> sérialisé via ObjectStream.
 *   → Requête  : { "commande": "NOM_CMD", "param1": ..., "param2": ... }
 *   → Réponse  : { "statut": "OK" | "ERREUR", "message": "...", "data": ... }
 *
 * Commandes supportées :
 *   INSCRIPTION, CONNEXION, DECONNEXION,
 *   LISTE_PRODUITS, DETAILS_PRODUIT,
 *   AJOUTER_PANIER, SUPPRIMER_PANIER, VOIR_PANIER, VIDER_PANIER,
 *   VALIDER_COMMANDE, HISTORIQUE_COMMANDES
 */
public class ClientHandler implements Runnable {

    // ─── Attributs ────────────────────────────────────────────────────────────

    private final Socket socket;
    private final Server server;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    /** -1 = non authentifié */
    private int    userId   = -1;
    private String username = null;

    // ─── Constructeur ─────────────────────────────────────────────────────────

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    // ─── Thread principal ─────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            /*
             * IMPORTANT : ObjectOutputStream DOIT être créé en premier pour éviter
             * le deadlock mutuel avec le client (qui crée OIS en premier côté client
             * après avoir envoyé l'en-tête OOS).
             */
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in  = new ObjectInputStream(socket.getInputStream());

            System.out.println("[HANDLER] Nouveau client : "
                    + socket.getInetAddress().getHostAddress());

            Object requete;
            while ((requete = in.readObject()) != null) {
                traiterRequete(requete);
            }

        } catch (EOFException | SocketException e) {
            // Déconnexion normale du client
            System.out.println("[HANDLER] Client déconnecté : "
                    + socket.getInetAddress().getHostAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[HANDLER] Erreur : " + e.getMessage());
        } finally {
            server.gererDeconnexion(this);
            fermerConnexion();
        }
    }

    // ─── Dispatch des commandes ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void traiterRequete(Object objet) {
        if (!(objet instanceof Map)) {
            envoyerMessage(reponseErreur("Format de requête invalide."));
            return;
        }

        Map<String, Object> req = (Map<String, Object>) objet;
        String commande = (String) req.getOrDefault("commande", "");

        System.out.println("[HANDLER] Commande reçue : " + commande
                + (userId != -1 ? " (user=" + username + ")" : " (non authentifié)"));

        switch (commande) {
            case "INSCRIPTION"          -> traiterInscription(req);
            case "CONNEXION"            -> traiterConnexion(req);
            case "DECONNEXION"          -> traiterDeconnexion();
            case "LISTE_PRODUITS"       -> traiterListeProduits();
            case "DETAILS_PRODUIT"      -> traiterDetailsProduit(req);
            case "AJOUTER_PANIER"       -> traiterAjouterPanier(req);
            case "SUPPRIMER_PANIER"     -> traiterSupprimerPanier(req);
            case "VOIR_PANIER"          -> traiterVoirPanier();
            case "VIDER_PANIER"         -> traiterViderPanier();
            case "VALIDER_COMMANDE"     -> traiterValiderCommande(req);
            case "HISTORIQUE_COMMANDES" -> traiterHistoriqueCommandes();
            default -> envoyerMessage(reponseErreur("Commande inconnue : " + commande));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GESTION DES UTILISATEURS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * INSCRIPTION
     * Requête  : { "commande":"INSCRIPTION", "login":"...", "motDePasse":"...",
     *              "email":"...", "nom":"...", "prenom":"..." }
     * Réponse  : OK + userId  |  ERREUR
     */
    private void traiterInscription(Map<String, Object> req) {
        String login      = (String) req.get("login");
        String motDePasse = (String) req.get("motDePasse");
        String email      = (String) req.get("email");
        String nom        = (String) req.get("nom");
        String prenom     = (String) req.get("prenom");

        if (login == null || motDePasse == null || email == null) {
            envoyerMessage(reponseErreur("Champs obligatoires manquants (login, motDePasse, email)."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            // Vérifier si le login est déjà pris
            PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM utilisateurs WHERE login = ?");
            check.setString(1, login);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                envoyerMessage(reponseErreur("Ce login est déjà utilisé."));
                return;
            }

            // Insérer le nouvel utilisateur (mot de passe en clair pour ce niveau minimal)
            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO utilisateurs (login, mot_de_passe, email, nom, prenom) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            insert.setString(1, login);
            insert.setString(2, motDePasse);
            insert.setString(3, email);
            insert.setString(4, nom   != null ? nom   : "");
            insert.setString(5, prenom != null ? prenom : "");
            insert.executeUpdate();

            ResultSet keys = insert.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;

            Map<String, Object> data = new HashMap<>();
            data.put("userId", newId);
            envoyerMessage(reponseOk("Inscription réussie.", data));
            System.out.println("[HANDLER] Nouvel utilisateur inscrit : " + login);

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur INSCRIPTION : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur lors de l'inscription."));
        }
    }

    /**
     * CONNEXION
     * Requête  : { "commande":"CONNEXION", "login":"...", "motDePasse":"..." }
     * Réponse  : OK + { userId, login, nom, prenom, email }  |  ERREUR
     */
    private void traiterConnexion(Map<String, Object> req) {
        String login      = (String) req.get("login");
        String motDePasse = (String) req.get("motDePasse");

        if (login == null || motDePasse == null) {
            envoyerMessage(reponseErreur("Login et mot de passe requis."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, login, nom, prenom, email FROM utilisateurs " +
                            "WHERE login = ? AND mot_de_passe = ?");
            ps.setString(1, login);
            ps.setString(2, motDePasse);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                envoyerMessage(reponseErreur("Login ou mot de passe incorrect."));
                return;
            }

            // Mémoriser la session côté handler
            this.userId   = rs.getInt("id");
            this.username = rs.getString("login");

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("login",  username);
            data.put("nom",    rs.getString("nom"));
            data.put("prenom", rs.getString("prenom"));
            data.put("email",  rs.getString("email"));

            envoyerMessage(reponseOk("Connexion réussie. Bienvenue, " + username + " !", data));
            System.out.println("[HANDLER] Utilisateur connecté : " + username + " (id=" + userId + ")");

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur CONNEXION : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur lors de la connexion."));
        }
    }

    /**
     * DECONNEXION
     * Réponse : OK
     */
    private void traiterDeconnexion() {
        System.out.println("[HANDLER] Déconnexion demandée par : "
                + (username != null ? username : "inconnu"));
        envoyerMessage(reponseOk("Déconnexion réussie.", null));
        this.userId   = -1;
        this.username = null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GESTION DES PRODUITS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * LISTE_PRODUITS
     * Requête  : { "commande":"LISTE_PRODUITS" }
     * Réponse  : OK + List<Map> [ { id, nom, prix, stock, categorie }, ... ]
     */
    private void traiterListeProduits() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nom, prix, stock, categorie FROM produits WHERE stock > 0 ORDER BY nom");
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> produits = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id",        rs.getInt("id"));
                p.put("nom",       rs.getString("nom"));
                p.put("prix",      rs.getDouble("prix"));
                p.put("stock",     rs.getInt("stock"));
                p.put("categorie", rs.getString("categorie"));
                produits.add(p);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("produits", produits);
            data.put("total",    produits.size());
            envoyerMessage(reponseOk("Liste des produits récupérée.", data));

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur LISTE_PRODUITS : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur lors de la récupération des produits."));
        }
    }

    /**
     * DETAILS_PRODUIT
     * Requête  : { "commande":"DETAILS_PRODUIT", "produitId": 42 }
     * Réponse  : OK + { id, nom, prix, description, stock, categorie }  |  ERREUR
     */
    private void traiterDetailsProduit(Map<String, Object> req) {
        Integer produitId = (Integer) req.get("produitId");
        if (produitId == null) {
            envoyerMessage(reponseErreur("produitId manquant."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nom, prix, description, stock, categorie FROM produits WHERE id = ?");
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                envoyerMessage(reponseErreur("Produit introuvable (id=" + produitId + ")."));
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id",          rs.getInt("id"));
            data.put("nom",         rs.getString("nom"));
            data.put("prix",        rs.getDouble("prix"));
            data.put("description", rs.getString("description"));
            data.put("stock",       rs.getInt("stock"));
            data.put("categorie",   rs.getString("categorie"));

            envoyerMessage(reponseOk("Détails du produit récupérés.", data));

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur DETAILS_PRODUIT : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GESTION DU PANIER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * AJOUTER_PANIER
     * Requête  : { "commande":"AJOUTER_PANIER", "produitId": 42, "quantite": 2 }
     * Réponse  : OK  |  ERREUR
     */
    private void traiterAjouterPanier(Map<String, Object> req) {
        if (!estAuthentifie()) return;

        Integer produitId = (Integer) req.get("produitId");
        Integer quantite  = (Integer) req.get("quantite");

        if (produitId == null || quantite == null || quantite <= 0) {
            envoyerMessage(reponseErreur("produitId et quantite (> 0) requis."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            // Vérifier le stock disponible
            PreparedStatement stock = conn.prepareStatement(
                    "SELECT stock FROM produits WHERE id = ?");
            stock.setInt(1, produitId);
            ResultSet rs = stock.executeQuery();
            if (!rs.next()) {
                envoyerMessage(reponseErreur("Produit introuvable."));
                return;
            }
            int stockDispo = rs.getInt("stock");
            if (quantite > stockDispo) {
                envoyerMessage(reponseErreur("Stock insuffisant (disponible : " + stockDispo + ")."));
                return;
            }

            // Vérifier si le produit est déjà dans le panier → mettre à jour la quantité
            PreparedStatement existant = conn.prepareStatement(
                    "SELECT id, quantite FROM panier WHERE utilisateur_id = ? AND produit_id = ?");
            existant.setInt(1, userId);
            existant.setInt(2, produitId);
            ResultSet rsExistant = existant.executeQuery();

            if (rsExistant.next()) {
                int nouvelleQte = rsExistant.getInt("quantite") + quantite;
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE panier SET quantite = ? WHERE id = ?");
                update.setInt(1, nouvelleQte);
                update.setInt(2, rsExistant.getInt("id"));
                update.executeUpdate();
                envoyerMessage(reponseOk("Quantité mise à jour dans le panier.", null));
            } else {
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO panier (utilisateur_id, produit_id, quantite) VALUES (?, ?, ?)");
                insert.setInt(1, userId);
                insert.setInt(2, produitId);
                insert.setInt(3, quantite);
                insert.executeUpdate();
                envoyerMessage(reponseOk("Produit ajouté au panier.", null));
            }

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur AJOUTER_PANIER : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    /**
     * SUPPRIMER_PANIER
     * Requête  : { "commande":"SUPPRIMER_PANIER", "produitId": 42 }
     * Réponse  : OK  |  ERREUR
     */
    private void traiterSupprimerPanier(Map<String, Object> req) {
        if (!estAuthentifie()) return;

        Integer produitId = (Integer) req.get("produitId");
        if (produitId == null) {
            envoyerMessage(reponseErreur("produitId manquant."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM panier WHERE utilisateur_id = ? AND produit_id = ?");
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                envoyerMessage(reponseErreur("Produit non trouvé dans votre panier."));
            } else {
                envoyerMessage(reponseOk("Produit supprimé du panier.", null));
            }

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur SUPPRIMER_PANIER : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    /**
     * VOIR_PANIER
     * Requête  : { "commande":"VOIR_PANIER" }
     * Réponse  : OK + { articles: [...], total: 59.90 }
     */
    private void traiterVoirPanier() {
        if (!estAuthentifie()) return;

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.id AS produit_id, p.nom, p.prix, p.stock, pa.quantite, " +
                            "       (p.prix * pa.quantite) AS sous_total " +
                            "FROM panier pa " +
                            "JOIN produits p ON p.id = pa.produit_id " +
                            "WHERE pa.utilisateur_id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> articles = new ArrayList<>();
            double total = 0.0;

            while (rs.next()) {
                Map<String, Object> article = new HashMap<>();
                article.put("produitId",  rs.getInt("produit_id"));
                article.put("nom",        rs.getString("nom"));
                article.put("prix",       rs.getDouble("prix"));
                article.put("quantite",   rs.getInt("quantite"));
                article.put("sousTotal",  rs.getDouble("sous_total"));
                articles.add(article);
                total += rs.getDouble("sous_total");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("articles", articles);
            data.put("total",    Math.round(total * 100.0) / 100.0);
            data.put("nbArticles", articles.size());

            envoyerMessage(reponseOk("Panier récupéré.", data));

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur VOIR_PANIER : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    /**
     * VIDER_PANIER
     * Requête  : { "commande":"VIDER_PANIER" }
     * Réponse  : OK
     */
    private void traiterViderPanier() {
        if (!estAuthentifie()) return;

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM panier WHERE utilisateur_id = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();

            envoyerMessage(reponseOk("Panier vidé.", null));

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur VIDER_PANIER : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  COMMANDES & PAIEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * VALIDER_COMMANDE  (inclut la simulation de paiement)
     *
     * Requête  : { "commande":"VALIDER_COMMANDE",
     *              "methodePaiement": "CARTE" | "VIREMENT" | "FICTIF",
     *              "numeroCarte": "xxxx-xxxx-xxxx-xxxx"   (optionnel)  }
     *
     * Réponse  : OK + { commandeId, idUnique, total, statut }  |  ERREUR
     *
     * Logique  :
     *   1. Lire le panier
     *   2. Vérifier les stocks
     *   3. Simuler le paiement
     *   4. Créer la commande + détails (transaction SQL)
     *   5. Décrémenter les stocks
     *   6. Vider le panier
     *   7. Envoyer une notification UDP au client
     */
    private void traiterValiderCommande(Map<String, Object> req) {
        if (!estAuthentifie()) return;

        String methodePaiement = (String) req.getOrDefault("methodePaiement", "FICTIF");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // ← Début de la transaction

            try {
                // 1. Lire le panier
                PreparedStatement psPanier = conn.prepareStatement(
                        "SELECT pa.produit_id, pa.quantite, p.prix, p.stock, p.nom " +
                                "FROM panier pa JOIN produits p ON p.id = pa.produit_id " +
                                "WHERE pa.utilisateur_id = ?");
                psPanier.setInt(1, userId);
                ResultSet rsPanier = psPanier.executeQuery();

                List<Map<String, Object>> lignes = new ArrayList<>();
                double total = 0.0;

                while (rsPanier.next()) {
                    Map<String, Object> ligne = new HashMap<>();
                    ligne.put("produitId", rsPanier.getInt("produit_id"));
                    ligne.put("quantite",  rsPanier.getInt("quantite"));
                    ligne.put("prix",      rsPanier.getDouble("prix"));
                    ligne.put("stock",     rsPanier.getInt("stock"));
                    ligne.put("nom",       rsPanier.getString("nom"));
                    lignes.add(ligne);
                    total += rsPanier.getDouble("prix") * rsPanier.getInt("quantite");
                }

                if (lignes.isEmpty()) {
                    conn.rollback();
                    envoyerMessage(reponseErreur("Votre panier est vide."));
                    return;
                }

                // 2. Vérifier les stocks
                for (Map<String, Object> ligne : lignes) {
                    int dispo = (int) ligne.get("stock");
                    int qte   = (int) ligne.get("quantite");
                    if (qte > dispo) {
                        conn.rollback();
                        envoyerMessage(reponseErreur(
                                "Stock insuffisant pour « " + ligne.get("nom") +
                                        " » (demandé : " + qte + ", disponible : " + dispo + ")."));
                        return;
                    }
                }

                // 3. Simulation du paiement
                String resultatPaiement = simulerPaiement(methodePaiement, total);
                if (!"APPROUVE".equals(resultatPaiement)) {
                    conn.rollback();
                    envoyerMessage(reponseErreur("Paiement refusé : " + resultatPaiement));
                    return;
                }

                // 4. Créer la commande
                String idUnique = "CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                double totalArrondi = Math.round(total * 100.0) / 100.0;

                PreparedStatement psCommande = conn.prepareStatement(
                        "INSERT INTO commandes (utilisateur_id, total, statut, date_commande, id_unique) " +
                                "VALUES (?, ?, 'EN_ATTENTE', NOW(), ?)",
                        Statement.RETURN_GENERATED_KEYS);
                psCommande.setInt(1, userId);
                psCommande.setDouble(2, totalArrondi);
                psCommande.setString(3, idUnique);
                psCommande.executeUpdate();

                ResultSet keysCmd = psCommande.getGeneratedKeys();
                int commandeId = keysCmd.next() ? keysCmd.getInt(1) : -1;

                // 5. Insérer les détails + décrémenter les stocks
                PreparedStatement psDetail = conn.prepareStatement(
                        "INSERT INTO details_commande (commande_id, produit_id, quantite, prix_unitaire) " +
                                "VALUES (?, ?, ?, ?)");
                PreparedStatement psStock = conn.prepareStatement(
                        "UPDATE produits SET stock = stock - ? WHERE id = ?");

                for (Map<String, Object> ligne : lignes) {
                    psDetail.setInt(1, commandeId);
                    psDetail.setInt(2, (int)    ligne.get("produitId"));
                    psDetail.setInt(3, (int)    ligne.get("quantite"));
                    psDetail.setDouble(4, (double) ligne.get("prix"));
                    psDetail.addBatch();

                    psStock.setInt(1, (int) ligne.get("quantite"));
                    psStock.setInt(2, (int) ligne.get("produitId"));
                    psStock.addBatch();
                }
                psDetail.executeBatch();
                psStock.executeBatch();

                // 6. Vider le panier
                PreparedStatement psVider = conn.prepareStatement(
                        "DELETE FROM panier WHERE utilisateur_id = ?");
                psVider.setInt(1, userId);
                psVider.executeUpdate();

                conn.commit(); // ← Valider la transaction

                // 7. Notification UDP de confirmation
                String notification = "COMMANDE_CONFIRMEE|" + idUnique + "|" + totalArrondi + "€";
                server.diffuserNotification(
                        notification,
                        socket.getInetAddress(),
                        9092 // CLIENT_UDP_PORT
                );

                // Réponse au client
                Map<String, Object> data = new HashMap<>();
                data.put("commandeId",  commandeId);
                data.put("idUnique",    idUnique);
                data.put("total",       totalArrondi);
                data.put("statut",      "EN_ATTENTE");
                data.put("paiement",    resultatPaiement);
                data.put("nbArticles",  lignes.size());

                envoyerMessage(reponseOk(
                        "Commande validée avec succès ! Référence : " + idUnique, data));

                System.out.println("[HANDLER] Commande " + idUnique
                        + " créée pour " + username + " → " + totalArrondi + " €");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur VALIDER_COMMANDE : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur lors de la validation de la commande."));
        }
    }

    /**
     * HISTORIQUE_COMMANDES
     * Requête  : { "commande":"HISTORIQUE_COMMANDES" }
     * Réponse  : OK + { commandes: [ { commandeId, idUnique, total, statut, date, articles:[...] } ] }
     */
    private void traiterHistoriqueCommandes() {
        if (!estAuthentifie()) return;

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            // Récupérer les commandes
            PreparedStatement psCmd = conn.prepareStatement(
                    "SELECT id, id_unique, total, statut, date_commande " +
                            "FROM commandes WHERE utilisateur_id = ? ORDER BY date_commande DESC");
            psCmd.setInt(1, userId);
            ResultSet rsCmd = psCmd.executeQuery();

            List<Map<String, Object>> commandes = new ArrayList<>();

            while (rsCmd.next()) {
                Map<String, Object> cmd = new HashMap<>();
                int cmdId = rsCmd.getInt("id");
                cmd.put("commandeId", cmdId);
                cmd.put("idUnique",   rsCmd.getString("id_unique"));
                cmd.put("total",      rsCmd.getDouble("total"));
                cmd.put("statut",     rsCmd.getString("statut"));
                cmd.put("date",       rsCmd.getString("date_commande"));

                // Récupérer les articles de cette commande
                PreparedStatement psDetails = conn.prepareStatement(
                        "SELECT p.nom, dc.quantite, dc.prix_unitaire, " +
                                "       (dc.quantite * dc.prix_unitaire) AS sous_total " +
                                "FROM details_commande dc JOIN produits p ON p.id = dc.produit_id " +
                                "WHERE dc.commande_id = ?");
                psDetails.setInt(1, cmdId);
                ResultSet rsDetails = psDetails.executeQuery();

                List<Map<String, Object>> articles = new ArrayList<>();
                while (rsDetails.next()) {
                    Map<String, Object> article = new HashMap<>();
                    article.put("nom",         rsDetails.getString("nom"));
                    article.put("quantite",    rsDetails.getInt("quantite"));
                    article.put("prixUnitaire",rsDetails.getDouble("prix_unitaire"));
                    article.put("sousTotal",   rsDetails.getDouble("sous_total"));
                    articles.add(article);
                }
                cmd.put("articles", articles);
                commandes.add(cmd);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("commandes",    commandes);
            data.put("nbCommandes",  commandes.size());

            envoyerMessage(reponseOk("Historique des commandes récupéré.", data));

        } catch (SQLException e) {
            System.err.println("[HANDLER] Erreur HISTORIQUE_COMMANDES : " + e.getMessage());
            envoyerMessage(reponseErreur("Erreur serveur."));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simulation du paiement.
     * @return "APPROUVE" ou un message de refus
     */
    private String simulerPaiement(String methode, double montant) {
        System.out.println("[PAIEMENT] Simulation → méthode=" + methode
                + ", montant=" + montant + " €");
        // Simulation : toujours approuvé (niveau minimum)
        // Niveau avancé : valider le format carte, appeler un service fictif, etc.
        return "APPROUVE";
    }

    /**
     * Vérifie que l'utilisateur est connecté. Envoie une erreur sinon.
     */
    private boolean estAuthentifie() {
        if (userId == -1) {
            envoyerMessage(reponseErreur("Accès refusé : vous devez être connecté."));
            return false;
        }
        return true;
    }

    /**
     * Construit une réponse de succès.
     */
    private Map<String, Object> reponseOk(String message, Map<String, Object> data) {
        Map<String, Object> rep = new HashMap<>();
        rep.put("statut",  "OK");
        rep.put("message", message);
        if (data != null) rep.put("data", data);
        return rep;
    }

    /**
     * Construit une réponse d'erreur.
     */
    private Map<String, Object> reponseErreur(String message) {
        Map<String, Object> rep = new HashMap<>();
        rep.put("statut",  "ERREUR");
        rep.put("message", message);
        return rep;
    }

    // ─── Méthodes publiques appelées par Server ───────────────────────────────

    /**
     * Envoie un objet sérialisé au client.
     * Appelé par Server.envoyerReponse() et directement dans ce handler.
     */
    public synchronized void envoyerMessage(Object objet) {
        try {
            if (out != null) {
                out.writeObject(objet);
                out.flush();
                out.reset(); // Évite que ObjectOutputStream mette les objets en cache
            }
        } catch (IOException e) {
            System.err.println("[HANDLER] Impossible d'envoyer le message : " + e.getMessage());
        }
    }

    /**
     * Ferme proprement le socket et les flux I/O.
     * Appelé par Server.gererDeconnexion().
     */
    public void fermerConnexion() {
        try {
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[HANDLER] Erreur lors de la fermeture : " + e.getMessage());
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public int    getUserId()   { return userId;   }
    public String getUsername() { return username; }
    public Socket getSocket()   { return socket;   }
}
