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

    public void add(Notification notification) throws SQLException {
        String query = """
                INSERT INTO notification (
                    message, type, created_at, is_read, user_id, source_annonce_id, source_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, notification.getMessage());
            statement.setString(2, notification.getType());
            statement.setTimestamp(3, Timestamp.valueOf(
                    notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now()
            ));
            statement.setBoolean(4, notification.isRead());
            statement.setInt(5, notification.getUserId());

            if (notification.getSourceAnnonceId() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, notification.getSourceAnnonceId());
            }
            if (notification.getSourceUserId() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, notification.getSourceUserId());
            }

            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void addUrgentAnnonceNotification(
            Integer recipientUserId,
            Integer coachUserId,
            Integer annonceId,
            String annonceTitle,
            String coachDisplayName
    ) throws SQLException {
        if (recipientUserId == null) {
            return;
        }
        String safeTitle = annonceTitle == null || annonceTitle.isBlank() ? "Nouvelle annonce" : annonceTitle.trim();
        String safeCoach = coachDisplayName == null || coachDisplayName.isBlank() ? "Un entraineur" : coachDisplayName.trim();

        Notification notification = new Notification(
                safeCoach + " a publie une annonce urgente: " + safeTitle,
                TYPE_URGENT_ANNONCE,
                LocalDateTime.now(),
                false,
                recipientUserId,
                annonceId,
                coachUserId
        );
        add(notification);
    }

    public List<Notification> getUnreadByUser(Integer userId) throws SQLException {
        if (userId == null) {
            return List.of();
        }
        String query = """
                SELECT * FROM notification
                WHERE user_id = ? AND is_read = FALSE
                ORDER BY created_at DESC, id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Notification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(mapRow(resultSet));
                }
                return notifications;
            }
        }
    }

    public List<Notification> getUnreadByUserAndType(Integer userId, String type) throws SQLException {
        if (userId == null || type == null || type.isBlank()) {
            return List.of();
        }
        String query = """
                SELECT * FROM notification
                WHERE user_id = ? AND is_read = FALSE AND type = ?
                ORDER BY created_at DESC, id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setString(2, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Notification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(mapRow(resultSet));
                }
                return notifications;
            }
        }
    }

    public void markAsRead(Integer notificationId) throws SQLException {
        if (notificationId == null) {
            return;
        }
        String query = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, notificationId);
            statement.executeUpdate();
        }
    }

    public void markAsRead(List<Integer> notificationIds) throws SQLException {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        String query = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            boolean hasBatch = false;
            for (Integer notificationId : notificationIds) {
                if (notificationId == null) {
                    continue;
                }
                statement.setInt(1, notificationId);
                statement.addBatch();
                hasBatch = true;
            }
            if (hasBatch) {
                statement.executeBatch();
            }
        }
    }

    private Notification mapRow(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setId(resultSet.getInt("id"));
        notification.setMessage(resultSet.getString("message"));
        notification.setType(resultSet.getString("type"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        notification.setRead(resultSet.getBoolean("is_read"));
        notification.setUserId(resultSet.getInt("user_id"));

        int sourceAnnonceId = resultSet.getInt("source_annonce_id");
        notification.setSourceAnnonceId(resultSet.wasNull() ? null : sourceAnnonceId);

        int sourceUserId = resultSet.getInt("source_user_id");
        notification.setSourceUserId(resultSet.wasNull() ? null : sourceUserId);
        return notification;
    }
}
