package com.chrionline.shared.models;

import java.io.Serializable;

public class WishlistProduit implements Serializable {

    private int idWishlistProduit;
    private int idProduit;

    public WishlistProduit(int idWishlistProduit, int idProduit) {
        this.idWishlistProduit = idWishlistProduit;
        this.idProduit = idProduit;
    }

    public int getIdWishlistProduit() {
        return idWishlistProduit;
    }

    public void setIdWishlistProduit(int idWishlistProduit) {
        this.idWishlistProduit = idWishlistProduit;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }
}
