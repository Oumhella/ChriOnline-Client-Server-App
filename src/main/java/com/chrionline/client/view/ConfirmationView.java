package com.chrionline.client.view;

import com.chrionline.client.controller.ConfirmationController;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ConfirmationView extends Application {

    private static final String CREME        = "#FAF7F2";
    private static final String CREME_CARD   = "#FFFEFB";
    private static final String CREME_INPUT  = "#F5EFE8";
    private static final String SAUGE_LIGHT  = "#D0E2D8";
    private static final String TERRACOTTA   = "#C96B4A";
    private static final String TERRA_HOVER  = "#A0522D";
    private static final String BRUN         = "#3E2C1E";
    private static final String BRUN_MED     = "#6B4F3A";
    private static final String BRUN_LIGHT   = "#9A7B65";
    private static final String BORDER       = "#E8E0D5";

    private TextField codeField;
    private Label     msgLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Confirmation de compte");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + CREME + ";");

        Circle bg1 = new Circle(200, Color.web(SAUGE_LIGHT, 0.3));
        StackPane.setAlignment(bg1, Pos.TOP_RIGHT);
        StackPane.setMargin(bg1, new Insets(-80, -80, 0, 0));

        VBox card = new VBox(30);
        card.setMaxWidth(450);
        card.setMaxHeight(500);
        card.setPadding(new Insets(50, 40, 50, 40));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 20;");
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(BRUN, 0.1));
        shadow.setRadius(40);
        card.setEffect(shadow);

        Text title = new Text("Vérifiez votre email");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web(BRUN));

        Text info = new Text("Veuillez saisir le code de confirmation à 8 caractères envoyé à votre adresse email.");
        info.setFont(Font.font("Georgia", 14));
        info.setFill(Color.web(BRUN_LIGHT));
        info.setWrappingWidth(350);
        info.setTextAlignment(TextAlignment.CENTER);

        codeField = new TextField();
        codeField.setPromptText("Ex: ABCDE123");
        codeField.setMaxWidth(300);
        codeField.setAlignment(Pos.CENTER);
        codeField.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        codeField.setStyle("-fx-background-color:" + CREME_INPUT + "; -fx-border-color:" + BORDER + "; -fx-border-radius:8; -fx-padding:10;");

        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", 13));

        Button btnValider = new Button("Confirmer mon compte");
        btnValider.setMaxWidth(300);
        btnValider.setStyle("-fx-background-color:" + TERRACOTTA + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;");
        btnValider.setCursor(javafx.scene.Cursor.HAND);
        btnValider.setOnMouseEntered(e -> btnValider.setStyle("-fx-background-color:" + TERRA_HOVER + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;"));
        btnValider.setOnMouseExited(e -> btnValider.setStyle("-fx-background-color:" + TERRACOTTA + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;"));

        Hyperlink retour = new Hyperlink("Retour à la connexion");
        retour.setFont(Font.font("Georgia", 13));
        retour.setTextFill(Color.web(BRUN_LIGHT));

        ConfirmationController ctrl = new ConfirmationController(codeField, msgLabel, stage);
        btnValider.setOnAction(e -> ctrl.confirmer());
        retour.setOnAction(e -> {
            try { new ConnexionView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(title, info, codeField, msgLabel, btnValider, retour);
        root.getChildren().addAll(bg1, card);

        Scene scene = new Scene(root, 980, 720);
        stage.setScene(scene);
        stage.show();

        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(20); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    public static void main(String[] args) { launch(args); }
}
