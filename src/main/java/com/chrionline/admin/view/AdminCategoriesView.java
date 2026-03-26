package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminCategoriesController;
import com.chrionline.shared.models.Categorie;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class AdminCategoriesView {

    // ─── Palette (same as AdminProduitsView) ──────────────────────────────────
    private static final String CREME        = "#FDFBF7";
    private static final String CREME_CARD   = "#FFFEFB";
    private static final String CREME_INPUT  = "#F5EFE8";
    private static final String BORDER       = "#E8E0D5";
    private static final String BRUN         = "#3E2C1E";
    private static final String BRUN_MED     = "#6B4F3A";
    private static final String BRUN_LIGHT   = "#9A7B65";
    private static final String SAUGE_DARK   = "#6B9E7A";
    private static final String SAUGE_LIGHT  = "#A8D5B5";
    private static final String TERRACOTTA   = "#C96B4A";
    private static final String DANGER       = "#B03A2E";

    private AdminCategoriesController controller;
    private FlowPane cardsPane;
    private List<Categorie> allCategories;
    private TextField searchField;

    public Node getView() {
        this.controller = new AdminCategoriesController();

        VBox content = new VBox(24);
        content.setPadding(new Insets(30, 34, 34, 34));

        content.getChildren().addAll(buildHeader(), buildStatsBar(), buildCardsContainer());
        chargerDonnees();

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    // ─── Header ───────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        VBox tb = new VBox(3);
        Text titre = new Text("Gestion des Catégories");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Organisez votre catalogue par thématiques.");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sousTitre.setFill(Color.web(BRUN_LIGHT));
        tb.getChildren().addAll(titre, sousTitre);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher une catégorie...");
        searchField.getStyleClass().add("text-field");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, o, n) -> filterCards(n));

        Button btnAdd = new Button("+ Nouvelle Catégorie");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> ouvrirPopupCategorie(null));

        HBox header = new HBox(15, tb, spacer, searchField, btnAdd);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ─── Stats Bar ────────────────────────────────────────────────────────────

    private HBox buildStatsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox totalBox = buildStatCard("📂 Total", "—", SAUGE_DARK);
        VBox rootBox  = buildStatCard("🌲 Racines", "—", BRUN_MED);
        VBox subBox   = buildStatCard("🔖 Sous-cat.", "—", TERRACOTTA);

        bar.getChildren().addAll(totalBox, rootBox, subBox);
        bar.setUserData(new Object[]{totalBox, rootBox, subBox});
        return bar;
    }

    private VBox buildStatCard(String label, String val, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 12, 20));
        box.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 10; " +
                     "-fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        box.setEffect(new DropShadow(4, Color.web(BRUN, 0.05)));

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
        lbl.setTextFill(Color.web(BRUN_LIGHT));

        Label valLbl = new Label(val);
        valLbl.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        valLbl.setTextFill(Color.web(color));
        valLbl.setId("stat_val_" + label.replace(" ", "_"));

        box.getChildren().addAll(lbl, valLbl);
        return box;
    }

    // ─── Cards Grid ───────────────────────────────────────────────────────────

    private Node buildCardsContainer() {
        cardsPane = new FlowPane(16, 16);
        cardsPane.setPadding(new Insets(4));
        cardsPane.setAlignment(Pos.TOP_LEFT);
        cardsPane.setStyle("-fx-background-color: transparent;");
        return cardsPane;
    }

    private void renderCards(List<Categorie> list) {
        cardsPane.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Aucune catégorie trouvée.");
            empty.setFont(Font.font("Georgia", FontPosture.ITALIC, 14));
            empty.setTextFill(Color.web(BRUN_LIGHT));
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Categorie c : list) {
            cardsPane.getChildren().add(buildCategoryCard(c, list));
        }
    }

    private Node buildCategoryCard(Categorie c, List<Categorie> all) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setMaxWidth(280);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 14; " +
                      "-fx-border-color: " + BORDER + "; -fx-border-radius: 14; -fx-border-width: 1;");
        card.setEffect(new DropShadow(6, Color.web(BRUN, 0.08)));
        card.setCursor(Cursor.HAND);

        // ─ Icon + Name row ─
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String emoji = c.getIdParent() > 0 ? "🔖" : "📂";
        Label icon = new Label(emoji);
        icon.setFont(Font.font(22));

        VBox nameBox = new VBox(2);
        Label name = new Label(c.getNom());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setTextFill(Color.web(BRUN));
        name.setWrapText(true);

        // Parent badge
        if (c.getIdParent() > 0) {
            String parentName = all.stream()
                .filter(p -> p.getId() == c.getIdParent())
                .map(Categorie::getNom)
                .findFirst().orElse("ID:" + c.getIdParent());
            Label parentBadge = new Label("↳ " + parentName);
            parentBadge.setFont(Font.font("Georgia", FontPosture.ITALIC, 11));
            parentBadge.setTextFill(Color.web(BRUN_LIGHT));
            nameBox.getChildren().addAll(name, parentBadge);
        } else {
            Label rootBadge = new Label("Catégorie racine");
            rootBadge.setFont(Font.font(10));
            rootBadge.setStyle("-fx-background-color: " + SAUGE_LIGHT + "; -fx-background-radius: 4; " +
                               "-fx-padding: 2 6; -fx-text-fill: #2D6A3F;");
            nameBox.getChildren().addAll(name, rootBadge);
        }

        topRow.getChildren().addAll(icon, nameBox);

        // ─ Description ─
        String descText = (c.getDescription() != null && !c.getDescription().isEmpty())
            ? c.getDescription() : "Aucune description.";
        Label desc = new Label(descText);
        desc.setFont(Font.font(12));
        desc.setTextFill(Color.web(BRUN_MED));
        desc.setWrapText(true);
        desc.setOpacity(0.75);
        desc.setMaxHeight(44);

        // ─ Sub-category count ─
        long subCount = all.stream().filter(x -> x.getIdParent() == c.getId()).count();
        Label subBadge = new Label("📌 " + subCount + " sous-catégorie" + (subCount != 1 ? "s" : ""));
        subBadge.setFont(Font.font(11));
        subBadge.setTextFill(Color.web(BRUN_LIGHT));

        // ─ Separator ─
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BORDER + ";");

        // ─ Actions ─
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✏️ Modifier");
        btnEdit.setFont(Font.font(11));
        btnEdit.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; " +
                         "-fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> ouvrirPopupCategorie(c));

        Button btnDel = new Button("🗑");
        btnDel.setFont(Font.font(12));
        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: " + DANGER + "; " +
                        "-fx-border-color: " + DANGER + "; -fx-border-radius: 6; -fx-padding: 4 10; " +
                        "-fx-cursor: hand; -fx-border-width: 1;");
        btnDel.setOnAction(e -> supprimerCategorie(c));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        actions.getChildren().addAll(sp, btnEdit, btnDel);

        card.getChildren().addAll(topRow, desc, subBadge, sep, actions);

        // Hover
        card.setOnMouseEntered(ev -> {
            card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 14; " +
                          "-fx-border-color: " + SAUGE_DARK + "; -fx-border-radius: 14; -fx-border-width: 1.5;");
            card.setEffect(new DropShadow(14, Color.web(SAUGE_DARK, 0.2)));
            card.setScaleX(1.02); card.setScaleY(1.02);
        });
        card.setOnMouseExited(ev -> {
            card.setStyle("-fx-background-color: " + CREME_CARD + "; -fx-background-radius: 14; " +
                          "-fx-border-color: " + BORDER + "; -fx-border-radius: 14; -fx-border-width: 1;");
            card.setEffect(new DropShadow(6, Color.web(BRUN, 0.08)));
            card.setScaleX(1); card.setScaleY(1);
        });

        return card;
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        new Thread(() -> {
            List<Categorie> list = controller.getAllCategories();
            Platform.runLater(() -> {
                allCategories = list;
                renderCards(list);
                updateStats(list);
            });
        }).start();
    }

    private void updateStats(List<Categorie> list) {
        long total = list.size();
        long roots = list.stream().filter(c -> c.getIdParent() <= 0).count();
        long subs  = total - roots;
        // Update stat labels by lookup
        lookupStat("📂 Total").setText(String.valueOf(total));
        lookupStat("🌲 Racines").setText(String.valueOf(roots));
        lookupStat("🔖 Sous-cat.").setText(String.valueOf(subs));
    }

    private Label lookupStat(String labelText) {
        // find node from cardsPane parent (stats bar is sibling in VBox)
        // We use id-based lookup: stat_val_emoji_label
        String id = "stat_val_" + labelText.replace(" ", "_");
        if (cardsPane.getParent() != null && cardsPane.getParent().getParent() != null) {
            Node found = cardsPane.getParent().lookup("#" + id);
            if (found instanceof Label) return (Label) found;
        }
        return new Label(); // safe fallback
    }

    private void filterCards(String query) {
        if (allCategories == null) return;
        if (query == null || query.isEmpty()) {
            renderCards(allCategories);
            return;
        }
        String low = query.toLowerCase();
        List<Categorie> filtered = allCategories.stream()
            .filter(c -> c.getNom().toLowerCase().contains(low) ||
                         (c.getDescription() != null && c.getDescription().toLowerCase().contains(low)))
            .toList();
        renderCards(filtered);
    }

    // ─── Popup Ajout/Édition ──────────────────────────────────────────────────

    private void ouvrirPopupCategorie(Categorie c) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(c == null ? "Nouvelle Catégorie" : "Édition — " + c.getNom());

        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + CREME + ";");
        root.setPrefWidth(400);

        // Title
        Text heading = new Text(c == null ? "Ajouter une Catégorie" : "Modifier la Catégorie");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        heading.setFill(Color.web(BRUN));

        Separator sep0 = new Separator();

        // Name
        Label lblNom = new Label("Nom *");
        lblNom.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lblNom.setTextFill(Color.web(BRUN_MED));

        TextField nomField = new TextField(c != null ? c.getNom() : "");
        nomField.setPromptText("ex: Équitation, Soins, Accessoires…");
        nomField.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; " +
                          "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10;");

        // Description
        Label lblDesc = new Label("Description");
        lblDesc.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lblDesc.setTextFill(Color.web(BRUN_MED));

        TextArea descArea = new TextArea(c != null ? c.getDescription() : "");
        descArea.setPromptText("Courte description de la catégorie…");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; " +
                          "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10;");

        // Parent
        Label lblParent = new Label("Catégorie Parente");
        lblParent.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        lblParent.setTextFill(Color.web(BRUN_MED));

        ComboBox<Categorie> parentCombo = new ComboBox<>();
        parentCombo.setPromptText("Choisir une catégorie parente (optionnel)");
        parentCombo.setMaxWidth(Double.MAX_VALUE);
        parentCombo.setStyle("-fx-background-color: " + CREME_INPUT + "; -fx-background-radius: 8; " +
                             "-fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        List<Categorie> others = controller.getAllCategories();
        if (c != null) others.removeIf(cat -> cat.getId() == c.getId());
        parentCombo.setItems(FXCollections.observableArrayList(others));
        parentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        parentCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        if (c != null && c.getIdParent() > 0) {
            others.stream().filter(cat -> cat.getId() == c.getIdParent()).findFirst().ifPresent(parentCombo::setValue);
        }

        // Error label
        Label errLabel = new Label("");
        errLabel.setTextFill(Color.web(DANGER));
        errLabel.setFont(Font.font(11));

        // Buttons
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BRUN_MED + "; " +
                           "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-padding: 10 20;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button(c == null ? "Créer" : "Enregistrer");
        btnSave.setStyle("-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white; " +
                         "-fx-background-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> {
            String nom = nomField.getText().trim();
            if (nom.isEmpty()) {
                errLabel.setText("⚠ Le nom est obligatoire.");
                return;
            }
            Categorie toSave = c != null ? c : new Categorie();
            toSave.setNom(nom);
            toSave.setDescription(descArea.getText().trim());
            toSave.setIdParent(parentCombo.getValue() != null ? parentCombo.getValue().getId() : 0);

            if (c == null) {
                int id = controller.ajouterCategorie(toSave);
                if (id > 0) { chargerDonnees(); modal.close(); }
                else errLabel.setText("❌ Erreur lors de la création.");
            } else {
                boolean ok = controller.modifierCategorie(toSave);
                if (ok) { chargerDonnees(); modal.close(); }
                else errLabel.setText("❌ Erreur lors de la modification.");
            }
        });

        btnRow.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(heading, sep0, lblNom, nomField, lblDesc, descArea, lblParent, parentCombo, errLabel, btnRow);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/admin.css").toExternalForm());
        modal.setScene(scene);
        modal.show();
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    private void supprimerCategorie(Categorie c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer « " + c.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible. Les produits liés pourraient être affectés.");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                if (controller.supprimerCategorie(c.getId())) chargerDonnees();
                else showSimpleAlert("Erreur", "Impossible de supprimer (cette catégorie est peut-être liée à des produits).");
            }
        });
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private void showSimpleAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
