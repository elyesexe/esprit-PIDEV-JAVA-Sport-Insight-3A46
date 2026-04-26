package tn.esprit.entities;

import java.time.LocalDateTime;

public class MatchFollowTarget {
    public static final String TYPE_TEAM = "TEAM";
    public static final String TYPE_COMPETITION = "COMPETITION";
    public static final String TYPE_MATCH = "MATCH";

    private Integer id;
    private Integer userId;
    private String targetType;
    private Integer teamId;
    private Integer matchId;
    private String competitionCode;
    private LocalDateTime createdAt;

    public MatchFollowTarget() {
    }

    public MatchFollowTarget(Integer userId, String targetType, Integer teamId, String competitionCode, LocalDateTime createdAt) {
        this(userId, targetType, teamId, null, competitionCode, createdAt);
    }

    public MatchFollowTarget(Integer userId, String targetType, Integer teamId, Integer matchId, String competitionCode, LocalDateTime createdAt) {
        this(null, userId, targetType, teamId, matchId, competitionCode, createdAt);
    }

    public MatchFollowTarget(Integer id, Integer userId, String targetType, Integer teamId, String competitionCode, LocalDateTime createdAt) {
        this(id, userId, targetType, teamId, null, competitionCode, createdAt);
    }

    public MatchFollowTarget(Integer id, Integer userId, String targetType, Integer teamId, Integer matchId, String competitionCode, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.targetType = targetType;
        this.teamId = teamId;
        this.matchId = matchId;
        this.competitionCode = competitionCode;
        this.createdAt = createdAt;
    }

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

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTeamTarget() {
        return TYPE_TEAM.equalsIgnoreCase(targetType);
    }

    public boolean isCompetitionTarget() {
        return TYPE_COMPETITION.equalsIgnoreCase(targetType);
    }

    public boolean isMatchTarget() {
        return TYPE_MATCH.equalsIgnoreCase(targetType);
    }

    @Override
    public String toString() {
        return "MatchFollowTarget{" +
                "id=" + id +
                ", userId=" + userId +
                ", targetType='" + targetType + '\'' +
                ", teamId=" + teamId +
                ", matchId=" + matchId +
                ", competitionCode='" + competitionCode + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
