package tn.esprit.services.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class WikidataApiClient {
    private static final long MIN_REQUEST_INTERVAL_MS = 1200L;
    private static final String API_BASE = "https://www.wikidata.org/w/api.php";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Instant lastRequestAt = Instant.EPOCH;

    public WikidataApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode searchEntities(String search, int limit) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(search, StandardCharsets.UTF_8);
        String url = API_BASE
                + "?action=wbsearchentities"
                + "&search=" + encoded
                + "&language=en"
                + "&format=json"
                + "&limit=" + Math.max(1, Math.min(limit, 10));
        return getJson(url);
    }

    public JsonNode getEntity(String entityId) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(entityId, StandardCharsets.UTF_8);
        String props = URLEncoder.encode("claims|labels|descriptions", StandardCharsets.UTF_8);
        String url = API_BASE
                + "?action=wbgetentities"
                + "&ids=" + encoded
                + "&props=" + props
                + "&languages=en"
                + "&format=json";
        return getJson(url);
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SportInsight/1.0 (JavaFX; Wikidata enrichment)")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Wikidata returned " + statusCode + " for " + url);
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
}
