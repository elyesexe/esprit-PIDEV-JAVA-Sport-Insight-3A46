package tn.esprit.entities;

public class Participation {
    private Integer id;
    private String presence;
    private String justificationAbsence;
    private Integer entrainementId;
    private Integer joueurId;

    public Participation() {
    }

    public Participation(String presence, String justificationAbsence, Integer entrainementId, Integer joueurId) {
        this.presence = presence;
        this.justificationAbsence = justificationAbsence;
        this.entrainementId = entrainementId;
        this.joueurId = joueurId;
    }
}
