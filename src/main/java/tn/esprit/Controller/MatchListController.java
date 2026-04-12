package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MatchListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final double CARD_LOGO_SIZE = 68;
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final Map<String, String> COMPETITION_CODES_BY_LABEL = COMPETITION_LABELS.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private static final String STATUS_FILTER_ALL = "Tous statuts";
    private static final String STATUS_PROGRAMME = "Programme";
    private static final String STATUS_EN_DIRECT = "En direct";
    private static final String STATUS_FINI = "Fini";
    private static final String STATUS_REPORTE = "Reporte";
    private static final String STATUS_ANNULE = "Annule";
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
    private ListView<Matchs> matchListView;
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
    private final AtomicLong refreshSequence = new AtomicLong();

    private MatchsService matchsService;
    private EquipeService equipeService;
    private FootballDataSyncService footballDataSyncService;
    private Map<Integer, Equipe> equipeById = Map.of();
    private String selectedCompetitionCode;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean syncingData;
    private SidebarModuleGroup sidebarModuleGroup;

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

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            serviceReady = true;
            refreshDataAsync("Chargement des matchs...", "status-success", "Calendrier pret.");
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
        refreshDataAsync("Actualisation des matchs...", "status-success", "Liste des matchs actualisee.");
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
        matchListView.setItems(filteredMatchs);
        matchListView.setPlaceholder(new Label(""));
        matchListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Matchs item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

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

        Image image = equipe == null ? null : EquipeUiSupport.loadEquipeImage(equipe.getImage());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(teamName, "SI"));
        fallbackLabel.getStyleClass().add("fixture-team-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
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
        if (matchsService == null || equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        loadingData = true;
        updateActionAvailability();
        if (loadingMessage != null) {
            showMutedStatus(loadingMessage);
        }

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

            matchs.setAll(payload.loadedMatchs);
            EquipeUiSupport.clearImageCache();
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
            showErrorStatus("Erreur pendant le chargement.");
            Throwable throwable = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Chargement",
                    "Erreur lors du chargement des matchs.\n" + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        String statusFilter = selectedStatusFilter();
        String competitionCode = selectedCompetitionCode;

        filteredMatchs.setPredicate(match ->
                (query == null || matchesQuery(match, query))
                        && (statusFilter == null || Objects.equals(statusFilter, normalizeMatchStatus(match.getStatut())))
                        && (competitionCode == null || Objects.equals(competitionCode, emptyToNull(match.getCompetitionCode())))
        );

        updateCounters();
        updateEmptyState();
    }

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
        FootballDataSyncService syncService = ensureSyncService();
        if (syncService == null) {
            return;
        }

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
            if (newValue == null || newValue.isBlank()) {
                return;
            }
            syncMetaLabel.setText(newValue);
            showMutedStatus(newValue);
        });

        syncTask.setOnSucceeded(event -> {
            syncingData = false;
            updateActionAvailability();

            FootballDataSyncSummary summary = syncTask.getValue();
            String summaryMessage = summary == null
                    ? "Synchronisation terminee."
                    : summary.toHumanMessage(!matchesOnly, matchesOnly);
            syncMetaLabel.setText("Synchronise : " + summaryMessage);
            refreshDataAsync(null, "status-success", summaryMessage);
        });

        syncTask.setOnFailed(event -> {
            syncingData = false;
            updateActionAvailability();

            syncMetaLabel.setText("La synchronisation a echoue.");
            showErrorStatus("Erreur pendant la synchronisation.");
            Throwable throwable = syncTask.getException();
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
        if (match == null) {
            return "-";
        }
        String reference = emptyToNull(match.getIdMatch());
        return reference == null ? (match.getId() == null ? "-" : "#" + match.getId()) : reference;
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
        if (normalized.contains("direct") || normalized.contains("cours") || normalized.contains("live")) {
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
        if (normalized.contains("cours") || normalized.contains("live")) {
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

    private record RefreshPayload(List<Equipe> loadedEquipes, List<Matchs> loadedMatchs) {
    }
}
