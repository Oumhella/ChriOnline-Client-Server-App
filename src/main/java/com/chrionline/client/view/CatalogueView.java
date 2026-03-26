package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;

public class CatalogueView extends Application {

    private static final String CREME      = "#FDFBF7";
    private static final String SAUGE      = "#A8C4B0";
    private static final String SAUGE_DARK = "#6B9E7A";
    private static final String BRUN       = "#3E2C1E";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String BORDER     = "#E8E0D5";

    private CatalogueController controller;
    private FlowPane productGrid;
    private Stage stage;

    // userId transmis depuis la vue de connexion
    private int userId = -1;

    /** Constructeur par défaut requis par JavaFX launch(). */
    public CatalogueView() {}

    /** Constructeur à utiliser depuis les autres vues (après login). */
    public CatalogueView(int userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage stage) {
        this.stage      = stage;
        this.controller = new CatalogueController(userId);

        stage.setTitle("ChriOnline — Catalogue");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        root.getChildren().add(buildHeader());

        productGrid = new FlowPane(30, 30);
        productGrid.setPadding(new Insets(40));
        productGrid.setAlignment(Pos.TOP_CENTER);
        productGrid.setStyle("-fx-background-color: " + CREME + ";");

        ScrollPane scroll = new ScrollPane(productGrid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(scroll);

        // Chargement dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            List<Produit> produits = controller.recupererProduits();
            javafx.application.Platform.runLater(() -> afficherProduits(produits));
        }).start();

        Scene scene = new Scene(root, 1100, 800);
        stage.setScene(scene);
        stage.show();
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════

    private HBox buildHeader() {
        HBox header = new HBox(40);
        header.setPadding(new Insets(30, 60, 30, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("SHE SOAP");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(30);
        nav.setAlignment(Pos.CENTER);
        nav.getChildren().addAll(
                navLink("Accueil"),
                navLink("Assortiment"),
                navLink("Livraison")
        );

        // Bouton panier (visible uniquement si connecté)
        if (userId != -1) {
            Button btnPanier = new Button("🛒 Mon panier");
            btnPanier.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            btnPanier.setStyle(
                    "-fx-background-color: " + TERRACOTTA + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 8 18;"
            );
            btnPanier.setCursor(javafx.scene.Cursor.HAND);
            btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(
                    "-fx-background-color: #A0522D; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 18;"
            ));
            btnPanier.setOnMouseExited(e -> btnPanier.setStyle(
                    "-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 18;"
            ));
            btnPanier.setOnAction(e -> ouvrirPanier());
            nav.getChildren().add(btnPanier);
        }

        header.getChildren().addAll(logo, spacer, nav);
        return header;
    }

    private Hyperlink navLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", 14));
        link.setTextFill(Color.web(BRUN));
        link.setUnderline(false);
        link.setStyle("-fx-border-color: transparent;");
        return link;
    }

    // ═══════════════════════════════════════════════════════
    //  AFFICHAGE DES PRODUITS
    // ═══════════════════════════════════════════════════════

