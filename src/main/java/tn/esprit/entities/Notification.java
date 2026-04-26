package tn.esprit.entities;

import java.time.LocalDateTime;

public class Notification {
    private Integer id;
    private String title;
    private String message;
    private String type;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Integer userId;
    private Integer matchId;
    private String dedupeKey;
    private String competitionCode;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamLogo;
    private String awayTeamLogo;
    private String actorName;
    private String minuteLabel;
    private String accentTone;

    public Notification() {
    }

    public Notification(String message, LocalDateTime createdAt, boolean isRead, Integer userId) {
        this(null, message, null, createdAt, isRead, userId, null, null, null, null, null, null, null, null, null, null);
    }

    public Notification(
            String title,
            String message,
            String type,
            LocalDateTime createdAt,
            boolean isRead,
            Integer userId,
            Integer matchId,
            String dedupeKey,
            String competitionCode,
            String homeTeamName,
            String awayTeamName,
            String homeTeamLogo,
            String awayTeamLogo,
            String actorName,
            String minuteLabel,
            String accentTone
    ) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.userId = userId;
        this.matchId = matchId;
        this.dedupeKey = dedupeKey;
        this.competitionCode = competitionCode;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.homeTeamLogo = homeTeamLogo;
        this.awayTeamLogo = awayTeamLogo;
        this.actorName = actorName;
        this.minuteLabel = minuteLabel;
        this.accentTone = accentTone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public String getHomeTeamLogo() {
        return homeTeamLogo;
    }

    public void setHomeTeamLogo(String homeTeamLogo) {
        this.homeTeamLogo = homeTeamLogo;
    }

    public String getAwayTeamLogo() {
        return awayTeamLogo;
    }

    public void setAwayTeamLogo(String awayTeamLogo) {
        this.awayTeamLogo = awayTeamLogo;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getMinuteLabel() {
        return minuteLabel;
    }

    public void setMinuteLabel(String minuteLabel) {
        this.minuteLabel = minuteLabel;
    }

    public String getAccentTone() {
        return accentTone;
    }

    public void setAccentTone(String accentTone) {
        this.accentTone = accentTone;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                ", isRead=" + isRead +
                ", userId=" + userId +
                ", matchId=" + matchId +
                ", dedupeKey='" + dedupeKey + '\'' +
                ", competitionCode='" + competitionCode + '\'' +
                ", homeTeamName='" + homeTeamName + '\'' +
                ", awayTeamName='" + awayTeamName + '\'' +
                ", actorName='" + actorName + '\'' +
                ", minuteLabel='" + minuteLabel + '\'' +
                ", accentTone='" + accentTone + '\'' +
                '}';
    }
}
