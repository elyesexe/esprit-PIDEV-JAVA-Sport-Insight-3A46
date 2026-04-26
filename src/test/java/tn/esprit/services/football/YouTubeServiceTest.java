package tn.esprit.services.football;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeServiceTest {
    @BeforeEach
    void clearHighlightsCache() {
        YouTubeService.clearHighlightsCacheForTests();
    }

    @Test
    void buildsStandardEmbedUrl() {
        YouTubeVideo video = new YouTubeVideo("abc123", "Highlights", "Channel");

        assertEquals("https://www.youtube.com/embed/abc123?autoplay=1&rel=0&modestbranding=1&playsinline=1&origin=https://www.youtube.com", video.getEmbedUrl());
        assertEquals("https://www.youtube.com/watch?v=abc123", video.getWatchUrl());
        assertEquals("https://www.youtube.com/watch?v=abc123&autoplay=1", video.getInAppWatchUrl());
    }

    @Test
    void nonFinishedMatchDoesNotSearchYouTube() throws Exception {
        FakeYouTubeService service = new FakeYouTubeService(
                List.of(new YouTubeVideo("abc123", "Highlights", "Channel")),
                Set.of("abc123")
        );

        List<YouTubeVideo> videos = service.searchInAppHighlights(
                fixture("LIVE"),
                team("Real Madrid"),
                team("Bayern Munich")
        );

        assertTrue(videos.isEmpty());
        assertFalse(service.searchCalled);
        assertFalse(service.statusCalled);
        assertFalse(YouTubeService.isFinishedStatus("not finished"));
    }

    @Test
    void finishedMatchKeepsOnlyPlayableVideos() throws Exception {
        FakeYouTubeService service = new FakeYouTubeService(
                List.of(
                        new YouTubeVideo("one", "One", "Channel A"),
                        new YouTubeVideo("two", "Two", "Channel B"),
                        new YouTubeVideo("three", "Three", "Channel C")
                ),
                Set.of("one", "three")
        );

        List<YouTubeVideo> videos = service.searchInAppHighlights(
                fixture("FINISHED"),
                team("Real Madrid"),
                team("Bayern Munich")
        );

        assertEquals(2, videos.size());
        assertEquals("one", videos.get(0).videoId());
        assertEquals("three", videos.get(1).videoId());
        assertEquals("Real Madrid vs Bayern Munich highlights", service.lastQuery);
    }

    @Test
    void finishedMatchSearchResultIsCachedPerMatch() throws Exception {
        FakeYouTubeService service = new FakeYouTubeService(
                List.of(new YouTubeVideo("one", "One", "Channel A")),
                Set.of("one")
        );

        Matchs match = fixture("FINISHED");
        Equipe homeTeam = team("Real Madrid");
        Equipe awayTeam = team("Bayern Munich");

        List<YouTubeVideo> firstVideos = service.searchInAppHighlights(match, homeTeam, awayTeam);
        service.searchResults = List.of(new YouTubeVideo("two", "Two", "Channel B"));
        service.playableIds = Set.of("two");
        List<YouTubeVideo> secondVideos = service.searchInAppHighlights(match, homeTeam, awayTeam);

        assertEquals(1, firstVideos.size());
        assertEquals("one", firstVideos.get(0).videoId());
        assertEquals(1, secondVideos.size());
        assertEquals("one", secondVideos.get(0).videoId());
        assertEquals(1, service.searchCallCount);
        assertEquals(1, service.statusCallCount);
        assertTrue(service.readCachedInAppHighlights(match, homeTeam, awayTeam).isPresent());
    }

    @Test
    void forceRefreshBypassesCachedHighlights() throws Exception {
        FakeYouTubeService service = new FakeYouTubeService(
                List.of(new YouTubeVideo("one", "One", "Channel A")),
                Set.of("one")
        );

        Matchs match = fixture("FINISHED");
        Equipe homeTeam = team("Real Madrid");
        Equipe awayTeam = team("Bayern Munich");

        service.searchInAppHighlights(match, homeTeam, awayTeam);
        service.searchResults = List.of(new YouTubeVideo("two", "Two", "Channel B"));
        service.playableIds = Set.of("two");
        List<YouTubeVideo> refreshedVideos = service.searchInAppHighlights(match, homeTeam, awayTeam, true);

        assertEquals(1, refreshedVideos.size());
        assertEquals("two", refreshedVideos.get(0).videoId());
        assertEquals(2, service.searchCallCount);
        assertEquals(2, service.statusCallCount);
    }

    @Test
    void parsesSearchJsonIntoVideos() throws Exception {
        String payload = """
                {
                  "items": [
                    {
                      "id": { "videoId": "abc123" },
                      "snippet": {
                        "title": "Real Madrid vs Bayern Munich Highlights",
                        "channelTitle": "Official Channel"
                      }
                    },
                    {
                      "id": {},
                      "snippet": {
                        "title": "Ignored",
                        "channelTitle": "No ID"
                      }
                    }
                  ]
                }
                """;

        List<YouTubeVideo> videos = new YouTubeService().parseSearchResults(new ObjectMapper().readTree(payload));

        assertEquals(1, videos.size());
        assertEquals("abc123", videos.get(0).videoId());
        assertEquals("Real Madrid vs Bayern Munich Highlights", videos.get(0).title());
        assertEquals("Official Channel", videos.get(0).channelTitle());
    }

    @Test
    void parsesPlayableStatusJson() throws Exception {
        String payload = """
                {
                  "items": [
                    { "id": "abc123", "status": { "embeddable": true, "privacyStatus": "public", "uploadStatus": "processed" } },
                    { "id": "blocked", "status": { "embeddable": false, "privacyStatus": "public", "uploadStatus": "processed" } },
                    { "id": "private", "status": { "embeddable": true, "privacyStatus": "private", "uploadStatus": "processed" } },
                    { "id": "failed", "status": { "embeddable": true, "privacyStatus": "public", "uploadStatus": "failed" } }
                  ]
                }
                """;

        Set<String> ids = new YouTubeService().parsePlayableIds(new ObjectMapper().readTree(payload));

        assertEquals(Set.of("abc123"), ids);
    }

    private static Matchs fixture(String status) {
        Matchs fixture = new Matchs();
        fixture.setId(1);
        fixture.setIdMatch("RMA-BAY-2026");
        fixture.setDateMatch(LocalDate.of(2026, 4, 24));
        fixture.setStatut(status);
        return fixture;
    }

    private static Equipe team(String name) {
        Equipe team = new Equipe();
        team.setNom(name);
        return team;
    }

    private static final class FakeYouTubeService extends YouTubeService {
        private List<YouTubeVideo> searchResults;
        private Set<String> playableIds;
        private boolean searchCalled;
        private boolean statusCalled;
        private int searchCallCount;
        private int statusCallCount;
        private String lastQuery;

        private FakeYouTubeService(List<YouTubeVideo> searchResults, Set<String> playableIds) {
            this.searchResults = searchResults;
            this.playableIds = playableIds;
        }

        @Override
        public List<YouTubeVideo> searchVideos(String query) throws IOException {
            searchCalled = true;
            searchCallCount++;
            lastQuery = query;
            return searchResults;
        }

        @Override
        public Set<String> getPlayableVideoIds(List<String> videoIds) throws IOException {
            statusCalled = true;
            statusCallCount++;
            return playableIds;
        }
    }
}
