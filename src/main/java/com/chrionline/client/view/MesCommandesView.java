package com.chrionline.client.view;

import com.chrionline.client.controller.MesCommandesController;
import com.chrionline.client.view.utils.HeaderComponent;
import com.chrionline.shared.dto.CommandeDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
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

        // Header Centralisé
        HBox header = HeaderComponent.build(stage, "Commandes");


        // Titre et bouton Suivre ma commande
        HBox topSection = new HBox(20);
        topSection.setPadding(new Insets(30, 60, 0, 60));
        topSection.setAlignment(Pos.CENTER_LEFT);
        
        Text titleText = new Text("Historique de vos commandes");
        titleText.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        titleText.setFill(Color.web(BRUN));
        
        Region sectionSpacer = new Region();
        HBox.setHgrow(sectionSpacer, Priority.ALWAYS);
        
        Button btnSuivre = new Button("🧭 Suivre ma commande");
        btnSuivre.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-font-family: Arial; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnSuivre.setCursor(javafx.scene.Cursor.HAND);
        btnSuivre.setOnAction(e -> {
            try { new SuiviCommandeView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        topSection.getChildren().addAll(titleText, sectionSpacer, btnSuivre);

        // Liste des commandes
        listContainer = new VBox(15);
        listContainer.setPadding(new Insets(20, 60, 30, 60));
        
        VBox mainContent = new VBox(10);
        mainContent.getChildren().addAll(topSection, listContainer);
        
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + CREME + "; -fx-border-color: transparent;");

        root.getChildren().addAll(header, scrollPane);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
            stage.getScene().getStylesheets().clear();
        }
        if (!stage.isShowing()) stage.show();

        loadOrders();
    }

    private void loadOrders() {
        Map<String, Object> res = controller.getMyOrders();
        if ("OK".equals(res.get("statut"))) {
            @SuppressWarnings("unchecked")
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
        Text ref = new Text("Commande " + order.getReference());
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

        card.getChildren().addAll(details, spacer, amount);
        return card;
    }
}
