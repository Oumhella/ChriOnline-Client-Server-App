package com.chrionline.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class ProductFormat implements Serializable {

    private int id;
    private Double prix;
    private int stock;
    private int stockAlerte;
    private String imageUrl;
    private LocalDateTime dateExpiration;


    private List<LabelValue> labelValues;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockAlerte() {
        return stockAlerte;
    }

    public void setStockAlerte(int stockAlerte) {
        this.stockAlerte = stockAlerte;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public List<LabelValue> getLabelValues() {
        return labelValues;
    }

    public void setLabelValues(List<LabelValue> labelValues) {
        this.labelValues = labelValues;
    }
}
