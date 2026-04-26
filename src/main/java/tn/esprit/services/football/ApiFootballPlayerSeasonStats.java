package tn.esprit.services.football;

public record ApiFootballPlayerSeasonStats(
        String playerName,
        String teamName,
        String competitionName,
        Integer seasonYear,
        Integer appearances,
        Integer goals,
        Integer assists,
        Integer yellowCards,
        Integer redCards,
        Integer minutes,
        String photoUrl
) {
}
