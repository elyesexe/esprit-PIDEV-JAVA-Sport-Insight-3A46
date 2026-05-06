package tn.esprit.Controller;

<<<<<<< HEAD
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
=======
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
<<<<<<< HEAD
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
=======
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
<<<<<<< HEAD
import javafx.util.Duration;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.LiveMatchNotificationRuntime;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.services.EquipeService;
import tn.esprit.services.FootballDataSyncService;
import tn.esprit.services.FootballDataSyncSummary;
import tn.esprit.services.MatchFollowTargetService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
=======
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.FootballDataSyncService;
import tn.esprit.services.FootballDataSyncSummary;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.time.LocalDate;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
<<<<<<< HEAD
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
=======
import java.util.List;
import java.util.Map;
import java.util.Objects;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

<<<<<<< HEAD
public class MatchListController implements AssistantContextProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_CATALOG_COLUMNS = 3;
    private static final double CATALOG_CARD_WIDTH = 380;
    private static final double CATALOG_CARD_GAP = 20;
    private static final double CATALOG_LOGO_SIZE = 70;
    private static final double CATALOG_TEAM_WIDTH = 120;
=======
public class MatchListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final double CARD_LOGO_SIZE = 68;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final Map<String, String> COMPETITION_CODES_BY_LABEL = COMPETITION_LABELS.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private static final String STATUS_FILTER_ALL = "Tous statuts";
    private static final String STATUS_PROGRAMME = "Programme";
    private static final String STATUS_EN_DIRECT = "En direct";
    private static final String STATUS_FINI = "Fini";
    private static final String STATUS_REPORTE = "Reporte";
    private static final String STATUS_ANNULE = "Annule";
<<<<<<< HEAD
    private static final int BACKGROUND_REFRESH_SECONDS = 30;
    private static final int BACKGROUND_SYNC_SECONDS = 90;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("match-list-db-worker"));

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
    private Label resultCountLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private ComboBox<String> syncCompetitionComboBox;
    @FXML
    private Label syncMetaLabel;
    @FXML
<<<<<<< HEAD
    private ListView<List<Matchs>> matchListView;
=======
    private ListView<Matchs> matchListView;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Button clearButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button syncTeamsButton;
    @FXML
    private Button syncMatchesButton;

    private final ObservableList<Matchs> matchs = FXCollections.observableArrayList();
    private final FilteredList<Matchs> filteredMatchs = new FilteredList<>(matchs, match -> true);
<<<<<<< HEAD
    private final SortedList<Matchs> sortedMatchs = new SortedList<>(filteredMatchs);
    private final ObservableList<List<Matchs>> catalogRows = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();
    private int catalogColumnCount = MAX_CATALOG_COLUMNS;
=======
    private final AtomicLong refreshSequence = new AtomicLong();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    private MatchsService matchsService;
    private EquipeService equipeService;
    private FootballDataSyncService footballDataSyncService;
<<<<<<< HEAD
    private ApiFootballInsightsService apiFootballInsightsService;
    private MatchFollowTargetService matchFollowTargetService;
    private Map<Integer, Equipe> equipeById = Map.of();
    private Set<Integer> favoriteMatchIds = Set.of();
=======
    private Map<Integer, Equipe> equipeById = Map.of();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private String selectedCompetitionCode;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean syncingData;
<<<<<<< HEAD
    private boolean backgroundSyncing;
    private SidebarModuleGroup sidebarModuleGroup;
    private Timeline liveRefreshTimeline;
    private long lastBackgroundMatchSyncAtMillis;
=======
    private SidebarModuleGroup sidebarModuleGroup;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabel();
        configureStatusFilter();
        configureSearch();
        configureSyncSection();
        configureMatchList();
        updateSelectionState();
        updateActionAvailability();
<<<<<<< HEAD
        configureLiveRefreshLifecycle();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
<<<<<<< HEAD
            apiFootballInsightsService = new ApiFootballInsightsService();
            matchFollowTargetService = new MatchFollowTargetService();
            serviceReady = true;
            refreshDataAsync("Chargement des matchs...", "status-success", "Calendrier pret.", false);
            LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
=======
            serviceReady = true;
            refreshDataAsync("Chargement des matchs...", "status-success", "Calendrier pret.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        } catch (Exception e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les matchs.\n" + e.getMessage());
        }
    }

    public void setCompetitionFilter(String competitionCode) {
        selectedCompetitionCode = emptyToNull(competitionCode);

        if (statusFilterComboBox != null) {
            statusFilterComboBox.getSelectionModel().select(STATUS_FILTER_ALL);
        }

        if (syncCompetitionComboBox != null) {
            syncCompetitionComboBox.getSelectionModel().select(
                    selectedCompetitionCode == null
                            ? FootballDataCompetitions.ALL_LABEL
                            : resolveCompetitionLabel(selectedCompetitionCode)
            );
        }

        if (matchListView != null) {
            applyFilters();
        } else {
            updateCounters();
        }
    }

