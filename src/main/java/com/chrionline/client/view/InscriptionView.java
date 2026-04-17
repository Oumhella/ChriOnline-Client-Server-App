package com.chrionline.client.view;

import com.chrionline.client.controller.InscriptionController;
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
    private static final String BORDER       = "#E8E0D5";

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
    private TextField     dateNaissanceField;

    private RecaptchaWidget  captchaWidget;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Créer un compte");

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

        HBox card = new HBox();
        card.setMaxWidth(980);
        card.setMaxHeight(700);
        card.setMinHeight(600);
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

        VBox left  = buildLeft();
        VBox right = buildRight(stage);
        HBox.setHgrow(right, Priority.ALWAYS);

        card.getChildren().addAll(left, right);
        root.getChildren().addAll(bg1, bg2, card);

        card.setOpacity(0);
        card.setTranslateY(20);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
            stage.getScene().getStylesheets().clear();
        }
        
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        if (!stage.isShowing()) stage.show();

        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(20); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    // ════════════════════════════════════════════════════════
    //  PANNEAU GAUCHE — inchangé
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

        Text tagline = new Text("Votre espace shopping");
        tagline.setFont(Font.font("Georgia", FontPosture.ITALIC, 14));
        tagline.setFill(Color.web(BRUN, 0.73));
        tagline.setLineSpacing(6);

        VBox brandBlock = new VBox(5, sub, brand, tagline);
        VBox.setMargin(brandBlock, new Insets(0, 0, 26, 0));

        Rectangle sepOr = new Rectangle(190, 1);
        sepOr.setFill(Color.web(GOLD_LIGHT, 0.50));
        VBox.setMargin(sepOr, new Insets(0, 0, 26, 0));

        VBox bullets = new VBox(15,
                bullet("✦", "Inscription simple et sécurisée en quelques secondes"),
                bullet("✦", "Suivi de commande en temps réel"),
                bullet("✦", "Enregistrez vos favoris et recevez des offres exclusives")
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
    //  PANNEAU DROIT — style professionnel
    // ════════════════════════════════════════════════════════
    private VBox buildRight(Stage stage) {

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_LEFT);
        content.setStyle("-fx-background-color: " + CREME_CARD + ";");

        // ── Barre de progression des étapes ──────────────────
        HBox stepBar = buildStepBar();
        content.getChildren().add(stepBar);

        // ── Zone de formulaire scrollable ─────────────────────
        VBox form = new VBox(16);
        form.setPadding(new Insets(32, 48, 32, 48));
        form.setAlignment(Pos.TOP_LEFT);

        Button btnRetour = new Button("← Retour Accueil");
        btnRetour.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN_LIGHT + "; -fx-cursor: hand; -fx-font-family: 'Georgia'; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        btnRetour.setOnAction(e -> {
            try { new HomeView().start(stage); }
            catch (Exception ex) { ex.printStackTrace(); }
        });

        // En-tête sobre
        Text titreForm = new Text("Créer votre compte");
        titreForm.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        titreForm.setFill(Color.web(BRUN));

        Text sousTitre = new Text("Tous les champs marqués * sont obligatoires.");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));
        sousTitre.setFill(Color.web(BRUN_LIGHT));

        VBox header = new VBox(6, btnRetour, titreForm, sousTitre);
        header.setPadding(new Insets(0, 0, 12, 0));
        form.getChildren().add(header);

        // ══ Bloc 1 : Identité ═════════════════════════════════
        form.getChildren().add(blocTitre("Informations personnelles"));

        nomField    = inputField("ex: moeniss");
        prenomField = inputField("ex: douae");
        form.getChildren().add(row(
                labelField("Nom *", nomField),
                labelField("Prénom *", prenomField)
        ));

        emailField = inputField("ex: douae@exemple.com");
        telField   = inputField("ex: +212 6XX XXX XXX");
        form.getChildren().add(row(
                labelField("Adresse email *", emailField),
                labelField("Téléphone", telField)
        ));

        dateNaissanceField = inputField("ex: 31/03/2005");
        form.getChildren().add(
                labelField("Date de naissance", dateNaissanceField)
        );

        // ══ Bloc 2 : Sécurité ═════════════════════════════════
        form.getChildren().add(blocTitre("Mot de passe"));

        mdpField     = new PasswordField();
        mdpConfField = new PasswordField();
        
        form.getChildren().add(row(
                labelField("Mot de passe *", creerChampMdp(mdpField, "Au moins 6 caractères")),
                labelField("Confirmation *", creerChampMdp(mdpConfField, "Répéter le mot de passe"))
        ));

        // Barre de force
        ProgressBar forceMdp = new ProgressBar(0);
        forceMdp.setMaxWidth(Double.MAX_VALUE);
        forceMdp.setPrefHeight(4);
        forceMdp.setStyle("-fx-accent: " + SAUGE_DARK + "; -fx-background-color: " + BORDER + ";");

        Label forceLbl = new Label("Saisissez un mot de passe");
        forceLbl.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
        forceLbl.setTextFill(Color.web(BRUN_LIGHT));

        mdpField.textProperty().addListener((obs, old, val) -> {
            String nom = nomField.getText();
            String prenom = prenomField.getText();
            String dob = dateNaissanceField.getText();
            double f = com.chrionline.shared.utils.PasswordValidator.calculerScore(val, nom, prenom, dob);
            forceMdp.setProgress(f);
            if (f < 0.34) {
                forceLbl.setText("● Sécurité faible");
                forceLbl.setTextFill(Color.web("#C0392B"));
                forceMdp.setStyle("-fx-accent: #C0392B; -fx-background-color: " + BORDER + ";");
            } else if (f < 0.67) {
                forceLbl.setText("● Sécurité moyenne");
                forceLbl.setTextFill(Color.web(GOLD));
                forceMdp.setStyle("-fx-accent: " + GOLD + "; -fx-background-color: " + BORDER + ";");
            } else {
                forceLbl.setText("✓ Mot de passe sécurisé");
                forceLbl.setTextFill(Color.web(SAUGE_DARK));
                forceMdp.setStyle("-fx-accent: " + SAUGE_DARK + "; -fx-background-color: " + BORDER + ";");
            }
        });
        form.getChildren().add(new VBox(5, forceMdp, forceLbl));

        // ══ Bloc 3 : Adresse ══════════════════════════════════
        form.getChildren().add(blocTitre("Adresse de livraison"));

        rueField = inputField("Numéro et nom de rue");
        form.getChildren().add(labelField("Rue", rueField));

        villeField = inputField("Ville");
        cpField    = inputField("Code postal");
        form.getChildren().add(row(
                labelField("Ville", villeField),
                labelField("Code postal", cpField)
        ));

        paysField = inputField("Maroc");
        form.getChildren().add(labelField("Pays", paysField));

        // ── Message retour ────────────────────────────────────
        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(Double.MAX_VALUE);
        msgLabel.setPadding(new Insets(4, 12, 4, 12));
        form.getChildren().add(msgLabel);

        // ── Bouton principal ──────────────────────────────────
        Button btnInscrire = new Button("Créer mon compte");
        btnInscrire.setMaxWidth(Double.MAX_VALUE);
        btnInscrire.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnInscrire.setStyle(btnPrimary(TERRACOTTA));
        btnInscrire.setCursor(javafx.scene.Cursor.HAND);
        btnInscrire.setOnMouseEntered(e -> btnInscrire.setStyle(btnPrimary(TERRA_HOVER)));
        btnInscrire.setOnMouseExited(e  -> btnInscrire.setStyle(btnPrimary(TERRACOTTA)));

        // ── Lien connexion ────────────────────────────────────
        Label dejaMembre = new Label("Vous avez déjà un compte ?");
        dejaMembre.setFont(Font.font("Georgia", 12));
        dejaMembre.setTextFill(Color.web(BRUN_LIGHT));

        Hyperlink lienCnx = new Hyperlink("Se connecter");
        lienCnx.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lienCnx.setTextFill(Color.web(SAUGE_DARK));
        lienCnx.setStyle("-fx-border-color: transparent; -fx-padding: 0 0 0 4;");
        lienCnx.setCursor(javafx.scene.Cursor.HAND);
        lienCnx.setUnderline(true);

        HBox lienBox = new HBox(4, dejaMembre, lienCnx);
        lienBox.setAlignment(Pos.CENTER);
        lienBox.setPadding(new Insets(4, 0, 0, 0));

        // ── Mentions légales ──────────────────────────────────
        Text mentions = new Text(
                "En créant un compte, vous acceptez nos Conditions générales\n" +
                        "d'utilisation et notre Politique de confidentialité."
        );
        mentions.setFont(Font.font("Georgia", FontPosture.ITALIC, 10));
        mentions.setFill(Color.web(BRUN_LIGHT, 0.7));
        HBox mentionsBox = new HBox(mentions);
        mentionsBox.setAlignment(Pos.CENTER);
        mentionsBox.setPadding(new Insets(6, 0, 0, 0));

        // ── Widget reCAPTCHA (pixel-perfect) ────────────────────
        captchaWidget = new RecaptchaWidget();
        form.getChildren().addAll(captchaWidget, btnInscrire, lienBox, mentionsBox);

        // ── Contrôleur ────────────────────────────────────────
        InscriptionController ctrl = new InscriptionController(
                nomField, prenomField, emailField, telField,
                mdpField, mdpConfField,
                rueField, villeField, cpField, paysField,
                msgLabel, stage, dateNaissanceField
        );
        btnInscrire.setOnAction(e -> {
            if (!captchaWidget.estValide()) {
                msgLabel.setText("✗ Veuillez cocher \"Je ne suis pas un robot\".");
                msgLabel.setStyle("-fx-text-fill: " + TERRACOTTA + ";");
                return;
            }
            ctrl.inscrire(captchaWidget.getToken());
        });
        lienCnx.setOnAction(e -> {
            try {
                new ConnexionView().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ── ScrollPane ────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: " + CREME_CARD + ";" +
                        "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        content.getChildren().add(scroll);
        HBox.setHgrow(content, Priority.ALWAYS);
        return content;
    }

    // ── Barre d'étapes en haut ────────────────────────────────
    private HBox buildStepBar() {
        HBox bar = new HBox(0);
        bar.setStyle(
                "-fx-background-color: " + CREME_INPUT + ";" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        bar.setPadding(new Insets(14, 48, 14, 48));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(8);

        bar.getChildren().addAll(
                stepItem("1", "Informations", true),
                stepArrow(),
                stepItem("2", "Sécurité", false),
                stepArrow(),
                stepItem("3", "Adresse", false)
        );
        return bar;
    }

    private HBox stepItem(String num, String label, boolean actif) {
        Circle circle = new Circle(12);
        circle.setFill(actif ? Color.web(TERRACOTTA) : Color.web(BORDER));
        circle.setStroke(actif ? Color.web(TERRACOTTA) : Color.web(SAUGE));
        circle.setStrokeWidth(1.5);

        Text numTxt = new Text(num);
        numTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        numTxt.setFill(actif ? Color.WHITE : Color.web(BRUN_LIGHT));

        StackPane cercle = new StackPane(circle, numTxt);

        Text lbl = new Text(label);
        lbl.setFont(Font.font("Georgia", actif ? FontWeight.BOLD : FontWeight.NORMAL, 12));
        lbl.setFill(actif ? Color.web(BRUN) : Color.web(BRUN_LIGHT));

        HBox item = new HBox(8, cercle, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private Text stepArrow() {
        Text arrow = new Text("  ›  ");
        arrow.setFont(Font.font("Georgia", 14));
        arrow.setFill(Color.web(SAUGE));
        return arrow;
    }

    // ── Titre de bloc ─────────────────────────────────────────
    private HBox blocTitre(String titre) {
        Rectangle bar = new Rectangle(3, 14);
        bar.setFill(Color.web(SAUGE_DARK));
        bar.setArcWidth(2); bar.setArcHeight(2);

        Text t = new Text(titre);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        t.setFill(Color.web(BRUN_MED));

        // Ligne séparatrice
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Rectangle ligne = new Rectangle(1, 1);
        ligne.setFill(Color.TRANSPARENT);

        HBox h = new HBox(10, bar, t);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(10, 0, 4, 0));

        // Séparateur complet
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BORDER + ";");

        VBox bloc = new VBox(2, sep, h);
        HBox wrapper = new HBox(bloc);
        HBox.setHgrow(bloc, Priority.ALWAYS);
        return wrapper;
    }

    // ── Champ avec label intégré ──────────────────────────────
    private VBox labelField(String label, Node field) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(BRUN_MED));
        VBox box = new VBox(5, lbl, field);
        if (field instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setFont(Font.font("Georgia", 13));
        f.setStyle(fieldStyle(BORDER));
        f.focusedProperty().addListener((o, old, focused) ->
                f.setStyle(focused
                        ? "-fx-background-color:" + CREME_CARD + ";" +
                        "-fx-border-color:" + SAUGE_DARK + ";" +
                        "-fx-border-radius:6;" +
                        "-fx-background-radius:6;" +
                        "-fx-padding:10 13 10 13;" +
                        "-fx-text-fill:" + BRUN + ";" +
                        "-fx-border-width:1.5;"
                        : fieldStyle(BORDER))
        );
        return f;
    }

    private HBox creerChampMdp(PasswordField pf, String prompt) {
        pf.setPromptText(prompt);
        pf.setFont(Font.font("Georgia", 13));
        pf.setStyle(fieldStyle(BORDER));
        
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setFont(Font.font("Georgia", 13));
        tf.setStyle(fieldStyle(BORDER));
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
        container.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 6; -fx-border-color: " + BORDER + "; -fx-border-radius: 6;");
        
        // Nettoyage des styles internes
        pf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding:10 13;");
        tf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding:10 13;");
        
        return container;
    }

    private PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setFont(Font.font("Georgia", 13));
        f.setStyle(fieldStyle(BORDER));
        f.focusedProperty().addListener((o, old, focused) ->
                f.setStyle(focused
                        ? "-fx-background-color:" + CREME_CARD + ";" +
                        "-fx-border-color:" + SAUGE_DARK + ";" +
                        "-fx-border-radius:6;" +
                        "-fx-background-radius:6;" +
                        "-fx-padding:10 13 10 13;" +
                        "-fx-text-fill:" + BRUN + ";" +
                        "-fx-border-width:1.5;"
                        : fieldStyle(BORDER))
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

    private HBox row(VBox a, VBox b) {
        HBox h = new HBox(16, a, b);
        h.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        return h;
    }

    private VBox wrap(String label, Node field) {
        return labelField(label, field);
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

    private String btnPrimary(String color) {
        return "-fx-background-color:" + color + ";" +
                "-fx-text-fill:white;" +
                "-fx-padding:13 0 13 0;" +
                "-fx-background-radius:8;" +
                "-fx-font-size:14px;";
    }

    private double calculerForce(String mdp) {
        return com.chrionline.shared.utils.PasswordValidator.calculerScore(mdp, nomField.getText(), prenomField.getText(), dateNaissanceField.getText());
    }

    public static void main(String[] args) { launch(args); }
}