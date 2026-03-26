package com.chrionline.server.service;

import com.chrionline.server.dao.WishlistDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WishlistService {

    public Map<String, Object> handleAjouterWishlist(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            int userId = (Integer) req.get("userId");
            int produitId = (Integer) req.get("produitId");

            WishlistDAO.add(userId, produitId);

            reponse.put("statut", "OK");
            reponse.put("message", "Produit ajouté à la wishlist");
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", "Erreur lors de l'ajout à la wishlist : " + e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleSupprimerWishlist(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            int userId = (Integer) req.get("userId");
            int produitId = (Integer) req.get("produitId");

            WishlistDAO.remove(userId, produitId);

            reponse.put("statut", "OK");
            reponse.put("message", "Produit retiré de la wishlist");
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", "Erreur lors de la suppression de la wishlist : " + e.getMessage());
        }
        return reponse;
    }

    public Map<String, Object> handleGetWishlist(Map<String, Object> req) {
        Map<String, Object> reponse = new HashMap<>();
        try {
            int userId = (Integer) req.get("userId");

            List<Integer> produitIds = WishlistDAO.findByUser(userId);

            reponse.put("statut", "OK");
            reponse.put("wishlistIds", produitIds);
        } catch (Exception e) {
            reponse.put("statut", "ERREUR");
            reponse.put("message", "Erreur lors de la récupération de la wishlist : " + e.getMessage());
        }
        return reponse;
    }
}
