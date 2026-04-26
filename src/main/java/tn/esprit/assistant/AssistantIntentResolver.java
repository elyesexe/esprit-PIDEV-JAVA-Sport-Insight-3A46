package tn.esprit.assistant;

import tn.esprit.Controller.MatchDetailController;
import tn.esprit.Controller.MatchListController;

public final class AssistantIntentResolver {
    public AssistantIntent resolve(
            String normalized,
            AssistantService.Context context,
            AssistantConversationMemory.MemorySnapshot memory
    ) {
        if (normalized == null || normalized.isBlank()) {
            return AssistantIntent.unknown();
        }

        AssistantConversationMemory.MemorySnapshot safeMemory = memory == null
                ? new AssistantConversationMemory.MemorySnapshot(java.util.Optional.empty(), java.util.Optional.empty(), AssistantIntentType.UNKNOWN)
                : memory;

        if (safeMemory.pendingConfirmation().isPresent()) {
            if (looksLikeConfirmation(normalized)) {
                return new AssistantIntent(
                        AssistantIntentType.CONFIRMATION,
                        AssistantIntentTarget.CURRENT_CONTEXT,
                        AssistantIntentScope.DEFAULT,
                        safeMemory.pendingConfirmation().get().originalRequest(),
                        false,
                        AssistantResponsePolicy.directFact()
                );
            }
            if (looksLikeCancellation(normalized)) {
                return new AssistantIntent(
                        AssistantIntentType.CANCELLATION,
                        AssistantIntentTarget.CURRENT_CONTEXT,
                        AssistantIntentScope.DEFAULT,
                        "",
                        false,
                        AssistantResponsePolicy.directFact()
                );
            }
        }

        if (looksLikeRiskyAction(normalized)) {
            return new AssistantIntent(
                    AssistantIntentType.RISKY_ACTION,
                    resolveRiskTarget(context, safeMemory),
                    AssistantIntentScope.DEFAULT,
                    normalized,
                    false,
                    AssistantResponsePolicy.riskyAction()
            );
        }

        String safeAction = resolveSafeAction(normalized, context);
        if (!safeAction.isBlank()) {
            return new AssistantIntent(
                    AssistantIntentType.SAFE_ACTION,
                    AssistantIntentTarget.CURRENT_CONTEXT,
                    AssistantIntentScope.DEFAULT,
                    safeAction,
                    false,
                    AssistantResponsePolicy.directFact()
            );
        }

        if (containsAny(normalized, "where am i", "current screen", "this screen", "this page")) {
            return new AssistantIntent(
                    AssistantIntentType.CURRENT_SCREEN,
                    AssistantIntentTarget.CURRENT_SCREEN,
                    AssistantIntentScope.DEFAULT,
                    "",
                    false,
                    AssistantResponsePolicy.directFact()
            );
        }

        if (containsAny(normalized, "what can i do here", "help here", "what can i do", "screen help")) {
            return new AssistantIntent(
                    AssistantIntentType.SCREEN_HELP,
                    AssistantIntentTarget.CURRENT_SCREEN,
                    AssistantIntentScope.DEFAULT,
                    "",
                    false,
                    AssistantResponsePolicy.directWithDetail()
            );
        }

        boolean followUp = looksLikeFollowUp(normalized);

        if (context != null && context.controller() instanceof AssistantPlayerProfileProvider) {
            OptionalPlayerIntent playerIntent = resolvePlayerIntent(normalized, followUp);
            if (playerIntent.type() != AssistantIntentType.UNKNOWN) {
                return playerIntent(playerIntent.type(), playerIntent.scope(), normalized, followUp);
            }
        }

        boolean hasCurrentMatch = (context != null && context.controller() instanceof tn.esprit.Controller.MatchDetailController)
                || safeMemory.hasRecentMatch();
        if (!hasCurrentMatch) {
            return AssistantIntent.unknown();
        }

        if (looksLikeAssistQuestion(normalized) || (followUp && containsAny(normalized, "assist", "assists", "assisted"))) {
            return matchIntent(AssistantIntentType.MATCH_ASSISTS, followUp);
        }
        if (looksLikeScorerQuestion(normalized) || (followUp && safeMemory.lastIntentType() == AssistantIntentType.MATCH_SCORERS)) {
            return matchIntent(AssistantIntentType.MATCH_SCORERS, followUp);
        }
        if (looksLikeCardQuestion(normalized) || (followUp && containsAny(normalized, "card", "cards", "booked", "booking"))) {
            return matchIntent(AssistantIntentType.MATCH_CARDS, followUp);
        }
        if (looksLikeWinnerQuestion(normalized)) {
            return matchIntent(AssistantIntentType.MATCH_WINNER, followUp);
        }
        if (looksLikeScoreQuestion(normalized)) {
            return new AssistantIntent(
                    AssistantIntentType.MATCH_SCORE,
                    AssistantIntentTarget.CURRENT_MATCH,
                    AssistantIntentScope.SUMMARY,
                    "",
                    followUp,
                    AssistantResponsePolicy.directWithDetail()
            );
        }
        if (looksLikeMvpQuestion(normalized)) {
            return matchIntent(AssistantIntentType.MATCH_MVP, followUp);
        }
        if (looksLikeStatisticQuestion(normalized)) {
            return new AssistantIntent(
                    AssistantIntentType.MATCH_STATISTICS,
                    AssistantIntentTarget.CURRENT_MATCH,
                    AssistantIntentScope.SUMMARY,
                    normalized,
                    followUp,
                    AssistantResponsePolicy.directWithDetail()
            );
        }
        if (looksLikeLineupQuestion(normalized)) {
            return new AssistantIntent(
                    AssistantIntentType.MATCH_LINEUP,
                    AssistantIntentTarget.CURRENT_MATCH,
                    AssistantIntentScope.ALL_PLAYERS,
                    normalized,
                    followUp,
                    AssistantResponsePolicy.directWithDetail()
            );
        }
        if (containsAny(normalized, "summary", "resume", "timeline", "overview")) {
            return matchIntent(AssistantIntentType.MATCH_SUMMARY, followUp);
        }

        return AssistantIntent.unknown();
    }

