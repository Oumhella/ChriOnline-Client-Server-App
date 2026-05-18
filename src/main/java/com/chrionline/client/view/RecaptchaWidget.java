package com.chrionline.client.view;

import com.sun.net.httpserver.HttpServer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Widget reCAPTCHA v2 utilisant l'implémentation HTML officielle.
 * Utilise un mini-serveur HTTP local pour contourner le blocage du domaine
 * (file://) de Google.
 */
public class RecaptchaWidget extends HBox {

    private boolean valide = false;
    private String token = null;
    private final WebView webView;
    private final WebEngine webEngine;
    private final JSBridge bridge = new JSBridge(); // RÉTABLI : Référence forte obligatoire contre le Garbage Collector !
    private javafx.stage.Stage popupStage = null;

    // ── Serveur HTTP local statique pour tromper reCAPTCHA ──
    private static HttpServer server;
    private static int serverPort = -1;

    static {
        try {
            // Port = 0 laisse l'OS choisir un port libre, évitant les conflits
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/captcha", exchange -> {
                try (InputStream in = RecaptchaWidget.class.getResourceAsStream("/recaptcha.html")) {
                    if (in == null) {
                        exchange.sendResponseHeaders(404, -1);
                        exchange.close();
                        return;
                    }
                    Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
                    String html = s.hasNext() ? s.next() : "";

                    byte[] bytes = html.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                } catch (Exception ex) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                }
            });
            // Désactiver l'exécuteur explicite pour utiliser le thread par défaut
            server.setExecutor(null);
            server.start();
            serverPort = server.getAddress().getPort();
            System.out.println("[reCAPTCHA] Mini-serveur démarré sur http://localhost:" + serverPort);
        } catch (Exception e) {
            System.err.println("[reCAPTCHA] Erreur de démarrage du mini-serveur HTTP: " + e.getMessage());
        }
    }

    public RecaptchaWidget() {
        super();
        setAlignment(Pos.TOP_CENTER);
        setPrefSize(440, 600);
        setStyle("-fx-background-color: #FAF7F2; -fx-padding: 15 10 10 10;");

        webView = new WebView();
        webEngine = webView.getEngine();

        webView.setContextMenuEnabled(false);
        webView.setPrefSize(420, 580);
        webView.setMaxSize(420, 580);

        // Pont JS <-> JavaFX
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("jsConnector", bridge);
            }
        });

        // Chargement via le VRAI protocole HTTP local (plus d'erreur de domaine de
        // Google!)
        if (serverPort != -1) {
            webEngine.load("http://localhost:" + serverPort + "/captcha");
        } else {
            // Fallback si le serveur plante
            webEngine.load(getClass().getResource("/recaptcha.html").toExternalForm());
        }

        getChildren().add(webView);
    }

    public void afficherEnPopup(javafx.stage.Window owner) {
        if (valide) {
            return; // Déjà validé, rien à faire
        }
        
        if (popupStage == null) {
            popupStage = new javafx.stage.Stage();
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(owner);
            popupStage.setTitle("Vérification de sécurité");
            
            // Retirer le webview de son parent s'il y est déjà pour éviter l'erreur de double parent
            if (this.getParent() != null) {
                ((javafx.scene.layout.Pane) this.getParent()).getChildren().remove(this);
            }
            
            javafx.scene.Scene scene = new javafx.scene.Scene(this, 440, 600);
            popupStage.setScene(scene);
            popupStage.setResizable(false);
            
            // Reset à la fermeture manuelle si non validé
            popupStage.setOnCloseRequest(e -> {
                if (!valide) {
                    reset();
                }
            });
        }
        popupStage.showAndWait();
    }

    public class JSBridge {
        public void captchaResolved(String t) {
            javafx.application.Platform.runLater(() -> {
                valide = true;
                token = t;
                System.out.println("[reCAPTCHA] Vérifié avec succès.");
                if (popupStage != null && popupStage.isShowing()) {
                    popupStage.close();
                }
            });
        }

        public void captchaExpired() {
            javafx.application.Platform.runLater(() -> {
                valide = false;
                token = null;
                System.out.println("[reCAPTCHA] Le token a expiré.");
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
    }
}