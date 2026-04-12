package tn.esprit.services.football;

import java.util.List;

public record LeagueStandingEntry(
        int position,
        long teamId,
        String teamName,
        String teamShortName,
        String teamTla,
        String teamCrest,
        int playedGames,
        int won,
        int draw,
        int lost,
        int points,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        List<String> form
) {
    public String displayName() {
        if (teamShortName != null && !teamShortName.isBlank()) {
            return teamShortName;
        }
        return teamName == null ? "" : teamName;
    }

    public String goalsSummary() {
        return goalsFor + ":" + goalsAgainst;
    }

    public String goalDifferenceSummary() {
        return goalDifference > 0 ? "+" + goalDifference : String.valueOf(goalDifference);
    }

    public boolean hasForm() {
        return form != null && !form.isEmpty();
    }
}
