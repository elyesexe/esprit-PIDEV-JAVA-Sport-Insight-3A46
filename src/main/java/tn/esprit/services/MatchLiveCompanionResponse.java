package tn.esprit.services;

import java.util.List;

public record MatchLiveCompanionResponse(
        int matchId,
        String score,
        String status,
        int minute,
        Momentum momentum,
        String dangerLevel,
        List<String> turningPoints,
        List<PlayerImpact> topImpacts,
        int intensityScore,
        String summary
) {
    public record Momentum(String dominantTeam, int homePressure, int awayPressure) {
    }

    public record PlayerImpact(String player, String team, double impactScore) {
    }
}
