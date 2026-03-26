package com.chrionline.client.view;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.text.FontPosture;

public class HomeView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Accueil");

        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: " + CREME + ";");

        // ── Header (Menu) ─────────────────────────────────────
        HBox header = buildHeader(stage);
        
        // ── Hero Section ──────────────────────────────────────
        StackPane hero = buildHeroSection(stage);
        
        // ── Features Section ──────────────────────────────────
        VBox features = buildFeaturesSection();

        // ── Footer ────────────────────────────────────────────
        VBox footer = buildFooter();

        mainContainer.getChildren().addAll(header, hero, features, footer);

        // ScrollPane pour englober toute la page
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + CREME + "; -fx-border-color: transparent; -fx-padding: 0;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Scene scene = new Scene(scrollPane, 1100, 800);
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildHeader(Stage stage) {
        HBox header = new HBox(40);
        header.setPadding(new Insets(25, 60, 25, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + CREME + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        logo.setFill(Color.web(BRUN));
        
        // Animation hover pour le logo
        logo.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), logo);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        logo.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), logo);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(35);
        nav.setAlignment(Pos.CENTER);
        
        Hyperlink hAcc = navLink("Accueil", true);
        Hyperlink hCat = navLink("Catalogue", false);
        Hyperlink hOrd = navLink("Mes Commandes", false);
        Hyperlink hAbo = navLink("À propos", false);
        
        hCat.setOnAction(e -> {
            try { new CatalogueView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        hOrd.setOnAction(e -> {
            try { new MesCommandesView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        hAbo.setOnAction(e -> {
            // Logique À propos si nécessaire
        });

        nav.getChildren().addAll(hAcc, hCat, hOrd, hAbo);
        
        // --- Avatar Profil (à droite) ---
        javafx.scene.layout.HBox rightControls = new javafx.scene.layout.HBox(20);
        rightControls.setAlignment(Pos.CENTER);
        
        boolean isLogged = com.chrionline.client.session.SessionManager.getInstance().isLogged();
        if (isLogged) {
            String prenom = com.chrionline.client.session.SessionManager.getInstance().getPrenom();
            String nom = com.chrionline.client.session.SessionManager.getInstance().getNom();
            String initials = "";
            if (prenom != null && !prenom.isEmpty()) initials += prenom.toUpperCase().charAt(0);
            if (nom != null && !nom.isEmpty()) initials += nom.toUpperCase().charAt(0);
            if (initials.isEmpty()) initials = "U";
            
            StackPane avatar = new StackPane();
            Circle circle = new Circle(18, Color.web(TERRACOTTA));
            Text initText = new Text(initials);
            initText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            initText.setFill(Color.WHITE);
            avatar.getChildren().addAll(circle, initText);
            avatar.setCursor(javafx.scene.Cursor.HAND);
            avatar.setOnMouseClicked(e -> {
                try { new ProfilView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            });
            avatar.setOnMouseEntered(e -> circle.setFill(Color.web(SAUGE_DARK)));
            avatar.setOnMouseExited(e -> circle.setFill(Color.web(TERRACOTTA)));
            
            rightControls.getChildren().add(avatar);
        }

        Button btnLogout = new Button(isLogged ? "Déconnexion" : "Connexion");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN + "; -fx-border-color: " + BRUN + "; -fx-border-radius: 20; -fx-font-family: 'Georgia'; -fx-padding: 8 20; -fx-font-size: 14px;");
        btnLogout.setCursor(javafx.scene.Cursor.HAND);
        
        // Effet hover pour le bouton de déconnexion
        btnLogout.setOnMouseEntered(e -> {
            btnLogout.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: " + CREME + "; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-family: 'Georgia'; -fx-padding: 8 20; -fx-font-size: 14px;");
        });
        btnLogout.setOnMouseExited(e -> {
            btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN + "; -fx-border-color: " + BRUN + "; -fx-border-radius: 20; -fx-font-family: 'Georgia'; -fx-padding: 8 20; -fx-font-size: 14px;");
        });
        
        if (isLogged) {
            btnLogout.setOnAction(e -> {
                com.chrionline.client.session.SessionManager.getInstance().clear();
                try { new ConnexionView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            });
        } else {
            btnLogout.setOnAction(e -> {
                try { new ConnexionView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            });
        }
        
        rightControls.getChildren().add(btnLogout);

        header.getChildren().addAll(logo, spacer, nav, rightControls);
        return header;
    }

    private Hyperlink navLink(String text, boolean actif) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", actif ? FontWeight.BOLD : FontWeight.NORMAL, 16));
        link.setTextFill(Color.web(actif ? SAUGE_DARK : BRUN));
        link.setUnderline(false);
        link.setStyle("-fx-border-color: transparent;");
        
        if (!actif) {
            link.setOnMouseEntered(e -> link.setTextFill(Color.web(TERRACOTTA)));
            link.setOnMouseExited(e -> link.setTextFill(Color.web(BRUN)));
        }
        return link;
    }

    private StackPane buildHeroSection(Stage stage) {
        StackPane hero = new StackPane();
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(100, 20, 100, 20));

        // Décorations de fond animées (cercles flottants)
        Circle c1 = new Circle(250, Color.web(SAUGE, 0.15));
        StackPane.setAlignment(c1, Pos.TOP_RIGHT);
        StackPane.setMargin(c1, new Insets(-50, -100, 0, 0));
        
        Circle c2 = new Circle(150, Color.web(TERRACOTTA, 0.08));
        StackPane.setAlignment(c2, Pos.BOTTOM_LEFT);
        StackPane.setMargin(c2, new Insets(0, 0, -80, -50));

        // Animation "flottement" infinie
        TranslateTransition floatC1 = new TranslateTransition(Duration.seconds(4), c1);
        floatC1.setByY(30); floatC1.setCycleCount(Animation.INDEFINITE); floatC1.setAutoReverse(true); floatC1.play();
        
        TranslateTransition floatC2 = new TranslateTransition(Duration.seconds(6), c2);
        floatC2.setByY(-25); floatC2.setByX(15); floatC2.setCycleCount(Animation.INDEFINITE); floatC2.setAutoReverse(true); floatC2.play();

        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(800);

        Text welcome = new Text("Bienvenue dans votre univers");
        welcome.setFont(Font.font("Georgia", FontPosture.ITALIC, 22));
        welcome.setFill(Color.web(TERRACOTTA));
        welcome.setOpacity(0);
        welcome.setTranslateY(20);

        Text mainTitle = new Text("Découvrez notre collection organique & éco-responsable");
        mainTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 54));
        mainTitle.setFill(Color.web(BRUN));
        mainTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        mainTitle.setWrappingWidth(800);
        mainTitle.setOpacity(0);
        mainTitle.setTranslateY(20);

        Text subTitle = new Text("Des produits naturels, pensés pour votre bien-être et respectueux de l'environnement, sélectionnés avec soin par nos experts.");
        subTitle.setFont(Font.font("Georgia", 20));
        subTitle.setFill(Color.web(BRUN, 0.65));
        subTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subTitle.setWrappingWidth(650);
        subTitle.setOpacity(0);
        subTitle.setTranslateY(20);

        Button btnShop = new Button("Explorer le Catalogue");
        btnShop.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-padding: 18 50; -fx-background-radius: 40; -fx-font-size: 18px; -fx-font-weight: bold;");
        btnShop.setCursor(javafx.scene.Cursor.HAND);
        btnShop.setOpacity(0);
        btnShop.setTranslateY(20);
        
        DropShadow ds = new DropShadow(20, Color.web(TERRACOTTA, 0.4));
        btnShop.setEffect(ds);
        
        btnShop.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnShop);
            st.setToX(1.05); st.setToY(1.05); st.play();
            btnShop.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; -fx-padding: 18 50; -fx-background-radius: 40; -fx-font-size: 18px; -fx-font-weight: bold;");
            btnShop.setEffect(new DropShadow(25, Color.web(SAUGE_DARK, 0.5)));
        });
        btnShop.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnShop);
            st.setToX(1.0); st.setToY(1.0); st.play();
            btnShop.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-padding: 18 50; -fx-background-radius: 40; -fx-font-size: 18px; -fx-font-weight: bold;");
            btnShop.setEffect(ds);
        });

        btnShop.setOnAction(e -> {
            try { new CatalogueView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        content.getChildren().addAll(welcome, mainTitle, subTitle, btnShop);
        hero.getChildren().addAll(c1, c2, content);

        // Animation d'entrée élégante en cascade (Staggered Animation)
        int delayMultiplier = 0;
        for (javafx.scene.Node node : content.getChildren()) {
            FadeTransition ft = new FadeTransition(Duration.millis(800), node);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(200 * delayMultiplier));
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(800), node);
            tt.setToY(0);
            tt.setDelay(Duration.millis(200 * delayMultiplier));
            
            new ParallelTransition(ft, tt).play();
            delayMultiplier++;
        }

        return hero;
    }

    private VBox buildFeaturesSection() {
        VBox featuresSection = new VBox(50);
        featuresSection.setAlignment(Pos.CENTER);
        featuresSection.setPadding(new Insets(80, 60, 100, 60));
        featuresSection.setStyle("-fx-background-color: white;");

        Text title = new Text("Pourquoi choisir ChriOnline ?");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        title.setFill(Color.web(BRUN));

        HBox cardsContainer = new HBox(40);
        cardsContainer.setAlignment(Pos.CENTER);

        VBox card1 = createFeatureCard("🌿", "100% Organique", "Nos produits sont issus d'une agriculture respectueuse de l'environnement, sans pesticides ni OGM.");
        VBox card2 = createFeatureCard("🚚", "Livraison Rapide", "Profitez d'une livraison express partout où vous êtes, avec un suivi en temps réel de vos commandes.");
        VBox card3 = createFeatureCard("✨", "Qualité Premium", "Une sélection rigoureuse de produits locaux garantissant la meilleure expérience et des résultats durables.");

        cardsContainer.getChildren().addAll(card1, card2, card3);
        featuresSection.getChildren().addAll(title, cardsContainer);

        return featuresSection;
    }

    private VBox createFeatureCard(String iconStr, String titleStr, String descStr) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(40, 30, 40, 30));
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: " + BORDER + "; -fx-border-width: 1;");

        DropShadow shadow = new DropShadow(30, Color.web(BRUN, 0.05));
        card.setEffect(shadow);

        Text icon = new Text(iconStr);
        icon.setFont(Font.font(45));

        Text title = new Text(titleStr);
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(BRUN));
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Text desc = new Text(descStr);
        desc.setFont(Font.font("Georgia", 15));
        desc.setFill(Color.web(BRUN, 0.7));
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        desc.setWrappingWidth(240);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(icon, title, desc, spacer);

        // Animation de la carte au survol
        card.setOnMouseEntered(e -> {
            TranslateTransition hoverAnim = new TranslateTransition(Duration.millis(300), card);
            hoverAnim.setToY(-10);
            hoverAnim.play();
            card.setEffect(new DropShadow(40, Color.web(SAUGE_DARK, 0.15)));
        });
        card.setOnMouseExited(e -> {
            TranslateTransition hoverOutAnim = new TranslateTransition(Duration.millis(300), card);
            hoverOutAnim.setToY(0);
            hoverOutAnim.play();
            card.setEffect(shadow);
        });

        return card;
    }

    private VBox buildFooter() {
        VBox footer = new VBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(40, 60, 40, 60));
        footer.setStyle("-fx-background-color: " + BRUN + ";");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        logo.setFill(Color.web(CREME));

        HBox links = new HBox(30);
        links.setAlignment(Pos.CENTER);
        
        Hyperlink l1 = new Hyperlink("Conditions Générales");
        Hyperlink l2 = new Hyperlink("Politique de Confidentialité");
        Hyperlink l3 = new Hyperlink("Contact");
        
        for (Hyperlink l : new Hyperlink[]{l1, l2, l3}) {
            l.setFont(Font.font("Georgia", 14));
            l.setTextFill(Color.web(CREME, 0.7));
            l.setUnderline(false);
            l.setStyle("-fx-border-color: transparent;");
            l.setOnMouseEntered(e -> l.setTextFill(Color.web(TERRACOTTA)));
            l.setOnMouseExited(e -> l.setTextFill(Color.web(CREME, 0.7)));
        }
        
        links.getChildren().addAll(l1, l2, l3);

        Text copyright = new Text("© 2026 ChriOnline. Tous droits réservés.");
        copyright.setFont(Font.font("Georgia", 12));
        copyright.setFill(Color.web(CREME, 0.4));

        footer.getChildren().addAll(logo, links, copyright);
        return footer;
    }
}
