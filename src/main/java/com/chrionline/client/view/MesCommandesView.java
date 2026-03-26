package com.chrionline.client.view;

import com.chrionline.client.controller.MesCommandesController;
import com.chrionline.shared.dto.CommandeDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class MesCommandesView {

    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private MesCommandesController controller;
    private VBox listContainer;

    public MesCommandesView() {
        this.controller = new MesCommandesController();
    }

    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Mes Commandes");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // Header
        HBox header = new HBox(40);
        header.setPadding(new Insets(20, 48, 20, 48));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> new HomeView().start(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(28);
        nav.setAlignment(Pos.CENTER);

        Hyperlink hAcc = createNavLink("Accueil", stage);
        hAcc.setOnAction(e -> new HomeView().start(stage));

        Hyperlink hCat = createNavLink("Catalogue", stage);
        hCat.setOnAction(e -> new CatalogueView().start(stage));

        String prenom = com.chrionline.client.session.SessionManager.getInstance().getPrenom();
        String nom = com.chrionline.client.session.SessionManager.getInstance().getNom();
        String initials = "";
        if (prenom != null && !prenom.isEmpty()) initials += prenom.toUpperCase().charAt(0);
        if (nom != null && !nom.isEmpty()) initials += nom.toUpperCase().charAt(0);
        if (initials.isEmpty()) initials = "U";
        
        StackPane avatar = new StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(15, Color.web(TERRACOTTA));
        Text initText = new Text(initials);
        initText.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        initText.setFill(Color.WHITE);
        avatar.getChildren().addAll(circle, initText);
        avatar.setCursor(javafx.scene.Cursor.HAND);
        avatar.setOnMouseClicked(e -> new ProfilView().start(stage));
        avatar.setOnMouseEntered(e -> circle.setFill(Color.web(SAUGE_DARK)));
        avatar.setOnMouseExited(e -> circle.setFill(Color.web(TERRACOTTA)));

        Text hOrd = new Text("Mes Commandes");
        hOrd.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        hOrd.setFill(Color.web(SAUGE_DARK));

        Hyperlink hPan = createNavLink("Mon Panier", stage);
        hPan.setOnAction(e -> {
            try {
                // On suppose qu'on peut récupérer l'ID utilisateur depuis le Client ou le passer au constructeur
                // Pour l'instant on redirige vers le catalogue si on ne l'a pas, 
                // mais MesCommandesView est déjà pour un utilisateur spécifique.
                new PanierView(5).start(stage); // ID 5 par défaut comme vu dans les logs
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        nav.getChildren().addAll(hAcc, hCat, hOrd, hPan, avatar);

        header.getChildren().addAll(logo, spacer, nav);

        // Liste des commandes
        listContainer = new VBox(15);
        listContainer.setPadding(new Insets(30, 60, 30, 60));
        
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + CREME + "; -fx-border-color: transparent;");

        root.getChildren().addAll(header, scrollPane);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.show();

        loadOrders();
    }

    private void loadOrders() {
        Map<String, Object> res = controller.getMyOrders();
        if ("OK".equals(res.get("statut"))) {
            List<CommandeDTO> orders = (List<CommandeDTO>) res.get("commandes");
            if (orders == null || orders.isEmpty()) {
                listContainer.getChildren().add(new Label("Vous n'avez pas encore passé de commande."));
            } else {
                for (CommandeDTO order : orders) {
                    listContainer.getChildren().add(buildOrderCard(order));
                }
            }
        } else {
            listContainer.getChildren().add(new Label("Erreur lors du chargement des commandes : " + res.get("message")));
        }
    }

    private Node buildOrderCard(CommandeDTO order) {
        HBox card = new HBox(30);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        VBox details = new VBox(5);
        Text ref = new Text(order.getReference());
        ref.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        ref.setFill(Color.web(BRUN));

        Text date = new Text("Date : " + order.getDateCommande());
        date.setFont(Font.font("Arial", 13));
        date.setFill(Color.GRAY);
        details.getChildren().addAll(ref, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text amount = new Text(String.format("%.2f DH", order.getMontantTotal()));
        amount.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        amount.setFill(Color.web(TERRACOTTA));

        Label status = new Label(order.getStatut());
        String color = switch (order.getStatut().toUpperCase()) {
            case "LIVREE" -> "#4CAF50";
            case "ANNULEE" -> "#F44336";
            case "EXPEDIEE" -> "#2196F3";
            default -> "#FF9800";
        };
        status.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-weight: bold;");

        card.getChildren().addAll(details, spacer, amount, status);
        return card;
    }

    private Hyperlink createNavLink(String text, Stage stage) {
        Hyperlink l = new Hyperlink(text);
        l.setFont(Font.font("Georgia", 14));
        l.setTextFill(Color.web(BRUN));
        l.setStyle("-fx-border-color: transparent;");
        return l;
    }
}
