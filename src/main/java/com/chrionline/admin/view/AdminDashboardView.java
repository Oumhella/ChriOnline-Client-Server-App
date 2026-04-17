package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminDashboardController;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dashboard administrateur ChriOnline.
 * Style sobre et élégant, cohérent avec la palette du projet.
 */
public class AdminDashboardView extends Application {

    private static final String CREME = "#FDFBF7";
    private static final String CREME_CARD = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String SAUGE_DARK = "#6B9E7A";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String TERRA_LIGHT = "#F5E6E0";
    private static final String BRUN = "#3E2C1E";
    private static final String BRUN_MED = "#6B4F3A";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String GOLD_LIGHT = "#F0E2C8";
    private static final String BORDER = "#E8E0D5";
    private static final String WARNING = "#D4920A";
    private static final String WARNING_BG = "#FDF3DC";
    private static final String DANGER = "#B03A2E";
    private static final String DANGER_BG = "#FBEAEA";
    private static final String INFO = "#2471A3";
    private static final String INFO_BG = "#E8F4FB";
    private static final String SUCCESS_BG = "#EAF5ED";

    private AdminDashboardController controller;
    private final List<String> notificationHistory = new ArrayList<>();
    private MenuButton btnNotifications;
    private HBox rootPane;

    @Override
    public void start(Stage stage) {
        this.controller = new AdminDashboardController();
        controller.chargerStats();

        stage.setTitle("ChriOnline — Administration");

        rootPane = new HBox(0);
        rootPane.setStyle("-fx-background-color: " + CREME + ";");
        rootPane.getChildren().addAll(buildSidebar(stage), buildMainArea());

        String css = getClass().getResource("/styles/admin.css").toExternalForm();
        if (stage.getScene() == null) {
            Scene scene = new Scene(rootPane, 1100, 800);
            scene.getStylesheets().add(css);
            stage.setScene(scene);
        } else {
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(css);
            scene.setRoot(rootPane);
        }

        stage.setTitle("ChriOnline - Administration Premium");
        stage.setMinWidth(1000);
        stage.setMinHeight(750);
        if (!stage.isShowing()) stage.show();
        try {
            com.chrionline.client.network.Client client = com.chrionline.client.network.Client.getInstance("127.0.0.1",
                    12345);

            // Connecter + enregistrer UDP dans le même thread (évite la race condition)
            Thread udpThread = new Thread(() -> {
                try {
                    client.connecter();
                    client.enregistrerUDP();
                } catch (Exception e) {
                    System.err.println("[ADMIN] Échec connexion/UDP : " + e.getMessage());
                }
            });
            udpThread.setDaemon(true);
            udpThread.start();

            client.setNotificationListener(notification -> {
                // Parser le format NOUVELLE_COMMANDE:REF:Utilisateur X
                String affichage;
                if (notification.startsWith("NOUVELLE_COMMANDE:")) {
                    String[] parts = notification.split(":", 3);
                    String ref = parts.length > 1 ? parts[1] : "?";
                    String user = parts.length > 2 ? parts[2] : "";
                    String heure = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
                    affichage = "📦 Nouvelle commande #" + ref + " — " + user + " (à " + heure + ")";
                } else if (notification.startsWith("STOCK_ALERTE:")) {
                    String corps = notification.substring("STOCK_ALERTE:".length());
                    String[] parts = corps.split(":", 3);
                    String nomProduit = parts.length > 0 ? parts[0] : "Inconnu";
                    String stockInfo = parts.length > 1 ? parts[1] : "";
                    String seuilInfo = parts.length > 2 ? parts[2] : "";
                    String stockVal = stockInfo.replace("stock=", "");
                    String seuilVal = seuilInfo.replace("seuil=", "");
                    String heure = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
                    affichage = "⚠️ Rupture stock — " + nomProduit
                            + " | Stock : " + stockVal + " (seuil : " + seuilVal + ")"
                            + " à " + heure;
                } else if (notification.startsWith("NEWSLETTER:")) {
                    String[] parts = notification.split(":", 3);
                    String sujet = parts.length > 1 ? parts[1] : "Sans sujet";
                    affichage = "📧 NEWSLETTER : " + sujet;
                } else if (notification.startsWith("VOTRE COMMANDE")) {
                    affichage = notification; // message de suivi commande client
                } else {
                    affichage = notification;
                }

                // Ajouter à l'historique
                notificationHistory.add(0, affichage);

                // Mettre à jour le MenuButton
                if (btnNotifications != null) {
                    MenuItem item = new MenuItem(affichage);
                    item.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 12px;");
                    btnNotifications.getItems().add(0, item);

                    int count = notificationHistory.size();
                    btnNotifications.setText("🔔 (" + count + ")");
                    btnNotifications.setStyle(
                            "-fx-background-color: " + TERRA_LIGHT + "; -fx-border-color: " + TERRACOTTA + ";"
                                    + "-fx-border-radius: 6; -fx-text-fill: " + TERRACOTTA
                                    + "; -fx-padding: 7 14; -fx-font-weight: bold;");
                }

                // Rafraîchir les statistiques du dashboard
                controller.chargerStats();
            });
        } catch (Exception e) {
            System.err.println("Impossible de configurer le listener UDP : " + e.getMessage());
        }

        stage.show();
    }

