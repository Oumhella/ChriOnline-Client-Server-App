package com.chrionline.admin.controller;

import com.chrionline.server.dao.DashboardDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur du dashboard admin.
 * Interagit directement avec la base de données (pas de serveur TCP ici,
 * l'admin tourne en local avec accès direct à la DB).
 */
public class AdminDashboardController {

    private Map<String, Object> statsCache;

    /**
     * Charge les statistiques depuis la DB.
     */
    public Map<String, Object> chargerStats() {
        this.statsCache = DashboardDAO.getStats();
        return statsCache;
    }

    public int getTotalClients() {
        return (int) statsCache.getOrDefault("totalClients", 0);
    }

    public int getTotalCommandes() {
        return (int) statsCache.getOrDefault("totalCommandes", 0);
    }

    public BigDecimal getChiffreAffaires() {
        Object val = statsCache.get("chiffreAffaires");
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return BigDecimal.ZERO;
    }

    public int getCommandesEnAttente() {
        return (int) statsCache.getOrDefault("commandesEnAttente", 0);
    }

    public int getStockAlerte() {
        return (int) statsCache.getOrDefault("stockAlerte", 0);
    }

    public int getTotalProduits() {
        return (int) statsCache.getOrDefault("totalProduits", 0);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCommandesRecentes() {
        return (List<Map<String, Object>>) statsCache.getOrDefault(
                "commandesRecentes", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getInscriptionsParMois() {
        return (List<Map<String, Object>>) statsCache.getOrDefault(
                "inscriptionsParMois", List.of());
    }

    public Map<String, Integer> getCommandesParStatut() {
        return DashboardDAO.getCommandesParStatut();
    }
}