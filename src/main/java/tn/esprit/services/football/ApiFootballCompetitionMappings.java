package tn.esprit.services.football;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiFootballCompetitionMappings {
    private static final Map<String, CompetitionMapping> MAPPINGS = createMappings();

    private ApiFootballCompetitionMappings() {
    }

    public static boolean supportsCompetition(String competitionCode) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        return normalizedCode != null && MAPPINGS.containsKey(normalizedCode);
    }

    public static Integer leagueIdOf(String competitionCode) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (normalizedCode == null) {
            return null;
        }
        CompetitionMapping mapping = MAPPINGS.get(normalizedCode);
        return mapping == null ? null : mapping.leagueId();
    }

    public static String labelOf(String competitionCode) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (normalizedCode == null) {
            return FootballDataCompetitions.labelOf(competitionCode);
        }
        CompetitionMapping mapping = MAPPINGS.get(normalizedCode);
        return mapping == null ? FootballDataCompetitions.labelOf(competitionCode) : mapping.label();
    }

    public static int resolveSeasonYear(String competitionCode, LocalDate referenceDate) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        CompetitionMapping mapping = normalizedCode == null ? null : MAPPINGS.get(normalizedCode);
        if (mapping == null) {
            throw new IllegalArgumentException("Competition non supportee par API-Football: " + competitionCode);
        }

        LocalDate safeDate = referenceDate == null ? LocalDate.now() : referenceDate;
        return safeDate.getMonthValue() >= mapping.seasonStartMonth()
                ? safeDate.getYear()
                : safeDate.getYear() - 1;
    }

    private static Map<String, CompetitionMapping> createMappings() {
        Map<String, CompetitionMapping> mappings = new LinkedHashMap<>();
        mappings.put("PL", new CompetitionMapping(39, "Premier League", 7));
        mappings.put("PD", new CompetitionMapping(140, "La Liga", 7));
        mappings.put("BL1", new CompetitionMapping(78, "Bundesliga", 7));
        mappings.put("SA", new CompetitionMapping(135, "Serie A", 7));
        mappings.put("FL1", new CompetitionMapping(61, "Ligue 1", 7));
        mappings.put("CL", new CompetitionMapping(2, "UEFA Champions League", 7));
        return Map.copyOf(mappings);
    }

    private record CompetitionMapping(int leagueId, String label, int seasonStartMonth) {
    }
}
