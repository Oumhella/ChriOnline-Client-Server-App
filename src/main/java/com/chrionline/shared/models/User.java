package com.chrionline.shared.models;

import java.io.Serializable;

public class User implements Serializable {
    private int    idUtilisateur;
    private String nom;
    private String prenom;
    private String email;
    private String password; // hash bcrypt

    public User() {}

    public User(String nom, String prenom, String email, String password) {
        this.nom      = nom;
        this.prenom   = prenom;
        this.email    = email;
        this.password = password;
    }

    // Getters / Setters
    public int    getIdUtilisateur()              { return idUtilisateur; }
    public void   setIdUtilisateur(int id)        { this.idUtilisateur = id; }
    public String getNom()                        { return nom; }
    public void   setNom(String nom)              { this.nom = nom; }
    public String getPrenom()                     { return prenom; }
    public void   setPrenom(String prenom)        { this.prenom = prenom; }
    public String getEmail()                      { return email; }
    public void   setEmail(String email)          { this.email = email; }
    public String getPassword()                   { return password; }
    public void   setPassword(String password)    { this.password = password; }
}