package com.chrionline;

import com.chrionline.server.security.SecurityLogger;
import com.chrionline.server.dao.SecurityBlacklistDAO;
import com.chrionline.shared.models.SecurityEvent;

import java.util.List;
import java.util.Map;

/**
 * Programme de test d'intégration pour valider le moteur IDS/IPS de ChriOnline.
 * Ce script simule les différents scénarios d'attaque décrits dans le sujet et
 * vérifie que le moteur IDS lève bien les alertes et que l'IPS applique les contre-mesures.
 *
 * Lancez ce fichier directement depuis votre IDE (IntelliJ IDEA) en faisant un clic droit
 * sur la classe puis "Run 'SecurityTestApp.main()'".
 */
public class SecurityTestApp {

    private static final String TEST_IP = "198.51.100.42"; // IP de test isolée (non-locale pour éviter le skip)
    private static final String TEST_EMAIL = "victim@chrionline.com";
    private static final String TEST_ADMIN_EMAIL = "admin_super@chrionline.com";

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("🛡️ DÉMARRAGE DE LA SIMULATION DU MOTEUR IDS/IPS DE CHRIONLINE 🛡️");
        System.out.println("================================================================\n");

        // Nettoyage initial d'éventuels résidus de tests précédents
        SecurityBlacklistDAO.unlockIp(TEST_IP);
        SecurityLogger.unblockIP(TEST_IP);

