package com.chrionline.client.view;

import com.chrionline.client.network.Client;
import com.chrionline.shared.models.SecurityEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interface d'administration pour la surveillance en temps réel de la sécurité.
 * Module IDS/IPS : affiche les alertes, les IPs bloquées et un graphique de répartition.
 */
public class SecurityDashboardView {

    private final Client client = Client.getInstance();
    private TableView<SecurityEvent> table;
    private ObservableList<SecurityEvent> eventList;
    private TableView<Map<String, Object>> blockedTable;
    private ObservableList<Map<String, Object>> blockedList;
    private PieChart alertChart;
    private ScheduledExecutorService scheduler;

    public javafx.scene.Parent getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FAF7F2;");

        // ─── Titre principal ──────────────────────────────────────────────
        Label title = new Label("🛡 Surveillance Sécurité IDS/IPS — Temps Réel");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#3E2C1E"));

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 1 : Tableau des alertes IDS
        // ═══════════════════════════════════════════════════════════════════
        Label alertTitle = new Label("📋 Alertes de Sécurité Récentes");
        alertTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        alertTitle.setTextFill(Color.web("#5A3E2B"));

        table = new TableView<>();
        eventList = FXCollections.observableArrayList();
        table.setItems(eventList);
        table.setPrefHeight(250);

        TableColumn<SecurityEvent, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(150);

