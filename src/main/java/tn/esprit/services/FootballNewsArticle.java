package tn.esprit.services;

import java.time.Instant;

public record FootballNewsArticle(
        String title,
        String summary,
        String url,
        String imageUrl,
        Instant publishedAt,
        String source
) {
}
