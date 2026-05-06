package tn.esprit.services.football;

public record ApiFootballPlayerMatchStats(
        Long playerId,
        Long teamId,
        String playerName,
        String photoUrl,
        String teamName,
        String teamLogoUrl,
        String side,
        String position,
        Integer shirtNumber,
        Integer minutes,
        Double rating,
        Double expectedGoals,
        Integer totalShots,
        Integer shotsOnTarget,
        Integer touches,
        Integer passes,
        Integer keyPasses,
        Integer passAccuracyPercent,
        Integer dribblesSuccess,
        Integer dribblesAttempts,
        Integer duelsWon,
        Integer duelsTotal,
        Integer tackles,
        Integer interceptions,
        Integer foulsDrawn,
        Integer foulsCommitted,
        Integer goals,
        Integer assists,
        Integer saves,
        String sourceLabel
) {
    public boolean hasMetricData() {
        return rating != null
                || expectedGoals != null
                || positive(totalShots)
                || positive(shotsOnTarget)
                || positive(touches)
                || positive(passes)
                || positive(keyPasses)
                || positive(dribblesSuccess)
                || positive(dribblesAttempts)
                || positive(duelsWon)
                || positive(duelsTotal)
                || positive(tackles)
                || positive(interceptions)
                || positive(foulsDrawn)
                || positive(foulsCommitted)
                || positive(goals)
                || positive(assists)
                || positive(saves);
    }

    public String displayPosition() {
        return position == null || position.isBlank() ? "Player" : position;
    }

    public boolean homeSide() {
        return "home".equalsIgnoreCase(side);
    }

    public boolean awaySide() {
        return "away".equalsIgnoreCase(side);
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
