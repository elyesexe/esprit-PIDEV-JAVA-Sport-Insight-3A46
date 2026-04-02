package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Entrainement {
    private Integer id;
    private LocalDate dateEntrainement;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String type;
    private String objectif;
    private String lieu;
    private Integer entraineurId;

    public Entrainement() {
    }

    public Entrainement(LocalDate dateEntrainement, LocalTime heureDebut, LocalTime heureFin, String type, String objectif, String lieu, Integer entraineurId) {
        this.dateEntrainement = dateEntrainement;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.type = type;
        this.objectif = objectif;
        this.lieu = lieu;
        this.entraineurId = entraineurId;
    }
}
