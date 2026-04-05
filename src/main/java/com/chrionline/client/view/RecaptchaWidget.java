package com.chrionline.client.view;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;

/**
 * Widget reCAPTCHA v2 "Je ne suis pas un robot"
 * Reproduction fidèle du widget Google officiel en JavaFX pur.
 */
public class RecaptchaWidget extends HBox {

    // ── Couleurs exactes du widget Google ──────────────────────────
    private static final String BG = "#f9f9f9";
    private static final String BORDER_CLR = "#c1c1c1";
    private static final String BORDER_ACT = "#c8c8c8";
    private static final String TEXT_CLR = "#4a4a4a";
    private static final String HINT_CLR = "#9aa0a6";
    private static final String CHECK_BG = "#4285F4"; // bleu Google quand coché
    private static final String CHECK_MARK = "#ffffff";
    private static final String LOGO_BLUE = "#4A90D9";
    private static final String LOGO_RED = "#D0312D";

    private boolean valide = false;

    // Éléments visuels de la checkbox
    private final Rectangle checkBox;
    private final Polyline checkMark;
    private final StackPane checkPane;

    public RecaptchaWidget() {
        super(0);
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(74);
        setMaxWidth(Double.MAX_VALUE);
        setStyle(
                "-fx-background-color: " + BG + ";" +
                        "-fx-border-color: " + BORDER_CLR + ";" +
                        "-fx-border-radius: 3;" +
                        "-fx-background-radius: 3;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 4, 0, 0, 2);");

        // ── Checkbox personnalisée ─────────────────────────────────
        checkBox = new Rectangle(24, 24);
        checkBox.setFill(Color.WHITE);
        checkBox.setStroke(Color.web("#c1c1c1"));
        checkBox.setStrokeWidth(2);
        checkBox.setArcWidth(4);
        checkBox.setArcHeight(4);

        checkMark = new Polyline(5.0, 12.0, 10.0, 18.0, 20.0, 6.0);
        checkMark.setStroke(Color.web(CHECK_MARK));
        checkMark.setStrokeWidth(2.8);
        checkMark.setStrokeLineCap(StrokeLineCap.ROUND);
        checkMark.setStrokeLineJoin(StrokeLineJoin.ROUND);
        checkMark.setFill(Color.TRANSPARENT);
        checkMark.setVisible(false);

        checkPane = new StackPane(checkBox, checkMark);
        checkPane.setPrefSize(24, 24);
        checkPane.setMaxSize(24, 24);
        checkPane.setCursor(Cursor.HAND);
        HBox.setMargin(checkPane, new Insets(0, 0, 0, 22));

        // Hover
        checkPane.setOnMouseEntered(e -> {
            if (!valide)
                checkBox.setStroke(Color.web("#4285F4"));
        });
        checkPane.setOnMouseExited(e -> {
            if (!valide)
                checkBox.setStroke(Color.web("#c1c1c1"));
        });

        // Clic
        checkPane.setOnMouseClicked(e -> toggle());

        // ── Label ──────────────────────────────────────────────────
        Label label = new Label("Je ne suis pas un robot");
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        label.setTextFill(Color.web(TEXT_CLR));
        label.setWrapText(false);
        HBox.setMargin(label, new Insets(0, 0, 0, 16));
        HBox.setHgrow(label, Priority.ALWAYS);

        // ── Logo + branding ────────────────────────────────────────
        VBox logoBox = buildLogoBox();
        HBox.setMargin(logoBox, new Insets(6, 12, 6, 6));

        getChildren().addAll(checkPane, label, logoBox);
    }

