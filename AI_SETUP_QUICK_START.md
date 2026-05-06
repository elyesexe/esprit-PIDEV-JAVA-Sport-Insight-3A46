# 🤖 AI Setup - Quick Start (5 Minutes)

## What You Get

AI-powered personalized recommendations for:
- 🏃 **Training exercises** based on weak areas
- 🍎 **Nutrition plans** for optimal performance
- 📊 **Progress tracking** advice

✨ **Nouveau**: Mode démo intégré - fonctionne sans configuration!

---

## Setup en 3 Étapes

### Option A: Mode Démo (0 minute)
✅ **Fonctionne immédiatement sans configuration**
- Recommandations générales détaillées
- Conseils pratiques et utilisables
- Pas besoin de clé API ou d'Internet

### Option B: Mode IA Complète (3 minutes)

#### 1️⃣ Obtenir une Clé API Gratuite (2 minutes)

Allez sur: **https://makersuite.google.com/app/apikey**

- Connectez-vous avec Google
- Cliquez sur "Create API Key"
- Copiez la clé (commence par `AIzaSy...`)

#### 2️⃣ Ajouter au Code (1 minute)

Ouvrez: `src/main/java/tn/esprit/services/AIRecommendationService.java`

Ligne 23, remplacez:
```java
private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";
```

With your key:
```java
private static final String GEMINI_API_KEY = "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
```

### 3️⃣ Run (2 minutes)

```bash
mvn clean compile
mvn javafx:run
```

---

## How I Did It

### Technology Stack:
- **AI Provider**: Google Gemini API (Free tier)
- **HTTP Client**: OkHttp (for API requests)
- **JSON Parser**: Jackson (already in project)
- **Language**: Java 17

### Architecture:

```
Player Performance Data
        ↓
AIRecommendationService
        ↓
Build Prompt (French)
        ↓
Send to Gemini API
        ↓
Parse JSON Response
        ↓
Display in UI Dialog
```

### Key Features:

1. **Smart Prompts**: Sends structured data to AI
   - Player name
   - Performance scores (Physical, Technical, Tactical)
   - Attendance rate
   - Weak areas identified

2. **Dual Recommendations**:
   - Training: Specific exercises, frequency, progression
   - Nutrition: Meal plans, hydration, supplements

3. **Fallback Mode**: Works without API key (generic advice)

4. **Non-Blocking**: API calls in background thread

5. **Beautiful UI**: Tabbed dialog with gradient header

---

## Why Gemini?

| Feature | Gemini | GPT-4 | Claude |
|---------|--------|-------|--------|
| Free Tier | ✅ Yes | ❌ No | ⚠️ Limited |
| Requests/min | 60 | N/A | 5 |
| Setup | Easy | Medium | Medium |
| French Support | ✅ Good | ✅ Excellent | ✅ Good |
| Cost | FREE | $$ | $ |

**Winner**: Gemini for free tier + ease of use!

---

## Code Highlights

### Prompt Engineering:
```java
String prompt = String.format("""
    Tu es un entraîneur de football professionnel expert.
    
    Joueur: %s
    Scores: Physique %.2f, Technique %.2f, Tactique %.2f
    
    Fournis 3-5 exercices concrets pour améliorer...
    """, playerName, avgPhys, avgTech, avgTact);
```

### API Call:
```java
Request request = new Request.Builder()
    .url(GEMINI_API_URL)
    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
    .build();

Response response = httpClient.newCall(request).execute();
```

### Response Parsing:
```java
JsonNode root = objectMapper.readTree(responseBody);
String aiText = root.path("candidates")
    .get(0).path("content")
    .path("parts").get(0)
    .path("text").asText();
```

---

## Testing

### Without API Key:
```
✅ Shows fallback recommendations
✅ Includes setup instructions
✅ Works immediately
```

### With API Key:
```
✅ Generates personalized advice
✅ Takes 3-10 seconds
✅ Detailed, specific recommendations
```

---

## What's Next?

### Phase 3: Food Tracking
- Nutrition API integration (USDA FoodData)
- Daily food log
- Calorie calculator
- Macronutrient tracking

Want me to implement Phase 3 now?
