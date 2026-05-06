import tn.esprit.services.AIRecommendationService;
import java.io.IOException;

/**
 * Demonstration of the AI Recommendation Service
 * This shows how to use the service and test it
 */
public class AIDemoTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test du Service de Recommandations IA ===\n");
        
        AIRecommendationService aiService = new AIRecommendationService();
        
        try {
            System.out.println("1. Test des recommandations d'entraînement:");
            System.out.println("-------------------------------------------");
            String trainingAdvice = aiService.generateTrainingRecommendations(
                "Mohamed Salah",
                16.5,  // avgPhysique
                18.2,  // avgTechnique  
                15.8,  // avgTactique
                92.5   // attendanceRate
            );
            System.out.println(trainingAdvice);
            
            System.out.println("\n\n2. Test des conseils nutritionnels:");
            System.out.println("-----------------------------------");
            String nutritionAdvice = aiService.generateNutritionAdvice(
                "Mohamed Salah",
                16.5,  // avgPhysique
                4.0,   // trainingFrequency
                "Tactique"  // weakArea
            );
            System.out.println(nutritionAdvice);
            
            System.out.println("\n\n3. Test des conseils complets:");
            System.out.println("-----------------------------");
            AIRecommendationService.AIAdvice comprehensiveAdvice = aiService.generateComprehensiveAdvice(
                "Mohamed Salah",
                16.5,  // avgPhysique
                18.2,  // avgTechnique
                15.8,  // avgTactique
                92.5,  // attendanceRate
                15     // totalEvaluations
            );
            
            System.out.println("Point faible identifié: " + comprehensiveAdvice.weakArea());
            System.out.println("\nRecommandations d'entraînement:");
            System.out.println(comprehensiveAdvice.trainingRecommendations());
            System.out.println("\nConseils nutritionnels:");
            System.out.println(comprehensiveAdvice.nutritionAdvice());
            
            System.out.println("\n✅ Test réussi! Le service fonctionne en mode démo.");
            System.out.println("\n⚠️ Pour activer l'IA Gemini:");
            System.out.println("1. Obtenez une clé API gratuite: https://makersuite.google.com/app/apikey");
            System.out.println("2. Remplacez 'YOUR_API_KEY_HERE' dans AIRecommendationService.java");
            System.out.println("3. Recompilez et relancez le test");
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors du test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}