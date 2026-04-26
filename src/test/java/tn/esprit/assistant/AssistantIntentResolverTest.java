package tn.esprit.assistant;

import org.junit.jupiter.api.Test;
import tn.esprit.Controller.MatchDetailController;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantIntentResolverTest {

    @Test
    void resolvesAssistFollowUpAgainstRecentMatchMemory() {
        AssistantConversationMemory memory = new AssistantConversationMemory();
        memory.rememberMatchSnapshot(
                new AssistantConversationMemory.MatchSnapshot(
                        "Real Madrid vs Deportivo Alaves",
                        "Real Madrid",
                        "Deportivo Alaves",
                        2,
                        1,
                        "2 : 1",
                        "Fini",
                        "La Liga",
                        List.of("Kylian Mbappe (12)", "Vinicius Junior (81)"),
                        List.of("Jude Bellingham (12)"),
                        List.of(),
                        Instant.now()
                ),
                AssistantIntentType.MATCH_SCORERS
        );

        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "and who assisted",
                new AssistantService.Context("/tn/esprit/views/home-view.fxml", "Sport Insight", true, false, "Tester", null),
                memory.snapshot()
        );

        assertEquals(AssistantIntentType.MATCH_ASSISTS, intent.type());
        assertEquals(AssistantIntentTarget.CURRENT_MATCH, intent.target());
        assertTrue(intent.followUp());
    }

    @Test
    void flagsRiskyActionForConfirmation() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "delete this match",
                new AssistantService.Context("/tn/esprit/views/match-detail-view.fxml", "Match", true, false, "Tester", null),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.RISKY_ACTION, intent.type());
        assertTrue(intent.policy().requiresConfirmation());
    }

    @Test
    void resolvesMatchDetailTabActionAsSafeAction() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "open statistics",
                new AssistantService.Context("/tn/esprit/views/match-detail-view.fxml", "Match", true, false, "Tester", new MatchDetailController()),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.SAFE_ACTION, intent.type());
        assertEquals("open_stats", intent.subject());
    }

    @Test
    void resolvesPlayerDetailAgeQuestionAgainstCurrentPlayer() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "how old is this player",
                playerContext(),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.PLAYER_AGE, intent.type());
        assertEquals(AssistantIntentTarget.CURRENT_PLAYER, intent.target());
    }

    @Test
    void resolvesPlayerDetailSeasonStatsQuestionAgainstCurrentPlayer() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "what are his season stats",
                playerContext(),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.PLAYER_SEASON_STATS, intent.type());
        assertEquals(AssistantIntentTarget.CURRENT_PLAYER, intent.target());
    }

    @Test
    void resolvesPlayerDetailIdentityQuestionsAgainstCurrentPlayer() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();

        assertEquals(
                AssistantIntentType.PLAYER_NATIONALITY,
                resolver.resolve("what is his nationality", playerContext(), new AssistantConversationMemory().snapshot()).type()
        );
        assertEquals(
                AssistantIntentType.PLAYER_CLUB,
                resolver.resolve("which club does he play for", playerContext(), new AssistantConversationMemory().snapshot()).type()
        );
        assertEquals(
                AssistantIntentType.PLAYER_POSITION,
                resolver.resolve("what position does he play", playerContext(), new AssistantConversationMemory().snapshot()).type()
        );
    }

    @Test
    void resolvesPlayerDetailRecentFormQuestionAgainstCurrentPlayer() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "what is his recent form",
                playerContext(),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.PLAYER_RECENT_FORM, intent.type());
        assertEquals(AssistantIntentTarget.CURRENT_PLAYER, intent.target());
    }

    @Test
    void playerAgeIntentDoesNotTriggerFromPageText() {
        AssistantIntentResolver resolver = new AssistantIntentResolver();
        AssistantIntent intent = resolver.resolve(
                "explain this page",
                playerContext(),
                new AssistantConversationMemory().snapshot()
        );

        assertEquals(AssistantIntentType.CURRENT_SCREEN, intent.type());
    }

    private AssistantService.Context playerContext() {
        return new AssistantService.Context(
                "/tn/esprit/views/joueur-detail-view.fxml",
                "Fiche joueur",
                true,
                false,
                "Tester",
                new PlayerProfileControllerStub()
        );
    }

    private static final class PlayerProfileControllerStub implements AssistantPlayerProfileProvider {
        @Override
        public AssistantPlayerProfileSnapshot assistantPlayerProfileSnapshot() {
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
}
