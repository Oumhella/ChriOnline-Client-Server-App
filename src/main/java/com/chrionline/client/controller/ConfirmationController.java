package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.client.view.ConnexionView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class ConfirmationController {

    private final TextField codeField;
    private final Label msgLabel;
    private final Stage stage;

    public ConfirmationController(TextField codeField, Label msgLabel, Stage stage) {
        this.codeField = codeField;
        this.msgLabel = msgLabel;
        this.stage = stage;
    }

    public void confirmer() {
        String code = codeField.getText().trim().toUpperCase();

        if (code.isEmpty()) {
            erreur("Veuillez saisir le code de confirmation.");
            return;
        }

        msgLabel.setText("Vérification...");
        msgLabel.setStyle("-fx-text-fill: #6B4F3A;");

        new Thread(() -> {
            try {
                Client client = Client.getInstance("localhost", 12345);
                client.connecter();

                Map<String, Object> req = new HashMap<>();
                req.put("commande", "CONFIRMER_EMAIL");
                req.put("token", code);

                client.envoyerRequete(req);
                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        succes((String) rep.get("message") + " Redirection...");
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    new ConnexionView().start(stage);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
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
