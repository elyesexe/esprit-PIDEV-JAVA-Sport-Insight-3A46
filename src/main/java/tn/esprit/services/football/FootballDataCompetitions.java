package tn.esprit.services.football;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public final class FootballDataCompetitions {
    public static final String ALL_LABEL = "Top 5 + UEFA Champions League";
    public static final List<String> DEFAULT_CODES = List.of("PL", "PD", "BL1", "SA", "FL1", "CL");
    public static final List<String> TEAM_CODES = List.of("PL", "PD", "BL1", "SA", "FL1");

    private static final Map<String, String> LABELS = createLabels();
    private static final Map<String, String> LOGO_RESOURCES = createLogoResources();

    private FootballDataCompetitions() {
    }

    public static Map<String, String> labels() {
        return LABELS;
    }

    public static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    public static String labelOf(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return ALL_LABEL;
        }
        return LABELS.getOrDefault(normalizedCode, normalizedCode);
    }

    public static String logoResourceOf(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return null;
        }
        return LOGO_RESOURCES.get(normalizedCode);
    }

    public static boolean isTeamCompetition(String code) {
        String normalizedCode = normalizeCode(code);
        return normalizedCode != null && TEAM_CODES.contains(normalizedCode);
    }

    private static Map<String, String> createLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("PL", "Premier League");
        labels.put("PD", "La Liga");
        labels.put("BL1", "Bundesliga");
        labels.put("SA", "Serie A");
        labels.put("FL1", "Ligue 1");
        labels.put("CL", "UEFA Champions League");
        return Collections.unmodifiableMap(labels);
    }

    private static Map<String, String> createLogoResources() {
        Map<String, String> logos = new LinkedHashMap<>();
        logos.put("BL1", "/tn/esprit/logos/Bundesliga.png");
        logos.put("PD", "/tn/esprit/logos/La liga.png");
        logos.put("FL1", "/tn/esprit/logos/Ligue 1.jpg");
        logos.put("PL", "/tn/esprit/logos/Premier League.png");
        logos.put("SA", "/tn/esprit/logos/Serie A.png");
        logos.put("CL", "/tn/esprit/logos/UCL.jpg");
        return Collections.unmodifiableMap(logos);
    }
}
