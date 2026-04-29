package tn.esprit.entities;

import java.time.LocalDateTime;

public class Notification {
    private Integer id;
    private String message;
    private String type;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Integer userId;
    private Integer sourceAnnonceId;
    private Integer sourceUserId;

    public Notification() {
    }

    public Notification(String message, String type, LocalDateTime createdAt, boolean isRead, Integer userId, Integer sourceAnnonceId, Integer sourceUserId) {
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.userId = userId;
        this.sourceAnnonceId = sourceAnnonceId;
        this.sourceUserId = sourceUserId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSourceAnnonceId() {
        return sourceAnnonceId;
    }

    public void setSourceAnnonceId(Integer sourceAnnonceId) {
        this.sourceAnnonceId = sourceAnnonceId;
    }

    public Integer getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(Integer sourceUserId) {
        this.sourceUserId = sourceUserId;
    }
}
