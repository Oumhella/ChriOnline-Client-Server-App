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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;

public class ConfirmationCommandeView extends Application {

    private static final String CREME = "#FDFBF7";
    private static final String CREME_CARD = "#FFFEFB";
    private static final String SAUGE_DARK = "#6B9E7A";
    private static final String SAUGE = "#A8C4B0";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String TERRA_HOVER = "#A0522D";
    private static final String BRUN = "#3E2C1E";
    private static final String BRUN_MED = "#6B4F3A";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String BORDER = "#E8E0D5";

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

        root.getChildren().addAll(buildNavigationBar(), buildBanner());

        // --- Contenu scrollable ---
        VBox content = new VBox(30);
        content.setPadding(new Insets(40, 60, 50, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(900);
        
        content.getChildren().addAll(
            buildTrackingCard(),
            buildQuickRecap(),
            buildItemsSection(),
            buildFooter()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: " + CREME + "; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        stage.setScene(new Scene(root, 950, 800));
        stage.centerOnScreen();
        stage.show();
    }

    private javafx.scene.layout.HBox buildNavigationBar() {
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(40);
        header.setPadding(new Insets(22, 48, 22, 48));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + CREME_CARD + ";" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> {
            try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(28);
        nav.setAlignment(Pos.CENTER);
        
        javafx.scene.control.Hyperlink lAccueil = new javafx.scene.control.Hyperlink("Accueil");
        lAccueil.setFont(Font.font("Georgia", 14));
        lAccueil.setTextFill(Color.web(BRUN));
        lAccueil.setStyle("-fx-border-color: transparent;");
        lAccueil.setOnAction(e -> { try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); } });

        javafx.scene.control.Hyperlink lCatalogue = new javafx.scene.control.Hyperlink("Catalogue");
        lCatalogue.setFont(Font.font("Georgia", 14));
        lCatalogue.setTextFill(Color.web(BRUN));
        lCatalogue.setStyle("-fx-border-color: transparent;");
        lCatalogue.setOnAction(e -> { try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); } });

        Text tAction = new Text("Commande Validée");
        tAction.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        tAction.setFill(Color.web(SAUGE_DARK));

