package tn.esprit.services;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommentSentimentService {
    private static final Map<String, Integer> LEXICON = new HashMap<>();
    private static final Set<String> NEGATIONS = new HashSet<>();
    private static final Set<String> BOOSTERS = new HashSet<>();

    static {
        registerPositive(3, "excellent", "excellente", "genial", "geniale", "super", "top", "amazing", "great", "good", "parfait", "parfaite", "incroyable", "bravo");
        registerPositive(2, "bien", "cool", "propre", "solide", "satisfait", "satisfaite", "utile", "helpful", "nice", "love", "aime", "adore", "rapide", "fluide");
        registerPositive(1, "correct", "ok", "acceptable", "interessant", "pratique", "positif", "positive");

        registerNegative(-3, "horrible", "nul", "nulle", "terrible", "catastrophique", "awful", "bad", "hate", "arnaque", "lent", "lente", "bug", "bugs");
        registerNegative(-2, "mauvais", "mauvaise", "decevant", "decevante", "probleme", "problem", "slow", "grave", "frustrant", "frustrante", "erreur");
        registerNegative(-1, "bof", "mitige", "mitigee", "moyen", "moyenne", "confus", "confuse", "negative", "negatif");

        NEGATIONS.addAll(Set.of("ne", "pas", "jamais", "plus", "aucun", "aucune", "non", "not", "never", "no"));
        BOOSTERS.addAll(Set.of("tres", "vraiment", "extremement", "super", "trop", "very", "really", "so"));
    }

    public SentimentResult analyze(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return new SentimentResult("Neutral", "status-muted", 0, "No sentiment detected.");
        }

        String[] tokens = normalized.split("\\s+");
        int score = 0;
        int hits = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            Integer weight = LEXICON.get(token);
            if (weight == null) {
                continue;
            }

            int adjusted = weight;
            if (i > 0 && NEGATIONS.contains(tokens[i - 1])) {
                adjusted = -adjusted;
            }
            if (i > 0 && BOOSTERS.contains(tokens[i - 1])) {
                adjusted += Integer.signum(adjusted);
            }

            score += adjusted;
            hits++;
        }

        if (hits == 0) {
            return new SentimentResult("Neutral", "status-muted", 0, "No sentiment keywords found.");
        }
        if (score >= 2) {
            return new SentimentResult("Positive", "status-success", score, "Mostly positive opinion.");
        }
        if (score <= -2) {
            return new SentimentResult("Negative", "status-error", score, "Mostly negative opinion.");
        }
        return new SentimentResult("Neutral", "status-warning", score, "Mixed or balanced opinion.");
    }

    private static void registerPositive(int weight, String... words) {
        for (String word : words) {
            LEXICON.put(word, weight);
        }
    }

    private static void registerNegative(int weight, String... words) {
        for (String word : words) {
            LEXICON.put(word, weight);
        }
    }

    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record SentimentResult(
            String label,
            String styleClass,
            int score,
            String summary
    ) {
    }
}
