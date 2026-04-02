package tn.esprit.entities;

import java.time.LocalDate;

public class Commentaire {
    private Integer id;
    private String contenu;
    private LocalDate dateCommentaire;
    private Integer joueurId;
    private Integer annonceId;
    private String auteurAnonyme;
    private int nbLikes;
    private String moderationStatus;
    private String moderationReason;

    public Commentaire() {
    }

    public Commentaire(String contenu, LocalDate dateCommentaire, Integer joueurId, Integer annonceId, String auteurAnonyme, int nbLikes, String moderationStatus, String moderationReason) {
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.joueurId = joueurId;
        this.annonceId = annonceId;
        this.auteurAnonyme = auteurAnonyme;
        this.nbLikes = nbLikes;
        this.moderationStatus = moderationStatus;
        this.moderationReason = moderationReason;
    }

    public Commentaire(Integer id, String contenu, LocalDate dateCommentaire, Integer joueurId, Integer annonceId, String auteurAnonyme, int nbLikes, String moderationStatus, String moderationReason) {
        this.id = id;
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.joueurId = joueurId;
        this.annonceId = annonceId;
        this.auteurAnonyme = auteurAnonyme;
        this.nbLikes = nbLikes;
        this.moderationStatus = moderationStatus;
        this.moderationReason = moderationReason;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDate getDateCommentaire() {
        return dateCommentaire;
    }

    public void setDateCommentaire(LocalDate dateCommentaire) {
        this.dateCommentaire = dateCommentaire;
    }

    public Integer getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(Integer joueurId) {
        this.joueurId = joueurId;
    }

    public Integer getAnnonceId() {
        return annonceId;
    }

    public void setAnnonceId(Integer annonceId) {
        this.annonceId = annonceId;
    }

    public String getAuteurAnonyme() {
        return auteurAnonyme;
    }

    public void setAuteurAnonyme(String auteurAnonyme) {
        this.auteurAnonyme = auteurAnonyme;
    }

    public int getNbLikes() {
        return nbLikes;
    }

    public void setNbLikes(int nbLikes) {
        this.nbLikes = nbLikes;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(String moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public void setModerationReason(String moderationReason) {
        this.moderationReason = moderationReason;
    }
}
