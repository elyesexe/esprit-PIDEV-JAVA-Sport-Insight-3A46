package tn.esprit.entities;

import java.time.LocalDate;

public class Annonce {
    private Integer id;
    private String titre;
    private String description;
    private String posteRecherche;
    private String niveauRequis;
    private LocalDate datePublication;
    private String statut;
    private Integer entraineurId;
    private Boolean commentsEnabled; // true = comments allowed
    private Boolean urgent; // true = annonce marked urgent (priority)

    public Annonce() {
        this.commentsEnabled = true;
        this.urgent = false;
    }

    public Annonce(String titre, String description, String posteRecherche, String niveauRequis, LocalDate datePublication, String statut, Integer entraineurId) {
        this.titre = titre;
        this.description = description;
        this.posteRecherche = posteRecherche;
        this.niveauRequis = niveauRequis;
        this.datePublication = datePublication;
        this.statut = statut;
        this.entraineurId = entraineurId;
        this.commentsEnabled = true;
        this.urgent = false;
    }

    public Annonce(String titre, String description, String posteRecherche, String niveauRequis, LocalDate datePublication, String statut, Integer entraineurId, Boolean commentsEnabled, Boolean urgent) {
        this.titre = titre;
        this.description = description;
        this.posteRecherche = posteRecherche;
        this.niveauRequis = niveauRequis;
        this.datePublication = datePublication;
        this.statut = statut;
        this.entraineurId = entraineurId;
        this.commentsEnabled = commentsEnabled != null ? commentsEnabled : true;
        this.urgent = urgent != null ? urgent : false;
    }

    public Annonce(Integer id, String titre, String description, String posteRecherche, String niveauRequis, LocalDate datePublication, String statut, Integer entraineurId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.posteRecherche = posteRecherche;
        this.niveauRequis = niveauRequis;
        this.datePublication = datePublication;
        this.statut = statut;
        this.entraineurId = entraineurId;
        this.commentsEnabled = true;
        this.urgent = false;
    }

    public Annonce(Integer id, String titre, String description, String posteRecherche, String niveauRequis, LocalDate datePublication, String statut, Integer entraineurId, Boolean commentsEnabled, Boolean urgent) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.posteRecherche = posteRecherche;
        this.niveauRequis = niveauRequis;
        this.datePublication = datePublication;
        this.statut = statut;
        this.entraineurId = entraineurId;
        this.commentsEnabled = commentsEnabled != null ? commentsEnabled : true;
        this.urgent = urgent != null ? urgent : false;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosteRecherche() {
        return posteRecherche;
    }

    public void setPosteRecherche(String posteRecherche) {
        this.posteRecherche = posteRecherche;
    }

    public String getNiveauRequis() {
        return niveauRequis;
    }

    public void setNiveauRequis(String niveauRequis) {
        this.niveauRequis = niveauRequis;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getEntraineurId() {
        return entraineurId;
    }

    public void setEntraineurId(Integer entraineurId) {
        this.entraineurId = entraineurId;
    }

    public Boolean getCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(Boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    @Override
    public String toString() {
        return "Annonce{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", posteRecherche='" + posteRecherche + '\'' +
                ", niveauRequis='" + niveauRequis + '\'' +
                ", datePublication=" + datePublication +
                ", statut='" + statut + '\'' +
                ", entraineurId=" + entraineurId +
                ", commentsEnabled=" + commentsEnabled +
                ", urgent=" + urgent +
                '}';
    }
}
