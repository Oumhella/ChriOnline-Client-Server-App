package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.Produit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogueController {

    private final Client client;

    public CatalogueController() {
        this.client = Client.getInstance("localhost", 12345);
    }

    @SuppressWarnings("unchecked")
    public List<Produit> recupererProduits() {
        try {
            client.connecter();
            Map<String, Object> req = new HashMap<>();
            req.put("commande", "LISTE_PRODUITS");
            
            client.envoyerRequete(req);
            
            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            
            if ("OK".equals(rep.get("statut"))) {
                return (List<Produit>) rep.get("produits");
            } else {
                System.err.println("[CatalogueController] Erreur : " + rep.get("message"));
            }
        } catch (Exception e) {
            System.err.println("[CatalogueController] Erreur réseau : " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Produit recupererProduitDetail(int id) {
        try {
            client.connecter();
            Map<String, Object> req = new HashMap<>();
            req.put("commande", "DETAIL_PRODUIT");
            req.put("id", id);

            client.envoyerRequete(req);

            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

            if ("OK".equals(rep.get("statut"))) {
                return (Produit) rep.get("produit");
            } else {
                System.err.println("[CatalogueController] Erreur détail : " + rep.get("message"));
            }
        } catch (Exception e) {
            System.err.println("[CatalogueController] Erreur réseau (détail) : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void ajouterAuPanier(Produit p) {
        // Logique panier à venir
        System.out.println("[CatalogueController] Ajout au panier : " + p.getNom());
    }
}
