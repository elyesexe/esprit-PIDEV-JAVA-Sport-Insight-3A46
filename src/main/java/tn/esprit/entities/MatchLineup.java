package tn.esprit.entities;

public class MatchLineup {
    private Integer id;
    private String type;
    private Integer buts;
    private Integer cartonsJaunes;
    private Integer cartonsRouges;
    private Double positionX;
    private Double positionY;
    private Integer matchsId;
    private Integer joueurId;

    public MatchLineup() {
    }

    public MatchLineup(String type, Integer buts, Integer cartonsJaunes, Integer cartonsRouges, Double positionX, Double positionY, Integer matchsId, Integer joueurId) {
        this.type = type;
        this.buts = buts;
        this.cartonsJaunes = cartonsJaunes;
        this.cartonsRouges = cartonsRouges;
        this.positionX = positionX;
        this.positionY = positionY;
        this.matchsId = matchsId;
        this.joueurId = joueurId;
    }
}
