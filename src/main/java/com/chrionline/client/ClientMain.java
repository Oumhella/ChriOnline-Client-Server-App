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
            // 1. Préparer une requête de test
            Map<String, Object> requete = new HashMap<>();
            requete.put("commande", "CONNEXION");
            requete.put("email", "test@test.com");
            
            System.out.println("[>] Envoi requête de test...");
            client.envoyerRequete(requete);

            // 2. Lire la réponse
            Object reponse = client.lireReponse();
            System.out.println("[<] Réponse du serveur : " + reponse);

            System.out.println("[!] Connexion maintenue ouverte. Appuyez sur Ctrl+C pour quitter.");
            
            // On ne ferme pas la connexion pour tester le multi-client
            while (true) {
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            System.err.println("[✗] Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
