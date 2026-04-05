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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getLogoName() {
        return logoName;
    }

    public void setLogoName(String logoName) {
        this.logoName = logoName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    @Override
    public String toString() {
        return "Sponsor{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", telephone='" + telephone + '\'' +
                ", budget=" + budget +
                ", logoName='" + logoName + '\'' +
                ", updatedAt=" + updatedAt +
                ", adresse='" + adresse + '\'' +
                '}';
    }
}
