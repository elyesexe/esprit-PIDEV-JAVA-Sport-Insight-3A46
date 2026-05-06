# ✅ Complete Setup Guide - Nutrition Tracking & Checklist Persistence

## What Has Been Done

### 1. Database Tables Created ✅
All three tables have been successfully created in the `sport_insight` database:

```sql
✓ ai_checklist_progress (8 columns)
✓ food_log (12 columns)  
✓ daily_nutrition_summary (10 columns)
```

**Verification:**
```bash
C:\xampp\mysql\bin\mysql.exe -u root -e "USE sport_insight; SHOW TABLES LIKE '%nutrition%'; SHOW TABLES LIKE '%checklist%'; SHOW TABLES LIKE 'food_log';"
```

### 2. Test Data Inserted ✅
Sample data has been inserted for testing:
- 3 food logs for today
- 1 daily nutrition summary
- 4 checklist progress items

**Verification Results:**
```
food_logs: 3
summaries: 1
checklist_items: 4
```

### 3. Code Compiled Successfully ✅
All Java classes compile without errors:
```
BUILD SUCCESS
Total time: 11.910 s
126 source files compiled
```

### 4. Files Created ✅

#### Entity Classes (3)
- `src/main/java/tn/esprit/entities/FoodLog.java`
- `src/main/java/tn/esprit/entities/DailyNutritionSummary.java`
- `src/main/java/tn/esprit/entities/AiChecklistProgress.java`

#### Service Classes (4)
- `src/main/java/tn/esprit/services/FoodLogService.java`
- `src/main/java/tn/esprit/services/DailyNutritionSummaryService.java`
- `src/main/java/tn/esprit/services/AiChecklistProgressService.java`
- `src/main/java/tn/esprit/services/NutritionApiService.java`

#### Controllers (1)
- `src/main/java/tn/esprit/Controller/FoodTrackingController.java`

#### Views (1)
- `src/main/resources/tn/esprit/views/food-tracking-view.fxml`

#### Styles (1)
- `src/main/resources/tn/esprit/styles/food-tracking-theme.css`

#### Database Scripts (2)
- `database/nutrition_tracking_schema.sql`
- `database/test_nutrition_data.sql`

#### Test Class (1)
- `src/main/java/tn/esprit/test/NutritionTrackingTest.java`

## How to Use

### Feature 1: Persistent Checklist Progress

**What it does:**
- Saves checkbox states when you check/uncheck items in AI plans
- Automatically loads your progress when you reopen a plan
- Each user has separate progress tracking

**How to test:**
1. Run your application
2. Navigate to Entrainement section
3. Click "Obtenir Recommandations IA"
4. Click on any exercise or meal card
5. Check some boxes in the checklist
6. Close the dialog
7. Reopen the same card
8. ✓ Your checked items should still be checked!

**Database verification:**
```sql
SELECT * FROM ai_checklist_progress WHERE user_id = 1;
```

### Feature 2: Food Tracking with Nutrition API

**What it does:**
- Log what you eat each day
- Automatic calorie calculation (using mock data or real API)
- Daily summary with progress tracking
- View history of all meals

**How to access:**
You need to add a navigation button to open the food tracking view. Add this code where you want the button:

```java
@FXML
private void handleOpenFoodTracking() {
    SceneNavigator.switchScene(yourButton, 
        "/tn/esprit/views/food-tracking-view.fxml", 
        "/tn/esprit/styles/food-tracking-theme.css", 
        "Suivi Nutritionnel");
}
```

**How to use:**
1. Select a date (defaults to today)
2. Choose meal type (breakfast, lunch, dinner, snack)
3. Describe what you ate: "2 eggs, 2 slices whole wheat bread, 1 banana"
4. Click "Analyser" - calculates nutrition
5. Review calories and macros
6. Click "Enregistrer" to save

**Database verification:**
```sql
-- View today's food logs
SELECT * FROM food_log WHERE user_id = 1 AND log_date = CURDATE();

-- View daily summary
SELECT * FROM daily_nutrition_summary WHERE user_id = 1 AND summary_date = CURDATE();
```

## Nutrition API Configuration (Optional)

### Using Mock Data (Default)
The system works out of the box with mock nutrition data for common foods:
- Apple: 95 kcal
- Chicken: 165 kcal  
- Rice: 206 kcal
- Banana: 105 kcal
- Egg: 78 kcal
- Default: 200 kcal

### Using Real API (Edamam)

**Step 1: Sign Up**
1. Go to https://developer.edamam.com/
2. Create a free account
3. Create an application for "Nutrition Analysis API"
4. Get your APP_ID and APP_KEY

**Step 2: Configure**
Edit `src/main/java/tn/esprit/services/NutritionApiService.java`:

```java
// Line 23-24
private static final String APP_ID = "your_app_id_here";
private static final String APP_KEY = "your_app_key_here";
```

**Step 3: Recompile**
```bash
mvn compile -DskipTests
```

**Free Tier Limits:**
- 100 requests per month
- 10 requests per minute
- Analyzes any food description

## Database Schema

