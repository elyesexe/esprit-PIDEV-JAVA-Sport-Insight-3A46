package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class AssistantNormalizationTest {

    @Test
    void correctsOpenMuchIntoOpenMatch() {
        assertEquals("open match", AssistantService.normalize("Open much"));
    }

    @Test
    void correctsPredictThisMuchIntoMatch() {
        assertEquals("predict this match", AssistantService.normalize("Predict this much"));
    }

    @Test
    void correctsWhoOnThisMuchIntoWhoWonThisMatch() {
        assertEquals("who won this match", AssistantService.normalize("Who on this much"));
    }

    @Test
    void correctsOneThisMuchIntoWonThisMatch() {
        assertEquals("won this match", AssistantService.normalize("One this much"));
    }

    @Test
    void leavesHowMuchQuestionsUntouched() {
        assertEquals("how much does this cost", AssistantService.normalize("How much does this cost?"));
    }

    @Test
    void findsMatchesTargetFromMuchTranscript() {
        AssistantNavigationTarget target = AssistantNavigationTarget.findMatch("Open much details").orElseThrow();
        assertSame(AssistantNavigationTarget.MATCHES, target);
    }

    @Test
    void treatsWonThisMatchAsWinnerQuestion() throws Exception {
        Method winnerMethod = AssistantService.class.getDeclaredMethod("looksLikeWinnerQuestion", String.class);
        winnerMethod.setAccessible(true);
        boolean winnerResult = (boolean) winnerMethod.invoke(null, "won this match");
        assertTrue(winnerResult);

        Method scoreMethod = AssistantService.class.getDeclaredMethod("looksLikeScoreQuestion", String.class);
        scoreMethod.setAccessible(true);
        boolean scoreResult = (boolean) scoreMethod.invoke(null, "won this match");
        assertEquals(false, scoreResult);
    }
}
