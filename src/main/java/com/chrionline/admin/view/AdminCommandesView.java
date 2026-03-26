package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminCommandesController;
import com.chrionline.shared.dto.CommandeDTO;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.List;

public class AdminCommandesView extends Application {

    // Couleurs du thème
    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String BORDER      = "#E8E0D5";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    
    private AdminCommandesController controller;
    private VBox tableContainer; // Contient les lignes de la table

    @Override
    public void start(Stage stage) {
        this.controller = new AdminCommandesController();

        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 34, 34, 34));
        content.setStyle("-fx-background-color: " + CREME + ";");

        // 1. HEADER (Titre)
        VBox titleBlock = new VBox(3);
        Text titre = new Text("Gestion des Commandes");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Gérez les expéditions et les statuts");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sousTitre.setFill(Color.web(BRUN_MED));
        
        Button btnRefresh = new Button("↻ Actualiser");
        btnRefresh.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6; -fx-text-fill: " + BRUN + ";");
        btnRefresh.setOnAction(e -> rafraichirTable());

        HBox topArea = new HBox(titleBlock, new Region(), btnRefresh);
        HBox.setHgrow(topArea.getChildren().get(1), Priority.ALWAYS); // Espace flex

        // 2. TABLE DES COMMANDES
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 10; -fx-border-color: " + BORDER + "; -fx-border-radius: 10;");
        panel.setEffect(new DropShadow(7, Color.web(BRUN, 0.05)));

        HBox tableHeader = tableRow("ID Commande", "Client", "Montant", "Date", "Statut", true);
        tableContainer = new VBox(0);
        
        panel.getChildren().addAll(tableHeader, tableContainer);

        content.getChildren().addAll(topArea, panel);

        // Affichage
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll, 1000, 700);
        stage.setTitle("ChriOnline - Commandes");
        stage.setScene(scene);
        stage.show();

        // 3. CHARGEMENT INITIAL
        rafraichirTable();
    }

    private void rafraichirTable() {
        tableContainer.getChildren().clear();
        List<CommandeDTO> commandes = controller.getToutesLesCommandes();

        if (commandes == null || commandes.isEmpty()) {
            Label vide = new Label("Aucune commande trouvée.");
            vide.setPadding(new Insets(20));
            vide.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
            tableContainer.getChildren().add(vide);
            return;
        }

        boolean alt = false;
        for (CommandeDTO cmd : commandes) {
            HBox row = tableRow(
                cmd.getIdCommande(), 
                cmd.getNomUtilisateur(), 
                cmd.getMontantTotal() + " MAD", 
                cmd.getDateCommande(), 
                cmd.getStatut(), 
                false
            );

            // Couleur alternée
            if (alt) row.setStyle("-fx-background-color: " + CREME + ";");
            tableContainer.getChildren().add(row);
            alt = !alt;
        }
    }

    private HBox tableRow(String id, String client, String montant, String date, String statutActuel, boolean isHeader) {
        HBox row = new HBox(0);
        row.setPadding(new Insets(11, 20, 11, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        
        if (isHeader) {
            row.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        }

        // Colonnes textuelles
        row.getChildren().addAll(
                cell(id, isHeader, 160),
                cell(client, isHeader, 160),
                cell(montant, isHeader, 120),
                cell(date, isHeader, 180)
        );

        // Dernière colonne : Selecteur interactif
        if (isHeader) {
            row.getChildren().add(cell("Action & Statut", true, 180));
        } else {
            ComboBox<String> statutBox = new ComboBox<>();
            statutBox.getItems().addAll("EN_PREPARATION", "EXPEDIEE", "LIVREE", "ANNULEE");
            statutBox.setValue(statutActuel); // Sélectionner le statut
            
            statutBox.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: transparent; -fx-border-color: " + BORDER + "; -fx-border-radius: 4; -fx-text-fill: " + BRUN + ";");

            if ("LIVREE".equalsIgnoreCase(statutActuel) || "ANNULEE".equalsIgnoreCase(statutActuel)) {
                statutBox.setDisable(true);
            }

            statutBox.setOnAction(e -> {
                String nouveauStatut = statutBox.getValue();
                boolean success = controller.changerStatutCommande(id, statutActuel, nouveauStatut);
                
                if (success) {
                    rafraichirTable(); // On recharge pour actualiser
                } else {
                    statutBox.setValue(statutActuel); // On remet l'ancienne valeur si erreur
                }
            });
            
            HBox b = new HBox(statutBox);
            b.setMinWidth(180); b.setPrefWidth(180);
            row.getChildren().add(b);
        }

        return row;
    }

    private HBox cell(String val, boolean isHeader, double w) {
        Text t = new Text(val != null ? val : "—");
        t.setFont(Font.font("Georgia", isHeader ? FontWeight.BOLD : FontWeight.NORMAL, isHeader ? 12 : 13));
        t.setFill(Color.web(isHeader ? BRUN_MED : BRUN));
        HBox b = new HBox(t);
        b.setMinWidth(w); b.setPrefWidth(w);
        return b;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
