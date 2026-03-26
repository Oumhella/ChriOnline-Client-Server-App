package com.chrionline.client.view;

import com.chrionline.client.controller.PanierController;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CheckoutView extends Application {

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

        // Header (Same as HomeView)
        root.getChildren().addAll(buildHeader(stage), buildBanner());

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

        content.getChildren().addAll(splitCols, msgLabel, buildFooter());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(scroll);

        stage.setScene(new Scene(root, 1000, 700));
        stage.centerOnScreen();
        stage.show();
    }

    // --- EXACT REPRODUCTION OF HOMEVIEW HEADER ---
    private HBox buildHeader(Stage stage) {
        HBox header = new HBox(40);
        header.setPadding(new Insets(25, 60, 25, 60));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + CREME + "; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> { try { new HomeView().start(stage); } catch (Exception ex) {} });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(35);
        nav.setAlignment(Pos.CENTER);
        
        Hyperlink hAcc = navLink("Accueil", false);
        Hyperlink hCat = navLink("Catalogue", false);
        Hyperlink hOrd = navLink("Mes Commandes", false);
        Hyperlink hAbo = navLink("À propos", false);
        
        hAcc.setOnAction(e -> { try { new HomeView().start(stage); } catch (Exception ex) {} });
        hCat.setOnAction(e -> { try { new CatalogueView().start(stage); } catch (Exception ex) {} });
        hOrd.setOnAction(e -> { try { new MesCommandesView().start(stage); } catch (Exception ex) {} });

        nav.getChildren().addAll(hAcc, hCat, hOrd, hAbo);
        
        HBox rightControls = new HBox(20);
        rightControls.setAlignment(Pos.CENTER);
        
        boolean isLogged = com.chrionline.client.session.SessionManager.getInstance().isLogged();
        if (isLogged) {
            String prenom = com.chrionline.client.session.SessionManager.getInstance().getPrenom();
            String nom = com.chrionline.client.session.SessionManager.getInstance().getNom();
            String initials = (prenom != null && !prenom.isEmpty() ? prenom.toUpperCase().substring(0,1) : "") + 
                              (nom != null && !nom.isEmpty() ? nom.toUpperCase().substring(0,1) : "");
            if (initials.isEmpty()) initials = "U";
            
            StackPane avatar = new StackPane();
            Circle circle = new Circle(18, Color.web(TERRACOTTA));
            Text initText = new Text(initials);
            initText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            initText.setFill(Color.WHITE);
            avatar.getChildren().addAll(circle, initText);
            avatar.setCursor(javafx.scene.Cursor.HAND);
            avatar.setOnMouseClicked(e -> { try { new ProfilView().start(stage); } catch (Exception ex) {} });
            rightControls.getChildren().add(avatar);
        }

        Button btnAuth = new Button(isLogged ? "Déconnexion" : "Connexion");
        btnAuth.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN + "; -fx-border-color: " + BRUN + "; -fx-border-radius: 20; -fx-padding: 8 20; -fx-font-family: 'Georgia';");
        btnAuth.setOnAction(e -> {
            if (isLogged) com.chrionline.client.session.SessionManager.getInstance().clear();
            try { new ConnexionView().start(stage); } catch (Exception ex) {}
        });
        rightControls.getChildren().add(btnAuth);

        header.getChildren().addAll(logo, spacer, nav, rightControls);
        return header;
    }

    private Hyperlink navLink(String text, boolean actif) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Georgia", actif ? FontWeight.BOLD : FontWeight.NORMAL, 16));
        link.setTextFill(Color.web(actif ? SAUGE_DARK : BRUN));
        link.setUnderline(false);
        link.setStyle("-fx-border-color: transparent;");
        return link;
    }

    private VBox buildBanner() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 20, 30, 20));
        box.setStyle("-fx-background-color: " + SAUGE_DARK + ";");

        Circle iconContainer = new Circle(30, Color.web(CREME_CARD));
        Text icon = new Text("💳");
        icon.setFont(Font.font(30));
        StackPane checkBadge = new StackPane(iconContainer, icon);

        Text title = new Text("Validation & Paiement");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
        title.setFill(Color.WHITE);

        Text sub = new Text("Dernière étape avant la préparation de votre commande !");
        sub.setFont(Font.font("Georgia", 15));
        sub.setFill(Color.web("#E8F3EB"));

        box.getChildren().addAll(checkBadge, title, sub);
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
            row.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

            Label nom = new Label(ligne.getNomProduit());
            nom.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            nom.setTextFill(Color.web(BRUN));
            HBox.setHgrow(nom, Priority.ALWAYS);

            Label qte = new Label("x" + ligne.getQuantite());
            qte.setFont(Font.font("Georgia", 13));
            qte.setTextFill(Color.web(BRUN_MED));
            qte.setMinWidth(40);

            Label prix = new Label(String.format("%,.2f MAD", ligne.getSousTotal()));
            prix.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            prix.setTextFill(Color.web(BRUN));
            prix.setMinWidth(90);
            prix.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(nom, qte, prix);
            itemsBox.getChildren().add(row);
        }

        Separator sep = new Separator();
        
        HBox totalRow = new HBox(10);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTotal = new Label("TOTAL À PAYER");
        lblTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        lblTotal.setTextFill(Color.web(BRUN_LIGHT));
        
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        
        Label valTotal = new Label(String.format("%,.2f MAD", recap.getMontantTotal()));
        valTotal.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        valTotal.setTextFill(Color.web(TERRACOTTA));
        
        totalRow.getChildren().addAll(lblTotal, sp2, valTotal);

        box.getChildren().addAll(itemsBox, sep, totalRow);
        return box;
    }

    private VBox buildPaymentSection() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");
        box.setEffect(new DropShadow(5, Color.web(BRUN, 0.04)));

        Label sectionTitle = new Label("Méthode de Paiement");
        sectionTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web(BRUN));

        paymentGroup = new ToggleGroup();
        
        RadioButton rbLivraison = new RadioButton(" Paiement à la livraison");
        rbLivraison.setFont(Font.font("Georgia", 15));
        rbLivraison.setTextFill(Color.web(BRUN));
        rbLivraison.setToggleGroup(paymentGroup);
        rbLivraison.setUserData("livraison");
        rbLivraison.setSelected(true);

        RadioButton rbCarte = new RadioButton(" Paiement par Carte Bancaire");
        rbCarte.setFont(Font.font("Georgia", 15));
        rbCarte.setTextFill(Color.web(BRUN));
        rbCarte.setToggleGroup(paymentGroup);
        rbCarte.setUserData("carte");

        carteDetailsBox = new VBox(15);
        carteDetailsBox.setPadding(new Insets(20));
        carteDetailsBox.setStyle("-fx-background-color: " + CREME + "; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");
        carteDetailsBox.setVisible(false);
        carteDetailsBox.setManaged(false);

        Label lblCarte = new Label("Veuillez saisir vos coordonnées :");
        lblCarte.setFont(Font.font("Georgia", 13));
        lblCarte.setTextFill(Color.web(BRUN_LIGHT));

        txtNomCarte = new TextField();
        txtNomCarte.setPromptText("Nom complet sur la carte");
        txtNomCarte.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 14; -fx-padding: 10; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: " + BORDER + ";");

        txtNumeroCarte = new TextField();
        txtNumeroCarte.setPromptText("4 derniers chiffres (Ex: 1234)");
        txtNumeroCarte.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14; -fx-padding: 10; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: " + BORDER + ";");

        carteDetailsBox.getChildren().addAll(lblCarte, txtNomCarte, txtNumeroCarte);

        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                boolean isCarte = "carte".equals(newV.getUserData().toString());
                carteDetailsBox.setVisible(isCarte);
                carteDetailsBox.setManaged(isCarte);
            }
        });

        box.getChildren().addAll(sectionTitle, rbLivraison, rbCarte, carteDetailsBox);
        return box;
    }

    private HBox buildFooter() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 0, 0, 0));

        Button btnRetour = new Button("← RETOUR AU PANIER");
        btnRetour.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnRetour.setPadding(new Insets(15, 30, 15, 30));
        btnRetour.setCursor(javafx.scene.Cursor.HAND);
        btnRetour.setStyle("-fx-background-color: transparent; -fx-border-color: " + BRUN + "; -fx-text-fill: " + BRUN + "; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;");
        btnRetour.setOnMouseEntered(e -> btnRetour.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-border-color: " + BRUN + "; -fx-text-fill: " + BRUN + "; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;"));
        btnRetour.setOnMouseExited(e -> btnRetour.setStyle("-fx-background-color: transparent; -fx-border-color: " + BRUN + "; -fx-text-fill: " + BRUN + "; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-width: 2;"));
        btnRetour.setOnAction(e -> {
            try { new PanierView(idUtilisateur).start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        Button btnConfirmer = new Button("CONFIRMER LA COMMANDE ✓");
        btnConfirmer.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnConfirmer.setPadding(new Insets(15, 40, 15, 40));
        btnConfirmer.setCursor(javafx.scene.Cursor.HAND);
        btnConfirmer.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30;");
        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle("-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnConfirmer.setOnMouseExited(e -> btnConfirmer.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 30;"));
        btnConfirmer.setOnAction(e -> confirmer());

        box.getChildren().addAll(btnRetour, btnConfirmer);
        return box;
    }

    private void confirmer() {
        Toggle selected = paymentGroup.getSelectedToggle();
        if (selected == null) {
            msgLabel.setText("⚠ Veuillez sélectionner une méthode de paiement.");
            return;
        }

        String methode = selected.getUserData().toString();
        String nomCarte = null;
        String numCarte = null;

        if ("carte".equals(methode)) {
            nomCarte = txtNomCarte.getText().trim();
            numCarte = txtNumeroCarte.getText().trim();
            if (nomCarte.isEmpty() || numCarte.isEmpty()) {
                msgLabel.setText("⚠ Veuillez remplir les informations de la carte.");
                return;
            }
        }

        msgLabel.setText("Traitement sécurisé en cours...");
        msgLabel.setTextFill(Color.web(SAUGE_DARK));
        
        String finalNomCarte = nomCarte;
        String finalNumCarte = numCarte;

        new Thread(() -> {
            CommandeDTO res = controller.confirmerCommande(methode, finalNomCarte, finalNumCarte);
            Platform.runLater(() -> {
                if (res != null) {
                    try {
                        new ConfirmationCommandeView(idUtilisateur, res).start(stage);
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    msgLabel.setTextFill(Color.web("#D32F2F"));
                    msgLabel.setText("⚠ Erreur lors de la confirmation de votre paiement.");
                }
            });
        }).start();
    }
}
