package com.chrionline.client.view;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.SecurityEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interface d'administration pour la surveillance en temps réel de la sécurité.
 */
public class SecurityDashboardView {

    private final Client client = Client.getInstance();
    private TableView<SecurityEvent> table;
    private ObservableList<SecurityEvent> eventList;
    private ScheduledExecutorService scheduler;

    public javafx.scene.Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FAF7F2;");

        // Titre
        Label title = new Label("Surveillance Sécurité Temps Réel");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#3E2C1E"));

        // Table des événements
        table = new TableView<>();
        eventList = FXCollections.observableArrayList();
        table.setItems(eventList);

        TableColumn<SecurityEvent, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(150);

        TableColumn<SecurityEvent, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(150);

        TableColumn<SecurityEvent, String> ipCol = new TableColumn<>("IP Source");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        ipCol.setPrefWidth(120);

        TableColumn<SecurityEvent, String> contextCol = new TableColumn<>("Contexte");
        contextCol.setCellValueFactory(new PropertyValueFactory<>("context"));
        contextCol.setPrefWidth(300);

        table.getColumns().addAll(timeCol, typeCol, ipCol, contextCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Bouton de blocage
        Button btnBlock = new Button("Bloquer IP sélectionnée");
        btnBlock.setStyle(
                "-fx-background-color: #C96B4A; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnBlock.setOnAction(e -> blockSelectedIP());

        HBox actions = new HBox(10, btnBlock);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, table, actions);

        // Démarrage auto du rafraîchissement
        startRefreshing();

        return root;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("ChriOnline - Dashboard Sécurité Admin");
        stage.setScene(new Scene(getView(), 800, 600));
        stage.show();
        stage.setOnCloseRequest(e -> stopRefreshing());
    }

    private void startRefreshing() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(this::refreshEvents);
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void stopRefreshing() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshEvents() {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("commande", "ADMIN_GET_SECURITY_EVENTS");

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            List<SecurityEvent> events = (List<SecurityEvent>) res.get("events");
            eventList.setAll(events);
        }
    }

    private void blockSelectedIP() {
        SecurityEvent selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une ligne pour bloquer l'IP.");
            return;
        }

        Map<String, Object> req = new java.util.HashMap<>();
        req.put("commande", "ADMIN_BLOCK_IP");
        req.put("ip", selected.getIp());

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            showAlert("Succès", "L'adresse IP " + selected.getIp() + " a été bannie.");
        } else {
            showAlert("Erreur", (String) res.get("message"));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
