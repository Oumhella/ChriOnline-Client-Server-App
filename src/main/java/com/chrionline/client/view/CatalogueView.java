package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
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
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class CatalogueView extends Application {

    private static final String CREME      = "#FDFBF7";
    private static final String BRUN       = "#3E2C1E";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String BORDER     = "#E8E0D5";

    private CatalogueController controller;
    private FlowPane productGrid;
    private Stage primaryStage;
    private int userId;
    private boolean isWishlistMode = false;
    private Set<Integer> wishlistIds = new HashSet<>();
    private MenuButton btnNotifications;

    public CatalogueView() {
        this.userId = com.chrionline.client.session.SessionManager.getInstance().getUserId();
    }

    public CatalogueView(int explicitUserId) {
        this(explicitUserId, false);
    }

    public CatalogueView(int explicitUserId, boolean isWishlistMode) {
        this.userId = explicitUserId;
        this.isWishlistMode = isWishlistMode;
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
        this.controller = new CatalogueController(userId);
        
        // --- UDP Notifications ---
        setupUdpListener();

        stage.setTitle(isWishlistMode ? "ChriOnline — Mes Favoris" : "ChriOnline — Catalogue");
        Parent root = buildCatalogueRoot();
        
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
        }
        if (!stage.isShowing()) stage.show();
    }

    private void setupUdpListener() {
        try {
            com.chrionline.client.network.Client networkClient = com.chrionline.client.network.Client.getInstance("127.0.0.1", 12345);
            networkClient.setNotificationListener(notification -> {
                com.chrionline.client.session.SessionManager.getInstance().addNotification(notification);
                Platform.runLater(() -> {
                    if (primaryStage != null) {
                        com.chrionline.client.view.utils.NotificationToast.show(primaryStage, notification);
                    }
                    if (btnNotifications != null) {
                        HeaderComponent.refreshNotificationMenu(btnNotifications);
                        int unread = com.chrionline.client.session.SessionManager.getInstance().getUnreadNotificationsCount();
                        btnNotifications.setText("🔔 (" + unread + ")");
                    }
                });
            });
        } catch (Exception e) {
            System.err.println("[UDP] Erreur setup : " + e.getMessage());
        }
    }

    public VBox buildCatalogueRoot() {
        if (controller == null) controller = new CatalogueController(userId);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");
        
        // Header
        root.getChildren().add(HeaderComponent.build(primaryStage, "Catalogue", btn -> this.btnNotifications = btn));

        productGrid = new FlowPane(35, 35);
        productGrid.setPadding(new Insets(50));
        productGrid.setAlignment(Pos.TOP_CENTER);
        productGrid.setStyle("-fx-background-color: " + CREME + ";");

        ScrollPane scroll = new ScrollPane(productGrid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        root.getChildren().add(scroll);

        chargerDonnees();

        return root;
    }

    private void chargerDonnees() {
        new Thread(() -> {
            List<Integer> ids = (userId != -1) ? controller.recupererWishlistIds(userId) : new ArrayList<>();
            List<Produit> produitsInit = controller.recupererProduits();
            Platform.runLater(() -> {
                wishlistIds.clear();
                if (ids != null) wishlistIds.addAll(ids);
                
                List<Produit> produitsAffiches = produitsInit;
                if (isWishlistMode) {
                    produitsAffiches = produitsInit.stream()
                            .filter(p -> wishlistIds.contains(p.getIdProduit()))
                            .toList();
                }
                
                afficherProduits(produitsAffiches);
            });
        }).start();
    }

    private void afficherProduits(List<Produit> produits) {
        productGrid.getChildren().clear();
        if (produits == null || produits.isEmpty()) {
            VBox emptyBox = new VBox(20);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(100, 0, 0, 0));
            Text empty = new Text(isWishlistMode ? "Votre liste de favoris est vide." : "Aucun produit trouvé.");
            empty.setFont(Font.font("Georgia", 20));
            empty.setFill(Color.web(BRUN_LIGHT));
            emptyBox.getChildren().add(empty);
            productGrid.getChildren().add(emptyBox);
        } else {
            for (Produit p : produits) {
                productGrid.getChildren().add(createProductCard(p));
            }
        }
    }

    private VBox createProductCard(Produit p) {
        VBox card = new VBox(0);
        card.setPrefWidth(240);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: " + BORDER + "; -fx-border-radius: 15; -fx-border-width: 0.5;");
        card.setCursor(Cursor.HAND);
        
        DropShadow ds = new DropShadow(15, Color.web(BRUN, 0.08));
        ds.setOffsetY(4);
        card.setEffect(ds);

        // ── Image Section ───────────────────────────────────
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(240, 300);
        imageContainer.setStyle("-fx-background-color: #F8F5F2; -fx-background-radius: 15 15 0 0;");

        Text initial = new Text(p.getNom().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Georgia", 50));
        initial.setFill(Color.web(BRUN_LIGHT, 0.4));

        String imageUrl = p.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && p.getFormats() != null && !p.getFormats().isEmpty()) {
            imageUrl = p.getFormats().get(0).getImageUrl();
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 240, 300, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(240); iv.setFitHeight(300); iv.setPreserveRatio(true);
                
                iv.setOpacity(0);
                img.progressProperty().addListener((obs, old, prog) -> {
                    if (prog.doubleValue() >= 1.0 && !img.isError()) {
                        FadeTransition ft = new FadeTransition(Duration.millis(500), iv);
                        ft.setToValue(1.0);
                        ft.play();
                        initial.setVisible(false);
                    }
                });

                Rectangle clip = new Rectangle(240, 300);
                clip.setArcWidth(30); clip.setArcHeight(30);
                iv.setClip(clip);
                imageContainer.getChildren().addAll(initial, iv);
            } catch (Exception e) {
                imageContainer.getChildren().add(initial);
            }
        } else {
            imageContainer.getChildren().add(initial);
        }

        // --- Glassmorphism Price Tag ---
        HBox priceTag = new HBox();
        priceTag.setAlignment(Pos.CENTER);
        priceTag.setPadding(new Insets(6, 12, 6, 12));
        priceTag.setStyle("-fx-background-color: rgba(255, 255, 255, 0.75); -fx-background-radius: 20; -fx-backdrop-filter: blur(10px); -fx-border-color: rgba(255, 255, 255, 0.5); -fx-border-radius: 20;");
        
        double minPrice = (p.getFormats() != null && !p.getFormats().isEmpty()) 
            ? p.getFormats().stream().mapToDouble(ProductFormat::getPrix).min().orElse(0) 
            : p.getPrix();
        
        Text price = new Text(String.format("%.2f MAD", minPrice));
        price.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        price.setFill(Color.web(BRUN));
        priceTag.getChildren().add(price);
        
        StackPane.setAlignment(priceTag, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(priceTag, new Insets(0, 15, 15, 0));

        // --- Heart Icon ---
        Text heart = new Text(wishlistIds.contains(p.getIdProduit()) ? "♥" : "♡");
        heart.setFont(Font.font("Arial", 24));
        heart.setFill(wishlistIds.contains(p.getIdProduit()) ? Color.web(TERRACOTTA) : Color.web(BRUN_LIGHT, 0.8));
        heart.setCursor(Cursor.HAND);
        heart.setOnMouseClicked(e -> {
            e.consume();
            toggleWishlist(p, heart, card);
        });
        
        StackPane.setAlignment(heart, Pos.TOP_RIGHT);
        StackPane.setMargin(heart, new Insets(12, 12, 0, 0));

        imageContainer.getChildren().addAll(priceTag, heart);

        // ── Info Section ────────────────────────────────────
        VBox info = new VBox(5);
        info.setPadding(new Insets(15, 18, 20, 18));
        info.setAlignment(Pos.TOP_LEFT);

        String catName = (p.getCategorie() != null && p.getCategorie().getNom() != null) ? p.getCategorie().getNom() : "PRODUIT";
        Text cat = new Text(catName.toUpperCase());
        cat.setFont(Font.font("Georgia", 10));
        cat.setFill(Color.web(BRUN_LIGHT));
        cat.setOpacity(0.8);

        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        name.setFill(Color.web(BRUN));
        name.setWrappingWidth(200);

        info.getChildren().addAll(cat, name);
        card.getChildren().addAll(imageContainer, info);

        // --- Hover ---
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(250), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
            card.setEffect(new DropShadow(30, Color.web(BRUN, 0.12)));
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(250), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setEffect(new DropShadow(15, Color.web(BRUN, 0.08)));
        });

        card.setOnMouseClicked(e -> ouvrirDetail(p));

        return card;
    }

    private void toggleWishlist(Produit p, Text heart, VBox card) {
        int uid = com.chrionline.client.session.SessionManager.getInstance().getUserId();
        if (uid == -1) return;

        boolean active = wishlistIds.contains(p.getIdProduit());
        if (active) {
            if (controller.supprimerWishlist(uid, p.getIdProduit())) {
                wishlistIds.remove(p.getIdProduit());
                heart.setText("♡");
                heart.setFill(Color.web(BRUN_LIGHT, 0.8));
                if (isWishlistMode) productGrid.getChildren().remove(card);
            }
        } else {
            if (controller.ajouterWishlist(uid, p.getIdProduit())) {
                wishlistIds.add(p.getIdProduit());
                heart.setText("♥");
                heart.setFill(Color.web(TERRACOTTA));
            }
        }
    }

    private void ouvrirDetail(Produit p) {
        if (primaryStage == null) return;
        Produit full = controller.recupererProduitDetail(p.getIdProduit());
        ProductDetailView detail = new ProductDetailView(full != null ? full : p, primaryStage, this, controller, userId);
        primaryStage.getScene().setRoot(detail.build());
    }

    public void retourCatalogue() {
        if (primaryStage != null) primaryStage.getScene().setRoot(buildCatalogueRoot());
    }

    public static void main(String[] args) { launch(args); }
}