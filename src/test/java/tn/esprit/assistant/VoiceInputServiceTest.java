package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceInputServiceTest {

    @Test
    void asksForClarificationOnLowConfidenceQuickTranscript() {
        assertTrue(VoiceInputService.shouldRequestClarification("who won this match", 0.42, false));
    }

    @Test
    void trustsRefinedTranscriptAtHealthyConfidence() {
        assertFalse(VoiceInputService.shouldRequestClarification("who won this match", 0.82, true));
    }
}
