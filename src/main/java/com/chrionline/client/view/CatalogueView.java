package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
    private static final String TERRA_HOVER= "#A0522D";
    private static final String BORDER     = "#E8E0D5";

    CatalogueController controller;  // package-private pour ProductDetailView
    private FlowPane    productGrid;
    Stage               primaryStage; // package-private pour ProductDetailView
    private int         userId;

    public CatalogueView() {
        this.userId = com.chrionline.client.session.SessionManager.getInstance().getUserId();
    }

    public CatalogueView(int explicitUserId) {
        this.userId = explicitUserId;
        // Si explicitUserId est passé, on force la session
        if (explicitUserId > 0) {
            com.chrionline.client.session.SessionManager.getInstance().setUser(
                java.util.Map.of("userId", explicitUserId)
            );
        }
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.userId = com.chrionline.client.session.SessionManager.getInstance().getUserId();
        this.controller   = new CatalogueController(userId);
        stage.setTitle("ChriOnline — Catalogue");
        stage.setScene(new Scene(buildCatalogueRoot(), 1100, 800));
        stage.show();
    }

    // ═══════════════════════════════════════════════════════
    //  BUILD ROOT
    // ═══════════════════════════════════════════════════════

    public VBox buildCatalogueRoot() {
        // Recréer le controller si nécessaire (appel depuis retourCatalogue)
        if (controller == null) controller = new CatalogueController(userId);

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
        scroll.setStyle("-fx-background: " + CREME
                + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        // Chargement dans un thread séparé → UI non bloquée
        new Thread(() -> {
            List<Integer> ids = (userId != -1) ? controller.recupererWishlistIds(userId) : new java.util.ArrayList<>();
            List<Produit> produits = controller.recupererProduits();
            Platform.runLater(() -> {
                wishlistIds.clear();
                if (ids != null) wishlistIds.addAll(ids);
                afficherProduits(produits);
            });
        }).start();

        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════

    private HBox buildHeader() {
        HBox header = new HBox(40);
        header.setPadding(new Insets(30, 60, 30, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color: transparent transparent " + BORDER
                + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("SHE SOAP");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(30);
        nav.setAlignment(Pos.CENTER);
        nav.getChildren().addAll(navLink("Accueil"), navLink("Assortiment"), navLink("Livraison"));

        // Bouton panier et déconnexion — visible seulement si connecté
        if (userId != -1) {
            Button btnPanier = new Button("🛒 Mon panier");
            btnPanier.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            btnPanier.setStyle(panierBtnStyle(TERRACOTTA));
            btnPanier.setCursor(Cursor.HAND);
            btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(panierBtnStyle(TERRA_HOVER)));
            btnPanier.setOnMouseExited(e  -> btnPanier.setStyle(panierBtnStyle(TERRACOTTA)));
            btnPanier.setOnAction(e -> ouvrirPanier());

            Button btnLogout = new Button("🚪 Déconnexion");
            btnLogout.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            btnLogout.setStyle(panierBtnStyle(BRUN_LIGHT));
            btnLogout.setCursor(Cursor.HAND);
            btnLogout.setOnMouseEntered(e -> btnLogout.setStyle(panierBtnStyle(BRUN)));
            btnLogout.setOnMouseExited(e  -> btnLogout.setStyle(panierBtnStyle(BRUN_LIGHT)));
            btnLogout.setOnAction(e -> deconnecter());

            nav.getChildren().addAll(btnPanier, btnLogout);
        } else {
            Button btnLogin = new Button("🔑 Connexion");
            btnLogin.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            btnLogin.setStyle(panierBtnStyle(SAUGE_DARK));
            btnLogin.setCursor(Cursor.HAND);
            btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(panierBtnStyle(SAUGE)));
            btnLogin.setOnMouseExited(e  -> btnLogin.setStyle(panierBtnStyle(SAUGE_DARK)));
            btnLogin.setOnAction(e -> {
                try {
                    new com.chrionline.client.view.ConnexionView().start(primaryStage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            nav.getChildren().add(btnLogin);
        }

        header.getChildren().addAll(logo, spacer, nav);
        return header;
    }

    private void deconnecter() {
        com.chrionline.client.session.SessionManager.getInstance().clear();
        try {
            new com.chrionline.client.view.ConnexionView().start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Hyperlink navLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", 14));
        link.setTextFill(Color.web(BRUN));
        link.setUnderline(false);
        link.setStyle("-fx-border-color: transparent;");
        return link;
    }

    private java.util.Set<Integer> wishlistIds = new java.util.HashSet<>();

    private void afficherProduits(List<Produit> produits) {
        productGrid.getChildren().clear();
        if (produits == null || produits.isEmpty()) {
            Text empty = new Text("Aucun produit disponible pour le moment.");
            empty.setFont(Font.font("Georgia", 16));
            empty.setFill(Color.web(BRUN_LIGHT));
            productGrid.getChildren().add(empty);
        } else {
            for (Produit p : produits) productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(15);
        card.setPrefWidth(220);
        card.setPadding(new Insets(0, 0, 20, 0));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        card.setCursor(Cursor.HAND);
        card.setEffect(new DropShadow(10, Color.web(BRUN, 0.05)));

        // ── Image ───────────────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(220, 260);
        imgBox.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 10 10 0 0;");

        Text initial = new Text(p.getNom().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Georgia", 40));
        initial.setFill(Color.web(BRUN_LIGHT, 0.5));

        String imageUrl = p.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank())
                && p.getFormats() != null && !p.getFormats().isEmpty()) {
            imageUrl = p.getFormats().get(0).getImageUrl();
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 220, 260, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(220); iv.setFitHeight(260); iv.setPreserveRatio(true);
                img.progressProperty().addListener((obs, old, prog) -> {
                    if (prog.doubleValue() >= 1.0 && !img.isError()) initial.setVisible(false);
                });
                Rectangle clip = new Rectangle(220, 260);
                clip.setArcWidth(20); clip.setArcHeight(20);
                iv.setClip(clip);
                imgBox.getChildren().addAll(initial, iv);
            } catch (Exception ignored) {
                imgBox.getChildren().add(initial);
            }
        } else {
            imgBox.getChildren().add(initial);
        }

        // --- Heart Icon for Wishlist ---
        Text heartIcon = new Text(wishlistIds.contains(p.getIdProduit()) ? "♥" : "♡");
        heartIcon.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        // Use an accent color if filled, else soft brown
        heartIcon.setFill(wishlistIds.contains(p.getIdProduit()) ? Color.web(TERRACOTTA) : Color.web(BRUN_LIGHT, 0.7));
        heartIcon.setCursor(Cursor.HAND);
        
        StackPane.setAlignment(heartIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(heartIcon, new Insets(10, 10, 0, 0));
        
        // Wishlist click handler
        heartIcon.setOnMouseClicked(e -> {
            e.consume(); // prevent opening detail view
            int userId = com.chrionline.client.session.SessionManager.getInstance().getUserId();
            if (userId == -1) {
                System.out.println("Veuillez vous connecter pour utiliser la wishlist.");
                return;
            }
            
            boolean currentlyInWishlist = wishlistIds.contains(p.getIdProduit());
            if (currentlyInWishlist) {
                if (controller.supprimerWishlist(userId, p.getIdProduit())) {
                    wishlistIds.remove(p.getIdProduit());
                    heartIcon.setText("♡");
                    heartIcon.setFill(Color.web(BRUN_LIGHT, 0.7));
                }
            } else {
                if (controller.ajouterWishlist(userId, p.getIdProduit())) {
                    wishlistIds.add(p.getIdProduit());
                    heartIcon.setText("♥");
                    heartIcon.setFill(Color.web(TERRACOTTA));
                }
            }
        });
        
        imgBox.getChildren().add(heartIcon);

        // ── Info ────────────────────────────────────────────
        VBox info = new VBox(6);
        info.setPadding(new Insets(0, 15, 0, 15));

        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setFill(Color.web(BRUN));
        name.setWrappingWidth(190);

        // Prix depuis les formats
        String prixAffiche = "—";
        if (p.getFormats() != null && !p.getFormats().isEmpty()) {
            double min = p.getFormats().stream()
                    .mapToDouble(ProductFormat::getPrix).min().orElse(0);
            prixAffiche = p.getFormats().size() > 1
                    ? String.format("Dès %.2f MAD", min)
                    : String.format("%.2f MAD", min);
        } else if (p.getPrix() > 0) {
            prixAffiche = String.format("%.2f MAD", p.getPrix());
        }

        Text priceText = new Text(prixAffiche);
        priceText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        priceText.setFill(Color.web(BRUN_LIGHT));

        Button btnDetail = new Button("Voir détails");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setStyle(detailBtnStyle(SAUGE));
        btnDetail.setCursor(Cursor.HAND);
        btnDetail.setOnMouseEntered(e -> btnDetail.setStyle(detailBtnStyle(SAUGE_DARK)));
        btnDetail.setOnMouseExited(e  -> btnDetail.setStyle(detailBtnStyle(SAUGE)));
        btnDetail.setOnAction(e -> ouvrirDetail(p));

        info.getChildren().addAll(name, priceText, btnDetail);
        card.getChildren().addAll(imgBox, info);

        card.setOnMouseClicked(e -> ouvrirDetail(p));
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(62,44,30,0.12), 18, 0, 0, 5);"));
        card.setOnMouseExited(e  -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;"));

        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════

    /**
     * Ouvre la vue détail.
     * On recharge le produit complet (avec formats et labels) depuis le serveur
     * car findAll() ne charge que les infos de base (sans formats).
     */
    private void ouvrirDetail(Produit p) {
        if (primaryStage == null) return;

        // Charger le produit complet avec ses formats
        Produit produitComplet = controller.recupererProduitDetail(p.getIdProduit());

        // Fallback : si le rechargement échoue, on utilise l'objet de base
        Produit produitAAfficher = (produitComplet != null) ? produitComplet : p;

        ProductDetailView detail =
                new ProductDetailView(produitAAfficher, primaryStage, this, controller, userId);
        primaryStage.getScene().setRoot(detail.build());
    }

    /**
     * Appelée par ProductDetailView pour revenir au catalogue.
     */
    public void retourCatalogue() {
        if (primaryStage == null) return;
        primaryStage.getScene().setRoot(buildCatalogueRoot());
    }

    private void ouvrirPanier() {
        try { new PanierView(userId).start(primaryStage); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════
    //  STYLES
    // ═══════════════════════════════════════════════════════

    private String panierBtnStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: white;"
                + "-fx-background-radius: 20; -fx-padding: 8 18;";
    }

    private String detailBtnStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: white;"
                + "-fx-font-family: Georgia; -fx-font-size: 13px; -fx-background-radius: 6;";
    }

    public static void main(String[] args) { launch(args); }
}