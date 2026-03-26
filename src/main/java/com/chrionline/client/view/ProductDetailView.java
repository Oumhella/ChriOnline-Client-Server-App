package com.chrionline.client.view;

import com.chrionline.client.controller.CatalogueController;
import com.chrionline.shared.models.LabelValue;
import com.chrionline.shared.models.ProductFormat;
import com.chrionline.shared.models.Produit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;

public class ProductDetailView {

    private static final String CREME      = "#FDFBF7";
    private static final String SAUGE      = "#A8C4B0";
    private static final String SAUGE_DARK = "#6B9E7A";
    private static final String BRUN       = "#3E2C1E";
    private static final String BRUN_LIGHT = "#9A7B65";
    private static final String TERRACOTTA = "#C96B4A";
    private static final String TERRA_HOVER= "#A0522D";
    private static final String BORDER     = "#E8E0D5";
    private static final String DANGER_BG  = "#FBEAEA";
    private static final String DANGER     = "#B03A2E";

    private final Produit             produit;
    private final Stage               stage;
    private final CatalogueView       catalogueView;
    private final CatalogueController controller;
    private final int                 userId;

    // Format sélectionné par l'utilisateur (par défaut le premier)
    private ProductFormat formatSelectionne;

    /**
     * @param produit       Le produit à afficher
     * @param stage         Le stage JavaFX courant
     * @param catalogueView La vue catalogue pour le retour
     * @param controller    Le controller catalogue (contient userId + ajouterAuPanier)
     * @param userId        L'id de l'utilisateur connecté (-1 si non connecté)
     */
    public ProductDetailView(Produit produit, Stage stage,
                             CatalogueView catalogueView,
                             CatalogueController controller,
                             int userId) {
        this.produit       = produit;
        this.stage         = stage;
        this.catalogueView = catalogueView;
        this.controller    = controller;
        this.userId        = userId;

        // Sélectionner le premier format par défaut
        if (produit.getFormats() != null && !produit.getFormats().isEmpty()) {
            this.formatSelectionne = produit.getFormats().get(0);
        }
    }

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        root.getChildren().add(buildTopBar());