### ai_checklist_progress
Stores user progress on AI recommendation checklists.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| user_id | INT | Foreign key to user table |
| plan_type | VARCHAR(50) | 'exercise' or 'meal' |
| plan_category | VARCHAR(50) | 'cardio', 'breakfast', etc. |
| item_text | VARCHAR(500) | The checklist item text |
| is_completed | BOOLEAN | Checked or not |
| completed_at | DATETIME | When it was completed |
| created_at | DATETIME | When it was created |

### food_log
Stores individual meal entries.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| user_id | INT | Foreign key to user table |
| log_date | DATE | Date of the meal |
| meal_type | VARCHAR(50) | breakfast, lunch, dinner, snack |
| food_description | TEXT | What was eaten |
| calories | DECIMAL(8,2) | Total calories |
| protein_g | DECIMAL(8,2) | Protein in grams |
| carbs_g | DECIMAL(8,2) | Carbs in grams |
| fat_g | DECIMAL(8,2) | Fat in grams |
| fiber_g | DECIMAL(8,2) | Fiber in grams |
| api_response | TEXT | Raw API response |
| created_at | DATETIME | When logged |

### daily_nutrition_summary
Aggregated daily nutrition totals.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| user_id | INT | Foreign key to user table |
| summary_date | DATE | Date of summary |
| total_calories | DECIMAL(8,2) | Sum of all meals |
| total_protein_g | DECIMAL(8,2) | Sum of protein |
| total_carbs_g | DECIMAL(8,2) | Sum of carbs |
| total_fat_g | DECIMAL(8,2) | Sum of fat |
| total_fiber_g | DECIMAL(8,2) | Sum of fiber |
| target_calories | DECIMAL(8,2) | Daily goal |
| updated_at | DATETIME | Last update |

## Testing

### Manual Database Test

```sql
-- Test checklist progress
INSERT INTO ai_checklist_progress (user_id, plan_type, plan_category, item_text, is_completed)
VALUES (1, 'exercise', 'cardio', 'Test exercise', true);

SELECT * FROM ai_checklist_progress WHERE user_id = 1;

-- Test food log
INSERT INTO food_log (user_id, log_date, meal_type, food_description, calories, protein_g, carbs_g, fat_g, fiber_g)
VALUES (1, CURDATE(), 'breakfast', 'Test meal', 300.0, 15.0, 40.0, 10.0, 5.0);

SELECT * FROM food_log WHERE user_id = 1 AND log_date = CURDATE();

-- Test daily summary
INSERT INTO daily_nutrition_summary (user_id, summary_date, total_calories, total_protein_g, total_carbs_g, total_fat_g, total_fiber_g, target_calories)
VALUES (1, CURDATE(), 1500.0, 75.0, 180.0, 50.0, 25.0, 2500.0)
ON DUPLICATE KEY UPDATE total_calories = 1500.0;

SELECT * FROM daily_nutrition_summary WHERE user_id = 1 AND summary_date = CURDATE();
```

### Java Test (Optional)

If you want to run the Java test class:

```bash
# Compile first
mvn compile -DskipTests

# Run test (requires proper classpath setup)
# The test verifies:
# 1. Nutrition API works (mock data)
# 2. Food Log Service can read from database
# 3. Daily Summary Service can read from database
# 4. Checklist Progress Service can save/load
```

## Troubleshooting

### Database Connection Issues
```sql
-- Check if tables exist
USE sport_insight;
SHOW TABLES;

-- Check table structure
DESCRIBE ai_checklist_progress;
DESCRIBE food_log;
DESCRIBE daily_nutrition_summary;

-- Check if data exists
SELECT COUNT(*) FROM food_log;
SELECT COUNT(*) FROM daily_nutrition_summary;
SELECT COUNT(*) FROM ai_checklist_progress;
```

### Compilation Issues
```bash
# Clean and recompile
mvn clean compile -DskipTests

# Check for errors
mvn compile 2>&1 | grep ERROR
```

### Runtime Issues
- Ensure MySQL is running (XAMPP Control Panel)
- Check database connection in `MyConnection.java`
- Verify user is logged in (AuthSession)
- Check console for error messages

## Next Steps

### 1. Add Navigation Button
Add a button in your main menu to access food tracking:

```java
// In your main controller
@FXML
private Button foodTrackingButton;

@FXML
private void handleOpenFoodTracking() {
    SceneNavigator.switchScene(foodTrackingButton, 
        "/tn/esprit/views/food-tracking-view.fxml", 
        "/tn/esprit/styles/food-tracking-theme.css", 
        "Suivi Nutritionnel | Sport Insight");
}
```

### 2. Test the Features
1. Test checklist persistence
2. Test food logging
3. Test daily summary
4. Test date navigation
5. Test delete functionality

### 3. (Optional) Configure Real API
Follow the "Nutrition API Configuration" section above.

### 4. Customize
- Adjust default calorie targets
- Add more mock food data
- Customize colors in CSS
- Add more meal types
- Add weekly/monthly reports

## Summary

✅ **Database**: 3 tables created and tested
✅ **Code**: 10 new files, all compiled successfully  
✅ **Test Data**: Sample data inserted
✅ **Views**: FXML and CSS created
✅ **API**: Mock data working, real API ready to configure

Everything is set up and ready to use! Just add a navigation button to access the food tracking feature.
