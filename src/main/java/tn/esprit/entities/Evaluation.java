package tn.esprit.entities;

public class Evaluation {
    private Integer id;
    private double notePhysique;
    private double noteTechnique;
    private double noteTactique;
    private String commentaire;
    private Integer entrainementId;
    private Integer joueurId;

    public Evaluation() {}

    public Evaluation(double notePhysique, double noteTechnique, double noteTactique,
                      String commentaire, Integer entrainementId, Integer joueurId) {
        this.notePhysique = notePhysique;
        this.noteTechnique = noteTechnique;
        this.noteTactique = noteTactique;
        this.commentaire = commentaire;
        this.entrainementId = entrainementId;
        this.joueurId = joueurId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public double getNotePhysique() { return notePhysique; }
    public void setNotePhysique(double notePhysique) { this.notePhysique = notePhysique; }

    public double getNoteTechnique() { return noteTechnique; }
    public void setNoteTechnique(double noteTechnique) { this.noteTechnique = noteTechnique; }

    public double getNoteTactique() { return noteTactique; }
    public void setNoteTactique(double noteTactique) { this.noteTactique = noteTactique; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Integer getEntrainementId() { return entrainementId; }
    public void setEntrainementId(Integer entrainementId) { this.entrainementId = entrainementId; }

    public Integer getJoueurId() { return joueurId; }
    public void setJoueurId(Integer joueurId) { this.joueurId = joueurId; }

    @Override
    public String toString() {
        return "Evaluation{" +
                "id=" + id +
                ", notePhysique=" + notePhysique +
                ", noteTechnique=" + noteTechnique +
                ", noteTactique=" + noteTactique +
                ", commentaire='" + commentaire + '\'' +
                ", entrainementId=" + entrainementId +
                ", joueurId=" + joueurId +
                '}';
    }
}
