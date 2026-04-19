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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OllamaClient {
    private static final String BASE_URL = "http://127.0.0.1:11434";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final List<String> FALLBACK_MODELS = List.of(
            "llama3.2:1b",
            "qwen2.5:1.5b-instruct",
            "llama3.2",
            "qwen2.5"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Status status(String preferredModel) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/tags"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Status.unavailable("Ollama answered with HTTP " + response.statusCode() + ".");
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
            String selectedModel = selectModel(preferredModel, installedModels).orElse(null);
            String note = installedModels.isEmpty()
                    ? "Ollama is installed but no local model is available yet."
                    : "Ollama is ready with " + installedModels.size() + " local model(s).";
            return new Status(true, installedModels, selectedModel, note);
        } catch (Exception ex) {
            return Status.unavailable("Ollama is not reachable on http://127.0.0.1:11434.");
        }
    }

    public String chat(String preferredModel, String systemPrompt, List<AssistantMessage> history) throws IOException, InterruptedException {
        Status status = status(preferredModel);
        if (!status.reachable() || status.selectedModel() == null) {
            return null;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", status.selectedModel());
        payload.put("stream", false);
        payload.put("keep_alive", "15m");
        payload.put("think", false);

        ArrayNode messages = payload.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        List<AssistantMessage> trimmedHistory = history.size() > 10
                ? history.subList(history.size() - 10, history.size())
                : history;

        for (AssistantMessage message : trimmedHistory) {
            ObjectNode node = messages.addObject();
            node.put("role", message.role() == AssistantMessage.Role.USER ? "user" : "assistant");
            node.put("content", message.content());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat"))
                .timeout(REQUEST_TIMEOUT)
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
}
