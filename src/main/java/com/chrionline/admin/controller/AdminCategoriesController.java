package com.chrionline.admin.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.Categorie;
import java.util.*;

@SuppressWarnings("unchecked")
public class AdminCategoriesController {

    private Client getClient() {
        return Client.getInstance("localhost", 12345);
    }

    private Object sendRequest(Map<String, Object> req) {
        try {
            Client c = getClient();
            c.connecter();
            c.envoyerRequete(req);
            return c.lireReponse();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Categorie> getAllCategories() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "LISTE_CATEGORIES");
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (List<Categorie>) res.get("categories");
        }
        return new ArrayList<>();
    }

    public int ajouterCategorie(Categorie c) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_CATEGORIE");
        req.put("nom", c.getNom());
        req.put("description", c.getDescription());
        if (c.getIdParent() > 0) req.put("id_parent", c.getIdParent());
        
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }

    public boolean modifierCategorie(Categorie c) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "MODIFIER_CATEGORIE");
        req.put("id_categorie", c.getId());
        req.put("nom", c.getNom());
        req.put("description", c.getDescription());
        if (c.getIdParent() > 0) req.put("id_parent", c.getIdParent());
        
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        return res != null && "OK".equals(res.get("statut"));
    }

    public boolean supprimerCategorie(int id) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "SUPPRIMER_CATEGORIE");
        req.put("id_categorie", id);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        return res != null && "OK".equals(res.get("statut"));
    }
}
