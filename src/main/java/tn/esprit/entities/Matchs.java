package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Matchs {
    private Integer id;
    private String idMatch;
    private LocalDate dateMatch;
    private LocalTime heureDebut;
    private String lieu;
    private String type;
    private String statut;
    private String lineupDomicile;
    private String lineupExterieur;
    private Integer scoreEquipeDomicile;
    private Integer scoreEquipeExterieur;
    private Integer equipeDomicileId;
    private Integer equipeExterieurId;

    public Matchs() {
    }

    public Matchs(String idMatch, LocalDate dateMatch, LocalTime heureDebut, String lieu, String type, String statut, String lineupDomicile, String lineupExterieur, Integer scoreEquipeDomicile, Integer scoreEquipeExterieur, Integer equipeDomicileId, Integer equipeExterieurId) {
        this.idMatch = idMatch;
        this.dateMatch = dateMatch;
        this.heureDebut = heureDebut;
        this.lieu = lieu;
        this.type = type;
        this.statut = statut;
        this.lineupDomicile = lineupDomicile;
        this.lineupExterieur = lineupExterieur;
        this.scoreEquipeDomicile = scoreEquipeDomicile;
        this.scoreEquipeExterieur = scoreEquipeExterieur;
        this.equipeDomicileId = equipeDomicileId;
        this.equipeExterieurId = equipeExterieurId;
    }
}
