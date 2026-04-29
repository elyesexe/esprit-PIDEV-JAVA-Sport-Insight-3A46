package tn.esprit.services;

import tn.esprit.entities.Notification;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    public static final String TYPE_URGENT_ANNONCE = "URGENT_ANNONCE";

    private final Connection connection;

    public NotificationService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public NotificationService(Connection connection) {
        this.connection = connection;
    }

    public Notification createIfAbsent(Notification notification) throws SQLException {
        if (notification == null || notification.getUserId() == null) {
            return null;
        }

        String sql = """
                INSERT INTO notification (
                    title,
                    message,
                    type,
                    created_at,
                    is_read,
                    user_id,
                    match_id,
                    dedupe_key,
                    competition_code,
                    home_team_name,
                    away_team_name,
                    home_team_logo,
                    away_team_logo,
                    actor_name,
                    actor_image,
                    minute_label,
                    accent_tone
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, notification);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                }
            }
            return notification;
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                return null;
            }
            throw e;
        }
    }

    public List<Notification> getRecentByUser(Integer userId, int limit) throws SQLException {
        if (userId == null) {
            return List.of();
        }

        int safeLimit = Math.max(1, limit);
        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, message, type, created_at, is_read, user_id, match_id, dedupe_key, competition_code, home_team_name, away_team_name, home_team_logo, away_team_logo, actor_name, actor_image, minute_label, accent_tone "
                        + "FROM notification WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?")) {
            statement.setInt(1, userId);
            statement.setInt(2, safeLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        return notifications;
    }

    public boolean markAsRead(Integer notificationId) throws SQLException {
        if (notificationId == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE notification SET is_read = TRUE WHERE id = ?")) {
            statement.setInt(1, notificationId);
            return statement.executeUpdate() > 0;
        }
    }

    public int markAllAsRead(Integer userId) throws SQLException {
        if (userId == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE notification SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE")) {
            statement.setInt(1, userId);
            return statement.executeUpdate();
        }
    }

    public int countUnreadByUser(Integer userId) throws SQLException {
        if (userId == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void addUrgentAnnonceNotification(
            Integer recipientUserId,
            Integer coachUserId,
            Integer annonceId,
            String annonceTitle,
            String coachDisplayName
    ) throws SQLException {
        addUrgentAnnonceNotification(recipientUserId, coachUserId, annonceId, annonceTitle, coachDisplayName, null);
    }

    public void addUrgentAnnonceNotification(
            Integer recipientUserId,
            Integer coachUserId,
            Integer annonceId,
            String annonceTitle,
            String coachDisplayName,
            String coachImage
    ) throws SQLException {
        if (recipientUserId == null) {
            return;
        }
        String safeTitle = annonceTitle == null || annonceTitle.isBlank() ? "Nouvelle annonce" : annonceTitle.trim();
        String safeCoach = coachDisplayName == null || coachDisplayName.isBlank() ? "Un entraineur" : coachDisplayName.trim();
        String dedupeKey = "urgent-annonce:" + (annonceId == null ? "unknown" : annonceId) + ":" + recipientUserId;
        Notification notification = new Notification(
                safeTitle,
                safeCoach + " a publie une annonce urgente.",
                TYPE_URGENT_ANNONCE,
                LocalDateTime.now(),
                false,
                recipientUserId,
                null,
                dedupeKey,
                null,
                null,
                null,
                null,
                null,
                safeCoach,
                null,
                "danger"
        );
        notification.setActorImage(coachImage);
        createIfAbsent(notification);
    }

    private void fillStatement(PreparedStatement statement, Notification notification) throws SQLException {
        statement.setString(1, notification.getTitle());
        statement.setString(2, notification.getMessage());
        statement.setString(3, notification.getType());
        statement.setTimestamp(4, Timestamp.valueOf(notification.getCreatedAt() == null ? LocalDateTime.now() : notification.getCreatedAt()));
        statement.setBoolean(5, notification.isRead());
        statement.setInt(6, notification.getUserId());
        if (notification.getMatchId() == null) {
            statement.setNull(7, java.sql.Types.INTEGER);
        } else {
            statement.setInt(7, notification.getMatchId());
        }
        statement.setString(8, notification.getDedupeKey());
        statement.setString(9, notification.getCompetitionCode());
        statement.setString(10, notification.getHomeTeamName());
        statement.setString(11, notification.getAwayTeamName());
        statement.setString(12, notification.getHomeTeamLogo());
        statement.setString(13, notification.getAwayTeamLogo());
        statement.setString(14, notification.getActorName());
        statement.setString(15, notification.getActorImage());
        statement.setString(16, notification.getMinuteLabel());
        statement.setString(17, notification.getAccentTone());
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Notification notification = new Notification(
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("type"),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                rs.getBoolean("is_read"),
                rs.getInt("user_id"),
                rs.getObject("match_id") == null ? null : rs.getInt("match_id"),
                rs.getString("dedupe_key"),
                rs.getString("competition_code"),
                rs.getString("home_team_name"),
                rs.getString("away_team_name"),
                rs.getString("home_team_logo"),
                rs.getString("away_team_logo"),
                rs.getString("actor_name"),
                rs.getString("minute_label"),
                rs.getString("accent_tone")
        );
        notification.setActorImage(rs.getString("actor_image"));
        notification.setId(rs.getInt("id"));
        return notification;
    }

    private boolean isDuplicateKey(SQLException e) {
        return e != null
                && (e.getErrorCode() == 1062
                || e.getSQLState() != null && e.getSQLState().startsWith("23")
                || e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"));
    }
}
