package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyNutritionSummary {
    private Integer id;
    private Integer userId;
    private LocalDate summaryDate;
    private Double totalCalories;
    private Double totalProteinG;
    private Double totalCarbsG;
    private Double totalFatG;
    private Double totalFiberG;
    private Double targetCalories;
    private LocalDateTime updatedAt;

    public DailyNutritionSummary() {
        this.summaryDate = LocalDate.now();
        this.totalCalories = 0.0;
        this.totalProteinG = 0.0;
        this.totalCarbsG = 0.0;
        this.totalFatG = 0.0;
        this.totalFiberG = 0.0;
        this.updatedAt = LocalDateTime.now();
    }

    public DailyNutritionSummary(Integer userId, LocalDate summaryDate) {
        this();
        this.userId = userId;
        this.summaryDate = summaryDate;
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

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public void setSummaryDate(LocalDate summaryDate) {
        this.summaryDate = summaryDate;
    }

    public Double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Double getTotalProteinG() {
        return totalProteinG;
    }

    public void setTotalProteinG(Double totalProteinG) {
        this.totalProteinG = totalProteinG;
    }

    public Double getTotalCarbsG() {
        return totalCarbsG;
    }

    public void setTotalCarbsG(Double totalCarbsG) {
        this.totalCarbsG = totalCarbsG;
    }

    public Double getTotalFatG() {
        return totalFatG;
    }

    public void setTotalFatG(Double totalFatG) {
        this.totalFatG = totalFatG;
    }

    public Double getTotalFiberG() {
        return totalFiberG;
    }

    public void setTotalFiberG(Double totalFiberG) {
        this.totalFiberG = totalFiberG;
    }

    public Double getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(Double targetCalories) {
        this.targetCalories = targetCalories;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
