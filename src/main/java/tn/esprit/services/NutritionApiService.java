package tn.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Service to analyze food and calculate nutrition using Edamam Nutrition Analysis API
 * Free API: https://www.edamam.com/
 * 
 * To use this service:
 * 1. Sign up at https://developer.edamam.com/
 * 2. Get your APP_ID and APP_KEY for Nutrition Analysis API
 * 3. Replace the placeholders below with your credentials
 */
public class NutritionApiService {
    
    private static final String API_URL = "https://api.edamam.com/api/nutrition-details";
    private static final String LOCAL_PROPERTIES_FILE = "nutrition-api.local.properties";
    private static final String USER_HOME_PROPERTIES_FILE = ".sport-insight/nutrition-api.local.properties";

    private final String appId;
    private final String appKey;
    
    public NutritionApiService() {
        ApiConfig config = ApiConfig.load();
        this.appId = config.appId();
        this.appKey = config.appKey();
    }
    
    /**
     * Analyze food description and get nutrition information
     * 
     * @param foodDescription What the user ate (e.g., "1 apple", "200g chicken breast", "1 cup rice")
     * @return NutritionInfo object with calories and macros
     * @throws Exception if API call fails
     */
    public NutritionInfo analyzeFood(String foodDescription) throws Exception {
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            // Return mock data for testing without API credentials
            return createMockNutritionInfo(foodDescription);
        }
        
        // Build API URL with credentials
        String urlString = API_URL + "?app_id=" + appId + "&app_key=" + appKey;
        URL url = new URL(urlString);
        
        // Create request body (simple JSON without Gson)
        String requestBody = "{\"ingr\":[\"" + foodDescription.replace("\"", "\\\"") + "\"]}";
        
