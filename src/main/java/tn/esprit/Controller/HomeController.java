package tn.esprit.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HomeController {
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;
    private static final String MATCH_STATUS_PROGRAMME = "Programme";
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("home-db-worker"));
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private static final Path NOTES_LOG_PATH =
            Path.of(System.getProperty("user.home"), ".sport-insight", "home-notes.log");

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Label sidebarSectionLabel;
    @FXML
    private Button sidebarToggleButton;
    @FXML
    private Button sidebarOpenButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button equipesButton;
    @FXML
    private Button joueursButton;
    @FXML
    private Button matchsButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label headerDateLabel;
    @FXML
    private Button newNoteButton;
    @FXML
    private Label matchesTodayMetricLabel;
    @FXML
    private Label activePlayersMetricLabel;
    @FXML
    private Label pendingTasksMetricLabel;

    private Timeline dateRefreshTimeline;
    private boolean sidebarVisible;

    @FXML
    public void initialize() {
        sidebarVisible = true;
        ThemeManager.bindToggle(themeToggleButton);
        applySidebarState();

        refreshHeaderDate();
        startDateRefreshTimeline();

        matchesTodayMetricLabel.setText("…");
        activePlayersMetricLabel.setText("…");
        pendingTasksMetricLabel.setText("…");

        loadDashboardMetricsAsync();
    }

    private void startDateRefreshTimeline() {
        if (dateRefreshTimeline != null) {
            dateRefreshTimeline.stop();
        }
        dateRefreshTimeline = new Timeline(new KeyFrame(Duration.minutes(30), e -> refreshHeaderDate()));
        dateRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        dateRefreshTimeline.play();
    }

    private void refreshHeaderDate() {
        LocalDate today = LocalDate.now();
        headerDateLabel.setText(HEADER_DATE_FORMAT.format(today));
    }

    private void loadDashboardMetricsAsync() {
        Task<DashboardCounts> task = new Task<>() {
            @Override
            protected DashboardCounts call() throws Exception {
                LocalDate today = LocalDate.now();
                MatchsService matchsService = new MatchsService();
                JoueurService joueurService = new JoueurService();
                int matchesToday = matchsService.countMatchesOnDate(today);
                int pendingProgramme = matchsService.countByStatut(MATCH_STATUS_PROGRAMME);
                int activePlayers = joueurService.countAll();
                return new DashboardCounts(matchesToday, activePlayers, pendingProgramme);
            }
        };
        task.setOnSucceeded(e -> {
            DashboardCounts c = task.getValue();
            matchesTodayMetricLabel.setText(String.valueOf(c.matchesToday));
            activePlayersMetricLabel.setText(String.valueOf(c.activePlayers));
            pendingTasksMetricLabel.setText(String.valueOf(c.pendingTasks));
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            matchesTodayMetricLabel.setText("—");
            activePlayersMetricLabel.setText("—");
            pendingTasksMetricLabel.setText("—");
            if (ex != null) {
                ex.printStackTrace();
            }
        });
        DB_EXECUTOR.execute(task);
    }

    /**
     * Opens a note dialog and appends non-empty text to a local log under the user home folder.
     */
    @FXML
    private void handleNewNote() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New note");
        dialog.setHeaderText("Add an operations note");
        dialog.initOwner(newNoteButton != null ? newNoteButton.getScene().getWindow() : null);

        TextArea area = new TextArea();
        area.setPromptText("Write your note…");
        area.setPrefRowCount(8);
        area.setWrapText(true);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(area, 0, 0);
        GridPane.setHgrow(area, Priority.ALWAYS);
        GridPane.setVgrow(area, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            String text = area.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            try {
                appendNoteToLog(text.trim());
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Note saved");
                info.setHeaderText(null);
                info.setContentText("Your note was saved to:\n" + NOTES_LOG_PATH.toAbsolutePath());
                info.showAndWait();
            } catch (IOException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Note");
                err.setHeaderText("Could not save the note");
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private static void appendNoteToLog(String body) throws IOException {
        Files.createDirectories(NOTES_LOG_PATH.getParent());
        String line = LocalDate.now()
                + " "
                + java.time.LocalTime.now()
                + " — "
                + body.replace("\r\n", " ").replace('\n', ' ')
                + System.lineSeparator();
        Files.writeString(
                NOTES_LOG_PATH,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    @FXML
    private void handleOpenHome() {
        // Already on the homepage.
    }

    @FXML
    private void handleOpenSidebar() {
        sidebarVisible = true;
        applySidebarState();
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarVisible = false;
        applySidebarState();
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesButton, equipesNavButton), "/tn/esprit/views/equipe-crud-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Sport Insight");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursButton, joueursNavButton), "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenMatchs() {
        SceneNavigator.switchScene(resolveNavigationSource(matchsButton, matchsNavButton), "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    private Button resolveNavigationSource(Button primary, Button fallback) {
        return primary != null ? primary : fallback;
    }

    private void applySidebarState() {
        sidebarRoot.setManaged(sidebarVisible);
        sidebarRoot.setVisible(sidebarVisible);
        sidebarSectionLabel.setManaged(sidebarVisible);
        sidebarSectionLabel.setVisible(sidebarVisible);
        sidebarOpenButton.setManaged(!sidebarVisible);
        sidebarOpenButton.setVisible(!sidebarVisible);

        sidebarBrandBox.setVisible(sidebarVisible);
        sidebarBrandBox.setManaged(sidebarVisible);
        sidebarToggleButton.setText("<");

        sidebarRoot.setMinWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
    }

    private static ThreadFactory daemonFactory(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    private record DashboardCounts(int matchesToday, int activePlayers, int pendingTasks) {
    }
}
