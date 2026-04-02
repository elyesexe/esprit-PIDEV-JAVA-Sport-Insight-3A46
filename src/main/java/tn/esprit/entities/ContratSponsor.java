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
}
