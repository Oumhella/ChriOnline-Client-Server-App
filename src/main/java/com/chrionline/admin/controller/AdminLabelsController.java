package com.chrionline.admin.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.Label;
import com.chrionline.shared.models.LabelValue;
import java.util.*;

@SuppressWarnings("unchecked")
public class AdminLabelsController {

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

    public int ajouterLabel(Label l) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_LABEL");
        req.put("nom", l.getNom());
        req.put("id_categorie", l.getIdCategorie());
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }

    public int ajouterLabelValue(LabelValue lv) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "AJOUTER_LABEL_VALUE");
        req.put("valeur", lv.getValeur());
        req.put("id_label", lv.getLabel().getId());
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        if (res != null && "OK".equals(res.get("statut"))) {
            return (int) res.get("id");
        }
        return -1;
    }

    public boolean supprimerLabelValue(int id) {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "SUPPRIMER_LABEL_VALUE");
        req.put("id_labelValues", id);
        Map<String, Object> res = (Map<String, Object>) sendRequest(req);
        return res != null && "OK".equals(res.get("statut"));
    }
}