    private void afficherProduits(List<Produit> produits) {
        productGrid.getChildren().clear();

        if (produits == null || produits.isEmpty()) {
            Text empty = new Text("Aucun produit disponible pour le moment.");
            empty.setFont(Font.font("Georgia", 16));
            empty.setFill(Color.web(BRUN_LIGHT));
            productGrid.getChildren().add(empty);
            return;
        }

        for (Produit p : produits) {
            productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(15);
        card.setPrefWidth(220);
        card.setPadding(new Insets(0, 0, 20, 0));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // ── Image ──────────────────────────────────────────
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(220, 260);
        imageContainer.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 10 10 0 0;");

        Text initial = new Text(p.getNom().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Georgia", 40));
        initial.setFill(Color.web(BRUN_LIGHT, 0.5));

        // Chercher l'image : d'abord sur le produit, sinon sur le premier format
        String imageUrl = p.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && p.getFormats() != null && !p.getFormats().isEmpty()) {
            imageUrl = p.getFormats().get(0).getImageUrl();
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 220, 260, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(220);
                iv.setFitHeight(260);
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        initial.setVisible(false);
                    }
                });
                Rectangle clip = new Rectangle(220, 260);
                clip.setArcWidth(20); clip.setArcHeight(20);
                iv.setClip(clip);
                imageContainer.getChildren().addAll(initial, iv);
            } catch (Exception e) {
                imageContainer.getChildren().add(initial);
            }
        } else {
            imageContainer.getChildren().add(initial);
        }

        // ── Infos ───────────────────────────────────────────
        VBox info = new VBox(6);
        info.setPadding(new Insets(0, 15, 0, 15));
        info.setAlignment(Pos.CENTER_LEFT);

        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setFill(Color.web(BRUN));
        name.setWrappingWidth(190);

        // Prix : depuis le premier format disponible
        String prixAffiche = "—";
        if (p.getFormats() != null && !p.getFormats().isEmpty()) {
            double prix = p.getFormats().get(0).getPrix();
            prixAffiche = String.format("%.2f MAD", prix);

            // Si plusieurs formats → afficher "Dès X MAD"
            if (p.getFormats().size() > 1) {
                double min = p.getFormats().stream()
                        .mapToDouble(ProductFormat::getPrix).min().orElse(prix);
                prixAffiche = String.format("Dès %.2f MAD", min);
            }
        } else if (p.getPrix() > 0) {
            prixAffiche = String.format("%.2f MAD", p.getPrix());
        }

        Text price = new Text(prixAffiche);
        price.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        price.setFill(Color.web(BRUN_LIGHT));

        // Variantes disponibles (ex: "100g, 200g")
        if (p.getFormats() != null && p.getFormats().size() > 1) {
            StringBuilder variants = new StringBuilder();
            for (ProductFormat fmt : p.getFormats()) {
                if (fmt.getLabelValues() != null && !fmt.getLabelValues().isEmpty()) {
                    if (variants.length() > 0) variants.append(", ");
                    variants.append(fmt.getLabelValues().get(0).getValeur());
                }
            }
            if (variants.length() > 0) {
                Text variantText = new Text(variants.toString());
                variantText.setFont(Font.font("Georgia", 11));
                variantText.setFill(Color.web(BRUN_LIGHT, 0.8));
                variantText.setWrappingWidth(190);
                info.getChildren().add(variantText);
            }
        }

        // ── Bouton Acheter ──────────────────────────────────
        boolean hasFormats = p.getFormats() != null && !p.getFormats().isEmpty();
        boolean connecte   = userId != -1;

        Button btnBuy = new Button(connecte ? "Ajouter au panier" : "Se connecter");
        btnBuy.setMaxWidth(Double.MAX_VALUE);
        btnBuy.setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER +
                "; -fx-border-radius: 5; -fx-text-fill: " + BRUN + "; -fx-font-family: 'Georgia';");
        btnBuy.setCursor(javafx.scene.Cursor.HAND);

        if (connecte && hasFormats) {
            btnBuy.setOnAction(e -> controller.ajouterAuPanier(p));
            btnBuy.setOnMouseEntered(e -> btnBuy.setStyle(
                    "-fx-background-color: " + SAUGE + "; -fx-text-fill: white; " +
                            "-fx-border-color: " + SAUGE + "; -fx-border-radius: 5; -fx-font-family: 'Georgia';"
            ));
            btnBuy.setOnMouseExited(e -> btnBuy.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: " + BORDER +
                            "; -fx-border-radius: 5; -fx-text-fill: " + BRUN + "; -fx-font-family: 'Georgia';"
            ));
        } else if (!hasFormats) {
            btnBuy.setDisable(true);
            btnBuy.setText("Indisponible");
        }

        info.getChildren().addAll(name, price, btnBuy);
        card.getChildren().addAll(imageContainer, info);

        DropShadow shadow = new DropShadow(10, Color.web(BRUN, 0.05));
        card.setEffect(shadow);

        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════

    private void ouvrirPanier() {
        try {
            new PanierView(userId).start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}