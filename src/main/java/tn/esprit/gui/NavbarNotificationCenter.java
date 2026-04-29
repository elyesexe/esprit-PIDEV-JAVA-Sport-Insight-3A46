package tn.esprit.gui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Controller.MatchDetailController;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.Notification;
import tn.esprit.security.AuthSession;
import tn.esprit.services.MatchsService;
import tn.esprit.services.NotificationService;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class NavbarNotificationCenter {
    private static final String FILTER_ALL = "all";
    private static final String FILTER_UNREAD = "unread";
    private static final int RECENT_LIMIT = 14;
    private static final String NOTIFICATION_CENTER_VIEW = "/tn/esprit/views/notification-center-view.fxml";
    private static final String NOTIFICATION_CENTER_CSS = "/tn/esprit/styles/notification-theme.css";
    private static final String STORE_VIEW = "/tn/esprit/views/store-view.fxml";
    private static final String STORE_CSS = "/tn/esprit/styles/store-theme.css";
    private static final String STORE_TITLE = "Store | Sport Insight";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("navbar-notification-worker"));
    private static final CopyOnWriteArrayList<WeakReference<NavbarNotificationCenter>> ACTIVE_CENTERS =
            new CopyOnWriteArrayList<>();

    private final Button bellButton;
    private final Label badgeLabel = new Label();
    private final ContextMenu popupMenu = new ContextMenu();
    private final VBox popupPanel = new VBox(12);
    private final Label summaryLabel = new Label("Loading...");
    private final Button allFilterButton = new Button("All");
    private final Button unreadFilterButton = new Button("Unread");
    private final Button markAllReadButton = new Button("Mark all read");
    private final Button seeAllButton = new Button("See all");
    private final VBox notificationListBox = new VBox(8);
    private final ScrollPane notificationScrollPane = new ScrollPane(notificationListBox);

    private volatile String activeFilter = FILTER_ALL;
    private volatile long menuRefreshSequence;
    private volatile long badgeRefreshSequence;
    private List<Notification> loadedNotifications = List.of();
    private int unreadCount;

    public NavbarNotificationCenter() {
        this.bellButton = buildBellButton();
        configurePopupMenu();
        registerActiveCenter();
        refreshBadgeAsync();
    }

    public Button getButton() {
        return bellButton;
    }

    public static void requestRefreshAll() {
        Platform.runLater(() -> {
            for (WeakReference<NavbarNotificationCenter> reference : ACTIVE_CENTERS) {
                NavbarNotificationCenter center = reference.get();
                if (center == null) {
                    ACTIVE_CENTERS.remove(reference);
                    continue;
                }
                center.refreshBadgeAsync();
                if (center.popupMenu.isShowing()) {
                    center.refreshMenuAsync(false);
                }
            }
        });
    }

    private Button buildBellButton() {
        Button button = new Button();
        button.setMnemonicParsing(false);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setFocusTraversable(false);
        button.setAccessibleText("Notifications");
        button.getStyleClass().add("navbar-bell-button");

        Label bellIconLabel = new Label("\uD83D\uDD14");
        bellIconLabel.getStyleClass().add("navbar-bell-icon");

        badgeLabel.getStyleClass().add("navbar-bell-badge");
        badgeLabel.setManaged(false);
        badgeLabel.setVisible(false);

        StackPane bellShell = new StackPane(bellIconLabel, badgeLabel);
        bellShell.getStyleClass().add("navbar-bell-shell");
        StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeLabel, new Insets(-5, -5, 0, 0));

        button.setGraphic(bellShell);
        button.setOnAction(event -> toggleMenu());
        button.getProperties().put(NavbarNotificationCenter.class.getName(), this);
        return button;
    }

    private void configurePopupMenu() {
        popupMenu.setAutoHide(true);
        popupMenu.setHideOnEscape(true);
        popupMenu.getStyleClass().add("navbar-notification-menu");

        Label titleLabel = new Label("Notifications");
        titleLabel.getStyleClass().add("navbar-notification-title");

        seeAllButton.getStyleClass().add("navbar-notification-header-action");
        seeAllButton.setFocusTraversable(false);
        seeAllButton.setOnAction(event -> {
            popupMenu.hide();
            SceneNavigator.switchScene(
                    bellButton,
                    NOTIFICATION_CENTER_VIEW,
                    NOTIFICATION_CENTER_CSS,
                    "Sport Insight | Notifications"
            );
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, titleLabel, headerSpacer, seeAllButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("navbar-notification-header");

        summaryLabel.getStyleClass().add("navbar-notification-summary");

        allFilterButton.getStyleClass().add("navbar-notification-filter");
        unreadFilterButton.getStyleClass().add("navbar-notification-filter");
        markAllReadButton.getStyleClass().add("navbar-notification-header-action");
        allFilterButton.setFocusTraversable(false);
        unreadFilterButton.setFocusTraversable(false);
        markAllReadButton.setFocusTraversable(false);

        allFilterButton.setOnAction(event -> {
            activeFilter = FILTER_ALL;
            applyFilterButtonState();
            renderNotifications();
        });
        unreadFilterButton.setOnAction(event -> {
            activeFilter = FILTER_UNREAD;
            applyFilterButtonState();
            renderNotifications();
        });
        markAllReadButton.setOnAction(event -> markAllReadAsync());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        HBox filterRow = new HBox(8, allFilterButton, unreadFilterButton, filterSpacer, markAllReadButton);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.getStyleClass().add("navbar-notification-filter-row");

        notificationListBox.getStyleClass().add("navbar-notification-list");

        notificationScrollPane.setFitToWidth(true);
        notificationScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        notificationScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        notificationScrollPane.setPannable(true);
        notificationScrollPane.setPrefViewportHeight(420);
        notificationScrollPane.setMinViewportHeight(220);
        notificationScrollPane.getStyleClass().add("navbar-notification-scroll");

        popupPanel.getStyleClass().add("navbar-notification-panel");
        popupPanel.setPrefWidth(392);
        popupPanel.setMaxWidth(392);
        popupPanel.getChildren().setAll(headerRow, filterRow, summaryLabel, notificationScrollPane);

        CustomMenuItem menuItem = new CustomMenuItem(popupPanel, false);
        menuItem.getStyleClass().add("navbar-notification-menu-item");
        popupMenu.getItems().setAll(menuItem);
        applyThemeClass();
        applyFilterButtonState();
        renderLoadingState("Loading notifications...");
    }

    private void toggleMenu() {
        if (popupMenu.isShowing()) {
            popupMenu.hide();
            return;
        }
        applyThemeClass();
        renderLoadingState("Loading notifications...");
        popupMenu.show(bellButton, Side.BOTTOM, 0, 10);
        positionPopupRightAligned();
        refreshMenuAsync(true);
    }

    private void refreshMenuAsync(boolean showLoadingState) {
        Integer userId = currentUserId();
        if (userId == null) {
            loadedNotifications = List.of();
            unreadCount = 0;
            updateBadge(0);
            renderPlaceholder("Sign in to see your live alerts.");
            return;
        }

        if (showLoadingState) {
            renderLoadingState("Loading notifications...");
        }

        long requestId = ++menuRefreshSequence;
        DB_EXECUTOR.execute(() -> {
            try {
                NotificationService notificationService = new NotificationService();
                List<Notification> notifications = new ArrayList<>(notificationService.getRecentByUser(userId, RECENT_LIMIT));
                notifications.sort(Comparator
                        .comparing(Notification::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Notification::getId, Comparator.nullsLast(Integer::compareTo))
                        .reversed());
                int unread = notificationService.countUnreadByUser(userId);
                NotificationMenuSnapshot snapshot = new NotificationMenuSnapshot(notifications, unread);
                Platform.runLater(() -> {
                    if (requestId != menuRefreshSequence) {
                        return;
                    }
                    loadedNotifications = snapshot.notifications();
                    unreadCount = snapshot.unreadCount();
                    updateBadge(unreadCount);
                    renderNotifications();
                    positionPopupRightAligned();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    if (requestId != menuRefreshSequence) {
                        return;
                    }
                    renderPlaceholder("Notifications are unavailable right now.");
                });
            }
        });
    }

    private void refreshBadgeAsync() {
        Integer userId = currentUserId();
        if (userId == null) {
            updateBadge(0);
            return;
        }

        long requestId = ++badgeRefreshSequence;
        DB_EXECUTOR.execute(() -> {
            try {
                NotificationService notificationService = new NotificationService();
                int unread = notificationService.countUnreadByUser(userId);
                Platform.runLater(() -> {
                    if (requestId == badgeRefreshSequence) {
                        unreadCount = unread;
                        updateBadge(unread);
                        updateSummary();
                    }
                });
            } catch (SQLException ignored) {
                Platform.runLater(() -> {
                    if (requestId == badgeRefreshSequence) {
                        updateBadge(0);
                    }
                });
            }
        });
    }

    private void markAllReadAsync() {
        Integer userId = currentUserId();
        if (userId == null || unreadCount == 0) {
            return;
        }

        renderLoadingState("Marking notifications as read...");
        DB_EXECUTOR.execute(() -> {
            try {
                NotificationService notificationService = new NotificationService();
                notificationService.markAllAsRead(userId);
                for (Notification notification : loadedNotifications) {
                    notification.setRead(true);
                }
                Platform.runLater(() -> {
                    unreadCount = 0;
                    updateBadge(0);
                    renderNotifications();
                    requestRefreshAll();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> renderNotifications());
            }
        });
    }

    private void markNotificationAsReadAsync(Notification notification) {
        if (notification == null || notification.isRead() || notification.getId() == null) {
            return;
        }

        DB_EXECUTOR.execute(() -> {
            try {
                NotificationService notificationService = new NotificationService();
                if (notificationService.markAsRead(notification.getId())) {
                    Platform.runLater(() -> {
                        notification.setRead(true);
                        unreadCount = Math.max(0, unreadCount - 1);
                        updateBadge(unreadCount);
                        renderNotifications();
                        requestRefreshAll();
                    });
                }
            } catch (SQLException ignored) {
                // Keep the dropdown responsive even if persistence is temporarily unavailable.
            }
        });
    }

    private void renderNotifications() {
        applyThemeClass();
        updateSummary();
        applyFilterButtonState();

        List<Notification> visibleNotifications = loadedNotifications.stream()
                .filter(notification -> FILTER_ALL.equals(activeFilter) || !notification.isRead())
                .toList();

        notificationListBox.getChildren().clear();
        if (visibleNotifications.isEmpty()) {
            Label emptyLabel = new Label(FILTER_UNREAD.equals(activeFilter)
                    ? "No unread notifications."
                    : "You do not have notifications yet.");
            emptyLabel.getStyleClass().add("navbar-notification-empty");
            emptyLabel.setWrapText(true);
            notificationListBox.getChildren().add(emptyLabel);
            markAllReadButton.setDisable(unreadCount == 0);
            return;
        }

        for (Notification notification : visibleNotifications) {
            notificationListBox.getChildren().add(buildNotificationRow(notification));
        }
        markAllReadButton.setDisable(unreadCount == 0);
    }

    private HBox buildNotificationRow(Notification notification) {
        boolean workflowNotification = notification != null && notification.isWorkflowType();
        HBox logosRow = workflowNotification
                ? new HBox(buildTeamLogo(null, resolveWorkflowShellLabel(notification)))
                : new HBox(-8,
                buildTeamLogo(notification.getHomeTeamLogo(), notification.getHomeTeamName()),
                buildTeamLogo(notification.getAwayTeamLogo(), notification.getAwayTeamName()));
        logosRow.setAlignment(Pos.TOP_LEFT);
        logosRow.getStyleClass().add("navbar-notification-logos");

        Label titleLabel = new Label(emptyToFallback(notification.getTitle(), workflowNotification ? "Notification" : "Match alert"));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("navbar-notification-item-title");

        Label messageLabel = new Label(emptyToFallback(notification.getMessage(), workflowNotification ? "Workflow updated." : "No additional details."));
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("navbar-notification-item-message");

        Label metaLabel = new Label(buildMetaLine(notification));
        metaLabel.getStyleClass().add("navbar-notification-item-meta");

        VBox textBox = new VBox(4, titleLabel, messageLabel, metaLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        boolean opensStorePayment = notification != null && notification.opensStorePayment();
        Button openButton = new Button(opensStorePayment ? "Paiement" : "Open");
        openButton.getStyleClass().add("navbar-notification-open-button");
        openButton.setFocusTraversable(false);
        boolean canOpen = opensStorePayment || (!workflowNotification && notification.getMatchId() != null);
        openButton.setManaged(canOpen);
        openButton.setVisible(canOpen);
        openButton.setDisable(!canOpen);
        openButton.setOnAction(event -> {
            event.consume();
            if (opensStorePayment) {
                openStoreAsync(notification);
            } else if (!workflowNotification) {
                openMatchAsync(notification);
            }
        });

        Label unreadDot = new Label();
        unreadDot.getStyleClass().add("navbar-notification-dot");
        unreadDot.setManaged(!notification.isRead());
        unreadDot.setVisible(!notification.isRead());

        VBox rightBox = new VBox(8, unreadDot, openButton);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        HBox row = new HBox(12, logosRow, textBox, rightBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("navbar-notification-row");
        if (!notification.isRead()) {
            row.getStyleClass().add("navbar-notification-row-unread");
        }
        row.setOnMouseClicked(event -> {
            if (opensStorePayment) {
                openStoreAsync(notification);
            } else if (workflowNotification) {
                markNotificationAsReadAsync(notification);
            } else {
                replayNotification(notification);
            }
        });
        return row;
    }

    private StackPane buildTeamLogo(String logoPath, String teamName) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(24);
        imageView.setFitHeight(24);
        imageView.setPreserveRatio(true);

        Image image = EquipeUiSupport.loadEquipeImage(logoPath);
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(teamName, "SI"));
        fallbackLabel.getStyleClass().add("navbar-notification-logo-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane shell = new StackPane(imageView, fallbackLabel);
        shell.getStyleClass().add("navbar-notification-logo-shell");
        shell.setMinSize(34, 34);
        shell.setPrefSize(34, 34);
        shell.setMaxSize(34, 34);
        return shell;
    }

    private void replayNotification(Notification notification) {
        Stage stage = currentStage();
        if (stage == null || notification == null) {
            return;
        }
        if (notification.opensStorePayment()) {
            openStoreAsync(notification);
            return;
        }
        if (notification.isWorkflowType()) {
            markNotificationAsReadAsync(notification);
            return;
        }

        popupMenu.hide();
        MatchAlertPopupManager.getInstance().show(stage, notification);
        markNotificationAsReadAsync(notification);
    }

    private String resolveWorkflowShellLabel(Notification notification) {
        if (notification == null) {
            return "SI";
        }
        if (notification.isOrderWorkflowType()) {
            return "Order";
        }
        if (notification.isStoreWorkflowType()) {
            return "Store";
        }
        return "SI";
    }

    private void openStoreAsync(Notification notification) {
        if (notification == null) {
            return;
        }

        popupMenu.hide();
        markNotificationAsReadAsync(notification);
        SceneNavigator.switchScene(
                bellButton,
                STORE_VIEW,
                STORE_CSS,
                STORE_TITLE,
                controller -> {
                    if (controller instanceof tn.esprit.Controller.StoreController storeController) {
                        storeController.openPaymentFromNotification();
                    }
                }
        );
    }

    private void openMatchAsync(Notification notification) {
        if (notification == null || notification.getMatchId() == null) {
            return;
        }

        popupMenu.hide();
        DB_EXECUTOR.execute(() -> {
            try {
                MatchsService matchsService = new MatchsService();
                Matchs match = matchsService.getById(notification.getMatchId());
                if (match == null) {
                    return;
                }
                Platform.runLater(() -> {
                    markNotificationAsReadAsync(notification);
                    SceneNavigator.switchScene(
                            bellButton,
                            "/tn/esprit/views/match-detail-view.fxml",
                            "/tn/esprit/styles/match-theme.css",
                            "Fiche match",
                            controller -> {
                                if (controller instanceof MatchDetailController matchDetailController) {
                                    matchDetailController.setMatchContext(match);
                                }
                            }
                    );
                });
            } catch (SQLException ignored) {
                // Ignore navigation errors here and keep the bell responsive.
            }
        });
    }

    private void renderLoadingState(String message) {
        summaryLabel.setText(message);
        notificationListBox.getChildren().clear();
        Label loadingLabel = new Label(message);
        loadingLabel.getStyleClass().add("navbar-notification-empty");
        loadingLabel.setWrapText(true);
        notificationListBox.getChildren().add(loadingLabel);
        markAllReadButton.setDisable(true);
    }

    private void renderPlaceholder(String message) {
        summaryLabel.setText(message);
        notificationListBox.getChildren().clear();
        Label placeholderLabel = new Label(message);
        placeholderLabel.getStyleClass().add("navbar-notification-empty");
        placeholderLabel.setWrapText(true);
        notificationListBox.getChildren().add(placeholderLabel);
        markAllReadButton.setDisable(true);
        positionPopupRightAligned();
    }

    private void updateSummary() {
        if (unreadCount <= 0) {
            summaryLabel.setText("No unread notifications");
            return;
        }
        summaryLabel.setText(unreadCount + (unreadCount == 1 ? " unread notification" : " unread notifications"));
    }

    private void applyFilterButtonState() {
        updateFilterButtonState(allFilterButton, FILTER_ALL.equals(activeFilter));
        updateFilterButtonState(unreadFilterButton, FILTER_UNREAD.equals(activeFilter));
    }

    private void updateFilterButtonState(Button button, boolean active) {
        button.getStyleClass().remove("navbar-notification-filter-active");
        if (active) {
            button.getStyleClass().add("navbar-notification-filter-active");
        }
    }

    private void applyThemeClass() {
        popupPanel.getStyleClass().remove("navbar-notification-panel-dark");
        if (ThemeManager.isDarkMode()) {
            popupPanel.getStyleClass().add("navbar-notification-panel-dark");
        }
    }

    private void updateBadge(int unread) {
        if (unread <= 0) {
            badgeLabel.setText("");
            badgeLabel.setManaged(false);
            badgeLabel.setVisible(false);
            return;
        }

        badgeLabel.setText(unread > 9 ? "9+" : String.valueOf(unread));
        badgeLabel.setManaged(true);
        badgeLabel.setVisible(true);
    }

    private void positionPopupRightAligned() {
        Platform.runLater(() -> {
            if (!popupMenu.isShowing() || popupMenu.getScene() == null || popupMenu.getScene().getWindow() == null) {
                return;
            }

            Bounds screenBounds = bellButton.localToScreen(bellButton.getBoundsInLocal());
            if (screenBounds == null) {
                return;
            }

            double menuWidth = popupMenu.getScene().getWindow().getWidth();
            popupMenu.getScene().getWindow().setX(screenBounds.getMaxX() - menuWidth);
        });
    }

    private void registerActiveCenter() {
        ACTIVE_CENTERS.add(new WeakReference<>(this));
    }

    private Stage currentStage() {
        return bellButton.getScene() == null ? null : (Stage) bellButton.getScene().getWindow();
    }

    private Integer currentUserId() {
        return AuthSession.getCurrentUser() == null ? null : AuthSession.getCurrentUser().getId();
    }

    private String buildMetaLine(Notification notification) {
        List<String> segments = new ArrayList<>();
        if (notification != null && notification.getMinuteLabel() != null && !notification.getMinuteLabel().isBlank()) {
            segments.add(notification.getMinuteLabel().trim());
        }
        segments.add(formatRelativeTime(notification == null ? null : notification.getCreatedAt()));
        return String.join("  •  ", segments);
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Now";
        }

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) {
            return "Now";
        }
        if (minutes < 60) {
            return minutes + "m";
        }

        long hours = Math.max(1, duration.toHours());
        if (hours < 24) {
            return hours + "h";
        }

        long days = Math.max(1, duration.toDays());
        if (days < 7) {
            return days + "d";
        }

        return DATE_TIME_FORMATTER.format(createdAt);
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static ThreadFactory daemonFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }

    private record NotificationMenuSnapshot(List<Notification> notifications, int unreadCount) {
    }
}
