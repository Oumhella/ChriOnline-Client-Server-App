package com.chrionline.client.view;

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
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;

/**
 * Vue de détail d'un produit — affiche l'image, la description,
 * la catégorie, et la liste des formats avec labels et prix.
 */
public class ProductDetailView {

    // Palette (identique à CatalogueView)
    private static final String CREME       = "#FDFBF7";
    private static final String SAUGE       = "#A8C4B0";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String BORDER      = "#E8E0D5";

    private final Produit produit;
    private final Stage stage;
    private final CatalogueView catalogueView;

    public ProductDetailView(Produit produit, Stage stage, CatalogueView catalogueView) {
        this.produit = produit;
        this.stage = stage;
        this.catalogueView = catalogueView;
    }

    /**
     * Builds and returns the complete detail view layout.
     */
    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + CREME + ";");

        // ── Top Bar with Back Button ──────────────────────────
        root.getChildren().add(buildTopBar());

        // ── Main Content ──────────────────────────────────────
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
    //  Top Bar
    // ══════════════════════════════════════════════════════════════

    private HBox buildTopBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(20, 60, 20, 60));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");

        Button btnRetour = new Button("← Retour au catalogue");
        btnRetour.setFont(Font.font("Georgia", 14));
        btnRetour.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + BRUN + ";" +
            "-fx-cursor: hand;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6;" +
            "-fx-padding: 8 18;"
        );
        btnRetour.setOnAction(e -> catalogueView.retourCatalogue());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text title = new Text("SHE SOAP");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(BRUN));

        bar.getChildren().addAll(btnRetour, spacer, title);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════
    //  Image Panel (left)
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

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                Image img = new Image(imageUrl, 420, 500, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(420);
                iv.setFitHeight(500);
                iv.setPreserveRatio(true);

                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        initial.setVisible(false);
                    }
                });

                Rectangle clip = new Rectangle(420, 500);
                clip.setArcWidth(28);
                clip.setArcHeight(28);
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
    //  Info Panel (right)
    // ══════════════════════════════════════════════════════════════

    private VBox buildInfoPanel() {
        VBox info = new VBox(18);
        info.setMaxWidth(500);
        info.setAlignment(Pos.TOP_LEFT);

        // ── Product Name ──
        Text nomText = new Text(produit.getNom());
        nomText.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        nomText.setFill(Color.web(BRUN));

        info.getChildren().add(nomText);

        // ── Category ──
        if (produit.getCategorie() != null && produit.getCategorie().getNom() != null) {
            HBox catBadge = new HBox();
            catBadge.setAlignment(Pos.CENTER_LEFT);
            Label catLabel = new Label(produit.getCategorie().getNom());
            catLabel.setFont(Font.font("Georgia", 13));
            catLabel.setStyle(
                "-fx-background-color: " + SAUGE + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 4 14;" +
                "-fx-background-radius: 12;"
            );
            catBadge.getChildren().add(catLabel);
            info.getChildren().add(catBadge);
        }

        // ── Price (from the produit's top-level prix) ──
        if (produit.getPrix() > 0) {
            Text priceLabel = new Text(String.format("%.2f MAD", produit.getPrix()));
            priceLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
            priceLabel.setFill(Color.web(TERRACOTTA));
            info.getChildren().add(priceLabel);
        }

        // ── Description ──
        if (produit.getDescription() != null && !produit.getDescription().isBlank()) {
            Separator sep1 = new Separator();
            sep1.setStyle("-fx-background-color: " + BORDER + ";");

            Text descTitle = new Text("Description");
            descTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            descTitle.setFill(Color.web(BRUN));

            Text descText = new Text(produit.getDescription());
            descText.setFont(Font.font("Georgia", 14));
            descText.setFill(Color.web(BRUN_LIGHT));
            descText.setWrappingWidth(460);
            descText.setLineSpacing(4);

            info.getChildren().addAll(sep1, descTitle, descText);
        }

        // ── Formats ──
        List<ProductFormat> formats = produit.getFormats();
        if (formats != null && !formats.isEmpty()) {
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-background-color: " + BORDER + ";");

            Text formatsTitle = new Text("Formats disponibles");
            formatsTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            formatsTitle.setFill(Color.web(BRUN));

            info.getChildren().addAll(sep2, formatsTitle);

            for (ProductFormat format : formats) {
                info.getChildren().add(buildFormatCard(format));
            }
        }

        // ── Add to Cart Button ──
        Button btnAcheter = new Button("Ajouter au panier");
        btnAcheter.setMaxWidth(Double.MAX_VALUE);
        btnAcheter.setCursor(Cursor.HAND);
        btnAcheter.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        btnAcheter.setStyle(
            "-fx-background-color: " + TERRACOTTA + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 14 0;"
        );
        btnAcheter.setOnAction(e -> System.out.println("[Detail] Ajout au panier : " + produit.getNom()));

        VBox.setMargin(btnAcheter, new Insets(10, 0, 0, 0));
        info.getChildren().add(btnAcheter);

        return info;
    }

    // ══════════════════════════════════════════════════════════════
    //  Format Card
    // ══════════════════════════════════════════════════════════════

    private VBox buildFormatCard(ProductFormat format) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 10;"
        );

        // ── Label-values (e.g. "Poids: 100g", "Parfum: Lavande") ──
        List<LabelValue> labelValues = format.getLabelValues();
        if (labelValues != null && !labelValues.isEmpty()) {
            HBox labelsRow = new HBox(12);
            labelsRow.setAlignment(Pos.CENTER_LEFT);

            for (LabelValue lv : labelValues) {
                String labelName = (lv.getLabel() != null) ? lv.getLabel().getNom() : "Attribut";
                Label tag = new Label(labelName + " : " + lv.getValeur());
                tag.setFont(Font.font("Georgia", 12));
                tag.setStyle(
                    "-fx-background-color: " + CREME + ";" +
                    "-fx-text-fill: " + BRUN + ";" +
                    "-fx-padding: 3 10;" +
                    "-fx-background-radius: 8;"
                );
                labelsRow.getChildren().add(tag);
            }
            card.getChildren().add(labelsRow);
        }

        // ── Price & Stock row ──
        HBox prixStockRow = new HBox(20);
        prixStockRow.setAlignment(Pos.CENTER_LEFT);

        if (format.getPrix() != null) {
            Text prixText = new Text(String.format("%.2f MAD", format.getPrix()));
            prixText.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            prixText.setFill(Color.web(TERRACOTTA));
            prixStockRow.getChildren().add(prixText);
        }

        // Stock indicator
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
            stockColor = "#CC3333";
        }

        Text stockText = new Text(stockLabel);
        stockText.setFont(Font.font("Georgia", 12));
        stockText.setFill(Color.web(stockColor));
        prixStockRow.getChildren().add(stockText);

        card.getChildren().add(prixStockRow);

        return card;
    }
}
