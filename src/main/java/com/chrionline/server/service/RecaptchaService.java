package com.chrionline.server.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * Service pour vérifier les tokens reCAPTCHA auprès de Google.
 * Utilise la clé secrète du serveur pour la validation.
 */
public class RecaptchaService {

    private static String secretKey;
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    static {
        try {
            java.util.Map<String, String> config = com.chrionline.securite.VaultServerService.getServerConfig();
            secretKey = config.get("recaptcha_secret");
            if (secretKey == null) {
                System.err.println("[RECAPTCHA] Clé secrète introuvable dans Vault (secret/server/config -> recaptcha_secret).");
            } else {
                System.out.println("[RECAPTCHA] Clé secrète chargée depuis Vault.");
            }
        } catch (Exception e) {
            System.err.println("[RECAPTCHA] Erreur chargement Vault : " + e.getMessage());
        }
    }

    /**
     * Vérifie si le token envoyé par le client est valide auprès de l'API Google.
     * @param responseToken Le token généré par le widget reCAPTCHA côté client.
     * @return true si la vérification est réussie.
     */
    public static boolean verify(String responseToken) {
        if (responseToken == null || responseToken.isBlank() || secretKey == null) {
            return false;
        }

        try {
            // Requête vers Google : https://www.google.com/recaptcha/api/siteverify
            String params = "secret=" + secretKey + "&response=" + responseToken;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.google.com/recaptcha/api/siteverify"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Pour des raisons de simplicité sans ajouter de bibliothèque JSON (GSON/Jackson),
            // on vérifie simplement la présence de `"success": true` dans le corps de la réponse.
            // (Standard professionnel pour les projets sans frameworks lourds)
            return response.body().contains("\"success\": true");

        } catch (Exception e) {
            System.err.println("[RECAPTCHA] Échec de la vérification : " + e.getMessage());
            return false;
        }
    }
}
