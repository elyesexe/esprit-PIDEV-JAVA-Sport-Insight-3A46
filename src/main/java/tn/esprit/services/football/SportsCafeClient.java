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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SportsCafeClient {
    private static final String BASE_URL = "https://sportscafe.in";
    private static final long MIN_REQUEST_INTERVAL_MS = 350L;
    private static final Duration PAGE_CACHE_TTL = Duration.ofMinutes(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry<JsonNode>> contentCache = new ConcurrentHashMap<>();
    private Instant lastRequestAt = Instant.EPOCH;

    public SportsCafeClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode fetchCompetitionResults(String leagueSlug, String competitionSlug) throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/football/results/");
        path.append(trimSlashes(leagueSlug));
        if (competitionSlug != null && !competitionSlug.isBlank()) {
            path.append('/').append(trimSlashes(competitionSlug));
        }
        return fetchWindowContent(path.toString());
    }

    public JsonNode fetchLeagueResults(String leagueSlug) throws IOException, InterruptedException {
        return fetchWindowContent("/football/results/" + trimSlashes(leagueSlug));
    }

    public JsonNode fetchMatchLineups(String eventLink) throws IOException, InterruptedException {
        String normalizedEventLink = normalizePath(eventLink);
        return fetchWindowContent(normalizedEventLink.endsWith("/lineups")
                ? normalizedEventLink
                : normalizedEventLink + "/lineups");
    }

    public JsonNode fetchWindowContent(String path) throws IOException, InterruptedException {
        String normalizedPath = normalizePath(path);
        CacheEntry<JsonNode> cached = contentCache.get(normalizedPath);
        if (cached != null && Duration.between(cached.storedAt(), Instant.now()).compareTo(PAGE_CACHE_TTL) < 0) {
            return cached.value();
        }

        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(normalizedPath))
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("SportsCafe a retourne " + statusCode + " pour " + normalizedPath + ". " + shrink(response.body()));
        }

        JsonNode content = objectMapper.readTree(extractWindowContentJson(response.body()));
        contentCache.put(normalizedPath, new CacheEntry<>(content, Instant.now()));
        return content;
    }

    private URI buildUri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        return URI.create(BASE_URL + normalizePath(path));
    }

    private String normalizePath(String path) {
        String normalized = path == null || path.isBlank() ? "/" : path.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String extractWindowContentJson(String html) throws IOException {
        if (html == null || html.isBlank()) {
            throw new IOException("SportsCafe a retourne une page vide.");
        }

        int markerIndex = html.indexOf("window.content");
        if (markerIndex < 0) {
            throw new IOException("SportsCafe n'a pas expose window.content sur cette page.");
        }

        int equalsIndex = html.indexOf('=', markerIndex);
        int objectStart = html.indexOf('{', equalsIndex);
        if (equalsIndex < 0 || objectStart < 0) {
            throw new IOException("SportsCafe n'a pas expose un JSON window.content exploitable.");
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;

        for (int index = objectStart; index < html.length(); index++) {
            char current = html.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return html.substring(objectStart, index + 1);
                }
            }
        }

        throw new IOException("SportsCafe a fourni un JSON window.content incomplet.");
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
