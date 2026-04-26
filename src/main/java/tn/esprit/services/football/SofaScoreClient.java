package tn.esprit.services.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SofaScoreClient {
    public static final String PLAYER_IMAGE_BASE_URL = "https://api.sofascore.app/api/v1/player/";

    private static final String BASE_URL = "https://www.sofascore.com/api/v1";
    private static final long MIN_REQUEST_INTERVAL_MS = 250L;
    private static final Duration CACHE_TTL = Duration.ofMinutes(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry<JsonNode>> responseCache = new ConcurrentHashMap<>();
    private Instant lastRequestAt = Instant.EPOCH;

    public SofaScoreClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode fetchScheduledEvents(LocalDate date) throws IOException, InterruptedException {
        if (date == null) {
            throw new IOException("La date du match est obligatoire pour interroger SofaScore.");
        }
        return getJson("/sport/football/scheduled-events/" + date);
    }

    public JsonNode fetchEventLineups(long eventId) throws IOException, InterruptedException {
        return getJson("/event/" + eventId + "/lineups");
    }

    public JsonNode fetchEventStatistics(long eventId) throws IOException, InterruptedException {
        return getJson("/event/" + eventId + "/statistics");
    }

    public JsonNode fetchEventIncidents(long eventId) throws IOException, InterruptedException {
        return getJson("/event/" + eventId + "/incidents");
    }

    private JsonNode getJson(String path) throws IOException, InterruptedException {
        CacheEntry<JsonNode> cached = responseCache.get(path);
        if (cached != null && Duration.between(cached.storedAt(), Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cached.value();
        }

        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("SofaScore a retourne " + response.statusCode() + " pour " + path + ". " + shrink(response.body()));
        }

        JsonNode payload = objectMapper.readTree(response.body());
        responseCache.put(path, new CacheEntry<>(payload, Instant.now()));
        return payload;
    }

    private synchronized void throttle() throws InterruptedException {
        long elapsed = Duration.between(lastRequestAt, Instant.now()).toMillis();
        long remaining = MIN_REQUEST_INTERVAL_MS - elapsed;
        if (remaining > 0) {
            Thread.sleep(remaining);
        }
        lastRequestAt = Instant.now();
    }

    private String shrink(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }

    private record CacheEntry<T>(T value, Instant storedAt) {
    }
}
