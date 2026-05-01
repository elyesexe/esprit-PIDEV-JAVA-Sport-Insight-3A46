package tn.esprit.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SponsorSentimentAnalysisServiceTest {

    private final SponsorSentimentAnalysisService service = new SponsorSentimentAnalysisService();

    @Test
    void detectsPositiveSponsorFeedback() {
        SponsorSentimentAnalysisService.SponsorSentimentAnalysis analysis = service.analyze(
                "Merci pour cette excellente collaboration. La visibilite est parfaite et nous souhaitons renouveler le contrat.",
                "Delice"
        );

        assertEquals(SponsorSentimentAnalysisService.Sentiment.POSITIVE, analysis.sentiment());
        assertTrue(analysis.score() > 0);
        assertEquals("OPPORTUNITY", analysis.priority());
        assertTrue(analysis.topics().contains("VISIBILITY"));
        assertFalse(analysis.recommendedActions().isEmpty());
    }

    @Test
    void detectsNegativeSponsorRisk() {
        SponsorSentimentAnalysisService.SponsorSentimentAnalysis analysis = service.analyze(
                "Nous sommes decus, il y a un retard important et aucun retour. Nous ne sommes pas satisfait et pensons annuler.",
                "Mercedes"
        );

        assertEquals(SponsorSentimentAnalysisService.Sentiment.NEGATIVE, analysis.sentiment());
        assertTrue(analysis.score() < 0);
        assertEquals("HIGH", analysis.priority());
        assertTrue(analysis.topics().contains("DELAY"));
        assertTrue(analysis.responseDraft().contains("action corrective"));
    }

    @Test
    void keepsInformationalMessageNeutral() {
        SponsorSentimentAnalysisService.SponsorSentimentAnalysis analysis = service.analyze(
                "Pouvez-vous envoyer les informations du contrat et confirmer la date du prochain match ?",
                "Ooredoo"
        );

        assertEquals(SponsorSentimentAnalysisService.Sentiment.NEUTRAL, analysis.sentiment());
        assertTrue(Math.abs(analysis.score()) < 15);
        assertTrue(analysis.topics().contains("CONTRACT"));
        assertTrue(analysis.responseDraft().contains("confirmer le besoin"));
    }
}