<<<<<<< HEAD
    public String getSelectedCompetitionCode() {
        return selectedCompetitionCode;
    }

    public void applyAssistantSearch(String query) {
        if (searchField == null) {
            return;
        }
        searchField.setText(query == null ? "" : query.trim());
        applyFilters();
        showMutedStatus((query == null || query.isBlank())
                ? "Recherche assistant reinitialisee."
                : "Recherche assistant appliquee : " + query.trim());
    }

    public void openMatchDetailFromAssistant(Matchs match) {
        openMatchDetail(match);
    }

    public List<Matchs> getFilteredMatchsSnapshot() {
        return List.copyOf(sortedMatchs);
    }

    public List<Equipe> getKnownTeamsSnapshot() {
        return List.copyOf(equipeById.values());
    }

    public String getAssistantCompetitionLabel() {
        return selectedCompetitionCode == null ? "All competitions" : resolveCompetitionLabel(selectedCompetitionCode);
    }

    public String getAssistantMatchLabel(Matchs match) {
        return buildMatchLabel(match);
    }

    public String getAssistantTeamName(Integer equipeId) {
        return getEquipeName(equipeId);
    }

    public String getAssistantFixtureSchedule(Matchs match) {
        if (match == null) {
            return "Unknown schedule";
        }
        return formatDate(match.getDateMatch()) + " at " + formatTime(match.getHeureDebut());
    }

    public String getAssistantStatus(Matchs match) {
        return resolveStatus(match);
    }

    private void configureLiveRefreshLifecycle() {
        if (navbarRoot == null) {
            return;
        }

        navbarRoot.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                stopLiveRefresh();
                return;
            }
            startLiveRefresh();
        });
    }

    private void startLiveRefresh() {
        LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
        if (liveRefreshTimeline == null) {
            liveRefreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(BACKGROUND_REFRESH_SECONDS), event -> {
                if (!serviceReady || loadingData || syncingData || backgroundSyncing) {
                    return;
                }
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
                if (shouldRunBackgroundMatchSync()) {
                    runSync(true, true);
                    return;
                }
                refreshDataAsync(null, null, null, true);
            }));
            liveRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        if (liveRefreshTimeline.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            liveRefreshTimeline.play();
        }
    }

    private void stopLiveRefresh() {
        if (liveRefreshTimeline != null) {
            liveRefreshTimeline.stop();
        }
    }

    @Override
    public String assistantContextSummary() {
        List<Matchs> visibleMatchs = getFilteredMatchsSnapshot();
        String header = "Matches page for " + getAssistantCompetitionLabel() + ". Visible fixtures: " + visibleMatchs.size() + ".";
        if (visibleMatchs.isEmpty()) {
            return header + " No fixtures are currently visible.";
        }

        List<Matchs> upcoming = visibleMatchs.stream()
                .sorted(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo)))
                .limit(3)
                .toList();

        String fixtures = upcoming.stream()
                .map(match -> getAssistantMatchLabel(match) + " on " + getAssistantFixtureSchedule(match) + " [" + getAssistantStatus(match) + "]")
                .collect(Collectors.joining(" | "));
        return header + " Next visible fixtures: " + fixtures + ".";
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        refreshDataAsync("Actualisation des matchs...", "status-success", "Liste des matchs actualisee.", false);
=======
        refreshDataAsync("Actualisation des matchs...", "status-success", "Liste des matchs actualisee.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        statusFilterComboBox.getSelectionModel().select(STATUS_FILTER_ALL);
        applyFilters();
        showMutedStatus("Recherche reinitialisee.");
    }

    @FXML
    private void handleSyncTeamsAndPlayers() {
        runSync(false);
    }

    @FXML
    private void handleSyncMatches() {
        runSync(true);
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

    private void configureStatusLabel() {
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        setStatusStyle("status-muted");
        statusLabel.setText("Pret");
    }

    private void configureStatusFilter() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                STATUS_FILTER_ALL,
                STATUS_PROGRAMME,
<<<<<<< HEAD
                STATUS_EN_DIRECT,
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                STATUS_FINI
        ));
        statusFilterComboBox.getSelectionModel().select(STATUS_FILTER_ALL);
    }

    private void configureSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureSyncSection() {
        ObservableList<String> competitionOptions = FXCollections.observableArrayList();
        competitionOptions.add(FootballDataCompetitions.ALL_LABEL);
        competitionOptions.addAll(COMPETITION_LABELS.values());
        syncCompetitionComboBox.setItems(competitionOptions);
        syncCompetitionComboBox.getSelectionModel().select(FootballDataCompetitions.ALL_LABEL);
        syncMetaLabel.setText("Plan gratuit : un lot complet sur les 6 competitions prend environ 40 secondes.");
    }

    private void configureMatchList() {
<<<<<<< HEAD
        sortedMatchs.setComparator(displayComparatorFor(selectedStatusFilter()));
        matchListView.setItems(catalogRows);
        matchListView.setPlaceholder(new Label(""));
        if (!matchListView.getStyleClass().contains("match-catalog-list-view")) {
            matchListView.getStyleClass().add("match-catalog-list-view");
        }
        matchListView.widthProperty().addListener((observable, oldValue, newValue) ->
                updateCatalogColumnCount(newValue.doubleValue()));
        matchListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(List<Matchs> rowMatches, boolean empty) {
                super.updateItem(rowMatches, empty);
                if (empty || rowMatches == null || rowMatches.isEmpty()) {
=======
        matchListView.setItems(filteredMatchs);
        matchListView.setPlaceholder(new Label(""));
        matchListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Matchs item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                    setText(null);
                    setGraphic(null);
                    return;
                }

<<<<<<< HEAD
                HBox row = buildCatalogRow(rowMatches);
                setText(null);
                setGraphic(row);
            }
        });
        rebuildCatalogRows();
    }

    private VBox buildMatchCard(Matchs match) {
        Equipe homeTeam = getEquipe(match.getEquipeDomicileId());
        Equipe awayTeam = getEquipe(match.getEquipeExterieurId());

        Button favoriteButton = buildFavoriteButton(match);
        StackPane.setAlignment(favoriteButton, Pos.TOP_LEFT);

        Label statusBadge = buildCatalogStatusBadge(match);
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);

        StackPane head = new StackPane(statusBadge, favoriteButton);
        head.setMaxWidth(Double.MAX_VALUE);
        head.getStyleClass().add("fixture-catalog-head");

        Label versusLabel = new Label("VS");
        versusLabel.setAlignment(Pos.CENTER);
        versusLabel.setMinSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        versusLabel.setPrefSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        versusLabel.setMaxSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        versusLabel.setTextOverrun(OverrunStyle.CLIP);
        versusLabel.getStyleClass().add("fixture-catalog-vs");

        HBox teamsRow = new HBox(16,
                buildCatalogTeam(homeTeam, "Domicile"),
                versusLabel,
                buildCatalogTeam(awayTeam, "Exterieur"));
        teamsRow.setAlignment(Pos.CENTER);
        teamsRow.getStyleClass().add("fixture-catalog-teams");

        VBox card = new VBox(18, head, teamsRow);
        card.getStyleClass().addAll("fixture-card", "fixture-catalog-card", "fixture-card-clickable");
        card.setMinWidth(CATALOG_CARD_WIDTH);
        card.setPrefWidth(CATALOG_CARD_WIDTH);
        card.setMaxWidth(CATALOG_CARD_WIDTH);
        card.setOnMouseClicked(event -> openMatchDetail(match));
        Tooltip.install(card, new Tooltip(buildMatchLabel(match)));
        return card;
    }

    private Label buildCatalogStatusBadge(Matchs match) {
        String normalizedStatus = normalizeMatchStatus(match == null ? null : match.getStatut());
        Label badge = new Label();
        badge.getStyleClass().add("fixture-catalog-status-badge");

        if (STATUS_FINI.equals(normalizedStatus)) {
            badge.setText("Finished");
            badge.getStyleClass().add("fixture-catalog-status-finished");
            return badge;
        }
        if (STATUS_EN_DIRECT.equals(normalizedStatus)) {
            badge.setText("Live");
            badge.getStyleClass().add("fixture-catalog-status-live");
            animateLiveBadge(badge);
            return badge;
        }
        if (STATUS_REPORTE.equals(normalizedStatus) || STATUS_ANNULE.equals(normalizedStatus)) {
            badge.setText(resolveStatus(match));
            badge.getStyleClass().add("fixture-catalog-status-muted");
            return badge;
        }

        badge.setText(formatDate(match == null ? null : match.getDateMatch()) + " | "
                + formatTime(match == null ? null : match.getHeureDebut()));
        badge.getStyleClass().add("fixture-catalog-date");
        return badge;
    }

    private void animateLiveBadge(Label badge) {
        FadeTransition pulse = new FadeTransition(Duration.millis(760), badge);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.58);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    private HBox buildCatalogRow(List<Matchs> rowMatches) {
        HBox row = new HBox(CATALOG_CARD_GAP);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("match-catalog-row");
        rowMatches.forEach(match -> row.getChildren().add(buildMatchCard(match)));
        return row;
    }

    private void updateCatalogColumnCount(double availableWidth) {
        if (availableWidth <= 0) {
            return;
        }
        int computedColumns = Math.max(1, Math.min(MAX_CATALOG_COLUMNS,
                (int) Math.floor((availableWidth + CATALOG_CARD_GAP) / (CATALOG_CARD_WIDTH + CATALOG_CARD_GAP))));
        if (computedColumns != catalogColumnCount) {
            catalogColumnCount = computedColumns;
            rebuildCatalogRows();
        }
    }

    private void rebuildCatalogRows() {
        if (matchListView == null) {
            return;
        }
        List<List<Matchs>> rows = new ArrayList<>();
        List<Matchs> currentRow = new ArrayList<>(catalogColumnCount);
        for (Matchs match : sortedMatchs) {
            currentRow.add(match);
            if (currentRow.size() == catalogColumnCount) {
                rows.add(List.copyOf(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(List.copyOf(currentRow));
        }
        catalogRows.setAll(rows);
    }

    private Button buildFavoriteButton(Matchs match) {
        boolean favorite = match != null && match.getId() != null && favoriteMatchIds.contains(match.getId());
        Button favoriteButton = new Button(favorite ? "\u2605" : "\u2606");
        favoriteButton.getStyleClass().add("fixture-favorite-button");
        if (favorite) {
            favoriteButton.getStyleClass().add("fixture-favorite-button-active");
        }
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setTooltip(new Tooltip(favorite ? "Remove from favorite matches" : "Add to favorite matches"));
        favoriteButton.setOnMouseClicked(event -> event.consume());
        favoriteButton.setOnAction(event -> {
            event.consume();
            toggleMatchFavorite(match);
        });
        return favoriteButton;
    }

    private void toggleMatchFavorite(Matchs match) {
        if (match == null || match.getId() == null) {
            showErrorStatus("Ce match ne peut pas encore etre ajoute aux favoris.");
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showErrorStatus("Connectez-vous pour ajouter un match aux favoris.");
            return;
        }

        try {
            ensureMatchFollowTargetService();
            boolean favorite = favoriteMatchIds.contains(match.getId())
                    || matchFollowTargetService.isMatchFavorite(currentUser.getId(), match.getId());
            Set<Integer> updatedFavorites = new LinkedHashSet<>(favoriteMatchIds);

            if (favorite) {
                matchFollowTargetService.removeMatchFavorite(currentUser.getId(), match.getId());
                updatedFavorites.remove(match.getId());
                showMutedStatus(buildMatchLabel(match) + " retire des matchs favoris.");
            } else {
                matchFollowTargetService.addMatchFavorite(currentUser.getId(), match.getId());
                updatedFavorites.add(match.getId());
                showMutedStatus(buildMatchLabel(match) + " ajoute aux matchs favoris.");
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            }

            favoriteMatchIds = Set.copyOf(updatedFavorites);
            rebuildCatalogRows();
            matchListView.refresh();
        } catch (SQLException e) {
            showErrorStatus("Impossible de mettre a jour les matchs favoris.");
        }
    }

    private void ensureMatchFollowTargetService() throws SQLException {
        if (matchFollowTargetService == null) {
            matchFollowTargetService = new MatchFollowTargetService();
        }
    }

    private StackPane buildCatalogLogo(Equipe equipe, String fallbackText) {
        String teamName = equipe == null ? fallbackText : emptyIfNull(equipe.getNom());
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CATALOG_LOGO_SIZE);
        imageView.setFitHeight(CATALOG_LOGO_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
=======
                VBox card = buildMatchCard(item);
                card.prefWidthProperty().bind(listView.widthProperty().subtract(26));
                card.setOnMouseClicked(event -> openMatchDetail(item));
                setText(null);
                setGraphic(card);
            }
        });
    }

    private VBox buildMatchCard(Matchs match) {
        Label statusChip = new Label(resolveStatus(match));
        statusChip.getStyleClass().add("fixture-status");
        applyFixtureStatusStyle(statusChip, match.getStatut());

        Label dateLabel = new Label(formatDate(match.getDateMatch()) + "  |  " + formatTime(match.getHeureDebut()));
        dateLabel.getStyleClass().add("fixture-date");

        Label idLabel = new Label(resolveMatchReference(match));
        idLabel.getStyleClass().add("fixture-id");

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        HBox head = new HBox(10, statusChip, headSpacer, dateLabel, idLabel);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("fixture-card-head");

        Equipe homeTeam = getEquipe(match.getEquipeDomicileId());
        Equipe awayTeam = getEquipe(match.getEquipeExterieurId());

        VBox homeBox = buildTeamPreview(homeTeam, "Domicile");
        VBox awayBox = buildTeamPreview(awayTeam, "Exterieur");

        Label scoreLabel = new Label(buildScore(match));
        scoreLabel.getStyleClass().add("fixture-score-value");

        Label versusLabel = new Label("VS");
        versusLabel.getStyleClass().add("fixture-score-caption");

        VBox scoreBox = new VBox(2, scoreLabel, versusLabel);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.getStyleClass().add("fixture-score-shell");

        HBox teamsRow = new HBox(16, homeBox, scoreBox, awayBox);
        teamsRow.setAlignment(Pos.CENTER);
        teamsRow.getStyleClass().add("fixture-teams-row");
        HBox.setHgrow(homeBox, Priority.ALWAYS);
        HBox.setHgrow(awayBox, Priority.ALWAYS);

        Label competitionChip = new Label(resolveCompetitionTag(match));
        competitionChip.getStyleClass().add("fixture-meta-chip");

        Label locationChip = new Label(resolveMatchLocation(match));
        locationChip.getStyleClass().add("fixture-meta-chip");

        Label typeChip = new Label(resolveMatchType(match));
        typeChip.getStyleClass().add("fixture-meta-chip");

        Label detailChip = new Label("Voir la fiche");
        detailChip.getStyleClass().add("fixture-link-chip");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        HBox metaRow = new HBox(10, competitionChip, locationChip, typeChip, metaSpacer, detailChip);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("fixture-meta-row");

        VBox card = new VBox(14, head, teamsRow, metaRow);
        card.getStyleClass().addAll("fixture-card", "fixture-card-clickable");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildTeamPreview(Equipe equipe, String fallbackRole) {
        String teamName = equipe == null ? "Equipe " + fallbackRole.toLowerCase() : emptyIfNull(equipe.getNom());

        ImageView imageView = new ImageView();
        imageView.setFitWidth(CARD_LOGO_SIZE);
        imageView.setFitHeight(CARD_LOGO_SIZE);
        imageView.setPreserveRatio(true);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        Image image = equipe == null ? null : EquipeUiSupport.loadEquipeImage(equipe.getImage());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(teamName, "SI"));
<<<<<<< HEAD
        fallbackLabel.getStyleClass().add("fixture-catalog-fallback");
=======
        fallbackLabel.getStyleClass().add("fixture-team-fallback");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
<<<<<<< HEAD
        logoPane.setMinSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        logoPane.setPrefSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        logoPane.setMaxSize(CATALOG_LOGO_SIZE, CATALOG_LOGO_SIZE);
        logoPane.getStyleClass().add("fixture-catalog-logo-shell");
        return logoPane;
    }

    private VBox buildCatalogTeam(Equipe equipe, String fallbackRole) {
        String teamName = equipe == null ? "Equipe " + fallbackRole.toLowerCase() : emptyIfNull(equipe.getNom());

        Label nameLabel = new Label(teamName);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(CATALOG_TEAM_WIDTH);
        nameLabel.setPrefWidth(CATALOG_TEAM_WIDTH);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setTooltip(new Tooltip(teamName));
        nameLabel.getStyleClass().add("fixture-catalog-team-name");

        VBox teamBox = new VBox(8, buildCatalogLogo(equipe, fallbackRole), nameLabel);
        teamBox.setAlignment(Pos.CENTER);
        teamBox.getStyleClass().add("fixture-catalog-team");
        return teamBox;
    }

    private void refreshDataAsync(String loadingMessage, String successStyleClass, String successMessage, boolean backgroundRefresh) {
=======
        logoPane.setMinSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.setPrefSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.setMaxSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.getStyleClass().add("fixture-team-logo-shell");

        Label nameLabel = new Label(teamName);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("fixture-team-name");

        Label roleLabel = new Label(fallbackRole);
        roleLabel.getStyleClass().add("fixture-team-role");

        VBox box = new VBox(10, logoPane, nameLabel, roleLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("fixture-team-box");
        return box;
    }

    private void refreshDataAsync(String loadingMessage, String successStyleClass, String successMessage) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (matchsService == null || equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
<<<<<<< HEAD
        loadingData = !backgroundRefresh;
        if (!backgroundRefresh) {
            updateActionAvailability();
        }
=======
        loadingData = true;
        updateActionAvailability();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (loadingMessage != null) {
            showMutedStatus(loadingMessage);
        }

<<<<<<< HEAD
        String statusFilterAtStart = selectedStatusFilter();
        Task<RefreshPayload> loadTask = new Task<>() {
            @Override
            protected RefreshPayload call() throws Exception {
                boolean refreshLiveSummaries = !STATUS_FINI.equals(statusFilterAtStart);
                List<Equipe> loadedEquipes = new ArrayList<>(equipeService.getAll());
                loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                Map<Integer, Equipe> loadedEquipeById = loadedEquipes.stream()
                        .filter(equipe -> equipe.getId() != null)
                        .collect(Collectors.toMap(Equipe::getId, equipe -> equipe, (left, right) -> left));

                List<Matchs> loadedMatchs = new ArrayList<>(matchsService.getAll());
                if (refreshLiveSummaries) {
                    refreshRelevantLiveSummaries(loadedMatchs, loadedEquipeById);
                    loadedMatchs = new ArrayList<>(matchsService.getAll());
                }

                return new RefreshPayload(loadedEquipes, loadedMatchs, loadFavoriteMatchIds());
=======
        Task<RefreshPayload> loadTask = new Task<>() {
            @Override
            protected RefreshPayload call() throws Exception {
                List<Equipe> loadedEquipes = new ArrayList<>(equipeService.getAll());
                loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

                List<Matchs> loadedMatchs = new ArrayList<>(matchsService.getAll());
                loadedMatchs.sort(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                        .reversed());

                return new RefreshPayload(loadedEquipes, loadedMatchs);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            RefreshPayload payload = loadTask.getValue();
            equipeById = payload.loadedEquipes.stream()
                    .filter(equipe -> equipe.getId() != null)
                    .collect(Collectors.toMap(Equipe::getId, equipe -> equipe, (left, right) -> left));
<<<<<<< HEAD
            favoriteMatchIds = payload.favoriteMatchIds == null ? Set.of() : payload.favoriteMatchIds;

            matchs.setAll(payload.loadedMatchs);
=======

            matchs.setAll(payload.loadedMatchs);
            EquipeUiSupport.clearImageCache();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            applyFilters();

            loadingData = false;
            updateActionAvailability();
            if (successMessage != null) {
                setStatus(successMessage, successStyleClass == null ? "status-muted" : successStyleClass);
            }
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            updateActionAvailability();
<<<<<<< HEAD
            if (!backgroundRefresh) {
                showErrorStatus("Erreur pendant le chargement.");
                Throwable throwable = loadTask.getException();
                showAlert(Alert.AlertType.ERROR, "Chargement",
                        "Erreur lors du chargement des matchs.\n" + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
            }
=======
            showErrorStatus("Erreur pendant le chargement.");
            Throwable throwable = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Chargement",
                    "Erreur lors du chargement des matchs.\n" + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        });

        DB_EXECUTOR.execute(loadTask);
    }

<<<<<<< HEAD
    private Set<Integer> loadFavoriteMatchIds() throws SQLException {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Set.of();
        }
        ensureMatchFollowTargetService();
        return matchFollowTargetService.getFollowedMatchIds(currentUser.getId());
    }

    private void refreshRelevantLiveSummaries(List<Matchs> loadedMatchs, Map<Integer, Equipe> loadedEquipeById) {
        if (apiFootballInsightsService == null || loadedMatchs == null || loadedMatchs.isEmpty()) {
            return;
        }

        List<Matchs> relevantMatches = loadedMatchs.stream()
                .filter(this::shouldSyncLiveSummary)
                .sorted(Comparator
                        .comparing(this::kickoffDateTimeOf, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Matchs::getId, Comparator.nullsLast(Integer::compareTo)))
                .limit(8)
                .toList();

        for (Matchs match : relevantMatches) {
            try {
                Equipe homeTeam = match.getEquipeDomicileId() == null ? null : loadedEquipeById.get(match.getEquipeDomicileId());
                Equipe awayTeam = match.getEquipeExterieurId() == null ? null : loadedEquipeById.get(match.getEquipeExterieurId());
                apiFootballInsightsService.refreshFixtureSnapshot(match, homeTeam, awayTeam);
            } catch (Exception ignored) {
                // Keep list refresh resilient if the live summary API is unavailable.
            }
        }
    }

    private boolean shouldSyncLiveSummary(Matchs match) {
        if (match == null) {
            return false;
        }

        if (selectedCompetitionCode != null && !Objects.equals(selectedCompetitionCode, emptyToNull(match.getCompetitionCode()))) {
            return false;
        }

        LocalDateTime kickoff = kickoffDateTimeOf(match);
        LocalDateTime now = LocalDateTime.now();
        String normalizedStatus = normalizeMatchStatus(match.getStatut());
        if (STATUS_EN_DIRECT.equals(normalizedStatus)) {
            return true;
        }
        if (kickoff == null) {
            return false;
        }
        return !kickoff.isBefore(now.minusHours(4)) && !kickoff.isAfter(now.plusMinutes(20));
    }

    private LocalDateTime kickoffDateTimeOf(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        return match.getHeureDebut() == null
                ? match.getDateMatch().atStartOfDay()
                : match.getDateMatch().atTime(match.getHeureDebut());
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private void applyFilters() {
        String query = normalize(searchField.getText());
        String statusFilter = selectedStatusFilter();
        String competitionCode = selectedCompetitionCode;

        filteredMatchs.setPredicate(match ->
                (query == null || matchesQuery(match, query))
                        && (statusFilter == null || Objects.equals(statusFilter, normalizeMatchStatus(match.getStatut())))
                        && (competitionCode == null || Objects.equals(competitionCode, emptyToNull(match.getCompetitionCode())))
        );
<<<<<<< HEAD
        sortedMatchs.setComparator(displayComparatorFor(statusFilter));

        rebuildCatalogRows();
=======

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        updateCounters();
        updateEmptyState();
    }

<<<<<<< HEAD
    private Comparator<Matchs> displayComparatorFor(String statusFilter) {
        boolean newestFirst = STATUS_FINI.equals(statusFilter);
        return (left, right) -> compareByKickoff(left, right, newestFirst);
    }

    private int compareByKickoff(Matchs left, Matchs right, boolean newestFirst) {
        LocalDateTime leftKickoff = kickoffDateTimeOf(left);
        LocalDateTime rightKickoff = kickoffDateTimeOf(right);

        int result;
        if (leftKickoff == null && rightKickoff == null) {
            result = 0;
        } else if (leftKickoff == null) {
            result = 1;
        } else if (rightKickoff == null) {
            result = -1;
        } else {
            result = leftKickoff.compareTo(rightKickoff);
        }

        if (newestFirst) {
            result = -result;
        }
        if (result != 0) {
            return result;
        }

        Integer leftId = left == null ? null : left.getId();
        Integer rightId = right == null ? null : right.getId();
        return Comparator.nullsLast(Integer::compareTo).compare(leftId, rightId);
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private boolean matchesQuery(Matchs match, String query) {
        return containsNormalized(buildMatchLabel(match), query)
                || containsNormalized(getEquipeName(match.getEquipeDomicileId()), query)
                || containsNormalized(getEquipeName(match.getEquipeExterieurId()), query)
                || containsNormalized(match.getLieu(), query)
                || containsNormalized(match.getType(), query)
                || containsNormalized(resolveCompetitionLabel(match.getCompetitionCode()), query)
                || containsNormalized(match.getStatut(), query)
                || containsNormalized(formatDate(match.getDateMatch()), query);
    }

    private void updateCounters() {
        int count = filteredMatchs.size();
        resultCountLabel.setText(count + " match(s)");

        StringBuilder meta = new StringBuilder(count + " carte(s)");
        if (selectedCompetitionCode != null) {
            meta.append(" | ").append(resolveCompetitionLabel(selectedCompetitionCode));
        }
        if (selectedStatusFilter() != null) {
            meta.append(" | ").append(selectedStatusFilter());
        }
        resultsMetaLabel.setText(meta.toString());
        updateSelectionState();
    }

    private void updateSelectionState() {
        if (selectedCompetitionCode != null) {
            selectionStateLabel.setText("Competition : " + resolveCompetitionLabel(selectedCompetitionCode));
            return;
        }
        selectionStateLabel.setText("Cliquez sur une carte pour ouvrir la fiche");
    }

    private void updateEmptyState() {
        boolean empty = filteredMatchs.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
    }

    private void runSync(boolean matchesOnly) {
<<<<<<< HEAD
        runSync(matchesOnly, false);
    }

    private void runSync(boolean matchesOnly, boolean backgroundSync) {
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        FootballDataSyncService syncService = ensureSyncService();
        if (syncService == null) {
            return;
        }

<<<<<<< HEAD
        List<String> competitionCodes = backgroundSync ? selectedBackgroundSyncCompetitionCodes() : selectedSyncCompetitionCodes();
        if (backgroundSync && competitionCodes.isEmpty()) {
            refreshDataAsync(null, null, null, true);
            return;
        }

        if (backgroundSync) {
            backgroundSyncing = true;
        } else {
            syncingData = true;
            updateActionAvailability();

            String scopeLabel = competitionCodes.size() == 1
                    ? FootballDataCompetitions.labelOf(competitionCodes.get(0))
                    : FootballDataCompetitions.ALL_LABEL;
            syncMetaLabel.setText("Synchronisation en cours : " + scopeLabel + ".");
            showMutedStatus(matchesOnly
                    ? "Import du calendrier en cours..."
                    : "Import des clubs et effectifs en cours...");
        }
=======
        List<String> competitionCodes = selectedSyncCompetitionCodes();
        syncingData = true;
        updateActionAvailability();

        String scopeLabel = competitionCodes.size() == 1
                ? FootballDataCompetitions.labelOf(competitionCodes.get(0))
                : FootballDataCompetitions.ALL_LABEL;
        syncMetaLabel.setText("Synchronisation en cours : " + scopeLabel + ".");
        showMutedStatus(matchesOnly
                ? "Import du calendrier en cours..."
                : "Import des clubs et effectifs en cours...");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        Task<FootballDataSyncSummary> syncTask = new Task<>() {
            @Override
            protected FootballDataSyncSummary call() throws Exception {
                updateMessage("Preparation de la synchronisation...");
                if (matchesOnly) {
                    return syncService.syncMatches(competitionCodes, this::updateMessage);
                }
                return syncService.syncTeamsAndPlayers(competitionCodes, this::updateMessage);
            }
        };

        syncTask.messageProperty().addListener((observable, oldValue, newValue) -> {
<<<<<<< HEAD
            if (backgroundSync || newValue == null || newValue.isBlank()) {
=======
            if (newValue == null || newValue.isBlank()) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                return;
            }
            syncMetaLabel.setText(newValue);
            showMutedStatus(newValue);
        });

        syncTask.setOnSucceeded(event -> {
<<<<<<< HEAD
=======
            syncingData = false;
            updateActionAvailability();

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            FootballDataSyncSummary summary = syncTask.getValue();
            String summaryMessage = summary == null
                    ? "Synchronisation terminee."
                    : summary.toHumanMessage(!matchesOnly, matchesOnly);
<<<<<<< HEAD
            if (backgroundSync) {
                backgroundSyncing = false;
                lastBackgroundMatchSyncAtMillis = System.currentTimeMillis();
            } else {
                syncingData = false;
                updateActionAvailability();
                syncMetaLabel.setText("Synchronise : " + summaryMessage);
            }
            if (!matchesOnly) {
                EquipeUiSupport.clearImageCache();
            }
            LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            refreshDataAsync(null, backgroundSync ? null : "status-success", backgroundSync ? null : summaryMessage, backgroundSync);
        });

        syncTask.setOnFailed(event -> {
            Throwable throwable = syncTask.getException();
            if (backgroundSync) {
                backgroundSyncing = false;
                lastBackgroundMatchSyncAtMillis = System.currentTimeMillis();
                System.err.println("Background match sync failed: " + (throwable == null ? "unknown error" : throwable.getMessage()));
                return;
            }

=======
            syncMetaLabel.setText("Synchronise : " + summaryMessage);
            refreshDataAsync(null, "status-success", summaryMessage);
        });

        syncTask.setOnFailed(event -> {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            syncingData = false;
            updateActionAvailability();

            syncMetaLabel.setText("La synchronisation a echoue.");
            showErrorStatus("Erreur pendant la synchronisation.");
<<<<<<< HEAD
=======
            Throwable throwable = syncTask.getException();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            showAlert(Alert.AlertType.ERROR, "Synchronisation football-data.org",
                    throwable == null ? "Erreur inconnue." : throwable.getMessage());
        });

        DB_EXECUTOR.execute(syncTask);
    }

    private FootballDataSyncService ensureSyncService() {
        if (footballDataSyncService != null) {
            return footballDataSyncService;
        }

        try {
            footballDataSyncService = new FootballDataSyncService();
            return footballDataSyncService;
        } catch (Exception e) {
            showErrorStatus("Configuration football-data.org invalide.");
            showAlert(Alert.AlertType.ERROR, "football-data.org",
                    "Impossible de preparer la synchronisation.\n" + e.getMessage());
            return null;
        }
    }

<<<<<<< HEAD
    private boolean shouldRunBackgroundMatchSync() {
        if (STATUS_FINI.equals(selectedStatusFilter())) {
            return false;
        }

        List<String> competitionCodes = selectedBackgroundSyncCompetitionCodes();
        if (competitionCodes.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        return now - lastBackgroundMatchSyncAtMillis >= BACKGROUND_SYNC_SECONDS * 1000L;
    }

    private List<String> selectedBackgroundSyncCompetitionCodes() {
        if (selectedCompetitionCode != null) {
            return List.of(selectedCompetitionCode);
        }

        List<String> selectedCodes = selectedSyncCompetitionCodes();
        if (selectedCodes.size() == 1) {
            return selectedCodes;
        }
        return List.of();
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private void openMatchDetail(Matchs match) {
        if (match == null) {
            return;
        }

        SceneNavigator.switchScene(
                matchListView,
                "/tn/esprit/views/match-detail-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Fiche match",
                controller -> {
                    if (controller instanceof MatchDetailController matchDetailController) {
                        matchDetailController.setMatchContext(match);
                    }
                }
        );
    }

    private void updateActionAvailability() {
        boolean busy = !serviceReady || loadingData || syncingData;
        refreshButton.setDisable(busy);
        clearButton.setDisable(busy);
        searchField.setDisable(busy);
        statusFilterComboBox.setDisable(busy);
        syncCompetitionComboBox.setDisable(busy);
        syncTeamsButton.setDisable(busy);
        syncMatchesButton.setDisable(busy);
        matchListView.setDisable(busy);
    }

    private String selectedStatusFilter() {
        String selectedStatus = statusFilterComboBox.getValue();
        if (selectedStatus == null || STATUS_FILTER_ALL.equals(selectedStatus)) {
            return null;
        }
        return normalizeMatchStatus(selectedStatus);
    }

    private List<String> selectedSyncCompetitionCodes() {
        String code = resolveCompetitionCode(syncCompetitionComboBox.getValue());
        return code == null ? FootballDataCompetitions.DEFAULT_CODES : List.of(code);
    }

    private String resolveCompetitionCode(String label) {
        if (label == null || FootballDataCompetitions.ALL_LABEL.equals(label)) {
            return null;
        }
        return COMPETITION_CODES_BY_LABEL.get(label);
    }

    private String resolveCompetitionLabel(String competitionCode) {
        return competitionCode == null ? null : COMPETITION_LABELS.getOrDefault(competitionCode, competitionCode);
    }

    private String resolveCompetitionTag(Matchs match) {
        String competitionLabel = resolveCompetitionLabel(match == null ? null : match.getCompetitionCode());
        return competitionLabel == null ? "Autre competition" : competitionLabel;
    }

    private String resolveMatchReference(Matchs match) {
<<<<<<< HEAD
        return "";
=======
        if (match == null) {
            return "-";
        }
        String reference = emptyToNull(match.getIdMatch());
        return reference == null ? (match.getId() == null ? "-" : "#" + match.getId()) : reference;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private String resolveMatchLocation(Matchs match) {
        String location = emptyToNull(match == null ? null : match.getLieu());
        return location == null ? "Lieu non renseigne" : location;
    }

    private String resolveMatchType(Matchs match) {
        String type = emptyToNull(match == null ? null : match.getType());
        return type == null ? "Type non renseigne" : type;
    }

    private String resolveStatus(Matchs match) {
        String status = emptyToNull(match == null ? null : match.getStatut());
        return status == null ? STATUS_PROGRAMME : status;
    }

    private String normalizeMatchStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("prog")) {
            return STATUS_PROGRAMME;
        }
<<<<<<< HEAD
        if (isLiveStatusText(normalized)) {
=======
        if (normalized.contains("direct") || normalized.contains("cours") || normalized.contains("live")) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return STATUS_EN_DIRECT;
        }
        if (normalized.startsWith("fini") || normalized.contains("term")) {
            return STATUS_FINI;
        }
        if (normalized.contains("report") || normalized.contains("postpon") || normalized.contains("suspend")) {
            return STATUS_REPORTE;
        }
        if (normalized.contains("annul") || normalized.contains("cancel")) {
            return STATUS_ANNULE;
        }
        return null;
    }

    private void applyFixtureStatusStyle(Label label, String status) {
        label.getStyleClass().removeAll("fixture-status-scheduled", "fixture-status-live", "fixture-status-finished", "fixture-status-cancelled");
        label.getStyleClass().add(resolveFixtureStatusClass(status));
    }

    private String resolveFixtureStatusClass(String status) {
        String normalized = normalize(status);
        if (normalized == null || normalized.contains("prog")) {
            return "fixture-status-scheduled";
        }
<<<<<<< HEAD
        if (isLiveStatusText(normalized)) {
=======
        if (normalized.contains("cours") || normalized.contains("live")) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return "fixture-status-live";
        }
        if (normalized.contains("fini") || normalized.contains("term")) {
            return "fixture-status-finished";
        }
        if (normalized.contains("annul")) {
            return "fixture-status-cancelled";
        }
        return "fixture-status-scheduled";
    }

<<<<<<< HEAD
    private boolean isLiveStatusText(String normalized) {
        if (normalized == null) {
            return false;
        }
        return normalized.contains("direct")
                || normalized.contains("cours")
                || normalized.contains("live")
                || normalized.contains("mi-temps")
                || normalized.contains("mi temps")
                || normalized.contains("1re mi")
                || normalized.contains("premiere mi")
                || normalized.contains("2e mi")
                || normalized.contains("deuxieme mi")
                || normalized.contains("half")
                || normalized.contains("1h")
                || normalized.contains("2h")
                || normalized.contains("prolong")
                || normalized.contains("extra time")
                || normalized.contains("tirs au but")
                || normalized.contains("penalties")
                || normalized.contains("shootout");
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private String buildMatchLabel(Matchs match) {
        if (match == null) {
            return "Match";
        }
        return getEquipeName(match.getEquipeDomicileId()) + " vs " + getEquipeName(match.getEquipeExterieurId());
    }

    private String buildScore(Matchs match) {
        return (match.getScoreEquipeDomicile() == null ? "-" : match.getScoreEquipeDomicile())
                + " : "
                + (match.getScoreEquipeExterieur() == null ? "-" : match.getScoreEquipeExterieur());
    }

    private String getEquipeName(Integer equipeId) {
        Equipe equipe = getEquipe(equipeId);
        return equipe == null ? "Equipe inconnue" : emptyIfNull(equipe.getNom());
    }

    private Equipe getEquipe(Integer equipeId) {
        return equipeId == null ? null : equipeById.get(equipeId);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private void showMutedStatus(String message) {
        setStatus(message, "status-muted");
    }

    private void showErrorStatus(String message) {
        setStatus(message, "status-error");
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(message);
        setStatusStyle(styleClass);
    }

    private void setStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
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

<<<<<<< HEAD
    private record RefreshPayload(List<Equipe> loadedEquipes, List<Matchs> loadedMatchs, Set<Integer> favoriteMatchIds) {
=======
    private record RefreshPayload(List<Equipe> loadedEquipes, List<Matchs> loadedMatchs) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }
}

