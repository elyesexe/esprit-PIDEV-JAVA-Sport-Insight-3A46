package tn.esprit.assistant;

import java.time.Instant;

public record AssistantMessage(Role role, String content, Instant createdAt) {
    public enum Role {
        USER,
        ASSISTANT
    }
}
