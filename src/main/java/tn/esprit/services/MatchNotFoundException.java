package tn.esprit.services;

public class MatchNotFoundException extends IllegalArgumentException {
    private final int matchId;

    public MatchNotFoundException(int matchId) {
        super("Match not found.");
        this.matchId = matchId;
    }

    public int getMatchId() {
        return matchId;
    }
}
