package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Notification;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;
import tn.esprit.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "LIVE_NOTIFICATION_" + System.currentTimeMillis() + "_";

    private NotificationService notificationService;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        notificationService = new NotificationService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (User user : userService.getAll()) {
            if (user.getEmail() != null && user.getEmail().startsWith(TEST_PREFIX.toLowerCase())) {
                deleteRowsForUser(user.getId());
                userService.delete(user.getId());
            }
        }
    }

    @Test
    void createDeduplicateAndMarkNotificationAsRead() throws SQLException {
        User user = createUser(userService, TEST_PREFIX, "USER", UserRoles.ROLE_USER);

        Notification kickoff = new Notification(
                "Kickoff | Real Madrid vs Barcelona",
                "The match started at 20:00.",
                "Match Start",
                LocalDateTime.now(),
                false,
                user.getId(),
                44,
                TEST_PREFIX + "kickoff",
                "La Liga",
                "Real Madrid",
                "Barcelona",
                "rm.png",
                "barca.png",
                null,
                "20:00",
                "info"
        );

        Notification created = notificationService.createIfAbsent(kickoff);
        Notification duplicate = notificationService.createIfAbsent(kickoff);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertNull(duplicate);

        List<Notification> recent = notificationService.getRecentByUser(user.getId(), 10);
        assertEquals(1, recent.size());
        assertEquals("Kickoff | Real Madrid vs Barcelona", recent.get(0).getTitle());
        assertFalse(recent.get(0).isRead());
        assertEquals(1, notificationService.countUnreadByUser(user.getId()));

        assertTrue(notificationService.markAsRead(recent.get(0).getId()));
        List<Notification> refreshed = notificationService.getRecentByUser(user.getId(), 10);
        assertTrue(refreshed.get(0).isRead());
        assertEquals(0, notificationService.countUnreadByUser(user.getId()));
    }

    @Test
    void markAllNotificationsAsReadForUser() throws SQLException {
        User user = createUser(userService, TEST_PREFIX, "USER_MARK_ALL", UserRoles.ROLE_USER);

        Notification goal = new Notification(
                "Goal | 1 - 0",
                "Mbappe scored for Real Madrid.",
                "Goal",
                LocalDateTime.now(),
                false,
                user.getId(),
                77,
                TEST_PREFIX + "goal",
                "La Liga",
                "Real Madrid",
                "Barcelona",
                "rm.png",
                "barca.png",
                "Mbappe",
                "51'",
                "goal"
        );
        Notification card = new Notification(
                "Yellow Card | Real Madrid vs Barcelona",
                "Camavinga was booked.",
                "Yellow Card",
                LocalDateTime.now().plusSeconds(1),
                false,
                user.getId(),
                77,
                TEST_PREFIX + "card",
                "La Liga",
                "Real Madrid",
                "Barcelona",
                "rm.png",
                "barca.png",
                "Camavinga",
                "63'",
                "warning"
        );

        assertNotNull(notificationService.createIfAbsent(goal));
        assertNotNull(notificationService.createIfAbsent(card));
        assertEquals(2, notificationService.countUnreadByUser(user.getId()));
        assertEquals(2, notificationService.markAllAsRead(user.getId()));

        List<Notification> notifications = notificationService.getRecentByUser(user.getId(), 10);
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(Notification::isRead));
        assertEquals(0, notificationService.countUnreadByUser(user.getId()));
    }

    private void deleteRowsForUser(Integer userId) throws SQLException {
        try (PreparedStatement statement = MyConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM notification WHERE user_id = ?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = MyConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM match_follow_target WHERE user_id = ?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }
}
