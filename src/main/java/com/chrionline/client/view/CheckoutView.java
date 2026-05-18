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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Map;

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
    private VBox livraisonDetailsBox;
    private ToggleGroup livraisonSousGroup;
    private Label msgLabel;
    private TextField txtCode2fa;
    private VBox box2faSection;
    private boolean enAttente2fa = false;

    // Éléments UI pour TOTP (Microsoft Authenticator)
    private Label lbl2faTitle;
    private VBox setupTotpBox;
    private ImageView imgQrCode;

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
        
        VBox rightCol = new VBox(20);
        rightCol.setMinWidth(380);
        box2faSection = build2FASection();
        rightCol.getChildren().addAll(buildPaymentSection(), box2faSection);

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

        livraisonSousGroup = new ToggleGroup();
        RadioButton rbLivraisonEspece = new RadioButton(" Par espèce");
        rbLivraisonEspece.setToggleGroup(livraisonSousGroup); rbLivraisonEspece.setUserData("livraison_espece"); rbLivraisonEspece.setSelected(true);

        RadioButton rbLivraisonCarte = new RadioButton(" Par carte bancaire (TPE)");
        rbLivraisonCarte.setToggleGroup(livraisonSousGroup); rbLivraisonCarte.setUserData("livraison_carte");
        
        livraisonDetailsBox = new VBox(10);
        livraisonDetailsBox.setPadding(new Insets(0, 0, 0, 30));
        livraisonDetailsBox.getChildren().addAll(rbLivraisonEspece, rbLivraisonCarte);
        
        RadioButton rbCarte = new RadioButton(" Paiement en ligne (Carte Bancaire)");
        rbCarte.setToggleGroup(paymentGroup); rbCarte.setUserData("carte");

        carteDetailsBox = new VBox(10);
        txtNomCarte = new TextField(); txtNomCarte.setPromptText("Nom sur la carte");
        txtNumeroCarte = new TextField(); txtNumeroCarte.setPromptText("Numéro de carte (16 chiffres)");
        txtNumeroCarte.textProperty().addListener((obs, oldVal, newVal) -> {
            // Accepter uniquement les chiffres et espaces, max 19 caractères (16 + 3 espaces)
            if (!newVal.matches("[0-9 ]{0,19}")) txtNumeroCarte.setText(oldVal);
        });
        carteDetailsBox.getChildren().addAll(txtNomCarte, txtNumeroCarte);
        carteDetailsBox.setVisible(false); carteDetailsBox.setManaged(false);

        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isCarte = "carte".equals(newV.getUserData().toString());
            boolean isLivraison = "livraison".equals(newV.getUserData().toString());
            carteDetailsBox.setVisible(isCarte); carteDetailsBox.setManaged(isCarte);
            livraisonDetailsBox.setVisible(isLivraison); livraisonDetailsBox.setManaged(isLivraison);
            
            // Afficher le champ TOTP systématiquement dès qu'on choisit un mode de paiement
            if (box2faSection != null) {
                new Thread(() -> {
                    java.util.Map<String, Object> req = new java.util.HashMap<>();
                    req.put("commande", "CHECK_TOTP_SETUP");
                    try {
                        com.chrionline.client.network.Client c = com.chrionline.client.network.Client.getInstance();
                        c.envoyerRequete(req);
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> rep = (java.util.Map<String, Object>) c.lireReponse();
                        javafx.application.Platform.runLater(() -> {
                            box2faSection.setVisible(true);
                            box2faSection.setManaged(true);
                            if ("REQUIRES_TOTP_SETUP".equals(rep.get("statut"))) {
                                String otpauthUri = (String) rep.get("otpauthUri");
                                try {
                                    String qrUrl = "https://quickchart.io/qr?size=180&text=" + java.net.URLEncoder.encode(otpauthUri, java.nio.charset.StandardCharsets.UTF_8.toString());
                                    imgQrCode.setImage(new javafx.scene.image.Image(qrUrl, true));
                                } catch (Exception ex) {
                                    System.err.println("[CheckoutView] Échec QR Code : " + ex.getMessage());
                                }
                                setupTotpBox.setVisible(true);
                                setupTotpBox.setManaged(true);
                                lbl2faTitle.setText("1. Liaison Microsoft Authenticator");
                            } else {
                                setupTotpBox.setVisible(false);
                                setupTotpBox.setManaged(false);
                                lbl2faTitle.setText("Code Microsoft Authenticator");
                            }
                            enAttente2fa = true; // Force la saisie du code avant de cliquer sur confirmer
                        });
                    } catch (Exception ex) {
                        System.err.println("[CheckoutView] Erreur CHECK_TOTP_SETUP: " + ex.getMessage());
                    }
                }).start();
            }
        });

        box.getChildren().addAll(new Label("Méthode de Paiement"), rbLivraison, livraisonDetailsBox, rbCarte, carteDetailsBox);
        return box;
    }

    private VBox build2FASection() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #F8F5F0; -fx-border-color: #E8E0D5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;");

        lbl2faTitle = new Label("Sécurité Double Facteur (2FA)");
        lbl2faTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        lbl2faTitle.setTextFill(Color.web(BRUN));

        // 1. Setup TOTP Section (Initialement invisible)
        setupTotpBox = new VBox(8);
        setupTotpBox.setVisible(false);
        setupTotpBox.setManaged(false);

        Label lblSetupDesc = new Label("1. Ouvrez l'application Microsoft Authenticator\n2. Ajoutez un compte professionnel ou scolaire\n3. Scannez ce QR Code :");
        lblSetupDesc.setFont(Font.font("System", 12));
        lblSetupDesc.setTextFill(Color.web(BRUN_MED));

        // QR Code ImageView
        imgQrCode = new ImageView();
        imgQrCode.setFitWidth(180);
        imgQrCode.setFitHeight(180);
        
        HBox qrBox = new HBox(imgQrCode);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setStyle("-fx-padding: 5; -fx-background-color: #FFF; -fx-border-color: #E8E0D5; -fx-border-radius: 4; -fx-background-radius: 4;");
        qrBox.setMaxWidth(190);

        setupTotpBox.getChildren().addAll(lblSetupDesc, qrBox);

        // 2. Champ de validation du code
        Label lblCodePrompt = new Label("Saisissez le code à 6 chiffres généré :");
        lblCodePrompt.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblCodePrompt.setTextFill(Color.web(BRUN));

        txtCode2fa = new TextField();
        txtCode2fa.setPromptText("Ex: 123456");
        txtCode2fa.setMaxWidth(160);
        txtCode2fa.setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-font-weight: bold; -fx-border-color: #E8E0D5; -fx-border-radius: 4; -fx-background-radius: 4;");

        box.getChildren().addAll(lbl2faTitle, setupTotpBox, lblCodePrompt, txtCode2fa);
        box.setVisible(false);
        box.setManaged(false);
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
        if ("livraison".equals(methode)) {
            methode = livraisonSousGroup.getSelectedToggle().getUserData().toString();
        }

        if ("carte".equals(methode)) {
            String digits = txtNumeroCarte.getText().replaceAll(" ", "");
            if (txtNomCarte.getText().isBlank()) {
                msgLabel.setText("⚠ Veuillez saisir le nom sur la carte."); return;
            }
            if (digits.length() < 13 || digits.length() > 19) {
                msgLabel.setText("⚠ Numéro de carte invalide (13 à 19 chiffres requis)."); return;
            }
        }

        if (enAttente2fa) {
            String code = txtCode2fa != null ? txtCode2fa.getText().trim() : "";
            if (code.length() != 6 || !code.chars().allMatch(Character::isDigit)) {
                msgLabel.setText("⚠ Saisissez le code à 6 chiffres généré par votre application Microsoft Authenticator.");
                return;
            }
        }

        msgLabel.setText("Traitement en cours...");
        msgLabel.setTextFill(Color.web("#D32F2F"));
        final String methodeFinale = methode;
        final String code2fa = enAttente2fa ? txtCode2fa.getText().trim() : null;

        // [Tokenisation] Récupération sous forme de char[] sans espaces, effacement immédiat de l'UI
        final char[] numeroCarteChars = "carte".equals(methodeFinale)
                ? txtNumeroCarte.getText().replaceAll(" ", "").toCharArray()
                : new char[0];
        final String nomCarteStr = txtNomCarte.getText();
        // Effacement immédiat des champs sensibles de l'affichage
        txtNumeroCarte.clear();
        txtNomCarte.clear();

        new Thread(() -> {
            Map<String, Object> rep = controller.confirmerCommandeEtape(
                    methodeFinale, nomCarteStr, numeroCarteChars, code2fa);
            Platform.runLater(() -> {
                if ("REQUIRES_TOTP_SETUP".equals(rep.get("statut"))) {
                    enAttente2fa = true;
                    String otpauthUri = (String) rep.get("otpauthUri");
                    
                    try {
                        String qrUrl = "https://quickchart.io/qr?size=180&text=" + URLEncoder.encode(otpauthUri, StandardCharsets.UTF_8.toString());
                        imgQrCode.setImage(new Image(qrUrl, true));
                    } catch (Exception ex) {
                        System.err.println("[CheckoutView] Échec QR Code : " + ex.getMessage());
                    }

                    setupTotpBox.setVisible(true);
                    setupTotpBox.setManaged(true);
                    box2faSection.setVisible(true);
                    box2faSection.setManaged(true);
                    lbl2faTitle.setText("1. Liaison Microsoft Authenticator");
                    msgLabel.setText("Liaison de sécurité double facteur requise.");
                    msgLabel.setTextFill(Color.web(BRUN_MED));
                    return;
                }
                if ("REQUIRES_PAYMENT_2FA".equals(rep.get("statut"))) {
                    enAttente2fa = true;
                    setupTotpBox.setVisible(false);
                    setupTotpBox.setManaged(false);
                    box2faSection.setVisible(true);
                    box2faSection.setManaged(true);
                    lbl2faTitle.setText("Code Microsoft Authenticator");
                    msgLabel.setText((String) rep.getOrDefault("message", "Code requis."));
                    msgLabel.setTextFill(Color.web(BRUN_MED));
                    return;
                }
                if ("OK".equals(rep.get("statut"))) {
                    CommandeDTO dto = (CommandeDTO) rep.get("commandeResult");
                    if (dto != null) {
                        try {
                            new ConfirmationCommandeView(idUtilisateur, dto).start(stage);
                        } catch (Exception ex) {
                            msgLabel.setText("⚠ Erreur d'affichage.");
                        }
                    }
                    return;
                }
                if ("ERROR".equals(rep.get("statut")) && "INVALID_2FA".equals(rep.get("message"))) {
                    msgLabel.setText("✗ Code invalide ou expiré.");
                    msgLabel.setTextFill(Color.web("#D32F2F"));
                    return;
                }
                msgLabel.setText("⚠ " + rep.getOrDefault("message", "Erreur lors de la confirmation."));
                msgLabel.setTextFill(Color.web("#D32F2F"));
            });
        }).start();
    }

    public static void main(String[] args) { launch(args); }
}
