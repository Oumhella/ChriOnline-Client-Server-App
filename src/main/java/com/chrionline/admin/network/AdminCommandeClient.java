package com.chrionline.admin.network;

import com.chrionline.shared.dto.CommandeDTO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCommandeClient {

    // IMPORTANT : Remplacez par le port/IP de votre serveur si vous avez changé 12345
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 12345; 

    @SuppressWarnings("unchecked")
    public List<CommandeDTO> fetchAllCommandes() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Map<String, Object> request = new HashMap<>();
            request.put("commande", "GET_ALL_ORDERS");
            out.writeObject(request);

            Map<String, Object> response = (Map<String, Object>) in.readObject();
            if ("OK".equals(response.get("statut"))) {
                return (List<CommandeDTO>) response.get("commandes");
            }
        } catch (Exception e) {
            System.err.println("Erreur de connexion Serveur (GET_ALL_ORDERS) : " + e.getMessage());
        }
        return List.of(); // Liste vide par défaut au lieu de null
    }

    @SuppressWarnings("unchecked")
    public boolean updateStatus(String idCommande, String nouveauStatut) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Map<String, Object> request = new HashMap<>();
            request.put("commande", "UPDATE_ORDER_STATUS");
            request.put("idCommande", idCommande);
            request.put("statut", nouveauStatut);
            out.writeObject(request);

            Map<String, Object> response = (Map<String, Object>) in.readObject();
            return "OK".equals(response.get("statut"));
        } catch (Exception e) {
            System.err.println("Erreur réseau (UPDATE_ORDER_STATUS) : " + e.getMessage());
        }
        return false;
    }
}
