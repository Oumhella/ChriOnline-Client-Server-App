package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.Produit;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    // Palette Organique & Minimaliste
    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private CatalogueController controller;
    private FlowPane productGrid;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.controller = new CatalogueController();
        
        stage.setTitle("ChriOnline — Catalogue");

        Scene scene = new Scene(buildCatalogueRoot(), 1100, 800);
        stage.setScene(scene);
        stage.show();
    }

    /** Builds the full catalogue root layout. */
    public VBox buildCatalogueRoot() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // ── Header ────────────────────────────────────────────
        root.getChildren().add(buildHeader());

        // ── Content ───────────────────────────────────────────
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

        // ── Chargement des produits ──────────────────────────
        chargerProduits();

        return root;
    }

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
                navLink("Livraison"),
                navLink("Blog")
        );

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

    private void chargerProduits() {
        List<Produit> produits = controller.recupererProduits();
        productGrid.getChildren().clear();

        if (produits.isEmpty()) {
            Text empty = new Text("Aucun produit disponible pour le moment.");
            empty.setFont(Font.font("Georgia", 16));
            empty.setFill(Color.web(BRUN_LIGHT));
            productGrid.getChildren().add(empty);
        } else {
            for (Produit p : produits) {
                productGrid.getChildren().add(createProductCard(p));
            }
        }
    }

    private VBox createProductCard(Produit p) {

        VBox card = new VBox(15);
        card.setPrefWidth(220);
        card.setPadding(new Insets(0, 0, 20, 0));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        card.setCursor(Cursor.HAND);

        // Use direct imageUrl and prix from Produit
        String imageUrl = p.getImageUrl();
        String prix = (p.getPrix() > 0) ? String.format("%.2f MAD", p.getPrix()) : "N/A";

        // ── Image ─────────────────────────────
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(220, 260);
        imageContainer.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 10 10 0 0;");

        Text initial = new Text(p.getNom().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Georgia", 40));
        initial.setFill(Color.web(BRUN_LIGHT, 0.5));

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 220, 260, true, true, true);
                ImageView iv = new ImageView(img);

                iv.setFitWidth(220);
                iv.setFitHeight(260);

                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        initial.setVisible(false);
                    }
                });

                Rectangle clip = new Rectangle(220, 260);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                iv.setClip(clip);

                imageContainer.getChildren().addAll(initial, iv);

            } catch (Exception e) {
                imageContainer.getChildren().add(initial);
            }
        } else {
            imageContainer.getChildren().add(initial);
        }

        // ── Info ─────────────────────────────
        VBox info = new VBox(8);
        info.setPadding(new Insets(0, 15, 0, 15));

        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setFill(Color.web(BRUN));

        // Category label
        if (p.getCategorie() != null && p.getCategorie().getNom() != null) {
            Text categoryLabel = new Text(p.getCategorie().getNom());
            categoryLabel.setFont(Font.font("Georgia", 12));
            categoryLabel.setFill(Color.web(SAUGE_DARK));
            info.getChildren().add(categoryLabel);
        }

        Text priceText = new Text(prix);
        priceText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        priceText.setFill(Color.web(BRUN_LIGHT));

        Button btnDetail = new Button("Voir détails");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setStyle(
            "-fx-background-color: " + SAUGE + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-family: Georgia;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        btnDetail.setOnAction(e -> ouvrirDetailProduit(p.getIdProduit()));

        info.getChildren().addAll(name, priceText, btnDetail);

        // Click anywhere on card to open detail
        card.setOnMouseClicked(e -> ouvrirDetailProduit(p.getIdProduit()));

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;"
        ));

        card.getChildren().addAll(imageContainer, info);

        return card;
    }

    /** Opens the ProductDetailView for the given product ID. */
    private void ouvrirDetailProduit(int idProduit) {
        Produit detail = controller.recupererProduitDetail(idProduit);
        if (detail != null && primaryStage != null) {
            ProductDetailView detailView = new ProductDetailView(detail, primaryStage, this);
            Scene detailScene = new Scene(detailView.build(), 1100, 800);
            primaryStage.setScene(detailScene);
        } else {
            System.err.println("[CatalogueView] Impossible de charger le détail du produit #" + idProduit);
        }
    }

    /** Called by ProductDetailView to return to the catalogue. */
    public void retourCatalogue() {
        if (primaryStage != null) {
            Scene catalogueScene = new Scene(buildCatalogueRoot(), 1100, 800);
            primaryStage.setScene(catalogueScene);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
