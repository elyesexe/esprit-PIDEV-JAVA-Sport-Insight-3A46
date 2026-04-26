package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantPlayerProfileAnswerTest {

    @Test
    void repliesWithVisiblePlayerAgeAndBirthDate() {
        assertEquals(
                "Kylian Mbappe is 27 ans. Birth date: 20/12/1998.",
                AssistantService.buildPlayerAgeReply(profile())
        );
    }

    @Test
    void repliesWithVisiblePlayerIdentityFields() {
        AssistantPlayerProfileSnapshot profile = profile();

        assertEquals("Kylian Mbappe's nationality is France.", AssistantService.buildPlayerNationalityReply(profile));
        assertEquals("Kylian Mbappe's club is Real Madrid.", AssistantService.buildPlayerClubReply(profile));
        assertEquals("Kylian Mbappe's position is Forward.", AssistantService.buildPlayerPositionReply(profile));
    }

    @Test
    void repliesWithVisibleSeasonStatsAndContext() {
        assertEquals(
                "Season stats for Kylian Mbappe: 31 appearances, 25 goals, 4 assists, 3 yellow cards, 0 red cards, 2460 minutes. Context: Real Madrid | La Liga | saison 2025/2026.",
                AssistantService.buildPlayerSeasonStatsReply(profile())
        );
    }

    @Test
    void doesNotInventRecentFormWhenNoVisibleFormExists() {
        assertEquals(
                "I don't see recent form on this player profile screen. I can answer from the visible profile fields and loaded season stats.",
                AssistantService.buildPlayerRecentFormReply(profile())
        );
    }

    @Test
    void repliesWithVisibleRecentFormWhenProvidedByScreen() {
        AssistantPlayerProfileSnapshot profile = new AssistantPlayerProfileSnapshot(
                "Kylian Mbappe",
                "Real Madrid | Forward | France",
                "Real Madrid",
                "#10",
                "20/12/1998",
                "27 ans",
                "Forward",
                "France",
                "API-Football",
                "Real Madrid | La Liga | saison 2025/2026",
                "31",
                "25",
                "4",
                "3",
                "0",
                "2460",
                "W-W-D-L-W"
        );

        assertEquals(
                "Recent form for Kylian Mbappe: W-W-D-L-W.",
                AssistantService.buildPlayerRecentFormReply(profile)
        );
    }

    private AssistantPlayerProfileSnapshot profile() {
        return new AssistantPlayerProfileSnapshot(
                "Kylian Mbappe",
                "Real Madrid | Forward | France",
                "Real Madrid",
                "#10",
                "20/12/1998",
                "27 ans",
                "Forward",
                "France",
                "API-Football",
                "Real Madrid | La Liga | saison 2025/2026",
                "31",
                "25",
                "4",
                "3",
                "0",
                "2460",
                null
        );
    }
}
