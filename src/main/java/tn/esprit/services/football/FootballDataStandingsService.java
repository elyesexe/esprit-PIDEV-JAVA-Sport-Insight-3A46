package tn.esprit.services.football;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FootballDataStandingsService {
    private final FootballDataApiClient apiClient;

    public FootballDataStandingsService() {
        this.apiClient = new FootballDataApiClient();
    }

    public LeagueStandingsSnapshot fetchStandings(String competitionCode) throws IOException, InterruptedException {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        JsonNode payload = apiClient.fetchCompetitionStandings(normalizedCode);

        JsonNode selectedStanding = selectStanding(payload.path("standings"));
        List<LeagueStandingEntry> table = parseTable(selectedStanding.path("table"));
        String competitionLabel = FootballDataCompetitions.labelOf(normalizedCode);
        String areaName = text(payload.path("area"), "name");
        JsonNode seasonNode = payload.path("season");

        return new LeagueStandingsSnapshot(
                normalizedCode,
                competitionLabel,
                areaName,
                prettifyEnum(text(selectedStanding, "stage")),
                text(selectedStanding, "type"),
                integerOrNull(seasonNode, "currentMatchday"),
                text(seasonNode, "startDate"),
                text(seasonNode, "endDate"),
                table
        );
    }

    private JsonNode selectStanding(JsonNode standingsNode) {
        if (!standingsNode.isArray() || standingsNode.isEmpty()) {
            return standingsNode;
        }

        for (JsonNode standingNode : standingsNode) {
            if ("TOTAL".equalsIgnoreCase(text(standingNode, "type"))) {
                return standingNode;
            }
        }

        return standingsNode.get(0);
    }

    private List<LeagueStandingEntry> parseTable(JsonNode tableNode) {
        if (!tableNode.isArray()) {
            return Collections.emptyList();
        }

        List<LeagueStandingEntry> rows = new ArrayList<>();
        for (JsonNode rowNode : tableNode) {
            JsonNode teamNode = rowNode.path("team");
            rows.add(new LeagueStandingEntry(
                    rowNode.path("position").asInt(0),
                    teamNode.path("id").asLong(0),
                    text(teamNode, "name"),
                    text(teamNode, "shortName"),
                    text(teamNode, "tla"),
                    text(teamNode, "crest"),
                    rowNode.path("playedGames").asInt(0),
                    rowNode.path("won").asInt(0),
                    rowNode.path("draw").asInt(0),
                    rowNode.path("lost").asInt(0),
                    rowNode.path("points").asInt(0),
                    rowNode.path("goalsFor").asInt(0),
                    rowNode.path("goalsAgainst").asInt(0),
                    rowNode.path("goalDifference").asInt(0),
                    splitForm(text(rowNode, "form"))
            ));
        }
        return rows;
    }

    private List<String> splitForm(String formValue) {
        if (formValue == null || formValue.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String part : formValue.split(",")) {
            String normalizedPart = part == null ? null : part.trim().toUpperCase(Locale.ROOT);
            if (normalizedPart != null && !normalizedPart.isBlank()) {
                tokens.add(normalizedPart);
            }
        }
        return tokens;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        return normalizeNullable(node.path(fieldName).asText(null));
    }

    private Integer integerOrNull(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.path(fieldName);
        return child.isNumber() ? child.asInt() : null;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String prettifyEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
