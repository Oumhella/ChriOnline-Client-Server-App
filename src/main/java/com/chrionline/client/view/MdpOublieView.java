package com.chrionline.client.view;

import com.chrionline.client.controller.MdpOublieController;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MdpOublieView extends Application {

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

    private TextField     emailField;
    private TextField     tokenField;
    private PasswordField mdpField;
    private PasswordField mdpConfField;
    private Label         msgLabel;

    private VBox paneStep1;
    private VBox paneStep2;
    private StackPane cardContainer;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Récupérer mon mot de passe");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + CREME + ";");

        Circle bg1 = new Circle(220, Color.web(TERRACOTTA, 0.05));
        StackPane.setAlignment(bg1, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bg1, new Insets(0, 0, -100, -100));

        cardContainer = new StackPane();
        cardContainer.setMaxWidth(480);
        cardContainer.setMaxHeight(600);
        cardContainer.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 20;");
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(BRUN, 0.12));
        shadow.setRadius(45);
        cardContainer.setEffect(shadow);

        buildStep1(stage);
        buildStep2(stage);

        cardContainer.getChildren().add(paneStep1);

        root.getChildren().addAll(bg1, cardContainer);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
            stage.getScene().getStylesheets().clear();
        }
        if (!stage.isShowing()) stage.show();

        // Animation d'entrée
        cardContainer.setOpacity(0);
        cardContainer.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(500), cardContainer);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), cardContainer);
        slide.setFromY(20); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void buildStep1(Stage stage) {
        paneStep1 = new VBox(25);
        paneStep1.setPadding(new Insets(50, 45, 50, 45));
        paneStep1.setAlignment(Pos.CENTER);

        Text title = new Text("Mot de passe oublié ?");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.web(BRUN));

        Text info = new Text("Entrez votre adresse email pour recevoir un code de réinitialisation.");
        info.setFont(Font.font("Georgia", 14));
        info.setFill(Color.web(BRUN_LIGHT));
        info.setWrappingWidth(360);
        info.setTextAlignment(TextAlignment.CENTER);

        emailField = inputField("votre@email.com");
        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", 13));

        Button btnEnvoyer = new Button("Envoyer le code");
        btnEnvoyer.setMaxWidth(Double.MAX_VALUE);
        btnEnvoyer.setStyle(btnPrimary(TERRACOTTA));
        btnEnvoyer.setCursor(javafx.scene.Cursor.HAND);
        btnEnvoyer.setOnMouseEntered(e -> btnEnvoyer.setStyle(btnPrimary(TERRA_HOVER)));
        btnEnvoyer.setOnMouseExited(e -> btnEnvoyer.setStyle(btnPrimary(TERRACOTTA)));

        Hyperlink retour = new Hyperlink("Retour à la connexion");
        retour.setTextFill(Color.web(BRUN_LIGHT));
        retour.setOnAction(e -> {
            try { new ConnexionView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        paneStep1.getChildren().addAll(title, info, labelField("Email", emailField), msgLabel, btnEnvoyer, retour);

        MdpOublieController ctrl = new MdpOublieController(
                emailField, tokenField, mdpField, mdpConfField, 
                msgLabel, stage, this::goToStep2
        );
        btnEnvoyer.setOnAction(e -> ctrl.demanderCode());
    }

    private void buildStep2(Stage stage) {
        paneStep2 = new VBox(20);
        paneStep2.setPadding(new Insets(50, 45, 50, 45));
        paneStep2.setAlignment(Pos.CENTER);

        Text title = new Text("Nouveau mot de passe");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.web(BRUN));

        tokenField = new TextField();
        tokenField.setPromptText("Code reçu (ex: UUID)");
        tokenField.setStyle(fieldStyle());

        mdpField     = new PasswordField();
        mdpConfField = new PasswordField();

        Button btnValider = new Button("Réinitialiser");
        btnValider.setMaxWidth(Double.MAX_VALUE);
        btnValider.setStyle(btnPrimary("#6B9E7A")); // SAUGE_DARK
        btnValider.setCursor(javafx.scene.Cursor.HAND);

        paneStep2.getChildren().addAll(
                title, 
                labelField("Code secret", tokenField),
                labelField("Nouveau mot de passe", creerChampMdp(mdpField, "Nouveau mot de passe")),
                labelField("Confirmation", creerChampMdp(mdpConfField, "Confirmer le mot de passe")),
                msgLabel,
                btnValider
        );

        MdpOublieController ctrl = new MdpOublieController(
                emailField, tokenField, mdpField, mdpConfField, 
                msgLabel, stage, null
        );
        btnValider.setOnAction(e -> ctrl.reinitialiser());
    }

    private void goToStep2() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), paneStep1);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            cardContainer.getChildren().setAll(paneStep2);
            paneStep2.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), paneStep2);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private VBox labelField(String label, Node field) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(BRUN_MED));
        return new VBox(8, lbl, field);
    }

    private TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private HBox creerChampMdp(PasswordField pf, String prompt) {
        pf.setPromptText(prompt);
        pf.setStyle(fieldStyle());
        
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        tf.setManaged(false);
        tf.setVisible(false);
        
        tf.textProperty().bindBidirectional(pf.textProperty());
        
        Button btnEye = new Button("👁️");
        btnEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " + BRUN_LIGHT + "; -fx-font-size: 14;");
        
        btnEye.setOnAction(e -> {
            if (pf.isVisible()) {
                pf.setVisible(false); pf.setManaged(false);
                tf.setVisible(true); tf.setManaged(true);
                btnEye.setText("🙈");
            } else {
                pf.setVisible(true); pf.setManaged(true);
                tf.setVisible(false); tf.setManaged(false);
                btnEye.setText("👁️");
            }
        });
        
        StackPane stack = new StackPane(pf, tf);
        HBox.setHgrow(stack, Priority.ALWAYS);
        
        HBox container = new HBox(stack, btnEye);
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");
        
        pf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding:12;");
        tf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding:12;");
        
        return container;
    }

    private String fieldStyle() {
        return "-fx-background-color:" + CREME_INPUT + "; -fx-border-color:" + BORDER + "; -fx-border-radius:8; -fx-padding:12;";
    }

    private String btnPrimary(String color) {
        if (color.startsWith("#")) {
             return "-fx-background-color:" + color + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;";
        }
        return "-fx-background-color:" + color + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;";
    }

    public static void main(String[] args) { launch(args); }
}
