package tn.esprit.services.football;

import java.time.LocalDateTime;

public record ApiFootballFixtureSnapshot(
        Long fixtureId,
        String matchStatus,
        String displayStatus,
        String statusShort,
        String statusLong,
        String minuteLabel,
        Integer elapsed,
        Integer addedTime,
        Integer homeScore,
        Integer awayScore,
        LocalDateTime kickoffAt
) {
    public boolean isLive() {
        return "En direct".equalsIgnoreCase(matchStatus);
    }

    public boolean isFinished() {
        return "Fini".equalsIgnoreCase(matchStatus);
    }

    public boolean isScheduled() {
        return "Programme".equalsIgnoreCase(matchStatus);
    }

    public boolean isPostponed() {
        return "Reporte".equalsIgnoreCase(matchStatus);
    }

    public boolean isCancelled() {
        return "Annule".equalsIgnoreCase(matchStatus);
    }

    public String effectiveStatusLabel() {
        return displayStatus == null || displayStatus.isBlank() ? matchStatus : displayStatus;
    }

    public boolean hasScore() {
        return homeScore != null || awayScore != null;
    }
}
