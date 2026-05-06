package tn.esprit.entities;

import java.time.LocalDate;

public class Commentaire {
    private Integer id;
    private String contenu;
    private LocalDate dateCommentaire;
    private Integer joueurId;
    private Integer annonceId;
    private String auteurAnonyme;
    private String cvName;
    private String cvTitle;
    private int nbLikes;
    private int nbDislikes;
    private String moderationStatus;
    private String moderationReason;
    private Integer authorUserId;
    private String authorRole;

    public Commentaire() {
    }

    public Commentaire(String contenu, LocalDate dateCommentaire, Integer joueurId, Integer annonceId, String auteurAnonyme, int nbLikes, String moderationStatus, String moderationReason) {
        this(contenu, dateCommentaire, joueurId, annonceId, auteurAnonyme, null, nbLikes, moderationStatus, moderationReason);
    }

    public Commentaire(
            String contenu,
            LocalDate dateCommentaire,
            Integer joueurId,
            Integer annonceId,
            String auteurAnonyme,
            String cvName,
            int nbLikes,
            String moderationStatus,
            String moderationReason
    ) {
        this(contenu, dateCommentaire, joueurId, annonceId, auteurAnonyme, cvName, null, nbLikes, moderationStatus, moderationReason, joueurId, null);
    }

    public Commentaire(
            String contenu,
            LocalDate dateCommentaire,
            Integer joueurId,
            Integer annonceId,
            String auteurAnonyme,
            String cvName,
            String cvTitle,
            int nbLikes,
            String moderationStatus,
            String moderationReason,
            Integer authorUserId,
            String authorRole
    ) {
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.joueurId = joueurId;
        this.annonceId = annonceId;
        this.auteurAnonyme = auteurAnonyme;
        this.cvName = cvName;
        this.cvTitle = cvTitle;
        this.nbLikes = nbLikes;
        this.nbDislikes = 0;
        this.moderationStatus = moderationStatus;
        this.moderationReason = moderationReason;
        this.authorUserId = authorUserId;
        this.authorRole = authorRole;
    }

    public Commentaire(Integer id, String contenu, LocalDate dateCommentaire, Integer joueurId, Integer annonceId, String auteurAnonyme, int nbLikes, String moderationStatus, String moderationReason) {
        this(id, contenu, dateCommentaire, joueurId, annonceId, auteurAnonyme, null, nbLikes, moderationStatus, moderationReason);
    }

    public Commentaire(
            Integer id,
            String contenu,
            LocalDate dateCommentaire,
            Integer joueurId,
            Integer annonceId,
            String auteurAnonyme,
            String cvName,
            int nbLikes,
            String moderationStatus,
            String moderationReason
    ) {
        this(id, contenu, dateCommentaire, joueurId, annonceId, auteurAnonyme, cvName, null, nbLikes, moderationStatus, moderationReason, joueurId, null);
    }

    public Commentaire(
            Integer id,
            String contenu,
            LocalDate dateCommentaire,
            Integer joueurId,
            Integer annonceId,
            String auteurAnonyme,
            String cvName,
            String cvTitle,
            int nbLikes,
            String moderationStatus,
            String moderationReason,
            Integer authorUserId,
            String authorRole
    ) {
        this.id = id;
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.joueurId = joueurId;
        this.annonceId = annonceId;
        this.auteurAnonyme = auteurAnonyme;
        this.cvName = cvName;
        this.cvTitle = cvTitle;
        this.nbLikes = nbLikes;
        this.nbDislikes = 0;
        this.moderationStatus = moderationStatus;
        this.moderationReason = moderationReason;
        this.authorUserId = authorUserId;
        this.authorRole = authorRole;
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

    public String getCvName() {
        return cvName;
    }

    public void setCvName(String cvName) {
        this.cvName = cvName;
    }

    public String getCvTitle() {
        return cvTitle;
    }

    public void setCvTitle(String cvTitle) {
        this.cvTitle = cvTitle;
    }

    public int getNbLikes() {
        return nbLikes;
    }

    public void setNbLikes(int nbLikes) {
        this.nbLikes = nbLikes;
    }

    public int getNbDislikes() {
        return nbDislikes;
    }

    public void setNbDislikes(int nbDislikes) {
        this.nbDislikes = nbDislikes;
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

    public Integer getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(Integer authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }
}
