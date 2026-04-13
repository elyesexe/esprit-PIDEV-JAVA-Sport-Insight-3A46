package tn.esprit.entities;

import tn.esprit.security.UserRoles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public List<String> getRoleList() {
        return UserRoles.parseRoles(roles);
    }

    public void setRoleList(Collection<String> roles) {
        this.roles = UserRoles.toDatabaseValue(roles);
    }

    public String getPrimaryRole() {
        return UserRoles.resolvePrimaryRole(roles);
    }

    public boolean hasRole(String role) {
        return UserRoles.hasRole(roles, role);
    }

    public boolean isAdmin() {
        return hasRole(UserRoles.ROLE_ADMIN);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getCvName() {
        return cvName;
    }

    public void setCvName(String cvName) {
        this.cvName = cvName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActiveAccount() {
        String normalized = statut == null ? "" : statut.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty()
                || "ACTIVE".equals(normalized)
                || "ACTIF".equals(normalized)
                || "ENABLED".equals(normalized);
    }

    public String getDisplayName() {
        String fullName = ((prenom == null ? "" : prenom.trim()) + " " + (nom == null ? "" : nom.trim())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return email == null || email.isBlank() ? "Sport Insight user" : email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", roles='" + roles + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", telephone='" + telephone + '\'' +
                ", statut='" + statut + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", dateInscription=" + dateInscription +
                ", cvName='" + cvName + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
