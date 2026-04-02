package tn.esprit.entities;

import java.time.LocalDateTime;

public class Notification {
    private Integer id;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Integer userId;

    public Notification() {
    }

    public Notification(String message, LocalDateTime createdAt, boolean isRead, Integer userId) {
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.userId = userId;
    }
}
