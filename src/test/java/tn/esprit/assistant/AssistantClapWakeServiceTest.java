package tn.esprit.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantClapWakeServiceTest {

    @Test
    void detectsDoubleClapWithinWakeWindow() {
        AssistantClapPatternDetector detector = new AssistantClapPatternDetector();

        assertFalse(detector.accept(0.02, 0.05, 0));
        assertFalse(detector.accept(0.03, 0.24, 120));
        assertTrue(detector.accept(0.03, 0.22, 700));
    }

    @Test
    void ignoresClapsThatAreTooCloseTogether() {
        AssistantClapPatternDetector detector = new AssistantClapPatternDetector();

        assertFalse(detector.accept(0.03, 0.24, 100));
        assertFalse(detector.accept(0.03, 0.23, 420));
    }

    @Test
    void allowsSharpTransientClapsWithLowerRms() {
        AssistantClapPatternDetector detector = new AssistantClapPatternDetector();

        assertFalse(detector.accept(0.025, 0.19, 100));
        assertTrue(detector.accept(0.024, 0.18, 650));
    }

    @Test
    void ignoresClapsThatAreTooFarApart() {
        AssistantClapPatternDetector detector = new AssistantClapPatternDetector();

        assertFalse(detector.accept(0.03, 0.24, 100));
        assertFalse(detector.accept(0.03, 0.23, 1_250));
    }
}
