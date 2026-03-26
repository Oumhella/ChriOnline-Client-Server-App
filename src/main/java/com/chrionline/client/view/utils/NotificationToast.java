package com.chrionline.client.view.utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Utilitaire pour afficher des notifications de type "Toast" (glissantes).
 */
public class NotificationToast {

    private static final String TERRACOTTA = "#C96B4A";
    private static final String CREME      = "#FDFBF7";

    public static void show(Stage owner, String message) {
        Popup popup = new Popup();
        
        HBox toast = new HBox(15);
        toast.setPadding(new Insets(15, 20, 15, 20));
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setStyle(
            "-fx-background-color: " + CREME + ";" +
            "-fx-border-color: " + TERRACOTTA + ";" +
            "-fx-border-width: 0 0 0 5;" +
            "-fx-background-radius: 4 10 10 4;" +
            "-fx-border-radius: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );

        Label icon = new Label("🔔");
        icon.setFont(Font.font(20));
        
        VBox content = new VBox(2);
        Label title = new Label("Notification");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        title.setTextFill(Color.web(TERRACOTTA));
        
        Label msg = new Label(message);
        msg.setFont(Font.font("Georgia", 13));
        msg.setWrapText(true);
        msg.setMaxWidth(300);
        
        content.getChildren().addAll(title, msg);
        toast.getChildren().addAll(icon, content);
        
        popup.getContent().add(toast);

        // Positionnement (en bas à droite de la fenêtre parente)
        popup.setOnShown(e -> {
            popup.setX(owner.getX() + owner.getWidth() - toast.getWidth() - 30);
            popup.setY(owner.getY() + owner.getHeight() - toast.getHeight() - 30);
            
            // Animation d'entrée (Slide in)
            toast.setTranslateX(50);
            toast.setOpacity(0);
            
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(toast.translateXProperty(), 50),
                    new KeyValue(toast.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(400),
                    new KeyValue(toast.translateXProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(toast.opacityProperty(), 1, Interpolator.EASE_OUT)
                )
            );
            timeline.play();
        });

        popup.show(owner);

        // Auto-close après 5 secondes
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> {
            Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(500),
                    new KeyValue(toast.opacityProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(toast.translateXProperty(), 20, Interpolator.EASE_IN)
                )
            );
            fadeOut.setOnFinished(f -> popup.hide());
            fadeOut.play();
        });
        delay.play();
    }
}
