package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantEntityCorrectionTest {

    @Test
    void correctsArdaGulerFromMisheardTranscript() {
        assertEquals(
                "take me to arda guler profile",
                AssistantEntityLexicon.correctTranscriptWithPhrases(
                        "take me to art de guler profile",
                        List.of("arda guler", "kylian mbappe", "champions league")
                )
        );
    }

    @Test
    void correctsKylianMbappeFromMisheardTranscript() {
        assertEquals(
                "open kylian mbappe profile",
                AssistantEntityLexicon.correctTranscriptWithPhrases(
                        "open kilian pape profile",
                        List.of("arda guler", "kylian mbappe", "champions league")
                )
        );
    }

    @Test
    void fuzzyMatcherPrefersCloserFootballName() {
        double ardaScore = AssistantFuzzyMatcher.similarity("art de guler", "arda guler");
        double judeScore = AssistantFuzzyMatcher.similarity("art de guler", "jude bellingham");

        assertTrue(ardaScore > judeScore);
        assertTrue(ardaScore >= 0.82);
    }
}
