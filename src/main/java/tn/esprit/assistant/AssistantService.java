package tn.esprit.assistant;

import tn.esprit.Controller.MatchDetailController;
import tn.esprit.Controller.MatchListController;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.security.AuthSession;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;
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
    private static final String LEGACY_MODEL = "llama3.2:1b";
    private static final String DEFAULT_MODEL = "qwen2.5:3b";
    private static final int MIN_TEAM_MATCH_SCORE = 45;
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
                "I'm the Sport Insight assistant. I can open modules, jump into competition pages, open exact match details, explain the current screen, and talk back with local voice models.",
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
        voiceInputService.startRecording();
    }

    public CompletableFuture<String> stopVoiceRecording(java.util.function.Consumer<String> statusConsumer) {
        return voiceInputService.stopRecordingAndTranscribe(statusConsumer);
    }

    public String runtimeStatus(Context context) {
        AssistantScreenCatalog.ScreenMeta screenMeta = AssistantScreenCatalog.resolve(context.fxmlPath());
        OllamaClient.Status status = ollamaClient.status(preferredModel);
        String modelStatus = status.reachable()
                ? (status.selectedModel() == null
                    ? "Local AI is reachable but no model is installed."
                    : "Local AI ready on " + status.selectedModel() + ".")
                : "Local AI offline. Install Ollama and pull " + preferredModel + ".";
        return screenMeta.title() + " active. " + modelStatus + " Mic input uses local Whisper with a Vosk fallback. " + voiceOutputService.statusSummary();
    }

    public CompletableFuture<Reply> submit(String rawPrompt, Context context) {
        String prompt = rawPrompt == null ? "" : rawPrompt.trim();
        if (prompt.isBlank()) {
            return CompletableFuture.completedFuture(new Reply(
                    "Ask me something about Sport Insight, for example: open teams, open Champions League matches, or open Bayern vs Real Madrid details.",
                    null,
                    true
            ));
        }

        remember(AssistantMessage.Role.USER, prompt);
        return CompletableFuture.supplyAsync(() -> buildReply(prompt, context), executor)
                .thenApply(reply -> {
                    remember(AssistantMessage.Role.ASSISTANT, reply.text());
                    if (speakReplies) {
                        voiceOutputService.speakAsync(reply.text());
                    }
                    return reply;
                });
    }

    private Reply buildReply(String prompt, Context context) {
        String normalized = normalize(prompt);
        AssistantScreenCatalog.ScreenMeta currentScreen = AssistantScreenCatalog.resolve(context.fxmlPath());

        Optional<Reply> localReply = tryHandleLocally(normalized, context, currentScreen);
        if (localReply.isPresent()) {
            return localReply.get();
        }

        List<AssistantMessage> snapshot = historySnapshot();
        String systemPrompt = buildSystemPrompt(context, currentScreen);
        try {
            String modelReply = ollamaClient.chat(preferredModel, systemPrompt, snapshot);
            if (modelReply != null && !modelReply.isBlank()) {
                return new Reply(modelReply, null, false);
            }
        } catch (Exception ignored) {
            // Fall through to the local fallback.
        }

        return new Reply(buildOfflineFallback(currentScreen), null, true);
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
                    "I'm your local Sport Insight copilot. I can explain screens, open modules, jump to competition pages, open exact match details, and talk with local voice models.",
                    null,
                    true
            ));
        }

        if (containsAny(normalized, "module", "modules", "feature", "features", "section", "sections")) {
            return Optional.of(new Reply(listModules(), null, true));
        }

        if (containsAny(normalized, "voice", "mic", "microphone", "speech")) {
            return Optional.of(new Reply(
                    "Voice chat is fully local. The mic now uses Whisper for higher-quality offline transcription, with the older Vosk path kept as a fallback. Spoken replies use Piper with the " + voiceOutputService.voiceLabel() + " neural voice. Everything stays local with no paid API.",
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

    private Optional<Reply> tryHandleMatchListPageActions(String normalized, Context context, MatchListController controller) {
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

    private String buildSystemPrompt(Context context, AssistantScreenCatalog.ScreenMeta currentScreen) {
        return """
                You are Sport Insight's built-in assistant inside a JavaFX desktop football management application.

                Rules:
                - Focus only on Sport Insight, its modules, screens, workflows, roles, and local setup.
                - If the user asks something unrelated to the application, say you only help with Sport Insight.
                - Never invent live database values or external API results you cannot see.
                - Keep answers concise, practical, and navigation-aware.
                - When the user asks how to do something, explain the nearest Sport Insight workflow.
                - Respect role boundaries: admin-only features require an admin account.
                - Never claim that you opened a page or changed data unless the local action layer already did it.
                - When the user asks to open a page or act on the current screen, prefer the local action layer over a descriptive answer.

                Current session:
                - Screen: %s
                - Screen summary: %s
                - Authenticated: %s
                - Admin: %s
                - User: %s

                App knowledge:
                %s
                """.formatted(
                currentScreen.title(),
                currentScreen.description(),
                context.authenticated(),
                context.admin(),
                context.displayName(),
                knowledgeBase
        );
    }

    private String buildOfflineFallback(AssistantScreenCatalog.ScreenMeta currentScreen) {
        return describeCurrentScreen(currentScreen)
                + "\n\nFor richer answers, start Ollama locally and pull "
                + preferredModel
                + ". The assistant still keeps local page navigation and voice features offline with no paid API.";
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

    private String buildMatchLabel(Matchs match, Map<Integer, Equipe> teamById) {
        if (match == null) {
            return "Match";
        }

        String home = resolveTeamName(teamById, match.getEquipeDomicileId(), "Home team");
        String away = resolveTeamName(teamById, match.getEquipeExterieurId(), "Away team");
        return home + " vs " + away;
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
}
