package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.client.view.ConnexionView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class MdpOublieController {

    private final TextField     emailField;
    private final TextField     tokenField;
    private final PasswordField mdpField;
    private final PasswordField mdpConfField;
    private final Label         msgLabel;
    private final Stage         stage;
    private final Runnable      onCodeSent;

    public MdpOublieController(
            TextField email, TextField token, PasswordField mdp, PasswordField mdpConf,
            Label msg, Stage stage, Runnable onCodeSent
    ) {
        this.emailField   = email;
        this.tokenField   = token;
        this.mdpField     = mdp;
        this.mdpConfField = mdpConf;
        this.msgLabel     = msg;
        this.stage        = stage;
        this.onCodeSent   = onCodeSent;
    }

    public void demanderCode() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            erreur("Veuillez saisir votre email.");
            return;
        }

        msgLabel.setText("Envoi en cours...");
        msgLabel.setStyle("-fx-text-fill: #6B4F3A;");

        new Thread(() -> {
            try {
                Client client = Client.getInstance("localhost", 12345);
                client.connecter();

                Map<String, Object> req = new HashMap<>();
                req.put("commande", "OUBLIER_MOT_DE_PASSE");
                req.put("email", email);

                client.envoyerRequete(req);
                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        succes("Code envoyé ! Vérifiez votre email.");
                        if (onCodeSent != null) onCodeSent.run();
                    } else {
                        erreur((String) rep.get("message"));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> erreur("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    public void reinitialiser() {
        String token = tokenField.getText().trim();
        String mdp   = mdpField.getText();
        String mdpC  = mdpConfField.getText();

        if (token.isEmpty() || mdp.isEmpty()) {
            erreur("Veuillez remplir tous les champs.");
            return;
        }
        if (!mdp.equals(mdpC)) {
            erreur("Les mots de passe ne correspondent pas.");
            return;
        }
        if (mdp.length() < 6) {
            erreur("Mot de passe trop court.");
            return;
        }

        msgLabel.setText("Réinitialisation...");
        msgLabel.setStyle("-fx-text-fill: #6B4F3A;");

        new Thread(() -> {
            try {
                Client client = Client.getInstance("localhost", 12345);
                client.connecter();

                Map<String, Object> req = new HashMap<>();
                req.put("commande", "REINITIALISER_MDP");
                req.put("token",    token);
                req.put("nouveauMdp", mdp);

                client.envoyerRequete(req);
                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        succes("Mot de passe modifié avec succès !");
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
