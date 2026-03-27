package com.chrionline.client.view;

import com.chrionline.client.controller.ProfilController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Map;

public class ProfilView {

    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private ProfilController controller;
    private TextField txtNom, txtPrenom, txtEmail, txtTel, txtRue, txtVille, txtCP, txtPays;

    public ProfilView() {
        this.controller = new ProfilController();
    }

    public void start(Stage stage) {
        stage.setTitle("ChriOnline — Mon Profil");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // Header
        HBox header = new HBox(40);
        header.setPadding(new Insets(20, 48, 20, 48));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> new HomeView().start(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(28);
        nav.setAlignment(Pos.CENTER);

        Hyperlink hAcc = createNavLink("Accueil", stage);
        hAcc.setOnAction(e -> new HomeView().start(stage));

        Hyperlink hCat = createNavLink("Catalogue", stage);
        hCat.setOnAction(e -> new CatalogueView().start(stage));

        String prenom = com.chrionline.client.session.SessionManager.getInstance().getPrenom();
        String nom = com.chrionline.client.session.SessionManager.getInstance().getNom();
        String initials = "";
        if (prenom != null && !prenom.isEmpty()) initials += prenom.toUpperCase().charAt(0);
        if (nom != null && !nom.isEmpty()) initials += nom.toUpperCase().charAt(0);
        if (initials.isEmpty()) initials = "U";
        
        StackPane avatar = new StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(15, Color.web(SAUGE_DARK));
        Text initText = new Text(initials);
        initText.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        initText.setFill(Color.WHITE);
        avatar.getChildren().addAll(circle, initText);
        avatar.setCursor(javafx.scene.Cursor.HAND);

        Hyperlink hOrd = createNavLink("Mes Commandes", stage);
        hOrd.setOnAction(e -> new MesCommandesView().start(stage));

        Hyperlink hPan = createNavLink("Mon Panier", stage);
        hPan.setOnAction(e -> {
            try { new PanierView(5).start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        nav.getChildren().addAll(hAcc, hCat, hOrd, hPan, avatar);

        header.getChildren().addAll(logo, spacer, nav);

        // Formulaire
        VBox form = new VBox(25);
        form.setPadding(new Insets(40, 100, 40, 100));
        form.setMaxWidth(800);
        form.setAlignment(Pos.TOP_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(20);

        txtNom = createField(grid, "Nom :", 0, 0);
        txtPrenom = createField(grid, "Prénom :", 1, 0);
        txtEmail = createField(grid, "Email :", 2, 0);
        txtTel = createField(grid, "Téléphone :", 3, 0);
        
        txtRue = createField(grid, "Rue :", 0, 1);
        txtVille = createField(grid, "Ville :", 1, 1);
        txtCP = createField(grid, "Code Postal :", 2, 1);
        txtPays = createField(grid, "Pays :", 3, 1);

        Button btnSave = new Button("Enregistrer les modifications");
        btnSave.setStyle("-fx-background-color: " + SAUGE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 5;");
        btnSave.setCursor(javafx.scene.Cursor.HAND);
        btnSave.setOnAction(e -> handleSave());

        form.getChildren().addAll(grid, btnSave);

        root.getChildren().addAll(header, form);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 1100, 800));
        } else {
            stage.getScene().setRoot(root);
            stage.getScene().getStylesheets().clear();
        }
        if (!stage.isShowing()) stage.show();

        loadData();
    }

    private TextField createField(GridPane grid, String label, int row, int col) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: 'Georgia'; -fx-text-fill: " + BRUN + "; -fx-font-weight: bold;");
        TextField tf = new TextField();
        tf.setPrefWidth(250);
        tf.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 3; -fx-padding: 8;");
        box.getChildren().addAll(lbl, tf);
        grid.add(box, col, row);
        return tf;
    }

    private void loadData() {
        Map<String, Object> profil = controller.getProfil();
        if ("OK".equals(profil.get("statut"))) {
            Map<String, Object> d = (Map<String, Object>) profil.get("data");
            txtNom.setText((String) d.getOrDefault("nom", ""));
            txtPrenom.setText((String) d.getOrDefault("prenom", ""));
            txtEmail.setText((String) d.getOrDefault("email", ""));
            txtTel.setText((String) d.getOrDefault("telephone", ""));
            txtRue.setText((String) d.getOrDefault("rue", ""));
            txtVille.setText((String) d.getOrDefault("ville", ""));
            txtCP.setText((String) d.getOrDefault("code_postal", ""));
            txtPays.setText((String) d.getOrDefault("pays", ""));
        } else {
            showAlert("Erreur", (String) profil.get("message"), Alert.AlertType.ERROR);
        }
    }

    private void handleSave() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
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
            showAlert("Succès", (String) res.get("message"), Alert.AlertType.INFORMATION);
        } else {
            showAlert("Erreur", (String) res.get("message"), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Hyperlink createNavLink(String text, Stage stage) {
        Hyperlink l = new Hyperlink(text);
        l.setFont(Font.font("Georgia", 14));
        l.setTextFill(Color.web(BRUN));
        l.setStyle("-fx-border-color: transparent;");
        return l;
    }
}
