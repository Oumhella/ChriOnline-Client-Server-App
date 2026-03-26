package com.chrionline.admin.network;

import com.chrionline.shared.dto.CommandeDTO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client réseau admin — connexion directe sans authentification.
 * L'accès admin est sécurisé au niveau réseau (localhost uniquement).
 * Le check isAdmin() côté serveur est désactivé pour cet usage interne.
 */
public class AdminCommandeClient {

    private final String SERVER_IP  = "127.0.0.1";
    private final int    SERVER_PORT = 12345;

    // ───── GET_ALL_ORDERS ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<CommandeDTO> fetchAllCommandes() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            Map<String, Object> request = new HashMap<>();
            request.put("commande", "GET_ALL_ORDERS");
            out.writeObject(request);
            out.flush();

            Map<String, Object> response = (Map<String, Object>) in.readObject();
            System.out.println("[ADMIN CLIENT] GET_ALL_ORDERS statut=" + response.get("statut")
                + " message=" + response.get("message"));

            if ("OK".equals(response.get("statut"))) {
                List<CommandeDTO> liste = (List<CommandeDTO>) response.get("commandes");
                System.out.println("[ADMIN CLIENT] nb commandes reçues : " + (liste != null ? liste.size() : "null"));
                return liste != null ? liste : List.of();
            }

        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur GET_ALL_ORDERS : " + e.getMessage());
            e.printStackTrace();
        }
        return List.of();
    }

    // ───── UPDATE_ORDER_STATUS ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public boolean updateStatus(String idCommande, String nouveauStatut) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            Map<String, Object> request = new HashMap<>();
            request.put("commande", "UPDATE_ORDER_STATUS");
            request.put("idCommande", idCommande);
            request.put("statut", nouveauStatut);
            out.writeObject(request);
            out.flush();

            Map<String, Object> response = (Map<String, Object>) in.readObject();
            System.out.println("[ADMIN CLIENT] UPDATE_ORDER_STATUS statut=" + response.get("statut"));
            return "OK".equals(response.get("statut"));

        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur UPDATE_ORDER_STATUS : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ───── GET_ORDER_DETAILS ──────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public CommandeDTO fetchOrderDetails(String idCommande) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            Map<String, Object> request = new HashMap<>();
            request.put("commande", "GET_ORDER_DETAILS");
            request.put("idCommande", idCommande);
            out.writeObject(request);
            out.flush();

            Map<String, Object> response = (Map<String, Object>) in.readObject();
            if ("OK".equals(response.get("statut"))) {
                return (CommandeDTO) response.get("commande");
            }
        } catch (Exception e) {
            System.err.println("[ADMIN CLIENT] Erreur GET_ORDER_DETAILS : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}