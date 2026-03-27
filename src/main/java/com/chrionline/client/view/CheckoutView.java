package com.chrionline.client.view;

import com.chrionline.client.controller.PanierController;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CheckoutView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String TERRA_HOVER = "#A0522D";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String BORDER      = "#E8E0D5";

    private final int idUtilisateur;
    private final CommandeDTO recap;
    private Stage stage;
    private PanierController controller;

    private ToggleGroup paymentGroup;
    private VBox carteDetailsBox;
    private TextField txtNomCarte;
    private TextField txtNumeroCarte;
    private Label msgLabel;

    public CheckoutView(int idUtilisateur, CommandeDTO recap) {
        this.idUtilisateur = idUtilisateur;
        this.recap = recap;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.controller = new PanierController(idUtilisateur);
        stage.setTitle("Paiement & Validation - ChriOnline");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // Header Centralisé
        root.getChildren().addAll(HeaderComponent.build(stage, "Panier"), buildBanner());

        VBox content = new VBox(30);
        content.setPadding(new Insets(30, 60, 50, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(900);

        HBox splitCols = new HBox(30);
        splitCols.setAlignment(Pos.TOP_CENTER);
        
        VBox leftCol = buildRecapSection();
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        
        VBox rightCol = buildPaymentSection();
        rightCol.setMinWidth(380);
        
        splitCols.getChildren().addAll(leftCol, rightCol);

        msgLabel = new Label();
        msgLabel.setTextFill(Color.web("#D32F2F"));
        msgLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 14));

        content.getChildren().addAll(splitCols, msgLabel, buildFooterControls());

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
        box.setPadding(new Insets(40, 20, 30, 20));
        box.setStyle("-fx-background-color: " + SAUGE_DARK + ";");

        Circle iconContainer = new Circle(30, Color.web(CREME_CARD));
        Text icon = new Text("💳"); icon.setFont(Font.font(30));
        StackPane badge = new StackPane(iconContainer, icon);

        Text title = new Text("Validation & Paiement");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 30)); title.setFill(Color.WHITE);

        Text sub = new Text("Dernière étape avant la préparation de votre commande !");
        sub.setFont(Font.font("Georgia", 15)); sub.setFill(Color.web("#E8F3EB"));

        box.getChildren().addAll(badge, title, sub);
        return box;
    }

    private VBox buildRecapSection() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");
        box.setEffect(new DropShadow(5, Color.web(BRUN, 0.04)));

        Label sectionTitle = new Label("Récapitulatif");
        sectionTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web(BRUN));
        box.getChildren().add(sectionTitle);

        VBox itemsBox = new VBox(12);
        for (LigneCommandeDTO ligne : recap.getLignes()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + BORDER + ";");

            Label nom = new Label(ligne.getNomProduit());
            nom.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            HBox.setHgrow(nom, Priority.ALWAYS);

            Label qte = new Label("x" + ligne.getQuantite());
            Label prix = new Label(String.format("%.2f MAD", ligne.getSousTotal()));
            prix.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            prix.setTextFill(Color.web(BRUN));

            row.getChildren().addAll(nom, qte, prix);
            itemsBox.getChildren().add(row);
        }

        Label valTotal = new Label(String.format("TOTAL : %.2f MAD", recap.getMontantTotal()));
        valTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        valTotal.setTextFill(Color.web(TERRACOTTA));

        box.getChildren().addAll(itemsBox, new Separator(), valTotal);
        return box;
    }

    private VBox buildPaymentSection() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");

        paymentGroup = new ToggleGroup();
        RadioButton rbLivraison = new RadioButton(" Paiement à la livraison");
        rbLivraison.setToggleGroup(paymentGroup); rbLivraison.setUserData("livraison"); rbLivraison.setSelected(true);

        RadioButton rbCarte = new RadioButton(" Paiement par Carte Bancaire");
        rbCarte.setToggleGroup(paymentGroup); rbCarte.setUserData("carte");

        carteDetailsBox = new VBox(10);
        txtNomCarte = new TextField(); txtNomCarte.setPromptText("Nom sur la carte");
        txtNumeroCarte = new TextField(); txtNumeroCarte.setPromptText("4 derniers chiffres");
        carteDetailsBox.getChildren().addAll(txtNomCarte, txtNumeroCarte);
        carteDetailsBox.setVisible(false); carteDetailsBox.setManaged(false);

        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isCarte = "carte".equals(newV.getUserData().toString());
            carteDetailsBox.setVisible(isCarte); carteDetailsBox.setManaged(isCarte);
        });

        box.getChildren().addAll(new Label("Méthode de Paiement"), rbLivraison, rbCarte, carteDetailsBox);
        return box;
    }

    private HBox buildFooterControls() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);

        Button btnRetour = new Button("← RETOUR AU PANIER");
        btnRetour.setStyle("-fx-background-color: transparent; -fx-border-color: " + BRUN + "; -fx-text-fill: " + BRUN + "; -fx-background-radius: 30; -fx-padding: 12 25;");
        btnRetour.setOnAction(e -> { try { new PanierView(idUtilisateur).start(stage); } catch (Exception ex) {} });

        Button btnConfirmer = new Button("CONFIRMER LA COMMANDE ✓");
        btnConfirmer.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 12 35; -fx-font-weight: bold;");
        btnConfirmer.setOnAction(e -> confirmer());

        box.getChildren().addAll(btnRetour, btnConfirmer);
        return box;
    }

    private void confirmer() {
        String methode = paymentGroup.getSelectedToggle().getUserData().toString();
        if ("carte".equals(methode) && (txtNomCarte.getText().isEmpty() || txtNumeroCarte.getText().isEmpty())) {
            msgLabel.setText("⚠ Veuillez remplir les informations de la carte."); return;
        }
        msgLabel.setText("Traitement en cours...");
        new Thread(() -> {
            CommandeDTO res = controller.confirmerCommande(methode, txtNomCarte.getText(), txtNumeroCarte.getText());
            Platform.runLater(() -> {
                if (res != null) {
                    try { new ConfirmationCommandeView(idUtilisateur, res).start(stage); } catch (Exception ex) {}
                } else {
                    msgLabel.setText("⚠ Erreur lors de la confirmation.");
                }
            });
        }).start();
    }

    public static void main(String[] args) { launch(args); }
}
