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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LiveMatchNotificationRuntime {
    private static final LiveMatchNotificationRuntime INSTANCE = new LiveMatchNotificationRuntime();
    private static final int MAX_POLLED_MATCHES = 80;
    private static final int MAX_QUEUED_POPUPS = 6;
    private static final int MATCH_REMINDER_MINUTES = 60;
    private static final int DIRECT_MATCH_CATCH_UP_HOURS = 48;
    private static final DateTimeFormatter KICKOFF_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(daemonFactory("live-match-notification-worker"));
    private final AtomicBoolean pollInProgress = new AtomicBoolean();
    private final Set<Integer> shownQueuedPopupIds = ConcurrentHashMap.newKeySet();

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
            showQueuedUnreadPopups(stage);
        } catch (SQLException e) {
            System.err.println("Live match notification runtime unavailable: " + e.getMessage());
        }
    }

    public void requestImmediatePoll() {
        if (!AuthSession.isAuthenticated()) {
            return;
        }
        try {
            ensureServices();
            startPollingIfNeeded();
        } catch (SQLException e) {
            System.err.println("Live match notification runtime unavailable: " + e.getMessage());
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
        Set<Integer> followedMatchIds = followTargetService.getFollowedMatchIds(currentUser.getId());
        Set<String> followedCompetitions = followTargetService.getFollowedCompetitionCodes(currentUser.getId());
        if (followedTeamIds.isEmpty() && followedMatchIds.isEmpty() && followedCompetitions.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Matchs> candidates = matchsService.getAll().stream()
                .filter(match -> matchesFollowTargets(match, followedMatchIds, followedTeamIds, followedCompetitions))
                .filter(match -> isMonitoringCandidate(match, now, isDirectMatchFavorite(match, followedMatchIds)))
                .sorted(Comparator
                        .comparingInt((Matchs match) -> isLiveStatus(match.getStatut()) ? 0 : 1)
                        .thenComparingLong(match -> kickoffDistanceMinutes(match, now))
                        .thenComparing(Matchs::getId, Comparator.nullsLast(Integer::compareTo)))
                .limit(MAX_POLLED_MATCHES)
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        Map<Integer, Equipe> teamsById = loadTeamsById(candidates);
        for (Matchs match : candidates) {
            evaluateMatch(
                    currentUser,
                    now,
                    match,
                    teamsById.get(match.getEquipeDomicileId()),
                    teamsById.get(match.getEquipeExterieurId()),
                    isDirectMatchFavorite(match, followedMatchIds)
            );
        }
    }

    private void evaluateMatch(User currentUser, LocalDateTime now, Matchs match, Equipe homeTeam, Equipe awayTeam, boolean directMatchFavorite) {
        if (match == null || currentUser == null || currentUser.getId() == null) {
            return;
        }

        Integer previousHomeScore = match.getScoreEquipeDomicile();
        Integer previousAwayScore = match.getScoreEquipeExterieur();

        ApiFootballFixtureSnapshot snapshot;
        try {
            snapshot = apiFootballInsightsService.refreshFixtureSnapshot(match, homeTeam, awayTeam);
        } catch (Exception e) {
            snapshot = null;
        }

        if (shouldEmitReminderAlert(match, snapshot, now, directMatchFavorite)) {
            emitNotification(buildReminderNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }

        if (shouldEmitKickoffAlert(match, snapshot, now, directMatchFavorite)) {
            emitNotification(buildKickoffNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }

        if (shouldEmitHalfTimeAlert(match, snapshot, now, directMatchFavorite)) {
            emitNotification(buildHalfTimeNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }

        if (shouldEmitSecondHalfAlert(match, snapshot, now, directMatchFavorite)) {
            emitNotification(buildSecondHalfNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }

        List<ApiFootballMatchIncident> liveIncidents = List.of();
        if (shouldFetchLiveIncidents(match, snapshot, now, directMatchFavorite)) {
            liveIncidents = loadLiveIncidents(match, homeTeam, awayTeam);
            emitIncidentNotifications(currentUser, match, homeTeam, awayTeam, snapshot, liveIncidents);
        }

        if (!hasGoalIncident(liveIncidents)) {
            emitScoreFallbackGoalNotifications(currentUser, match, homeTeam, awayTeam, snapshot, previousHomeScore, previousAwayScore);
        }

        if (shouldEmitFinalAlert(snapshot, now, match, directMatchFavorite)) {
            emitNotification(buildFinalNotification(currentUser, match, homeTeam, awayTeam, snapshot));
        }
    }

    private List<ApiFootballMatchIncident> loadLiveIncidents(Matchs match, Equipe homeTeam, Equipe awayTeam) {
        List<ApiFootballMatchIncident> incidents;
        try {
            incidents = apiFootballInsightsService.refreshMatchIncidents(match, homeTeam, awayTeam);
        } catch (Exception e) {
            ApiFootballMatchDetails cached = apiFootballInsightsService.readCachedMatchDetails(match);
            incidents = cached == null || cached.incidents() == null ? List.of() : cached.incidents();
        }

        if (hasAlertworthyIncident(incidents)) {
            return incidents;
        }

        try {
            ApiFootballMatchDetails details = apiFootballInsightsService.loadMatchDetails(match, homeTeam, awayTeam);
            if (details != null && hasAlertworthyIncident(details.incidents())) {
                return details.incidents();
            }
        } catch (Exception e) {
            System.err.println("Live match detail incident fallback failed: " + e.getMessage());
        }
        return incidents == null ? List.of() : incidents;
    }

    private void emitScoreFallbackGoalNotifications(
            User currentUser,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballFixtureSnapshot snapshot,
            Integer previousHomeScore,
            Integer previousAwayScore
    ) {
        int homeScore = normalizedScore(snapshot == null ? null : snapshot.homeScore(), match == null ? null : match.getScoreEquipeDomicile());
        int awayScore = normalizedScore(snapshot == null ? null : snapshot.awayScore(), match == null ? null : match.getScoreEquipeExterieur());
        int previousHome = previousHomeScore == null ? 0 : Math.max(0, previousHomeScore);
        int previousAway = previousAwayScore == null ? 0 : Math.max(0, previousAwayScore);

        int firstHomeGoal = previousHome < homeScore ? previousHome + 1 : 1;
        for (int goalNumber = firstHomeGoal; goalNumber <= homeScore; goalNumber++) {
            emitNotification(buildScoreFallbackGoalNotification(currentUser, match, homeTeam, awayTeam, snapshot, true, goalNumber));
        }

        int firstAwayGoal = previousAway < awayScore ? previousAway + 1 : 1;
        for (int goalNumber = firstAwayGoal; goalNumber <= awayScore; goalNumber++) {
            emitNotification(buildScoreFallbackGoalNotification(currentUser, match, homeTeam, awayTeam, snapshot, false, goalNumber));
        }
    }

    private void emitIncidentNotifications(
            User currentUser,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballFixtureSnapshot snapshot,
            List<ApiFootballMatchIncident> currentIncidents
    ) {
        if (currentIncidents == null || currentIncidents.isEmpty()) {
            return;
        }

        for (ApiFootballMatchIncident incident : currentIncidents) {
            if (!isAlertworthyIncident(incident)) {
                continue;
            }

            String incidentKey = buildIncidentKey(incident);
            if (incidentKey == null) {
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

            try {
                NavbarNotificationCenter.requestRefreshAll();
            } catch (IllegalStateException ignored) {
                // The database notification is still valid when this runtime is exercised before JavaFX is ready.
            }

            Stage stage = ownerStage;
            if (stage == null || !stage.isShowing()) {
                return;
            }
            try {
                Platform.runLater(() -> {
                    MatchAlertPopupManager.getInstance().show(stage, created);
                    markPopupNotificationAsDelivered(created);
                });
            } catch (IllegalStateException ignored) {
                // No JavaFX toolkit is available yet; the stored notification will be shown in the bell center.
            }
        } catch (SQLException e) {
            System.err.println("Live notification could not be stored: " + e.getMessage());
        }
    }

    private void showQueuedUnreadPopups(Stage stage) {
        User currentUser = AuthSession.getCurrentUser();
        if (stage == null || currentUser == null || currentUser.getId() == null) {
            return;
        }

        scheduler.schedule(() -> {
            List<Notification> queued;
            try {
                ensureServices();
                LocalDateTime earliestCreatedAt = LocalDateTime.now().minusHours(DIRECT_MATCH_CATCH_UP_HOURS);
                queued = notificationService.getRecentByUser(currentUser.getId(), 50).stream()
                        .filter(notification -> notification != null && !notification.isRead())
                        .filter(notification -> notification.getId() != null && notification.getMatchId() != null)
                        .filter(notification -> notification.getCreatedAt() == null || !notification.getCreatedAt().isBefore(earliestCreatedAt))
                        .filter(notification -> shownQueuedPopupIds.add(notification.getId()))
                        .limit(MAX_QUEUED_POPUPS)
                        .sorted(Comparator
                                .comparing(Notification::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                .thenComparing(Notification::getId, Comparator.nullsLast(Integer::compareTo)))
                        .toList();
            } catch (Exception e) {
                System.err.println("Queued match popups could not be loaded: " + e.getMessage());
                return;
            }

            if (queued.isEmpty()) {
                return;
            }

            try {
                Platform.runLater(() -> {
                    if (!stage.isShowing()) {
                        return;
                    }
                    for (Notification notification : queued) {
                        MatchAlertPopupManager.getInstance().show(stage, notification);
                        markPopupNotificationAsDelivered(notification);
                    }
                });
            } catch (IllegalStateException ignored) {
                // The notifications remain in the bell center if JavaFX is not ready yet.
            }
        }, 900, TimeUnit.MILLISECONDS);
    }

    private void markPopupNotificationAsDelivered(Notification notification) {
        if (notification == null || notification.getId() == null || notification.isRead()) {
            return;
        }

        scheduler.execute(() -> {
            try {
                ensureServices();
                if (notificationService.markAsRead(notification.getId())) {
                    notification.setRead(true);
                    try {
                        NavbarNotificationCenter.requestRefreshAll();
                    } catch (IllegalStateException ignored) {
                        // Notification persistence matters more than an immediate JavaFX refresh.
                    }
                }
            } catch (SQLException e) {
                System.err.println("Popup notification delivery state could not be stored: " + e.getMessage());
            }
        });
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

    private Notification buildReminderNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam, ApiFootballFixtureSnapshot snapshot) {
        String fixtureLabel = buildFixtureLabel(homeTeam, awayTeam);
        String kickoffTime = snapshot != null && snapshot.kickoffAt() != null
                ? snapshot.kickoffAt().toLocalTime().format(KICKOFF_TIME_FORMATTER)
                : kickoffOf(match) == null ? "" : kickoffOf(match).toLocalTime().format(KICKOFF_TIME_FORMATTER);
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setType("Match Reminder");
        notification.setTitle("Reminder | " + fixtureLabel);
        notification.setMessage(fixtureLabel
                + (kickoffTime.isBlank() ? " starts soon." : " starts at " + kickoffTime + ".")
                + " You asked to be reminded one hour before kickoff.");
        notification.setMinuteLabel("T-" + MATCH_REMINDER_MINUTES);
        notification.setDedupeKey("match-reminder-" + MATCH_REMINDER_MINUTES + ":" + match.getId());
        notification.setAccentTone("warning");
        return notification;
    }

    private Notification buildHalfTimeNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam, ApiFootballFixtureSnapshot snapshot) {
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setType("Half Time");
        notification.setTitle("Half Time | " + buildResultLabel(snapshot, match));
        notification.setMessage("First half ended for " + buildFixtureLabel(homeTeam, awayTeam) + ".");
        notification.setMinuteLabel("HT");
        notification.setDedupeKey("match-half-time:" + match.getId());
        notification.setAccentTone("info");
        return notification;
    }

    private Notification buildSecondHalfNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam, ApiFootballFixtureSnapshot snapshot) {
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        notification.setType("Second Half");
        notification.setTitle("Second Half | " + buildFixtureLabel(homeTeam, awayTeam));
        notification.setMessage("Second half started for " + buildFixtureLabel(homeTeam, awayTeam) + ".");
        notification.setMinuteLabel(snapshot != null && snapshot.minuteLabel() != null ? snapshot.minuteLabel() : "2H");
        notification.setDedupeKey("match-second-half:" + match.getId());
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
            String scoreDedupeKey = buildGoalScoreDedupeKey(match, incident.homeSide(), scoreNumberForIncidentSide(incident));
            if (scoreDedupeKey != null) {
                notification.setDedupeKey(scoreDedupeKey);
            }
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

    private Notification buildScoreFallbackGoalNotification(
            User user,
            Matchs match,
            Equipe homeTeam,
            Equipe awayTeam,
            ApiFootballFixtureSnapshot snapshot,
            boolean homeSide,
            int goalNumber
    ) {
        Notification notification = buildBaseNotification(user, match, homeTeam, awayTeam);
        String scoringTeam = homeSide
                ? empty(homeTeam == null ? null : homeTeam.getNom(), "Home")
                : empty(awayTeam == null ? null : awayTeam.getNom(), "Away");
        notification.setType("Goal");
        notification.setTitle("Goal | " + buildResultLabel(snapshot, match));
        notification.setMessage(scoringTeam + " scored in " + buildFixtureLabel(homeTeam, awayTeam)
                + ". Score is now " + buildResultLabel(snapshot, match) + ".");
        notification.setMinuteLabel(snapshot == null || snapshot.minuteLabel() == null ? null : snapshot.minuteLabel());
        notification.setActorName(scoringTeam);
        notification.setDedupeKey(buildGoalScoreDedupeKey(match, homeSide, goalNumber));
        notification.setAccentTone("goal");
        return notification;
    }

    private Notification buildBaseNotification(User user, Matchs match, Equipe homeTeam, Equipe awayTeam) {
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setUserId(user.getId());
        notification.setMatchId(match == null ? null : match.getId());
        notification.setCompetitionCode(FootballDataCompetitions.normalizeCode(match == null ? null : match.getCompetitionCode()));
        notification.setHomeTeamName(homeTeam == null ? "Home" : homeTeam.getNom());
        notification.setAwayTeamName(awayTeam == null ? "Away" : awayTeam.getNom());
        notification.setHomeTeamLogo(homeTeam == null ? null : homeTeam.getImage());
        notification.setAwayTeamLogo(awayTeam == null ? null : awayTeam.getImage());
        return notification;
    }

    private boolean shouldEmitReminderAlert(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        if (now == null || snapshot != null && snapshot.isFinished()) {
            return false;
        }
        LocalDateTime kickoff = snapshot != null && snapshot.kickoffAt() != null ? snapshot.kickoffAt() : kickoffOf(match);
        if (kickoff == null) {
            return false;
        }
        LocalDateTime reminderTime = kickoff.minusMinutes(MATCH_REMINDER_MINUTES);
        return !now.isBefore(reminderTime) && now.isBefore(kickoff.plusMinutes(10));
    }

    private boolean shouldEmitKickoffAlert(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        LocalDateTime kickoff = snapshot != null && snapshot.kickoffAt() != null ? snapshot.kickoffAt() : kickoffOf(match);
        if (kickoff == null) {
            return false;
        }
        if (snapshot != null && snapshot.isLive()) {
            return true;
        }
        if (snapshot == null && isLiveStatus(match == null ? null : match.getStatut())) {
            return true;
        }
        int catchUpHours = directMatchFavorite ? DIRECT_MATCH_CATCH_UP_HOURS : 4;
        return !now.isBefore(kickoff) && now.isBefore(kickoff.plusHours(catchUpHours));
    }

    private boolean shouldEmitHalfTimeAlert(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        if (hasReachedHalfTime(snapshot)) {
            return isWithinMatchCatchUp(match, snapshot, now, directMatchFavorite);
        }
        return snapshot == null
                && hasLikelyReachedMatchMinute(match, now, 45)
                && isWithinMatchCatchUp(match, null, now, directMatchFavorite);
    }

    private boolean shouldEmitSecondHalfAlert(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        if (hasStartedSecondHalf(snapshot)) {
            return isWithinMatchCatchUp(match, snapshot, now, directMatchFavorite);
        }
        return snapshot == null
                && hasLikelyReachedMatchMinute(match, now, 46)
                && isWithinMatchCatchUp(match, null, now, directMatchFavorite);
    }

    private boolean shouldEmitFinalAlert(ApiFootballFixtureSnapshot snapshot, LocalDateTime now, Matchs match, boolean directMatchFavorite) {
        if (snapshot != null && snapshot.isFinished()) {
            return isWithinMatchCatchUp(match, snapshot, now, directMatchFavorite);
        }

        if (!isFinishedStatus(match == null ? null : match.getStatut())) {
            return false;
        }

        return isWithinMatchCatchUp(match, null, now, directMatchFavorite);
    }

    private boolean isWithinMatchCatchUp(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        if (now == null) {
            return false;
        }
        LocalDateTime kickoff = snapshot == null || snapshot.kickoffAt() == null ? kickoffOf(match) : snapshot.kickoffAt();
        int catchUpHours = directMatchFavorite ? DIRECT_MATCH_CATCH_UP_HOURS : 6;
        if (kickoff == null) {
            return true;
        }
        return !now.isAfter(kickoff.plusHours(catchUpHours));
    }

    private boolean shouldFetchLiveIncidents(Matchs match, ApiFootballFixtureSnapshot snapshot, LocalDateTime now, boolean directMatchFavorite) {
        int catchUpHours = directMatchFavorite ? DIRECT_MATCH_CATCH_UP_HOURS : 4;
        if (snapshot != null && snapshot.isLive()) {
            return true;
        }
        if (snapshot == null && isLiveStatus(match == null ? null : match.getStatut())) {
            return true;
        }
        if (snapshot != null && snapshot.isFinished()) {
            LocalDateTime kickoff = snapshot.kickoffAt() == null ? kickoffOf(match) : snapshot.kickoffAt();
            return kickoff != null && now.isBefore(kickoff.plusHours(catchUpHours));
        }
        if (snapshot == null && isFinishedStatus(match == null ? null : match.getStatut())) {
            LocalDateTime kickoff = kickoffOf(match);
            return kickoff != null && now.isBefore(kickoff.plusHours(catchUpHours));
        }

        LocalDateTime kickoff = kickoffOf(match);
        return kickoff != null && !now.isBefore(kickoff.minusMinutes(1)) && now.isBefore(kickoff.plusHours(catchUpHours));
    }

    private boolean matchesFollowTargets(Matchs match, Set<Integer> followedMatchIds, Set<Integer> followedTeamIds, Set<String> followedCompetitions) {
        if (match == null) {
            return false;
        }
        if (isDirectMatchFavorite(match, followedMatchIds)) {
            return true;
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

    private boolean isMonitoringCandidate(Matchs match, LocalDateTime now, boolean directMatchFavorite) {
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

        int catchUpHours = directMatchFavorite ? DIRECT_MATCH_CATCH_UP_HOURS : 4;
        int futureMinutes = MATCH_REMINDER_MINUTES + 5;
        if (isFinishedStatus(match.getStatut())) {
            return now.isBefore(kickoff.plusHours(catchUpHours));
        }

        return !kickoff.isBefore(now.minusHours(catchUpHours)) && !kickoff.isAfter(now.plusMinutes(futureMinutes));
    }

    private boolean isDirectMatchFavorite(Matchs match, Set<Integer> followedMatchIds) {
        return match != null && match.getId() != null && followedMatchIds != null && followedMatchIds.contains(match.getId());
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
        String normalized = safeStatus(status);
        return normalized.contains("fini")
                || normalized.contains("finished")
                || normalized.contains("full time")
                || normalized.contains("termine")
                || normalized.contains("ended")
                || normalized.contains("complete");
    }

    private boolean hasReachedHalfTime(ApiFootballFixtureSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        String code = safeStatus(snapshot.statusShort()).toUpperCase(Locale.ROOT);
        if (Set.of("HT", "2H", "ET", "BT", "P", "FT", "AET", "PEN").contains(code)) {
            return true;
        }
        String label = safeStatus(snapshot.effectiveStatusLabel());
        return label.contains("mi-temps")
                || label.contains("mi temps")
                || label.contains("half time")
                || label.contains("halftime")
                || label.contains("2e mi")
                || label.contains("second half")
                || snapshot.isFinished();
    }

    private boolean hasStartedSecondHalf(ApiFootballFixtureSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        String code = safeStatus(snapshot.statusShort()).toUpperCase(Locale.ROOT);
        if (Set.of("2H", "ET", "BT", "P", "FT", "AET", "PEN").contains(code)) {
            return true;
        }
        String label = safeStatus(snapshot.effectiveStatusLabel());
        return label.contains("2e mi")
                || label.contains("deuxieme mi")
                || label.contains("second half")
                || label.contains("prolong")
                || label.contains("extra time")
                || label.contains("tirs au but")
                || label.contains("penalties")
                || snapshot.isFinished();
    }

    private boolean hasLikelyReachedMatchMinute(Matchs match, LocalDateTime now, int minute) {
        if (match == null || now == null || (!isLiveStatus(match.getStatut()) && !isFinishedStatus(match.getStatut()))) {
            return false;
        }
        LocalDateTime kickoff = kickoffOf(match);
        return kickoff != null && !now.isBefore(kickoff.plusMinutes(minute));
    }

    private boolean isAlertworthyIncident(ApiFootballMatchIncident incident) {
        return incident != null && (incident.isGoal() || incident.isCard() || incident.isSubstitution());
    }

    private boolean hasAlertworthyIncident(List<ApiFootballMatchIncident> incidents) {
        return incidents != null && incidents.stream().anyMatch(this::isAlertworthyIncident);
    }

    private boolean hasGoalIncident(List<ApiFootballMatchIncident> incidents) {
        return incidents != null && incidents.stream().anyMatch(ApiFootballMatchIncident::isGoal);
    }

    private int normalizedScore(Integer primaryScore, Integer fallbackScore) {
        Integer score = primaryScore == null ? fallbackScore : primaryScore;
        return score == null ? 0 : Math.max(0, score);
    }

    private Integer scoreNumberForIncidentSide(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return null;
        }
        return incident.homeSide() ? incident.homeScore() : incident.awayScore();
    }

    private String buildGoalScoreDedupeKey(Matchs match, boolean homeSide, Integer goalNumber) {
        if (match == null || match.getId() == null || goalNumber == null || goalNumber <= 0) {
            return null;
        }
        return "match-goal-score:" + match.getId() + ":" + (homeSide ? "H" : "A") + ":" + goalNumber;
    }

    private long kickoffDistanceMinutes(Matchs match, LocalDateTime now) {
        LocalDateTime kickoff = kickoffOf(match);
        if (kickoff == null || now == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(Duration.between(now, kickoff).toMinutes());
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