        TableColumn<SecurityEvent, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(200);
        // Coloration des cellules selon la gravité
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("ALERT") || item.contains("BAN") || item.contains("SPOOF")) {
                        setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                    } else if (item.contains("FAILED") || item.contains("REFUSE") || item.contains("BLOQUE")) {
                        setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27AE60;");
                    }
                }
            }
        });

        TableColumn<SecurityEvent, String> ipCol = new TableColumn<>("IP Source");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        ipCol.setPrefWidth(120);

        TableColumn<SecurityEvent, String> contextCol = new TableColumn<>("Contexte");
        contextCol.setCellValueFactory(new PropertyValueFactory<>("context"));
        contextCol.setPrefWidth(350);

        table.getColumns().addAll(timeCol, typeCol, ipCol, contextCol);

        Button btnBlockFromAlert = new Button("🚫 Bloquer l'IP sélectionnée");
        btnBlockFromAlert.setStyle(
                "-fx-background-color: #C0392B; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 5; -fx-font-weight: bold;");
        btnBlockFromAlert.setOnAction(e -> blockSelectedIP());

        HBox alertActions = new HBox(10, btnBlockFromAlert);
        alertActions.setAlignment(Pos.CENTER_RIGHT);

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 2 : Tableau des IPs bloquées + déblocage
        // ═══════════════════════════════════════════════════════════════════
        Label blockedTitle = new Label("🔒 Adresses IP en Liste Noire");
        blockedTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        blockedTitle.setTextFill(Color.web("#5A3E2B"));

        blockedTable = new TableView<>();
        blockedList = FXCollections.observableArrayList();
        blockedTable.setItems(blockedList);
        blockedTable.setPrefHeight(180);

        TableColumn<Map<String, Object>, String> bIpCol = new TableColumn<>("Adresse IP");
        bIpCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue().get("ip")));
        bIpCol.setPrefWidth(130);

        TableColumn<Map<String, Object>, String> bEmailCol = new TableColumn<>("Email Associé");
        bEmailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue().getOrDefault("email", "-")));
        bEmailCol.setPrefWidth(180);

        TableColumn<Map<String, Object>, String> bRaisonCol = new TableColumn<>("Raison");
        bRaisonCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue().get("raison")));
        bRaisonCol.setPrefWidth(250);

        TableColumn<Map<String, Object>, String> bExpCol = new TableColumn<>("Expire le");
        bExpCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                (String) data.getValue().get("expire_le")));
        bExpCol.setPrefWidth(150);

        blockedTable.getColumns().addAll(bIpCol, bEmailCol, bRaisonCol, bExpCol);

        Button btnUnblock = new Button("✅ Débloquer l'IP sélectionnée");
        btnUnblock.setStyle(
                "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 5; -fx-font-weight: bold;");
        btnUnblock.setOnAction(e -> unblockSelectedIP());

        HBox blockedActions = new HBox(10, btnUnblock);
        blockedActions.setAlignment(Pos.CENTER_RIGHT);

        // ═══════════════════════════════════════════════════════════════════
        // SECTION 3 : Graphique PieChart de répartition des alertes
        // ═══════════════════════════════════════════════════════════════════
        Label chartTitle = new Label("📊 Répartition des Types d'Alertes");
        chartTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        chartTitle.setTextFill(Color.web("#5A3E2B"));

        alertChart = new PieChart();
        alertChart.setPrefHeight(250);
        alertChart.setLabelsVisible(true);
        alertChart.setLegendVisible(true);

        // Séparateur visuel
        Separator sep1 = new Separator();
        Separator sep2 = new Separator();

        root.getChildren().addAll(
                title,
                alertTitle, table, alertActions,
                sep1,
                blockedTitle, blockedTable, blockedActions,
                sep2,
                chartTitle, alertChart
        );

        // Démarrage auto du rafraîchissement
        startRefreshing();

        return root;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("ChriOnline — Dashboard Sécurité IDS/IPS");
        stage.setScene(new Scene(getView(), 900, 850));
        stage.show();
        stage.setOnCloseRequest(e -> stopRefreshing());
    }

    private void startRefreshing() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                refreshEvents();
                refreshBlockedIPs();
            });
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void stopRefreshing() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshEvents() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "ADMIN_GET_SECURITY_EVENTS");

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            List<SecurityEvent> events = (List<SecurityEvent>) res.get("events");
            if (events != null) {
                eventList.setAll(events);
                updatePieChart(events);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshBlockedIPs() {
        Map<String, Object> req = new HashMap<>();
        req.put("commande", "ADMIN_GET_BLOCKED_IPS");

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            List<Map<String, Object>> ips = (List<Map<String, Object>>) res.get("blockedIPs");
            if (ips != null) {
                blockedList.setAll(ips);
            }
        }
    }

    /**
     * Met à jour le graphique PieChart avec la répartition des types d'alertes.
     */
    private void updatePieChart(List<SecurityEvent> events) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (SecurityEvent ev : events) {
            String category = categorizeEvent(ev.getType());
            typeCounts.merge(category, 1, Integer::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        typeCounts.forEach((cat, count) -> pieData.add(new PieChart.Data(cat + " (" + count + ")", count)));
        alertChart.setData(pieData);
    }

    /**
     * Regroupe les types d'événements en catégories lisibles pour le PieChart.
     */
    private String categorizeEvent(String type) {
        if (type.contains("BRUTE") || type.contains("LOGIN_FAILED")) return "Brute Force";
        if (type.contains("OTP")) return "OTP Suspect";
        if (type.contains("SPOOF")) return "IP Spoofing";
        if (type.contains("FLOOD") || type.contains("BAN")) return "Flood / Ban";
        if (type.contains("ADMIN")) return "Activité Admin";
        if (type.contains("LOGIN_SUCCESS") || type.contains("SESSION")) return "Connexions";
        return "Autres";
    }

    private void blockSelectedIP() {
        SecurityEvent selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une ligne pour bloquer l'IP.");
            return;
        }

        Map<String, Object> req = new HashMap<>();
        req.put("commande", "ADMIN_BLOCK_IP");
        req.put("ip", selected.getIp());

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            showAlert("Succès", "L'adresse IP " + selected.getIp() + " a été bannie.");
            refreshBlockedIPs();
        } else {
            showAlert("Erreur", (String) res.get("message"));
        }
    }

    private void unblockSelectedIP() {
        Map<String, Object> selected = blockedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une IP à débloquer.");
            return;
        }

        String ip = (String) selected.get("ip");

        Map<String, Object> req = new HashMap<>();
        req.put("commande", "ADMIN_UNBLOCK_IP");
        req.put("ip", ip);

        Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);
        if ("OK".equals(res.get("statut"))) {
            showAlert("Succès", "L'adresse IP " + ip + " a été débloquée.");
            refreshBlockedIPs();
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
