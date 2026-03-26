package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminUsersController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Map;

public class AdminUsersView {

    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String WARNING     = "#D4920A";
    private static final String WARNING_BG  = "#FDF3DC";
    private static final String DANGER      = "#B03A2E";
    private static final String DANGER_BG   = "#FBEAEA";
    private static final String SUCCESS_BG  = "#EAF5ED";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String BORDER      = "#E8E0D5";

    private final AdminUsersController controller;
    private final VBox tableContainer;

    public AdminUsersView() {
        this.controller = new AdminUsersController();
        this.tableContainer = new VBox(0);
    }

    public VBox getView() {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: " + CREME + ";");

        HBox bar = new HBox(12);
        bar.setPadding(new Insets(16, 34, 16, 34));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color: " + CREME_CARD + ";" +
                "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                "-fx-border-width: 0 0 1 0;");
        Text bc = new Text("Admin  /  Clients");
        bc.setFont(Font.font("Georgia", 12));
        bc.setFill(Color.web(BRUN_LIGHT));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("↻  Actualiser");
        btnRefresh.setFont(Font.font("Georgia", 12));
        btnRefresh.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 7 14;");
        btnRefresh.setCursor(Cursor.HAND);
        btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(
                "-fx-background-color: " + CREME_INPUT + "; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6; -fx-text-fill: " + BRUN + "; -fx-padding: 7 14;"));
        btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 7 14;"));
        btnRefresh.setOnAction(e -> chargerClients());

        bar.getChildren().addAll(bc, spacer, btnRefresh);

        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 34, 34, 34));

        VBox titleBlock = new VBox(3);
        Text titre = new Text("Gestion des Clients");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Consulter et bloquer/débloquer l'accès des utilisateurs");
        sousTitre.setFont(Font.font("Georgia", 13));
        sousTitre.setFill(Color.web(BRUN_LIGHT));
        titleBlock.getChildren().addAll(titre, sousTitre);
        
        VBox panel = new VBox(0);
        panel.setStyle(
                "-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 10;" +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        panel.setEffect(new DropShadow(7, Color.web(BRUN, 0.05)));
        
        HBox headerList = new HBox();
        headerList.setPadding(new Insets(16, 20, 14, 20));
        headerList.setAlignment(Pos.CENTER_LEFT);
        headerList.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        Text t = new Text("Liste des utilisateurs");
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        t.setFill(Color.web(BRUN));
        headerList.getChildren().add(t);
        
        HBox headerRow = new HBox(0);
        headerRow.setPadding(new Insets(11, 20, 11, 20));
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        headerRow.getChildren().addAll(
                cellText("ID", true, 60),
                cellText("Nom", true, 180),
                cellText("Email", true, 260),
                cellText("Statut", true, 130),
                cellText("Actions", true, 120)
        );

        panel.getChildren().addAll(headerList, headerRow, tableContainer);

        content.getChildren().addAll(titleBlock, panel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        main.getChildren().addAll(bar, scroll);

        // Load data in background
        chargerClients();

        return main;
    }

    private void chargerClients() {
        tableContainer.getChildren().clear();
        Label loading = new Label("Chargement des clients...");
        loading.setPadding(new Insets(20));
        loading.setFont(Font.font("Georgia", 13));
        loading.setTextFill(Color.web(BRUN_LIGHT));
        tableContainer.getChildren().add(loading);

        new Thread(() -> {
            List<Map<String, Object>> clients = controller.listerClients();
            Platform.runLater(() -> afficherClients(clients));
        }).start();
    }

    private void afficherClients(List<Map<String, Object>> clients) {
        tableContainer.getChildren().clear();
        if (clients.isEmpty()) {
            Label vide = new Label("Aucun client trouvé.");
            vide.setPadding(new Insets(20));
            vide.setFont(Font.font("Georgia", 13));
            vide.setTextFill(Color.web(BRUN_LIGHT));
            tableContainer.getChildren().add(vide);
            return;
        }

        boolean alt = false;
        for (Map<String, Object> cli : clients) {
            HBox row = new HBox(0);
            row.setPadding(new Insets(11, 20, 11, 20));
            row.setAlignment(Pos.CENTER_LEFT);
            if (alt) row.setStyle("-fx-background-color: " + CREME + ";");

            int id = (int) cli.get("idUtilisateur");
            String nom = cli.get("prenom") + " " + cli.get("nom");
            String email = (String) cli.get("email");
            String statut = (String) cli.get("statut_compte"); // 'actif' / 'non actif'

            row.getChildren().addAll(
                    cellText(String.valueOf(id), false, 60),
                    cellText(nom, false, 180),
                    cellText(email, false, 260),
                    cellStatut(statut, 130),
                    cellActions(id, statut, 120)
            );

            tableContainer.getChildren().add(row);
            alt = !alt;
        }
    }

    private HBox cellText(String text, boolean isHeader, double width) {
        Text t = new Text(text != null ? text : "—");
        t.setFont(Font.font("Georgia", isHeader ? FontWeight.BOLD : FontWeight.NORMAL, isHeader ? 11 : 13));
        t.setFill(Color.web(isHeader ? BRUN_MED : BRUN));
        HBox box = new HBox(t);
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox cellStatut(String statut, double width) {
        String label; String color; String bg;
        if ("actif".equals(statut)) {
            label = "Actif"; color = SAUGE_DARK; bg = SUCCESS_BG;
        } else if ("bloque".equals(statut)) {
            label = "Bloqué"; color = DANGER; bg = DANGER_BG;
        } else {
            label = "Non actif"; color = WARNING; bg = WARNING_BG;
        }

        Label b = new Label(label);
        b.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        b.setTextFill(Color.web(color));
        b.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20; -fx-padding: 3 10;");
        
        HBox box = new HBox(b);
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox cellActions(int idUtilisateur, String statutInitial, double width) {
        boolean estBloque = "bloque".equals(statutInitial);
        Button btnToggle = new Button(estBloque ? "Débloquer" : "Bloquer");
        btnToggle.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        btnToggle.setCursor(Cursor.HAND);
        
        // Si estBloque, le bouton est vert (Débloquer), sinon rouge (Bloquer)
        validerStyleBoutonAction(btnToggle, estBloque);

        btnToggle.setOnAction(e -> {
            boolean actionEstBloquer = "Bloquer".equals(btnToggle.getText());
            String targetStatus = actionEstBloquer ? "bloque" : "actif";
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Voulez-vous vraiment " + (actionEstBloquer ? "bloquer" : "débloquer") + " ce client ?",
                ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    boolean ok = controller.changerStatut(idUtilisateur, targetStatus);
                    if (ok) {
                        chargerClients(); // Recharge la liste pour mettre à jour l'UI
                    } else {
                        Alert err = new Alert(Alert.AlertType.ERROR, "Erreur lors du changement de statut.", ButtonType.OK);
                        err.show();
                    }
                }
            });
        });

        HBox box = new HBox(btnToggle);
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void validerStyleBoutonAction(Button btn, boolean estActuelBloque) {
        String color = estActuelBloque ? SAUGE_DARK : DANGER;
        String bgHover = estActuelBloque ? "#4E7A5C" : "#A03226";
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 4 12;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + bgHover + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 4 12;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 4 12;"));
    }
}
