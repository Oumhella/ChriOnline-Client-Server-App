package com.chrionline.client.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.Random;

/**
 * Widget CAPTCHA natif JavaFX (sans WebView, sans dépendance externe).
 * Génère un code alphanumérique aléatoire affiché sur un canvas avec du bruit visuel.
 */
public class CaptchaWidget extends VBox {

    private static final String[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".split("");
    private static final int CODE_LENGTH = 5;

    private final Canvas canvas;
    private final TextField inputField;
    private String currentCode;
    private final Random random = new Random();

    // Couleurs du thème ChriOnline
    private static final String SAUGE_DARK   = "#6B9E7A";
    private static final String TERRACOTTA   = "#C96B4A";
    private static final String BRUN_MED     = "#6B4F3A";
    private static final String CREME_INPUT  = "#F5EFE8";
    private static final String BORDER       = "#E8E0D5";

    public CaptchaWidget() {
        super(8);
        setPadding(new Insets(0));
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);

        // ── Titre ──────────────────────────────────────────────
        Label titre = new Label("Vérification de sécurité");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        titre.setTextFill(Color.web(BRUN_MED));

        // ── Canvas d'affichage du code ─────────────────────────
        canvas = new Canvas(240, 60);
        canvas.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);");

        // ── Bouton rafraîchir + canvas ─────────────────────────
        Button btnRefresh = new Button("↻");
        btnRefresh.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        btnRefresh.setStyle(
            "-fx-background-color: " + SAUGE_DARK + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 10;" +
            "-fx-cursor: hand;"
        );
        btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(
            "-fx-background-color: #4E7A5C;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 10;" +
            "-fx-cursor: hand;"
        ));
        btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(
            "-fx-background-color: " + SAUGE_DARK + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 10;" +
            "-fx-cursor: hand;"
        ));
        btnRefresh.setOnAction(e -> genererNouveauCode());
        btnRefresh.setTooltip(new javafx.scene.control.Tooltip("Générer un nouveau code"));

        HBox canvasRow = new HBox(8, canvas, btnRefresh);
        canvasRow.setAlignment(Pos.CENTER_LEFT);

        // ── Champ de saisie ────────────────────────────────────
        inputField = new TextField();
        inputField.setPromptText("Entrez le code affiché ci-dessus");
        inputField.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        inputField.setStyle(
            "-fx-background-color:" + CREME_INPUT + ";" +
            "-fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:6;" +
            "-fx-background-radius:6;" +
            "-fx-padding:8 12;" +
            "-fx-text-fill:" + BRUN_MED + ";"
        );
        inputField.setMaxWidth(Double.MAX_VALUE);
        inputField.focusedProperty().addListener((o, old, f) ->
            inputField.setStyle(f
                ? "-fx-background-color:#FFFEFB;-fx-border-color:" + SAUGE_DARK + ";-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;-fx-text-fill:" + BRUN_MED + ";-fx-border-width:1.5;"
                : "-fx-background-color:" + CREME_INPUT + ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;-fx-text-fill:" + BRUN_MED + ";"
            )
        );

        getChildren().addAll(titre, canvasRow, inputField);

        // Générer le premier code
        genererNouveauCode();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Génération et dessin du code
    // ─────────────────────────────────────────────────────────────────────────

    private void genererNouveauCode() {
        // Générer un code aléatoire
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        currentCode = sb.toString();
        inputField.clear();
        dessinerCode();
    }

    private void dessinerCode() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Fond avec dégradé subtil
        gc.setFill(Color.web("#F8F3EC"));
        gc.fillRoundRect(0, 0, w, h, 10, 10);

        // Bordure
        gc.setStroke(Color.web(BORDER));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(0, 0, w, h, 10, 10);

        // Lignes de bruit (perturbations visuelles)
        gc.setLineWidth(1);
        for (int i = 0; i < 6; i++) {
            gc.setStroke(Color.web(i % 2 == 0 ? SAUGE_DARK : TERRACOTTA, 0.15 + random.nextDouble() * 0.15));
            gc.strokeLine(
                random.nextDouble() * w, random.nextDouble() * h,
                random.nextDouble() * w, random.nextDouble() * h
            );
        }

        // Points de bruit
        for (int i = 0; i < 40; i++) {
            gc.setFill(Color.web(BRUN_MED, 0.07 + random.nextDouble() * 0.10));
            double r = 1 + random.nextDouble() * 2;
            gc.fillOval(random.nextDouble() * w, random.nextDouble() * h, r, r);
        }

        // Dessiner chaque caractère avec rotation et position aléatoires
        String[] fontFamilies = {"Georgia", "Verdana", "Arial"};
        String[] colors = {"#3E2C1E", "#6B9E7A", "#C96B4A", "#4E7A5C", "#8B5E3C"};
        double charW = (w - 20) / CODE_LENGTH;

        for (int i = 0; i < currentCode.length(); i++) {
            char c = currentCode.charAt(i);
            double size   = 22 + random.nextDouble() * 8;
            double x      = 10 + i * charW + charW * 0.5;
            double y      = h * 0.5 + (random.nextDouble() - 0.5) * 10;
            double angle  = (random.nextDouble() - 0.5) * 35;

            gc.save();
            gc.translate(x, y);
            gc.rotate(angle);
            gc.setFont(Font.font(
                fontFamilies[random.nextInt(fontFamilies.length)],
                random.nextBoolean() ? FontWeight.BOLD : FontWeight.NORMAL,
                size
            ));
            gc.setFill(Color.web(colors[random.nextInt(colors.length)]));
            gc.fillText(String.valueOf(c), -size * 0.3, size * 0.35);
            gc.restore();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  API publique
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie si le code saisi par l'utilisateur correspond au code affiché.
     * @return true si le captcha est validé.
     */
    public boolean estValide() {
        return currentCode.equalsIgnoreCase(inputField.getText().trim());
    }

    /**
     * Retourne le code actuel (pour validation côté serveur si nécessaire).
     */
    public String getCodeActuel() {
        return currentCode;
    }

    /**
     * Réinitialise le widget et génère un nouveau code.
     */
    public void reinitialiser() {
        genererNouveauCode();
    }
}
