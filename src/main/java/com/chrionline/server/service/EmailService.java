package com.chrionline.server.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Service d'envoi d'emails via SMTP.
 * La configuration est chargée depuis email.properties (classpath serveur).
 * Cette classe ne doit JAMAIS être exposée au client.
 */
public class EmailService {

    private static final Properties smtpProps = new Properties();
    private static String username;
    private static String password;
    private static String fromAddress;
    private static String fromName;

    static {
        try (InputStream in = EmailService.class
                .getClassLoader().getResourceAsStream("email.properties")) {
            if (in == null) {
                System.err.println("[EMAIL] email.properties introuvable dans le classpath.");
                // Ne pas bloquer le démarrage mais log l'erreur
            } else {
                Properties config = new Properties();
                config.load(in);

                username    = config.getProperty("smtp.username");
                password    = config.getProperty("smtp.password");
                fromAddress = config.getProperty("smtp.from");
                fromName    = config.getProperty("smtp.from.name", "ChriOnline");

                smtpProps.put("mail.smtp.auth",            config.getProperty("smtp.auth", "true"));
                smtpProps.put("mail.smtp.starttls.enable", config.getProperty("smtp.starttls", "true"));
                smtpProps.put("mail.smtp.host",            config.getProperty("smtp.host"));
                smtpProps.put("mail.smtp.port",            config.getProperty("smtp.port"));
                smtpProps.put("mail.smtp.ssl.trust",       config.getProperty("smtp.host"));
            }

        } catch (IOException e) {
            System.err.println("[EMAIL] Erreur chargement email.properties : " + e.getMessage());
        }
    }

    // ─── Session ───────────────────────────────────────────────────────────────

