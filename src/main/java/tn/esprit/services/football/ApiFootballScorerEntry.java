package tn.esprit.services.football;

public record ApiFootballScorerEntry(
        int rank,
        String playerName,
        String teamName,
        Integer goals,
        Integer assists,
        Integer appearances,
        Integer minutes
) {
}
