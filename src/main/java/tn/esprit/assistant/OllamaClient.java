package tn.esprit.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OllamaClient {
    private static final String BASE_URL = "http://127.0.0.1:11434";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration STATUS_CACHE_TTL = Duration.ofSeconds(45);
    private static final String CHAT_KEEP_ALIVE = "30m";
    private static final List<String> FALLBACK_MODELS = List.of(
            "llama3.2:1b",
            "qwen2.5:1.5b-instruct",
            "llama3.2",
            "qwen2.5"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile ModelCatalog cachedCatalog;

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Status status(String preferredModel) {
        ModelCatalog catalog = cachedCatalog;
        if (catalog != null && !catalog.isExpired()) {
            return buildStatus(preferredModel, catalog.installedModels());
        }

        try {
            List<String> installedModels = fetchInstalledModels();
            cachedCatalog = new ModelCatalog(installedModels, Instant.now());
            return buildStatus(preferredModel, installedModels);
        } catch (Exception ex) {
            if (catalog != null) {
                return buildStatus(preferredModel, catalog.installedModels());
            }
            return Status.unavailable("Ollama is not reachable on http://127.0.0.1:11434.");
        }
    }

    public String chat(String preferredModel, String systemPrompt, List<AssistantMessage> history) throws IOException, InterruptedException {
        return chat(preferredModel, systemPrompt, history, ChatProfile.STANDARD);
    }

    public String chat(String preferredModel, String systemPrompt, List<AssistantMessage> history, ChatProfile profile) throws IOException, InterruptedException {
        Status status = status(preferredModel);
        if (!status.reachable() || status.selectedModel() == null) {
            return null;
        }

        ObjectNode payload = createChatPayload(status.selectedModel(), systemPrompt, history, profile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat"))
                .timeout(profile.requestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama chat failed with HTTP " + response.statusCode() + ".");
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("message").path("content").asText("");
        return content == null ? null : content.trim();
    }

    public void warmModel(String preferredModel, ChatProfile profile) {
        try {
            Status status = status(preferredModel);
            if (!status.reachable() || status.selectedModel() == null) {
                return;
            }

            List<AssistantMessage> warmHistory = List.of(new AssistantMessage(
                    AssistantMessage.Role.USER,
                    "Reply with ok.",
                    Instant.now()
            ));
            ObjectNode payload = createChatPayload(status.selectedModel(), "You are warming up for fast voice replies.", warmHistory, profile.warmVariant());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/chat"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Warm-up is best effort only.
        }
    }

    private ObjectNode createChatPayload(String modelName, String systemPrompt, List<AssistantMessage> history, ChatProfile profile) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelName);
        payload.put("stream", false);
        payload.put("keep_alive", CHAT_KEEP_ALIVE);
        payload.put("think", false);
        applyOptions(payload.putObject("options"), profile);

        ArrayNode messages = payload.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        int maxHistoryMessages = profile.maxHistoryMessages();
        List<AssistantMessage> trimmedHistory = history.size() > maxHistoryMessages
                ? history.subList(history.size() - maxHistoryMessages, history.size())
                : history;

        for (AssistantMessage message : trimmedHistory) {
            ObjectNode node = messages.addObject();
            node.put("role", message.role() == AssistantMessage.Role.USER ? "user" : "assistant");
            node.put("content", abbreviate(message.content(), profile.maxHistoryMessageChars()));
        }
        return payload;
    }

    private void applyOptions(ObjectNode options, ChatProfile profile) {
        options.put("num_ctx", profile.numCtx());
        options.put("num_predict", profile.numPredict());
        options.put("temperature", profile.temperature());
    }

    private Status buildStatus(String preferredModel, List<String> installedModels) {
        String selectedModel = selectModel(preferredModel, installedModels).orElse(null);
        String note = installedModels.isEmpty()
                ? "Ollama is installed but no local model is available yet."
                : "Ollama is ready with " + installedModels.size() + " local model(s).";
        return new Status(true, installedModels, selectedModel, note);
    }

    private List<String> fetchInstalledModels() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama answered with HTTP " + response.statusCode() + ".");
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<String> installedModels = new ArrayList<>();
        for (JsonNode modelNode : root.path("models")) {
            String name = modelNode.path("name").asText("");
            if (!name.isBlank()) {
                installedModels.add(name);
            }
        }
        installedModels.sort(Comparator.naturalOrder());
        return installedModels;
    }

    private Optional<String> selectModel(String preferredModel, List<String> installedModels) {
        if (preferredModel != null && installedModels.contains(preferredModel)) {
            return Optional.of(preferredModel);
        }
        for (String candidate : FALLBACK_MODELS) {
            if (installedModels.contains(candidate)) {
                return Optional.of(candidate);
            }
        }
        return installedModels.stream().findFirst();
    }

    public record Status(
            boolean reachable,
            List<String> installedModels,
            String selectedModel,
            String note
    ) {
        public static Status unavailable(String note) {
            return new Status(false, List.of(), null, note);
        }
    }

    public enum ChatProfile {
        REALTIME(1280, 40, 0.1, 2, 180, Duration.ofSeconds(2)),
        STANDARD(2816, 120, 0.18, 4, 320, Duration.ofSeconds(12)),
        DEEP(4096, 220, 0.24, 6, 500, Duration.ofSeconds(28)),
        WARMUP(1024, 12, 0.0, 1, 80, Duration.ofSeconds(8));

        private final int numCtx;
        private final int numPredict;
        private final double temperature;
        private final int maxHistoryMessages;
        private final int maxHistoryMessageChars;
        private final Duration requestTimeout;

        ChatProfile(int numCtx, int numPredict, double temperature, int maxHistoryMessages, int maxHistoryMessageChars, Duration requestTimeout) {
            this.numCtx = numCtx;
            this.numPredict = numPredict;
            this.temperature = temperature;
            this.maxHistoryMessages = maxHistoryMessages;
            this.maxHistoryMessageChars = maxHistoryMessageChars;
            this.requestTimeout = requestTimeout;
        }

        public int numCtx() {
            return numCtx;
        }

        public int numPredict() {
            return numPredict;
        }

        public double temperature() {
            return temperature;
        }

        public int maxHistoryMessages() {
            return maxHistoryMessages;
        }

        public int maxHistoryMessageChars() {
            return maxHistoryMessageChars;
        }

        public Duration requestTimeout() {
            return requestTimeout;
        }

        private ChatProfile warmVariant() {
            return WARMUP;
        }
    }

    private String abbreviate(String content, int maxChars) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars).trim() + "...";
    }

    private record ModelCatalog(List<String> installedModels, Instant fetchedAt) {
        private boolean isExpired() {
            return fetchedAt == null || fetchedAt.plus(STATUS_CACHE_TTL).isBefore(Instant.now());
        }
    }
}
