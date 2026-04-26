package tn.esprit.services;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FootballNewsServiceTest {
    @Test
    void parsesBbcFootballRssItemsWithThumbnail() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                  <channel>
                    <item>
                      <title><![CDATA[Test football headline]]></title>
                      <description><![CDATA[Test summary about a transfer story.]]></description>
                      <link>https://www.bbc.com/sport/football/articles/test</link>
                      <pubDate>Sun, 26 Apr 2026 18:14:00 GMT</pubDate>
                      <media:thumbnail width="240" height="134" url="https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/test.jpg"/>
                    </item>
                  </channel>
                </rss>
                """;

        FootballNewsService service = new FootballNewsService(HttpClient.newHttpClient(), "https://feeds.bbci.co.uk/sport/football/rss.xml");
        List<FootballNewsArticle> articles = service.parseFeed(rss);

        assertEquals(1, articles.size());
        FootballNewsArticle article = articles.get(0);
        assertEquals("Test football headline", article.title());
        assertEquals("https://www.bbc.com/sport/football/articles/test", article.url());
        assertEquals("https://ichef.bbci.co.uk/ace/standard/1920/cpsprodpb/test.jpg", article.imageUrl());
        assertEquals(Instant.parse("2026-04-26T18:14:00Z"), article.publishedAt());
        assertFalse(article.summary().isBlank());
    }

    @Test
    void upgradesSmallFeedImageRecipeToHighResolution() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                  <channel>
                    <item>
                      <title>Video headline</title>
                      <description>Video summary.</description>
                      <link>https://www.bbc.co.uk/iplayer/live/test</link>
                      <pubDate>Sun, 26 Apr 2026 18:14:00 GMT</pubDate>
                      <media:thumbnail width="240" height="135" url="https://ichef.bbci.co.uk/images/ic/240x135/p0ng4tnp.jpg"/>
                    </item>
                  </channel>
                </rss>
                """;

        FootballNewsService service = new FootballNewsService(HttpClient.newHttpClient(), "https://feeds.bbci.co.uk/sport/football/rss.xml");
        List<FootballNewsArticle> articles = service.parseFeed(rss);

        assertEquals(1, articles.size());
        assertEquals("https://ichef.bbci.co.uk/images/ic/1920x1080/p0ng4tnp.jpg", articles.get(0).imageUrl());
    }
}
