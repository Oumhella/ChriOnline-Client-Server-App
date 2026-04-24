package com.chrionline.admin.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.chrionline.securite.KeyStoreManager;
import com.chrionline.securite.Signer;

/**
 * Fenêtre de connexion Administrateur cachée (Ctrl+Shift+A).
 *
 * Flux :
 * 1. L'admin entre email + mot de passe → "Se connecter"
 * 2. Le serveur vérifie si l'admin a une clé publique en BDD :
 *    - ERREUR_NO_KEY → Premier lancement : on affiche "Confirmer mot de passe",
 *      on génère les clés RSA, on crée admin.jks, on enregistre la clé publique
 *      ET on met à jour le mot de passe en BDD (même mot de passe pour les deux).
 *    - OK + challenge → On signe avec la clé privée locale → session admin.
 */
public class AdminLoginFrame extends Stage {

    private static final String KEYSTORE_FILE = "admin.jks";
    private static final String KEY_ALIAS = "adminKey";

    private TextField txtEmail;
    private PasswordField txtPassword;
    private PasswordField txtConfirmPassword;
    private TextField txtTotpCode;
    private Label lblTitle;
    private Label lblStatus;
    private Button btnAction;
    private VBox root;

    /** true lorsque le serveur a répondu ERREUR_NO_KEY et qu'on attend la confirmation du mdp */
    private boolean pendingInit = false;

    public AdminLoginFrame() {
        setTitle("Accès Admin SÉCURISÉ");

        root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a1a;");

        lblTitle = new Label("Connexion Admin RSA");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.WHITE);

        lblStatus = new Label();
        lblStatus.setTextFill(Color.ORANGE);
        lblStatus.setWrapText(true);

