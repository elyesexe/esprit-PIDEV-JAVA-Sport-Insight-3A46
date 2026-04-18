package tn.esprit.services.football;

import java.util.Map;

public final class SportsCafeCompetitionMappings {
    private static final Map<String, CompetitionPath> COMPETITIONS = Map.of(
            "PL", new CompetitionPath("england", "premier-league"),
            "PD", new CompetitionPath("spain", "la-liga"),
            "BL1", new CompetitionPath("germany", "bundesliga"),
            "SA", new CompetitionPath("italy", "serie-a"),
            "FL1", new CompetitionPath("france", "ligue-1"),
            "CL", new CompetitionPath("international", "uefa-champions-league")
    );

    private SportsCafeCompetitionMappings() {
    }

    public static boolean supportsCompetition(String competitionCode) {
        return competitionPathOf(competitionCode) != null;
    }

    public static CompetitionPath competitionPathOf(String competitionCode) {
        if (competitionCode == null || competitionCode.isBlank()) {
            return null;
        }
        return COMPETITIONS.get(FootballDataCompetitions.normalizeCode(competitionCode));
    }

    public record CompetitionPath(String leagueSlug, String competitionSlug) {
    }
}
