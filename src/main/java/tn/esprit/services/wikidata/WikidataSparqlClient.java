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

public class WikidataSparqlClient {
    private static final String ENDPOINT = "https://query.wikidata.org/sparql";
    private static final long MIN_REQUEST_INTERVAL_MS = 1200L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Instant lastRequestAt = Instant.EPOCH;

    public WikidataSparqlClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode query(String sparql) throws IOException, InterruptedException {
        throttle();
        String encoded = URLEncoder.encode(sparql, StandardCharsets.UTF_8);
        String url = ENDPOINT + "?format=json&query=" + encoded;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/sparql-results+json")
                .header("User-Agent", "SportInsight/1.0 (JavaFX; Wikidata enrichment)")
                .GET()
                .timeout(Duration.ofSeconds(45))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Wikidata SPARQL returned " + statusCode);
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

