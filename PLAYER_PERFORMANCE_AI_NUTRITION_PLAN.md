# Player Performance Evolution, AI Training & Nutrition System

## Overview
Comprehensive system to track player performance over time, provide AI-powered training recommendations, nutrition advice, and food tracking with calorie calculation.

---

## Phase 1: Performance Evolution Dashboard ⚡

### Features
1. **Performance Charts**
   - Line chart showing Physical/Technical/Tactical scores over time
   - Compare multiple training sessions
   - Filter by date range
   - Show trend lines and averages

2. **Statistics Panel**
   - Current average scores
   - Improvement percentage
   - Best/worst performances
   - Consistency rating

3. **Player Profile View**
   - Personal information
   - Performance history
   - Training attendance rate
   - Overall progress

### Implementation
- Create `PlayerPerformanceController.java`
- Create `player-performance-view.fxml`
- Use JavaFX LineChart for visualization
- Query evaluations from database grouped by date

---

## Phase 2: AI-Powered Training & Nutrition 🤖

### Features
1. **AI Exercise Recommendations**
   - Analyze weak areas (Physical/Technical/Tactical)
   - Generate personalized exercise plans
   - Suggest drills based on performance gaps
   - Progressive difficulty adjustment

2. **Nutrition Advice**
   - Personalized meal plans
   - Pre/post-training nutrition
   - Hydration recommendations
   - Supplement suggestions based on training intensity

3. **AI Integration Options**
   - **Option A**: OpenAI API (GPT-4)
   - **Option B**: Google Gemini API
   - **Option C**: Local AI model (Ollama)

### Implementation
- Create `AITrainingService.java`
- Create `NutritionService.java`
- Integrate AI API (OpenAI/Gemini)
- Create recommendation UI

---

## Phase 3: Food Tracking & Calorie Calculator 🍎

### Features
1. **Food Database API**
   - Integration with nutrition API (USDA FoodData Central, Nutritionix, or Edamam)
   - Search foods by name
   - Get nutritional information
   - Barcode scanning (optional)

2. **Daily Food Log**
   - Add meals (breakfast, lunch, dinner, snacks)
   - Track portions
   - Calculate total calories
   - Macronutrient breakdown (protein, carbs, fats)

3. **Nutrition Dashboard**
   - Daily calorie intake vs target
   - Macronutrient distribution chart
   - Weekly nutrition summary
   - Hydration tracking

4. **Calorie Needs Calculator**
   - Based on player profile (age, weight, height)
   - Activity level adjustment
   - Training intensity factor
   - Goal-based recommendations (maintain, gain, lose)

### Implementation
- Create `FoodTrackingService.java`
- Create `CalorieCalculatorService.java`
- Integrate nutrition API
- Create food log UI
- Create nutrition dashboard

---

## Database Schema Updates

### New Tables Needed

```sql
-- Food log table
CREATE TABLE food_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    joueur_id INT NOT NULL,
    date DATE NOT NULL,
    meal_type ENUM('breakfast', 'lunch', 'dinner', 'snack') NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    calories DECIMAL(10,2) NOT NULL,
    protein DECIMAL(10,2),
    carbs DECIMAL(10,2),
    fats DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (joueur_id) REFERENCES user(id)
);

-- AI recommendations table
CREATE TABLE ai_recommendations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    joueur_id INT NOT NULL,
    recommendation_type ENUM('exercise', 'nutrition') NOT NULL,
    content TEXT NOT NULL,
    based_on_evaluation_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (joueur_id) REFERENCES user(id),
    FOREIGN KEY (based_on_evaluation_id) REFERENCES evaluation(id)
);

-- Player profile extended
CREATE TABLE player_profile (
    id INT PRIMARY KEY AUTO_INCREMENT,
    joueur_id INT NOT NULL UNIQUE,
    height DECIMAL(5,2), -- in cm
    weight DECIMAL(5,2), -- in kg
    birth_date DATE,
    activity_level ENUM('sedentary', 'light', 'moderate', 'active', 'very_active'),
    fitness_goal ENUM('maintain', 'gain_muscle', 'lose_weight', 'performance'),
    daily_calorie_target DECIMAL(10,2),
    FOREIGN KEY (joueur_id) REFERENCES user(id)
);
```

---

## API Integrations

### 1. AI Service (Choose One)
- **OpenAI GPT-4**: https://api.openai.com/v1/chat/completions
- **Google Gemini**: https://generativelanguage.googleapis.com/v1/models/gemini-pro
- **Anthropic Claude**: https://api.anthropic.com/v1/messages

### 2. Nutrition API (Choose One)
- **USDA FoodData Central**: https://fdc.nal.usda.gov/api-guide.html (FREE)
- **Nutritionix**: https://www.nutritionix.com/business/api (FREE tier available)
- **Edamam**: https://www.edamam.com/ (FREE tier available)

---

## Development Phases

### Phase 1: Performance Evolution (Week 1)
- [ ] Create performance chart view
- [ ] Implement data queries for historical evaluations
- [ ] Add statistics calculations
- [ ] Create player profile view

### Phase 2: AI Integration (Week 2)
- [ ] Set up AI API integration
- [ ] Create exercise recommendation engine
- [ ] Implement nutrition advice generator
- [ ] Create recommendation UI

### Phase 3: Food Tracking (Week 3)
- [ ] Integrate nutrition API
- [ ] Create food search functionality
- [ ] Implement daily food log
- [ ] Build calorie calculator
- [ ] Create nutrition dashboard

---

## User Flows

### Player Performance Evolution
1. Player logs in
2. Navigates to "My Performance"
3. Views charts showing progress over time
4. Sees statistics and trends
5. Identifies areas for improvement

### AI Training Recommendations
1. System analyzes recent evaluations
2. Identifies weak areas
3. Generates personalized exercise plan
4. Player views recommendations
5. Coach can review and approve

### Food Tracking
1. Player opens "Nutrition" section
2. Searches for food items
3. Adds to daily log with portions
4. System calculates calories and macros
5. Views daily/weekly summary
6. Gets AI nutrition advice based on intake

---

## Next Steps

Which phase would you like to start with?
1. **Performance Evolution Dashboard** (visualize progress)
2. **AI Training & Nutrition** (smart recommendations)
3. **Food Tracking System** (calorie calculator)

Or should I implement all three in sequence?
