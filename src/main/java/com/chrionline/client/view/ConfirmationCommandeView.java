package com.chrionline.client.view;

import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LignePanierDTO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ConfirmationCommandeView extends Application {

    // Couleurs Thème (identiques à PanierView pour la cohérence)
    private static final String BRUN       = "#3E2723";
    private static final String BRUN_MED   = "#5D4037";
    private static final String BRUN_LIGHT = "#8D6E63";
    private static final String SAUGE      = "#818C78";
    private static final String SAUGE_DARK = "#5F6B58";
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
        VBox header = buildHeader();
        root.getChildren().add(header);

        // --- Corps (Scrollable) ---
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: " + CREME + "; -fx-border-color: transparent;");
        
        VBox content = new VBox(30);
        content.setPadding(new Insets(30, 50, 40, 50));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(900);

        // Section 1: Récapitulatif Rapide (Ref/Date)
        content.getChildren().add(buildQuickRecap());

        // Section 2: Info Livraison & Client
        content.getChildren().add(buildClientSection());

        // Section 3: Détails des produits
        content.getChildren().add(buildItemsSection());

        // Section 4: Total & Bouton Retour
        content.getChildren().add(buildFooter());

        scroll.setContent(content);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        Scene scene = new Scene(root, 950, 750);
        stage.setScene(scene);
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

        VBox v1 = buildInfoItem("RÉFÉRENCE", recap.getReference());
        VBox v2 = buildInfoItem("DATE", recap.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        VBox v3 = buildInfoItem("STATUT", "En préparation");

        box.getChildren().addAll(v1, v2, v3);
        return box;
    }

    private VBox buildClientSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");

        Label title = new Label("Informations de Livraison");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(BRUN));

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(10);

        addGridInfo(grid, 0, "Client", recap.getClientPrenom() + " " + recap.getClientNom());
        addGridInfo(grid, 1, "E-mail", recap.getClientEmail());
        addGridInfo(grid, 2, "Téléphone", recap.getClientTelephone() != null ? recap.getClientTelephone() : "Non renseigné");
        addGridInfo(grid, 3, "Adresse", recap.getClientAdresse());

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox buildItemsSection() {
        VBox box = new VBox(15);
        
        Label title = new Label("Détails de la commande");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(BRUN));

        VBox itemsBox = new VBox(12);
        for (LignePanierDTO ligne : recap.getLignes()) {
            itemsBox.getChildren().add(buildItemRow(ligne));
        }

        box.getChildren().addAll(title, itemsBox);
        return box;
    }

    private HBox buildItemRow(LignePanierDTO ligne) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + CREME_DARK + "; -fx-border-radius: 8;");

        // Image
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(60, 60);
        imgBox.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 6;");
        
        try {
            if (ligne.getImage_url() != null && !ligne.getImage_url().isBlank()) {
                ImageView iv = new ImageView(new Image(ligne.getImage_url(), true));
                iv.setFitWidth(50); iv.setFitHeight(50); iv.setPreserveRatio(true);
                imgBox.getChildren().add(iv);
            } else {
                Text txt = new Text("?"); txt.setFill(Color.web(BRUN_LIGHT));
                imgBox.getChildren().add(txt);
            }
        } catch(Exception e) {
            Text txt = new Text("?"); txt.setFill(Color.web(BRUN_LIGHT));
            imgBox.getChildren().add(txt);
        }

        VBox infos = new VBox(4);
        Label name = new Label(ligne.getNomProduit());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        name.setTextFill(Color.web(BRUN));

        Label variant = new Label(ligne.getDescriptionVariant());
        variant.setFont(Font.font("Georgia", 11));
        variant.setTextFill(Color.web(BRUN_LIGHT));

        infos.getChildren().addAll(name, variant);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label qte = new Label("x" + ligne.getQuantite());
        qte.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        qte.setTextFill(Color.web(BRUN_MED));
        qte.setMinWidth(40);

        Label prix = new Label(formatMonnaie(ligne.getTotal()) + " MAD");
        prix.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        prix.setTextFill(Color.web(BRUN));
        prix.setMinWidth(100);
        prix.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(imgBox, infos, qte, prix);
        return row;
    }

    private VBox buildFooter() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 0, 0, 0));

        HBox totalBox = new HBox(20);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setMaxWidth(400);

        Label lbl = new Label("MONTANT TOTAL :");
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.web(BRUN_LIGHT));

        Label val = new Label(formatMonnaie(recap.getMontantTotal()) + " MAD");
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
        btnCatalogue.setOnAction(e -> retourCatalogue());

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

    private void addGridInfo(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label + " :");
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(BRUN_LIGHT));
        Label val = new Label(value);
        val.setFont(Font.font("Georgia", 13));
        val.setWrapText(true);
        val.setTextFill(Color.web(BRUN));
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private String formatMonnaie(BigDecimal val) {
        return val != null ? String.format("%,.2f", val) : "0,00";
    }

    private void retourCatalogue() {
        try { new CatalogueView(idUtilisateur).start(stage); }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
