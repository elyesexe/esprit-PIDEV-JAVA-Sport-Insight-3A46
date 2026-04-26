package tn.esprit.assistant;

import tn.esprit.Controller.MatchDetailController;
import tn.esprit.entities.Matchs;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class AssistantConversationMemory {
    private static final Duration MATCH_MEMORY_WINDOW = Duration.ofMinutes(4);
    private static final Duration CONFIRMATION_WINDOW = Duration.ofMinutes(2);

    private volatile MatchSnapshot recentMatch;
    private volatile PendingConfirmation pendingConfirmation;
    private volatile AssistantIntentType lastIntentType = AssistantIntentType.UNKNOWN;

    public void observeContext(AssistantService.Context context) {
        if (context == null || !(context.controller() instanceof MatchDetailController controller)) {
            return;
        }

        MatchSnapshot snapshot = MatchSnapshot.fromController(controller);
        if (snapshot != null) {
            recentMatch = snapshot;
        }
    }

    public void rememberIntent(AssistantIntent intent, AssistantService.Context context) {
        if (intent == null) {
            return;
        }
        observeContext(context);
        lastIntentType = intent.type();
        if (intent.type() != AssistantIntentType.CONFIRMATION && intent.type() != AssistantIntentType.CANCELLATION) {
            pendingConfirmation = null;
        }
    }

    public void rememberPendingConfirmation(AssistantIntent intent, String originalRequest) {
        if (intent == null) {
            return;
        }
        pendingConfirmation = new PendingConfirmation(intent, emptyToFallback(originalRequest, "that action"), Instant.now());
    }

    public void clearPendingConfirmation() {
        pendingConfirmation = null;
    }

    public MemorySnapshot snapshot() {
        MatchSnapshot safeMatch = isFresh(recentMatch == null ? null : recentMatch.capturedAt(), MATCH_MEMORY_WINDOW)
                ? recentMatch
                : null;
        PendingConfirmation safePending = isFresh(pendingConfirmation == null ? null : pendingConfirmation.createdAt(), CONFIRMATION_WINDOW)
                ? pendingConfirmation
                : null;
        return new MemorySnapshot(Optional.ofNullable(safeMatch), Optional.ofNullable(safePending), lastIntentType);
    }

    void rememberMatchSnapshot(MatchSnapshot snapshot, AssistantIntentType intentType) {
        recentMatch = snapshot;
        lastIntentType = intentType == null ? AssistantIntentType.UNKNOWN : intentType;
    }

    private static boolean isFresh(Instant value, Duration window) {
        return value != null && value.isAfter(Instant.now().minus(window));
    }

    private static String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record MemorySnapshot(
            Optional<MatchSnapshot> recentMatch,
            Optional<PendingConfirmation> pendingConfirmation,
            AssistantIntentType lastIntentType
    ) {
        public boolean hasRecentMatch() {
            return recentMatch != null && recentMatch.isPresent();
        }
    }

    public record PendingConfirmation(
            AssistantIntent intent,
            String originalRequest,
            Instant createdAt
    ) {
    }

    public record MatchSnapshot(
            String matchLabel,
            String homeTeam,
            String awayTeam,
            Integer homeScore,
            Integer awayScore,
            String scoreLabel,
            String statusLabel,
            String competitionLabel,
            List<String> scorers,
            List<String> assists,
            List<String> cards,
            Instant capturedAt
    ) {
        static MatchSnapshot fromController(MatchDetailController controller) {
            if (controller == null || controller.getCurrentMatch() == null) {
                return null;
            }

            Matchs match = controller.getCurrentMatch();
            return new MatchSnapshot(
                    controller.getCurrentMatchLabel(),
                    controller.getCurrentHomeTeamName(),
                    controller.getCurrentAwayTeamName(),
                    match.getScoreEquipeDomicile(),
                    match.getScoreEquipeExterieur(),
                    controller.getCurrentScoreLabel(),
                    controller.getCurrentStatusLabel(),
                    controller.getCurrentCompetitionLabel(),
                    controller.getCurrentGoalScorerSummaries(),
                    controller.getCurrentAssistSummaries(),
                    controller.getCurrentCardHighlights(),
                    Instant.now()
            );
        }
    }
}
