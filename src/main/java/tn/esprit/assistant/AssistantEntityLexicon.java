package tn.esprit.assistant;

import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AssistantEntityLexicon {
    private static final Duration CACHE_TTL = Duration.ofMinutes(4);
    private static final AssistantEntityLexicon INSTANCE = new AssistantEntityLexicon();
    private static final Set<String> WINDOW_STOP_WORDS = Set.of(
            "open", "show", "go", "goto", "to", "take", "me", "bring", "profile", "profiles",
            "detail", "details", "page", "screen", "player", "players", "joueur", "joueurs",
            "match", "matches", "matchs", "game", "games", "league", "leagues", "competition",
            "competitions", "table", "standings", "who", "what", "the", "a", "an", "this", "that"
    );

    private final Object cacheLock = new Object();
    private volatile EntityCache cache;

    private AssistantEntityLexicon() {
    }

    public static AssistantEntityLexicon getInstance() {
        return INSTANCE;
    }

    public String correctTranscriptEntities(String normalizedTranscript) {
        if (normalizedTranscript == null || normalizedTranscript.isBlank()) {
            return "";
        }
        return correctTranscriptWithEntries(normalizedTranscript, snapshot().phrases());
    }

    public String buildSpeechPromptHint() {
        EntityCache snapshot = snapshot();
        List<String> phrases = snapshot.phrases().stream()
                .map(PhraseEntry::phrase)
                .filter(phrase -> AssistantFuzzyMatcher.tokens(phrase).size() >= 2)
                .sorted(Comparator.comparingInt(String::length))
                .limit(18)
                .toList();
        return String.join(", ", phrases);
    }

    static String correctTranscriptWithPhrases(String normalizedTranscript, List<String> phrases) {
        if (normalizedTranscript == null || normalizedTranscript.isBlank()) {
            return "";
        }
        List<PhraseEntry> entries = phrases == null
                ? List.of()
                : phrases.stream()
                .filter(phrase -> phrase != null && !phrase.isBlank())
                .map(phrase -> new PhraseEntry(AssistantFuzzyMatcher.normalizeBasic(phrase), PhraseType.GENERIC))
                .distinct()
                .toList();
        return correctTranscriptWithEntries(normalizedTranscript, entries);
    }

    private static String correctTranscriptWithEntries(String normalizedTranscript, List<PhraseEntry> entries) {
        List<String> words = AssistantFuzzyMatcher.tokens(normalizedTranscript);
        if (words.isEmpty() || entries == null || entries.isEmpty()) {
            return AssistantFuzzyMatcher.normalizeBasic(normalizedTranscript);
        }

        List<String> corrected = new ArrayList<>();
        int index = 0;
        while (index < words.size()) {
            Replacement replacement = findBestReplacement(words, index, entries);
            if (replacement == null) {
                corrected.add(words.get(index));
                index++;
                continue;
            }

            corrected.add(replacement.phrase());
            index += replacement.windowTokenCount();
        }
        return String.join(" ", corrected).replaceAll("\\s+", " ").trim();
    }

    private static Replacement findBestReplacement(List<String> words, int startIndex, List<PhraseEntry> entries) {
        Replacement best = null;
        int maxWindow = Math.min(4, words.size() - startIndex);
        for (int windowSize = 1; windowSize <= maxWindow; windowSize++) {
            String window = String.join(" ", words.subList(startIndex, startIndex + windowSize));
            if (allStopWords(window)) {
                continue;
            }

            for (PhraseEntry entry : entries) {
                int candidateTokenCount = AssistantFuzzyMatcher.tokens(entry.phrase()).size();
                if (candidateTokenCount == 0 || Math.abs(candidateTokenCount - windowSize) > 1) {
                    continue;
                }

                if (entry.phrase().equals(window)) {
                    continue;
                }

                double similarity = AssistantFuzzyMatcher.similarity(window, entry.phrase());
                double threshold = thresholdFor(windowSize, candidateTokenCount, entry.type());
                if (similarity < threshold) {
                    continue;
                }

                Replacement candidate = new Replacement(entry.phrase(), windowSize, similarity);
                if (best == null || candidate.isBetterThan(best)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static boolean allStopWords(String window) {
        List<String> tokens = AssistantFuzzyMatcher.tokens(window);
        return !tokens.isEmpty() && tokens.stream().allMatch(WINDOW_STOP_WORDS::contains);
    }

    private static double thresholdFor(int windowSize, int candidateTokenCount, PhraseType type) {
        double threshold = candidateTokenCount >= 2 ? 0.74 : 0.90;
        if (windowSize == 1 && candidateTokenCount >= 2) {
            threshold += 0.03;
        }
        if (type == PhraseType.COMPETITION) {
            threshold -= 0.02;
        }
        return Math.max(0.74, Math.min(0.96, threshold));
    }

    private EntityCache snapshot() {
        EntityCache current = cache;
        if (current != null && current.loadedAt().isAfter(Instant.now().minus(CACHE_TTL))) {
            return current;
        }

        synchronized (cacheLock) {
            current = cache;
            if (current != null && current.loadedAt().isAfter(Instant.now().minus(CACHE_TTL))) {
                return current;
            }
            cache = loadCache();
            return cache;
        }
    }

    private EntityCache loadCache() {
        Map<String, PhraseType> phrases = new LinkedHashMap<>();
        addCompetitionPhrases(phrases);
        addTeamPhrases(phrases);
        addPlayerPhrases(phrases);

        List<PhraseEntry> entries = phrases.entrySet().stream()
                .map(entry -> new PhraseEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt((PhraseEntry entry) -> AssistantFuzzyMatcher.tokens(entry.phrase()).size())
                        .reversed()
                        .thenComparing(PhraseEntry::phrase))
                .toList();
        return new EntityCache(entries, Instant.now());
    }

    private void addCompetitionPhrases(Map<String, PhraseType> phrases) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add("champions league");
        aliases.add("uefa champions league");
        aliases.add("premier league");
        aliases.add("la liga");
        aliases.add("bundesliga");
        aliases.add("serie a");
        aliases.add("ligue 1");
        aliases.addAll(FootballDataCompetitions.labels().values().stream()
                .map(AssistantFuzzyMatcher::normalizeBasic)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        for (String alias : aliases) {
            phrases.put(alias, PhraseType.COMPETITION);
        }
    }

    private void addTeamPhrases(Map<String, PhraseType> phrases) {
        try {
            for (Equipe equipe : new EquipeService().getAll()) {
                if (equipe == null || equipe.getNom() == null || equipe.getNom().isBlank()) {
                    continue;
                }
                phrases.put(AssistantFuzzyMatcher.normalizeBasic(equipe.getNom()), PhraseType.TEAM);
            }
        } catch (Exception ignored) {
            // Team lexicon is best-effort.
        }
    }

    private void addPlayerPhrases(Map<String, PhraseType> phrases) {
        try {
            for (Joueur joueur : new JoueurService().getAll()) {
                if (joueur == null) {
                    continue;
                }

                String firstName = AssistantFuzzyMatcher.normalizeBasic(joueur.getPrenom());
                String lastName = AssistantFuzzyMatcher.normalizeBasic(joueur.getNom());
                String fullName = (firstName + " " + lastName).trim();
                if (fullName.isBlank()) {
                    continue;
                }
                phrases.put(fullName, PhraseType.PLAYER);
            }
        } catch (Exception ignored) {
            // Player lexicon is best-effort.
        }
    }

    private enum PhraseType {
        GENERIC,
        PLAYER,
        TEAM,
        COMPETITION
    }

    private record PhraseEntry(String phrase, PhraseType type) {
    }

    private record EntityCache(List<PhraseEntry> phrases, Instant loadedAt) {
    }

    private record Replacement(String phrase, int windowTokenCount, double score) {
        private boolean isBetterThan(Replacement other) {
            if (other == null) {
                return true;
            }
            if (windowTokenCount != other.windowTokenCount()) {
                return windowTokenCount > other.windowTokenCount();
            }
            return score > other.score();
        }
    }
}
