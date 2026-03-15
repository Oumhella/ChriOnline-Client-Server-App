package com.chrionline.client;

import com.chrionline.client.network.Client;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for the JavaFX Client application.
 * ⚠️ TEST TEMPORAIRE — Simulation d'échange de données.
 */
public class ClientMain {
    public static void main(String[] args) {
        System.out.println("=== ChriOnline Client — Test de Protocole ===");
        try {
            Client client = Client.getInstance("localhost", 12345);
            client.connecter();
            System.out.println("[✓] Connecté au serveur.");

            // 1. Préparer une requête (ex: test de connexion)
            Map<String, Object> requete = new HashMap<>();
            requete.put("commande", "CONNEXION");
            requete.put("login", "admin");
            requete.put("motDePasse", "admin123");

            System.out.println("[>] Envoi requête CONNEXION...");
            client.envoyerRequete(requete);

            // 2. Lire la réponse
            Object reponse = client.lireReponse();
            System.out.println("[<] Réponse reçue : " + reponse);

            // Pause pour voir les logs
            Thread.sleep(1000);

            client.deconnecter();
            System.out.println("[✓] Test fini.");

        } catch (Exception e) {
            System.err.println("[✗] Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
