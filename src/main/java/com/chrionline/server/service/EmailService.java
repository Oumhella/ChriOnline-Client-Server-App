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

    // ─── Méthode publique d'envoi ─────────────────────────────────────────────
    
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