        // Make HTTP POST request
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API Error: " + responseCode + " - " + conn.getResponseMessage());
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // Parse response (simple parsing without Gson)
        return parseNutritionResponse(response.toString(), foodDescription);
    }
    
    private NutritionInfo parseNutritionResponse(String jsonResponse, String foodDescription) {
        NutritionInfo info = new NutritionInfo();
        info.setFoodDescription(foodDescription);
        info.setApiResponse(jsonResponse);
        
        // Simple JSON parsing (extract values between quotes and colons)
        try {
            info.setCalories(extractJsonNumber(jsonResponse, "calories"));
            info.setProteinG(extractJsonNestedNumber(jsonResponse, "PROCNT", "quantity"));
            info.setCarbsG(extractJsonNestedNumber(jsonResponse, "CHOCDF", "quantity"));
            info.setFatG(extractJsonNestedNumber(jsonResponse, "FAT", "quantity"));
            info.setFiberG(extractJsonNestedNumber(jsonResponse, "FIBTG", "quantity"));
        } catch (Exception e) {
            // If parsing fails, use mock data
            return createMockNutritionInfo(foodDescription);
        }
        
        return info;
    }
    
    private double extractJsonNumber(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
    }
    
    private double extractJsonNestedNumber(String json, String parentKey, String childKey) {
        try {
            // Find the parent object
            String pattern = "\"" + parentKey + "\"\\s*:\\s*\\{([^}]+)\\}";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                String parentObject = m.group(1);
                return extractJsonNumber(parentObject, childKey);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
    }
    
    /**
     * Create mock nutrition data for testing without API credentials
     */
    private NutritionInfo createMockNutritionInfo(String foodDescription) {
        NutritionInfo info = new NutritionInfo();
        info.setFoodDescription(foodDescription);
        
        // Simple estimation based on common foods
        String lower = foodDescription.toLowerCase();
        
        if (lower.contains("apple") || lower.contains("pomme")) {
            info.setCalories(95.0);
            info.setProteinG(0.5);
            info.setCarbsG(25.0);
            info.setFatG(0.3);
            info.setFiberG(4.4);
        } else if (lower.contains("chicken") || lower.contains("poulet")) {
            info.setCalories(165.0);
            info.setProteinG(31.0);
            info.setCarbsG(0.0);
            info.setFatG(3.6);
            info.setFiberG(0.0);
        } else if (lower.contains("rice") || lower.contains("riz")) {
            info.setCalories(206.0);
            info.setProteinG(4.3);
            info.setCarbsG(45.0);
            info.setFatG(0.4);
            info.setFiberG(0.6);
        } else if (lower.contains("banana") || lower.contains("banane")) {
            info.setCalories(105.0);
            info.setProteinG(1.3);
            info.setCarbsG(27.0);
            info.setFatG(0.4);
            info.setFiberG(3.1);
        } else if (lower.contains("egg") || lower.contains("oeuf")) {
            info.setCalories(78.0);
            info.setProteinG(6.3);
            info.setCarbsG(0.6);
            info.setFatG(5.3);
            info.setFiberG(0.0);
        } else {
            // Default estimation
            info.setCalories(200.0);
            info.setProteinG(10.0);
            info.setCarbsG(30.0);
            info.setFatG(5.0);
            info.setFiberG(2.0);
        }
        
        info.setApiResponse("{\"mock\": true, \"message\": \"Using mock data. Configure API credentials for real analysis.\"}");
        return info;
    }

    private record ApiConfig(String appId, String appKey) {
        static ApiConfig load() {
            Properties props = new Properties();
            loadProperties(props, Path.of(LOCAL_PROPERTIES_FILE));
            loadProperties(props, Path.of(System.getProperty("user.home"), USER_HOME_PROPERTIES_FILE));
            loadProperties(props, Path.of("src/main/resources/nutrition-api.local.properties"));

            String appId = firstNonBlank(
                    System.getProperty("sport.insight.edamam.app.id"),
                    System.getenv("SPORT_INSIGHT_EDAMAM_APP_ID"),
                    props.getProperty("edamam.app.id"),
                    props.getProperty("nutrition.app.id"));
            String appKey = firstNonBlank(
                    System.getProperty("sport.insight.edamam.app.key"),
                    System.getenv("SPORT_INSIGHT_EDAMAM_APP_KEY"),
                    props.getProperty("edamam.app.key"),
                    props.getProperty("nutrition.app.key"));
            return new ApiConfig(appId, appKey);
        }

        private static void loadProperties(Properties props, Path path) {
            if (path == null || !Files.exists(path)) {
                return;
            }
            try (var input = Files.newInputStream(path)) {
                props.load(input);
            } catch (Exception ignored) {
                // Keep mock nutrition analysis available when local config is missing or unreadable.
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
    }
    
    /**
     * Data class to hold nutrition information
     */
    public static class NutritionInfo {
        private String foodDescription;
        private Double calories;
        private Double proteinG;
        private Double carbsG;
        private Double fatG;
        private Double fiberG;
        private String apiResponse;
        
        public NutritionInfo() {
            this.calories = 0.0;
            this.proteinG = 0.0;
            this.carbsG = 0.0;
            this.fatG = 0.0;
            this.fiberG = 0.0;
        }
        
        // Getters and Setters
        public String getFoodDescription() {
            return foodDescription;
        }
        
        public void setFoodDescription(String foodDescription) {
            this.foodDescription = foodDescription;
        }
        
        public Double getCalories() {
            return calories;
        }
        
        public void setCalories(Double calories) {
            this.calories = calories;
        }
        
        public Double getProteinG() {
            return proteinG;
        }
        
        public void setProteinG(Double proteinG) {
            this.proteinG = proteinG;
        }
        
        public Double getCarbsG() {
            return carbsG;
        }
        
        public void setCarbsG(Double carbsG) {
            this.carbsG = carbsG;
        }
        
        public Double getFatG() {
            return fatG;
        }
        
        public void setFatG(Double fatG) {
            this.fatG = fatG;
        }
        
        public Double getFiberG() {
            return fiberG;
        }
        
        public void setFiberG(Double fiberG) {
            this.fiberG = fiberG;
        }
        
        public String getApiResponse() {
            return apiResponse;
        }
        
        public void setApiResponse(String apiResponse) {
            this.apiResponse = apiResponse;
        }
        
        @Override
        public String toString() {
            return String.format("%.0f kcal | P: %.1fg | C: %.1fg | F: %.1fg | Fiber: %.1fg",
                    calories, proteinG, carbsG, fatG, fiberG);
        }
    }
}
