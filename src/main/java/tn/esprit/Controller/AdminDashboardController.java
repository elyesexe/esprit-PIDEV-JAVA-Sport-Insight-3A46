package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.UserRoles;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.UserService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class AdminDashboardController {
    private static final String DARK_TABLE_CLASS = "admin-dashboard-force-dark";
    private static final String TRANSPARENT_SURFACE_STYLE =
            "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;";
    private static final String DARK_DASHBOARD_SURFACE_STYLE =
            "-fx-background-color: "
                    + "radial-gradient(center 14% 10%, radius 46%, rgba(221, 110, 255, 0.30) 0%, rgba(221, 110, 255, 0.10) 48%, transparent 49%), "
                    + "radial-gradient(center 88% 14%, radius 34%, rgba(87, 213, 255, 0.18) 0%, rgba(87, 213, 255, 0.05) 46%, transparent 47%), "
                    + "linear-gradient(from 0% 0% to 100% 100%, #1a1246 0%, #24175b 48%, #2c1d70 100%); "
                    + "-fx-background-insets: 0; "
                    + "-fx-background-radius: 0; "
                    + "-fx-border-color: transparent; "
                    + "-fx-padding: 0;";
    private static final String LIGHT_DASHBOARD_SURFACE_STYLE =
            "-fx-background-color:"
                    + " linear-gradient(from 0% 0% to 100% 100%, #f8fbff 0%, #eef2ff 52%, #f6f8ff 100%);"
                    + " -fx-padding: 0;";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> LIGHT_TEAM_RATE_COLORS = List.of("#16a34a", "#f59e0b", "#dc2626");
    private static final List<String> DARK_TEAM_RATE_COLORS = List.of("#38d9ff", "#9d71ff", "#ff63d0");
    private static final List<String> LIGHT_PLAYER_DISTRIBUTION_COLORS =
            List.of("#38bdf8", "#34d399", "#f59e0b", "#f97316", "#a78bfa", "#f43f5e");
    private static final List<String> DARK_PLAYER_DISTRIBUTION_COLORS =
            List.of("#36d7ff", "#5f8bff", "#b667ff", "#ff63d0", "#ffc14d", "#45e6c3");
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("admin-dashboard-db"));

    @FXML
    private Label userCountLabel;
    @FXML
    private Label annonceCountLabel;
    @FXML
    private Label entrainementCountLabel;
    @FXML
    private Label equipeCountLabel;
    @FXML
    private Label joueurCountLabel;
    @FXML
    private Label matchCountLabel;
    @FXML
    private Label dashboardStatusLabel;
    @FXML
    private ComboBox<TeamOption> teamStatsComboBox;
    @FXML
    private Label teamStatsSummaryLabel;
    @FXML
    private Label playerChartSummaryLabel;
    @FXML
    private Label matchChartSummaryLabel;
    @FXML
    private ScrollPane dashboardScroll;
    @FXML
    private StackPane dashboardWrap;
    @FXML
    private BarChart<String, Number> teamRateChart;
    @FXML
    private BarChart<String, Number> playerDistributionChart;
    @FXML
    private PieChart matchStatusChart;

    @FXML
    private TableView<UserRow> usersTableView;
    @FXML
    private TableColumn<UserRow, Integer> userIdColumn;
    @FXML
    private TableColumn<UserRow, String> userNameColumn;
    @FXML
    private TableColumn<UserRow, String> userRoleColumn;
    @FXML
    private TableColumn<UserRow, String> userStatusColumn;

    @FXML
    private TableView<AnnonceRow> annoncesTableView;
    @FXML
    private TableColumn<AnnonceRow, Integer> annonceIdColumn;
    @FXML
    private TableColumn<AnnonceRow, String> annonceTitleColumn;
    @FXML
    private TableColumn<AnnonceRow, String> annonceLevelColumn;
    @FXML
    private TableColumn<AnnonceRow, String> annonceStatusColumn;

    @FXML
    private TableView<EntrainementRow> entrainementsTableView;
    @FXML
    private TableColumn<EntrainementRow, Integer> entrainementIdColumn;
    @FXML
    private TableColumn<EntrainementRow, String> entrainementDateColumn;
    @FXML
    private TableColumn<EntrainementRow, String> entrainementTypeColumn;
    @FXML
    private TableColumn<EntrainementRow, String> entrainementPlaceColumn;
    @FXML
    private TableColumn<EntrainementRow, String> entrainementTimeColumn;

    @FXML
    private TableView<OperationRow> operationsTableView;
    @FXML
    private TableColumn<OperationRow, String> operationModuleColumn;
    @FXML
    private TableColumn<OperationRow, String> operationPrimaryColumn;
    @FXML
    private TableColumn<OperationRow, String> operationSecondaryColumn;
    @FXML
    private TableColumn<OperationRow, String> operationMetaColumn;

    private List<Joueur> dashboardJoueurs = List.of();
    private List<Matchs> dashboardMatchs = List.of();
    private boolean darkMode = ThemeManager.isDarkMode();

    @FXML
    public void initialize() {
        configureTables();
        configureCharts();
        applyWorkspaceSurface();
        if (dashboardScroll != null) {
            dashboardScroll.skinProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::applyWorkspaceSurface));
            dashboardScroll.sceneProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::applyWorkspaceSurface));
        }
        Platform.runLater(this::applyWorkspaceSurface);
        setLoadingState();
        loadDashboardAsync();
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        toggleDarkClass(usersTableView, darkMode);
        toggleDarkClass(annoncesTableView, darkMode);
        toggleDarkClass(entrainementsTableView, darkMode);
        toggleDarkClass(operationsTableView, darkMode);
        applyWorkspaceSurface();
        Platform.runLater(this::applyWorkspaceSurface);
        refreshActiveChartColors();
    }

    private void configureTables() {
        userIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        userNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        userRoleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().role()));
        userStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));

        annonceIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        annonceTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        annonceLevelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().level()));
        annonceStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));

        entrainementIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        entrainementDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().date()));
        entrainementTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type()));
        entrainementPlaceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().place()));
        entrainementTimeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().time()));

        operationModuleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().module()));
        operationPrimaryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().primary()));
        operationSecondaryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().secondary()));
        operationMetaColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().meta()));

        usersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        annoncesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        entrainementsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        operationsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configureCharts() {
        if (teamRateChart != null) {
            teamRateChart.setTitle("Taux de resultat (%)");
            teamRateChart.setAnimated(false);
        }
        if (playerDistributionChart != null) {
            playerDistributionChart.setTitle("Top equipes par effectif");
            playerDistributionChart.setAnimated(false);
        }
        if (matchStatusChart != null) {
            matchStatusChart.setTitle("Repartition des statuts");
            matchStatusChart.setAnimated(false);
        }
        if (teamStatsComboBox != null) {
            teamStatsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateTeamRateChart(newValue));
        }
    }

    private void loadDashboardAsync() {
        Task<DashboardPayload> task = new Task<>() {
            @Override
            protected DashboardPayload call() throws Exception {
                UserService userService = new UserService();
                AnnonceService annonceService = new AnnonceService();
                EntrainementService entrainementService = new EntrainementService();
                EquipeService equipeService = new EquipeService();
                JoueurService joueurService = new JoueurService();
                MatchsService matchsService = new MatchsService();

                List<User> users = new ArrayList<>(userService.getAll());
                users.sort(Comparator.comparing(User::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

                List<Annonce> annonces = new ArrayList<>(annonceService.getAll());
                annonces.sort(Comparator.comparing(Annonce::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

                List<Entrainement> entrainements = new ArrayList<>(entrainementService.getAll());
                entrainements.sort(Comparator
                        .comparing(Entrainement::getDateEntrainement, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Entrainement::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                        .reversed());

                List<Equipe> equipes = new ArrayList<>(equipeService.getAll());
                equipes.sort(Comparator.comparing(Equipe::getId, Comparator.nullsLast(Integer::compareTo)).reversed());
                Map<Integer, String> equipeNames = equipes.stream()
                        .filter(equipe -> equipe.getId() != null)
                        .collect(Collectors.toMap(
                                Equipe::getId,
                                equipe -> emptyIfNull(equipe.getNom(), "Equipe inconnue"),
                                (left, right) -> left
                        ));

                List<Joueur> joueurs = new ArrayList<>(joueurService.getAll());
                joueurs.sort(Comparator.comparing(Joueur::getId, Comparator.nullsLast(Integer::compareTo)).reversed());

                List<Matchs> matchs = new ArrayList<>(matchsService.getAll());
                matchs.sort(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                        .reversed());

                List<UserRow> userRows = users.stream()
                        .limit(6)
                        .map(user -> new UserRow(
                                user.getId(),
                                user.getDisplayName(),
                                UserRoles.displayName(user.getPrimaryRole()),
                                emptyIfNull(user.getStatut(), "ACTIVE")
                        ))
                        .toList();

                List<AnnonceRow> annonceRows = annonces.stream()
                        .limit(6)
                        .map(annonce -> new AnnonceRow(
                                annonce.getId(),
                                emptyIfNull(annonce.getTitre(), "Annonce"),
                                emptyIfNull(annonce.getNiveauRequis(), "-"),
                                emptyIfNull(annonce.getStatut(), "ACTIVE")
                        ))
                        .toList();

                List<EntrainementRow> entrainementRows = entrainements.stream()
                        .limit(6)
                        .map(entrainement -> new EntrainementRow(
                                entrainement.getId(),
                                formatDate(entrainement.getDateEntrainement()),
                                emptyIfNull(entrainement.getType(), "-"),
                                emptyIfNull(entrainement.getLieu(), "-"),
                                buildTrainingTime(entrainement)
                        ))
                        .toList();

                List<OperationRow> operationRows = new ArrayList<>();
                equipes.stream().limit(3).forEach(equipe -> operationRows.add(new OperationRow(
                        "Equipe",
                        emptyIfNull(equipe.getNom(), "Equipe"),
                        emptyIfNull(equipe.getCoach(), "Coach non renseigne"),
                        resolveCompetition(equipe)
                )));
                joueurs.stream().limit(3).forEach(joueur -> operationRows.add(new OperationRow(
                        "Joueur",
                        buildPlayerName(joueur),
                        equipeNames.getOrDefault(joueur.getEquipeId(), "Sans equipe"),
                        joueur.getNumero() > 0 ? "#" + joueur.getNumero() : "-"
                )));
                matchs.stream().limit(2).forEach(match -> operationRows.add(new OperationRow(
                        "Match",
                        buildMatchLabel(match, equipeNames),
                        buildMatchDate(match),
                        emptyIfNull(match.getStatut(), "Programme")
                )));

                return new DashboardPayload(
                        users.size(),
                        annonces.size(),
                        entrainements.size(),
                        equipes.size(),
                        joueurs.size(),
                        matchs.size(),
                        equipes,
                        joueurs,
                        matchs,
                        equipeNames,
                        userRows,
                        annonceRows,
                        entrainementRows,
                        operationRows
                );
            }
        };

        task.setOnSucceeded(event -> {
            DashboardPayload payload = task.getValue();
            userCountLabel.setText(String.valueOf(payload.userCount()));
            annonceCountLabel.setText(String.valueOf(payload.annonceCount()));
            entrainementCountLabel.setText(String.valueOf(payload.entrainementCount()));
            equipeCountLabel.setText(String.valueOf(payload.equipeCount()));
            joueurCountLabel.setText(String.valueOf(payload.joueurCount()));
            matchCountLabel.setText(String.valueOf(payload.matchCount()));
            usersTableView.setItems(FXCollections.observableArrayList(payload.latestUsers()));
            annoncesTableView.setItems(FXCollections.observableArrayList(payload.latestAnnonces()));
            entrainementsTableView.setItems(FXCollections.observableArrayList(payload.latestEntrainements()));
            operationsTableView.setItems(FXCollections.observableArrayList(payload.operations()));
            applyStatistics(payload);
            setStatus("Vue generale chargee.", "status-success");
        });

        task.setOnFailed(event -> {
            userCountLabel.setText("-");
            annonceCountLabel.setText("-");
            entrainementCountLabel.setText("-");
            equipeCountLabel.setText("-");
            joueurCountLabel.setText("-");
            matchCountLabel.setText("-");
            usersTableView.setItems(FXCollections.observableArrayList());
            annoncesTableView.setItems(FXCollections.observableArrayList());
            entrainementsTableView.setItems(FXCollections.observableArrayList());
            operationsTableView.setItems(FXCollections.observableArrayList());
            resetStatisticsCharts();
            setStatus("Chargement impossible.", "status-error");

            Throwable exception = task.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
        });

        DB_EXECUTOR.execute(task);
    }

    private void setLoadingState() {
        userCountLabel.setText("...");
        annonceCountLabel.setText("...");
        entrainementCountLabel.setText("...");
        equipeCountLabel.setText("...");
        joueurCountLabel.setText("...");
        matchCountLabel.setText("...");
        usersTableView.setItems(FXCollections.observableArrayList());
        annoncesTableView.setItems(FXCollections.observableArrayList());
        entrainementsTableView.setItems(FXCollections.observableArrayList());
        operationsTableView.setItems(FXCollections.observableArrayList());
        resetStatisticsCharts();
        setStatus("Chargement...", "status-muted");
    }

    private void applyStatistics(DashboardPayload payload) {
        if (teamStatsComboBox == null && playerDistributionChart == null && matchStatusChart == null) {
            return;
        }
        dashboardJoueurs = List.copyOf(payload.joueurs());
        dashboardMatchs = List.copyOf(payload.matchs());

        if (teamStatsComboBox != null) {
            List<TeamOption> options = payload.equipes().stream()
                    .map(equipe -> new TeamOption(
                            equipe.getId(),
                            emptyIfNull(equipe.getNom(), "Equipe"),
                            resolveCompetition(equipe)
                    ))
                    .toList();
            teamStatsComboBox.setItems(FXCollections.observableArrayList(options));
            if (!options.isEmpty()) {
                TeamOption currentSelection = teamStatsComboBox.getValue();
                TeamOption nextSelection = options.stream()
                        .filter(option -> currentSelection != null && java.util.Objects.equals(option.id(), currentSelection.id()))
                        .findFirst()
                        .orElse(options.get(0));
                teamStatsComboBox.getSelectionModel().select(nextSelection);
                updateTeamRateChart(nextSelection);
            } else {
                teamStatsComboBox.getSelectionModel().clearSelection();
                updateTeamRateChart(null);
            }
        }

        updatePlayerDistributionChart(payload.joueurs(), payload.equipeNames());
        updateMatchStatusChart(payload.matchs());
    }

    private void resetStatisticsCharts() {
        dashboardJoueurs = List.of();
        dashboardMatchs = List.of();

        if (teamStatsComboBox != null) {
            teamStatsComboBox.setItems(FXCollections.observableArrayList());
            teamStatsComboBox.getSelectionModel().clearSelection();
        }
        if (teamRateChart != null) {
            teamRateChart.getData().clear();
        }
        if (playerDistributionChart != null) {
            playerDistributionChart.getData().clear();
        }
        if (matchStatusChart != null) {
            matchStatusChart.setData(FXCollections.observableArrayList());
        }
        if (teamStatsSummaryLabel != null) {
            teamStatsSummaryLabel.setText("Selectionnez une equipe pour voir ses statistiques.");
        }
        if (playerChartSummaryLabel != null) {
            playerChartSummaryLabel.setText("Aucune statistique joueur disponible.");
        }
        if (matchChartSummaryLabel != null) {
            matchChartSummaryLabel.setText("Aucune statistique match disponible.");
        }
    }

    private void updateTeamRateChart(TeamOption option) {
        if (teamRateChart == null) {
            return;
        }

        teamRateChart.getData().clear();
        if (option == null || option.id() == null) {
            teamStatsSummaryLabel.setText("Selectionnez une equipe pour voir ses statistiques.");
            return;
        }

        List<Matchs> teamMatches = dashboardMatchs.stream()
                .filter(match -> option.id().equals(match.getEquipeDomicileId()) || option.id().equals(match.getEquipeExterieurId()))
                .toList();
        List<Matchs> completedMatches = teamMatches.stream()
                .filter(this::hasFinalScore)
                .toList();

        long wins = completedMatches.stream()
                .filter(match -> resolveResult(match, option.id()) == MatchResult.WIN)
                .count();
        long draws = completedMatches.stream()
                .filter(match -> resolveResult(match, option.id()) == MatchResult.DRAW)
                .count();
        long losses = completedMatches.stream()
                .filter(match -> resolveResult(match, option.id()) == MatchResult.LOSS)
                .count();

        int playerCount = (int) dashboardJoueurs.stream()
                .filter(joueur -> option.id().equals(joueur.getEquipeId()))
                .count();
        int goalsScored = completedMatches.stream()
                .mapToInt(match -> goalsFor(match, option.id()))
                .sum();
        int goalsConceded = completedMatches.stream()
                .mapToInt(match -> goalsAgainst(match, option.id()))
                .sum();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (completedMatches.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Victoires", 0));
            series.getData().add(new XYChart.Data<>("Nuls", 0));
            series.getData().add(new XYChart.Data<>("Defaites", 0));
            teamStatsSummaryLabel.setText(option.label() + " n'a pas encore de match avec score final. "
                    + playerCount + " joueurs relies a cette equipe.");
        } else {
            double total = completedMatches.size();
            series.getData().add(new XYChart.Data<>("Victoires", roundRate(wins, total)));
            series.getData().add(new XYChart.Data<>("Nuls", roundRate(draws, total)));
            series.getData().add(new XYChart.Data<>("Defaites", roundRate(losses, total)));
            long pendingMatches = teamMatches.size() - completedMatches.size();
            teamStatsSummaryLabel.setText(option.label() + " | " + playerCount + " joueurs | "
                    + completedMatches.size() + " matchs joues | "
                    + goalsScored + " buts marques / " + goalsConceded + " encaisses"
                    + (pendingMatches > 0 ? " | " + pendingMatches + " matchs en attente" : ""));
        }
        teamRateChart.getData().add(series);
        applyBarColors(series, darkMode ? DARK_TEAM_RATE_COLORS : LIGHT_TEAM_RATE_COLORS);
    }

    private void updatePlayerDistributionChart(List<Joueur> joueurs, Map<Integer, String> equipeNames) {
        if (playerDistributionChart == null) {
            return;
        }

        playerDistributionChart.getData().clear();
        if (joueurs == null || joueurs.isEmpty()) {
            playerChartSummaryLabel.setText("Aucune statistique joueur disponible.");
            return;
        }

        Map<String, Long> counts = joueurs.stream()
                .collect(Collectors.groupingBy(
                        joueur -> equipeNames.getOrDefault(joueur.getEquipeId(), "Sans equipe"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(6)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        playerDistributionChart.getData().add(series);
        applyBarColors(series, darkMode ? DARK_PLAYER_DISTRIBUTION_COLORS : LIGHT_PLAYER_DISTRIBUTION_COLORS);

        long unassignedCount = joueurs.stream().filter(joueur -> joueur.getEquipeId() == null).count();
        String averageAge = buildAverageAge(joueurs);
        playerChartSummaryLabel.setText(joueurs.size() + " joueurs au total | age moyen " + averageAge
                + " | " + unassignedCount + " sans equipe");
    }

    private void updateMatchStatusChart(List<Matchs> matchs) {
        if (matchStatusChart == null) {
            return;
        }

        if (matchs == null || matchs.isEmpty()) {
            matchStatusChart.setData(FXCollections.observableArrayList());
            matchChartSummaryLabel.setText("Aucune statistique match disponible.");
            return;
        }

        Map<String, Long> statusCounts = matchs.stream()
                .collect(Collectors.groupingBy(
                        match -> emptyIfNull(normalizeStatus(match.getStatut()), "Non renseigne"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<PieChart.Data> data = statusCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()))
                .toList();
        matchStatusChart.setData(FXCollections.observableArrayList(data));

        long finishedCount = matchs.stream().filter(this::hasFinalScore).count();
        long scheduledCount = matchs.size() - finishedCount;
        matchChartSummaryLabel.setText(matchs.size() + " matchs au total | "
                + finishedCount + " avec score final | "
                + scheduledCount + " a suivre");
    }

    private void toggleDarkClass(TableView<?> tableView, boolean darkMode) {
        if (tableView == null) {
            return;
        }
        if (darkMode) {
            if (!tableView.getStyleClass().contains(DARK_TABLE_CLASS)) {
                tableView.getStyleClass().add(DARK_TABLE_CLASS);
            }
            return;
        }
        tableView.getStyleClass().remove(DARK_TABLE_CLASS);
    }

    private void refreshActiveChartColors() {
        if (teamRateChart != null && !teamRateChart.getData().isEmpty()) {
            repaintBarColors(teamRateChart.getData().get(0), darkMode ? DARK_TEAM_RATE_COLORS : LIGHT_TEAM_RATE_COLORS);
        }
        if (playerDistributionChart != null && !playerDistributionChart.getData().isEmpty()) {
            repaintBarColors(
                    playerDistributionChart.getData().get(0),
                    darkMode ? DARK_PLAYER_DISTRIBUTION_COLORS : LIGHT_PLAYER_DISTRIBUTION_COLORS
            );
        }
    }

    private void applyWorkspaceSurface() {
        if (dashboardScroll != null) {
            dashboardScroll.setStyle(TRANSPARENT_SURFACE_STYLE);
            forceTransparent(dashboardScroll);
            forceTransparent(dashboardScroll.lookup(".viewport"));
            forceTransparent(dashboardScroll.lookup(".content"));
        }
        if (dashboardWrap != null) {
            dashboardWrap.setStyle(darkMode ? DARK_DASHBOARD_SURFACE_STYLE : LIGHT_DASHBOARD_SURFACE_STYLE);
        }
    }

    private void setStatus(String message, String styleClass) {
        dashboardStatusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!dashboardStatusLabel.getStyleClass().contains(styleClass)) {
            dashboardStatusLabel.getStyleClass().add(styleClass);
        }
        dashboardStatusLabel.setText(message);
    }

    private String resolveCompetition(Equipe equipe) {
        String competitionCode = equipe == null ? null : FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode());
        if (competitionCode != null) {
            return FootballDataCompetitions.labelOf(competitionCode);
        }
        String source = equipe == null ? null : emptyToNull(equipe.getExternalSource());
        return source == null ? "Locale" : source;
    }

    private String buildPlayerName(Joueur joueur) {
        String prenom = emptyToNull(joueur.getPrenom());
        String nom = emptyToNull(joueur.getNom());
        String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return fullName.isBlank() ? "Joueur" : fullName;
    }

    private String buildMatchLabel(Matchs match, Map<Integer, String> equipeNames) {
        String home = equipeNames.getOrDefault(match.getEquipeDomicileId(), "Equipe domicile");
        String away = equipeNames.getOrDefault(match.getEquipeExterieurId(), "Equipe exterieur");
        return home + " vs " + away;
    }

    private String buildMatchDate(Matchs match) {
        String date = match.getDateMatch() == null ? "-" : DATE_FORMATTER.format(match.getDateMatch());
        String time = match.getHeureDebut() == null ? "--:--" : TIME_FORMATTER.format(match.getHeureDebut());
        return date + " " + time;
    }

    private boolean hasFinalScore(Matchs match) {
        return match != null && match.getScoreEquipeDomicile() != null && match.getScoreEquipeExterieur() != null;
    }

    private MatchResult resolveResult(Matchs match, Integer teamId) {
        int goalsFor = goalsFor(match, teamId);
        int goalsAgainst = goalsAgainst(match, teamId);
        if (goalsFor > goalsAgainst) {
            return MatchResult.WIN;
        }
        if (goalsFor < goalsAgainst) {
            return MatchResult.LOSS;
        }
        return MatchResult.DRAW;
    }

    private int goalsFor(Matchs match, Integer teamId) {
        if (match == null || teamId == null) {
            return 0;
        }
        if (teamId.equals(match.getEquipeDomicileId())) {
            return match.getScoreEquipeDomicile() == null ? 0 : match.getScoreEquipeDomicile();
        }
        return match.getScoreEquipeExterieur() == null ? 0 : match.getScoreEquipeExterieur();
    }

    private int goalsAgainst(Matchs match, Integer teamId) {
        if (match == null || teamId == null) {
            return 0;
        }
        if (teamId.equals(match.getEquipeDomicileId())) {
            return match.getScoreEquipeExterieur() == null ? 0 : match.getScoreEquipeExterieur();
        }
        return match.getScoreEquipeDomicile() == null ? 0 : match.getScoreEquipeDomicile();
    }

    private double roundRate(long count, double total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((count * 1000.0) / total) / 10.0;
    }

    private String buildAverageAge(List<Joueur> joueurs) {
        double averageAge = joueurs.stream()
                .map(Joueur::getDateNaissance)
                .filter(date -> date != null)
                .mapToInt(date -> Period.between(date, LocalDate.now()).getYears())
                .average()
                .orElse(0);
        return averageAge <= 0 ? "-" : String.format("%.1f ans", averageAge);
    }

    private String normalizeStatus(String status) {
        String normalized = emptyToNull(status);
        if (normalized == null) {
            return null;
        }
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase();
    }

    private void applyBarColors(XYChart.Series<String, Number> series, List<String> colors) {
        Platform.runLater(() -> {
            for (int index = 0; index < series.getData().size(); index++) {
                XYChart.Data<String, Number> data = series.getData().get(index);
                String color = colors.get(index % colors.size());
                applyBarColor(data, color);
                data.nodeProperty().addListener((observable, oldNode, newNode) -> applyBarColor(data, color));
            }
        });
    }

    private void repaintBarColors(XYChart.Series<String, Number> series, List<String> colors) {
        Platform.runLater(() -> {
            for (int index = 0; index < series.getData().size(); index++) {
                XYChart.Data<String, Number> data = series.getData().get(index);
                applyBarColor(data, colors.get(index % colors.size()));
            }
        });
    }

    private void forceTransparent(Node node) {
        if (node == null) {
            return;
        }
        node.setStyle(TRANSPARENT_SURFACE_STYLE);
    }

    private void applyBarColor(XYChart.Data<String, Number> data, String color) {
        if (data == null) {
            return;
        }
        Node node = data.getNode();
        if (node != null) {
            node.setStyle("-fx-bar-fill: " + color + ";");
        }
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String buildTrainingTime(Entrainement entrainement) {
        String start = entrainement.getHeureDebut() == null ? "--:--" : TIME_FORMATTER.format(entrainement.getHeureDebut());
        String end = entrainement.getHeureFin() == null ? "--:--" : TIME_FORMATTER.format(entrainement.getHeureFin());
        return start + " - " + end;
    }

    private String emptyIfNull(String value) {
        return emptyIfNull(value, "");
    }

    private String emptyIfNull(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    private record DashboardPayload(
            int userCount,
            int annonceCount,
            int entrainementCount,
            int equipeCount,
            int joueurCount,
            int matchCount,
            List<Equipe> equipes,
            List<Joueur> joueurs,
            List<Matchs> matchs,
            Map<Integer, String> equipeNames,
            List<UserRow> latestUsers,
            List<AnnonceRow> latestAnnonces,
            List<EntrainementRow> latestEntrainements,
            List<OperationRow> operations
    ) {
    }

    private record UserRow(Integer id, String name, String role, String status) {
    }

    private record AnnonceRow(Integer id, String title, String level, String status) {
    }

    private record EntrainementRow(Integer id, String date, String type, String place, String time) {
    }

    private record OperationRow(String module, String primary, String secondary, String meta) {
    }

    private record TeamOption(Integer id, String label, String competition) {
        @Override
        public String toString() {
            return competition == null || competition.isBlank() ? label : label + " - " + competition;
        }
    }

    private enum MatchResult {
        WIN,
        DRAW,
        LOSS
    }
}
