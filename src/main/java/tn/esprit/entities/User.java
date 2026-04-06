package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    private int id;
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

    // ── Constructeur vide ─────────────────────────────────────────────────
    public User() {}

    // ── Constructeur complet (sans id) ────────────────────────────────────
    public User(String email, String roles, String password,
                String nom, String prenom, String telephone,
                LocalDate dateNaissance, String photo, String statut,
                LocalDateTime dateInscription, String cvName, LocalDateTime updatedAt) {
        this.email          = email;
        this.roles          = roles;
        this.password       = password;
        this.nom            = nom;
        this.prenom         = prenom;
        this.telephone      = telephone;
        this.dateNaissance  = dateNaissance;
        this.photo          = photo;
        this.statut         = statut;
        this.dateInscription = dateInscription;
        this.cvName         = cvName;
        this.updatedAt      = updatedAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }

    public String getRoles()                  { return roles; }
    public void setRoles(String roles)        { this.roles = roles; }

    public String getPassword()               { return password; }
    public void setPassword(String password)  { this.password = password; }

    public String getNom()                    { return nom; }
    public void setNom(String nom)            { this.nom = nom; }

    public String getPrenom()                 { return prenom; }
    public void setPrenom(String prenom)      { this.prenom = prenom; }

    public String getTelephone()              { return telephone; }
    public void setTelephone(String telephone){ this.telephone = telephone; }

    public LocalDate getDateNaissance()               { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance){ this.dateNaissance = dateNaissance; }

    public String getPhoto()                  { return photo; }
    public void setPhoto(String photo)        { this.photo = photo; }

    public String getStatut()                 { return statut; }
    public void setStatut(String statut)      { this.statut = statut; }

    public LocalDateTime getDateInscription()                   { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription){ this.dateInscription = dateInscription; }

    public String getCvName()                 { return cvName; }
    public void setCvName(String cvName)      { this.cvName = cvName; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── toString ──────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", roles='" + roles + '\'' +
                ", telephone='" + telephone + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}