    private static Session creerSession() {
        return Session.getInstance(smtpProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    // ─── Emails publics ───────────────────────────────────────────────────────

    /**
     * Envoie un code OTP de confirmation d'inscription.
     */
    public static void envoyerConfirmation(String destinataire, String token) throws MessagingException {
        String sujet = "Confirmez votre inscription — ChriOnline";
        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
              <h2 style="color:#1a1a2e">Bienvenue sur ChriOnline !</h2>
              <p>Votre code de confirmation :</p>
              <div style="font-size:32px;font-weight:bold;letter-spacing:8px;
                          background:#f4f4f4;padding:16px;text-align:center;
                          border-radius:8px">%s</div>
              <p style="color:#888;font-size:12px">Ce code expire dans 24h.
                 Si vous n'avez pas créé ce compte, ignorez cet email.</p>
            </div>
            """.formatted(token);
        envoyer(destinataire, sujet, corps);
    }

    /**
     * Envoie un token de réinitialisation de mot de passe.
     */
    public static void envoyerReset(String destinataire, String token) throws MessagingException {
        String sujet = "Réinitialisation de votre mot de passe — ChriOnline";
        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
              <h2 style="color:#1a1a2e">Réinitialisation de mot de passe</h2>
              <p>Votre code de réinitialisation :</p>
              <div style="font-size:24px;font-weight:bold;letter-spacing:4px;
                          background:#f4f4f4;padding:16px;text-align:center;
                          border-radius:8px;word-break:break-all">%s</div>
              <p style="color:#e74c3c;font-weight:bold">Ce code expire dans 1h.</p>
              <p style="color:#888;font-size:12px">Si vous n'avez pas demandé cette réinitialisation,
                 ignorez cet email — votre mot de passe reste inchangé.</p>
            </div>
            """.formatted(token);
        envoyer(destinataire, sujet, corps);
    }

    /**
     * Envoie une alerte de stock à un administrateur.
     */
    public static void envoyerAlerteStock(String destinataire, String nomProduit, String stock, String seuil) throws MessagingException {
        String sujet = "⚠️ Alerte de Stock — " + nomProduit;
        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
              <h2 style="color:#d32f2f">Alerte de Rupture de Stock</h2>
              <p>Le produit suivant a atteint ou dépassé son seuil d'alerte :</p>
              <div style="background:#f4f4f4;padding:16px;border-radius:8px;border-left:4px solid #d32f2f;">
                <p><strong>Produit :</strong> %s</p>
                <p><strong>Stock restant :</strong> <span style="color:#d32f2f;font-weight:bold;">%s</span></p>
                <p><strong>Seuil d'alerte :</strong> %s</p>
              </div>
              <p style="color:#888;font-size:12px;margin-top:20px;">Veuillez vous connecter au tableau de bord pour réapprovisionner le stock de ce produit.</p>
            </div>
            """.formatted(nomProduit, stock, seuil);
        envoyer(destinataire, sujet, corps);
    }

    /**
     * Envoie le code OTP à 6 chiffres pour l'authentification 2FA.
     */
    public static void envoyerOTP2FA(String destinataire, String codeOTP) throws MessagingException {
        String sujet = "Code de connexion sécurisé — ChriOnline";
        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
              <h2 style="color:#1a1a2e">Vérification de connexion</h2>
              <p>Voici votre code d'authentification unique :</p>
              <div style="font-size:36px;font-weight:bold;letter-spacing:10px;
                          background:#f4f4f4;padding:20px;text-align:center;
                          border-radius:8px;color:#2c3e50;">%s</div>
              <p style="color:#e74c3c;font-weight:bold;margin-top:20px;">Indication de sécurité : Ce code expire dans 5 minutes.</p>
              <p style="color:#888;font-size:12px">Si vous n'êtes pas à l'origine de cette tentative de connexion, veuillez changer votre mot de passe immédiatement.</p>
            </div>
            """.formatted(codeOTP);
        envoyer(destinataire, sujet, corps);
    }

    /**
     * Envoie une notification de déblocage de compte à l'utilisateur.
     */
    public static void envoyerDeblocageCompte(String destinataire) throws MessagingException {
        String sujet = "Votre compte a été réactivé — ChriOnline";
        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
              <h2 style="color:#6B9E7A">Bonne nouvelle !</h2>
              <p>Votre compte ChriOnline a été <strong>réactivé</strong> par un administrateur.</p>
              <div style="background:#f4f4f4;padding:16px;border-radius:8px;border-left:4px solid #6B9E7A;">
                <p style="margin:0">Vous pouvez désormais vous connecter et accéder à l'ensemble de nos services.</p>
              </div>
              <p style="color:#888;font-size:12px;margin-top:20px;">
                Si vous pensez qu'il s'agit d'une erreur, veuillez contacter notre support.<br>
                L'équipe ChriOnline
              </p>
            </div>
            """;
        envoyer(destinataire, sujet, corps);
    }

    // ─── Méthode publique d'envoi ─────────────────────────────────────────────
    
    /**
     * Envoie une confirmation de commande au client.
     */
    public static void envoyerConfirmationCommande(String destinataire, String nomClient, String reference, double total, java.util.List<com.chrionline.shared.dto.LigneCommandeDTO> lignes) throws MessagingException {
        String sujet = "Confirmation de votre commande " + reference + " — ChriOnline";
        
        StringBuilder lignesHtml = new StringBuilder();
        for (com.chrionline.shared.dto.LigneCommandeDTO l : lignes) {
            double sousTotal = l.getSousTotal() > 0 ? l.getSousTotal() : l.getQuantite() * l.getPrixUnitaire();
            lignesHtml.append("<tr>")
                      .append("<td style=\"padding:8px;border-bottom:1px solid #ddd;\">").append(l.getNomProduit()).append("</td>")
                      .append("<td style=\"padding:8px;border-bottom:1px solid #ddd;text-align:center;\">").append(l.getQuantite()).append("</td>")
                      .append("<td style=\"padding:8px;border-bottom:1px solid #ddd;text-align:right;\">").append(String.format("%.2f MAD", sousTotal)).append("</td>")
                      .append("</tr>");
        }

        String corps = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">
              <h2 style="color:#6B9E7A">Merci pour votre commande, %s !</h2>
              <p>Votre commande a été passée avec succès et est actuellement en cours de préparation.</p>
              
              <div style="background:#FDFBF7;padding:20px;border-radius:8px;border:1px solid #E8E0D5;margin:20px 0;">
                <p style="margin-top:0"><strong>Référence de la commande :</strong> <span style="color:#C96B4A;font-size:18px;font-weight:bold">%s</span></p>
                <p style="color:#666;font-size:13px">Conservez cette référence pour suivre l'état de votre commande.</p>
              </div>

              <h3 style="color:#3E2C1E">Récapitulatif de votre commande</h3>
              <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                <thead>
                  <tr style="background:#F5EFE8;text-align:left;">
                    <th style="padding:10px;color:#6B4F3A">Produit</th>
                    <th style="padding:10px;text-align:center;color:#6B4F3A">Qté</th>
                    <th style="padding:10px;text-align:right;color:#6B4F3A">Total</th>
                  </tr>
                </thead>
                <tbody>
                  %s
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="2" style="padding:10px;text-align:right;font-weight:bold;color:#3E2C1E">TOTAL :</td>
                    <td style="padding:10px;text-align:right;font-weight:bold;color:#C96B4A;font-size:16px">%.2f MAD</td>
                  </tr>
                </tfoot>
              </table>
              <p style="color:#888;font-size:12px;margin-top:30px;text-align:center;">
                Si vous avez la moindre question, n'hésitez pas à nous contacter.<br>
                L'équipe ChriOnline
              </p>
            </div>
            """.formatted(nomClient, reference, lignesHtml.toString(), total);
        envoyer(destinataire, sujet, corps);
    }

    // ─── Utilitaires d'envoi ─────────────────────────────────────────────
    
    public static void envoyer(String destinataire, String sujet, String corpsHtml) throws MessagingException {
        if (username == null || password == null) {
            throw new MessagingException("Config SMTP incomplète (email.properties)");
        }
        
        Session session = creerSession();
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
        } catch (Exception e) {
            msg.setFrom(new InternetAddress(fromAddress));
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        msg.setSubject(sujet);
        msg.setContent(corpsHtml, "text/html; charset=utf-8");
        Transport.send(msg);
        System.out.println("[EMAIL] Envoyé à " + destinataire + " — " + sujet);
    }
}
