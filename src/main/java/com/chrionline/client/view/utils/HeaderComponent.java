package com.chrionline.client.view.utils;

import com.chrionline.client.session.SessionManager;
import com.chrionline.client.view.*;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HeaderComponent {

    private static final String CREME       = "#FDFBF7";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";
    private static final String BRUN_MED    = "#6B4F3A";

    public static HBox build(Stage stage, String activePage) {
        return build(stage, activePage, null);
    }

    public static HBox build(Stage stage, String activePage, java.util.function.Consumer<MenuButton> onNotifBtnReady) {
        HBox header = new HBox(40);
        header.setPadding(new Insets(20, 60, 20, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + CREME + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        // --- LOGO ---
        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(Cursor.HAND);
        logo.setOnMouseClicked(e -> navigate(stage, "Accueil"));
        
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

        // --- NAVIGATION ---
        HBox nav = new HBox(30);
        nav.setAlignment(Pos.CENTER);
        
        nav.getChildren().addAll(
            createNavLink("Accueil", "Accueil".equals(activePage), e -> navigate(stage, "Accueil")),
            createNavLink("Catalogue", "Catalogue".equals(activePage), e -> navigate(stage, "Catalogue")),
            createNavLink("Mes Commandes", "Commandes".equals(activePage), e -> navigate(stage, "Commandes")),
            createNavLink("Panier", "Panier".equals(activePage), e -> navigate(stage, "Panier"))
        );

        // --- NOTIFICATIONS & RIGHT CONTROLS ---
        HBox rightControls = new HBox(15);
        rightControls.setAlignment(Pos.CENTER);
        
        boolean isLogged = SessionManager.getInstance().isLogged();
        if (isLogged) {
            MenuButton btnNotif = createNotificationButton();
            rightControls.getChildren().add(btnNotif);
            if (onNotifBtnReady != null) onNotifBtnReady.accept(btnNotif);

            String prenom = SessionManager.getInstance().getPrenom();
            String nom = SessionManager.getInstance().getNom();
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
            avatar.setCursor(Cursor.HAND);
            avatar.setOnMouseClicked(e -> navigate(stage, "Profil"));
            
            avatar.setOnMouseEntered(e -> circle.setFill(Color.web(SAUGE_DARK)));
            avatar.setOnMouseExited(e -> circle.setFill(Color.web(TERRACOTTA)));
            
            rightControls.getChildren().add(avatar);
        }

        Button btnAuth = new Button(isLogged ? "Déconnexion" : "Connexion");
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + BRUN + "; -fx-border-color: " + BRUN + "; -fx-border-radius: 20; -fx-font-family: 'Georgia'; -fx-padding: 7 18; -fx-font-size: 13px;";
        String hoverStyle = "-fx-background-color: " + BRUN + "; -fx-text-fill: " + CREME + "; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-family: 'Georgia'; -fx-padding: 7 18; -fx-font-size: 13px;";
        
        btnAuth.setStyle(baseStyle);
        btnAuth.setCursor(Cursor.HAND);
        btnAuth.setOnMouseEntered(e -> btnAuth.setStyle(hoverStyle));
        btnAuth.setOnMouseExited(e -> btnAuth.setStyle(baseStyle));
        
        btnAuth.setOnAction(e -> {
            if (isLogged) {
                SessionManager.getInstance().clear();
            }
            navigate(stage, "Connexion");
        });
        
        rightControls.getChildren().add(btnAuth);

        header.getChildren().addAll(logo, spacer, nav, rightControls);
        return header;
    }

    private static Hyperlink createNavLink(String text, boolean active, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", active ? FontWeight.BOLD : FontWeight.NORMAL, 15));
        link.setTextFill(Color.web(BRUN, active ? 1.0 : 0.7));
        link.setStyle("-fx-border-color: transparent; -fx-underline: false;");
        
        if (active) {
            link.setStyle("-fx-border-color: transparent transparent " + TERRACOTTA + " transparent; -fx-border-width: 0 0 2 0; -fx-underline: false;");
        }

        link.setOnMouseEntered(e -> {
            if (!active) link.setTextFill(Color.web(BRUN, 1.0));
        });
        link.setOnMouseExited(e -> {
            if (!active) link.setTextFill(Color.web(BRUN, 0.7));
        });

        link.setOnAction(action);
        return link;
    }

    private static void navigate(Stage stage, String page) {
        try {
            switch (page) {
                case "Accueil":   new HomeView().start(stage); break;
                case "Catalogue": new CatalogueView().start(stage); break;
                case "Commandes": new MesCommandesView().start(stage); break;
                case "Panier":    new PanierView(SessionManager.getInstance().getUserId()).start(stage); break;
                case "Profil":    new ProfilView().start(stage); break;
                case "Connexion": new ConnexionView().start(stage); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MenuButton createNotificationButton() {
        int unread = SessionManager.getInstance().getUnreadNotificationsCount();
        MenuButton mb = new MenuButton("🔔 (" + unread + ")");
        mb.setFont(Font.font("Georgia", 13));
        mb.setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER + "; -fx-border-radius: 20; -fx-text-fill: " + BRUN + "; -fx-padding: 5 12;");
        mb.setCursor(Cursor.HAND);
        
        mb.setOnShowing(e -> {
            SessionManager.getInstance().resetUnreadCount();
            mb.setText("🔔 (0)");
        });

        refreshNotificationMenu(mb);
        return mb;
    }

    public static void refreshNotificationMenu(MenuButton mb) {
        mb.getItems().clear();
        java.util.List<String> history = SessionManager.getInstance().getNotificationHistory();
        if (history.isEmpty()) {
            MenuItem empty = new MenuItem("Aucune notification");
            empty.setDisable(true);
            mb.getItems().add(empty);
        } else {
            for (int i = history.size() - 1; i >= 0; i--) {
                MenuItem item = new MenuItem(history.get(i));
                item.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 13px;");
                mb.getItems().add(item);
            }
        }
    }
}
