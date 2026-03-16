package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.Map;

public class InscriptionController {

    private final TextField     nomField;
    private final TextField     prenomField;
    private final TextField     emailField;
    private final PasswordField mdpField;
    private final PasswordField mdpConfField;
    private final Label         messageLabel;

    public InscriptionController(TextField nom,TextField prenom, TextField email,
                                 PasswordField mdp, PasswordField mdpConf,
                                 Label message) {
        this.nomField    = nom;
        this.prenomField = prenom;
        this.emailField  = email;
        this.mdpField    = mdp;
        this.mdpConfField = mdpConf;
        this.messageLabel = message;
    }

    public void inscrire() {
        String nom   = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp   = mdpField.getText();
        String mdpC  = mdpConfField.getText();

        // ── Validation côté client ─────────────────────────────
        if (nom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            afficherErreur("Tous les champs sont obligatoires.");
            return;
        }
        if (!email.contains("@")) {
            afficherErreur("Email invalide.");
            return;
        }
        if (!mdp.equals(mdpC)) {
            afficherErreur("Les mots de passe ne correspondent pas.");
            return;
        }
        if (mdp.length() < 6) {
            afficherErreur("Mot de passe trop court (min. 6 caractères).");
            return;
        }

        // ── Envoi au serveur ───────────────────────────────────
        try {
            Client client = Client.getInstance("localhost", 12345);
            client.connecter();

            Map<String, Object> requete = new HashMap<>();
            requete.put("commande", "INSCRIPTION");
            requete.put("nom",   nom);
            requete.put("prenom", prenomField.getText().trim());
            requete.put("email", email);
            requete.put("mdp",   mdp);  // le serveur hashera avec jBCrypt

            client.envoyerRequete(requete);

            @SuppressWarnings("unchecked")
            Map<String, Object> reponse = (Map<String, Object>) client.lireReponse();

            if ("OK".equals(reponse.get("statut"))) {
                afficherSucces("Compte créé avec succès !");
            } else {
                afficherErreur((String) reponse.get("message"));
            }

        } catch (Exception ex) {
            afficherErreur("Erreur connexion serveur : " + ex.getMessage());
        }
    }

    private void afficherErreur(String msg) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(msg);
    }

    private void afficherSucces(String msg) {
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText(msg);
    }
}