package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.client.view.ConfirmationView;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.chrionline.shared.utils.PasswordValidator;
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
    private final Stage         stage;
    private final TextField     dateNaissanceField;

    public InscriptionController(
            TextField nom, TextField prenom, TextField email, TextField tel,
            PasswordField mdp, PasswordField mdpConf,
            TextField rue, TextField ville, TextField cp, TextField pays,
            Label msg, Stage stage, TextField dateNaissance
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
        this.stage        = stage;
        this.dateNaissanceField = dateNaissance;
    }

    public void inscrire(String captchaToken) {
        System.out.println("[DEBUG] Tentative d'inscription avec validation de mot de passe fort...");

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
        String dob    = dateNaissanceField.getText().trim();

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

        // ── Vérification Force du Mot de Passe ────────────────
        if (!com.chrionline.shared.utils.PasswordValidator.estFort(mdp, nom, prenom, dob)) {
            erreur("Mot de passe trop faible. Il ne doit pas contenir votre nom, prénom ou date de naissance, et doit être complexe (8+ chars, majuscule, chiffre, spécial).");
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
        req.put("date_naissance", dob);
        req.put("recaptchaToken", captchaToken);

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

            if ("OK".equals(rep.get("statut")) || "EN_ATTENTE".equals(rep.get("statut"))) {
                succes("✓ " + rep.get("message"));
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> {
                        try {
                            new ConfirmationView().start(stage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }).start();
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