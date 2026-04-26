package tn.esprit.entities;

import java.time.LocalDateTime;

public class AiChecklistProgress {
    private Integer id;
    private Integer userId;
    private String planType; // "exercise" or "meal"
    private String planCategory; // "cardio", "breakfast", etc.
    private String itemText;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public AiChecklistProgress() {
        this.isCompleted = false;
        this.createdAt = LocalDateTime.now();
    }

    public AiChecklistProgress(Integer userId, String planType, String planCategory, String itemText) {
        this();
        this.userId = userId;
        this.planType = planType;
        this.planCategory = planCategory;
        this.itemText = itemText;
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

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getPlanCategory() {
        return planCategory;
    }

    public void setPlanCategory(String planCategory) {
        this.planCategory = planCategory;
    }

    public String getItemText() {
        return itemText;
    }

    public void setItemText(String itemText) {
        this.itemText = itemText;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
        if (isCompleted != null && isCompleted) {
            this.completedAt = LocalDateTime.now();
        } else {
            this.completedAt = null;
        }
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
