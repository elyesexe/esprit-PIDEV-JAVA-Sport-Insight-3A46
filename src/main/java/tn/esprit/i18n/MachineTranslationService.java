package tn.esprit.i18n;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class MachineTranslationService {
    private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&dt=t";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(6, daemonFactory());

    public MachineTranslationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String translate(String text, Locale targetLocale) {
        if (text == null || text.isBlank() || targetLocale == null) {
            return text;
        }
        String language = I18n.normalize(targetLocale).getLanguage();
        String cacheKey = language + "\n" + text;
        return cache.computeIfAbsent(cacheKey, ignored -> doTranslate(text, language));
    }

    public List<String> translateAll(List<String> texts, Locale targetLocale) {
        if (texts == null || texts.isEmpty() || targetLocale == null) {
            return texts == null ? List.of() : List.copyOf(texts);
        }
        String language = I18n.normalize(targetLocale).getLanguage();
        List<CompletableFuture<String>> futures = new ArrayList<>(texts.size());
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                futures.add(CompletableFuture.completedFuture(text));
                continue;
            }
            futures.add(CompletableFuture.supplyAsync(() -> {
                String cacheKey = language + "\n" + text;
                return cache.computeIfAbsent(cacheKey, ignored -> doTranslate(text, language));
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<String> translated = new ArrayList<>(futures.size());
        for (CompletableFuture<String> future : futures) {
            translated.add(future.join());
        }
        return translated;
    }

    private String doTranslate(String text, String language) {
        try {
            String url = ENDPOINT
                    + "&tl=" + URLEncoder.encode(language, StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "SportInsight/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return text;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode sentences = root.path(0);
            if (!sentences.isArray()) {
                return text;
            }

            StringBuilder translated = new StringBuilder();
            for (JsonNode sentence : sentences) {
                JsonNode chunk = sentence.path(0);
                if (chunk.isTextual()) {
                    translated.append(chunk.asText());
                }
            }
            String result = translated.toString().trim();
            return result.isBlank() ? text : result;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return text;
        } catch (IOException ignored) {
            return text;
        } catch (Exception ignored) {
            return text;
        }
    }

    private static ThreadFactory daemonFactory() {
        AtomicLong sequence = new AtomicLong();
        return runnable -> {
            Thread thread = new Thread(runnable, "machine-translation-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
