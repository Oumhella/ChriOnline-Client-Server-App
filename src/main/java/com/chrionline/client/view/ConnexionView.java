package com.chrionline.client.view;

import com.chrionline.client.controller.ConnexionController;
import com.chrionline.client.view.MdpOublieView;
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

public class ConnexionView extends Application {

    private static final String CREME        = "#FAF7F2";
    private static final String CREME_CARD   = "#FFFEFB";
    private static final String CREME_INPUT  = "#F5EFE8";
    private static final String SAUGE_LIGHT  = "#D0E2D8";
    private static final String SAUGE        = "#A8C4B0";
    private static final String SAUGE_DARK   = "#6B9E7A";
    private static final String TERRACOTTA   = "#C96B4A";
    private static final String TERRA_HOVER  = "#A0522D";
    private static final String BRUN         = "#3E2C1E";
    private static final String BRUN_MED     = "#6B4F3A";
    private static final String BRUN_LIGHT   = "#9A7B65";
    private static final String GOLD_LIGHT   = "#E8CFA0";
    private static final String BORDER       = "#E8E0D5";

    private TextField     emailField;
    private PasswordField mdpField;
    private Label         msgLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Connexion");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + CREME + ";");

        // Cercles de fond décoratifs (comme InscriptionView)
        Circle bg1 = new Circle(240);
        bg1.setFill(Color.web(SAUGE_LIGHT, 0.35));
        StackPane.setAlignment(bg1, Pos.TOP_LEFT);
        StackPane.setMargin(bg1, new Insets(-120, 0, 0, -120));

        Circle bg2 = new Circle(160);
        bg2.setFill(Color.web(TERRACOTTA, 0.07));
        StackPane.setAlignment(bg2, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bg2, new Insets(0, -70, -70, 0));

