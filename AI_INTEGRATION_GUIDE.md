# AI Integration Guide - Google Gemini API

## What I Implemented

I've integrated **Google Gemini AI** to generate personalized training and nutrition recommendations based on player performance data.

---

## How It Works

### 1. **Data Analysis**
The system analyzes:
- Physical, Technical, and Tactical scores (averages)
- Training attendance rate
- Performance trends over time
- Identifies weak areas automatically

### 2. **AI Processing**
- Sends performance data to Google Gemini API
- AI analyzes the data using advanced language models
- Generates personalized recommendations in French

### 3. **Recommendations Generated**
- **Training Recommendations**:
  - Specific exercises for weak areas
  - Training frequency suggestions
  - Progressive difficulty plans
  - Concrete drills and techniques

- **Nutrition Advice**:
  - Daily meal plans (breakfast, lunch, dinner, snacks)
  - Pre/post-training nutrition
  - Hydration recommendations
  - Foods to improve weak areas

---

## API Choice: Google Gemini

### Why Gemini?
✅ **FREE tier available** (60 requests per minute)
✅ **No credit card required** for free tier
✅ **Powerful AI** (comparable to GPT-4)
✅ **Easy to use** REST API
✅ **Good French language support**

### Alternatives Considered:
- **OpenAI GPT-4**: More powerful but requires payment ($)
- **Anthropic Claude**: Good but limited free tier
- **Local AI (Ollama)**: Free but requires local setup and resources

---

## Setup Instructions

### Step 1: Get Your Free API Key

1. Go to: **https://makersuite.google.com/app/apikey**
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the API key (looks like: `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`)

### Step 2: Add API Key to Code

1. Open: `src/main/java/tn/esprit/services/AIRecommendationService.java`
2. Find line 23:
   ```java
   private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";
   ```
3. Replace `YOUR_API_KEY_HERE` with your actual API key:
   ```java
   private static final String GEMINI_API_KEY = "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
   ```
4. Save the file

### Step 3: Compile and Run

```bash
mvn clean compile
mvn javafx:run
```

---

## How to Use

### For Players:

1. **Navigate to Performance Dashboard**
   - Log in as a player
   - Go to "Mon Évolution" / "My Performance"

2. **View Your Stats**
   - See your performance charts
   - Check your averages and improvements

3. **Get AI Recommendations**
   - Click the "🤖 Obtenir des recommandations IA" button
   - Wait a few seconds while AI analyzes your data
   - View personalized training and nutrition advice in tabs

4. **Save Recommendations** (optional)
   - Click "💾 Sauvegarder" to save for later reference

---

## Fallback Mode (Without API Key)

If you don't configure an API key, the system provides **generic recommendations**:
- Basic training exercises for Physical, Technical, Tactical
- General nutrition guidelines
- Standard meal plans

This allows the feature to work even without AI, but recommendations won't be personalized.

---

## Technical Implementation

### Files Created/Modified:

1. **`AIRecommendationService.java`** (NEW)
   - Handles API communication with Gemini
   - Builds prompts based on performance data
   - Parses AI responses
   - Provides fallback recommendations

2. **`PlayerPerformanceController.java`** (MODIFIED)
   - Added AI service integration
   - Created recommendation dialog UI
   - Background thread for API calls (non-blocking)

3. **`pom.xml`** (MODIFIED)
   - Added OkHttp library for HTTP requests
   - Already had Jackson for JSON parsing

### API Request Flow:

```
Player Data → Build Prompt → Send to Gemini API → Parse Response → Display in UI
```

### Example Prompt Sent to AI:

```
Tu es un entraîneur de football professionnel expert. Analyse les performances du joueur et donne des recommandations d'entraînement personnalisées.

Joueur: John Doe
Scores moyens (sur 20):
- Physique: 12.5/20
- Technique: 15.8/20
- Tactique: 11.2/20
- Taux de présence: 85.0%

Fournis des recommandations d'entraînement spécifiques et détaillées:
1. Identifie les points faibles
2. Propose 3-5 exercices concrets pour améliorer chaque domaine faible
3. Suggère une fréquence d'entraînement
4. Donne des conseils de progression

Réponds en français, de manière structurée et motivante. Sois concret et pratique.
```

