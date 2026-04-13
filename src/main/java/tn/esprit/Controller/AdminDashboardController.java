package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
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

    @FXML
    public void initialize() {
        configureTables();
        setLoadingState();
        loadDashboardAsync();
    }

    public void setDarkMode(boolean darkMode) {
        toggleDarkClass(usersTableView, darkMode);
        toggleDarkClass(annoncesTableView, darkMode);
        toggleDarkClass(entrainementsTableView, darkMode);
        toggleDarkClass(operationsTableView, darkMode);
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
}
