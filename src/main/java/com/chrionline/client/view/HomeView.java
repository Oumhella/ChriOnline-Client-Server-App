package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.Produit;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.util.List;

public class HomeView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private Stage primaryStage;
    private FlowPane featuredGrid;
    private MenuButton btnNotifications;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("ChriOnline — L'Art de l'Organique");

        VBox layout = new VBox(0);
        layout.setStyle("-fx-background-color: " + CREME + ";");

        // --- Header Centralisé ---
        layout.getChildren().add(HeaderComponent.build(stage, "Accueil", btn -> this.btnNotifications = btn));
        
        // --- UDP Notifications (Same as Catalogue) ---
        setupUdpListener();

        // ── Content ──────────────────────────────────────────
        VBox content = new VBox(0);
        
        // 1. Hero
        content.getChildren().add(buildHeroSection(stage));
        
        // 2. Featured Section
        content.getChildren().add(buildFeaturedSection());
        
        // 3. Why Us
        content.getChildren().add(buildFeaturesSection());

        // 4. Footer
        content.getChildren().add(buildFooter());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        layout.getChildren().add(scroll);

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(layout, 1100, 800);
            stage.setScene(scene);
        } else {
            scene.setRoot(layout);
        }

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.isShiftDown() && e.getCode() == javafx.scene.input.KeyCode.A) {
                try {
                    new com.chrionline.admin.view.AdminLoginFrame().show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        if (!stage.isShowing()) stage.show();
        
        loadFeaturedProducts();
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
        } catch (Exception ignored) {}
    }

    private StackPane buildHeroSection(Stage stage) {
        StackPane hero = new StackPane();
        hero.setPrefHeight(650);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(60, 20, 80, 20));
        hero.setStyle("-fx-background-color: linear-gradient(to bottom right, #FDFBF7, #F5EFEB);");

        // Background Blobs / Circles
        Circle c1 = new Circle(280, Color.web(SAUGE, 0.12));
        StackPane.setAlignment(c1, Pos.TOP_RIGHT);
        StackPane.setMargin(c1, new Insets(-80, -120, 0, 0));
        
        Circle c2 = new Circle(180, Color.web(TERRACOTTA, 0.06));
        StackPane.setAlignment(c2, Pos.BOTTOM_LEFT);
        StackPane.setMargin(c2, new Insets(0, 0, -100, -60));

        // Floating Animations
        applyFloatAnimation(c1, 4, 40, -20);
        applyFloatAnimation(c2, 6, -30, 15);

        VBox textContent = new VBox(28);
        textContent.setAlignment(Pos.CENTER);
        textContent.setMaxWidth(850);

        Text intro = new Text("PURETÉ • QUALITÉ • ENGAGEMENT");
        intro.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        intro.setFill(Color.web(TERRACOTTA));
        intro.setOpacity(0.8);
        intro.setTranslateY(20);

        Text title = new Text("Le meilleur de l'organique,\ndirectement chez vous.");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 58));
        title.setFill(Color.web(BRUN));
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        title.setLineSpacing(-5);
        title.setTranslateY(25);

        Text desc = new Text("Découvrez une sélection rigoureuse de produits naturels et éco-responsables, conçus pour sublimer votre quotidien en harmonie avec la nature.");
        desc.setFont(Font.font("Georgia", 20));
        desc.setFill(Color.web(BRUN, 0.7));
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        desc.setWrappingWidth(700);
        desc.setTranslateY(30);

        Button btnShop = new Button("DÉCOUVRIR LE CATALOGUE");
        btnShop.setCursor(Cursor.HAND);
        btnShop.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-padding: 20 55; -fx-background-radius: 50; -fx-font-size: 16px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        btnShop.setTranslateY(40);
        
        DropShadow ds = new DropShadow(25, Color.web(BRUN, 0.25));
        btnShop.setEffect(ds);

        btnShop.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnShop);
            st.setToX(1.05); st.setToY(1.05); st.play();
            btnShop.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; -fx-padding: 20 55; -fx-background-radius: 50; -fx-font-size: 16px; -fx-font-weight: bold;");
        });
        btnShop.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnShop);
            st.setToX(1.0); st.setToY(1.0); st.play();
            btnShop.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-padding: 20 55; -fx-background-radius: 50; -fx-font-size: 16px; -fx-font-weight: bold;");
        });
        
        btnShop.setOnAction(e -> new CatalogueView().start(stage));

        textContent.getChildren().addAll(intro, title, desc, btnShop);
        hero.getChildren().addAll(c1, c2, textContent);

        // Entrance Staggered Animation
        int d = 0;
        for (javafx.scene.Node n : textContent.getChildren()) {
            n.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(900), n);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(250 * d));
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(900), n);
            tt.setToY(0);
            tt.setDelay(Duration.millis(250 * d));
            
            new ParallelTransition(ft, tt).play();
            d++;
        }

        return hero;
    }

    private VBox buildFeaturedSection() {
        VBox section = new VBox(45);
        section.setPadding(new Insets(100, 60, 40, 60));
        section.setAlignment(Pos.TOP_CENTER);
        section.setStyle("-fx-background-color: white;");

        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        Text title = new Text("Sélectionné pour vous");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        title.setFill(Color.web(BRUN));
        
        Rectangle line = new Rectangle(80, 3, Color.web(TERRACOTTA));
        line.setArcWidth(3); line.setArcHeight(3);
        titleBox.getChildren().addAll(title, line);

        featuredGrid = new FlowPane(35, 35);
        featuredGrid.setAlignment(Pos.CENTER);
        featuredGrid.setPadding(new Insets(20, 0, 40, 0));

        section.getChildren().addAll(titleBox, featuredGrid);
        return section;
    }

    private void loadFeaturedProducts() {
        int uid = com.chrionline.client.session.SessionManager.getInstance().getUserId();
        CatalogueController ctrl = new CatalogueController(uid);
        new Thread(() -> {
            List<Produit> all = ctrl.recupererProduits();
            if (all != null && !all.isEmpty()) {
                // Take 4 random or top products
                List<Produit> featured = all.stream().limit(4).toList();
                Platform.runLater(() -> {
                    featuredGrid.getChildren().clear();
                    for (Produit p : featured) {
                        featuredGrid.getChildren().add(createSimplePremiumCard(p));
                    }
                });
            } else {
                Platform.runLater(() -> {
                    featuredGrid.getChildren().clear();
                    Label placeholder = new Label("Aucun produit disponible pour le moment.");
                    placeholder.setFont(javafx.scene.text.Font.font("Georgia", 16));
                    placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                    featuredGrid.getChildren().add(placeholder);
                });
            }
        }).start();
    }

    private VBox createSimplePremiumCard(Produit p) {
        VBox card = new VBox(0);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: " + BORDER + "; -fx-border-radius: 15; -fx-border-width: 0.5;");
        card.setCursor(Cursor.HAND);
        card.setEffect(new DropShadow(15, Color.web(BRUN, 0.08)));

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(220, 240);
        imgBox.setStyle("-fx-background-color: #F8F5F2; -fx-background-radius: 15 15 0 0;");
        
        String url = p.getImageUrl();
        if ((url == null || url.isEmpty()) && p.getFormats() != null && !p.getFormats().isEmpty()) {
            url = p.getFormats().get(0).getImageUrl();
        }
        
        if (url != null && !url.isEmpty()) {
            try {
                Image img = new Image(url, 220, 240, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(220); iv.setFitHeight(240); iv.setPreserveRatio(true);
                Rectangle clip = new Rectangle(220, 240);
                clip.setArcWidth(30); clip.setArcHeight(30);
                iv.setClip(clip);
                imgBox.getChildren().add(iv);
            } catch (Exception e) {
                Text init = new Text(p.getNom().substring(0,1).toUpperCase());
                init.setFont(Font.font("Georgia", 40));
                init.setFill(Color.web(BRUN_LIGHT, 0.4));
                imgBox.getChildren().add(init);
            }
        }

        VBox info = new VBox(5);
        info.setPadding(new Insets(15, 15, 15, 15));
        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        name.setFill(Color.web(BRUN));
        name.setWrappingWidth(190);
        
        double minP = (p.getFormats() != null && !p.getFormats().isEmpty()) 
            ? p.getFormats().stream().mapToDouble(ProductFormat::getPrix).min().orElse(0) : p.getPrix();
        Text price = new Text(String.format("%.2f MAD", minP));
        price.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        price.setFill(Color.web(TERRACOTTA));
        
        info.getChildren().addAll(name, price);
        card.getChildren().addAll(imgBox, info);
        
        card.setOnMouseClicked(e -> new CatalogueView().start(primaryStage));
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private VBox buildFeaturesSection() {
        VBox sec = new VBox(60);
        sec.setPadding(new Insets(100, 60, 120, 60));
        sec.setAlignment(Pos.CENTER);
        sec.setStyle("-fx-background-color: white;");

        HBox grid = new HBox(50);
        grid.setAlignment(Pos.CENTER);

        grid.getChildren().addAll(
            createFeatureCard("🌿", "100% Naturel", "Éthique & Durable"),
            createFeatureCard("💎", "Premium", "Excellence Artisanale"),
            createFeatureCard("🚀", "Livraison", "Express & Sécurisée")
        );

        sec.getChildren().add(grid);
        return sec;
    }

    private VBox createFeatureCard(String icon, String title, String sub) {
        VBox v = new VBox(15);
        v.setAlignment(Pos.CENTER);
        v.setPrefWidth(250);
        v.setPadding(new Insets(30));
        v.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 20;");
        
        Text i = new Text(icon); i.setFont(Font.font(40));
        Text t = new Text(title); t.setFont(Font.font("Georgia", FontWeight.BOLD, 18)); t.setFill(Color.web(BRUN));
        Text s = new Text(sub); s.setFont(Font.font("Georgia", 14)); s.setFill(Color.web(BRUN, 0.6));
        
        v.getChildren().addAll(i, t, s);
        return v;
    }

    private VBox buildFooter() {
        VBox f = new VBox(25);
        f.setPadding(new Insets(60));
        f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-background-color: " + BRUN + ";");
        Text l = new Text("ChriOnline"); l.setFont(Font.font("Georgia", FontWeight.BOLD, 22)); l.setFill(Color.web(CREME));
        Text c = new Text("© 2026 ChriOnline • Expérience Client Premium"); c.setFont(Font.font("Georgia", 13)); c.setFill(Color.web(CREME, 0.4));
        f.getChildren().addAll(l, c);
        return f;
    }

    private void applyFloatAnimation(Circle c, double sec, double tx, double ty) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(sec), c);
        tt.setByX(tx); tt.setByY(ty);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    public static void main(String[] args) { launch(args); }
}
