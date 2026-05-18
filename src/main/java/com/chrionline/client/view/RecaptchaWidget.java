package com.chrionline.client.view;

import com.sun.net.httpserver.HttpServer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import netscape.javascript.JSObject;

import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * Widget reCAPTCHA v2 utilisant l'implémentation HTML officielle.
 * La case checkbox s'affiche sur le formulaire via un masque de rognage (clipping) de 302x78.
 * Cela force Google à calculer un viewport large de 420x580 au chargement, éliminant ainsi les barres de défilement.
 * Si le défi s'ouvre, la WebView est détachée du formulaire et glissée dans un pop-up à la taille idéale du défi.
 */
public class RecaptchaWidget extends HBox {

    private boolean valide = false;
    private String token = null;
    private final WebView webView;
    private final WebEngine webEngine;
    private final JSBridge bridge = new JSBridge();
    private Stage popupStage = null;
    private StackPane popupContainer = null;
    
    // Conteneur de rognage (clipping) pour la case à cocher
    private final StackPane clippingContainer;

    // ── Serveur HTTP local statique pour tromper reCAPTCHA ──
    private static HttpServer server;
    private static int serverPort = -1;

    static {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/captcha", exchange -> {
                try (InputStream in = RecaptchaWidget.class.getResourceAsStream("/recaptcha.html")) {
                    if (in == null) {
                        String err = "Fichier recaptcha.html introuvable.";
                        exchange.sendResponseHeaders(404, err.length());
                        exchange.getResponseBody().write(err.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }
                    byte[] data = in.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, data.length);
                    exchange.getResponseBody().write(data);
                    exchange.getResponseBody().close();
                }
            });
            server.start();
            serverPort = server.getAddress().getPort();
            System.out.println("[reCAPTCHA] Mini-serveur démarré sur http://localhost:" + serverPort);
        } catch (Exception e) {
            System.err.println("[reCAPTCHA] Erreur de démarrage du mini-serveur HTTP: " + e.getMessage());
        }
    }

    public RecaptchaWidget() {
        super();
        setAlignment(Pos.CENTER);
        
        // Taille fixe du HBox dans le formulaire
        setMinSize(302, 78);
        setPrefSize(302, 78);
        setMaxSize(302, 78);
        setStyle("-fx-background-color: transparent;");

        // 1. WebView est TOUJOURS bloqué à 420x580 (min, pref, max)
        // pour empêcher JavaFX de le réduire et forcer Google à utiliser le grand viewport standard.
        webView = new WebView();
        webEngine = webView.getEngine();
        webView.setContextMenuEnabled(false);
        webView.setMinSize(420, 580);
        webView.setPrefSize(420, 580);
        webView.setMaxSize(420, 580);

        // 2. Création du conteneur de rognage (clipping)
        clippingContainer = new StackPane();
        clippingContainer.setMinSize(302, 78);
        clippingContainer.setPrefSize(302, 78);
        clippingContainer.setMaxSize(302, 78);
        clippingContainer.setStyle("-fx-background-color: transparent;");

        // Appliquer un rectangle de rognage pour ne laisser voir que la case à cocher
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(302, 78);
        clippingContainer.setClip(clip);

        // Placer la WebView dans le conteneur rogné, calée en haut au centre
        clippingContainer.getChildren().add(webView);
        StackPane.setAlignment(webView, Pos.TOP_CENTER);

        // Pont JS <-> JavaFX
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("jsConnector", bridge);
            }
        });

        if (serverPort != -1) {
            webEngine.load("http://localhost:" + serverPort + "/captcha");
        } else {
            webEngine.load(getClass().getResource("/recaptcha.html").toExternalForm());
        }

        // Ajouter le conteneur rogné à l'affichage principal
        getChildren().add(clippingContainer);
    }

    public void afficherEnPopup(javafx.stage.Window owner) {
        if (valide) return;
        bridge.showChallengePopup();
    }

    public class JSBridge {
        public void captchaResolved(String t) {
            javafx.application.Platform.runLater(() -> {
                valide = true;
                token = t;
                System.out.println("[reCAPTCHA] Vérifié avec succès.");
                hideChallengePopup();
            });
        }

        public void captchaExpired() {
            javafx.application.Platform.runLater(() -> {
                valide = false;
                token = null;
                System.out.println("[reCAPTCHA] Le token a expiré.");
            });
        }

        /**
         * Déclenché automatiquement par l'observateur JS lorsque Google affiche le défi d'images.
         */
        public void showChallengePopup() {
            javafx.application.Platform.runLater(() -> {
                // Pour éviter "Cannot set owner once stage has been set visible",
                // on détruit et recrée le Stage à chaque apparition.
                if (popupStage != null) {
                    if (popupStage.isShowing()) {
                        popupStage.close();
                    }
                    popupStage = null;
                }

                popupStage = new Stage();
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.setTitle("Vérification de sécurité");
                popupStage.setResizable(false);
                
                popupContainer = new StackPane();
                popupContainer.setStyle("-fx-background-color: #FAF7F2; -fx-padding: 15 10 10 10;");
                
                Scene scene = new Scene(popupContainer, 440, 600);
                popupStage.setScene(scene);
                
                popupStage.setOnCloseRequest(e -> {
                    hideChallengePopup();
                });

                // Positionner au centre de la fenêtre parente active
                if (RecaptchaWidget.this.getScene() != null && RecaptchaWidget.this.getScene().getWindow() != null) {
                    popupStage.initOwner(RecaptchaWidget.this.getScene().getWindow());
                }

                // Détacher la WebView de sa boîte de rognage
                clippingContainer.getChildren().remove(webView);
                
                // Mettre la WebView dans la boîte popup (sans rognage)
                popupContainer.getChildren().add(webView);
                StackPane.setAlignment(webView, Pos.TOP_CENTER);
                
                popupStage.show();
            });
        }

        /**
         * Déclenché lorsque le défi d'images se ferme ou est complété.
         */
        public void hideChallengePopup() {
            javafx.application.Platform.runLater(() -> {
                if (popupStage != null && popupStage.isShowing()) {
                    popupStage.hide();
                    popupStage = null; // Libère le Stage pour la prochaine fois
                }
                
                // Retirer la WebView de la boîte popup
                if (popupContainer != null) {
                    popupContainer.getChildren().remove(webView);
                }
                
                // Ré-attacher la WebView à sa boîte de rognage sur le formulaire
                if (!clippingContainer.getChildren().contains(webView)) {
                    clippingContainer.getChildren().add(webView);
                    StackPane.setAlignment(webView, Pos.TOP_CENTER);
                }
            });
        }
    }

    public boolean estValide() {
        return valide;
    }

    public String getToken() {
        return token;
    }

    public void reset() {
        valide = false;
        token = null;
        try {
            webEngine.executeScript("if (typeof grecaptcha !== 'undefined') { grecaptcha.reset(); }");
        } catch (Exception ignored) {
        }
        bridge.hideChallengePopup();
    }
}