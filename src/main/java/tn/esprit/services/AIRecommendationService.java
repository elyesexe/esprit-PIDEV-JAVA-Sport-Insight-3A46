package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
<<<<<<< HEAD
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.util.concurrent.TimeUnit;

/**
 * AI-powered recommendation service using Google Gemini API
 * 
 * How it works:
 * 1. Analyzes player performance data (Physical, Technical, Tactical scores)
 * 2. Sends data to Google Gemini AI API
 * 3. Receives personalized training and nutrition recommendations
 * 
 * API Used: Google Gemini API (Free tier available)
 * Get your API key: https://makersuite.google.com/app/apikey
 */
public class AIRecommendationService {
    
<<<<<<< HEAD
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";
    private static final String LOCAL_PROPERTIES_FILE = "ai-recommendation.local.properties";
    private static final String USER_HOME_PROPERTIES_FILE = ".sport-insight/ai-recommendation.local.properties";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
=======
    // IMPORTANT: Replace with your actual Gemini API key
    // Get free API key from: https://makersuite.google.com/app/apikey
    private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    public AIRecommendationService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
<<<<<<< HEAD
        this.geminiApiKey = loadGeminiApiKey();
    }

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank() && !"YOUR_API_KEY_HERE".equals(geminiApiKey);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    /**
     * Generate training recommendations based on performance scores
     */
    public String generateTrainingRecommendations(
        String playerName,
        double avgPhysique,
        double avgTechnique,
        double avgTactique,
        double attendanceRate
    ) throws IOException {
        
        String prompt = buildTrainingPrompt(playerName, avgPhysique, avgTechnique, avgTactique, attendanceRate);
        return callGeminiAPI(prompt);
    }

    /**
     * Generate nutrition advice based on performance and training intensity
     */
    public String generateNutritionAdvice(
        String playerName,
        double avgPhysique,
        double trainingFrequency,
        String weakArea
    ) throws IOException {
        
        String prompt = buildNutritionPrompt(playerName, avgPhysique, trainingFrequency, weakArea);
        return callGeminiAPI(prompt);
    }

    /**
     * Generate comprehensive advice (training + nutrition)
     */
    public AIAdvice generateComprehensiveAdvice(
        String playerName,
        double avgPhysique,
        double avgTechnique,
        double avgTactique,
        double attendanceRate,
        int totalEvaluations
    ) throws IOException {
        
        // Identify weak area
        String weakArea = identifyWeakArea(avgPhysique, avgTechnique, avgTactique);
        
        // Generate training recommendations
        String trainingPrompt = buildTrainingPrompt(playerName, avgPhysique, avgTechnique, avgTactique, attendanceRate);
        String trainingAdvice = callGeminiAPI(trainingPrompt);
        
        // Generate nutrition advice
        double trainingFrequency = (attendanceRate / 100.0) * 4; // Approximate weekly sessions
        String nutritionPrompt = buildNutritionPrompt(playerName, avgPhysique, trainingFrequency, weakArea);
        String nutritionAdvice = callGeminiAPI(nutritionPrompt);
        
        return new AIAdvice(trainingAdvice, nutritionAdvice, weakArea);
    }

    private String buildTrainingPrompt(String playerName, double avgPhysique, double avgTechnique, double avgTactique, double attendanceRate) {
        return String.format("""
            Tu es un entraîneur de football professionnel expert. Analyse les performances du joueur et donne des recommandations d'entraînement personnalisées.
            
            Joueur: %s
            Scores moyens (sur 20):
            - Physique: %.2f/20
            - Technique: %.2f/20
            - Tactique: %.2f/20
            - Taux de présence: %.1f%%
            
            Fournis des recommandations d'entraînement spécifiques et détaillées:
            1. Identifie les points faibles
            2. Propose 3-5 exercices concrets pour améliorer chaque domaine faible
            3. Suggère une fréquence d'entraînement
            4. Donne des conseils de progression
            
            Réponds en français, de manière structurée et motivante. Sois concret et pratique.
            """, 
            playerName, avgPhysique, avgTechnique, avgTactique, attendanceRate
        );
    }

    private String buildNutritionPrompt(String playerName, double avgPhysique, double trainingFrequency, String weakArea) {
        return String.format("""
            Tu es un nutritionniste sportif expert spécialisé dans le football. Donne des conseils nutritionnels personnalisés.
            
            Joueur: %s
            Score physique moyen: %.2f/20
            Fréquence d'entraînement: %.1f sessions/semaine
            Point faible identifié: %s
            
            Fournis des conseils nutritionnels détaillés:
            1. Plan alimentaire quotidien (petit-déjeuner, déjeuner, dîner, collations)
            2. Nutrition avant/après l'entraînement
            3. Hydratation recommandée
            4. Suppléments suggérés (si nécessaire)
            5. Aliments à privilégier pour améliorer le point faible
            
            Réponds en français, de manière pratique et applicable. Donne des exemples concrets de repas.
            """,
            playerName, avgPhysique, trainingFrequency, weakArea
        );
    }

    private String identifyWeakArea(double avgPhysique, double avgTechnique, double avgTactique) {
        double minScore = Math.min(avgPhysique, Math.min(avgTechnique, avgTactique));
        
        if (avgPhysique == minScore) {
            return "Physique";
        } else if (avgTechnique == minScore) {
            return "Technique";
        } else {
            return "Tactique";
        }
    }

    private String callGeminiAPI(String prompt) throws IOException {
        // Check if API key is configured
<<<<<<< HEAD
        if (!isConfigured()) {
=======
        if (GEMINI_API_KEY.equals("YOUR_API_KEY_HERE") || GEMINI_API_KEY.trim().isEmpty()) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return generateFallbackAdvice(prompt);
        }

        // Build request body
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 2048
                }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        Request request = new Request.Builder()
