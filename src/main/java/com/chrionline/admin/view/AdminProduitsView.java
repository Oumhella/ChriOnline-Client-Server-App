package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminProduitsController;
import com.chrionline.shared.models.Categorie;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;
import java.io.InputStream;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;

public class AdminProduitsView extends Application {

    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String BORDER      = "#E8E0D5";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String DANGER      = "#B03A2E";

    /*
     * - [x] Redesign UI (Cards/Bloques)
     *   - [x] Remplacement de la TableView par un TilePane.
     *   - [x] Design de cartes produits premium avec images et badges de prix.
     *   - [x] Mise à jour de la recherche et du chargement pour les cartes.
     * - [x] Correction Finale Modification Produit
     *   - [x] Implémentation de GET_PRODUIT_BY_ID sur le serveur (manquait dans le dispatcher).
     *   - [x] Initialisation systématique de `labelValues` dans `ProductFormat`.
     *   - [x] Ajout de sécurités null-check dans `AdminProduitsView`.
     *   - [x] Fix dans ProduitDAO pour gérer les retours MySQL et l'aliasing de colonnes.
     */
    private AdminProduitsController controller;
    private TilePane cardsPane;
    private ObservableList<Produit> produitsData;
    private List<Categorie> categoriesCache = new ArrayList<>();

