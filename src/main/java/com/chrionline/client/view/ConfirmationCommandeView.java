package com.chrionline.client.view;

import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;

public class ConfirmationCommandeView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String SAUGE       = "#A8C4B0";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String TERRA_HOVER = "#A0522D";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String BORDER      = "#E8E0D5";

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

        // Header Centralisé
        root.getChildren().addAll(HeaderComponent.build(stage, "Commandes"), buildBanner());

        VBox content = new VBox(30);
        content.setPadding(new Insets(30, 60, 50, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(900);
        
        content.getChildren().addAll(
            buildTrackingCard(),
            buildQuickRecap(),
            buildItemsSection(),
            buildFooterButtons()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
        }
        if (!stage.isShowing()) stage.show();
    }

    private VBox buildBanner() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50, 20, 40, 20));
        box.setStyle("-fx-background-color: " + SAUGE_DARK + ";");

        Circle iconContainer = new Circle(35, Color.web(CREME_CARD));
        Text icon = new Text("✓"); icon.setFont(Font.font("Georgia", FontWeight.BOLD, 40)); icon.setFill(Color.web(SAUGE_DARK));
        StackPane badge = new StackPane(iconContainer, icon);

        Text title = new Text("Merci pour votre commande !");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 32)); title.setFill(Color.WHITE);

        Text sub = new Text("Votre commande a été validée avec succès.");
        sub.setFont(Font.font("Georgia", 16)); sub.setFill(Color.web("#E8F3EB"));

        box.getChildren().addAll(badge, title, sub);
        return box;
    }

    private VBox buildTrackingCard() {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 12; -fx-border-color: #FFE0B2; -fx-border-radius: 12; -fx-border-width: 2;");
        
        Text trackingMsg = new Text("Conservez votre référence pour le suivi :");
        trackingMsg.setFont(Font.font("Georgia", 15)); trackingMsg.setFill(Color.web(BRUN));
        
        Label refLabel = new Label(recap.getReference() != null ? recap.getReference() : "N/A");
        refLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 26)); refLabel.setTextFill(Color.web(TERRACOTTA));
        refLabel.setStyle("-fx-background-color: white; -fx-padding: 10 20; -fx-background-radius: 8; -fx-border-color: " + BORDER + ";");

        card.getChildren().addAll(trackingMsg, refLabel);
        return card;
    }

    private HBox buildQuickRecap() {
        HBox box = new HBox(40);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + ";");
        box.getChildren().addAll(
            buildInfoItem("DATE", recap.getDateCommande() != null ? recap.getDateCommande() : "—"),
            buildInfoItem("STATUT", "En préparation", TERRACOTTA)
        );
        return box;
    }

    private VBox buildItemsSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + ";");
        
        Label title = new Label("Articles commandés");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        box.getChildren().add(title);

        if (recap.getLignes() != null) {
            for (LigneCommandeDTO l : recap.getLignes()) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));
                row.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 6;");
                Label name = new Label(l.getNomProduit()); HBox.setHgrow(name, Priority.ALWAYS);
                Label qte = new Label("x" + l.getQuantite());
                Label st = new Label(String.format("%.2f MAD", l.getSousTotal()));
                row.getChildren().addAll(name, qte, st);
                box.getChildren().add(row);
            }
        }
        
        Label valTotal = new Label(String.format("TOTAL RÉGLÉ : %.2f MAD", recap.getMontantTotal()));
        valTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 20)); valTotal.setTextFill(Color.web(SAUGE_DARK));
        box.getChildren().addAll(new Separator(), valTotal);
        
        return box;
    }

    private HBox buildFooterButtons() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        Button btnFacture = new Button("📄 TÉLÉCHARGER LA FACTURE");
        btnFacture.setStyle("-fx-background-color: transparent; -fx-border-color: " + SAUGE_DARK + "; -fx-text-fill: " + SAUGE_DARK + "; -fx-background-radius: 30; -fx-padding: 12 25;");
        btnFacture.setOnAction(e -> {
            try {
                File f = new File("factures/" + recap.getReference() + ".pdf");
                if (f.exists()) java.awt.Desktop.getDesktop().open(f);
            } catch (Exception ex) {}
        });

        Button btnCatalogue = new Button("← RETOUR AU CATALOGUE");
        btnCatalogue.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 12 35; -fx-font-weight: bold;");
        btnCatalogue.setOnAction(e -> { try { new CatalogueView(idUtilisateur).start(stage); } catch (Exception ex) {} });

        box.getChildren().addAll(btnFacture, btnCatalogue);
        return box;
    }

    private VBox buildInfoItem(String l, String v) { return buildInfoItem(l, v, BRUN); }
    private VBox buildInfoItem(String l, String v, String color) {
        VBox box = new VBox(5); box.setAlignment(Pos.CENTER);
        Label lb = new Label(l); lb.setFont(Font.font("Georgia", FontWeight.BOLD, 12)); lb.setTextFill(Color.web(BRUN_LIGHT));
        Label va = new Label(v); va.setFont(Font.font("Georgia", FontWeight.BOLD, 16)); va.setTextFill(Color.web(color));
        box.getChildren().addAll(lb, va);
        return box;
    }

    public static void main(String[] args) { launch(args); }
}
