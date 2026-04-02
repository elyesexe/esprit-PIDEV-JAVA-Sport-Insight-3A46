package tn.esprit.entities;

import java.time.LocalDateTime;

public class Message {
    private Integer id;
    private Integer senderId;
    private Integer receiverId;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;

    public Message() {
    }

    public Message(Integer senderId, Integer receiverId, String content, LocalDateTime sentAt, boolean isRead) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = sentAt;
        this.isRead = isRead;
    }
}
