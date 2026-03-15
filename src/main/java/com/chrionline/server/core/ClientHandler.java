package com.chrionline.server.core;

import com.chrionline.database.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

/**
 * Gère la communication avec UN client connecté au serveur ChriOnline.
 * Adapté au schéma final v3 (utilisateur, client, produit, panier).
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

    private final Socket socket;
    private final Server server;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Session (idUtilisateur)
    private int    userId   = -1;
    private int    clientId = -1; // id_client si c'est un client
    private String userEmail = null;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    // ─── Thread principal ─────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in  = new ObjectInputStream(socket.getInputStream());

            System.out.println("[HANDLER] Nouveau client : " + socket.getInetAddress().getHostAddress());

            Object requete;
            while ((requete = in.readObject()) != null) {
                traiterRequete(requete);
            }

        } catch (EOFException | SocketException e) {
            System.out.println("[HANDLER] Client déconnecté.");
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

        System.out.println("[HANDLER] Commande reçue : " + commande + (userId != -1 ? " (" + userEmail + ")" : " (non authentifié)"));

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
    //  AUTH & UTILISATEURS (schema: utilisateur, client)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * INSCRIPTION
     * Requête  : { "commande":"INSCRIPTION", "email":"...", "motDePasse":"...",
     *              "nom":"...", "prenom":"..." }
     * Réponse  : OK | ERREUR
     */
    private void traiterInscription(Map<String, Object> req) {
        String email    = (String) req.get("email");
        String password = (String) req.get("motDePasse"); // On garde la clé du protocole
        String nom      = (String) req.get("nom");
        String prenom   = (String) req.get("prenom");

        if (email == null || password == null) {
            envoyerMessage(reponseErreur("Email et mot de passe requis."));
            return;
        }

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            try {
                // 1. Créer l'utilisateur
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                PreparedStatement psUser = conn.prepareStatement(
                        "INSERT INTO utilisateur (nom, prenom, email, password) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                psUser.setString(1, nom != null ? nom : "");
                psUser.setString(2, prenom != null ? prenom : "");
                psUser.setString(3, email);
                psUser.setString(4, hashedPassword);
                psUser.executeUpdate();

                ResultSet rsKeys = psUser.getGeneratedKeys();
                if (!rsKeys.next()) throw new SQLException("Erreur création utilisateur.");
                int newUserId = rsKeys.getInt(1);

                // 2. Créer le profil client par défaut
                PreparedStatement psClient = conn.prepareStatement(
                        "INSERT INTO client (idUtilisateur) VALUES (?)");
                psClient.setInt(1, newUserId);
                psClient.executeUpdate();

                conn.commit();
                envoyerMessage(reponseOk("Inscription réussie.", null));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            envoyerMessage(reponseErreur("Erreur : email probablement déjà utilisé."));
        }
    }

    /**
     * CONNEXION
     * Requête  : { "commande":"CONNEXION", "email":"...", "motDePasse":"..." }
     * Réponse  : OK + { userId, nom, prenom } | ERREUR
     */
    private void traiterConnexion(Map<String, Object> req) {
        String email    = (String) req.get("email");
        String password = (String) req.get("motDePasse");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.idUtilisateur, u.password, u.nom, u.prenom, c.id_client " +
                    "FROM utilisateur u LEFT JOIN client c ON u.idUtilisateur = c.idUtilisateur " +
                    "WHERE u.email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                this.userId    = rs.getInt("idUtilisateur");
                this.clientId  = rs.getInt("id_client");
                this.userEmail = email;

                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                data.put("nom", rs.getString("nom"));
                data.put("prenom", rs.getString("prenom"));
                envoyerMessage(reponseOk("Bienvenue !", data));
            } else {
                envoyerMessage(reponseErreur("Identifiants incorrects."));
            }
        } catch (SQLException e) {
            envoyerMessage(reponseErreur("Erreur technique : " + e.getMessage()));
        }
    }

    private void traiterDeconnexion() {
        this.userId = -1; this.clientId = -1; this.userEmail = null;
        envoyerMessage(reponseOk("Déconnecté.", null));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRODUITS (schema: produit, categorie)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * LISTE_PRODUITS
     * Requête  : { "commande":"LISTE_PRODUITS" }
     * Réponse  : OK + List<Map> [ { id, nom, prix, stock, categorie, image }, ... ]
     */
    private void traiterListeProduits() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            // On joint avec categorie pour avoir le nom
            String sql = "SELECT p.*, c.nom as nom_categorie FROM produit p " +
                         "JOIN categorie c ON p.id_categorie = c.id_categorie WHERE p.stock > 0";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            List<Map<String, Object>> produits = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id",        rs.getInt("id_produit"));
                p.put("nom",       rs.getString("nom"));
                p.put("prix",      rs.getDouble("prix"));
                p.put("stock",     rs.getInt("stock"));
                p.put("categorie", rs.getString("nom_categorie"));
                p.put("image",     rs.getString("image_url"));
                produits.add(p);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("produits", produits);
            envoyerMessage(reponseOk("Liste reçue", data));
        } catch (SQLException e) {
            envoyerMessage(reponseErreur("Erreur produits."));
        }
    }

    private void traiterDetailsProduit(Map<String, Object> req) {
        Integer id = (Integer) req.get("produitId");
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM produit WHERE id_produit = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> d = new HashMap<>();
                d.put("id", rs.getInt("id_produit"));
                d.put("nom", rs.getString("nom"));
                d.put("description", rs.getString("description"));
                d.put("prix", rs.getDouble("prix"));
                d.put("stock", rs.getInt("stock"));
                envoyerMessage(reponseOk("Détails", d));
            }
        } catch (SQLException e) { envoyerMessage(reponseErreur("Erreur")); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PANIER (schema: panier, ligne_panier)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * AJOUTER_PANIER
     * Requête  : { "commande":"AJOUTER_PANIER", "produitId": 42, "quantite": 2 }
     * Réponse  : OK | ERREUR
     */
    private void traiterAjouterPanier(Map<String, Object> req) {
        if (userId == -1) { envoyerMessage(reponseErreur("Connexion requise")); return; }
        int pId = (int) req.get("produitId");
        int qte = (int) req.get("quantite");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            // 1. Récupérer ou créer le panier 'ouvert'
            PreparedStatement psP = conn.prepareStatement("SELECT id_panier FROM panier WHERE idUtilisateur = ? AND statut = 'ouvert'");
            psP.setInt(1, userId);
            ResultSet rsP = psP.executeQuery();
            int panierId;
            if (rsP.next()) {
                panierId = rsP.getInt("id_panier");
            } else {
                PreparedStatement psC = conn.prepareStatement("INSERT INTO panier (idUtilisateur) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                psC.setInt(1, userId);
                psC.executeUpdate();
                ResultSet rsK = psC.getGeneratedKeys();
                rsK.next();
                panierId = rsK.getInt(1);
            }

            // 2. Ajouter/Update ligne_panier
            PreparedStatement psL = conn.prepareStatement(
                "INSERT INTO ligne_panier (id_panier, id_produit, quantite) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantite = quantite + ?");
            psL.setInt(1, panierId);
            psL.setInt(2, pId);
            psL.setInt(3, qte);
            psL.setInt(4, qte);
            psL.executeUpdate();

            envoyerMessage(reponseOk("Ajouté au panier", null));
        } catch (SQLException e) { envoyerMessage(reponseErreur(e.getMessage())); }
    }

    /**
     * VOIR_PANIER
     * Requête  : { "commande":"VOIR_PANIER" }
     * Réponse  : OK + List आर्टिकल [ { nom, prix, quantite, total } ]
     */
    private void traiterVoirPanier() {
        if (userId == -1) return;
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT p.nom, p.prix, lp.quantite, (p.prix * lp.quantite) as total " +
                         "FROM panier pa JOIN ligne_panier lp ON pa.id_panier = lp.id_panier " +
                         "JOIN produit p ON lp.id_produit = p.id_produit " +
                         "WHERE pa.idUtilisateur = ? AND pa.statut = 'ouvert'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            List<Map<String, Object>> articles = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> a = new HashMap<>();
                a.put("nom", rs.getString("nom"));
                a.put("prix", rs.getDouble("prix"));
                a.put("quantite", rs.getInt("quantite"));
                a.put("total", rs.getDouble("total"));
                articles.add(a);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("articles", articles);
            envoyerMessage(reponseOk("Panier", data));
        } catch (SQLException e) { envoyerMessage(reponseErreur("Erreur")); }
    }

    private void traiterSupprimerPanier(Map<String, Object> req) {
        // Logique de suppression d'une ligne spécifique (lp)
        envoyerMessage(reponseOk("Non implémenté", null));
    }

    private void traiterViderPanier() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("DELETE lp FROM ligne_panier lp " +
                "JOIN panier p ON lp.id_panier = p.id_panier WHERE p.idUtilisateur = ? AND p.statut = 'ouvert'");
            ps.setInt(1, userId);
            ps.executeUpdate();
            envoyerMessage(reponseOk("Vidé", null));
        } catch (SQLException e) { envoyerMessage(reponseErreur("Erreur")); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  COMMANDES (schema: commande, ligne_commande, paiement)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * VALIDER_COMMANDE
     * Effectue le paiement, crée la commande et les lignes de commande, décrémente les stocks.
     */
    private void traiterValiderCommande(Map<String, Object> req) {
        if (clientId == -1) { envoyerMessage(reponseErreur("Profil client manquant")); return; }
        String methode = (String) req.getOrDefault("methodePaiement", "CARTE");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            try {
                // 1. Calculer le total du panier actuel
                String sqlP = "SELECT p.id_produit, p.prix, lp.quantite FROM panier pa " +
                              "JOIN ligne_panier lp ON pa.id_panier = lp.id_panier " +
                              "JOIN produit p ON lp.id_produit = p.id_produit " +
                              "WHERE pa.idUtilisateur = ? AND pa.statut = 'ouvert'";
                PreparedStatement psP = conn.prepareStatement(sqlP);
                psP.setInt(1, userId);
                ResultSet rsP = psP.executeQuery();
                
                double total = 0;
                List<int[]> items = new ArrayList<>();
                while(rsP.next()){
                    total += rsP.getDouble("prix") * rsP.getInt("quantite");
                    items.add(new int[]{rsP.getInt("id_produit"), rsP.getInt("quantite"), (int)(rsP.getDouble("prix")*100)});
                }

                if (items.isEmpty()) throw new SQLException("Panier vide");

                // 2. Créer Paiement
                PreparedStatement psPay = conn.prepareStatement(
                    "INSERT INTO paiement (montant, methode_paiement, statut_paiement, date_paiement) VALUES (?, ?, 'succes', NOW())", 
                    Statement.RETURN_GENERATED_KEYS);
                psPay.setDouble(1, total);
                psPay.setString(2, methode);
                psPay.executeUpdate();
                ResultSet rsK = psPay.getGeneratedKeys(); rsK.next();
                int payId = rsK.getInt(1);

                // 3. Créer Commande
                PreparedStatement psCmd = conn.prepareStatement(
                    "INSERT INTO commande (id_client, id_paiement, status) VALUES (?, ?, 'validee')", 
                    Statement.RETURN_GENERATED_KEYS);
                psCmd.setInt(1, clientId);
                psCmd.setInt(2, payId);
                psCmd.executeUpdate();
                ResultSet rsC = psCmd.getGeneratedKeys(); rsC.next();
                int cmdId = rsC.getInt(1);

                // 4. Lignes commandes & Stock
                for(int[] item : items) {
                    PreparedStatement psL = conn.prepareStatement(
                        "INSERT INTO ligne_commande (id_commande, id_produit, quantite, prix_unitaire) VALUES (?, ?, ?, ?)");
                    psL.setInt(1, cmdId); psL.setInt(2, item[0]); psL.setInt(3, item[1]); psL.setDouble(4, item[2]/100.0);
                    psL.executeUpdate();
                    
                    PreparedStatement psS = conn.prepareStatement("UPDATE produit SET stock = stock - ? WHERE id_produit = ?");
                    psS.setInt(1, item[1]); psS.setInt(2, item[0]); psS.executeUpdate();
                }

                // 5. Fermer le panier
                PreparedStatement psF = conn.prepareStatement("UPDATE panier SET statut = 'ferme' WHERE idUtilisateur = ? AND statut = 'ouvert'");
                psF.setInt(1, userId); psF.executeUpdate();

                conn.commit();
                envoyerMessage(reponseOk("Commande validée !", null));

                // Notification UDP
                server.diffuserNotification("CONFIRMATION_COMMANDE|REF:" + cmdId, socket.getInetAddress(), 9092);

            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { envoyerMessage(reponseErreur(e.getMessage())); }
    }

    private void traiterHistoriqueCommandes() {
        if (clientId == -1) return;
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id_commande, status, date_commande FROM commande WHERE id_client = ? ORDER BY date_commande DESC");
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            List<Map<String, Object>> cmds = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id_commande"));
                m.put("statut", rs.getString("status"));
                m.put("date", rs.getString("date_commande"));
                cmds.add(m);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("commandes", cmds);
            envoyerMessage(reponseOk("Historique", data));
        } catch (SQLException e) { envoyerMessage(reponseErreur("Erreur")); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILS
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, Object> reponseOk(String msg, Map<String, Object> data) {
        Map<String, Object> r = new HashMap<>(); r.put("statut", "OK"); r.put("message", msg);
        if (data != null) r.put("data", data); return r;
    }

    private Map<String, Object> reponseErreur(String msg) {
        Map<String, Object> r = new HashMap<>(); r.put("statut", "ERREUR"); r.put("message", msg); return r;
    }

    public synchronized void envoyerMessage(Object objet) {
        try {
            if (out != null) {
                out.writeObject(objet);
                out.flush();
                out.reset();
            }
        } catch (IOException e) { System.err.println("[HANDLER] Erreur envoi : " + e.getMessage()); }
    }

    public void fermerConnexion() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) { }
    }
}
