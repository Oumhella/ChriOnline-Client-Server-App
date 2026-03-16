package com.chrionline.client.view;

import com.chrionline.client.controller.InscriptionController;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class InscriptionView extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ChriOnline — Inscription");

        // ── Titre ──────────────────────────────────────────────
        Text titre = new Text("Créer un compte");
        titre.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        // ── Champs du formulaire ───────────────────────────────
        TextField nomField    = new TextField();
        nomField.setPromptText("Nom complet");
        TextField prenomField = new TextField();
        prenomField.setPromptText("Prénom");

        TextField emailField  = new TextField();
        emailField.setPromptText("Adresse email");

        PasswordField mdpField    = new PasswordField();
        mdpField.setPromptText("Mot de passe");

        PasswordField mdpConfField = new PasswordField();
        mdpConfField.setPromptText("Confirmer le mot de passe");

        // ── Label de retour (erreur / succès) ──────────────────
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        // ── Bouton Inscription ─────────────────────────────────
        Button btnInscrire = new Button("S'inscrire");
        btnInscrire.setMaxWidth(Double.MAX_VALUE);
        btnInscrire.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 10;");

        // ── Lien vers la connexion ─────────────────────────────
        Hyperlink lienConnexion = new Hyperlink("Déjà un compte ? Se connecter");

        // ── Layout ────────────────────────────────────────────
        VBox form = new VBox(12,
                titre,
                new Label("Nom :"), nomField,
                new Label("Email :"), emailField,
                new Label("Mot de passe :"), mdpField,
                new Label("Confirmer :"), mdpConfField,
                messageLabel,
                btnInscrire,
                lienConnexion
        );
        form.setPadding(new Insets(30));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(400);

        StackPane root = new StackPane(form);
        root.setStyle("-fx-background-color: #f5f5f5;");

        // ── Contrôleur ────────────────────────────────────────
        InscriptionController controller = new InscriptionController(
                nomField,  prenomField,emailField, mdpField, mdpConfField, messageLabel
        );
        btnInscrire.setOnAction(e -> controller.inscrire());
        lienConnexion.setOnAction(e -> primaryStage.close()); // ou ouvrir ConnexionView

        primaryStage.setScene(new Scene(root, 460, 500));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}