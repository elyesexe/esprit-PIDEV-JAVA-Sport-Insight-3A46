package tn.esprit.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.entities.Equipe;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.FootballDataSyncService;
import tn.esprit.services.FootballDataSyncSummary;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class JoueurTeamController {
    private static final double TEAM_CARD_WIDTH = 232;
    private static final double TEAM_CARD_HEIGHT = 250;
    private static final double TEAM_LOGO_SIZE = 104;
    private static final int AUTO_REFRESH_SECONDS = 45;
    private static final int AUTO_SYNC_SECONDS = 180;
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-team-db-worker"));

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
    private Label competitionChipLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label teamCountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button refreshButton;
    @FXML
    private FlowPane teamCardsPane;
    @FXML
    private VBox emptyStateBox;

    private final ObservableList<Equipe> allTeams = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();

    private EquipeService equipeService;
    private FootballDataSyncService syncService;
    private SidebarModuleGroup sidebarModuleGroup;
    private String competitionFilterCode;
    private String lastAutoSyncedCompetitionCode;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean syncingData;
    private boolean backgroundSyncing;
    private boolean dataLoaded;
    private Timeline autoRefreshTimeline;
    private long lastBackgroundSyncAtMillis;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndRender());
        configureAutoRefreshLifecycle();
        updateCompetitionTexts();
        updateToolbarState();

        try {
            equipeService = new EquipeService();
            serviceReady = true;
            updateToolbarState();
            Platform.runLater(() -> refreshTeamsAsync("Chargement des equipes..."));
        } catch (SQLException e) {
            serviceReady = false;
            updateToolbarState();
            showStatus("status-error", "Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les equipes.\n" + e.getMessage());
        }
    }

    public void setCompetitionFilter(String competitionCode) {
        competitionFilterCode = FootballDataCompetitions.normalizeCode(competitionCode);
        updateCompetitionTexts();
        if (dataLoaded) {
            applyFiltersAndRender();
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
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Competitions");
    }

    @FXML
    private void handleBack() {
        handleOpenJoueurs();
    }

    @FXML
    private void handleRefresh() {
        if (competitionFilterCode != null) {
            syncCompetitionAsync(competitionFilterCode, false);
            return;
        }
        refreshTeamsAsync("Actualisation des equipes...");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        applyFiltersAndRender();
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.JOUEURS);
    }

    private void configureAutoRefreshLifecycle() {
        if (navbarRoot == null) {
            return;
        }

        navbarRoot.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                stopAutoRefresh();
                return;
            }
            startAutoRefresh();
        });
    }

    private void startAutoRefresh() {
        runBackgroundRefreshCycle();
        if (autoRefreshTimeline == null) {
            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(AUTO_REFRESH_SECONDS), event -> runBackgroundRefreshCycle()));
            autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        if (autoRefreshTimeline.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            autoRefreshTimeline.play();
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private void runBackgroundRefreshCycle() {
        if (!serviceReady || loadingData || syncingData || backgroundSyncing) {
            return;
        }

        if (competitionFilterCode != null && shouldRunBackgroundSync()) {
            syncCompetitionAsync(competitionFilterCode, false, true);
            return;
        }

        refreshTeamsAsync(null, null, true);
    }

    private boolean shouldRunBackgroundSync() {
        long now = System.currentTimeMillis();
        return now - lastBackgroundSyncAtMillis >= AUTO_SYNC_SECONDS * 1000L;
    }

    private void refreshTeamsAsync(String loadingMessage) {
        refreshTeamsAsync(loadingMessage, "Equipes chargees.", false);
    }

    private void refreshTeamsAsync(String loadingMessage, String successMessage, boolean backgroundRefresh) {
        if (equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        loadingData = !backgroundRefresh;
        if (!backgroundRefresh) {
            updateToolbarState();
        }
        if (loadingMessage != null) {
            showStatus("status-muted", loadingMessage);
        }

        Task<List<Equipe>> loadTask = new Task<>() {
            @Override
            protected List<Equipe> call() throws Exception {
                List<Equipe> equipes = new ArrayList<>(equipeService.getAll());
                equipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                return equipes;
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            dataLoaded = true;
            allTeams.setAll(loadTask.getValue());
            applyFiltersAndRender();
            if (!backgroundRefresh) {
                updateToolbarState();
            }
            if (shouldAutoSyncSelectedCompetition()) {
                syncCompetitionAsync(competitionFilterCode, true, false);
                return;
            }
            if (successMessage != null) {
                showStatus("status-success", successMessage);
            }
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            if (!backgroundRefresh) {
                updateToolbarState();
                showStatus("status-error", "Chargement impossible.");
                Throwable throwable = loadTask.getException();
                showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les equipes.\n"
                        + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
            }
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void syncCompetitionAsync(String competitionCode, boolean automatic) {
        syncCompetitionAsync(competitionCode, automatic, false);
    }

    private void syncCompetitionAsync(String competitionCode, boolean automatic, boolean backgroundSync) {
        FootballDataSyncService footballSyncService = ensureSyncService();
        if (footballSyncService == null) {
            return;
        }

        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (normalizedCode == null) {
            refreshTeamsAsync(backgroundSync ? null : "Actualisation des equipes...", null, backgroundSync);
            return;
        }

        if (automatic) {
            lastAutoSyncedCompetitionCode = normalizedCode;
        }

        if (backgroundSync) {
            backgroundSyncing = true;
        } else {
            syncingData = true;
            updateToolbarState();
            showStatus("status-muted", automatic
                    ? "Import des equipes et joueurs " + FootballDataCompetitions.labelOf(normalizedCode) + "..."
                    : "Synchronisation " + FootballDataCompetitions.labelOf(normalizedCode) + "...");
        }

        Task<FootballDataSyncSummary> syncTask = new Task<>() {
            @Override
            protected FootballDataSyncSummary call() throws Exception {
                return footballSyncService.syncTeamsAndPlayers(List.of(normalizedCode), this::updateMessage);
            }
        };

        syncTask.messageProperty().addListener((observable, oldValue, newValue) -> {
            if (!backgroundSync && newValue != null && !newValue.isBlank()) {
                showStatus("status-muted", newValue);
            }
        });

        syncTask.setOnSucceeded(event -> {
            if (backgroundSync) {
                backgroundSyncing = false;
                lastBackgroundSyncAtMillis = System.currentTimeMillis();
            } else {
                syncingData = false;
                updateToolbarState();
            }
            FootballDataSyncSummary summary = syncTask.getValue();
            refreshTeamsAsync(null, backgroundSync ? null : (summary == null ? "Synchronisation terminee." : summary.toHumanMessage(true, false)), backgroundSync);
        });

        syncTask.setOnFailed(event -> {
            if (backgroundSync) {
                backgroundSyncing = false;
                lastBackgroundSyncAtMillis = System.currentTimeMillis();
                Throwable throwable = syncTask.getException();
                System.err.println("Background joueur-team sync failed: " + (throwable == null ? "unknown error" : throwable.getMessage()));
                return;
            }

            syncingData = false;
            updateToolbarState();
            Throwable throwable = syncTask.getException();
            showStatus("status-error", "Synchronisation impossible.");
            showAlert(Alert.AlertType.ERROR, "football-data.org",
                    "Impossible de synchroniser " + FootballDataCompetitions.labelOf(normalizedCode) + ".\n"
                            + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        DB_EXECUTOR.execute(syncTask);
    }

    private FootballDataSyncService ensureSyncService() {
        if (syncService != null) {
            return syncService;
        }
        try {
            syncService = new FootballDataSyncService();
            return syncService;
        } catch (Exception e) {
            showStatus("status-error", "Service de synchronisation indisponible.");
            showAlert(Alert.AlertType.ERROR, "football-data.org", "Impossible de preparer la synchronisation.\n" + e.getMessage());
            return null;
        }
    }

    private void applyFiltersAndRender() {
        List<Equipe> filtered = new ArrayList<>(allTeams);
        filtered.removeIf(equipe -> !matchesCompetition(equipe));

        String keyword = normalize(searchField.getText());
        if (keyword != null) {
            filtered.removeIf(equipe -> {
                String normalizedName = normalize(equipe.getNom());
                return normalizedName == null || !normalizedName.contains(keyword);
            });
        }

        teamCardsPane.getChildren().setAll(filtered.stream()
                .map(this::createTeamCard)
                .toList());

        boolean empty = filtered.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
        teamCountLabel.setText(filtered.size() + " equipe(s)");
    }

    private boolean matchesCompetition(Equipe equipe) {
        String equipeCompetition = equipe == null ? null : FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode());
        if (competitionFilterCode != null) {
            return Objects.equals(competitionFilterCode, equipeCompetition);
        }
        return FootballDataCompetitions.isTeamCompetition(equipeCompetition);
    }

    private Button createTeamCard(Equipe equipe) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(TEAM_LOGO_SIZE);
        imageView.setFitHeight(TEAM_LOGO_SIZE);
        imageView.setPreserveRatio(true);
        Image image = EquipeUiSupport.loadEquipeImage(equipe.getImage());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(equipe.getNom(), "SI"));
        fallbackLabel.getStyleClass().add("competition-card-logo-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(TEAM_LOGO_SIZE + 24, TEAM_LOGO_SIZE + 24);
        logoPane.setPrefSize(TEAM_LOGO_SIZE + 24, TEAM_LOGO_SIZE + 24);
        logoPane.setMaxSize(TEAM_LOGO_SIZE + 24, TEAM_LOGO_SIZE + 24);
        logoPane.getStyleClass().add("competition-card-logo-shell");

        Label titleLabel = new Label(emptyIfNull(equipe.getNom()));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(TEAM_CARD_WIDTH - 36);
        titleLabel.getStyleClass().add("competition-card-title");

        Label metaLabel = new Label(emptyToFallback(equipe.getCoach(), "Effectif"));
        metaLabel.setWrapText(true);
        metaLabel.setMaxWidth(TEAM_CARD_WIDTH - 36);
        metaLabel.getStyleClass().add("competition-card-subtitle");

        VBox content = new VBox(14, logoPane, titleLabel, metaLabel);
        content.setAlignment(Pos.CENTER);

        Button cardButton = new Button();
        cardButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        cardButton.setGraphic(content);
        cardButton.getStyleClass().add("competition-card-button");
        cardButton.setPrefSize(TEAM_CARD_WIDTH, TEAM_CARD_HEIGHT);
        cardButton.setMinSize(TEAM_CARD_WIDTH, TEAM_CARD_HEIGHT);
        cardButton.setMaxSize(TEAM_CARD_WIDTH, TEAM_CARD_HEIGHT);
        cardButton.setOnAction(event -> openTeamPlayers(cardButton, equipe));
        return cardButton;
    }

    private void openTeamPlayers(Button source, Equipe equipe) {
        if (equipe == null || equipe.getId() == null) {
            showStatus("status-warning", "Cette equipe ne peut pas etre ouverte.");
            return;
        }

        SceneNavigator.switchScene(
                source,
                "/tn/esprit/views/joueur-list-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                emptyIfNull(equipe.getNom()) + " | Joueurs",
                controller -> {
                    if (controller instanceof JoueurListController joueurListController) {
                        joueurListController.setTeamContext(equipe, competitionFilterCode);
                    }
                }
        );
    }

    private boolean shouldAutoSyncSelectedCompetition() {
        String normalizedCompetitionCode = FootballDataCompetitions.normalizeCode(competitionFilterCode);
        return normalizedCompetitionCode != null
                && teamCardsPane.getChildren().isEmpty()
                && !syncingData
                && !Objects.equals(lastAutoSyncedCompetitionCode, normalizedCompetitionCode);
    }

    private void updateCompetitionTexts() {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionFilterCode);
        competitionChipLabel.setText(competitionFilterCode == null ? "Top 5" : competitionLabel);
        pageTitleLabel.setText(competitionFilterCode == null ? "Choisissez une equipe" : competitionLabel);
        pageSubtitleLabel.setText(competitionFilterCode == null
                ? "Selectionnez une equipe pour ouvrir directement son effectif."
                : "Selectionnez une equipe de " + competitionLabel + " pour voir uniquement ses joueurs.");
    }

    private void updateToolbarState() {
        boolean disabled = !serviceReady || loadingData || syncingData;
        refreshButton.setDisable(disabled);
        searchField.setDisable(disabled);
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
