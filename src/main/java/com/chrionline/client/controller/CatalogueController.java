package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.Produit;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogueController {

    private final Client client;
    private int userId;

    /**
     * Constructeur principal — userId obligatoire pour que l'ajout au panier fonctionne.
     */
    public CatalogueController(int userId) {
        this.client = Client.getInstance("localhost", 12345);
        this.userId = userId;
    }

    /** Constructeur sans userId (panier désactivé — affichage seul) */
    public CatalogueController() {
        this(-1);
    }

    // ── Récupérer les produits ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Produit> recupererProduits() {
        try {
            client.connecter();
            Map<String, Object> req = new HashMap<>();
            req.put("commande", "LISTE_PRODUITS");
            client.envoyerRequete(req);

            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            if ("OK".equals(rep.get("statut"))) {
                List<Produit> produits = (List<Produit>) rep.get("produits");
                System.out.println("[CatalogueController] " + produits.size() + " produit(s) reçu(s)");
                return produits;
            } else {
                System.err.println("[CatalogueController] Erreur : " + rep.get("message"));
            }
        } catch (Exception e) {
            System.err.println("[CatalogueController] Erreur réseau : " + e.getMessage());
        }
        return new ArrayList<>();
    }


    // ── Ajouter au panier ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void ajouterAuPanier(Produit p, int idFormat) {
        // Vérifier que l'utilisateur est connecté
        if (userId == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Non connecté",
                    "Vous devez être connecté pour ajouter au panier.");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> req = new HashMap<>();
                req.put("commande",         "PANIER_AJOUTER");
                req.put("idUtilisateur",    userId);
                req.put("idProductFormats", idFormat);
                req.put("quantite",         1);

                client.connecter();
                client.envoyerRequete(req);

                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        String nomProd = (p != null) ? p.getNom() : "Produit";
                        afficherAlerte(Alert.AlertType.INFORMATION, "Panier",
                                "\"" + nomProd + "\" ajouté au panier ✓");
                    } else {
                        afficherAlerte(Alert.AlertType.WARNING,
                                "Impossible d'ajouter", (String) rep.get("message"));
                    }
                });

            } catch (Exception e) {
                System.err.println("[CatalogueController] Erreur ajout panier : " + e.getMessage());
                Platform.runLater(() ->
                        afficherAlerte(Alert.AlertType.ERROR, "Erreur réseau", e.getMessage())
                );
            }
        }).start();
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


    // ── Getters / Setters ────────────────────────────────────────────────

    public void setUserId(int userId) { this.userId = userId; }
    public int  getUserId()           { return userId; }

    // ── Utilitaire alerte ────────────────────────────────────────────────

    private void afficherAlerte(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}