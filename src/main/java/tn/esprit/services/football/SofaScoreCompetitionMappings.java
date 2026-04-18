package tn.esprit.services.football;

import java.util.Map;

public final class SofaScoreCompetitionMappings {
    private static final Map<String, CompetitionDescriptor> COMPETITIONS = Map.of(
            "PL", new CompetitionDescriptor("england", "premier-league", "Premier League"),
            "PD", new CompetitionDescriptor("spain", "laliga", "LaLiga"),
            "BL1", new CompetitionDescriptor("germany", "bundesliga", "Bundesliga"),
            "SA", new CompetitionDescriptor("italy", "serie-a", "Serie A"),
            "FL1", new CompetitionDescriptor("france", "ligue-1", "Ligue 1"),
            "CL", new CompetitionDescriptor("europe", "uefa-champions-league", "UEFA Champions League")
    );

    private SofaScoreCompetitionMappings() {
    }

    public static boolean supportsCompetition(String competitionCode) {
        return descriptorOf(competitionCode) != null;
    }

    public static CompetitionDescriptor descriptorOf(String competitionCode) {
        if (competitionCode == null || competitionCode.isBlank()) {
            return null;
        }
        return COMPETITIONS.get(FootballDataCompetitions.normalizeCode(competitionCode));
    }

    public record CompetitionDescriptor(String categorySlug, String tournamentSlug, String tournamentName) {
    }
}
