package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.client.view.CatalogueView;
import com.chrionline.client.view.ConfirmationView;
import com.chrionline.admin.view.AdminDashboardView;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.*;

public class ConnexionController {

    private final TextField     emailField;
    private final PasswordField mdpField;
    private final Label         msgLabel;
    private final Stage         stage;

    public ConnexionController(TextField email, PasswordField mdp, Label msg, Stage stage) {
        this.emailField = email;
        this.mdpField   = mdp;
        this.msgLabel   = msg;
        this.stage      = stage;
    }

    @SuppressWarnings("unchecked")
    public void connecter() {
        String email = emailField.getText().trim();
        String mdp   = mdpField.getText();

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

                        Map<String, Object> data = (Map<String, Object>) rep.get("data");
                        String role = data != null ? (String) data.getOrDefault("role", "client") : "client";

                        // ✅ Stockage dans le SessionManager
                        com.chrionline.client.session.SessionManager.getInstance().setUser(data);

                        // ✅ Enregistrement du port UDP auprès du serveur
                        client.enregistrerUDP();

                        System.out.println("[ConnexionController] userId=" + 
                                com.chrionline.client.session.SessionManager.getInstance().getUserId() + " role=" + role);

                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    if ("admin".equals(role)) {
                                        new com.chrionline.admin.view.AdminDashboardView().start(stage);
                                    } else {
                                        // CatalogueView ne prend plus d'identifiant en paramètre, il lit le SessionManager
                                        new CatalogueView().start(stage);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    erreur("Erreur de redirection : " + ex.getMessage());
                                }
                            });
                        }).start();

                    } else if ("EN_ATTENTE".equals(rep.get("statut"))) {
                        erreur((String) rep.get("message"));
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try { new ConfirmationView().start(stage); }
                                catch (Exception ex) { ex.printStackTrace(); }
                            });
                        }).start();
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