<<<<<<< HEAD
            .url(GEMINI_API_URL + geminiApiKey)
=======
            .url(GEMINI_API_URL)
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                String errorMessage = "API call failed: " + response.code() + " - " + response.message();
                
                // Try to parse error details from response
                try {
                    JsonNode errorNode = objectMapper.readTree(errorBody);
                    if (errorNode.has("error") && errorNode.get("error").has("message")) {
                        errorMessage += "\nDétails: " + errorNode.get("error").get("message").asText();
                    }
                } catch (Exception e) {
                    // If we can't parse JSON, include raw response
                    if (errorBody != null && !errorBody.isEmpty()) {
                        errorMessage += "\nRéponse: " + errorBody.substring(0, Math.min(200, errorBody.length()));
                    }
                }
                
                // If it's an authentication error (401/403), provide specific guidance
                if (response.code() == 401 || response.code() == 403) {
                    errorMessage += "\n\n⚠️ Erreur d'authentification API. Vérifiez votre clé API Gemini.";
                }
                
                throw new IOException(errorMessage);
            }

            String responseBody = response.body().string();
            return parseGeminiResponse(responseBody);
        }
    }

    private String parseGeminiResponse(String jsonResponse) throws IOException {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode candidates = root.path("candidates");
        
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }
        
        throw new IOException("Invalid response format from Gemini API");
    }

