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

    public Entrainement() {}

    public Entrainement(LocalDate dateEntrainement, LocalTime heureDebut, LocalTime heureFin,
                        String type, String objectif, String lieu, Integer entraineurId) {
        this.dateEntrainement = dateEntrainement;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.type = type;
        this.objectif = objectif;
        this.lieu = lieu;
        this.entraineurId = entraineurId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDate getDateEntrainement() { return dateEntrainement; }
    public void setDateEntrainement(LocalDate dateEntrainement) { this.dateEntrainement = dateEntrainement; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public Integer getEntraineurId() { return entraineurId; }
    public void setEntraineurId(Integer entraineurId) { this.entraineurId = entraineurId; }

    @Override
    public String toString() {
        return "Entrainement{" +
                "id=" + id +
                ", date=" + dateEntrainement +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", type='" + type + '\'' +
                ", objectif='" + objectif + '\'' +
                ", lieu='" + lieu + '\'' +
                ", entraineurId=" + entraineurId +
                '}';
    }
}
