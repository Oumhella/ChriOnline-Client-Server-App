package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import javafx.scene.control.*;
import java.util.*;

public class InscriptionController {

    private final TextField     nomField;
    private final TextField     prenomField;
    private final TextField     emailField;
    private final TextField     telField;
    private final PasswordField mdpField;
    private final PasswordField mdpConfField;
    private final TextField     rueField;
    private final TextField     villeField;
    private final TextField     cpField;
    private final TextField     paysField;
    private final Label         msgLabel;

    public InscriptionController(
            TextField nom, TextField prenom, TextField email, TextField tel,
            PasswordField mdp, PasswordField mdpConf,
            TextField rue, TextField ville, TextField cp, TextField pays,
            Label msg
    ) {
        this.nomField     = nom;
        this.prenomField  = prenom;
        this.emailField   = email;
        this.telField     = tel;
        this.mdpField     = mdp;
        this.mdpConfField = mdpConf;
        this.rueField     = rue;
        this.villeField   = ville;
        this.cpField      = cp;
        this.paysField    = pays;
        this.msgLabel     = msg;
    }

    public void inscrire() {

        String nom    = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email  = emailField.getText().trim();
        String tel    = telField.getText().trim();
        String mdp    = mdpField.getText();
        String mdpC   = mdpConfField.getText();
        String rue    = rueField.getText().trim();
        String ville  = villeField.getText().trim();
        String cp     = cpField.getText().trim();
        String pays   = paysField.getText().trim();

        // ── Validation ────────────────────────────────────────
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            erreur("Les champs marqués * sont obligatoires.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            erreur("Adresse email invalide.");
            return;
        }
        if (!mdp.equals(mdpC)) {
            erreur("Les mots de passe ne correspondent pas.");
            return;
        }
        if (mdp.length() < 6) {
            erreur("Mot de passe trop court (minimum 6 caractères).");
            return;
        }

        // ── Construction de la requête ─────────────────────────
        Map<String, Object> req = new HashMap<>();
        req.put("commande",    "INSCRIPTION");

        // Table utilisateur
        req.put("nom",         nom);
        req.put("prenom",      prenom);
        req.put("email",       email);
        req.put("mdp",         mdp);

        // Table client
        req.put("telephone", tel);

        // Table adresse
        req.put("rue",  rue);
        req.put("ville", ville);
        req.put("code_postal",  cp);
        req.put("pays",  pays);

        System.out.println("[CLIENT] Envoi inscription : " + req);

        // ── Envoi au serveur ───────────────────────────────────
        try {
            Client client = Client.getInstance("localhost", 12345);
            client.connecter();
            client.envoyerRequete(req);

            @SuppressWarnings("unchecked")
            Map<String, Object> rep = (Map<String, Object>) client.lireReponse();
            System.out.println("[CLIENT] Réponse : " + rep);

            if ("OK".equals(rep.get("statut"))) {
                succes("✓ Compte créé avec succès !");
                viderChamps();
            } else {
                erreur((String) rep.get("message"));
            }

        } catch (Exception ex) {
            erreur("Erreur serveur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void viderChamps() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        telField.clear();
        mdpField.clear();
        mdpConfField.clear();
        rueField.clear();
        villeField.clear();
        cpField.clear();
        paysField.clear();
    }

    private void erreur(String m) {
        msgLabel.setStyle("-fx-text-fill: #B03A2E;");
        msgLabel.setText("✗  " + m);
    }

    private void succes(String m) {
        msgLabel.setStyle("-fx-text-fill: #7A9E8A;");
        msgLabel.setText(m);
    }
}