        try {
            // ----------------------------------------------------------------
            // SCÉNARIO 1 : Attaque par Force Brute (IDS Cas 1 & IPS Auto-Ban)
            // ----------------------------------------------------------------
            testScenarioForceBrute();

            System.out.println("\n----------------------------------------------------------------\n");

            // ----------------------------------------------------------------
            // SCÉNARIO 2 : OTP Suspect (IDS Cas 2)
            // ----------------------------------------------------------------
            testScenarioOtpSuspect();

            System.out.println("\n----------------------------------------------------------------\n");



            // ----------------------------------------------------------------
            // SCÉNARIO 4 : Activité Admin Anormale - Consultation Massive (IDS Cas 3b)
            // ----------------------------------------------------------------
            testScenarioAdminMassiveRead();

            System.out.println("\n================================================================");
            System.out.println("📊 RÉSULTAT GLOBAL DE LA SURVEILLANCE DU DASHBOARD (RÉCENT)");
            System.out.println("================================================================\n");
            
            afficherDashboardMemoire();

        } finally {
            // NETTOYAGE : Déverrouiller l'IP de test pour laisser le système propre
            System.out.println("\n🧹 Nettoyage de la base de données de test...");
            SecurityBlacklistDAO.unlockIp(TEST_IP);
            SecurityLogger.unblockIP(TEST_IP);
            System.out.println("✅ IP de test débloquée. Simulation terminée avec succès !");
        }
    }

    private static void testScenarioForceBrute() {
        System.out.println("🔥 [SCÉNARIO 1] Simulation d'une attaque par Force Brute");
        System.out.println("👉 Règle IDS : Plus de 3 échecs de connexion en moins d'une minute.");
        System.out.println("👉 Action IPS attendue : Blocage automatique temporaire (15 min) de l'IP.");

        System.out.println("\nAction : L'attaquant effectue 3 tentatives de connexion échouées...");
        for (int i = 1; i <= 3; i++) {
            System.out.println("  ❌ Tentative d'échec #" + i + "...");
            SecurityLogger.loginEchec(TEST_EMAIL, TEST_IP);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // Vérification
        boolean isBannedInDB = SecurityBlacklistDAO.isIpBlacklisted(TEST_IP);
        boolean isBannedInRAM = SecurityLogger.isBlacklisted(TEST_IP);
        long remaining = SecurityBlacklistDAO.getRemainingSeconds(TEST_IP);

        System.out.println("\n[RÉSULTATS DU TEST 1] :");
        System.out.println("  - IP bannie en BDD MySQL ? " + (isBannedInDB ? "✅ OUI" : "❌ NON"));
        System.out.println("  - IP bannie en RAM (Serveur) ? " + (isBannedInRAM ? "✅ OUI" : "❌ NON"));
        System.out.println("  - Durée restante de l'IPS : " + (remaining / 60) + " minutes et " + (remaining % 60) + " secondes.");

        if (isBannedInDB && isBannedInRAM) {
            System.out.println("🌟 SUCCÈS : L'IDS a détecté la force brute et l'IPS a banni l'IP !");
        } else {
            System.err.println("⚠️ ÉCHEC : Le blocage n'a pas fonctionné.");
        }
    }

    private static void testScenarioOtpSuspect() {
        System.out.println("🔥 [SCÉNARIO 2] Simulation de tentatives OTP suspectes");
        System.out.println("👉 Règle IDS : 2 OTP invalides successifs consécutifs.");
        System.out.println("👉 Action IDS attendue : Journalisation + Levée d'alerte OTP_SUSPECT.");

        System.out.println("\nAction : L'attaquant saisit 2 codes OTP incorrects de suite...");
        SecurityLogger.otpEchec(TEST_EMAIL, TEST_IP);
        SecurityLogger.otpEchec(TEST_EMAIL, TEST_IP);

        // Vérification dans l'historique
        List<SecurityEvent> events = SecurityLogger.getRecentEvents();
        boolean alertFound = false;
        for (SecurityEvent ev : events) {
            if ("IDS_ALERT_OTP_SUSPECT".equals(ev.getType())) {
                alertFound = true;
                System.out.println("  🚨 Alerte interceptée : " + ev);
                break;
            }
        }

        System.out.println("\n[RÉSULTATS DU TEST 2] :");
        System.out.println("  - Alerte IDS_ALERT_OTP_SUSPECT générée ? " + (alertFound ? "✅ OUI" : "❌ NON"));

        if (alertFound) {
            System.out.println("🌟 SUCCÈS : La détection d'OTP suspect fonctionne parfaitement !");
        } else {
            System.err.println("⚠️ ÉCHEC : L'alerte OTP suspect n'a pas été levée.");
        }
    }



    private static void testScenarioAdminMassiveRead() {
        System.out.println("🔥 [SCÉNARIO 4] Simulation d'une consultation massive de données par un Admin");
        System.out.println("👉 Règle IDS : Plus de 5 accès aux données utilisateurs en moins d'une minute.");
        System.out.println("👉 Action IDS attendue : Levée d'alerte ADMIN_MASSIVE_READ.");

        System.out.println("\nAction : L'administrateur interroge la liste des clients 5 fois consécutives...");
        for (int i = 1; i <= 5; i++) {
            System.out.println("  🔍 Requête de lecture des données #" + i + "...");
            SecurityLogger.trackAdminDataAccess(TEST_ADMIN_EMAIL, TEST_IP);
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        // Vérification dans l'historique
        List<SecurityEvent> events = SecurityLogger.getRecentEvents();
        boolean alertFound = false;
        for (SecurityEvent ev : events) {
            if ("IDS_ALERT_ADMIN_MASSIVE_READ".equals(ev.getType())) {
                alertFound = true;
                System.out.println("  🚨 Alerte interceptée : " + ev);
                break;
            }
        }

        System.out.println("\n[RÉSULTATS DU TEST 4] :");
        System.out.println("  - Alerte IDS_ALERT_ADMIN_MASSIVE_READ générée ? " + (alertFound ? "✅ OUI" : "❌ NON"));

        if (alertFound) {
            System.out.println("🌟 SUCCÈS : La détection de vol/lecture massive de données fonctionne parfaitement !");
        } else {
            System.err.println("⚠️ ÉCHEC : L'alerte de consultation massive n'a pas été levée.");
        }
    }

    private static void afficherDashboardMemoire() {
        List<SecurityEvent> events = SecurityLogger.getRecentEvents();
        System.out.println("Visualisation des 10 dernières alertes enregistrées en mémoire pour l'IHM :");
        int limit = Math.min(10, events.size());
        for (int i = 0; i < limit; i++) {
            System.out.println("  " + (i + 1) + ". " + events.get(i));
        }

        System.out.println("\nVisualisation des IPs bloquées actives :");
        List<Map<String, Object>> blocked = SecurityBlacklistDAO.getAllActiveBlacklist();
        if (blocked.isEmpty()) {
            System.out.println("  (Aucune IP bloquée active)");
        } else {
            for (Map<String, Object> map : blocked) {
                System.out.println("  🔒 IP: " + map.get("ip") + " | Raison: " + map.get("raison") + " | Expire: " + map.get("expire_le"));
            }
        }
    }
}
