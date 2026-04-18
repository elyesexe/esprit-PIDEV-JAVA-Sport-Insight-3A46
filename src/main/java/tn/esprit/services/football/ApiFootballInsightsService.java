package tn.esprit.services.football;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.tools.MyConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApiFootballInsightsService {
    private static final Duration TEAM_LOOKUP_CACHE_TTL = Duration.ofHours(12);
    private static final Duration TOP_SCORERS_CACHE_TTL = Duration.ofHours(6);
    private static final Duration THE_SPORTS_DB_TEAM_CACHE_TTL = Duration.ofHours(12);

    private static final Map<String, CacheEntry<Map<String, Long>>> TEAM_LOOKUP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry<List<ApiFootballScorerEntry>>> COMPETITION_SCORERS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry<List<ApiFootballScorerEntry>>> TEAM_SCORERS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry<List<FootballDataScorerSnapshot>>> FOOTBALL_DATA_SCORERS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry<List<TheSportsDbTeamProfile>>> THE_SPORTS_DB_TEAM_CACHE = new ConcurrentHashMap<>();

    private static final TypeReference<List<ApiFootballStatisticRow>> STATISTICS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<CachedLineups> LINEUPS_TYPE = new TypeReference<>() {
    };

    private final Connection connection;
    private final FootballDataApiClient footballDataApiClient;
    private final ApiFootballClient apiClient;
    private final SofaScoreClient sofaScoreClient;
    private final SportsCafeClient sportsCafeClient;
    private final TheSportsDbClient theSportsDbClient;
    private final ObjectMapper objectMapper;

    public ApiFootballInsightsService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
        FootballDataApiClient footballDataClient;
        try {
            footballDataClient = new FootballDataApiClient();
        } catch (IllegalStateException e) {
            footballDataClient = null;
        }
        ApiFootballClient client;
        try {
            client = new ApiFootballClient();
        } catch (IllegalStateException e) {
            client = null;
        }
        this.footballDataApiClient = footballDataClient;
        this.apiClient = client;
        this.sofaScoreClient = new SofaScoreClient();
        this.sportsCafeClient = new SportsCafeClient();
        this.theSportsDbClient = new TheSportsDbClient();
        this.objectMapper = new ObjectMapper();
    }

    public ApiFootballMatchDetails loadMatchDetails(Matchs match, Equipe homeTeam, Equipe awayTeam)
            throws SQLException, IOException, InterruptedException {
        Objects.requireNonNull(match, "match");

        ApiFootballMatchDetails cached = readMatchDetailsFromCache(match);
        boolean cacheNeedsMoreData = needsSofaScoreEnrichment(cached)
                || needsSportsCafeEnrichment(cached)
                || needsTheSportsDbEnrichment(cached);
        if (isCacheFresh(match, cached) && !cacheNeedsMoreData) {
            return cached;
        }

        IOException apiFootballError = null;
        ApiFootballMatchDetails apiFootballDetails = null;
        if (apiClient != null) {
            try {
                apiFootballDetails = loadMatchDetailsFromApiFootball(match, homeTeam, awayTeam, cached);
            } catch (IOException e) {
                apiFootballError = e;
            }
        }

        IOException sofaScoreError = null;
        ApiFootballMatchDetails sofaScoreDetails = null;
        try {
            ApiFootballMatchDetails baseline = apiFootballDetails != null ? apiFootballDetails : cached;
            if (needsSofaScoreEnrichment(baseline)) {
                sofaScoreDetails = loadMatchDetailsFromSofaScore(match, homeTeam, awayTeam, baseline);
            }
        } catch (IOException e) {
            sofaScoreError = e;
        }

        IOException sportsCafeError = null;
        ApiFootballMatchDetails sportsCafeDetails = null;
        try {
            ApiFootballMatchDetails baseline = sofaScoreDetails != null
                    ? sofaScoreDetails
                    : (apiFootballDetails != null ? apiFootballDetails : cached);
            if (needsSportsCafeEnrichment(baseline)) {
                sportsCafeDetails = loadMatchDetailsFromSportsCafe(match, homeTeam, awayTeam, baseline);
            }
        } catch (IOException e) {
            sportsCafeError = e;
        }

        IOException theSportsDbError = null;
        try {
            ApiFootballMatchDetails baseline = sportsCafeDetails != null
                    ? sportsCafeDetails
                    : (sofaScoreDetails != null
                    ? sofaScoreDetails
                    : (apiFootballDetails != null ? apiFootballDetails : cached));
            if (needsTheSportsDbEnrichment(baseline)) {
                ApiFootballMatchDetails fallback = loadMatchDetailsFromTheSportsDb(match, homeTeam, awayTeam, baseline);
                if (fallback != null && (fallback.hasLineups() || fallback.hasStatistics())) {
                    return fallback;
                }
            }
        } catch (IOException e) {
            theSportsDbError = e;
        }

        if (sofaScoreDetails != null && (sofaScoreDetails.hasLineups() || sofaScoreDetails.hasStatistics())) {
            return sofaScoreDetails;
        }
        if (sportsCafeDetails != null && (sportsCafeDetails.hasLineups() || sportsCafeDetails.hasStatistics())) {
            return sportsCafeDetails;
        }
        if (apiFootballDetails != null && (apiFootballDetails.hasLineups() || apiFootballDetails.hasStatistics())) {
            return apiFootballDetails;
        }

        if (cached != null && (cached.hasLineups() || cached.hasStatistics())) {
            return cached;
        }

        if (apiFootballError != null && sofaScoreError != null && sportsCafeError != null && theSportsDbError != null) {
            IOException combinedError = new IOException(
                    "API-Football, SofaScore, SportsCafe et TheSportsDB n'ont retourne aucune donnee detaillee pour "
                            + buildMatchLabel(match, homeTeam, awayTeam) + "."
            );
            combinedError.addSuppressed(apiFootballError);
            combinedError.addSuppressed(sofaScoreError);
            combinedError.addSuppressed(sportsCafeError);
            combinedError.addSuppressed(theSportsDbError);
            throw combinedError;
        }
        if (sofaScoreError != null) {
            throw sofaScoreError;
        }
        if (sportsCafeError != null) {
            throw sportsCafeError;
        }
        if (apiFootballError != null) {
            throw apiFootballError;
        }
        if (theSportsDbError != null) {
            throw theSportsDbError;
        }
        throw new IOException("Aucune source gratuite n'a retourne de stats detaillees pour ce match.");
    }

    public List<ApiFootballScorerEntry> loadCompetitionTopScorers(String competitionCode) throws IOException, InterruptedException {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        String cacheKey = normalizedCode + ":competition";
        List<ApiFootballScorerEntry> cached = getCached(COMPETITION_SCORERS_CACHE, cacheKey, TOP_SCORERS_CACHE_TTL);
        if (cached != null) {
            return cached;
        }

        List<ApiFootballScorerEntry> scorers = loadCompetitionTopScorersFromFootballData(normalizedCode);
        if (scorers.isEmpty()) {
            scorers = loadCompetitionTopScorersFromApiFootball(normalizedCode);
        }
        COMPETITION_SCORERS_CACHE.put(cacheKey, new CacheEntry<>(scorers, Instant.now()));
        return scorers;
    }

    public List<ApiFootballScorerEntry> loadTeamTopScorers(Equipe equipe) throws SQLException, IOException, InterruptedException {
        if (equipe == null || equipe.getId() == null) {
            return List.of();
        }

        String competitionCode = FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode());
        String cacheKey = (equipe.getExternalApiId() == null ? equipe.getId() : equipe.getExternalApiId()) + ":" + competitionCode;
        List<ApiFootballScorerEntry> cached = getCached(TEAM_SCORERS_CACHE, cacheKey, TOP_SCORERS_CACHE_TTL);
        if (cached != null) {
            return cached;
        }

        List<ApiFootballScorerEntry> ranked = loadTeamTopScorersFromFootballData(equipe, competitionCode);
        if (ranked.isEmpty()) {
            ranked = loadTeamTopScorersFromApiFootball(equipe, competitionCode);
        }
        TEAM_SCORERS_CACHE.put(cacheKey, new CacheEntry<>(ranked, Instant.now()));
        return ranked;
    }

    public ApiFootballMatchDetails readCachedMatchDetails(Matchs match) {
        return readMatchDetailsFromCache(match);
    }

    public String formatStartingLineup(ApiFootballLineupSide lineup) {
        if (lineup == null || lineup.startingPlayers() == null || lineup.startingPlayers().isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (int index = 0; index < lineup.startingPlayers().size(); index++) {
            lines.add((index + 1) + ". " + formatLineupPlayer(lineup.startingPlayers().get(index)));
        }
        return String.join("\n", lines);
    }

    private boolean needsTheSportsDbEnrichment(ApiFootballMatchDetails details) {
        if (details == null) {
            return true;
        }
        return lineupNeedsEnrichment(details.homeLineup())
                || lineupNeedsEnrichment(details.awayLineup())
                || !details.hasStatistics();
    }

    private boolean needsSofaScoreEnrichment(ApiFootballMatchDetails details) {
        if (details == null) {
            return true;
        }
        return lineupNeedsEnrichment(details.homeLineup())
                || lineupNeedsEnrichment(details.awayLineup())
                || !details.hasStatistics();
    }

    private boolean needsSportsCafeEnrichment(ApiFootballMatchDetails details) {
        if (details == null) {
            return true;
        }
        return lineupNeedsEnrichment(details.homeLineup()) || lineupNeedsEnrichment(details.awayLineup());
    }

    private boolean lineupNeedsEnrichment(ApiFootballLineupSide lineup) {
        if (lineup == null || !lineup.hasStartingPlayers()) {
            return true;
        }
        return lineup.startingPlayerCount() < 11;
    }

    private String formatLineupPlayer(ApiFootballLineupPlayer player) {
        if (player == null) {
            return "Joueur";
        }

        StringBuilder builder = new StringBuilder();
        if (player.shirtNumber() != null && !player.shirtNumber().isBlank()) {
            builder.append('#').append(player.shirtNumber().trim()).append(' ');
        }
        builder.append(firstNonBlank(player.playerName(), "Joueur"));
        if (player.position() != null && !player.position().isBlank()) {
            builder.append(" (").append(player.position().trim()).append(')');
        }
        return builder.toString();
    }

    private ApiFootballMatchDetails loadMatchDetailsFromApiFootball(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails cached
    ) throws SQLException, IOException, InterruptedException {
        String competitionCode = FootballDataCompetitions.normalizeCode(match.getCompetitionCode());
        if (!ApiFootballCompetitionMappings.supportsCompetition(competitionCode)) {
            throw new IOException("Competition non supportee par API-Football: " + FootballDataCompetitions.labelOf(competitionCode));
        }

        int seasonYear = ApiFootballCompetitionMappings.resolveSeasonYear(competitionCode, referenceDateOf(match));
        Integer homeApiFootballId = ensureTeamApiFootballId(homeTeam, competitionCode, seasonYear);
        Integer awayApiFootballId = ensureTeamApiFootballId(awayTeam, competitionCode, seasonYear);
        long fixtureId = resolveFixtureId(match, competitionCode, seasonYear, homeTeam, awayTeam, homeApiFootballId, awayApiFootballId);

        JsonNode lineupsPayload = requireApiClient().fetchFixtureLineups(fixtureId);
        JsonNode statisticsPayload = requireApiClient().fetchFixtureStatistics(fixtureId);

        ApiFootballLineupSide homeLineup = parseLineup(lineupsPayload, true, homeTeam);
        ApiFootballLineupSide awayLineup = parseLineup(lineupsPayload, false, awayTeam);
        List<ApiFootballStatisticRow> statistics = parseStatistics(statisticsPayload);

        ApiFootballMatchDetails fresh = new ApiFootballMatchDetails(
                fixtureId,
                Instant.now().toString(),
                mergeLineup(cached == null ? null : cached.homeLineup(), homeLineup),
                mergeLineup(cached == null ? null : cached.awayLineup(), awayLineup),
                statistics.isEmpty() && cached != null && cached.statistics() != null ? cached.statistics() : statistics
        );

        persistMatchDetails(match, fresh);
        return fresh;
    }

    private ApiFootballMatchDetails loadMatchDetailsFromSofaScore(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails cached
    ) throws SQLException, IOException, InterruptedException {
        String competitionCode = FootballDataCompetitions.normalizeCode(match == null ? null : match.getCompetitionCode());
        if (!SofaScoreCompetitionMappings.supportsCompetition(competitionCode)) {
            throw new IOException("Competition non supportee par SofaScore: " + FootballDataCompetitions.labelOf(competitionCode));
        }

        long eventId = resolveSofaScoreEventId(match, homeTeam, awayTeam, competitionCode);
        JsonNode lineupsPayload = sofaScoreClient.fetchEventLineups(eventId);
        JsonNode statisticsPayload = sofaScoreClient.fetchEventStatistics(eventId);

        ApiFootballLineupSide homeLineup = parseSofaScoreLineup(lineupsPayload.path("home"), homeTeam);
        ApiFootballLineupSide awayLineup = parseSofaScoreLineup(lineupsPayload.path("away"), awayTeam);
        List<ApiFootballStatisticRow> statistics = parseSofaScoreStatistics(statisticsPayload);

        if ((homeLineup == null || !homeLineup.hasStartingPlayers())
                && (awayLineup == null || !awayLineup.hasStartingPlayers())
                && statistics.isEmpty()) {
            throw new IOException("SofaScore n'a retourne ni compositions ni statistiques detaillees pour ce match.");
        }

        ApiFootballMatchDetails details = new ApiFootballMatchDetails(
                cached == null ? null : cached.fixtureId(),
                Instant.now().toString(),
                mergeLineup(cached == null ? null : cached.homeLineup(), homeLineup),
                mergeLineup(cached == null ? null : cached.awayLineup(), awayLineup),
                statistics.isEmpty() && cached != null && cached.statistics() != null ? cached.statistics() : statistics
        );

        persistMatchDetails(match, details);
        return details;
    }

    private ApiFootballMatchDetails loadMatchDetailsFromSportsCafe(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails cached
    ) throws SQLException, IOException, InterruptedException {
        String competitionCode = FootballDataCompetitions.normalizeCode(match == null ? null : match.getCompetitionCode());
        SportsCafeCompetitionMappings.CompetitionPath competitionPath = SportsCafeCompetitionMappings.competitionPathOf(competitionCode);
        if (competitionPath == null) {
            throw new IOException("Competition non supportee par SportsCafe: " + FootballDataCompetitions.labelOf(competitionCode));
        }

        String eventLink = resolveSportsCafeEventLink(match, homeTeam, awayTeam, cached, competitionPath);
        JsonNode payload = sportsCafeClient.fetchMatchLineups(eventLink);

        ApiFootballLineupSide homeLineup = parseSportsCafeLineup(payload, "home", homeTeam);
        ApiFootballLineupSide awayLineup = parseSportsCafeLineup(payload, "away", awayTeam);
        if ((homeLineup == null || !homeLineup.hasStartingPlayers())
                && (awayLineup == null || !awayLineup.hasStartingPlayers())) {
            throw new IOException("SportsCafe n'a retourne aucune composition detaillee pour ce match.");
        }

        Long fixtureId = cached == null ? null : cached.fixtureId();
        if (fixtureId == null) {
            fixtureId = parseNullableLong(payload.path("event").path("externalId").asText(null));
        }

        ApiFootballMatchDetails details = new ApiFootballMatchDetails(
                fixtureId,
                Instant.now().toString(),
                mergeLineup(cached == null ? null : cached.homeLineup(), homeLineup),
                mergeLineup(cached == null ? null : cached.awayLineup(), awayLineup),
                cached != null && cached.statistics() != null ? cached.statistics() : List.of()
        );

        persistMatchDetails(match, details);
        return details;
    }

    private ApiFootballMatchDetails loadMatchDetailsFromTheSportsDb(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails cached
    ) throws SQLException, IOException, InterruptedException {
        String fallbackCompetitionCode = match == null ? null : match.getCompetitionCode();
        TheSportsDbTeamProfile homeTeamProfile = resolveTheSportsDbTeamProfile(homeTeam, fallbackCompetitionCode);
        TheSportsDbTeamProfile awayTeamProfile = resolveTheSportsDbTeamProfile(awayTeam, fallbackCompetitionCode);
        String homeTeamName = homeTeamProfile == null ? null : homeTeamProfile.teamName();
        String awayTeamName = awayTeamProfile == null ? null : awayTeamProfile.teamName();
        long eventId = resolveTheSportsDbEventId(match, homeTeam, awayTeam, homeTeamProfile, awayTeamProfile);

        JsonNode lineupsPayload = theSportsDbClient.fetchEventLineup(eventId);
        JsonNode statisticsPayload = theSportsDbClient.fetchEventStats(eventId);

        ApiFootballLineupSide homeLineup = parseTheSportsDbLineup(lineupsPayload, true, homeTeam, homeTeamName);
        ApiFootballLineupSide awayLineup = parseTheSportsDbLineup(lineupsPayload, false, awayTeam, awayTeamName);
        List<ApiFootballStatisticRow> statistics = parseTheSportsDbStatistics(statisticsPayload);

        if ((homeLineup == null || !homeLineup.hasStartingPlayers())
                && (awayLineup == null || !awayLineup.hasStartingPlayers())
                && statistics.isEmpty()) {
            throw new IOException("TheSportsDB n'a retourne ni compositions ni statistiques detaillees pour ce match.");
        }

        ApiFootballMatchDetails details = new ApiFootballMatchDetails(
                null,
                Instant.now().toString(),
                mergeLineup(cached == null ? null : cached.homeLineup(), homeLineup),
                mergeLineup(cached == null ? null : cached.awayLineup(), awayLineup),
                statistics.isEmpty() && cached != null && cached.statistics() != null ? cached.statistics() : statistics
        );

        persistMatchDetails(match, details);
        return details;
    }

    private long resolveSofaScoreEventId(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            String competitionCode
    ) throws IOException, InterruptedException {
        if (match == null || match.getDateMatch() == null) {
            throw new IOException("Impossible de rechercher un evenement SofaScore sans date de match.");
        }

        long bestEventId = 0L;
        double bestScore = 0.0;
        for (int dayOffset : List.of(0, -1, 1)) {
            JsonNode payload = sofaScoreClient.fetchScheduledEvents(match.getDateMatch().plusDays(dayOffset));
            SofaScoreEventMatch candidate = selectSofaScoreEvent(payload.path("events"), match, homeTeam, awayTeam, competitionCode);
            if (candidate != null && candidate.score() > bestScore) {
                bestScore = candidate.score();
                bestEventId = candidate.eventId();
            }
        }

        if (bestEventId > 0L) {
            return bestEventId;
        }

        throw new IOException("Aucun evenement SofaScore correspondant n'a ete trouve pour "
                + buildMatchLabel(match, homeTeam, awayTeam) + ".");
    }

    private SofaScoreEventMatch selectSofaScoreEvent(
            JsonNode eventsNode,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            String competitionCode
    ) {
        if (!eventsNode.isArray()) {
            return null;
        }

        String expectedHome = normalizeTeamName(homeTeam == null ? null : homeTeam.getNom());
        String expectedAway = normalizeTeamName(awayTeam == null ? null : awayTeam.getNom());
        LocalDateTime expectedKickoff = toLocalDateTime(match);

        double bestScore = 0.0;
        long bestEventId = 0L;

        for (JsonNode eventNode : eventsNode) {
            if (!matchesSofaScoreCompetition(eventNode, competitionCode) || !isSofaScoreMensClubFixture(eventNode)) {
                continue;
            }

            JsonNode homeNode = eventNode.path("homeTeam");
            JsonNode awayNode = eventNode.path("awayTeam");

            double directHomeScore = bestSofaScoreTeamSimilarity(expectedHome, homeNode);
            double directAwayScore = bestSofaScoreTeamSimilarity(expectedAway, awayNode);
            double swappedHomeScore = bestSofaScoreTeamSimilarity(expectedHome, awayNode);
            double swappedAwayScore = bestSofaScoreTeamSimilarity(expectedAway, homeNode);

            boolean swapped = swappedHomeScore + swappedAwayScore > directHomeScore + directAwayScore;
            double homeScore = swapped ? swappedHomeScore : directHomeScore;
            double awayScore = swapped ? swappedAwayScore : directAwayScore;
            if (homeScore < 0.72 || awayScore < 0.72) {
                continue;
            }

            double score = homeScore + awayScore + 0.7;
            LocalDateTime eventKickoff = parseSofaScoreKickoff(eventNode.path("startTimestamp"));
            if (expectedKickoff != null && eventKickoff != null) {
                long distanceMinutes = Math.abs(ChronoUnit.MINUTES.between(expectedKickoff, eventKickoff));
                if (distanceMinutes <= 10) {
                    score += 0.5;
                } else if (distanceMinutes <= 120) {
                    score += 0.25;
                }
            }

            if (eventNode.path("hasEventPlayerStatistics").asBoolean(false)) {
                score += 0.1;
            }
            if (eventNode.path("hasXg").asBoolean(false)) {
                score += 0.1;
            }

            long eventId = eventNode.path("id").asLong(0L);
            if (eventId > 0L && score > bestScore) {
                bestScore = score;
                bestEventId = eventId;
            }
        }

        return bestEventId > 0L ? new SofaScoreEventMatch(bestEventId, bestScore) : null;
    }

    private boolean matchesSofaScoreCompetition(JsonNode eventNode, String competitionCode) {
        SofaScoreCompetitionMappings.CompetitionDescriptor descriptor = SofaScoreCompetitionMappings.descriptorOf(competitionCode);
        if (descriptor == null) {
            return false;
        }

        JsonNode tournamentNode = eventNode.path("tournament");
        JsonNode uniqueTournamentNode = tournamentNode.path("uniqueTournament");
        String categorySlug = normalizeLeagueName(firstNonBlank(
                uniqueTournamentNode.path("category").path("slug").asText(null),
                tournamentNode.path("category").path("slug").asText(null)
        ));
        String tournamentSlug = normalizeLeagueName(firstNonBlank(
                uniqueTournamentNode.path("slug").asText(null),
                tournamentNode.path("slug").asText(null)
        ));
        String tournamentName = normalizeLeagueName(firstNonBlank(
                uniqueTournamentNode.path("name").asText(null),
                tournamentNode.path("name").asText(null)
        ));

        return Objects.equals(normalizeLeagueName(descriptor.categorySlug()), categorySlug)
                && (Objects.equals(normalizeLeagueName(descriptor.tournamentSlug()), tournamentSlug)
                || Objects.equals(normalizeLeagueName(descriptor.tournamentName()), tournamentName));
    }

    private boolean isSofaScoreMensClubFixture(JsonNode eventNode) {
        JsonNode homeNode = eventNode.path("homeTeam");
        JsonNode awayNode = eventNode.path("awayTeam");
        String homeGender = normalizeNullable(homeNode.path("gender").asText(null));
        String awayGender = normalizeNullable(awayNode.path("gender").asText(null));
        return (homeGender == null || "M".equalsIgnoreCase(homeGender))
                && (awayGender == null || "M".equalsIgnoreCase(awayGender));
    }

    private double bestSofaScoreTeamSimilarity(String expectedName, JsonNode teamNode) {
        if (expectedName == null || teamNode == null || !teamNode.isObject()) {
            return 0.0;
        }
        return Math.max(
                similarity(expectedName, normalizeTeamName(teamNode.path("name").asText(null))),
                similarity(expectedName, normalizeTeamName(teamNode.path("shortName").asText(null)))
        );
    }

    private String resolveSportsCafeEventLink(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails baseline,
            SportsCafeCompetitionMappings.CompetitionPath competitionPath
    ) throws IOException, InterruptedException {
        Map<String, JsonNode> candidates = new LinkedHashMap<>();
        collectSportsCafeEventCandidates(
                sportsCafeClient.fetchCompetitionResults(competitionPath.leagueSlug(), competitionPath.competitionSlug()),
                candidates
        );
        collectSportsCafeEventCandidates(
                sportsCafeClient.fetchLeagueResults(competitionPath.leagueSlug()),
                candidates
        );

        String bestMatch = selectSportsCafeEventLink(candidates.values(), match, homeTeam, awayTeam, baseline);
        if (bestMatch != null) {
            return bestMatch;
        }

        throw new IOException("Aucun evenement SportsCafe correspondant n'a ete trouve pour "
                + buildMatchLabel(match, homeTeam, awayTeam) + ".");
    }

    private void collectSportsCafeEventCandidates(JsonNode node, Map<String, JsonNode> candidates) {
        if (node == null || candidates == null) {
            return;
        }

        if (node.isObject()) {
            String eventLink = buildSportsCafeEventLink(node);
            if (eventLink != null && node.path("competitors").isArray()) {
                String key = firstNonBlank(eventLink, "event:" + node.path("id").asText(""));
                candidates.putIfAbsent(key, node);
            }
            node.fields().forEachRemaining(entry -> collectSportsCafeEventCandidates(entry.getValue(), candidates));
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectSportsCafeEventCandidates(child, candidates);
            }
        }
    }

    private String selectSportsCafeEventLink(
            Iterable<JsonNode> candidates,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballMatchDetails baseline
    ) {
        String expectedHome = normalizeTeamName(homeTeam == null ? null : homeTeam.getNom());
        String expectedAway = normalizeTeamName(awayTeam == null ? null : awayTeam.getNom());
        String expectedCompetition = normalizeLeagueName(FootballDataCompetitions.labelOf(match == null ? null : match.getCompetitionCode()));
        LocalDateTime expectedKickoff = toLocalDateTime(match);
        Long expectedFixtureId = baseline == null ? null : baseline.fixtureId();

        double bestScore = 0.0;
        String bestEventLink = null;

        for (JsonNode candidate : candidates) {
            if (candidate == null || !candidate.isObject()) {
                continue;
            }

            String eventLink = buildSportsCafeEventLink(candidate);
            if (eventLink == null) {
                continue;
            }

            JsonNode homeCandidate = findSportsCafeCompetitor(candidate, "home");
            JsonNode awayCandidate = findSportsCafeCompetitor(candidate, "away");
            if (homeCandidate == null || awayCandidate == null) {
                homeCandidate = candidate.path("competitors").size() > 0 ? candidate.path("competitors").get(0) : null;
                awayCandidate = candidate.path("competitors").size() > 1 ? candidate.path("competitors").get(1) : null;
            }
            if (homeCandidate == null || awayCandidate == null) {
                continue;
            }

            String homeName = normalizeTeamName(homeCandidate.path("name").asText(null));
            String awayName = normalizeTeamName(awayCandidate.path("name").asText(null));
            double directHomeScore = similarity(expectedHome, homeName);
            double directAwayScore = similarity(expectedAway, awayName);
            double swappedHomeScore = similarity(expectedHome, awayName);
            double swappedAwayScore = similarity(expectedAway, homeName);

            boolean swapped = swappedHomeScore + swappedAwayScore > directHomeScore + directAwayScore;
            double homeScore = swapped ? swappedHomeScore : directHomeScore;
            double awayScore = swapped ? swappedAwayScore : directAwayScore;
            if (homeScore < 0.68 || awayScore < 0.68) {
                continue;
            }

            LocalDate eventDate = parseDateOnly(candidate.path("date").asText(null));
            if (match != null && match.getDateMatch() != null && eventDate != null) {
                long daysDifference = Math.abs(ChronoUnit.DAYS.between(match.getDateMatch(), eventDate));
                if (daysDifference > 2) {
                    continue;
                }
            }

            double score = homeScore + awayScore;
            if (match != null && match.getDateMatch() != null && eventDate != null) {
                long daysDifference = Math.abs(ChronoUnit.DAYS.between(match.getDateMatch(), eventDate));
                if (daysDifference == 0) {
                    score += 0.6;
                } else if (daysDifference == 1) {
                    score += 0.25;
                }
            }

            String competitionName = normalizeLeagueName(candidate.path("competition").asText(null));
            if (expectedCompetition != null && competitionName != null) {
                score += similarity(expectedCompetition, competitionName);
            }

            LocalDateTime eventKickoff = parseFixtureDateTime(candidate.path("date").asText(null));
            if (expectedKickoff != null && eventKickoff != null) {
                long distanceMinutes = Math.abs(ChronoUnit.MINUTES.between(expectedKickoff, eventKickoff));
                if (distanceMinutes <= 15) {
                    score += 0.35;
                } else if (distanceMinutes <= 180) {
                    score += 0.15;
                }
            }

            String externalId = normalizeNullable(candidate.path("externalId").asText(null));
            if (expectedFixtureId != null && externalId != null && externalId.equals(String.valueOf(expectedFixtureId))) {
                score += 3.0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestEventLink = eventLink;
            }
        }

        return bestScore >= 1.7 ? bestEventLink : null;
    }

    private String buildSportsCafeEventLink(JsonNode candidate) {
        if (candidate == null || !candidate.isObject()) {
            return null;
        }

        String eventLink = normalizeNullable(candidate.path("eventLink").asText(null));
        if (eventLink != null) {
            return eventLink;
        }

        String leagueUrl = normalizeNullable(candidate.path("leagueUrl").asText(null));
        String competitionUrl = normalizeNullable(candidate.path("competitionUrl").asText(null));
        String urlSegment = normalizeNullable(candidate.path("urlSegment").asText(null));
        if (leagueUrl == null || urlSegment == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder("/football/live/").append(leagueUrl);
        if (competitionUrl != null) {
            builder.append('/').append(competitionUrl);
        }
        builder.append('/').append(urlSegment);
        return builder.toString();
    }

    private JsonNode findSportsCafeCompetitor(JsonNode eventNode, String qualifier) {
        JsonNode competitors = eventNode == null ? null : eventNode.path("competitors");
        if (!competitors.isArray()) {
            return null;
        }

        for (JsonNode competitor : competitors) {
            if (qualifier.equalsIgnoreCase(competitor.path("qualifier").asText(""))) {
                return competitor;
            }
        }
        return null;
    }

    private ApiFootballLineupSide parseSportsCafeLineup(JsonNode payload, String sideKey, Equipe preferredTeam) {
        JsonNode detailsNode = payload.path("event").path("details");
        JsonNode playersNode = detailsNode.path("players").path(sideKey);
        if (!playersNode.isArray()) {
            return null;
        }

        List<ApiFootballLineupPlayer> starters = new ArrayList<>();
        List<ApiFootballLineupPlayer> substitutes = new ArrayList<>();
        for (JsonNode playerNode : playersNode) {
            ApiFootballLineupPlayer player = mapSportsCafeLineupPlayer(playerNode);
            if (player == null) {
                continue;
            }
            if (playerNode.path("isMainCast").asBoolean(false)) {
                starters.add(player);
            } else {
                substitutes.add(player);
            }
        }

        starters = dedupeSportsCafePlayers(starters);
        substitutes = dedupeSportsCafePlayers(substitutes);
        sortSportsCafePlayers(starters);
        sortSportsCafePlayers(substitutes);

        String teamName = resolveSportsCafeTeamName(payload.path("event"), sideKey, preferredTeam);
        String formation = normalizeNullable(detailsNode.path("lineups").path(sideKey).path("formation").asText(null));
        String coachName = normalizeNullable(detailsNode.path("lineups").path(sideKey).path("coach").path("name").asText(null));

        return new ApiFootballLineupSide(teamName, formation, coachName, starters, substitutes);
    }

    private List<ApiFootballLineupPlayer> dedupeSportsCafePlayers(List<ApiFootballLineupPlayer> players) {
        return new ArrayList<>(indexPlayers(players).values());
    }

    private void sortSportsCafePlayers(List<ApiFootballLineupPlayer> players) {
        if (players == null) {
            return;
        }
        players.sort(Comparator
                .comparingInt(this::sportsCafeGridRow)
                .thenComparingInt(this::sportsCafeGridColumn)
                .thenComparingInt(this::sportsCafePositionRank)
                .thenComparingInt(player -> parseNullableInteger(player == null ? null : player.shirtNumber()) == null
                        ? Integer.MAX_VALUE
                        : parseNullableInteger(player.shirtNumber()))
                .thenComparing(player -> firstNonBlank(player == null ? null : player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));
    }

    private int sportsCafeGridRow(ApiFootballLineupPlayer player) {
        return parseSportsCafeGridComponent(player == null ? null : player.grid(), 0, Integer.MAX_VALUE);
    }

    private int sportsCafeGridColumn(ApiFootballLineupPlayer player) {
        return parseSportsCafeGridComponent(player == null ? null : player.grid(), 1, Integer.MAX_VALUE);
    }

    private int parseSportsCafeGridComponent(String grid, int componentIndex, int fallback) {
        String normalizedGrid = normalizeNullable(grid);
        if (normalizedGrid == null) {
            return fallback;
        }

        String[] parts = normalizedGrid.replace('-', ':').split(":");
        if (parts.length < 2 || componentIndex >= parts.length) {
            return fallback;
        }

        try {
            return Integer.parseInt(parts[componentIndex].trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int sportsCafePositionRank(ApiFootballLineupPlayer player) {
        String position = normalizeNullable(player == null ? null : player.position());
        if (position == null) {
            return 99;
        }
        String normalized = position.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("G")) {
            return 0;
        }
        if (normalized.startsWith("D")) {
            return 1;
        }
        if (normalized.startsWith("M")) {
            return 2;
        }
        if (normalized.startsWith("F") || normalized.startsWith("A")) {
            return 3;
        }
        return 4;
    }

    private ApiFootballLineupPlayer mapSportsCafeLineupPlayer(JsonNode playerNode) {
        String playerName = normalizeNullable(playerNode.path("name").asText(null));
        if (playerName == null) {
            return null;
        }

        String shirtNumber = normalizeNullable(playerNode.path("number").asText(null));
        String position = normalizeNullable(playerNode.path("position").asText(null));
        String grid = normalizeNullable(playerNode.path("grid").asText(null));
        if (grid != null) {
            grid = grid.replace('-', ':');
        }
        String photoUrl = normalizeNullable(playerNode.path("photo").asText(null));

        return new ApiFootballLineupPlayer(playerName, shirtNumber, position, grid, photoUrl);
    }

    private String resolveSportsCafeTeamName(JsonNode eventNode, String qualifier, Equipe preferredTeam) {
        JsonNode competitor = findSportsCafeCompetitor(eventNode, qualifier);
        return firstNonBlank(
                normalizeNullable(competitor == null ? null : competitor.path("name").asText(null)),
                preferredTeam == null ? null : preferredTeam.getNom()
        );
    }

    private ApiFootballLineupSide parseSofaScoreLineup(JsonNode sideNode, Equipe preferredTeam) {
        if (sideNode == null || !sideNode.isObject()) {
            return null;
        }

        String teamName = preferredTeam == null ? null : preferredTeam.getNom();
        String formation = normalizeNullable(sideNode.path("formation").asText(null));
        List<Integer> formationRows = parseFormationRows(formation);
        List<ApiFootballLineupPlayer> starters = new ArrayList<>();
        List<ApiFootballLineupPlayer> substitutes = new ArrayList<>();

        int startingIndex = 0;
        JsonNode playersNode = sideNode.path("players");
        if (playersNode.isArray()) {
            for (JsonNode playerNode : playersNode) {
                boolean substitute = playerNode.path("substitute").asBoolean(false);
                ApiFootballLineupPlayer player = mapSofaScoreLineupPlayer(playerNode, substitute ? -1 : startingIndex, formationRows);
                if (player == null) {
                    continue;
                }
                if (substitute) {
                    substitutes.add(player);
                } else {
                    starters.add(player);
                    startingIndex++;
                }
            }
        }

        substitutes.sort(Comparator
                .comparingInt((ApiFootballLineupPlayer player) -> parseNullableInteger(player == null ? null : player.shirtNumber()) == null
                        ? Integer.MAX_VALUE
                        : parseNullableInteger(player.shirtNumber()))
                .thenComparing((ApiFootballLineupPlayer player) -> firstNonBlank(player == null ? null : player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));

        return new ApiFootballLineupSide(teamName, formation, null, starters, substitutes);
    }

    private ApiFootballLineupPlayer mapSofaScoreLineupPlayer(JsonNode playerNode, int startingIndex, List<Integer> formationRows) {
        if (playerNode == null || !playerNode.isObject()) {
            return null;
        }

        JsonNode playerInfoNode = playerNode.path("player");
        String playerName = normalizeNullable(firstNonBlank(
                playerInfoNode.path("name").asText(null),
                playerInfoNode.path("shortName").asText(null)
        ));
        if (playerName == null) {
            return null;
        }

        String shirtNumber = normalizeNullable(firstNonBlank(
                playerNode.path("jerseyNumber").asText(null),
                playerNode.path("shirtNumber").asText(null)
        ));
        String position = normalizeNullable(firstNonBlank(
                playerNode.path("position").asText(null),
                playerInfoNode.path("position").asText(null)
        ));
        String grid = startingIndex >= 0 ? buildSofaScoreGrid(startingIndex, formationRows) : null;
        Long playerId = nullableLong(playerInfoNode.path("id"));
        String photoUrl = playerId == null ? null : SofaScoreClient.PLAYER_IMAGE_BASE_URL + playerId + "/image";

        return new ApiFootballLineupPlayer(playerName, shirtNumber, position, grid, photoUrl);
    }

    private List<Integer> parseFormationRows(String formation) {
        String normalizedFormation = normalizeNullable(formation);
        if (normalizedFormation == null) {
            return List.of();
        }

        List<Integer> rows = new ArrayList<>();
        for (String token : normalizedFormation.replace(':', '-').split("-")) {
            try {
                rows.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }
        return rows;
    }

    private String buildSofaScoreGrid(int startingIndex, List<Integer> formationRows) {
        if (startingIndex == 0) {
            return "1:1";
        }
        if (formationRows == null || formationRows.isEmpty()) {
            return null;
        }

        int outfieldIndex = startingIndex - 1;
        int row = 2;
        for (Integer playersInRow : formationRows) {
            int safeCount = playersInRow == null ? 0 : Math.max(playersInRow, 0);
            if (outfieldIndex < safeCount) {
                return row + ":" + (outfieldIndex + 1);
            }
            outfieldIndex -= safeCount;
            row++;
        }
        return null;
    }

    private List<ApiFootballStatisticRow> parseSofaScoreStatistics(JsonNode payload) {
        JsonNode statisticsArray = payload.path("statistics");
        if (!statisticsArray.isArray() || statisticsArray.isEmpty()) {
            return List.of();
        }

        JsonNode selectedPeriod = null;
        for (JsonNode periodNode : statisticsArray) {
            if ("ALL".equalsIgnoreCase(periodNode.path("period").asText(""))) {
                selectedPeriod = periodNode;
                break;
            }
        }
        if (selectedPeriod == null) {
            selectedPeriod = statisticsArray.get(0);
        }

        Map<String, ApiFootballStatisticRow> rowsByLabel = new LinkedHashMap<>();
        for (JsonNode groupNode : selectedPeriod.path("groups")) {
            JsonNode statisticsItems = groupNode.path("statisticsItems");
            if (!statisticsItems.isArray()) {
                continue;
            }
            for (JsonNode itemNode : statisticsItems) {
                String label = normalizeNullable(itemNode.path("name").asText(null));
                String homeValue = normalizeNullable(itemNode.path("home").asText(null));
                String awayValue = normalizeNullable(itemNode.path("away").asText(null));
                if (label == null || (homeValue == null && awayValue == null)) {
                    continue;
                }
                rowsByLabel.putIfAbsent(label, new ApiFootballStatisticRow(
                        label,
                        defaultStatValue(homeValue),
                        defaultStatValue(awayValue)
                ));
            }
        }

        return new ArrayList<>(rowsByLabel.values());
    }

    private List<ApiFootballScorerEntry> loadCompetitionTopScorersFromFootballData(String competitionCode)
            throws IOException, InterruptedException {
        List<FootballDataScorerSnapshot> snapshots = loadFootballDataScorerSnapshots(competitionCode, 12);
        if (snapshots.isEmpty()) {
            return List.of();
        }

        List<ApiFootballScorerEntry> scorers = new ArrayList<>();
        for (FootballDataScorerSnapshot snapshot : snapshots) {
            scorers.add(new ApiFootballScorerEntry(
                    0,
                    snapshot.playerName(),
                    snapshot.teamName(),
                    snapshot.goals(),
                    snapshot.assists(),
                    snapshot.appearances(),
                    null
            ));
        }
        return rankEntries(limit(scorers, 8));
    }

    private List<ApiFootballScorerEntry> loadCompetitionTopScorersFromApiFootball(String competitionCode)
            throws IOException, InterruptedException {
        if (!ApiFootballCompetitionMappings.supportsCompetition(competitionCode) || apiClient == null) {
            return List.of();
        }

        int seasonYear = ApiFootballCompetitionMappings.resolveSeasonYear(competitionCode, LocalDate.now());
        JsonNode payload = requireApiClient().fetchTopScorers(ApiFootballCompetitionMappings.leagueIdOf(competitionCode), seasonYear);
        return parseScorers(payload.path("response"));
    }

    private List<ApiFootballScorerEntry> loadTeamTopScorersFromFootballData(Equipe equipe, String competitionCode)
            throws IOException, InterruptedException {
        List<FootballDataScorerSnapshot> snapshots = loadFootballDataScorerSnapshots(competitionCode, 50);
        if (snapshots.isEmpty()) {
            return List.of();
        }

        List<ApiFootballScorerEntry> scorers = new ArrayList<>();
        for (FootballDataScorerSnapshot snapshot : snapshots) {
            if (!matchesFootballDataTeam(equipe, snapshot)) {
                continue;
            }
            scorers.add(new ApiFootballScorerEntry(
                    0,
                    snapshot.playerName(),
                    snapshot.teamName(),
                    snapshot.goals(),
                    snapshot.assists(),
                    snapshot.appearances(),
                    null
            ));
        }
        return rankEntries(limit(scorers, 8));
    }

    private List<ApiFootballScorerEntry> loadTeamTopScorersFromApiFootball(Equipe equipe, String competitionCode)
            throws SQLException, IOException, InterruptedException {
        if (!ApiFootballCompetitionMappings.supportsCompetition(competitionCode) || apiClient == null) {
            return List.of();
        }

        int seasonYear = ApiFootballCompetitionMappings.resolveSeasonYear(competitionCode, LocalDate.now());
        Integer teamApiFootballId = ensureTeamApiFootballId(equipe, competitionCode, seasonYear);
        if (teamApiFootballId == null) {
            return List.of();
        }

        int leagueId = ApiFootballCompetitionMappings.leagueIdOf(competitionCode);
        List<ApiFootballScorerEntry> scorers = new ArrayList<>();
        int page = 1;

        while (true) {
            JsonNode payload = requireApiClient().fetchTeamPlayers(teamApiFootballId, leagueId, seasonYear, page);
            JsonNode responseNode = payload.path("response");
            if (!responseNode.isArray() || responseNode.isEmpty()) {
                break;
            }

            for (JsonNode playerNode : responseNode) {
                ApiFootballScorerEntry scorer = parseTeamScorer(playerNode, leagueId, teamApiFootballId);
                if (scorer != null) {
                    scorers.add(scorer);
                }
            }

            int currentPage = payload.path("paging").path("current").asInt(page);
            int totalPages = payload.path("paging").path("total").asInt(currentPage);
            if (currentPage >= totalPages) {
                break;
            }
            page++;
        }

        scorers.sort(Comparator
                .comparing(ApiFootballScorerEntry::goals, Comparator.nullsLast(Integer::compareTo)).reversed()
                .thenComparing(ApiFootballScorerEntry::assists, Comparator.nullsLast(Integer::compareTo)).reversed()
                .thenComparing(ApiFootballScorerEntry::appearances, Comparator.nullsLast(Integer::compareTo)).reversed()
                .thenComparing(ApiFootballScorerEntry::minutes, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ApiFootballScorerEntry::playerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return rankEntries(limit(scorers, 8));
    }

    private List<FootballDataScorerSnapshot> loadFootballDataScorerSnapshots(String competitionCode, int limit)
            throws IOException, InterruptedException {
        if (footballDataApiClient == null || competitionCode == null || competitionCode.isBlank()) {
            return List.of();
        }

        String cacheKey = competitionCode + ":" + limit;
        List<FootballDataScorerSnapshot> cached = getCached(FOOTBALL_DATA_SCORERS_CACHE, cacheKey, TOP_SCORERS_CACHE_TTL);
        if (cached != null) {
            return cached;
        }

        JsonNode payload = requireFootballDataApiClient().fetchCompetitionScorers(competitionCode, limit);
        List<FootballDataScorerSnapshot> snapshots = parseFootballDataScorers(payload.path("scorers"));
        FOOTBALL_DATA_SCORERS_CACHE.put(cacheKey, new CacheEntry<>(snapshots, Instant.now()));
        return snapshots;
    }

    private List<FootballDataScorerSnapshot> parseFootballDataScorers(JsonNode scorersNode) {
        if (!scorersNode.isArray()) {
            return List.of();
        }

        List<FootballDataScorerSnapshot> snapshots = new ArrayList<>();
        for (JsonNode scorerNode : scorersNode) {
            JsonNode playerNode = scorerNode.path("player");
            JsonNode teamNode = scorerNode.path("team");
            snapshots.add(new FootballDataScorerSnapshot(
                    teamNode.path("id").isNumber() ? teamNode.path("id").asLong() : null,
                    normalizeNullable(teamNode.path("name").asText(null)),
                    normalizeNullable(playerNode.path("name").asText(null)),
                    nullableInt(scorerNode.path("goals")),
                    nullableInt(scorerNode.path("assists")),
                    nullableInt(scorerNode.path("playedMatches"))
            ));
        }
        return snapshots;
    }

    private boolean matchesFootballDataTeam(Equipe equipe, FootballDataScorerSnapshot snapshot) {
        if (equipe == null || snapshot == null) {
            return false;
        }
        if (equipe.getExternalApiId() != null && snapshot.teamId() != null) {
            return Objects.equals(equipe.getExternalApiId(), snapshot.teamId());
        }
        return similarity(normalizeTeamName(equipe.getNom()), normalizeTeamName(snapshot.teamName())) >= 0.72;
    }

    private String resolveTheSportsDbTeamName(Equipe team) throws IOException, InterruptedException {
        TheSportsDbTeamProfile profile = resolveTheSportsDbTeamProfile(team, team == null ? null : team.getCompetitionCode());
        return profile == null ? null : profile.teamName();
    }

    private TheSportsDbTeamProfile resolveTheSportsDbTeamProfile(Equipe team, String fallbackCompetitionCode)
            throws IOException, InterruptedException {
        if (team == null || team.getNom() == null) {
            return null;
        }

        List<TheSportsDbTeamProfile> teams = new ArrayList<>();
        String competitionCode = firstNonBlank(team.getCompetitionCode(), fallbackCompetitionCode);
        String leagueQuery = TheSportsDbCompetitionMappings.leagueQueryOf(competitionCode);
        if (leagueQuery != null) {
            mergeTheSportsDbProfiles(teams, loadTheSportsDbLeagueTeams(leagueQuery));
        }

        for (String query : buildTheSportsDbTeamQueries(team.getNom())) {
            mergeTheSportsDbProfiles(teams, loadTheSportsDbSearchTeams(query));
        }

        if (teams.isEmpty()) {
            return null;
        }

        String normalizedLocalName = normalizeTeamName(team.getNom());
        double bestScore = 0.0;
        TheSportsDbTeamProfile bestMatch = null;

        for (TheSportsDbTeamProfile profile : teams) {
            double score = bestTeamProfileScore(profile, normalizedLocalName);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = profile;
            }
        }

        return bestScore >= 0.68 ? bestMatch : null;
    }

    private List<TheSportsDbTeamProfile> loadTheSportsDbLeagueTeams(String leagueQuery) throws IOException, InterruptedException {
        String cacheKey = "league:" + leagueQuery;
        List<TheSportsDbTeamProfile> cached = getCached(THE_SPORTS_DB_TEAM_CACHE, cacheKey, THE_SPORTS_DB_TEAM_CACHE_TTL);
        if (cached != null) {
            return cached;
        }

        JsonNode payload = theSportsDbClient.fetchLeagueTeams(leagueQuery);
        List<TheSportsDbTeamProfile> profiles = mapTheSportsDbTeamProfiles(payload.path("teams"));
        THE_SPORTS_DB_TEAM_CACHE.put(cacheKey, new CacheEntry<>(profiles, Instant.now()));
        return profiles;
    }

    private List<TheSportsDbTeamProfile> loadTheSportsDbSearchTeams(String teamQuery) throws IOException, InterruptedException {
        String normalizedQuery = normalizeNullable(teamQuery);
        if (normalizedQuery == null) {
            return List.of();
        }

        String cacheKey = "search:" + normalizedQuery.toLowerCase(Locale.ROOT);
        List<TheSportsDbTeamProfile> cached = getCached(THE_SPORTS_DB_TEAM_CACHE, cacheKey, THE_SPORTS_DB_TEAM_CACHE_TTL);
        if (cached != null) {
            return cached;
        }

        JsonNode payload = theSportsDbClient.searchTeams(normalizedQuery);
        List<TheSportsDbTeamProfile> profiles = mapTheSportsDbTeamProfiles(payload.path("teams"));
        THE_SPORTS_DB_TEAM_CACHE.put(cacheKey, new CacheEntry<>(profiles, Instant.now()));
        return profiles;
    }

    private List<String> buildTheSportsDbTeamQueries(String teamName) {
        List<String> queries = new ArrayList<>();
        String raw = normalizeNullable(teamName);
        if (raw != null) {
            queries.add(raw);
        }

        String simplified = simplifyTeamName(teamName);
        if (simplified != null && !queries.contains(simplified)) {
            queries.add(simplified);
        }

        String normalized = normalizeTeamName(teamName);
        if (normalized != null) {
            String readable = normalized.replace(' ', '_');
            if (!queries.contains(readable)) {
                queries.add(readable);
            }
        }

        return queries;
    }

    private void mergeTheSportsDbProfiles(List<TheSportsDbTeamProfile> target, List<TheSportsDbTeamProfile> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (TheSportsDbTeamProfile profile : source) {
            if (profile == null) {
                continue;
            }
            boolean exists = false;
            for (TheSportsDbTeamProfile existing : target) {
                if (sameTheSportsDbProfile(existing, profile)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                target.add(profile);
            }
        }
    }

    private boolean sameTheSportsDbProfile(TheSportsDbTeamProfile left, TheSportsDbTeamProfile right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.teamId() != null && right.teamId() != null) {
            return Objects.equals(left.teamId(), right.teamId());
        }
        return Objects.equals(normalizeTeamName(left.teamName()), normalizeTeamName(right.teamName()));
    }

    private List<TheSportsDbTeamProfile> mapTheSportsDbTeamProfiles(JsonNode teamsNode) {
        if (!teamsNode.isArray()) {
            return List.of();
        }

        List<TheSportsDbTeamProfile> profiles = new ArrayList<>();
        for (JsonNode teamNode : teamsNode) {
            Long teamId = nullableLong(teamNode.path("idTeam"));
            String teamName = normalizeNullable(teamNode.path("strTeam").asText(null));
            if (teamName == null) {
                continue;
            }

            List<String> aliases = new ArrayList<>();
            aliases.add(teamName);
            String alternateNames = normalizeNullable(teamNode.path("strTeamAlternate").asText(null));
            if (alternateNames != null) {
                for (String part : alternateNames.split(",")) {
                    String alias = normalizeNullable(part);
                    if (alias != null) {
                        aliases.add(alias);
                    }
                }
            }
            String keywords = normalizeNullable(teamNode.path("strKeywords").asText(null));
            if (keywords != null) {
                for (String part : keywords.split(",")) {
                    String alias = normalizeNullable(part);
                    if (alias != null) {
                        aliases.add(alias);
                    }
                }
            }
            profiles.add(new TheSportsDbTeamProfile(teamId, teamName, aliases));
        }
        return profiles;
    }

    private long resolveTheSportsDbEventId(
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            TheSportsDbTeamProfile homeTeamProfile,
            TheSportsDbTeamProfile awayTeamProfile
    ) throws IOException, InterruptedException {
        String resolvedHomeTeamName = homeTeamProfile == null ? null : homeTeamProfile.teamName();
        String resolvedAwayTeamName = awayTeamProfile == null ? null : awayTeamProfile.teamName();
        List<String> queries = new ArrayList<>();
        addEventQuery(queries, resolvedHomeTeamName, resolvedAwayTeamName);
        addEventQuery(queries, simplifyTeamName(homeTeam == null ? null : homeTeam.getNom()), simplifyTeamName(awayTeam == null ? null : awayTeam.getNom()));
        addEventQuery(queries, homeTeam == null ? null : homeTeam.getNom(), awayTeam == null ? null : awayTeam.getNom());

        for (String query : queries) {
            JsonNode payload = theSportsDbClient.searchEvents(query, match == null ? null : match.getDateMatch());
            Optional<Long> eventId = selectTheSportsDbEvent(
                    payload.path("event"),
                    match,
                    homeTeam,
                    awayTeam,
                    resolvedHomeTeamName,
                    resolvedAwayTeamName,
                    homeTeamProfile == null ? null : homeTeamProfile.teamId(),
                    awayTeamProfile == null ? null : awayTeamProfile.teamId()
            );
            if (eventId.isPresent()) {
                return eventId.get();
            }
        }

        if (match.getDateMatch() != null) {
            String leagueFilter = FootballDataCompetitions.labelOf(match.getCompetitionCode());
            List<LocalDate> datesToTry = List.of(
                    match.getDateMatch(),
                    match.getDateMatch().minusDays(1),
                    match.getDateMatch().plusDays(1)
            );

            for (LocalDate date : datesToTry) {
                JsonNode payload = theSportsDbClient.fetchEventsDay(date, leagueFilter);
                Optional<Long> eventId = selectTheSportsDbEvent(
                        payload.path("events"),
                        match,
                        homeTeam,
                        awayTeam,
                        resolvedHomeTeamName,
                        resolvedAwayTeamName,
                        homeTeamProfile == null ? null : homeTeamProfile.teamId(),
                        awayTeamProfile == null ? null : awayTeamProfile.teamId()
                );
                if (eventId.isPresent()) {
                    return eventId.get();
                }
            }
        }

        for (TheSportsDbTeamProfile profile : List.of(homeTeamProfile, awayTeamProfile)) {
            if (profile == null || profile.teamId() == null) {
                continue;
            }

            for (JsonNode eventsNode : List.of(
                    extractTheSportsDbEvents(theSportsDbClient.fetchTeamLastEvents(profile.teamId())),
                    extractTheSportsDbEvents(theSportsDbClient.fetchTeamNextEvents(profile.teamId()))
            )) {
                Optional<Long> eventId = selectTheSportsDbEvent(
                        eventsNode,
                        match,
                        homeTeam,
                        awayTeam,
                        resolvedHomeTeamName,
                        resolvedAwayTeamName,
                        homeTeamProfile == null ? null : homeTeamProfile.teamId(),
                        awayTeamProfile == null ? null : awayTeamProfile.teamId()
                );
                if (eventId.isPresent()) {
                    return eventId.get();
                }
            }
        }

        throw new IOException("Aucun evenement TheSportsDB correspondant n'a ete trouve pour " + buildMatchLabel(match, homeTeam, awayTeam) + ".");
    }

    private JsonNode extractTheSportsDbEvents(JsonNode payload) {
        JsonNode results = payload.path("results");
        if (results.isArray()) {
            return results;
        }
        JsonNode events = payload.path("events");
        return events.isArray() ? events : results;
    }

    private void addEventQuery(List<String> queries, String homeTeamName, String awayTeamName) {
        String home = normalizeNullable(homeTeamName);
        String away = normalizeNullable(awayTeamName);
        if (home == null || away == null) {
            return;
        }
        String query = home + "_vs_" + away;
        if (!queries.contains(query)) {
            queries.add(query);
        }
    }

    private Optional<Long> selectTheSportsDbEvent(
            JsonNode eventsNode,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            String resolvedHomeTeamName,
            String resolvedAwayTeamName,
            Long expectedHomeTeamId,
            Long expectedAwayTeamId
    ) {
        if (!eventsNode.isArray() || eventsNode.isEmpty()) {
            return Optional.empty();
        }

        String expectedHome = normalizeTeamName(firstNonBlank(resolvedHomeTeamName, homeTeam == null ? null : homeTeam.getNom()));
        String expectedAway = normalizeTeamName(firstNonBlank(resolvedAwayTeamName, awayTeam == null ? null : awayTeam.getNom()));
        String expectedCompetition = normalizeLeagueName(FootballDataCompetitions.labelOf(match.getCompetitionCode()));
        LocalDateTime localKickoff = toLocalDateTime(match);

        double bestScore = 0.0;
        long bestEventId = 0L;

        for (JsonNode eventNode : eventsNode) {
            String sport = normalizeNullable(eventNode.path("strSport").asText(null));
            if (sport != null && !"soccer".equalsIgnoreCase(sport)) {
                continue;
            }

            String homeName = normalizeTeamName(eventNode.path("strHomeTeam").asText(null));
            String awayName = normalizeTeamName(eventNode.path("strAwayTeam").asText(null));
            Long homeTeamId = nullableLong(eventNode.path("idHomeTeam"));
            Long awayTeamId = nullableLong(eventNode.path("idAwayTeam"));
            double homeScore = similarity(expectedHome, homeName);
            double awayScore = similarity(expectedAway, awayName);
            boolean sameTeamsById = expectedHomeTeamId != null
                    && expectedAwayTeamId != null
                    && Objects.equals(expectedHomeTeamId, homeTeamId)
                    && Objects.equals(expectedAwayTeamId, awayTeamId);
            if (!sameTeamsById && (homeScore < 0.68 || awayScore < 0.68)) {
                continue;
            }

            LocalDate eventDate = parseDateOnly(eventNode.path("dateEvent").asText(null));
            if (match.getDateMatch() != null && eventDate != null) {
                long daysDifference = Math.abs(ChronoUnit.DAYS.between(match.getDateMatch(), eventDate));
                if (daysDifference > 1) {
                    continue;
                }
            }

            double score = sameTeamsById ? 2.4 : homeScore + awayScore;
            String leagueName = normalizeLeagueName(eventNode.path("strLeague").asText(null));
            if (expectedCompetition != null && leagueName != null) {
                score += similarity(expectedCompetition, leagueName);
            }

            LocalDateTime eventKickoff = parseFixtureDateTime(eventNode.path("strTimestamp").asText(null));
            if (eventKickoff == null) {
                eventKickoff = parseTheSportsDbLocalDateTime(eventNode.path("dateEvent").asText(null), eventNode.path("strTime").asText(null));
            }
            if (localKickoff != null && eventKickoff != null) {
                long distanceMinutes = Math.abs(ChronoUnit.MINUTES.between(localKickoff, eventKickoff));
                if (distanceMinutes <= 10) {
                    score += 0.4;
                } else if (distanceMinutes <= 120) {
                    score += 0.2;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestEventId = eventNode.path("idEvent").asLong(0);
            }
        }

        return bestScore >= 1.6 && bestEventId > 0 ? Optional.of(bestEventId) : Optional.empty();
    }

    private ApiFootballLineupSide parseTheSportsDbLineup(
            JsonNode payload,
            boolean homeSide,
            Equipe preferredTeam,
            String preferredTeamName
    ) {
        JsonNode lineupNode = payload.path("lineup");
        if (!lineupNode.isArray() || lineupNode.isEmpty()) {
            return null;
        }

        String preferredName = firstNonBlank(preferredTeamName, preferredTeam == null ? null : preferredTeam.getNom());
        String preferredKey = normalizeTeamName(preferredName);
        Map<String, List<JsonNode>> rowsByTeamKey = new LinkedHashMap<>();
        Map<String, String> teamLabels = new LinkedHashMap<>();
        List<JsonNode> sideFallbackRows = new ArrayList<>();

        for (JsonNode playerNode : lineupNode) {
            String playerName = normalizeNullable(playerNode.path("strPlayer").asText(null));
            if (playerName == null) {
                continue;
            }

            String candidateTeamName = normalizeNullable(playerNode.path("strTeam").asText(null));
            String teamKey = normalizeTeamName(candidateTeamName);
            if (teamKey != null) {
                rowsByTeamKey.computeIfAbsent(teamKey, ignored -> new ArrayList<>()).add(playerNode);
                teamLabels.putIfAbsent(teamKey, candidateTeamName);
            }

            boolean rowMarkedHome = "yes".equalsIgnoreCase(playerNode.path("strHome").asText(""));
            if (rowMarkedHome == homeSide) {
                sideFallbackRows.add(playerNode);
            }
        }

        List<JsonNode> selectedRows = null;
        String detectedTeamName = null;
        double bestScore = 0.0;

        if (preferredKey != null) {
            for (Map.Entry<String, List<JsonNode>> entry : rowsByTeamKey.entrySet()) {
                double score = similarity(preferredKey, entry.getKey());
                if (score > bestScore) {
                    bestScore = score;
                    selectedRows = entry.getValue();
                    detectedTeamName = teamLabels.get(entry.getKey());
                }
            }
        }

        if (selectedRows == null || bestScore < 0.68) {
            selectedRows = sideFallbackRows.isEmpty() ? null : sideFallbackRows;
        }
        if ((selectedRows == null || selectedRows.isEmpty()) && rowsByTeamKey.size() == 2) {
            selectedRows = new ArrayList<>(rowsByTeamKey.values()).get(homeSide ? 0 : 1);
            if (detectedTeamName == null) {
                detectedTeamName = new ArrayList<>(teamLabels.values()).get(homeSide ? 0 : 1);
            }
        }
        if (selectedRows == null || selectedRows.isEmpty()) {
            return null;
        }

        List<TheSportsDbLineupPlayer> starters = new ArrayList<>();
        List<TheSportsDbLineupPlayer> substitutes = new ArrayList<>();
        for (JsonNode playerNode : selectedRows) {
            TheSportsDbLineupPlayer player = mapTheSportsDbLineupPlayer(playerNode);
            if (player == null) {
                continue;
            }
            if (detectedTeamName == null) {
                detectedTeamName = normalizeNullable(playerNode.path("strTeam").asText(null));
            }
            if ("yes".equalsIgnoreCase(playerNode.path("strSubstitute").asText(""))) {
                substitutes.add(player);
            } else {
                starters.add(player);
            }
        }

        starters = dedupeTheSportsDbPlayers(starters);
        substitutes = dedupeTheSportsDbPlayers(substitutes);
        starters.sort(TheSportsDbLineupPlayer.COMPARATOR);
        substitutes.sort(TheSportsDbLineupPlayer.COMPARATOR);

        if (starters.size() < 11 && starters.size() + substitutes.size() >= 11) {
            List<TheSportsDbLineupPlayer> promoted = new ArrayList<>(starters);
            for (TheSportsDbLineupPlayer substitute : substitutes) {
                if (promoted.size() >= 11) {
                    break;
                }
                promoted.add(substitute);
            }
            List<TheSportsDbLineupPlayer> remainingSubstitutes = new ArrayList<>();
            for (TheSportsDbLineupPlayer substitute : substitutes) {
                if (promoted.contains(substitute) && promoted.size() <= 11) {
                    continue;
                }
                remainingSubstitutes.add(substitute);
            }
            starters = promoted;
            substitutes = remainingSubstitutes;
        }
        if (starters.size() > 11) {
            List<TheSportsDbLineupPlayer> remainingSubstitutes = new ArrayList<>(substitutes);
            remainingSubstitutes.addAll(starters.subList(11, starters.size()));
            starters = new ArrayList<>(starters.subList(0, 11));
            substitutes = remainingSubstitutes;
        }

        return new ApiFootballLineupSide(
                firstNonBlank(detectedTeamName, firstNonBlank(preferredTeamName, preferredTeam == null ? null : preferredTeam.getNom())),
                null,
                null,
                mapTheSportsDbPlayers(starters),
                mapTheSportsDbPlayers(substitutes)
        );
    }

    private List<ApiFootballLineupPlayer> mapTheSportsDbPlayers(List<TheSportsDbLineupPlayer> players) {
        List<ApiFootballLineupPlayer> formattedPlayers = new ArrayList<>();
        for (TheSportsDbLineupPlayer player : players) {
            formattedPlayers.add(new ApiFootballLineupPlayer(
                    player.playerName(),
                    player.squadNumber() == null ? null : String.valueOf(player.squadNumber()),
                    player.position(),
                    null,
                    player.photoUrl()
            ));
        }
        return formattedPlayers;
    }

    private TheSportsDbLineupPlayer mapTheSportsDbLineupPlayer(JsonNode playerNode) {
        String playerName = normalizeNullable(playerNode.path("strPlayer").asText(null));
        if (playerName == null) {
            return null;
        }

        Integer squadNumber = parseNullableInteger(playerNode.path("intSquadNumber").asText(null));
        String position = normalizeNullable(playerNode.path("strPosition").asText(null));
        String photoUrl = firstNonBlank(
                normalizeNullable(playerNode.path("strCutout").asText(null)),
                firstNonBlank(
                        normalizeNullable(playerNode.path("strRender").asText(null)),
                        normalizeNullable(playerNode.path("strThumb").asText(null))
                )
        );
        return new TheSportsDbLineupPlayer(playerName, position, squadNumber, photoUrl);
    }

    private List<TheSportsDbLineupPlayer> dedupeTheSportsDbPlayers(List<TheSportsDbLineupPlayer> players) {
        Map<String, TheSportsDbLineupPlayer> deduped = new LinkedHashMap<>();
        if (players == null) {
            return List.of();
        }
        for (TheSportsDbLineupPlayer player : players) {
            if (player == null) {
                continue;
            }
            deduped.putIfAbsent(theSportsDbPlayerKey(player), player);
        }
        return new ArrayList<>(deduped.values());
    }

    private String theSportsDbPlayerKey(TheSportsDbLineupPlayer player) {
        if (player == null) {
            return "";
        }
        String normalizedName = normalizePlayerName(player.playerName());
        String number = player.squadNumber() == null ? null : String.valueOf(player.squadNumber());
        if (normalizedName != null && number != null) {
            return normalizedName + "|shirt:" + number;
        }
        if (normalizedName != null) {
            return normalizedName;
        }
        if (number != null) {
            return "shirt:" + number;
        }
        return "photo:" + firstNonBlank(player.photoUrl(), Integer.toHexString(System.identityHashCode(player)));
    }

    private List<ApiFootballStatisticRow> parseTheSportsDbStatistics(JsonNode payload) {
        JsonNode statisticsNode = payload.path("eventstats");
        if (!statisticsNode.isArray() || statisticsNode.isEmpty()) {
            return List.of();
        }

        Map<String, ApiFootballStatisticRow> rowsByLabel = new LinkedHashMap<>();
        for (JsonNode statNode : statisticsNode) {
            String rawLabel = normalizeNullable(statNode.path("strStat").asText(null));
            if (rawLabel == null) {
                continue;
            }
            rowsByLabel.put(prettyTheSportsDbStatLabel(rawLabel), new ApiFootballStatisticRow(
                    prettyTheSportsDbStatLabel(rawLabel),
                    defaultStatValue(normalizeNullable(statNode.path("intHome").asText(null))),
                    defaultStatValue(normalizeNullable(statNode.path("intAway").asText(null)))
            ));
        }

        List<ApiFootballStatisticRow> rows = new ArrayList<>();
        for (String preferredLabel : List.of(
                "Expected Goals",
                "Ball Possession",
                "Total Shots",
                "Shots on Goal",
                "Shots off Goal",
                "Blocked Shots",
                "Shots Inside Box",
                "Shots Outside Box",
                "Corner Kicks",
                "Offsides",
                "Fouls",
                "Yellow Cards",
                "Red Cards"
        )) {
            ApiFootballStatisticRow row = rowsByLabel.remove(preferredLabel);
            if (row != null) {
                rows.add(row);
            }
        }
        rows.addAll(rowsByLabel.values());
        return rows;
    }

    private String prettyTheSportsDbStatLabel(String rawLabel) {
        return switch (rawLabel) {
            case "Shots insidebox" -> "Shots Inside Box";
            case "Shots outsidebox" -> "Shots Outside Box";
            default -> rawLabel;
        };
    }

    private LocalDate parseDateOnly(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawValue);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTheSportsDbLocalDateTime(String dateValue, String timeValue) {
        LocalDate date = parseDateOnly(dateValue);
        if (date == null) {
            return null;
        }
        try {
            LocalTime time = timeValue == null || timeValue.isBlank() ? LocalTime.MIDNIGHT : LocalTime.parse(timeValue);
            return date.atTime(time);
        } catch (Exception e) {
            return date.atStartOfDay();
        }
    }

    private Integer parseNullableInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseNullableLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String simplifyTeamName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String simplified = foldSpecialLetters(normalized)
                .replaceAll("(?i)\\b(rcd|rcde|rc|ud|sd|cd|ad|as|us|fk|sk|kv|fc|cf|sc|ac|afc|cfc|club)\\b", " ")
                .replaceAll("(?i)\\b(de|del|da|do|of|the)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return simplified.isBlank() ? normalized : simplified;
    }

    private ApiFootballScorerEntry parseTeamScorer(JsonNode playerNode, int leagueId, int teamId) {
        JsonNode statisticsNode = findRelevantStatistics(playerNode.path("statistics"), leagueId, teamId);
        if (statisticsNode == null) {
            return null;
        }

        return new ApiFootballScorerEntry(
                0,
                normalizeNullable(playerNode.path("player").path("name").asText(null)),
                normalizeNullable(statisticsNode.path("team").path("name").asText(null)),
                nullableInt(statisticsNode.path("goals").path("total")),
                nullableInt(statisticsNode.path("goals").path("assists")),
                nullableInt(statisticsNode.path("games").path("appearences")),
                nullableInt(statisticsNode.path("games").path("minutes"))
        );
    }

    private JsonNode findRelevantStatistics(JsonNode statisticsNode, int leagueId, int teamId) {
        if (!statisticsNode.isArray()) {
            return null;
        }

        for (JsonNode node : statisticsNode) {
            if (node.path("team").path("id").asInt(0) == teamId && node.path("league").path("id").asInt(0) == leagueId) {
                return node;
            }
        }

        for (JsonNode node : statisticsNode) {
            if (node.path("team").path("id").asInt(0) == teamId) {
                return node;
            }
        }

        return statisticsNode.isEmpty() ? null : statisticsNode.get(0);
    }

    private List<ApiFootballScorerEntry> parseScorers(JsonNode responseNode) {
        if (!responseNode.isArray()) {
            return List.of();
        }

        List<ApiFootballScorerEntry> scorers = new ArrayList<>();
        int rank = 1;
        for (JsonNode scorerNode : responseNode) {
            JsonNode statisticsNode = scorerNode.path("statistics").isArray() && !scorerNode.path("statistics").isEmpty()
                    ? scorerNode.path("statistics").get(0)
                    : null;
            if (statisticsNode == null) {
                continue;
            }

            scorers.add(new ApiFootballScorerEntry(
                    rank++,
                    normalizeNullable(scorerNode.path("player").path("name").asText(null)),
                    normalizeNullable(statisticsNode.path("team").path("name").asText(null)),
                    nullableInt(statisticsNode.path("goals").path("total")),
                    nullableInt(statisticsNode.path("goals").path("assists")),
                    nullableInt(statisticsNode.path("games").path("appearences")),
                    nullableInt(statisticsNode.path("games").path("minutes"))
            ));
        }
        return scorers;
    }

    private ApiFootballMatchDetails readMatchDetailsFromCache(Matchs match) {
        if (match == null) {
            return null;
        }

        try {
            List<ApiFootballStatisticRow> statistics = match.getApiFootballStatsJson() == null || match.getApiFootballStatsJson().isBlank()
                    ? List.of()
                    : objectMapper.readValue(match.getApiFootballStatsJson(), STATISTICS_TYPE);
            CachedLineups lineups = match.getApiFootballLineupJson() == null || match.getApiFootballLineupJson().isBlank()
                    ? null
                    : objectMapper.readValue(match.getApiFootballLineupJson(), LINEUPS_TYPE);

            if (match.getApiFootballId() == null && statistics.isEmpty() && lineups == null) {
                return null;
            }

            return new ApiFootballMatchDetails(
                    match.getApiFootballId(),
                    match.getApiFootballSyncedAt() == null ? null : match.getApiFootballSyncedAt().toString(),
                    lineups == null ? null : lineups.homeLineup(),
                    lineups == null ? null : lineups.awayLineup(),
                    statistics
            );
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCacheFresh(Matchs match, ApiFootballMatchDetails cached) {
        if (match == null || cached == null || match.getApiFootballSyncedAt() == null) {
            return false;
        }
        if (!cached.hasLineups() && !cached.hasStatistics()) {
            return false;
        }

        Duration age = Duration.between(match.getApiFootballSyncedAt(), LocalDateTime.now());
        String normalizedStatus = normalizeNullable(match.getStatut());
        String lowerStatus = normalizedStatus == null ? "" : normalizedStatus.toLowerCase(Locale.ROOT);
        boolean live = lowerStatus.contains("direct") || lowerStatus.contains("cours") || lowerStatus.contains("live");
        boolean finished = lowerStatus.contains("fini");

        if (live) {
            return age.compareTo(Duration.ofMinutes(2)) < 0;
        }
        if (finished) {
            if (cached.hasLineups() && cached.hasStatistics()) {
                return age.compareTo(Duration.ofDays(30)) < 0;
            }
            return age.compareTo(Duration.ofHours(6)) < 0;
        }
        return age.compareTo(Duration.ofMinutes(20)) < 0;
    }

    private ApiFootballLineupSide mergeLineup(ApiFootballLineupSide cached, ApiFootballLineupSide fresh) {
        if (cached == null) {
            return fresh;
        }
        if (fresh == null) {
            return cached;
        }

        return new ApiFootballLineupSide(
                firstNonBlank(fresh.teamName(), cached.teamName()),
                firstNonBlank(fresh.formation(), cached.formation()),
                firstNonBlank(fresh.coachName(), cached.coachName()),
                mergePlayers(cached.startingPlayers(), fresh.startingPlayers()),
                mergePlayers(cached.substitutePlayers(), fresh.substitutePlayers())
        );
    }

    private List<ApiFootballLineupPlayer> mergePlayers(
            List<ApiFootballLineupPlayer> cachedPlayers,
            List<ApiFootballLineupPlayer> freshPlayers
    ) {
        if (freshPlayers == null || freshPlayers.isEmpty()) {
            return cachedPlayers == null ? List.of() : cachedPlayers;
        }
        if (cachedPlayers == null || cachedPlayers.isEmpty()) {
            return freshPlayers;
        }
        if (freshPlayers.size() >= 11 && cachedPlayers.size() < 11) {
            return freshPlayers;
        }
        if (cachedPlayers.size() >= 11 && freshPlayers.size() < 11) {
            return cachedPlayers;
        }

        Map<String, ApiFootballLineupPlayer> cachedByKey = indexPlayers(cachedPlayers);
        Map<String, ApiFootballLineupPlayer> freshByKey = indexPlayers(freshPlayers);

        List<ApiFootballLineupPlayer> baseOrder = playerCollectionScore(freshPlayers) >= playerCollectionScore(cachedPlayers)
                ? freshPlayers
                : cachedPlayers;
        List<ApiFootballLineupPlayer> secondaryOrder = baseOrder == freshPlayers ? cachedPlayers : freshPlayers;

        List<ApiFootballLineupPlayer> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ApiFootballLineupPlayer player : baseOrder) {
            String key = playerKey(player);
            ApiFootballLineupPlayer combined = mergePlayer(cachedByKey.get(key), freshByKey.get(key));
            if (combined == null) {
                continue;
            }
            seen.add(key);
            merged.add(combined);
        }

        for (ApiFootballLineupPlayer player : secondaryOrder) {
            String key = playerKey(player);
            if (seen.contains(key)) {
                continue;
            }
            ApiFootballLineupPlayer combined = mergePlayer(cachedByKey.get(key), freshByKey.get(key));
            if (combined == null) {
                continue;
            }
            seen.add(key);
            merged.add(combined);
        }

        return merged;
    }

    private Map<String, ApiFootballLineupPlayer> indexPlayers(List<ApiFootballLineupPlayer> players) {
        Map<String, ApiFootballLineupPlayer> indexed = new LinkedHashMap<>();
        if (players == null) {
            return indexed;
        }

        for (ApiFootballLineupPlayer player : players) {
            if (player == null) {
                continue;
            }
            indexed.putIfAbsent(playerKey(player), player);
        }
        return indexed;
    }

    private int playerCollectionScore(List<ApiFootballLineupPlayer> players) {
        if (players == null || players.isEmpty()) {
            return 0;
        }

        int score = players.size() * 100;
        for (ApiFootballLineupPlayer player : players) {
            if (player == null) {
                continue;
            }
            score += player.hasPhoto() ? 10 : 0;
            score += player.hasGrid() ? 6 : 0;
            score += player.hasPosition() ? 4 : 0;
            score += player.shirtNumber() != null && !player.shirtNumber().isBlank() ? 2 : 0;
        }
        return score;
    }

    private ApiFootballLineupPlayer mergePlayer(ApiFootballLineupPlayer cached, ApiFootballLineupPlayer fresh) {
        if (cached == null) {
            return fresh;
        }
        if (fresh == null) {
            return cached;
        }

        return new ApiFootballLineupPlayer(
                firstNonBlank(fresh.playerName(), cached.playerName()),
                firstNonBlank(fresh.shirtNumber(), cached.shirtNumber()),
                firstNonBlank(fresh.position(), cached.position()),
                firstNonBlank(fresh.grid(), cached.grid()),
                firstNonBlank(fresh.photoUrl(), cached.photoUrl())
        );
    }

    private String playerKey(ApiFootballLineupPlayer player) {
        if (player == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        String nameKey = normalizePlayerName(player.playerName());
        if (nameKey != null) {
            parts.add("name:" + nameKey);
        }
        String number = normalizeNullable(player.shirtNumber());
        if (number != null) {
            parts.add("shirt:" + number);
        }
        String grid = normalizeNullable(player.grid());
        if (grid != null) {
            parts.add("grid:" + grid);
        }
        String position = normalizePlayerName(player.position());
        if (position != null) {
            parts.add("pos:" + position);
        }
        String photo = normalizeNullable(player.photoUrl());
        if (photo != null) {
            parts.add("photo:" + photo);
        }
        return parts.isEmpty() ? "unknown:" + Integer.toHexString(System.identityHashCode(player)) : String.join("|", parts);
    }

    private String normalizePlayerName(String value) {
        return normalizeTeamName(value);
    }

    private Integer ensureTeamApiFootballId(Equipe team, String competitionCode, int seasonYear)
            throws SQLException, IOException, InterruptedException {
        if (team == null) {
            return null;
        }
        if (team.getApiFootballId() != null) {
            return Math.toIntExact(team.getApiFootballId());
        }

        Integer leagueId = ApiFootballCompetitionMappings.leagueIdOf(competitionCode);
        if (leagueId == null) {
            return null;
        }

        String cacheKey = leagueId + ":" + seasonYear;
        Map<String, Long> mappedTeams = getCached(TEAM_LOOKUP_CACHE, cacheKey, TEAM_LOOKUP_CACHE_TTL);
        if (mappedTeams == null) {
            mappedTeams = loadCompetitionTeamIds(leagueId, seasonYear);
            TEAM_LOOKUP_CACHE.put(cacheKey, new CacheEntry<>(mappedTeams, Instant.now()));
        }

        Long resolvedId = resolveTeamId(mappedTeams, team.getNom());
        if (resolvedId == null) {
            return null;
        }

        updateTeamApiFootballId(team.getId(), resolvedId);
        team.setApiFootballId(resolvedId);
        return Math.toIntExact(resolvedId);
    }

    private Map<String, Long> loadCompetitionTeamIds(int leagueId, int seasonYear) throws IOException, InterruptedException {
        JsonNode payload = requireApiClient().fetchCompetitionTeams(leagueId, seasonYear);
        JsonNode responseNode = payload.path("response");
        Map<String, Long> teams = new LinkedHashMap<>();
        if (!responseNode.isArray()) {
            return teams;
        }

        for (JsonNode teamEntry : responseNode) {
            JsonNode teamNode = teamEntry.path("team");
            long teamId = teamNode.path("id").asLong(0);
            String teamName = normalizeNullable(teamNode.path("name").asText(null));
            if (teamId <= 0 || teamName == null) {
                continue;
            }
            teams.put(normalizeTeamName(teamName), teamId);
        }
        return teams;
    }

    private Long resolveTeamId(Map<String, Long> mappedTeams, String localTeamName) {
        String normalizedLocalName = normalizeTeamName(localTeamName);
        if (normalizedLocalName == null || mappedTeams == null || mappedTeams.isEmpty()) {
            return null;
        }

        Long exact = mappedTeams.get(normalizedLocalName);
        if (exact != null) {
            return exact;
        }

        double bestScore = 0.0;
        Long bestMatch = null;
        for (Map.Entry<String, Long> entry : mappedTeams.entrySet()) {
            double score = similarity(normalizedLocalName, entry.getKey());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getValue();
            }
        }
        return bestScore >= 0.72 ? bestMatch : null;
    }

    private long resolveFixtureId(
            Matchs match,
            String competitionCode,
            int seasonYear,
            Equipe homeTeam,
            Equipe awayTeam,
            Integer homeApiFootballId,
            Integer awayApiFootballId
    ) throws SQLException, IOException, InterruptedException {
        if (match.getApiFootballId() != null) {
            return match.getApiFootballId();
        }

        Integer leagueId = ApiFootballCompetitionMappings.leagueIdOf(competitionCode);
        if (leagueId == null || match.getDateMatch() == null) {
            throw new IOException("Impossible d'identifier la competition ou la date du match.");
        }

        List<LocalDate> datesToTry = List.of(
                match.getDateMatch(),
                match.getDateMatch().minusDays(1),
                match.getDateMatch().plusDays(1)
        );

        for (LocalDate date : datesToTry) {
            JsonNode payload = requireApiClient().fetchFixturesByDate(leagueId, seasonYear, date);
            Optional<Long> candidate = selectFixtureId(payload.path("response"), match, homeTeam, awayTeam, homeApiFootballId, awayApiFootballId);
            if (candidate.isPresent()) {
                updateMatchApiFootballId(match.getId(), candidate.get());
                match.setApiFootballId(candidate.get());
                return candidate.get();
            }
        }

        throw new IOException("Aucun fixture API-Football correspondant n'a ete trouve pour " + buildMatchLabel(match, homeTeam, awayTeam) + ".");
    }

    private Optional<Long> selectFixtureId(
            JsonNode fixturesNode,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            Integer homeApiFootballId,
            Integer awayApiFootballId
    ) {
        if (!fixturesNode.isArray()) {
            return Optional.empty();
        }

        long bestFixtureId = 0;
        long bestDistanceMinutes = Long.MAX_VALUE;
        String normalizedHome = normalizeTeamName(homeTeam == null ? null : homeTeam.getNom());
        String normalizedAway = normalizeTeamName(awayTeam == null ? null : awayTeam.getNom());
        LocalDateTime localKickoff = toLocalDateTime(match);

        for (JsonNode fixtureNode : fixturesNode) {
            JsonNode teamsNode = fixtureNode.path("teams");
            JsonNode homeNode = teamsNode.path("home");
            JsonNode awayNode = teamsNode.path("away");

            boolean sameTeamsById = homeApiFootballId != null && awayApiFootballId != null
                    && homeNode.path("id").asInt(0) == homeApiFootballId
                    && awayNode.path("id").asInt(0) == awayApiFootballId;

            double homeNameScore = similarity(normalizedHome, normalizeTeamName(homeNode.path("name").asText(null)));
            double awayNameScore = similarity(normalizedAway, normalizeTeamName(awayNode.path("name").asText(null)));
            boolean sameTeamsByName = homeNameScore >= 0.72 && awayNameScore >= 0.72;

            if (!sameTeamsById && !sameTeamsByName) {
                continue;
            }

            LocalDateTime fixtureKickoff = parseFixtureDateTime(fixtureNode.path("fixture").path("date").asText(null));
            long distanceMinutes = localKickoff == null || fixtureKickoff == null
                    ? 0
                    : Math.abs(ChronoUnit.MINUTES.between(localKickoff, fixtureKickoff));

            if (distanceMinutes < bestDistanceMinutes) {
                bestDistanceMinutes = distanceMinutes;
                bestFixtureId = fixtureNode.path("fixture").path("id").asLong(0);
            }
        }

        return bestFixtureId > 0 ? Optional.of(bestFixtureId) : Optional.empty();
    }

    private ApiFootballLineupSide parseLineup(JsonNode payload, boolean homeSide, Equipe preferredTeam) {
        JsonNode responseNode = payload.path("response");
        if (!responseNode.isArray() || responseNode.isEmpty()) {
            return null;
        }

        if (responseNode.size() == 1) {
            return mapLineupSide(responseNode.get(0));
        }

        Integer preferredId = preferredTeam == null || preferredTeam.getApiFootballId() == null
                ? null
                : Math.toIntExact(preferredTeam.getApiFootballId());
        String preferredName = normalizeTeamName(preferredTeam == null ? null : preferredTeam.getNom());

        for (JsonNode teamLineupNode : responseNode) {
            JsonNode teamNode = teamLineupNode.path("team");
            if (preferredId != null && teamNode.path("id").asInt(0) == preferredId) {
                return mapLineupSide(teamLineupNode);
            }
            if (preferredName != null
                    && similarity(preferredName, normalizeTeamName(teamNode.path("name").asText(null))) >= 0.72) {
                return mapLineupSide(teamLineupNode);
            }
        }

        return homeSide ? mapLineupSide(responseNode.get(0)) : mapLineupSide(responseNode.get(1));
    }

    private ApiFootballLineupSide mapLineupSide(JsonNode lineupNode) {
        if (lineupNode == null || lineupNode.isMissingNode()) {
            return null;
        }

        String teamName = normalizeNullable(lineupNode.path("team").path("name").asText(null));
        String formation = normalizeNullable(lineupNode.path("formation").asText(null));
        String coachName = normalizeNullable(lineupNode.path("coach").path("name").asText(null));
        List<ApiFootballLineupPlayer> startXi = extractPlayers(lineupNode.path("startXI"));
        List<ApiFootballLineupPlayer> substitutes = extractPlayers(lineupNode.path("substitutes"));

        return new ApiFootballLineupSide(teamName, formation, coachName, startXi, substitutes);
    }

    private List<ApiFootballLineupPlayer> extractPlayers(JsonNode playersNode) {
        if (!playersNode.isArray()) {
            return List.of();
        }

        List<ApiFootballLineupPlayer> players = new ArrayList<>();
        for (JsonNode playerWrapper : playersNode) {
            JsonNode playerNode = playerWrapper.path("player");
            ApiFootballLineupPlayer player = mapApiFootballPlayer(playerNode);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private ApiFootballLineupPlayer mapApiFootballPlayer(JsonNode playerNode) {
        if (playerNode == null || playerNode.isMissingNode()) {
            return null;
        }

        String name = normalizeNullable(playerNode.path("name").asText(null));
        if (name == null) {
            return null;
        }

        return new ApiFootballLineupPlayer(
                name,
                normalizeNullable(playerNode.path("number").asText(null)),
                prettifyApiFootballPosition(normalizeNullable(playerNode.path("pos").asText(null))),
                normalizeNullable(playerNode.path("grid").asText(null)),
                normalizeNullable(playerNode.path("photo").asText(null))
        );
    }

    private String prettifyApiFootballPosition(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "G" -> "Goalkeeper";
            case "D" -> "Defender";
            case "M" -> "Midfielder";
            case "F" -> "Forward";
            default -> value;
        };
    }

    private List<ApiFootballStatisticRow> parseStatistics(JsonNode payload) {
        JsonNode responseNode = payload.path("response");
        if (!responseNode.isArray() || responseNode.size() < 2) {
            return List.of();
        }

        JsonNode homeNode = responseNode.get(0);
        JsonNode awayNode = responseNode.get(1);
        Map<String, String> homeStats = extractStatisticsMap(homeNode.path("statistics"));
        Map<String, String> awayStats = extractStatisticsMap(awayNode.path("statistics"));

        List<ApiFootballStatisticRow> rows = new ArrayList<>();
        for (String key : List.of(
                "Expected Goals",
                "Ball Possession",
                "Total Shots",
                "Shots on Goal",
                "Shots off Goal",
                "Corner Kicks",
                "Offsides",
                "Fouls",
                "Yellow Cards",
                "Red Cards",
                "Goalkeeper Saves",
                "Total passes",
                "Passes accurate",
                "Passes %"
        )) {
            String homeValue = homeStats.get(key);
            String awayValue = awayStats.get(key);
            if (homeValue == null && awayValue == null) {
                continue;
            }
            rows.add(new ApiFootballStatisticRow(key, defaultStatValue(homeValue), defaultStatValue(awayValue)));
        }
        return rows;
    }

    private Map<String, String> extractStatisticsMap(JsonNode statisticsNode) {
        Map<String, String> statistics = new LinkedHashMap<>();
        if (!statisticsNode.isArray()) {
            return statistics;
        }

        for (JsonNode statisticNode : statisticsNode) {
            String type = normalizeNullable(statisticNode.path("type").asText(null));
            if (type == null) {
                continue;
            }
            JsonNode valueNode = statisticNode.path("value");
            String value = valueNode.isNull() || valueNode.isMissingNode() ? null : normalizeNullable(valueNode.asText(null));
            if (value != null) {
                statistics.put(type, value);
            }
        }
        return statistics;
    }

    private void persistMatchDetails(Matchs match, ApiFootballMatchDetails details) throws SQLException, IOException {
        if (match == null || match.getId() == null || details == null) {
            return;
        }

        String statisticsJson = details.statistics() == null || details.statistics().isEmpty()
                ? match.getApiFootballStatsJson()
                : objectMapper.writeValueAsString(details.statistics());
        CachedLineups cachedLineups = new CachedLineups(details.homeLineup(), details.awayLineup());
        String lineupsJson = (details.homeLineup() == null && details.awayLineup() == null && match.getApiFootballLineupJson() != null)
                ? match.getApiFootballLineupJson()
                : objectMapper.writeValueAsString(cachedLineups);
        String homeStartingLineup = details.homeLineup() != null && details.homeLineup().hasStartingPlayers()
                ? formatStartingLineup(details.homeLineup())
                : match.getLineupDomicile();
        String awayStartingLineup = details.awayLineup() != null && details.awayLineup().hasStartingPlayers()
                ? formatStartingLineup(details.awayLineup())
                : match.getLineupExterieur();

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE matchs SET api_football_id = ?, api_football_stats_json = ?, api_football_lineup_json = ?, api_football_synced_at = ?, lineup_domicile = ?, lineup_exterieur = ? WHERE id = ?")) {
            setNullableLong(statement, 1, details.fixtureId());
            statement.setString(2, statisticsJson);
            statement.setString(3, lineupsJson);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(5, homeStartingLineup);
            statement.setString(6, awayStartingLineup);
            statement.setInt(7, match.getId());
            statement.executeUpdate();
        }

        match.setApiFootballId(details.fixtureId());
        match.setApiFootballStatsJson(statisticsJson);
        match.setApiFootballLineupJson(lineupsJson);
        match.setApiFootballSyncedAt(LocalDateTime.now());
        match.setLineupDomicile(homeStartingLineup);
        match.setLineupExterieur(awayStartingLineup);
    }

    private List<ApiFootballScorerEntry> rankEntries(List<ApiFootballScorerEntry> entries) {
        List<ApiFootballScorerEntry> ranked = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            ApiFootballScorerEntry entry = entries.get(index);
            ranked.add(new ApiFootballScorerEntry(
                    index + 1,
                    entry.playerName(),
                    entry.teamName(),
                    entry.goals(),
                    entry.assists(),
                    entry.appearances(),
                    entry.minutes()
            ));
        }
        return ranked;
    }

    private ApiFootballClient requireApiClient() throws IOException {
        if (apiClient != null) {
            return apiClient;
        }
        throw new IOException("API-Football non configure. Ajoutez API_FOOTBALL_KEY ou api-football.local.properties.");
    }

    private FootballDataApiClient requireFootballDataApiClient() throws IOException {
        if (footballDataApiClient != null) {
            return footballDataApiClient;
        }
        throw new IOException("football-data.org non configure. Ajoutez FOOTBALL_DATA_API_KEY ou football-data.local.properties.");
    }

    private <T> List<T> limit(List<T> values, int size) {
        if (values.size() <= size) {
            return values;
        }
        return new ArrayList<>(values.subList(0, size));
    }

    private <T> T getCached(Map<String, CacheEntry<T>> cache, String key, Duration ttl) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (Duration.between(entry.storedAt(), Instant.now()).compareTo(ttl) >= 0) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    private void updateTeamApiFootballId(Integer teamId, long apiFootballId) throws SQLException {
        if (teamId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE equipe SET api_football_id = ? WHERE id = ?")) {
            statement.setLong(1, apiFootballId);
            statement.setInt(2, teamId);
            statement.executeUpdate();
        }
    }

    private void updateMatchApiFootballId(Integer matchId, long apiFootballId) throws SQLException {
        if (matchId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE matchs SET api_football_id = ? WHERE id = ?")) {
            statement.setLong(1, apiFootballId);
            statement.setInt(2, matchId);
            statement.executeUpdate();
        }
    }

    private Integer nullableInt(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() || !node.isNumber()
                ? null
                : node.asInt();
    }

    private Long nullableLong(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() || !node.canConvertToLong()
                ? null
                : node.asLong();
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private LocalDate referenceDateOf(Matchs match) {
        return match == null || match.getDateMatch() == null ? LocalDate.now() : match.getDateMatch();
    }

    private LocalDateTime toLocalDateTime(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        return match.getHeureDebut() == null
                ? match.getDateMatch().atStartOfDay()
                : match.getDateMatch().atTime(match.getHeureDebut());
    }

    private LocalDateTime parseFixtureDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawValue)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseSofaScoreKickoff(JsonNode timestampNode) {
        if (timestampNode == null || timestampNode.isMissingNode() || timestampNode.isNull()) {
            return null;
        }

        long epochSeconds;
        if (timestampNode.isNumber()) {
            epochSeconds = timestampNode.asLong(0L);
        } else {
            String rawValue = normalizeNullable(timestampNode.asText(null));
            Long parsedValue = parseNullableLong(rawValue);
            epochSeconds = parsedValue == null ? 0L : parsedValue;
        }
        if (epochSeconds <= 0L) {
            return null;
        }

        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private String buildMatchLabel(Matchs match, Equipe homeTeam, Equipe awayTeam) {
        String home = homeTeam == null ? "Equipe domicile" : normalizeNullable(homeTeam.getNom());
        String away = awayTeam == null ? "Equipe exterieur" : normalizeNullable(awayTeam.getNom());
        return (home == null ? "Equipe domicile" : home) + " vs " + (away == null ? "Equipe exterieur" : away);
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private String normalizeTeamName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        String ascii = Normalizer.normalize(foldSpecialLetters(normalized), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ');
        ascii = ascii
                .replace("kobenhavn", "copenhagen")
                .replace("munchen", "munich")
                .replace("koln", "cologne");
        ascii = ascii.replaceAll("\\b(rcd|rcde|rc|ud|sd|cd|ad|as|us|fk|sk|kv|fc|cf|sc|ac|afc|cfc|club|calcio)\\b", " ");
        ascii = ascii.replaceAll("\\b(de|del|da|do|of|the)\\b", " ");
        ascii = ascii.replaceAll("[^a-z0-9]+", " ").trim();
        return ascii.replaceAll("\\s+", " ");
    }

    private String normalizeLeagueName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        String ascii = Normalizer.normalize(foldSpecialLetters(normalized), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        ascii = ascii.replaceAll("[^a-z0-9]+", " ").trim();
        return ascii.replaceAll("\\s+", " ");
    }

    private String foldSpecialLetters(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("ø", "o")
                .replace("Ø", "O")
                .replace("ß", "ss")
                .replace("æ", "ae")
                .replace("Æ", "Ae")
                .replace("œ", "oe")
                .replace("Œ", "Oe")
                .replace("ł", "l")
                .replace("Ł", "L")
                .replace("đ", "d")
                .replace("Đ", "D")
                .replace("þ", "th")
                .replace("Þ", "Th");
    }

    private double bestTeamProfileScore(TheSportsDbTeamProfile profile, String normalizedLocalName) {
        if (profile == null || normalizedLocalName == null) {
            return 0.0;
        }

        double bestScore = similarity(normalizedLocalName, normalizeTeamName(profile.teamName()));
        for (String alias : profile.searchableNames()) {
            bestScore = Math.max(bestScore, similarity(normalizedLocalName, normalizeTeamName(alias)));
        }
        return bestScore;
    }

    private double similarity(String left, String right) {
        if (left == null || right == null) {
            return 0.0;
        }
        if (Objects.equals(left, right)) {
            return 1.0;
        }

        List<String> leftTokens = List.of(left.split("\\s+"));
        List<String> rightTokens = List.of(right.split("\\s+"));
        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        long union = leftTokens.stream().distinct().count() + rightTokens.stream().filter(token -> !leftTokens.contains(token)).count();
        if (union == 0) {
            return 0.0;
        }

        double tokenScore = (double) overlap / union;
        if (left.contains(right) || right.contains(left)) {
            return Math.max(tokenScore, 0.8);
        }
        return tokenScore;
    }

    private String defaultStatValue(String value) {
        return value == null ? "N/A" : value;
    }

    private record CacheEntry<T>(T value, Instant storedAt) {
    }

    private record CachedLineups(ApiFootballLineupSide homeLineup, ApiFootballLineupSide awayLineup) {
    }

    private record FootballDataScorerSnapshot(
            Long teamId,
            String teamName,
            String playerName,
            Integer goals,
            Integer assists,
            Integer appearances
    ) {
    }

    private record TheSportsDbTeamProfile(Long teamId, String teamName, List<String> searchableNames) {
    }

    private record SofaScoreEventMatch(long eventId, double score) {
    }

    private record TheSportsDbLineupPlayer(String playerName, String position, Integer squadNumber, String photoUrl) {
        private static final Comparator<TheSportsDbLineupPlayer> COMPARATOR = Comparator
                .comparing(TheSportsDbLineupPlayer::squadNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TheSportsDbLineupPlayer::playerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
