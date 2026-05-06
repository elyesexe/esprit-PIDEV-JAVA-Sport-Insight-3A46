package tn.esprit.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.ChromiumBrowserView;
import tn.esprit.gui.LiveMatchNotificationRuntime;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchLiveCompanionResponse;
import tn.esprit.services.MatchFollowTargetService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.ApiFootballFixtureSnapshot;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballLineupPlayer;
import tn.esprit.services.football.ApiFootballLineupSide;
import tn.esprit.services.football.ApiFootballMatchIncident;
import tn.esprit.services.football.ApiFootballMatchDetails;
import tn.esprit.services.football.ApiFootballOddsService;
import tn.esprit.services.football.ApiFootballOddsSnapshot;
import tn.esprit.services.football.ApiFootballStatisticRow;
import tn.esprit.services.football.FootballDataCompetitions;
import tn.esprit.services.football.YouTubeService;
import tn.esprit.services.football.YouTubeVideo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchDetailController implements AssistantContextProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final String PITCH_LAYOUT_KEY = "matchDetailPitchLayout";
    private static final String PITCH_DECORATIONS_KEY = "matchDetailPitchDecorations";
    private static final String PITCH_DECORATION_NODE_KEY = "matchDetailPitchDecorationNode";
    private static final double PITCH_MARKER_WIDTH = 114.0;
    private static final double PITCH_MARKER_MIN_WIDTH = 64.0;
    private static final double PITCH_MARKER_HEIGHT = 74.0;
    private static final double PITCH_HORIZONTAL_INSET = 44.0;
    private static final double PITCH_VERTICAL_INSET = 30.0;
    private static final double PITCH_FIELD_LINE_INSET = 18.0;
    private static final Map<String, Image> PLAYER_PHOTO_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Image>> PLAYER_PHOTO_REQUESTS = new ConcurrentHashMap<>();
    private static final ExecutorService API_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("match-detail-api-worker"));
    private static final ExecutorService VIDEO_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("match-detail-video-worker"));
    private static final ExecutorService PHOTO_EXECUTOR =
            Executors.newFixedThreadPool(4, daemonFactory("match-detail-photo-worker"));
    private static final HttpClient PHOTO_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static final Pattern LIVE_MINUTE_PATTERN = Pattern.compile("(\\d+(?:\\+\\d+)?')");
    private static final Color LIVE_ATTENTION_ACCENT = Color.web("#ef4444");
    private static final Color LIVE_ATTENTION_LIGHT_BASE = Color.web("#111827");
    private static final Color LIVE_ATTENTION_DARK_BASE = Color.web("#f8fafc");

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
    private Label competitionBadgeLabel;
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
    private Label detailLivePhaseLabel;
    @FXML
    private Label detailLiveMinuteLabel;
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
    private Label detailCompetitionValueLabel;
    @FXML
    private Button summaryTabButton;
    @FXML
    private Button statsTabButton;
    @FXML
    private Button lineupTabButton;
    @FXML
    private Button videosTabButton;
    @FXML
    private Button oddsTabButton;
    @FXML
    private Button followHomeTeamButton;
    @FXML
    private Button followAwayTeamButton;
    @FXML
    private Button followCompetitionButton;
    @FXML
    private Button followMatchButton;
    @FXML
    private VBox summarySection;
    @FXML
    private VBox statsSection;
    @FXML
    private VBox lineupSection;
    @FXML
    private VBox videosSection;
    @FXML
    private VBox oddsSection;
    @FXML
    private Label summaryHomeTeamLabel;
    @FXML
    private Label summaryAwayTeamLabel;
    @FXML
    private Label liveCompanionSummaryLabel;
    @FXML
    private Label liveCompanionDominantTeamValueLabel;
    @FXML
    private Label liveCompanionPressureValueLabel;
    @FXML
    private Label liveCompanionDangerValueLabel;
    @FXML
    private Label liveCompanionIntensityValueLabel;
    @FXML
    private VBox liveCompanionTurningPointsContainer;
    @FXML
    private VBox liveCompanionImpactsContainer;
    @FXML
    private VBox summaryTimelineContainer;
    @FXML
    private Label summaryEmptyLabel;
    @FXML
    private Label lineupHomeTeamLabel;
    @FXML
    private Label lineupAwayTeamLabel;
    @FXML
    private Label lineupDomicileMetaLabel;
    @FXML
    private Label lineupExterieurMetaLabel;
    @FXML
    private Label apiFootballStatusLabel;
    @FXML
    private Pane homeLineupPitchContainer;
    @FXML
    private Pane awayLineupPitchContainer;
    @FXML
    private FlowPane homeBenchContainer;
    @FXML
    private FlowPane awayBenchContainer;
    @FXML
    private Label statsHomeTeamLabel;
    @FXML
    private Label statsAwayTeamLabel;
    @FXML
    private VBox matchStatsContainer;
    @FXML
    private Label matchStatsEmptyLabel;
    @FXML
    private Label matchVideoStatusLabel;
    @FXML
    private Label youtubeMatchInfoLabel;
    @FXML
    private Label selectedVideoTitleLabel;
    @FXML
    private Label selectedVideoMetaLabel;
    @FXML
    private Button fullscreenVideoButton;
    @FXML
    private Button inAppYouTubePlayerButton;
    @FXML
    private Button refreshVideosButton;
    @FXML
    private Button localMp4DemoButton;
    @FXML
    private StackPane chromiumPlayerHost;
    @FXML
    private StackPane matchVideoPlayerShell;
    @FXML
    private ListView<YouTubeVideo> youtubeVideoListView;
    @FXML
    private WebView matchVideoWebView;
    @FXML
    private MediaView localDemoMediaView;
    @FXML
    private VBox matchVideoEmptyStateBox;
    @FXML
    private Label matchVideoEmptyTitleLabel;
    @FXML
    private Label matchVideoEmptyBodyLabel;
    @FXML
    private Button refreshOddsButton;
    @FXML
    private Label oddsStateLabel;
    @FXML
    private Label oddsSourceLabel;
    @FXML
    private Label oddsUpdatedLabel;
    @FXML
    private Label oddsMessageLabel;
    @FXML
    private VBox oddsMarketsContainer;
    @FXML
    private Label oddsGestureTitleLabel;
    @FXML
    private Label oddsGestureBodyLabel;
    @FXML
    private Label oddsGesturePrimaryLabel;
    @FXML
    private Label oddsGestureSecondaryLabel;

    private MatchsService matchsService;
    private EquipeService equipeService;
    private ApiFootballInsightsService apiFootballInsightsService;
    private ApiFootballOddsService apiFootballOddsService;
    private MatchFollowTargetService matchFollowTargetService;
    private YouTubeService youtubeService;
    private Matchs match;
    private Equipe homeTeam;
    private Equipe awayTeam;
    private SidebarModuleGroup sidebarModuleGroup;
    private final AtomicLong apiRequestSequence = new AtomicLong();
    private final AtomicLong videoRequestSequence = new AtomicLong();
    private final AtomicLong oddsRequestSequence = new AtomicLong();
    private List<ApiFootballMatchIncident> currentIncidents = List.of();
    private ApiFootballLineupSide currentHomeLineup;
    private ApiFootballLineupSide currentAwayLineup;
    private List<ApiFootballStatisticRow> currentStatistics = List.of();
    private Long currentMvpPlayerId;
    private String currentMvpPlayerNameKey;
    private String currentApiFootballStatus = "Detailed match data not loaded yet.";
    private MatchDetailTab activeTab = MatchDetailTab.SUMMARY;
    private Timeline liveRefreshTimeline;
    private Timeline liveAttentionTimeline;
    private boolean liveAttentionAccentFrame;
    private boolean liveAttentionMinuteVisible;
    private boolean apiRefreshInProgress;
    private final ObservableList<YouTubeVideo> youtubeVideos = FXCollections.observableArrayList();
    private YouTubeVideo selectedYouTubeVideo;
    private YouTubeVideo loadedYouTubeVideo;
    private ChromiumBrowserView chromiumBrowserView;
    private MediaPlayer localDemoMediaPlayer;
    private File currentLocalMp4File;
    private boolean matchVideoLookupCompleted;
    private boolean matchVideoRefreshInProgress;
    private boolean oddsRefreshInProgress;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        if (matchVideoWebView != null) {
            matchVideoWebView.setContextMenuEnabled(false);
            matchVideoWebView.getEngine().setJavaScriptEnabled(true);
            matchVideoWebView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
        }
        configureYouTubeVideoListView();
        configureChromiumPlayer();
        configureLocalDemoMediaView();
        applyActiveTab();
        configureLiveRefreshLifecycle();
        youtubeService = new YouTubeService();
        apiFootballOddsService = new ApiFootballOddsService();

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            apiFootballInsightsService = new ApiFootballInsightsService();
            matchFollowTargetService = new MatchFollowTargetService();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer la fiche match.\n" + e.getMessage());
        }

        if (match != null) {
            renderMatch();
        }
    }

    public void setMatchContext(Matchs match) {
        this.match = match;
        if (detailTitleLabel != null) {
            renderMatch();
        }
    }

    public Matchs getCurrentMatch() {
        return match;
    }

    private void configureYouTubeVideoListView() {
        if (youtubeVideoListView == null) {
            return;
        }

        youtubeVideoListView.setItems(youtubeVideos);
        youtubeVideoListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(YouTubeVideo video, boolean empty) {
                super.updateItem(video, empty);
                if (empty || video == null) {
                    setText(null);
                    return;
                }
                setText(emptyToFallback(video.title(), "YouTube highlight")
                        + "\n"
                        + emptyToFallback(video.channelTitle(), "YouTube"));
                setWrapText(true);
            }
        });
        youtubeVideoListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedYouTubeVideo = newValue;
            renderSelectedMatchVideo();
        });
    }

    private void configureChromiumPlayer() {
        if (chromiumPlayerHost == null) {
            return;
        }
        chromiumBrowserView = new ChromiumBrowserView();
        chromiumBrowserView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        chromiumPlayerHost.getChildren().setAll(chromiumBrowserView);
        chromiumPlayerHost.setManaged(false);
        chromiumPlayerHost.setVisible(false);
    }

    private void configureLocalDemoMediaView() {
        if (localDemoMediaView == null) {
            return;
        }
        StackPane sizingHost = matchVideoPlayerShell == null ? chromiumPlayerHost : matchVideoPlayerShell;
        if (sizingHost != null) {
            localDemoMediaView.fitWidthProperty().bind(sizingHost.widthProperty());
            localDemoMediaView.fitHeightProperty().bind(sizingHost.heightProperty());
        }
        localDemoMediaView.setPreserveRatio(true);
        localDemoMediaView.setManaged(false);
        localDemoMediaView.setVisible(false);
    }

    @FXML
    private void handleToggleFollowMatch() {
        if (match == null || match.getId() == null || matchFollowTargetService == null) {
            showApiFootballStatus("Ce match ne peut pas encore etre suivi.", "status-warning");
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        if (userTarget == null) {
            showApiFootballStatus("Connectez-vous pour suivre ce match.", "status-warning");
            return;
        }

        try {
            boolean alreadyFollowing = matchFollowTargetService.isMatchFavorite(userTarget.userId(), match.getId());
            if (alreadyFollowing) {
                matchFollowTargetService.removeMatchFavorite(userTarget.userId(), match.getId());
                showApiFootballStatus("Match retire des alertes live.", "status-muted");
            } else {
                matchFollowTargetService.addMatchFavorite(userTarget.userId(), match.getId());
                showApiFootballStatus("Match ajoute aux alertes live.", "status-success");
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            }
            refreshFollowButtons();
        } catch (SQLException e) {
            showApiFootballStatus("Impossible de mettre a jour les alertes live.", "status-warning");
        }
    }

    @FXML
    private void handleToggleFollowHomeTeam() {
        toggleTeamFollow(homeTeam);
    }

    @FXML
    private void handleToggleFollowAwayTeam() {
        toggleTeamFollow(awayTeam);
    }

    @FXML
    private void handleToggleFollowCompetition() {
        if (match == null || matchFollowTargetService == null) {
            showApiFootballStatus("Les alertes live ne sont pas disponibles pour le moment.", "status-warning");
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        if (userTarget == null) {
            showApiFootballStatus("Connectez-vous pour suivre cette competition.", "status-warning");
            return;
        }

        String competitionCode = FootballDataCompetitions.normalizeCode(match.getCompetitionCode());
        if (competitionCode == null) {
            showApiFootballStatus("Aucune competition n'est associee a ce match.", "status-warning");
            return;
        }

        try {
            boolean alreadyFollowing = matchFollowTargetService.isCompetitionFavorite(userTarget.userId(), competitionCode);
            if (alreadyFollowing) {
                matchFollowTargetService.removeCompetitionFavorite(userTarget.userId(), competitionCode);
                showApiFootballStatus(resolveCompetitionLabel(competitionCode) + " retiree des alertes live.", "status-muted");
            } else {
                matchFollowTargetService.addCompetitionFavorite(userTarget.userId(), competitionCode);
                showApiFootballStatus(resolveCompetitionLabel(competitionCode) + " ajoutee aux alertes live.", "status-success");
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            }
            refreshFollowButtons();
        } catch (SQLException e) {
            showApiFootballStatus("Impossible de mettre a jour les alertes live.", "status-warning");
        }
    }

    private void configureLiveRefreshLifecycle() {
        if (navbarRoot == null) {
            return;
        }

        navbarRoot.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                stopLiveRefresh();
                stopLiveAttentionAnimation();
                unloadMatchVideoPlayer();
                return;
            }
            startLiveRefreshIfNeeded();
        });
    }

    private void startLiveRefreshIfNeeded() {
        if (liveRefreshTimeline == null) {
            liveRefreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(25), event -> refreshLiveMatchAsync(false)));
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

    private void toggleTeamFollow(Equipe team) {
        if (team == null || team.getId() == null || matchFollowTargetService == null) {
            showApiFootballStatus("Cette equipe ne peut pas encore etre suivie.", "status-warning");
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        if (userTarget == null) {
            showApiFootballStatus("Connectez-vous pour suivre cette equipe.", "status-warning");
            return;
        }

        try {
            boolean alreadyFollowing = matchFollowTargetService.isTeamFavorite(userTarget.userId(), team.getId());
            if (alreadyFollowing) {
                matchFollowTargetService.removeTeamFavorite(userTarget.userId(), team.getId());
                showApiFootballStatus(emptyToFallback(team.getNom(), "Equipe") + " retiree des alertes live.", "status-muted");
            } else {
                matchFollowTargetService.addTeamFavorite(userTarget.userId(), team.getId());
                showApiFootballStatus(emptyToFallback(team.getNom(), "Equipe") + " ajoutee aux alertes live.", "status-success");
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            }
            refreshFollowButtons();
        } catch (SQLException e) {
            showApiFootballStatus("Impossible de mettre a jour les alertes live.", "status-warning");
        }
    }

    private void refreshFollowButtons() {
        updateMatchFollowButton();
        updateTeamFollowButton(followHomeTeamButton, homeTeam);
        updateTeamFollowButton(followAwayTeamButton, awayTeam);
        updateCompetitionFollowButton();
    }

    private void updateMatchFollowButton() {
        if (followMatchButton == null) {
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        boolean enabled = userTarget != null && match != null && match.getId() != null && matchFollowTargetService != null;
        followMatchButton.setDisable(!enabled);
        if (!enabled) {
            followMatchButton.setText("Follow match");
            return;
        }

        try {
            boolean following = matchFollowTargetService.isMatchFavorite(userTarget.userId(), match.getId());
            followMatchButton.setText(following ? "Following match" : "Follow match");
            followMatchButton.getStyleClass().removeAll("ghost-button", "soft-button", "primary-button");
            followMatchButton.getStyleClass().add(following ? "primary-button" : "soft-button");
        } catch (SQLException e) {
            followMatchButton.setText("Follow match");
        }
    }

    private void updateTeamFollowButton(Button button, Equipe team) {
        if (button == null) {
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        boolean enabled = userTarget != null && team != null && team.getId() != null && matchFollowTargetService != null;
        button.setDisable(!enabled);
        if (!enabled) {
            button.setText(team == null ? "Follow team" : "Follow " + emptyToFallback(team.getNom(), "team"));
            return;
        }

        try {
            boolean following = matchFollowTargetService.isTeamFavorite(userTarget.userId(), team.getId());
            button.setText((following ? "Following " : "Follow ") + emptyToFallback(team.getNom(), "team"));
            button.getStyleClass().removeAll("ghost-button", "soft-button", "primary-button");
            button.getStyleClass().add(following ? "primary-button" : "soft-button");
        } catch (SQLException e) {
            button.setText("Follow " + emptyToFallback(team.getNom(), "team"));
        }
    }

    private void updateCompetitionFollowButton() {
        if (followCompetitionButton == null) {
            return;
        }

        UserSessionTarget userTarget = resolveCurrentUserTarget();
        String competitionCode = match == null ? null : FootballDataCompetitions.normalizeCode(match.getCompetitionCode());
        boolean enabled = userTarget != null && competitionCode != null && matchFollowTargetService != null;
        followCompetitionButton.setDisable(!enabled);
        if (!enabled) {
            followCompetitionButton.setText("Follow competition");
            return;
        }

        try {
            boolean following = matchFollowTargetService.isCompetitionFavorite(userTarget.userId(), competitionCode);
            followCompetitionButton.setText((following ? "Following " : "Follow ") + resolveCompetitionLabel(competitionCode));
            followCompetitionButton.getStyleClass().removeAll("ghost-button", "soft-button", "primary-button");
            followCompetitionButton.getStyleClass().add(following ? "primary-button" : "ghost-button");
        } catch (SQLException e) {
            followCompetitionButton.setText("Follow competition");
        }
    }

    private UserSessionTarget resolveCurrentUserTarget() {
        var currentUser = AuthSession.getCurrentUser();
        return currentUser == null || currentUser.getId() == null ? null : new UserSessionTarget(currentUser.getId());
    }

    public String getCurrentCompetitionCode() {
        return match == null ? null : match.getCompetitionCode();
    }

    public String getCurrentHomeTeamName() {
        return detailHomeNameLabel != null ? detailHomeNameLabel.getText() : getEquipeName(match == null ? null : match.getEquipeDomicileId());
    }

    public String getCurrentAwayTeamName() {
        return detailAwayNameLabel != null ? detailAwayNameLabel.getText() : getEquipeName(match == null ? null : match.getEquipeExterieurId());
    }

    public String getCurrentMatchLabel() {
        if (detailTitleLabel != null && detailTitleLabel.getText() != null && !detailTitleLabel.getText().isBlank()) {
            return detailTitleLabel.getText();
        }
        return match == null ? "Current match" : buildMatchLabel(match);
    }

    public List<String> getCurrentHomeStartingLineupNames() {
        return extractStartingLineupNames(currentHomeLineup);
    }

    public List<String> getCurrentAwayStartingLineupNames() {
        return extractStartingLineupNames(currentAwayLineup);
    }

    public String getCurrentHomeLineupMeta() {
        return buildLineupMeta(currentHomeLineup);
    }

    public String getCurrentAwayLineupMeta() {
        return buildLineupMeta(currentAwayLineup);
    }

    public String getCurrentScoreLabel() {
        return detailScoreValueLabel != null && detailScoreValueLabel.getText() != null && !detailScoreValueLabel.getText().isBlank()
                ? detailScoreValueLabel.getText()
                : (match == null ? "- : -" : buildScore(match));
    }

    public String getCurrentStatusLabel() {
        return detailStatutValueLabel != null && detailStatutValueLabel.getText() != null && !detailStatutValueLabel.getText().isBlank()
                ? detailStatutValueLabel.getText()
                : resolveStatus(match);
    }

    public String getCurrentCompetitionLabel() {
        return detailCompetitionValueLabel != null && detailCompetitionValueLabel.getText() != null && !detailCompetitionValueLabel.getText().isBlank()
                ? detailCompetitionValueLabel.getText()
                : emptyToFallback(resolveCompetitionLabel(match == null ? null : match.getCompetitionCode()), "Competition");
    }

    public String getCurrentDateLabel() {
        return detailDateValueLabel != null && detailDateValueLabel.getText() != null && !detailDateValueLabel.getText().isBlank()
                ? detailDateValueLabel.getText()
                : formatDate(match == null ? null : match.getDateMatch());
    }

    public String getCurrentTimeLabel() {
        return detailHeureValueLabel != null && detailHeureValueLabel.getText() != null && !detailHeureValueLabel.getText().isBlank()
                ? detailHeureValueLabel.getText()
                : formatTime(match == null ? null : match.getHeureDebut());
    }

    public String getCurrentVenueLabel() {
        return detailLieuValueLabel != null && detailLieuValueLabel.getText() != null && !detailLieuValueLabel.getText().isBlank()
                ? detailLieuValueLabel.getText()
                : emptyToFallback(match == null ? null : match.getLieu(), "Unknown venue");
    }

    public String getCurrentApiStatusLabel() {
        return currentApiFootballStatus;
    }

    public List<ApiFootballStatisticRow> getCurrentStatistics() {
        return currentStatistics == null ? List.of() : List.copyOf(currentStatistics);
    }

    public List<String> getCurrentGoalHighlights() {
        return currentIncidents == null ? List.of() : currentIncidents.stream()
                .filter(java.util.Objects::nonNull)
                .filter(ApiFootballMatchIncident::isGoal)
                .map(this::buildIncidentHighlight)
                .toList();
    }

    public List<String> getCurrentGoalScorerSummaries() {
        if (currentIncidents == null || currentIncidents.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> scorerMinutes = new LinkedHashMap<>();
        for (ApiFootballMatchIncident incident : currentIncidents) {
            if (incident == null || !incident.isGoal()) {
                continue;
            }

            String scorer = emptyToFallback(incident.playerName(), "Unknown scorer");
            String minute = emptyToFallback(incident.minuteLabel(), "--");
            scorerMinutes.computeIfAbsent(scorer, ignored -> new ArrayList<>()).add(minute);
        }

        return scorerMinutes.entrySet().stream()
                .map(entry -> entry.getKey() + " (" + String.join(", ", entry.getValue()) + ")")
                .toList();
    }

    public List<String> getCurrentAssistSummaries() {
        if (currentIncidents == null || currentIncidents.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> assistMinutes = new LinkedHashMap<>();
        for (ApiFootballMatchIncident incident : currentIncidents) {
            if (incident == null || !incident.isGoal()) {
                continue;
            }

            String assistName = emptyToNull(incident.assistPlayerName());
            if (assistName == null) {
                continue;
            }

            String minute = emptyToFallback(incident.minuteLabel(), "--");
            assistMinutes.computeIfAbsent(assistName, ignored -> new ArrayList<>()).add(minute);
        }

        return assistMinutes.entrySet().stream()
                .map(entry -> entry.getKey() + " (" + String.join(", ", entry.getValue()) + ")")
                .toList();
    }

    public List<String> getCurrentCardHighlights() {
        return currentIncidents == null ? List.of() : currentIncidents.stream()
                .filter(java.util.Objects::nonNull)
                .filter(ApiFootballMatchIncident::isCard)
                .map(this::buildIncidentHighlight)
                .toList();
    }

    public String getCurrentMvpSummary() {
        ApiFootballLineupPlayer mvpPlayer = findCurrentMvpPlayer();
        if (mvpPlayer == null) {
            return null;
        }

        String teamName = lineupContainsPlayer(currentHomeLineup, mvpPlayer)
                ? getCurrentHomeTeamName()
                : lineupContainsPlayer(currentAwayLineup, mvpPlayer) ? getCurrentAwayTeamName() : "this match";
        PlayerIncidentSummary summary = summarizePlayerIncidents(mvpPlayer);

        List<String> parts = new ArrayList<>();
        if (mvpPlayer.rating() != null) {
            parts.add("rating " + formatRating(mvpPlayer.rating()));
        }
        if (summary.goals() > 0) {
            parts.add(summary.goals() + " goal" + (summary.goals() > 1 ? "s" : ""));
        }
        if (summary.assists() > 0) {
            parts.add(summary.assists() + " assist" + (summary.assists() > 1 ? "s" : ""));
        }
        if (summary.yellowCards() > 0) {
            parts.add(summary.yellowCards() + " yellow");
        }
        if (summary.redCards() > 0) {
            parts.add(summary.redCards() + " red");
        }

        StringBuilder builder = new StringBuilder(emptyToFallback(mvpPlayer.playerName(), "Unknown player"))
                .append(" is the current MVP for ")
                .append(teamName);
        if (!parts.isEmpty()) {
            builder.append(" with ").append(String.join(", ", parts));
        }
        builder.append(".");
        return builder.toString();
    }

    @Override
    public String assistantContextSummary() {
        StringBuilder summary = new StringBuilder()
                .append("Current match screen.\n")
                .append("Match: ").append(getCurrentMatchLabel()).append(".\n")
                .append("Competition: ").append(getCurrentCompetitionLabel()).append(".\n")
                .append("Score: ").append(getCurrentScoreLabel()).append(". Status: ").append(getCurrentStatusLabel()).append(".\n")
                .append("Date: ").append(getCurrentDateLabel()).append(" ").append(getCurrentTimeLabel()).append(". Venue: ").append(getCurrentVenueLabel()).append(".\n")
                .append("Home lineup: ").append(getCurrentHomeTeamName()).append(" - ").append(getCurrentHomeLineupMeta()).append(".\n")
                .append("Away lineup: ").append(getCurrentAwayTeamName()).append(" - ").append(getCurrentAwayLineupMeta()).append(".\n");

        String mvpSummary = getCurrentMvpSummary();
        if (mvpSummary != null && !mvpSummary.isBlank()) {
            summary.append("MVP: ").append(mvpSummary).append('\n');
        }

        List<String> goals = getCurrentGoalHighlights();
        if (!goals.isEmpty()) {
            summary.append("Goal events: ").append(String.join(" | ", goals.stream().limit(6).toList())).append(".\n");
        }

        List<ApiFootballStatisticRow> statistics = getCurrentStatistics();
        if (!statistics.isEmpty()) {
            List<String> statSummaries = statistics.stream()
                    .filter(java.util.Objects::nonNull)
                    .limit(6)
                    .map(row -> emptyToFallback(row.label(), "Stat") + ": "
                            + emptyToFallback(row.homeValue(), "N/A") + " - "
                            + emptyToFallback(row.awayValue(), "N/A"))
                    .toList();
            summary.append("Key stats: ").append(String.join(" | ", statSummaries)).append(".\n");
        }

        summary.append("Detailed data status: ").append(emptyToFallback(currentApiFootballStatus, "Unknown"));
        return summary.toString();
    }

    public void openSummaryTabFromAssistant() {
        activeTab = MatchDetailTab.SUMMARY;
        applyActiveTab();
    }

    public void openStatsTabFromAssistant() {
        activeTab = MatchDetailTab.STATS;
        applyActiveTab();
    }

    public void openLineupTabFromAssistant() {
        activeTab = MatchDetailTab.LINEUP;
        applyActiveTab();
    }

    public void openOddsTabFromAssistant() {
        activeTab = MatchDetailTab.ODDS;
        applyActiveTab();
        refreshOddsAsync(false);
    }

    public void openMatchListFromAssistant() {
        openMatchList();
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
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        openMatchList();
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
    private void handleBack() {
        stopLiveRefresh();
        openMatchList();
    }

    @FXML
    private void handleEdit() {
        if (match == null) {
            return;
        }

        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/match-form-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Modifier un match",
                controller -> {
                    if (controller instanceof MatchFormController matchFormController) {
                        matchFormController.configureForUpdate(match);
                    }
                }
        );
    }

    @FXML
    private void handleDelete() {
        if (match == null || match.getId() == null || matchsService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le match \"" + buildMatchLabel(match) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            matchsService.delete(match.getId());
            openMatchList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Suppression", "Impossible de supprimer le match.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleShowSummaryTab() {
        activeTab = MatchDetailTab.SUMMARY;
        applyActiveTab();
    }

    @FXML
    private void handleShowStatsTab() {
        activeTab = MatchDetailTab.STATS;
        applyActiveTab();
    }

    @FXML
    private void handleShowLineupTab() {
        activeTab = MatchDetailTab.LINEUP;
        applyActiveTab();
    }

    @FXML
    private void handleShowVideosTab() {
        activeTab = MatchDetailTab.VIDEOS;
        applyActiveTab();
        ensureFinishedMatchHighlightsLoaded(false);
    }

    @FXML
    private void handleShowOddsTab() {
        activeTab = MatchDetailTab.ODDS;
        applyActiveTab();
        refreshOddsAsync(false);
    }

    @FXML
    private void handleRefreshOdds() {
        refreshOddsAsync(true);
    }

    @FXML
    private void handleRefreshVideos() {
        refreshMatchVideosAsync(true);
    }

    @FXML
    private void handleOpenLocalMp4Demo() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choose a local MP4 highlight");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("MP4 videos", "*.mp4"));
        File file = chooser.showOpenDialog(videosSection == null || videosSection.getScene() == null
                ? null
                : videosSection.getScene().getWindow());
        if (file == null) {
            return;
        }
        playLocalMp4Demo(file);
    }

    @FXML
    private void handleOpenVideoFullscreen() {
        if (localDemoMediaPlayer != null && localDemoMediaView != null && localDemoMediaView.isVisible()) {
            openLocalMp4Fullscreen();
            return;
        }
        if (selectedYouTubeVideo == null) {
            return;
        }

        ChromiumBrowserView.openYouTubePlayerWindow(
                selectedYouTubeVideo.videoId(),
                selectedYouTubeVideo.getWatchUrl(),
                emptyToFallback(selectedYouTubeVideo.title(), "Match video"),
                true
        ).thenAccept(loaded -> {
            if (!loaded) {
                Platform.runLater(() -> showAlert(
                        Alert.AlertType.WARNING,
                        "Video",
                        "Impossible d'ouvrir le lecteur Sport Insight.\n" + ChromiumBrowserView.getLastErrorMessage()
                ));
            }
        });
    }

    @FXML
    private void handleOpenSelectedVideoInApp() {
        if (selectedYouTubeVideo == null) {
            return;
        }
        loadedYouTubeVideo = null;
        loadYouTubeInChromium(selectedYouTubeVideo);
    }

    private void renderMatch() {
        if (match == null || equipeService == null) {
            return;
        }

        try {
            PLAYER_PHOTO_CACHE.clear();
            PLAYER_PHOTO_REQUESTS.clear();
            homeTeam = resolveEquipe(match.getEquipeDomicileId());
            awayTeam = resolveEquipe(match.getEquipeExterieurId());
            String competitionLabel = resolveCompetitionLabel(match.getCompetitionCode());

            competitionBadgeLabel.setText(competitionLabel == null ? "Competition" : competitionLabel);
            detailStatusChipLabel.setText(resolveStatus(match));
            applyDetailStatusStyle(detailStatusChipLabel, match.getStatut());
            detailTitleLabel.setText(buildMatchLabel(match));
            detailSubtitleLabel.setText(buildSubtitle(competitionLabel, homeTeam, awayTeam));
            detailScoreValueLabel.setText(buildScore(match));
            detailDateValueLabel.setText(formatDate(match.getDateMatch()));
            detailHeureValueLabel.setText(formatTime(match.getHeureDebut()));
            updateLiveScoreboard(null, match.getStatut());
            detailLieuValueLabel.setText(emptyToFallback(match.getLieu(), "Non renseigne"));
            detailTypeValueLabel.setText(emptyToFallback(match.getType(), "Non renseigne"));
            detailStatutValueLabel.setText(resolveStatus(match));
            detailIdValueLabel.setText("Match");
            detailCompetitionValueLabel.setText(competitionLabel == null ? "Non renseignee" : competitionLabel);
            detailHomeNameLabel.setText(homeTeam == null ? "Equipe domicile" : emptyToFallback(homeTeam.getNom(), "Equipe domicile"));
            detailAwayNameLabel.setText(awayTeam == null ? "Equipe exterieur" : emptyToFallback(awayTeam.getNom(), "Equipe exterieur"));
            lineupHomeTeamLabel.setText(detailHomeNameLabel.getText());
            lineupAwayTeamLabel.setText(detailAwayNameLabel.getText());
            summaryHomeTeamLabel.setText(detailHomeNameLabel.getText());
            summaryAwayTeamLabel.setText(detailAwayNameLabel.getText());
            statsHomeTeamLabel.setText(detailHomeNameLabel.getText());
            statsAwayTeamLabel.setText(detailAwayNameLabel.getText());
            lineupDomicileMetaLabel.setText("Formation indisponible");
            lineupExterieurMetaLabel.setText("Formation indisponible");
            currentIncidents = List.of();
            currentHomeLineup = null;
            currentAwayLineup = null;
            currentStatistics = List.of();
            currentMvpPlayerId = null;
            currentMvpPlayerNameKey = null;
            resetMatchVideoUiForNewMatch();
            resetOddsUiForNewMatch();
            renderSummary(List.of());
            renderStoredLineups();
            renderStatistics(List.of());

            updateLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, homeTeam, "D");
            updateLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, awayTeam, "E");
            refreshFollowButtons();
            renderCachedInsights();
            ensureFinishedMatchHighlightsLoaded(false);
            refreshOddsAsync(false);
            refreshLiveMatchAsync(true);
            startLiveRefreshIfNeeded();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les informations du match.\n" + e.getMessage());
        }
    }

    private Equipe resolveEquipe(Integer equipeId) throws SQLException {
        return equipeId == null || equipeService == null ? null : equipeService.getById(equipeId);
    }

    private void renderCachedInsights() {
        if (apiFootballInsightsService == null || match == null) {
            showApiFootballStatus("Les sources detaillees sont indisponibles pour le moment.", "status-warning");
            return;
        }

        ApiFootballMatchDetails cached = apiFootballInsightsService.readCachedMatchDetails(match);
        if (cached != null && (cached.hasLineups() || cached.hasStatistics() || cached.hasIncidents())) {
            renderApiFootballInsights(cached);
            showApiFootballStatus("Donnees detaillees en cache affichees.", "status-success");
            return;
        }

        showApiFootballStatus("Chargement des stats et compositions...", "status-muted");
    }

    private void refreshLiveMatchAsync(boolean forceDetailedRefresh) {
        if (match == null || apiFootballInsightsService == null) {
            return;
        }
        if (apiRefreshInProgress && !forceDetailedRefresh) {
            return;
        }

        long requestId = apiRequestSequence.incrementAndGet();
        apiRefreshInProgress = true;
        if (forceDetailedRefresh) {
            showApiFootballStatus("Chargement des stats et compositions...", "status-muted");
        }

        Task<LiveRefreshPayload> task = new Task<>() {
            @Override
            protected LiveRefreshPayload call() {
                ApiFootballFixtureSnapshot snapshot = null;
                ApiFootballMatchDetails details = null;
                Throwable error = null;

                try {
                    snapshot = apiFootballInsightsService.refreshFixtureSnapshot(match, homeTeam, awayTeam);
                } catch (Throwable throwable) {
                    error = throwable;
                }

                boolean shouldLoadDetails = forceDetailedRefresh || shouldRefreshDetailedInsights(snapshot);
                if (shouldLoadDetails) {
                    try {
                        details = apiFootballInsightsService.loadMatchDetails(match, homeTeam, awayTeam);
                    } catch (Throwable throwable) {
                        if (error == null) {
                            error = throwable;
                        }
                    }
                }
                return new LiveRefreshPayload(snapshot, details, error);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != apiRequestSequence.get()) {
                return;
            }
            apiRefreshInProgress = false;
            LiveRefreshPayload payload = task.getValue();
            if (payload == null) {
                showApiFootballStatus("Aucune donnee detaillee disponible pour ce match.", "status-warning");
                return;
            }

            applyFixtureSnapshot(payload.snapshot());
            refreshOddsAsync(false);
            if (payload.details() != null) {
                renderApiFootballInsights(payload.details());
            }
            if (isFinishedMatch(match)) {
                ensureFinishedMatchHighlightsLoaded(false);
            } else if (activeTab == MatchDetailTab.VIDEOS) {
                refreshMatchVideosAsync(false);
            }

            if (payload.error() != null && payload.details() == null && payload.snapshot() == null) {
                showApiFootballStatus(shortError(payload.error()), "status-warning");
                return;
            }

            if (payload.snapshot() != null && payload.snapshot().isLive()) {
                showApiFootballStatus("Suivi live actif : " + payload.snapshot().effectiveStatusLabel(), "status-success");
            } else if (payload.details() != null) {
                showApiFootballStatus("Donnees detaillees synchronisees pour ce match.", "status-success");
            } else if (payload.snapshot() != null) {
                showApiFootballStatus("Statut du match synchronise : " + payload.snapshot().effectiveStatusLabel(), "status-success");
            } else {
                showApiFootballStatus("Aucune composition detaillee disponible pour ce match.", "status-warning");
            }
        });

        task.setOnFailed(event -> {
            if (requestId != apiRequestSequence.get()) {
                return;
            }
            apiRefreshInProgress = false;
            Throwable throwable = task.getException();
            showApiFootballStatus(shortError(throwable), "status-warning");
        });

        task.setOnCancelled(event -> {
            if (requestId == apiRequestSequence.get()) {
                apiRefreshInProgress = false;
                showApiFootballStatus("Chargement des stats annule.", "status-warning");
            }
        });

        API_EXECUTOR.execute(task);
    }

    private void refreshOddsAsync(boolean force) {
        if (match == null || apiFootballOddsService == null) {
            return;
        }
        if (oddsRefreshInProgress && !force) {
            return;
        }

        long requestId = oddsRequestSequence.incrementAndGet();
        Matchs requestedMatch = match;
        String requestedHomeName = detailHomeNameLabel == null ? "Domicile" : detailHomeNameLabel.getText();
        String requestedAwayName = detailAwayNameLabel == null ? "Exterieur" : detailAwayNameLabel.getText();
        oddsRefreshInProgress = true;
        setOddsLoading(true);
        if (oddsMarketsContainer != null && oddsMarketsContainer.getChildren().isEmpty()) {
            renderOddsPlaceholder("Chargement des cotes API-Football...");
        }
        if (oddsMessageLabel != null) {
            oddsMessageLabel.setText("Chargement des cotes API-Football...");
        }

        Task<ApiFootballOddsSnapshot> task = new Task<>() {
            @Override
            protected ApiFootballOddsSnapshot call() throws Exception {
                return apiFootballOddsService.loadMatchOdds(requestedMatch, requestedHomeName, requestedAwayName);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != oddsRequestSequence.get()) {
                return;
            }
            oddsRefreshInProgress = false;
            setOddsLoading(false);
            renderOddsSnapshot(task.getValue());
        });

        task.setOnFailed(event -> {
            if (requestId != oddsRequestSequence.get()) {
                return;
            }
            oddsRefreshInProgress = false;
            setOddsLoading(false);
            Throwable throwable = task.getException();
            renderOddsError(shortError(throwable));
        });

        task.setOnCancelled(event -> {
            if (requestId == oddsRequestSequence.get()) {
                oddsRefreshInProgress = false;
                setOddsLoading(false);
                renderOddsError("Chargement des cotes annule.");
            }
        });

        API_EXECUTOR.execute(task);
    }

    private void resetOddsUiForNewMatch() {
        oddsRequestSequence.incrementAndGet();
        oddsRefreshInProgress = false;
        setOddsLoading(false);
        if (oddsStateLabel != null) {
            oddsStateLabel.setText("ODDS");
            applyOddsStateStyle("pending");
        }
        if (oddsSourceLabel != null) {
            oddsSourceLabel.setText("API-Football free API");
        }
        if (oddsUpdatedLabel != null) {
            oddsUpdatedLabel.setText("Derniere mise a jour inconnue");
        }
        if (oddsMessageLabel != null) {
            oddsMessageLabel.setText("Les cotes seront chargees depuis API-Football.");
        }
        renderOddsPlaceholder("Les cotes API apparaitront ici.");
        renderOddsGesture(null);
    }

    private void setOddsLoading(boolean loading) {
        if (refreshOddsButton == null) {
            return;
        }
        refreshOddsButton.setDisable(loading);
        refreshOddsButton.setText(loading ? "Loading..." : "Refresh odds");
    }

    private void renderOddsSnapshot(ApiFootballOddsSnapshot snapshot) {
        if (snapshot == null) {
            renderOddsError("Aucune reponse API-Football pour les cotes.");
            return;
        }

        if (oddsStateLabel != null) {
            oddsStateLabel.setText(emptyToFallback(snapshot.stateLabel(), "ODDS"));
            applyOddsStateStyle(snapshot.locked() ? "closed" : (snapshot.statusLabel() == null ? "pending" : snapshot.statusLabel()));
        }
        if (oddsSourceLabel != null) {
            oddsSourceLabel.setText(emptyToFallback(snapshot.sourceLabel(), "API-Football odds"));
        }
        if (oddsUpdatedLabel != null) {
            oddsUpdatedLabel.setText(emptyToFallback(snapshot.updatedAt(), "Derniere mise a jour inconnue"));
        }
        if (oddsMessageLabel != null) {
            oddsMessageLabel.setText(emptyToFallback(snapshot.message(), "Cotes indisponibles pour ce match."));
        }

        if (oddsMarketsContainer != null) {
            oddsMarketsContainer.getChildren().clear();
            if (!snapshot.hasMarkets()) {
                renderOddsPlaceholder(emptyToFallback(snapshot.message(), "Aucune cote API disponible pour ce match."));
            } else {
                for (ApiFootballOddsSnapshot.Market market : snapshot.markets()) {
                    oddsMarketsContainer.getChildren().add(buildOddsMarketCard(market, snapshot.locked()));
                }
            }
        }
        renderOddsGesture(snapshot.gestureInsight());
    }

    private void renderOddsError(String message) {
        if (oddsStateLabel != null) {
            oddsStateLabel.setText("ODDS");
            applyOddsStateStyle("unavailable");
        }
        if (oddsSourceLabel != null) {
            oddsSourceLabel.setText("API-Football odds");
        }
        if (oddsUpdatedLabel != null) {
            oddsUpdatedLabel.setText("Derniere mise a jour inconnue");
        }
        if (oddsMessageLabel != null) {
            oddsMessageLabel.setText(emptyToFallback(message, "Cotes indisponibles."));
        }
        renderOddsPlaceholder(emptyToFallback(message, "Cotes indisponibles."));
        renderOddsGesture(new ApiFootballOddsSnapshot.GestureInsight(
                "API watch",
                "Relancez les cotes apres la synchronisation du match ou quand le quota API-Football est disponible.",
                "Refresh odds",
                "Sync fixture",
                40,
                "pending"
        ));
    }

    private void renderOddsPlaceholder(String message) {
        if (oddsMarketsContainer == null) {
            return;
        }
        oddsMarketsContainer.getChildren().clear();
        Label placeholder = new Label(message);
        placeholder.setWrapText(true);
        placeholder.getStyleClass().add("odds-placeholder");
        oddsMarketsContainer.getChildren().add(placeholder);
    }

    private VBox buildOddsMarketCard(ApiFootballOddsSnapshot.Market market, boolean locked) {
        VBox card = new VBox(8);
        card.getStyleClass().add("odds-market-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(emptyToFallback(market == null ? null : market.name(), "Market"));
        title.getStyleClass().add("odds-market-title");
        Label description = new Label(emptyToFallback(market == null ? null : market.description(), "API"));
        description.getStyleClass().add("odds-market-description");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lockLabel = new Label(locked ? "Closed" : "Open");
        lockLabel.getStyleClass().addAll("odds-market-state", locked ? "odds-market-state-closed" : "odds-market-state-open");
        header.getChildren().addAll(title, description, spacer, lockLabel);

        VBox rows = new VBox(0);
        rows.getStyleClass().add("odds-table");
        if (market != null && market.rows() != null) {
            for (ApiFootballOddsSnapshot.BookmakerRow row : market.rows()) {
                rows.getChildren().add(buildOddsBookmakerRow(row, locked));
            }
        }

        card.getChildren().addAll(header, rows);
        return card;
    }

    private HBox buildOddsBookmakerRow(ApiFootballOddsSnapshot.BookmakerRow row, boolean locked) {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("odds-bookmaker-row");

        Label bookmakerLabel = new Label(emptyToFallback(row == null ? null : row.bookmaker(), "Bookmaker"));
        bookmakerLabel.setMinWidth(150);
        bookmakerLabel.setPrefWidth(150);
        bookmakerLabel.getStyleClass().add("odds-bookmaker-label");

        HBox selectionsBox = new HBox(8);
        selectionsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(selectionsBox, Priority.ALWAYS);

        if (row != null && row.selections() != null) {
            for (ApiFootballOddsSnapshot.Selection selection : row.selections()) {
                selectionsBox.getChildren().add(buildOddsSelectionCell(selection, locked));
            }
        }

        container.getChildren().addAll(bookmakerLabel, selectionsBox);
        return container;
    }

    private VBox buildOddsSelectionCell(ApiFootballOddsSnapshot.Selection selection, boolean locked) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.CENTER);
        cell.setMinWidth(92);
        cell.setPrefWidth(108);
        cell.getStyleClass().add("odds-selection-cell");
        if (locked || (selection != null && selection.suspended())) {
            cell.getStyleClass().add("odds-selection-locked");
        }
        String trend = selection == null ? "neutral" : emptyToFallback(selection.trend(), "neutral").toLowerCase(Locale.ROOT);
        cell.getStyleClass().add("odds-trend-" + trend);

        Label label = new Label(selection == null ? "-" : emptyToFallback(selection.label(), "-"));
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.getStyleClass().add("odds-selection-label");

        Label odd = new Label(selection == null ? "-" : emptyToFallback(selection.odd(), "-"));
        odd.getStyleClass().add("odds-selection-odd");

        Label signal = new Label(oddsTrendLabel(trend, locked || (selection != null && selection.suspended())));
        signal.getStyleClass().add("odds-selection-signal");

        cell.getChildren().addAll(label, odd, signal);
        return cell;
    }

    private String oddsTrendLabel(String trend, boolean locked) {
        if (locked || "locked".equals(trend)) {
            return "LOCK";
        }
        if ("up".equals(trend)) {
            return "UP";
        }
        if ("down".equals(trend)) {
            return "DOWN";
        }
        return "STABLE";
    }

    private void renderOddsGesture(ApiFootballOddsSnapshot.GestureInsight insight) {
        String title = insight == null ? "API watch" : emptyToFallback(insight.title(), "API watch");
        String body = insight == null
                ? "Le panneau suit l'etat du match et recharge les marches disponibles via API-Football."
                : emptyToFallback(insight.body(), "Le panneau suit l'etat du match et recharge les marches disponibles via API-Football.");
        if (oddsGestureTitleLabel != null) {
            oddsGestureTitleLabel.setText(title);
        }
        if (oddsGestureBodyLabel != null) {
            oddsGestureBodyLabel.setText(body);
        }
        if (oddsGesturePrimaryLabel != null) {
            oddsGesturePrimaryLabel.setText(insight == null ? "Refresh odds" : emptyToFallback(insight.primaryAction(), "Refresh odds"));
        }
        if (oddsGestureSecondaryLabel != null) {
            oddsGestureSecondaryLabel.setText(insight == null ? "Sync fixture" : emptyToFallback(insight.secondaryAction(), "Sync fixture"));
        }
    }

    private void applyOddsStateStyle(String state) {
        if (oddsStateLabel == null) {
            return;
        }
        oddsStateLabel.getStyleClass().removeAll(
                "odds-state-live",
                "odds-state-programmed",
                "odds-state-closed",
                "odds-state-unavailable",
                "odds-state-pending"
        );
        String normalized = lowercase(state);
        String styleClass;
        if (normalized != null && normalized.contains("live")) {
            styleClass = "odds-state-live";
        } else if (normalized != null && (normalized.contains("finished") || normalized.contains("closed"))) {
            styleClass = "odds-state-closed";
        } else if (normalized != null && normalized.contains("unavailable")) {
            styleClass = "odds-state-unavailable";
        } else if (normalized != null && (normalized.contains("programme") || normalized.contains("programmed") || normalized.contains("pre"))) {
            styleClass = "odds-state-programmed";
        } else {
            styleClass = "odds-state-pending";
        }
        oddsStateLabel.getStyleClass().add(styleClass);
    }

    private void refreshMatchVideosAsync(boolean force) {
        if (match == null || youtubeService == null) {
            return;
        }
        if (matchVideoRefreshInProgress) {
            return;
        }
        if (!force && matchVideoLookupCompleted) {
            return;
        }

        if (!isFinishedMatch(match)) {
            showUnfinishedMatchVideoUnavailable();
            return;
        }

        long requestId = videoRequestSequence.incrementAndGet();
        Matchs requestedMatch = match;
        Equipe requestedHomeTeam = homeTeam;
        Equipe requestedAwayTeam = awayTeam;
        matchVideoRefreshInProgress = true;
        matchVideoLookupCompleted = false;
        setMatchVideoLoading(true);
        showMatchVideoStatus("Searching YouTube highlights...");
        if (youtubeVideos.isEmpty()) {
            showMatchVideoPlaceholder(
                    "Searching YouTube highlights...",
                    "Checking YouTube for a playable highlight."
            );
        }

        Task<List<YouTubeVideo>> task = new Task<>() {
            @Override
            protected List<YouTubeVideo> call() throws Exception {
                return youtubeService.searchInAppHighlights(requestedMatch, requestedHomeTeam, requestedAwayTeam, force);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != videoRequestSequence.get()) {
                return;
            }

            matchVideoRefreshInProgress = false;
            setMatchVideoLoading(false);
            List<YouTubeVideo> videos = task.getValue() == null ? List.of() : task.getValue();
            applyMatchVideoResults(videos, false);
        });

        task.setOnFailed(event -> {
            if (requestId != videoRequestSequence.get()) {
                return;
            }

            matchVideoRefreshInProgress = false;
            matchVideoLookupCompleted = true;
            setMatchVideoLoading(false);
            youtubeVideos.clear();
            selectedYouTubeVideo = null;
            if (youtubeVideoListView != null) {
                youtubeVideoListView.getSelectionModel().clearSelection();
            }
            resetSelectedMatchVideoLabels();
            showMatchVideoStatus(YouTubeService.API_ERROR_MESSAGE);
            showMatchVideoPlaceholder(
                    "YouTube API error. Check API key or quota.",
                    "Set the YOUTUBE_API_KEY environment variable and make sure the quota is available."
            );
        });

        task.setOnCancelled(event -> {
            if (requestId != videoRequestSequence.get()) {
                return;
            }
            matchVideoRefreshInProgress = false;
            setMatchVideoLoading(false);
        });

        VIDEO_EXECUTOR.execute(task);
    }

    private boolean shouldRefreshDetailedInsights(ApiFootballFixtureSnapshot snapshot) {
        if (snapshot != null) {
            if (snapshot.isLive()) {
                return true;
            }
            LocalDateTime kickoff = snapshot.kickoffAt();
            if (kickoff != null) {
                LocalDateTime now = LocalDateTime.now();
                return !now.isBefore(kickoff.minusMinutes(1)) && now.isBefore(kickoff.plusHours(4));
            }
        }

        if (match == null || match.getDateMatch() == null) {
            return false;
        }
        LocalDateTime kickoff = match.getHeureDebut() == null
                ? match.getDateMatch().atStartOfDay()
                : match.getDateMatch().atTime(match.getHeureDebut());
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(kickoff.minusMinutes(1)) && now.isBefore(kickoff.plusHours(4));
    }

    private void applyFixtureSnapshot(ApiFootballFixtureSnapshot snapshot) {
        if (snapshot == null || match == null) {
            return;
        }

        if (snapshot.kickoffAt() != null) {
            match.setDateMatch(snapshot.kickoffAt().toLocalDate());
            match.setHeureDebut(snapshot.kickoffAt().toLocalTime().withSecond(0).withNano(0));
        }
        String statusLabel = snapshot.effectiveStatusLabel();
        match.setStatut(statusLabel);
        if (snapshot.homeScore() != null) {
            match.setScoreEquipeDomicile(snapshot.homeScore());
        }
        if (snapshot.awayScore() != null) {
            match.setScoreEquipeExterieur(snapshot.awayScore());
        }
        match.setApiFootballId(snapshot.fixtureId());

        detailStatusChipLabel.setText(statusLabel);
        applyDetailStatusStyle(detailStatusChipLabel, snapshot.matchStatus());
        detailStatutValueLabel.setText(statusLabel);
        detailScoreValueLabel.setText(buildScore(match));
        detailDateValueLabel.setText(formatDate(match.getDateMatch()));
        detailHeureValueLabel.setText(formatTime(match.getHeureDebut()));
        if (youtubeMatchInfoLabel != null) {
            youtubeMatchInfoLabel.setText(buildYouTubeMatchInfo());
        }
        updateLiveScoreboard(snapshot, statusLabel);
    }

    private void resetMatchVideoUiForNewMatch() {
        videoRequestSequence.incrementAndGet();
        youtubeVideos.clear();
        selectedYouTubeVideo = null;
        loadedYouTubeVideo = null;
        matchVideoLookupCompleted = false;
        matchVideoRefreshInProgress = false;
        setMatchVideoLoading(false);

        if (youtubeMatchInfoLabel != null) {
            youtubeMatchInfoLabel.setText(buildYouTubeMatchInfo());
        }
        showMatchVideoStatus("Click Load Highlights to search YouTube.");
        showMatchVideoPlaceholder(
                "No video selected",
                "YouTube highlights will appear here after the match is finished."
        );
        if (selectedVideoTitleLabel != null) {
            selectedVideoTitleLabel.setText("No video selected");
        }
        if (selectedVideoMetaLabel != null) {
            selectedVideoMetaLabel.setText("YouTube videos play inside the embedded Chromium player.");
        }
        if (fullscreenVideoButton != null) {
            fullscreenVideoButton.setDisable(true);
        }
        setInAppYouTubePlayerButtonVisible(false);
        stopLocalMp4Demo();
    }

    private void ensureFinishedMatchHighlightsLoaded(boolean force) {
        if (match == null || youtubeService == null) {
            return;
        }
        if (!isFinishedMatch(match)) {
            showUnfinishedMatchVideoUnavailable();
            return;
        }
        if (!force && applyCachedMatchVideoResults()) {
            return;
        }
        refreshMatchVideosAsync(force);
    }

    private boolean applyCachedMatchVideoResults() {
        Optional<List<YouTubeVideo>> cachedVideos = youtubeService.readCachedInAppHighlights(match, homeTeam, awayTeam);
        if (cachedVideos.isEmpty()) {
            return false;
        }
        applyMatchVideoResults(cachedVideos.get(), true);
        return true;
    }

    private void applyMatchVideoResults(List<YouTubeVideo> videos, boolean fromCache) {
        List<YouTubeVideo> playableVideos = videos == null ? List.of() : List.copyOf(videos);
        matchVideoLookupCompleted = true;
        youtubeVideos.setAll(playableVideos);

        if (playableVideos.isEmpty()) {
            selectedYouTubeVideo = null;
            if (youtubeVideoListView != null) {
                youtubeVideoListView.getSelectionModel().clearSelection();
            }
            resetSelectedMatchVideoLabels();
            showMatchVideoStatus(fromCache
                    ? "No cached in-app highlight is available for this match."
                    : "No in-app highlight available for this match.");
            showMatchVideoPlaceholder(
                    "No in-app highlight available for this match.",
                    "YouTube did not return a playable highlight for this finished match."
            );
            return;
        }

        String prefix = fromCache ? "Cached " : "";
        showMatchVideoStatus(prefix + playableVideos.size()
                + " YouTube highlight" + (playableVideos.size() > 1 ? "s" : "")
                + " ready for in-app playback.");

        YouTubeVideo preferredSelection = selectedYouTubeVideo == null
                ? playableVideos.get(0)
                : playableVideos.stream()
                .filter(video -> Objects.equals(video, selectedYouTubeVideo))
                .findFirst()
                .orElse(playableVideos.get(0));

        if (youtubeVideoListView != null) {
            youtubeVideoListView.getSelectionModel().select(preferredSelection);
        } else {
            selectedYouTubeVideo = preferredSelection;
            renderSelectedMatchVideo();
        }
    }

    private void showUnfinishedMatchVideoUnavailable() {
        matchVideoLookupCompleted = false;
        matchVideoRefreshInProgress = false;
        setMatchVideoLoading(false);
        youtubeVideos.clear();
        selectedYouTubeVideo = null;
        if (youtubeVideoListView != null) {
            youtubeVideoListView.getSelectionModel().clearSelection();
        }
        resetSelectedMatchVideoLabels();
        showMatchVideoStatus("This match has not finished yet. Highlights are not available.");
        showMatchVideoPlaceholder(
                "This match has not finished yet.",
                "Highlights are available after full time when YouTube has a playable upload."
        );
    }

    private void resetSelectedMatchVideoLabels() {
        if (selectedVideoTitleLabel != null) {
            selectedVideoTitleLabel.setText("No video selected");
        }
        if (selectedVideoMetaLabel != null) {
            selectedVideoMetaLabel.setText("YouTube videos play inside the embedded Chromium player.");
        }
        if (fullscreenVideoButton != null) {
            fullscreenVideoButton.setDisable(true);
        }
        setInAppYouTubePlayerButtonVisible(false);
    }

    private void setMatchVideoLoading(boolean loading) {
        if (refreshVideosButton != null) {
            refreshVideosButton.setDisable(loading);
            refreshVideosButton.setText(loading ? "Loading..." : "Load Highlights");
        }
    }

    private void renderSelectedMatchVideo() {
        stopLocalMp4Demo();
        if (selectedVideoTitleLabel != null) {
            selectedVideoTitleLabel.setText(selectedYouTubeVideo == null
                    ? "No video selected"
                    : emptyToFallback(selectedYouTubeVideo.title(), "YouTube highlight"));
        }
        if (selectedVideoMetaLabel != null) {
            selectedVideoMetaLabel.setText(selectedYouTubeVideo == null
                    ? "YouTube videos play inside the embedded Chromium player."
                    : emptyToFallback(selectedYouTubeVideo.channelTitle(), "YouTube"));
        }
        if (fullscreenVideoButton != null) {
            fullscreenVideoButton.setDisable(selectedYouTubeVideo == null);
        }
        setInAppYouTubePlayerButtonVisible(false);

        if (activeTab != MatchDetailTab.VIDEOS) {
            unloadMatchVideoPlayer();
            return;
        }

        if (selectedYouTubeVideo == null) {
            showMatchVideoPlaceholder(
                    youtubeVideos.isEmpty() ? "No in-app highlight available for this match." : "No video selected",
                    youtubeVideos.isEmpty() ? "Load highlights to search YouTube videos for this match." : "Select a highlight from the list."
            );
            return;
        }

        setInAppYouTubePlayerButtonVisible(true);
        if (Objects.equals(selectedYouTubeVideo, loadedYouTubeVideo)) {
            if (matchVideoEmptyStateBox != null) {
                matchVideoEmptyStateBox.setManaged(false);
                matchVideoEmptyStateBox.setVisible(false);
            }
            return;
        }

        loadYouTubeInChromium(selectedYouTubeVideo);
        if (matchVideoEmptyStateBox != null) {
            matchVideoEmptyStateBox.setManaged(false);
            matchVideoEmptyStateBox.setVisible(false);
        }
    }

    private void loadYouTubeInChromium(YouTubeVideo video) {
        if (video == null) {
            return;
        }
        loadedYouTubeVideo = video;
        showInAppPlayerWindowMessage(
                "Opening Sport Insight player",
                "Your highlight is opening in a dedicated in-app Chromium window."
        );
        showMatchVideoStatus("Opening YouTube highlight in the Sport Insight player...");

        ChromiumBrowserView.openYouTubePlayerWindow(
                video.videoId(),
                video.getWatchUrl(),
                emptyToFallback(video.title(), "Sport Insight Highlights"),
                false
        ).thenAccept(loaded -> Platform.runLater(() -> {
            if (!Objects.equals(video, selectedYouTubeVideo)) {
                return;
            }
            if (loaded) {
                showInAppPlayerWindowMessage(
                        "Playing in Sport Insight player",
                        "The video is playing in a dedicated app window. If you closed it, click Reload in-app player."
                );
                showMatchVideoStatus("Playing in the Sport Insight in-app player.");
                return;
            }
            showWebViewUnsupportedFallback(ChromiumBrowserView.getLastErrorMessage());
        }));
    }

    private void showChromiumPlayer() {
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(true);
            chromiumPlayerHost.setVisible(true);
        }
        if (matchVideoWebView != null) {
            matchVideoWebView.setManaged(false);
            matchVideoWebView.setVisible(false);
        }
        if (localDemoMediaView != null) {
            localDemoMediaView.setManaged(false);
            localDemoMediaView.setVisible(false);
        }
    }

    private void showInAppPlayerWindowMessage(String title, String body) {
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(false);
            chromiumPlayerHost.setVisible(false);
        }
        if (localDemoMediaView != null) {
            localDemoMediaView.setManaged(false);
            localDemoMediaView.setVisible(false);
        }
        if (matchVideoEmptyStateBox != null) {
            matchVideoEmptyStateBox.setManaged(false);
            matchVideoEmptyStateBox.setVisible(false);
        }
        if (matchVideoWebView != null) {
            matchVideoWebView.getEngine().loadContent("""
                    <html>
                      <body style='margin:0;background:#050915;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;text-align:center;'>
                        <div>
                          <h2>%s</h2>
                          <p style='max-width:680px;color:#c8d5e8;font-size:16px;'>%s</p>
                        </div>
                      </body>
                    </html>
                    """.formatted(escapeHtml(title), escapeHtml(body)), "text/html");
            matchVideoWebView.setManaged(true);
            matchVideoWebView.setVisible(true);
        }
    }

    private void playLocalMp4Demo(File file) {
        if (file == null) {
            return;
        }

        stopLocalMp4Demo();
        currentLocalMp4File = file;
        loadedYouTubeVideo = null;
        if (chromiumBrowserView != null) {
            chromiumBrowserView.clear();
        }
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(false);
            chromiumPlayerHost.setVisible(false);
        }
        if (matchVideoWebView != null) {
            matchVideoWebView.getEngine().loadContent("<html><body style='margin:0;background:#0b1220;'></body></html>");
            matchVideoWebView.setManaged(false);
            matchVideoWebView.setVisible(false);
        }
        if (matchVideoEmptyStateBox != null) {
            matchVideoEmptyStateBox.setManaged(false);
            matchVideoEmptyStateBox.setVisible(false);
        }
        setInAppYouTubePlayerButtonVisible(false);

        try {
            Media media = new Media(file.toURI().toString());
            localDemoMediaPlayer = new MediaPlayer(media);
            localDemoMediaPlayer.setOnError(() -> {
                String error = localDemoMediaPlayer.getError() == null
                        ? "Unknown media error."
                        : localDemoMediaPlayer.getError().getMessage();
                showMatchVideoPlaceholder("Local MP4 unavailable", error);
                showMatchVideoStatus("Local MP4 demo could not be played.");
            });
            localDemoMediaView.setMediaPlayer(localDemoMediaPlayer);
            localDemoMediaView.setManaged(true);
            localDemoMediaView.setVisible(true);
            if (selectedVideoTitleLabel != null) {
                selectedVideoTitleLabel.setText("Local MP4 demo");
            }
            if (selectedVideoMetaLabel != null) {
                selectedVideoMetaLabel.setText(file.getName());
            }
            if (fullscreenVideoButton != null) {
                fullscreenVideoButton.setDisable(false);
            }
            showMatchVideoStatus("Playing local MP4 demo.");
            localDemoMediaPlayer.play();
        } catch (RuntimeException e) {
            currentLocalMp4File = null;
            showMatchVideoPlaceholder("Local MP4 unavailable", e.getMessage());
            showMatchVideoStatus("Local MP4 demo could not be played.");
        }
    }

    private void stopLocalMp4Demo() {
        if (localDemoMediaPlayer != null) {
            localDemoMediaPlayer.stop();
            localDemoMediaPlayer.dispose();
            localDemoMediaPlayer = null;
        }
        currentLocalMp4File = null;
        if (localDemoMediaView != null) {
            localDemoMediaView.setMediaPlayer(null);
            localDemoMediaView.setManaged(false);
            localDemoMediaView.setVisible(false);
        }
    }

    private void openLocalMp4Fullscreen() {
        if (currentLocalMp4File == null || !currentLocalMp4File.isFile()) {
            return;
        }

        try {
            MediaPlayer player = new MediaPlayer(new Media(currentLocalMp4File.toURI().toString()));
            MediaView mediaView = new MediaView(player);
            mediaView.setPreserveRatio(true);
            StackPane root = new StackPane(mediaView);
            root.setStyle("-fx-background-color: #000000;");
            mediaView.fitWidthProperty().bind(root.widthProperty());
            mediaView.fitHeightProperty().bind(root.heightProperty());

            Stage stage = new Stage();
            stage.setTitle(currentLocalMp4File.getName());
            stage.setScene(new Scene(root, 1280, 720, Color.BLACK));
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("Press ESC to leave full screen");
            stage.setOnHidden(event -> {
                player.stop();
                player.dispose();
            });
            stage.show();
            player.play();
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.WARNING, "Video", "Impossible d'ouvrir la video locale.\n" + e.getMessage());
        }
    }

    private void showWebViewUnsupportedFallback(String detail) {
        String safeDetail = escapeHtml(emptyToFallback(detail, "No JCEF error details were reported."));
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(false);
            chromiumPlayerHost.setVisible(false);
        }
        if (matchVideoWebView != null) {
            matchVideoWebView.getEngine().loadContent("""
                    <html>
                      <body style='margin:0;background:#0b1220;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;text-align:center;'>
                        <div>
                          <h2>Embedded Chromium is unavailable.</h2>
                          <p>In-app YouTube playback needs the JCEF Chromium runtime.</p>
                          <p style='max-width:720px;color:#93a4bd;font-size:14px;'>%s</p>
                        </div>
                      </body>
                    </html>
                    """.formatted(safeDetail), "text/html");
            matchVideoWebView.setManaged(true);
            matchVideoWebView.setVisible(true);
        }
        if (matchVideoEmptyStateBox != null) {
            matchVideoEmptyStateBox.setManaged(false);
            matchVideoEmptyStateBox.setVisible(false);
        }
        showMatchVideoStatus("Embedded Chromium is unavailable: " + emptyToFallback(detail, "JCEF startup failed."));
        setInAppYouTubePlayerButtonVisible(false);
    }

    private void showMatchVideoPlaceholder(String title, String body) {
        stopLocalMp4Demo();
        if (matchVideoEmptyTitleLabel != null) {
            matchVideoEmptyTitleLabel.setText(emptyToFallback(title, "Video unavailable"));
        }
        if (matchVideoEmptyBodyLabel != null) {
            matchVideoEmptyBodyLabel.setText(emptyToFallback(body, "No in-app highlight available for this match."));
        }
        if (matchVideoWebView != null) {
            matchVideoWebView.getEngine().loadContent("<html><body style='margin:0;background:#0b1220;'></body></html>");
            matchVideoWebView.setManaged(false);
            matchVideoWebView.setVisible(false);
        }
        loadedYouTubeVideo = null;
        setInAppYouTubePlayerButtonVisible(false);
        if (chromiumBrowserView != null) {
            chromiumBrowserView.clear();
        }
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(false);
            chromiumPlayerHost.setVisible(false);
        }
        if (localDemoMediaView != null) {
            localDemoMediaView.setManaged(false);
            localDemoMediaView.setVisible(false);
        }
        if (matchVideoEmptyStateBox != null) {
            matchVideoEmptyStateBox.setManaged(true);
            matchVideoEmptyStateBox.setVisible(true);
        }
    }

    private void unloadMatchVideoPlayer() {
        if (matchVideoWebView != null) {
            matchVideoWebView.getEngine().loadContent("<html><body style='margin:0;background:#0b1220;'></body></html>");
            matchVideoWebView.setManaged(false);
            matchVideoWebView.setVisible(false);
        }
        loadedYouTubeVideo = null;
        setInAppYouTubePlayerButtonVisible(false);
        if (chromiumBrowserView != null) {
            chromiumBrowserView.clear();
        }
        if (chromiumPlayerHost != null) {
            chromiumPlayerHost.setManaged(false);
            chromiumPlayerHost.setVisible(false);
        }
        stopLocalMp4Demo();
    }

    private void showMatchVideoStatus(String message) {
        if (matchVideoStatusLabel != null) {
            matchVideoStatusLabel.setText(emptyToFallback(message, "Videos du match"));
        }
    }

    private String buildYouTubeMatchInfo() {
        return emptyToFallback(buildMatchLabel(match), "Match")
                + " | Status: "
                + emptyToFallback(resolveStatus(match), "Unknown");
    }

    private boolean isFinishedMatch(Matchs value) {
        return YouTubeService.isFinishedStatus(value == null ? null : value.getStatut());
    }

    private void setInAppYouTubePlayerButtonVisible(boolean visible) {
        if (inAppYouTubePlayerButton != null) {
            inAppYouTubePlayerButton.setText("Reload in-app player");
            inAppYouTubePlayerButton.setDisable(selectedYouTubeVideo == null);
            inAppYouTubePlayerButton.setManaged(visible);
            inAppYouTubePlayerButton.setVisible(visible);
        }
    }

    private void updateLiveScoreboard(ApiFootballFixtureSnapshot snapshot, String rawStatus) {
        if (detailScoreValueLabel == null || detailLivePhaseLabel == null || detailLiveMinuteLabel == null) {
            return;
        }

        LiveScoreboardState state = resolveLiveScoreboardState(snapshot, rawStatus);
        boolean showPhase = state != null && state.phaseLabel() != null && !state.phaseLabel().isBlank();
        boolean showMinute = state != null && state.minuteLabel() != null && !state.minuteLabel().isBlank();

        detailLivePhaseLabel.setManaged(showPhase);
        detailLivePhaseLabel.setVisible(showPhase);
        detailLivePhaseLabel.setText(showPhase ? state.phaseLabel() : "");

        detailLiveMinuteLabel.setManaged(showMinute);
        detailLiveMinuteLabel.setVisible(showMinute);
        detailLiveMinuteLabel.setText(showMinute ? state.minuteLabel() : "");

        updateLiveAttentionAnimation(snapshot, rawStatus, showMinute);
    }

    private void updateLiveAttentionAnimation(ApiFootballFixtureSnapshot snapshot, String rawStatus, boolean showMinute) {
        if (!isBlinkingLiveState(snapshot, rawStatus)) {
            stopLiveAttentionAnimation();
            return;
        }

        liveAttentionMinuteVisible = showMinute;
        if (liveAttentionTimeline == null) {
            liveAttentionTimeline = new Timeline(
                    new KeyFrame(javafx.util.Duration.ZERO, event -> {
                        liveAttentionAccentFrame = true;
                        applyLiveAttentionFrame();
                    }),
                    new KeyFrame(javafx.util.Duration.millis(680), event -> {
                        liveAttentionAccentFrame = false;
                        applyLiveAttentionFrame();
                    })
            );
            liveAttentionTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        if (liveAttentionTimeline.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            liveAttentionTimeline.playFromStart();
            return;
        }
        applyLiveAttentionFrame();
    }

    private boolean isBlinkingLiveState(ApiFootballFixtureSnapshot snapshot, String rawStatus) {
        if (snapshot != null && snapshot.isLive()) {
            return true;
        }
        return isLiveStatusText(normalize(rawStatus));
    }

    private void applyLiveAttentionFrame() {
        String colorHex = toHexColor(liveAttentionAccentFrame
                ? LIVE_ATTENTION_ACCENT
                : (ThemeManager.isDarkMode() ? LIVE_ATTENTION_DARK_BASE : LIVE_ATTENTION_LIGHT_BASE));
        detailScoreValueLabel.setStyle("-fx-text-fill: " + colorHex + ";");
        detailLiveMinuteLabel.setStyle(liveAttentionMinuteVisible ? "-fx-text-fill: " + colorHex + ";" : "");
    }

    private void stopLiveAttentionAnimation() {
        if (liveAttentionTimeline != null) {
            liveAttentionTimeline.stop();
        }
        liveAttentionAccentFrame = false;
        liveAttentionMinuteVisible = false;
        clearLiveAttentionStyles();
    }

    private void clearLiveAttentionStyles() {
        if (detailScoreValueLabel != null) {
            detailScoreValueLabel.setStyle("");
        }
        if (detailLiveMinuteLabel != null) {
            detailLiveMinuteLabel.setStyle("");
        }
    }

    private String toHexColor(Color color) {
        if (color == null) {
            return "#ffffff";
        }
        return String.format(
                Locale.ROOT,
                "#%02x%02x%02x",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255)
        );
    }

    private LiveScoreboardState resolveLiveScoreboardState(ApiFootballFixtureSnapshot snapshot, String rawStatus) {
        if (snapshot != null) {
            String phaseLabel = mapLivePhaseLabel(snapshot.statusShort(), snapshot.statusLong());
            String minuteLabel = normalizeLiveMinuteLabel(snapshot.minuteLabel());
            if (phaseLabel != null || minuteLabel != null) {
                return new LiveScoreboardState(phaseLabel, minuteLabel);
            }
            if (!snapshot.isLive()) {
                return null;
            }
        }

        String normalizedStatus = emptyToNull(rawStatus);
        if (normalizedStatus == null) {
            return null;
        }

        String lowerStatus = normalizedStatus.toLowerCase();
        String phaseLabel = null;
        if (lowerStatus.contains("1re mi-temps") || lowerStatus.contains("premiere mi-temps") || lowerStatus.contains("first half")) {
            phaseLabel = "1RE MI-TEMPS";
        } else if (lowerStatus.contains("2e mi-temps") || lowerStatus.contains("deuxieme mi-temps") || lowerStatus.contains("second half")) {
            phaseLabel = "2E MI-TEMPS";
        } else if (lowerStatus.contains("mi-temps")) {
            phaseLabel = "MI-TEMPS";
        } else if (lowerStatus.contains("prolong")) {
            phaseLabel = "PROLONGATIONS";
        } else if (lowerStatus.contains("tab") || lowerStatus.contains("pen") || lowerStatus.contains("tirs au but")) {
            phaseLabel = "TIRS AU BUT";
        } else if (lowerStatus.contains("direct") || lowerStatus.contains("live") || lowerStatus.contains("cours")) {
            phaseLabel = "EN DIRECT";
        }

        Matcher matcher = LIVE_MINUTE_PATTERN.matcher(normalizedStatus);
        String minuteLabel = matcher.find() ? matcher.group(1) : null;
        if (phaseLabel == null && minuteLabel == null) {
            return null;
        }
        return new LiveScoreboardState(phaseLabel, minuteLabel);
    }

    private String mapLivePhaseLabel(String statusShort, String statusLong) {
        String normalizedShort = emptyToNull(statusShort);
        if (normalizedShort != null) {
            return switch (normalizedShort.toUpperCase()) {
                case "1H" -> "1RE MI-TEMPS";
                case "HT" -> "MI-TEMPS";
                case "2H" -> "2E MI-TEMPS";
                case "ET" -> "PROLONGATIONS";
                case "BT" -> "PAUSE PROLONGATIONS";
                case "P" -> "TIRS AU BUT";
                case "LIVE" -> "EN DIRECT";
                default -> null;
            };
        }

        String normalizedLong = emptyToNull(statusLong);
        if (normalizedLong == null) {
            return null;
        }

        String lowerStatus = normalizedLong.toLowerCase();
        if (lowerStatus.contains("first half")) {
            return "1RE MI-TEMPS";
        }
        if (lowerStatus.contains("second half")) {
            return "2E MI-TEMPS";
        }
        if (lowerStatus.contains("half-time")) {
            return "MI-TEMPS";
        }
        if (lowerStatus.contains("extra time")) {
            return "PROLONGATIONS";
        }
        if (lowerStatus.contains("penalties")) {
            return "TIRS AU BUT";
        }
        return null;
    }

    private String normalizeLiveMinuteLabel(String minuteLabel) {
        String normalized = emptyToNull(minuteLabel);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = LIVE_MINUTE_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : normalized;
    }

    private void renderApiFootballInsights(ApiFootballMatchDetails details) {
        if (details == null) {
            return;
        }

        currentIncidents = normalizeIncidentsForDisplay(details.incidents());
        renderSummary(currentIncidents);

        ApiFootballLineupSide storedHomeLineup = buildStoredLineup(match == null ? null : match.getLineupDomicile(), homeTeam);
        ApiFootballLineupSide storedAwayLineup = buildStoredLineup(match == null ? null : match.getLineupExterieur(), awayTeam);
        ApiFootballLineupSide homeLineup = chooseRenderableLineup(details.homeLineup(), storedHomeLineup);
        ApiFootballLineupSide awayLineup = chooseRenderableLineup(details.awayLineup(), storedAwayLineup);
        currentHomeLineup = homeLineup;
        currentAwayLineup = awayLineup;
        currentStatistics = details.statistics() == null ? List.of() : List.copyOf(details.statistics());
        selectMatchMvp(homeLineup, awayLineup);

        lineupDomicileMetaLabel.setText(buildLineupMeta(homeLineup));
        renderLineupSection(homeLineupPitchContainer, homeBenchContainer, homeLineup, false);

        lineupExterieurMetaLabel.setText(buildLineupMeta(awayLineup));
        renderLineupSection(awayLineupPitchContainer, awayBenchContainer, awayLineup, true);

        renderStatistics(details.statistics());
        renderLiveCompanion();
    }

    private void renderStoredLineups() {
        currentIncidents = List.of();
        currentMvpPlayerId = null;
        currentMvpPlayerNameKey = null;
        currentStatistics = List.of();
        renderSummary(List.of());
        ApiFootballLineupSide homeStored = chooseRenderableLineup(buildStoredLineup(match == null ? null : match.getLineupDomicile(), homeTeam), null);
        ApiFootballLineupSide awayStored = chooseRenderableLineup(buildStoredLineup(match == null ? null : match.getLineupExterieur(), awayTeam), null);
        currentHomeLineup = homeStored;
        currentAwayLineup = awayStored;
        lineupDomicileMetaLabel.setText(buildLineupMeta(homeStored));
        lineupExterieurMetaLabel.setText(buildLineupMeta(awayStored));
        renderLineupSection(homeLineupPitchContainer, homeBenchContainer, homeStored, false);
        renderLineupSection(awayLineupPitchContainer, awayBenchContainer, awayStored, true);
        renderLiveCompanion();
    }

    private List<String> extractStartingLineupNames(ApiFootballLineupSide lineup) {
        if (lineup == null || lineup.startingPlayers() == null || lineup.startingPlayers().isEmpty()) {
            return List.of();
        }

        return lineup.startingPlayers().stream()
                .filter(java.util.Objects::nonNull)
                .map(player -> emptyToFallback(player.playerName(), "Joueur"))
                .toList();
    }

    private String buildIncidentHighlight(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "";
        }

        String minute = emptyToFallback(incident.minuteLabel(), "--");
        String title = buildTimelineTitle(incident);
        String meta = buildTimelineMeta(incident);
        if (meta == null || meta.isBlank()) {
            return minute + " " + title;
        }
        return minute + " " + title + " (" + meta + ")";
    }

    private void renderLiveCompanion() {
        if (liveCompanionSummaryLabel == null
                || liveCompanionDominantTeamValueLabel == null
                || liveCompanionPressureValueLabel == null
                || liveCompanionDangerValueLabel == null
                || liveCompanionIntensityValueLabel == null
                || liveCompanionTurningPointsContainer == null
                || liveCompanionImpactsContainer == null) {
            return;
        }

        if (match == null || matchsService == null) {
            liveCompanionSummaryLabel.setText("Le compagnon live sera disponible des qu'un match detaille est charge.");
            liveCompanionDominantTeamValueLabel.setText("Equilibre");
            liveCompanionPressureValueLabel.setText("Domicile 50 | Exterieur 50");
            liveCompanionDangerValueLabel.setText("Faible");
            liveCompanionIntensityValueLabel.setText("0/100");
            applyCompanionTone(liveCompanionDominantTeamValueLabel, "balanced");
            applyCompanionTone(liveCompanionDangerValueLabel, "low");
            applyCompanionTone(liveCompanionIntensityValueLabel, "low");
            renderCompanionTurningPoints(List.of());
            renderCompanionImpacts(List.of());
            return;
        }

        try {
            MatchLiveCompanionResponse response = matchsService.getLiveCompanion(match);
            liveCompanionSummaryLabel.setText(emptyToFallback(response.summary(), "Le match reste ouvert, sans tendance nette pour l'instant."));
            liveCompanionDominantTeamValueLabel.setText(formatCompanionDominantTeam(response.momentum().dominantTeam()));
            liveCompanionPressureValueLabel.setText(buildCompanionPressureLabel(response));
            liveCompanionDangerValueLabel.setText(formatCompanionDangerLevel(response.dangerLevel()));
            liveCompanionIntensityValueLabel.setText(response.intensityScore() + "/100");
            applyCompanionTone(liveCompanionDominantTeamValueLabel, response.momentum().dominantTeam());
            applyCompanionTone(liveCompanionDangerValueLabel, response.dangerLevel());
            applyCompanionTone(liveCompanionIntensityValueLabel, intensityTone(response.intensityScore()));
            renderCompanionTurningPoints(response.turningPoints());
            renderCompanionImpacts(response.topImpacts());
        } catch (RuntimeException e) {
            liveCompanionSummaryLabel.setText("Le compagnon live n'a pas pu etre calcule pour ce match.");
            liveCompanionDominantTeamValueLabel.setText("Indisponible");
            liveCompanionPressureValueLabel.setText("Domicile -- | Exterieur --");
            liveCompanionDangerValueLabel.setText("Indisponible");
            liveCompanionIntensityValueLabel.setText("--");
            applyCompanionTone(liveCompanionDominantTeamValueLabel, "balanced");
            applyCompanionTone(liveCompanionDangerValueLabel, "medium");
            applyCompanionTone(liveCompanionIntensityValueLabel, "medium");
            renderCompanionTurningPoints(List.of());
            renderCompanionImpacts(List.of());
        }
    }

    private void renderCompanionTurningPoints(List<String> turningPoints) {
        liveCompanionTurningPointsContainer.getChildren().clear();
        List<String> safePoints = turningPoints == null ? List.of() : turningPoints.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(point -> !point.isBlank())
                .limit(5)
                .toList();
        if (safePoints.isEmpty()) {
            liveCompanionTurningPointsContainer.getChildren().add(buildCompanionPlaceholder(
                    "Aucun basculement net detecte pour le moment."
            ));
            return;
        }

        for (String point : safePoints) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("live-companion-list-row");

            Label bullet = new Label("•");
            bullet.getStyleClass().add("live-companion-bullet");

            Label text = new Label(point);
            text.setWrapText(true);
            text.getStyleClass().add("live-companion-list-text");
            HBox.setHgrow(text, Priority.ALWAYS);

            row.getChildren().addAll(bullet, text);
            liveCompanionTurningPointsContainer.getChildren().add(row);
        }
    }

    private void renderCompanionImpacts(List<MatchLiveCompanionResponse.PlayerImpact> topImpacts) {
        liveCompanionImpactsContainer.getChildren().clear();
        List<MatchLiveCompanionResponse.PlayerImpact> safeImpacts = topImpacts == null ? List.of() : topImpacts.stream()
                .filter(Objects::nonNull)
                .limit(5)
                .toList();
        if (safeImpacts.isEmpty()) {
            liveCompanionImpactsContainer.getChildren().add(buildCompanionPlaceholder(
                    "Aucun impact joueur notable n'est encore disponible."
            ));
            return;
        }

        for (MatchLiveCompanionResponse.PlayerImpact impact : safeImpacts) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("live-companion-impact-row");

            VBox info = new VBox(2);
            info.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label playerName = new Label(emptyToFallback(impact.player(), "Joueur"));
            playerName.getStyleClass().add("live-companion-impact-name");

            Label meta = new Label(formatImpactMeta(impact));
            meta.getStyleClass().add("live-companion-impact-meta");

            info.getChildren().addAll(playerName, meta);

            Label score = new Label(String.format(Locale.US, "%.1f", impact.impactScore()));
            score.getStyleClass().addAll("live-companion-impact-score", ratingToneStyle(impact.impactScore()));

            row.getChildren().addAll(info, score);
            liveCompanionImpactsContainer.getChildren().add(row);
        }
    }

    private Label buildCompanionPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.setWrapText(true);
        placeholder.getStyleClass().add("live-companion-placeholder");
        return placeholder;
    }

    private String formatCompanionDominantTeam(String dominantTeam) {
        String normalized = lowercase(dominantTeam);
        if ("home".equals(normalized)) {
            return detailHomeNameLabel == null ? "Domicile" : emptyToFallback(detailHomeNameLabel.getText(), "Domicile");
        }
        if ("away".equals(normalized)) {
            return detailAwayNameLabel == null ? "Exterieur" : emptyToFallback(detailAwayNameLabel.getText(), "Exterieur");
        }
        return "Equilibre";
    }

    private String buildCompanionPressureLabel(MatchLiveCompanionResponse response) {
        if (response == null || response.momentum() == null) {
            return "Domicile -- | Exterieur --";
        }
        return "Domicile " + response.momentum().homePressure()
                + " | Exterieur " + response.momentum().awayPressure();
    }

    private String formatCompanionDangerLevel(String dangerLevel) {
        String normalized = lowercase(dangerLevel);
        if ("high".equals(normalized)) {
            return "Elevee";
        }
        if ("medium".equals(normalized)) {
            return "Moyenne";
        }
        if ("low".equals(normalized)) {
            return "Faible";
        }
        return "Indisponible";
    }

    private String intensityTone(int intensityScore) {
        if (intensityScore >= 75) {
            return "high";
        }
        if (intensityScore >= 45) {
            return "medium";
        }
        return "low";
    }

    private String formatImpactMeta(MatchLiveCompanionResponse.PlayerImpact impact) {
        String teamLabel = switch (lowercase(impact == null ? null : impact.team())) {
            case "home" -> emptyToFallback(detailHomeNameLabel == null ? null : detailHomeNameLabel.getText(), "Domicile");
            case "away" -> emptyToFallback(detailAwayNameLabel == null ? null : detailAwayNameLabel.getText(), "Exterieur");
            default -> "Match";
        };
        return teamLabel + " | Impact";
    }

    private void applyCompanionTone(Label label, String tone) {
        if (label == null) {
            return;
        }
        label.getStyleClass().removeAll(
                "live-companion-tone-home",
                "live-companion-tone-away",
                "live-companion-tone-balanced",
                "live-companion-tone-low",
                "live-companion-tone-medium",
                "live-companion-tone-high"
        );
        String normalized = lowercase(tone);
        if ("home".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-home");
        } else if ("away".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-away");
        } else if ("balanced".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-balanced");
        } else if ("high".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-high");
        } else if ("medium".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-medium");
        } else if ("low".equals(normalized)) {
            label.getStyleClass().add("live-companion-tone-low");
        }
    }

    private void applyActiveTab() {
        updateSectionVisibility(summarySection, activeTab == MatchDetailTab.SUMMARY);
        updateSectionVisibility(statsSection, activeTab == MatchDetailTab.STATS);
        updateSectionVisibility(lineupSection, activeTab == MatchDetailTab.LINEUP);
        updateSectionVisibility(videosSection, activeTab == MatchDetailTab.VIDEOS);
        updateSectionVisibility(oddsSection, activeTab == MatchDetailTab.ODDS);
        updateTabButton(summaryTabButton, activeTab == MatchDetailTab.SUMMARY);
        updateTabButton(statsTabButton, activeTab == MatchDetailTab.STATS);
        updateTabButton(lineupTabButton, activeTab == MatchDetailTab.LINEUP);
        updateTabButton(videosTabButton, activeTab == MatchDetailTab.VIDEOS);
        updateTabButton(oddsTabButton, activeTab == MatchDetailTab.ODDS);
        if (activeTab == MatchDetailTab.VIDEOS) {
            renderSelectedMatchVideo();
        } else {
            unloadMatchVideoPlayer();
        }
    }

    private void updateSectionVisibility(VBox section, boolean visible) {
        if (section == null) {
            return;
        }
        section.setManaged(visible);
        section.setVisible(visible);
    }

    private void updateTabButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("detail-tab-button-active");
        if (active && !button.getStyleClass().contains("detail-tab-button-active")) {
            button.getStyleClass().add("detail-tab-button-active");
        }
    }

    private ApiFootballLineupSide chooseRenderableLineup(ApiFootballLineupSide primary, ApiFootballLineupSide fallback) {
        if (primary == null) {
            return fallback;
        }
        if (fallback == null) {
            return primary;
        }
        return lineupRenderScore(primary) >= lineupRenderScore(fallback) ? primary : fallback;
    }

    private int lineupRenderScore(ApiFootballLineupSide lineup) {
        if (lineup == null || !lineup.hasStartingPlayers()) {
            return 0;
        }

        int score = lineup.startingPlayerCount() * 100;
        if (lineup.formation() != null && !lineup.formation().isBlank()) {
            score += 40;
        }
        if (lineup.coachName() != null && !lineup.coachName().isBlank()) {
            score += 20;
        }
        if (lineup.substitutePlayers() != null) {
            score += Math.min(15, lineup.substitutePlayers().size());
        }
        for (ApiFootballLineupPlayer player : lineup.startingPlayers()) {
            if (player == null) {
                continue;
            }
            score += player.hasGrid() ? 8 : 0;
            score += player.hasPhoto() ? 5 : 0;
            score += player.hasPosition() ? 3 : 0;
            score += player.shirtNumber() != null && !player.shirtNumber().isBlank() ? 2 : 0;
        }
        return score;
    }

    private ApiFootballLineupSide buildStoredLineup(String rawLineup, Equipe team) {
        String normalizedLineup = emptyToNull(rawLineup);
        if (normalizedLineup == null) {
            return null;
        }

        List<ApiFootballLineupPlayer> players = new ArrayList<>();
        for (String line : normalizedLineup.split("\\R")) {
            ApiFootballLineupPlayer player = parseStoredPlayer(line);
            if (player != null) {
                players.add(player);
            }
        }

        if (players.isEmpty()) {
            return null;
        }

        return new ApiFootballLineupSide(
                team == null ? null : team.getNom(),
                null,
                null,
                players,
                List.of()
        );
    }

    private ApiFootballLineupPlayer parseStoredPlayer(String rawPlayer) {
        String sanitized = emptyToNull(rawPlayer);
        if (sanitized == null) {
            return null;
        }

        sanitized = sanitized.replaceFirst("^\\d+\\.\\s*", "").trim();

        String shirtNumber = null;
        java.util.regex.Matcher shirtMatcher = java.util.regex.Pattern.compile("^#?(\\d+)\\s+").matcher(sanitized);
        if (shirtMatcher.find()) {
            shirtNumber = shirtMatcher.group(1);
            sanitized = sanitized.substring(shirtMatcher.end()).trim();
        }

        String position = null;
        java.util.regex.Matcher metaMatcher = java.util.regex.Pattern.compile("\\(([^()]+)\\)\\s*$").matcher(sanitized);
        if (metaMatcher.find()) {
            String candidateMeta = metaMatcher.group(1).trim();
            if (!candidateMeta.matches("\\d+[:\\-]\\d+")) {
                position = candidateMeta;
            }
            sanitized = sanitized.substring(0, metaMatcher.start()).trim();
        }

        if (sanitized.isBlank()) {
            sanitized = "Joueur";
        }

        return new ApiFootballLineupPlayer(sanitized, shirtNumber, position, null, null, null, null);
    }

    private String buildLineupMeta(ApiFootballLineupSide lineup) {
        if (lineup == null || !lineup.hasStartingPlayers()) {
            return "Onze de depart indisponible";
        }

        List<String> parts = new ArrayList<>();
        String formation = resolveFormationLabel(lineup);
        if (formation != null) {
            parts.add("Formation " + formation);
        }
        if (lineup.coachName() != null && !lineup.coachName().isBlank()) {
            parts.add("Coach " + lineup.coachName());
        }
        if (!lineup.hasCompleteStartingEleven()) {
            parts.add("Composition partielle " + lineup.startingPlayerCount() + "/11");
        }
        if (lineup.substitutePlayers() != null && !lineup.substitutePlayers().isEmpty()) {
            parts.add(lineup.substitutePlayers().size() + " remplacants");
        }
        return parts.isEmpty() ? "Formation indisponible" : String.join(" | ", parts);
    }

    private List<ApiFootballMatchIncident> normalizeIncidentsForDisplay(List<ApiFootballMatchIncident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }
        return incidents.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::normalizeIncidentForDisplay)
                .sorted(Comparator
                        .comparingInt((ApiFootballMatchIncident incident) -> incident.minute() == null ? Integer.MAX_VALUE : incident.minute())
                        .thenComparingInt(incident -> incident.addedTime() == null ? 0 : incident.addedTime()))
                .toList();
    }

    private ApiFootballMatchIncident normalizeIncidentForDisplay(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return null;
        }

        Integer minute = incident.minute();
        Integer addedTime = incident.addedTime();
        if (minute != null && minute < 0) {
            addedTime = Math.abs(minute);
            minute = 90;
        }

        return new ApiFootballMatchIncident(
                incident.incidentType(),
                incident.incidentClass(),
                buildIncidentMinuteLabel(minute, addedTime),
                minute,
                addedTime,
                incident.homeSide(),
                incident.playerName(),
                incident.playerId(),
                incident.assistPlayerName(),
                incident.assistPlayerId(),
                incident.playerInName(),
                incident.playerInId(),
                incident.playerOutName(),
                incident.playerOutId(),
                incident.reason(),
                incident.homeScore(),
                incident.awayScore()
        );
    }

    private String buildIncidentMinuteLabel(Integer minute, Integer addedTime) {
        if (minute == null) {
            return "--";
        }
        if (addedTime != null && addedTime > 0) {
            return minute + "+" + addedTime + "'";
        }
        return minute + "'";
    }

    private void renderSummary(List<ApiFootballMatchIncident> incidents) {
        summaryTimelineContainer.getChildren().clear();
        List<ApiFootballMatchIncident> safeIncidents = incidents == null ? List.of() : incidents.stream()
                .filter(java.util.Objects::nonNull)
                .toList();

        boolean hasIncidents = !safeIncidents.isEmpty();
        summaryEmptyLabel.setManaged(!hasIncidents);
        summaryEmptyLabel.setVisible(!hasIncidents);
        if (!hasIncidents) {
            summaryEmptyLabel.setText("Aucun resume detaille disponible pour ce match.");
            return;
        }

        for (ApiFootballMatchIncident incident : safeIncidents) {
            summaryTimelineContainer.getChildren().add(buildTimelineRow(incident));
        }
    }

    private HBox buildTimelineRow(ApiFootballMatchIncident incident) {
        VBox homeBox = buildTimelineEventBox(incident, true);
        VBox awayBox = buildTimelineEventBox(incident, false);

        Label minuteLabel = new Label(emptyToFallback(incident.minuteLabel(), "--"));
        minuteLabel.getStyleClass().add("timeline-minute-chip");
        minuteLabel.setMinWidth(44);
        minuteLabel.setAlignment(Pos.CENTER);

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(homeBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(awayBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(leftSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("timeline-row");

        if (incident.homeSide()) {
            row.getChildren().addAll(homeBox, minuteLabel, rightSpacer);
        } else {
            row.getChildren().addAll(leftSpacer, minuteLabel, awayBox);
        }
        return row;
    }

    private VBox buildTimelineEventBox(ApiFootballMatchIncident incident, boolean homeSide) {
        VBox box = new VBox(2);
        box.setAlignment(homeSide ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        box.getStyleClass().addAll(
                "timeline-event-box",
                homeSide ? "timeline-event-home" : "timeline-event-away"
        );
        box.setMaxWidth(Double.MAX_VALUE);

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(homeSide ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        Label badge = new Label(buildTimelineBadgeText(incident));
        badge.getStyleClass().addAll("timeline-event-badge", timelineBadgeTone(incident));

        Label scoreLabel = null;
        if (incident.isGoal() && incident.homeScore() != null && incident.awayScore() != null) {
            scoreLabel = new Label(incident.homeScore() + " - " + incident.awayScore());
            scoreLabel.getStyleClass().add("timeline-score-chip");
        }

        if (homeSide) {
            badgeRow.getChildren().add(badge);
            if (scoreLabel != null) {
                badgeRow.getChildren().add(scoreLabel);
            }
        } else {
            if (scoreLabel != null) {
                badgeRow.getChildren().add(scoreLabel);
            }
            badgeRow.getChildren().add(badge);
        }

        Label title = new Label(buildTimelineTitle(incident));
        title.getStyleClass().add("timeline-event-title");
        title.setWrapText(true);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setMaxWidth(Double.MAX_VALUE);

        Label meta = new Label(buildTimelineMeta(incident));
        meta.getStyleClass().add("timeline-event-meta");
        meta.setWrapText(true);
        meta.setManaged(!meta.getText().isBlank());
        meta.setVisible(!meta.getText().isBlank());

        box.getChildren().addAll(badgeRow, title, meta);
        return box;
    }

    private String buildTimelineTitle(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "Evenement";
        }
        if (incident.isSubstitution()) {
            String inName = emptyToFallback(incident.playerInName(), "Entrant");
            String outName = emptyToFallback(incident.playerOutName(), "Sortant");
            return inName + " pour " + outName;
        }
        return emptyToFallback(incident.playerName(), "Evenement");
    }

    private String buildTimelineMeta(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (incident.isGoal()) {
            if (incident.assistPlayerName() != null && !incident.assistPlayerName().isBlank()) {
                parts.add("Passe decisive: " + incident.assistPlayerName().trim());
            }
            String goalClass = prettifyIncidentClass(incident.incidentClass());
            if (goalClass != null && !"Regular".equalsIgnoreCase(goalClass)) {
                parts.add(goalClass);
            }
        } else if (incident.isCard()) {
            parts.add(incident.isRedCard() ? "Carton rouge" : "Carton jaune");
            if (incident.reason() != null && !incident.reason().isBlank()) {
                parts.add(incident.reason().trim());
            }
        } else if (incident.isSubstitution()) {
            if (incident.reason() != null && !incident.reason().isBlank()) {
                parts.add(incident.reason().trim());
            } else {
                parts.add("Remplacement");
            }
        } else if (incident.reason() != null && !incident.reason().isBlank()) {
            parts.add(incident.reason().trim());
        }

        return String.join(" • ", parts);
    }

    private String buildTimelineBadgeText(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "EVT";
        }
        if (incident.isGoal()) {
            return "BUT";
        }
        if (incident.isYellowCard()) {
            return "CJ";
        }
        if (incident.isRedCard()) {
            return "CR";
        }
        if (incident.isSubstitution()) {
            return "REM";
        }
        return "EVT";
    }

    private String timelineBadgeTone(ApiFootballMatchIncident incident) {
        if (incident == null) {
            return "timeline-badge-neutral";
        }
        if (incident.isGoal()) {
            return "timeline-badge-goal";
        }
        if (incident.isYellowCard()) {
            return "timeline-badge-yellow";
        }
        if (incident.isRedCard()) {
            return "timeline-badge-red";
        }
        if (incident.isSubstitution()) {
            return "timeline-badge-sub";
        }
        return "timeline-badge-neutral";
    }

    private String prettifyIncidentClass(String rawClass) {
        String normalized = emptyToNull(rawClass);
        if (normalized == null) {
            return null;
        }
        String readable = normalized.replace('-', ' ').replace('_', ' ').trim();
        if (readable.equalsIgnoreCase("yellowRed")) {
            return "Deuxieme carton jaune";
        }
        if (readable.isBlank()) {
            return null;
        }
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private String resolveFormationLabel(ApiFootballLineupSide lineup) {
        if (lineup == null || !lineup.hasStartingPlayers()) {
            return null;
        }

        String explicitFormation = emptyToNull(lineup.formation());
        if (explicitFormation != null) {
            String normalizedExplicit = explicitFormation.replace(':', '-').trim();
            if (normalizedExplicit.matches("\\d(?:-\\d){2,4}")) {
                return normalizedExplicit;
            }
        }

        List<List<ApiFootballLineupPlayer>> inferredRows = inferOutfieldRows(lineup.startingPlayers());
        if (inferredRows.isEmpty() || inferredRows.size() < 3 || inferredRows.size() > 5) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        int totalOutfield = 0;
        for (List<ApiFootballLineupPlayer> row : inferredRows) {
            if (row != null && !row.isEmpty()) {
                parts.add(String.valueOf(row.size()));
                totalOutfield += row.size();
            }
        }
        return parts.isEmpty() || totalOutfield != 10 ? null : String.join("-", parts);
    }

    private void renderLineupSection(Pane pitchContainer, FlowPane benchContainer, ApiFootballLineupSide lineup, boolean awaySide) {
        clearPitchContent(pitchContainer);
        benchContainer.getChildren().clear();
        attachPitchRelayout(pitchContainer);
        installPitchDecorations(pitchContainer, awaySide);

        if (lineup == null || !lineup.hasStartingPlayers()) {
            showPitchPlaceholder(pitchContainer, "Composition non disponible");
            benchContainer.getChildren().add(buildBenchPlaceholder("Aucun remplacant renseigne"));
            return;
        }

        List<List<ApiFootballLineupPlayer>> rows = buildPitchRows(lineup);
        if (rows.isEmpty()) {
            showPitchPlaceholder(pitchContainer, "Composition non disponible");
        } else {
            List<VBox> markers = new ArrayList<>();
            for (List<ApiFootballLineupPlayer> row : rows) {
                for (ApiFootballLineupPlayer player : row) {
                    VBox marker = buildPitchPlayerMarker(player);
                    marker.setManaged(false);
                    markers.add(marker);
                    pitchContainer.getChildren().add(marker);
                }
            }

            Runnable layout = () -> layoutPitchMarkers(pitchContainer, rows, markers, awaySide);
            pitchContainer.getProperties().put(PITCH_LAYOUT_KEY, layout);
            layout.run();
            Platform.runLater(layout);
        }

        List<ApiFootballLineupPlayer> substitutes = lineup.substitutePlayers() == null ? List.of() : lineup.substitutePlayers();
        if (substitutes.isEmpty()) {
            benchContainer.getChildren().add(buildBenchPlaceholder("Aucun remplacant renseigne"));
            return;
        }

        for (ApiFootballLineupPlayer player : substitutes) {
            benchContainer.getChildren().add(buildBenchPlayerCard(player));
        }
    }

    private void clearPitchContent(Pane pitchContainer) {
        pitchContainer.getChildren().removeIf(node -> !Boolean.TRUE.equals(node.getProperties().get(PITCH_DECORATION_NODE_KEY)));
    }

    private void attachPitchRelayout(Pane pitchContainer) {
        String attachedKey = PITCH_LAYOUT_KEY + ".attached";
        if (Boolean.TRUE.equals(pitchContainer.getProperties().get(attachedKey))) {
            return;
        }

        ChangeListener<Number> listener = (observable, oldValue, newValue) -> {
            Object callback = pitchContainer.getProperties().get(PITCH_LAYOUT_KEY);
            if (callback instanceof Runnable runnable) {
                runnable.run();
            }
        };

        pitchContainer.widthProperty().addListener(listener);
        pitchContainer.heightProperty().addListener(listener);
        pitchContainer.getProperties().put(attachedKey, Boolean.TRUE);
    }

    private void installPitchDecorations(Pane pitchContainer, boolean awaySide) {
        if (Boolean.TRUE.equals(pitchContainer.getProperties().get(PITCH_DECORATIONS_KEY))) {
            return;
        }

        Region penaltyBox = new Region();
        penaltyBox.getStyleClass().add("pitch-marking-penalty");
        penaltyBox.setManaged(false);
        penaltyBox.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Region sixYardBox = new Region();
        sixYardBox.getStyleClass().add("pitch-marking-six-yard");
        sixYardBox.setManaged(false);
        sixYardBox.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Region goalBox = new Region();
        goalBox.getStyleClass().add("pitch-marking-goal");
        goalBox.setManaged(false);
        goalBox.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Region touchline = new Region();
        touchline.getStyleClass().add("pitch-marking-touchline");
        touchline.getStyleClass().add(awaySide ? "pitch-marking-touchline-away" : "pitch-marking-touchline-home");
        touchline.setManaged(false);
        touchline.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Region halfwayLine = new Region();
        halfwayLine.getStyleClass().add("pitch-marking-halfway");
        halfwayLine.setManaged(false);
        halfwayLine.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Circle penaltySpot = new Circle();
        penaltySpot.getStyleClass().add("pitch-marking-spot");
        penaltySpot.setManaged(false);
        penaltySpot.setMouseTransparent(true);
        penaltySpot.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Circle centerCircle = new Circle();
        centerCircle.getStyleClass().add("pitch-marking-circle");
        centerCircle.setManaged(false);
        centerCircle.setMouseTransparent(true);
        centerCircle.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Circle centerSpot = new Circle();
        centerSpot.getStyleClass().add("pitch-marking-spot");
        centerSpot.setManaged(false);
        centerSpot.setMouseTransparent(true);
        centerSpot.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Arc penaltyArc = new Arc();
        penaltyArc.getStyleClass().add("pitch-marking-penalty-arc");
        penaltyArc.setManaged(false);
        penaltyArc.setType(ArcType.OPEN);
        penaltyArc.setMouseTransparent(true);
        penaltyArc.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Arc topCornerArc = new Arc();
        topCornerArc.getStyleClass().add("pitch-marking-corner");
        topCornerArc.setManaged(false);
        topCornerArc.setType(ArcType.OPEN);
        topCornerArc.setMouseTransparent(true);
        topCornerArc.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        Arc bottomCornerArc = new Arc();
        bottomCornerArc.getStyleClass().add("pitch-marking-corner");
        bottomCornerArc.setManaged(false);
        bottomCornerArc.setType(ArcType.OPEN);
        bottomCornerArc.setMouseTransparent(true);
        bottomCornerArc.getProperties().put(PITCH_DECORATION_NODE_KEY, Boolean.TRUE);

        pitchContainer.getChildren().addAll(
                penaltyBox,
                sixYardBox,
                goalBox,
                touchline,
                halfwayLine,
                centerCircle,
                penaltySpot,
                centerSpot,
                penaltyArc,
                topCornerArc,
                bottomCornerArc
        );

        ChangeListener<Number> listener = (observable, oldValue, newValue) ->
                layoutPitchDecorations(
                        pitchContainer,
                        awaySide,
                        penaltyBox,
                        sixYardBox,
                        goalBox,
                        touchline,
                        halfwayLine,
                        penaltySpot,
                        centerCircle,
                        centerSpot,
                        penaltyArc,
                        topCornerArc,
                        bottomCornerArc
                );
        pitchContainer.widthProperty().addListener(listener);
        pitchContainer.heightProperty().addListener(listener);
        pitchContainer.getProperties().put(PITCH_DECORATIONS_KEY, Boolean.TRUE);
        Platform.runLater(() -> layoutPitchDecorations(
                pitchContainer,
                awaySide,
                penaltyBox,
                sixYardBox,
                goalBox,
                touchline,
                halfwayLine,
                penaltySpot,
                centerCircle,
                centerSpot,
                penaltyArc,
                topCornerArc,
                bottomCornerArc
        ));
    }

    private void layoutPitchDecorations(
            Pane pitchContainer,
            boolean awaySide,
            Region penaltyBox,
            Region sixYardBox,
            Region goalBox,
            Region touchline,
            Region halfwayLine,
            Circle penaltySpot,
            Circle centerCircle,
            Circle centerSpot,
            Arc penaltyArc,
            Arc topCornerArc,
            Arc bottomCornerArc
    ) {
        double width = resolvePitchWidth(pitchContainer);
        double height = resolvePitchHeight(pitchContainer);
        double fieldInset = Math.min(PITCH_FIELD_LINE_INSET, Math.max(12.0, Math.min(width, height) * 0.045));
        double fieldHeight = Math.max(120.0, height - (fieldInset * 2.0));
        double fieldTop = fieldInset;
        double fieldBottom = fieldTop + fieldHeight;

        double penaltyWidth = Math.min(108.0, Math.max(88.0, width * 0.18));
        double penaltyHeight = Math.min(240.0, Math.max(170.0, fieldHeight * 0.48));
        double penaltyY = fieldTop + ((fieldHeight - penaltyHeight) / 2.0);
        double goalLineX = awaySide ? width - fieldInset : fieldInset;
        double penaltyX = awaySide ? goalLineX - penaltyWidth : goalLineX;

        double sixWidth = Math.min(48.0, Math.max(36.0, width * 0.08));
        double sixHeight = Math.min(112.0, Math.max(80.0, fieldHeight * 0.22));
        double sixY = fieldTop + ((fieldHeight - sixHeight) / 2.0);
        double sixX = awaySide ? goalLineX - sixWidth : goalLineX;

        double goalWidth = 8.0;
        double goalHeight = Math.min(54.0, Math.max(40.0, fieldHeight * 0.11));
        double goalY = fieldTop + ((fieldHeight - goalHeight) / 2.0);
        double goalX = awaySide ? goalLineX - goalWidth : goalLineX;

        double halfwayWidth = 1.6;
        double halfwayX = awaySide ? 0.0 : Math.max(0.0, width - halfwayWidth);

        double touchlineX = awaySide ? 0.0 : fieldInset;
        double touchlineWidth = Math.max(0.0, width - fieldInset);
        touchline.resizeRelocate(touchlineX, fieldTop, touchlineWidth, fieldHeight);
        penaltyBox.resizeRelocate(penaltyX, penaltyY, penaltyWidth, penaltyHeight);
        sixYardBox.resizeRelocate(sixX, sixY, sixWidth, sixHeight);
        goalBox.resizeRelocate(goalX, goalY, goalWidth, goalHeight);
        halfwayLine.resizeRelocate(halfwayX, fieldTop, halfwayWidth, fieldHeight);

        penaltySpot.setRadius(2.4);
        penaltySpot.setCenterY(height / 2.0);
        double penaltySpotX = awaySide
                ? goalLineX - Math.min(72.0, Math.max(58.0, width * 0.12))
                : goalLineX + Math.min(72.0, Math.max(58.0, width * 0.12));
        penaltySpot.setCenterX(penaltySpotX);

        centerCircle.setRadius(Math.min(58.0, Math.max(42.0, height * 0.11)));
        centerCircle.setCenterY(height / 2.0);
        centerCircle.setCenterX(awaySide ? 0.0 : width);

        centerSpot.setRadius(2.6);
        centerSpot.setCenterY(height / 2.0);
        centerSpot.setCenterX(awaySide ? 0.0 : width);

        double penaltyArcRadius = Math.min(54.0, Math.max(36.0, height * 0.108));
        penaltyArc.setCenterX(penaltySpotX);
        penaltyArc.setCenterY(height / 2.0);
        penaltyArc.setRadiusX(penaltyArcRadius);
        penaltyArc.setRadiusY(penaltyArcRadius);
        penaltyArc.setStartAngle(awaySide ? 127.0 : 307.0);
        penaltyArc.setLength(106.0);

        double cornerRadius = Math.min(18.0, Math.max(12.0, height * 0.036));
        topCornerArc.setRadiusX(cornerRadius);
        topCornerArc.setRadiusY(cornerRadius);
        topCornerArc.setCenterY(fieldTop);
        topCornerArc.setCenterX(goalLineX);
        topCornerArc.setStartAngle(awaySide ? 180.0 : 270.0);
        topCornerArc.setLength(90.0);

        bottomCornerArc.setRadiusX(cornerRadius);
        bottomCornerArc.setRadiusY(cornerRadius);
        bottomCornerArc.setCenterY(fieldBottom);
        bottomCornerArc.setCenterX(goalLineX);
        bottomCornerArc.setStartAngle(awaySide ? 90.0 : 0.0);
        bottomCornerArc.setLength(90.0);
    }

    private void showPitchPlaceholder(Pane pitchContainer, String message) {
        clearPitchContent(pitchContainer);
        StackPane placeholder = buildPitchPlaceholder(message);
        placeholder.setManaged(false);
        pitchContainer.getChildren().add(placeholder);

        Runnable layout = () -> {
            double width = resolvePitchWidth(pitchContainer);
            double height = resolvePitchHeight(pitchContainer);
            double placeholderWidth = Math.min(280.0, Math.max(220.0, width - 48.0));
            double placeholderHeight = 90.0;
            placeholder.resizeRelocate(
                    (width - placeholderWidth) / 2.0,
                    (height - placeholderHeight) / 2.0,
                    placeholderWidth,
                    placeholderHeight
            );
        };
        pitchContainer.getProperties().put(PITCH_LAYOUT_KEY, layout);
        layout.run();
        Platform.runLater(layout);
    }

    private StackPane buildPitchPlaceholder(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("pitch-placeholder-label");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);

        StackPane placeholder = new StackPane(label);
        placeholder.getStyleClass().add("pitch-placeholder-card");
        return placeholder;
    }

    private StackPane buildBenchPlaceholder(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("bench-placeholder-label");
        label.setWrapText(true);

        StackPane placeholder = new StackPane(label);
        placeholder.getStyleClass().add("bench-placeholder-chip");
        return placeholder;
    }

    private VBox buildPitchPlayerMarker(ApiFootballLineupPlayer player) {
        String displayName = buildPitchPlayerDisplayName(player == null ? null : player.playerName());
        double namePillWidth = estimatePitchNameWidth(displayName);
        boolean mvpPlayer = isMvpPlayer(player);

        ImageView photoView = new ImageView();
        photoView.setFitWidth(42);
        photoView.setFitHeight(48);
        photoView.setPreserveRatio(true);
        photoView.setSmooth(true);

        Label fallbackLabel = new Label(buildPlayerInitials(player == null ? null : player.playerName()));
        fallbackLabel.getStyleClass().add("pitch-player-photo-fallback");

        StackPane photoShell = new StackPane(photoView, fallbackLabel);
        photoShell.getStyleClass().add("pitch-player-photo-shell");
        bindPlayerPhoto(photoView, fallbackLabel, player);

        Label ratingLabel = buildRatingBadge(player == null ? null : player.rating());
        if (ratingLabel != null) {
            StackPane.setAlignment(ratingLabel, Pos.TOP_LEFT);
            photoShell.getChildren().add(ratingLabel);
        }

        VBox eventChips = buildPlayerEventPills(player);
        int eventChipCount = eventChips.getChildren().size();
        boolean hasEventChips = eventChipCount > 0;

        Label shirtLabel = new Label(emptyToFallback(player == null ? null : player.shirtNumber(), "?"));
        shirtLabel.getStyleClass().add("pitch-player-number-badge");
        shirtLabel.setAlignment(Pos.CENTER);
        shirtLabel.setMinWidth(16);
        shirtLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        shirtLabel.setMaxWidth(Region.USE_PREF_SIZE);

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("pitch-player-name-text");
        nameLabel.setWrapText(false);
        nameLabel.setTextOverrun(OverrunStyle.CLIP);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        nameLabel.setMinWidth(Region.USE_PREF_SIZE);
        nameLabel.setPrefWidth(namePillWidth);
        nameLabel.setMaxWidth(namePillWidth);

        HBox identityPill = new HBox(4, shirtLabel, nameLabel);
        identityPill.setAlignment(Pos.CENTER_LEFT);
        identityPill.getStyleClass().add("pitch-player-identity-pill");
        double identityWidth = computeIdentityPillWidth(namePillWidth);
        identityPill.setMinWidth(identityWidth);
        identityPill.setPrefWidth(identityWidth);
        identityPill.setMaxWidth(identityWidth);

        HBox tagRow = new HBox(3);
        tagRow.setAlignment(Pos.CENTER_LEFT);
        tagRow.getStyleClass().add("pitch-player-pill-row");
        if (hasEventChips) {
            tagRow.getChildren().add(eventChips);
        }
        tagRow.getChildren().add(identityPill);
        if (mvpPlayer) {
            tagRow.getChildren().add(buildMvpBadge(player));
        }

        VBox marker = new VBox(1, photoShell, tagRow);
        marker.setAlignment(Pos.TOP_CENTER);
        marker.getStyleClass().add("pitch-player-marker");
        double markerWidth = computePitchMarkerWidth(identityWidth, eventChipCount, mvpPlayer);
        tagRow.setMinWidth(markerWidth);
        tagRow.setPrefWidth(markerWidth);
        tagRow.setMaxWidth(markerWidth);
        marker.setPrefSize(markerWidth, PITCH_MARKER_HEIGHT);
        marker.setMinSize(markerWidth, PITCH_MARKER_HEIGHT);
        marker.setMaxSize(markerWidth, PITCH_MARKER_HEIGHT);
        return marker;
    }

    private Label buildRatingBadge(Double rating) {
        if (rating == null) {
            return null;
        }

        Label ratingLabel = new Label(formatRating(rating));
        ratingLabel.getStyleClass().addAll("player-rating-badge", ratingToneStyle(rating));
        return ratingLabel;
    }

    private Label buildMvpBadge(ApiFootballLineupPlayer player) {
        if (!isMvpPlayer(player)) {
            return null;
        }
        Label label = new Label("★");
        label.getStyleClass().add("player-mvp-badge");
        label.setText("\u2605");
        return label;
    }

    private VBox buildPlayerEventPills(ApiFootballLineupPlayer player) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("player-event-pill-row");
        PlayerIncidentSummary summary = summarizePlayerIncidents(player);
        appendEventPills(box, summary.goals(), "G", "player-event-pill-goal");
        appendEventPills(box, summary.assists(), "A", "player-event-pill-assist");
        appendEventPills(box, summary.yellowCards(), "Y", "player-event-pill-yellow");
        appendEventPills(box, summary.redCards(), "R", "player-event-pill-red");
        return box;
    }

    private void appendEventPills(VBox box, int count, String label, String styleClass) {
        if (box == null || count <= 0) {
            return;
        }
        Label pill = new Label(count > 1 ? label + "x" + count : label);
        pill.getStyleClass().addAll("player-event-pill", styleClass);
        box.getChildren().add(pill);
    }

    private PlayerIncidentSummary summarizePlayerIncidents(ApiFootballLineupPlayer player) {
        if (player == null || currentIncidents == null || currentIncidents.isEmpty()) {
            return PlayerIncidentSummary.EMPTY;
        }

        int goals = 0;
        int assists = 0;
        int yellowCards = 0;
        int redCards = 0;
        for (ApiFootballMatchIncident incident : currentIncidents) {
            if (incident == null) {
                continue;
            }
            if (matchesPlayer(player, incident.playerId(), incident.playerName())) {
                if (incident.isGoal()) {
                    goals++;
                }
                if (incident.isYellowCard()) {
                    yellowCards++;
                }
                if (incident.isRedCard()) {
                    redCards++;
                }
            }
            if (matchesPlayer(player, incident.assistPlayerId(), incident.assistPlayerName())) {
                assists++;
            }
        }
        return new PlayerIncidentSummary(goals, assists, yellowCards, redCards);
    }

    private void selectMatchMvp(ApiFootballLineupSide homeLineup, ApiFootballLineupSide awayLineup) {
        currentMvpPlayerId = null;
        currentMvpPlayerNameKey = null;

        ApiFootballLineupPlayer bestPlayer = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ApiFootballLineupPlayer player : allRatedPlayers(homeLineup, awayLineup)) {
            if (player == null || player.rating() == null) {
                continue;
            }
            PlayerIncidentSummary summary = summarizePlayerIncidents(player);
            double score = player.rating()
                    + (summary.goals() * 0.10)
                    + (summary.assists() * 0.05)
                    - (summary.redCards() * 0.08)
                    - (summary.yellowCards() * 0.03);
            if (score > bestScore) {
                bestScore = score;
                bestPlayer = player;
            }
        }

        if (bestPlayer != null) {
            currentMvpPlayerId = bestPlayer.playerId();
            currentMvpPlayerNameKey = normalizeComparableName(bestPlayer.playerName());
        }
    }

    private List<ApiFootballLineupPlayer> allRatedPlayers(ApiFootballLineupSide homeLineup, ApiFootballLineupSide awayLineup) {
        List<ApiFootballLineupPlayer> players = new ArrayList<>();
        addLineupPlayers(players, homeLineup);
        addLineupPlayers(players, awayLineup);
        return players;
    }

    private void addLineupPlayers(List<ApiFootballLineupPlayer> target, ApiFootballLineupSide lineup) {
        if (lineup == null) {
            return;
        }
        if (lineup.startingPlayers() != null) {
            target.addAll(lineup.startingPlayers());
        }
        if (lineup.substitutePlayers() != null) {
            target.addAll(lineup.substitutePlayers());
        }
    }

    private boolean isMvpPlayer(ApiFootballLineupPlayer player) {
        if (player == null) {
            return false;
        }
        if (currentMvpPlayerId != null && player.playerId() != null) {
            return currentMvpPlayerId.equals(player.playerId());
        }
        return currentMvpPlayerNameKey != null
                && currentMvpPlayerNameKey.equals(normalizeComparableName(player.playerName()));
    }

    private ApiFootballLineupPlayer findCurrentMvpPlayer() {
        for (ApiFootballLineupPlayer player : allRatedPlayers(currentHomeLineup, currentAwayLineup)) {
            if (isMvpPlayer(player)) {
                return player;
            }
        }
        return null;
    }

    private boolean lineupContainsPlayer(ApiFootballLineupSide lineup, ApiFootballLineupPlayer targetPlayer) {
        if (lineup == null || targetPlayer == null) {
            return false;
        }

        if (lineup.startingPlayers() != null) {
            for (ApiFootballLineupPlayer player : lineup.startingPlayers()) {
                if (matchesPlayer(player, targetPlayer.playerId(), targetPlayer.playerName())) {
                    return true;
                }
            }
        }
        if (lineup.substitutePlayers() != null) {
            for (ApiFootballLineupPlayer player : lineup.substitutePlayers()) {
                if (matchesPlayer(player, targetPlayer.playerId(), targetPlayer.playerName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPlayer(ApiFootballLineupPlayer player, Long playerId, String playerName) {
        if (player == null) {
            return false;
        }
        if (player.playerId() != null && playerId != null) {
            return player.playerId().equals(playerId);
        }
        String left = normalizeComparableName(player.playerName());
        String right = normalizeComparableName(playerName);
        return left != null && left.equals(right);
    }

    private String normalizeComparableName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String formatRating(Double rating) {
        return String.format(java.util.Locale.US, "%.1f", rating);
    }

    private double estimatePitchNameWidth(String displayName) {
        String value = emptyToNull(displayName);
        if (value == null) {
            return 36.0;
        }
        double width = 10.0;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (Character.isWhitespace(ch)) {
                width += 2.0;
            } else if (Character.isUpperCase(ch)) {
                width += 6.4;
            } else if ("mwMW".indexOf(ch) >= 0) {
                width += 6.7;
            } else if ("ijlItfr".indexOf(ch) >= 0) {
                width += 4.1;
            } else {
                width += 5.3;
            }
        }
        return Math.max(28.0, Math.min(70.0, width));
    }

    private double computeIdentityPillWidth(double namePillWidth) {
        return Math.max(50.0, Math.min(92.0, 14.0 + 4.0 + namePillWidth + 12.0));
    }

    private double computePitchMarkerWidth(double identityWidth, int eventChipCount, boolean mvpPlayer) {
        double width = identityWidth;
        if (eventChipCount > 0) {
            width += 14.0;
        }
        if (mvpPlayer) {
            width += 14.0;
        }
        return Math.max(PITCH_MARKER_MIN_WIDTH, Math.min(PITCH_MARKER_WIDTH, width));
    }

    private String ratingToneStyle(Double rating) {
        if (rating == null) {
            return "player-rating-neutral";
        }
        if (rating >= 9.0) {
            return "player-rating-elite";
        }
        if (rating >= 8.0) {
            return "player-rating-good";
        }
        if (rating >= 7.0) {
            return "player-rating-mid";
        }
        return "player-rating-low";
    }

    private void bindPlayerPhoto(ImageView photoView, Label fallbackLabel, ApiFootballLineupPlayer player) {
        String photoPath = resolvePlayerPhotoPath(player);
        if (photoPath == null) {
            updatePlayerPhotoVisibility(photoView, fallbackLabel, null);
            return;
        }

        Image cachedImage = PLAYER_PHOTO_CACHE.get(photoPath);
        if (cachedImage != null) {
            updatePlayerPhotoVisibility(photoView, fallbackLabel, cachedImage);
            return;
        }

        if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
            updatePlayerPhotoVisibility(photoView, fallbackLabel, null);
            CompletableFuture<Image> request = PLAYER_PHOTO_REQUESTS.computeIfAbsent(
                    photoPath,
                    path -> CompletableFuture.supplyAsync(() -> fetchPlayerPhoto(path), PHOTO_EXECUTOR)
                            .whenComplete((image, error) -> {
                                if (image != null) {
                                    PLAYER_PHOTO_CACHE.put(path, image);
                                }
                                PLAYER_PHOTO_REQUESTS.remove(path);
                            })
            );
            request.whenComplete((image, error) -> Platform.runLater(() ->
                    updatePlayerPhotoVisibility(photoView, fallbackLabel, error == null ? image : null)));
            return;
        }

        updatePlayerPhotoVisibility(photoView, fallbackLabel, loadImage(photoPath));
    }

    private void updatePlayerPhotoVisibility(ImageView photoView, Label fallbackLabel, Image image) {
        boolean ready = image != null && !image.isError();
        photoView.setImage(image);
        photoView.setVisible(ready);
        photoView.setManaged(ready);
        fallbackLabel.setVisible(!ready);
        fallbackLabel.setManaged(!ready);
    }

    private Image fetchPlayerPhoto(String imageSource) {
        BufferedImage bufferedImage = fetchRemoteBufferedImage(imageSource);
        if (bufferedImage != null) {
            return SwingFXUtils.toFXImage(makeLightBackgroundTransparent(bufferedImage), null);
        }
        return createImage(imageSource);
    }

    private BufferedImage fetchRemoteBufferedImage(String imageSource) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageSource))
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Referer", "https://www.sofascore.com/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = PHOTO_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
                return null;
            }

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(response.body())) {
                return ImageIO.read(inputStream);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage makeLightBackgroundTransparent(BufferedImage source) {
        if (source == null) {
            return null;
        }

        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = converted.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        BackgroundProfile profile = analyzeBackgroundProfile(converted);
        boolean edgeWhiteBackdrop = hasStrongWhiteEdges(converted);
        if ((profile == null || !profile.isLightNeutral()) && !edgeWhiteBackdrop) {
            return converted;
        }
        if (profile == null) {
            profile = BackgroundProfile.defaultLight();
        }

        int width = converted.getWidth();
        int height = converted.getHeight();
        boolean[][] backgroundMask = new boolean[height][width];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        seedBackgroundFloodFill(converted, profile, backgroundMask, queue);
        if (queue.isEmpty()) {
            return converted;
        }

        floodFillBackground(converted, profile, backgroundMask, queue);
        trimConnectedLightBackgroundFromEdges(converted, profile, backgroundMask);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = converted.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                double nx = (x + 0.5) / width;
                double ny = (y + 0.5) / height;
                double subjectSupport = portraitSubjectSupport(nx, ny);
                double whiteKitSupport = whiteKitPreserveSupport(nx, ny);
                double backgroundStrength = backgroundStrength(argb, profile);
                if (backgroundMask[y][x]) {
                    double fade = clamp01((backgroundStrength - 0.58) / 0.24);
                    int nextAlpha = (int) Math.round(alpha * (1.0 - fade));
                    converted.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
                    continue;
                }

                if (backgroundStrength >= 0.76) {
                    double preserve = clamp01((subjectSupport - 0.12) / 0.28);
                    if (preserve < 0.995) {
                        int nextAlpha = (int) Math.round(alpha * preserve);
                        converted.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
                        continue;
                    }
                }

                if (backgroundStrength >= 0.74 && whiteKitSupport < 0.92) {
                    double preserve = clamp01((whiteKitSupport - 0.18) / 0.74);
                    if (preserve < 0.995) {
                        int nextAlpha = (int) Math.round(alpha * preserve);
                        converted.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
                        continue;
                    }
                }

                if (touchesBackground(backgroundMask, x, y)) {
                    double fringeFade = clamp01((backgroundStrength - 0.70) / 0.22) * 0.55;
                    if (fringeFade > 0.0) {
                        int nextAlpha = (int) Math.round(alpha * (1.0 - fringeFade));
                        converted.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
                    }
                }
            }
        }

        softenBrightFringe(converted, profile);
        return converted;
    }

    private void softenBrightFringe(BufferedImage image, BackgroundProfile profile) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0 || !touchesTransparentPixel(image, x, y)) {
                    continue;
                }

                double nx = (x + 0.5) / width;
                double ny = (y + 0.5) / height;
                double subjectSupport = portraitSubjectSupport(nx, ny);
                double whiteKitSupport = whiteKitPreserveSupport(nx, ny);
                double backgroundStrength = backgroundStrength(argb, profile);
                double preserve = Math.max(subjectSupport, whiteKitSupport);
                if (backgroundStrength < 0.58 || preserve >= 0.94) {
                    continue;
                }

                double fade = clamp01((backgroundStrength - 0.58) / 0.20)
                        * clamp01((0.94 - preserve) / 0.34);
                if (fade <= 0.0) {
                    continue;
                }

                int nextAlpha = (int) Math.round(alpha * (1.0 - Math.min(0.92, fade)));
                if (nextAlpha < 26) {
                    nextAlpha = 0;
                }
                image.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
            }
        }
    }

    private void seedBackgroundFloodFill(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask,
            ArrayDeque<int[]> queue
    ) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            trySeedBackgroundPixel(image, profile, backgroundMask, queue, x, 0, true);
        }

        for (int y = 1; y < height; y++) {
            trySeedBackgroundPixel(image, profile, backgroundMask, queue, 0, y, true);
            trySeedBackgroundPixel(image, profile, backgroundMask, queue, width - 1, y, true);
        }
    }

    private void floodFillBackground(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask,
            ArrayDeque<int[]> queue
    ) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] offsetX = {1, -1, 0, 0};
        int[] offsetY = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int x = point[0];
            int y = point[1];

            for (int direction = 0; direction < offsetX.length; direction++) {
                int nextX = x + offsetX[direction];
                int nextY = y + offsetY[direction];
                if (nextX < 0 || nextY < 0 || nextX >= width || nextY >= height || backgroundMask[nextY][nextX]) {
                    continue;
                }

                trySeedBackgroundPixel(image, profile, backgroundMask, queue, nextX, nextY, false);
            }
        }
    }

    private void trimConnectedLightBackgroundFromEdges(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask
    ) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            trimRowFromEdge(image, profile, backgroundMask, y, 0, width, 1);
            trimRowFromEdge(image, profile, backgroundMask, y, width - 1, -1, -1);
        }

        for (int x = 0; x < width; x++) {
            trimColumnFromTop(image, profile, backgroundMask, x, height);
        }
    }

    private void trimRowFromEdge(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask,
            int y,
            int startX,
            int stopExclusive,
            int step
    ) {
        for (int x = startX; x != stopExclusive; x += step) {
            if (!isStrongEdgeBackground(image.getRGB(x, y), profile)) {
                break;
            }
            backgroundMask[y][x] = true;
        }
    }

    private void trimColumnFromTop(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask,
            int x,
            int height
    ) {
        for (int y = 0; y < height; y++) {
            if (!isStrongEdgeBackground(image.getRGB(x, y), profile)) {
                break;
            }
            backgroundMask[y][x] = true;
        }
    }

    private void trySeedBackgroundPixel(
            BufferedImage image,
            BackgroundProfile profile,
            boolean[][] backgroundMask,
            ArrayDeque<int[]> queue,
            int x,
            int y,
            boolean borderSeed
    ) {
        if (!isBackgroundFloodCandidate(image.getRGB(x, y), profile, x, y, image.getWidth(), image.getHeight(), borderSeed)) {
            return;
        }

        backgroundMask[y][x] = true;
        queue.addLast(new int[]{x, y});
    }

    private boolean isBackgroundFloodCandidate(
            int argb,
            BackgroundProfile profile,
            int x,
            int y,
            int width,
            int height,
            boolean borderSeed
    ) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 148) {
            return false;
        }

        double nx = (x + 0.5) / width;
        double ny = (y + 0.5) / height;
        double subjectSupport = portraitSubjectSupport(nx, ny);
        double whiteKitSupport = whiteKitPreserveSupport(nx, ny);
        double backgroundStrength = backgroundStrength(argb, profile);

        if (borderSeed) {
            if (subjectSupport >= 0.22) {
                return false;
            }
            return backgroundStrength >= 0.68;
        }

        if (backgroundStrength >= 0.92 && whiteKitSupport < 0.82) {
            return true;
        }

        if (subjectSupport >= 0.36) {
            return false;
        }

        if (subjectSupport >= 0.18) {
            if (backgroundStrength >= 0.86 && whiteKitSupport < 0.78) {
                return true;
            }
            return backgroundStrength >= 0.82;
        }

        return backgroundStrength >= 0.70;
    }

    private boolean touchesBackground(boolean[][] backgroundMask, int x, int y) {
        int height = backgroundMask.length;
        int width = height == 0 ? 0 : backgroundMask[0].length;
        for (int deltaY = -1; deltaY <= 1; deltaY++) {
            for (int deltaX = -1; deltaX <= 1; deltaX++) {
                if (deltaX == 0 && deltaY == 0) {
                    continue;
                }

                int nextX = x + deltaX;
                int nextY = y + deltaY;
                if (nextX < 0 || nextY < 0 || nextX >= width || nextY >= height) {
                    continue;
                }

                if (backgroundMask[nextY][nextX]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean touchesTransparentPixel(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int deltaY = -1; deltaY <= 1; deltaY++) {
            for (int deltaX = -1; deltaX <= 1; deltaX++) {
                if (deltaX == 0 && deltaY == 0) {
                    continue;
                }
                int nextX = x + deltaX;
                int nextY = y + deltaY;
                if (nextX < 0 || nextY < 0 || nextX >= width || nextY >= height) {
                    return true;
                }
                int neighborAlpha = (image.getRGB(nextX, nextY) >>> 24) & 0xFF;
                if (neighborAlpha == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isStrongEdgeBackground(int argb, BackgroundProfile profile) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 148) {
            return false;
        }
        return backgroundStrength(argb, profile) >= 0.82;
    }

    private BackgroundProfile analyzeBackgroundProfile(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int sampleRows = Math.max(8, Math.min(height / 5, 24));
        int edgeBand = Math.max(8, Math.min(width / 6, 24));

        long redTotal = 0L;
        long greenTotal = 0L;
        long blueTotal = 0L;
        long brightnessTotal = 0L;
        long chromaTotal = 0L;
        int count = 0;

        for (int y = 0; y < sampleRows; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= edgeBand && x < width - edgeBand && y >= sampleRows / 2) {
                    continue;
                }

                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < 140) {
                    continue;
                }

                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                int max = Math.max(red, Math.max(green, blue));
                int min = Math.min(red, Math.min(green, blue));
                redTotal += red;
                greenTotal += green;
                blueTotal += blue;
                brightnessTotal += (red + green + blue) / 3L;
                chromaTotal += (max - min);
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return new BackgroundProfile(
                (int) (redTotal / count),
                (int) (greenTotal / count),
                (int) (blueTotal / count),
                (int) (brightnessTotal / count),
                (int) (chromaTotal / count)
        );
    }

    private boolean hasStrongWhiteEdges(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }

        int topRows = Math.max(6, Math.min(height / 4, 26));
        int sideCols = Math.max(4, Math.min(width / 10, 12));
        int samples = 0;
        int strong = 0;

        for (int y = 0; y < topRows; y++) {
            for (int x = 0; x < width; x++) {
                samples++;
                if (absoluteLightBackgroundStrength(image.getRGB(x, y)) >= 0.80) {
                    strong++;
                }
            }
        }

        for (int y = topRows; y < height; y++) {
            for (int x = 0; x < sideCols; x++) {
                samples++;
                if (absoluteLightBackgroundStrength(image.getRGB(x, y)) >= 0.80) {
                    strong++;
                }
            }
            for (int x = width - sideCols; x < width; x++) {
                if (x < 0 || x >= width) {
                    continue;
                }
                samples++;
                if (absoluteLightBackgroundStrength(image.getRGB(x, y)) >= 0.80) {
                    strong++;
                }
            }
        }

        return samples > 0 && ((double) strong / (double) samples) >= 0.48;
    }

    private double portraitSubjectSupport(double nx, double ny) {
        double head = ellipseSupport(nx, ny, 0.50, 0.20, 0.20, 0.18, 0.10);
        double shoulders = ellipseSupport(nx, ny, 0.50, 0.44, 0.38, 0.24, 0.14);
        double torso = ellipseSupport(nx, ny, 0.50, 0.76, 0.34, 0.28, 0.12);
        double neck = verticalBandSupport(nx, ny, 0.50, 0.54, 0.22, 0.26, 0.14, 0.10);
        return clamp01(Math.max(Math.max(head, shoulders), Math.max(torso, neck)));
    }

    private double whiteKitPreserveSupport(double nx, double ny) {
        double torsoCore = ellipseSupport(nx, ny, 0.50, 0.82, 0.20, 0.18, 0.10);
        double torsoWide = ellipseSupport(nx, ny, 0.50, 0.76, 0.26, 0.20, 0.10);
        double lowerBand = verticalBandSupport(nx, ny, 0.50, 0.84, 0.18, 0.18, 0.10, 0.10);
        return clamp01(Math.max(Math.max(torsoCore, torsoWide), lowerBand));
    }

    private double ellipseSupport(double nx, double ny, double cx, double cy, double rx, double ry, double feather) {
        double dx = (nx - cx) / rx;
        double dy = (ny - cy) / ry;
        double distance = Math.sqrt((dx * dx) + (dy * dy));
        return smoothInside(1.0 - distance, feather);
    }

    private double verticalBandSupport(double nx, double ny, double cx, double cy, double halfWidth, double halfHeight, double edgeFeather, double verticalFeather) {
        double horizontal = smoothInside(halfWidth - Math.abs(nx - cx), edgeFeather);
        double vertical = smoothInside(halfHeight - Math.abs(ny - cy), verticalFeather);
        return horizontal * vertical;
    }

    private double smoothInside(double distanceToEdge, double feather) {
        if (distanceToEdge <= -feather) {
            return 0.0;
        }
        if (distanceToEdge >= feather) {
            return 1.0;
        }
        double normalized = (distanceToEdge + feather) / (feather * 2.0);
        return smoothstep(normalized);
    }

    private double backgroundStrength(int argb, BackgroundProfile profile) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 140) {
            return 1.0;
        }

        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int brightness = (red + green + blue) / 3;
        int chroma = max - min;
        int colorDistance = Math.abs(red - profile.red())
                + Math.abs(green - profile.green())
                + Math.abs(blue - profile.blue());
        double brightnessScore = clamp01((brightness - Math.max(218, profile.brightness() - 18)) / 26.0);
        double chromaScore = 1.0 - clamp01((chroma - Math.max(12, profile.chroma() + 4)) / 26.0);
        double distanceScore = 1.0 - clamp01((colorDistance - Math.max(10, profile.chroma() * 3)) / 72.0);
        double profileScore = clamp01((brightnessScore * 0.45) + (chromaScore * 0.30) + (distanceScore * 0.25));
        return Math.max(profileScore, absoluteLightBackgroundStrength(argb));
    }

    private double absoluteLightBackgroundStrength(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 140) {
            return 1.0;
        }

        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int brightness = (red + green + blue) / 3;
        int chroma = max - min;
        double brightnessScore = clamp01((brightness - 214) / 34.0);
        double chromaScore = 1.0 - clamp01((chroma - 22) / 40.0);
        return clamp01((brightnessScore * 0.72) + (chromaScore * 0.28));
    }

    private double smoothstep(double value) {
        double clamped = clamp01(value);
        return clamped * clamped * (3.0 - (2.0 * clamped));
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record BackgroundProfile(int red, int green, int blue, int brightness, int chroma) {
        private boolean isLightNeutral() {
            return brightness >= 220 && chroma <= 26;
        }

        private static BackgroundProfile defaultLight() {
            return new BackgroundProfile(245, 245, 245, 245, 6);
        }
    }

    private HBox buildBenchPlayerCard(ApiFootballLineupPlayer player) {
        Label shirtLabel = new Label(emptyToFallback(player == null ? null : player.shirtNumber(), "?"));
        shirtLabel.getStyleClass().add("bench-player-number");

        Label nameLabel = new Label(buildPitchPlayerDisplayName(player == null ? null : player.playerName()));
        nameLabel.getStyleClass().add("bench-player-name");
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setMaxWidth(160);

        HBox card = new HBox(8, shirtLabel, nameLabel);
        Label ratingLabel = buildRatingBadge(player == null ? null : player.rating());
        if (ratingLabel != null) {
            ratingLabel.getStyleClass().add("bench-rating-badge");
            card.getChildren().add(ratingLabel);
        }
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("bench-player-chip");
        return card;
    }

    private void layoutPitchMarkers(
            Pane pitchContainer,
            List<List<ApiFootballLineupPlayer>> rows,
            List<VBox> markers,
            boolean awaySide
    ) {
        if (rows.isEmpty() || markers.isEmpty()) {
            return;
        }

        double width = resolvePitchWidth(pitchContainer);
        double height = resolvePitchHeight(pitchContainer);
        double maxMarkerWidth = markers.stream()
                .mapToDouble(this::resolvePitchMarkerWidth)
                .max()
                .orElse(PITCH_MARKER_WIDTH);
        double maxMarkerHeight = markers.stream()
                .mapToDouble(this::resolvePitchMarkerHeight)
                .max()
                .orElse(PITCH_MARKER_HEIGHT);
        double halfMarkerWidth = maxMarkerWidth / 2.0;
        double halfMarkerHeight = maxMarkerHeight / 2.0;
        double minX = PITCH_FIELD_LINE_INSET + halfMarkerWidth + 4.0;
        double maxX = width - PITCH_FIELD_LINE_INSET - halfMarkerWidth - 4.0;
        double minY = PITCH_FIELD_LINE_INSET + halfMarkerHeight + 4.0;
        double maxY = height - PITCH_FIELD_LINE_INSET - halfMarkerHeight - 4.0;
        double usableWidth = Math.max(120.0, maxX - minX);
        double usableHeight = Math.max(180.0, maxY - minY);

        int markerIndex = 0;
        int rowCount = rows.size();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<ApiFootballLineupPlayer> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }

            double depthRatio = rowCount == 1 ? 0.5 : (double) rowIndex / (double) (rowCount - 1);
            double xCenter = minX + (usableWidth * depthRatio);
            if (awaySide) {
                xCenter = maxX - (usableWidth * depthRatio);
            }

            for (int playerIndex = 0; playerIndex < row.size() && markerIndex < markers.size(); playerIndex++) {
                VBox marker = markers.get(markerIndex++);
                double markerWidth = resolvePitchMarkerWidth(marker);
                double markerHeight = resolvePitchMarkerHeight(marker);
                double yCenter = minY + (usableHeight * verticalSlotRatio(playerIndex, row.size()));
                yCenter = Math.max(minY, Math.min(maxY, yCenter));
                marker.resizeRelocate(
                        xCenter - (markerWidth / 2.0),
                        yCenter - (markerHeight / 2.0),
                        markerWidth,
                        markerHeight
                );
            }
        }
    }

    private double verticalSlotRatio(int playerIndex, int rowSize) {
        if (rowSize <= 1) {
            return 0.50;
        }

        double[] template = switch (rowSize) {
            case 2 -> new double[]{0.39, 0.61};
            case 3 -> new double[]{0.24, 0.50, 0.76};
            case 4 -> new double[]{0.16, 0.39, 0.61, 0.84};
            case 5 -> new double[]{0.10, 0.30, 0.50, 0.70, 0.90};
            default -> null;
        };

        if (template != null && playerIndex >= 0 && playerIndex < template.length) {
            return template[playerIndex];
        }

        return 0.10 + ((double) playerIndex / (double) (Math.max(1, rowSize - 1)) * 0.80);
    }

    private double resolvePitchMarkerWidth(VBox marker) {
        if (marker == null) {
            return PITCH_MARKER_WIDTH;
        }
        double prefWidth = marker.getPrefWidth();
        return prefWidth > 0 ? prefWidth : PITCH_MARKER_WIDTH;
    }

    private double resolvePitchMarkerHeight(VBox marker) {
        if (marker == null) {
            return PITCH_MARKER_HEIGHT;
        }
        double prefHeight = marker.getPrefHeight();
        return prefHeight > 0 ? prefHeight : PITCH_MARKER_HEIGHT;
    }

    private double resolvePitchWidth(Pane pitchContainer) {
        return pitchContainer.getWidth() > 0
                ? pitchContainer.getWidth()
                : Math.max(420.0, pitchContainer.getPrefWidth());
    }

    private double resolvePitchHeight(Pane pitchContainer) {
        return pitchContainer.getHeight() > 0
                ? pitchContainer.getHeight()
                : Math.max(480.0, pitchContainer.getPrefHeight());
    }

    private String resolvePlayerPhotoPath(ApiFootballLineupPlayer player) {
        String photoUrl = player == null ? null : emptyToNull(player.photoUrl());
        if (photoUrl == null) {
            return null;
        }
        return photoUrl;
    }

    private String buildPlayerInitials(String playerName) {
        String normalizedName = emptyToNull(playerName);
        if (normalizedName == null) {
            return "?";
        }

        String[] parts = normalizedName.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String buildPitchPlayerDisplayName(String playerName) {
        String normalizedName = emptyToNull(playerName);
        if (normalizedName == null) {
            return "Joueur";
        }

        String[] parts = normalizedName.split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }

        String candidate = parts[parts.length - 1];
        String lowerCandidate = candidate.toLowerCase(java.util.Locale.ROOT);
        if (List.of("jr", "junior", "júnior", "ii", "iii", "iv", "v").contains(lowerCandidate) && parts.length >= 2) {
            candidate = parts[parts.length - 2];
        }
        if (candidate.length() > 12 && candidate.contains("-")) {
            String[] hyphenParts = candidate.split("-");
            String tail = hyphenParts[hyphenParts.length - 1];
            if (tail.length() >= 3) {
                candidate = tail;
            }
        }
        return candidate;
    }

    private List<List<ApiFootballLineupPlayer>> buildPitchRows(ApiFootballLineupSide lineup) {
        if (lineup == null || !lineup.hasStartingPlayers()) {
            return List.of();
        }

        List<ApiFootballLineupPlayer> starters = new ArrayList<>(lineup.startingPlayers());
        starters.removeIf(java.util.Objects::isNull);
        if (starters.isEmpty()) {
            return List.of();
        }

        List<List<ApiFootballLineupPlayer>> rows = new ArrayList<>();
        int goalkeeperIndex = findGoalkeeperIndex(starters);
        if (goalkeeperIndex >= 0 && goalkeeperIndex < starters.size()) {
            ApiFootballLineupPlayer goalkeeper = starters.remove(goalkeeperIndex);
            if (goalkeeper != null) {
                rows.add(List.of(goalkeeper));
            }
        }
        rows.addAll(inferOutfieldRows(lineup.formation(), starters));
        return rows;
    }

    private List<List<ApiFootballLineupPlayer>> inferOutfieldRows(List<ApiFootballLineupPlayer> starters) {
        if (starters == null || starters.isEmpty()) {
            return List.of();
        }
        List<ApiFootballLineupPlayer> outfield = new ArrayList<>(starters);
        int goalkeeperIndex = findGoalkeeperIndex(outfield);
        if (goalkeeperIndex >= 0 && goalkeeperIndex < outfield.size()) {
            outfield.remove(goalkeeperIndex);
        }
        return inferOutfieldRows(null, outfield);
    }

    private List<List<ApiFootballLineupPlayer>> inferOutfieldRows(String formation, List<ApiFootballLineupPlayer> outfieldPlayers) {
        if (outfieldPlayers == null || outfieldPlayers.isEmpty()) {
            return List.of();
        }

        List<List<ApiFootballLineupPlayer>> rows = buildRowsFromGrid(outfieldPlayers);
        if (!rows.isEmpty()) {
            return rows;
        }

        rows = buildRowsFromFormation(formation, outfieldPlayers);
        if (!rows.isEmpty()) {
            return rows;
        }

        rows = buildRowsFromPositionBands(outfieldPlayers);
        if (!rows.isEmpty()) {
            return rows;
        }

        return buildRowsFromFallback(outfieldPlayers);
    }

    private List<List<ApiFootballLineupPlayer>> buildRowsFromGrid(List<ApiFootballLineupPlayer> outfieldPlayers) {
        Map<Integer, List<GridPlacement>> rowsByIndex = new java.util.TreeMap<>();
        for (ApiFootballLineupPlayer player : outfieldPlayers) {
            GridPlacement placement = parseGridPlacement(player);
            if (placement == null) {
                return List.of();
            }
            rowsByIndex.computeIfAbsent(placement.row(), ignored -> new ArrayList<>()).add(placement);
        }

        List<List<ApiFootballLineupPlayer>> rows = new ArrayList<>();
        for (List<GridPlacement> placements : rowsByIndex.values()) {
            placements.sort(Comparator.comparingInt(GridPlacement::column));
            List<ApiFootballLineupPlayer> row = new ArrayList<>();
            for (GridPlacement placement : placements) {
                row.add(placement.player());
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return rows;
    }

    private GridPlacement parseGridPlacement(ApiFootballLineupPlayer player) {
        String grid = player == null ? null : emptyToNull(player.grid());
        if (grid == null) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)[:\\-](\\d+)$").matcher(grid.trim());
        if (!matcher.matches()) {
            return null;
        }

        try {
            int row = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            if (row <= 1) {
                row = 2;
            }
            return new GridPlacement(player, row, column);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<List<ApiFootballLineupPlayer>> buildRowsFromFormation(String formation, List<ApiFootballLineupPlayer> outfieldPlayers) {
        List<Integer> segments = parseFormation(formation);
        if (segments.isEmpty() || segments.stream().mapToInt(Integer::intValue).sum() != outfieldPlayers.size()) {
            return List.of();
        }

        List<ApiFootballLineupPlayer> orderedPlayers = new ArrayList<>(outfieldPlayers);
        orderedPlayers.sort(Comparator
                .comparingInt(this::depthRank)
                .thenComparingInt(this::verticalRank)
                .thenComparing(player -> emptyToFallback(player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));

        List<List<ApiFootballLineupPlayer>> rows = new ArrayList<>();
        int cursor = 0;
        for (Integer segment : segments) {
            List<ApiFootballLineupPlayer> row = new ArrayList<>(orderedPlayers.subList(cursor, cursor + segment));
            row.sort(Comparator.comparingInt(this::verticalRank)
                    .thenComparing(player -> emptyToFallback(player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));
            rows.add(row);
            cursor += segment;
        }
        return rows;
    }

    private List<List<ApiFootballLineupPlayer>> buildRowsFromPositionBands(List<ApiFootballLineupPlayer> outfieldPlayers) {
        List<ApiFootballLineupPlayer> defenders = new ArrayList<>();
        List<ApiFootballLineupPlayer> holdingMidfielders = new ArrayList<>();
        List<ApiFootballLineupPlayer> midfielders = new ArrayList<>();
        List<ApiFootballLineupPlayer> attackingMidfielders = new ArrayList<>();
        List<ApiFootballLineupPlayer> attackers = new ArrayList<>();

        for (ApiFootballLineupPlayer player : outfieldPlayers) {
            switch (classifyTacticalBand(player)) {
                case DEFENSE -> defenders.add(player);
                case HOLDING_MIDFIELD -> holdingMidfielders.add(player);
                case ATTACKING_MIDFIELD -> attackingMidfielders.add(player);
                case ATTACK -> attackers.add(player);
                default -> midfielders.add(player);
            }
        }

        List<List<ApiFootballLineupPlayer>> rows = new ArrayList<>();
        appendSortedRow(rows, defenders);
        appendSortedRow(rows, holdingMidfielders);
        appendSortedRow(rows, midfielders);
        appendSortedRow(rows, attackingMidfielders);
        appendSortedRow(rows, attackers);

        int totalPlayers = 0;
        for (List<ApiFootballLineupPlayer> row : rows) {
            totalPlayers += row.size();
        }
        return totalPlayers == outfieldPlayers.size() ? rows : List.of();
    }

    private void appendSortedRow(List<List<ApiFootballLineupPlayer>> rows, List<ApiFootballLineupPlayer> players) {
        if (players == null || players.isEmpty()) {
            return;
        }
        players.sort(Comparator.comparingInt(this::verticalRank)
                .thenComparing(player -> emptyToFallback(player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));
        rows.add(players);
    }

    private List<List<ApiFootballLineupPlayer>> buildRowsFromFallback(List<ApiFootballLineupPlayer> outfieldPlayers) {
        List<ApiFootballLineupPlayer> orderedPlayers = new ArrayList<>(outfieldPlayers);
        orderedPlayers.sort(Comparator.comparingInt(this::verticalRank)
                .thenComparing(player -> emptyToFallback(player.playerName(), "Joueur"), String.CASE_INSENSITIVE_ORDER));

        List<Integer> fallbackShape = fallbackShapeFor(orderedPlayers.size());
        List<List<ApiFootballLineupPlayer>> rows = new ArrayList<>();
        int cursor = 0;
        for (int index = 0; index < fallbackShape.size(); index++) {
            int segment = fallbackShape.get(index);
            if (index == fallbackShape.size() - 1) {
                segment = orderedPlayers.size() - cursor;
            }
            rows.add(new ArrayList<>(orderedPlayers.subList(cursor, Math.min(orderedPlayers.size(), cursor + segment))));
            cursor += segment;
            if (cursor >= orderedPlayers.size()) {
                break;
            }
        }
        return rows;
    }

    private int findGoalkeeperIndex(List<ApiFootballLineupPlayer> players) {
        for (int index = 0; index < players.size(); index++) {
            if (isGoalkeeper(players.get(index))) {
                return index;
            }
        }
        return 0;
    }

    private boolean isGoalkeeper(ApiFootballLineupPlayer player) {
        if (player == null) {
            return false;
        }
        String normalizedPosition = normalize(emptyToNull(player.position()));
        if (normalizedPosition != null && (normalizedPosition.contains("goalkeeper")
                || normalizedPosition.contains("keeper")
                || normalizedPosition.contains("gardien"))) {
            return true;
        }

        String grid = player.grid();
        return grid != null && grid.startsWith("1:");
    }

    private List<Integer> parseFormation(String formation) {
        String value = emptyToNull(formation);
        if (value == null) {
            return List.of();
        }

        List<Integer> rows = new ArrayList<>();
        for (String token : value.replace(':', '-').split("-")) {
            try {
                rows.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }
        return rows;
    }

    private TacticalBand classifyTacticalBand(ApiFootballLineupPlayer player) {
        String value = normalize(emptyToNull(player == null ? null : player.position()));
        if (value == null) {
            return TacticalBand.MIDFIELD;
        }
        if (value.contains("goalkeeper") || value.contains("keeper") || value.contains("gardien")) {
            return TacticalBand.GOALKEEPER;
        }
        if (value.contains("back") || value.contains("defender") || value.contains("defence")
                || value.contains("defense") || value.contains("stopper") || value.contains("sweeper")) {
            return TacticalBand.DEFENSE;
        }
        if (value.contains("defensive midfielder") || value.contains("holding")) {
            return TacticalBand.HOLDING_MIDFIELD;
        }
        if (value.contains("attacking midfielder") || value.contains("playmaker") || value.contains("second striker")) {
            return TacticalBand.ATTACKING_MIDFIELD;
        }
        if (value.contains("wing") || value.contains("forward") || value.contains("striker") || value.contains("attacker")) {
            return TacticalBand.ATTACK;
        }
        return TacticalBand.MIDFIELD;
    }

    private int depthRank(ApiFootballLineupPlayer player) {
        GridPlacement placement = parseGridPlacement(player);
        if (placement != null) {
            return placement.row();
        }
        return switch (classifyTacticalBand(player)) {
            case GOALKEEPER -> 0;
            case DEFENSE -> 1;
            case HOLDING_MIDFIELD -> 2;
            case MIDFIELD -> 3;
            case ATTACKING_MIDFIELD -> 4;
            case ATTACK -> 5;
        };
    }

    private int verticalRank(ApiFootballLineupPlayer player) {
        GridPlacement placement = parseGridPlacement(player);
        if (placement != null) {
            return placement.column();
        }

        String value = normalize(emptyToNull(player == null ? null : player.position()));
        if (value == null) {
            return 50;
        }
        if (value.contains("centre-left") || value.contains("center-left") || value.contains("left centre")) {
            return 25;
        }
        if (value.contains("centre-right") || value.contains("center-right") || value.contains("right centre")) {
            return 75;
        }
        if (value.contains("left")) {
            return 10;
        }
        if (value.contains("right")) {
            return 90;
        }
        if (value.contains("centre") || value.contains("center")) {
            return 50;
        }
        return 50;
    }

    private List<Integer> fallbackShapeFor(int outfieldPlayers) {
        return switch (outfieldPlayers) {
            case 10 -> List.of(4, 3, 3);
            case 9 -> List.of(4, 2, 3);
            case 8 -> List.of(4, 4);
            case 7 -> List.of(3, 2, 2);
            case 6 -> List.of(3, 3);
            case 5 -> List.of(2, 3);
            case 4 -> List.of(2, 2);
            case 3 -> List.of(1, 2);
            case 2 -> List.of(2);
            default -> List.of(Math.max(1, outfieldPlayers));
        };
    }

    private void renderStatistics(List<ApiFootballStatisticRow> statistics) {
        matchStatsContainer.getChildren().clear();
        boolean hasStatistics = statistics != null && !statistics.isEmpty();
        matchStatsEmptyLabel.setManaged(!hasStatistics);
        matchStatsEmptyLabel.setVisible(!hasStatistics);
        if (!hasStatistics) {
            matchStatsEmptyLabel.setText("Aucune statistique detaillee disponible pour ce match.");
            return;
        }

        for (ApiFootballStatisticRow row : statistics) {
            matchStatsContainer.getChildren().add(buildStatComparisonCard(row));
        }
    }

    private VBox buildStatComparisonCard(ApiFootballStatisticRow row) {
        Label homeValueLabel = new Label(emptyToFallback(row.homeValue(), "N/A"));
        homeValueLabel.getStyleClass().add("flashscore-stat-value");

        Label labelLabel = new Label(emptyToFallback(row.label(), "Stat"));
        labelLabel.getStyleClass().add("flashscore-stat-label");
        labelLabel.setWrapText(true);

        Label awayValueLabel = new Label(emptyToFallback(row.awayValue(), "N/A"));
        awayValueLabel.getStyleClass().add("flashscore-stat-value");

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox valueRow = new HBox(14, homeValueLabel, leftSpacer, labelLabel, rightSpacer, awayValueLabel);
        valueRow.setAlignment(Pos.CENTER);

        double[] ratios = normalizeStatRatios(row.homeValue(), row.awayValue());

        StackPane homeTrack = new StackPane();
        homeTrack.getStyleClass().add("flashscore-stat-track");
        HBox.setHgrow(homeTrack, javafx.scene.layout.Priority.ALWAYS);

        Region homeFill = new Region();
        homeFill.getStyleClass().addAll("flashscore-stat-fill", "flashscore-stat-fill-home");
        homeFill.prefWidthProperty().bind(homeTrack.widthProperty().multiply(ratios[0]));
        StackPane.setAlignment(homeFill, Pos.CENTER_LEFT);
        homeTrack.getChildren().add(homeFill);

        StackPane centerMarker = new StackPane();
        centerMarker.getStyleClass().add("flashscore-stat-center-marker");

        StackPane awayTrack = new StackPane();
        awayTrack.getStyleClass().add("flashscore-stat-track");
        HBox.setHgrow(awayTrack, javafx.scene.layout.Priority.ALWAYS);

        Region awayFill = new Region();
        awayFill.getStyleClass().addAll("flashscore-stat-fill", "flashscore-stat-fill-away");
        awayFill.prefWidthProperty().bind(awayTrack.widthProperty().multiply(ratios[1]));
        StackPane.setAlignment(awayFill, Pos.CENTER_RIGHT);
        awayTrack.getChildren().add(awayFill);

        HBox barRow = new HBox(8, homeTrack, centerMarker, awayTrack);
        barRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, valueRow, barRow);
        card.getStyleClass().add("flashscore-stat-card");
        return card;
    }

    private double[] normalizeStatRatios(String homeValue, String awayValue) {
        Double homeNumeric = parseComparableStat(homeValue);
        Double awayNumeric = parseComparableStat(awayValue);
        if (homeNumeric == null && awayNumeric == null) {
            return new double[] {0.5, 0.5};
        }
        if (homeNumeric == null) {
            return new double[] {0.0, 1.0};
        }
        if (awayNumeric == null) {
            return new double[] {1.0, 0.0};
        }

        double total = Math.max(0.0, homeNumeric) + Math.max(0.0, awayNumeric);
        if (total <= 0.0) {
            return new double[] {0.5, 0.5};
        }
        return new double[] {Math.max(0.06, homeNumeric / total), Math.max(0.06, awayNumeric / total)};
    }

    private Double parseComparableStat(String value) {
        String normalized = emptyToNull(value);
        if (normalized == null) {
            return null;
        }

        String candidate = normalized.replace(',', '.');
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(candidate);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showApiFootballStatus(String message, String styleClass) {
        apiFootballStatusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!apiFootballStatusLabel.getStyleClass().contains("status-pill")) {
            apiFootballStatusLabel.getStyleClass().add("status-pill");
        }
        if (!apiFootballStatusLabel.getStyleClass().contains(styleClass)) {
            apiFootballStatusLabel.getStyleClass().add(styleClass);
        }
        apiFootballStatusLabel.setText(message);
        currentApiFootballStatus = message;
    }

    private String shortError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Donnees detaillees indisponibles pour le moment.";
        }

        String message = throwable.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() <= 140 ? message : message.substring(0, 140) + "...";
    }

    private void updateLogo(ImageView imageView, Label fallbackLabel, Equipe equipe, String defaultLetter) {
        Image image = equipe == null ? null : loadImage(equipe.getImage());
        boolean hasImage = image != null;

        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setText(equipe == null ? defaultLetter : buildInitials(equipe.getNom(), defaultLetter));
    }

    private String buildSubtitle(String competitionLabel, Equipe homeTeam, Equipe awayTeam) {
        String home = homeTeam == null ? "Equipe domicile" : emptyToFallback(homeTeam.getNom(), "Equipe domicile");
        String away = awayTeam == null ? "Equipe exterieur" : emptyToFallback(awayTeam.getNom(), "Equipe exterieur");
        if (competitionLabel == null) {
            return home + " recoit " + away + ".";
        }
        return competitionLabel + " | " + home + " recoit " + away + ".";
    }

    private void openMatchList() {
        String competitionCode = match == null ? null : match.getCompetitionCode();
        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/match-crud-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                (resolveCompetitionLabel(competitionCode) == null ? "Matchs" : resolveCompetitionLabel(competitionCode)) + " | Matchs",
                controller -> {
                    if (controller instanceof MatchListController matchListController) {
                        matchListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
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

    private String buildMatchLabel(Matchs value) {
        return getEquipeName(value.getEquipeDomicileId()) + " vs " + getEquipeName(value.getEquipeExterieurId());
    }

    private String getEquipeName(Integer equipeId) {
        if (equipeService == null) {
            return "Equipe inconnue";
        }
        try {
            Equipe equipe = resolveEquipe(equipeId);
            return equipe == null ? "Equipe inconnue" : emptyToFallback(equipe.getNom(), "Equipe inconnue");
        } catch (SQLException e) {
            return "Equipe inconnue";
        }
    }

    private String buildScore(Matchs value) {
        return (value.getScoreEquipeDomicile() == null ? "-" : value.getScoreEquipeDomicile())
                + " : "
                + (value.getScoreEquipeExterieur() == null ? "-" : value.getScoreEquipeExterieur());
    }

    private String resolveStatus(Matchs value) {
        String status = value == null ? null : emptyToNull(value.getStatut());
        return status == null ? "Programme" : status;
    }

    private String resolveCompetitionLabel(String competitionCode) {
        if (competitionCode == null) {
            return null;
        }
        return COMPETITION_LABELS.getOrDefault(competitionCode, competitionCode);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : TIME_FORMATTER.format(time);
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
        if (isLiveStatusText(normalized)) {
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

    private String emptyToFallback(String value, String fallback) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String escapeHtml(String value) {
        String cleaned = emptyToFallback(value, "");
        return cleaned
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String firstNonBlank(String primary, String fallback) {
        String cleaned = emptyToNull(primary);
        return cleaned == null ? fallback : cleaned;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String lowercase(String value) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    private String buildInitials(String teamName, String fallback) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return fallback;
        }

        StringBuilder initials = new StringBuilder();
        for (String part : normalizedName.split("\\s+")) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? fallback : initials.toString();
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
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
            image = createImage(resource.toExternalForm());
            if (image != null) {
                return image;
            }
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

    private URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/equipes/" + imagePath,
                "/uploads/equipes/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = MatchDetailController.class.getResource(candidate);
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

        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            return bufferedImage == null ? null : SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
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

    private enum MatchDetailTab {
        SUMMARY,
        STATS,
        LINEUP,
        ODDS,
        VIDEOS
    }

    private record PlayerIncidentSummary(int goals, int assists, int yellowCards, int redCards) {
        private static final PlayerIncidentSummary EMPTY = new PlayerIncidentSummary(0, 0, 0, 0);
    }

    private record LiveRefreshPayload(
            ApiFootballFixtureSnapshot snapshot,
            ApiFootballMatchDetails details,
            Throwable error
    ) {
    }

    private record LiveScoreboardState(String phaseLabel, String minuteLabel) {
    }

    private record UserSessionTarget(Integer userId) {
    }

    private enum TacticalBand {
        GOALKEEPER,
        DEFENSE,
        HOLDING_MIDFIELD,
        MIDFIELD,
        ATTACKING_MIDFIELD,
        ATTACK
    }

    private record GridPlacement(ApiFootballLineupPlayer player, int row, int column) {
    }
}