        nav.getChildren().addAll(lAccueil, lCatalogue, tAction);
        header.getChildren().addAll(logo, spacer, nav);
        return header;
    }

    private VBox buildBanner() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50, 20, 40, 20));
        box.setStyle("-fx-background-color: " + SAUGE_DARK + ";");

        Circle iconContainer = new Circle(35, Color.web(CREME_CARD));
        Text icon = new Text("✓");
        icon.setFont(Font.font("Georgia", FontWeight.BOLD, 40));
        icon.setFill(Color.web(SAUGE_DARK));
        StackPane checkBadge = new StackPane(iconContainer, icon);

        Text title = new Text("Merci pour votre commande !");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 32));
        title.setFill(Color.WHITE);

        Text sub = new Text("Votre commande a été validée avec succès et est en cours de préparation.");
        sub.setFont(Font.font("Georgia", 16));
        sub.setFill(Color.web("#E8F3EB"));

        box.getChildren().addAll(checkBadge, title, sub);
        return box;
    }

    private VBox buildTrackingCard() {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 12; -fx-border-color: #FFE0B2; -fx-border-radius: 12; -fx-border-width: 2;");
        
        DropShadow shadow = new DropShadow(8, Color.web(BRUN, 0.05));
        card.setEffect(shadow);

        Text warningIcon = new Text("🔔 À CONSERVER");
        warningIcon.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        warningIcon.setFill(Color.web("#E65100"));

        Text trackingMsg = new Text("Veuillez conserver précieusement votre référence de commande ci-dessous.\nElle est indispensable pour suivre l'état d'avancement de votre livraison.");
        trackingMsg.setFont(Font.font("Georgia", 15));
        trackingMsg.setTextAlignment(TextAlignment.CENTER);
        trackingMsg.setFill(Color.web(BRUN));
        
        Label refLabel = new Label(recap.getReference() != null ? recap.getReference() : "N/A");
        refLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 26));
        refLabel.setTextFill(Color.web(TERRACOTTA));
        refLabel.setStyle("-fx-background-color: white; -fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        card.getChildren().addAll(warningIcon, trackingMsg, refLabel);
        return card;
    }

    private HBox buildQuickRecap() {
        HBox box = new HBox(40);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 30, 20, 30));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");
        box.setEffect(new DropShadow(5, Color.web(BRUN, 0.03)));

        // Le client n'a besoin que des infos de baser
        box.getChildren().addAll(
            buildInfoItem("DATE DE COMMANDE", recap.getDateCommande() != null ? recap.getDateCommande() : "—"),
            buildInfoItem("CLIENT",           recap.getNomUtilisateur() != null ? recap.getNomUtilisateur() : "—"),
            buildInfoItem("STATUT ACTUEL",    "En préparation", TERRACOTTA)
        );
        return box;
    }

    private VBox buildItemsSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");

        Label title = new Label("Détails des articles");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(BRUN));

        VBox itemsBox = new VBox(12);
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
        row.setPadding(new Insets(15));
        row.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        Label name = new Label(ligne.getNomProduit());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setTextFill(Color.web(BRUN));
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label qte = new Label("Quantité : " + ligne.getQuantite());
        qte.setFont(Font.font("Georgia", 14));
        qte.setTextFill(Color.web(BRUN_MED));
        qte.setMinWidth(90);

        Label prix = new Label(String.format("%,.2f MAD", ligne.getSousTotal()));
        prix.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        prix.setTextFill(Color.web(BRUN));
        prix.setMinWidth(110);
        prix.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(name, qte, prix);
        return row;
    }

    private VBox buildFooter() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.CENTER);

        HBox totalBox = new HBox(20);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(20, 30, 20, 30));
        totalBox.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-background-radius: 12;");

        Label lbl = new Label("MONTANT TOTAL RÉGLÉ");
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.WHITE);

        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        Label val = new Label(String.format("%,.2f MAD", recap.getMontantTotal()));
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        val.setTextFill(Color.WHITE);

        totalBox.getChildren().addAll(lbl, space, val);

        Button btnFacture = new Button("📄 TÉLÉCHARGER LA FACTURE (PDF)");
        btnFacture.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnFacture.setPadding(new Insets(15, 30, 15, 30));
        btnFacture.setCursor(javafx.scene.Cursor.HAND);
        btnFacture.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-border-color: " + SAUGE_DARK + "; -fx-text-fill: " + SAUGE_DARK + "; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;");
        btnFacture.setOnMouseEntered(e -> btnFacture.setStyle("-fx-background-color: " + SAUGE + "; -fx-border-color: " + SAUGE_DARK + "; -fx-text-fill: white; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;"));
        btnFacture.setOnMouseExited(e -> btnFacture.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-border-color: " + SAUGE_DARK + "; -fx-text-fill: " + SAUGE_DARK + "; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;"));
        btnFacture.setOnAction(e -> {
            try {
                File file = new File("factures/" + recap.getReference() + ".pdf");
                if (file.exists()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    System.out.println("Facture non trouvée localement pour : " + file.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnCatalogue = new Button("← RETOURNER AU CATALOGUE");
        btnCatalogue.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnCatalogue.setPadding(new Insets(15, 30, 15, 30));
        btnCatalogue.setCursor(javafx.scene.Cursor.HAND);
        btnCatalogue.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30;");
        btnCatalogue.setOnMouseEntered(e -> btnCatalogue.setStyle("-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnCatalogue.setOnMouseExited(e -> btnCatalogue.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnCatalogue.setOnAction(e -> { try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); } });

        HBox boutonsBox = new HBox(20);
        boutonsBox.setAlignment(Pos.CENTER);
        boutonsBox.getChildren().addAll(btnFacture, btnCatalogue);

        box.getChildren().addAll(totalBox, boutonsBox);
        return box;
    }

    private VBox buildInfoItem(String label, String value) {
        return buildInfoItem(label, value, BRUN);
    }

    private VBox buildInfoItem(String label, String value, String colorHex) {
        VBox v = new VBox(6);
        v.setAlignment(Pos.CENTER);
        Label l = new Label(label);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(BRUN_LIGHT));
        Label v1 = new Label(value);
        v1.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        v1.setTextFill(Color.web(colorHex));
        v.getChildren().addAll(l, v1);
        return v;
    }

}
