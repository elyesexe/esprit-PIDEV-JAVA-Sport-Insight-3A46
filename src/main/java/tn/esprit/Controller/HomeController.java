package tn.esprit.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HomeController {
    private static final String MATCH_STATUS_PROGRAMME = "Programme";
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("home-db-worker"));
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private static final Path NOTES_LOG_PATH =
            Path.of(System.getProperty("user.home"), ".sport-insight", "home-notes.log");

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
    @FXML
    private TextField homeSearchField;
    @FXML
    private Button annoncesModuleBox;
    @FXML
    private Button trainModuleBox;
    @FXML
    private VBox storeModuleBox;

    private Timeline dateRefreshTimeline;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);

        refreshHeaderDate();
        startDateRefreshTimeline();

        if (matchesTodayMetricLabel != null && activePlayersMetricLabel != null && pendingTasksMetricLabel != null) {
            matchesTodayMetricLabel.setText("...");
            activePlayersMetricLabel.setText("...");
            pendingTasksMetricLabel.setText("...");
            loadDashboardMetricsAsync();
        }

        if (homeSearchField != null) {
            homeSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyHomeSearchFilter(newVal));
        }
    }

    /**
     * Filters Core Modules tiles; Enter runs {@link #handleHomeSearch()} to open a module when unambiguous.
     */
    private void applyHomeSearchFilter(String raw) {
        String n = normalizeSearchQuery(raw);
        if (n.isEmpty()) {
            setModuleVisible(equipesButton, true);
            setModuleVisible(joueursButton, true);
            setModuleVisible(matchsButton, true);
            setModuleVisible(annoncesModuleBox, true);
            setModuleVisible(trainModuleBox, true);
            setModuleVisible(storeModuleBox, true);
            return;
        }
        boolean teams = matchesTokens(n, "team", "teams", "equipe", "equipes", "club", "clubs", "roster");
        boolean players = matchesTokens(n, "player", "players", "joueur", "joueurs", "profile", "profiles");
        boolean matches = matchesTokens(n, "match", "matches", "matchs", "fixture", "fixtures", "game", "games");
        boolean news = matchesTokens(n, "news", "annonce", "annonces", "update", "updates");
        boolean training = matchesTokens(n, "train", "training", "entrainement", "entrainements", "session", "sessions");
        boolean store = matchesTokens(n, "store", "shop", "product", "products", "order", "orders");
        boolean any = teams || players || matches || news || training || store;
        if (!any) {
            setModuleVisible(equipesButton, true);
            setModuleVisible(joueursButton, true);
            setModuleVisible(matchsButton, true);
            setModuleVisible(annoncesModuleBox, true);
            setModuleVisible(trainModuleBox, true);
            setModuleVisible(storeModuleBox, true);
            return;
        }
        setModuleVisible(equipesButton, teams);
        setModuleVisible(joueursButton, players);
        setModuleVisible(matchsButton, matches);
        setModuleVisible(annoncesModuleBox, news);
        setModuleVisible(trainModuleBox, training);
        setModuleVisible(storeModuleBox, store);
    }

    private static void setModuleVisible(Node node, boolean visible) {
        if (node != null) {
            node.setManaged(visible);
            node.setVisible(visible);
        }
    }

    private static String normalizeSearchQuery(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ù', 'u')
                .replace('ç', 'c');
    }

    private static boolean matchesTokens(String normalizedQuery, String... tokens) {
        for (String t : tokens) {
            if (normalizedQuery.contains(t)) {
                return true;
            }
        }
        return false;
    }

    @FXML
    private void handleHomeSearch() {
        if (homeSearchField == null) {
            return;
        }
        applyHomeSearchFilter(homeSearchField.getText());
        Button soleMain = soleVisibleMainModule();
        if (soleMain != null) {
            if (soleMain == equipesButton) {
                handleOpenEquipes();
            } else if (soleMain == joueursButton) {
                handleOpenJoueurs();
            } else if (soleMain == matchsButton) {
                openMatchsModule();
            }
            return;
        }
        String n = normalizeSearchQuery(homeSearchField.getText());
        if (n.isEmpty()) {
            return;
        }
        if (matchesTokens(n, "team", "teams", "equipe", "equipes", "club", "clubs", "roster")) {
            handleOpenEquipes();
        } else if (matchesTokens(n, "player", "players", "joueur", "joueurs", "profile", "profiles")) {
            handleOpenJoueurs();
        } else if (matchesTokens(n, "match", "matches", "matchs", "fixture", "fixtures", "game", "games")) {
            openMatchsModule();
        } else if (matchesTokens(n, "news", "annonce", "annonces", "update", "updates")) {
            handleOpenAnnonces();
        } else if (matchesTokens(n, "train", "training", "entrainement", "entrainements", "session", "sessions")) {
            handleOpenEntrainements();
        } else {
            Alert hint = new Alert(Alert.AlertType.INFORMATION);
            hint.setTitle("Search");
            hint.setHeaderText(null);
            hint.setContentText(
                    "Type part of a keyword to filter tiles (e.g. team, player, match), then press Enter to open a module.\n"
                            + "Examples: \"team\" -> Teams, \"joueur\" -> Players, \"match\" -> Matches.");
            hint.initOwner(homeSearchField.getScene() != null ? homeSearchField.getScene().getWindow() : null);
            hint.showAndWait();
        }
    }

    private Button soleVisibleMainModule() {
        int count = 0;
        Button last = null;
        for (Button b : new Button[] { equipesButton, joueursButton, matchsButton }) {
            if (b != null && b.isVisible()) {
                count++;
                last = b;
            }
        }
        return count == 1 ? last : null;
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
        if (headerDateLabel == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        headerDateLabel.setText(HEADER_DATE_FORMAT.format(today));
    }

    private void loadDashboardMetricsAsync() {
        if (matchesTodayMetricLabel == null || activePlayersMetricLabel == null || pendingTasksMetricLabel == null) {
            return;
        }
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
            matchesTodayMetricLabel.setText("-");
            activePlayersMetricLabel.setText("-");
            pendingTasksMetricLabel.setText("-");
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
        area.setPromptText("Write your note...");
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
                + " - "
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
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesButton, equipesNavButton), "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursButton, joueursNavButton), "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenMatchs(ActionEvent event) {
        if (event != null
                && event.getSource() == matchsNavButton
                && sidebarModuleGroup != null
                && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        openMatchsModule();
    }

    private void openMatchsModule() {
        SceneNavigator.switchScene(resolveNavigationSource(matchsButton, matchsNavButton), "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    @FXML
    private void handleOpenAnnonces() {
        Node source = annonceNavButton != null ? annonceNavButton : annoncesModuleBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/annonce-user-view.fxml", "/tn/esprit/styles/annonce-theme.css", "Anonce | Sport Insight");
    }

    @FXML
    private void handleOpenEntrainements() {
        Node source = trainModuleBox != null ? trainModuleBox : sidebarBrandBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/entrainement-user-view.fxml", "/tn/esprit/styles/entrainement-theme.css", "Entrainements | Sport Insight");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    private Button resolveNavigationSource(Button primary, Button fallback) {
        return primary != null ? primary : fallback;
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
