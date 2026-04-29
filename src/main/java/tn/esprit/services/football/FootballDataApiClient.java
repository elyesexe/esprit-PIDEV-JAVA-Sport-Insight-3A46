package tn.esprit.services.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.tools.FootballDataConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public class FootballDataApiClient {
    private static final long MIN_REQUEST_INTERVAL_MS = 6500L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private Instant lastRequestAt = Instant.EPOCH;

    public FootballDataApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = FootballDataConfig.resolveApiKey();
    }

    public JsonNode fetchCompetitionTeams(String competitionCode) throws IOException, InterruptedException {
        return getJson("/competitions/" + competitionCode + "/teams");
    }

    public JsonNode fetchCompetitionMatches(String competitionCode) throws IOException, InterruptedException {
        return getJson("/competitions/" + competitionCode + "/matches");
    }

    public JsonNode fetchCompetitionStandings(String competitionCode) throws IOException, InterruptedException {
        return getJson("/competitions/" + competitionCode + "/standings");
    }

    private JsonNode getJson(String path) throws IOException, InterruptedException {
        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FootballDataConfig.BASE_URL + path))
                .header("X-Auth-Token", apiKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("football-data.org a retourne " + statusCode + " pour " + path + ". " + shrink(response.body()));
        }

        return objectMapper.readTree(response.body());
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
}