        txtEmail = new TextField();
        txtEmail.setPromptText("Email Administrateur");
        txtEmail.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10;");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mot de passe");
        txtPassword.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10;");

        // Champ de confirmation (caché au départ, affiché uniquement au premier lancement)
        txtConfirmPassword = new PasswordField();
        txtConfirmPassword.setPromptText("Confirmer le mot de passe");
        txtConfirmPassword.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10;");

        txtTotpCode = new TextField();
        txtTotpCode.setPromptText("Code TOTP (Microsoft Authenticator)");
        txtTotpCode.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10;");
        txtTotpCode.setVisible(false);
        txtTotpCode.setManaged(false);

        btnAction = new Button("Se connecter");
        btnAction.setStyle("-fx-background-color: #C96B4A; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnAction.setCursor(javafx.scene.Cursor.HAND);
        btnAction.setOnAction(e -> handleAction());

        // Au départ : pas de champ de confirmation ni TOTP
        root.getChildren().addAll(lblTitle, txtEmail, txtPassword, txtTotpCode, btnAction, lblStatus);
        setScene(new Scene(root, 450, 450));
    }

    private void handleAction() {
        String email = txtEmail.getText().trim();
        String pass  = txtPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            if (pendingInit) {
                // ── Mode Initialisation (2ème clic) ──
                String confirm = txtConfirmPassword.getText();
                if (!pass.equals(confirm)) {
                    lblStatus.setText("Les mots de passe ne correspondent pas.");
                    return;
                }
                runInitialSetup(email, pass);
            } else {
                // ── Mode normal (1er clic) : demander un challenge ──
                checkAccountAndProceed(email, pass);
            }
        } catch (Exception ex) {
            lblStatus.setText("Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Demande un challenge au serveur. Si ERREUR_NO_KEY, passe en mode initialisation.
     */
    private void checkAccountAndProceed(String email, String pass) throws Exception {
        com.chrionline.client.network.Client client =
                com.chrionline.client.network.Client.getInstance("localhost", 12345);
        client.connecter();

        lblStatus.setText("Vérification du compte...");
        Map<String, Object> reqChallenge = new HashMap<>();
        reqChallenge.put("commande", "ADMIN_GET_CHALLENGE");
        reqChallenge.put("email", email);
        reqChallenge.put("mdp", pass); // Envoyer le mot de passe pour vérification serveur

        Map<String, Object> resChallenge = client.envoyerRequeteAttendreReponse(reqChallenge);
        String statut = (String) resChallenge.get("statut");

        if ("ERREUR_NO_KEY".equals(statut)) {
            // ── Premier lancement pour ce compte : afficher le champ de confirmation ──
            switchToInitMode();

        } else if ("OK".equals(statut)) {
            // ── Compte déjà initialisé : signer le challenge ──
            String challenge = (String) resChallenge.get("challenge");
            runChallengeLogin(client, email, pass, challenge);

        } else if ("ERREUR_BLOQUE".equals(statut)) {
            String msg = (String) resChallenge.get("message");
            lblStatus.setText(msg);
            lblStatus.setTextFill(Color.RED);
            // Optionnel: désactiver le bouton pendant X secondes
            if (resChallenge.containsKey("delaySeconds")) {
                long delay = ((Number) resChallenge.get("delaySeconds")).longValue();
                btnAction.setDisable(true);
                new Thread(() -> {
                    try { Thread.sleep(delay * 1000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> btnAction.setDisable(false));
                }).start();
            }
        } else {
            lblStatus.setText("Erreur : " + resChallenge.get("message"));
            lblStatus.setTextFill(Color.ORANGE);
        }
    }

    /**
     * Affiche le formulaire d'initialisation (ajoute le champ de confirmation).
     */
    private void switchToInitMode() {
        pendingInit = true;
        lblTitle.setText("Initialisation Sécurité Admin");
        lblStatus.setText("Premier lancement détecté !\nChoisissez un mot de passe qui protégera votre coffre-fort de clés.");
        btnAction.setText("Générer les clés et Initialiser");

        // Ajouter le champ de confirmation AVANT le bouton
        if (!root.getChildren().contains(txtConfirmPassword)) {
            int btnIndex = root.getChildren().indexOf(btnAction);
            root.getChildren().add(btnIndex, txtConfirmPassword);
        }
    }

    /**
     * Premier lancement : crée admin.jks via keytool (vrai certificat X.509),
     * lit la clé publique depuis le keystore créé, et synchronise avec le serveur.
     * Le mot de passe est le MÊME pour le keystore ET la BDD.
     */
    private void runInitialSetup(String email, String pass) throws Exception {
        lblStatus.setText("Génération des clés RSA via keytool (2048 bits)...");

        // 1. Créer le KeyStore via keytool (génère un vrai certificat X.509 auto-signé)
        //    Le mot de passe choisi protège à la fois le fichier JKS et la clé privée
        KeyStoreManager.createKeyStore(KEYSTORE_FILE, pass, KEY_ALIAS, null);

        // 2. Lire la clé publique depuis le keystore fraîchement créé
        java.security.PublicKey pubKey = KeyStoreManager.extractPublicKey(KEYSTORE_FILE, pass, KEY_ALIAS);
        String pubKeyBase64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());

        // 3. Envoyer au serveur : le serveur hashera le mdp (BCrypt) et stockera la clé publique
        com.chrionline.client.network.Client client =
                com.chrionline.client.network.Client.getInstance("localhost", 12345);
        client.connecter();

        Map<String, Object> reqInit = new HashMap<>();
        reqInit.put("commande", "ADMIN_INIT_SECURITY");
        reqInit.put("email", email);
        reqInit.put("mdp", pass);
        reqInit.put("publicKey", pubKeyBase64);

        Map<String, Object> resInit = client.envoyerRequeteAttendreReponse(reqInit);

        if ("OK".equals(resInit.get("statut"))) {
            lblStatus.setText("Clés générées et mot de passe synchronisé !");

            if (resInit.containsKey("otpauthUri")) {
                String uri = (String) resInit.get("otpauthUri");
                String totpSecret = (String) resInit.get("totpSecret");
                showQrCodePopup(uri, totpSecret, () -> continueLoginAfterSetup(client, email, pass));
            } else {
                continueLoginAfterSetup(client, email, pass);
            }
        } else {
            // Échec serveur → supprimer le keystore local orphelin
            new File(KEYSTORE_FILE).delete();
            lblStatus.setText("Erreur serveur : " + resInit.get("message"));
        }
    }

    private void continueLoginAfterSetup(com.chrionline.client.network.Client client, String email, String pass) {
        try {
            // 4. Auto-connexion immédiate
            Map<String, Object> reqChallenge = new HashMap<>();
            reqChallenge.put("commande", "ADMIN_GET_CHALLENGE");
            reqChallenge.put("email", email);
            reqChallenge.put("mdp", pass); // Requis par le serveur pour vérifier le 1er facteur
            Map<String, Object> resChallenge = client.envoyerRequeteAttendreReponse(reqChallenge);

            if ("OK".equals(resChallenge.get("statut"))) {
                runChallengeLogin(client, email, pass, (String) resChallenge.get("challenge"));
            } else {
                lblStatus.setText("Init OK mais échec challenge : " + resChallenge.get("message"));
            }
        } catch (Exception e) {
            lblStatus.setText("Erreur post-init : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Connexion par challenge-response RSA.
     * Le mot de passe ouvre le keystore local → signe le challenge → envoie la signature.
     */
    @SuppressWarnings("unchecked")
    private void runChallengeLogin(com.chrionline.client.network.Client client,
                                   String email, String pass, String challenge) throws Exception {
        File ksFile = new File(KEYSTORE_FILE);
        if (!ksFile.exists()) {
            lblStatus.setText("Keystore introuvable sur cette machine.\n" +
                    "Contactez un administrateur pour réinitialiser votre clé.");
            return;
        }

        lblStatus.setText("Signature du défi...");

        // 1. Ouvrir le keystore avec le mot de passe et récupérer la clé privée
        java.security.PrivateKey privateKey = KeyStoreManager.getPrivateKey(
                KEYSTORE_FILE, pass, KEY_ALIAS, pass);

        // 2. Signer le challenge
        byte[] signature = Signer.sign(challenge, privateKey);
        String sigBase64 = Base64.getEncoder().encodeToString(signature);

        // 3. Envoyer la signature au serveur
        Map<String, Object> reqLogin = new HashMap<>();
        reqLogin.put("commande", "ADMIN_LOGIN_CHALLENGE");
        reqLogin.put("email", email);
        reqLogin.put("signature", sigBase64);

        Map<String, Object> resLogin = client.envoyerRequeteAttendreReponse(reqLogin);

        if ("REQUIRES_TOTP".equals(resLogin.get("statut"))) {
            lblStatus.setText("Signature valide. Veuillez entrer le code TOTP.");
            lblStatus.setTextFill(Color.web("#6B9E7A")); // Vert

            // Mettre à jour l'interface pour demander le code TOTP
            txtEmail.setVisible(false);
            txtEmail.setManaged(false);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            
            txtTotpCode.setVisible(true);
            txtTotpCode.setManaged(true);
            
            btnAction.setText("Vérifier TOTP");
            btnAction.setOnAction(e -> runTotpVerification(client, email));
        } else if ("OK".equals(resLogin.get("statut"))) {
            lblStatus.setText("Authentification RSA réussie !");
            Map<String, Object> data = (Map<String, Object>) resLogin.get("data");
            com.chrionline.client.session.SessionManager.getInstance().setUser(data);

            AdminDashboardView dashboard = new AdminDashboardView();
            dashboard.start(new Stage());
            this.close();
        } else if ("ERREUR_BLOQUE".equals(resLogin.get("statut"))) {
            String msg = (String) resLogin.get("message");
            lblStatus.setText(msg);
            lblStatus.setTextFill(Color.RED);
            if (resLogin.containsKey("delaySeconds")) {
                long delay = ((Number) resLogin.get("delaySeconds")).longValue();
                btnAction.setDisable(true);
                new Thread(() -> {
                    try { Thread.sleep(delay * 1000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> btnAction.setDisable(false));
                }).start();
            }
        } else {
            lblStatus.setText("Échec : " + resLogin.get("message"));
            lblStatus.setTextFill(Color.ORANGE);
        }
    }

    /**
     * Affiche le QR Code pour configurer Microsoft Authenticator.
     */
    private void showQrCodePopup(String otpauthUri, String totpSecret, Runnable onComplete) {
        javafx.application.Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Configuration 2FA - Microsoft Authenticator");

            VBox layout = new VBox(15);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(20));
            layout.setStyle("-fx-background-color: #1a1a1a;");

            Label info = new Label("1. Ouvrez Microsoft Authenticator\n2. Ajoutez un compte professionnel/scolaire\n3. Scannez ce QR Code :");
            info.setTextFill(Color.WHITE);
            info.setWrapText(true);

            // Charger l'image depuis l'API publique (quickchart.io est plus fiable)
            try {
                String qrUrl = "https://quickchart.io/qr?size=250&text=" + 
                               java.net.URLEncoder.encode(otpauthUri, "UTF-8");
                
                // backgroundLoading = false pour forcer le chargement immédiat et lever une exception si erreur
                Image qrImage = new Image(qrUrl, false);
                
                if (qrImage.isError()) {
                    throw new Exception("Erreur interne Image JavaFX", qrImage.getException());
                }
                
                ImageView qrView = new ImageView(qrImage);
                layout.getChildren().add(qrView);
            } catch (Exception e) {
                Label err = new Label("Erreur de chargement du QR Code (vérifiez votre connexion).\nEntrez le secret manuel ci-dessous dans votre application.");
                err.setTextFill(Color.RED);
                err.setWrapText(true);
                err.setAlignment(Pos.CENTER);
                layout.getChildren().add(err);
            }

            Label manual = new Label("Secret manuel : " + totpSecret);
            manual.setTextFill(Color.ORANGE);
            manual.setStyle("-fx-font-weight: bold;");

            Button btnDone = new Button("J'ai scanné le code");
            btnDone.setStyle("-fx-background-color: #6B9E7A; -fx-text-fill: white; -fx-font-weight: bold;");
            btnDone.setOnAction(e -> {
                popup.close();
                onComplete.run();
            });

            layout.getChildren().addAll(info, manual, btnDone);
            popup.setScene(new Scene(layout, 350, 450));
            popup.showAndWait();
        });
    }

    /**
     * Vérification finale du code TOTP
     */
    @SuppressWarnings("unchecked")
    private void runTotpVerification(com.chrionline.client.network.Client client, String email) {
        String code = txtTotpCode.getText().trim();
        if (code.length() != 6) {
            lblStatus.setText("Veuillez entrer les 6 chiffres.");
            lblStatus.setTextFill(Color.ORANGE);
            return;
        }

        lblStatus.setText("Vérification...");
        lblStatus.setTextFill(Color.WHITE);
        btnAction.setDisable(true);

        new Thread(() -> {
            try {
                Map<String, Object> req = new HashMap<>();
                req.put("commande", "ADMIN_VERIFY_TOTP");
                req.put("email", email);
                req.put("totpCode", code);

                Map<String, Object> res = client.envoyerRequeteAttendreReponse(req);

                javafx.application.Platform.runLater(() -> {
                    btnAction.setDisable(false);
                    if ("OK".equals(res.get("statut"))) {
                        lblStatus.setText("Authentification réussie !");
                        lblStatus.setTextFill(Color.web("#6B9E7A"));

                        Map<String, Object> data = (Map<String, Object>) res.get("data");
                        com.chrionline.client.session.SessionManager.getInstance().setUser(data);

                        AdminDashboardView dashboard = new AdminDashboardView();
                        dashboard.start(new Stage());
                        this.close();
                    } else {
                        lblStatus.setText("Code invalide : " + res.get("message"));
                        lblStatus.setTextFill(Color.RED);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    btnAction.setDisable(false);
                    lblStatus.setText("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }
}
