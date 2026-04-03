package com.chrionline.admin.view;

import com.chrionline.admin.controller.AdminCommandesController;
import com.chrionline.shared.dto.CommandeDTO;
import com.chrionline.shared.dto.LigneCommandeDTO;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommandesView extends Application {

    // ─── Palette du thème (identique AdminDashboardView) ───────────────────────
    private static final String CREME       = "#FDFBF7";
    private static final String CREME_CARD  = "#FFFEFB";
    private static final String CREME_INPUT = "#F5EFE8";
    private static final String BORDER      = "#E8E0D5";
    private static final String BRUN        = "#3E2C1E";
    private static final String BRUN_MED    = "#6B4F3A";
    private static final String BRUN_LIGHT  = "#9A7B65";
    private static final String SAUGE_DARK  = "#6B9E7A";
    private static final String TERRACOTTA  = "#C96B4A";
    private static final String WARNING     = "#D4920A";
    private static final String WARNING_BG  = "#FDF3DC";
    private static final String DANGER      = "#B03A2E";
    private static final String DANGER_BG   = "#FBEAEA";
    private static final String INFO        = "#2471A3";
    private static final String INFO_BG     = "#E8F4FB";
    private static final String SUCCESS_BG  = "#EAF5ED";

    // ─── État de la vue ─────────────────────────────────────────────────────────
    private AdminCommandesController controller;
    private List<CommandeDTO>         toutesLesCommandes = new ArrayList<>();
    private VBox                      tableContainer;
    private Label                     toastLabel;
    private String                    filtreStatut    = "TOUS";
    private String                    recherche       = "";
    private boolean                   triDateDesc     = true;   // true = récent→ancien

    // ─── Boutons de filtre (pour les activer/désactiver visuellement) ───────────
    private final List<Button> filtresBtns = new ArrayList<>();

    /**
     * Construit et retourne le contenu de la vue Commandes sous forme de Node.
     * Utilisé pour l'intégrer dans le layout principal sans ouvrir une nouvelle fenêtre.
     */
    public Node getView() {
        this.controller = new AdminCommandesController();

        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 34, 34, 34));

        // 1. HEADER (no stage reference needed when embedded)
        content.getChildren().add(buildHeader(null));

        // 2. BARRE RECHERCHE + FILTRES
        content.getChildren().add(buildFiltresRow());

        // 4. TABLE
        VBox panel = new VBox(0);
        panel.setStyle(
            "-fx-background-color: " + CREME_CARD + ";" +
            "-fx-background-radius: 10; -fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 10; -fx-border-width: 1;"
        );
        panel.setEffect(new DropShadow(7, Color.web(BRUN, 0.05)));
        panel.getChildren().add(buildTableHeader());

        tableContainer = new VBox(0);
        panel.getChildren().add(tableContainer);
        content.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
            "-fx-background: " + CREME + ";" +
            "-fx-background-color: transparent; -fx-border-color: transparent;"
        );

        chargerDonnees();
        return scroll;
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + CREME + ";");
        root.setCenter(getView());

        Scene scene = new Scene(root, 1100, 750);
        stage.setTitle("ChriOnline — Gestion des Commandes");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox buildHeader(Stage stage) {
        VBox titleBlock = new VBox(3);
        Text titre = new Text("Gestion des Commandes");
        titre.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        titre.setFill(Color.web(BRUN));
        Text sousTitre = new Text("Consultez, filtrez et mettez à jour les statuts de livraison");
        sousTitre.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        sousTitre.setFill(Color.web(BRUN_LIGHT));
        titleBlock.getChildren().addAll(titre, sousTitre);

        Button btnRefresh = new Button("↻  Actualiser");
        btnRefresh.setFont(Font.font("Georgia", 12));
        btnRefresh.setStyle(
            "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 8 16; -fx-cursor: hand;"
        );
        btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(
            "-fx-background-color: " + CREME_INPUT + "; -fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6; -fx-text-fill: " + BRUN + "; -fx-padding: 8 16; -fx-cursor: hand;"
        ));
        btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(
            "-fx-background-color: transparent; -fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6; -fx-text-fill: " + BRUN_MED + "; -fx-padding: 8 16; -fx-cursor: hand;"
        ));
        btnRefresh.setOnAction(e -> chargerDonnees());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titleBlock, spacer, btnRefresh);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════════
    //  BARRE : RECHERCHE + FILTRES STATUT
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox buildFiltresRow() {
        // ── Barre de recherche ───────────────────────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher par ID ou nom client...");
        searchField.setFont(Font.font("Georgia", 13));
        searchField.setStyle(
            "-fx-background-color: " + CREME_CARD + ";" +
            "-fx-border-color: " + BORDER + "; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-padding: 9 14; -fx-text-fill: " + BRUN + ";"
        );
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, old, nv) -> {
            recherche = nv.trim().toLowerCase();
            appliquerFiltres();
        });

        // ── Boutons filtres statut ────────────────────────────────────────────────
        HBox filtresBox = new HBox(6);
        filtresBox.setAlignment(Pos.CENTER_LEFT);

        String[][] filtres = {
            {"Tous", "TOUS"},
            {"En préparation", "EN_PREPARATION"},
            {"Expédiée", "EXPEDIEE"},
            {"Livrée", "LIVREE"},
            {"Annulée", "ANNULEE"}
        };

        for (String[] f : filtres) {
            Button btn = new Button(f[0]);
            btn.setFont(Font.font("Georgia", 12));
            btn.setUserData(f[1]);
            appliquerStyleFiltre(btn, f[1].equals("TOUS")); // "Tous" actif par défaut
            btn.setOnAction(e -> {
                filtreStatut = (String) btn.getUserData();
                filtresBtns.forEach(b -> appliquerStyleFiltre(b, b.getUserData().equals(filtreStatut)));
                appliquerFiltres();
            });
            filtresBtns.add(btn);
            filtresBox.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, searchField, spacer, filtresBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void appliquerStyleFiltre(Button btn, boolean actif) {
        if (actif) {
            btn.setStyle(
                "-fx-background-color: " + SAUGE_DARK + "; -fx-text-fill: white;" +
                "-fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-family: 'Georgia';"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: " + CREME_INPUT + "; -fx-text-fill: " + BRUN_MED + ";" +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 20; -fx-background-radius: 20;" +
                "-fx-padding: 6 14; -fx-cursor: hand; -fx-font-family: 'Georgia';"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EN-TÊTE DU TABLEAU AVEC TRI PAR DATE
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox buildTableHeader() {
        HBox header = new HBox(0);
        header.setPadding(new Insets(11, 20, 11, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: " + CREME_INPUT + ";" +
            "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        // Bouton de tri sur la colonne Date
        Button btnTriDate = new Button("Date ↓");
        btnTriDate.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        btnTriDate.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: " + BRUN_MED + ";" +
            "-fx-cursor: hand; -fx-padding: 0; -fx-border-color: transparent;"
        );
        btnTriDate.setOnAction(e -> {
            triDateDesc = !triDateDesc;
            btnTriDate.setText("Date " + (triDateDesc ? "↓" : "↑"));
            appliquerFiltres();
        });

        HBox dateCell = new HBox(btnTriDate);
        dateCell.setMinWidth(180); dateCell.setPrefWidth(180);
        dateCell.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(
            headerCell("N° Commande", 170),
            headerCell("Client", 180),
            headerCell("Montant", 110),
            dateCell,
            headerCell("Statut", 155),
            headerCell("Actions", 160)
        );
        return header;
    }

    private HBox headerCell(String val, double w) {
        Text t = new Text(val);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        t.setFill(Color.web(BRUN_MED));
        HBox b = new HBox(t);
        b.setMinWidth(w); b.setPrefWidth(w);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT & FILTRAGE
    // ═══════════════════════════════════════════════════════════════════════════

    private void chargerDonnees() {
        tableContainer.getChildren().clear();
        Label chargement = new Label("Chargement en cours...");
        chargement.setPadding(new Insets(20));
        chargement.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        chargement.setTextFill(Color.web(BRUN_LIGHT));
        tableContainer.getChildren().add(chargement);

        // Appel réseau dans un thread background pour ne pas bloquer JavaFX
        new Thread(() -> {
            List<CommandeDTO> donnees = controller.getToutesLesCommandes();
            Platform.runLater(() -> {
                toutesLesCommandes = donnees != null ? donnees : new ArrayList<>();
                appliquerFiltres();
            });
        }).start();
    }

    private void appliquerFiltres() {
        // 1. Filtrer par statut
        List<CommandeDTO> resultat = toutesLesCommandes.stream()
            .filter(c -> filtreStatut.equals("TOUS") ||
                filtreStatut.equalsIgnoreCase(c.getStatut()))
            .filter(c -> {
                // 2. Filtrer par recherche (ID + nom client)
                if (recherche.isEmpty()) return true;
                String id     = c.getIdCommande()     != null ? c.getIdCommande().toLowerCase()     : "";
                String client = c.getNomUtilisateur() != null ? c.getNomUtilisateur().toLowerCase() : "";
                return id.contains(recherche) || client.contains(recherche);
            })
            .sorted(triDateDesc
                ? Comparator.comparing(CommandeDTO::getDateCommande,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                : Comparator.comparing(CommandeDTO::getDateCommande,
                    Comparator.nullsLast(Comparator.naturalOrder()))
            )
            .collect(Collectors.toList());

        afficherLignes(resultat);
    }

    private void afficherLignes(List<CommandeDTO> liste) {
        tableContainer.getChildren().clear();

        if (liste.isEmpty()) {
            Label vide = new Label("Aucune commande trouvée.");
            vide.setPadding(new Insets(24));
            vide.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
            vide.setTextFill(Color.web(BRUN_LIGHT));
            tableContainer.getChildren().add(vide);
            return;
        }

        boolean alt = false;
        for (CommandeDTO cmd : liste) {
            HBox row = buildDataRow(cmd);
            if (alt) row.setStyle("-fx-background-color: " + CREME + ";");
            tableContainer.getChildren().add(row);
            alt = !alt;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LIGNE DE DONNÉES
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox buildDataRow(CommandeDTO cmd) {
        HBox row = new HBox(0);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setAlignment(Pos.CENTER_LEFT);

        // ── Col 1 : Référence (numéro de commande) ────────────────────────────
        String ref = cmd.getReference() != null ? cmd.getReference() : "#" + cmd.getIdCommande();
        Text refText = new Text(ref);
        refText.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        refText.setFill(Color.web(SAUGE_DARK));
        HBox refCell = new HBox(refText);
        refCell.setMinWidth(170); refCell.setPrefWidth(170);
        refCell.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(refCell);

        // ── Col 2 : Nom client (nomUtilisateur vient du DTO, ex: "Yassine Benali") ──
        row.getChildren().add(dataCell(cmd.getNomUtilisateur(), 180));

        // ── Col 3 : Montant ───────────────────────────────────────────────────
        String montantFmt = String.format("%.2f MAD", cmd.getMontantTotal());
        row.getChildren().add(dataCell(montantFmt, 110));

        // ── Col 4 : Date ──────────────────────────────────────────────────────
        String dateFmt = cmd.getDateCommande() != null
            ? cmd.getDateCommande().replace("T", "  ").substring(0, Math.min(16, cmd.getDateCommande().length()))
            : "—";
        row.getChildren().add(dataCell(dateFmt, 180));

        // ── Col 5 : ComboBox Statut ────────────────────────────────────────────
        ComboBox<String> statutBox = new ComboBox<>();
        statutBox.getItems().addAll("EN_PREPARATION", "EXPEDIEE", "LIVREE", "ANNULEE");
        String statutActuel = cmd.getStatut() != null ? cmd.getStatut() : "EN_PREPARATION";
        statutBox.setValue(statutActuel);
        statutBox.setStyle(
            "-fx-font-family: 'Georgia'; -fx-background-color: transparent;" +
            "-fx-border-color: " + BORDER + "; -fx-border-radius: 4; -fx-text-fill: " + BRUN + ";"
        );
        if ("LIVREE".equalsIgnoreCase(statutActuel) || "ANNULEE".equalsIgnoreCase(statutActuel)) {
            statutBox.setDisable(true);
        }
        statutBox.setOnAction(e -> {
            String nouveauStatut = statutBox.getValue();
            if (nouveauStatut.equals(statutActuel)) return;
            new Thread(() -> {
                boolean success = controller.changerStatutCommande(
                    cmd.getIdCommande(), statutActuel, nouveauStatut);
                Platform.runLater(() -> {
                    if (success) {
                        chargerDonnees();
                    } else {
                        statutBox.setValue(statutActuel);
                    }
                });
            }).start();
        });
        HBox statutCell = new HBox(statutBox);
        statutCell.setMinWidth(160); statutCell.setPrefWidth(160);
        statutCell.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(statutCell);

        // ── Col 6 : Bouton "Voir détails" ─────────────────────────────────────
        Button btnDetails = new Button("Voir détails");
        btnDetails.setFont(Font.font("Georgia", 11));
        btnDetails.setStyle(
            "-fx-background-color: " + INFO_BG + "; -fx-text-fill: " + INFO + ";" +
            "-fx-border-color: " + INFO + "; -fx-border-width: 1; -fx-border-radius: 6;" +
            "-fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;"
        );
        btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(
            "-fx-background-color: " + INFO + "; -fx-text-fill: white;" +
            "-fx-border-color: " + INFO + "; -fx-border-width: 1; -fx-border-radius: 6;" +
            "-fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;"
        ));
        btnDetails.setOnMouseExited(e -> btnDetails.setStyle(
            "-fx-background-color: " + INFO_BG + "; -fx-text-fill: " + INFO + ";" +
            "-fx-border-color: " + INFO + "; -fx-border-width: 1; -fx-border-radius: 6;" +
            "-fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;"
        ));
        btnDetails.setOnAction(e -> ouvrirModaleDetails(cmd.getIdCommande()));

        HBox actionsCell = new HBox(btnDetails);
        actionsCell.setMinWidth(160); actionsCell.setPrefWidth(160);
        actionsCell.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(actionsCell);

        return row;
    }

    private HBox dataCell(String val, double w) {
        Text t = new Text(val != null ? val : "—");
        t.setFont(Font.font("Georgia", 13));
        t.setFill(Color.web(BRUN));
        HBox b = new HBox(t);
        b.setMinWidth(w); b.setPrefWidth(w);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    // ─── Badge coloré par statut ──────────────────────────────────────────────
    private void appliquerStyleStatut(ComboBox<String> box, String statut) {
        String bg, fg, border;
        switch (statut != null ? statut.toUpperCase() : "") {
            case "EXPEDIEE" -> { bg = "#E8F4FD"; fg = "#2874A6"; border = "#2874A6"; }
            case "LIVREE"   -> { bg = "#E9F7EF"; fg = "#1E8449"; border = "#1E8449"; }
            case "ANNULEE"  -> { bg = "#FDEDEC"; fg = "#C0392B"; border = "#C0392B"; }
            default         -> { bg = "#FEF9E7"; fg = "#7D6608"; border = "#D4AC0D"; } // EN_PREPARATION orange
        }
        box.setStyle(
            "-fx-font-family: 'Georgia'; -fx-font-size: 11px;" +
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + "; -fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-text-fill: " + fg + "; -fx-padding: 2 8;"
        );
    }


    // ═══════════════════════════════════════════════════════════════════════════
    //  MODALE DÉTAILS COMMANDE
    // ═══════════════════════════════════════════════════════════════════════════

    private void ouvrirModaleDetails(String idCommande) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);
        modal.setTitle("Détails — Commande #" + idCommande);

        VBox body = new VBox(0);
        body.setStyle(
            "-fx-background-color: " + CREME_CARD + ";" +
            "-fx-border-color: " + BORDER + "; -fx-border-width: 1;" +
            "-fx-border-radius: 12; -fx-background-radius: 12;"
        );
        body.setEffect(new DropShadow(18, Color.web(BRUN, 0.12)));

        // ── Titre de la modale ────────────────────────────────────────────────
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(18, 22, 14, 22));
        titleBar.setStyle(
            "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Text titleTxt = new Text("Détails — Commande #" + idCommande);
        titleTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        titleTxt.setFill(Color.web(BRUN));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFermer = new Button("✕");
        btnFermer.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent;" +
            "-fx-text-fill: " + BRUN_LIGHT + "; -fx-cursor: hand; -fx-font-size: 14;"
        );
        btnFermer.setOnAction(e -> modal.close());
        titleBar.getChildren().addAll(titleTxt, spacer, btnFermer);

        // ── Corps : chargement ────────────────────────────────────────────────
        VBox contenu = new VBox(0);
        contenu.setPadding(new Insets(0, 0, 10, 0));

        Label loading = new Label("Chargement des lignes...");
        loading.setPadding(new Insets(24));
        loading.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
        loading.setTextFill(Color.web(BRUN_LIGHT));
        contenu.getChildren().add(loading);

        body.getChildren().addAll(titleBar, contenu);

        Scene scene = new Scene(body, 640, 450);
        modal.setScene(scene);
        modal.show();

        // Chargement en background
        new Thread(() -> {
            CommandeDTO details = controller.getDetailsCommande(idCommande);
            Platform.runLater(() -> {
                contenu.getChildren().clear();
                if (details == null || details.getLignes() == null || details.getLignes().isEmpty()) {
                    Label vide = new Label("Aucune ligne de commande disponible.");
                    vide.setPadding(new Insets(24));
                    vide.setFont(Font.font("Georgia", FontPosture.ITALIC, 13));
                    vide.setTextFill(Color.web(BRUN_LIGHT));
                    contenu.getChildren().add(vide);
                    return;
                }

                // En-tête du tableau de détails
                HBox ligneHeader = ligneRow("Produit", "Qté", "Prix unitaire", "Sous-total", true);
                contenu.getChildren().add(ligneHeader);

                double total = 0;
                boolean altLigne = false;
                for (LigneCommandeDTO ligne : details.getLignes()) {
                    double sousTotal = ligne.getSousTotal() > 0
                        ? ligne.getSousTotal()
                        : ligne.getQuantite() * ligne.getPrixUnitaire();
                    HBox ligneRow = ligneRow(
                        ligne.getNomProduit(),
                        String.valueOf(ligne.getQuantite()),
                        String.format("%.2f MAD", ligne.getPrixUnitaire()),
                        String.format("%.2f MAD", sousTotal),
                        false
                    );
                    if (altLigne) ligneRow.setStyle("-fx-background-color: " + CREME + ";");
                    contenu.getChildren().add(ligneRow);
                    total += sousTotal;
                    altLigne = !altLigne;
                }

                // Ligne TOTAL
                double finalTotal = total;
                HBox totalRow = new HBox();
                totalRow.setPadding(new Insets(14, 22, 14, 22));
                totalRow.setAlignment(Pos.CENTER_RIGHT);
                totalRow.setStyle(
                    "-fx-border-color: " + BORDER + " transparent transparent transparent;" +
                    "-fx-border-width: 1 0 0 0;"
                );
                Text totalLabel = new Text(String.format("Total général :   %.2f MAD", finalTotal));
                totalLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
                totalLabel.setFill(Color.web(BRUN));
                totalRow.getChildren().add(totalLabel);
                contenu.getChildren().add(totalRow);
            });
        }).start();
    }

    private HBox ligneRow(String produit, String qte, String prix, String sousTotal, boolean isHeader) {
        HBox row = new HBox(0);
        row.setPadding(new Insets(10, 22, 10, 22));
        row.setAlignment(Pos.CENTER_LEFT);
        if (isHeader) {
            row.setStyle(
                "-fx-background-color: " + CREME_INPUT + ";" +
                "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                "-fx-border-width: 0 0 1 0;"
            );
        }
        row.getChildren().addAll(
            ligneCell(produit,   isHeader, 210),
            ligneCell(qte,       isHeader, 70),
            ligneCell(prix,      isHeader, 140),
            ligneCell(sousTotal, isHeader, 140)
        );
        return row;
    }

    private HBox ligneCell(String val, boolean isHeader, double w) {
        Text t = new Text(val != null ? val : "—");
        t.setFont(Font.font("Georgia", isHeader ? FontWeight.BOLD : FontWeight.NORMAL,
                            isHeader ? 11 : 13));
        t.setFill(Color.web(isHeader ? BRUN_MED : BRUN));
        HBox b = new HBox(t);
        b.setMinWidth(w); b.setPrefWidth(w);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
