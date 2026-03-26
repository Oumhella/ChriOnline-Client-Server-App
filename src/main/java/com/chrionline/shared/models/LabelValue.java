package com.chrionline.shared.models;

import java.io.Serializable;

public class LabelValue implements Serializable {

    private int id;
    private String valeur;

    private Label label;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return valeur;
    }
}
