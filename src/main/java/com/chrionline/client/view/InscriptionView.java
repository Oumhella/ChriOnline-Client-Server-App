package com.chrionline.client.view;

import com.chrionline.client.controller.InscriptionController;
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

public class InscriptionView extends Application {

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
    private static final String GOLD         = "#D4AA70";
    private static final String GOLD_LIGHT   = "#E8CFA0";

    private TextField     nomField;
    private TextField     prenomField;
    private TextField     emailField;
    private TextField     telField;
    private PasswordField mdpField;
    private PasswordField mdpConfField;
    private TextField     rueField;
    private TextField     villeField;
    private TextField     cpField;
    private TextField     paysField;
    private Label         msgLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Créer un compte");

        // ── Fond décoratif ────────────────────────────────────
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + CREME + ";");

        Circle bg1 = new Circle(240);
        bg1.setFill(Color.web(SAUGE_LIGHT, 0.35));
        StackPane.setAlignment(bg1, Pos.TOP_LEFT);
        StackPane.setMargin(bg1, new Insets(-120, 0, 0, -120));

        Circle bg2 = new Circle(160);
        bg2.setFill(Color.web(TERRACOTTA, 0.07));
        StackPane.setAlignment(bg2, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bg2, new Insets(0, -70, -70, 0));

        Circle bg3 = new Circle(60);
        bg3.setFill(Color.web(GOLD, 0.18));
        StackPane.setAlignment(bg3, Pos.TOP_RIGHT);
        StackPane.setMargin(bg3, new Insets(50, 90, 0, 0));

        // ── CARD ──────────────────────────────────────────────
        HBox card = new HBox();
        card.setMaxWidth(980);
        card.setMaxHeight(680);
        card.setMinHeight(580);
        card.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-background-radius: 20;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(BRUN, 0.16));
        shadow.setRadius(50);
        shadow.setOffsetY(10);
        card.setEffect(shadow);
        StackPane.setMargin(card, new Insets(30));

        // ── Panneau gauche (fixe) ─────────────────────────────
        VBox left = buildLeft();

        // ── Panneau droit (scrollable) ────────────────────────
        VBox right = buildRight(stage);
        HBox.setHgrow(right, Priority.ALWAYS);

        card.getChildren().addAll(left, right);
        root.getChildren().addAll(bg1, bg2, bg3, card);

        // Animation d'entrée
        card.setOpacity(0);
        card.setTranslateY(20);

        Scene scene = new Scene(root, 980, 700);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(20); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    // ════════════════════════════════════════════════════════
    //  PANNEAU GAUCHE
    // ════════════════════════════════════════════════════════
    private VBox buildLeft() {
        VBox left = new VBox();
        left.setPrefWidth(280);
        left.setMinWidth(280);
        left.setMaxWidth(280);
        left.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #A8C4B0 0%, #6B9E7A 55%, #4E7A5C 100%);" +
                        "-fx-background-radius: 20 0 0 20;"
        );
        left.setPadding(new Insets(48, 32, 48, 32));
        left.setAlignment(Pos.TOP_LEFT);

        Rectangle trait = new Rectangle(40, 3);
        trait.setFill(Color.web(TERRACOTTA));
        trait.setArcWidth(3); trait.setArcHeight(3);
        VBox.setMargin(trait, new Insets(0, 0, 16, 0));

        Text sub = new Text("bienvenue sur");
        sub.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sub.setFill(Color.web("#FFFFFF", 0.68));

        Text brand = new Text("ChriOnline");
        brand.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        brand.setFill(Color.web(BRUN));

        Text tagline = new Text("Votre espace shopping\nnaturel & élégant.");
        tagline.setFont(Font.font("Georgia", FontPosture.ITALIC, 14));
        tagline.setFill(Color.web(BRUN, 0.73));
        tagline.setLineSpacing(6);

        VBox brandBlock = new VBox(5, sub, brand, tagline);
        VBox.setMargin(brandBlock, new Insets(0, 0, 26, 0));

        Rectangle sepOr = new Rectangle(190, 1);
        sepOr.setFill(Color.web(GOLD_LIGHT, 0.50));
        VBox.setMargin(sepOr, new Insets(0, 0, 26, 0));

        VBox bullets = new VBox(15,
                bullet("✦", "Inscription rapide & sécurisée"),
                bullet("✦", "Suivi de commandes en temps réel"),
                bullet("✦", "Livraison éco-responsable"),
                bullet("✦", "Wishlist & offres personnalisées")
        );

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        HBox formes = new HBox(10);
        formes.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(formes, new Insets(0, 0, 18, 0));
        Rectangle sq1 = new Rectangle(46, 46);
        sq1.setFill(Color.web("#4E7A5C", 0.60));
        sq1.setArcWidth(7); sq1.setArcHeight(7);
        Rectangle sq2 = new Rectangle(30, 30);
        sq2.setFill(Color.web(TERRACOTTA, 0.48));
        sq2.setArcWidth(5); sq2.setArcHeight(5);
        Circle rnd = new Circle(16);
        rnd.setFill(Color.web(GOLD_LIGHT, 0.52));
        formes.getChildren().addAll(sq1, sq2, rnd);

        Text copy = new Text("© 2026 ChriOnline");
        copy.setFont(Font.font("Georgia", 11));
        copy.setFill(Color.web(BRUN, 0.40));

        left.getChildren().addAll(trait, brandBlock, sepOr, bullets, sp, formes, copy);
        return left;
    }

    // ════════════════════════════════════════════════════════
    //  PANNEAU DROIT — VBox scrollable natif
    // ════════════════════════════════════════════════════════
    private VBox buildRight(Stage stage) {

        // Contenu interne
        VBox content = new VBox(18);
        content.setPadding(new Insets(44, 48, 44, 48));
        content.setAlignment(Pos.TOP_LEFT);

        // ── En-tête ───────────────────────────────────────────
        Text labelSmall = new Text("— Nouveau membre —");
        labelSmall.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));
        labelSmall.setFill(Color.web(BRUN_LIGHT));

        Text titreForm = new Text("Créer un compte");
        titreForm.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titreForm.setFill(Color.web(BRUN));

        Rectangle traitTitre = new Rectangle(55, 2);
        traitTitre.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(TERRACOTTA)),
                new Stop(1, Color.web(TERRACOTTA, 0.0))
        ));

        VBox header = new VBox(4, labelSmall, titreForm, traitTitre);
        header.setPadding(new Insets(0, 0, 8, 0));
        content.getChildren().add(header);

        // ── Section 1 ─────────────────────────────────────────
        content.getChildren().add(sectionHeader("01", "Informations personnelles"));

        nomField    = inputField("Dupont");
        prenomField = inputField("Jean");
        content.getChildren().add(row(wrap("Nom *", nomField), wrap("Prénom *", prenomField)));

        emailField = inputField("jean@exemple.com");
        telField   = inputField("+212 6XX XXX XXX");
        content.getChildren().add(row(wrap("Email *", emailField), wrap("Téléphone", telField)));

        // ── Section 2 ─────────────────────────────────────────
        content.getChildren().add(sectionHeader("02", "Sécurité"));

        mdpField     = passField("Minimum 6 caractères");
        mdpConfField = passField("Répéter le mot de passe");
        content.getChildren().add(row(wrap("Mot de passe *", mdpField), wrap("Confirmer *", mdpConfField)));

        // Indicateur force
        ProgressBar forceMdp = new ProgressBar(0);
        forceMdp.setMaxWidth(Double.MAX_VALUE);
        forceMdp.setPrefHeight(5);
        forceMdp.setStyle("-fx-accent: " + SAUGE_DARK + "; -fx-background-color: " + CREME_INPUT + ";");

        Label forceLbl = new Label("Saisissez un mot de passe");
        forceLbl.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
        forceLbl.setTextFill(Color.web(BRUN_LIGHT));

        mdpField.textProperty().addListener((obs, old, val) -> {
            double force = calculerForce(val);
            forceMdp.setProgress(force);
            if (force < 0.34) {
                forceLbl.setText("Faible");
                forceMdp.setStyle("-fx-accent: #C0392B; -fx-background-color: " + CREME_INPUT + ";");
            } else if (force < 0.67) {
                forceLbl.setText("Moyen");
                forceMdp.setStyle("-fx-accent: " + GOLD + "; -fx-background-color: " + CREME_INPUT + ";");
            } else {
                forceLbl.setText("Fort ✓");
                forceMdp.setStyle("-fx-accent: " + SAUGE_DARK + "; -fx-background-color: " + CREME_INPUT + ";");
            }
        });
        content.getChildren().add(new VBox(4, forceMdp, forceLbl));

        // ── Section 3 ─────────────────────────────────────────
        content.getChildren().add(sectionHeader("03", "Adresse de livraison"));

        rueField = inputField("12 Rue Mohammed V");
        content.getChildren().add(wrap("Rue", rueField));

        villeField = inputField("Casablanca");
        cpField    = inputField("20000");
        content.getChildren().add(row(wrap("Ville", villeField), wrap("Code postal", cpField)));

        paysField = inputField("Maroc");
        content.getChildren().add(wrap("Pays", paysField));

        // ── Message ───────────────────────────────────────────
        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(msgLabel);

        // ── Bouton ────────────────────────────────────────────
        Button btnInscrire = new Button("S'inscrire");
        btnInscrire.setMaxWidth(Double.MAX_VALUE);
        btnInscrire.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        btnInscrire.setStyle(btnStyle(TERRACOTTA));
        btnInscrire.setCursor(javafx.scene.Cursor.HAND);
        btnInscrire.setOnMouseEntered(e -> btnInscrire.setStyle(btnStyle(TERRA_HOVER)));
        btnInscrire.setOnMouseExited(e  -> btnInscrire.setStyle(btnStyle(TERRACOTTA)));
        content.getChildren().add(btnInscrire);

        // ── Séparateur "ou" ───────────────────────────────────
        Line l1 = new Line(0, 0, 100, 0); l1.setStroke(Color.web(SAUGE, 0.5));
        Line l2 = new Line(0, 0, 100, 0); l2.setStroke(Color.web(SAUGE, 0.5));
        Text ou = new Text("  ou  ");
        ou.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));
        ou.setFill(Color.web(BRUN_LIGHT));
        HBox sepRow = new HBox(l1, ou, l2);
        sepRow.setAlignment(Pos.CENTER);
        content.getChildren().add(sepRow);

        // ── Lien connexion ────────────────────────────────────
        Hyperlink lienCnx = new Hyperlink("Déjà un compte ? Se connecter →");
        lienCnx.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        lienCnx.setTextFill(Color.web(SAUGE_DARK));
        lienCnx.setStyle("-fx-border-color: transparent; -fx-padding: 0;");
        lienCnx.setCursor(javafx.scene.Cursor.HAND);
        HBox lienBox = new HBox(lienCnx);
        lienBox.setAlignment(Pos.CENTER);
        content.getChildren().add(lienBox);

        // ── Contrôleur ────────────────────────────────────────
        InscriptionController ctrl = new InscriptionController(
                nomField, prenomField, emailField, telField,
                mdpField, mdpConfField,
                rueField, villeField, cpField, paysField,
                msgLabel
        );
        btnInscrire.setOnAction(e -> ctrl.inscrire());
        lienCnx.setOnAction(e -> stage.close());

        // ── Wrapper avec scroll interne ───────────────────────
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: " + CREME_CARD + ";" +
                        "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-border-color: transparent;"
        );

        // VBox container qui contient le scroll et prend tout l'espace
        VBox right = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        right.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-background-radius: 0 20 20 0;"
        );
        HBox.setHgrow(right, Priority.ALWAYS);
        return right;
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setFont(Font.font("Georgia", 13));
        f.setStyle(fieldStyle(SAUGE));
        f.focusedProperty().addListener((o, old, focused) ->
                f.setStyle(focused ? fieldStyle(TERRACOTTA) + "-fx-border-width:1.5;" : fieldStyle(SAUGE))
        );
        return f;
    }

    private PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setFont(Font.font("Georgia", 13));
        f.setStyle(fieldStyle(SAUGE));
        f.focusedProperty().addListener((o, old, focused) ->
                f.setStyle(focused ? fieldStyle(TERRACOTTA) + "-fx-border-width:1.5;" : fieldStyle(SAUGE))
        );
        return f;
    }

    private String fieldStyle(String border) {
        return "-fx-background-color:" + CREME_INPUT + ";" +
                "-fx-border-color:" + border + ";" +
                "-fx-border-radius:6;" +
                "-fx-background-radius:6;" +
                "-fx-padding:10 13 10 13;" +
                "-fx-text-fill:" + BRUN + ";";
    }

    private VBox wrap(String label, Control field) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(BRUN_MED));
        VBox box = new VBox(5, lbl, field);
        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox row(VBox a, VBox b) {
        HBox h = new HBox(16, a, b);
        h.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        return h;
    }

    private HBox sectionHeader(String num, String titre) {
        Rectangle bar = new Rectangle(3, 16);
        bar.setFill(Color.web(TERRACOTTA));
        bar.setArcWidth(3); bar.setArcHeight(3);

        Text numTxt = new Text(num);
        numTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        numTxt.setFill(Color.web(TERRACOTTA));

        Text titTxt = new Text("  " + titre);
        titTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        titTxt.setFill(Color.web(BRUN_MED));

        HBox h = new HBox(10, bar, new TextFlow(numTxt, titTxt));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(6, 0, 2, 0));
        return h;
    }

    private VBox bullet(String icon, String txt) {
        Text ic = new Text(icon + "  ");
        ic.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
        ic.setFill(Color.web(TERRACOTTA));
        Text tx = new Text(txt);
        tx.setFont(Font.font("Georgia", 13));
        tx.setFill(Color.web(BRUN, 0.80));
        return new VBox(new TextFlow(ic, tx));
    }

    private String btnStyle(String color) {
        return "-fx-background-color:" + color + ";" +
                "-fx-text-fill:white;" +
                "-fx-padding:13 0 13 0;" +
                "-fx-background-radius:8;" +
                "-fx-font-size:15px;";
    }

    private double calculerForce(String mdp) {
        if (mdp == null || mdp.isEmpty()) return 0;
        double s = 0;
        if (mdp.length() >= 6)  s += 0.25;
        if (mdp.length() >= 10) s += 0.25;
        if (mdp.matches(".*[A-Z].*")) s += 0.20;
        if (mdp.matches(".*[0-9].*")) s += 0.15;
        if (mdp.matches(".*[^a-zA-Z0-9].*")) s += 0.15;
        return Math.min(s, 1.0);
    }

    public static void main(String[] args) { launch(args); }
}