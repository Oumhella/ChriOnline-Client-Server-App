package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminLabelsController;
import com.chrionline.admin.controller.AdminCategoriesController;
import com.chrionline.shared.models.Categorie;
import com.chrionline.shared.models.Categorie;
import com.chrionline.shared.models.LabelValue;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.util.List;

public class AdminLabelsView {

    private static final String CREME       = "#FDFBF7";
    private static final String BRUN        = "#3E2C1E";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String DANGER      = "#B03A2E";

    private AdminLabelsController controller;
    private AdminCategoriesController catController;
    
    private ComboBox<Categorie> catSelector;
    private ListView<com.chrionline.shared.models.Label> listLabels;
    private TableView<LabelValue> tableValues;

    public Node getView() {
        this.controller = new AdminLabelsController();
        this.catController = new AdminCategoriesController();

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("creme");

        // Header
        HBox header = new HBox(20);
        VBox titleBox = new VBox(5);
        Text title = new Text("Gestion des Variantes");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.web(BRUN));
        Text sub = new Text("Gérez les types de variantes (Taille, Couleur) par catégorie.");
        sub.setFill(Color.web("#9A7B65"));
        titleBox.getChildren().addAll(title, sub);
        header.getChildren().add(titleBox);

        // Category Selector
        catSelector = new ComboBox<>();
        catSelector.setPromptText("Filtrer par catégorie...");
        catSelector.setItems(FXCollections.observableArrayList(catController.getAllCategories()));
        catSelector.setOnAction(e -> chargerLabels());

        // Main Content Area (SplitPane)
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(split, Priority.ALWAYS);

        // LEFT: Labels List
        VBox leftBox = new VBox(10);
        leftBox.getStyleClass().add("card");
        leftBox.setPadding(new Insets(15));
        Text labelTitle = new Text("Types de Variantes");
        labelTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        listLabels = new ListView<>();
        listLabels.setPrefWidth(200);
        listLabels.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> chargerValues(newV));
        
        Button btnAddLabel = new Button("+ Nouveau Type");
        btnAddLabel.getStyleClass().add("btn-secondary");
        btnAddLabel.setMaxWidth(Double.MAX_VALUE);
        btnAddLabel.setOnAction(e -> ouvrirPopupLabel());
        
        leftBox.getChildren().addAll(labelTitle, listLabels, btnAddLabel);

        // RIGHT: Values Table
        VBox rightBox = new VBox(10);
        rightBox.getStyleClass().add("card");
        rightBox.setPadding(new Insets(15));
        Text valuesTitle = new Text("Valeurs Disponibles");
        valuesTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        tableValues = new TableView<>();
        tableValues.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<LabelValue, String> colVal = new TableColumn<>("Valeur");
        colVal.setCellValueFactory(new PropertyValueFactory<>("valeur"));
        
        TableColumn<LabelValue, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDel = new Button("❌");
            {
                btnDel.setStyle("-fx-text-fill: " + DANGER + "; -fx-background-color: transparent; -fx-cursor: hand;");
                btnDel.setOnAction(e -> supprimerValue(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDel);
                setAlignment(Pos.CENTER);
            }
        });
        
        tableValues.getColumns().addAll(colVal, colActions);
        
        HBox valAddBox = new HBox(10);
        TextField newValField = new TextField();
        newValField.setPromptText("Ex: XL, Rouge, 42...");
        HBox.setHgrow(newValField, Priority.ALWAYS);
        Button btnAddValue = new Button("Ajouter");
        btnAddValue.getStyleClass().add("btn-primary");
        btnAddValue.setOnAction(e -> ajouterValue(newValField));
        valAddBox.getChildren().addAll(newValField, btnAddValue);
        
        rightBox.getChildren().addAll(valuesTitle, tableValues, valAddBox);

        split.getItems().addAll(leftBox, rightBox);
        split.setDividerPositions(0.35);

        root.getChildren().addAll(header, catSelector, new Separator(), split);
        return root;
    }

    private void chargerLabels() {
        Categorie selected = catSelector.getValue();
        if (selected == null) return;
        new Thread(() -> {
            List<com.chrionline.shared.models.Label> list = controller.getLabelsByCategorie(selected.getId());
            Platform.runLater(() -> listLabels.setItems(FXCollections.observableArrayList(list)));
        }).start();
    }

    private void chargerValues(com.chrionline.shared.models.Label l) {
        if (l == null) {
            tableValues.setItems(FXCollections.observableArrayList());
            return;
        }
        new Thread(() -> {
            List<LabelValue> list = controller.getLabelValues(l.getId());
            Platform.runLater(() -> tableValues.setItems(FXCollections.observableArrayList(list)));
        }).start();
    }

    private void ouvrirPopupLabel() {
        if (catSelector.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez d'abord sélectionner une catégorie.").show();
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouveau Type de Variante");
        dialog.setHeaderText("Créer un type pour : " + catSelector.getValue().getNom());
        dialog.setContentText("Nom (ex: Taille, Couleur, Matière) :");
        dialog.showAndWait().ifPresent(name -> {
            com.chrionline.shared.models.Label l = new com.chrionline.shared.models.Label();
            l.setNom(name);
            l.setIdCategorie(catSelector.getValue().getId());
            controller.ajouterLabel(l);
            chargerLabels();
        });
    }

    private void ajouterValue(TextField field) {
        com.chrionline.shared.models.Label selected = listLabels.getSelectionModel().getSelectedItem();
        if (selected == null || field.getText().isEmpty()) return;
        
        LabelValue lv = new LabelValue();
        lv.setValeur(field.getText());
        lv.setLabel(selected);
        controller.ajouterLabelValue(lv);
        field.clear();
        chargerValues(selected);
    }

    private void supprimerValue(LabelValue lv) {
        if (controller.supprimerLabelValue(lv.getId())) {
            chargerValues(listLabels.getSelectionModel().getSelectedItem());
        }
    }
}
