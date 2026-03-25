package com.chrionline.server.service;
import com.chrionline.shared.models.Panier;
import com.chrionline.shared.models.Produit;

import java.util.ArrayList;
import java.util.List;

public class PanierService {

    private  List<Produit> panierList = new ArrayList<>();
    public  boolean ajouterAuPanier(Produit produit){
        if(produit != null){
            panierList.add(produit);
            return true;
        }
        return false;
    }
    public boolean supprimerDuPanier(int produitId) {
        return panierList.removeIf(p -> p.getId() == produitId);
    }
}