    /**
     * Retourne la vue pour intégration dans le Dashboard.
     */
    public Node getView() {
        this.controller = new AdminProduitsController();
        
        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 34, 34, 34));

        content.getChildren().addAll(buildHeader(), buildCardsContainer());
        chargerDonnees();

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        
        return scroll;
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setCenter(getView());
        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.setTitle("ChriOnline — Gestion Catalogue");
        stage.show();
    }

    private HBox buildHeader() {
        VBox tb = new VBox(3);
        Text titre = new Text("Gestion des Produits");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Consultez et gérez votre catalogue de produits et variantes.");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sousTitre.setFill(Color.web(BRUN_LIGHT));
        tb.getChildren().addAll(titre, sousTitre);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher un produit...");
        searchField.getStyleClass().add("text-field");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterProduits(newV));

        Button btnAdd = new Button("+ Nouveau Produit");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> ouvrirPopupProduit(null));

        HBox header = new HBox(15, tb, spacer, searchField, btnAdd);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private ScrollPane buildCardsContainer() {
        cardsPane = new TilePane();
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        cardsPane.setPadding(new Insets(10));
        cardsPane.setPrefColumns(3); // Will wrap naturally
        cardsPane.setAlignment(Pos.TOP_LEFT);
        cardsPane.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(cardsPane);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private Node buildProductCard(Produit p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(260);
        card.setMinWidth(260);
        card.setMaxWidth(260);
        card.setAlignment(Pos.TOP_CENTER);
        card.setCursor(Cursor.HAND);

        // --- Image ---
        String url = p.getImageUrl();
        ImageView iv = new ImageView();
        iv.setFitWidth(230);
        iv.setFitHeight(160);
        iv.setPreserveRatio(true);
        
        StackPane imgFrame = new StackPane(iv);
        imgFrame.setPrefHeight(160);
        imgFrame.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8;");

        try {
            if (url != null && !url.isEmpty()) {
                if (url.startsWith("http")) iv.setImage(new Image(url, true));
                else {
                    File imgFile = new File(url);
                    if (imgFile.exists()) iv.setImage(new Image(imgFile.toURI().toString()));
                    else throw new Exception("File not found");
                }
            } else throw new Exception("Empty URL");
        } catch (Exception e) {
            // Placeholder si image absente
            iv.setVisible(false);
            iv.setManaged(false);
            Text icon = new Text("📷");
            icon.setFont(Font.font(40));
            icon.setFill(Color.web(BRUN_LIGHT, 0.4));
            imgFrame.getChildren().add(icon);
        }
        
        // --- Badge Prix ---
        String prixRange = formatPrixRange(p);
        Label badgePrix = new Label(prixRange);
        badgePrix.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 3 8; -fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane.setAlignment(badgePrix, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(badgePrix, new Insets(8));
        imgFrame.getChildren().add(badgePrix);

        // --- Info ---
        VBox info = new VBox(4);
        Text name = new Text(p.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setFill(Color.web(BRUN));
        name.setWrappingWidth(230);

        String catName = categoriesCache.stream().filter(c -> c.getId() == p.getIdCategorie()).map(Categorie::getNom).findFirst().orElse("Catégorie #" + p.getIdCategorie());
        Label catLabel = new Label(catName);
        catLabel.setStyle("-fx-text-fill: " + BRUN_LIGHT + "; -fx-font-style: italic; -fx-font-size: 12px;");

        Text desc = new Text(p.getDescription());
        desc.setFont(Font.font(12));
        desc.setFill(Color.web(BRUN_MED));
        desc.setWrappingWidth(230);
        desc.setOpacity(0.7);

        info.getChildren().addAll(name, catLabel, desc);

        // --- Stats ---
        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);
        int totalStock = p.getFormats().stream().mapToInt(ProductFormat::getStock).sum();
        Label lblStock = new Label("📦 " + totalStock + " unités");
        lblStock.setFont(Font.font(11));
        lblStock.setTextFill(Color.web(totalStock < 10 ? TERRACOTTA : BRUN_LIGHT));
        
        Label lblVariants = new Label("✨ " + p.getFormats().size() + " types");
        lblVariants.setFont(Font.font(11));
        lblVariants.setTextFill(Color.web(BRUN_LIGHT));
        
        stats.getChildren().addAll(lblStock, lblVariants);

        // --- Actions ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("✏️ Modifier");
        btnEdit.getStyleClass().add("btn-secondary");
        btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        btnEdit.setOnAction(e -> {
            new Thread(() -> {
                System.out.println("Client: Loading product ID=" + p.getIdProduit());
                Produit complet = controller.getProduitById(p.getIdProduit());
                if (complet == null) {
                    System.err.println("Client Error: getProduitById returned null for ID=" + p.getIdProduit());
                    complet = p; // Fallback to basic info from card
                }
                
                final Produit toEdit = complet;
                Platform.runLater(() -> ouvrirPopupProduit(toEdit));
            }).start();
        });

        Button btnDel = new Button("❌");
        btnDel.getStyleClass().add("btn-danger");
        btnDel.setStyle("-fx-padding: 5 10;");
        btnDel.setOnAction(e -> supprimerProduit(p));

        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(imgFrame, info, stats, new Separator(), actions);
        
        // Hover effects
        card.setOnMouseEntered(e -> {
            card.setEffect(new DropShadow(15, Color.web(BRUN, 0.15)));
            card.setScaleX(1.02); card.setScaleY(1.02);
        });
        card.setOnMouseExited(e -> {
            card.setEffect(null);
            card.setScaleX(1); card.setScaleY(1);
        });

        return card;
    }

    private String formatPrixRange(Produit p) {
        if (p.getFormats().isEmpty()) return "—";
        double min = p.getFormats().stream().mapToDouble(ProductFormat::getPrix).min().orElse(0);
        double max = p.getFormats().stream().mapToDouble(ProductFormat::getPrix).max().orElse(0);
        if (min == max) return String.format("%.0f MAD", min);
        return String.format("%.0f-%.0f MAD", min, max);
    }

    private void chargerDonnees() {
        new Thread(() -> {
            List<Categorie> cats = controller.getCategories();
            List<Produit> list = controller.getAllProduits();
            Platform.runLater(() -> {
                categoriesCache = cats;
                produitsData = FXCollections.observableArrayList(list);
                renderCards(produitsData);
            });
        }).start();
    }

    private void renderCards(List<Produit> list) {
        cardsPane.getChildren().clear();
        for (Produit p : list) {
            cardsPane.getChildren().add(buildProductCard(p));
        }
    }

    private void supprimerProduit(Produit p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer \u00ab " + p.getNom() + " \u00bb ?");
        alert.setContentText("Tous ses formats et variantes seront supprim\u00e9s. Cette action est irr\u00e9versible.");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                if (controller.supprimerProduit(p.getIdProduit())) chargerDonnees();
                else showSimpleAlert("Erreur", "Impossible de supprimer ce produit.");
            }
        });
    }

    private void ouvrirPopupProduit(Produit p) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(p == null ? "Nouveau Produit" : "Édition — " + p.getNom());

        // ── Root ──
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // ── Dark header bar ──
        HBox popHeader = new HBox();
        popHeader.setPadding(new Insets(20, 28, 16, 28));
        popHeader.setAlignment(Pos.CENTER_LEFT);
        popHeader.setStyle("-fx-background-color: " + BRUN + ";");
        Text heading = new Text(p == null ? "✨ Nouveau Produit" : "✏️  Édition du Produit");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 17));
        heading.setFill(Color.web(CREME));
        popHeader.getChildren().add(heading);

        // ── Body ──
        VBox body = new VBox(18);
        body.setPadding(new Insets(22, 28, 28, 28));

        // § Infos générales
        VBox infoSection = buildPopupSection("Informations Générales");

        TextField nomField = new TextField(p != null ? p.getNom() : "");
        nomField.setPromptText("Nom du produit (ex: Selle de Dressage)");
        styleField(nomField);

        TextArea descArea = new TextArea(p != null ? p.getDescription() : "");
        descArea.setPromptText("Description du produit...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; " +
                          "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10;");

        ComboBox<Categorie> catCombo = new ComboBox<>();
        catCombo.setPromptText("Choisir une catégorie *");
        catCombo.setMaxWidth(Double.MAX_VALUE);
        catCombo.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-border-color: " + BORDER + ";");
        List<Categorie> cats = categoriesCache.isEmpty() ? controller.getCategories() : categoriesCache;
        catCombo.setItems(FXCollections.observableArrayList(cats));
        catCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        catCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        if (p != null) {
            cats.stream().filter(c -> c.getId() == p.getIdCategorie()).findFirst().ifPresent(catCombo::setValue);
        }

        infoSection.getChildren().addAll(
            buildFieldLabel("Nom *"), nomField,
            buildFieldLabel("Description"), descArea,
            buildFieldLabel("Catégorie *"), catCombo
        );

        // § Formats & Tarifs
        VBox formatsSection = buildPopupSection("📊 Formats et Tarifs");

        Label fmtHint = new Label("Chaque format = un tarif/stock unique (ex: Taille S, Bleu, etc.).");
        fmtHint.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
        fmtHint.setTextFill(Color.web(BRUN_LIGHT));

        ObservableList<ProductFormat> formatsData = FXCollections.observableArrayList(
            p != null && p.getFormats() != null ? p.getFormats() : new ArrayList<>()
        );

        TableView<ProductFormat> tableFormats = new TableView<>(formatsData);
        tableFormats.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableFormats.setPrefHeight(160);
        tableFormats.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 8; " +
                              "-fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        TableColumn<ProductFormat, String> colFmtPrix = new TableColumn<>("Prix");
        colFmtPrix.setCellValueFactory(cell -> new SimpleStringProperty(
            String.format("%.2f MAD", cell.getValue().getPrix() != null ? cell.getValue().getPrix() : 0)));

        TableColumn<ProductFormat, String> colFmtStock = new TableColumn<>("Stock");
        colFmtStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                int s = ((ProductFormat) getTableRow().getItem()).getStock();
                Label badge = new Label(String.valueOf(s));
                badge.setFont(Font.font(String.valueOf(FontWeight.BOLD), 11));
                badge.setStyle("-fx-background-radius: 5; -fx-padding: 2 8; -fx-text-fill: white; -fx-background-color: " +
                    (s == 0 ? DANGER : s < 5 ? TERRACOTTA : SAUGE_DARK) + ";");
                setGraphic(badge);
            }
        });

        TableColumn<ProductFormat, String> colFmtVars = new TableColumn<>("Variantes");
        colFmtVars.setCellValueFactory(cell -> {
            List<LabelValue> lvs = cell.getValue().getLabelValues();
            String vars = (lvs == null || lvs.isEmpty()) ? ""
                : lvs.stream().map(LabelValue::getValeur).collect(Collectors.joining(", "));
            return new SimpleStringProperty(vars.isEmpty() ? "Standard" : vars);
        });

        TableColumn<ProductFormat, Void> colFmtActions = new TableColumn<>("Actions");
        colFmtActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDel  = new Button("🗑");
            private final HBox pane = new HBox(8, btnEdit, btnDel);
            {
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13;");
                btnDel.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " + DANGER + "; -fx-font-size: 13;");
                pane.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    if (catCombo.getValue() != null) {
                        ouvrirPopupFormat(getTableView().getItems().get(getIndex()), formatsData, catCombo.getValue().getId());
                    } else {
                        showSimpleAlert("Erreur", "Choisissez d'abord une catégorie.");
                    }
                });
                btnDel.setOnAction(e -> formatsData.remove(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableFormats.getColumns().addAll(colFmtPrix, colFmtStock, colFmtVars, colFmtActions);

        Button btnAddFmt = new Button("＋  Ajouter un Format");
        btnAddFmt.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SAUGE_DARK + "; " +
                           "-fx-border-color: " + SAUGE_DARK + "; -fx-border-radius: 7; -fx-border-width: 1.2; " +
                           "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-size: 11; -fx-font-weight: bold;");
        btnAddFmt.setOnAction(e -> {
            if (catCombo.getValue() == null) {
                showSimpleAlert("Catégorie manquante", "Choisissez d'abord la catégorie du produit.");
            } else {
                ouvrirPopupFormat(null, formatsData, catCombo.getValue().getId());
            }
        });

        formatsSection.getChildren().addAll(fmtHint, tableFormats, btnAddFmt);

        // ── Error label + Buttons ──
        Label errLabel = new Label("");
        errLabel.setTextFill(Color.web(DANGER));
        errLabel.setFont(Font.font(11));
        errLabel.setWrapText(true);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN_MED + "; " +
                           "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10 20;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button(p == null ? "💾 Créer le Produit" : "💾 Enregistrer");
        btnSave.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; " +
                         "-fx-background-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> {
            if (nomField.getText().trim().isEmpty() || catCombo.getValue() == null) {
                errLabel.setText("⚠  Le nom et la catégorie sont obligatoires.");
                return;
            }
            if (formatsData.isEmpty()) {
                errLabel.setText("⚠  Ajoutez au moins un format de tarification.");
                return;
            }
            Produit toSave = p != null ? p : new Produit();
            toSave.setNom(nomField.getText().trim());
            toSave.setDescription(descArea.getText().trim());
            toSave.setIdCategorie(catCombo.getValue().getId());
            toSave.setFormats(new ArrayList<>(formatsData));

            String origText = btnSave.getText();
            btnSave.setText("Enregistrement...");
            btnSave.setDisable(true);

            int result = (p == null) ? controller.ajouterProduit(toSave) : (controller.modifierProduit(toSave) ? 1 : -1);

            if (result > 0) {
                chargerDonnees();
                modal.close();
            } else {
                btnSave.setText(origText);
                btnSave.setDisable(false);
                errLabel.setText("❌ Échec. Vérifiez la console serveur pour les détails.");
            }
        });

        btnRow.getChildren().addAll(btnCancel, btnSave);

        body.getChildren().addAll(infoSection, formatsSection, errLabel, btnRow);
        root.getChildren().addAll(popHeader, body);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: " + CREME + "; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll, 520, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/admin.css").toExternalForm());
        modal.setScene(scene);
        modal.show();
    }

    // ─── Popup Helpers ────────────────────────────────────────────────────────

    private VBox buildPopupSection(String title) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 10; " +
                         "-fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(BRUN));
        Separator sep = new Separator();
        section.getChildren().addAll(lbl, sep);
        return section;
    }

    private Label buildFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(BRUN_MED));
        return lbl;
    }

    private void styleField(TextField field) {
        field.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; " +
                       "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10;");
    }

    private void ouvrirPopupFormat(ProductFormat f, ObservableList<ProductFormat> parentList, int idCategorie) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(f == null ? "Nouveau Format" : "Édition Format");

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.getStyleClass().add("creme");
        
        TextField prixField = new TextField(f != null ? f.getPrix().toString() : ""); 
        prixField.setPromptText("Prix (ex: 150.00)");
        
        TextField stockField = new TextField(f != null ? String.valueOf(f.getStock()) : ""); 
        stockField.setPromptText("Stock initial");
        
        TextField stockAlerteField = new TextField(f != null ? String.valueOf(f.getStockAlerte()) : "5"); 
        stockAlerteField.setPromptText("Seuil d'alerte");

        // Image Selection
        HBox imgBox = new HBox(10);
        TextField imgUrlField = new TextField(f != null ? f.getImageUrl() : "");
        imgUrlField.setPromptText("URL ou chemin de l'image");
        HBox.setHgrow(imgUrlField, Priority.ALWAYS);
        Button btnBrowse = new Button("📁 Parcourir...");
        btnBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
                    String serverPath = controller.uploadImage(bytes, ext);
                    if (serverPath != null) {
                        imgUrlField.setText(serverPath);
                        showSimpleAlert("Succès", "Image envoyée au serveur !");
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        imgBox.getChildren().addAll(imgUrlField, btnBrowse);

        // Variantes (Labels & Values)
        VBox varBox = new VBox(10);
        HBox varHeader = new HBox(10);
        Text varTitle = new Text("Variantes (Taille, Couleur, etc.)");
        varTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        
        Button btnAddLabel = new Button("+ Nouveau Label");
        btnAddLabel.getStyleClass().add("btn-secondary");
        btnAddLabel.setStyle("-fx-font-size: 10px; -fx-padding: 2 5;");
        
        varHeader.getChildren().addAll(varTitle, btnAddLabel);
        
        List<LabelValue> selectedValues = new ArrayList<>();
        if (f != null) selectedValues.addAll(f.getLabelValues());

        GridPane gridVars = new GridPane();
        gridVars.setHgap(10); gridVars.setVgap(10);
        
        // Refreshable building logic
        Runnable refreshGrid = () -> buildVariantGrid(gridVars, idCategorie, f, selectedValues);
        refreshGrid.run();

        btnAddLabel.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Nouveau Label");
            dialog.setHeaderText("Ajouter une caractéristique pour cette catégorie");
            dialog.setContentText("Nom (ex: Matière) :");
            dialog.showAndWait().ifPresent(nom -> {
                if (!nom.trim().isEmpty()) {
                    controller.ajouterLabel(idCategorie, nom.trim());
                    refreshGrid.run();
                }
            });
        });
        
        Button btnOk = new Button("Valider ce format");
        btnOk.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white;");
        btnOk.setOnAction(e -> {
            try {
                if (prixField.getText().isEmpty() || stockField.getText().isEmpty()) {
                    showSimpleAlert("Champs manquants", "Le prix et le stock sont obligatoires.");
                    return;
                }
                
                ProductFormat target = (f != null) ? f : new ProductFormat();
                target.setPrix(Double.parseDouble(prixField.getText()));
                target.setStock(Integer.parseInt(stockField.getText()));
                target.setStockAlerte(Integer.parseInt(stockAlerteField.getText().isEmpty() ? "5" : stockAlerteField.getText()));
                target.setLabelValues(new ArrayList<>(selectedValues));
                target.setImageUrl(imgUrlField.getText());
                
                if (f == null) parentList.add(target);
                else {
                    // Force refresh table
                    int idx = parentList.indexOf(f);
                    parentList.set(idx, target);
                }
                
                stage.close();
            } catch (Exception ex) {
                showSimpleAlert("Saisie invalide", "Veuillez entrer des nombres valides pour le prix et le stock.");
            }
        });

        root.getChildren().addAll(new Label("Tarification et Stock"), prixField, stockField, stockAlerteField, imgBox, new Separator(), varHeader, gridVars, btnOk);
        stage.setScene(new Scene(root, 350, 520));
        stage.show();
    }

    private void buildVariantGrid(GridPane grid, int idCat, ProductFormat f, List<LabelValue> selectedValues) {
        grid.getChildren().clear();
        List<com.chrionline.shared.models.Label> labels = controller.getLabelsByCategorie(idCat);
        
        int rowCnt = 0;
        for (com.chrionline.shared.models.Label l : labels) {
            Text lName = new Text(l.getNom());
            ComboBox<LabelValue> lvCombo = new ComboBox<>();
            lvCombo.setPromptText("Choisir " + l.getNom());
            HBox.setHgrow(lvCombo, Priority.ALWAYS);
            lvCombo.setMaxWidth(Double.MAX_VALUE);
            
            List<LabelValue> values = controller.getLabelValues(l.getId());
            lvCombo.setItems(FXCollections.observableArrayList(values));
            
            Button btnAddValue = new Button("+");
            btnAddValue.getStyleClass().add("btn-secondary");
            btnAddValue.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Nouvelle Valeur");
                dialog.setHeaderText("Ajouter une valeur pour " + l.getNom());
                dialog.setContentText("Nom de la valeur :");
                dialog.showAndWait().ifPresent(valStr -> {
                    if (!valStr.trim().isEmpty()) {
                        int newId = controller.ajouterLabelValue(l.getId(), valStr.trim());
                        if (newId > 0) {
                            List<LabelValue> updated = controller.getLabelValues(l.getId());
                            lvCombo.setItems(FXCollections.observableArrayList(updated));
                            updated.stream().filter(v -> v.getId() == newId).findFirst().ifPresent(lvCombo::setValue);
                            LabelValue v = lvCombo.getValue();
                            if (v != null) {
                                selectedValues.removeIf(val -> val.getLabel() != null && val.getLabel().getId() == l.getId());
                                selectedValues.add(v);
                            }
                        }
                    }
                });
            });

            HBox rowBox = new HBox(5, lvCombo, btnAddValue);
            rowBox.setAlignment(Pos.CENTER_LEFT);

            if (f != null) {
                for (LabelValue existing : f.getLabelValues()) {
                    if (existing.getLabel() != null && existing.getLabel().getId() == l.getId()) {
                        for (LabelValue v : values) {
                            if (v.getId() == existing.getId()) {
                                lvCombo.setValue(v);
                                break;
                            }
                        }
                    }
                }
            }
            
            grid.add(lName, 0, rowCnt);
            grid.add(rowBox, 1, rowCnt);
            rowCnt++;
            
            lvCombo.setOnAction(e -> {
                LabelValue v = lvCombo.getValue();
                if (v != null) {
                    selectedValues.removeIf(val -> val.getLabel() != null && val.getLabel().getId() == l.getId());
                    selectedValues.add(v);
                }
            });
        }
    }

    private void filterProduits(String query) {
        if (query == null || query.isEmpty()) {
            renderCards(produitsData);
            return;
        }
        String lower = query.toLowerCase();
        List<Produit> filtered = produitsData.stream().filter(p -> 
            p.getNom().toLowerCase().contains(lower) || 
            p.getDescription().toLowerCase().contains(lower)
        ).collect(Collectors.toList());
        renderCards(filtered);
    }

    private void showSimpleAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
