package tn.esprit.services.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Matchs;
import tn.esprit.tools.MyConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ApiFootballOddsService {
    private static final int MAX_MARKETS = 3;
    private static final int MAX_BOOKMAKER_ROWS = 3;
    private static final String THE_ODDS_API_REGIONS = "eu";
    private static final String THE_ODDS_API_MARKETS = "h2h,spreads,totals";
    private static final Map<String, String> THE_ODDS_API_SPORT_KEYS = Map.of(
            "PL", "soccer_epl",
            "PD", "soccer_spain_la_liga",
            "BL1", "soccer_germany_bundesliga",
            "SA", "soccer_italy_serie_a",
            "FL1", "soccer_france_ligue_one",
            "CL", "soccer_uefa_champs_league"
    );
    private static final List<String> BOOKMAKER_PRIORITY = List.of(
            "Bet365",
            "Bwin",
            "Unibet",
            "1xBet",
            "Betfair",
            "Pinnacle",
            "Marathonbet"
    );
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ApiFootballClient apiClient;
    private final String configurationError;
    private final TheOddsApiClient theOddsApiClient;
    private final String theOddsApiConfigurationError;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiFootballOddsService() {
        ApiFootballClient resolvedClient = null;
        String error = null;
        try {
            resolvedClient = new ApiFootballClient();
        } catch (IllegalStateException exception) {
            error = exception.getMessage();
        }
        this.apiClient = resolvedClient;
        this.configurationError = error;

        TheOddsApiClient resolvedTheOddsApiClient = null;
        String fallbackError = null;
        try {
            resolvedTheOddsApiClient = new TheOddsApiClient();
        } catch (IllegalStateException exception) {
            fallbackError = exception.getMessage();
        }
        this.theOddsApiClient = resolvedTheOddsApiClient;
        this.theOddsApiConfigurationError = fallbackError;
    }

    public ApiFootballOddsSnapshot loadMatchOdds(Matchs match, String homeName, String awayName)
            throws IOException, InterruptedException {
        if (match == null) {
            return unavailable("Match indisponible.", "Odds", false, false);
        }

        ApiFootballOddsSnapshot storedSnapshot = readStoredOddsSnapshot(match);
        if (isFinished(match) && storedSnapshot != null && storedSnapshot.hasMarkets()) {
            return storedOddsSnapshot(
                    storedSnapshot,
                    match,
                    homeName,
                    awayName,
                    true,
                    "Cotes conservees en base avant la fin du match. Les free APIs ne gardent pas toujours l'historique apres le coup de sifflet final."
            );
        }

        String primaryMessage = null;
        ApiFootballOddsSnapshot apiFootballSnapshot = null;
        if (apiClient == null) {
            primaryMessage = "API-Football non configure: " + compactConfigHint();
        } else if (match.getApiFootballId() == null) {
            primaryMessage = "Identifiant API-Football manquant; bascule vers The Odds API par equipes/date.";
        } else {
            try {
                apiFootballSnapshot = loadApiFootballFixtureOdds(match, homeName, awayName);
                if (apiFootballSnapshot.hasMarkets()) {
                    persistOddsSnapshot(match, apiFootballSnapshot);
                    return apiFootballSnapshot;
                }
                primaryMessage = apiFootballSnapshot.message();
            } catch (IOException exception) {
                primaryMessage = "API-Football indisponible: " + exception.getMessage();
            }
        }

        ApiFootballOddsSnapshot fallbackSnapshot = loadTheOddsApiFallback(match, homeName, awayName, primaryMessage);
        if (fallbackSnapshot.hasMarkets()) {
            persistOddsSnapshot(match, fallbackSnapshot);
            return fallbackSnapshot;
        }

        if (storedSnapshot != null && storedSnapshot.hasMarkets()) {
            return storedOddsSnapshot(
                    storedSnapshot,
                    match,
                    homeName,
                    awayName,
                    isFinished(match),
                    emptyToString(fallbackSnapshot.message())
            );
        }

        if (apiFootballSnapshot != null && apiFootballSnapshot.hasMarkets()) {
            persistOddsSnapshot(match, apiFootballSnapshot);
            return apiFootballSnapshot;
        }
        return fallbackSnapshot;
    }

    private ApiFootballOddsSnapshot readStoredOddsSnapshot(Matchs match) {
        if (match == null) {
            return null;
        }

        String json = match.getOddsSnapshotJson();
        if ((json == null || json.isBlank()) && match.getId() != null) {
            try {
                Connection connection = MyConnection.getInstance().getConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT odds_snapshot_json, odds_source, odds_synced_at FROM matchs WHERE id = ?"
                )) {
                    statement.setInt(1, match.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            json = resultSet.getString("odds_snapshot_json");
                            match.setOddsSnapshotJson(json);
                            match.setOddsSource(resultSet.getString("odds_source"));
                            Timestamp syncedAt = resultSet.getTimestamp("odds_synced_at");
                            match.setOddsSyncedAt(syncedAt == null ? null : syncedAt.toLocalDateTime());
                        }
                    }
                }
            } catch (SQLException ignored) {
                return null;
            }
        }

        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ApiFootballOddsSnapshot.class);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void persistOddsSnapshot(Matchs match, ApiFootballOddsSnapshot snapshot) {
        if (match == null || match.getId() == null || snapshot == null || !snapshot.hasMarkets()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(snapshot);
            LocalDateTime syncedAt = LocalDateTime.now();
            Connection connection = MyConnection.getInstance().getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE matchs SET odds_snapshot_json = ?, odds_source = ?, odds_synced_at = ? WHERE id = ?"
            )) {
                statement.setString(1, json);
                statement.setString(2, firstNonBlank(snapshot.sourceLabel(), "Stored odds"));
                statement.setTimestamp(3, Timestamp.valueOf(syncedAt));
                statement.setInt(4, match.getId());
                statement.executeUpdate();
            }
            match.setOddsSnapshotJson(json);
            match.setOddsSource(firstNonBlank(snapshot.sourceLabel(), "Stored odds"));
            match.setOddsSyncedAt(syncedAt);
        } catch (IOException | SQLException exception) {
            System.err.println("Odds cache skipped: " + exception.getMessage());
        }
    }

    private ApiFootballOddsSnapshot storedOddsSnapshot(
            ApiFootballOddsSnapshot storedSnapshot,
            Matchs match,
            String homeName,
            String awayName,
            boolean locked,
            String detail
    ) {
        boolean live = isLive(match);
        String source = firstNonBlank(match == null ? null : match.getOddsSource(), storedSnapshot.sourceLabel(), "Stored odds");
        String update = firstNonBlank(
                storedSnapshot.updatedAt(),
                formatStoredTimestamp(match == null ? null : match.getOddsSyncedAt()),
                "Derniere mise a jour inconnue"
        );
        String message = locked
                ? "Cotes finales conservees en base locale."
                : "Affichage des dernieres cotes conservees en base locale.";
        if (detail != null && !detail.isBlank()) {
            message = message + " " + detail.trim();
        } else if (storedSnapshot.message() != null && !storedSnapshot.message().isBlank()) {
            message = message + " " + storedSnapshot.message().trim();
        }

        return new ApiFootballOddsSnapshot(
                "Base locale - " + source,
                locked ? "CLOSED ODDS" : (live ? "LIVE ODDS" : "PRE-MATCH ODDS"),
                locked ? "Finished" : (live ? "Stored live" : "Stored"),
                update,
                message,
                storedSnapshot.apiBacked(),
                locked || storedSnapshot.locked(),
                storedSnapshot.markets(),
                buildGestureInsight(match, homeName, awayName, live, locked || storedSnapshot.locked(), storedSnapshot.markets())
        );
    }

    private String formatStoredTimestamp(LocalDateTime value) {
        return value == null ? null : DISPLAY_TIME_FORMAT.format(value);
    }

    private ApiFootballOddsSnapshot loadApiFootballFixtureOdds(Matchs match, String homeName, String awayName)
            throws IOException, InterruptedException {
        long fixtureId = match.getApiFootballId();
        boolean live = isLive(match);
        boolean finished = isFinished(match);
        IOException liveError = null;

        if (live) {
            try {
                ApiFootballOddsSnapshot liveSnapshot = parsePayload(
                        apiClient.fetchLiveFixtureOdds(fixtureId),
                        "API-Football live odds",
                        "LIVE ODDS",
                        "Live",
                        "Cotes live actualisees par API-Football.",
                        true,
                        false,
                        match,
                        homeName,
                        awayName
                );
                if (liveSnapshot.hasMarkets()) {
                    return liveSnapshot;
                }
            } catch (IOException exception) {
                liveError = exception;
            }
        }

        try {
            ApiFootballOddsSnapshot preMatchSnapshot = parsePayload(
                    apiClient.fetchFixtureOdds(fixtureId),
                    "API-Football pre-match odds",
                    finished ? "CLOSED ODDS" : "PRE-MATCH ODDS",
                    finished ? "Finished" : (live ? "Pre-match fallback" : "Programmed"),
                    finished
                            ? "Le match est termine. Les cotes API sont affichees seulement si elles sont encore dans la fenetre gratuite."
                            : (live
                            ? "Les cotes live ne sont pas disponibles pour ce match, affichage du dernier marche pre-match."
                            : "Cotes pre-match fournies par API-Football."),
                    false,
                    finished,
                    match,
                    homeName,
                    awayName
            );
            if (preMatchSnapshot.hasMarkets() || liveError == null) {
                return preMatchSnapshot;
            }
        } catch (IOException exception) {
            if (liveError != null) {
                exception.addSuppressed(liveError);
            }
            throw exception;
        }

        return unavailable(
                "Aucune cote API disponible pour ce match.",
                finished
                        ? "La fenetre gratuite d'API-Football conserve peu d'historique apres la fin du match."
                        : "API-Football n'a pas encore publie de marche pour cette fixture.",
                true,
                finished
        );
    }

    private ApiFootballOddsSnapshot loadTheOddsApiFallback(
            Matchs match,
            String homeName,
            String awayName,
            String primaryMessage
    ) throws IOException, InterruptedException {
        boolean live = isLive(match);
        boolean finished = isFinished(match);
        if (theOddsApiClient == null) {
            return providerUnavailable(
                    "The Odds API fallback",
                    finished ? "CLOSED ODDS" : "ODDS",
                    finished ? "Finished" : "Unavailable",
                    "The Odds API fallback n'est pas configure.",
                    "Ajoutez THE_ODDS_API_KEY ou the-odds-api.local.properties. " + emptyToString(primaryMessage),
                    true,
                    finished
            );
        }

        String sportKey = resolveTheOddsApiSportKey(match);
        if (sportKey == null) {
            return providerUnavailable(
                    "The Odds API fallback",
                    finished ? "CLOSED ODDS" : "ODDS",
                    finished ? "Finished" : "Unavailable",
                    "Competition non supportee par The Odds API fallback.",
                    "Competition locale: " + blankToFallback(match == null ? null : match.getCompetitionCode(), "inconnue") + ". " + emptyToString(primaryMessage),
                    true,
                    finished
            );
        }

        JsonNode payload = theOddsApiClient.fetchOdds(sportKey, THE_ODDS_API_REGIONS, THE_ODDS_API_MARKETS);
        JsonNode eventNode = findBestTheOddsApiEvent(payload, match, homeName, awayName);
        if (eventNode == null) {
            return providerUnavailable(
                    "The Odds API fallback",
                    finished ? "CLOSED ODDS" : (live ? "LIVE ODDS" : "PRE-MATCH ODDS"),
                    finished ? "Finished" : (live ? "Live" : "Programmed"),
                    "The Odds API n'a pas trouve ce match dans les evenements live/upcoming.",
                    "Recherche: " + blankToFallback(homeName, "Domicile") + " vs " + blankToFallback(awayName, "Exterieur")
                            + ". " + emptyToString(primaryMessage),
                    true,
                    finished
            );
        }

        ApiFootballOddsSnapshot snapshot = parseTheOddsApiEvent(eventNode, match, homeName, awayName, live, finished, primaryMessage);
        return snapshot.hasMarkets()
                ? snapshot
                : providerUnavailable(
                "The Odds API fallback",
                finished ? "CLOSED ODDS" : (live ? "LIVE ODDS" : "PRE-MATCH ODDS"),
                finished ? "Finished" : (live ? "Live" : "Programmed"),
                "The Odds API a trouve le match, mais aucun marche h2h/spreads/totals n'est disponible.",
                emptyToString(primaryMessage),
                true,
                finished
        );
    }

    private ApiFootballOddsSnapshot parsePayload(
            JsonNode payload,
            String sourceLabel,
            String stateLabel,
            String statusLabel,
            String emptyMessage,
            boolean live,
            boolean locked,
            Matchs match,
            String homeName,
            String awayName
    ) {
        List<JsonNode> responseNodes = asNodeList(payload == null ? null : payload.path("response"));
        Map<String, MarketBuilder> marketsByName = new LinkedHashMap<>();
        String updatedAt = null;
        boolean anySuspended = false;

        for (JsonNode responseNode : responseNodes) {
            updatedAt = firstNonBlank(updatedAt, extractUpdatedAt(responseNode));
            anySuspended = anySuspended || responseNode.path("status").path("blocked").asBoolean(false);
            anySuspended = anySuspended || responseNode.path("status").path("stopped").asBoolean(false);
            parseResponseNode(responseNode, marketsByName, live, homeName, awayName);
        }

        List<ApiFootballOddsSnapshot.Market> markets = marketsByName.values().stream()
                .sorted(Comparator.comparingInt(builder -> marketPriority(builder.name)))
                .limit(MAX_MARKETS)
                .map(MarketBuilder::build)
                .filter(market -> market.rows() != null && !market.rows().isEmpty())
                .toList();

        String message = markets.isEmpty()
                ? emptyMessage
                : markets.size() + " marche(s) API disponibles via la free API.";
        if (anySuspended && !locked) {
            message = message + " Certains prix sont suspendus par le bookmaker.";
        }

        return new ApiFootballOddsSnapshot(
                sourceLabel,
                stateLabel,
                statusLabel,
                updatedAt == null ? "Derniere mise a jour inconnue" : updatedAt,
                message,
                true,
                locked,
                markets,
                buildGestureInsight(match, homeName, awayName, live, locked, markets)
        );
    }

    private ApiFootballOddsSnapshot parseTheOddsApiEvent(
            JsonNode eventNode,
            Matchs match,
            String homeName,
            String awayName,
            boolean live,
            boolean locked,
            String primaryMessage
    ) {
        Map<String, MarketBuilder> marketsByName = new LinkedHashMap<>();
        String updatedAt = null;
        String eventHomeName = text(eventNode, "home_team");
        String eventAwayName = text(eventNode, "away_team");

        JsonNode bookmakersNode = eventNode.path("bookmakers");
        if (bookmakersNode.isArray()) {
            for (JsonNode bookmakerNode : bookmakersNode) {
                String bookmaker = firstNonBlank(text(bookmakerNode, "title"), text(bookmakerNode, "key"), "Bookmaker");
                updatedAt = firstNonBlank(updatedAt, formatProviderTimestamp(text(bookmakerNode, "last_update")));

                JsonNode marketsNode = bookmakerNode.path("markets");
                if (!marketsNode.isArray()) {
                    continue;
                }

                for (JsonNode marketNode : marketsNode) {
                    String marketKey = text(marketNode, "key");
                    String marketName = mapTheOddsMarketName(marketKey);
                    if (marketName == null) {
                        continue;
                    }
                    updatedAt = firstNonBlank(updatedAt, formatProviderTimestamp(text(marketNode, "last_update")));
                    List<ApiFootballOddsSnapshot.Selection> selections = parseTheOddsSelections(
                            marketNode,
                            bookmaker,
                            marketKey,
                            eventHomeName,
                            eventAwayName,
                            live
                    );
                    if (selections.isEmpty()) {
                        continue;
                    }
                    MarketBuilder builder = marketsByName.computeIfAbsent(marketName, MarketBuilder::new);
                    builder.addRow(bookmaker, selections);
                }
            }
        }

        List<ApiFootballOddsSnapshot.Market> markets = marketsByName.values().stream()
                .sorted(Comparator.comparingInt(builder -> marketPriority(builder.name)))
                .limit(MAX_MARKETS)
                .map(MarketBuilder::build)
                .filter(market -> market.rows() != null && !market.rows().isEmpty())
                .toList();

        String matchedEvent = blankToFallback(eventHomeName, "Domicile")
                + " vs "
                + blankToFallback(eventAwayName, "Exterieur");
        String message = markets.isEmpty()
                ? "The Odds API a trouve " + matchedEvent + ", mais aucun marche afficheable n'est disponible."
                : markets.size() + " marche(s) trouves via The Odds API fallback pour " + matchedEvent + ".";
        if (primaryMessage != null && !primaryMessage.isBlank()) {
            message = message + " " + primaryMessage;
        }

        return new ApiFootballOddsSnapshot(
                "The Odds API fallback",
                locked ? "CLOSED ODDS" : (live ? "LIVE ODDS" : "PRE-MATCH ODDS"),
                locked ? "Finished" : (live ? "Live" : "Programmed"),
                updatedAt == null ? "Derniere mise a jour inconnue" : updatedAt,
                message,
                true,
                locked,
                markets,
                buildGestureInsight(match, homeName, awayName, live, locked, markets)
        );
    }

    private List<ApiFootballOddsSnapshot.Selection> parseTheOddsSelections(
            JsonNode marketNode,
            String bookmaker,
            String marketKey,
            String eventHomeName,
            String eventAwayName,
            boolean live
    ) {
        JsonNode outcomesNode = marketNode.path("outcomes");
        if (!outcomesNode.isArray()) {
            return List.of();
        }

        List<ApiFootballOddsSnapshot.Selection> selections = new ArrayList<>();
        for (JsonNode outcomeNode : outcomesNode) {
            String rawName = text(outcomeNode, "name");
            String price = formatOddValue(outcomeNode.path("price"));
            if (rawName == null || price == null) {
                continue;
            }
            String point = formatPointValue(outcomeNode.path("point"));
            String label = formatTheOddsOutcomeLabel(marketKey, rawName, point, eventHomeName, eventAwayName);
            selections.add(new ApiFootballOddsSnapshot.Selection(
                    label,
                    price,
                    resolveTrend(price, bookmaker, label, live, false),
                    false,
                    true
            ));
        }
        return selections.stream()
                .limit(4)
                .toList();
    }

    private JsonNode findBestTheOddsApiEvent(JsonNode payload, Matchs match, String homeName, String awayName) {
        if (payload == null || !payload.isArray()) {
            return null;
        }

        JsonNode bestNode = null;
        double bestScore = 0.0;
        for (JsonNode eventNode : payload) {
            double score = scoreTheOddsApiEvent(eventNode, match, homeName, awayName);
            if (score > bestScore) {
                bestScore = score;
                bestNode = eventNode;
            }
        }
        return bestScore >= 0.58 ? bestNode : null;
    }

    private double scoreTheOddsApiEvent(JsonNode eventNode, Matchs match, String homeName, String awayName) {
        String eventHomeName = text(eventNode, "home_team");
        String eventAwayName = text(eventNode, "away_team");
        double directScore = (teamSimilarity(homeName, eventHomeName) + teamSimilarity(awayName, eventAwayName)) / 2.0;
        double swappedScore = (teamSimilarity(homeName, eventAwayName) + teamSimilarity(awayName, eventHomeName)) / 2.0;
        double teamScore = Math.max(directScore, swappedScore);
        double dateScore = dateClosenessScore(kickoffDateTimeOf(match), parseProviderDateTime(text(eventNode, "commence_time")));
        return teamScore * 0.78 + dateScore * 0.22;
    }

    private double dateClosenessScore(LocalDateTime expected, LocalDateTime actual) {
        if (expected == null || actual == null) {
            return 0.65;
        }
        long minutes = Math.abs(Duration.between(expected, actual).toMinutes());
        if (minutes <= 180) {
            return 1.0;
        }
        if (expected.toLocalDate().equals(actual.toLocalDate())) {
            return 0.85;
        }
        if (minutes <= 48L * 60L) {
            return 0.35;
        }
        return 0.0;
    }

    private LocalDateTime kickoffDateTimeOf(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        return match.getHeureDebut() == null
                ? match.getDateMatch().atStartOfDay()
                : match.getDateMatch().atTime(match.getHeureDebut());
    }

    private LocalDateTime parseProviderDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawValue)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(rawValue.replace("Z", ""));
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private String formatProviderTimestamp(String rawValue) {
        LocalDateTime dateTime = parseProviderDateTime(rawValue);
        if (dateTime != null) {
            return DISPLAY_TIME_FORMAT.format(dateTime);
        }
        return rawValue == null || rawValue.isBlank() ? null : rawValue.trim();
    }

    private double teamSimilarity(String left, String right) {
        String normalizedLeft = normalizeTeamName(left);
        String normalizedRight = normalizeTeamName(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return 0.0;
        }
        if (Objects.equals(normalizedLeft, normalizedRight)) {
            return 1.0;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return 0.92;
        }

        List<String> leftTokens = List.of(normalizedLeft.split("\\s+"));
        List<String> rightTokens = List.of(normalizedRight.split("\\s+"));
        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        long union = leftTokens.stream().distinct().count()
                + rightTokens.stream().filter(token -> !leftTokens.contains(token)).distinct().count();
        return union == 0 ? 0.0 : (double) overlap / union;
    }

    private String normalizeTeamName(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized
                .replace("munchen", "munich")
                .replace("muenchen", "munich")
                .replace("paris sg", "paris saint germain")
                .replace("psg", "paris saint germain")
                .replace("st germain", "saint germain");
        List<String> tokens = List.of(normalized.split("\\s+")).stream()
                .filter(token -> !isTeamNoiseToken(token))
                .toList();
        if (tokens.isEmpty()) {
            return normalized;
        }
        return String.join(" ", tokens);
    }

    private boolean isTeamNoiseToken(String token) {
        return token == null
                || token.isBlank()
                || "fc".equals(token)
                || "cf".equals(token)
                || "sc".equals(token)
                || "afc".equals(token)
                || "cfc".equals(token)
                || "club".equals(token)
                || "football".equals(token)
                || "futbol".equals(token)
                || "calcio".equals(token)
                || "de".equals(token)
                || "la".equals(token)
                || "the".equals(token);
    }

    private String resolveTheOddsApiSportKey(Matchs match) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(match == null ? null : match.getCompetitionCode());
        if (normalizedCode == null) {
            return null;
        }
        return THE_ODDS_API_SPORT_KEYS.get(normalizedCode);
    }

    private String mapTheOddsMarketName(String marketKey) {
        String normalized = normalize(marketKey);
        if ("h2h".equals(normalized)) {
            return "Match Winner";
        }
        if ("spreads".equals(normalized)) {
            return "Handicap";
        }
        if ("totals".equals(normalized)) {
            return "Over/Under";
        }
        return null;
    }

    private String formatTheOddsOutcomeLabel(String marketKey, String rawName, String point, String eventHomeName, String eventAwayName) {
        String normalizedMarket = normalize(marketKey);
        String normalizedName = normalize(rawName);
        if ("h2h".equals(normalizedMarket)) {
            if ("draw".equals(normalizedName)) {
                return "X";
            }
            if (teamSimilarity(rawName, eventHomeName) >= 0.82) {
                return "1";
            }
            if (teamSimilarity(rawName, eventAwayName) >= 0.82) {
                return "2";
            }
            return rawName;
        }
        if ("spreads".equals(normalizedMarket)) {
            String side = teamSimilarity(rawName, eventHomeName) >= 0.82
                    ? "1"
                    : (teamSimilarity(rawName, eventAwayName) >= 0.82 ? "2" : rawName);
            return point == null ? side : side + " " + point;
        }
        if ("totals".equals(normalizedMarket)) {
            return point == null ? rawName : rawName + " " + point;
        }
        return point == null ? rawName : rawName + " " + point;
    }

    private String formatOddValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return String.format(Locale.US, "%.2f", node.asDouble());
        }
        Double parsed = parseDouble(node.asText(null));
        return parsed == null ? null : String.format(Locale.US, "%.2f", parsed);
    }

    private String formatPointValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            double value = node.asDouble();
            return value > 0 ? "+" + trimDecimal(value) : trimDecimal(value);
        }
        String raw = node.asText(null);
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    private String trimDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void parseResponseNode(
            JsonNode responseNode,
            Map<String, MarketBuilder> marketsByName,
            boolean live,
            String homeName,
            String awayName
    ) {
        if (responseNode == null || responseNode.isNull() || responseNode.isMissingNode()) {
            return;
        }

        JsonNode bookmakersNode = responseNode.path("bookmakers");
        if (bookmakersNode.isArray()) {
            for (JsonNode bookmakerNode : bookmakersNode) {
                parseBookmakerNode(bookmakerNode, marketsByName, live, homeName, awayName);
            }
            return;
        }

        JsonNode oddsNode = responseNode.path("odds");
        if (oddsNode.isArray()) {
            parseBetsNode("API-Football live", oddsNode, marketsByName, live, homeName, awayName);
            return;
        }

        if (responseNode.path("bets").isArray()) {
            parseBookmakerNode(responseNode, marketsByName, live, homeName, awayName);
        }
    }

    private void parseBookmakerNode(
            JsonNode bookmakerNode,
            Map<String, MarketBuilder> marketsByName,
            boolean live,
            String homeName,
            String awayName
    ) {
        String bookmaker = firstNonBlank(
                text(bookmakerNode, "name"),
                text(bookmakerNode, "bookmaker"),
                text(bookmakerNode.path("bookmaker"), "name"),
                "Bookmaker"
        );

        JsonNode betsNode = bookmakerNode.path("bets");
        if (betsNode.isArray()) {
            parseBetsNode(bookmaker, betsNode, marketsByName, live, homeName, awayName);
            return;
        }

        JsonNode oddsNode = bookmakerNode.path("odds");
        if (oddsNode.isArray()) {
            parseBetsNode(bookmaker, oddsNode, marketsByName, live, homeName, awayName);
        }
    }

    private void parseBetsNode(
            String bookmaker,
            JsonNode betsNode,
            Map<String, MarketBuilder> marketsByName,
            boolean live,
            String homeName,
            String awayName
    ) {
        for (JsonNode betNode : betsNode) {
            String marketName = firstNonBlank(
                    text(betNode, "name"),
                    text(betNode, "label"),
                    text(betNode, "type")
            );
            if (marketName == null || !isWantedMarket(marketName)) {
                continue;
            }

            List<ApiFootballOddsSnapshot.Selection> selections = parseSelections(betNode, bookmaker, live, homeName, awayName);
            if (selections.isEmpty()) {
                continue;
            }

            String normalizedMarketName = normalizeMarketName(marketName);
            MarketBuilder builder = marketsByName.computeIfAbsent(normalizedMarketName, key -> new MarketBuilder(marketName));
            builder.addRow(bookmaker, selections);
        }
    }

    private List<ApiFootballOddsSnapshot.Selection> parseSelections(
            JsonNode betNode,
            String bookmaker,
            boolean live,
            String homeName,
            String awayName
    ) {
        JsonNode valuesNode = betNode.path("values");
        if (!valuesNode.isArray()) {
            valuesNode = betNode.path("odds");
        }
        if (!valuesNode.isArray()) {
            return List.of();
        }

        List<ApiFootballOddsSnapshot.Selection> selections = new ArrayList<>();
        for (JsonNode valueNode : valuesNode) {
            String rawLabel = firstNonBlank(
                    text(valueNode, "value"),
                    text(valueNode, "label"),
                    text(valueNode, "name"),
                    text(valueNode, "handicap")
            );
            String odd = firstNonBlank(
                    text(valueNode, "odd"),
                    text(valueNode, "odds"),
                    text(valueNode, "price")
            );
            if (rawLabel == null || odd == null) {
                continue;
            }

            boolean suspended = valueNode.path("suspended").asBoolean(false)
                    || valueNode.path("blocked").asBoolean(false)
                    || valueNode.path("stop").asBoolean(false);
            boolean main = !valueNode.has("main") || valueNode.path("main").asBoolean(true);
            selections.add(new ApiFootballOddsSnapshot.Selection(
                    formatSelectionLabel(rawLabel, homeName, awayName),
                    odd,
                    resolveTrend(odd, bookmaker, rawLabel, live, suspended),
                    suspended,
                    main
            ));
        }

        return selections.stream()
                .limit(4)
                .toList();
    }

    private ApiFootballOddsSnapshot.GestureInsight buildGestureInsight(
            Matchs match,
            String homeName,
            String awayName,
            boolean live,
            boolean locked,
            List<ApiFootballOddsSnapshot.Market> markets
    ) {
        String safeHome = blankToFallback(homeName, "Domicile");
        String safeAway = blankToFallback(awayName, "Exterieur");
        if (locked) {
            return new ApiFootballOddsSnapshot.GestureInsight(
                    "Replay gesture",
                    "Marche ferme: gardez le score final, les cotes restantes et les moments forts dans le meme geste de lecture.",
                    "Replay odds",
                    "Compare result",
                    100,
                    "closed"
            );
        }

        Integer homeScore = match == null ? null : match.getScoreEquipeDomicile();
        Integer awayScore = match == null ? null : match.getScoreEquipeExterieur();
        if (live && homeScore != null && awayScore != null && !Objects.equals(homeScore, awayScore)) {
            String leader = homeScore > awayScore ? safeHome : safeAway;
            return new ApiFootballOddsSnapshot.GestureInsight(
                    "Lead shield",
                    leader + " mene. Le gesture deck garde le prix du leader, le nul et le prochain but visibles ensemble pour suivre le risque sans changer d'onglet.",
                    "Pin leader",
                    "Watch draw swing",
                    82,
                    "live"
            );
        }
        if (live) {
            return new ApiFootballOddsSnapshot.GestureInsight(
                    "Momentum swipe",
                    "Match ouvert: utilisez le geste horizontal mental 1X2 -> Next goal -> Over/Under pour lire la pression plus vite qu'un tableau classique.",
                    "Track next goal",
                    "Freeze swing",
                    76,
                    "live"
            );
        }
        if (markets == null || markets.isEmpty()) {
            return new ApiFootballOddsSnapshot.GestureInsight(
                    "API watch",
                    "Les cotes ne sont pas encore publiees. Le panneau est pret et se remplira des que l'API-Football expose le marche.",
                    "Refresh odds",
                    "Sync fixture",
                    52,
                    "pending"
            );
        }
        return new ApiFootballOddsSnapshot.GestureInsight(
                "Scenario swipe",
                "Avant le coup d'envoi, comparez 1X2, handicap et Over/Under dans un seul flux au lieu d'ouvrir plusieurs panneaux.",
                "Compare markets",
                "Mark value",
                68,
                "prematch"
        );
    }

    private ApiFootballOddsSnapshot unavailable(String message, String detail, boolean apiBacked, boolean locked) {
        return new ApiFootballOddsSnapshot(
                apiBacked ? "API-Football odds" : "API-Football",
                locked ? "CLOSED ODDS" : "ODDS",
                locked ? "Finished" : "Unavailable",
                "Derniere mise a jour inconnue",
                message + (detail == null || detail.isBlank() ? "" : " " + detail),
                apiBacked,
                locked,
                List.of(),
                new ApiFootballOddsSnapshot.GestureInsight(
                        locked ? "Replay gesture" : "API watch",
                        detail == null || detail.isBlank() ? message : detail,
                        locked ? "Review result" : "Refresh odds",
                        "Sync fixture",
                        locked ? 100 : 40,
                        locked ? "closed" : "pending"
                )
        );
    }

    private ApiFootballOddsSnapshot providerUnavailable(
            String sourceLabel,
            String stateLabel,
            String statusLabel,
            String message,
            String detail,
            boolean apiBacked,
            boolean locked
    ) {
        String fullMessage = message + (detail == null || detail.isBlank() ? "" : " " + detail.trim());
        return new ApiFootballOddsSnapshot(
                sourceLabel,
                stateLabel,
                statusLabel,
                "Derniere mise a jour inconnue",
                fullMessage,
                apiBacked,
                locked,
                List.of(),
                new ApiFootballOddsSnapshot.GestureInsight(
                        locked ? "Replay gesture" : "API watch",
                        detail == null || detail.isBlank() ? message : detail.trim(),
                        locked ? "Review result" : "Refresh odds",
                        "Sync fixture",
                        locked ? 100 : 45,
                        locked ? "closed" : "pending"
                )
        );
    }

    private List<JsonNode> asNodeList(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        if (!node.isArray()) {
            return List.of(node);
        }
        List<JsonNode> nodes = new ArrayList<>();
        for (JsonNode child : node) {
            nodes.add(child);
        }
        return nodes;
    }

    private String extractUpdatedAt(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String raw = firstNonBlank(text(node, "update"), text(node, "updatedAt"), text(node, "last_update"));
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.replace("Z", "")).format(DISPLAY_TIME_FORMAT);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String compactConfigHint() {
        if (configurationError == null || configurationError.isBlank()) {
            return "Ajoutez API_FOOTBALL_KEY ou api-football.local.properties.";
        }
        return "Ajoutez API_FOOTBALL_KEY ou api-football.local.properties.";
    }

    private boolean isWantedMarket(String name) {
        String normalized = normalize(name);
        return normalized != null
                && (normalized.contains("match winner")
                || normalized.contains("winner")
                || normalized.contains("home away")
                || normalized.contains("1x2")
                || normalized.contains("next goal")
                || normalized.contains("over under")
                || normalized.contains("asian handicap")
                || normalized.contains("handicap")
                || normalized.contains("double chance")
                || normalized.contains("both teams"));
    }

    private int marketPriority(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return 99;
        }
        if (normalized.contains("match winner") || normalized.contains("1x2") || normalized.equals("winner")) {
            return 0;
        }
        if (normalized.contains("next goal")) {
            return 1;
        }
        if (normalized.contains("over under")) {
            return 2;
        }
        if (normalized.contains("asian handicap") || normalized.contains("handicap")) {
            return 3;
        }
        if (normalized.contains("double chance")) {
            return 4;
        }
        if (normalized.contains("both teams")) {
            return 5;
        }
        return 50;
    }

    private String normalizeMarketName(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return "Market";
        }
        if (normalized.contains("match winner") || normalized.equals("winner") || normalized.contains("1x2")) {
            return "Match Winner";
        }
        if (normalized.contains("next goal")) {
            return "Next Goal";
        }
        if (normalized.contains("over under")) {
            return "Over/Under";
        }
        if (normalized.contains("asian handicap")) {
            return "Asian Handicap";
        }
        if (normalized.contains("handicap")) {
            return "Handicap";
        }
        if (normalized.contains("double chance")) {
            return "Double Chance";
        }
        if (normalized.contains("both teams")) {
            return "Both Teams To Score";
        }
        return name;
    }

    private String formatSelectionLabel(String rawLabel, String homeName, String awayName) {
        String normalized = normalize(rawLabel);
        if ("home".equals(normalized)) {
            return "1";
        }
        if ("draw".equals(normalized)) {
            return "X";
        }
        if ("away".equals(normalized)) {
            return "2";
        }
        if (normalized != null && normalized.equals(normalize(homeName))) {
            return "1";
        }
        if (normalized != null && normalized.equals(normalize(awayName))) {
            return "2";
        }
        return rawLabel;
    }

    private String resolveTrend(String odd, String bookmaker, String rawLabel, boolean live, boolean suspended) {
        if (suspended) {
            return "locked";
        }
        Double price = parseDouble(odd);
        if (price == null) {
            return live ? "stable" : "neutral";
        }
        if (price >= 3.0) {
            return "up";
        }
        if (price <= 1.7) {
            return "down";
        }
        int hash = Math.abs(Objects.hash(bookmaker, rawLabel, odd));
        return hash % 3 == 0 ? "up" : (hash % 3 == 1 ? "down" : "stable");
    }

    private boolean isLive(Matchs match) {
        String normalized = normalize(match == null ? null : match.getStatut());
        return normalized != null
                && (normalized.contains("direct")
                || normalized.contains("live")
                || normalized.contains("cours")
                || normalized.contains("half")
                || normalized.contains("mi temps")
                || normalized.contains("mi-temps")
                || normalized.contains("1h")
                || normalized.contains("2h")
                || normalized.contains("prolong"));
    }

    private boolean isFinished(Matchs match) {
        String normalized = normalize(match == null ? null : match.getStatut());
        return normalized != null
                && (normalized.contains("fini")
                || normalized.contains("term")
                || normalized.contains("finished")
                || normalized.contains("full time"));
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null || !node.has(field)) {
            return null;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String emptyToString(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(',', '.').trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ');
        normalized = normalized.replaceAll("[^a-z0-9]+", " ").trim();
        return normalized.isEmpty() ? null : normalized.replaceAll("\\s+", " ");
    }

    private class MarketBuilder {
        private final String name;
        private final Map<String, ApiFootballOddsSnapshot.BookmakerRow> rows = new LinkedHashMap<>();

        private MarketBuilder(String name) {
            this.name = normalizeMarketName(name);
        }

        private void addRow(String bookmaker, List<ApiFootballOddsSnapshot.Selection> selections) {
            if (bookmaker == null || selections == null || selections.isEmpty() || rows.containsKey(bookmaker)) {
                return;
            }
            rows.put(bookmaker, new ApiFootballOddsSnapshot.BookmakerRow(bookmaker, selections));
        }

        private ApiFootballOddsSnapshot.Market build() {
            List<ApiFootballOddsSnapshot.BookmakerRow> orderedRows = rows.values().stream()
                    .sorted(Comparator
                            .comparingInt((ApiFootballOddsSnapshot.BookmakerRow row) -> bookmakerPriority(row.bookmaker()))
                            .thenComparing(ApiFootballOddsSnapshot.BookmakerRow::bookmaker, String.CASE_INSENSITIVE_ORDER))
                    .limit(MAX_BOOKMAKER_ROWS)
                    .toList();
            return new ApiFootballOddsSnapshot.Market(name, marketDescription(name), orderedRows);
        }

        private int bookmakerPriority(String bookmaker) {
            for (int index = 0; index < BOOKMAKER_PRIORITY.size(); index++) {
                if (BOOKMAKER_PRIORITY.get(index).equalsIgnoreCase(bookmaker)) {
                    return index;
                }
            }
            return BOOKMAKER_PRIORITY.size() + 1;
        }

        private String marketDescription(String marketName) {
            String normalized = normalize(marketName);
            if (normalized == null) {
                return "Marche API-Football";
            }
            if (normalized.contains("match winner")) {
                return "1X2";
            }
            if (normalized.contains("next goal")) {
                return "Prochain but";
            }
            if (normalized.contains("over under")) {
                return "Total buts";
            }
            if (normalized.contains("handicap")) {
                return "Handicap";
            }
            return "Marche API";
        }
    }
}