    private void afficherVue(Node node) {
        node.setOpacity(0);
        HBox.setHgrow(node, Priority.ALWAYS);
        rootPane.getChildren().set(1, node);
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildSidebar(Stage stage) {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        sidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #A8C4B0 0%, #6B9E7A 55%, #4E7A5C 100%);");

        VBox logoBox = new VBox(3);
        logoBox.setPadding(new Insets(28, 24, 24, 24));
        logoBox.setStyle(
                "-fx-border-color: transparent transparent rgba(62,44,30,0.12) transparent;" +
                        "-fx-border-width: 0 0 1 0;");
        Label badge = new Label("ESPACE ADMIN");
        badge.setFont(Font.font("Georgia", FontWeight.BOLD, 8));
        badge.setTextFill(Color.web(TERRACOTTA));
        badge.setStyle(
                "-fx-background-color: rgba(201,107,74,0.12);" +
                        "-fx-background-radius: 3; -fx-padding: 2 6;");
        Text logoText = new Text("ChriOnline");
        logoText.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        logoText.setFill(Color.web(BRUN));
        logoBox.getChildren().addAll(badge, logoText);

        VBox nav = new VBox(2);
        nav.setPadding(new Insets(20, 10, 20, 10));
        VBox.setVgrow(nav, Priority.ALWAYS);
        HBox logoutItem = navItem("🚪", "Déconnexion", false);
        logoutItem.setOnMouseClicked(e -> deconnecter(stage));

        HBox itemDashboard = navItem("📊", "Dashboard", true);
        HBox itemProduits = navItem("📦", "Produits", false);
        HBox itemCategories = navItem("🏷️", "Catégories", false);
        HBox itemCommandes = navItem("🛒", "Commandes", false);
        HBox itemPaiements = navItem("💳", "Paiements", false);
        HBox itemLivraisons = navItem("🚚", "Livraisons", false);
        HBox itemClients = navItem("👥", "Clients", false);
        HBox itemParams = navItem("⚙️", "Paramètres", false);

        // Actions de navigation
        itemDashboard.setOnMouseClicked(e -> rootPane.getChildren().set(1, buildMainArea()));
        itemClients.setOnMouseClicked(e -> {
            try {
                rootPane.getChildren().set(1, new AdminUsersView().getView());
            } catch (Exception ex) {
                System.err.println("[DASHBOARD] Erreur ouverture Clients : " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        itemCommandes.setOnMouseClicked(e -> {
            try {
                rootPane.getChildren().set(1, new AdminCommandesView().getView());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        nav.getChildren().addAll(
                navSection("VUE GÉNÉRALE"),
                itemDashboard,
                navSection("CATALOGUE"),
                navItem("📦", "Produits", true, () -> {
                    afficherVue(new AdminProduitsView().getView());
                }),
                navItem("🏷️", "Catégories", false, () -> {
                    afficherVue(new AdminCategoriesView().getView());
                }),
                navSection("VENTES"),
                itemCommandes,
                itemPaiements,
                itemLivraisons,
                navSection("UTILISATEURS"),
                itemClients,
                navSection("SYSTÈME"),
                navItem("🛡️", "Sécurité", false, () -> {
                    try {
                        rootPane.getChildren().set(1, new com.chrionline.client.view.SecurityDashboardView().getView());
                    } catch (Exception ex) {
                        System.err.println("[DASHBOARD] Erreur ouverture Sécurité : " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }),
                navItem("📧", "Newsletter", false, () -> {
                    afficherVue(new AdminNewsletterView().getView());
                }),
                itemParams,
                navSection("COMPTE"),
                logoutItem);

        HBox footer = new HBox(10);
        footer.setPadding(new Insets(14, 16, 18, 16));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-border-color: rgba(62,44,30,0.12) transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;");
        Circle av = new Circle(16, Color.web(BRUN, 0.15));
        Text ini = new Text("A");
        ini.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        ini.setFill(Color.web(BRUN));
        StackPane avatarPane = new StackPane(av, ini);
        VBox adminInfo = new VBox(1);
        Text adminNom = new Text("Administrateur");
        adminNom.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        adminNom.setFill(Color.web(BRUN));
        Text adminRole = new Text("Super Admin");
        adminRole.setFont(Font.font("Georgia", 10));
        adminRole.setFill(Color.web(BRUN, 0.55));
        adminInfo.getChildren().addAll(adminNom, adminRole);
        footer.getChildren().addAll(avatarPane, adminInfo);

        sidebar.getChildren().addAll(logoBox, nav, footer);
        return sidebar;
    }

    private Label navSection(String titre) {
        Label lbl = new Label(titre);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 8));
        lbl.setTextFill(Color.web(BRUN, 0.45));
        lbl.setPadding(new Insets(14, 12, 3, 14));
        return lbl;
    }

    private HBox navItem(String icon, String label, boolean actif, Runnable action) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(9, 14, 9, 14));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);
        if (actif)
            item.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 7;");

        Label ico = new Label(icon);
        ico.setFont(Font.font(12));
        Text txt = new Text(label);
        txt.setFont(Font.font("Georgia", actif ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        txt.setFill(Color.web(BRUN, actif ? 1.0 : 0.78));
        item.getChildren().addAll(ico, txt);

        if (action != null) {
            item.setOnMouseClicked(e -> action.run());
        }

        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("0.22")) {
                item.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 7;");
            }
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("0.22")) {
                item.setStyle("");
            }
        });

        return item;
    }

    private HBox navItem(String icon, String label, boolean actif) {
        return navItem(icon, label, actif, null);
    }

    private void deconnecter(Stage stage) {
        try {
            com.chrionline.client.network.Client c = com.chrionline.client.network.Client.getInstance();
            if (c != null) {
                c.connecter();
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("commande", "DECONNEXION");
                c.envoyerRequete(m);
                c.lireReponse();
            }
        } catch (Exception ignored) { }
        com.chrionline.client.session.SessionManager.getInstance().clear();
        try {
            new com.chrionline.client.view.ConnexionView().start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ZONE PRINCIPALE
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildMainArea() {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: " + CREME + ";");
        HBox.setHgrow(main, Priority.ALWAYS);
        main.getChildren().add(buildTopbar());

        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 34, 34, 34));

        VBox titleBlock = new VBox(3);
        Text titre = new Text("Tableau de bord");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Vue d'ensemble de l'activité");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sousTitre.setFill(Color.web(BRUN_LIGHT));
        titleBlock.getChildren().addAll(titre, sousTitre);

        HBox midRow = new HBox(22);
        VBox tableCmd = buildTableauCommandes();
        VBox panelStatuts = buildPanneauStatuts();
        HBox.setHgrow(tableCmd, Priority.ALWAYS);
        midRow.getChildren().addAll(tableCmd, panelStatuts);

        content.getChildren().addAll(titleBlock, buildKpiRow(), midRow, buildStockAlerte());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        main.getChildren().add(scroll);
        return main;
    }

    private HBox buildTopbar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(16, 34, 16, 34));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");
        Text bc = new Text("Admin  /  Dashboard");
        bc.setFont(Font.font("Georgia", 12));
        bc.setFill(Color.web(BRUN_LIGHT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("↻  Actualiser");
        btnRefresh.setFont(Font.font("Georgia", 12));
        btnRefresh.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 7 14;");
        btnRefresh.setCursor(javafx.scene.Cursor.HAND);
        btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(
                "-fx-background-color: " + CREME_INPUT + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-text-fill: " + BRUN + "; -fx-padding: 7 14;"));
        btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 7 14;"));

        // --- Bouton Notifications ---
        btnNotifications = new MenuButton("🔔 (0)");
        btnNotifications.setFont(Font.font("Georgia", 12));
        btnNotifications.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 7 14;");
        btnNotifications.setCursor(javafx.scene.Cursor.HAND);

        // Si on a déjà des notifs (cas de reconstruction de vue)
        for (String n : notificationHistory) {
            MenuItem mi = new MenuItem(n);
            mi.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 12px;");
            btnNotifications.getItems().add(mi);
        }
        if (!notificationHistory.isEmpty()) {
            btnNotifications.setText("🔔 (" + notificationHistory.size() + ")");
            btnNotifications.setStyle(
                    "-fx-background-color: " + TERRA_LIGHT + "; -fx-border-color: " + TERRACOTTA + ";" +
                            "-fx-border-radius: 6; -fx-text-fill: " + TERRACOTTA
                            + "; -fx-padding: 7 14; -fx-font-weight: bold;");
        }

        bar.getChildren().addAll(bc, spacer, btnNotifications, btnRefresh);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KPI CARDS
    // ═════════════════════════════════════════════════════════════════════════

    private HBox buildKpiRow() {
        HBox row = new HBox(16);
        VBox[] cards = {
                kpiCard("👥", "Clients actifs", String.valueOf(controller.getTotalClients()), SAUGE_DARK, SUCCESS_BG),
                kpiCard("🛒", "Commandes", String.valueOf(controller.getTotalCommandes()), TERRACOTTA, TERRA_LIGHT),
                kpiCard("💰", "Chiffre d'affaires", formatMonnaie(controller.getChiffreAffaires()) + " MAD", BRUN_MED,
                        GOLD_LIGHT),
                kpiCard("⏳", "En attente", String.valueOf(controller.getCommandesEnAttente()), WARNING, WARNING_BG),
                kpiCard("📦", "Produits", String.valueOf(controller.getTotalProduits()), INFO, INFO_BG),
        };
        for (VBox c : cards) {
            HBox.setHgrow(c, Priority.ALWAYS);
            row.getChildren().add(c);
        }
        return row;
    }

    private VBox kpiCard(String icon, String label, String valeur, String accent, String bg) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-background-radius: 10; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 10; -fx-border-width: 1;");
        Rectangle topBar = new Rectangle(0, 3);
        topBar.setArcWidth(3);
        topBar.setArcHeight(3);
        topBar.setFill(Color.web(accent));
        topBar.setWidth(220); // largeur fixe

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(17));
        iconLbl.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 7; -fx-padding: 5 9;");

        Text lbl = new Text(label);
        lbl.setFont(Font.font("Georgia", 11));
        lbl.setFill(Color.web(BRUN_LIGHT));

        Text val = new Text(valeur);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 23));
        val.setFill(Color.web(BRUN));

        card.getChildren().addAll(topBar, iconLbl, lbl, val);
        card.setEffect(new DropShadow(6, Color.web(BRUN, 0.05)));
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TABLEAU COMMANDES RÉCENTES
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildTableauCommandes() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("card");

        HBox header = panelHeader("Commandes récentes");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Hyperlink lien = new Hyperlink("Voir tout →");
        lien.setFont(Font.font("Georgia", 12));
        lien.setTextFill(Color.web(SAUGE_DARK));
        lien.setStyle("-fx-border-color: transparent;");
        header.getChildren().addAll(sp, lien);

        panel.getChildren().addAll(header, tableRow("Référence", "Client", "Date", "Montant", "Statut", true));

        List<Map<String, Object>> commandes = controller.getCommandesRecentes();
        if (commandes.isEmpty()) {
            Label vide = new Label("Aucune commande pour l'instant.");
            vide.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
            vide.setTextFill(Color.web(BRUN_LIGHT));
            vide.setPadding(new Insets(18, 20, 18, 20));
            panel.getChildren().add(vide);
        } else {
            boolean alt = false;
            for (Map<String, Object> cmd : commandes) {
                String date = "";
                if (cmd.get("dateCommande") instanceof Timestamp ts)
                    date = ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
                String montant = formatMonnaie((BigDecimal) cmd.getOrDefault("montant", BigDecimal.ZERO)) + " MAD";
                HBox ligne = tableRow(
                        (String) cmd.get("reference"), (String) cmd.get("client"),
                        date, montant, (String) cmd.get("status"), false);
                if (alt)
                    ligne.setStyle("-fx-background-color: " + CREME + ";");
                panel.getChildren().add(ligne);
                alt = !alt;
            }
        }
        return panel;
    }

    private HBox tableRow(String ref, String client, String date,
            String montant, String statut, boolean isHeader) {
        HBox row = new HBox(0);
        row.setPadding(new Insets(11, 20, 11, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        if (isHeader)
            row.setStyle(
                    "-fx-background-color: " + CREME_INPUT + ";" +
                            "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                            "-fx-border-width: 0 0 1 0;");
        row.getChildren().addAll(
                cell(ref, isHeader, 145),
                cell(client, isHeader, 145),
                cell(date, isHeader, 115),
                cell(montant, isHeader, 110),
                isHeader ? statutHeaderCell() : statutBadge(statut));
        return row;
    }

    private HBox cell(String val, boolean isHeader, double w) {
        Text t = new Text(val != null ? val : "—");
        t.setFont(Font.font("Georgia", isHeader ? FontWeight.BOLD : FontWeight.NORMAL, isHeader ? 11 : 13));
        t.setFill(Color.web(isHeader ? BRUN_MED : BRUN));
        HBox b = new HBox(t);
        b.setMinWidth(w);
        b.setPrefWidth(w);
        return b;
    }

    private Text statutHeaderCell() {
        Text t = new Text("Statut");
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        t.setFill(Color.web(BRUN_MED));
        return t;
    }

    private Label statutBadge(String statut) {
        record Cfg(String label, String color, String bg) {
        }
        Cfg cfg = switch (statut != null ? statut : "") {
            case "expediee" -> new Cfg("Expédiée", INFO, INFO_BG);
            case "livree" -> new Cfg("Livrée", "#1E7A45", "#DFF5E8");
            case "annulee" -> new Cfg("Annulée", DANGER, DANGER_BG);
            default -> new Cfg("En préparation", WARNING, WARNING_BG);
        };
        Label b = new Label(cfg.label());
        b.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        b.setTextFill(Color.web(cfg.color()));
        b.setStyle("-fx-background-color: " + cfg.bg() + "; -fx-background-radius: 20; -fx-padding: 3 10;");
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PANNEAU STATUTS
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildPanneauStatuts() {
        VBox panel = card();
        panel.setPrefWidth(240);
        panel.setMinWidth(220);
        panel.setMaxWidth(255);
        panel.getChildren().add(panelHeader("Répartition statuts"));

        Map<String, Integer> parStatut = controller.getCommandesParStatut();
        int total = Math.max(parStatut.values().stream().mapToInt(Integer::intValue).sum(), 1);

        VBox items = new VBox(10);
        items.setPadding(new Insets(14, 18, 14, 18));

        Object[][] cfg = {
                { "en_preparation", "En préparation", WARNING },
                { "expediee", "Expédiée", INFO },
                { "livree", "Livrée", "#1E7A45" },
                { "annulee", "Annulée", DANGER }
        };
        for (Object[] c : cfg) {
            int nb = parStatut.getOrDefault((String) c[0], 0);
            items.getChildren().add(barStatut((String) c[1], nb, (double) nb / total, (String) c[2]));
        }

        Separator sep = new Separator();
        VBox alerteBox = new VBox(3);
        alerteBox.setPadding(new Insets(10, 18, 14, 18));
        Text alerteLbl = new Text("Stock en alerte");
        alerteLbl.setFont(Font.font("Georgia", 11));
        alerteLbl.setFill(Color.web(BRUN_LIGHT));
        int nbAlerte = controller.getStockAlerte();
        Text alerteVal = new Text(nbAlerte + " référence(s)");
        alerteVal.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        alerteVal.setFill(Color.web(nbAlerte > 0 ? DANGER : SAUGE_DARK));
        alerteBox.getChildren().addAll(alerteLbl, alerteVal);

        panel.getChildren().addAll(items, sep, alerteBox);
        return panel;
    }

    private VBox barStatut(String label, int nb, double pct, String color) {
        HBox top = new HBox();
        Text lbl = new Text(label);
        lbl.setFont(Font.font("Georgia", 12));
        lbl.setFill(Color.web(BRUN));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Text nbT = new Text(String.valueOf(nb));
        nbT.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        nbT.setFill(Color.web(BRUN_MED));
        top.getChildren().addAll(lbl, sp, nbT);

        StackPane track = new StackPane();
        track.setPrefHeight(5);
        track.setStyle("-fx-background-color: " + BORDER + "; -fx-background-radius: 3;");
        track.setAlignment(Pos.CENTER_LEFT);
        Rectangle fill = new Rectangle(0, 5);
        fill.setArcWidth(5);
        fill.setArcHeight(5);
        fill.setFill(Color.web(color));
        track.widthProperty().addListener((obs, o, w) -> fill.setWidth(w.doubleValue() * pct));
        track.getChildren().add(fill);

        return new VBox(5, top, track);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BANDEAU ALERTE STOCK
    // ═════════════════════════════════════════════════════════════════════════

    private HBox buildStockAlerte() {
        int nbAlerte = controller.getStockAlerte();
        HBox row = new HBox(16);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        String accent = nbAlerte > 0 ? DANGER : SAUGE_DARK;
        row.setStyle(
                "-fx-background-color: " + (nbAlerte > 0 ? DANGER_BG : SUCCESS_BG) + ";" +
                        "-fx-background-radius: 10; -fx-border-color: " + accent + ";" +
                        "-fx-border-radius: 10; -fx-border-width: 1;");

        Text icon = new Text(nbAlerte > 0 ? "⚠" : "✓");
        icon.setFont(Font.font(18));
        icon.setFill(Color.web(accent));

        VBox info = new VBox(3);
        Text titre = new Text(nbAlerte > 0
                ? nbAlerte + " format(s) de produit en alerte de stock"
                : "Stocks suffisants — aucune alerte");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        titre.setFill(Color.web(accent));
        Text desc = new Text(nbAlerte > 0
                ? "Ces références ont atteint ou dépassé le seuil d'alerte configuré."
                : "Tous les stocks sont au-dessus des seuils définis.");
        desc.setFont(Font.font("Georgia", 12));
        desc.setFill(Color.web(BRUN_MED));
        info.getChildren().addAll(titre, desc);

        row.getChildren().addAll(icon, info);

        if (nbAlerte > 0) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Button btn = new Button("Voir les produits →");
            btn.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
            btn.setStyle(
                    "-fx-background-color: " + DANGER + "; -fx-text-fill: white;" +
                            "-fx-background-radius: 6; -fx-padding: 8 16;");
            btn.setCursor(javafx.scene.Cursor.HAND);
            row.getChildren().addAll(sp, btn);
        }
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private VBox card() {
        VBox v = new VBox(0);
        v.setStyle(
                "-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 10;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        v.setEffect(new DropShadow(7, Color.web(BRUN, 0.05)));
        return v;
    }

    private HBox panelHeader(String titre) {
        HBox h = new HBox();
        h.setPadding(new Insets(16, 20, 14, 20));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle(
                "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");
        Text t = new Text(titre);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        t.setFill(Color.web(BRUN));
        h.getChildren().add(t);
        return h;
    }

    private String formatMonnaie(BigDecimal val) {
        return val != null ? String.format("%,.2f", val) : "0,00";
    }

    public static void main(String[] args) {
        launch(args);
    }
}