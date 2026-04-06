package tn.esprit.entities;

public class Participation {
    private Integer id;
    private String presence;
    private String justificationAbsence;
    private Integer entrainementId;
    private Integer joueurId;

    public Participation() {}

    public Participation(String presence, String justificationAbsence,
                         Integer entrainementId, Integer joueurId) {
        this.presence = presence;
        this.justificationAbsence = justificationAbsence;
        this.entrainementId = entrainementId;
        this.joueurId = joueurId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPresence() { return presence; }
    public void setPresence(String presence) { this.presence = presence; }

    public String getJustificationAbsence() { return justificationAbsence; }
    public void setJustificationAbsence(String justificationAbsence) { this.justificationAbsence = justificationAbsence; }

    public Integer getEntrainementId() { return entrainementId; }
    public void setEntrainementId(Integer entrainementId) { this.entrainementId = entrainementId; }

    public Integer getJoueurId() { return joueurId; }
    public void setJoueurId(Integer joueurId) { this.joueurId = joueurId; }

    @Override
    public String toString() {
        return "Participation{" +
                "id=" + id +
                ", presence='" + presence + '\'' +
                ", justificationAbsence='" + justificationAbsence + '\'' +
                ", entrainementId=" + entrainementId +
                ", joueurId=" + joueurId +
                '}';
    }
}
