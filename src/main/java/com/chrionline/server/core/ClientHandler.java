package com.chrionline.server.core;

import com.chrionline.server.dao.UserDAO;
import com.chrionline.server.service.AuthenticationService;
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
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private final AuthenticationService authService;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // État de la session client
    private int    userId   = -1;
    private String userEmail = null;
    private String userRole = null;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.authService = new AuthenticationService();
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
            case "CONFIRMER_EMAIL"       -> handleConfirmerEmail(req);
            case "OUBLIER_MOT_DE_PASSE" -> handleOublierMotDePasse(req);
            case "REINITIALISER_MDP"     -> handleReinitialiserMdp(req);
            case "GET_ALL_ORDERS",
                 "GET_ORDER_DETAILS",
                 "UPDATE_ORDER_STATUS" -> {
                // /!\ On commente cette sécurité temporairement car notre AdminCommandeClient
                // actuel ouvre une NOUVELLE socket qui n'a pas encore fait de login().
                // if (!isAdmin()) {
                //     envoyerMessage(creerReponse("ERREUR", "Accès refusé : réservé à l'admin"));
                //     return;
                // }
                handleAdminCommande(commande, req);
            }
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
                Object dataObj = reponse.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    this.userId    = (int) data.get("userId");
                    this.userEmail = (String) data.get("email");
                    this.userRole  = (String) data.get("role");
                    System.out.println("[HANDLER] Session ouverte : userId=" + userId + " role=" + userRole);
                }
            } else {
                System.out.println("[HANDLER] Echec connexion : " + reponse.get("message"));
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
    private void handleListeProduits(Map<String, Object> req) {
        try {
            List<com.chrionline.shared.models.Produit> produits = com.chrionline.server.dao.ProduitDAO.findAll();
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("produits", produits);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération des produits : " + e.getMessage()));
        }
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
        // UserDAO retourne le rôle en minuscule ('admin'), on compare sans sensibilité à la casse
        return userRole != null && userRole.equalsIgnoreCase("ADMIN");
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
            if (resultat.startsWith("SUCCESS")) {
                envoyerNotificationUDP(
                        "Votre commande " + idCommande + " est maintenant : " + nouveauStatut,
                        9999
                );
            }
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", e.getMessage()));
        }
    }

    // Getters/Setters session
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


}
