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
}
