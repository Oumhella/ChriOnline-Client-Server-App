package com.chrionline.server.core;

import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.models.User;

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

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // État de la session client
    private int    userId   = -1;
    private String userEmail = null;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
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
            // ... autres commandes ...
            default -> envoyerMessage(creerReponse("ERREUR", "Commande non reconnue : " + commande));
        }
    }

    // ─── Placeholders pour la logique (À déplacer en DAO plus tard) ─────────────

    private void handleConnexion(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleConnexion appelée");
        try {
            Map<String, Object> reponse = UserDAO.connexion(req);
            
            if ("OK".equals(reponse.get("statut"))) {
                Map<String, Object> data = (Map<String, Object>) reponse.get("data");
                this.userId = (int) data.get("userId");
                this.userEmail = (String) data.get("email");
            }
            
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur technique : " + e.getMessage()));
        }
    }
    private void handleInscription(Map<String, Object> req) {
        System.out.println("[HANDLER] >>> handleInscription appelée");
        System.out.println("[HANDLER] Données reçues : " + req);
        try {
            Map<String, Object> reponse = UserDAO.inscrire(req);
            System.out.println("[HANDLER] Réponse DAO : " + reponse);
            envoyerMessage(reponse);
        } catch (Throwable t) {
            System.err.println("[HANDLER] EXCEPTION FATALE : " + t.getMessage());
            t.printStackTrace();
            envoyerMessage(creerReponse("ERREUR", t.getMessage()));
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

    public void fermerConnexion() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignoré
        }
    }


    // Getters/Setters session
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


}
