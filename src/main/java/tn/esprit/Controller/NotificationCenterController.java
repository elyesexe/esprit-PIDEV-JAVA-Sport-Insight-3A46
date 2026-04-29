package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.Notification;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.MatchAlertPopupManager;
import tn.esprit.gui.NavbarNotificationCenter;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.services.MatchsService;
import tn.esprit.services.NotificationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class NotificationCenterController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String FILTER_ALL = "Toutes";
    private static final String FILTER_UNREAD = "Non lues";
    private static final String FILTER_READ = "Lues";
    private static final String STORE_VIEW = "/tn/esprit/views/store-view.fxml";
    private static final String STORE_CSS = "/tn/esprit/styles/store-theme.css";
    private static final String STORE_TITLE = "Store | Sport Insight";
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("notification-center-db-worker"));

    @FXML
    private HBox navbarRoot;
    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button annonceNavButton;
    @FXML
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label unreadCountChipLabel;
    @FXML
    private Label totalCountChipLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterComboBox;
    @FXML
    private Button refreshButton;
    @FXML
    private Button markAllReadButton;
    @FXML
    private ListView<Notification> notificationListView;
    @FXML
    private VBox emptyStateBox;

    private final ObservableList<Notification> masterNotifications = FXCollections.observableArrayList();
    private final ObservableList<Notification> displayedNotifications = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();

    private NotificationService notificationService;
    private MatchsService matchsService;
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean serviceReady;
    private boolean loadingData;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureToolbar();
        configureListView();
        updateToolbarState();
        updateCounters();

        try {
            notificationService = new NotificationService();
            matchsService = new MatchsService();
            serviceReady = true;
            loadNotificationsAsync("Chargement des alertes...");
        } catch (SQLException e) {
            serviceReady = false;
            updateToolbarState();
            showStatus("status-error", "Impossible de charger le centre de notifications.");
            showAlert(Alert.AlertType.ERROR, "Notifications", "Impossible de preparer le centre de notifications.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleRefresh() {
        loadNotificationsAsync("Actualisation des alertes...");
    }

    @FXML
    private void handleMarkAllRead() {
        Integer userId = currentUserId();
        if (notificationService == null || userId == null) {
            return;
        }

        try {
            int updated = notificationService.markAllAsRead(userId);
            if (updated == 0) {
                showStatus("status-muted", "Toutes les alertes etaient deja lues.");
                return;
            }

            for (Notification notification : masterNotifications) {
                notification.setRead(true);
            }
            applyFilters();
            notificationListView.refresh();
            NavbarNotificationCenter.requestRefreshAll();
            showStatus("status-success", updated + " alerte(s) marquee(s) comme lues.");
        } catch (SQLException e) {
            showStatus("status-error", "Impossible de mettre a jour les alertes.");
        }
    }

    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton != null ? annonceNavButton : matchsNavButton, "/tn/esprit/views/annonce-user-view.fxml", "/tn/esprit/styles/annonce-theme.css", "Anonce | Sport Insight");
    }

    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.MATCHS);
    }

    private void configureToolbar() {
        filterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL, FILTER_UNREAD, FILTER_READ));
        filterComboBox.getSelectionModel().select(FILTER_ALL);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureListView() {
        notificationListView.setItems(displayedNotifications);
        notificationListView.setPlaceholder(new Label(""));
        notificationListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox card = buildNotificationCard(notification);
                card.prefWidthProperty().bind(listView.widthProperty().subtract(26));
                setText(null);
                setGraphic(card);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    private void loadNotificationsAsync(String loadingMessage) {
        Integer userId = currentUserId();
        if (notificationService == null || userId == null) {
            showStatus("status-warning", "Connectez-vous pour consulter vos alertes.");
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        loadingData = true;
        updateToolbarState();
        showStatus("status-muted", loadingMessage);

        Task<List<Notification>> loadTask = new Task<>() {
            @Override
            protected List<Notification> call() throws Exception {
                List<Notification> notifications = new ArrayList<>(notificationService.getRecentByUser(userId, 160));
                notifications.sort(Comparator
                        .comparing(Notification::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Notification::getId, Comparator.nullsLast(Integer::compareTo))
                        .reversed());
                return notifications;
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            masterNotifications.setAll(loadTask.getValue());
            applyFilters();
            updateToolbarState();
            NavbarNotificationCenter.requestRefreshAll();
            showStatus("status-success", "Historique des alertes actualise.");
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            updateToolbarState();
            showStatus("status-error", "Erreur lors du chargement des alertes.");
            Throwable throwable = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Notifications",
                    "Impossible de charger les alertes.\n" + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        String filter = filterComboBox.getValue();

        List<Notification> filtered = masterNotifications.stream()
                .filter(notification -> matchesFilter(notification, filter))
                .filter(notification -> query == null || matchesQuery(notification, query))
                .toList();

        displayedNotifications.setAll(filtered);
        updateCounters();
        updateEmptyState();
        updateToolbarState();
    }

    private boolean matchesFilter(Notification notification, String filter) {
        if (notification == null || filter == null || FILTER_ALL.equals(filter)) {
            return true;
        }
        return switch (filter) {
            case FILTER_UNREAD -> !notification.isRead();
            case FILTER_READ -> notification.isRead();
            default -> true;
        };
    }

    private boolean matchesQuery(Notification notification, String query) {
        return containsNormalized(notification.getTitle(), query)
                || containsNormalized(notification.getMessage(), query)
                || containsNormalized(notification.getType(), query)
                || containsNormalized(notification.getHomeTeamName(), query)
                || containsNormalized(notification.getAwayTeamName(), query)
                || containsNormalized(notification.getActorName(), query)
                || containsNormalized(notification.getCompetitionCode(), query);
    }

    private void updateCounters() {
        int total = masterNotifications.size();
        long unread = masterNotifications.stream().filter(notification -> !notification.isRead()).count();
        unreadCountChipLabel.setText(unread + " non lue(s)");
        totalCountChipLabel.setText(total + " alerte(s)");

        String filterLabel = filterComboBox == null || filterComboBox.getValue() == null ? FILTER_ALL : filterComboBox.getValue();
        resultsMetaLabel.setText(displayedNotifications.size() + " resultat(s) | " + filterLabel);
    }

    private void updateEmptyState() {
        boolean empty = displayedNotifications.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
    }

    private void updateToolbarState() {
        long unreadCount = masterNotifications.stream().filter(notification -> !notification.isRead()).count();
        boolean disabled = !serviceReady || loadingData;
        refreshButton.setDisable(disabled);
        markAllReadButton.setDisable(disabled || unreadCount == 0);
        searchField.setDisable(disabled);
        filterComboBox.setDisable(disabled);
        notificationListView.setDisable(disabled);
    }

    private VBox buildNotificationCard(Notification notification) {
        if (notification != null && notification.isWorkflowType()) {
            return buildWorkflowNotificationCard(notification);
        }

        Label unreadChip = new Label(notification.isRead() ? "Lue" : "Nouveau");
        unreadChip.getStyleClass().addAll("notification-meta-chip", notification.isRead() ? "notification-meta-chip-read" : "notification-meta-chip-unread");

        Label typeChip = new Label(emptyToFallback(notification.getType(), "Alerte"));
        typeChip.getStyleClass().add("notification-meta-chip");

        Label timeLabel = new Label(formatCreatedAt(notification.getCreatedAt()));
        timeLabel.getStyleClass().add("notification-time-label");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, unreadChip, typeChip, topSpacer, timeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane homeLogo = createLogo(notification.getHomeTeamLogo(), notification.getHomeTeamName());
        StackPane awayLogo = createLogo(notification.getAwayTeamLogo(), notification.getAwayTeamName());

        Label homeName = new Label(emptyToFallback(notification.getHomeTeamName(), "Home"));
        homeName.getStyleClass().add("notification-team-name");

        Label awayName = new Label(emptyToFallback(notification.getAwayTeamName(), "Away"));
        awayName.getStyleClass().add("notification-team-name");

        Label scoreLabel = new Label(extractScoreLabel(notification));
        scoreLabel.getStyleClass().add("notification-score-label");

        Label competitionLabel = new Label(emptyToFallback(notification.getCompetitionCode(), "Match alert"));
        competitionLabel.getStyleClass().add("notification-score-caption");

        VBox scoreBox = new VBox(3, scoreLabel, competitionLabel);
        scoreBox.setAlignment(Pos.CENTER);

        VBox homeBox = new VBox(8, homeLogo, homeName);
        homeBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(homeBox, Priority.ALWAYS);

        VBox awayBox = new VBox(8, awayLogo, awayName);
        awayBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(awayBox, Priority.ALWAYS);

        HBox teamsRow = new HBox(14, homeBox, scoreBox, awayBox);
        teamsRow.setAlignment(Pos.CENTER);
        teamsRow.getStyleClass().add("notification-teams-row");

        Label titleLabel = new Label(emptyToFallback(notification.getTitle(), "Alerte match"));
        titleLabel.getStyleClass().add("notification-title");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(emptyToFallback(notification.getMessage(), "Aucun detail supplementaire."));
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        Button replayButton = new Button("Rejouer l'alerte");
        replayButton.getStyleClass().add("soft-button");
        replayButton.setFocusTraversable(false);
        replayButton.setOnAction(event -> {
            event.consume();
            replayNotification(notification);
        });

        Button openMatchButton = new Button("Ouvrir le match");
        openMatchButton.getStyleClass().add("primary-button");
        openMatchButton.setDisable(notification.getMatchId() == null);
        openMatchButton.setManaged(notification.getMatchId() != null);
        openMatchButton.setVisible(notification.getMatchId() != null);
        openMatchButton.setFocusTraversable(false);
        openMatchButton.setOnAction(event -> {
            event.consume();
            openMatchFromNotification(notification, openMatchButton);
        });

        Button markReadButton = new Button(notification.isRead() ? "Deja lue" : "Marquer comme lue");
        markReadButton.getStyleClass().add("ghost-button");
        markReadButton.setDisable(notification.isRead());
        markReadButton.setFocusTraversable(false);
        markReadButton.setOnAction(event -> {
            event.consume();
            markNotificationAsRead(notification, true);
        });

        HBox actionsRow = new HBox(10, replayButton, openMatchButton, markReadButton);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.getStyleClass().add("notification-actions-row");

        VBox card = new VBox(14, topRow, teamsRow, titleLabel, messageLabel, actionsRow);
        card.setPadding(new Insets(16, 18, 18, 18));
        card.getStyleClass().add("notification-card");
        if (!notification.isRead()) {
            card.getStyleClass().add("notification-card-unread");
        }
        card.setOnMouseClicked(event -> replayNotification(notification));
        return card;
    }

    private VBox buildWorkflowNotificationCard(Notification notification) {
        Label unreadChip = new Label(notification.isRead() ? "Lue" : "Nouveau");
        unreadChip.getStyleClass().addAll("notification-meta-chip", notification.isRead() ? "notification-meta-chip-read" : "notification-meta-chip-unread");

        Label typeChip = new Label(resolveWorkflowTypeLabel(notification));
        typeChip.getStyleClass().add("notification-meta-chip");

        Label timeLabel = new Label(formatCreatedAt(notification.getCreatedAt()));
        timeLabel.getStyleClass().add("notification-time-label");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, unreadChip, typeChip, topSpacer, timeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane storeLogo = createLogo(null, resolveWorkflowTypeLabel(notification));
        Label productLabel = new Label(emptyToFallback(notification.getActorName(), "Notification"));
        productLabel.getStyleClass().add("notification-team-name");

        Label cartLabel = new Label(emptyToFallback(notification.getMinuteLabel(), "Mise a jour"));
        cartLabel.getStyleClass().add("notification-score-caption");

        VBox summaryTextBox = new VBox(4, productLabel, cartLabel);
        summaryTextBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(summaryTextBox, Priority.ALWAYS);

        HBox summaryRow = new HBox(12, storeLogo, summaryTextBox);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        summaryRow.getStyleClass().add("notification-teams-row");

        Label titleLabel = new Label(emptyToFallback(notification.getTitle(), "Notification"));
        titleLabel.getStyleClass().add("notification-title");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(emptyToFallback(notification.getMessage(), "Un evenement a ete enregistre."));
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        Button paymentButton = new Button("Passer au paiement");
        paymentButton.getStyleClass().add("primary-button");
        paymentButton.setManaged(notification.opensStorePayment());
        paymentButton.setVisible(notification.opensStorePayment());
        paymentButton.setDisable(!notification.opensStorePayment());
        paymentButton.setFocusTraversable(false);
        paymentButton.setOnAction(event -> {
            event.consume();
            openStoreFromNotification(notification, paymentButton);
        });

        Button markReadButton = new Button(notification.isRead() ? "Deja lue" : "Marquer comme lue");
        markReadButton.getStyleClass().add("ghost-button");
        markReadButton.setDisable(notification.isRead());
        markReadButton.setFocusTraversable(false);
        markReadButton.setOnAction(event -> {
            event.consume();
            markNotificationAsRead(notification, true);
        });

        HBox actionsRow = new HBox(10);
        if (notification.opensStorePayment()) {
            actionsRow.getChildren().add(paymentButton);
        }
        actionsRow.getChildren().add(markReadButton);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.getStyleClass().add("notification-actions-row");

        VBox card = new VBox(14, topRow, summaryRow, titleLabel, messageLabel, actionsRow);
        card.setPadding(new Insets(16, 18, 18, 18));
        card.getStyleClass().add("notification-card");
        if (!notification.isRead()) {
            card.getStyleClass().add("notification-card-unread");
        }
        card.setOnMouseClicked(event -> {
            if (notification.opensStorePayment()) {
                openStoreFromNotification(notification, card);
            } else {
                markNotificationAsRead(notification, false);
            }
        });
        return card;
    }

    private StackPane createLogo(String logoPath, String teamName) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);

        Image image = EquipeUiSupport.loadEquipeImage(logoPath);
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(teamName, "SI"));
        fallbackLabel.getStyleClass().add("notification-logo-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane shell = new StackPane(imageView, fallbackLabel);
        shell.getStyleClass().add("notification-logo-shell");
        shell.setMinSize(52, 52);
        shell.setPrefSize(52, 52);
        shell.setMaxSize(52, 52);
        return shell;
    }

    private void replayNotification(Notification notification) {
        if (notification != null && notification.opensStorePayment()) {
            openStoreFromNotification(notification, notificationListView);
            return;
        }
        if (notification != null && notification.isWorkflowType()) {
            markNotificationAsRead(notification, false);
            return;
        }
        Stage owner = notificationListView == null || notificationListView.getScene() == null
                ? null
                : (Stage) notificationListView.getScene().getWindow();
        if (owner == null || !owner.isShowing()) {
            showStatus("status-warning", "La fenetre active est indisponible pour rejouer l'alerte.");
            return;
        }

        MatchAlertPopupManager.getInstance().show(owner, notification);
        markNotificationAsRead(notification, false);
        showStatus("status-success", "Alerte rouverte.");
    }

    private void openMatchFromNotification(Notification notification, Node source) {
        if (notification == null || notification.getMatchId() == null || matchsService == null) {
            showStatus("status-warning", "Cette alerte n'est rattachee a aucun match.");
            return;
        }

        try {
            Matchs match = matchsService.getById(notification.getMatchId());
            if (match == null) {
                showStatus("status-warning", "Le match associe est introuvable.");
                return;
            }

            markNotificationAsRead(notification, false);
            SceneNavigator.switchScene(
                    source,
                    "/tn/esprit/views/match-detail-view.fxml",
                    "/tn/esprit/styles/match-theme.css",
                    "Fiche match",
                    controller -> {
                        if (controller instanceof MatchDetailController matchDetailController) {
                            matchDetailController.setMatchContext(match);
                        }
                    }
            );
        } catch (SQLException e) {
            showStatus("status-error", "Impossible d'ouvrir le match.");
        }
    }

    private void openStoreFromNotification(Notification notification, Node source) {
        if (notification == null) {
            return;
        }

        Node navigationSource = source != null ? source : notificationListView;
        markNotificationAsRead(notification, false);
        SceneNavigator.switchScene(
                navigationSource,
                STORE_VIEW,
                STORE_CSS,
                STORE_TITLE,
                controller -> {
                    if (controller instanceof StoreController storeController) {
                        storeController.openPaymentFromNotification();
                    }
                }
        );
    }

    private String resolveWorkflowTypeLabel(Notification notification) {
        if (notification == null) {
            return "Workflow";
        }
        if (notification.isOrderWorkflowType()) {
            return "Commande";
        }
        if (notification.isStoreWorkflowType()) {
            return "Paiement";
        }
        return "Workflow";
    }

    private void markNotificationAsRead(Notification notification, boolean showFeedback) {
        if (notification == null || notification.isRead() || notificationService == null) {
            return;
        }

        try {
            if (notificationService.markAsRead(notification.getId())) {
                notification.setRead(true);
                applyFilters();
                notificationListView.refresh();
                NavbarNotificationCenter.requestRefreshAll();
                if (showFeedback) {
                    showStatus("status-success", "Alerte marquee comme lue.");
                }
            }
        } catch (SQLException e) {
            if (showFeedback) {
                showStatus("status-error", "Impossible de mettre a jour l'alerte.");
            }
        }
    }

    private String extractScoreLabel(Notification notification) {
        if (notification == null || notification.getTitle() == null) {
            return "VS";
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+\\s*[-:]\\s*\\d+)").matcher(notification.getTitle());
        if (matcher.find()) {
            return matcher.group(1).replace(':', '-');
        }
        return "VS";
    }

    private String formatCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Date inconnue";
        }

        LocalDate today = LocalDate.now();
        LocalDate date = createdAt.toLocalDate();
        String timeLabel = createdAt.toLocalTime().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"));
        if (Objects.equals(date, today)) {
            return "Aujourd'hui " + timeLabel;
        }
        if (Objects.equals(date, today.minusDays(1))) {
            return "Hier " + timeLabel;
        }
        return DATE_TIME_FORMATTER.format(createdAt);
    }

    private void showStatus(String styleClass, String message) {
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer currentUserId() {
        return AuthSession.getCurrentUser() == null ? null : AuthSession.getCurrentUser().getId();
    }

    private static ThreadFactory daemonFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
