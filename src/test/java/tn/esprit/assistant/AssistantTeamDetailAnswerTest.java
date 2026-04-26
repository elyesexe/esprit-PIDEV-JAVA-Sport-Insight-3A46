package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantTeamDetailAnswerTest {

    @Test
    void answersCoachFromCurrentTeamDetail() {
        AssistantService.Reply reply = AssistantService.tryBuildTeamDetailAnswer(
                AssistantService.normalize("Who is the coach?"),
                snapshot()
        ).orElseThrow();

        assertEquals("The coach of Tunis United is Amine Trabelsi.", reply.text());
        assertNull(reply.command());
        assertTrue(reply.localHandled());
    }

    @Test
    void answersCompetitionFromCurrentTeamDetail() {
        AssistantService.Reply reply = AssistantService.tryBuildTeamDetailAnswer(
                AssistantService.normalize("Which competition is this team in?"),
                snapshot()
        ).orElseThrow();

        assertEquals("Tunis United is in Ligue 1.", reply.text());
        assertTrue(reply.localHandled());
    }

    @Test
    void summarizesVisibleTeamInfoFromSnapshot() {
        AssistantService.Reply reply = AssistantService.tryBuildTeamDetailAnswer(
                AssistantService.normalize("Give me the visible team info"),
                snapshot()
        ).orElseThrow();

        assertEquals(
                "Tunis United: competition Ligue 1, coach Amine Trabelsi, 24 players, source API-Football. Contact: address Tunis, phone +216 71 000 000, email contact@tunis.test. Visible squad sample: Sami Ben Ali (Forward), Youssef Mansouri (Midfielder).",
                reply.text()
        );
    }

    @Test
    void answersVisibleSquadAndMatchPanelsFromSnapshot() {
        assertEquals(
                "The visible squad for Tunis United shows 24 players. Sample: Sami Ben Ali (Forward), Youssef Mansouri (Midfielder).",
                AssistantService.tryBuildTeamDetailAnswer(AssistantService.normalize("How many players are in this squad?"), snapshot()).orElseThrow().text()
        );
        assertEquals(
                "Top scorers for Tunis United: 1. Sami Ben Ali - 14 goals | 2. Youssef Mansouri - 8 goals.",
                AssistantService.tryBuildTeamDetailAnswer(AssistantService.normalize("Who are the top scorers?"), snapshot()).orElseThrow().text()
        );
        assertEquals(
                "Next matches for Tunis United: Tunis United vs Club Nord on 28/04/2026 at 20:00.",
                AssistantService.tryBuildTeamDetailAnswer(AssistantService.normalize("What is the next match?"), snapshot()).orElseThrow().text()
        );
        assertEquals(
                "Recent results for Tunis United: WIN 2 : 0 - Tunis United vs Club Sud.",
                AssistantService.tryBuildTeamDetailAnswer(AssistantService.normalize("Show recent results"), snapshot()).orElseThrow().text()
        );
    }

    @Test
    void doesNotHijackTeamNavigationRequests() {
        assertTrue(AssistantService.tryBuildTeamDetailAnswer(
                AssistantService.normalize("Open teams"),
                snapshot()
        ).isEmpty());
        assertTrue(AssistantService.tryBuildTeamDetailAnswer(
                AssistantService.normalize("Show league table"),
                snapshot()
        ).isEmpty());
    }

    private AssistantTeamDetailSnapshot snapshot() {
        return new AssistantTeamDetailSnapshot(
                "Tunis United",
                "Ligue 1 | Amine Trabelsi",
                "Amine Trabelsi",
                "Ligue 1",
                "24",
                "Tunis",
                "+216 71 000 000",
                "contact@tunis.test",
                "API-Football",
                List.of("Sami Ben Ali (Forward)", "Youssef Mansouri (Midfielder)"),
                List.of("1. Sami Ben Ali - 14 goals", "2. Youssef Mansouri - 8 goals"),
                List.of("Tunis United vs Club Nord on 28/04/2026 at 20:00"),
                List.of("WIN 2 : 0 - Tunis United vs Club Sud"),
                "Meilleurs buteurs actualises."
        );
    }
}
