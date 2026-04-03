package com.chrionline.admin.controller;
import com.chrionline.admin.network.AdminCommandeClient;
import com.chrionline.shared.dto.CommandeDTO;

import java.util.List;

public class AdminCommandesController {

    private final AdminCommandeClient apiClient;

    public AdminCommandesController() {
        this.apiClient = new AdminCommandeClient();
    }

    public List<CommandeDTO> getToutesLesCommandes() {
        return apiClient.fetchAllCommandes();
    }

    public CommandeDTO getDetailsCommande(String idCommande) {
        return apiClient.fetchOrderDetails(idCommande);
    }

    public boolean changerStatutCommande(String idCommande, String statutActuel, String nouveauStatut) {
        // Empêcher des changements inutiles ou interdits avant même de contacter le serveur
        if (nouveauStatut.equals(statutActuel)) return false;
        if (statutActuel.equalsIgnoreCase("LIVREE") || statutActuel.equalsIgnoreCase("ANNULEE")) {
            return false; // Impossible de changer une commande finale
        }

        return apiClient.updateStatus(idCommande, nouveauStatut);
    }
}
