package com.chrionline.client.view;

import com.chrionline.client.controller.PanierController;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LignePanierDTO;
import com.chrionline.shared.dto.PanierDTO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.math.BigDecimal;

/**
 * Vue du panier client ChriOnline.
 * Palette et style cohérents avec le reste du projet.
 *
 * Usage : new PanierView(idUtilisateur).start(stage);
 */
public class PanierView extends Application {

    // ── Palette ──────────────────────────────────────────────────────────
    private static final String CREME = "#FDFBF7";
    private static final String CREME_CARD = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String SAUGE_DARK = "#6B9E7A";
    private static final String SAUGE = "#A8C4B0";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String TERRA_HOVER = "#A0522D";
    private static final String BRUN = "#3E2C1E";
    private static final String BRUN_MED = "#6B4F3A";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String BORDER = "#E8E0D5";
    private static final String DANGER = "#B03A2E";
    private static final String DANGER_BG = "#FBEAEA";

    private int idUtilisateur;
    private PanierController controller;
    private VBox listeZone;
    private Text totalText;
    private Text nbArticlesText;
    private Label msgLabel;
    private Stage stage;

    /** Constructeur par défaut requis par JavaFX launch(). */
    public PanierView() {
    }

    /** Constructeur à utiliser depuis les autres vues. */
    public PanierView(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.controller = new PanierController(idUtilisateur);

        stage.setTitle("ChriOnline — Mon Panier");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        root.getChildren().add(buildHeader());

        // ── Contenu ──────────────────────────────────────────
        HBox content = new HBox(28);
        content.setPadding(new Insets(36, 48, 36, 48));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Colonne gauche : liste articles
        VBox leftCol = buildLeftColumn();
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // Colonne droite : récapitulatif
        VBox rightCol = buildRightColumn();

        content.getChildren().addAll(leftCol, rightCol);
        root.getChildren().add(content);

        Scene scene = new Scene(root, 1100, 800);
        stage.setScene(scene);
        stage.show();

        // Charger le panier dans un thread séparé
        chargerPanier();
    }

    // ═════════════════════════════════════════════════════════════════════
    // HEADER
    // ═════════════════════════════════════════════════════════════════════

    private HBox buildHeader() {
        HBox header = new HBox(40);
        header.setPadding(new Insets(22, 48, 22, 48));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> retourCatalogue());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(28);
        nav.setAlignment(Pos.CENTER);

