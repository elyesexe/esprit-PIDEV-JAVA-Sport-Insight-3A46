package tn.esprit.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SponsorSentimentAnalysisService {
    private static final Map<String, Integer> POSITIVE_SIGNALS = buildPositiveSignals();
    private static final Map<String, Integer> NEGATIVE_SIGNALS = buildNegativeSignals();
    private static final Map<String, List<String>> TOPIC_HINTS = buildTopicHints();
    private static final List<String> NEGATIONS = List.of(
            "pas",
            "non",
            "jamais",
            "aucun",
            "aucune",
            "sans",
            "not",
            "never",
            "no"
    );
    private static final List<String> URGENCY_SIGNALS = List.of(
            "urgent",
            "rapidement",
            "immediat",
            "immediate",
            "asap",
            "deadline",
            "critique",
            "critical"
    );

    public SponsorSentimentAnalysis analyze(String rawMessage, String sponsorName) {
        String message = trimToNull(rawMessage);
        if (message == null) {
            return SponsorSentimentAnalysis.empty();
        }

        String normalized = normalizeForAnalysis(message);
        SignalScore positiveScore = scoreSignals(normalized, POSITIVE_SIGNALS, true);
        SignalScore negativeScore = scoreSignals(normalized, NEGATIVE_SIGNALS, false);
        List<String> topics = detectTopics(normalized);
        boolean urgent = containsAny(normalized, URGENCY_SIGNALS);

        int score = clamp((positiveScore.score() - negativeScore.score()) * 10, -100, 100);
        Sentiment sentiment = resolveSentiment(positiveScore.score(), negativeScore.score(), score);
        double confidence = computeConfidence(positiveScore.score(), negativeScore.score(), sentiment);
        String priority = resolvePriority(sentiment, normalized, urgent);
        List<String> actions = buildActions(sentiment, topics, urgent, negativeScore.evidence(), positiveScore.evidence());
        String summary = buildSummary(sentiment, score, topics, priority);
        String responseDraft = buildResponseDraft(sentiment, sponsorName, topics, urgent);

        return new SponsorSentimentAnalysis(
                sentiment,
                score,
                confidence,
                priority,
                topics,
                positiveScore.evidence(),
                negativeScore.evidence(),
                actions,
                summary,
                responseDraft
        );
    }

    private SignalScore scoreSignals(String normalized, Map<String, Integer> signals, boolean positiveSignals) {
        int score = 0;
        Set<String> evidence = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : signals.entrySet()) {
            String term = entry.getKey();
            int fromIndex = 0;
            while (fromIndex >= 0 && fromIndex < normalized.length()) {
                int index = normalized.indexOf(term, fromIndex);
                if (index < 0) {
                    break;
                }
                if (isWordBoundary(normalized, index - 1) && isWordBoundary(normalized, index + term.length())) {
                    if (positiveSignals && isNegated(normalized, index)) {
                        score -= entry.getValue() + 1;
                        evidence.add("negation: " + term);
                    } else {
                        score += entry.getValue();
                        evidence.add(term);
                    }
                }
                fromIndex = index + term.length();
            }
        }
        if (score < 0) {
            return new SignalScore(0, List.of());
        }
        return new SignalScore(score, new ArrayList<>(evidence));
    }

    private boolean isNegated(String normalized, int termStart) {
        int windowStart = Math.max(0, termStart - 28);
        String before = normalized.substring(windowStart, termStart);
        return NEGATIONS.stream().anyMatch(negation -> before.contains(" " + negation + " "));
    }

    private boolean isWordBoundary(String normalized, int index) {
        if (index < 0 || index >= normalized.length()) {
            return true;
        }
        char c = normalized.charAt(index);
        return !Character.isLetterOrDigit(c);
    }

    private Sentiment resolveSentiment(int positiveScore, int negativeScore, int score) {
        int margin = Math.abs(positiveScore - negativeScore);
        if (positiveScore + negativeScore == 0 || margin <= 1 || Math.abs(score) < 15) {
            return Sentiment.NEUTRAL;
        }
        return score > 0 ? Sentiment.POSITIVE : Sentiment.NEGATIVE;
    }

    private double computeConfidence(int positiveScore, int negativeScore, Sentiment sentiment) {
        int total = positiveScore + negativeScore;
        if (total == 0) {
            return sentiment == Sentiment.NEUTRAL ? 0.52 : 0.45;
        }
        int margin = Math.abs(positiveScore - negativeScore);
        double marginFactor = Math.min(0.28, (margin / (double) Math.max(1, total)) * 0.35);
        double volumeFactor = Math.min(0.14, total / 60.0);
        return Math.min(0.95, 0.55 + marginFactor + volumeFactor);
    }

    private List<String> detectTopics(String normalized) {
        List<String> topics = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : TOPIC_HINTS.entrySet()) {
            if (containsAny(normalized, entry.getValue())) {
                topics.add(entry.getKey());
            }
        }
        return topics;
    }

    private boolean containsAny(String normalized, List<String> values) {
        if (normalized == null || values == null) {
            return false;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeForAnalysis)
                .anyMatch(value -> normalized.contains(value));
    }

    private String resolvePriority(Sentiment sentiment, String normalized, boolean urgent) {
        if (sentiment == Sentiment.NEGATIVE && (urgent || normalized.contains(" annuler ") || normalized.contains(" resilier "))) {
            return "HIGH";
        }
        if (sentiment == Sentiment.NEGATIVE) {
            return "MEDIUM";
        }
        if (sentiment == Sentiment.POSITIVE && (normalized.contains(" renouvel") || normalized.contains(" renew"))) {
            return "OPPORTUNITY";
        }
        return "NORMAL";
    }

    private List<String> buildActions(
            Sentiment sentiment,
            List<String> topics,
            boolean urgent,
            List<String> negativeEvidence,
            List<String> positiveEvidence
    ) {
        List<String> actions = new ArrayList<>();
        if (sentiment == Sentiment.NEGATIVE) {
            actions.add(urgent
                    ? "Repondre aujourd'hui avec reconnaissance du probleme et prochaine etape claire."
                    : "Repondre sous 24h avec empathie et reformulation du point bloque.");
            if (!topics.isEmpty()) {
                actions.add("Traiter explicitement: " + String.join(", ", topics) + ".");
            }
            if (!negativeEvidence.isEmpty()) {
                actions.add("Preparer une action corrective liee aux signaux: " + String.join(", ", negativeEvidence) + ".");
            }
            actions.add("Proposer un appel court pour aligner attentes, calendrier et responsabilites.");
            return actions;
        }

        if (sentiment == Sentiment.POSITIVE) {
            actions.add("Remercier le sponsor et identifier ce qui a cree la satisfaction.");
            if (positiveEvidence.stream().anyMatch(signal -> signal.contains("renouvel") || signal.contains("renew"))) {
                actions.add("Proposer une discussion de renouvellement avec chiffres de visibilite et options de pack.");
            } else {
                actions.add("Envoyer un recap des resultats et une prochaine activation concrete.");
            }
            if (!topics.isEmpty()) {
                actions.add("Capitaliser sur: " + String.join(", ", topics) + ".");
            }
            return actions;
        }

        actions.add("Clarifier la demande et confirmer le besoin exact du sponsor.");
        actions.add("Envoyer un recap court avec delai, responsable et prochaine action.");
        if (!topics.isEmpty()) {
            actions.add("Joindre les informations attendues sur: " + String.join(", ", topics) + ".");
        }
        return actions;
    }

    private String buildSummary(Sentiment sentiment, int score, List<String> topics, String priority) {
        String topicText = topics.isEmpty() ? "aucun sujet dominant" : String.join(", ", topics);
        return sentiment.label()
                + " | score "
                + score
                + "/100 | priorite "
                + priority
                + " | "
                + topicText;
    }

    private String buildResponseDraft(Sentiment sentiment, String sponsorName, List<String> topics, boolean urgent) {
        String name = trimToNull(sponsorName) == null ? "cher partenaire" : sponsorName.trim();
        String topicText = topics.isEmpty() ? "votre retour" : String.join(", ", topics).toLowerCase(Locale.ROOT);
        if (sentiment == Sentiment.NEGATIVE) {
            return "Bonjour " + name + ",\n\n"
                    + "Merci pour votre retour. Nous avons bien note le point concernant " + topicText + ". "
                    + "Je vous propose de revenir vers vous " + (urgent ? "aujourd'hui" : "sous 24h")
                    + " avec une action corrective claire et un calendrier de suivi.\n\n"
                    + "Cordialement,";
        }
        if (sentiment == Sentiment.POSITIVE) {
            return "Bonjour " + name + ",\n\n"
                    + "Merci pour ce retour positif. Nous sommes ravis que la collaboration apporte de la valeur, "
                    + "notamment sur " + topicText + ". Je vous envoie un recap des resultats et une proposition "
                    + "pour la prochaine activation.\n\n"
                    + "Cordialement,";
        }
        return "Bonjour " + name + ",\n\n"
                + "Merci pour votre message. Pour bien avancer sur " + topicText
                + ", pouvez-vous confirmer le besoin prioritaire et le delai attendu ? "
                + "Je vous enverrai ensuite un recap avec les prochaines etapes.\n\n"
                + "Cordialement,";
    }

    private String normalizeForAnalysis(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return " " + withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Map<String, Integer> buildPositiveSignals() {
        Map<String, Integer> signals = new LinkedHashMap<>();
        signals.put("tres satisfait", 5);
        signals.put("satisfait", 4);
        signals.put("satisfaits", 4);
        signals.put("excellent", 5);
        signals.put("excellente", 5);
        signals.put("parfait", 5);
        signals.put("parfaite", 5);
        signals.put("merci", 2);
        signals.put("content", 3);
        signals.put("heureux", 3);
        signals.put("positif", 3);
        signals.put("bonne collaboration", 4);
        signals.put("collaboration", 2);
        signals.put("confiance", 3);
        signals.put("professionnel", 3);
        signals.put("rapide", 2);
        signals.put("valeur", 3);
        signals.put("visibilite", 3);
        signals.put("renouveler", 5);
        signals.put("renouvellement", 5);
        signals.put("interesse", 4);
        signals.put("recommander", 4);
        signals.put("happy", 4);
        signals.put("satisfied", 4);
        signals.put("great", 4);
        signals.put("good", 2);
        signals.put("valuable", 3);
        signals.put("renew", 5);
        signals.put("interested", 4);
        signals.put("clear", 2);
        return signals;
    }

    private static Map<String, Integer> buildNegativeSignals() {
        Map<String, Integer> signals = new LinkedHashMap<>();
        signals.put("pas satisfait", 6);
        signals.put("non satisfait", 6);
        signals.put("decu", 5);
        signals.put("decus", 5);
        signals.put("deception", 5);
        signals.put("insatisfait", 5);
        signals.put("probleme", 4);
        signals.put("plainte", 5);
        signals.put("retard", 4);
        signals.put("aucun retour", 5);
        signals.put("pas de retour", 5);
        signals.put("mauvaise", 4);
        signals.put("manque", 3);
        signals.put("inquiet", 4);
        signals.put("confus", 3);
        signals.put("cher", 3);
        signals.put("annuler", 6);
        signals.put("resilier", 6);
        signals.put("rupture", 5);
        signals.put("negative", 4);
        signals.put("unhappy", 5);
        signals.put("not satisfied", 6);
        signals.put("disappointed", 5);
        signals.put("issue", 4);
        signals.put("problem", 4);
        signals.put("delay", 4);
        signals.put("late", 3);
        signals.put("complaint", 5);
        signals.put("poor", 4);
        signals.put("angry", 5);
        signals.put("cancel", 6);
        signals.put("terminate", 6);
        return signals;
    }

    private static Map<String, List<String>> buildTopicHints() {
        Map<String, List<String>> topics = new LinkedHashMap<>();
        topics.put("VISIBILITY", List.of("visibilite", "visibility", "logo", "affichage", "exposition", "exposure", "banner"));
        topics.put("PAYMENT", List.of("paiement", "payment", "facture", "invoice", "budget", "montant", "prix", "cher"));
        topics.put("DELAY", List.of("retard", "delay", "late", "deadline", "attente", "waiting"));
        topics.put("COMMUNICATION", List.of("retour", "reponse", "communication", "email", "appel", "call", "message"));
        topics.put("CONTRACT", List.of("contrat", "contract", "renouveler", "renouvellement", "renew", "resilier", "annuler", "cancel"));
        topics.put("EVENT", List.of("match", "evenement", "event", "activation", "campagne", "campaign"));
        return topics;
    }

    private record SignalScore(int score, List<String> evidence) {
    }

    public enum Sentiment {
        POSITIVE("POSITIF"),
        NEUTRAL("NEUTRE"),
        NEGATIVE("NEGATIF");

        private final String label;

        Sentiment(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record SponsorSentimentAnalysis(
            Sentiment sentiment,
            int score,
            double confidence,
            String priority,
            List<String> topics,
            List<String> positiveSignals,
            List<String> negativeSignals,
            List<String> recommendedActions,
            String summary,
            String responseDraft
    ) {
        public static SponsorSentimentAnalysis empty() {
            return new SponsorSentimentAnalysis(
                    Sentiment.NEUTRAL,
                    0,
                    0.0,
                    "NORMAL",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("Saisir un message sponsor avant l'analyse."),
                    "NEUTRE | score 0/100 | aucun message",
                    ""
            );
        }
    }
}
