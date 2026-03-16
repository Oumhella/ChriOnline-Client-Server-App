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
import javafx.scene.shape.Rectangle;
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

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // ── Header (Menu) ─────────────────────────────────────
        HBox header = buildHeader(stage);
        root.getChildren().add(header);

        // ── Hero Section (Contenu Principal) ──────────────────
        StackPane hero = buildHeroSection(stage);
        VBox.setVgrow(hero, Priority.ALWAYS);
        root.getChildren().add(hero);

        Scene scene = new Scene(root, 1100, 800);
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildHeader(Stage stage) {
        HBox header = new HBox(40);
        header.setPadding(new Insets(25, 60, 25, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        logo.setFill(Color.web(BRUN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(35);
        nav.setAlignment(Pos.CENTER);
        
        Hyperlink hAcc = navLink("Accueil", true);
        Hyperlink hCat = navLink("Catalogue", false);
        Hyperlink hAbo = navLink("À propos", false);
        
        hCat.setOnAction(e -> {
            try {
                new CatalogueView().start(stage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        nav.getChildren().addAll(hAcc, hCat, hAbo);

        Button btnLogout = new Button("Déconnexion");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-border-color: " + BRUN + "; -fx-border-radius: 20; -fx-font-family: 'Georgia';");
        btnLogout.setCursor(javafx.scene.Cursor.HAND);
        btnLogout.setOnAction(e -> {
            try { new ConnexionView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        header.getChildren().addAll(logo, spacer, nav, btnLogout);
        return header;
    }

    private Hyperlink navLink(String text, boolean actif) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", actif ? FontWeight.BOLD : FontWeight.NORMAL, 15));
        link.setTextFill(Color.web(actif ? SAUGE_DARK : BRUN));
        link.setUnderline(false);
        link.setStyle("-fx-border-color: transparent;");
        return link;
    }

    private StackPane buildHeroSection(Stage stage) {
        StackPane hero = new StackPane();
        hero.setAlignment(Pos.CENTER);

        // Décorations de fond
        Circle c1 = new Circle(300, Color.web(SAUGE, 0.1));
        StackPane.setAlignment(c1, Pos.TOP_RIGHT);
        StackPane.setMargin(c1, new Insets(-100, -100, 0, 0));

        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60));

        Text welcome = new Text("Bienvenue dans votre univers");
        welcome.setFont(Font.font("Georgia", FontPosture.ITALIC, 20));
        welcome.setFill(Color.web(TERRACOTTA));

        Text mainTitle = new Text("Découvrez notre collection organique");
        mainTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 48));
        mainTitle.setFill(Color.web(BRUN));
        mainTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        mainTitle.setWrappingWidth(700);

        Text subTitle = new Text("Des produits naturels, pensés pour votre bien-être et respectueux de l'environnement.");
        subTitle.setFont(Font.font("Georgia", 18));
        subTitle.setFill(Color.web(BRUN, 0.6));
        subTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subTitle.setWrappingWidth(600);

        Button btnShop = new Button("Explorer le Catalogue");
        btnShop.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 30; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnShop.setCursor(javafx.scene.Cursor.HAND);
        btnShop.setOnAction(e -> {
            try { new CatalogueView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        DropShadow ds = new DropShadow(20, Color.web(TERRACOTTA, 0.3));
        btnShop.setEffect(ds);

        content.getChildren().addAll(welcome, mainTitle, subTitle, btnShop);
        
        // Animation
        content.setOpacity(0);
        content.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(1000), content);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(1000), content);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        hero.getChildren().addAll(c1, content);
        return hero;
    }

    public static void main(String[] args) { launch(args); }
}
