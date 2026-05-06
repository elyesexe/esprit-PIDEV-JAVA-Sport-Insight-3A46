package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
<<<<<<< HEAD
import javafx.scene.layout.FlowPane;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
<<<<<<< HEAD
import tn.esprit.assistant.AssistantContextProvider;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
<<<<<<< HEAD
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballScorerEntry;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.services.football.FootballDataCompetitions;
import tn.esprit.services.football.FootballDataStandingsService;
import tn.esprit.services.football.LeagueStandingEntry;
import tn.esprit.services.football.LeagueStandingsSnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
<<<<<<< HEAD
import java.util.Locale;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

<<<<<<< HEAD
public class LeagueTableController implements AssistantContextProvider {
    private static final double CREST_SIZE = 26;
=======
public class LeagueTableController {
    private static final double CREST_SIZE = 42;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static final ExecutorService API_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("league-table-api-worker"));

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
    private Label clubCountChipLabel;
    @FXML
    private Label matchdayChipLabel;
    @FXML
    private Label seasonChipLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label sectionSubtitleLabel;
    @FXML
    private GridPane standingsHeaderGrid;
    @FXML
    private ListView<LeagueStandingEntry> standingsListView;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Button refreshButton;
<<<<<<< HEAD
    @FXML
    private Label scorersStatusLabel;
    @FXML
    private VBox scorersContainer;
    @FXML
    private Label scorersEmptyLabel;

    private final ObservableList<LeagueStandingEntry> standings = FXCollections.observableArrayList();
    private final AtomicLong requestSequence = new AtomicLong();
    private final AtomicLong scorersRequestSequence = new AtomicLong();

    private FootballDataStandingsService standingsService;
    private ApiFootballInsightsService apiFootballInsightsService;
    private String competitionFilterCode;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean loadingScorers;
    private SidebarModuleGroup sidebarModuleGroup;
    private LeagueStandingsSnapshot currentSnapshot;
    private List<ApiFootballScorerEntry> currentTopScorers = List.of();
=======

    private final ObservableList<LeagueStandingEntry> standings = FXCollections.observableArrayList();
    private final AtomicLong requestSequence = new AtomicLong();

    private FootballDataStandingsService standingsService;
    private String competitionFilterCode;
    private boolean serviceReady;
    private boolean loadingData;
    private SidebarModuleGroup sidebarModuleGroup;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureHeaderGrid();
        configureListView();
        updateCompetitionTexts();
        updateToolbarState();