    // ────────────────────────────────────────────────────────────────
    // Toggle avec animation
    // ────────────────────────────────────────────────────────────────
    private void toggle() {
        valide = !valide;

        if (valide) {
            // Remplissage bleu + coche
            checkBox.setFill(Color.web(CHECK_BG));
            checkBox.setStroke(Color.web(CHECK_BG));
            checkMark.setVisible(true);

            // Petit "pop" de scale
            ScaleTransition pop = new ScaleTransition(Duration.millis(150), checkPane);
            pop.setFromX(1.0);
            pop.setFromY(1.0);
            pop.setToX(1.15);
            pop.setToY(1.15);
            pop.setAutoReverse(true);
            pop.setCycleCount(2);
            pop.setInterpolator(Interpolator.EASE_BOTH);
            pop.play();

        } else {
            checkBox.setFill(Color.WHITE);
            checkBox.setStroke(Color.web("#c1c1c1"));
            checkMark.setVisible(false);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Logo reCAPTCHA (dessiné en pur JavaFX)
    // ────────────────────────────────────────────────────────────────
    private VBox buildLogoBox() {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(68);

        Pane logoPane = new Pane();
        logoPane.setPrefSize(32, 32);
        logoPane.setMaxSize(32, 32);

        double cx = 16, cy = 16, r = 11.5;

        // ── Arc bleu (en haut, sens horaire ~200°) ──────────────────
        Arc arcBlue = new Arc(cx, cy, r, r, 95, -210);
        arcBlue.setType(ArcType.OPEN);
        arcBlue.setStroke(Color.web(LOGO_BLUE));
        arcBlue.setStrokeWidth(3.8);
        arcBlue.setFill(Color.TRANSPARENT);
        arcBlue.setStrokeLineCap(StrokeLineCap.BUTT);

        // Flèche bleu (pointe en haut-gauche)
        Polygon arrowBlue = arrowHead(
                cx - r * Math.cos(Math.toRadians(97)),
                cy - r * Math.sin(Math.toRadians(97)),
                -30, LOGO_BLUE);

        // ── Arc rouge (en bas, sens horaire ~200°) ──────────────────
        Arc arcRed = new Arc(cx, cy, r, r, -85, -210);
        arcRed.setType(ArcType.OPEN);
        arcRed.setStroke(Color.web(LOGO_RED));
        arcRed.setStrokeWidth(3.8);
        arcRed.setFill(Color.TRANSPARENT);
        arcRed.setStrokeLineCap(StrokeLineCap.BUTT);

        // Flèche rouge (pointe en bas-droite)
        Polygon arrowRed = arrowHead(
                cx - r * Math.cos(Math.toRadians(-83)),
                cy - r * Math.sin(Math.toRadians(-83)),
                150, LOGO_RED);

        // Animation de rotation douce au hover du widget
        RotateTransition spin = new RotateTransition(Duration.millis(800), logoPane);
        spin.setByAngle(360);
        spin.setCycleCount(1);
        spin.setInterpolator(Interpolator.EASE_BOTH);

        setOnMouseEntered(e -> spin.playFromStart());

        logoPane.getChildren().addAll(arcBlue, arcRed, arrowBlue, arrowRed);

        // ── Texte "reCAPTCHA" ───────────────────────────────────────
        Text brand = new Text("reCAPTCHA");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        brand.setFill(Color.web(HINT_CLR));

        // ── "Privacy - Terms" ───────────────────────────────────────
        Text privacy = new Text("Privacy - Terms");
        privacy.setFont(Font.font("Arial", 7));
        privacy.setFill(Color.web(HINT_CLR));
        privacy.setCursor(Cursor.HAND);
        privacy.setOnMouseEntered(e -> privacy.setUnderline(true));
        privacy.setOnMouseExited(e -> privacy.setUnderline(false));

        box.getChildren().addAll(logoPane, brand, privacy);
        return box;
    }

    /**
     * Crée une petite flèche triangulaire positionnée à (px, py)
     * avec une rotation donnée et une couleur de remplissage.
     */
    private Polygon arrowHead(double px, double py, double rotation, String color) {
        Polygon p = new Polygon(0.0, -5.0, 4.0, 3.0, -4.0, 3.0);
        p.setFill(Color.web(color));
        p.setStroke(Color.TRANSPARENT);
        p.setTranslateX(px);
        p.setTranslateY(py);
        p.setRotate(rotation);
        return p;
    }

    // ────────────────────────────────────────────────────────────────
    // API publique
    // ────────────────────────────────────────────────────────────────

    /** @return true si l'utilisateur a coché la case */
    public boolean estValide() {
        return valide;
    }

    /** Réinitialise le widget (décoché) */
    public void reset() {
        valide = false;
        checkBox.setFill(Color.WHITE);
        checkBox.setStroke(Color.web("#c1c1c1"));
        checkMark.setVisible(false);
    }
}