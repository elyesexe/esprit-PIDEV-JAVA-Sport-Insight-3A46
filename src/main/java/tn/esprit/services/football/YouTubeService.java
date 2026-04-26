package tn.esprit.services.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class YouTubeService {
    static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    public static final String API_ERROR_MESSAGE = "YouTube API error. Check API key or quota.";
    private static final Duration HIGHLIGHTS_CACHE_TTL = Duration.ofMinutes(15);
    private static final ConcurrentHashMap<String, CachedHighlights> HIGHLIGHTS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<List<YouTubeVideo>>> IN_FLIGHT_HIGHLIGHTS = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public YouTubeService() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build(),
                new ObjectMapper(),
                resolveConfiguredApiKey()
        );
    }

    YouTubeService(HttpClient httpClient, ObjectMapper objectMapper, String apiKey) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.apiKey = sanitize(apiKey);
    }

    public List<YouTubeVideo> searchInAppHighlights(Matchs match, Equipe homeTeam, Equipe awayTeam)
            throws IOException, InterruptedException {
        return searchInAppHighlights(match, homeTeam, awayTeam, false);
    }

    public List<YouTubeVideo> searchInAppHighlights(Matchs match, Equipe homeTeam, Equipe awayTeam, boolean forceRefresh)
            throws IOException, InterruptedException {
        if (!isFinishedStatus(match == null ? null : match.getStatut())) {
            return List.of();
        }

        String cacheKey = buildHighlightsCacheKey(match, homeTeam, awayTeam);
        Instant now = Instant.now();
        if (!forceRefresh) {
            Optional<List<YouTubeVideo>> cached = readCachedHighlights(cacheKey, now);
            if (cached.isPresent()) {
                return cached.get();
            }

            CompletableFuture<List<YouTubeVideo>> existingRequest = IN_FLIGHT_HIGHLIGHTS.get(cacheKey);
            if (existingRequest != null) {
                return awaitHighlights(existingRequest);
            }
        }

        CompletableFuture<List<YouTubeVideo>> request = new CompletableFuture<>();
        if (!forceRefresh) {
            CompletableFuture<List<YouTubeVideo>> existingRequest = IN_FLIGHT_HIGHLIGHTS.putIfAbsent(cacheKey, request);
            if (existingRequest != null) {
                return awaitHighlights(existingRequest);
            }
        }

        try {
            List<YouTubeVideo> videos = loadInAppHighlights(homeTeam, awayTeam);
            HIGHLIGHTS_CACHE.put(cacheKey, new CachedHighlights(videos, Instant.now()));
            request.complete(videos);
            return videos;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            request.completeExceptionally(e);
            throw e;
        } catch (IOException | RuntimeException e) {
            request.completeExceptionally(e);
            throw e;
        } finally {
            if (!forceRefresh) {
                IN_FLIGHT_HIGHLIGHTS.remove(cacheKey, request);
            }
        }
    }

    public Optional<List<YouTubeVideo>> readCachedInAppHighlights(Matchs match, Equipe homeTeam, Equipe awayTeam) {
        if (!isFinishedStatus(match == null ? null : match.getStatut())) {
            return Optional.empty();
        }
        return readCachedHighlights(buildHighlightsCacheKey(match, homeTeam, awayTeam), Instant.now());
    }

    private List<YouTubeVideo> loadInAppHighlights(Equipe homeTeam, Equipe awayTeam)
            throws IOException, InterruptedException {
        List<YouTubeVideo> videos = searchVideos(buildHighlightsQuery(homeTeam, awayTeam));
        Set<String> playableIds = getPlayableVideoIds(videos.stream()
                .map(YouTubeVideo::videoId)
                .filter(Objects::nonNull)
                .toList());

        return videos.stream()
                .filter(video -> playableIds.contains(video.videoId()))
                .toList();
    }

    private Optional<List<YouTubeVideo>> readCachedHighlights(String cacheKey, Instant now) {
        CachedHighlights cached = HIGHLIGHTS_CACHE.get(cacheKey);
        if (cached == null) {
            return Optional.empty();
        }
        if (cached.isExpired(now)) {
            HIGHLIGHTS_CACHE.remove(cacheKey, cached);
            return Optional.empty();
        }
        return Optional.of(cached.videos());
    }

    private List<YouTubeVideo> awaitHighlights(CompletableFuture<List<YouTubeVideo>> request)
            throws IOException, InterruptedException {
        try {
            return request.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw interruptedException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException(API_ERROR_MESSAGE, cause);
        }
    }

    public List<YouTubeVideo> searchVideos(String query) throws IOException, InterruptedException {
        String apiKeyValue = requireApiKey();
        String url = SEARCH_URL
                + "?part=snippet"
                + "&type=video"
                + "&videoEmbeddable=true"
                + "&maxResults=10"
                + "&q=" + encode(query)
                + "&key=" + encode(apiKeyValue);

        JsonNode payload = sendJson(url);
        return parseSearchResults(payload);
    }

    List<YouTubeVideo> parseSearchResults(JsonNode payload) {
        List<YouTubeVideo> videos = new ArrayList<>();
        JsonNode items = payload.path("items");
        if (!items.isArray()) {
            return List.of();
        }

        for (JsonNode item : items) {
            String videoId = sanitize(item.path("id").path("videoId").asText(null));
            if (videoId == null) {
                continue;
            }
            JsonNode snippet = item.path("snippet");
            videos.add(new YouTubeVideo(
                    videoId,
                    snippet.path("title").asText(null),
                    snippet.path("channelTitle").asText(null)
            ));
        }
        return List.copyOf(videos);
    }

    public Set<String> getPlayableVideoIds(List<String> videoIds) throws IOException, InterruptedException {
        if (videoIds == null || videoIds.isEmpty()) {
            return Set.of();
        }

        String apiKeyValue = requireApiKey();
        String joinedIds = String.join(",", videoIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        if (joinedIds.isBlank()) {
            return Set.of();
        }

        String url = VIDEOS_URL
                + "?part=status"
                + "&id=" + encode(joinedIds)
                + "&key=" + encode(apiKeyValue);

        JsonNode payload = sendJson(url);
        return parsePlayableIds(payload);
    }

    Set<String> parsePlayableIds(JsonNode payload) {
        Set<String> playableIds = new LinkedHashSet<>();
        JsonNode items = payload.path("items");
        if (!items.isArray()) {
            return Set.of();
        }

        for (JsonNode item : items) {
            JsonNode status = item.path("status");
            String privacyStatus = sanitize(status.path("privacyStatus").asText(null));
            String uploadStatus = sanitize(status.path("uploadStatus").asText(null));
            boolean embeddable = status.path("embeddable").asBoolean(false);
            boolean publicVideo = privacyStatus == null || "public".equalsIgnoreCase(privacyStatus);
            boolean processedVideo = uploadStatus == null || "processed".equalsIgnoreCase(uploadStatus);
            if (embeddable && publicVideo && processedVideo) {
                String id = sanitize(item.path("id").asText(null));
                if (id != null) {
                    playableIds.add(id);
                }
            }
        }
        return Set.copyOf(playableIds);
    }

    private JsonNode sendJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(API_ERROR_MESSAGE);
        }
        return objectMapper.readTree(response.body());
    }

    private String requireApiKey() throws IOException {
        if (apiKey == null) {
            throw new IOException(API_ERROR_MESSAGE);
        }
        return apiKey;
    }

    private String buildHighlightsQuery(Equipe homeTeam, Equipe awayTeam) {
        String home = sanitize(homeTeam == null ? null : homeTeam.getNom());
        String away = sanitize(awayTeam == null ? null : awayTeam.getNom());
        if (home == null || away == null) {
            return "football highlights";
        }
        return home + " vs " + away + " highlights";
    }

    private static String buildHighlightsCacheKey(Matchs match, Equipe homeTeam, Equipe awayTeam) {
        if (match != null && match.getId() != null) {
            return "db:" + match.getId();
        }

        String providerId = sanitize(match == null ? null : match.getIdMatch());
        if (providerId != null) {
            return "provider:" + keyPart(providerId);
        }

        String date = match == null || match.getDateMatch() == null ? "unknown-date" : match.getDateMatch().toString();
        String home = homeTeam == null ? null : homeTeam.getNom();
        String away = awayTeam == null ? null : awayTeam.getNom();
        return "fallback:" + keyPart(date) + "|" + keyPart(home) + "|" + keyPart(away);
    }

    public static boolean isFinishedStatus(String status) {
        String normalized = sanitize(status);
        if (normalized == null) {
            return false;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.contains("not finished") || normalized.contains("unfinished")) {
            return false;
        }
        return normalized.equals("finished")
                || normalized.equals("ft")
                || normalized.equals("fini")
                || normalized.contains("match finished")
                || normalized.contains("full time")
                || normalized.contains("fini")
                || normalized.contains("termine")
                || normalized.contains("termin");
    }

    private static String resolveConfiguredApiKey() {
        String systemProperty = sanitize(System.getProperty("YOUTUBE_API_KEY"));
        return systemProperty == null ? System.getenv("YOUTUBE_API_KEY") : systemProperty;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String keyPart(String value) {
        String sanitized = sanitize(value);
        return sanitized == null ? "unknown" : sanitized.toLowerCase(Locale.ROOT);
    }

    static void clearHighlightsCacheForTests() {
        HIGHLIGHTS_CACHE.clear();
        IN_FLIGHT_HIGHLIGHTS.clear();
    }

    private record CachedHighlights(List<YouTubeVideo> videos, Instant cachedAt) {
        private CachedHighlights {
            videos = videos == null ? List.of() : List.copyOf(videos);
            cachedAt = cachedAt == null ? Instant.EPOCH : cachedAt;
        }

        private boolean isExpired(Instant now) {
            return cachedAt.plus(HIGHLIGHTS_CACHE_TTL).isBefore(now);
        }
    }
}
