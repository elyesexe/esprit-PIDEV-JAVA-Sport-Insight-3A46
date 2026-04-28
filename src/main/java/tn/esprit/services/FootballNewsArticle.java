package tn.esprit.services;

import java.time.Instant;

public record FootballNewsArticle(
        String title,
        String summary,
        String url,
        String imageUrl,
        Instant publishedAt,
        String source,
        String originalTitle,
        String originalSummary
) {
    public FootballNewsArticle(String title, String summary, String url, String imageUrl, Instant publishedAt, String source) {
        this(title, summary, url, imageUrl, publishedAt, source, title, summary);
    }
}
