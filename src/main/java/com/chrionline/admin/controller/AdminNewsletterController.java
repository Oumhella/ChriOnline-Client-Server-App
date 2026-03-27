package com.chrionline.admin.controller;

import com.chrionline.client.network.Client;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour la gestion de l'envoi de newsletters côté Admin.
 */
public class AdminNewsletterController {

    private Client getClient() {
        return Client.getInstance("localhost", 12345);
    }

    public Map<String, Object> envoyerNewsletter(String sujet, String corps) {
        try {
            Client c = getClient();
            c.connecter();

            Map<String, Object> req = new HashMap<>();
            req.put("commande", "ENVOYER_NEWSLETTER");
            req.put("sujet", sujet);
            req.put("corps", corps);

            c.envoyerRequete(req);
            return (Map<String, Object>) c.lireReponse();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("statut", "ERREUR");
            err.put("message", "Erreur réseau : " + e.getMessage());
            return err;
        }
    }
}
