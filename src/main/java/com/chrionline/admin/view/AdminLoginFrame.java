package com.chrionline.admin.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

/**
 * Fenêtre de connexion Administrateur cachée déclenchée par un raccourci clavier.
 */
public class AdminLoginFrame extends Stage {
    private static final int PORT = 12345;
    private static final String HOST = "127.0.0.1";

    public AdminLoginFrame() {
        setTitle("Accès Admin RESTREINT");
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #3E2C1E;"); // Couleur BRUN de la charte

        Label lblTitle = new Label("Secure Admin Login");
        lblTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.web("#FDFBF7"));

        Label lblStatus = new Label();
        lblStatus.setFont(Font.font("Georgia", 13));
        lblStatus.setTextFill(Color.web("#C96B4A"));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Identifiant Administrateur");
        txtUsername.setStyle("-fx-background-color: #F5EFE8; -fx-padding: 10px; -fx-font-family: 'Georgia';");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Clé / Mot de passe");
        txtPassword.setStyle("-fx-background-color: #F5EFE8; -fx-padding: 10px; -fx-font-family: 'Georgia';");

        Button btnLogin = new Button("Authentification");
        btnLogin.setStyle("-fx-background-color: #C96B4A; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-family: 'Georgia'; -fx-font-weight: bold;");
        btnLogin.setCursor(javafx.scene.Cursor.HAND);
        
        btnLogin.setOnAction(e -> {
            lblStatus.setText("Vérification en cours...");
            try {
                if (authentifierAdmin(txtUsername.getText(), txtPassword.getText())) {
                    lblStatus.setText("Authentification réussie !");
                    AdminDashboardView dashboard = new AdminDashboardView();
                    dashboard.start(new Stage());
                    this.close();
                } else {
                    lblStatus.setText("Échec: identifiants invalides ou droits insuffisants.");
                }
            } catch (Exception ex) {
                lblStatus.setText("Erreur système : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(lblTitle, txtUsername, txtPassword, btnLogin, lblStatus);

        Scene scene = new Scene(root, 400, 350);
        setScene(scene);
    }

    private boolean authentifierAdmin(String username, String pass) {
        Map<String, Object> reqAuth = new HashMap<>();
        reqAuth.put("commande", "LOGIN_ADMIN");
        reqAuth.put("email", username);
        reqAuth.put("mdp", pass);

        try {
            com.chrionline.client.network.Client client = com.chrionline.client.network.Client.getInstance("localhost", 12345);
            try { client.connecter(); } catch(Exception ignored) {}
            
            Map<String, Object> finalRep = client.envoyerRequeteAttendreReponse(reqAuth);
            if (finalRep != null && "OK".equals(finalRep.get("statut"))) {
                Map<String, Object> data = (Map<String, Object>) finalRep.get("data");
                com.chrionline.client.session.SessionManager.getInstance().setUser(data);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
