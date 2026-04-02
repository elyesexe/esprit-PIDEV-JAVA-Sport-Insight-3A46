package tn.esprit.entities;

import java.time.LocalDateTime;

public class MessengerMessage {
    private Long id;
    private String body;
    private String headers;
    private String queueName;
    private LocalDateTime createdAt;
    private LocalDateTime availableAt;
    private LocalDateTime deliveredAt;

    public MessengerMessage() {
    }

    public MessengerMessage(String body, String headers, String queueName, LocalDateTime createdAt, LocalDateTime availableAt, LocalDateTime deliveredAt) {
        this.body = body;
        this.headers = headers;
        this.queueName = queueName;
        this.createdAt = createdAt;
        this.availableAt = availableAt;
        this.deliveredAt = deliveredAt;
    }
}