### API Response Format:

```json
{
  "candidates": [{
    "content": {
      "parts": [{
        "text": "Recommandations détaillées ici..."
      }]
    }
  }]
}
```

---

## Cost & Limits

### Free Tier (Gemini):
- **60 requests per minute**
- **1,500 requests per day**
- **1 million tokens per month**

This is MORE than enough for a sports application!

### Estimated Usage:
- Each recommendation = 1 request
- Average tokens per request = ~500-1000
- Can serve hundreds of players daily

---

## Security Best Practices

### ⚠️ Important:
1. **Never commit API keys to Git**
   - Add to `.gitignore`
   - Use environment variables in production

2. **For Production**:
   - Store API key in configuration file outside code
   - Use environment variables:
     ```java
     private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
     ```

3. **Rate Limiting**:
   - Current implementation has 30-second timeout
   - Consider adding request caching for repeated queries

---

## Future Enhancements

### Phase 2.5 (Optional):
1. **Save Recommendations to Database**
   - Create `ai_recommendations` table
   - Store history of recommendations
   - Track which advice was followed

2. **Progress Tracking**
   - Compare performance before/after following AI advice
   - Measure effectiveness of recommendations

3. **More AI Features**:
   - Injury prevention advice
   - Mental preparation tips
   - Match strategy suggestions
   - Recovery recommendations

---

## Troubleshooting

### Issue: "API call failed: 400/401/403"
**Solution**: 
- Vérifiez que votre clé API Gemini est correcte et active
- Assurez-vous que la clé n'a pas expiré
- Vérifiez les quotas d'API sur Google Cloud Console

### Issue: "API call failed: 429"
**Solution**: Vous avez dépassé les limites de taux, attendez 1 minute

### Issue: "Invalid response format"
**Solution**: L'API Gemini pourrait être indisponible, réessayez plus tard

### Issue: Réponse lente
**Solution**: Normal - Le traitement IA prend 3-10 secondes

### Issue: Mode démo toujours actif
**Solution**: 
1. Vérifiez que vous avez remplacé "YOUR_API_KEY_HERE" par votre vraie clé
2. Vérifiez qu'il n'y a pas d'espaces avant/après la clé
3. Recompilez l'application avec `mvn clean compile`

### Issue: Erreurs de réseau
**Solution**: 
- Vérifiez votre connexion Internet
- Vérifiez que les URL API ne sont pas bloquées par le firewall

---

## Testing

### Test Scenarios:

1. **Mode Démo (sans clé API)**:
   - Génère des recommandations générales détaillées
   - Fonctionne immédiatement sans connexion Internet
   - Fournit des conseils pratiques et utilisables

2. **Avec Clé API**:
   - Génère des recommandations personnalisées par IA
   - Prend 3-10 secondes
   - Montre des conseils spécifiques et détaillés
   - Utilise l'IA Gemini pour l'analyse avancée

### Test Rapide:
```bash
# Compilez le fichier de démonstration
javac -cp ".:target/classes" AIDemoTest.java

# Exécutez le test
java -cp ".:target/classes" AIDemoTest
```

### Vérification:
1. Le service fonctionne en mode démo sans configuration
2. Les messages d'erreur sont clairs et informatifs
3. L'interface utilisateur affiche correctement les recommandations
4. La détection des points faibles fonctionne

2. **Without API Key**:
   - Shows fallback generic recommendations
   - Instant response
   - Includes setup instructions

3. **No Evaluations**:
   - Shows message: "Need at least one evaluation"

---

## Summary

✅ **AI Service Created**: `AIRecommendationService.java`
✅ **Gemini API Integrated**: Free tier, powerful AI
✅ **UI Updated**: Beautiful dialog with tabs
✅ **Fallback Mode**: Works without API key
✅ **Background Processing**: Non-blocking UI
✅ **French Language**: All prompts and responses in French

**Next Step**: Get your free API key and test it!

🔗 **Get API Key**: https://makersuite.google.com/app/apikey
