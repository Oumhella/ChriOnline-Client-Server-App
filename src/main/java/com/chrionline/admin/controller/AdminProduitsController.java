package com.chrionline.admin.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.Categorie;
import com.chrionline.shared.models.Label;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.Produit;

import java.util.*;

@SuppressWarnings("unchecked")
public class AdminProduitsController {

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

    public List<Produit> getAllProduits() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "LISTE_PRODUITS");
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (List<Produit>) res.get("produits");
        }
        return new ArrayList<>();
    }

    public Produit getProduitById(int id) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "GET_PRODUIT_BY_ID");
        req.put("id", id);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (Produit) res.get("produit");
        }
        return null;
    }

    public int ajouterProduit(Produit p) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_PRODUIT");
        req.put("nom", p.getNom());
        req.put("description", p.getDescription());
        req.put("id_categorie", p.getIdCategorie());
        
        List<Map<String, Object>> fmts = new ArrayList<>();
        for (var f : p.getFormats()) {
            Map<String, Object> fm = new HashMap<>();
            fm.put("prix", f.getPrix());
            fm.put("stock", f.getStock());
            fm.put("stock_alerte", f.getStockAlerte());
            fm.put("image_url", f.getImageUrl());
            List<Integer> lvIds = new ArrayList<>();
            for (var lv : f.getLabelValues()) lvIds.add(lv.getId());
            fm.put("labelValueIds", lvIds);
            fmts.add(fm);
        }
        req.put("formats", fmts);

        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }

    public boolean modifierProduit(Produit p) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "MODIFIER_PRODUIT");
        req.put("id_produit", p.getIdProduit());
        req.put("nom", p.getNom());
        req.put("description", p.getDescription());
        req.put("id_categorie", p.getIdCategorie());
        
        List<Map<String, Object>> fmts = new ArrayList<>();
        for (var f : p.getFormats()) {
            Map<String, Object> fm = new HashMap<>();
            fm.put("id_product_formats", f.getId());
            fm.put("prix", f.getPrix());
            fm.put("stock", f.getStock());
            fm.put("stock_alerte", f.getStockAlerte());
            fm.put("image_url", f.getImageUrl());
            List<Integer> lvIds = new ArrayList<>();
            if (f.getLabelValues() != null) {
                for (var lv : f.getLabelValues()) lvIds.add(lv.getId());
            }
            fm.put("labelValueIds", lvIds);
            fmts.add(fm);
        }
        req.put("formats", fmts);

        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        return res != null && "OK".equals(res.get("statut"));
    }

    public boolean supprimerProduit(int idProduit) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "SUPPRIMER_PRODUIT");
        req.put("id_produit", idProduit);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        return res != null && "OK".equals(res.get("statut"));
    }

    public List<Categorie> getCategories() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "LISTE_CATEGORIES");
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (List<Categorie>) res.get("categories");
        }
        return new ArrayList<>();
    }

    public String uploadImage(byte[] data, String ext) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "UPLOAD_IMAGE");
        req.put("data", data);
        req.put("extension", ext);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (String) res.get("url");
        }
        return null;
    }

    public List<Label> getLabelsByCategorie(int idCat) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "LISTE_LABELS");
        req.put("id_categorie", idCat);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (List<Label>) res.get("labels");
        }
        return new ArrayList<>();
    }

    public List<LabelValue> getLabelValues(int idLabel) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "LISTE_LABEL_VALUES");
        req.put("id_label", idLabel);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (List<LabelValue>) res.get("values");
        }
        return new ArrayList<>();
    }

    public int ajouterLabel(int idCat, String nom) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_LABEL");
        req.put("id_categorie", idCat);
        req.put("nom", nom);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }

    public int ajouterLabelValue(int idLabel, String valeur) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_LABEL_VALUE");
        req.put("id_label", idLabel);
        req.put("valeur", valeur);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }
}