        HBox content = new HBox(50);
        content.setPadding(new Insets(40, 60, 40, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(buildImagePanel(), buildInfoPanel());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + CREME + "; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(scroll);
        return root;
    }

    // ══════════════════════════════════════════════════════════════
    //  Top Bar — retour + logo + bouton panier
    // ══════════════════════════════════════════════════════════════

    private HBox buildTopBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(20, 60, 20, 60));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        // ── Bouton retour ──────────────────────────────────────
        Button btnRetour = new Button("← Retour au catalogue");
        btnRetour.setFont(Font.font("Georgia", 14));
        btnRetour.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + BRUN + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 8 18;"
        );
        btnRetour.setCursor(Cursor.HAND);
        btnRetour.setOnMouseEntered(e -> btnRetour.setStyle(
                "-fx-background-color: " + BORDER + ";" +
                        "-fx-text-fill: " + BRUN + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-padding: 8 18;"
        ));
        btnRetour.setOnMouseExited(e -> btnRetour.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + BRUN + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6; -fx-padding: 8 18;"
        ));
        btnRetour.setOnAction(e -> catalogueView.retourCatalogue());

        // ── Logo centré ────────────────────────────────────────
        Region spacerL = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);

        Text logo = new Text("SHE SOAP");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setFill(Color.web(BRUN));

        Region spacerR = new Region();
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        // ── Bouton panier (visible seulement si connecté) ──────
        if (userId != -1) {
            Button btnPanier = new Button("🛒 Mon panier");
            btnPanier.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
            btnPanier.setStyle(
                    "-fx-background-color: " + TERRACOTTA + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 8 18;"
            );
            btnPanier.setCursor(Cursor.HAND);
            btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(
                    "-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 18;"
            ));
            btnPanier.setOnMouseExited(e -> btnPanier.setStyle(
                    "-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 18;"
            ));
            btnPanier.setOnAction(e -> ouvrirPanier());

            bar.getChildren().addAll(btnRetour, spacerL, logo, spacerR, btnPanier);
        } else {
            bar.getChildren().addAll(btnRetour, spacerL, logo, spacerR);
        }

        return bar;
    }

    // ══════════════════════════════════════════════════════════════
    //  Image Panel
    // ══════════════════════════════════════════════════════════════

    private StackPane buildImagePanel() {
        StackPane container = new StackPane();
        container.setPrefSize(420, 500);
        container.setMinWidth(420);
        container.setMaxWidth(420);
        container.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14;");

        Text initial = new Text(produit.getNom().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Georgia", 70));
        initial.setFill(Color.web(BRUN_LIGHT, 0.3));

        String imageUrl = produit.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && formatSelectionne != null) {
            imageUrl = formatSelectionne.getImageUrl();
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 420, 500, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(420);
                iv.setFitHeight(500);
                iv.setPreserveRatio(true);
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError())
                        initial.setVisible(false);
                });
                Rectangle clip = new Rectangle(420, 500);
                clip.setArcWidth(28); clip.setArcHeight(28);
                iv.setClip(clip);
                container.getChildren().addAll(initial, iv);
            } catch (Exception e) {
                container.getChildren().add(initial);
            }
        } else {
            container.getChildren().add(initial);
        }

        return container;
    }

    // ══════════════════════════════════════════════════════════════
    //  Info Panel
    // ══════════════════════════════════════════════════════════════

    private VBox buildInfoPanel() {
        VBox info = new VBox(18);
        info.setMaxWidth(500);
        info.setAlignment(Pos.TOP_LEFT);

        // ── Nom ───────────────────────────────────────────────
        Text nomText = new Text(produit.getNom());
        nomText.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        nomText.setFill(Color.web(BRUN));
        info.getChildren().add(nomText);

        // ── Catégorie badge ───────────────────────────────────
        if (produit.getCategorie() != null && produit.getCategorie().getNom() != null) {
            Label catLabel = new Label(produit.getCategorie().getNom());
            catLabel.setFont(Font.font("Georgia", 13));
            catLabel.setStyle(
                    "-fx-background-color: " + SAUGE + "; -fx-text-fill: white;" +
                            "-fx-padding: 4 14; -fx-background-radius: 12;"
            );
            info.getChildren().add(catLabel);
        }

        // ── Prix affiché depuis le format sélectionné ──────────
        if (formatSelectionne != null && formatSelectionne.getPrix() != null) {
            Text priceLabel = new Text(String.format("%.2f MAD", formatSelectionne.getPrix()));
            priceLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
            priceLabel.setFill(Color.web(TERRACOTTA));
            info.getChildren().add(priceLabel);
        }

        // ── Description ───────────────────────────────────────
        if (produit.getDescription() != null && !produit.getDescription().isBlank()) {
            info.getChildren().add(new Separator());
            Text descTitle = new Text("Description");
            descTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            descTitle.setFill(Color.web(BRUN));
            Text descText = new Text(produit.getDescription());
            descText.setFont(Font.font("Georgia", 14));
            descText.setFill(Color.web(BRUN_LIGHT));
            descText.setWrappingWidth(460);
            descText.setLineSpacing(4);
            info.getChildren().addAll(descTitle, descText);
        }

        // ── Formats ───────────────────────────────────────────
        List<ProductFormat> formats = produit.getFormats();
        if (formats != null && !formats.isEmpty()) {
            info.getChildren().add(new Separator());
            Text formatsTitle = new Text("Formats disponibles");
            formatsTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            formatsTitle.setFill(Color.web(BRUN));
            info.getChildren().add(formatsTitle);

            for (ProductFormat format : formats) {
                info.getChildren().add(buildFormatCard(format));
            }
        }

        // ── Bouton Ajouter au panier ──────────────────────────
        info.getChildren().add(new Separator());

        if (userId != -1 && formatSelectionne != null) {
            Button btnAcheter = new Button("Ajouter au panier");
            btnAcheter.setMaxWidth(Double.MAX_VALUE);
            btnAcheter.setCursor(Cursor.HAND);
            btnAcheter.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
            btnAcheter.setStyle(
                    "-fx-background-color: " + TERRACOTTA + ";" +
                            "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 14 0;"
            );
            btnAcheter.setOnMouseEntered(e -> btnAcheter.setStyle(
                    "-fx-background-color: " + TERRA_HOVER + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 14 0;"
            ));
            btnAcheter.setOnMouseExited(e -> btnAcheter.setStyle(
                    "-fx-background-color: " + TERRACOTTA + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 14 0;"
            ));
            // ✅ Appel réel au controller avec le produit et l'ID du format sélectionné
            btnAcheter.setOnAction(e -> {
                if (formatSelectionne != null) {
                    controller.ajouterAuPanier(produit, formatSelectionne.getId());
                }
            });

            // Bouton aller au panier
            Button btnVoirPanier = new Button("🛒 Voir mon panier");
            btnVoirPanier.setMaxWidth(Double.MAX_VALUE);
            btnVoirPanier.setCursor(Cursor.HAND);
            btnVoirPanier.setFont(Font.font("Georgia", 14));
            btnVoirPanier.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: " + TERRACOTTA + ";" +
                            "-fx-border-radius: 8;" +
                            "-fx-text-fill: " + TERRACOTTA + ";" +
                            "-fx-padding: 12 0;"
            );
            btnVoirPanier.setOnMouseEntered(e -> btnVoirPanier.setStyle(
                    "-fx-background-color: " + TERRACOTTA + "; -fx-border-color: " + TERRACOTTA + ";" +
                            "-fx-border-radius: 8; -fx-text-fill: white; -fx-padding: 12 0;"
            ));
            btnVoirPanier.setOnMouseExited(e -> btnVoirPanier.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: " + TERRACOTTA + ";" +
                            "-fx-border-radius: 8; -fx-text-fill: " + TERRACOTTA + "; -fx-padding: 12 0;"
            ));
            btnVoirPanier.setOnAction(e -> ouvrirPanier());

            VBox.setMargin(btnAcheter,   new Insets(10, 0, 0, 0));
            VBox.setMargin(btnVoirPanier, new Insets(4, 0, 0, 0));
            info.getChildren().addAll(btnAcheter, btnVoirPanier);

        } else if (userId == -1) {
            // Non connecté
            Label msg = new Label("Connectez-vous pour ajouter au panier");
            msg.setFont(Font.font("Georgia", 13));
            msg.setStyle("-fx-text-fill: " + BRUN_LIGHT + "; -fx-font-style: italic;");
            info.getChildren().add(msg);
        } else {
            // Pas de format disponible
            Label msg = new Label("Produit indisponible (aucun format trouvé)");
            msg.setFont(Font.font("Georgia", 13));
            msg.setStyle(
                    "-fx-background-color: " + DANGER_BG + ";" +
                            "-fx-text-fill: " + DANGER + ";" +
                            "-fx-padding: 8 14; -fx-background-radius: 6;"
            );
            info.getChildren().add(msg);
        }

        return info;
    }

    // ══════════════════════════════════════════════════════════════
    //  Format Card
    // ══════════════════════════════════════════════════════════════

    private VBox buildFormatCard(ProductFormat format) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));

        boolean estSelectionne = formatSelectionne != null && formatSelectionne.getId() == format.getId();

        card.setStyle(
                "-fx-background-color: " + (estSelectionne ? "#F0F8F2" : "white") + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + (estSelectionne ? SAUGE_DARK : BORDER) + ";" +
                        "-fx-border-radius: 10; -fx-border-width: " + (estSelectionne ? "2" : "1") + ";"
        );
        card.setCursor(Cursor.HAND);

        // Clic sur le format → le sélectionner et rafraîchir l'info panel
        card.setOnMouseClicked(e -> {
            formatSelectionne = format;
            // Rafraîchir l'affichage des informations en reconstruisant la vue
            stage.getScene().setRoot(build()); 
        });

        // ── Labels (ex: Taille: 100g) ──
        List<LabelValue> labelValues = format.getLabelValues();
        if (labelValues != null && !labelValues.isEmpty()) {
            HBox labelsRow = new HBox(12);
            labelsRow.setAlignment(Pos.CENTER_LEFT);
            for (LabelValue lv : labelValues) {
                String labelName = (lv.getLabel() != null) ? lv.getLabel().getNom() : "Attribut";
                Label tag = new Label(labelName + " : " + lv.getValeur());
                tag.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
                tag.setStyle(
                        "-fx-background-color: " + CREME + ";" +
                                "-fx-text-fill: " + BRUN + ";" +
                                "-fx-padding: 3 10; -fx-background-radius: 8;"
                );
                labelsRow.getChildren().add(tag);
            }
            card.getChildren().add(labelsRow);
        }

        // ── Prix + Stock ──
        HBox prixStockRow = new HBox(20);
        prixStockRow.setAlignment(Pos.CENTER_LEFT);

        if (format.getPrix() != null) {
            Text prixText = new Text(String.format("%.2f MAD", format.getPrix()));
            prixText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            prixText.setFill(Color.web(TERRACOTTA));
            prixStockRow.getChildren().add(prixText);
        }

        String stockLabel;
        String stockColor;
        if (format.getStock() > format.getStockAlerte()) {
            stockLabel = "En stock (" + format.getStock() + ")";
            stockColor = SAUGE_DARK;
        } else if (format.getStock() > 0) {
            stockLabel = "Stock faible (" + format.getStock() + ")";
            stockColor = TERRACOTTA;
        } else {
            stockLabel = "Rupture de stock";
            stockColor = DANGER;
        }

        Text stockText = new Text(stockLabel);
        stockText.setFont(Font.font("Georgia", 12));
        stockText.setFill(Color.web(stockColor));
        prixStockRow.getChildren().add(stockText);

        card.getChildren().add(prixStockRow);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════

    private void ouvrirPanier() {
        try {
            new PanierView(userId).start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}