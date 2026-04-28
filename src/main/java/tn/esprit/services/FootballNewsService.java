package tn.esprit.services;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tn.esprit.i18n.I18n;
import tn.esprit.i18n.MachineTranslationService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FootballNewsService {
    private static final String DEFAULT_FEED_URL = "https://feeds.bbci.co.uk/sport/football/rss.xml";
    private static final String FEED_PROPERTY = "sport.insight.football.news.feed";
    private static final String FEED_ENV = "SPORT_INSIGHT_FOOTBALL_NEWS_FEED";
    private static final String MEDIA_NS = "http://search.yahoo.com/mrss/";

    private final HttpClient httpClient;
    private final String feedUrl;
    private final MachineTranslationService translationService = new MachineTranslationService();

    public FootballNewsService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), resolveFeedUrl());
    }

    FootballNewsService(HttpClient httpClient, String feedUrl) {
        this.httpClient = httpClient;
        this.feedUrl = feedUrl;
    }

    public List<FootballNewsArticle> fetchLatest() throws IOException, InterruptedException {
        return translateArticles(fetchLatestRaw(), I18n.getLocale());
    }

    public List<FootballNewsArticle> fetchLatestRaw() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .header("User-Agent", "SportInsight/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Football news feed returned HTTP " + response.statusCode());
        }
        return parseFeed(response.body());
    }

    List<FootballNewsArticle> parseFeed(String xml) throws IOException {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            configureSecureXml(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            NodeList items = document.getElementsByTagName("item");
            Map<String, FootballNewsArticle> uniqueByUrl = new LinkedHashMap<>();

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                FootballNewsArticle article = parseItem(item);
                if (article.url() == null || article.url().isBlank()) {
                    continue;
                }
                uniqueByUrl.putIfAbsent(article.url(), article);
            }

            return uniqueByUrl.values().stream()
                    .sorted(Comparator.comparing(FootballNewsArticle::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to parse football news feed", e);
        }
    }

    public List<FootballNewsArticle> fallbackArticles() {
        Instant now = Instant.now();
        return List.of(
                new FootballNewsArticle(
                        I18n.get("news.fallback.unavailableTitle"),
                        I18n.get("news.fallback.unavailableSummary"),
                        "https://www.bbc.co.uk/sport/football",
                        "",
                        now,
                        "Sport Insight"
                ),
                new FootballNewsArticle(
                        I18n.get("news.fallback.searchTitle"),
                        I18n.get("news.fallback.searchSummary"),
                        "https://www.bbc.co.uk/sport/football",
                        "",
                        now.minusSeconds(1800),
                        "Sport Insight"
                )
        );
    }

    private FootballNewsArticle parseItem(Element item) {
        String title = clean(text(item, "title"));
        String summary = clean(text(item, "description"));
        String url = clean(text(item, "link"));
        String thumbnail = thumbnailUrl(item);
        Instant publishedAt = parsePublishedAt(text(item, "pubDate"));
        return new FootballNewsArticle(title, summary, url, thumbnail, publishedAt, "Sport Insight");
    }

    public List<FootballNewsArticle> translateArticles(List<FootballNewsArticle> articles, Locale locale) {
        if (articles == null || articles.isEmpty()) {
            return articles;
        }
        if (!Locale.FRENCH.getLanguage().equalsIgnoreCase(I18n.normalize(locale).getLanguage())) {
            return articles.stream()
                    .map(this::restoreOriginalArticle)
                    .toList();
        }

        List<String> titles = articles.stream()
                .map(article -> article == null ? null : article.originalTitle())
                .toList();
        List<String> summaries = articles.stream()
                .map(article -> article == null ? null : article.originalSummary())
                .toList();

        List<String> translatedTitles = translationService.translateAll(titles, Locale.FRENCH);
        List<String> translatedSummaries = translationService.translateAll(summaries, Locale.FRENCH);

        List<FootballNewsArticle> translatedArticles = new ArrayList<>(articles.size());
        for (int index = 0; index < articles.size(); index++) {
            FootballNewsArticle article = articles.get(index);
            if (article == null) {
                translatedArticles.add(null);
                continue;
            }
            translatedArticles.add(new FootballNewsArticle(
                    fallbackToOriginal(translatedTitles.get(index), article.originalTitle()),
                    fallbackToOriginal(translatedSummaries.get(index), article.originalSummary()),
                    article.url(),
                    article.imageUrl(),
                    article.publishedAt(),
                    article.source(),
                    article.originalTitle(),
                    article.originalSummary()
            ));
        }
        return translatedArticles;
    }

    private FootballNewsArticle restoreOriginalArticle(FootballNewsArticle article) {
        if (article == null) {
            return null;
        }
        return new FootballNewsArticle(
                fallbackToOriginal(article.originalTitle(), article.title()),
                fallbackToOriginal(article.originalSummary(), article.summary()),
                article.url(),
                article.imageUrl(),
                article.publishedAt(),
                article.source(),
                article.originalTitle(),
                article.originalSummary()
        );
    }

    private String fallbackToOriginal(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static void configureSecureXml(DocumentBuilderFactory factory) {
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
            // Best effort: keep parsing available on older XML implementations.
        }
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            // Best effort.
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignored) {
            // Best effort.
        }
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private static String thumbnailUrl(Element item) {
        NodeList mediaThumbs = item.getElementsByTagNameNS(MEDIA_NS, "thumbnail");
        if (mediaThumbs.getLength() > 0 && mediaThumbs.item(0) instanceof Element mediaThumb) {
            String url = mediaThumb.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return upgradeFeedImageUrl(url.trim());
            }
        }

        NodeList thumbnails = item.getElementsByTagName("media:thumbnail");
        if (thumbnails.getLength() > 0 && thumbnails.item(0) instanceof Element thumb) {
            return upgradeFeedImageUrl(thumb.getAttribute("url").trim());
        }
        return "";
    }

    private static String upgradeFeedImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String upgraded = url.trim()
                .replaceAll("/standard/(\\d+)/", "/standard/1920/")
                .replace("/images/ic/{recipe}/", "/images/ic/1920x1080/")
                .replaceAll("/images/ic/\\d+x\\d+/", "/images/ic/1920x1080/");
        return upgraded;
    }

    private static Instant parsePublishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutTags = raw.replaceAll("<[^>]+>", " ");
        return withoutTags
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String resolveFeedUrl() {
        String configured = firstNonBlank(
                System.getProperty(FEED_PROPERTY),
                System.getenv(FEED_ENV)
        );
        if (configured == null) {
            return DEFAULT_FEED_URL;
        }
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return configured.trim();
        }
        return DEFAULT_FEED_URL;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
