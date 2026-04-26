package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FoodLog {
    private Integer id;
    private Integer userId;
    private LocalDate logDate;
    private String mealType; // breakfast, lunch, dinner, snack
    private String foodDescription;
    private Double calories;
    private Double proteinG;
    private Double carbsG;
    private Double fatG;
    private Double fiberG;
    private String apiResponse;
    private LocalDateTime createdAt;

    public FoodLog() {
        this.logDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.calories = 0.0;
    }

    public FoodLog(Integer userId, LocalDate logDate, String mealType, String foodDescription) {
        this();
        this.userId = userId;
        this.logDate = logDate;
        this.mealType = mealType;
        this.foodDescription = foodDescription;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
