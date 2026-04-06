package tn.esprit.entities;

import java.time.LocalDate;

public class Joueur {
    private Integer id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private int numero;
    private String image;
    private Integer equipeId;

    public Joueur() {
    }

    public Joueur(String nom, String prenom, LocalDate dateNaissance, int numero, String image, Integer equipeId) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.numero = numero;
        this.image = image;
        this.equipeId = equipeId;
    }

    public Joueur(Integer id, String nom, String prenom, LocalDate dateNaissance, int numero, String image, Integer equipeId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.numero = numero;
        this.image = image;
        this.equipeId = equipeId;
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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(Integer equipeId) {
        this.equipeId = equipeId;
    }

    @Override
    public String toString() {
        return "Joueur{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", numero=" + numero +
                ", image='" + image + '\'' +
                ", equipeId=" + equipeId +
                '}';
    }
}
