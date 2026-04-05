package tn.esprit.entities;

import java.time.LocalDate;

public class ContratSponsor {
    private Integer id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double montant;
    private String description;
    private String statut;
    private boolean notified;
    private String statutPaiement;
    private Integer sponsorId;
    private Integer equipeId;

    public ContratSponsor() {
    }

    public ContratSponsor(LocalDate dateDebut, LocalDate dateFin, double montant, String description, String statut, boolean notified, String statutPaiement, Integer sponsorId, Integer equipeId) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montant = montant;
        this.description = description;
        this.statut = statut;
        this.notified = notified;
        this.statutPaiement = statutPaiement;
        this.sponsorId = sponsorId;
        this.equipeId = equipeId;
    }

    public ContratSponsor(Integer id, LocalDate dateDebut, LocalDate dateFin, double montant, String description, String statut, boolean notified, String statutPaiement, Integer sponsorId, Integer equipeId) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montant = montant;
        this.description = description;
        this.statut = statut;
        this.notified = notified;
        this.statutPaiement = statutPaiement;
        this.sponsorId = sponsorId;
        this.equipeId = equipeId;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public String getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(String statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public Integer getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(Integer sponsorId) {
        this.sponsorId = sponsorId;
    }

    public Integer getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(Integer equipeId) {
        this.equipeId = equipeId;
    }

    @Override
    public String toString() {
        return "ContratSponsor{" +
                "id=" + id +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", montant=" + montant +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", notified=" + notified +
                ", statutPaiement='" + statutPaiement + '\'' +
                ", sponsorId=" + sponsorId +
                ", equipeId=" + equipeId +
                '}';
    }
}
