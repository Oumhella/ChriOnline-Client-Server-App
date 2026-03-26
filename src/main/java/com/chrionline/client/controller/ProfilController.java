package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import java.util.HashMap;
import java.util.Map;

public class ProfilController {

    private Client client;

    public ProfilController() {
        this.client = Client.getInstance();
    }

    public Map<String, Object> getProfil() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "GET_PROFIL");
        return client.envoyerRequeteAttendreReponse(req);
    }

    public Map<String, Object> updateProfil(Map<String, Object> data) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "UPDATE_PROFIL");
        req.put("data", data);
        return client.envoyerRequeteAttendreReponse(req);
    }
}
