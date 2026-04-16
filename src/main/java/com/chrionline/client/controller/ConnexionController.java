package com.chrionline.client.controller;

import com.chrionline.client.network.Client;
import com.chrionline.client.view.CatalogueView;
import com.chrionline.client.view.ConfirmationView;
import com.chrionline.client.view.OTPView;
import com.chrionline.admin.view.AdminDashboardView;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class ConnexionController {

    private final TextField     emailField;
    private final PasswordField mdpField;
    private final Label         msgLabel;
    private final Stage         stage;
    private final Button        loginButton;

    public ConnexionController(TextField email, PasswordField mdp, Label msg, Stage stage, Button loginBtn) {
        this.emailField = email;
        this.mdpField   = mdp;
        this.msgLabel   = msg;
        this.stage      = stage;
        this.loginButton = loginBtn;
    }

    @SuppressWarnings("unchecked")
    public void connecter(String captchaToken) {
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
                req.put("recaptchaToken", captchaToken);

                client.envoyerRequete(req);
                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        succes((String) rep.get("message"));

                        Map<String, Object> data = (Map<String, Object>) rep.get("data");
                        String role = data != null ? (String) data.getOrDefault("role", "client") : "client";

                        //  Stockage dans le SessionManager
                        com.chrionline.client.session.SessionManager.getInstance().setUser(data);

                        // ✅ Enregistrement du port UDP auprès du serveur
                        client.enregistrerUDP();

                        System.out.println("[ConnexionController] userId=" + 
                                com.chrionline.client.session.SessionManager.getInstance().getUserId() + " role=" + role);

                        // Enregistrement du port UDP dynamique auprès du serveur
                        try {
                            Map<String, Object> udpReg = new HashMap<>();
                            udpReg.put("commande", "REGISTER_UDP");
                            udpReg.put("udpPort", client.getUdpPort());
                            client.envoyerRequete(udpReg);
                        } catch (Exception e) {
                            System.err.println("[CONNEXION] Échec enregistrement UDP : " + e.getMessage());
                        }

                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    if ("admin".equals(role)) {
                                        new com.chrionline.admin.view.AdminDashboardView().start(stage);
                                    } else {
                                        new com.chrionline.client.view.HomeView().start(stage);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    erreur("Erreur de redirection : " + ex.getMessage());
                                }
                            });
                        }).start();

                    } else if ("REQUIRES_2FA".equals(rep.get("statut"))) {
                        // redirection vers la vue OTP
                        succes((String) rep.get("message"));
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    new OTPView(email, this).start(stage);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    erreur("Erreur de chargement OTP : " + ex.getMessage());
                                }
                            });
                        }).start();
                    } else if ("ERREUR_BLOQUE".equals(rep.get("statut"))) {
                        long    secondes     = ((Number) rep.get("delaySeconds")).longValue();
                        boolean showRecovery = Boolean.TRUE.equals(rep.get("showPasswordRecovery"));
                        lancerCompteAReboursBlocage(secondes, showRecovery);
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

    private void lancerCompteAReboursBlocage(long totalSec, boolean showPasswordRecovery) {
        loginButton.setDisable(true);
        final long[] remaining = {totalSec};

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                loginButton.setDisable(false);
                loginButton.setText("Connexion");
                msgLabel.setText("");
            } else {
                long h = remaining[0] / 3600;
                long m = (remaining[0] % 3600) / 60;
                long s = remaining[0] % 60;

                String timeStr = (h > 0)
                        ? String.format("%02dh %02dm %02ds", h, m, s)
                        : String.format("%02dm %02ds", m, s);
                loginButton.setText("Suspendu (" + timeStr + ")");

                if (showPasswordRecovery) {
                    erreur("Trop de tentatives. Accès suspendu.\nMot de passe oublié ? Utilisez \"Mot de passe oublié\".");
                } else {
                    erreur("Trop de tentatives. Accès suspendu.");
                }
            }
        }));
        timeline.setCycleCount((int) totalSec);
        timeline.play();
    }

    private void erreur(String m) {
        msgLabel.setStyle("-fx-text-fill: #C96B4A;");
        msgLabel.setText("✗ " + m);
    }

    private void succes(String m) {
        msgLabel.setStyle("-fx-text-fill: #6B9E7A;");
        msgLabel.setText("✓ " + m);
    }

    @SuppressWarnings("unchecked")
    public void validerOTP(String email, String codeOTP, Label msgOtp) {
        msgOtp.setStyle("-fx-text-fill: #6B4F3A;");
        msgOtp.setText("Vérification...");

        new Thread(() -> {
            try {
                Client client = Client.getInstance();
                Map<String, Object> req = new HashMap<>();
                req.put("commande", "VERIFIER_OTP");
                req.put("email", email);
                req.put("otp", codeOTP);

                client.envoyerRequete(req);
                Map<String, Object> rep = (Map<String, Object>) client.lireReponse();

                Platform.runLater(() -> {
                    if ("OK".equals(rep.get("statut"))) {
                        msgOtp.setStyle("-fx-text-fill: #6B9E7A;");
                        msgOtp.setText("✓ Code valide ! Connexion...");

                        Map<String, Object> data = (Map<String, Object>) rep.get("data");
                        String role = data != null ? (String) data.getOrDefault("role", "client") : "client";

                        // Stockage session
                        com.chrionline.client.session.SessionManager.getInstance().setUser(data);
                        
                        // Enregistrement UDP
                        client.enregistrerUDP();

                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                try {
                                    if ("admin".equals(role)) {
                                        new AdminDashboardView().start(stage);
                                    } else {
                                        new com.chrionline.client.view.HomeView().start(stage);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }).start();
                    } else {
                        msgOtp.setStyle("-fx-text-fill: #C96B4A;");
                        msgOtp.setText("✗ " + rep.get("message"));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    msgOtp.setStyle("-fx-text-fill: #C96B4A;");
                    msgOtp.setText("✗ Erreur : " + e.getMessage());
                });
            }
        }).start();
    }
}