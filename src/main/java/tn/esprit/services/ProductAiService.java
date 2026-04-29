package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.entities.Product;
import tn.esprit.tools.OpenAiConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProductAiService {
    public static final String SOURCE_OPENAI = "openai";
    public static final String SOURCE_LOCAL_TEMPLATE = "local-smart-template";
    public static final String SOURCE_LOCAL_PARSER = "local-parser";
    public static final String SOURCE_OPENAI_FALLBACK = "local-openai-error";

    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile("(?:under|below|less than|moins de|budget|up to)\\s*(\\d+(?:[\\.,]\\d{1,2})?)");
    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile("(?:over|above|more than|plus de|starting at|from)\\s*(\\d+(?:[\\.,]\\d{1,2})?)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\b(XXS|XS|S|M|L|XL|XXL|XXXL|\\d{2}(?:-\\d{2})?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Map<String, List<String>> CATEGORY_HINTS = buildCategoryHints();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProductAiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return OpenAiConfig.isConfigured();
    }

    public GeneratedProductContent generateContent(ProductDraft draft) {
        GeneratedProductContent local = generateLocalContent(draft);
        if (!isConfigured()) {
            return local.withSource(SOURCE_LOCAL_TEMPLATE);
        }

        try {
            String responseText = sendPrompt(
                    """
                    You write concise ecommerce copy for a sports store.
                    Return JSON only with keys: marketingTitle, description, tags.
                    - marketingTitle: 3 to 8 words
                    - description: 2 short sentences, factual and persuasive
                    - tags: array of 4 to 8 short lowercase tags
                    """,
                    buildGenerationPrompt(draft)
            );
            GeneratedProductContent parsed = parseGeneratedContent(responseText);
            return parsed == null ? local.withSource(SOURCE_OPENAI_FALLBACK) : parsed.withSource(SOURCE_OPENAI);
        } catch (Exception e) {
            return local.withSource(SOURCE_OPENAI_FALLBACK);
        }
    }

    public SmartProductQuery interpretSearch(String query, List<Product> products) {
        String normalizedQuery = trimToNull(query);
        if (normalizedQuery == null) {
            return SmartProductQuery.empty();
        }
        return localSearchQuery(normalizedQuery, products).withSource(SOURCE_LOCAL_PARSER);
    }

    private GeneratedProductContent generateLocalContent(ProductDraft draft) {
        String name = fallback(draft.name(), "Sport product");
        String category = fallback(draft.category(), "equipment");
        String brand = fallback(draft.brand(), "trusted brand");
        String size = trimToNull(draft.size());
        String price = draft.price() == null
                ? "competitive pricing"
                : draft.price().setScale(2, RoundingMode.HALF_UP).toPlainString() + " DT";
        ProductTone tone = inferTone(draft);
        StringBuilder description = new StringBuilder();
        description.append(name)
                .append(" by ")
                .append(brand)
                .append(" is designed for ")
                .append(tone.primaryUse())
                .append(" with a focus on ")
                .append(tone.coreBenefit())
                .append(". ");
        description.append("This ")
                .append(category.toLowerCase(Locale.ROOT))
                .append(" stands out with ")
                .append(tone.secondaryBenefit());
        if (size != null) {
            description.append(", available in ").append(size);
        }
        description.append(", at ").append(price).append(".");

        Set<String> tags = new LinkedHashSet<>();
        addTag(tags, name);
        addTag(tags, category);
        addTag(tags, brand);
        addTag(tags, size);
        addTag(tags, tone.primaryUse());
        addTag(tags, tone.coreBenefit());
        addTag(tags, tone.secondaryBenefit());
        addCategoryTags(tags, category);
        addKeywordTags(tags, name);

        return new GeneratedProductContent(
                buildMarketingTitle(name, brand, tone),
                description.toString().trim(),
                new ArrayList<>(tags),
                SOURCE_LOCAL_TEMPLATE
        );
    }

    private String buildMarketingTitle(String name, String brand, ProductTone tone) {
        String normalizedName = fallback(name, "Sport product");
        String normalizedBrand = fallback(brand, "Sport");
        if (normalizedName.toLowerCase(Locale.ROOT).contains(normalizedBrand.toLowerCase(Locale.ROOT))) {
            return normalizedName + " | " + tone.titleAccent();
        }
        return normalizedBrand + " " + normalizedName + " " + tone.titleAccent();
    }

    private String buildGenerationPrompt(ProductDraft draft) {
        return """
                Product name: %s
                Category: %s
                Brand: %s
                Size: %s
                Price: %s
                """.formatted(
                fallback(draft.name(), "-"),
                fallback(draft.category(), "-"),
                fallback(draft.brand(), "-"),
                fallback(draft.size(), "-"),
                draft.price() == null ? "-" : draft.price().setScale(2, RoundingMode.HALF_UP).toPlainString() + " DT"
        );
    }

    private String buildSearchPrompt(String query, List<Product> products) {
        String categories = products.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        String brands = products.stream()
                .map(Product::getBrand)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));

        return """
                User query: %s
                Known categories: %s
                Known brands: %s
                """.formatted(query, categories, brands);
    }

    private SmartProductQuery localSearchQuery(String query, List<Product> products) {
        List<Product> safeProducts = products == null ? List.of() : products;
        String lower = query.toLowerCase(Locale.ROOT);
        String category = findCategoryMatch(lower, safeProducts);
        String brand = findBestMatch(lower, safeProducts.stream().map(Product::getBrand).toList());
        String size = findSize(lower);
        BigDecimal minPrice = findPrice(lower, MIN_PRICE_PATTERN);
        BigDecimal maxPrice = findPrice(lower, MAX_PRICE_PATTERN);
        DerivedPriceBand derivedPriceBand = derivePriceBand(lower, category, safeProducts);
        if (minPrice == null) {
            minPrice = derivedPriceBand.minPrice();
        }
        if (maxPrice == null) {
            maxPrice = derivedPriceBand.maxPrice();
        }
        boolean inStockOnly = lower.contains("available")
                || lower.contains("in stock")
                || lower.contains("disponible")
                || lower.contains("stock");

        List<String> keywords = extractKeywords(lower, category, brand, size);
        String summary = buildSummary(category, brand, size, minPrice, maxPrice, inStockOnly, keywords, derivedPriceBand.label());

        return new SmartProductQuery(query, keywords, category, brand, size, minPrice, maxPrice, inStockOnly, summary, SOURCE_LOCAL_PARSER);
    }

    private String findCategoryMatch(String lowerQuery, List<Product> products) {
        List<String> categories = products == null
                ? List.of()
                : products.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();

        String directMatch = findBestMatch(lowerQuery, categories);
        if (directMatch != null) {
            return directMatch;
        }

        for (String category : categories) {
            if (matchesCategoryHint(lowerQuery, category)) {
                return category;
            }
        }
        return null;
    }

    private GeneratedProductContent parseGeneratedContent(String rawJson) throws IOException {
        JsonNode root = objectMapper.readTree(cleanJson(rawJson));
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = root.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                String tag = trimToNull(tagNode.asText(null));
                if (tag != null) {
                    tags.add(tag.toLowerCase(Locale.ROOT));
                }
            }
        }

        String title = trimToNull(root.path("marketingTitle").asText(null));
        String description = trimToNull(root.path("description").asText(null));
        if (title == null || description == null || tags.isEmpty()) {
            return null;
        }
        return new GeneratedProductContent(title, description, tags, SOURCE_OPENAI);
    }

    private String sendPrompt(String instructions, String prompt) throws IOException, InterruptedException {
        String apiKey = OpenAiConfig.resolveApiKey();
        if (apiKey == null) {
            throw new IOException("OpenAI API key is missing.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", OpenAiConfig.resolveModel());
        ArrayNode input = payload.putArray("input");

        ObjectNode developerMessage = input.addObject();
        developerMessage.put("role", "developer");
        developerMessage.put("content", instructions);

        ObjectNode userMessage = input.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(OpenAiConfig.BASE_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI request failed with status " + response.statusCode() + ".");
        }

        JsonNode root = objectMapper.readTree(response.body());
        String outputText = extractOutputText(root);
        if (outputText == null) {
            throw new IOException("OpenAI response did not contain text output.");
        }
        return outputText;
    }

    private String extractOutputText(JsonNode root) {
        String directText = trimToNull(root.path("output_text").asText(null));
        if (directText != null) {
            return directText;
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText("");
                    if (!text.isBlank()) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(text);
                    }
                }
            }
        }
        String normalized = trimToNull(builder.toString());
        return normalized;
    }

    private boolean matchesCategoryHint(String lowerQuery, String category) {
        List<String> hints = CATEGORY_HINTS.get(normalizeCategoryKey(category));
        if (hints == null || hints.isEmpty()) {
            return false;
        }
        return hints.stream().anyMatch(lowerQuery::contains);
    }

    private String normalizeCategoryKey(String value) {
        return fallback(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, List<String>> buildCategoryHints() {
        Map<String, List<String>> hints = new LinkedHashMap<>();
        hints.put("boots", List.of("boot", "boots", "shoe", "shoes", "cleat", "cleats", "crampon", "crampons", "chaussure", "chaussures"));
        hints.put("jerseys", List.of("jersey", "jerseys", "maillot", "maillots", "shirt", "shirts", "kit", "kits", "top"));
        hints.put("balls", List.of("ball", "balls", "football", "futsal"));
        hints.put("gloves", List.of("glove", "gloves", "gk", "goalkeeper", "gardien"));
        hints.put("protection", List.of("protection", "guard", "guards", "shin", "shinpad", "shinpads", "ankle"));
        hints.put("training", List.of("training", "drill", "drills", "cone", "cones", "ladder", "bib", "bibs", "board", "tactic", "tactics"));
        hints.put("training wear", List.of("jacket", "jackets", "thermal", "base layer", "base-layer", "wear", "veste"));
        hints.put("accessories", List.of("accessory", "accessories", "bag", "backpack", "armband", "care kit", "kit"));
        return hints;
    }

    private String findBestMatch(String lowerQuery, List<String> candidates) {
        return candidates.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .filter(value -> lowerQuery.contains(value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private String findSize(String lowerQuery) {
        Matcher matcher = SIZE_PATTERN.matcher(lowerQuery.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private BigDecimal findPrice(String lowerQuery, Pattern pattern) {
        Matcher matcher = pattern.matcher(lowerQuery);
        if (!matcher.find()) {
            return null;
        }
        return parseNullableDecimal(matcher.group(1));
    }

    private DerivedPriceBand derivePriceBand(String lowerQuery, String category, List<Product> products) {
        boolean budgetIntent = containsAny(lowerQuery,
                List.of("cheap", "budget", "affordable", "economical", "low cost", "pas cher", "promo", "deal"));
        boolean premiumIntent = containsAny(lowerQuery,
                List.of("expensive", "premium", "luxury", "high end", "high-end", "top tier", "cher", "haut de gamme"));

        if (budgetIntent == premiumIntent) {
            return DerivedPriceBand.empty();
        }

        List<BigDecimal> scopedPrices = collectScopedPrices(products, category);
        if (scopedPrices.isEmpty()) {
            return DerivedPriceBand.empty();
        }

        if (budgetIntent) {
            int index = Math.min(scopedPrices.size() - 1, Math.max(0, scopedPrices.size() / 3));
            return new DerivedPriceBand(null, scopedPrices.get(index), "budget");
        }

        int index = Math.min(scopedPrices.size() - 1, Math.max(0, (scopedPrices.size() * 2) / 3));
        return new DerivedPriceBand(scopedPrices.get(index), null, "premium");
    }

    private List<BigDecimal> collectScopedPrices(List<Product> products, String category) {
        List<Product> safeProducts = products == null ? List.of() : products;
        List<BigDecimal> categoryPrices = safeProducts.stream()
                .filter(product -> product.getPrice() != null)
                .filter(product -> category == null || matchesText(product.getCategory(), category))
                .map(product -> product.getPrice().setScale(2, RoundingMode.HALF_UP))
                .sorted()
                .toList();
        if (!categoryPrices.isEmpty() || category == null) {
            return categoryPrices;
        }
        return safeProducts.stream()
                .filter(product -> product.getPrice() != null)
                .map(product -> product.getPrice().setScale(2, RoundingMode.HALF_UP))
                .sorted()
                .toList();
    }

    private BigDecimal parseNullableDecimal(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> extractKeywords(String lowerQuery, String category, String brand, String size) {
        String sanitized = lowerQuery
                .replaceAll("\\d+(?:[\\.,]\\d{1,2})?", " ")
                .replaceAll("\\b(under|below|less|than|budget|up|to|over|above|more|plus|de|from|available|stock|in|cheap|affordable|economical|promo|deal|expensive|premium|luxury|high|end|size|trendy|trend|recommended|recommendation)\\b", " ");
        List<String> keywords = new ArrayList<>();
        for (String token : sanitized.split("[^a-z0-9]+")) {
            String normalized = trimToNull(token);
            if (normalized == null || normalized.length() < 3) {
                continue;
            }
            if (equalsIgnoreCase(normalized, category)
                    || equalsIgnoreCase(normalized, brand)
                    || equalsIgnoreCase(normalized, size)
                    || matchesCategoryHint(normalized, category)) {
                continue;
            }
            keywords.add(normalized.toLowerCase(Locale.ROOT));
        }
        return keywords.stream().distinct().toList();
    }

    private String buildSummary(
            String category,
            String brand,
            String size,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            List<String> keywords,
            String priceIntentLabel
    ) {
        List<String> parts = new ArrayList<>();
        if (category != null) {
            parts.add("category " + category);
        }
        if (brand != null) {
            parts.add("brand " + brand);
        }
        if (size != null) {
            parts.add("size " + size);
        }
        if (minPrice != null && priceIntentLabel != null && maxPrice == null) {
            parts.add(priceIntentLabel + " range >= " + minPrice.toPlainString() + " DT");
        } else if (minPrice != null) {
            parts.add("min " + minPrice.toPlainString() + " DT");
        }
        if (maxPrice != null && priceIntentLabel != null && minPrice == null) {
            parts.add(priceIntentLabel + " range <= " + maxPrice.toPlainString() + " DT");
        } else if (maxPrice != null) {
            parts.add("max " + maxPrice.toPlainString() + " DT");
        }
        if (inStockOnly) {
            parts.add("in stock");
        }
        if (!keywords.isEmpty()) {
            parts.add("keywords " + String.join(", ", keywords));
        }
        return parts.isEmpty() ? "Recherche texte standard." : "Filtres: " + String.join(" | ", parts);
    }

    private void addTag(Set<String> tags, String rawValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return;
        }
        for (String token : normalized.split("[^A-Za-z0-9]+")) {
            String tag = trimToNull(token);
            if (tag != null && tag.length() > 1) {
                tags.add(tag.toLowerCase(Locale.ROOT));
            }
        }
    }

    private void addCategoryTags(Set<String> tags, String category) {
        String normalizedCategory = fallback(category, "").toLowerCase(Locale.ROOT);
        if (normalizedCategory.contains("boot")) {
            tags.addAll(List.of("traction", "speed", "matchday"));
        } else if (normalizedCategory.contains("jersey") || normalizedCategory.contains("maillot")) {
            tags.addAll(List.of("breathable", "lightweight", "clubwear"));
        } else if (normalizedCategory.contains("ball")) {
            tags.addAll(List.of("touch", "control", "training"));
        } else if (normalizedCategory.contains("glove")) {
            tags.addAll(List.of("grip", "goalkeeper", "protection"));
        } else if (normalizedCategory.contains("training")) {
            tags.addAll(List.of("drill", "session", "conditioning"));
        } else if (normalizedCategory.contains("accessor")) {
            tags.addAll(List.of("kit", "essential", "support"));
        } else {
            tags.addAll(List.of("sport", "performance", "training"));
        }
    }

    private void addKeywordTags(Set<String> tags, String productName) {
        String lowerName = fallback(productName, "").toLowerCase(Locale.ROOT);
        if (lowerName.contains("indoor") || lowerName.contains("futsal")) {
            tags.add("indoor");
        }
        if (lowerName.contains("elite") || lowerName.contains("pro")) {
            tags.add("elite");
        }
        if (lowerName.contains("training")) {
            tags.add("training");
        }
        if (lowerName.contains("match")) {
            tags.add("match");
        }
        if (lowerName.contains("speed")) {
            tags.add("speed");
        }
        if (lowerName.contains("control")) {
            tags.add("control");
        }
        if (lowerName.contains("power")) {
            tags.add("power");
        }
        if (lowerName.contains("goalkeeper")) {
            tags.add("goalkeeper");
        }
        if (lowerName.contains("rain")) {
            tags.add("rain");
        }
        if (lowerName.contains("thermal")) {
            tags.add("thermal");
        }
    }

    private ProductTone inferTone(ProductDraft draft) {
        String category = fallback(draft.category(), "").toLowerCase(Locale.ROOT);
        String name = fallback(draft.name(), "").toLowerCase(Locale.ROOT);

        if (category.contains("boot")) {
            if (name.contains("speed") || name.contains("mercurial") || name.contains("vapor")) {
                return new ProductTone("explosive runs and quick direction changes", "traction and acceleration", "a streamlined fit for aggressive attacking play", "Speed Edition");
            }
            if (name.contains("predator") || name.contains("control")) {
                return new ProductTone("precise passing and controlled possession", "touch and stability", "a confident platform for technical players", "Control Series");
            }
            return new ProductTone("full-speed match performance", "traction and foot lockdown", "dependable balance for training and competition", "Match Ready");
        }

        if (category.contains("jersey") || category.contains("maillot")) {
            return new ProductTone("training sessions and match-day wear", "breathability and light comfort", "a clean athletic fit that stays easy to wear", "Club Edition");
        }

        if (category.contains("ball")) {
            return new ProductTone("regular drills and competitive play", "touch consistency and stable flight", "reliable control for repeated sessions", "Play Series");
        }

        if (category.contains("glove")) {
            return new ProductTone("goalkeeping sessions and shot stopping", "grip and hand confidence", "extra security during saves and catches", "Keeper Edition");
        }

        if (category.contains("training")) {
            return new ProductTone("structured training blocks", "practical setup and repeated use", "durable support for everyday drills", "Training Pack");
        }

        if (category.contains("accessor")) {
            return new ProductTone("daily kit support", "practical convenience", "easy integration with the rest of your gear", "Essential Kit");
        }

        if (name.contains("rain")) {
            return new ProductTone("wet-weather sessions", "coverage and day-to-day comfort", "a practical build for unstable conditions", "Weather Ready");
        }

        return new ProductTone("regular sport use", "comfort and dependable performance", "balanced everyday utility", "Performance Line");
    }

    private String cleanJson(String rawValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return "{}";
        }
        String cleaned = normalized;
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "");
            cleaned = cleaned.replaceFirst("```$", "");
        }
        String trimmed = cleaned.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            int objectStart = trimmed.indexOf('{');
            int objectEnd = trimmed.lastIndexOf('}');
            if (objectStart >= 0 && objectEnd > objectStart) {
                return trimmed.substring(objectStart, objectEnd + 1);
            }
            int arrayStart = trimmed.indexOf('[');
            int arrayEnd = trimmed.lastIndexOf(']');
            if (arrayStart >= 0 && arrayEnd > arrayStart) {
                return trimmed.substring(arrayStart, arrayEnd + 1);
            }
        }
        return trimmed;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean matchesText(String actualValue, String expectedValue) {
        String normalizedExpected = trimToNull(expectedValue);
        if (normalizedExpected == null) {
            return true;
        }
        String normalizedActual = trimToNull(actualValue);
        return normalizedActual != null && normalizedActual.toLowerCase(Locale.ROOT).contains(normalizedExpected.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String source, List<String> values) {
        if (source == null || values == null || values.isEmpty()) {
            return false;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .anyMatch(source::contains);
    }

    private String fallback(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ProductDraft(
            String name,
            String category,
            String brand,
            String size,
            BigDecimal price
    ) {
    }

    public record GeneratedProductContent(
            String marketingTitle,
            String description,
            List<String> tags,
            String source
    ) {
        public GeneratedProductContent withSource(String source) {
            return new GeneratedProductContent(marketingTitle, description, tags, source);
        }

        public String tagsAsText() {
            return tags == null ? "" : tags.stream().distinct().collect(Collectors.joining(", "));
        }
    }

    private record ProductTone(
            String primaryUse,
            String coreBenefit,
            String secondaryBenefit,
            String titleAccent
    ) {
    }

    private record DerivedPriceBand(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String label
    ) {
        private static DerivedPriceBand empty() {
            return new DerivedPriceBand(null, null, null);
        }
    }

    public record SmartProductQuery(
            String originalQuery,
            List<String> keywords,
            String category,
            String brand,
            String size,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            String summary,
            String source
    ) {
        public static SmartProductQuery empty() {
            return new SmartProductQuery(null, List.of(), null, null, null, null, null, false, "Recherche vide.", "none");
        }

        public SmartProductQuery withSource(String source) {
            return new SmartProductQuery(originalQuery, keywords, category, brand, size, minPrice, maxPrice, inStockOnly, summary, source);
        }

        public SmartProductQuery withSummary(String summary) {
            return new SmartProductQuery(originalQuery, keywords, category, brand, size, minPrice, maxPrice, inStockOnly, summary, source);
        }

        public boolean hasFilterCriteria() {
            return (keywords != null && !keywords.isEmpty())
                    || category != null
                    || brand != null
                    || size != null
                    || minPrice != null
                    || maxPrice != null
                    || inStockOnly;
        }

        public boolean isEmpty() {
            return !hasFilterCriteria();
        }
    }
}
