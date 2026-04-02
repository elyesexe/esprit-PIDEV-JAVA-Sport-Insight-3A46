package tn.esprit.entities;

import java.time.LocalDateTime;

public class DoctrineMigrationVersion {
    private String version;
    private LocalDateTime executedAt;
    private Integer executionTime;

    public DoctrineMigrationVersion() {
    }

    public DoctrineMigrationVersion(String version, LocalDateTime executedAt, Integer executionTime) {
        this.version = version;
        this.executedAt = executedAt;
        this.executionTime = executionTime;
    }
}
