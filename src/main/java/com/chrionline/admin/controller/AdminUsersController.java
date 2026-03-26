package com.chrionline.admin.controller;

import com.chrionline.client.network.Client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUsersController {

    private final Client client;

    public AdminUsersController() {
        this.client = Client.getInstance("localhost", 12345);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listerClients() {
        try {
            client.connecter();
            Map<String, Object> req = new HashMap<>();
            req.put("commande", "ADMIN_LISTE_USERS");

            client.envoyerRequete(req);

            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            if ("OK".equals(rep.get("statut"))) {
                return (List<Map<String, Object>>) rep.get("data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public boolean changerStatut(int idUtilisateur, String nouveauStatut) {
        try {
            client.connecter();
            Map<String, Object> req = new HashMap<>();
            req.put("commande", "ADMIN_CHANGER_STATUT_USER");
            req.put("idUtilisateur", idUtilisateur);
            req.put("statut", nouveauStatut);

            client.envoyerRequete(req);

            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            return "OK".equals(rep.get("statut"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
