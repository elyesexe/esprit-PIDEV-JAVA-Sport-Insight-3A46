package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String email;
    private String roles;
    private String password;
    private String nom;
    private String prenom;
    private String telephone;
    private LocalDate dateNaissance;
    private String photo;
    private String statut;
    private LocalDateTime dateInscription;
    private String cvName;
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(String email, String roles, String password, String nom, String prenom, String telephone, LocalDate dateNaissance, String photo, String statut, LocalDateTime dateInscription, String cvName, LocalDateTime updatedAt) {
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.dateNaissance = dateNaissance;
        this.photo = photo;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.cvName = cvName;
        this.updatedAt = updatedAt;
    }
}
