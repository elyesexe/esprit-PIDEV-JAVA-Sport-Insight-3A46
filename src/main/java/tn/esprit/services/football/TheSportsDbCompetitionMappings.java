package tn.esprit.services.football;

import java.util.Map;

public final class TheSportsDbCompetitionMappings {
    private static final Map<String, String> LEAGUE_QUERIES = Map.of(
            "PL", "English_Premier_League",
            "PD", "Spanish_La_Liga",
            "BL1", "German_Bundesliga",
            "SA", "Italian_Serie_A",
            "FL1", "French_Ligue_1"
    );

    private TheSportsDbCompetitionMappings() {
    }

    public static String leagueQueryOf(String competitionCode) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (normalizedCode == null) {
            return null;
        }
        return LEAGUE_QUERIES.get(normalizedCode);
    }
}
