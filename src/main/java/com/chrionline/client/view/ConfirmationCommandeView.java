package com.chrionline.client.view;

import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ConfirmationCommandeView extends Application {

    private static final String BRUN       = "#3E2723";
    private static final String BRUN_MED   = "#5D4037";
    private static final String BRUN_LIGHT = "#8D6E63";
    private static final String SAUGE      = "#818C78";
    private static final String CREME      = "#F9F7F2";
    private static final String CREME_DARK = "#E0D7C6";
    private static final String BORDER     = "#D7CCC8";

    private final int idUtilisateur;
    private final CommandeDTO recap;
    private Stage stage;

    public ConfirmationCommandeView(int idUtilisateur, CommandeDTO recap) {
        this.idUtilisateur = idUtilisateur;
        this.recap = recap;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Confirmation de Commande - ChriOnline");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // --- Header ---
        root.getChildren().add(buildHeader());

        // --- Contenu scrollable ---
        VBox content = new VBox(28);
        content.setPadding(new Insets(30, 50, 40, 50));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(900);
        content.getChildren().addAll(
            buildQuickRecap(),
            buildClientSection(),
            buildItemsSection(),
            buildFooter()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: " + CREME + "; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        stage.setScene(new Scene(root, 950, 750));
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildHeader() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 0, 30, 0));
        box.setStyle("-fx-background-color: " + SAUGE + ";");

        Text icon = new Text("✓");
        icon.setFont(Font.font("Georgia", FontWeight.BOLD, 48));
        icon.setFill(Color.WHITE);

        Text title = new Text("Merci pour votre commande !");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);

        Text sub = new Text("Votre commande a été validée et est en cours de préparation.");
        sub.setFont(Font.font("Georgia", 15));
        sub.setFill(Color.web("#E0E0E0"));

        box.getChildren().addAll(icon, title, sub);
        return box;
    }

    private HBox buildQuickRecap() {
        HBox box = new HBox(50);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15, 25, 15, 25));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        box.getChildren().addAll(
            buildInfoItem("RÉFÉRENCE",  recap.getReference() != null ? recap.getReference() : "—"),
            buildInfoItem("DATE",       recap.getDateCommande() != null ? recap.getDateCommande() : "—"),
            buildInfoItem("STATUT",     "En préparation")
        );
        return box;
    }

    private VBox buildClientSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");

        Label title = new Label("Informations Client");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(BRUN));

        Label client = new Label(recap.getNomUtilisateur() != null ? recap.getNomUtilisateur() : "—");
        client.setFont(Font.font("Georgia", 14));
        client.setTextFill(Color.web(BRUN_MED));

        box.getChildren().addAll(title, client);
        return box;
    }

    private VBox buildItemsSection() {
        VBox box = new VBox(15);

        Label title = new Label("Détails de la commande");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(BRUN));

        VBox itemsBox = new VBox(10);
        if (recap.getLignes() != null) {
            for (LigneCommandeDTO ligne : recap.getLignes()) {
                itemsBox.getChildren().add(buildItemRow(ligne));
            }
        }

        box.getChildren().addAll(title, itemsBox);
        return box;
    }

    private HBox buildItemRow(LigneCommandeDTO ligne) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + CREME_DARK + "; -fx-border-radius: 8;");

        VBox infos = new VBox(4);
        Label name = new Label(ligne.getNomProduit());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        name.setTextFill(Color.web(BRUN));
        infos.getChildren().add(name);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label qte = new Label("x" + ligne.getQuantite());
        qte.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        qte.setTextFill(Color.web(BRUN_MED));
        qte.setMinWidth(40);

        Label prix = new Label(String.format("%,.2f MAD", ligne.getSousTotal()));
        prix.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        prix.setTextFill(Color.web(BRUN));
        prix.setMinWidth(100);
        prix.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(infos, qte, prix);
        return row;
    }

    private VBox buildFooter() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.CENTER);

        HBox totalBox = new HBox(20);
        totalBox.setAlignment(Pos.CENTER_RIGHT);

        Label lbl = new Label("MONTANT TOTAL :");
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.web(BRUN_LIGHT));

        Label val = new Label(String.format("%,.2f MAD", recap.getMontantTotal()));
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        val.setTextFill(Color.web(BRUN));

        totalBox.getChildren().addAll(lbl, val);

        Button btnCatalogue = new Button("RETOURNER AU CATALOGUE");
        btnCatalogue.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnCatalogue.setPadding(new Insets(15, 40, 15, 40));
        btnCatalogue.setCursor(javafx.scene.Cursor.HAND);
        btnCatalogue.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-background-radius: 30;");
        btnCatalogue.setOnMouseEntered(e -> btnCatalogue.setStyle("-fx-background-color: " + SAUGE + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnCatalogue.setOnMouseExited(e -> btnCatalogue.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnCatalogue.setOnAction(e -> { try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); } });

        box.getChildren().addAll(totalBox, btnCatalogue);
        return box;
    }

    private VBox buildInfoItem(String label, String value) {
        VBox v = new VBox(4);
        v.setAlignment(Pos.CENTER);
        Label l = new Label(label);
        l.setFont(Font.font("System", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(BRUN_LIGHT));
        Label v1 = new Label(value);
        v1.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        v1.setTextFill(Color.web(BRUN));
        v.getChildren().addAll(l, v1);
        return v;
    }

    public static void main(String[] args) { launch(args); }
}
