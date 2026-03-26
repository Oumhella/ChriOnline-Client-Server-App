package com.chrionline.server.core;

import com.chrionline.server.dao.UserDAO;
import com.chrionline.server.service.AuthenticationService;
import com.chrionline.server.service.PanierService;
import com.chrionline.server.service.ProduitService;
import com.chrionline.shared.models.User;
import com.chrionline.server.dao.CommandeDAO;
import com.chrionline.server.dao.LigneCommandeDAO;
import com.chrionline.server.service.CommandeService;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.shared.enums.StatutCommande;
import com.chrionline.database.DatabaseConnection;
import java.sql.Connection;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Gère la communication réseau (TCP/UDP) avec un client connecté.
 * Cette classe se concentre sur le transport et la gestion de session de base.
 * La logique métier sera déléguée aux DAOs/Controllers plus tard.
 */
@SuppressWarnings("unchecked")
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private final AuthenticationService authService;
    private final ProduitService produitService;
    private final PanierService panierService;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // État de la session client
    private int    userId   = -1;
    private String userEmail = null;
    private String userRole      = null;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.produitService = new ProduitService();
        this.authService = new AuthenticationService();
        this.panierService = new PanierService();
    }

    // ─── Gestion de la Connexion TCP ──────────────────────────────────────────

    @Override
    public void run() {
        try {
            // Initialisation des flux
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in  = new ObjectInputStream(socket.getInputStream());

            System.out.println("[HANDLER] Client connecté : " + socket.getInetAddress().getHostAddress());

            Object requete;
            while ((requete = in.readObject()) != null) {
                traiterRequete(requete);
            }

        } catch (EOFException | SocketException e) {
            System.out.println("[HANDLER] Déconnexion du client.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[HANDLER] Erreur réseau : " + e.getMessage());
        } finally {
            server.gererDeconnexion(this);
            fermerConnexion();
        }
    }

    // ─── Dispatcher de Requêtes ───────────────────────────────────────────────

    /**
     * Reçoit un objet (Map) et l'oriente vers le bon traitement.
     */
    @SuppressWarnings("unchecked")
    private void traiterRequete(Object objet) {
        if (!(objet instanceof Map)) {
            envoyerMessage(creerReponse("ERREUR", "Format de protocole invalide."));
            return;
        }

        Map<String, Object> req = (Map<String, Object>) objet;
        String commande = (String) req.getOrDefault("commande", "INCONNUE");

        System.out.println("[HANDLER] Reçu : " + commande);

        // TODO : Plus tard, ces appels seront redirigés vers des Services ou DAOs
        switch (commande) {
            case "CONNEXION" -> handleConnexion(req);
            case "INSCRIPTION" -> handleInscription(req);
            case "LISTE_PRODUITS" -> handleListeProduits(req);
            case "DETAIL_PRODUIT", "GET_PRODUIT_BY_ID" -> handleDetailProduit(req);
            case "AJOUTER_WISHLIST"  -> handleAjouterWishlist(req);
            case "SUPPRIMER_WISHLIST"-> handleSupprimerWishlist(req);
            case "LISTE_WISHLIST"    -> handleListeWishlist(req);
            case "CONFIRMER_EMAIL"       -> handleConfirmerEmail(req);
            case "OUBLIER_MOT_DE_PASSE" -> handleOublierMotDePasse(req);
            case "REINITIALISER_MDP"     -> handleReinitialiserMdp(req);
            case "PANIER_GET"            -> envoyerMessage(panierService.getPanier(req));
            case "PANIER_AJOUTER"        -> envoyerMessage(panierService.ajouterProduit(req));
            case "PANIER_MODIFIER_QTE"   -> envoyerMessage(panierService.modifierQuantite(req));
            case "PANIER_RETIRER"        -> envoyerMessage(panierService.retirerProduit(req));
            case "PANIER_VIDER"          -> envoyerMessage(panierService.viderPanier(req));
            case "PANIER_VALIDER"        -> envoyerMessage(panierService.validerPanier(req));
            
            // Admin Produits
            case "AJOUTER_PRODUIT"       -> handleAjouterProduit(req);
            case "MODIFIER_PRODUIT"      -> handleModifierProduit(req);
            case "SUPPRIMER_PRODUIT"     -> handleSupprimerProduit(req);
            case "UPLOAD_IMAGE"          -> handleUploadImage(req);
            case "LISTE_CATEGORIES"      -> handleListeCategories(req);
            case "LISTE_LABELS"          -> handleListeLabels(req);
            case "LISTE_LABEL_VALUES"    -> handleListeLabelValues(req);
            case "AJOUTER_LABEL"         -> handleAjouterLabel(req);
            case "AJOUTER_LABEL_VALUE"   -> handleAjouterLabelValue(req);
            case "SUPPRIMER_LABEL_VALUE" -> handleSupprimerLabelValue(req);
            case "AJOUTER_CATEGORIE"     -> handleAjouterCategorie(req);
            case "MODIFIER_CATEGORIE"    -> handleModifierCategorie(req);
            case "SUPPRIMER_CATEGORIE"    -> handleSupprimerCategorie(req);

            case "GET_ALL_ORDERS",
                 "GET_ORDER_DETAILS",
                 "UPDATE_ORDER_STATUS" -> {
                // /!\ On doit commenter ce bloc jusqu'à ce que `AdminCommandeClient`
                //     envoie son `idUtilisateur` ou maintienne une session, sinon `this.userId`
                //     vaut 0 (car c'est une toute nouvelle socket) et bloque l'affichage !
                /*
                if (!isAdmin()) {
                    envoyerMessage(creerReponse("ERREUR", "Accès refusé : réservé à l'admin"));
                    return;
                }
                */
                handleAdminCommande(commande, req);
            }
            case "ADMIN_LISTE_USERS" -> envoyerMessage(com.chrionline.server.service.AdminUserService.handleListerClients());
            case "ADMIN_CHANGER_STATUT_USER" -> envoyerMessage(com.chrionline.server.service.AdminUserService.handleChangerStatutClient(req));
            // ... autres commandes ...
            default -> envoyerMessage(creerReponse("ERREUR", "Commande non reconnue : " + commande));
        }
    }

    // ─── Placeholders pour la logique (À déplacer en DAO plus tard) ─────────────

    private void handleConnexion(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleConnexion appelée");
        try {
            Map<String, Object> reponse = authService.login(req);

            System.out.println("[HANDLER] Login statut = " + reponse.get("statut"));

            if ("OK".equals(reponse.get("statut"))) {
                Map<String, Object> data = (Map<String, Object>) reponse.get("data");
                this.userId = (int) data.get("userId");
                this.userEmail = (String) data.get("email");
                this.userRole = (String) data.get("role");
            }
            
            envoyerMessage(reponse);
        } catch (Exception e) {
            System.err.println("[HANDLER] Exception handleConnexion : " + e.getMessage());
            e.printStackTrace();
            envoyerMessage(creerReponse("ERREUR", "Erreur technique : " + e.getMessage()));
        }
    }
    private void handleInscription(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleInscription appelée");
        try {
            Map<String, Object> reponse = authService.register(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de l'inscription : " + e.getMessage()));
        }
    }

    private void handleConfirmerEmail(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleConfirmerEmail");
        try {
            Map<String, Object> reponse = authService.confirmerEmail(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur confirmation : " + e.getMessage()));
        }
    }

    private void handleOublierMotDePasse(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleOublierMotDePasse");
        try {
            Map<String, Object> reponse = authService.oublierMotDePasse(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur : " + e.getMessage()));
        }
    }

    private void handleReinitialiserMdp(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleReinitialiserMdp");
        try {
            Map<String, Object> reponse = authService.reinitialiserMotDePasse(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur : " + e.getMessage()));
        }
    }

    private void handlePanierValider(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handlePanierValider");
        try {
            Map<String, Object> reponse = panierService.validerPanier(req);

            // Si la commande est validée avec succès, on notifie les administrateurs via UDP
            if ("OK".equals(reponse.get("statut"))) {
                String ref = (String) reponse.get("reference");
                String messageAdmin = "NOUVELLE_COMMANDE:" + (ref != null ? ref : "Inconnue") + ":Utilisateur " + this.userId;
                server.notifierAdmins(messageAdmin);
            }

            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleListeProduits(Map<String, Object> req) {
        try {

            Map<String, Object> reponse = produitService.handleListeProduits(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération des produits : " + e.getMessage()));
        }
    }

    private void handleDetailProduit(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = produitService.handleGetProduitById(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération du produit : " + e.getMessage()));
        }
    }

    private void handleAjouterWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService().handleAjouterWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleSupprimerWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService().handleSupprimerWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleListeWishlist(Map<String, Object> req) {
        try {
            Map<String, Object> reponse = new com.chrionline.server.service.WishlistService().handleGetWishlist(req);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur réseau : " + e.getMessage()));
        }
    }

    private void handleAjouterProduit(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleAjouterProduit(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleModifierProduit(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleModifierProduit(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleSupprimerProduit(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleSupprimerProduit(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleUploadImage(Map<String, Object> req) {
        try {
            envoyerMessage(produitService.handleUploadImage(req));
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleListeCategories(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeCategories(req));
    }

    private void handleListeLabels(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeLabels(req));
    }

    private void handleListeLabelValues(Map<String, Object> req) {
        envoyerMessage(produitService.handleListeLabelValues(req));
    }

    private void handleAjouterLabel(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterLabel(req));
    }

    private void handleAjouterLabelValue(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterLabelValue(req));
    }

    private void handleSupprimerLabelValue(Map<String, Object> req) {
        envoyerMessage(produitService.handleSupprimerLabelValue(req));
    }

    private void handleAjouterCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleAjouterCategorie(req));
    }

    private void handleModifierCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleModifierCategorie(req));
    }

    private void handleSupprimerCategorie(Map<String, Object> req) {
        envoyerMessage(produitService.handleSupprimerCategorie(req));
    }

    // ─── Gestion UDP ──────────────────────────────────────────────────────────

    /**
     * Envoie une notification via le protocole UDP (délégué au serveur).
     */
    public void envoyerNotificationUDP(String msg, int portClientUDP) {
        server.diffuserNotification(msg, socket.getInetAddress(), portClientUDP);
    }

    // ─── Utilitaires de Communication ─────────────────────────────────────────

    public synchronized void envoyerMessage(Object objet) {
        try {
            if (out != null) {
                out.writeObject(objet);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("[HANDLER] Échec d'envoi client : " + e.getMessage());
        }
    }

    private Map<String, Object> creerReponse(String statut, String message) {
        Map<String, Object> r = new HashMap<>();
        r.put("statut", statut);
        r.put("message", message);
        return r;
    }

    private boolean isAdmin() {
        if (this.userId <= 0) return false;

        try (java.sql.Connection conn = com.chrionline.database.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM admin WHERE idAdmin = ?")) {
            ps.setInt(1, this.userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si présent dans la table admin
            }
        } catch (Exception e) {
            System.err.println("[HANDLER] Erreur isAdmin : " + e.getMessage());
            return false;
        }
    }

    public void fermerConnexion() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignoré
        }
    }


    // ─── Gestion des Commandes Admin ─────────────────────────────────────────

    private void handleAdminCommande(String commande, Map<String, Object> req) {
        switch (commande) {
            case "GET_ALL_ORDERS"      -> handleGetAllOrders(req);
            case "GET_ORDER_DETAILS"   -> handleGetOrderDetails(req);
            case "UPDATE_ORDER_STATUS" -> handleUpdateOrderStatus(req);
        }
    }

    private void handleGetAllOrders(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleGetAllOrders appelée — userId=" + userId + " role=" + userRole);
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn)
            );
            List<CommandeDTO> commandes = service.getAllCommandes();
            System.out.println("[HANDLER] commandes trouvées : " + commandes.size());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("commandes", new java.util.ArrayList<>(commandes)); // ArrayList est sérialisable
            envoyerMessage(reponse);
        } catch (Exception e) {
            System.err.println("[HANDLER] ERREUR handleGetAllOrders : " + e.getMessage());
            e.printStackTrace(); // Stack trace complet dans la console du serveur
            envoyerMessage(creerReponse("ERREUR", e.getClass().getSimpleName() + " : " + e.getMessage()));
        }
    }

    private void handleGetOrderDetails(Map<String, Object> req) {
        try {
            String idCommande = (String) req.get("idCommande");
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn)
            );
            CommandeDTO dto = service.getCommandeById(idCommande);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("commande", dto);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    private void handleUpdateOrderStatus(Map<String, Object> req) {
        try {
            String idCommande    = (String) req.get("idCommande");
            String nouveauStatut = (String) req.get("statut");
            Connection conn = DatabaseConnection.getInstance().getConnection();
            CommandeService service = new CommandeService(
                    new CommandeDAO(conn),
                    new LigneCommandeDAO(conn)
            );
            String resultat = service.updateStatut(idCommande, nouveauStatut);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", resultat.startsWith("SUCCESS") ? "OK" : "ERREUR");
            reponse.put("message", resultat);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    // Getters/Setters session
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getRole() { return userRole; }
    public Socket getSocket() { return socket; }


}