        HBox card = new HBox();
        card.setMaxWidth(980);
        card.setMaxHeight(700);
        card.setMinHeight(600);
        card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 20;");
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(BRUN, 0.16));
        shadow.setRadius(50);
        shadow.setOffsetY(10);
        card.setEffect(shadow);
        StackPane.setMargin(card, new Insets(30));

        VBox left  = buildLeft();
        VBox right = buildRight(stage);
        HBox.setHgrow(right, Priority.ALWAYS);

        card.getChildren().addAll(left, right);
        root.getChildren().addAll(bg1, bg2, card);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
            stage.getScene().getStylesheets().clear(); // On s'assure de nettoyer les styles admin si on revient de là
        }
        
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        if (!stage.isShowing()) stage.show();

        // Animation d'entrée
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(600), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), card);
        slide.setFromY(20); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private VBox buildLeft() {
        VBox left = new VBox();
        left.setPrefWidth(280);
        left.setMinWidth(280);
        left.setMaxWidth(280);
        left.setStyle("-fx-background-color: linear-gradient(to bottom, #A8C4B0 0%, #6B9E7A 55%, #4E7A5C 100%); -fx-background-radius: 20 0 0 20;");
        left.setPadding(new Insets(48, 32, 48, 32));
        left.setAlignment(Pos.CENTER_LEFT);

        Text welcome = new Text("Heureux de vous revoir");
        welcome.setFont(Font.font("Georgia", FontPosture.ITALIC, 14));
        welcome.setFill(Color.web("#FFFFFF", 0.7));

        Text brand = new Text("ChriOnline");
        brand.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        brand.setFill(Color.web(BRUN));

        Rectangle trait = new Rectangle(40, 3, Color.web(TERRACOTTA));
        VBox.setMargin(trait, new Insets(10, 0, 20, 0));

        Text desc = new Text("Connectez-vous pour accéder à vos commandes et vos produits favoris.");
        desc.setFont(Font.font("Georgia", 14));
        desc.setFill(Color.web(BRUN, 0.8));
        desc.setWrappingWidth(230);

        left.getChildren().addAll(welcome, brand, trait, desc);
        return left;
    }

    private VBox buildRight(Stage stage) {
        VBox right = new VBox(25);
        right.setPadding(new Insets(50, 60, 50, 60));
        right.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("Se connecter");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        title.setFill(Color.web(BRUN));

        VBox emailBox = labelField("Adresse email", emailField = inputField("votre@email.com"));
        
        // Système de mot de passe avec "œil"
        mdpField = new PasswordField();
        mdpField.setPromptText("••••••••");
        mdpField.setStyle(fieldStyle());
        
        TextField mdpVisibleField = new TextField();
        mdpVisibleField.setPromptText("••••••••");
        mdpVisibleField.setStyle(fieldStyle());
        mdpVisibleField.setManaged(false);
        mdpVisibleField.setVisible(false);
        
        // Synchronisation bidirectionnelle
        mdpVisibleField.textProperty().bindBidirectional(mdpField.textProperty());
        
        Button btnEye = new Button("👁️");
        btnEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " + BRUN_LIGHT + "; -fx-font-size: 16;");
        btnEye.setPadding(new Insets(0, 10, 0, 0));
        
        btnEye.setOnAction(e -> {
            if (mdpField.isVisible()) {
                mdpField.setVisible(false);
                mdpField.setManaged(false);
                mdpVisibleField.setVisible(true);
                mdpVisibleField.setManaged(true);
                btnEye.setText("🙈");
            } else {
                mdpField.setVisible(true);
                mdpField.setManaged(true);
                mdpVisibleField.setVisible(false);
                mdpVisibleField.setManaged(false);
                btnEye.setText("👁️");
            }
        });
        
        StackPane mdpStack = new StackPane(mdpField, mdpVisibleField);
        HBox mdpContainer = new HBox(mdpStack, btnEye);
        HBox.setHgrow(mdpStack, Priority.ALWAYS);
        mdpContainer.setAlignment(Pos.CENTER_RIGHT);
        mdpContainer.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 6; -fx-border-color: " + BORDER + "; -fx-border-radius: 6;");
        
        // Ajustement des styles internes pour ne pas doubler les bordures
        mdpField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12;");
        mdpVisibleField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12;");
        
        VBox mdpBox = labelField("Mot de passe", mdpContainer);

        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", 13));

        Button btnLogin = new Button("Connexion");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle(btnStyle(TERRACOTTA));
        btnLogin.setCursor(javafx.scene.Cursor.HAND);
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(btnStyle(TERRA_HOVER)));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle(btnStyle(TERRACOTTA)));

        Hyperlink linkOublie = new Hyperlink("Mot de passe oublié ?");
        linkOublie.setFont(Font.font("Georgia", 11));
        linkOublie.setTextFill(Color.web(BRUN_LIGHT));
        linkOublie.setUnderline(true);
        linkOublie.setPadding(new Insets(-10, 0, 0, 0));
        linkOublie.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(linkOublie, new Insets(-10, 0, 0, 0));

        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER);
        Text txt = new Text("Pas encore membre ?");
        txt.setFill(Color.web(BRUN_LIGHT));
        Hyperlink link = new Hyperlink("Créer un compte");
        link.setTextFill(Color.web(SAUGE_DARK));
        link.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        footer.getChildren().addAll(txt, link);

        // Action contrôleur
        ConnexionController ctrl = new ConnexionController(emailField, mdpField, msgLabel, stage, btnLogin);
        btnLogin.setOnAction(e -> ctrl.connecter());
        link.setOnAction(e -> {
            try {
                new InscriptionView().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        linkOublie.setOnAction(e -> {
            try {
                new MdpOublieView().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        right.getChildren().addAll(title, emailBox, mdpBox, linkOublie, msgLabel, btnLogin, footer);
        return right;
    }

    private VBox labelField(String label, Node field) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(BRUN_MED));
        VBox box = new VBox(8, lbl, field);
        return box;
    }

    private TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private String fieldStyle() {
        return "-fx-background-color:" + CREME_INPUT + ";" +
               "-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:12;";
    }

    private String btnStyle(String color) {
        return "-fx-background-color:" + color + "; -fx-text-fill:white; -fx-padding:14; -fx-background-radius:8; -fx-font-weight:bold;";
    }

    public static void main(String[] args) { launch(args); }
}
