package tn.esprit.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class LiteralTranslationCatalog {
    private static final String RESOURCE_PATH = "/i18n/literal-translations.json";

    private final Map<String, String> frenchByLiteral;
    private final Map<String, String> englishByLiteral;

    private LiteralTranslationCatalog(Map<String, String> frenchByLiteral, Map<String, String> englishByLiteral) {
        this.frenchByLiteral = frenchByLiteral;
        this.englishByLiteral = englishByLiteral;
    }

    static LiteralTranslationCatalog load() {
        try (InputStream input = LiteralTranslationCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                return empty();
            }
            ObjectMapper mapper = new ObjectMapper();
            List<LiteralTranslationEntry> entries = mapper.readValue(input, new TypeReference<>() {});
            return fromEntries(entries);
        } catch (Exception ignored) {
            return empty();
        }
    }

    String translate(String text, Locale locale) {
        if (!isTranslatable(text)) {
            return text;
        }
        String normalized = normalizeKey(text);
        Map<String, String> translations = Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage())
                ? englishByLiteral
                : frenchByLiteral;
        return translations.getOrDefault(normalized, text);
    }

    private static LiteralTranslationCatalog fromEntries(List<LiteralTranslationEntry> entries) {
        Map<String, String> frenchByLiteral = new HashMap<>();
        Map<String, String> englishByLiteral = new HashMap<>();

        for (LiteralTranslationEntry entry : entries == null ? List.<LiteralTranslationEntry>of() : new ArrayList<>(entries)) {
            register(frenchByLiteral, entry.literal(), entry.fr());
            register(frenchByLiteral, entry.fr(), entry.fr());
            register(frenchByLiteral, entry.en(), entry.fr());

            register(englishByLiteral, entry.literal(), entry.en());
            register(englishByLiteral, entry.fr(), entry.en());
            register(englishByLiteral, entry.en(), entry.en());
        }

        return new LiteralTranslationCatalog(frenchByLiteral, englishByLiteral);
    }

    private static void register(Map<String, String> target, String key, String value) {
        if (!isTranslatable(key) || value == null || value.isBlank()) {
            return;
        }
        target.putIfAbsent(normalizeKey(key), decodeEntities(value).trim());
    }

    private static LiteralTranslationCatalog empty() {
        return new LiteralTranslationCatalog(Map.of(), Map.of());
    }

    private static boolean isTranslatable(String value) {
        if (value == null) {
            return false;
        }
        String normalized = decodeEntities(value).trim();
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.codePoints().anyMatch(Character::isLetter);
    }

    private static String normalizeKey(String value) {
        return decodeEntities(value)
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String decodeEntities(String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value == null ? "" : value;
        }
        String decoded = value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
        return decoded
                .replace("&#9728;", "\u2600")
                .replace("&#9790;", "\u263E");
    }

    private record LiteralTranslationEntry(String literal, String fr, String en) {
    }
}
