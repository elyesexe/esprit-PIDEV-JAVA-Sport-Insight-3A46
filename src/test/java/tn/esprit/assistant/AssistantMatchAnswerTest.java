package tn.esprit.assistant;

import org.junit.jupiter.api.Test;
import tn.esprit.Controller.MatchDetailController;
import tn.esprit.services.football.ApiFootballMatchIncident;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantMatchAnswerTest {

    @Test
    void winnerReplyIsDirectForFinishedMatch() {
        assertEquals(
                "Real Madrid won 2 : 1.",
                AssistantService.buildWinnerReplyText("Real Madrid", "Deportivo Alaves", 2, 1, "2 : 1", "Fini")
        );
    }

    @Test
    void winnerReplyHandlesDrawDirectly() {
        assertEquals(
                "It ended in a draw, 1 : 1.",
                AssistantService.buildWinnerReplyText("Real Madrid", "Deportivo Alaves", 1, 1, "1 : 1", "Finished")
        );
    }

    @Test
    void scorerReplyListsPlayerNamesDirectly() {
        assertEquals(
                "Scorers: Kylian Mbappe (15), Vinicius Junior (78).",
                AssistantService.summarizeGoalScorers(List.of("Kylian Mbappe (15)", "Vinicius Junior (78)"))
        );
    }

    @Test
    void controllerGroupsMultipleGoalsFromSamePlayer() throws Exception {
        MatchDetailController controller = new MatchDetailController();
        Field currentIncidentsField = MatchDetailController.class.getDeclaredField("currentIncidents");
        currentIncidentsField.setAccessible(true);
        currentIncidentsField.set(controller, List.of(
                new ApiFootballMatchIncident("goal", "Regular", "12", 12, null, true, "Kylian Mbappe", 9L, null, null, null, null, null, null, null, 1, 0),
                new ApiFootballMatchIncident("goal", "Regular", "77", 77, null, true, "Kylian Mbappe", 9L, null, null, null, null, null, null, null, 2, 0),
                new ApiFootballMatchIncident("goal", "Regular", "81", 81, null, false, "Vinicius Junior", 7L, null, null, null, null, null, null, null, 2, 1)
        ));

        assertEquals(
                List.of("Kylian Mbappe (12, 77)", "Vinicius Junior (81)"),
                controller.getCurrentGoalScorerSummaries()
        );
    }
}