        Hyperlink hAcc = navLink("Accueil");
        hAcc.setOnAction(e -> {
            try { new HomeView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        Hyperlink hCat = navLink("Catalogue");
        hCat.setOnAction(e -> {
            try { new CatalogueView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        String prenom = com.chrionline.client.session.SessionManager.getInstance().getPrenom();
        String nom = com.chrionline.client.session.SessionManager.getInstance().getNom();
        String initials = "";
        if (prenom != null && !prenom.isEmpty()) initials += prenom.toUpperCase().charAt(0);
        if (nom != null && !nom.isEmpty()) initials += nom.toUpperCase().charAt(0);
        if (initials.isEmpty()) initials = "U";
        
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(15, Color.web(TERRACOTTA));
        Text initText = new Text(initials);
        initText.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        initText.setFill(Color.WHITE);
        avatar.getChildren().addAll(circle, initText);
        avatar.setCursor(javafx.scene.Cursor.HAND);
        avatar.setOnMouseClicked(e -> {
            try { new ProfilView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });
        avatar.setOnMouseEntered(e -> circle.setFill(Color.web(SAUGE_DARK)));
        avatar.setOnMouseExited(e -> circle.setFill(Color.web(TERRACOTTA)));

        Hyperlink hOrd = navLink("Mes Commandes");
        hOrd.setOnAction(e -> {
            try { new MesCommandesView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        nav.getChildren().addAll(hAcc, hCat, hOrd, navLinkActif("Mon Panier"), avatar);

        header.getChildren().addAll(logo, spacer, nav);
        return header;
    }

    private Hyperlink navLink(String text) {
        Hyperlink l = new Hyperlink(text);
        l.setFont(Font.font("Georgia", 14));
        l.setTextFill(Color.web(BRUN));
        l.setStyle("-fx-border-color: transparent;");
        l.setOnAction(e -> retourCatalogue());
        return l;
    }

    private Text navLinkActif(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        t.setFill(Color.web(SAUGE_DARK));
        return t;
    }

    // ═════════════════════════════════════════════════════════════════════
    // COLONNE GAUCHE — Liste des articles
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildLeftColumn() {
        VBox col = new VBox(0);

        // Titre section
        HBox titreRow = new HBox(12);
        titreRow.setAlignment(Pos.CENTER_LEFT);
        titreRow.setPadding(new Insets(0, 0, 20, 0));

        Text titre = new Text("Mon panier");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        titre.setFill(Color.web(BRUN));

        nbArticlesText = new Text("(0 article)");
        nbArticlesText.setFont(Font.font("Georgia", FontPosture.ITALIC, 14));
        nbArticlesText.setFill(Color.web(BRUN_LIGHT));

        titreRow.getChildren().addAll(titre, nbArticlesText);

        // Message feedback
        msgLabel = new Label();
        msgLabel.setFont(Font.font("Georgia", 13));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(Double.MAX_VALUE);

        // Zone liste
        listeZone = new VBox(14);

        ScrollPane scroll = new ScrollPane(listeZone);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        col.getChildren().addAll(titreRow, msgLabel, scroll);
        return col;
    }

    // ═════════════════════════════════════════════════════════════════════
    // COLONNE DROITE — Récapitulatif + commande
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildRightColumn() {
        VBox col = new VBox(0);
        col.setPrefWidth(300);
        col.setMinWidth(270);
        col.setMaxWidth(320);
        col.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;");
        col.setEffect(new DropShadow(8, Color.web(BRUN, 0.06)));

        // Titre récap
        HBox titreBox = new HBox();
        titreBox.setPadding(new Insets(20, 20, 16, 20));
        titreBox.setStyle(
                "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");
        Text titreRecap = new Text("Récapitulatif");
        titreRecap.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        titreRecap.setFill(Color.web(BRUN));
        titreBox.getChildren().add(titreRecap);

        // Zone totaux
        VBox totauxZone = new VBox(10);
        totauxZone.setPadding(new Insets(16, 20, 16, 20));

        HBox ligneTotal = new HBox();
        Text labelTotal = new Text("Total");
        labelTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        labelTotal.setFill(Color.web(BRUN));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        totalText = new Text("0,00 MAD");
        totalText.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        totalText.setFill(Color.web(TERRACOTTA));
        ligneTotal.getChildren().addAll(labelTotal, sp, totalText);

        Text note = new Text("Livraison calculée à l'étape suivante.");
        note.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
        note.setFill(Color.web(BRUN_LIGHT));
        note.setWrappingWidth(250);

        totauxZone.getChildren().addAll(ligneTotal, new Separator(), note);

        // Bouton commander
        Button btnCommander = new Button("Passer la commande");
        btnCommander.setMaxWidth(Double.MAX_VALUE);
        btnCommander.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnCommander.setStyle(btnStyle(TERRACOTTA));
        btnCommander.setCursor(javafx.scene.Cursor.HAND);
        btnCommander.setOnMouseEntered(e -> btnCommander.setStyle(btnStyle(TERRA_HOVER)));
        btnCommander.setOnMouseExited(e -> btnCommander.setStyle(btnStyle(TERRACOTTA)));
        btnCommander.setOnAction(e -> passerCommande());

        VBox btnZone = new VBox(10);
        btnZone.setPadding(new Insets(0, 20, 20, 20));

        // Bouton vider
        Button btnVider = new Button("Vider le panier");
        btnVider.setMaxWidth(Double.MAX_VALUE);
        btnVider.setFont(Font.font("Georgia", 12));
        btnVider.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-text-fill: " + BRUN_LIGHT + ";" +
                        "-fx-padding: 9 0;");
        btnVider.setCursor(javafx.scene.Cursor.HAND);
        btnVider.setOnMouseEntered(e -> btnVider.setStyle(
                "-fx-background-color: " + DANGER_BG + ";" +
                        "-fx-border-color: " + DANGER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-text-fill: " + DANGER + ";" +
                        "-fx-padding: 9 0;"));
        btnVider.setOnMouseExited(e -> btnVider.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-text-fill: " + BRUN_LIGHT + ";" +
                        "-fx-padding: 9 0;"));
        btnVider.setOnAction(e -> viderPanier());

        // Lien continuer shopping
        Hyperlink continuer = new Hyperlink("← Continuer mes achats");
        continuer.setFont(Font.font("Georgia", 12));
        continuer.setTextFill(Color.web(SAUGE_DARK));
        continuer.setStyle("-fx-border-color: transparent;");
        continuer.setOnAction(e -> retourCatalogue());

        btnZone.getChildren().addAll(btnCommander, btnVider, continuer);

        col.getChildren().addAll(titreBox, totauxZone, btnZone);
        return col;
    }

    // ═════════════════════════════════════════════════════════════════════
    // CHARGEMENT & RENDU DU PANIER
    // ═════════════════════════════════════════════════════════════════════

    private void chargerPanier() {
        new Thread(() -> {
            PanierDTO panier = controller.getPanier();
            Platform.runLater(() -> afficherPanier(panier));
        }).start();
    }

    private void afficherPanier(PanierDTO panier) {
        listeZone.getChildren().clear();

        if (panier == null) {
            afficherErreur("Impossible de charger le panier. Vérifiez votre connexion.");
            return;
        }

        int nbArticles = panier.getNombreArticles();
        nbArticlesText.setText("(" + nbArticles + " article" + (nbArticles > 1 ? "s" : "") + ")");
        totalText.setText(formatMonnaie(panier.getMontantTotal()) + " MAD");

        if (panier.getLignes().isEmpty()) {
            listeZone.getChildren().add(buildPanierVide());
        } else {
            for (LignePanierDTO ligne : panier.getLignes()) {
                listeZone.getChildren().add(buildLigneArticle(ligne));
            }
        }
    }

    private VBox buildPanierVide() {
        VBox vide = new VBox(16);
        vide.setAlignment(Pos.CENTER);
        vide.setPadding(new Insets(60, 0, 60, 0));

        Text emoji = new Text("🛒");
        emoji.setFont(Font.font(48));

        Text msg = new Text("Votre panier est vide");
        msg.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        msg.setFill(Color.web(BRUN));

        Text sub = new Text("Explorez notre catalogue pour ajouter des produits.");
        sub.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sub.setFill(Color.web(BRUN_LIGHT));

        Button btnCatalogue = new Button("Voir le catalogue →");
        btnCatalogue.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        btnCatalogue.setStyle(btnStyle(SAUGE_DARK));
        btnCatalogue.setCursor(javafx.scene.Cursor.HAND);
        btnCatalogue.setOnAction(e -> retourCatalogue());

        vide.getChildren().addAll(emoji, msg, sub, btnCatalogue);
        return vide;
    }

    // ─── Carte article ────────────────────────────────────────────────────

    private HBox buildLigneArticle(LignePanierDTO ligne) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;");
        card.setEffect(new DropShadow(5, Color.web(BRUN, 0.04)));

        // Image ou placeholder
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(70, 80);
        imgBox.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8;");

        if (ligne.getImage_url() != null && !ligne.getImage_url().isBlank()) {
            try {
                Image img = new Image(ligne.getImage_url(), 70, 80, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(70);
                iv.setFitHeight(80);
                iv.setPreserveRatio(true);
                Rectangle clip = new Rectangle(70, 80);
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                iv.setClip(clip);
                imgBox.getChildren().add(iv);
            } catch (Exception ignored) {
                imgBox.getChildren().add(placeholderText(ligne.getNomProduit()));
            }
        } else {
            imgBox.getChildren().add(placeholderText(ligne.getNomProduit()));
        }

        // Infos produit
        VBox infos = new VBox(5);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Text nom = new Text(ligne.getNomProduit() != null ? ligne.getNomProduit() : "—");
        nom.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        nom.setFill(Color.web(BRUN));

        if (ligne.getDescriptionVariant() != null && !ligne.getDescriptionVariant().isBlank()) {
            Text variant = new Text(ligne.getDescriptionVariant());
            variant.setFont(Font.font("Georgia", 12));
            variant.setFill(Color.web(BRUN_LIGHT));
            infos.getChildren().add(variant);
        }

        Text prix = new Text(formatMonnaie(ligne.getPrix()) + " MAD / unité");
        prix.setFont(Font.font("Georgia", 12));
        prix.setFill(Color.web(BRUN_LIGHT));

        infos.getChildren().addAll(nom, prix);

        // Contrôle quantité
        HBox qteControl = new HBox(8);
        qteControl.setAlignment(Pos.CENTER);

        Button btnMoins = roundBtn("−");
        Label qteLabel = new Label(String.valueOf(ligne.getQuantite()));
        qteLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        qteLabel.setTextFill(Color.web(BRUN));
        qteLabel.setMinWidth(28);
        qteLabel.setAlignment(Pos.CENTER);
        Button btnPlus = roundBtn("+");

        btnMoins.setOnAction(e -> {
            int nouvelle = ligne.getQuantite() - 1;
            majQuantite(ligne, nouvelle, qteLabel);
        });
        btnPlus.setOnAction(e -> {
            int nouvelle = ligne.getQuantite() + 1;
            majQuantite(ligne, nouvelle, qteLabel);
        });

        qteControl.getChildren().addAll(btnMoins, qteLabel, btnPlus);

        // Sous-total
        Text sousTotal = new Text(formatMonnaie(ligne.getTotal()) + " MAD");
        sousTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        sousTotal.setFill(Color.web(BRUN));
        sousTotal.setTextAlignment(TextAlignment.RIGHT);

        // Bouton supprimer
        Button btnSuppr = new Button("✕");
        btnSuppr.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + BRUN_LIGHT + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 4 6;" +
                        "-fx-background-radius: 6;");
        btnSuppr.setCursor(javafx.scene.Cursor.HAND);
        btnSuppr.setOnMouseEntered(e -> btnSuppr.setStyle(
                "-fx-background-color: " + DANGER_BG + ";" +
                        "-fx-text-fill: " + DANGER + ";" +
                        "-fx-font-size: 13; -fx-padding: 4 6; -fx-background-radius: 6;"));
        btnSuppr.setOnMouseExited(e -> btnSuppr.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + BRUN_LIGHT + ";" +
                        "-fx-font-size: 13; -fx-padding: 4 6; -fx-background-radius: 6;"));
        btnSuppr.setOnAction(e -> {
            new Thread(() -> {
                PanierDTO maj = controller.retirerProduit(ligne.getId_product_formats());
                Platform.runLater(() -> afficherPanier(maj));
            }).start();
        });

        VBox droite = new VBox(8);
        droite.setAlignment(Pos.CENTER_RIGHT);
        droite.getChildren().addAll(sousTotal, qteControl, btnSuppr);

        card.getChildren().addAll(imgBox, infos, droite);
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═════════════════════════════════════════════════════════════════════

    private void majQuantite(LignePanierDTO ligne, int nouvelleQte, Label qteLabel) {
        new Thread(() -> {
            PanierDTO maj = controller.modifierQuantite(ligne.getId_product_formats(), nouvelleQte);
            Platform.runLater(() -> {
                if (maj != null) {
                    afficherPanier(maj);
                } else {
                    afficherErreur("Stock insuffisant pour cette quantité.");
                }
            });
        }).start();
    }

    private void viderPanier() {
        new Thread(() -> {
            PanierDTO maj = controller.viderPanier();
            Platform.runLater(() -> afficherPanier(maj));
        }).start();
    }

    private void passerCommande() {
        msgLabel.setText("Traitement en cours...");
        msgLabel.setStyle("-fx-text-fill: " + BRUN_MED + ";");

        new Thread(() -> {
            CommandeDTO recap = controller.validerPanier();
            Platform.runLater(() -> {
                if (recap != null) {
                    afficherSuccesCommande(recap);
                } else {
                    afficherErreur("Impossible de valider la commande. Vérifiez le stock disponible.");
                }
            });
        }).start();
    }

    private void afficherSuccesCommande(CommandeDTO recap) {
        try {
            new ConfirmationCommandeView(idUtilisateur, recap).start(stage);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: simple message if view fails
            listeZone.getChildren().clear();
            Text t = new Text("Commande passée ! Réf: " + recap.getReference());
            t.setFont(Font.font("Georgia", 20));
            listeZone.getChildren().add(t);
        }
    }



    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private Button roundBtn(String label) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btn.setPrefSize(28, 28);
        btn.setStyle(
                "-fx-background-color: " + CREME_INPUT + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-text-fill: " + BRUN + ";" +
                        "-fx-padding: 0;");
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + SAUGE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + SAUGE + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 0;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + CREME_INPUT + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-text-fill: " + BRUN + ";" +
                        "-fx-padding: 0;"));
        return btn;
    }

    private Text placeholderText(String nom) {
        Text t = new Text(nom != null && !nom.isBlank()
                ? nom.substring(0, 1).toUpperCase()
                : "?");
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        t.setFill(Color.web(BRUN_LIGHT, 0.5));
        return t;
    }

    private void afficherErreur(String msg) {
        msgLabel.setStyle("-fx-text-fill: " + DANGER + ";");
        msgLabel.setText("✗ " + msg);
    }

    private void retourCatalogue() {
        try {
            new CatalogueView(idUtilisateur).start(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String formatMonnaie(BigDecimal val) {
        return val != null ? String.format("%,.2f", val) : "0,00";
    }

    private String btnStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: white;" +
                "-fx-padding: 13 0; -fx-background-radius: 8;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}