    private AssistantIntent playerIntent(
            AssistantIntentType type,
            AssistantIntentScope scope,
            String subject,
            boolean followUp
    ) {
        return new AssistantIntent(
                type,
                AssistantIntentTarget.CURRENT_PLAYER,
                scope,
                subject == null ? "" : subject,
                followUp,
                scope == AssistantIntentScope.SUMMARY
                        ? AssistantResponsePolicy.directWithDetail()
                        : AssistantResponsePolicy.directFact()
        );
    }

    private OptionalPlayerIntent resolvePlayerIntent(String normalized, boolean followUp) {
        if (looksLikePlayerAgeQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_AGE, AssistantIntentScope.DEFAULT);
        }
        if (looksLikePlayerNationalityQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_NATIONALITY, AssistantIntentScope.DEFAULT);
        }
        if (looksLikePlayerPositionQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_POSITION, AssistantIntentScope.DEFAULT);
        }
        if (looksLikePlayerRecentFormQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_RECENT_FORM, AssistantIntentScope.SUMMARY);
        }
        if (looksLikePlayerSeasonStatsQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_SEASON_STATS, AssistantIntentScope.SUMMARY);
        }
        if (looksLikePlayerClubQuestion(normalized)) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_CLUB, AssistantIntentScope.DEFAULT);
        }
        if (looksLikePlayerProfileSummaryQuestion(normalized)
                || (followUp && containsAny(normalized, "summary", "overview", "profile"))) {
            return new OptionalPlayerIntent(AssistantIntentType.PLAYER_PROFILE_SUMMARY, AssistantIntentScope.SUMMARY);
        }
        return new OptionalPlayerIntent(AssistantIntentType.UNKNOWN, AssistantIntentScope.DEFAULT);
    }

    private AssistantIntent matchIntent(AssistantIntentType type, boolean followUp) {
        return new AssistantIntent(
                type,
                AssistantIntentTarget.CURRENT_MATCH,
                AssistantIntentScope.ALL_PLAYERS,
                "",
                followUp,
                AssistantResponsePolicy.directFact()
        );
    }

    private AssistantIntentTarget resolveRiskTarget(
            AssistantService.Context context,
            AssistantConversationMemory.MemorySnapshot memory
    ) {
        if (context != null && context.controller() instanceof tn.esprit.Controller.MatchDetailController) {
            return AssistantIntentTarget.CURRENT_MATCH;
        }
        if (memory != null && memory.hasRecentMatch()) {
            return AssistantIntentTarget.CURRENT_MATCH;
        }
        return AssistantIntentTarget.CURRENT_CONTEXT;
    }

    private boolean looksLikeFollowUp(String normalized) {
        return normalized.startsWith("and ")
                || normalized.startsWith("what about")
                || normalized.startsWith("how about")
                || normalized.startsWith("what else")
                || normalized.equals("cards")
                || normalized.equals("assists")
                || normalized.equals("scorers")
                || normalized.equals("winner");
    }

    private boolean looksLikeAssistQuestion(String normalized) {
        return containsAny(
                normalized,
                "who assisted",
                "who got the assist",
                "who made the assist",
                "assist",
                "assists",
                "assisted"
        );
    }

    private boolean looksLikeWinnerQuestion(String normalized) {
        return containsAny(
                normalized,
                "who won",
                "winner",
                "which team won",
                "did they win",
                "did we win"
        ) || normalized.matches(".*\\bwon (this|that|the|current|last|latest|next) (match|game|fixture)\\b.*");
    }

    private boolean looksLikeScorerQuestion(String normalized) {
        return containsAny(
                normalized,
                "who scored",
                "scorer",
                "scorers",
                "goalscorer",
                "goal scorers",
                "goal events",
                "which players scored",
                "name the scorers"
        );
    }

    private boolean looksLikeScoreQuestion(String normalized) {
        if (looksLikeScorerQuestion(normalized) || looksLikeWinnerQuestion(normalized)) {
            return false;
        }
        return containsAny(normalized, "score", "result", "who is winning", "what s the score", "what is the score");
    }

    private boolean looksLikeCardQuestion(String normalized) {
        return containsAny(normalized, "yellow card", "red card", "cards", "booked", "booking", "sent off");
    }

    private boolean looksLikeStatisticQuestion(String normalized) {
        return containsAny(
                normalized,
                "stat", "stats", "statistics", "possession", "shots", "passes", "corners",
                "fouls", "offsides", "xg", "expected goals", "saves", "duels", "accuracy"
        );
    }

    private boolean looksLikeLineupQuestion(String normalized) {
        return containsAny(
                normalized,
                "starting 11",
                "starting eleven",
                "starting xi",
                "who is playing",
                "who s playing",
                "who plays",
                "who is starting",
                "who s starting",
                "starting lineup",
                "starting line up",
                "which players",
                "who is in the lineup",
                "who s in the lineup"
        );
    }

    private boolean looksLikeMvpQuestion(String normalized) {
        return containsAny(normalized, "mvp", "man of the match", "player of the match", "best player", "star player");
    }

    private boolean looksLikePlayerAgeQuestion(String normalized) {
        return containsToken(normalized, "age")
                || containsAny(normalized, "how old", "old is he", "old is she", "old is this player", "birth date", "birthday", "date of birth");
    }

    private boolean looksLikePlayerNationalityQuestion(String normalized) {
        return containsAny(normalized, "nationality", "nationalite", "country", "nation", "where is he from", "where is she from", "where is this player from");
    }

    private boolean looksLikePlayerClubQuestion(String normalized) {
        return containsAny(
                normalized,
                "club",
                "team",
                "equipe",
                "plays for",
                "play for",
                "which side",
                "what side",
                "who does he play for",
                "who does she play for",
                "who does this player play for"
        );
    }

    private boolean looksLikePlayerPositionQuestion(String normalized) {
        return containsAny(normalized, "position", "poste", "role", "where does he play", "where does she play", "where does this player play");
    }

    private boolean looksLikePlayerSeasonStatsQuestion(String normalized) {
        return containsAny(
                normalized,
                "season stat",
                "season stats",
                "statistics",
                "stats",
                "appearances",
                "matches played",
                "games played",
                "goals",
                "assists",
                "minutes",
                "yellow cards",
                "red cards",
                "cards"
        );
    }

    private boolean looksLikePlayerRecentFormQuestion(String normalized) {
        return containsToken(normalized, "form")
                || containsAny(normalized, "recent form", "current form", "last form", "form lately", "last matches", "last games", "recent matches", "recent games");
    }

    private boolean looksLikePlayerProfileSummaryQuestion(String normalized) {
        return containsAny(normalized, "tell me about this player", "who is this player", "player summary", "profile summary", "overview", "summarize this player");
    }

    private boolean looksLikeRiskyAction(String normalized) {
        return containsAny(
                normalized,
                "delete",
                "remove",
                "erase",
                "drop",
                "reset",
                "overwrite",
                "clear all"
        );
    }

    private String resolveSafeAction(String normalized, AssistantService.Context context) {
        if (context == null) {
            return "";
        }

        Object controller = context.controller();
        if (controller instanceof MatchDetailController) {
            return resolveMatchDetailAction(normalized);
        }
        if (controller instanceof MatchListController) {
            return resolveMatchListAction(normalized);
        }
        return "";
    }

    private String resolveMatchDetailAction(String normalized) {
        if (looksLikeActionRequest(normalized, "lineup", "lineups", "composition", "formation")) {
            return "open_lineups";
        }
        if (looksLikeActionRequest(normalized, "stat", "stats", "statistics")) {
            return "open_stats";
        }
        if (looksLikeActionRequest(normalized, "summary", "resume", "timeline", "overview")) {
            return "open_summary";
        }
        if (containsAny(normalized, "back to matches", "back to match list", "return to matches", "open competition matches")) {
            return "open_match_list";
        }
        return "";
    }

    private String resolveMatchListAction(String normalized) {
        if (containsAny(normalized, "clear filter", "clear filters", "reset filter", "reset filters", "clear search", "reset search", "show all matches", "show all fixtures")) {
            return "clear_match_search";
        }
        return "";
    }

    private boolean looksLikeActionRequest(String normalized, String... topics) {
        return containsAny(normalized, topics)
                && containsAny(normalized, "open", "show", "take me", "bring me", "switch", "view", "tab");
    }

    private boolean looksLikeConfirmation(String normalized) {
        return normalized.equals("confirm")
                || normalized.equals("yes")
                || normalized.equals("yes confirm")
                || normalized.equals("do it")
                || normalized.equals("go ahead");
    }

    private boolean looksLikeCancellation(String normalized) {
        return normalized.equals("cancel")
                || normalized.equals("stop")
                || normalized.equals("never mind")
                || normalized.equals("don t")
                || normalized.equals("do not");
    }

    private boolean containsAny(String source, String... terms) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsToken(String source, String token) {
        if (source == null || source.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String part : source.split("\\s+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private record OptionalPlayerIntent(AssistantIntentType type, AssistantIntentScope scope) {
    }
}
