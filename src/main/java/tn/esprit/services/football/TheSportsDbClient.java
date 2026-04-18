package tn.esprit.services.football;

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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TheSportsDbClient {
    private static final String BASE_URL = "https://www.thesportsdb.com/api/v1/json/123";
    private static final long MIN_REQUEST_INTERVAL_MS = 350L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Instant lastRequestAt = Instant.EPOCH;

    public TheSportsDbClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode fetchLeagueTeams(String leagueQuery) throws IOException, InterruptedException {
        return getJson("/search_all_teams.php", Map.of("l", leagueQuery));
    }

    public JsonNode searchTeams(String teamQuery) throws IOException, InterruptedException {
        return getJson("/searchteams.php", Map.of("t", teamQuery));
    }

    public JsonNode searchEvents(String eventQuery) throws IOException, InterruptedException {
        return getJson("/searchevents.php", Map.of("e", eventQuery));
    }

    public JsonNode searchEvents(String eventQuery, LocalDate date) throws IOException, InterruptedException {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("e", eventQuery);
        if (date != null) {
            query.put("d", date.toString());
        }
        return getJson("/searchevents.php", query);
    }

    public JsonNode fetchEventLineup(long eventId) throws IOException, InterruptedException {
        return getJson("/lookuplineup.php", Map.of("id", String.valueOf(eventId)));
    }

    public JsonNode fetchEventStats(long eventId) throws IOException, InterruptedException {
        return getJson("/lookupeventstats.php", Map.of("id", String.valueOf(eventId)));
    }

    public JsonNode fetchEventsDay(LocalDate date) throws IOException, InterruptedException {
        if (date == null) {
            throw new IOException("TheSportsDB exige une date pour eventsday.php.");
        }
        return getJson("/eventsday.php", Map.of(
                "d", date.toString(),
                "s", "Soccer"
        ));
    }

    public JsonNode fetchEventsDay(LocalDate date, String leagueFilter) throws IOException, InterruptedException {
        if (date == null) {
            throw new IOException("TheSportsDB exige une date pour eventsday.php.");
        }
        Map<String, String> query = new LinkedHashMap<>();
        query.put("d", date.toString());
        query.put("s", "Soccer");
        if (leagueFilter != null && !leagueFilter.isBlank()) {
            query.put("l", leagueFilter);
        }
        return getJson("/eventsday.php", query);
    }

    public JsonNode fetchTeamLastEvents(long teamId) throws IOException, InterruptedException {
        return getJson("/eventslast.php", Map.of("id", String.valueOf(teamId)));
    }

    public JsonNode fetchTeamNextEvents(long teamId) throws IOException, InterruptedException {
        return getJson("/eventsnext.php", Map.of("id", String.valueOf(teamId)));
    }

    private JsonNode getJson(String path, Map<String, String> query) throws IOException, InterruptedException {
        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path, query))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("TheSportsDB a retourne " + statusCode + " pour " + path + ". " + shrink(response.body()));
        }

        return objectMapper.readTree(response.body());
    }

    private URI buildUri(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(BASE_URL).append(path);
        if (query != null && !query.isEmpty()) {
            builder.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                first = false;
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                builder.append('=');
                builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return URI.create(builder.toString());
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
