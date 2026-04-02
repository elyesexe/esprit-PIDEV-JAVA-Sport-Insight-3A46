package tn.esprit.entities;

import java.time.LocalDateTime;

public class ChatMessage {
    private Integer id;
    private Integer auteurId;
    private Integer destinataireId;
    private Integer annonceId;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private boolean notificationSent;

    public ChatMessage() {
    }

    public ChatMessage(Integer auteurId, Integer destinataireId, Integer annonceId, String message, LocalDateTime createdAt, boolean isRead, boolean notificationSent) {
        this.auteurId = auteurId;
        this.destinataireId = destinataireId;
        this.annonceId = annonceId;
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.notificationSent = notificationSent;
    }

    public ChatMessage(Integer id, Integer auteurId, Integer destinataireId, Integer annonceId, String message, LocalDateTime createdAt, boolean isRead, boolean notificationSent) {
        this.id = id;
        this.auteurId = auteurId;
        this.destinataireId = destinataireId;
        this.annonceId = annonceId;
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.notificationSent = notificationSent;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAuteurId() {
        return auteurId;
    }

    public void setAuteurId(Integer auteurId) {
        this.auteurId = auteurId;
    }

    public Integer getDestinataireId() {
        return destinataireId;
    }

    public void setDestinataireId(Integer destinataireId) {
        this.destinataireId = destinataireId;
    }

    public Integer getAnnonceId() {
        return annonceId;
    }

    public void setAnnonceId(Integer annonceId) {
        this.annonceId = annonceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
