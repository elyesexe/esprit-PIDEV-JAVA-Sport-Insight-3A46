package tn.esprit.entities;

public class Evaluation {
    private Integer id;
    private double notePhysique;
    private double noteTechnique;
    private double noteTactique;
    private String commentaire;
    private Integer entrainementId;
    private Integer joueurId;

    public Evaluation() {
    }

    public Evaluation(double notePhysique, double noteTechnique, double noteTactique, String commentaire, Integer entrainementId, Integer joueurId) {
        this.notePhysique = notePhysique;
        this.noteTechnique = noteTechnique;
        this.noteTactique = noteTactique;
        this.commentaire = commentaire;
        this.entrainementId = entrainementId;
        this.joueurId = joueurId;
    }
}
