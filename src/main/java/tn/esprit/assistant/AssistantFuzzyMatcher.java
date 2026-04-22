package tn.esprit.assistant;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

final class AssistantFuzzyMatcher {
    private AssistantFuzzyMatcher() {
    }

    static double similarity(String left, String right) {
        String normalizedLeft = normalizeBasic(left);
        String normalizedRight = normalizeBasic(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return 0.0;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return 1.0;
        }

        String compactLeft = normalizedLeft.replace(" ", "");
        String compactRight = normalizedRight.replace(" ", "");
        double compactScore = editSimilarity(compactLeft, compactRight);
        double coreCompactScore = editSimilarity(compact(removeShortTokens(normalizedLeft)), compact(removeShortTokens(normalizedRight)));
        double phoneticScore = editSimilarity(phoneticKey(normalizedLeft), phoneticKey(normalizedRight));
        double tokenScore = tokenOverlap(normalizedLeft, normalizedRight);
        double prefixScore = tokenPrefixScore(normalizedLeft, normalizedRight);
        double tokenFuzzyScore = tokenFuzzyScore(normalizedLeft, normalizedRight);
        double tokenPenalty = Math.min(0.06, Math.abs(tokens(normalizedLeft).size() - tokens(normalizedRight).size()) * 0.02);
        double weightedScore = compactScore * 0.24
                + coreCompactScore * 0.16
                + phoneticScore * 0.26
                + tokenScore * 0.08
                + prefixScore * 0.06
                + tokenFuzzyScore * 0.20
                - tokenPenalty;
        double phoneticTokenScore = (phoneticScore * 0.55) + (tokenFuzzyScore * 0.45);
        double boostedScore = Math.max(weightedScore, phoneticTokenScore);
        if (prefixScore >= 0.5 && Math.min(tokens(normalizedLeft).size(), tokens(normalizedRight).size()) >= 2) {
            boostedScore += 0.03;
        }
        return clamp(boostedScore);
    }

    static List<String> tokens(String value) {
        String normalized = normalizeBasic(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    static String normalizeBasic(String rawText) {
        if (rawText == null) {
            return "";
        }
        return Normalizer.normalize(rawText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String phoneticKey(String rawText) {
        String normalized = normalizeBasic(rawText)
                .replaceAll("\\b(de|da|di|la|le|el)\\b", " ")
                .replace("ph", "f")
                .replace("ck", "k")
                .replace("cq", "k")
                .replace("qu", "k")
                .replace("q", "k")
                .replace("x", "ks")
                .replace("mb", "b")
                .replace("ght", "t")
                .replace("gh", "g")
                .replace("ou", "u")
                .replace("y", "i")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return "";
        }

        StringBuilder signature = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            appendTokenSignature(signature, token);
        }
        return signature.toString();
    }

    private static void appendTokenSignature(StringBuilder signature, String token) {
        char previous = 0;
        for (int index = 0; index < token.length(); index++) {
            char current = token.charAt(index);
            if (index > 0 && isIgnoredPhoneticChar(current)) {
                continue;
            }
            if (current == previous) {
                continue;
            }
            signature.append(current);
            previous = current;
        }
    }

    private static boolean isIgnoredPhoneticChar(char current) {
        return current == 'a'
                || current == 'e'
                || current == 'i'
                || current == 'o'
                || current == 'u'
                || current == 'h';
    }

    private static double tokenOverlap(String left, String right) {
        List<String> leftTokens = tokens(left);
        List<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }

        long matched = leftTokens.stream()
                .filter(token -> token.length() > 1)
                .filter(token -> rightTokens.contains(token) || right.contains(token))
                .count();
        return matched == 0 ? 0.0 : matched / (double) Math.max(leftTokens.size(), rightTokens.size());
    }

    private static double tokenPrefixScore(String left, String right) {
        List<String> leftTokens = tokens(left);
        List<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }

        int comparisons = Math.min(leftTokens.size(), rightTokens.size());
        if (comparisons == 0) {
            return 0.0;
        }

        int matched = 0;
        for (int index = 0; index < comparisons; index++) {
            String leftToken = leftTokens.get(index);
            String rightToken = rightTokens.get(index);
            if (!leftToken.isBlank() && !rightToken.isBlank() && leftToken.charAt(0) == rightToken.charAt(0)) {
                matched++;
            }
        }
        return matched / (double) comparisons;
    }

    private static double tokenFuzzyScore(String left, String right) {
        List<String> leftTokens = tokens(left);
        List<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }

        List<String> shorter = leftTokens.size() <= rightTokens.size() ? leftTokens : rightTokens;
        List<String> longer = shorter == leftTokens ? rightTokens : leftTokens;
        double total = 0.0;
        for (String token : shorter) {
            double best = 0.0;
            for (String candidate : longer) {
                double score = (editSimilarity(token, candidate) * 0.55)
                        + (editSimilarity(phoneticKey(token), phoneticKey(candidate)) * 0.45);
                best = Math.max(best, score);
            }
            total += best;
        }
        return total / shorter.size();
    }

    private static double editSimilarity(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }
        int longest = Math.max(left.length(), right.length());
        if (longest == 0) {
            return 1.0;
        }
        int distance = levenshtein(left, right);
        return clamp(1.0 - (distance / (double) longest));
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + substitutionCost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static String removeShortTokens(String rawText) {
        return tokens(rawText).stream()
                .filter(token -> token.length() > 2)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static String compact(String rawText) {
        return normalizeBasic(rawText).replace(" ", "");
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
