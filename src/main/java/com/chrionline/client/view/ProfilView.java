package com.chrionline.client.view;

import com.chrionline.client.controller.ProfilController;
import com.chrionline.client.view.utils.HeaderComponent;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Vue Profil Premium : Design Glassmorphism, sections structurées et feedback fluide.
 */
public class ProfilView {

    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private final ProfilController controller;
    private Stage stage;

    // Fields
    private TextField txtNom, txtPrenom, txtEmail, txtTel;
    private TextField txtRue, txtVille, txtCP, txtPays;
    private Label lblInitials;
    private Circle avatarCircle;

    public ProfilView() {
        this.controller = new ProfilController();
    }

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("ChriOnline — Mon Profil");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // 1. Header Global
        root.getChildren().add(HeaderComponent.build(stage, "Profil"));

        // 2. Content Area (Scrollable)
        VBox content = new VBox(40);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(1000);

        // --- SECTION HEADER PROFIL ---
        content.getChildren().add(buildProfileHeader());

        // --- GRID DE FORMULAIRES ---
        GridPane mainGrid = new GridPane();
        mainGrid.setHgap(30);
        mainGrid.setVgap(30);
        mainGrid.setAlignment(Pos.TOP_CENTER);

        // Section : Informations Personnelles
        VBox sectionPerso = buildSection("Informations Personnelles", "👤",
                createField("Nom", txtNom = new TextField()),
                createField("Prénom", txtPrenom = new TextField()),
                createField("Email", txtEmail = new TextField()),
                createField("Téléphone", txtTel = new TextField())
        );
        txtEmail.setEditable(false); // Email non modifiable pour la sécurité
        txtEmail.setOpacity(0.7);

        // Section : Adresse de Livraison
        VBox sectionAdresse = buildSection("Adresse de Livraison", "📍",
                createField("Rue", txtRue = new TextField()),
                createField("Ville", txtVille = new TextField()),
                createField("Code Postal", txtCP = new TextField()),
                createField("Pays", txtPays = new TextField())
        );

        mainGrid.add(sectionPerso, 0, 0);
        mainGrid.add(sectionAdresse, 1, 0);

        content.getChildren().add(mainGrid);

        // --- BOUTON ENREGISTRER ---
        Button btnSave = new Button("Enregistrer les modifications");
        btnSave.setPrefWidth(300);
        btnSave.setCursor(Cursor.HAND);
        btnSave.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        btnSave.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 15;");
        
        btnSave.setOnMouseEntered(e -> {
            btnSave.setStyle("-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 15;");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnSave);
            st.setToX(1.02); st.setToY(1.02); st.play();
        });
        btnSave.setOnMouseExited(e -> {
            btnSave.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 15;");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnSave);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        
        btnSave.setOnAction(e -> handleSave());
        content.getChildren().add(btnSave);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(scroll);

        Scene scene = new Scene(root, 1100, 800);
        stage.setScene(scene);
        stage.show();

        loadData();
    }

    private VBox buildProfileHeader() {
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);

        StackPane avatarStack = new StackPane();
        avatarCircle = new Circle(50, Color.web(TERRACOTTA));
        avatarCircle.setEffect(new DropShadow(15, Color.web("#000000", 0.1)));
        
        lblInitials = new Label("??");
        lblInitials.setFont(Font.font("Georgia", FontWeight.BOLD, 32));
        lblInitials.setTextFill(Color.WHITE);
        
        avatarStack.getChildren().addAll(avatarCircle, lblInitials);

        Text welcome = new Text("Mon Compte");
        welcome.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
        welcome.setFill(Color.web(BRUN));

        Text sub = new Text("Gérez vos informations personnelles et vos préférences");
        sub.setFont(Font.font("Georgia", 14));
        sub.setFill(Color.web(BRUN_LIGHT));

        header.getChildren().addAll(avatarStack, welcome, sub);
        return header;
    }

    private VBox buildSection(String title, String icon, Node... fields) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(25));
        section.setPrefWidth(420);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: " + BORDER + "; -fx-border-radius: 15;");
        section.setEffect(new DropShadow(10, Color.web("#000000", 0.03)));

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Text tIcon = new Text(icon);
        tIcon.setFont(Font.font(18));
        Text tTitle = new Text(title);
        tTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        tTitle.setFill(Color.web(BRUN));
        titleBox.getChildren().addAll(tIcon, tTitle);

        VBox fieldsBox = new VBox(15);
        fieldsBox.getChildren().addAll(fields);

        section.getChildren().addAll(titleBox, new Separator(), fieldsBox);
        return section;
    }

    private VBox createField(String label, TextField tf) {
        VBox box = new VBox(5);
        Label l = new Label(label);
        l.setFont(Font.font("Georgia", 13));
        l.setTextFill(Color.web(BRUN_LIGHT));

        tf.setPrefHeight(40);
        tf.setStyle("-fx-background-color: #F9F7F5; -fx-border-color: " + BORDER + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 0 10; -fx-font-family: 'Georgia';");
        
        tf.focusedProperty().addListener((obs, oldV, newV) -> {
            if (newV) tf.setStyle("-fx-background-color: white; -fx-border-color: " + SAUGE + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 0 10; -fx-font-family: 'Georgia';");
            else tf.setStyle("-fx-background-color: #F9F7F5; -fx-border-color: " + BORDER + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 0 10; -fx-font-family: 'Georgia';");
        });

        box.getChildren().addAll(l, tf);
        return box;
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Map<String, Object> profil = controller.getProfil();
        if ("OK".equals(profil.get("statut"))) {
            Map<String, Object> d = (Map<String, Object>) profil.get("data");
            txtNom.setText((String) d.get("nom"));
            txtPrenom.setText((String) d.get("prenom"));
            txtEmail.setText((String) d.get("email"));
            txtTel.setText((String) d.get("telephone"));
            txtRue.setText((String) d.get("rue"));
            txtVille.setText((String) d.get("ville"));
            txtCP.setText((String) d.get("code_postal"));
            txtPays.setText((String) d.get("pays"));

            // MAJ Initials
            String initials = "";
            if (txtPrenom.getText() != null && !txtPrenom.getText().isEmpty()) initials += txtPrenom.getText().substring(0,1).toUpperCase();
            if (txtNom.getText() != null && !txtNom.getText().isEmpty()) initials += txtNom.getText().substring(0,1).toUpperCase();
            lblInitials.setText(initials.isEmpty() ? "U" : initials);
        }
    }

    private void handleSave() {
        Map<String, Object> data = new HashMap<>();
        data.put("nom", txtNom.getText());
        data.put("prenom", txtPrenom.getText());
        data.put("email", txtEmail.getText());
        data.put("telephone", txtTel.getText());
        data.put("rue", txtRue.getText());
        data.put("ville", txtVille.getText());
        data.put("code_postal", txtCP.getText());
        data.put("pays", txtPays.getText());

        Map<String, Object> res = controller.updateProfil(data);
        if ("OK".equals(res.get("statut"))) {
            showAlert("Profil Mis à Jour", "Vos informations ont été enregistrées avec succès.", Alert.AlertType.INFORMATION);
            loadData(); // Rafraîchir
        } else {
            showAlert("Erreur", (String) res.get("message"), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        DialogPane pane = alert.getDialogPane();
        pane.setStyle("-fx-background-color: " + CREME + "; -fx-font-family: 'Georgia';");
        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) okBtn.setStyle("-fx-background-color: " + BRUN + "; -fx-text-fill: white;");
        
        alert.showAndWait();
    }
}
