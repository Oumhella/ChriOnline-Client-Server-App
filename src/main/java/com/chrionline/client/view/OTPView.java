package com.chrionline.client.view;

import com.chrionline.client.controller.ConnexionController;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class OTPView {

    private static final String CREME        = "#FAF7F2";
    private static final String CREME_CARD   = "#FFFEFB";
    private static final String CREME_INPUT  = "#F5EFE8";
    private static final String SAUGE        = "#A8C4B0";
    private static final String SAUGE_DARK   = "#6B9E7A";
    private static final String TERRACOTTA   = "#C96B4A";
    private static final String TERRA_HOVER  = "#A0522D";
    private static final String BRUN         = "#3E2C1E";
    private static final String BRUN_MED     = "#6B4F3A";
    private static final String BRUN_LIGHT   = "#9A7B65";
    private static final String BORDER       = "#E8E0D5";

    private final String userEmail;
    private final ConnexionController parentController;

    public OTPView(String email, ConnexionController controller) {
        this.userEmail = email;
        this.parentController = controller;
    }

    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Vérification 2FA");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + CREME + ";");

        // Décoration de fond
        Circle bg1 = new Circle(200);
        bg1.setFill(Color.web(SAUGE, 0.2));
        StackPane.setAlignment(bg1, Pos.TOP_RIGHT);
        StackPane.setMargin(bg1, new Insets(-100, -100, 0, 0));

        VBox card = new VBox(30);
        card.setMaxWidth(450);
        card.setMaxHeight(550);
        card.setPadding(new Insets(50));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 20;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(BRUN, 0.1));
        shadow.setRadius(40);
        shadow.setOffsetY(10);
        card.setEffect(shadow);

        // Header
        Text title = new Text("Vérification");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        title.setFill(Color.web(BRUN));

        Text desc = new Text("Un code de sécurité a été envoyé à :\n" + userEmail);
        desc.setFont(Font.font("Georgia", 14));
        desc.setFill(Color.web(BRUN_LIGHT));
        desc.setTextAlignment(TextAlignment.CENTER);

        Rectangle trait = new Rectangle(50, 3, Color.web(TERRACOTTA));

        // Champ OTP (On peut faire un champ stylisé ou 6 petits carrés)
        // Pour la simplicité et l'efficacité, un champ centralisé avec gros texte
        TextField otpField = new TextField();
        otpField.setPromptText("000000");
        otpField.setAlignment(Pos.CENTER);
        otpField.setFont(Font.font("Monospaced", FontWeight.BOLD, 36));
        otpField.setStyle("-fx-background-color: " + CREME_INPUT + "; " +
                          "-fx-border-color: " + BORDER + "; " +
                          "-fx-border-radius: 10; -fx-background-radius: 10; " +
                          "-fx-padding: 15; -fx-text-fill: " + BRUN_MED + "; " +
                          "-fx-letter-spacing: 12px;");
        
        // Limiter à 6 chiffres et ne permettre que les chiffres
        otpField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) otpField.setText(newV.replaceAll("[^\\d]", ""));
            if (otpField.getText().length() > 6) otpField.setText(otpField.getText().substring(0, 6));
        });

        Label msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", 13));
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(TextAlignment.CENTER);

        Button btnVerify = new Button("Vérifier le code");
        btnVerify.setMaxWidth(Double.MAX_VALUE);
        btnVerify.setCursor(javafx.scene.Cursor.HAND);
        btnVerify.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 16;");
        
        btnVerify.setOnMouseEntered(e -> btnVerify.setStyle("-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 16;"));
        btnVerify.setOnMouseExited(e -> btnVerify.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-padding: 15; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 16;"));

        // Timer Label
        Label timerLabel = new Label("Chargement du timer...");
        timerLabel.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        timerLabel.setTextFill(Color.web(BRUN_LIGHT));
        VBox.setMargin(timerLabel, new Insets(10, 0, 0, 0));

        Button btnBack = new Button("Retour à la connexion");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN_LIGHT + "; -fx-cursor: hand; -fx-font-size: 13; -fx-underline: true;");
        btnBack.setOnAction(e -> new ConnexionView().start(stage));

        btnVerify.setOnAction(e -> {
            String code = otpField.getText();
            if (code.length() != 6) {
                msgLabel.setText("Veuillez entrer les 6 chiffres du code.");
                msgLabel.setTextFill(Color.web(TERRACOTTA));
                return;
            }
            parentController.validerOTP(userEmail, code, msgLabel);
        });

        card.getChildren().addAll(title, trait, desc, otpField, msgLabel, btnVerify, timerLabel, btnBack);
        
        // Démarrage du compte à rebours
        demarrerCountdownOTP(timerLabel, btnVerify);
        
        root.getChildren().addAll(bg1, card);

        Scene scene = new Scene(root, 1100, 800);
        stage.setScene(scene);
        
        // Animation
        card.setOpacity(0);
        card.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }

    private void demarrerCountdownOTP(Label timerLabel, Button btnVerify) {
        final int[] temps = {300}; // 5 minutes en secondes
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            temps[0]--;
            int mins = temps[0] / 60;
            int secs = temps[0] % 60;
            timerLabel.setText(String.format("Le code expire dans %02d:%02d", mins, secs));
            
            if (temps[0] <= 10) {
                timerLabel.setTextFill(Color.web(TERRACOTTA));
            }

            if (temps[0] <= 0) {
                timerLabel.setText("Code expiré. Veuillez recommencer.");
                btnVerify.setDisable(true);
            }
        }));
        timeline.setCycleCount(300);
        timeline.play();
    }
}
