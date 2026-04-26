package tn.esprit.assistant;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Joueur;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantPlayerNavigationTest {

    @Test
    void extractsNamedPlayerFocusFromProfileCommand() throws Exception {
        Method focusMethod = AssistantService.class.getDeclaredMethod("extractPlayerSearchFocus", String.class);
        focusMethod.setAccessible(true);
        assertEquals("arda guler", focusMethod.invoke(null, AssistantService.normalize("Take me to Arda Guler profile")));
    }

    @Test
    void detectsSpecificPlayerProfileNavigationWithoutHijackingMyProfile() throws Exception {
        Method detectMethod = AssistantService.class.getDeclaredMethod("looksLikeSpecificPlayerNavigationRequest", String.class);
        detectMethod.setAccessible(true);

        assertTrue((boolean) detectMethod.invoke(null, AssistantService.normalize("Take me to Arda Guler profile")));
        assertFalse((boolean) detectMethod.invoke(null, AssistantService.normalize("Open my profile")));
    }

    @Test
    void scoresRequestedPlayerHigherThanOtherPlayers() throws Exception {
        Method scoreMethod = AssistantService.class.getDeclaredMethod("scorePlayerCandidate", String.class, Joueur.class);
        scoreMethod.setAccessible(true);

        String query = AssistantService.normalize("Arda Guler");
        Joueur arda = new Joueur(10, "Guler", "Arda", null, 15, null, 1);
        Joueur jude = new Joueur(5, "Bellingham", "Jude", null, 5, null, 1);

        int ardaScore = (int) scoreMethod.invoke(null, query, arda);
        int judeScore = (int) scoreMethod.invoke(null, query, jude);

        assertTrue(ardaScore > judeScore);
        assertTrue(ardaScore >= 155);
    }
}
