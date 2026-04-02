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
}
