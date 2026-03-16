package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import javafx.application.Platform;
import javafx.scene.control.*;
import java.util.*;

public class ConnexionController {

    private final TextField emailField;
    private final PasswordField mdpField;
    private final Label msgLabel;

    public ConnexionController(TextField email, PasswordField mdp, Label msg) {
        this.emailField = email;
        this.mdpField = mdp;
        this.msgLabel = msg;
    }

    public void connecter() {
        String email = emailField.getText().trim();
        String mdp = mdpField.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            erreur("Veuillez remplir tous les champs.");
            return;
        }

        msgLabel.setStyle("-fx-text-fill: #6B4F3A;");
        msgLabel.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                Client client = Client.getInstance("localhost", 12345);
                client.connecter();

                Map<String, Object> req = new HashMap<>();
                req.put("commande", "CONNEXION");
                req.put("email", email);
                req.put("mdp", mdp);

                client.envoyerRequete(req);

                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        succes((String) rep.get("message"));
                        // Prochaine étape : redirection vers dashboard
                    } else {
                        erreur((String) rep.get("message"));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> erreur("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private void erreur(String m) {
        msgLabel.setStyle("-fx-text-fill: #C96B4A;");
        msgLabel.setText("✗ " + m);
    }

    private void succes(String m) {
        msgLabel.setStyle("-fx-text-fill: #6B9E7A;");
        msgLabel.setText("✓ " + m);
    }
}
