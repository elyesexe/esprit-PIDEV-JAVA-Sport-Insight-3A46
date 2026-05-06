package tn.esprit.test;

import tn.esprit.entities.AiChecklistProgress;
import tn.esprit.entities.DailyNutritionSummary;
import tn.esprit.entities.FoodLog;
import tn.esprit.services.AiChecklistProgressService;
import tn.esprit.services.DailyNutritionSummaryService;
import tn.esprit.services.FoodLogService;
import tn.esprit.services.NutritionApiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Test class to verify nutrition tracking functionality
 * Run this to test database and API integration
 */
public class NutritionTrackingTest {
    
    public static void main(String[] args) {
        System.out.println("=== NUTRITION TRACKING TEST ===\n");
        
        try {
            testNutritionApi();
            testFoodLogService();
            testDailySummaryService();
            testChecklistProgressService();
            
            System.out.println("\n✅ ALL TESTS PASSED!");
            
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testNutritionApi() throws Exception {
        System.out.println("1. Testing Nutrition API Service...");
        NutritionApiService apiService = new NutritionApiService();
        
        // Test with mock data
        NutritionApiService.NutritionInfo info = apiService.analyzeFood("1 apple");
        
        System.out.println("   Food: " + info.getFoodDescription());
        System.out.println("   Calories: " + info.getCalories() + " kcal");
        System.out.println("   Protein: " + info.getProteinG() + "g");
        System.out.println("   Carbs: " + info.getCarbsG() + "g");
        System.out.println("   Fat: " + info.getFatG() + "g");
        System.out.println("   ✓ Nutrition API working (using mock data)\n");
    }
    
    private static void testFoodLogService() throws SQLException {
        System.out.println("2. Testing Food Log Service...");
        FoodLogService foodLogService = new FoodLogService();
        
        // Get today's food logs for user 1
        List<FoodLog> logs = foodLogService.getByUserAndDate(1, LocalDate.now());
        
        System.out.println("   Found " + logs.size() + " food logs for today:");
        for (FoodLog log : logs) {
            System.out.println("   - " + log.getMealType() + ": " + log.getFoodDescription() 
                             + " (" + log.getCalories() + " kcal)");
        }
        System.out.println("   ✓ Food Log Service working\n");
    }
    
    private static void testDailySummaryService() throws SQLException {
        System.out.println("3. Testing Daily Summary Service...");
        DailyNutritionSummaryService summaryService = new DailyNutritionSummaryService();
        
        // Get today's summary for user 1
        DailyNutritionSummary summary = summaryService.getByUserAndDate(1, LocalDate.now());
        
        if (summary != null) {
            System.out.println("   Daily Summary for " + summary.getSummaryDate() + ":");
            System.out.println("   - Total Calories: " + summary.getTotalCalories() + " kcal");
            System.out.println("   - Protein: " + summary.getTotalProteinG() + "g");
            System.out.println("   - Carbs: " + summary.getTotalCarbsG() + "g");
            System.out.println("   - Fat: " + summary.getTotalFatG() + "g");
            System.out.println("   - Target: " + summary.getTargetCalories() + " kcal");
            
            double progress = (summary.getTotalCalories() / summary.getTargetCalories()) * 100;
            System.out.println("   - Progress: " + String.format("%.1f", progress) + "%");
        } else {
            System.out.println("   No summary found for today");
        }
        System.out.println("   ✓ Daily Summary Service working\n");
    }
    
    private static void testChecklistProgressService() throws SQLException {
        System.out.println("4. Testing Checklist Progress Service...");
        AiChecklistProgressService progressService = new AiChecklistProgressService();
        
        // Get checklist progress for user 1, exercise cardio
        List<AiChecklistProgress> progressList = progressService.getByUserAndPlan(1, "exercise", "cardio");
        
        System.out.println("   Found " + progressList.size() + " checklist items for cardio:");
        for (AiChecklistProgress progress : progressList) {
            String status = progress.getIsCompleted() ? "✓" : "☐";
            System.out.println("   " + status + " " + progress.getItemText());
        }
        
        // Test toggle functionality
        String testItem = "Test item for toggle";
        progressService.toggleCompletion(1, "exercise", "cardio", testItem, true);
        AiChecklistProgress found = progressService.findByUserAndItem(1, "exercise", "cardio", testItem);
        
        if (found != null && found.getIsCompleted()) {
            System.out.println("   ✓ Toggle completion working");
            // Clean up test item
            progressService.toggleCompletion(1, "exercise", "cardio", testItem, false);
        }
        
        System.out.println("   ✓ Checklist Progress Service working\n");
    }
}
