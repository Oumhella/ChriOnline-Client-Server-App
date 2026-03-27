package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminNewsletterController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.scene.web.WebView;
import java.util.Map;

/**
 * Vue premium pour la gestion de la Newsletter.
 */
public class AdminNewsletterView {

    private static final String CREME       = "#FDFBF7";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BORDER      = "#E8E0D5";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";

    private AdminNewsletterController controller;
    private TextField txtSujet;
    private TextArea txtCorps;
    private WebView preview;
    private Label lblStatus;
    private Button btnEnvoyer;

    public Node getView() {
        this.controller = new AdminNewsletterController();

        VBox root = new VBox(24);
        root.setPadding(new Insets(30, 40, 40, 40));
        root.setStyle("-fx-background-color: " + CREME + ";");

        // --- Header ---
        VBox header = new VBox(4);
        Text title = new Text("Diffusion Newsletter");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        title.setFill(Color.web(BRUN));
        Text sub = new Text("Envoyez des actualités par Email et notifications UDP en temps réel.");
        sub.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sub.setFill(Color.web(BRUN_MED));
        header.getChildren().addAll(title, sub);

        // --- Content ---
        HBox mainContent = new HBox(30);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // LEFT: Form
        VBox form = new VBox(15);
        form.setPrefWidth(450);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-border-color: " + BORDER + "; -fx-border-radius: 12;");
        form.setEffect(new DropShadow(10, Color.web(BRUN, 0.05)));

        Label lbl1 = new Label("Sujet du message *");
        lbl1.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        txtSujet = new TextField();
        txtSujet.setPromptText("Ex: Nouvelle collection de printemps !");
        txtSujet.getStyleClass().add("text-field");

        Label lbl2 = new Label("Contenu (HTML supporté) *");
        lbl2.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        txtCorps = new TextArea();
        txtCorps.setPromptText("Écrivez votre message ici...");
        txtCorps.setPrefRowCount(12);
        txtCorps.setWrapText(true);
        VBox.setVgrow(txtCorps, Priority.ALWAYS);

        txtCorps.textProperty().addListener((obs, oldV, newV) -> updatePreview(newV));

        lblStatus = new Label("");
        lblStatus.setFont(Font.font("Georgia", FontPosture.ITALIC, 12));

        btnEnvoyer = new Button("🚀 Diffuser la Newsletter");
        btnEnvoyer.getStyleClass().add("btn-primary");
        btnEnvoyer.setPrefWidth(Double.MAX_VALUE);
        btnEnvoyer.setPadding(new Insets(12));
        btnEnvoyer.setOnAction(e -> envoyer());

        form.getChildren().addAll(lbl1, txtSujet, lbl2, txtCorps, lblStatus, btnEnvoyer);

        // RIGHT: Live Preview
        VBox previewBox = new VBox(10);
        HBox.setHgrow(previewBox, Priority.ALWAYS);
        
        Label lblPreview = new Label("Aperçu du rendu Email");
        lblPreview.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        lblPreview.setTextFill(Color.web(BRUN_MED));

        preview = new WebView();
        preview.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + ";");
        VBox.setVgrow(preview, Priority.ALWAYS);
        
        previewBox.getChildren().addAll(lblPreview, preview);

        mainContent.getChildren().addAll(form, previewBox);
        root.getChildren().addAll(header, new Separator(), mainContent);

        return root;
    }

    private void updatePreview(String content) {
        String html = "<html><body style='font-family: sans-serif; padding: 20px; color: #333;'>"
                    + content
                    + "</body></html>";
        preview.getEngine().loadContent(html);
    }

    private void envoyer() {
        String sujet = txtSujet.getText().trim();
        String corps = txtCorps.getText().trim();

        if (sujet.isEmpty() || corps.isEmpty()) {
            lblStatus.setText("⚠ Veuillez remplir tous les champs.");
            lblStatus.setTextFill(Color.web(TERRACOTTA));
            return;
        }

        btnEnvoyer.setDisable(true);
        btnEnvoyer.setText("Envoi en cours...");
        lblStatus.setText("Communication avec le serveur...");
        lblStatus.setTextFill(Color.web(BRUN_MED));

        new Thread(() -> {
            Map<String, Object> res = controller.envoyerNewsletter(sujet, corps);
            Platform.runLater(() -> {
                if ("OK".equals(res.get("statut"))) {
                    lblStatus.setText("✅ Succès ! Envoyé à " + res.getOrDefault("count", "?") + " utilisateurs.");
                    lblStatus.setTextFill(Color.web(SAUGE_DARK));
                    txtSujet.clear();
                    txtCorps.clear();
                } else {
                    lblStatus.setText("❌ Erreur : " + res.get("message"));
                    lblStatus.setTextFill(Color.web(TERRACOTTA));
                }
                btnEnvoyer.setDisable(false);
                btnEnvoyer.setText("🚀 Diffuser la Newsletter");
            });
        }).start();
    }
}