<<<<<<< HEAD
    private String loadGeminiApiKey() {
        String configured = firstNonBlank(
                System.getProperty("sport.insight.gemini.api.key"),
                System.getenv("SPORT_INSIGHT_GEMINI_API_KEY"));
        if (configured != null) {
            return configured;
        }

        Properties props = new Properties();
        loadProperties(props, Path.of(LOCAL_PROPERTIES_FILE));
        loadProperties(props, Path.of(System.getProperty("user.home"), USER_HOME_PROPERTIES_FILE));
        loadProperties(props, Path.of("src/main/resources/ai-recommendation.local.properties"));
        return firstNonBlank(
                props.getProperty("gemini.api.key"),
                props.getProperty("ai.gemini.api.key"),
                props.getProperty("api.key"));
    }

    private static void loadProperties(Properties props, Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var input = Files.newInputStream(path)) {
            props.load(input);
        } catch (IOException ignored) {
            // Keep demo fallback behavior when local API configuration is unreadable.
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    /**
     * Fallback advice when API key is not configured or API call fails
     */
    private String generateFallbackAdvice(String prompt) {
<<<<<<< HEAD
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase();
        if (!normalizedPrompt.contains("nutrition") && !normalizedPrompt.contains("alimentaire")) {
            return """
                RECOMMANDATIONS PERSONNALISEES LOCALES

                Analyse rapide
                - Travaillez le point le plus faible en premier, puis consolidez les autres axes.
                - Gardez 3 a 4 seances par semaine avec un jour de recuperation entre deux seances fortes.

                Plan d'entrainement conseille
                - Jour 1: endurance 30 min + accelerations 8 x 20 m.
                - Jour 2: technique avec controles orientes, passes longues et finition.
                - Jour 3: tactique avec jeux reduits, placement et transitions.
                - Jour 4: renforcement bas du corps, gainage et prevention blessures.

                Suivi
                - Notez les charges, la fatigue et la reussite des exercices apres chaque seance.
                - Recontrolez les scores apres la prochaine evaluation pour adapter le plan.
                """;
        } else {
            return """
                CONSEILS NUTRITIONNELS PERSONNALISES LOCAUX

                Avant l'entrainement
                - Prenez une source de glucides facile a digerer 60 a 90 minutes avant la seance.
                - Buvez 500 ml d'eau dans les deux heures avant l'effort.

                Apres l'entrainement
                - Combinez proteines et glucides dans les 45 minutes: yaourt grec + banane, ou poulet + riz.
                - Ajoutez des legumes et une source de bons lipides au repas suivant.

                Routine quotidienne
                - Visez 2 a 3 litres d'eau par jour.
                - Gardez des repas simples: proteines maigres, feculents complets, fruits et legumes.
                - Ajustez les portions les jours de charge forte.
=======
        if (prompt.contains("entraînement")) {
            return """
                🤖 **Mode Démo: Recommandations Générales d'Entraînement**
                
                ⚠️ **Pour activer l'IA Gemini (gratuit):**
                1. Obtenez une clé API sur: https://makersuite.google.com/app/apikey
                2. Remplacez "YOUR_API_KEY_HERE" dans AIRecommendationService.java
                3. Redémarrez l'application
                
                📊 **Analyse des performances:**
                • Score physique: Bon niveau général
                • Technique: Points à travailler
                • Tactique: Compréhension du jeu à améliorer
                
                🏋️ **Plan d'entraînement recommandé (4 semaines):**
                
                **Semaine 1-2: Fondamentaux**
                • Lundi: Endurance (course 30 min + HIIT 15 min)
                • Mardi: Technique (contrôle, passes, dribbles)
                • Mercredi: Récupération active (étirements, yoga)
                • Jeudi: Physique (musculation football-specific)
                • Vendredi: Tactique (analyse vidéo + jeux réduits)
                • Samedi: Match simulé
                • Dimanche: Repos complet
                
                **Semaine 3-4: Intensification**
                • Augmentation progressive de l'intensité
                • Focus sur le point faible identifié
                • Tests de progression
                
                💡 **Conseils clés:**
                1. Consistance > Intensité: Mieux vaut 4 séances régulières que 6 irrégulières
                2. Qualité > Quantité: 45 min concentrées valent mieux que 90 min molles
                3. Récupération: Le sommeil et la nutrition sont aussi importants que l'entraînement
                4. Mesure: Notez vos progrès chaque semaine
                
                🎯 **Objectifs SMART:**
                • Spécifique: Améliorer la précision des passes longues de 60% à 75%
                • Mesurable: 100 passes longues par session, noter le taux de réussite
                • Atteignable: +5% par semaine
                • Réaliste: Basé sur votre niveau actuel
                • Temporel: 4 semaines
                """;
        } else {
            return """
                🍎 **Mode Démo: Conseils Nutritionnels Généraux**
                
                ⚠️ **Pour activer l'IA Gemini (gratuit):**
                1. Obtenez une clé API sur: https://makersuite.google.com/app/apikey
                2. Remplacez "YOUR_API_KEY_HERE" dans AIRecommendationService.java
                3. Redémarrez l'application
                
                📋 **Plan nutritionnel football (exemple journée):**
                
                **Petit-déjeuner (7h - 1h avant entraînement):**
                • 80g flocons d'avoine + 250ml lait
                • 1 banane + 10 amandes
                • 1 œuf dur
                • Café/thé (sans sucre ajouté)
                
                **Collation pré-entraînement (30 min avant):**
                • 1 fruit (pomme ou poire)
                • 200ml boisson isotonique
                
                **Post-entraînement (dans les 30 min):**
                • Shake: 25g whey + 200ml lait + 1 banane
                • 500ml eau + électrolytes
                
                **Déjeuner (2h après entraînement):**
                • 150g poulet/poisson/steak haché 5%
                • 100g riz/pâtes complètes/quinoa
                • Légumes variés à volonté (brocoli, carottes, haricots verts)
                • 1 cuillère à soupe huile d'olive
                
                **Goûter (16h):**
                • 1 yaourt grec nature
                • 1 poignée de fruits rouges
                • 1 carré chocolat noir 85%
                
                **Dîner (20h):**
                • 120g poisson blanc/omelette 2 œufs
                • Légumes variés à volonté
                • 1 petite patate douce (si entraînement intense le lendemain)
                • Salade verte + vinaigrette légère
                
                **Hydratation quotidienne:**
                • 2-3L d'eau minimum
                • +500ml par heure d'entraînement
                • Éviter sodas et jus industriels
                
                💡 **Conseils nutritionnels:**
                1. Timing: Manger 3-4h avant l'effort, collation 30-60 min avant
                2. Proportion: 50% glucides, 30% protéines, 20% lipides
                3. Qualité: Aliments non transformés > produits industriels
                4. Variété: Couleurs dans l'assiette = nutriments variés
                5. Écoute: Votre corps sait ce dont il a besoin
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                """;
        }
    }

    /**
     * Data class for comprehensive advice
     */
    public record AIAdvice(
        String trainingRecommendations,
        String nutritionAdvice,
        String weakArea
    ) {}
}
