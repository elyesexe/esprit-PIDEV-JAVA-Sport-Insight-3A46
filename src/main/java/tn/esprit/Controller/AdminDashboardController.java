package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class AdminDashboardController {
    private static final String DARK_TABLE_CLASS = "admin-dashboard-force-dark";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("admin-dashboard-db"));

    @FXML
    private Label equipeCountLabel;
    @FXML
    private Label joueurCountLabel;
    @FXML
    private Label matchCountLabel;
    @FXML
    private Label dashboardStatusLabel;
    @FXML
    private TableView<Equipe> teamsTableView;
    @FXML
    private TableColumn<Equipe, Integer> teamIdColumn;
    @FXML
    private TableColumn<Equipe, String> teamNameColumn;
    @FXML
    private TableColumn<Equipe, String> teamCoachColumn;
    @FXML
    private TableColumn<Equipe, String> teamCompetitionColumn;
    @FXML
    private TableView<PlayerRow> playersTableView;
    @FXML
    private TableColumn<PlayerRow, Integer> playerIdColumn;
    @FXML
    private TableColumn<PlayerRow, String> playerNameColumn;
    @FXML
    private TableColumn<PlayerRow, String> playerTeamColumn;
    @FXML
    private TableColumn<PlayerRow, String> playerNumberColumn;
    @FXML
    private TableView<MatchRow> matchesTableView;
    @FXML
    private TableColumn<MatchRow, String> matchReferenceColumn;
    @FXML
    private TableColumn<MatchRow, String> matchLabelColumn;
    @FXML
    private TableColumn<MatchRow, String> matchDateColumn;
    @FXML
    private TableColumn<MatchRow, String> matchStatusColumn;

    @FXML
    public void initialize() {
        configureTables();
        setLoadingState();
        loadDashboardAsync();
    }

    public void setDarkMode(boolean darkMode) {
        toggleDarkClass(teamsTableView, darkMode);
        toggleDarkClass(playersTableView, darkMode);
        toggleDarkClass(matchesTableView, darkMode);
    }

    private void configureTables() {
        teamIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        teamNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getNom())));
        teamCoachColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getCoach(), "Non renseigne")));
        teamCompetitionColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveCompetition(cell.getValue())));

        playerIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        playerNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        playerTeamColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().team()));
        playerNumberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().number()));

        matchReferenceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().reference()));
        matchLabelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().label()));
        matchDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().date()));
        matchStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));

        teamsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        playersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        matchesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadDashboardAsync() {
        Task<DashboardPayload> task = new Task<>() {
            @Override
            protected DashboardPayload call() throws Exception {
                EquipeService equipeService = new EquipeService();
                JoueurService joueurService = new JoueurService();
                MatchsService matchsService = new MatchsService();

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
                List<PlayerRow> playerRows = joueurs.stream()
                        .limit(6)
                        .map(joueur -> new PlayerRow(
                                joueur.getId(),
                                buildPlayerName(joueur),
                                equipeNames.getOrDefault(joueur.getEquipeId(), "Sans equipe"),
                                joueur.getNumero() > 0 ? "#" + joueur.getNumero() : "-"
                        ))
                        .toList();

                List<Matchs> matchs = new ArrayList<>(matchsService.getAll());
                matchs.sort(Comparator
                        .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                        .reversed());
                List<MatchRow> matchRows = matchs.stream()
                        .limit(8)
                        .map(match -> new MatchRow(
                                emptyIfNull(match.getIdMatch(), match.getId() == null ? "-" : "#" + match.getId()),
                                buildMatchLabel(match, equipeNames),
                                buildMatchDate(match),
                                emptyIfNull(match.getStatut(), "Programme")
                        ))
                        .toList();

                return new DashboardPayload(
                        equipes.size(),
                        joueurs.size(),
                        matchs.size(),
                        equipes.stream().limit(6).toList(),
                        playerRows,
                        matchRows
                );
            }
        };

        task.setOnSucceeded(event -> {
            DashboardPayload payload = task.getValue();
            equipeCountLabel.setText(String.valueOf(payload.teamCount()));
            joueurCountLabel.setText(String.valueOf(payload.playerCount()));
            matchCountLabel.setText(String.valueOf(payload.matchCount()));
            teamsTableView.setItems(FXCollections.observableArrayList(payload.latestTeams()));
            playersTableView.setItems(FXCollections.observableArrayList(payload.latestPlayers()));
            matchesTableView.setItems(FXCollections.observableArrayList(payload.latestMatches()));
            setStatus("Donnees chargees.", "status-success");
        });

        task.setOnFailed(event -> {
            equipeCountLabel.setText("-");
            joueurCountLabel.setText("-");
            matchCountLabel.setText("-");
            teamsTableView.setItems(FXCollections.observableArrayList());
            playersTableView.setItems(FXCollections.observableArrayList());
            matchesTableView.setItems(FXCollections.observableArrayList());
            setStatus("Chargement impossible.", "status-error");

            Throwable exception = task.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
        });

        DB_EXECUTOR.execute(task);
    }

    private void setLoadingState() {
        equipeCountLabel.setText("...");
        joueurCountLabel.setText("...");
        matchCountLabel.setText("...");
        teamsTableView.setItems(FXCollections.observableArrayList());
        playersTableView.setItems(FXCollections.observableArrayList());
        matchesTableView.setItems(FXCollections.observableArrayList());
        setStatus("Chargement...", "status-muted");
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
            int teamCount,
            int playerCount,
            int matchCount,
            List<Equipe> latestTeams,
            List<PlayerRow> latestPlayers,
            List<MatchRow> latestMatches
    ) {
    }

    private record PlayerRow(Integer id, String name, String team, String number) {
    }

    private record MatchRow(String reference, String label, String date, String status) {
    }
}
