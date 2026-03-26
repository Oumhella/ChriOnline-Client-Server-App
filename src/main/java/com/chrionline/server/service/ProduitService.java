package com.chrionline.server.service;

import com.chrionline.database.DatabaseConnection;
import com.chrionline.server.dao.ProduitDAO;
import com.chrionline.shared.models.Label;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProduitService {

    public Map<String, Object> handleListeProduits(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            List<Produit> produits = ProduitDAO.findAll();
            reponse.put("statut", "OK");
            reponse.put("produits", produits);
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleGetProduitById(Map<String, Object> req) {

        Map<String, Object> reponse = new HashMap<>();

        try {
            Object idObj = req.get("id");

            if (idObj == null) {
                reponse.put("statut", "ERREUR");
                reponse.put("message", "ID manquant");
                return reponse;
            }

            int id = Integer.parseInt(idObj.toString());

            Produit produit = ProduitDAO.findById(id);

            if (produit != null) {
                reponse.put("statut", "OK");
                reponse.put("produit", produit);
            } else {
                reponse.put("statut", "NOT_FOUND");
                reponse.put("message", "Produit introuvable");
            }

        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", e.getMessage());
        }

        return reponse;
    }
}