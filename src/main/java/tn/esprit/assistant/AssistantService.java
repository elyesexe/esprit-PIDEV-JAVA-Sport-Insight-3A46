package tn.esprit.assistant;

import tn.esprit.Controller.MatchDetailController;
import tn.esprit.Controller.MatchListController;
import tn.esprit.Controller.LeagueTableController;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.security.AuthSession;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.ApiFootballStatisticRow;
import tn.esprit.services.football.FootballDataCompetitions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public final class AssistantService {
    private static final String PREF_SPEAK_REPLIES = "assistant_speak_replies";
    private static final String PREF_MODEL = "assistant_model";
    private static final String FAST_MODEL = "llama3.2:1b";
    private static final String LEGACY_MODEL = FAST_MODEL;
    private static final String DEFAULT_MODEL = "qwen2.5:3b";
    private static final int FAST_PROMPT_CHAR_LIMIT = 120;
    private static final int FAST_PROMPT_TOKEN_LIMIT = 18;
    private static final int MIN_TEAM_MATCH_SCORE = 45;
    private static final int PREDICTION_RECENT_MATCH_LIMIT = 6;
    private static final List<String> MATCH_DELIMITERS = List.of(" versus ", " against ", " contre ", " face ", " vs ", " v ");
    private static final Map<String, String> COMPETITION_ALIASES = createCompetitionAliases();
    private static final AssistantService INSTANCE = new AssistantService();

    private final Preferences preferences = Preferences.userNodeForPackage(AssistantService.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(daemonFactory("assistant-worker"));
    private final List<AssistantMessage> history = new CopyOnWriteArrayList<>();
    private final OllamaClient ollamaClient = new OllamaClient();
    private final VoiceOutputService voiceOutputService = new VoiceOutputService();
    private final VoiceInputService voiceInputService = new VoiceInputService();
    private final String knowledgeBase;

    private volatile boolean panelOpen;
    private volatile boolean speakReplies;
    private volatile String preferredModel;

    private AssistantService() {
        this.knowledgeBase = loadKnowledgeBase();
        this.speakReplies = preferences.getBoolean(PREF_SPEAK_REPLIES, true);
        this.preferredModel = resolvePreferredModel(preferences.get(PREF_MODEL, DEFAULT_MODEL));
        history.add(new AssistantMessage(
                AssistantMessage.Role.ASSISTANT,
                "I'm Jarvis, your Sport Insight assistant. I can open modules, jump into competition pages, open exact match details, predict upcoming matches from local form data, explain the current screen, and talk back with local voice models.",
                Instant.now()
        ));
    }

    public static AssistantService getInstance() {
        return INSTANCE;
    }

    public List<AssistantMessage> historySnapshot() {
        return List.copyOf(history);
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void setPanelOpen(boolean panelOpen) {
        this.panelOpen = panelOpen;
    }

    public boolean isSpeakRepliesEnabled() {
        return speakReplies;
    }

    public void setSpeakRepliesEnabled(boolean speakReplies) {
        this.speakReplies = speakReplies;
        preferences.putBoolean(PREF_SPEAK_REPLIES, speakReplies);
        if (!speakReplies) {
            voiceOutputService.stop();
        }
    }

    public String preferredModel() {
        return preferredModel;
    }

    public String modelRoutingLabel() {
        return "Auto";
    }

    public void setPreferredModel(String preferredModel) {
        if (preferredModel == null || preferredModel.isBlank()) {
            return;
        }
        this.preferredModel = resolvePreferredModel(preferredModel.trim());
        preferences.put(PREF_MODEL, this.preferredModel);
    }

    public String voiceLabel() {
        return voiceOutputService.voiceLabel();
    }

    public boolean isVoiceRecording() {
        return voiceInputService.isRecording();
    }

    public void startVoiceRecording() throws Exception {
        voiceInputService.prepareRealtimeRecognitionAsync();
        voiceOutputService.prepareFastReplyAsync();
        CompletableFuture.runAsync(() -> ollamaClient.warmModel(FAST_MODEL, OllamaClient.ChatProfile.REALTIME));
        voiceInputService.startRecording();
    }

    public CompletableFuture<String> stopVoiceRecording(java.util.function.Consumer<String> statusConsumer) {
        return voiceInputService.stopRecordingAndTranscribe(statusConsumer);
    }

    public String runtimeStatus(Context context) {
        AssistantScreenCatalog.ScreenMeta screenMeta = AssistantScreenCatalog.resolve(context.fxmlPath());
        String modelStatus = buildModelRuntimeStatus();
        return screenMeta.title() + " active. " + modelStatus + " Mic input uses low-latency Vosk with Whisper refinement fallback. " + voiceOutputService.statusSummary();
    }

    public CompletableFuture<Reply> submit(String rawPrompt, Context context) {
        return submit(rawPrompt, context, InteractionMode.TEXT);
    }

    public CompletableFuture<Reply> submitVoice(String rawPrompt, Context context) {
        return submit(rawPrompt, context, InteractionMode.VOICE);
    }

    private CompletableFuture<Reply> submit(String rawPrompt, Context context, InteractionMode mode) {
        String prompt = rawPrompt == null ? "" : rawPrompt.trim();
        if (prompt.isBlank()) {
            return CompletableFuture.completedFuture(new Reply(
                    "Ask me something about Sport Insight, for example: open teams, open Champions League matches, or open Bayern vs Real Madrid details.",
                    null,
                    true
            ));
        }

        remember(AssistantMessage.Role.USER, prompt);
        return CompletableFuture.supplyAsync(() -> buildReply(prompt, context, mode), executor)
                .thenApply(reply -> {
                    remember(AssistantMessage.Role.ASSISTANT, reply.text());
                    if (speakReplies) {
                        voiceOutputService.speakAsync(reply.text());
                    }
                    return reply;
                });
    }

    private Reply buildReply(String prompt, Context context, InteractionMode mode) {
        String normalized = normalize(prompt);
        AssistantScreenCatalog.ScreenMeta currentScreen = AssistantScreenCatalog.resolve(context.fxmlPath());

        Optional<Reply> localReply = tryHandleLocally(normalized, context, currentScreen);
        if (localReply.isPresent()) {
            return localReply.get();
        }

        List<AssistantMessage> snapshot = historySnapshot();
        String selectedModel = resolveModelForPrompt(prompt, normalized, context, mode);
        String systemPrompt = buildSystemPrompt(context, currentScreen, mode);
        try {
            String modelReply = ollamaClient.chat(selectedModel, systemPrompt, snapshot, resolveChatProfile(mode, prompt, normalized));
            if (modelReply != null && !modelReply.isBlank()) {
                return new Reply(modelReply, null, false);
            }
        } catch (Exception ignored) {
            // Fall through to the local fallback.
        }

        return new Reply(buildOfflineFallback(currentScreen, context, mode), null, true);
    }

    private Optional<Reply> tryHandleLocally(String normalized, Context context, AssistantScreenCatalog.ScreenMeta currentScreen) {
        if (containsAny(normalized, "where am i", "current screen", "this screen", "this page")) {
            return Optional.of(new Reply(describeCurrentScreen(currentScreen), null, true));
        }

        if (containsAny(normalized, "what can i do here", "help here", "what can i do", "screen help")) {
            return Optional.of(new Reply(describeCurrentActions(currentScreen), null, true));
        }

        Optional<Reply> controllerAwareReply = tryHandleCurrentPageActions(normalized, context);
        if (controllerAwareReply.isPresent()) {
            return controllerAwareReply;
        }

        Optional<Reply> specificMatchReply = tryHandleSpecificMatchNavigation(normalized, context);
        if (specificMatchReply.isPresent()) {
            return specificMatchReply;
        }

        Optional<Reply> competitionStandingsReply = tryHandleCompetitionStandingsNavigation(normalized, context);
        if (competitionStandingsReply.isPresent()) {
            return competitionStandingsReply;
        }

        Optional<Reply> competitionReply = tryHandleCompetitionNavigation(normalized, context);
        if (competitionReply.isPresent()) {
            return competitionReply;
        }

        Optional<Reply> teamMatchReply = tryHandleSingleTeamMatchNavigation(normalized, context);
        if (teamMatchReply.isPresent()) {
            return teamMatchReply;
        }

        if (containsAny(normalized, "who are you", "what can you do", "what do you do", "introduce yourself")) {
            return Optional.of(new Reply(
                    "I'm Jarvis, your local Sport Insight copilot. I can explain screens, open modules, jump to competition pages, open exact match details, predict upcoming fixtures, and talk with local voice models.",
                    null,
                    true
            ));
        }

        if (containsAny(normalized, "module", "modules", "feature", "features", "section", "sections")) {
            return Optional.of(new Reply(listModules(), null, true));
        }

        if (containsAny(normalized, "voice", "mic", "microphone", "speech")) {
            return Optional.of(new Reply(
                    "Voice chat is fully local. The mic now starts with a low-latency Vosk pass and only refines with Whisper when needed. Spoken replies use a fast local speech path for short answers and Piper for longer answers. Everything stays local with no paid API.",
                    null,
                    true
            ));
        }

        if (containsAny(normalized, "ollama", "model", "local ai")) {
            OllamaClient.Status status = ollamaClient.status(preferredModel);
            String note = status.reachable()
                    ? (status.selectedModel() == null
                        ? "Ollama is reachable, but no model is installed yet. Pull " + preferredModel + " to unlock richer answers."
                        : "Ollama is online and the assistant can use " + status.selectedModel() + ".")
                    : "Ollama is not reachable yet. Install it locally, then pull " + preferredModel + ".";
            return Optional.of(new Reply(note, null, true));
        }

        Optional<AssistantNavigationTarget> requestedTarget = AssistantNavigationTarget.findMatch(normalized);
        if (requestedTarget.isPresent() && looksLikeNavigationRequest(normalized)) {
            return Optional.of(handleNavigation(requestedTarget.get(), context));
        }

        if (requestedTarget.isPresent() && looksLikeExplanationRequest(normalized)) {
            return Optional.of(new Reply(describeTarget(requestedTarget.get()), null, true));
        }

        return Optional.empty();
    }

    private Optional<Reply> tryHandleCurrentPageActions(String normalized, Context context) {
        Object controller = context.controller();
        if (controller instanceof MatchDetailController matchDetailController) {
            return tryHandleMatchDetailPageActions(normalized, matchDetailController);
        }
        if (controller instanceof MatchListController matchListController) {
            return tryHandleMatchListPageActions(normalized, context, matchListController);
        }
        return Optional.empty();
    }

    private Optional<Reply> tryHandleMatchDetailPageActions(String normalized, MatchDetailController controller) {
        Matchs currentMatch = controller.getCurrentMatch();
        if (currentMatch == null) {
            return Optional.empty();
        }

        String currentLabel = controller.getCurrentMatchLabel();
        Optional<Reply> lineupAnswerReply = tryHandleMatchDetailLineupAnswer(normalized, controller, currentLabel);
        if (lineupAnswerReply.isPresent()) {
            return lineupAnswerReply;
        }

        Optional<Reply> factAnswerReply = tryHandleMatchDetailFactAnswer(normalized, controller, currentLabel);
        if (factAnswerReply.isPresent()) {
            return factAnswerReply;
        }

        Optional<Reply> predictionReply = tryHandleMatchPrediction(normalized, controller, currentLabel);
        if (predictionReply.isPresent()) {
            return predictionReply;
        }

        if (containsAny(normalized, "lineup", "lineups", "composition", "compositions", "formation")) {
            return Optional.of(new Reply(
                    "Opening lineups for " + currentLabel + ".",
                    stage -> controller.openLineupTabFromAssistant(),
                    true
            ));
        }

        if (containsAny(normalized, "stat", "stats", "statistics")) {
            return Optional.of(new Reply(
                    "Opening match statistics for " + currentLabel + ".",
                    stage -> controller.openStatsTabFromAssistant(),
                    true
            ));
        }

        if (containsAny(normalized, "summary", "resume", "timeline", "overview")) {
            return Optional.of(new Reply(
                    "Opening the summary view for " + currentLabel + ".",
                    stage -> controller.openSummaryTabFromAssistant(),
                    true
            ));
        }

        if (containsAny(normalized, "back to matches", "back to match list", "return to matches", "open competition matches")) {
            return Optional.of(new Reply(
                    "Opening the competition match list for " + currentLabel + ".",
                    stage -> controller.openMatchListFromAssistant(),
                    true
            ));
        }

        if (refersToCurrentMatch(normalized, controller)) {
            return Optional.of(new Reply("You're already on " + currentLabel + ".", null, true));
        }

        return Optional.empty();
    }

    private Optional<Reply> tryHandleMatchDetailLineupAnswer(String normalized, MatchDetailController controller, String currentLabel) {
        if (!looksLikeLineupQuestion(normalized)) {
            return Optional.empty();
        }

        CurrentMatchTeamSide requestedSide = resolveCurrentMatchTeamSide(normalized, controller);
        if (requestedSide == CurrentMatchTeamSide.HOME) {
            return Optional.of(buildStartingLineupReply(
                    controller.getCurrentHomeTeamName(),
                    controller.getCurrentHomeStartingLineupNames(),
                    controller.getCurrentHomeLineupMeta(),
                    currentLabel,
                    controller
            ));
        }

        if (requestedSide == CurrentMatchTeamSide.AWAY) {
            return Optional.of(buildStartingLineupReply(
                    controller.getCurrentAwayTeamName(),
                    controller.getCurrentAwayStartingLineupNames(),
                    controller.getCurrentAwayLineupMeta(),
                    currentLabel,
                    controller
            ));
        }

        List<String> homePlayers = controller.getCurrentHomeStartingLineupNames();
        List<String> awayPlayers = controller.getCurrentAwayStartingLineupNames();
        if (homePlayers.isEmpty() && awayPlayers.isEmpty()) {
            return Optional.of(new Reply(
                    "I don't have the starting elevens for " + currentLabel + " yet, so I'm opening the lineups tab for you.",
                    stage -> controller.openLineupTabFromAssistant(),
                    true
            ));
        }

        StringBuilder response = new StringBuilder("Starting elevens for ")
                .append(currentLabel)
                .append(": ");

        if (!homePlayers.isEmpty()) {
            response.append(controller.getCurrentHomeTeamName())
                    .append(": ")
                    .append(formatLineupList(homePlayers));
        }
        if (!homePlayers.isEmpty() && !awayPlayers.isEmpty()) {
            response.append(" | ");
        }
        if (!awayPlayers.isEmpty()) {
            response.append(controller.getCurrentAwayTeamName())
                    .append(": ")
                    .append(formatLineupList(awayPlayers));
        }

        response.append(".");
        return Optional.of(new Reply(response.toString(), stage -> controller.openLineupTabFromAssistant(), true));
    }

    private Optional<Reply> tryHandleMatchDetailFactAnswer(String normalized, MatchDetailController controller, String currentLabel) {
        if (looksLikeMvpQuestion(normalized)) {
            String mvpSummary = controller.getCurrentMvpSummary();
            if (mvpSummary != null && !mvpSummary.isBlank()) {
                return Optional.of(new Reply(mvpSummary, stage -> controller.openLineupTabFromAssistant(), true));
            }
            return Optional.of(new Reply(
                    "I don't have a reliable MVP for " + currentLabel + " yet, so I'm opening the lineups view for you.",
                    stage -> controller.openLineupTabFromAssistant(),
                    true
            ));
        }

        if (looksLikeScoreQuestion(normalized)) {
            return Optional.of(new Reply(
                    currentLabel + " is " + controller.getCurrentScoreLabel()
                            + ". Status: " + controller.getCurrentStatusLabel()
                            + ". Competition: " + controller.getCurrentCompetitionLabel() + ".",
                    null,
                    true
            ));
        }

        if (looksLikeScorerQuestion(normalized)) {
            List<String> goals = controller.getCurrentGoalHighlights();
            if (goals.isEmpty()) {
                return Optional.of(new Reply(
                        "I don't have any recorded goal events for " + currentLabel + " yet. I'll open the summary tab for you.",
                        stage -> controller.openSummaryTabFromAssistant(),
                        true
                ));
            }
            return Optional.of(new Reply(
                    "Goal events for " + currentLabel + ": " + String.join(" | ", goals.stream().limit(8).toList()) + ".",
                    stage -> controller.openSummaryTabFromAssistant(),
                    true
            ));
        }

        if (looksLikeCardQuestion(normalized)) {
            List<String> cards = controller.getCurrentCardHighlights();
            if (cards.isEmpty()) {
                return Optional.of(new Reply(
                        "I don't have any card events for " + currentLabel + " right now. I'll open the summary tab for you.",
                        stage -> controller.openSummaryTabFromAssistant(),
                        true
                ));
            }
            return Optional.of(new Reply(
                    "Card events for " + currentLabel + ": " + String.join(" | ", cards.stream().limit(8).toList()) + ".",
                    stage -> controller.openSummaryTabFromAssistant(),
                    true
            ));
        }

        Optional<ApiFootballStatisticRow> statistic = findRelevantStatistic(normalized, controller.getCurrentStatistics());
        if (statistic.isPresent()) {
            ApiFootballStatisticRow row = statistic.get();
            return Optional.of(new Reply(
                    emptyToFallback(row.label(), "Stat")
                            + " in " + currentLabel + ": "
                            + controller.getCurrentHomeTeamName() + " " + emptyToFallback(row.homeValue(), "N/A")
                            + ", " + controller.getCurrentAwayTeamName() + " " + emptyToFallback(row.awayValue(), "N/A") + ".",
                    stage -> controller.openStatsTabFromAssistant(),
                    true
            ));
        }

        return Optional.empty();
    }

    private Optional<Reply> tryHandleMatchPrediction(String normalized, MatchDetailController controller, String currentLabel) {
        if (!looksLikePredictionQuestion(normalized)) {
            return Optional.empty();
        }

        Matchs currentMatch = controller.getCurrentMatch();
        if (currentMatch == null) {
            return Optional.empty();
        }

        String normalizedStatus = normalize(controller.getCurrentStatusLabel());
        if (hasRecordedResult(currentMatch) || isFinishedStatus(normalizedStatus)) {
            return Optional.of(new Reply(
                    "Jarvis does not need to predict " + currentLabel + " anymore. The recorded result is "
                            + controller.getCurrentScoreLabel() + " with status " + controller.getCurrentStatusLabel() + ".",
                    null,
                    true
            ));
        }

        if (isLiveStatus(normalizedStatus)) {
            return Optional.of(new Reply(
                    "Jarvis will not fake a prediction because " + currentLabel + " is already live. The current score is "
                            + controller.getCurrentScoreLabel() + ".",
                    null,
                    true
            ));
        }

        try {
            MatchPredictionInsight insight = buildMatchPredictionInsight(currentMatch, controller);
            if (insight == null) {
                return Optional.of(new Reply(
                        "Jarvis does not have enough completed local form data to make a serious prediction for "
                                + currentLabel + " yet.",
                        null,
                        true
                ));
            }

            String firstSentence = insight.drawFavorite()
                    ? "Jarvis prediction: " + currentLabel + " looks closest to a draw at about "
                        + formatPercent(insight.drawProbability()) + ", with a "
                        + insight.predictedHomeGoals() + "-" + insight.predictedAwayGoals() + " lean."
                    : "Jarvis prediction: " + insight.favoredTeamName() + " have the edge at about "
                        + formatPercent(insight.favoredProbability()) + ", with a "
                        + insight.predictedHomeGoals() + "-" + insight.predictedAwayGoals() + " scoreline lean.";
            String secondSentence = insight.reasons().isEmpty()
                    ? "That call comes from recent local form data."
                    : "Main reasons: " + String.join(" and ", insight.reasons()) + ".";
            return Optional.of(new Reply(firstSentence + " " + secondSentence, null, true));
        } catch (Exception ex) {
            return Optional.of(new Reply(
                    "Jarvis could not build a grounded prediction right now because the local match history is unavailable.",
                    null,
                    true
            ));
        }
    }

    private Optional<Reply> tryHandleMatchListPageActions(String normalized, Context context, MatchListController controller) {
        Optional<Reply> fixtureAnswerReply = tryHandleMatchListFixtureAnswer(normalized, controller);
        if (fixtureAnswerReply.isPresent()) {
            return fixtureAnswerReply;
        }

        if (!containsAny(normalized, "search", "find", "look for", "filter", "show")) {
            return Optional.empty();
        }

        try {
            List<Equipe> equipes = new EquipeService().getAll();
            TeamCandidate teamCandidate = findBestTeamCandidate(extractSearchFocus(normalized), equipes, Set.of());
            if (teamCandidate == null) {
                return Optional.empty();
            }

            String teamName = teamCandidate.team().getNom();
            return Optional.of(new Reply(
                    "Filtering the current match list for " + teamName + ".",
                    stage -> controller.applyAssistantSearch(teamName),
                    true
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Reply> tryHandleMatchListFixtureAnswer(String normalized, MatchListController controller) {
        if (!looksLikeFixtureTimingQuestion(normalized)) {
            return Optional.empty();
        }

        List<Matchs> visibleMatchs = controller.getFilteredMatchsSnapshot();
        if (visibleMatchs.isEmpty()) {
            return Optional.of(new Reply(
                    "I don't have any visible fixtures on this page yet. Try refreshing the matches list first.",
                    null,
                    true
            ));
        }

        TeamCandidate teamCandidate = findBestTeamCandidate(
                extractFixtureTeamFocus(normalized),
                controller.getKnownTeamsSnapshot(),
                Set.of()
        );
        if (teamCandidate == null) {
            return Optional.empty();
        }

        Integer teamId = teamCandidate.team().getId();
        String teamName = emptyToFallback(teamCandidate.team().getNom(), "That team");
        List<Matchs> teamFixtures = visibleMatchs.stream()
                .filter(match -> teamParticipates(match, teamId))
                .sorted(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo)))
                .toList();

        if (teamFixtures.isEmpty()) {
            return Optional.of(new Reply(
                    "I can't see any " + teamName + " fixtures in the current " + controller.getAssistantCompetitionLabel() + " view.",
                    null,
                    true
            ));
        }

        Matchs todaysFixture = findTeamFixtureOnDate(teamFixtures, LocalDate.now());
        Matchs nextFixture = findNextTeamFixture(teamFixtures, LocalDate.now());
        String competitionLabel = controller.getAssistantCompetitionLabel();

        if (looksLikeTodayFixtureQuestion(normalized)) {
            if (todaysFixture != null) {
                String opponent = resolveOpponentName(todaysFixture, teamId, controller);
                String answer = teamName + "'s visible fixture today is "
                        + controller.getAssistantMatchLabel(todaysFixture)
                        + " at " + controller.getAssistantFixtureSchedule(todaysFixture)
                        + " in " + competitionLabel + ".";
                if (looksLikeOpponentQuestion(normalized)) {
                    answer = teamName + " play " + opponent + " today at "
                            + controller.getAssistantFixtureSchedule(todaysFixture)
                            + " in " + competitionLabel + ".";
                }
                return Optional.of(new Reply(
                        answer,
                        null,
                        true
                ));
            }

            if (nextFixture != null) {
                String opponent = resolveOpponentName(nextFixture, teamId, controller);
                return Optional.of(new Reply(
                        teamName + " do not play today in " + competitionLabel + ". Their next visible fixture is against "
                                + opponent + " on " + controller.getAssistantFixtureSchedule(nextFixture) + ".",
                        null,
                        true
                ));
            }

            return Optional.of(new Reply(
                    teamName + " do not have a visible fixture today in " + competitionLabel + ".",
                    null,
                    true
            ));
        }

        if (nextFixture != null) {
            String opponent = resolveOpponentName(nextFixture, teamId, controller);
            String answer = teamName + "'s next visible fixture is "
                    + controller.getAssistantMatchLabel(nextFixture)
                    + " on " + controller.getAssistantFixtureSchedule(nextFixture)
                    + " in " + competitionLabel + ".";

            if (looksLikeOpponentQuestion(normalized)) {
                answer = teamName + " next play against " + opponent + " on "
                        + controller.getAssistantFixtureSchedule(nextFixture) + " in " + competitionLabel + ".";
            }
            return Optional.of(new Reply(answer, null, true));
        }

        Matchs latestVisibleFixture = teamFixtures.get(teamFixtures.size() - 1);
        return Optional.of(new Reply(
                "I can't see an upcoming " + teamName + " fixture in the current " + competitionLabel
                        + " view. The latest visible one is "
                        + controller.getAssistantMatchLabel(latestVisibleFixture)
                        + " on " + controller.getAssistantFixtureSchedule(latestVisibleFixture) + ".",
                null,
                true
        ));
    }

    private Optional<Reply> tryHandleCompetitionNavigation(String normalized, Context context) {
        Optional<String> requestedCompetitionCode = findRequestedCompetitionCode(normalized);
        if (requestedCompetitionCode.isEmpty() || !looksLikeCompetitionNavigationRequest(normalized)) {
            return Optional.empty();
        }

        if (!context.authenticated()) {
            return Optional.of(new Reply("You need to sign in before opening competition pages.", null, true));
        }

        String competitionCode = requestedCompetitionCode.get();
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        return Optional.of(new Reply(
                "Opening " + competitionLabel + " matches.",
                commandForCompetitionMatches(competitionCode),
                true
        ));
    }

    private Optional<Reply> tryHandleCompetitionStandingsNavigation(String normalized, Context context) {
        Optional<String> requestedCompetitionCode = findRequestedCompetitionCode(normalized);
        if (requestedCompetitionCode.isEmpty() || !looksLikeCompetitionStandingsRequest(normalized)) {
            return Optional.empty();
        }

        if (!context.authenticated()) {
            return Optional.of(new Reply("You need to sign in before opening league tables.", null, true));
        }

        String competitionCode = requestedCompetitionCode.get();
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        return Optional.of(new Reply(
                "Opening the " + competitionLabel + " league table.",
                commandForCompetitionStandings(competitionCode),
                true
        ));
    }

    private Optional<Reply> tryHandleSpecificMatchNavigation(String normalized, Context context) {
        if (!looksLikeSpecificMatchNavigationRequest(normalized)) {
            return Optional.empty();
        }

        Optional<TeamPairQuery> teamPair = extractTeamPairQuery(normalized);
        if (teamPair.isEmpty()) {
            return Optional.empty();
        }

        if (!context.authenticated()) {
            return Optional.of(new Reply("You need to sign in before opening match details.", null, true));
        }

        String requestedCompetitionCode = findRequestedCompetitionCode(normalized).orElse(null);
        try {
            MatchLookupResult lookupResult = findBestMatch(teamPair.get(), requestedCompetitionCode);
            if (lookupResult.match() != null) {
                String competitionLabel = FootballDataCompetitions.labelOf(lookupResult.match().getCompetitionCode());
                String matchLabel = buildMatchLabel(lookupResult.match(), lookupResult.teamById());
                return Optional.of(new Reply(
                        "Opening " + matchLabel + (competitionLabel == null ? "." : " in " + competitionLabel + "."),
                        commandForMatchDetail(lookupResult.match()),
                        true
                ));
            }
        } catch (Exception exception) {
            if (requestedCompetitionCode != null) {
                String competitionLabel = FootballDataCompetitions.labelOf(requestedCompetitionCode);
                return Optional.of(new Reply(
                        "I could not load that exact fixture right now, so I'm opening " + competitionLabel + " matches instead.",
                        commandForCompetitionMatches(requestedCompetitionCode),
                        true
                ));
            }
            return Optional.of(new Reply(
                    "I could not reach the match data right now. Try opening the competition page first, then ask again.",
                    commandFor(AssistantNavigationTarget.MATCHES),
                    true
            ));
        }

        if (requestedCompetitionCode != null) {
            String competitionLabel = FootballDataCompetitions.labelOf(requestedCompetitionCode);
            return Optional.of(new Reply(
                    "I could not find that exact fixture, so I'm opening " + competitionLabel + " matches for you.",
                    commandForCompetitionMatches(requestedCompetitionCode),
                    true
            ));
        }

        return Optional.of(new Reply(
                "I could not find that exact fixture in the current match data. Try adding the competition name or opening the competition page first.",
                commandFor(AssistantNavigationTarget.MATCHES),
                true
        ));
    }

    private Optional<Reply> tryHandleSingleTeamMatchNavigation(String normalized, Context context) {
        if (!looksLikeSingleTeamNavigationRequest(normalized)) {
            return Optional.empty();
        }

        if (!context.authenticated()) {
            return Optional.of(new Reply("You need to sign in before opening match details.", null, true));
        }

        String competitionCode = findRequestedCompetitionCode(normalized).orElse(currentCompetitionCodeFor(context));
        String focus = extractSearchFocus(normalized);
        try {
            MatchLookupResult lookupResult = findBestRecentMatchForTeam(focus, competitionCode);
            if (lookupResult.match() == null) {
                return Optional.empty();
            }

            String matchLabel = buildMatchLabel(lookupResult.match(), lookupResult.teamById());
            return Optional.of(new Reply(
                    "Opening the latest " + resolveTeamName(lookupResult.teamById(), findPrimaryRequestedTeamId(lookupResult), "team")
                            + " match: " + matchLabel + ".",
                    commandForMatchDetail(lookupResult.match()),
                    true
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private MatchLookupResult findBestMatch(TeamPairQuery teamPair, String requestedCompetitionCode) throws Exception {
        EquipeService equipeService = new EquipeService();
        MatchsService matchsService = new MatchsService();

        List<Equipe> equipes = equipeService.getAll();
        TeamCandidate homeCandidate = findBestTeamCandidate(teamPair.homeQuery(), equipes, Set.of());
        if (homeCandidate == null) {
            return new MatchLookupResult(null, Map.of(), null);
        }

        TeamCandidate awayCandidate = findBestTeamCandidate(teamPair.awayQuery(), equipes, Set.of(homeCandidate.team().getId()));
        if (awayCandidate == null) {
            return new MatchLookupResult(null, Map.of(), null);
        }

        Map<Integer, Equipe> teamById = equipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, equipe -> equipe, (left, right) -> left));

        Comparator<Matchs> comparator = buildMatchSelectionComparator(requestedCompetitionCode, true);

        Matchs bestMatch = matchsService.getAll().stream()
                .filter(match -> pairMatches(match, homeCandidate.team().getId(), awayCandidate.team().getId()))
                .sorted(comparator)
                .findFirst()
                .orElse(null);

        return new MatchLookupResult(bestMatch, teamById, null);
    }

    private MatchLookupResult findBestRecentMatchForTeam(String teamQuery, String requestedCompetitionCode) throws Exception {
        EquipeService equipeService = new EquipeService();
        MatchsService matchsService = new MatchsService();

        List<Equipe> equipes = equipeService.getAll();
        TeamCandidate teamCandidate = findBestTeamCandidate(teamQuery, equipes, Set.of());
        if (teamCandidate == null) {
            return new MatchLookupResult(null, Map.of(), null);
        }

        Map<Integer, Equipe> teamById = equipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, equipe -> equipe, (left, right) -> left));

        Matchs bestMatch = matchsService.getAll().stream()
                .filter(match -> teamParticipates(match, teamCandidate.team().getId()))
                .filter(match -> requestedCompetitionCode == null
                        || requestedCompetitionCode.equalsIgnoreCase(match.getCompetitionCode()))
                .sorted(buildMatchSelectionComparator(requestedCompetitionCode, false))
                .findFirst()
                .orElse(null);

        return new MatchLookupResult(bestMatch, teamById, teamCandidate.team().getId());
    }

    private Comparator<Matchs> buildMatchSelectionComparator(String requestedCompetitionCode, boolean preferNearestMatch) {
        LocalDate today = LocalDate.now();
        Comparator<Matchs> comparator = Comparator
                .comparingInt((Matchs match) -> requestedCompetitionCode != null
                        && requestedCompetitionCode.equalsIgnoreCase(match.getCompetitionCode()) ? 0 : 1);

        if (preferNearestMatch) {
            comparator = comparator.thenComparingLong(match -> dateDistance(match, today));
        } else {
            comparator = comparator.thenComparingInt((Matchs match) -> {
                if (match == null || match.getDateMatch() == null) {
                    return 1;
                }
                return match.getDateMatch().isAfter(today) ? 1 : 0;
            });
        }

        return comparator
                .thenComparing(Matchs::getDateMatch, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private TeamCandidate findBestTeamCandidate(String query, List<Equipe> equipes, Set<Integer> excludedIds) {
        if (query == null || query.isBlank()) {
            return null;
        }

        TeamCandidate bestCandidate = null;
        for (Equipe equipe : equipes) {
            if (equipe == null || equipe.getId() == null || excludedIds.contains(equipe.getId())) {
                continue;
            }

            int score = scoreTeamCandidate(query, normalize(equipe.getNom()));
            if (score < MIN_TEAM_MATCH_SCORE) {
                continue;
            }

            if (bestCandidate == null || score > bestCandidate.score()) {
                bestCandidate = new TeamCandidate(equipe, score);
            }
        }

        return bestCandidate;
    }

    private int scoreTeamCandidate(String query, String teamName) {
        if (query == null || query.isBlank() || teamName == null || teamName.isBlank()) {
            return 0;
        }

        if (query.equals(teamName)) {
            return 140;
        }

        int score = 0;
        if (teamName.contains(query)) {
            score = Math.max(score, 110 - Math.abs(teamName.length() - query.length()));
        }
        if (query.contains(teamName)) {
            score = Math.max(score, 92 - Math.abs(query.length() - teamName.length()));
        }

        List<String> queryTokens = tokens(query);
        List<String> teamTokens = tokens(teamName);
        long matchedTokens = queryTokens.stream()
                .filter(token -> token.length() > 1)
                .filter(token -> teamName.contains(token) || teamTokens.contains(token))
                .count();

        if (matchedTokens > 0) {
            score = Math.max(score, (int) (40 + matchedTokens * 16));
        }
        if (!queryTokens.isEmpty() && matchedTokens == queryTokens.size()) {
            score = Math.max(score, 92 + queryTokens.size() * 4);
        }

        return score;
    }

    private boolean pairMatches(Matchs match, Integer homeId, Integer awayId) {
        if (match == null || homeId == null || awayId == null) {
            return false;
        }

        return (homeId.equals(match.getEquipeDomicileId()) && awayId.equals(match.getEquipeExterieurId()))
                || (awayId.equals(match.getEquipeDomicileId()) && homeId.equals(match.getEquipeExterieurId()));
    }

    private boolean teamParticipates(Matchs match, Integer teamId) {
        if (match == null || teamId == null) {
            return false;
        }
        return teamId.equals(match.getEquipeDomicileId()) || teamId.equals(match.getEquipeExterieurId());
    }

    private long dateDistance(Matchs match, LocalDate today) {
        if (match == null || match.getDateMatch() == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(ChronoUnit.DAYS.between(today, match.getDateMatch()));
    }

    private Optional<TeamPairQuery> extractTeamPairQuery(String normalized) {
        for (String delimiter : MATCH_DELIMITERS) {
            int separatorIndex = normalized.indexOf(delimiter);
            if (separatorIndex < 0) {
                continue;
            }

            String left = cleanTeamQuery(normalized.substring(0, separatorIndex));
            String right = cleanTeamQuery(normalized.substring(separatorIndex + delimiter.length()));
            if (!left.isBlank() && !right.isBlank()) {
                return Optional.of(new TeamPairQuery(left, right));
            }
        }
        return Optional.empty();
    }

    private String extractSearchFocus(String normalized) {
        String cleaned = normalized == null ? "" : normalized;
        for (String delimiter : MATCH_DELIMITERS) {
            cleaned = cleaned.replace(delimiter, " ");
        }
        for (String competitionAlias : COMPETITION_ALIASES.keySet()) {
            cleaned = cleaned.replace(competitionAlias, " ");
        }
        return cleaned
                .replaceAll("\\b(open|opening|show|search|find|look|filter|for|matches|matchs|match|fixture|fixtures|details|detail|page|screen|latest|last|recent|game|games|champions|league|uefa|please|the|this|that|want|need|take|me|to|go|bring)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanTeamQuery(String rawSegment) {
        String cleaned = rawSegment == null ? "" : rawSegment;

        for (String competitionAlias : COMPETITION_ALIASES.keySet()) {
            cleaned = cleaned.replace(competitionAlias, " ");
        }

        cleaned = cleaned
                .replaceAll("\\b(open|show|bring|take|navigate|go|goto|to|me|the|match|matches|matchs|details|detail|page|screen|fixture|game|for|of|please|now|view|section)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned;
    }

    private Optional<String> findRequestedCompetitionCode(String normalized) {
        return COMPETITION_ALIASES.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .sorted(Map.Entry.<String, String>comparingByKey(Comparator.comparingInt(String::length)).reversed())
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private String currentCompetitionCodeFor(Context context) {
        Object controller = context.controller();
        if (controller instanceof MatchDetailController matchDetailController) {
            return matchDetailController.getCurrentCompetitionCode();
        }
        if (controller instanceof MatchListController matchListController) {
            return matchListController.getSelectedCompetitionCode();
        }
        return null;
    }

    private boolean refersToCurrentMatch(String normalized, MatchDetailController controller) {
        if (containsAny(normalized, "this game", "this match", "this fixture", "current match", "current game", "this one")) {
            return true;
        }

        String home = normalize(controller.getCurrentHomeTeamName());
        String away = normalize(controller.getCurrentAwayTeamName());
        return home != null && away != null && normalized.contains(home) && normalized.contains(away);
    }

    private MatchPredictionInsight buildMatchPredictionInsight(Matchs currentMatch, MatchDetailController controller) throws Exception {
        if (currentMatch == null || currentMatch.getEquipeDomicileId() == null || currentMatch.getEquipeExterieurId() == null) {
            return null;
        }

        MatchsService matchsService = new MatchsService();
        List<Matchs> completedHistory = matchsService.getAll().stream()
                .filter(match -> isPredictionHistoryCandidate(match, currentMatch))
                .sorted(predictionHistoryComparator())
                .toList();
        if (completedHistory.isEmpty()) {
            return null;
        }

        Map<Integer, Equipe> teamById = new EquipeService().getAll().stream()
                .filter(team -> team != null && team.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, team -> team, (left, right) -> left));

        Integer homeTeamId = currentMatch.getEquipeDomicileId();
        Integer awayTeamId = currentMatch.getEquipeExterieurId();
        String competitionCode = currentMatch.getCompetitionCode();

        TeamPredictionSnapshot home = summarizeTeamPrediction(
                completedHistory,
                homeTeamId,
                resolveTeamName(teamById, homeTeamId, emptyToFallback(controller.getCurrentHomeTeamName(), "Home team")),
                true,
                competitionCode
        );
        TeamPredictionSnapshot away = summarizeTeamPrediction(
                completedHistory,
                awayTeamId,
                resolveTeamName(teamById, awayTeamId, emptyToFallback(controller.getCurrentAwayTeamName(), "Away team")),
                false,
                competitionCode
        );

        if (home.recentMatches() < 2 || away.recentMatches() < 2) {
            return null;
        }

        List<Matchs> headToHead = completedHistory.stream()
                .filter(match -> pairMatches(match, homeTeamId, awayTeamId))
                .limit(4)
                .toList();

        double homeVenuePoints = home.venueMatches() == 0 ? home.pointsPerMatch() : home.venuePointsPerMatch();
        double awayVenuePoints = away.venueMatches() == 0 ? away.pointsPerMatch() : away.venuePointsPerMatch();
        double homeCompetitionPoints = home.competitionMatches() == 0 ? home.pointsPerMatch() : home.competitionPointsPerMatch();
        double awayCompetitionPoints = away.competitionMatches() == 0 ? away.pointsPerMatch() : away.competitionPointsPerMatch();
        double headToHeadBias = headToHead.isEmpty()
                ? 0.0
                : (averagePointsPerMatch(headToHead, homeTeamId) - averagePointsPerMatch(headToHead, awayTeamId)) * 0.10
                    + averageGoalDiffPerMatch(headToHead, homeTeamId) * 0.06;

        double homeStrength = 0.35
                + home.pointsPerMatch() * 0.90
                + home.goalDiffPerMatch() * 0.22
                + home.goalsForPerMatch() * 0.13
                - home.goalsAgainstPerMatch() * 0.10
                + homeVenuePoints * 0.35
                + homeCompetitionPoints * 0.28;
        double awayStrength = away.pointsPerMatch() * 0.90
                + away.goalDiffPerMatch() * 0.22
                + away.goalsForPerMatch() * 0.13
                - away.goalsAgainstPerMatch() * 0.10
                + awayVenuePoints * 0.35
                + awayCompetitionPoints * 0.28;
        double delta = homeStrength - awayStrength + headToHeadBias;

        double drawProbability = clamp(0.24 - Math.min(Math.abs(delta) * 0.03, 0.10), 0.14, 0.28);
        double decisiveShare = 1.0 - drawProbability;
        double homeShare = 1.0 / (1.0 + Math.exp(-delta));
        double homeWinProbability = clamp(decisiveShare * homeShare, 0.12, 0.78);
        double awayWinProbability = clamp(decisiveShare - homeWinProbability, 0.10, 0.74);
        double totalProbability = homeWinProbability + drawProbability + awayWinProbability;
        homeWinProbability /= totalProbability;
        drawProbability /= totalProbability;
        awayWinProbability /= totalProbability;

        double homeExpectedGoals = clamp(
                0.55
                        + home.goalsForPerMatch() * 0.42
                        + homeVenuePoints * 0.12
                        + Math.max(home.goalDiffPerMatch(), 0.0) * 0.08
                        + away.goalsAgainstPerMatch() * 0.14
                        + 0.20,
                0.4,
                3.4
        );
        double awayExpectedGoals = clamp(
                0.40
                        + away.goalsForPerMatch() * 0.40
                        + awayVenuePoints * 0.10
                        + Math.max(away.goalDiffPerMatch(), 0.0) * 0.07
                        + home.goalsAgainstPerMatch() * 0.13,
                0.3,
                3.0
        );

        int predictedHomeGoals = clampInt((int) Math.round(homeExpectedGoals), 0, 4);
        int predictedAwayGoals = clampInt((int) Math.round(awayExpectedGoals), 0, 4);
        boolean drawFavorite = drawProbability >= homeWinProbability && drawProbability >= awayWinProbability;
        if (drawFavorite) {
            int drawGoals = clampInt((int) Math.round((homeExpectedGoals + awayExpectedGoals) / 2.0), 0, 3);
            predictedHomeGoals = drawGoals;
            predictedAwayGoals = drawGoals;
        } else if (homeWinProbability >= awayWinProbability && predictedHomeGoals <= predictedAwayGoals) {
            predictedHomeGoals = Math.min(predictedAwayGoals + 1, 4);
        } else if (awayWinProbability > homeWinProbability && predictedAwayGoals <= predictedHomeGoals) {
            predictedAwayGoals = Math.min(predictedHomeGoals + 1, 4);
        }

        String favoredTeamName = drawFavorite
                ? "Draw"
                : homeWinProbability >= awayWinProbability ? home.teamName() : away.teamName();
        return new MatchPredictionInsight(
                favoredTeamName,
                homeWinProbability,
                drawProbability,
                awayWinProbability,
                predictedHomeGoals,
                predictedAwayGoals,
                buildPredictionReasons(home, away, homeVenuePoints, awayVenuePoints, headToHeadBias),
                drawFavorite
        );
    }

    private TeamPredictionSnapshot summarizeTeamPrediction(
            List<Matchs> completedHistory,
            Integer teamId,
            String teamName,
            boolean currentHomeContext,
            String competitionCode
    ) {
        List<Matchs> teamMatches = completedHistory.stream()
                .filter(match -> teamParticipates(match, teamId))
                .limit(PREDICTION_RECENT_MATCH_LIMIT)
                .toList();
        List<Matchs> venueMatches = completedHistory.stream()
                .filter(match -> teamParticipates(match, teamId))
                .filter(match -> currentHomeContext ? teamId.equals(match.getEquipeDomicileId()) : teamId.equals(match.getEquipeExterieurId()))
                .limit(4)
                .toList();
        List<Matchs> competitionMatches = competitionCode == null || competitionCode.isBlank()
                ? List.of()
                : completedHistory.stream()
                .filter(match -> teamParticipates(match, teamId))
                .filter(match -> competitionCode.equalsIgnoreCase(match.getCompetitionCode()))
                .limit(4)
                .toList();

        return new TeamPredictionSnapshot(
                teamName,
                teamMatches.size(),
                averagePointsPerMatch(teamMatches, teamId),
                averageGoalDiffPerMatch(teamMatches, teamId),
                averageGoalsForPerMatch(teamMatches, teamId),
                averageGoalsAgainstPerMatch(teamMatches, teamId),
                venueMatches.size(),
                averagePointsPerMatch(venueMatches, teamId),
                competitionMatches.size(),
                averagePointsPerMatch(competitionMatches, teamId)
        );
    }

    private List<String> buildPredictionReasons(
            TeamPredictionSnapshot home,
            TeamPredictionSnapshot away,
            double homeVenuePoints,
            double awayVenuePoints,
            double headToHeadBias
    ) {
        List<String> reasons = new ArrayList<>();
        if (home.pointsPerMatch() - away.pointsPerMatch() >= 0.45) {
            reasons.add(home.teamName() + " have the stronger recent form");
        } else if (away.pointsPerMatch() - home.pointsPerMatch() >= 0.45) {
            reasons.add(away.teamName() + " have the stronger recent form");
        }

        if (homeVenuePoints - awayVenuePoints >= 0.45) {
            reasons.add(home.teamName() + " have the better home trend");
        } else if (awayVenuePoints - homeVenuePoints >= 0.45) {
            reasons.add(away.teamName() + " have the better away trend");
        }

        if (home.goalDiffPerMatch() - away.goalDiffPerMatch() >= 0.55) {
            reasons.add(home.teamName() + " are carrying the better goal balance");
        } else if (away.goalDiffPerMatch() - home.goalDiffPerMatch() >= 0.55) {
            reasons.add(away.teamName() + " are carrying the better goal balance");
        }

        if (headToHeadBias >= 0.22) {
            reasons.add(home.teamName() + " have edged the recent head to head data");
        } else if (headToHeadBias <= -0.22) {
            reasons.add(away.teamName() + " have edged the recent head to head data");
        }

        if (reasons.isEmpty()) {
            reasons.add("the local form data is very tight between both sides");
        }
        return reasons.stream().limit(2).toList();
    }

    private Comparator<Matchs> predictionHistoryComparator() {
        return Comparator.comparing(Matchs::getDateMatch, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean isPredictionHistoryCandidate(Matchs historical, Matchs currentMatch) {
        return historical != null
                && !sameMatch(historical, currentMatch)
                && historical.getEquipeDomicileId() != null
                && historical.getEquipeExterieurId() != null
                && hasRecordedResult(historical)
                && happenedBeforeCurrentMatch(historical, currentMatch);
    }

    private boolean happenedBeforeCurrentMatch(Matchs historical, Matchs currentMatch) {
        LocalDate referenceDate = currentMatch == null || currentMatch.getDateMatch() == null
                ? LocalDate.now()
                : currentMatch.getDateMatch();
        LocalTime referenceTime = currentMatch == null ? null : currentMatch.getHeureDebut();
        if (historical == null || historical.getDateMatch() == null) {
            return false;
        }
        if (historical.getDateMatch().isBefore(referenceDate)) {
            return true;
        }
        if (historical.getDateMatch().isAfter(referenceDate)) {
            return false;
        }
        if (referenceTime == null || historical.getHeureDebut() == null) {
            return true;
        }
        return !historical.getHeureDebut().isAfter(referenceTime);
    }

    private boolean sameMatch(Matchs left, Matchs right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        if (left.getIdMatch() != null && right.getIdMatch() != null) {
            return left.getIdMatch().equalsIgnoreCase(right.getIdMatch());
        }
        return false;
    }

    private boolean hasRecordedResult(Matchs match) {
        return match != null && match.getScoreEquipeDomicile() != null && match.getScoreEquipeExterieur() != null;
    }

    private double averagePointsPerMatch(List<Matchs> matches, Integer teamId) {
        if (matches == null || matches.isEmpty() || teamId == null) {
            return 0.0;
        }
        return matches.stream()
                .mapToDouble(match -> pointsForTeam(match, teamId))
                .average()
                .orElse(0.0);
    }

    private double averageGoalDiffPerMatch(List<Matchs> matches, Integer teamId) {
        if (matches == null || matches.isEmpty() || teamId == null) {
            return 0.0;
        }
        return matches.stream()
                .mapToDouble(match -> goalsForTeam(match, teamId) - goalsAgainstTeam(match, teamId))
                .average()
                .orElse(0.0);
    }

    private double averageGoalsForPerMatch(List<Matchs> matches, Integer teamId) {
        if (matches == null || matches.isEmpty() || teamId == null) {
            return 0.0;
        }
        return matches.stream()
                .mapToDouble(match -> goalsForTeam(match, teamId))
                .average()
                .orElse(0.0);
    }

    private double averageGoalsAgainstPerMatch(List<Matchs> matches, Integer teamId) {
        if (matches == null || matches.isEmpty() || teamId == null) {
            return 0.0;
        }
        return matches.stream()
                .mapToDouble(match -> goalsAgainstTeam(match, teamId))
                .average()
                .orElse(0.0);
    }

    private double pointsForTeam(Matchs match, Integer teamId) {
        if (match == null || teamId == null || !teamParticipates(match, teamId) || !hasRecordedResult(match)) {
            return 0.0;
        }
        int goalsFor = goalsForTeam(match, teamId);
        int goalsAgainst = goalsAgainstTeam(match, teamId);
        if (goalsFor > goalsAgainst) {
            return 3.0;
        }
        if (goalsFor == goalsAgainst) {
            return 1.0;
        }
        return 0.0;
    }

    private int goalsForTeam(Matchs match, Integer teamId) {
        if (match == null || teamId == null || !hasRecordedResult(match)) {
            return 0;
        }
        if (teamId.equals(match.getEquipeDomicileId())) {
            return match.getScoreEquipeDomicile();
        }
        if (teamId.equals(match.getEquipeExterieurId())) {
            return match.getScoreEquipeExterieur();
        }
        return 0;
    }

    private int goalsAgainstTeam(Matchs match, Integer teamId) {
        if (match == null || teamId == null || !hasRecordedResult(match)) {
            return 0;
        }
        if (teamId.equals(match.getEquipeDomicileId())) {
            return match.getScoreEquipeExterieur();
        }
        if (teamId.equals(match.getEquipeExterieurId())) {
            return match.getScoreEquipeDomicile();
        }
        return 0;
    }

    private boolean isFinishedStatus(String normalizedStatus) {
        return containsAny(normalizedStatus, "fini", "finished", "full time", "termine", "ended", "complete");
    }

    private boolean isLiveStatus(String normalizedStatus) {
        return containsAny(
                normalizedStatus,
                "en cours",
                "live",
                "1st half",
                "2nd half",
                "first half",
                "second half",
                "mi temps",
                "half time",
                "extra time",
                "penalties"
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatPercent(double probability) {
        return Math.round(clamp(probability, 0.0, 1.0) * 100.0) + "%";
    }

    private Reply buildStartingLineupReply(
            String teamName,
            List<String> players,
            String lineupMeta,
            String currentLabel,
            MatchDetailController controller
    ) {
        if (players == null || players.isEmpty()) {
            return new Reply(
                    "I don't have the starting eleven for " + teamName + " on " + currentLabel + " yet, so I'm opening the lineups tab for you.",
                    stage -> controller.openLineupTabFromAssistant(),
                    true
            );
        }

        String meta = lineupMeta == null || lineupMeta.isBlank() || lineupMeta.toLowerCase(Locale.ROOT).contains("indisponible")
                ? ""
                : " " + lineupMeta + ".";
        return new Reply(
                teamName + " starting XI: " + formatLineupList(players) + "." + meta,
                stage -> controller.openLineupTabFromAssistant(),
                true
        );
    }

    private CurrentMatchTeamSide resolveCurrentMatchTeamSide(String normalized, MatchDetailController controller) {
        if (containsAny(normalized, "home team", "home side", "domicile", "local")) {
            return CurrentMatchTeamSide.HOME;
        }
        if (containsAny(normalized, "away team", "away side", "exterieur", "visitor", "visitors")) {
            return CurrentMatchTeamSide.AWAY;
        }

        String homeName = normalize(controller.getCurrentHomeTeamName());
        String awayName = normalize(controller.getCurrentAwayTeamName());
        int homeScore = scoreTeamCandidate(normalized, homeName);
        int awayScore = scoreTeamCandidate(normalized, awayName);

        if (homeScore == 0 && awayScore == 0) {
            return null;
        }
        if (homeScore == awayScore) {
            return null;
        }
        return homeScore > awayScore ? CurrentMatchTeamSide.HOME : CurrentMatchTeamSide.AWAY;
    }

    private Optional<ApiFootballStatisticRow> findRelevantStatistic(String normalized, List<ApiFootballStatisticRow> statistics) {
        if (statistics == null || statistics.isEmpty() || !looksLikeStatisticQuestion(normalized)) {
            return Optional.empty();
        }

        ApiFootballStatisticRow bestRow = null;
        int bestScore = 0;
        for (ApiFootballStatisticRow row : statistics) {
            int score = scoreStatisticRow(normalized, row);
            if (score > bestScore) {
                bestScore = score;
                bestRow = row;
            }
        }

        return bestScore >= 18 ? Optional.ofNullable(bestRow) : Optional.empty();
    }

    private int scoreStatisticRow(String query, ApiFootballStatisticRow row) {
        if (row == null || row.label() == null || row.label().isBlank()) {
            return 0;
        }

        String label = normalize(row.label());
        if (label.isBlank()) {
            return 0;
        }
        if (query.contains(label) || label.contains(query)) {
            return 100;
        }

        int score = 0;
        for (String token : tokens(label)) {
            if (token.length() < 3) {
                continue;
            }
            if (query.contains(token)) {
                score += 18;
            }
        }
        return score;
    }

    private String formatLineupList(List<String> players) {
        if (players == null || players.isEmpty()) {
            return "Unavailable";
        }

        List<String> visiblePlayers = players.stream()
                .filter(name -> name != null && !name.isBlank())
                .limit(11)
                .toList();

        List<String> numberedPlayers = new ArrayList<>();
        for (int index = 0; index < visiblePlayers.size(); index++) {
            numberedPlayers.add((index + 1) + ". " + visiblePlayers.get(index).trim());
        }
        return String.join(", ", numberedPlayers);
    }

    private Reply handleNavigation(AssistantNavigationTarget target, Context context) {
        if (target.adminOnly() && !context.admin()) {
            return new Reply("Only admin accounts can open the admin dashboard and moderation views.", null, true);
        }
        if (target.requiresAuthentication() && !context.authenticated()) {
            return new Reply("You need to sign in before opening " + target.label() + ".", null, true);
        }
        return new Reply("Opening " + target.label() + ".", commandFor(target), true);
    }

    private AssistantCommand commandFor(AssistantNavigationTarget target) {
        return stage -> SceneNavigator.setScene(stage, target.fxmlPath(), target.cssPath(), target.title());
    }

    private AssistantCommand commandForCompetitionMatches(String competitionCode) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        return stage -> SceneNavigator.setScene(
                stage,
                "/tn/esprit/views/match-crud-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                competitionLabel + " | Matchs",
                controller -> {
                    if (controller instanceof MatchListController matchListController) {
                        matchListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

    private AssistantCommand commandForCompetitionStandings(String competitionCode) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        return stage -> SceneNavigator.setScene(
                stage,
                "/tn/esprit/views/league-table-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                competitionLabel + " | League Table",
                controller -> {
                    if (controller instanceof LeagueTableController leagueTableController) {
                        leagueTableController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

    private AssistantCommand commandForMatchDetail(Matchs match) {
        return stage -> SceneNavigator.setScene(
                stage,
                "/tn/esprit/views/match-detail-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Fiche match",
                controller -> {
                    if (controller instanceof MatchDetailController matchDetailController) {
                        matchDetailController.setMatchContext(match);
                    }
                }
        );
    }

    private String buildSystemPrompt(Context context, AssistantScreenCatalog.ScreenMeta currentScreen, InteractionMode mode) {
        String liveContext = resolveLiveContext(context.controller());
        if (mode == InteractionMode.VOICE) {
            return """
                    You are Jarvis, Sport Insight's built-in voice assistant inside a JavaFX desktop football app.

                    Rules:
                    - Help only with Sport Insight.
                    - Answer in 1 short sentence when possible, or 2 short sentences if needed.
                    - Use the live screen data as ground truth.
                    - If the live screen data contains the answer, answer directly.
                    - Prefer practical, immediate answers over explanations.
                    - Never claim you opened a page unless the local action layer already did it.
                    - If the user asks your name, say you are Jarvis.
                    - If the user asks something unrelated to the app, say you only help with Sport Insight.

                    Session:
                    - Screen: %s
                    - Authenticated: %s
                    - Admin: %s
                    - Live data: %s
                    - App modules: %s
                    """.formatted(
                    currentScreen.title(),
                    context.authenticated(),
                    context.admin(),
                    liveContext,
                    buildVoiceKnowledgeSummary()
            );
        }

        String interactionRules = mode == InteractionMode.VOICE
                ? """
                - This reply will be spoken out loud, so answer in at most 2 short sentences unless the user explicitly asks for a full list.
                - Start with the direct answer, then give one short useful detail.
                - Avoid long bullet lists in voice mode unless the user asked for names, lineups, rankings, or multiple items.
                """
                : "";
        return """
                You are Jarvis, Sport Insight's built-in assistant inside a JavaFX desktop football management application.

                Rules:
                - Help only with Sport Insight modules, screens, workflows, roles, and local setup.
                - If the user asks something unrelated to the application, say you only help with Sport Insight.
                - Use the live screen data below as ground truth for the current page.
                - If the live screen data already contains the answer, answer directly instead of giving generic guidance.
                - Keep answers concise, practical, and navigation-aware.
                - Accept casual phrasing, typos, and short follow-ups like "this match", "that player", or "who is MVP".
                - When the user asks how to do something, explain the nearest Sport Insight workflow.
                - Respect role boundaries: admin-only features require an admin account.
                - Never invent live database values or external API results you cannot see.
                - Never claim that you opened a page or changed data unless the local action layer already did it.
                - When the user asks to open a page or act on the current screen, prefer the local action layer over a descriptive answer.
                - If the user asks your name, say you are Jarvis.
                %s

                Current session:
                - Screen: %s
                - Screen summary: %s
                - Authenticated: %s
                - Admin: %s
                - User: %s
                - Live screen data:
                %s

                App knowledge:
                %s
                """.formatted(
                interactionRules,
                currentScreen.title(),
                currentScreen.description(),
                context.authenticated(),
                context.admin(),
                context.displayName(),
                liveContext,
                knowledgeBase
        );
    }

    private String buildVoiceKnowledgeSummary() {
        return "Home, Equipes, Joueurs, Matchs, Leagues, Annonces, Entrainements, Sponsors, Store, Admin.";
    }

    private String buildOfflineFallback(AssistantScreenCatalog.ScreenMeta currentScreen, Context context, InteractionMode mode) {
        if (mode == InteractionMode.VOICE) {
            return buildVoiceFallback(currentScreen, context);
        }
        return describeCurrentScreen(currentScreen)
                + "\n\nLive screen data:\n"
                + resolveLiveContext(context == null ? null : context.controller())
                + "\n\nFor richer answers, start Ollama locally and pull "
                + preferredModel
                + ". The assistant still keeps local page navigation and voice features offline with no paid API.";
    }

    private String buildVoiceFallback(AssistantScreenCatalog.ScreenMeta currentScreen, Context context) {
        String liveContext = resolveLiveContext(context == null ? null : context.controller());
        String firstLine = liveContext == null ? "" : liveContext.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.equalsIgnoreCase("No additional live screen data is available for this page."))
                .findFirst()
                .orElse("");
        if (!firstLine.isBlank()) {
            return firstLine.length() <= 180 ? firstLine : firstLine.substring(0, 180).trim() + ".";
        }
        return "You're on " + currentScreen.title() + ". Ask me about this page, a match, a team, a player, a league, or say open a page.";
    }

    private String describeCurrentScreen(AssistantScreenCatalog.ScreenMeta currentScreen) {
        String tips = currentScreen.tips().stream()
                .map(tip -> "- " + tip)
                .collect(Collectors.joining("\n"));
        return currentScreen.title() + ": " + currentScreen.description() + "\n" + tips;
    }

    private String describeCurrentActions(AssistantScreenCatalog.ScreenMeta currentScreen) {
        return "Here is what you can do on " + currentScreen.title() + ":\n"
                + currentScreen.tips().stream()
                .map(tip -> "- " + tip)
                .collect(Collectors.joining("\n"));
    }

    private String describeTarget(AssistantNavigationTarget target) {
        AssistantScreenCatalog.ScreenMeta meta = AssistantScreenCatalog.resolve(target);
        return meta.title() + ": " + meta.description() + "\n"
                + meta.tips().stream().map(tip -> "- " + tip).collect(Collectors.joining("\n"));
    }

    private String listModules() {
        return """
                Sport Insight modules:
                - Home: launch point for the main user workspace.
                - Equipes: manage clubs, coaches, and team information.
                - Joueurs: manage player profiles and team assignment.
                - Matchs: manage fixtures, competitions, scores, and detailed match pages.
                - Leagues: browse competitions and standings.
                - Annonces: publish updates and announcements.
                - Entrainements: manage training sessions, participation, and evaluation flows.
                - Sponsors: manage sponsorship and contract workflows.
                - Store: manage products, orders, and shopping flows.
                - Admin: dashboard, moderation, and admin CRUD views.
                """;
    }

    private String resolveLiveContext(Object controller) {
        if (controller instanceof AssistantContextProvider provider) {
            String summary = provider.assistantContextSummary();
            if (summary != null && !summary.isBlank()) {
                return summary.trim();
            }
        }
        return "No additional live screen data is available for this page.";
    }

    private String buildModelRuntimeStatus() {
        OllamaClient.Status fastStatus = ollamaClient.status(FAST_MODEL);
        OllamaClient.Status deepStatus = ollamaClient.status(resolveDeepReasoningModel());
        if (!fastStatus.reachable() && !deepStatus.reachable()) {
            return "Local AI offline. Install Ollama and pull " + preferredModel + ".";
        }

        String fastModel = fastStatus.selectedModel();
        String deepModel = deepStatus.selectedModel();
        if (fastModel == null && deepModel == null) {
            return "Local AI is reachable but no model is installed.";
        }
        if (fastModel != null && deepModel != null && !fastModel.equalsIgnoreCase(deepModel)) {
            return "Local AI ready. Quick replies use " + fastModel + " and deeper answers use " + deepModel + ".";
        }
        return "Local AI ready on " + emptyToFallback(deepModel != null ? deepModel : fastModel, preferredModel) + ".";
    }

    private String resolveModelForPrompt(String rawPrompt, String normalizedPrompt, Context context, InteractionMode mode) {
        boolean preferFastModel = shouldPreferFastModel(rawPrompt, normalizedPrompt, context, mode);
        String requestedModel = preferFastModel ? FAST_MODEL : resolveDeepReasoningModel();
        OllamaClient.Status requestedStatus = ollamaClient.status(requestedModel);
        if (requestedStatus.reachable() && requestedStatus.selectedModel() != null) {
            return requestedStatus.selectedModel();
        }

        if (!preferFastModel) {
            OllamaClient.Status fastStatus = ollamaClient.status(FAST_MODEL);
            if (fastStatus.reachable() && fastStatus.selectedModel() != null) {
                return fastStatus.selectedModel();
            }
        }
        return requestedModel;
    }

    private boolean shouldPreferFastModel(String rawPrompt, String normalizedPrompt, Context context, InteractionMode mode) {
        if (mode == InteractionMode.VOICE) {
            return true;
        }
        if (looksLikeDeepReasoningRequest(rawPrompt, normalizedPrompt)) {
            return false;
        }
        if (context != null && context.controller() instanceof AssistantContextProvider) {
            return true;
        }
        return (rawPrompt == null || rawPrompt.length() <= FAST_PROMPT_CHAR_LIMIT)
                && countTokens(normalizedPrompt) <= FAST_PROMPT_TOKEN_LIMIT;
    }

    private String resolveDeepReasoningModel() {
        if (preferredModel == null || preferredModel.isBlank() || FAST_MODEL.equalsIgnoreCase(preferredModel.trim())) {
            return DEFAULT_MODEL;
        }
        return preferredModel.trim();
    }

    private OllamaClient.ChatProfile resolveChatProfile(InteractionMode mode, String rawPrompt, String normalizedPrompt) {
        if (mode == InteractionMode.VOICE) {
            return OllamaClient.ChatProfile.REALTIME;
        }
        if (looksLikeDeepReasoningRequest(rawPrompt, normalizedPrompt)) {
            return OllamaClient.ChatProfile.DEEP;
        }
        return OllamaClient.ChatProfile.STANDARD;
    }

    private String buildMatchLabel(Matchs match, Map<Integer, Equipe> teamById) {
        if (match == null) {
            return "Match";
        }

        String home = resolveTeamName(teamById, match.getEquipeDomicileId(), "Home team");
        String away = resolveTeamName(teamById, match.getEquipeExterieurId(), "Away team");
        return home + " vs " + away;
    }

    private Matchs findTeamFixtureOnDate(List<Matchs> fixtures, LocalDate targetDate) {
        if (fixtures == null || fixtures.isEmpty() || targetDate == null) {
            return null;
        }
        return fixtures.stream()
                .filter(match -> targetDate.equals(match.getDateMatch()))
                .sorted(Comparator.comparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private Matchs findNextTeamFixture(List<Matchs> fixtures, LocalDate today) {
        if (fixtures == null || fixtures.isEmpty()) {
            return null;
        }
        LocalDate referenceDate = today == null ? LocalDate.now() : today;
        return fixtures.stream()
                .filter(match -> match != null && match.getDateMatch() != null && !match.getDateMatch().isBefore(referenceDate))
                .sorted(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private String resolveOpponentName(Matchs match, Integer teamId, MatchListController controller) {
        if (match == null || teamId == null || controller == null) {
            return "the opponent";
        }
        if (teamId.equals(match.getEquipeDomicileId())) {
            return controller.getAssistantTeamName(match.getEquipeExterieurId());
        }
        return controller.getAssistantTeamName(match.getEquipeDomicileId());
    }

    private String extractFixtureTeamFocus(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        return normalized
                .replaceAll("\\b(when|does|do|is|are|will|next|fixture|fixtures|match|matches|matchs|game|games|play|playing|today|tomorrow|who|against|opponent|their|the|a|an|for|of|in|on|at|time|date)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String resolveTeamName(Map<Integer, Equipe> teamById, Integer teamId, String fallback) {
        if (teamId == null) {
            return fallback;
        }
        Equipe equipe = teamById.get(teamId);
        if (equipe == null || equipe.getNom() == null || equipe.getNom().isBlank()) {
            return fallback;
        }
        return equipe.getNom().trim();
    }

    private Integer findPrimaryRequestedTeamId(MatchLookupResult lookupResult) {
        Matchs match = lookupResult.match();
        if (match == null) {
            return null;
        }
        Integer requestedTeamId = lookupResult.focusTeamId();
        if (requestedTeamId != null) {
            return requestedTeamId;
        }
        return match.getEquipeDomicileId();
    }

    private void remember(AssistantMessage.Role role, String content) {
        history.add(new AssistantMessage(role, content, Instant.now()));
        if (history.size() > 40) {
            List<AssistantMessage> trimmed = new ArrayList<>(history.subList(history.size() - 40, history.size()));
            history.clear();
            history.addAll(trimmed);
        }
    }

    private String loadKnowledgeBase() {
        try (InputStream inputStream = AssistantService.class.getResourceAsStream("/tn/esprit/assistant/assistant-knowledge.md")) {
            if (inputStream == null) {
                return "Sport Insight contains football management modules for users, admins, teams, players, matches, announcements, training, sponsors, and store operations.";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "Sport Insight contains football management modules for users, admins, teams, players, matches, announcements, training, sponsors, and store operations.";
        }
    }

    private static boolean looksLikeNavigationRequest(String normalized) {
        return containsAny(normalized, "open", "go to", "goto", "show", "take me", "bring me", "switch to", "navigate");
    }

    private static boolean looksLikeCompetitionNavigationRequest(String normalized) {
        return looksLikeNavigationRequest(normalized)
                || containsAny(normalized, "fixtures", "calendar", "schedule", "match page", "matches page");
    }

    private static boolean looksLikeFixtureTimingQuestion(String normalized) {
        return containsAny(normalized,
                "next fixture",
                "next fixtures",
                "next match",
                "next game",
                "play today",
                "playing today",
                "who do",
                "who does",
                "who is",
                "when does",
                "when do",
                "when is",
                "fixture today",
                "game today")
                && containsAny(normalized, "fixture", "fixtures", "match", "matches", "matchs", "game", "games", "play", "playing", "opponent", "against", "today", "next");
    }

    private static boolean looksLikeTodayFixtureQuestion(String normalized) {
        return containsAny(normalized, "today", "tonight", "this evening");
    }

    private static boolean looksLikeOpponentQuestion(String normalized) {
        return containsAny(normalized, "who do", "who does", "opponent", "against", "play next");
    }

    private static boolean looksLikeCompetitionStandingsRequest(String normalized) {
        return containsAny(
                normalized,
                "standing",
                "standings",
                "league table",
                "table",
                "classement",
                "ranking",
                "leaderboard"
        ) && containsAny(
                normalized,
                "open",
                "show",
                "take me",
                "bring me",
                "go to",
                "goto",
                "navigate",
                "page"
        );
    }

    private static boolean looksLikeSpecificMatchNavigationRequest(String normalized) {
        boolean hasDelimiter = MATCH_DELIMITERS.stream().anyMatch(normalized::contains);
        boolean hasDetailLanguage = containsAny(normalized, "detail", "details", "page", "screen", "fiche", "show", "open", "want", "need", "search", "find", "take me");
        return hasDelimiter && hasDetailLanguage;
    }

    private static boolean looksLikeSingleTeamNavigationRequest(String normalized) {
        return containsAny(normalized, "last", "latest", "recent", "search", "find", "look for", "show", "open")
                && containsAny(normalized, "match", "matches", "matchs", "fixture", "fixtures", "game", "games");
    }

    private static boolean looksLikeExplanationRequest(String normalized) {
        return containsAny(normalized, "how", "what", "explain", "help", "guide", "walk me");
    }

    private static boolean looksLikePredictionQuestion(String normalized) {
        return containsAny(
                normalized,
                "predict",
                "prediction",
                "who will win",
                "who s winning",
                "winner",
                "win this game",
                "win this match",
                "forecast",
                "favorite",
                "favourite",
                "expected score",
                "score prediction"
        );
    }

    private static boolean looksLikeMvpQuestion(String normalized) {
        return containsAny(normalized, "mvp", "man of the match", "player of the match", "best player", "star player");
    }

    private static boolean looksLikeScoreQuestion(String normalized) {
        return containsAny(normalized, "score", "result", "who is winning", "who won", "what s the score", "what is the score");
    }

    private static boolean looksLikeScorerQuestion(String normalized) {
        return containsAny(normalized, "who scored", "scorer", "scorers", "goalscorer", "goal scorers", "goal events");
    }

    private static boolean looksLikeCardQuestion(String normalized) {
        return containsAny(normalized, "yellow card", "red card", "cards", "booked", "booking", "sent off");
    }

    private static boolean looksLikeStatisticQuestion(String normalized) {
        return containsAny(normalized,
                "stat", "stats", "statistics", "possession", "shots", "passes", "corners",
                "fouls", "offsides", "xg", "expected goals", "saves", "duels", "accuracy");
    }

    private static boolean looksLikeLineupQuestion(String normalized) {
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
                "name the lineup",
                "name me the lineup",
                "name the starting",
                "name me the starting",
                "which players",
                "who is in the lineup",
                "who s in the lineup"
        );
    }

    private static boolean looksLikeDeepReasoningRequest(String rawPrompt, String normalizedPrompt) {
        return countTokens(normalizedPrompt) > 24
                || (rawPrompt != null && rawPrompt.length() > 180)
                || containsAny(
                normalizedPrompt,
                "why",
                "explain",
                "compare",
                "analysis",
                "analyze",
                "analyse",
                "difference",
                "differences",
                "best way",
                "step by step",
                "workflow",
                "guide me",
                "advantages",
                "disadvantages",
                "architecture",
                "summarize",
                "summary"
        );
    }

    private static boolean containsAny(String source, String... terms) {
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

    private static String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int countTokens(String value) {
        return tokens(value).size();
    }

    private static Map<String, String> createCompetitionAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("uefa champions league", "CL");
        aliases.put("champions league", "CL");
        aliases.put("ucl", "CL");
        aliases.put("premier league", "PL");
        aliases.put("epl", "PL");
        aliases.put("la liga", "PD");
        aliases.put("laliga", "PD");
        aliases.put("bundesliga", "BL1");
        aliases.put("serie a", "SA");
        aliases.put("ligue 1", "FL1");
        for (Map.Entry<String, String> entry : FootballDataCompetitions.labels().entrySet()) {
            aliases.put(normalize(entry.getValue()), entry.getKey());
        }
        return aliases;
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\s+"));
    }

    public static String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        String normalized = Normalizer.normalize(rawText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static Context contextFor(String fxmlPath, String title, Object controller) {
        User user = AuthSession.getCurrentUser();
        return new Context(
                fxmlPath == null ? "" : fxmlPath,
                title == null ? "" : title,
                AuthSession.isAuthenticated(),
                AuthSession.isAdmin(),
                user == null ? "Guest" : user.getDisplayName(),
                controller
        );
    }

    private String resolvePreferredModel(String storedModel) {
        if (storedModel == null || storedModel.isBlank() || LEGACY_MODEL.equalsIgnoreCase(storedModel.trim())) {
            preferences.put(PREF_MODEL, DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        return storedModel.trim();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    public record Reply(String text, AssistantCommand command, boolean localHandled) {
    }

    public record Context(
            String fxmlPath,
            String title,
            boolean authenticated,
            boolean admin,
            String displayName,
            Object controller
    ) {
    }

    private record TeamPairQuery(String homeQuery, String awayQuery) {
    }

    private record TeamCandidate(Equipe team, int score) {
    }

    private record MatchLookupResult(Matchs match, Map<Integer, Equipe> teamById, Integer focusTeamId) {
    }

    private record TeamPredictionSnapshot(
            String teamName,
            int recentMatches,
            double pointsPerMatch,
            double goalDiffPerMatch,
            double goalsForPerMatch,
            double goalsAgainstPerMatch,
            int venueMatches,
            double venuePointsPerMatch,
            int competitionMatches,
            double competitionPointsPerMatch
    ) {
    }

    private record MatchPredictionInsight(
            String favoredTeamName,
            double homeWinProbability,
            double drawProbability,
            double awayWinProbability,
            int predictedHomeGoals,
            int predictedAwayGoals,
            List<String> reasons,
            boolean drawFavorite
    ) {
        double favoredProbability() {
            return drawFavorite ? drawProbability : Math.max(homeWinProbability, awayWinProbability);
        }
    }

    private enum CurrentMatchTeamSide {
        HOME,
        AWAY
    }

    private enum InteractionMode {
        TEXT,
        VOICE
    }
}
