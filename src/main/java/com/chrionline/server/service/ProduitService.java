package com.chrionline.server.service;

import com.chrionline.shared.models.Produit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProduitService {
    private void handleListeProduits(Map<String, Object> req) {
        try {
            List<Produit> produits = com.chrionline.server.dao.ProduitDAO.findAll();
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("statut", "OK");
            reponse.put("produits", produits);
            envoyerMessage(reponse);
        } catch (Exception e) {
            envoyerMessage(creerReponse("ERREUR", "Erreur lors de la récupération des produits : " + e.getMessage()));
        }
    }

}
