package tn.esprit.entities;

import java.time.LocalDateTime;

public class Sponsor {
    private Integer id;
    private String nom;
    private String email;
    private String telephone;
    private double budget;
    private String logoName;
    private LocalDateTime updatedAt;
    private String adresse;

    public Sponsor() {
    }

    public Sponsor(String nom, String email, String telephone, double budget, String logoName, LocalDateTime updatedAt, String adresse) {
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.budget = budget;
        this.logoName = logoName;
        this.updatedAt = updatedAt;
        this.adresse = adresse;
    }
}
