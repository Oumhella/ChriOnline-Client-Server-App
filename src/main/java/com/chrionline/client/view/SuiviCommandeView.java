package com.chrionline.client.view;

import com.chrionline.client.network.Client;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.animation.*;
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
import javafx.util.Duration;
import java.util.Map;

public class SuiviCommandeView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String TERRA_HOVER = "#A0522D";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String BORDER      = "#E8E0D5";
    private static final String GREY        = "#F0F0F0";
    private static final String DANGER      = "#D32F2F";

    private VBox trackerContainer;
    private TextField txtReference;
    private Label lblErreur;
    private Stage stage;
    
    private static final String STATUT_PREPARATION = "EN_PREPARATION";
    private static final String STATUT_EXPEDIEE    = "EXPEDIEE";
    private static final String STATUT_LIVREE      = "LIVREE";
    private static final String STATUT_ANNULEE     = "ANNULEE";

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        VBox mainLayout = new VBox(0);
        mainLayout.setStyle("-fx-background-color: " + CREME + ";");

        // Header Centralisé
        mainLayout.getChildren().add(HeaderComponent.build(stage, "Commandes"));

        // Top Section (Custom per view)
        HBox topSection = buildTopSection();

        VBox content = new VBox(35);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20, 60, 50, 60));
        content.setMaxWidth(900);

        VBox searchCard = buildSearchCard();

        lblErreur = new Label("");
        lblErreur.setTextFill(Color.web(DANGER));
        lblErreur.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        lblErreur.setVisible(false);

        trackerContainer = new VBox(30);
        trackerContainer.setAlignment(Pos.CENTER);
        trackerContainer.setPadding(new Insets(40));
        trackerContainer.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 15; -fx-border-color: " + BORDER + "; -fx-border-radius: 15;");
        trackerContainer.setVisible(false);
        trackerContainer.setEffect(new DropShadow(15, Color.web(BRUN, 0.08)));

        content.getChildren().addAll(searchCard, lblErreur, trackerContainer);
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        mainLayout.getChildren().addAll(topSection, scroll);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(mainLayout, 1100, 800));
        } else {
            stage.getScene().setRoot(mainLayout);
        }
        stage.setTitle("ChriOnline - Suivi de ma Commande");
        if (!stage.isShowing()) stage.show();
    }

    private HBox buildTopSection() {
        HBox topSection = new HBox(20);
        topSection.setPadding(new Insets(30, 60, 10, 60));
        topSection.setAlignment(Pos.CENTER_LEFT);
        
        Text titleText = new Text("Suivi de ma commande");
        titleText.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titleText.setFill(Color.web(BRUN));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnRetour = new Button("📂 Retour à mes commandes");
        btnRetour.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 30;");
        btnRetour.setCursor(javafx.scene.Cursor.HAND);
        btnRetour.setOnAction(e -> {
            try { new MesCommandesView().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        topSection.getChildren().addAll(titleText, spacer, btnRetour);
        return topSection;
    }

    private VBox buildSearchCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 12; -fx-border-color: " + BORDER + "; -fx-border-radius: 12;");
        card.setEffect(new DropShadow(5, Color.web(BRUN, 0.04)));

        Text lblDesc = new Text("Renseignez votre numéro de commande pour suivre son trajet");
        lblDesc.setFont(Font.font("Georgia", 16));
        lblDesc.setFill(Color.web(BRUN_LIGHT));

        HBox searchBox = new HBox(15);
        searchBox.setAlignment(Pos.CENTER);
        
        txtReference = new TextField();
        txtReference.setPromptText("Ex : CMD-2026-00042");
        txtReference.setPrefWidth(400);
        txtReference.setStyle("-fx-font-size: 16px; -fx-padding: 14; -fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 40; -fx-background-radius: 40;");
        
        Button btnSuivre = new Button("VOIR LE STATUT");
        btnSuivre.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        btnSuivre.setPadding(new Insets(16, 40, 16, 40));
        btnSuivre.setCursor(javafx.scene.Cursor.HAND);
        btnSuivre.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 40;");
        btnSuivre.setOnAction(e -> handleSuivre());
        btnSuivre.setOnMouseEntered(e -> btnSuivre.setStyle("-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-background-radius: 40;"));
        btnSuivre.setOnMouseExited(e -> btnSuivre.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 40;"));

        searchBox.getChildren().addAll(txtReference, btnSuivre);
        card.getChildren().addAll(lblDesc, searchBox);
        return card;
    }

    private void handleSuivre() {
        String ref = txtReference.getText().trim();
        if (ref.isEmpty()) {
            afficherErreur("⚠ Veuillez saisir une référence.");
            return;
        }
        
        lblErreur.setVisible(false);
        try {
            Client client = Client.getInstance("127.0.0.1", 12345);
            client.envoyerRequete(Map.of("commande", "SUIVRE_COMMANDE", "reference", ref));

            Object rep = client.lireReponse();
            if (rep instanceof Map) {
                Map<?, ?> responseMap = (Map<?, ?>) rep;
                if ("OK".equals(responseMap.get("statut"))) {
                    CommandeDTO dto = (CommandeDTO) responseMap.get("commande");
                    String statut = dto.getStatut() != null ? dto.getStatut() : STATUT_PREPARATION;
                    Platform.runLater(() -> afficherTracker(dto.getReference(), statut));
                } else {
                    String msg = (String) responseMap.get("message");
                    Platform.runLater(() -> afficherErreur(msg != null ? msg : "⚠ Commande introuvable."));
                }
            }
        } catch (Exception e) {
            afficherErreur("⚠ Erreur de connexion au serveur.");
        }
    }

    private void afficherErreur(String msg) {
        lblErreur.setText(msg);
        lblErreur.setVisible(true);
        trackerContainer.setVisible(false);
    }

    private void afficherTracker(String ref, String statut) {
        trackerContainer.getChildren().clear();
        trackerContainer.setVisible(true);

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        Text refTitre = new Text("COLIS #" + ref);
        refTitre.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        refTitre.setFill(Color.web(BRUN));
        
        Text statusLabel = new Text("Progression : " + formatStatutUI(statut));
        statusLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        statusLabel.setFill(STATUT_ANNULEE.equals(statut) ? Color.web(DANGER) : Color.web(SAUGE_DARK));
        header.getChildren().addAll(refTitre, statusLabel);

        int currentStep = STATUT_ANNULEE.equals(statut) ? -1 : 
                         (STATUT_LIVREE.equals(statut) ? 3 : 
                         (STATUT_EXPEDIEE.equals(statut) ? 2 : 1));

        HBox trackerLayout = new HBox(0);
        trackerLayout.setAlignment(Pos.CENTER);
        trackerLayout.setPadding(new Insets(30, 0, 10, 0));

        boolean annulee = STATUT_ANNULEE.equals(statut);
        trackerLayout.getChildren().addAll(
            creerEtape("📦", "En préparation", 1, currentStep, annulee),
            creerLigneProgressive(currentStep >= 2, annulee),
            creerEtape("🚚", "Expédiée", 2, currentStep, annulee),
            creerLigneProgressive(currentStep >= 3, annulee),
            annulee ? creerEtape("✕", "Annulée", 3, -1, true) : creerEtape("✨", "Livrée", 3, currentStep, false)
        );

        trackerContainer.getChildren().addAll(header, trackerLayout);
    }

    private String formatStatutUI(String s) {
        if (STATUT_PREPARATION.equals(s)) return "En préparation";
        if (STATUT_EXPEDIEE.equals(s)) return "Envoyée / Transport";
        if (STATUT_LIVREE.equals(s)) return "Arrivée à destination";
        if (STATUT_ANNULEE.equals(s)) return "Annulée";
        return s;
    }

    private VBox creerEtape(String iconeText, String labelText, int stepIndex, int currentStep, boolean isError) {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefWidth(140);

        boolean reached = !isError && (stepIndex <= currentStep);
        boolean isCurrent = !isError && (stepIndex == currentStep);
        if (isError && stepIndex == 3) { reached = true; isCurrent = true; }

        String bgCouleur = GREY;
        if (isError && stepIndex == 3) bgCouleur = DANGER;
        else if (reached || (currentStep > stepIndex)) bgCouleur = SAUGE_DARK;

        Circle circle = new Circle(30, Color.web(bgCouleur));
        circle.setStroke(Color.WHITE); circle.setStrokeWidth(3);
        Text icon = new Text(iconeText);
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        icon.setFill(reached || (isError && stepIndex == 3) ? Color.WHITE : Color.web(BRUN_LIGHT));

        StackPane circlePane = new StackPane(circle, icon);
        if (isCurrent) {
            ScaleTransition st = new ScaleTransition(Duration.millis(800), circlePane);
            st.setToX(1.1); st.setToY(1.1); st.setAutoReverse(true); st.setCycleCount(Animation.INDEFINITE); st.play();
        }

        Text label = new Text(labelText);
        label.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        label.setFill(reached ? Color.web(BRUN) : Color.web(BRUN_LIGHT));

        vbox.getChildren().addAll(circlePane, label);
        return vbox;
    }

    private StackPane creerLigneProgressive(boolean reached, boolean annulee) {
        StackPane pane = new StackPane();
        pane.setPrefWidth(120); pane.setAlignment(Pos.CENTER);
        Rectangle bgLine = new Rectangle(120, 6, Color.web(GREY));
        bgLine.setArcWidth(6); bgLine.setArcHeight(6);
        pane.getChildren().add(bgLine);

        if (reached) {
            Rectangle coloredLine = new Rectangle(0, 6, Color.web(SAUGE_DARK));
            coloredLine.setArcWidth(6); coloredLine.setArcHeight(6);
            pane.getChildren().add(coloredLine);
            Timeline anim = new Timeline(new KeyFrame(Duration.millis(1000), new KeyValue(coloredLine.widthProperty(), 120)));
            anim.setDelay(Duration.millis(300)); anim.play();
        } else if (annulee) {
             Rectangle errorLine = new Rectangle(120, 6, Color.web(DANGER, 0.2));
             errorLine.setArcWidth(6); errorLine.setArcHeight(6);
             pane.getChildren().add(errorLine);
        }
        pane.setTranslateY(-19);
        return pane;
    }

    public static void main(String[] args) { launch(args); }
}
