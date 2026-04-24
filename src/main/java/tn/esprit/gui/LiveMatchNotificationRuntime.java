package tn.esprit.gui;

import javafx.application.Platform;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.Notification;
import tn.esprit.entities.User;
import tn.esprit.security.AuthSession;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchFollowTargetService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.football.ApiFootballFixtureSnapshot;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballMatchDetails;
import tn.esprit.services.football.ApiFootballMatchIncident;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LiveMatchNotificationRuntime {
    private static final LiveMatchNotificationRuntime INSTANCE = new LiveMatchNotificationRuntime();
    private static final int MAX_POLLED_MATCHES = 10;
    private static final DateTimeFormatter KICKOFF_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(daemonFactory("live-match-notification-worker"));
    private final AtomicBoolean pollInProgress = new AtomicBoolean();

    private volatile ScheduledFuture<?> scheduledPoll;
    private volatile Stage ownerStage;

    private volatile MatchFollowTargetService followTargetService;
    private volatile NotificationService notificationService;
    private volatile MatchsService matchsService;
    private volatile EquipeService equipeService;
    private volatile ApiFootballInsightsService apiFootballInsightsService;

    private LiveMatchNotificationRuntime() {
    }

    public static LiveMatchNotificationRuntime getInstance() {
        return INSTANCE;
    }

    public synchronized void bindStage(Stage stage, String fxmlPath, boolean publicView) {
        ownerStage = stage;
        if (publicView || !AuthSession.isAuthenticated()) {
            stopPolling();
            MatchAlertPopupManager.getInstance().dismissAll();
            return;
        }

        try {
            ensureServices();
            startPollingIfNeeded();
            requestImmediatePoll();
        } catch (SQLException e) {
            System.err.println("Live match notification runtime unavailable: " + e.getMessage());
        }
    }

    public void requestImmediatePoll() {
        if (!AuthSession.isAuthenticated()) {
            return;
        }
        scheduler.schedule(this::pollSafely, 0, TimeUnit.SECONDS);
    }

    private synchronized void startPollingIfNeeded() {
        if (scheduledPoll != null && !scheduledPoll.isCancelled()) {
            return;
        }
        scheduledPoll = scheduler.scheduleWithFixedDelay(this::pollSafely, 15, 25, TimeUnit.SECONDS);
    }

    private synchronized void stopPolling() {
        if (scheduledPoll != null) {
            scheduledPoll.cancel(false);
            scheduledPoll = null;
        }
        pollInProgress.set(false);
    }

    private void pollSafely() {
        if (!pollInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            pollFavoriteMatches();
        } catch (Exception e) {
            System.err.println("Live match notification poll failed: " + e.getMessage());
        } finally {
            pollInProgress.set(false);
        }
    }

    private void pollFavoriteMatches() throws SQLException {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null || !currentUser.isActiveAccount()) {
            return;
        }

        ensureServices();

        Set<Integer> followedTeamIds = followTargetService.getFollowedTeamIds(currentUser.getId());
        Set<String> followedCompetitions = followTargetService.getFollowedCompetitionCodes(currentUser.getId());
        if (followedTeamIds.isEmpty() && followedCompetitions.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Matchs> candidates = matchsService.getAll().stream()
                .filter(match -> matchesFollowTargets(match, followedTeamIds, followedCompetitions))
                .filter(match -> isMonitoringCandidate(match, now))
                .sorted(Comparator
                        .comparing(this::kickoffOf, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Matchs::getId, Comparator.nullsLast(Integer::compareTo)))
                .limit(MAX_POLLED_MATCHES)
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        Map<Integer, Equipe> teamsById = loadTeamsById(candidates);
        for (Matchs match : candidates) {
            evaluateMatch(currentUser, now, match, teamsById.get(match.getEquipeDomicileId()), teamsById.get(match.getEquipeExterieurId()));
        }
    }

    private void evaluateMatch(User currentUser, LocalDateTime now, Matchs match, Equipe homeTeam, Equipe awayTeam) {
        if (match == null || currentUser == null || currentUser.getId() == null) {
            return;
        }

        String previousStatus = safeStatus(match.getStatut());
        Integer previousHomeScore = match.getScoreEquipeDomicile();
        Integer previousAwayScore = match.getScoreEquipeExterieur();
        ApiFootballMatchDetails cachedBefore = apiFootballInsightsService.readCachedMatchDetails(match);
        List<ApiFootballMatchIncident> previousIncidents = cachedBefore == null || cachedBefore.incidents() == null
                ? List.of()
                : cachedBefore.incidents();

        ApiFootballFixtureSnapshot snapshot;
        try {
            snapshot = apiFootballInsightsService.refreshFixtureSnapshot(match, homeTeam, awayTeam);
        } catch (Exception e) {
            snapshot = null;
        }

        if (shouldEmitKickoffAlert(match, snapshot, now)) {
            emitNotification(buildKickoffNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }

        if (shouldFetchLiveIncidents(match, snapshot, now)) {
            List<ApiFootballMatchIncident> liveIncidents;
            try {
                liveIncidents = apiFootballInsightsService.refreshMatchIncidents(match, homeTeam, awayTeam);
            } catch (Exception e) {
                liveIncidents = previousIncidents;
            }
            emitIncidentNotifications(currentUser, match, homeTeam, awayTeam, snapshot, previousIncidents, liveIncidents);
        }

        if (shouldEmitFinalAlert(previousStatus, previousHomeScore, previousAwayScore, snapshot, now, match)) {
            emitNotification(buildFinalNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }
    }

    private void emitIncidentNotifications(
            User currentUser,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballFixtureSnapshot snapshot,
            List<ApiFootballMatchIncident> previousIncidents,
            List<ApiFootballMatchIncident> currentIncidents
    ) {
        if (currentIncidents == null || currentIncidents.isEmpty()) {
            return;
        }

        Set<String> previousKeys = new HashSet<>();
        for (ApiFootballMatchIncident incident : previousIncidents) {
            String key = buildIncidentKey(incident);
            if (key != null) {
                previousKeys.add(key);
            }
        }

        for (ApiFootballMatchIncident incident : currentIncidents) {
            if (!isAlertworthyIncident(incident)) {
                continue;
            }

            String incidentKey = buildIncidentKey(incident);
            if (incidentKey == null || previousKeys.contains(incidentKey)) {
                continue;
            }

            emitNotification(buildIncidentNotification(currentUser, match, homeTeam, awayTeam, snapshot, incident, incidentKey));
        }
    }

    private void emitNotification(Notification notification) {
        if (notification == null) {
            return;
        }

        try {
            Notification created = notificationService.createIfAbsent(notification);
            if (created == null) {
                return;
            }

            NavbarNotificationCenter.requestRefreshAll();

            Stage stage = ownerStage;
            if (stage == null || !stage.isShowing()) {
                return;
            }
            Platform.runLater(() -> MatchAlertPopupManager.getInstance().show(stage, created));
        } catch (SQLException e) {
            System.err.println("Live notification could not be stored: " + e.getMessage());
        }
    }

    private Notification buildKickoffNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam, ApiFootballFixtureSnapshot snapshot) {
        String fixtureLabel = buildFixtureLabel(homeTeam, awayTeam);
        String kickoffTime = snapshot != null && snapshot.kickoffAt() != null
                ? snapshot.kickoffAt().toLocalTime().format(KICKOFF_TIME_FORMATTER)
                : kickoffOf(match) == null ? "" : kickoffOf(match).toLocalTime().format(KICKOFF_TIME_FORMATTER);
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setType("Match Start");
        notification.setTitle("Kickoff | " + fixtureLabel);
        notification.setMessage((fixtureLabel + " started" + (kickoffTime.isBlank() ? "." : " at " + kickoffTime + "."))
                + " Live alerts are now active.");
        notification.setMinuteLabel(kickoffTime);
        notification.setDedupeKey("match-start:" + match.getId());
        notification.setAccentTone("info");
        return notification;
    }

    private Notification buildFinalNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam, ApiFootballFixtureSnapshot snapshot) {
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setType("Full Time");
        notification.setTitle("Full Time | " + buildResultLabel(snapshot, match));
        notification.setMessage(buildFixtureLabel(homeTeam, awayTeam) + " is finished.");
        notification.setMinuteLabel("FT");
        notification.setDedupeKey("match-finished:" + match.getId());
        notification.setAccentTone("success");
        return notification;
    }

    private Notification buildIncidentNotification(
            User user,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballFixtureSnapshot snapshot,
            ApiFootballMatchIncident incident,
            String incidentKey
    ) {
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setMinuteLabel(incident.minuteLabel());
        notification.setActorName(resolveIncidentActor(incident));
        notification.setDedupeKey("match-event:" + match.getId() + ":" + incidentKey);

        String scoreLabel = buildResultLabel(snapshot, match);
        if (incident.isGoal()) {
            notification.setType("Goal");
            notification.setTitle("Goal | " + scoreLabel);
            notification.setMessage(resolveIncidentActor(incident) + " scored for "
                    + resolveIncidentTeamName(incident, homeTeam, awayTeam)
                    + incidentMetaSuffix(incident));
            notification.setAccentTone("goal");
            return notification;
        }

        if (incident.isRedCard()) {
            notification.setType("Red Card");
            notification.setTitle("Red Card | " + buildFixtureLabel(homeTeam, awayTeam));
            notification.setMessage(resolveIncidentActor(incident) + " was sent off for "
                    + resolveIncidentTeamName(incident, homeTeam, awayTeam)
                    + incidentMetaSuffix(incident));
            notification.setAccentTone("danger");
            return notification;
        }

        if (incident.isYellowCard()) {
            notification.setType("Yellow Card");
            notification.setTitle("Yellow Card | " + buildFixtureLabel(homeTeam, awayTeam));
            notification.setMessage(resolveIncidentActor(incident) + " was booked for "
                    + resolveIncidentTeamName(incident, homeTeam, awayTeam)
                    + incidentMetaSuffix(incident));
            notification.setAccentTone("warning");
            return notification;
        }

        notification.setType("Substitution");
        notification.setTitle("Substitution | " + buildFixtureLabel(homeTeam, awayTeam));
        notification.setMessage(resolveSubstitutionSummary(incident, homeTeam, awayTeam));
        notification.setAccentTone("info");
        return notification;
    }

    private Notification buildBaseNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam) {
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setUserId(user.getId());
        notification.setMatchId(match == null ? null : match.getId());
        notification.setCompetitionCode(FootballDataCompetitions.labelOf(match == null ? null : match.getCompetitionCode()));
        notification.setHomeTeamName(homeTeam == null ? "Home" : homeTeam.getNom());
        notification.setAwayTeamName(awayTeam == null ? "Away" : awayTeam.getNom());
        notification.setHomeTeamLogo(homeTeam == null ? null : homeTeam.getImage());
        notification.setAwayTeamLogo(awayTeam == null ? null : awayTeam.getImage());
        return notification;
    }

    private boolean shouldEmitKickoffAlert(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now) {
        LocalDateTime kickoff = snapshot != null && snapshot.kickoffAt() != null ? snapshot.kickoffAt() : kickoffOf(match);
        if (kickoff == null) {
            return false;
        }
        if (snapshot != null && snapshot.isLive()) {
            return true;
        }
        return !now.isBefore(kickoff) && now.isBefore(kickoff.plusMinutes(2));
    }

    private boolean shouldEmitFinalAlert(
            String previousStatus,
            Integer previousHomeScore,
            Integer previousAwayScore,
            ApiFootballFixtureSnapshot snapshot,
            LocalDateTime now,
            Matchs match
    ) {
        if (snapshot == null || !snapshot.isFinished()) {
            return false;
        }

        LocalDateTime kickoff = snapshot.kickoffAt() == null ? kickoffOf(match) : snapshot.kickoffAt();
        if (kickoff != null && now.isAfter(kickoff.plusHours(6))) {
            return false;
        }

        if (!isFinishedStatus(previousStatus)) {
            return true;
        }
        return previousHomeScore == null || previousAwayScore == null;
    }

    private boolean shouldFetchLiveIncidents(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now) {
        if (snapshot != null && snapshot.isLive()) {
            return true;
        }
        if (snapshot != null && snapshot.isFinished()) {
            LocalDateTime kickoff = snapshot.kickoffAt() == null ? kickoffOf(match) : snapshot.kickoffAt();
            return kickoff != null && now.isBefore(kickoff.plusHours(4));
        }

        LocalDateTime kickoff = kickoffOf(match);
        return kickoff != null && !now.isBefore(kickoff.minusMinutes(1)) && now.isBefore(kickoff.plusHours(4));
    }

    private boolean matchesFollowTargets(Matchs match, Set<Integer> followedTeamIds, Set<String> followedCompetitions) {
        if (match == null) {
            return false;
        }
        if (match.getEquipeDomicileId() != null && followedTeamIds.contains(match.getEquipeDomicileId())) {
            return true;
        }
        if (match.getEquipeExterieurId() != null && followedTeamIds.contains(match.getEquipeExterieurId())) {
            return true;
        }
        String competitionCode = FootballDataCompetitions.normalizeCode(match.getCompetitionCode());
        return competitionCode != null && followedCompetitions.contains(competitionCode);
    }

    private boolean isMonitoringCandidate(Matchs match, LocalDateTime now) {
        if (match == null) {
            return false;
        }

        if (isLiveStatus(match.getStatut())) {
            return true;
        }

        LocalDateTime kickoff = kickoffOf(match);
        if (kickoff == null) {
            return false;
        }

        if (isFinishedStatus(match.getStatut())) {
            return now.isBefore(kickoff.plusHours(4));
        }

        return !kickoff.isBefore(now.minusHours(4)) && !kickoff.isAfter(now.plusMinutes(20));
    }

    private Map<Integer, Equipe> loadTeamsById(List<Matchs> matches) throws SQLException {
        Map<Integer, Equipe> teamsById = new HashMap<>();
        for (Matchs match : matches) {
            if (match == null) {
                continue;
            }
            loadTeamIntoMap(teamsById, match.getEquipeDomicileId());
            loadTeamIntoMap(teamsById, match.getEquipeExterieurId());
        }
        return teamsById;
    }

    private void loadTeamIntoMap(Map<Integer, Equipe> teamsById, Integer teamId) throws SQLException {
        if (teamId == null || teamsById.containsKey(teamId)) {
            return;
        }
        teamsById.put(teamId, equipeService.getById(teamId));
    }

    private LocalDateTime kickoffOf(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        LocalTime time = match.getHeureDebut() == null ? LocalTime.MIDNIGHT : match.getHeureDebut();
        return match.getDateMatch().atTime(time);
    }

    private boolean isLiveStatus(String status) {
        String normalized = safeStatus(status);
        return normalized.contains("direct")
                || normalized.contains("live")
                || normalized.contains("cours")
                || normalized.contains("mi-temps")
                || normalized.contains("mi temps")
                || normalized.contains("1re mi")
                || normalized.contains("premiere mi")
                || normalized.contains("2e mi")
                || normalized.contains("deuxieme mi")
                || normalized.contains("prolong")
                || normalized.contains("extra time")
                || normalized.contains("tirs au but")
                || normalized.contains("penalties")
                || normalized.contains("shootout");
    }

    private boolean isFinishedStatus(String status) {
        return safeStatus(status).contains("fini");
    }

    private boolean isAlertworthyIncident(ApiFootballMatchIncident incident) {
        return incident != null && (incident.isGoal() || incident.isCard() || incident.isSubstitution());
    }

    private String buildIncidentKey(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return null;
        }

        return String.join("|",
                safeStatus(incident.incidentType()),
                safeStatus(incident.incidentClass()),
                Boolean.toString(incident.homeSide()),
                safeStatus(incident.minuteLabel()),
                safeStatus(incident.playerName()),
                safeStatus(incident.assistPlayerName()),
                safeStatus(incident.playerInName()),
                safeStatus(incident.playerOutName()),
                safeStatus(incident.reason())
        );
    }

    private String buildFixtureLabel(Equipe homeTeam, Equipe awayTeam) {
        return (homeTeam == null || homeTeam.getNom() == null || homeTeam.getNom().isBlank() ? "Home" : homeTeam.getNom())
                + " vs "
                + (awayTeam == null || awayTeam.getNom() == null || awayTeam.getNom().isBlank() ? "Away" : awayTeam.getNom());
    }

    private String buildResultLabel(ApiFootballFixtureSnapshot snapshot, Matchs match) {
        Integer homeScore = snapshot != null && snapshot.homeScore() != null ? snapshot.homeScore() : match == null ? null : match.getScoreEquipeDomicile();
        Integer awayScore = snapshot != null && snapshot.awayScore() != null ? snapshot.awayScore() : match == null ? null : match.getScoreEquipeExterieur();
        return (homeScore == null ? "-" : homeScore) + " - " + (awayScore == null ? "-" : awayScore);
    }

    private String resolveIncidentActor(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "A player";
        }
        if (incident.isSubstitution()) {
            return (empty(incident.playerInName(), "A player") + " for " + empty(incident.playerOutName(), "A player")).trim();
        }
        return empty(incident.playerName(), "A player");
    }

    private String resolveIncidentTeamName(ApiFootballMatchIncident incident, Equipe homeTeam, Equipe awayTeam) {
        if (incident == null) {
            return "their team";
        }
        Equipe team = incident.homeSide() ? homeTeam : awayTeam;
        return team == null || team.getNom() == null || team.getNom().isBlank() ? "their team" : team.getNom();
    }

    private String resolveSubstitutionSummary(ApiFootballMatchIncident incident, Equipe homeTeam, Equipe awayTeam) {
        String teamName = resolveIncidentTeamName(incident, homeTeam, awayTeam);
        String base = empty(incident.playerInName(), "A player")
                + " replaced "
                + empty(incident.playerOutName(), "a teammate")
                + " for "
                + teamName;
        String reason = incident.reason() == null || incident.reason().isBlank() ? "" : ". " + incident.reason().trim();
        return base + reason;
    }

    private String incidentMetaSuffix(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return ".";
        }

        List<String> parts = new ArrayList<>();
        if (incident.minuteLabel() != null && !incident.minuteLabel().isBlank()) {
            parts.add("at " + incident.minuteLabel().trim());
        }
        if (incident.assistPlayerName() != null && !incident.assistPlayerName().isBlank()) {
            parts.add("assist " + incident.assistPlayerName().trim());
        }
        if (incident.reason() != null && !incident.reason().isBlank()) {
            parts.add(incident.reason().trim());
        }
        if (parts.isEmpty()) {
            return ".";
        }
        return " " + String.join(", ", parts) + ".";
    }

    private String safeStatus(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String empty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void ensureServices() throws SQLException {
        if (followTargetService == null) {
            followTargetService = new MatchFollowTargetService();
        }
        if (notificationService == null) {
            notificationService = new NotificationService();
        }
        if (matchsService == null) {
            matchsService = new MatchsService();
        }
        if (equipeService == null) {
            equipeService = new EquipeService();
        }
        if (apiFootballInsightsService == null) {
            apiFootballInsightsService = new ApiFootballInsightsService();
        }
    }

    private static ThreadFactory daemonFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }
}