        try {
            standingsService = new FootballDataStandingsService();
<<<<<<< HEAD
            apiFootballInsightsService = new ApiFootballInsightsService();
            serviceReady = true;
            if (competitionFilterCode != null) {
                loadStandingsAsync("Chargement du classement...");
                loadTopScorersAsync("Chargement des meilleurs buteurs...");
=======
            serviceReady = true;
            if (competitionFilterCode != null) {
                loadStandingsAsync("Chargement du classement...");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
        } catch (Exception e) {
            serviceReady = false;
            updateToolbarState();
            showStatus("status-error", "Connexion football-data.org impossible.");
            showAlert(Alert.AlertType.ERROR, "Classements", "Impossible de preparer le service de classements.\n" + e.getMessage());
        }
    }

    public void setCompetitionFilter(String competitionCode) {
        competitionFilterCode = FootballDataCompetitions.normalizeCode(competitionCode);
<<<<<<< HEAD
        currentSnapshot = null;
        currentTopScorers = List.of();
        updateCompetitionTexts();
        if (serviceReady) {
            loadStandingsAsync("Chargement du classement...");
            loadTopScorersAsync("Chargement des meilleurs buteurs...");
=======
        updateCompetitionTexts();
        if (serviceReady) {
            loadStandingsAsync("Chargement du classement...");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
        openCompetitionSelector();
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleBack() {
        openCompetitionSelector();
    }

    @FXML
    private void handleRefresh() {
        loadStandingsAsync("Actualisation du classement...");
<<<<<<< HEAD
        loadTopScorersAsync("Actualisation des meilleurs buteurs...");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.LEAGUES);
    }

    private void configureHeaderGrid() {
        standingsHeaderGrid.getColumnConstraints().setAll(buildColumnConstraints());
        addHeaderLabel("#", 0, "standings-header-pill");
        addHeaderLabel("Club", 1, "standings-header-main");
<<<<<<< HEAD
        addHeaderLabel("MP", 2, "standings-header-stat");
        addHeaderLabel("W", 3, "standings-header-stat");
        addHeaderLabel("D", 4, "standings-header-stat");
        addHeaderLabel("L", 5, "standings-header-stat");
        addHeaderLabel("G", 6, "standings-header-stat");
        addHeaderLabel("GD", 7, "standings-header-stat");
        addHeaderLabel("PTS", 8, "standings-header-points");
        addHeaderLabel("FORM", 9, "standings-header-form");
=======
        addHeaderLabel("PJ", 2, "standings-header-stat");
        addHeaderLabel("G", 3, "standings-header-stat");
        addHeaderLabel("N", 4, "standings-header-stat");
        addHeaderLabel("P", 5, "standings-header-stat");
        addHeaderLabel("Buts", 6, "standings-header-stat");
        addHeaderLabel("Diff", 7, "standings-header-stat");
        addHeaderLabel("Pts", 8, "standings-header-points");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private void addHeaderLabel(String text, int columnIndex, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        standingsHeaderGrid.add(label, columnIndex, 0);
        GridPane.setHalignment(label, columnIndex == 1 ? HPos.LEFT : HPos.CENTER);
    }

    private List<ColumnConstraints> buildColumnConstraints() {
<<<<<<< HEAD
        ColumnConstraints positionColumn = new ColumnConstraints(54, 54, 54);
        ColumnConstraints teamColumn = new ColumnConstraints();
        teamColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints playedColumn = centeredColumn(46);
        ColumnConstraints wonColumn = centeredColumn(46);
        ColumnConstraints drawColumn = centeredColumn(46);
        ColumnConstraints lostColumn = centeredColumn(46);
        ColumnConstraints goalsColumn = centeredColumn(76);
        ColumnConstraints diffColumn = centeredColumn(58);
        ColumnConstraints pointsColumn = centeredColumn(58);
        ColumnConstraints formColumn = new ColumnConstraints(190, 190, 190);
        formColumn.setHalignment(HPos.RIGHT);
        return List.of(positionColumn, teamColumn, playedColumn, wonColumn, drawColumn, lostColumn, goalsColumn, diffColumn, pointsColumn, formColumn);
=======
        ColumnConstraints positionColumn = new ColumnConstraints(60, 60, 60);
        ColumnConstraints teamColumn = new ColumnConstraints();
        teamColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints playedColumn = centeredColumn(58);
        ColumnConstraints wonColumn = centeredColumn(58);
        ColumnConstraints drawColumn = centeredColumn(58);
        ColumnConstraints lostColumn = centeredColumn(58);
        ColumnConstraints goalsColumn = centeredColumn(84);
        ColumnConstraints diffColumn = centeredColumn(66);
        ColumnConstraints pointsColumn = centeredColumn(70);
        return List.of(positionColumn, teamColumn, playedColumn, wonColumn, drawColumn, lostColumn, goalsColumn, diffColumn, pointsColumn);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private ColumnConstraints centeredColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints(width, width, width);
        constraints.setHalignment(HPos.CENTER);
        return constraints;
    }

    private void configureListView() {
        standingsListView.setItems(standings);
        standingsListView.setCellFactory(listView -> createStandingCell());
    }

    private ListCell<LeagueStandingEntry> createStandingCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(LeagueStandingEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                GridPane rowGrid = new GridPane();
                rowGrid.getStyleClass().add("standings-row-grid");
                rowGrid.setHgap(12);
                rowGrid.setAlignment(Pos.CENTER_LEFT);
                rowGrid.getColumnConstraints().setAll(buildColumnConstraints());

                rowGrid.add(createPositionBadge(entry.position()), 0, 0);
                rowGrid.add(createTeamCell(entry), 1, 0);
                rowGrid.add(createStatValueLabel(String.valueOf(entry.playedGames()), false), 2, 0);
                rowGrid.add(createStatValueLabel(String.valueOf(entry.won()), false), 3, 0);
                rowGrid.add(createStatValueLabel(String.valueOf(entry.draw()), false), 4, 0);
                rowGrid.add(createStatValueLabel(String.valueOf(entry.lost()), false), 5, 0);
                rowGrid.add(createStatValueLabel(entry.goalsSummary(), false), 6, 0);
                rowGrid.add(createStatValueLabel(entry.goalDifferenceSummary(), false), 7, 0);
                rowGrid.add(createStatValueLabel(String.valueOf(entry.points()), true), 8, 0);
<<<<<<< HEAD
                rowGrid.add(createFormCell(entry), 9, 0);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

                StackPane rowShell = new StackPane(rowGrid);
                rowShell.getStyleClass().add("standings-row-shell");

                setText(null);
                setGraphic(rowShell);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private StackPane createPositionBadge(int position) {
        Label label = new Label(position + ".");
        label.getStyleClass().add("standings-position-label");

        StackPane badge = new StackPane(label);
        badge.getStyleClass().addAll("standings-position-badge", resolvePositionStyleClass(position));
<<<<<<< HEAD
        badge.setMinSize(34, 34);
        badge.setPrefSize(34, 34);
        badge.setMaxSize(34, 34);
=======
        badge.setMinSize(42, 42);
        badge.setPrefSize(42, 42);
        badge.setMaxSize(42, 42);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        return badge;
    }

    private String resolvePositionStyleClass(int position) {
        if (position <= 4) {
            return "position-zone-champions";
        }
        if (position <= 6) {
            return "position-zone-europe";
        }
        if (position >= 18) {
            return "position-zone-relegation";
        }
        return "position-zone-neutral";
    }

    private HBox createTeamCell(LeagueStandingEntry entry) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CREST_SIZE);
        imageView.setFitHeight(CREST_SIZE);
        imageView.setPreserveRatio(true);

        Image image = EquipeUiSupport.loadEquipeImage(entry.teamCrest());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(entry.displayName(), "FC"));
        fallbackLabel.getStyleClass().add("standings-team-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane crestShell = new StackPane(imageView, fallbackLabel);
        crestShell.getStyleClass().add("standings-team-crest-shell");
<<<<<<< HEAD
        crestShell.setMinSize(CREST_SIZE + 10, CREST_SIZE + 10);
        crestShell.setPrefSize(CREST_SIZE + 10, CREST_SIZE + 10);
        crestShell.setMaxSize(CREST_SIZE + 10, CREST_SIZE + 10);
=======
        crestShell.setMinSize(CREST_SIZE + 14, CREST_SIZE + 14);
        crestShell.setPrefSize(CREST_SIZE + 14, CREST_SIZE + 14);
        crestShell.setMaxSize(CREST_SIZE + 14, CREST_SIZE + 14);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        Label nameLabel = new Label(entry.displayName());
        nameLabel.getStyleClass().add("standings-team-name");
        nameLabel.setWrapText(true);

        String subline = entry.teamTla() == null || entry.teamTla().isBlank()
                ? "Club"
                : entry.teamTla();
        Label sublineLabel = new Label(subline);
        sublineLabel.getStyleClass().add("standings-team-meta");

        VBox textBox = new VBox(2, nameLabel, sublineLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox teamBox = new HBox(12, crestShell, textBox);
        teamBox.setAlignment(Pos.CENTER_LEFT);
        return teamBox;
    }

<<<<<<< HEAD
    private FlowPane createFormCell(LeagueStandingEntry entry) {
        FlowPane formPane = new FlowPane();
        formPane.getStyleClass().add("standings-form-pane");
        formPane.setAlignment(Pos.CENTER_RIGHT);
        formPane.setHgap(6);
        formPane.setVgap(4);
        formPane.setPrefWrapLength(182);

        List<String> recentForm = entry.form();
        if (recentForm == null || recentForm.isEmpty()) {
            Label placeholder = new Label("-");
            placeholder.getStyleClass().addAll("standings-form-chip", "form-empty");
            formPane.getChildren().add(placeholder);
            return formPane;
        }

        int startIndex = Math.max(0, recentForm.size() - 5);
        for (String result : recentForm.subList(startIndex, recentForm.size())) {
            Label chip = new Label(resolveFormLabel(result));
            chip.getStyleClass().addAll("standings-form-chip", resolveFormStyleClass(result));
            formPane.getChildren().add(chip);
        }
        return formPane;
    }

    private String resolveFormLabel(String result) {
        String normalized = normalizeForm(result);
        return switch (normalized) {
            case "W" -> "W";
            case "D" -> "D";
            case "L" -> "L";
            default -> "?";
        };
    }

    private String resolveFormStyleClass(String result) {
        String normalized = normalizeForm(result);
        return switch (normalized) {
            case "W" -> "form-win";
            case "D" -> "form-draw";
            case "L" -> "form-loss";
            default -> "form-empty";
        };
    }

    private String normalizeForm(String result) {
        if (result == null || result.isBlank()) {
            return "";
        }
        return result.trim().toUpperCase(Locale.ROOT);
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private Label createStatValueLabel(String value, boolean highlighted) {
        Label label = new Label(value);
        label.getStyleClass().add(highlighted ? "standings-points-value" : "standings-stat-value");
        return label;
    }

    private void loadStandingsAsync(String loadingMessage) {
        if (!serviceReady || competitionFilterCode == null) {
            return;
        }

        long requestId = requestSequence.incrementAndGet();
        loadingData = true;
        updateToolbarState();
        showStatus("status-muted", loadingMessage);

        Task<LeagueStandingsSnapshot> loadTask = new Task<>() {
            @Override
            protected LeagueStandingsSnapshot call() throws Exception {
                return standingsService.fetchStandings(competitionFilterCode);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != requestSequence.get()) {
                return;
            }

            loadingData = false;
            updateToolbarState();

            LeagueStandingsSnapshot snapshot = loadTask.getValue();
<<<<<<< HEAD
            currentSnapshot = snapshot;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            standings.setAll(snapshot == null ? List.of() : snapshot.table());
            updateCompetitionTexts(snapshot);
            updateEmptyState();
            showStatus("status-success", "Classement actualise.");
        });

        loadTask.setOnFailed(event -> {
            if (requestId != requestSequence.get()) {
                return;
            }

            loadingData = false;
            updateToolbarState();
<<<<<<< HEAD
            currentSnapshot = null;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            updateEmptyState();
            Throwable throwable = loadTask.getException();
            showStatus("status-error", "Erreur lors du chargement du classement.");
            showAlert(Alert.AlertType.ERROR, "Classements",
                    "Impossible de charger le classement.\n" + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        API_EXECUTOR.execute(loadTask);
    }

<<<<<<< HEAD
    private void loadTopScorersAsync(String loadingMessage) {
        if (!serviceReady || competitionFilterCode == null || apiFootballInsightsService == null) {
            return;
        }

        long requestId = scorersRequestSequence.incrementAndGet();
        loadingScorers = true;
        updateToolbarState();
        showScorersStatus("status-muted", loadingMessage);

        Task<List<ApiFootballScorerEntry>> loadTask = new Task<>() {
            @Override
            protected List<ApiFootballScorerEntry> call() throws Exception {
                return apiFootballInsightsService.loadCompetitionTopScorers(competitionFilterCode);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != scorersRequestSequence.get()) {
                return;
            }

            loadingScorers = false;
            updateToolbarState();
            currentTopScorers = loadTask.getValue() == null ? List.of() : List.copyOf(loadTask.getValue());
            renderTopScorers(currentTopScorers);
            showScorersStatus("status-success", "Meilleurs buteurs actualises.");
        });

        loadTask.setOnFailed(event -> {
            if (requestId != scorersRequestSequence.get()) {
                return;
            }

            loadingScorers = false;
            updateToolbarState();
            currentTopScorers = List.of();
            renderTopScorers(List.of());
            Throwable throwable = loadTask.getException();
            showScorersStatus("status-warning", shortError(throwable));
        });

        API_EXECUTOR.execute(loadTask);
    }

    private void renderTopScorers(List<ApiFootballScorerEntry> scorers) {
        currentTopScorers = scorers == null ? List.of() : List.copyOf(scorers);
        scorersContainer.getChildren().clear();
        boolean hasScorers = scorers != null && !scorers.isEmpty();
        scorersEmptyLabel.setManaged(!hasScorers);
        scorersEmptyLabel.setVisible(!hasScorers);
        if (!hasScorers) {
            scorersEmptyLabel.setText("Aucun classement des buteurs disponible pour cette competition.");
            return;
        }

        for (ApiFootballScorerEntry scorer : scorers) {
            Label rankLabel = new Label(scorer.rank() + ".");
            rankLabel.getStyleClass().add("top-scorer-rank");

            Label playerLabel = new Label(defaultIfBlank(scorer.playerName(), "Joueur"));
            playerLabel.getStyleClass().add("top-scorer-name");
            playerLabel.setWrapText(true);

            Label teamLabel = new Label(defaultIfBlank(scorer.teamName(), "Equipe"));
            teamLabel.getStyleClass().add("top-scorer-team");

            VBox textBox = new VBox(2, playerLabel, teamLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label goalsLabel = new Label(buildScorerMeta("Buts", scorer.goals()));
            goalsLabel.getStyleClass().add("top-scorer-pill");

            Label assistsLabel = new Label(buildScorerMeta("Assists", scorer.assists()));
            assistsLabel.getStyleClass().add("top-scorer-pill");

            Label appearancesLabel = new Label(buildScorerMeta("Matchs", scorer.appearances()));
            appearancesLabel.getStyleClass().add("top-scorer-pill");

            HBox metaBox = new HBox(8, goalsLabel, assistsLabel, appearancesLabel);
            metaBox.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(14, rankLabel, textBox, metaBox);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("top-scorer-row");
            scorersContainer.getChildren().add(row);
        }
    }

    private String buildScorerMeta(String label, Integer value) {
        return label + " " + (value == null ? "-" : value);
    }

    private void showScorersStatus(String styleClass, String message) {
        scorersStatusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!scorersStatusLabel.getStyleClass().contains(styleClass)) {
            scorersStatusLabel.getStyleClass().add(styleClass);
        }
        scorersStatusLabel.setText(message);
    }

    private String shortError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Classement des buteurs indisponible.";
        }

        String message = throwable.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() <= 140 ? message : message.substring(0, 140) + "...";
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private void updateCompetitionTexts() {
        updateCompetitionTexts(null);
    }

    private void updateCompetitionTexts(LeagueStandingsSnapshot snapshot) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionFilterCode);
        competitionChipLabel.setText(competitionLabel == null ? "League" : competitionLabel);
        pageTitleLabel.setText(competitionLabel == null ? "League Table" : competitionLabel);
        pageSubtitleLabel.setText(snapshot == null
                ? "Classement officiel en direct charge depuis football-data.org."
                : buildSubtitle(snapshot));
        clubCountChipLabel.setText((snapshot == null ? standings.size() : snapshot.clubCount()) + " clubs");
        matchdayChipLabel.setText(snapshot == null || snapshot.currentMatchday() == null
                ? "Journée --"
                : "Journée " + snapshot.currentMatchday());
        seasonChipLabel.setText(snapshot == null ? "Saison --" : buildSeasonLabel(snapshot));
        sectionSubtitleLabel.setText(snapshot == null
<<<<<<< HEAD
                ? "Vue generale du championnat avec bilan, difference, points et forme recente."
                : "Source : football-data.org | " + defaultIfBlank(snapshot.stage(), "Classement total"));
    }

    @Override
    public String assistantContextSummary() {
        StringBuilder summary = new StringBuilder()
                .append("Current league table screen.\n")
                .append("Competition: ").append(defaultIfBlank(pageTitleLabel == null ? null : pageTitleLabel.getText(), "League Table")).append(".\n")
                .append("Subtitle: ").append(defaultIfBlank(pageSubtitleLabel == null ? null : pageSubtitleLabel.getText(), "No subtitle")).append(".\n")
                .append("Chips: ").append(defaultIfBlank(clubCountChipLabel == null ? null : clubCountChipLabel.getText(), "Unknown"))
                .append(", ").append(defaultIfBlank(matchdayChipLabel == null ? null : matchdayChipLabel.getText(), "Unknown"))
                .append(", ").append(defaultIfBlank(seasonChipLabel == null ? null : seasonChipLabel.getText(), "Unknown")).append(".\n")
                .append("Status: ").append(defaultIfBlank(statusLabel == null ? null : statusLabel.getText(), "Unknown")).append(".\n");

        List<LeagueStandingEntry> table = currentSnapshot != null && currentSnapshot.table() != null
                ? currentSnapshot.table()
                : List.copyOf(standings);
        if (!table.isEmpty()) {
            List<String> leaders = table.stream()
                    .limit(5)
                    .map(entry -> entry.position() + ". " + defaultIfBlank(entry.displayName(), "Team")
                            + " - " + entry.points() + " pts, goals " + entry.goalsSummary()
                            + ", GD " + entry.goalDifferenceSummary())
                    .toList();
            summary.append("Top 5: ").append(String.join(" | ", leaders)).append(".\n");
        }

        if (!currentTopScorers.isEmpty()) {
            List<String> scorers = currentTopScorers.stream()
                    .limit(3)
                    .map(entry -> entry.rank() + ". "
                            + defaultIfBlank(entry.playerName(), "Player")
                            + " (" + defaultIfBlank(entry.teamName(), "Team") + ") - "
                            + (entry.goals() == null ? "-" : entry.goals()) + " goals")
                    .toList();
            summary.append("Top scorers: ").append(String.join(" | ", scorers)).append(".\n");
        }

        if (currentSnapshot != null) {
            summary.append("Stage: ").append(defaultIfBlank(currentSnapshot.stage(), "Overall"))
                    .append(". Area: ").append(defaultIfBlank(currentSnapshot.areaName(), "Unknown"))
                    .append(". Matchday: ").append(currentSnapshot.currentMatchday() == null ? "-" : currentSnapshot.currentMatchday()).append('.');
        }
        return summary.toString();
    }

=======
                ? "Position, matchs joues, bilan, buts, difference et points."
                : "Source : football-data.org | " + defaultIfBlank(snapshot.stage(), "Classement total"));
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private String buildSubtitle(LeagueStandingsSnapshot snapshot) {
        String areaName = defaultIfBlank(snapshot.areaName(), "Europe");
        String stage = defaultIfBlank(snapshot.stage(), "Classement total");
        return areaName + " | " + stage + " | Donnees officielles football-data.org";
    }

    private String buildSeasonLabel(LeagueStandingsSnapshot snapshot) {
        Integer startYear = extractYear(snapshot.seasonStartDate());
        Integer endYear = extractYear(snapshot.seasonEndDate());
        if (startYear == null && endYear == null) {
            return "Saison --";
        }
        if (startYear != null && endYear != null && !startYear.equals(endYear)) {
            return "Saison " + startYear + "-" + endYear;
        }
        int seasonYear = startYear != null ? startYear : endYear;
        return "Saison " + seasonYear;
    }

    private Integer extractYear(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE).getYear();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void updateEmptyState() {
        boolean isEmpty = standings.isEmpty();
        emptyStateBox.setManaged(isEmpty);
        emptyStateBox.setVisible(isEmpty);
    }

    private void updateToolbarState() {
<<<<<<< HEAD
        refreshButton.setDisable(!serviceReady || loadingData || loadingScorers || competitionFilterCode == null);
=======
        refreshButton.setDisable(!serviceReady || loadingData || competitionFilterCode == null);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private void openCompetitionSelector() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
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

