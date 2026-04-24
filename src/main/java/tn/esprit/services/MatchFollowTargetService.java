package tn.esprit.services;

import tn.esprit.entities.MatchFollowTarget;
import tn.esprit.services.football.FootballDataCompetitions;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MatchFollowTargetService {
    private final Connection connection;

    public MatchFollowTargetService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public boolean addTeamFavorite(Integer userId, Integer teamId) throws SQLException {
        if (userId == null || teamId == null) {
            return false;
        }
        return insertTarget(new MatchFollowTarget(
                userId,
                MatchFollowTarget.TYPE_TEAM,
                teamId,
                null,
                LocalDateTime.now()
        ));
    }

    public boolean addCompetitionFavorite(Integer userId, String competitionCode) throws SQLException {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (userId == null || normalizedCode == null) {
            return false;
        }
        return insertTarget(new MatchFollowTarget(
                userId,
                MatchFollowTarget.TYPE_COMPETITION,
                null,
                normalizedCode,
                LocalDateTime.now()
        ));
    }

    public boolean removeTeamFavorite(Integer userId, Integer teamId) throws SQLException {
        return deleteTarget(userId, MatchFollowTarget.TYPE_TEAM, teamId, null);
    }

    public boolean removeCompetitionFavorite(Integer userId, String competitionCode) throws SQLException {
        return deleteTarget(userId, MatchFollowTarget.TYPE_COMPETITION, null, FootballDataCompetitions.normalizeCode(competitionCode));
    }

    public List<MatchFollowTarget> getFavoritesByUser(Integer userId) throws SQLException {
        if (userId == null) {
            return List.of();
        }

        List<MatchFollowTarget> targets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, user_id, target_type, team_id, competition_code, created_at FROM match_follow_target WHERE user_id = ? ORDER BY created_at DESC, id DESC")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    targets.add(mapRow(rs));
                }
            }
        }
        return targets;
    }

    public Set<Integer> getFollowedTeamIds(Integer userId) throws SQLException {
        if (userId == null) {
            return Set.of();
        }

        Set<Integer> teamIds = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT team_id FROM match_follow_target WHERE user_id = ? AND target_type = ? AND team_id IS NOT NULL ORDER BY created_at DESC")) {
            statement.setInt(1, userId);
            statement.setString(2, MatchFollowTarget.TYPE_TEAM);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    teamIds.add(rs.getInt("team_id"));
                }
            }
        }
        return teamIds;
    }

    public Set<String> getFollowedCompetitionCodes(Integer userId) throws SQLException {
        if (userId == null) {
            return Set.of();
        }

        Set<String> competitionCodes = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT competition_code FROM match_follow_target WHERE user_id = ? AND target_type = ? AND competition_code IS NOT NULL ORDER BY created_at DESC")) {
            statement.setInt(1, userId);
            statement.setString(2, MatchFollowTarget.TYPE_COMPETITION);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String code = FootballDataCompetitions.normalizeCode(rs.getString("competition_code"));
                    if (code != null) {
                        competitionCodes.add(code);
                    }
                }
            }
        }
        return competitionCodes;
    }

    public boolean isTeamFavorite(Integer userId, Integer teamId) throws SQLException {
        return exists(userId, MatchFollowTarget.TYPE_TEAM, teamId, null);
    }

    public boolean isCompetitionFavorite(Integer userId, String competitionCode) throws SQLException {
        return exists(userId, MatchFollowTarget.TYPE_COMPETITION, null, FootballDataCompetitions.normalizeCode(competitionCode));
    }

    private boolean insertTarget(MatchFollowTarget target) throws SQLException {
        if (target == null || target.getUserId() == null || normalizeType(target.getTargetType()) == null) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO match_follow_target (user_id, target_type, team_id, competition_code, created_at) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, target.getUserId());
            statement.setString(2, normalizeType(target.getTargetType()));
            if (target.getTeamId() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, target.getTeamId());
            }
            statement.setString(4, FootballDataCompetitions.normalizeCode(target.getCompetitionCode()));
            statement.setTimestamp(5, Timestamp.valueOf(target.getCreatedAt() == null ? LocalDateTime.now() : target.getCreatedAt()));
            int updated = statement.executeUpdate();
            if (updated > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        target.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    private boolean deleteTarget(Integer userId, String targetType, Integer teamId, String competitionCode) throws SQLException {
        if (userId == null || normalizeType(targetType) == null) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM match_follow_target WHERE user_id = ? AND target_type = ? AND "
                        + "(? IS NULL OR team_id = ?) AND (? IS NULL OR UPPER(competition_code) = UPPER(?))")) {
            statement.setInt(1, userId);
            statement.setString(2, normalizeType(targetType));
            if (teamId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, teamId);
                statement.setInt(4, teamId);
            }
            if (competitionCode == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, competitionCode);
                statement.setString(6, competitionCode);
            }
            return statement.executeUpdate() > 0;
        }
    }

    private boolean exists(Integer userId, String targetType, Integer teamId, String competitionCode) throws SQLException {
        if (userId == null || normalizeType(targetType) == null) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM match_follow_target
                WHERE user_id = ?
                  AND target_type = ?
                  AND ((? IS NOT NULL AND team_id = ?) OR (? IS NOT NULL AND UPPER(competition_code) = UPPER(?)))
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, normalizeType(targetType));
            if (teamId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, teamId);
                statement.setInt(4, teamId);
            }
            if (competitionCode == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, competitionCode);
                statement.setString(6, competitionCode);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private MatchFollowTarget mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new MatchFollowTarget(
                rs.getInt("id"),
                rs.getInt("user_id"),
                normalizeType(rs.getString("target_type")),
                rs.getObject("team_id") == null ? null : rs.getInt("team_id"),
                FootballDataCompetitions.normalizeCode(rs.getString("competition_code")),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return MatchFollowTarget.TYPE_TEAM.equals(normalized) || MatchFollowTarget.TYPE_COMPETITION.equals(normalized)
                ? normalized
                : null;
    }
}
