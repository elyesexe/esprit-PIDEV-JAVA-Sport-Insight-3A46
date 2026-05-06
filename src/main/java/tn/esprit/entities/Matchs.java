package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private Long externalApiId;
    private String externalSource;
    private String competitionCode;
    private Long apiFootballId;
    private String apiFootballStatsJson;
    private String apiFootballLineupJson;
    private String apiFootballIncidentsJson;
    private LocalDateTime apiFootballSyncedAt;
    private String oddsSnapshotJson;
    private String oddsSource;
    private LocalDateTime oddsSyncedAt;

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

    public Matchs(Integer id, String idMatch, LocalDate dateMatch, LocalTime heureDebut, String lieu, String type, String statut, String lineupDomicile, String lineupExterieur, Integer scoreEquipeDomicile, Integer scoreEquipeExterieur, Integer equipeDomicileId, Integer equipeExterieurId) {
        this.id = id;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdMatch() {
        return idMatch;
    }

    public void setIdMatch(String idMatch) {
        this.idMatch = idMatch;
    }

    public LocalDate getDateMatch() {
        return dateMatch;
    }

    public void setDateMatch(LocalDate dateMatch) {
        this.dateMatch = dateMatch;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getLineupDomicile() {
        return lineupDomicile;
    }

    public void setLineupDomicile(String lineupDomicile) {
        this.lineupDomicile = lineupDomicile;
    }

    public String getLineupExterieur() {
        return lineupExterieur;
    }

    public void setLineupExterieur(String lineupExterieur) {
        this.lineupExterieur = lineupExterieur;
    }

    public Integer getScoreEquipeDomicile() {
        return scoreEquipeDomicile;
    }

    public void setScoreEquipeDomicile(Integer scoreEquipeDomicile) {
        this.scoreEquipeDomicile = scoreEquipeDomicile;
    }

    public Integer getScoreEquipeExterieur() {
        return scoreEquipeExterieur;
    }

    public void setScoreEquipeExterieur(Integer scoreEquipeExterieur) {
        this.scoreEquipeExterieur = scoreEquipeExterieur;
    }

    public Integer getEquipeDomicileId() {
        return equipeDomicileId;
    }

    public void setEquipeDomicileId(Integer equipeDomicileId) {
        this.equipeDomicileId = equipeDomicileId;
    }

    public Integer getEquipeExterieurId() {
        return equipeExterieurId;
    }

    public void setEquipeExterieurId(Integer equipeExterieurId) {
        this.equipeExterieurId = equipeExterieurId;
    }

    public Long getExternalApiId() {
        return externalApiId;
    }

    public void setExternalApiId(Long externalApiId) {
        this.externalApiId = externalApiId;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public Long getApiFootballId() {
        return apiFootballId;
    }

    public void setApiFootballId(Long apiFootballId) {
        this.apiFootballId = apiFootballId;
    }

    public String getApiFootballStatsJson() {
        return apiFootballStatsJson;
    }

    public void setApiFootballStatsJson(String apiFootballStatsJson) {
        this.apiFootballStatsJson = apiFootballStatsJson;
    }

    public String getApiFootballLineupJson() {
        return apiFootballLineupJson;
    }

    public void setApiFootballLineupJson(String apiFootballLineupJson) {
        this.apiFootballLineupJson = apiFootballLineupJson;
    }

    public String getApiFootballIncidentsJson() {
        return apiFootballIncidentsJson;
    }

    public void setApiFootballIncidentsJson(String apiFootballIncidentsJson) {
        this.apiFootballIncidentsJson = apiFootballIncidentsJson;
    }

    public LocalDateTime getApiFootballSyncedAt() {
        return apiFootballSyncedAt;
    }

    public void setApiFootballSyncedAt(LocalDateTime apiFootballSyncedAt) {
        this.apiFootballSyncedAt = apiFootballSyncedAt;
    }

    public String getOddsSnapshotJson() {
        return oddsSnapshotJson;
    }

    public void setOddsSnapshotJson(String oddsSnapshotJson) {
        this.oddsSnapshotJson = oddsSnapshotJson;
    }

    public String getOddsSource() {
        return oddsSource;
    }

    public void setOddsSource(String oddsSource) {
        this.oddsSource = oddsSource;
    }

    public LocalDateTime getOddsSyncedAt() {
        return oddsSyncedAt;
    }

    public void setOddsSyncedAt(LocalDateTime oddsSyncedAt) {
        this.oddsSyncedAt = oddsSyncedAt;
    }

    @Override
    public String toString() {
        return "Matchs{" +
                "id=" + id +
                ", idMatch='" + idMatch + '\'' +
                ", dateMatch=" + dateMatch +
                ", heureDebut=" + heureDebut +
                ", lieu='" + lieu + '\'' +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                ", lineupDomicile='" + lineupDomicile + '\'' +
                ", lineupExterieur='" + lineupExterieur + '\'' +
                ", scoreEquipeDomicile=" + scoreEquipeDomicile +
                ", scoreEquipeExterieur=" + scoreEquipeExterieur +
                ", equipeDomicileId=" + equipeDomicileId +
                ", equipeExterieurId=" + equipeExterieurId +
                ", externalApiId=" + externalApiId +
                ", externalSource='" + externalSource + '\'' +
                ", competitionCode='" + competitionCode + '\'' +
                ", apiFootballId=" + apiFootballId +
                ", apiFootballIncidentsJson='" + apiFootballIncidentsJson + '\'' +
                ", apiFootballSyncedAt=" + apiFootballSyncedAt +
                ", oddsSource='" + oddsSource + '\'' +
                ", oddsSyncedAt=" + oddsSyncedAt +
                '}';
    }
}
