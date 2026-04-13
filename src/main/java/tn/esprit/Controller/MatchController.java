package tn.esprit.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.FootballDataSyncService;
import tn.esprit.services.FootballDataSyncSummary;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final double CARD_LOGO_SIZE = 68;
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final Map<String, String> COMPETITION_CODES_BY_LABEL = COMPETITION_LABELS.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private static final String ALL_COMPETITIONS_LABEL = "Toutes competitions";
    private static final String STATUS_FILTER_ALL = "Tous statuts";
    private static final String STATUS_PROGRAMME = "Programme";
    private static final String STATUS_EN_DIRECT = "En direct";
    private static final String STATUS_FINI = "Fini";
    private static final String STATUS_REPORTE = "Reporte";
    private static final String STATUS_ANNULE = "Annule";
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("match-db-worker"));

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
    private Label resultsMetaLabel;
    @FXML
    private Label selectionStateLabel;
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
    private TableView<Matchs> matchTableView;
    @FXML
    private TableColumn<Matchs, String> matchReferenceColumn;
    @FXML
    private TableColumn<Matchs, String> matchDateColumn;
    @FXML
    private TableColumn<Matchs, String> matchTimeColumn;
    @FXML
    private TableColumn<Matchs, String> matchHomeColumn;
    @FXML
    private TableColumn<Matchs, String> matchAwayColumn;
    @FXML
    private TableColumn<Matchs, String> matchScoreColumn;
    @FXML
    private TableColumn<Matchs, String> matchStatusColumn;
    @FXML
    private TableColumn<Matchs, String> matchCompetitionColumn;
    @FXML
    private TableColumn<Matchs, String> matchLocationColumn;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Label detailBadgeLabel;
    @FXML
    private Label detailStatusChipLabel;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private ImageView detailHomeLogoView;
    @FXML
    private Label detailHomeLogoFallbackLabel;
    @FXML
    private Label detailHomeNameLabel;
    @FXML
    private ImageView detailAwayLogoView;
    @FXML
    private Label detailAwayLogoFallbackLabel;
    @FXML
    private Label detailAwayNameLabel;
    @FXML
    private Label detailScoreValueLabel;
    @FXML
    private Label detailDateValueLabel;
    @FXML
    private Label detailHeureValueLabel;
    @FXML
    private Label detailLieuValueLabel;
    @FXML
    private Label detailTypeValueLabel;
    @FXML
    private Label detailStatutValueLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private DatePicker dateMatchPicker;
    @FXML
    private TextField heureDebutField;
    @FXML
    private TextField lieuField;
    @FXML
    private TextField typeField;
    @FXML
    private ComboBox<String> statutComboBox;
    @FXML
    private ComboBox<Equipe> equipeDomicileComboBox;
    @FXML
    private ComboBox<Equipe> equipeExterieurComboBox;
    @FXML
    private TextField scoreDomicileField;
    @FXML
    private TextField scoreExterieurField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
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
    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final Map<String, Optional<Image>> imageCache = new ConcurrentHashMap<>();

    private MatchsService matchsService;
    private EquipeService equipeService;
    private FootballDataSyncService footballDataSyncService;
    private Map<Integer, Equipe> equipeById = Map.of();
    private Matchs selectedMatch;
    private String selectedCompetitionCode;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean mutatingData;
    private boolean equipesLoaded;
    private boolean syncingData;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabel();
        configureSyncSection();
        configureStatusFilter();
        configureSearch();
        configureTeamComboBoxes();
        configureStatusChoices();
        configureFormatters();
        configureMatchList();
        bindFormState();
        updateActionAvailability();
        updateCounters(null);
        updateDetailPanel();

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            serviceReady = true;
            refreshDataAsync(null, true, "Chargement des matchs...", "status-success", "Module Match pret.");
        } catch (SQLException e) {
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
            String selectedLabel = selectedCompetitionCode == null
                    ? FootballDataCompetitions.ALL_LABEL
                    : resolveCompetitionLabel(selectedCompetitionCode);
            syncCompetitionComboBox.getSelectionModel().select(selectedLabel);
        }

        if (searchField != null && (matchTableView != null || matchListView != null)) {
            applyFilters();
        } else {
            updateCounters(null);
        }
    }

    @FXML
    private void handleAdd() {
        clearValidation();
        Matchs match = buildMatchFromForm(false);
        if (match == null || matchsService == null) {
            return;
        }

        runMutation(
                () -> matchsService.add(match),
                null,
                true,
                false,
                "Match ajoute avec succes.",
                "Ajout",
                "Erreur pendant l'ajout.",
                "Erreur lors de l'ajout du match."
        );
    }

    @FXML
    private void handleUpdate() {
        clearValidation();
        if (selectedMatch == null) {
            showValidation("Selectionnez un match a modifier.");
            return;
        }

        Matchs match = buildMatchFromForm(true);
        if (match == null || matchsService == null) {
            return;
        }

        match.setId(selectedMatch.getId());
        match.setIdMatch(selectedMatch.getIdMatch());

        Integer preferredSelectionId = selectedMatch.getId();
        runMutation(
                () -> matchsService.update(match),
                preferredSelectionId,
                false,
                false,
                "Match modifie avec succes.",
                "Modification",
                "Erreur pendant la modification.",
                "Erreur lors de la modification du match."
        );
    }

    @FXML
    private void handleDelete() {
        clearValidation();
        if (selectedMatch == null) {
            showValidation("Selectionnez un match a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le match \"" + buildMatchLabel(selectedMatch) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        Integer selectedMatchId = selectedMatch.getId();
        runMutation(
                () -> matchsService.delete(selectedMatchId),
                null,
                true,
                false,
                "Match supprime avec succes.",
                "Suppression",
                "Erreur pendant la suppression.",
                "Erreur lors de la suppression du match."
        );
    }

    @FXML
    private void handleRefresh() {
        refreshDataAsync(
                getSelectedMatchId(),
                true,
                "Actualisation des matchs...",
                "status-muted",
                "Liste des matchs actualisee."
        );
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
    private void handleClear() {
        searchField.clear();
        statusFilterComboBox.getSelectionModel().select(STATUS_FILTER_ALL);
        clearForm();
        applyFilters();
        showMutedStatus("Recherche et formulaire reinitialises.");
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
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs(ActionEvent event) {
        if (event != null
                && event.getSource() == matchsNavButton
                && sidebarModuleGroup != null
                && sidebarModuleGroup.handleMatchsClick()) {
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
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
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

    private void configureSyncSection() {
        ObservableList<String> competitionOptions = FXCollections.observableArrayList();
        competitionOptions.add(FootballDataCompetitions.ALL_LABEL);
        competitionOptions.addAll(COMPETITION_LABELS.values());

        syncCompetitionComboBox.setItems(competitionOptions);
        syncCompetitionComboBox.getSelectionModel().select(FootballDataCompetitions.ALL_LABEL);

        syncMetaLabel.setText("Plan gratuit : un lot complet sur les 6 competitions prend environ 40 secondes.");
    }

    private void configureSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureStatusFilter() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                STATUS_FILTER_ALL,
                STATUS_PROGRAMME,
                STATUS_FINI
        ));
        statusFilterComboBox.getSelectionModel().select(STATUS_FILTER_ALL);
    }

    private void configureTeamComboBoxes() {
        equipeDomicileComboBox.setItems(equipes);
        equipeExterieurComboBox.setItems(equipes);
        equipeDomicileComboBox.setCellFactory(listView -> createEquipeCell());
        equipeDomicileComboBox.setButtonCell(createEquipeCell());
        equipeExterieurComboBox.setCellFactory(listView -> createEquipeCell());
        equipeExterieurComboBox.setButtonCell(createEquipeCell());
    }

    private void configureStatusChoices() {
        statutComboBox.setItems(FXCollections.observableArrayList(
                STATUS_PROGRAMME,
                STATUS_EN_DIRECT,
                STATUS_FINI,
                STATUS_REPORTE,
                STATUS_ANNULE
        ));
        statutComboBox.getSelectionModel().select(STATUS_PROGRAMME);
        updateScoreFieldsForStatus(true);
    }

    private ListCell<Equipe> createEquipeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        };
    }

    private void configureFormatters() {
        scoreDomicileField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        scoreExterieurField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        heureDebutField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9:]{0,5}") ? change : null));
    }

    private void configureMatchList() {
        if (matchTableView != null) {
            matchReferenceColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveMatchReference(cell.getValue())));
            matchDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDateMatch())));
            matchTimeColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue().getHeureDebut())));
            matchHomeColumn.setCellValueFactory(cell -> new SimpleStringProperty(getEquipeName(cell.getValue().getEquipeDomicileId())));
            matchAwayColumn.setCellValueFactory(cell -> new SimpleStringProperty(getEquipeName(cell.getValue().getEquipeExterieurId())));
            matchScoreColumn.setCellValueFactory(cell -> new SimpleStringProperty(buildScore(cell.getValue())));
            matchStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveStatus(cell.getValue())));
            matchCompetitionColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveCompetitionTag(cell.getValue())));
            matchLocationColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveMatchLocation(cell.getValue())));

            matchTableView.setItems(filteredMatchs);
            matchTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            matchTableView.setPlaceholder(new Label(""));
            matchTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    handleSelectedMatchChange(newValue));
        }

        if (matchListView != null) {
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
                    setText(null);
                    VBox card = buildMatchCard(item);
                    card.prefWidthProperty().bind(listView.widthProperty().subtract(26));
                    setGraphic(card);
                }
            });
            matchListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    handleSelectedMatchChange(newValue));
        }
    }

    private void handleSelectedMatchChange(Matchs newValue) {
        selectedMatch = newValue;
        if (newValue != null) {
            populateForm(newValue);
        } else if (!hasDraftContent()) {
            clearFormFieldsOnly();
        }

        clearValidation();
        updateActionAvailability();
        updateSelectionState();
        updateDetailPanel();
    }

    private VBox buildMatchCard(Matchs match) {
        Label statusChip = new Label(resolveStatus(match));
        statusChip.getStyleClass().add("fixture-status");
        applyFixtureStatusStyle(statusChip, match.getStatut());

        Label dateLabel = new Label(formatDate(match.getDateMatch()) + "  |  " + formatTime(match.getHeureDebut()));
        dateLabel.getStyleClass().add("fixture-date");

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        Label idLabel = new Label(match.getIdMatch() == null ? "#" + match.getId() : match.getIdMatch());
        idLabel.getStyleClass().add("fixture-id");

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

        Label locationChip = new Label(emptyToNull(match.getLieu()) == null ? "Lieu non renseigne" : match.getLieu());
        locationChip.getStyleClass().add("fixture-meta-chip");

        Label typeChip = new Label(emptyToNull(match.getType()) == null ? "Type non renseigne" : match.getType());
        typeChip.getStyleClass().add("fixture-meta-chip");

        Label detailChip = new Label("Selectionner");
        detailChip.getStyleClass().add("fixture-link-chip");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        HBox metaRow = new HBox(10, competitionChip, locationChip, typeChip, metaSpacer, detailChip);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("fixture-meta-row");

        VBox card = new VBox(14, head, teamsRow, metaRow);
        card.getStyleClass().add("fixture-card");
        card.getStyleClass().add("fixture-card-clickable");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildTeamPreview(Equipe equipe, String fallbackRole) {
        String teamName = equipe == null ? "Equipe " + fallbackRole.toLowerCase() : emptyIfNull(equipe.getNom());
        StackPane logoPane = createLogoPane(equipe == null ? null : equipe.getImage(), teamName, CARD_LOGO_SIZE, "fixture-team-logo-shell", "fixture-team-fallback");

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

    private void bindFormState() {
        dateMatchPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(dateMatchPicker);
            updateSelectionState();
            updateDetailPanel();
        });
        heureDebutField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(heureDebutField);
            updateSelectionState();
            updateDetailPanel();
        });
        lieuField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(lieuField);
            updateSelectionState();
            updateDetailPanel();
        });
        typeField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(typeField);
            updateSelectionState();
            updateDetailPanel();
        });
        statutComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(statutComboBox);
            updateScoreFieldsForStatus(true);
            updateSelectionState();
            updateDetailPanel();
        });
        equipeDomicileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(equipeDomicileComboBox);
            updateSelectionState();
            updateDetailPanel();
        });
        equipeExterieurComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(equipeExterieurComboBox);
            updateSelectionState();
            updateDetailPanel();
        });
        scoreDomicileField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(scoreDomicileField);
            updateDetailPanel();
        });
        scoreExterieurField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(scoreExterieurField);
            updateDetailPanel();
        });
    }

    private void openMatchDetail(Matchs match) {
        if (match == null) {
            return;
        }

        SceneNavigator.switchScene(
                resolveMatchNavigationSource(),
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

    private void refreshDataAsync(
            Integer preferredSelectionId,
            boolean reloadEquipes,
            String loadingMessage,
            String successStyleClass,
            String successMessage
    ) {
        if (matchsService == null || equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        boolean shouldLoadEquipes = reloadEquipes || !equipesLoaded || equipes.isEmpty();
        loadingData = true;
        updateActionAvailability();
        if (loadingMessage != null) {
            showMutedStatus(loadingMessage);
        }

        Task<RefreshPayload> loadTask = new Task<>() {
            @Override
            protected RefreshPayload call() throws Exception {
                List<Equipe> loadedEquipes = null;
                if (shouldLoadEquipes) {
                    loadedEquipes = new ArrayList<>(equipeService.getAll());
                    loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                }

                List<Matchs> loadedMatchs = new ArrayList<>(matchsService.getAll());
                loadedMatchs.sort(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                        .reversed());

                return new RefreshPayload(loadedEquipes, loadedMatchs, shouldLoadEquipes);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            RefreshPayload payload = loadTask.getValue();
            if (payload.equipesReloaded) {
                Integer selectedDomicileId = selectedEquipeId(equipeDomicileComboBox);
                Integer selectedExterieurId = selectedEquipeId(equipeExterieurComboBox);
                applyLoadedEquipes(payload.loadedEquipes, selectedDomicileId, selectedExterieurId);
                imageCache.clear();
                equipesLoaded = true;
            }

            matchs.setAll(payload.loadedMatchs);
            applyFilters();
            restoreSelection(preferredSelectionId);
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
            String details = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des matchs.\n" + details);
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        String competitionCode = selectedCompetitionFilterCode();
        String statusFilter = selectedStatusFilter();
        filteredMatchs.setPredicate(match ->
                (query == null || matchesQuery(match, query))
                        && matchesStatusFilter(match, statusFilter)
                        && matchesCompetitionFilter(match, competitionCode)
        );
        updateCounters(query);
        updateEmptyState();

        if (selectedMatch != null && !filteredMatchs.contains(selectedMatch)) {
            selectedMatch = null;
            clearMatchSelection();
            updateActionAvailability();
        }
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

    private boolean matchesCompetitionFilter(Matchs match, String competitionCode) {
        return competitionCode == null || Objects.equals(competitionCode, emptyToNull(match.getCompetitionCode()));
    }

    private boolean matchesStatusFilter(Matchs match, String statusFilter) {
        return statusFilter == null || Objects.equals(statusFilter, normalizeMatchStatus(match == null ? null : match.getStatut()));
    }

    private void updateCounters(String query) {
        int count = filteredMatchs.size();
        resultCountLabel.setText(count + " match(s)");
        StringBuilder meta = new StringBuilder(count + (isCardLayout() ? " carte(s)" : " rencontre(s)"));
        if (competitionCodeSelected()) {
            meta.append(" | ").append(selectedCompetitionLabel());
        } else if (!isCardLayout()) {
            meta.append(" affichee(s)");
        }
        if (selectedStatusFilter() != null) {
            meta.append(" | ").append(selectedStatusFilter());
        }
        resultsMetaLabel.setText(meta.toString());
        updateSelectionState();
    }

    private void updateEmptyState() {
        boolean empty = filteredMatchs.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
    }

    private void updateSelectionState() {
        if (selectedMatch != null) {
            selectionStateLabel.setText("Selection : " + buildMatchLabel(selectedMatch));
            return;
        }

        if (hasDraftContent()) {
            selectionStateLabel.setText("Brouillon en cours");
            return;
        }

        selectionStateLabel.setText(competitionCodeSelected()
                ? "Competition : " + selectedCompetitionLabel()
                : "Aucune selection");
    }

    private void applyLoadedEquipes(List<Equipe> loadedEquipes, Integer selectedDomicileId, Integer selectedExterieurId) {
        if (loadedEquipes == null) {
            return;
        }

        equipes.setAll(loadedEquipes);
        equipeById = loadedEquipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, Function.identity(), (left, right) -> left));

        selectEquipe(equipeDomicileComboBox, selectedDomicileId);
        selectEquipe(equipeExterieurComboBox, selectedExterieurId);
    }

    private Matchs buildMatchFromForm(boolean updateMode) {
        LocalDate dateMatch = dateMatchPicker.getValue();
        String heureText = emptyToNull(heureDebutField.getText());
        String lieu = emptyToNull(lieuField.getText());
        String type = emptyToNull(typeField.getText());
        String statut = normalizeMatchStatus(statutComboBox.getValue());
        Equipe equipeDomicile = equipeDomicileComboBox.getValue();
        Equipe equipeExterieur = equipeExterieurComboBox.getValue();
        String scoreDomicileText = emptyToNull(scoreDomicileField.getText());
        String scoreExterieurText = emptyToNull(scoreExterieurField.getText());

        if (dateMatch == null) {
            markFieldInvalid(dateMatchPicker);
            showValidation("La date du match est obligatoire.");
            return null;
        }

        if (heureText == null) {
            markFieldInvalid(heureDebutField);
            showValidation("L'heure de debut est obligatoire.");
            return null;
        }

        LocalTime heureDebut;
        try {
            heureDebut = LocalTime.parse(heureText);
        } catch (DateTimeParseException e) {
            markFieldInvalid(heureDebutField);
            showValidation("L'heure doit etre au format HH:mm.");
            return null;
        }

        if (lieu == null) {
            markFieldInvalid(lieuField);
            showValidation("Le lieu est obligatoire.");
            return null;
        }

        if (type == null) {
            markFieldInvalid(typeField);
            showValidation("Le type est obligatoire.");
            return null;
        }

        if (statut == null) {
            markFieldInvalid(statutComboBox);
            showValidation("Choisissez un statut: Programme ou Fini.");
            return null;
        }

        if (equipeDomicile == null || equipeDomicile.getId() == null) {
            markFieldInvalid(equipeDomicileComboBox);
            showValidation("L'equipe domicile est obligatoire.");
            return null;
        }

        if (equipeExterieur == null || equipeExterieur.getId() == null) {
            markFieldInvalid(equipeExterieurComboBox);
            showValidation("L'equipe exterieur est obligatoire.");
            return null;
        }

        if (Objects.equals(equipeDomicile.getId(), equipeExterieur.getId())) {
            markFieldInvalid(equipeDomicileComboBox);
            markFieldInvalid(equipeExterieurComboBox);
            showValidation("Les equipes domicile et exterieur doivent etre differentes.");
            return null;
        }

        Integer scoreDomicile = null;
        Integer scoreExterieur = null;
        if (isScoreLockedStatus(statut)) {
            if (scoreDomicileText != null || scoreExterieurText != null) {
                markFieldInvalid(scoreDomicileField);
                markFieldInvalid(scoreExterieurField);
                showValidation("Ce statut ne permet pas de score.");
                return null;
            }
        } else {
            if (requiresFinalScores(statut) && (scoreDomicileText == null || scoreExterieurText == null)) {
                if (scoreDomicileText == null) {
                    markFieldInvalid(scoreDomicileField);
                }
                if (scoreExterieurText == null) {
                    markFieldInvalid(scoreExterieurField);
                }
                showValidation("Pour un match fini, les deux scores sont obligatoires.");
                return null;
            }

            if (scoreDomicileText != null) {
                scoreDomicile = parseScore(scoreDomicileText, scoreDomicileField);
                if (scoreDomicile == null) {
                    return null;
                }
            }

            if (scoreExterieurText != null) {
                scoreExterieur = parseScore(scoreExterieurText, scoreExterieurField);
                if (scoreExterieur == null) {
                    return null;
                }
            }
        }

        String idMatch = updateMode && selectedMatch != null && selectedMatch.getIdMatch() != null
                ? selectedMatch.getIdMatch()
                : generateMatchReference();

        Matchs match = new Matchs(
                idMatch,
                dateMatch,
                heureDebut,
                lieu,
                type,
                statut,
                "",
                "",
                scoreDomicile,
                scoreExterieur,
                equipeDomicile.getId(),
                equipeExterieur.getId()
        );
        match.setCompetitionCode(resolveFormCompetitionCode(updateMode));
        return match;
    }

    private Integer parseScore(String scoreText, Control field) {
        if (scoreText == null) {
            return null;
        }

        try {
            int value = Integer.parseInt(scoreText);
            if (value < 0) {
                markFieldInvalid(field);
                showValidation("Les scores doivent etre positifs.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            markFieldInvalid(field);
            showValidation("Les scores doivent etre numeriques.");
            return null;
        }
    }

    private void populateForm(Matchs match) {
        dateMatchPicker.setValue(match.getDateMatch());
        heureDebutField.setText(match.getHeureDebut() == null ? "" : match.getHeureDebut().format(TIME_FORMATTER));
        lieuField.setText(emptyIfNull(match.getLieu()));
        typeField.setText(emptyIfNull(match.getType()));
        String status = normalizeMatchStatus(match.getStatut());
        statutComboBox.setValue(status == null ? STATUS_PROGRAMME : status);
        scoreDomicileField.setText(match.getScoreEquipeDomicile() == null ? "" : String.valueOf(match.getScoreEquipeDomicile()));
        scoreExterieurField.setText(match.getScoreEquipeExterieur() == null ? "" : String.valueOf(match.getScoreEquipeExterieur()));
        selectEquipe(equipeDomicileComboBox, match.getEquipeDomicileId());
        selectEquipe(equipeExterieurComboBox, match.getEquipeExterieurId());
        updateScoreFieldsForStatus(true);
    }

    private void restoreSelection(Integer preferredSelectionId) {
        if (preferredSelectionId == null) {
            selectedMatch = null;
            clearMatchSelection();
            updateActionAvailability();
            updateSelectionState();
            return;
        }

        for (Matchs match : filteredMatchs) {
            if (Objects.equals(match.getId(), preferredSelectionId)) {
                selectMatch(match);
                selectedMatch = match;
                updateActionAvailability();
                updateSelectionState();
                return;
            }
        }

        selectedMatch = null;
        clearMatchSelection();
        updateActionAvailability();
        updateSelectionState();
    }

    private void updateDetailPanel() {
        Matchs effectiveMatch = selectedMatch;
        Equipe homeTeam = resolveEquipe(equipeDomicileComboBox.getValue(), effectiveMatch == null ? null : effectiveMatch.getEquipeDomicileId());
        Equipe awayTeam = resolveEquipe(equipeExterieurComboBox.getValue(), effectiveMatch == null ? null : effectiveMatch.getEquipeExterieurId());
        String draftLabel = buildDraftLabel(homeTeam, awayTeam);

        if (effectiveMatch == null && !hasDraftContent()) {
            detailBadgeLabel.setText("Apercu");
            detailStatusChipLabel.setText("A venir");
            applyDetailStatusStyle(detailStatusChipLabel, null);
            detailTitleLabel.setText("Aucun match selectionne");
            detailSubtitleLabel.setText("Selectionnez une rencontre ou commencez une nouvelle creation pour afficher la fiche detail.");
            detailScoreValueLabel.setText("-  :  -");
            detailDateValueLabel.setText("-");
            detailHeureValueLabel.setText("-");
            detailLieuValueLabel.setText("Non renseigne");
            detailTypeValueLabel.setText("Non renseigne");
            detailStatutValueLabel.setText("Programme");
            detailIdValueLabel.setText("Nouveau");
            detailHomeNameLabel.setText("Equipe domicile");
            detailAwayNameLabel.setText("Equipe exterieur");
            updateDetailLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, null, "D");
            updateDetailLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, null, "E");
            return;
        }

        String status = resolveFieldValue(
                normalizeMatchStatus(statutComboBox.getValue()),
                normalizeMatchStatus(effectiveMatch == null ? null : effectiveMatch.getStatut()),
                STATUS_PROGRAMME
        );
        String title = draftLabel == null ? buildMatchLabel(effectiveMatch) : draftLabel;

        detailBadgeLabel.setText(selectedMatch == null ? "Brouillon" : "Fiche match");
        detailStatusChipLabel.setText(status);
        applyDetailStatusStyle(detailStatusChipLabel, status);
        detailTitleLabel.setText(title);
        detailSubtitleLabel.setText(buildDetailSubtitle(effectiveMatch, homeTeam, awayTeam));
        detailScoreValueLabel.setText(buildDraftScore(effectiveMatch));
        detailDateValueLabel.setText(formatDate(dateMatchPicker.getValue() != null ? dateMatchPicker.getValue() : effectiveMatch == null ? null : effectiveMatch.getDateMatch()));
        detailHeureValueLabel.setText(formatTime(parseTimeSafely(heureDebutField.getText(), effectiveMatch == null ? null : effectiveMatch.getHeureDebut())));
        detailLieuValueLabel.setText(resolveFieldValue(lieuField.getText(), effectiveMatch == null ? null : effectiveMatch.getLieu(), "Non renseigne"));
        detailTypeValueLabel.setText(resolveFieldValue(typeField.getText(), effectiveMatch == null ? null : effectiveMatch.getType(), "Non renseigne"));
        detailStatutValueLabel.setText(status);
        detailIdValueLabel.setText(selectedMatch == null ? "Nouveau" : (effectiveMatch.getIdMatch() == null ? "#" + effectiveMatch.getId() : effectiveMatch.getIdMatch()));
        detailHomeNameLabel.setText(homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom()));
        detailAwayNameLabel.setText(awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom()));
        updateDetailLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, homeTeam, "D");
        updateDetailLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, awayTeam, "E");
    }

    private String buildDetailSubtitle(Matchs effectiveMatch, Equipe homeTeam, Equipe awayTeam) {
        String home = homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom());
        String away = awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom());
        String competitionLabel = effectiveMatch == null
                ? (competitionCodeSelected() ? selectedCompetitionLabel() : null)
                : resolveCompetitionLabel(effectiveMatch.getCompetitionCode());
        if (competitionLabel == null) {
            return home + " recoit " + away + " dans une presentation inspiree du front-office Symfony.";
        }
        return competitionLabel + " | " + home + " recoit " + away + ".";
    }

    private void updateDetailLogo(ImageView imageView, Label fallbackLabel, Equipe equipe, String defaultLetter) {
        Image image = equipe == null ? null : loadImage(equipe.getImage());
        boolean hasImage = image != null;

        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setText(equipe == null ? defaultLetter : buildInitials(equipe.getNom(), defaultLetter));
    }

    private void clearForm() {
        clearMatchSelection();
        selectedMatch = null;
        clearFormFieldsOnly();
        clearValidation();
        updateActionAvailability();
        updateSelectionState();
        updateDetailPanel();
    }

    private void clearFormFieldsOnly() {
        dateMatchPicker.setValue(null);
        heureDebutField.clear();
        lieuField.clear();
        typeField.clear();
        statutComboBox.setValue(STATUS_PROGRAMME);
        scoreDomicileField.clear();
        scoreExterieurField.clear();
        equipeDomicileComboBox.getSelectionModel().clearSelection();
        equipeExterieurComboBox.getSelectionModel().clearSelection();
        updateScoreFieldsForStatus(true);
    }

    private void runSync(boolean matchesOnly) {
        clearValidation();

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
            String summaryMessage = summary.toHumanMessage(!matchesOnly, matchesOnly);
            syncMetaLabel.setText("Synchronise : " + summaryMessage);
            refreshDataAsync(getSelectedMatchId(), true, null, "status-success", summaryMessage);
        });

        syncTask.setOnFailed(event -> {
            syncingData = false;
            updateActionAvailability();

            syncMetaLabel.setText("La synchronisation a echoue.");
            showErrorStatus("Erreur pendant la synchronisation.");
            Throwable throwable = syncTask.getException();
            String details = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showAlert(Alert.AlertType.ERROR, "Synchronisation football-data.org", details);
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

    private String selectedCompetitionFilterCode() {
        return selectedCompetitionCode;
    }

    private String selectedStatusFilter() {
        String selectedStatus = statusFilterComboBox == null ? null : statusFilterComboBox.getValue();
        if (selectedStatus == null || STATUS_FILTER_ALL.equals(selectedStatus)) {
            return null;
        }
        return normalizeMatchStatus(selectedStatus);
    }

    private List<String> selectedSyncCompetitionCodes() {
        String code = resolveCompetitionCode(syncCompetitionComboBox.getValue());
        return code == null ? FootballDataCompetitions.DEFAULT_CODES : List.of(code);
    }

    private boolean competitionCodeSelected() {
        return selectedCompetitionCode != null;
    }

    private String resolveCompetitionCode(String label) {
        if (label == null || FootballDataCompetitions.ALL_LABEL.equals(label)) {
            return null;
        }
        return COMPETITION_CODES_BY_LABEL.get(label);
    }

    private String resolveCompetitionLabel(String competitionCode) {
        if (competitionCode == null) {
            return null;
        }
        return COMPETITION_LABELS.getOrDefault(competitionCode, competitionCode);
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
        if (reference != null) {
            return reference;
        }
        return match.getId() == null ? "-" : "#" + match.getId();
    }

    private String resolveMatchLocation(Matchs match) {
        String location = emptyToNull(match == null ? null : match.getLieu());
        return location == null ? "Non renseigne" : location;
    }

    private String resolveFormCompetitionCode(boolean updateMode) {
        String currentCompetitionCode = emptyToNull(selectedCompetitionCode);
        if (currentCompetitionCode != null) {
            return currentCompetitionCode;
        }
        if (updateMode && selectedMatch != null) {
            return emptyToNull(selectedMatch.getCompetitionCode());
        }
        return null;
    }

    private String selectedCompetitionLabel() {
        return selectedCompetitionCode == null ? ALL_COMPETITIONS_LABEL : resolveCompetitionLabel(selectedCompetitionCode);
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedMatch != null;
        boolean busy = loadingData || mutatingData || syncingData;
        addButton.setDisable(!serviceReady || busy);
        updateButton.setDisable(!serviceReady || !hasSelection || busy);
        deleteButton.setDisable(!serviceReady || !hasSelection || busy);
        clearButton.setDisable(!serviceReady || busy);
        refreshButton.setDisable(!serviceReady || busy);
        searchField.setDisable(!serviceReady || busy);
        statusFilterComboBox.setDisable(!serviceReady || busy);
        syncCompetitionComboBox.setDisable(!serviceReady || busy);
        syncTeamsButton.setDisable(!serviceReady || busy);
        syncMatchesButton.setDisable(!serviceReady || busy);
        if (matchTableView != null) {
            matchTableView.setDisable(!serviceReady || busy);
        }
        if (matchListView != null) {
            matchListView.setDisable(!serviceReady || busy);
        }
    }

    private boolean isCardLayout() {
        return matchListView != null;
    }

    private void clearMatchSelection() {
        if (matchTableView != null) {
            matchTableView.getSelectionModel().clearSelection();
        }
        if (matchListView != null) {
            matchListView.getSelectionModel().clearSelection();
        }
    }

    private void selectMatch(Matchs match) {
        if (matchTableView != null) {
            matchTableView.getSelectionModel().select(match);
            matchTableView.scrollTo(match);
        }
        if (matchListView != null) {
            matchListView.getSelectionModel().select(match);
            matchListView.scrollTo(match);
        }
    }

    private Node resolveMatchNavigationSource() {
        if (matchListView != null) {
            return matchListView;
        }
        if (matchTableView != null) {
            return matchTableView;
        }
        return detailTitleLabel;
    }

    private boolean hasDraftContent() {
        return dateMatchPicker.getValue() != null
                || emptyToNull(heureDebutField.getText()) != null
                || emptyToNull(lieuField.getText()) != null
                || emptyToNull(typeField.getText()) != null
                || (normalizeMatchStatus(statutComboBox.getValue()) != null
                && !STATUS_PROGRAMME.equals(normalizeMatchStatus(statutComboBox.getValue())))
                || emptyToNull(scoreDomicileField.getText()) != null
                || emptyToNull(scoreExterieurField.getText()) != null
                || equipeDomicileComboBox.getValue() != null
                || equipeExterieurComboBox.getValue() != null;
    }

    private Integer getSelectedMatchId() {
        return selectedMatch == null ? null : selectedMatch.getId();
    }

    private Integer selectedEquipeId(ComboBox<Equipe> comboBox) {
        Equipe equipe = comboBox.getValue();
        return equipe == null ? null : equipe.getId();
    }

    private void selectEquipe(ComboBox<Equipe> comboBox, Integer equipeId) {
        if (equipeId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        for (Equipe equipe : equipes) {
            if (Objects.equals(equipe.getId(), equipeId)) {
                comboBox.getSelectionModel().select(equipe);
                return;
            }
        }
        comboBox.getSelectionModel().clearSelection();
    }

    private String buildMatchLabel(Matchs match) {
        if (match == null) {
            return "Nouveau match";
        }
        return getEquipeName(match.getEquipeDomicileId()) + " vs " + getEquipeName(match.getEquipeExterieurId());
    }

    private String buildDraftLabel(Equipe homeTeam, Equipe awayTeam) {
        if (homeTeam == null && awayTeam == null) {
            return null;
        }
        String home = homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom());
        String away = awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom());
        return home + " vs " + away;
    }

    private String getEquipeName(Integer equipeId) {
        Equipe equipe = getEquipe(equipeId);
        return equipe == null ? "Equipe inconnue" : emptyIfNull(equipe.getNom());
    }

    private Equipe getEquipe(Integer equipeId) {
        return equipeId == null ? null : equipeById.get(equipeId);
    }

    private Equipe resolveEquipe(Equipe selectedEquipe, Integer fallbackId) {
        if (selectedEquipe != null) {
            return selectedEquipe;
        }
        return getEquipe(fallbackId);
    }

    private String buildDraftScore(Matchs effectiveMatch) {
        String status = resolveFieldValue(
                normalizeMatchStatus(statutComboBox.getValue()),
                normalizeMatchStatus(effectiveMatch == null ? null : effectiveMatch.getStatut()),
                STATUS_PROGRAMME
        );
        if (isScoreLockedStatus(status)) {
            return "-  :  -";
        }

        String home = emptyToNull(scoreDomicileField.getText());
        String away = emptyToNull(scoreExterieurField.getText());
        if (home == null && effectiveMatch != null && effectiveMatch.getScoreEquipeDomicile() != null) {
            home = String.valueOf(effectiveMatch.getScoreEquipeDomicile());
        }
        if (away == null && effectiveMatch != null && effectiveMatch.getScoreEquipeExterieur() != null) {
            away = String.valueOf(effectiveMatch.getScoreEquipeExterieur());
        }
        return (home == null ? "-" : home) + "  :  " + (away == null ? "-" : away);
    }

    private String buildScore(Matchs match) {
        return (match.getScoreEquipeDomicile() == null ? "-" : match.getScoreEquipeDomicile())
                + " : "
                + (match.getScoreEquipeExterieur() == null ? "-" : match.getScoreEquipeExterieur());
    }

    private String resolveFieldValue(String draftValue, String fallbackValue, String emptyValue) {
        String cleanedDraft = emptyToNull(draftValue);
        if (cleanedDraft != null) {
            return cleanedDraft;
        }
        String cleanedFallback = emptyToNull(fallbackValue);
        return cleanedFallback == null ? emptyValue : cleanedFallback;
    }

    private String resolveStatus(Matchs match) {
        String status = match == null ? null : emptyToNull(match.getStatut());
        return status == null ? "Programme" : status;
    }

    private LocalTime parseTimeSafely(String text, LocalTime fallback) {
        String value = emptyToNull(text);
        if (value == null) {
            return fallback;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private String generateMatchReference() {
        return "MATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void showMutedStatus(String message) {
        setStatus(message, "status-muted");
    }

    private void showSuccessStatus(String message) {
        setStatus(message, "status-success");
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

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void clearValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        clearFieldError(dateMatchPicker);
        clearFieldError(heureDebutField);
        clearFieldError(lieuField);
        clearFieldError(typeField);
        clearFieldError(statutComboBox);
        clearFieldError(equipeDomicileComboBox);
        clearFieldError(equipeExterieurComboBox);
        clearFieldError(scoreDomicileField);
        clearFieldError(scoreExterieurField);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private void applyFixtureStatusStyle(Label label, String status) {
        label.getStyleClass().removeAll("fixture-status-scheduled", "fixture-status-live", "fixture-status-finished", "fixture-status-cancelled");
        label.getStyleClass().add(resolveFixtureStatusClass(status));
    }

    private void applyDetailStatusStyle(Label label, String status) {
        label.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        String styleClass = switch (resolveFixtureStatusClass(status)) {
            case "fixture-status-live" -> "status-success";
            case "fixture-status-finished" -> "status-muted";
            case "fixture-status-cancelled" -> "status-error";
            default -> "status-warning";
        };
        if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private String resolveFixtureStatusClass(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
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
        if (normalized.contains("report") || normalized.contains("postpon") || normalized.contains("suspend")) {
            return "fixture-status-scheduled";
        }
        if (normalized.contains("prog")) {
            return "fixture-status-scheduled";
        }
        return "fixture-status-scheduled";
    }

    private void updateScoreFieldsForStatus(boolean clearWhenProgramme) {
        String status = normalizeMatchStatus(statutComboBox.getValue());
        boolean locked = isScoreLockedStatus(status);
        scoreDomicileField.setDisable(locked);
        scoreExterieurField.setDisable(locked);
        if (locked && clearWhenProgramme) {
            scoreDomicileField.clear();
            scoreExterieurField.clear();
        }
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

    private boolean isScoreLockedStatus(String status) {
        String normalizedStatus = normalizeMatchStatus(status);
        return normalizedStatus == null
                || STATUS_PROGRAMME.equals(normalizedStatus)
                || STATUS_REPORTE.equals(normalizedStatus)
                || STATUS_ANNULE.equals(normalizedStatus);
    }

    private boolean requiresFinalScores(String status) {
        return STATUS_FINI.equals(normalizeMatchStatus(status));
    }

    private StackPane createLogoPane(String imagePath, String teamName, double size, String shellStyle, String fallbackStyle) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("match-logo-image");

        Label fallbackLabel = new Label(buildInitials(teamName, "SI"));
        fallbackLabel.getStyleClass().add(fallbackStyle);

        Image image = loadImage(imagePath);
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setVisible(hasImage);
        imageView.setManaged(hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setManaged(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(size, size);
        logoPane.setPrefSize(size, size);
        logoPane.setMaxSize(size, size);
        logoPane.getStyleClass().add(shellStyle);
        return logoPane;
    }

    private String buildInitials(String teamName, String fallback) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return fallback;
        }

        String[] parts = normalizedName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }

        return initials.isEmpty() ? fallback : initials.toString();
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

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
        Optional<Image> cachedImage = imageCache.get(normalizedPath);
        if (cachedImage != null) {
            return cachedImage.orElse(null);
        }

        Image resolvedImage = resolveImage(normalizedPath);
        imageCache.put(normalizedPath, Optional.ofNullable(resolvedImage));
        return resolvedImage;
    }

    private Image resolveImage(String normalizedPath) {
        Image image = loadImageFromUri(normalizedPath);
        if (image != null) {
            return image;
        }

        Path directPath = toPathIfValid(normalizedPath);
        if (directPath != null && directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        URL resource = resolveResource(normalizedPath);
        if (resource != null) {
            return createImage(resource.toExternalForm());
        }

        if (directPath != null && !directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        for (Path candidate : buildRelativeCandidates(normalizedPath)) {
            image = loadImageFromFile(candidate);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }

    private static final class RefreshPayload {
        private final List<Equipe> loadedEquipes;
        private final List<Matchs> loadedMatchs;
        private final boolean equipesReloaded;

        private RefreshPayload(List<Equipe> loadedEquipes, List<Matchs> loadedMatchs, boolean equipesReloaded) {
            this.loadedEquipes = loadedEquipes;
            this.loadedMatchs = loadedMatchs;
            this.equipesReloaded = equipesReloaded;
        }
    }

    private void runMutation(
            SqlRunnable mutation,
            Integer preferredSelectionId,
            boolean clearFormOnSuccess,
            boolean reloadEquipesAfter,
            String successMessage,
            String errorTitle,
            String errorStatusMessage,
            String errorDialogPrefix
    ) {
        if (matchsService == null) {
            return;
        }

        mutatingData = true;
        updateActionAvailability();
        showMutedStatus("Operation en cours...");

        Task<Void> mutationTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                mutation.run();
                return null;
            }
        };

        mutationTask.setOnSucceeded(event -> {
            mutatingData = false;
            updateActionAvailability();
            if (clearFormOnSuccess) {
                clearForm();
            }
            refreshDataAsync(preferredSelectionId, reloadEquipesAfter, null, "status-success", successMessage);
        });

        mutationTask.setOnFailed(event -> {
            mutatingData = false;
            updateActionAvailability();
            showErrorStatus(errorStatusMessage);
            Throwable throwable = mutationTask.getException();
            String details = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showAlert(Alert.AlertType.ERROR, errorTitle, errorDialogPrefix + "\n" + details);
        });

        DB_EXECUTOR.execute(mutationTask);
    }

    private URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/equipes/" + imagePath,
                "/uploads/equipes/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = MatchController.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit", "images"), imagePath);
        appendCandidate(candidates, SYMFONY_UPLOADS_DIRECTORY, imagePath);
        return candidates;
    }

    private void appendCandidate(List<Path> candidates, Path base, String imagePath) {
        Path childPath = toPathIfValid(imagePath);
        if (childPath == null || childPath.isAbsolute()) {
            return;
        }

        candidates.add(base.resolve(childPath));
    }

    private Image loadImageFromUri(String imagePath) {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
            return createImage(imagePath);
        }

        return null;
    }

    private Path toPathIfValid(String pathValue) {
        try {
            return Path.of(pathValue);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private Image loadImageFromFile(Path path) {
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        Image image = createImage(file.toURI().toString());
        if (image != null) {
            return image;
        }

        return loadImageWithImageIo(file);
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Image loadImageWithImageIo(File file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                return null;
            }
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
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

