package tn.esprit.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Matchs;
import tn.esprit.services.football.ApiFootballLineupPlayer;
import tn.esprit.services.football.ApiFootballLineupSide;
import tn.esprit.services.football.ApiFootballMatchIncident;
import tn.esprit.services.football.ApiFootballStatisticRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MatchLiveCompanionAnalyzer {
    private static final TypeReference<List<ApiFootballStatisticRow>> STATISTICS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ApiFootballMatchIncident>> INCIDENTS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<StoredLineups> LINEUPS_TYPE = new TypeReference<>() {
    };
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)(?:\\+(\\d+))?'");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    MatchLiveCompanionResponse analyze(Matchs match) {
        Objects.requireNonNull(match, "match");

        ParsedMatchData parsed = parse(match);
        int minute = resolveMinute(match, parsed.incidents());
        MomentumSnapshot momentum = computeMomentum(parsed.statistics(), parsed.incidents(), minute);
        RecentDangerSnapshot recentDanger = computeRecentDanger(parsed.incidents(), minute, momentum);
        List<String> turningPoints = detectTurningPoints(parsed.incidents());
        List<MatchLiveCompanionResponse.PlayerImpact> topImpacts = rankPlayerImpacts(parsed.lineups(), parsed.incidents());
        int intensityScore = computeIntensity(match, parsed.statistics(), parsed.incidents(), minute);
        String summary = buildSummary(match, momentum, recentDanger, intensityScore, turningPoints);

        return new MatchLiveCompanionResponse(
                match.getId(),
                formatScore(match),
                normalizeApiStatus(match.getStatut()),
                minute,
                new MatchLiveCompanionResponse.Momentum(
                        momentum.dominantTeam(),
                        momentum.homePressure(),
                        momentum.awayPressure()
                ),
                recentDanger.dangerLevel(),
                turningPoints,
                topImpacts,
                intensityScore,
                summary
        );
    }

    private ParsedMatchData parse(Matchs match) {
        List<ApiFootballStatisticRow> statistics = readValue(match.getApiFootballStatsJson(), STATISTICS_TYPE, List.of());
        List<ApiFootballMatchIncident> incidents = readValue(match.getApiFootballIncidentsJson(), INCIDENTS_TYPE, List.of()).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt((ApiFootballMatchIncident incident) -> incident.minute() == null ? Integer.MAX_VALUE : incident.minute())
                        .thenComparingInt(incident -> incident.addedTime() == null ? 0 : incident.addedTime()))
                .toList();
        StoredLineups lineups = readValue(match.getApiFootballLineupJson(), LINEUPS_TYPE, new StoredLineups(null, null));
        return new ParsedMatchData(statistics, incidents, lineups);
    }

    private <T> T readValue(String rawJson, TypeReference<T> typeReference, T fallback) {
        if (rawJson == null || rawJson.isBlank()) {
            return fallback;
        }
        try {
            T value = objectMapper.readValue(rawJson, typeReference);
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int resolveMinute(Matchs match, List<ApiFootballMatchIncident> incidents) {
        Integer statusMinute = extractStatusMinute(match == null ? null : match.getStatut());
        if (statusMinute != null) {
            return statusMinute;
        }
        return incidents.stream()
                .map(ApiFootballMatchIncident::minute)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private Integer extractStatusMinute(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        Matcher matcher = MINUTE_PATTERN.matcher(status);
        if (!matcher.find()) {
            return null;
        }
        int minute = safeParseInt(matcher.group(1));
        int addedTime = safeParseInt(matcher.group(2));
        return minute + addedTime;
    }

    private MomentumSnapshot computeMomentum(
            List<ApiFootballStatisticRow> statistics,
            List<ApiFootballMatchIncident> incidents,
            int currentMinute
    ) {
        double homeRaw = 0;
        double awayRaw = 0;

        homeRaw += cappedContribution(statistics, true, 32, 8, "shots on goal", "shots on target");
        awayRaw += cappedContribution(statistics, false, 32, 8, "shots on goal", "shots on target");

        homeRaw += cappedContribution(statistics, true, 28, 4, "total shots");
        awayRaw += cappedContribution(statistics, false, 28, 4, "total shots");

        homeRaw += cappedContribution(statistics, true, 25, 1, "dangerous attacks", "dangerous attack");
        awayRaw += cappedContribution(statistics, false, 25, 1, "dangerous attacks", "dangerous attack");

        homeRaw += cappedContribution(statistics, true, 18, 3, "corner kicks", "corners");
        awayRaw += cappedContribution(statistics, false, 18, 3, "corner kicks", "corners");

        homeRaw += Math.min(22, numericStat(statistics, true, "ball possession", "possession") * 0.4);
        awayRaw += Math.min(22, numericStat(statistics, false, "ball possession", "possession") * 0.4);

        for (ApiFootballMatchIncident incident : incidents) {
            boolean recent = incident.minute() != null && currentMinute > 0 && incident.minute() >= Math.max(0, currentMinute - 10);
            if (incident.isGoal() && recent) {
                if (incident.homeSide()) {
                    homeRaw += 15;
                } else {
                    awayRaw += 15;
                }
            }
            if (incident.isRedCard()) {
                if (incident.homeSide()) {
                    awayRaw += 10;
                } else {
                    homeRaw += 10;
                }
            }
            if (incident.isSubstitution() && recent) {
                if (incident.homeSide()) {
                    homeRaw += 2;
                } else {
                    awayRaw += 2;
                }
            }
            if (isPenaltyMissed(incident) && recent) {
                if (incident.homeSide()) {
                    awayRaw += 5;
                } else {
                    homeRaw += 5;
                }
            }
            if (isPenaltyScored(incident) && recent) {
                if (incident.homeSide()) {
                    homeRaw += 5;
                } else {
                    awayRaw += 5;
                }
            }
        }

        int homePressure = normalizePressure(homeRaw, awayRaw);
        int awayPressure = normalizePressure(awayRaw, homeRaw);
        int difference = Math.abs(homePressure - awayPressure);
        String dominantTeam = difference <= 8 ? "balanced" : (homePressure > awayPressure ? "home" : "away");
        return new MomentumSnapshot(homeRaw, awayRaw, homePressure, awayPressure, dominantTeam);
    }

    private double cappedContribution(
            List<ApiFootballStatisticRow> statistics,
            boolean homeSide,
            double cap,
            double weight,
            String... labels
    ) {
        return Math.min(cap, numericStat(statistics, homeSide, labels) * weight);
    }

    private int normalizePressure(double ownRaw, double otherRaw) {
        double total = ownRaw + otherRaw;
        if (total <= 0.01) {
            return 50;
        }

        double shareScore = (ownRaw / total) * 70.0;
        double absoluteScore = Math.min(1.0, ownRaw / 120.0) * 30.0;
        return clampInt((int) Math.round(shareScore + absoluteScore), 0, 100);
    }

    private RecentDangerSnapshot computeRecentDanger(
            List<ApiFootballMatchIncident> incidents,
            int currentMinute,
            MomentumSnapshot momentum
    ) {
        if (currentMinute <= 0 || incidents.isEmpty()) {
            int pressureGap = Math.abs(momentum.homePressure() - momentum.awayPressure());
            String fallbackLevel = pressureGap >= 20 ? "medium" : "low";
            String side = pressureGap <= 8 ? "balanced" : momentum.dominantTeam();
            return new RecentDangerSnapshot(side, 0, 0, fallbackLevel);
        }

        int windowStart = Math.max(0, currentMinute - 10);
        double homeDanger = 0;
        double awayDanger = 0;
        boolean majorRecentEvent = false;

        for (ApiFootballMatchIncident incident : incidents) {
            Integer minute = incident.minute();
            if (minute == null || minute < windowStart || minute > currentMinute) {
                continue;
            }

            if (incident.isGoal()) {
                if (incident.homeSide()) {
                    homeDanger += 4;
                } else {
                    awayDanger += 4;
                }
                majorRecentEvent = true;
            } else if (incident.isRedCard()) {
                if (incident.homeSide()) {
                    awayDanger += 3;
                } else {
                    homeDanger += 3;
                }
                majorRecentEvent = true;
            } else if (incident.isSubstitution()) {
                if (incident.homeSide()) {
                    homeDanger += 1;
                } else {
                    awayDanger += 1;
                }
            }

            if (isPenaltyScored(incident)) {
                if (incident.homeSide()) {
                    homeDanger += 2;
                } else {
                    awayDanger += 2;
                }
                majorRecentEvent = true;
            } else if (isPenaltyMissed(incident)) {
                if (incident.homeSide()) {
                    awayDanger += 2;
                } else {
                    homeDanger += 2;
                }
                majorRecentEvent = true;
            }
        }

        double maxDanger = Math.max(homeDanger, awayDanger);
        String dangerLevel;
        if (majorRecentEvent || maxDanger >= 5) {
            dangerLevel = "high";
        } else if (maxDanger >= 2 || Math.abs(momentum.homePressure() - momentum.awayPressure()) >= 15) {
            dangerLevel = "medium";
        } else {
            dangerLevel = "low";
        }

        String dangerousTeam;
        if (Math.abs(homeDanger - awayDanger) < 1) {
            dangerousTeam = momentum.dominantTeam();
        } else {
            dangerousTeam = homeDanger > awayDanger ? "home" : "away";
        }
        if (dangerousTeam == null || dangerousTeam.isBlank()) {
            dangerousTeam = "balanced";
        }

        return new RecentDangerSnapshot(dangerousTeam, homeDanger, awayDanger, dangerLevel);
    }

    private List<String> detectTurningPoints(List<ApiFootballMatchIncident> incidents) {
        List<String> turningPoints = new ArrayList<>();
        int runningHomeScore = 0;
        int runningAwayScore = 0;
        boolean firstGoalCaptured = false;
        int lateSubstitutions = 0;

        for (ApiFootballMatchIncident incident : incidents) {
            String minuteLabel = minuteLabel(incident);
            String teamLabel = incident.homeSide() ? "Home team" : "Away team";

            if (incident.isGoal()) {
                int previousHomeScore = runningHomeScore;
                int previousAwayScore = runningAwayScore;

                if (incident.homeScore() != null && incident.awayScore() != null) {
                    runningHomeScore = incident.homeScore();
                    runningAwayScore = incident.awayScore();
                } else if (incident.homeSide()) {
                    runningHomeScore++;
                } else {
                    runningAwayScore++;
                }

                if (!firstGoalCaptured) {
                    addTurningPoint(turningPoints, teamLabel + " opened the scoring at " + minuteLabel);
                    firstGoalCaptured = true;
                }
                if (runningHomeScore == runningAwayScore) {
                    addTurningPoint(turningPoints, teamLabel + " found the equalizer at " + minuteLabel);
                } else if ((previousHomeScore == previousAwayScore)
                        || (incident.homeSide() && previousHomeScore < previousAwayScore && runningHomeScore > runningAwayScore)
                        || (!incident.homeSide() && previousAwayScore < previousHomeScore && runningAwayScore > runningHomeScore)) {
                    addTurningPoint(turningPoints, teamLabel + " took the lead at " + minuteLabel);
                }
                if (isPenaltyScored(incident)) {
                    addTurningPoint(turningPoints, "Penalty converted for " + teamLabel.toLowerCase(Locale.ROOT) + " at " + minuteLabel);
                }
                if (incident.minute() != null && incident.minute() >= 75) {
                    addTurningPoint(turningPoints, "Late goal for " + teamLabel.toLowerCase(Locale.ROOT) + " at " + minuteLabel);
                }
            } else if (incident.isRedCard()) {
                addTurningPoint(turningPoints, "Red card for " + teamLabel.toLowerCase(Locale.ROOT) + " at " + minuteLabel);
            } else if (isPenaltyMissed(incident)) {
                addTurningPoint(turningPoints, "Penalty missed by " + teamLabel.toLowerCase(Locale.ROOT) + " at " + minuteLabel);
            } else if (incident.isSubstitution() && incident.minute() != null && incident.minute() >= 60 && lateSubstitutions < 2) {
                addTurningPoint(turningPoints, teamLabel + " made a late substitution at " + minuteLabel);
                lateSubstitutions++;
            }

            if (turningPoints.size() >= 8) {
                break;
            }
        }

        return turningPoints;
    }

    private void addTurningPoint(List<String> turningPoints, String text) {
        if (text == null || text.isBlank() || turningPoints.contains(text)) {
            return;
        }
        turningPoints.add(text);
    }

    private List<MatchLiveCompanionResponse.PlayerImpact> rankPlayerImpacts(
            StoredLineups lineups,
            List<ApiFootballMatchIncident> incidents
    ) {
        Map<String, PlayerImpactAccumulator> impactByPlayer = new LinkedHashMap<>();
        registerLineupPlayers(impactByPlayer, lineups.homeLineup(), "home");
        registerLineupPlayers(impactByPlayer, lineups.awayLineup(), "away");

        for (ApiFootballMatchIncident incident : incidents) {
            String side = incident.homeSide() ? "home" : "away";
            if (incident.isGoal()) {
                addImpact(impactByPlayer, incident.playerId(), incident.playerName(), side, 5.0);
                if (isPenaltyScored(incident)) {
                    addImpact(impactByPlayer, incident.playerId(), incident.playerName(), side, 4.0);
                }
            }
            if (isPenaltyMissed(incident)) {
                addImpact(impactByPlayer, incident.playerId(), incident.playerName(), side, -2.0);
            }
            if (incident.assistPlayerName() != null || incident.assistPlayerId() != null) {
                addImpact(impactByPlayer, incident.assistPlayerId(), incident.assistPlayerName(), side, 3.0);
            }
            if (incident.isYellowCard()) {
                addImpact(impactByPlayer, incident.playerId(), incident.playerName(), side, -1.0);
            }
            if (incident.isRedCard()) {
                addImpact(impactByPlayer, incident.playerId(), incident.playerName(), side, -4.0);
            }
            if (incident.isSubstitution()) {
                addImpact(impactByPlayer, incident.playerInId(), incident.playerInName(), side, 1.0);
                addImpact(impactByPlayer, incident.playerOutId(), incident.playerOutName(), side, -0.5);
            }
        }

        return impactByPlayer.values().stream()
                .filter(player -> player.displayName() != null && !player.displayName().isBlank())
                .sorted(Comparator
                        .comparingDouble(PlayerImpactAccumulator::score).reversed()
                        .thenComparing(PlayerImpactAccumulator::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(player -> new MatchLiveCompanionResponse.PlayerImpact(
                        player.displayName(),
                        player.team(),
                        roundToSingleDecimal(player.score())
                ))
                .toList();
    }

    private void registerLineupPlayers(
            Map<String, PlayerImpactAccumulator> impactByPlayer,
            ApiFootballLineupSide lineup,
            String team
    ) {
        if (lineup == null) {
            return;
        }
        registerPlayers(impactByPlayer, lineup.startingPlayers(), team);
        registerPlayers(impactByPlayer, lineup.substitutePlayers(), team);
    }

    private void registerPlayers(
            Map<String, PlayerImpactAccumulator> impactByPlayer,
            List<ApiFootballLineupPlayer> players,
            String team
    ) {
        if (players == null) {
            return;
        }
        for (ApiFootballLineupPlayer player : players) {
            String key = playerKey(player.playerId(), player.playerName());
            if (key == null) {
                continue;
            }
            PlayerImpactAccumulator accumulator = impactByPlayer.computeIfAbsent(
                    key,
                    ignored -> new PlayerImpactAccumulator(
                            player.playerName(),
                            team,
                            0
                    )
            );
            accumulator.setDisplayName(firstNonBlank(accumulator.displayName(), player.playerName()));
            accumulator.setTeam(firstNonBlank(accumulator.team(), team));
            if (player.rating() != null) {
                accumulator.addScore(clamp(player.rating() - 6.5, -1.0, 3.0));
            }
        }
    }

    private void addImpact(
            Map<String, PlayerImpactAccumulator> impactByPlayer,
            Long playerId,
            String playerName,
            String team,
            double delta
    ) {
        String key = playerKey(playerId, playerName);
        if (key == null) {
            return;
        }
        PlayerImpactAccumulator accumulator = impactByPlayer.computeIfAbsent(
                key,
                ignored -> new PlayerImpactAccumulator(playerName, team, 0)
        );
        accumulator.setDisplayName(firstNonBlank(accumulator.displayName(), playerName));
        accumulator.setTeam(firstNonBlank(accumulator.team(), team));
        accumulator.addScore(delta);
    }

    private int computeIntensity(
            Matchs match,
            List<ApiFootballStatisticRow> statistics,
            List<ApiFootballMatchIncident> incidents,
            int minute
    ) {
        int totalGoals = totalGoals(match, incidents);
        int totalShots = (int) Math.round(numericStat(statistics, true, "total shots") + numericStat(statistics, false, "total shots"));
        int totalCards = (int) Math.round(
                sumNumericStats(statistics, true, "yellow cards", "red cards")
                        + sumNumericStats(statistics, false, "yellow cards", "red cards")
        );
        if (totalCards == 0) {
            totalCards = (int) incidents.stream().filter(ApiFootballMatchIncident::isCard).count();
        }
        int totalCorners = (int) Math.round(numericStat(statistics, true, "corner kicks", "corners")
                + numericStat(statistics, false, "corner kicks", "corners"));
        long penalties = incidents.stream().filter(this::isPenaltyEvent).count();
        long lateEvents = incidents.stream()
                .filter(incident -> incident.minute() != null && incident.minute() >= 75)
                .filter(incident -> incident.isGoal() || incident.isCard() || isPenaltyEvent(incident))
                .count();

        int homeScore = defaultScore(match.getScoreEquipeDomicile());
        int awayScore = defaultScore(match.getScoreEquipeExterieur());
        int goalDifference = Math.abs(homeScore - awayScore);
        boolean closeScoreline = goalDifference <= 1;

        double intensity = 0;
        intensity += Math.min(36, totalGoals * 12.0);
        intensity += Math.min(20, totalShots * 1.5);
        intensity += Math.min(15, totalCorners * 1.5);
        intensity += Math.min(20, totalCards * 5.0);
        intensity += Math.min(16, penalties * 8.0);
        intensity += Math.min(15, lateEvents * 5.0);
        if (closeScoreline && (totalGoals > 0 || isLiveStatus(match.getStatut()))) {
            intensity += 10;
        }
        if (minute >= 70 && closeScoreline) {
            intensity += 6;
        }

        return clampInt((int) Math.round(intensity), 0, 100);
    }

    private int totalGoals(Matchs match, List<ApiFootballMatchIncident> incidents) {
        int scoredGoals = defaultScore(match.getScoreEquipeDomicile()) + defaultScore(match.getScoreEquipeExterieur());
        int incidentGoals = (int) incidents.stream().filter(ApiFootballMatchIncident::isGoal).count();
        return Math.max(scoredGoals, incidentGoals);
    }

    private String buildSummary(
            Matchs match,
            MomentumSnapshot momentum,
            RecentDangerSnapshot recentDanger,
            int intensityScore,
            List<String> turningPoints
    ) {
        int pressureGap = Math.abs(momentum.homePressure() - momentum.awayPressure());
        boolean tightScoreline = Math.abs(defaultScore(match.getScoreEquipeDomicile()) - defaultScore(match.getScoreEquipeExterieur())) <= 1;

        String baseSummary;
        if ("home".equals(momentum.dominantTeam())) {
            if ("high".equals(recentDanger.dangerLevel()) || pressureGap >= 20) {
                baseSummary = "Home team is pressing heavily and creating more dangerous actions in the last phase.";
            } else {
                baseSummary = "Away team is defending deep while home team controls momentum.";
            }
        } else if ("away".equals(momentum.dominantTeam())) {
            if ("high".equals(recentDanger.dangerLevel()) || pressureGap >= 20) {
                baseSummary = "Away team is controlling momentum and looks the more dangerous side right now.";
            } else {
                baseSummary = "Home team is being pushed back as away team keeps the stronger rhythm.";
            }
        } else if ("high".equals(recentDanger.dangerLevel())) {
            baseSummary = "The match is balanced, but recent incidents suggest the tempo is increasing.";
        } else if ("medium".equals(recentDanger.dangerLevel())) {
            baseSummary = "The match is balanced with short swings of pressure on both sides.";
        } else {
            baseSummary = "The match is balanced with no clear momentum swing right now.";
        }

        if (tightScoreline && intensityScore >= 60) {
            return baseSummary + " The score is still tight, so the next major action could decide the match.";
        }
        if (!turningPoints.isEmpty() && turningPoints.get(turningPoints.size() - 1).toLowerCase(Locale.ROOT).contains("red card")) {
            return baseSummary + " A recent red card is shaping the rhythm of the game.";
        }
        return baseSummary;
    }

    private double numericStat(List<ApiFootballStatisticRow> statistics, boolean homeSide, String... labels) {
        if (statistics == null || statistics.isEmpty()) {
            return 0;
        }
        for (String label : labels) {
            String normalizedLabel = normalizeLabel(label);
            for (ApiFootballStatisticRow row : statistics) {
                if (row == null || !normalizeLabel(row.label()).equals(normalizedLabel)) {
                    continue;
                }
                return parseNumeric(homeSide ? row.homeValue() : row.awayValue());
            }
        }
        return 0;
    }

    private double sumNumericStats(List<ApiFootballStatisticRow> statistics, boolean homeSide, String... labels) {
        double sum = 0;
        for (String label : labels) {
            sum += numericStat(statistics, homeSide, label);
        }
        return sum;
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
    }

    private double parseNumeric(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(value.replace(',', '.'));
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatScore(Matchs match) {
        return defaultScore(match.getScoreEquipeDomicile()) + "-" + defaultScore(match.getScoreEquipeExterieur());
    }

    private int defaultScore(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeApiStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SCHEDULED";
        }
        String lower = status.toLowerCase(Locale.ROOT);
        if (lower.contains("direct") || lower.contains("cours") || lower.contains("live") || lower.contains("mi-temps")) {
            return "LIVE";
        }
        if (lower.contains("fini") || lower.contains("term")) {
            return "FINISHED";
        }
        if (lower.contains("report") || lower.contains("postpon") || lower.contains("suspend")) {
            return "POSTPONED";
        }
        if (lower.contains("annul") || lower.contains("cancel") || lower.contains("abandon") || lower.contains("forfait")) {
            return "CANCELLED";
        }
        return "SCHEDULED";
    }

    private boolean isLiveStatus(String status) {
        return "LIVE".equals(normalizeApiStatus(status));
    }

    private boolean isPenaltyScored(ApiFootballMatchIncident incident) {
        return incident != null
                && incident.isGoal()
                && containsIgnoreCase(incident.incidentClass(), "penalty");
    }

    private boolean isPenaltyMissed(ApiFootballMatchIncident incident) {
        return incident != null
                && (containsIgnoreCase(incident.reason(), "penalty missed")
                || containsIgnoreCase(incident.reason(), "missed penalty")
                || containsIgnoreCase(incident.incidentClass(), "penalty miss")
                || containsIgnoreCase(incident.incidentClass(), "missed penalty"));
    }

    private boolean isPenaltyEvent(ApiFootballMatchIncident incident) {
        return isPenaltyScored(incident) || isPenaltyMissed(incident);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && needle != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String minuteLabel(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "--";
        }
        if (incident.minuteLabel() != null && !incident.minuteLabel().isBlank()) {
            return incident.minuteLabel();
        }
        if (incident.minute() != null) {
            if (incident.addedTime() != null && incident.addedTime() > 0) {
                return incident.minute() + "+" + incident.addedTime() + "'";
            }
            return incident.minute() + "'";
        }
        return "--";
    }

    private String playerKey(Long playerId, String playerName) {
        if (playerId != null) {
            return "id:" + playerId;
        }
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        return "name:" + playerName.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private double roundToSingleDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int safeParseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ParsedMatchData(
            List<ApiFootballStatisticRow> statistics,
            List<ApiFootballMatchIncident> incidents,
            StoredLineups lineups
    ) {
    }

    private record StoredLineups(ApiFootballLineupSide homeLineup, ApiFootballLineupSide awayLineup) {
    }

    private record MomentumSnapshot(
            double homeRaw,
            double awayRaw,
            int homePressure,
            int awayPressure,
            String dominantTeam
    ) {
    }

    private record RecentDangerSnapshot(
            String dangerousTeam,
            double homeDanger,
            double awayDanger,
            String dangerLevel
    ) {
    }

    private static final class PlayerImpactAccumulator {
        private String displayName;
        private String team;
        private double score;

        private PlayerImpactAccumulator(String displayName, String team, double score) {
            this.displayName = displayName;
            this.team = team;
            this.score = score;
        }

        private String displayName() {
            return displayName;
        }

        private void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        private String team() {
            return team;
        }

        private void setTeam(String team) {
            this.team = team;
        }

        private double score() {
            return score;
        }

        private void addScore(double delta) {
            score += delta;
        }
    }
}
