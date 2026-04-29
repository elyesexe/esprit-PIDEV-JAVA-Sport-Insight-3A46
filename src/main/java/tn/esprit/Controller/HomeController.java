package tn.esprit.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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
import javafx.scene.CacheHint;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import tn.esprit.entities.User;
import tn.esprit.i18n.I18n;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.security.AuthSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HomeController {
    private static final String MATCH_STATUS_PROGRAMME = "Programme";
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("home-db-worker"));
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
    private Label welcomeTitleLabel;
    @FXML
    private StackPane homeWelcomeCard;
    @FXML
    private FlowPane homeLinksGrid;
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
    private Button newsModuleBox;
    @FXML
    private Button trainModuleBox;
    @FXML
    private Button sponsorsModuleBox;
    @FXML
    private Button storeModuleBox;

    private Timeline dateRefreshTimeline;
    private SidebarModuleGroup sidebarModuleGroup;
    private final Map<Node, ParallelTransition> activeHoverAnimations = new WeakHashMap<>();

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

        refreshWelcomeTitle();
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

        Platform.runLater(() -> {
            enableAnimationCaching();
            installTileHoverAnimations();
            installNavbarHoverAnimations();
            playHomeIntroAnimations();
        });
    }

    private void refreshWelcomeTitle() {
        if (welcomeTitleLabel == null) {
            return;
        }
        User currentUser = AuthSession.getCurrentUser();
        String displayName = currentUser == null ? "" : currentUser.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            welcomeTitleLabel.setText(I18n.get("home.welcome.back"));
            return;
        }
        welcomeTitleLabel.setText(I18n.format("home.welcome.back.named", displayName));
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
            setModuleVisible(newsModuleBox, true);
            setModuleVisible(annoncesModuleBox, true);
            setModuleVisible(trainModuleBox, true);
            setModuleVisible(sponsorsModuleBox, true);
            setModuleVisible(storeModuleBox, true);
            return;
        }
        boolean teams = matchesTokens(n, "team", "teams", "equipe", "equipes", "club", "clubs", "roster");
        boolean players = matchesTokens(n, "player", "players", "joueur", "joueurs", "profile", "profiles");
        boolean matches = matchesTokens(n, "match", "matches", "matchs", "fixture", "fixtures", "game", "games");
        boolean news = matchesTokens(n, "news", "headline", "headlines", "football news", "sport insight news");
        boolean announcements = matchesTokens(n, "annonce", "annonces", "announcement", "announcements", "update", "updates");
        boolean training = matchesTokens(n, "train", "training", "entrainement", "entrainements", "session", "sessions");
        boolean sponsors = matchesTokens(n, "sponsor", "sponsors", "contract", "contracts", "partnership", "partnerships");
        boolean store = matchesTokens(n, "store", "shop", "product", "products", "order", "orders");
        boolean any = teams || players || matches || news || announcements || training || sponsors || store;
        if (!any) {
            setModuleVisible(equipesButton, true);
            setModuleVisible(joueursButton, true);
            setModuleVisible(matchsButton, true);
            setModuleVisible(newsModuleBox, true);
            setModuleVisible(annoncesModuleBox, true);
            setModuleVisible(trainModuleBox, true);
            setModuleVisible(sponsorsModuleBox, true);
            setModuleVisible(storeModuleBox, true);
            return;
        }
        setModuleVisible(equipesButton, teams);
        setModuleVisible(joueursButton, players);
        setModuleVisible(matchsButton, matches);
        setModuleVisible(newsModuleBox, news);
        setModuleVisible(annoncesModuleBox, announcements);
        setModuleVisible(trainModuleBox, training);
        setModuleVisible(sponsorsModuleBox, sponsors);
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
        } else if (matchesTokens(n, "news", "headline", "headlines", "football news", "sport insight news")) {
            handleOpenNews();
        } else if (matchesTokens(n, "annonce", "annonces", "announcement", "announcements", "update", "updates")) {
            handleOpenAnnonces();
        } else if (matchesTokens(n, "train", "training", "entrainement", "entrainements", "session", "sessions")) {
            handleOpenEntrainements();
        } else if (matchesTokens(n, "sponsor", "sponsors", "contract", "contracts", "partnership", "partnerships")) {
            handleOpenSponsors();
        } else if (matchesTokens(n, "store", "shop", "product", "products", "order", "orders")) {
            handleOpenStore();
        } else {
            Alert hint = new Alert(Alert.AlertType.INFORMATION);
            hint.setTitle(I18n.get("home.search.help.title"));
            hint.setHeaderText(null);
            hint.setContentText(I18n.get("home.search.help.message"));
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
        headerDateLabel.setText(DateTimeFormatter.ofPattern("EEEE d MMM", I18n.getLocale()).format(today));
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
    private void handleOpenNews() {
        Node source = newsModuleBox != null ? newsModuleBox : sidebarBrandBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/football-news-view.fxml", "/tn/esprit/styles/football-news-theme.css", "Sport Insight News | Sport Insight");
    }

    @FXML
    private void handleOpenEntrainements() {
        Node source = trainModuleBox != null ? trainModuleBox : sidebarBrandBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/entrainement-user-view.fxml", "/tn/esprit/styles/entrainement-theme.css", "Entrainements | Sport Insight");
    }

    @FXML
    private void handleOpenSponsors() {
        Node source = sponsorsModuleBox != null ? sponsorsModuleBox : sidebarBrandBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/sponsor-user-view.fxml", "/tn/esprit/styles/sponsor-theme.css", "Sponsors | Sport Insight");
    }

    @FXML
    private void handleOpenStore() {
        Node source = storeModuleBox != null ? storeModuleBox : sidebarBrandBox;
        SceneNavigator.switchScene(source, "/tn/esprit/views/store-view.fxml", "/tn/esprit/styles/store-theme.css", "Store | Sport Insight");
    }

    @FXML
    private void handleRegisterFace() {
        User currentUser = AuthSession.getCurrentUser();
        SceneNavigator.switchScene(sidebarBrandBox,
                "/tn/esprit/views/face_register.fxml",
                "/tn/esprit/styles/auth-theme.css",
                "Register Face | Sport Insight",
                controller -> {
                    if (controller instanceof FaceRegisterController faceRegisterController && currentUser != null) {
                        faceRegisterController.setTargetUser(currentUser);
                    }
                });
    }

    @FXML
    private void handleDeleteFace() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/face_login.fxml", "/tn/esprit/styles/auth-theme.css", "Delete Face | Sport Insight");
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

    private void installTileHoverAnimations() {
        installHoverAnimation(equipesButton);
        installHoverAnimation(joueursButton);
        installHoverAnimation(matchsButton);
        installHoverAnimation(newsModuleBox);
        installHoverAnimation(annoncesModuleBox);
        installHoverAnimation(trainModuleBox);
        installHoverAnimation(sponsorsModuleBox);
        installHoverAnimation(storeModuleBox);
    }

    private void installNavbarHoverAnimations() {
        installHoverAnimation(matchsNavButton, 1.02, -3.0, 150, false);
        installHoverAnimation(annonceNavButton, 1.02, -3.0, 150, false);
        installHoverAnimation(equipesNavButton, 1.015, -2.0, 140, false);
        installHoverAnimation(leaguesNavButton, 1.015, -2.0, 140, false);
        installHoverAnimation(joueursNavButton, 1.015, -2.0, 140, false);
        installHoverAnimation(adminNavButton, 1.02, -3.0, 150, false);
    }

    private void enableAnimationCaching() {
        for (Node node : new Node[] {
                homeWelcomeCard,
                equipesButton,
                joueursButton,
                matchsButton,
                newsModuleBox,
                annoncesModuleBox,
                trainModuleBox,
                sponsorsModuleBox,
                storeModuleBox,
                matchsNavButton,
                annonceNavButton,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton,
                adminNavButton
        }) {
            if (node == null) {
                continue;
            }
            node.setCache(true);
            node.setCacheHint(CacheHint.SPEED);
        }
    }

    private void playHomeIntroAnimations() {
        if (homeWelcomeCard != null) {
            homeWelcomeCard.setOpacity(0.0);
            homeWelcomeCard.setTranslateY(18);

            FadeTransition fade = new FadeTransition(Duration.millis(420), homeWelcomeCard);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition rise = new TranslateTransition(Duration.millis(420), homeWelcomeCard);
            rise.setFromY(18);
            rise.setToY(0.0);
            rise.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, rise).play();
        }

        playTileEntrance(equipesButton, 0);
        playTileEntrance(joueursButton, 45);
        playTileEntrance(matchsButton, 90);
        playTileEntrance(newsModuleBox, 135);
        playTileEntrance(annoncesModuleBox, 180);
        playTileEntrance(trainModuleBox, 225);
        playTileEntrance(sponsorsModuleBox, 270);
        playTileEntrance(storeModuleBox, 315);
    }

    private void playTileEntrance(Button card, int delayMillis) {
        if (card == null) {
            return;
        }

        Node content = card.lookup(".home-link-content");
        if (content == null) {
            content = card;
        }

        content.setOpacity(0.0);
        content.setTranslateY(22);

        FadeTransition fade = new FadeTransition(Duration.millis(300), content);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition rise = new TranslateTransition(Duration.millis(360), content);
        rise.setFromY(22);
        rise.setToY(0.0);
        rise.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition reveal = new ParallelTransition(fade, rise);
        SequentialTransition delayedReveal = new SequentialTransition(
                new javafx.animation.PauseTransition(Duration.millis(delayMillis)),
                reveal
        );
        delayedReveal.play();
    }

    private void installHoverAnimation(Button card) {
        installHoverAnimation(card, 1.04, -8.0, 200, true);
    }

    private void installHoverAnimation(Button card, double targetScale, double targetTranslate, int durationMillis, boolean cardStyle) {
        if (card == null) {
            return;
        }

        card.setOnMouseEntered(e -> playHover(card, true, targetScale, targetTranslate, durationMillis, cardStyle));
        card.setOnMouseExited(e -> playHover(card, false, targetScale, targetTranslate, durationMillis, cardStyle));
    }

    private void playHover(Button card, boolean hovered, double targetScale, double targetTranslate, int durationMillis, boolean cardStyle) {
        Node content = card.lookup(".home-link-content");
        if (content == null) {
            content = card;
        }
        Node bg = card.lookup(".home-link-bg");
        Node inner = card.lookup(".home-link-inner");
        Node iconWrap = card.lookup(".home-link-icon-wrap");

        ParallelTransition existing = activeHoverAnimations.remove(content);
        if (existing != null) {
            existing.stop();
        }

        double scaleValue = hovered ? targetScale : 1.0;
        double translateValue = hovered ? targetTranslate : 0.0;

        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMillis), content);
        scale.setToX(scaleValue);
        scale.setToY(scaleValue);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition lift = new TranslateTransition(Duration.millis(durationMillis), content);
        lift.setToY(translateValue);
        lift.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition parallel = new ParallelTransition(scale, lift);

        if (bg != null) {
            TranslateTransition parallax = new TranslateTransition(Duration.millis(durationMillis + 30), bg);
            parallax.setToY(hovered && cardStyle ? -12.0 : 0.0);
            parallax.setInterpolator(Interpolator.EASE_OUT);
            parallel.getChildren().add(parallax);
        }

        if (inner != null && cardStyle) {
            TranslateTransition overlayShift = new TranslateTransition(Duration.millis(durationMillis), inner);
            overlayShift.setToY(hovered ? -4.0 : 0.0);
            overlayShift.setInterpolator(Interpolator.EASE_BOTH);
            parallel.getChildren().add(overlayShift);
        }

        if (iconWrap != null && cardStyle) {
            ScaleTransition iconPop = new ScaleTransition(Duration.millis(durationMillis), iconWrap);
            iconPop.setToX(hovered ? 1.08 : 1.0);
            iconPop.setToY(hovered ? 1.08 : 1.0);
            iconPop.setInterpolator(Interpolator.EASE_BOTH);
            parallel.getChildren().add(iconPop);
        }

        if (hovered) {
            content.setEffect(new DropShadow(cardStyle ? 34 : 20, 0, cardStyle ? 14 : 8, Color.color(0.06, 0.09, 0.16, cardStyle ? 0.38 : 0.22)));
        } else {
            content.setEffect(null);
        }

        activeHoverAnimations.put(content, parallel);
        parallel.play();
    }
}
