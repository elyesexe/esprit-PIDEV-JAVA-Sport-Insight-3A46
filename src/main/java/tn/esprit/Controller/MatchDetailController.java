package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballLineupPlayer;
import tn.esprit.services.football.ApiFootballLineupSide;
import tn.esprit.services.football.ApiFootballMatchDetails;
import tn.esprit.services.football.ApiFootballStatisticRow;
import tn.esprit.services.football.FootballDataCompetitions;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class MatchDetailController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final String PITCH_LAYOUT_KEY = "matchDetailPitchLayout";
    private static final String PITCH_DECORATIONS_KEY = "matchDetailPitchDecorations";
    private static final String PITCH_DECORATION_NODE_KEY = "matchDetailPitchDecorationNode";
    private static final double PITCH_MARKER_WIDTH = 84.0;
    private static final double PITCH_MARKER_HEIGHT = 78.0;
    private static final double PITCH_HORIZONTAL_INSET = 44.0;
    private static final double PITCH_VERTICAL_INSET = 30.0;
    private static final double PITCH_FIELD_LINE_INSET = 18.0;
    private static final Map<String, Image> PLAYER_PHOTO_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Image>> PLAYER_PHOTO_REQUESTS = new ConcurrentHashMap<>();
    private static final ExecutorService API_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("match-detail-api-worker"));
    private static final ExecutorService PHOTO_EXECUTOR =
            Executors.newFixedThreadPool(4, daemonFactory("match-detail-photo-worker"));
    private static final HttpClient PHOTO_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

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

    private MatchsService matchsService;
    private EquipeService equipeService;
    private ApiFootballInsightsService apiFootballInsightsService;
    private Matchs match;
    private Equipe homeTeam;
    private Equipe awayTeam;
    private SidebarModuleGroup sidebarModuleGroup;
    private final AtomicLong apiRequestSequence = new AtomicLong();

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            apiFootballInsightsService = new ApiFootballInsightsService();
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
            detailLieuValueLabel.setText(emptyToFallback(match.getLieu(), "Non renseigne"));
            detailTypeValueLabel.setText(emptyToFallback(match.getType(), "Non renseigne"));
            detailStatutValueLabel.setText(resolveStatus(match));
            detailIdValueLabel.setText(match.getIdMatch() == null ? "#" + match.getId() : match.getIdMatch());
            detailCompetitionValueLabel.setText(competitionLabel == null ? "Non renseignee" : competitionLabel);
            detailHomeNameLabel.setText(homeTeam == null ? "Equipe domicile" : emptyToFallback(homeTeam.getNom(), "Equipe domicile"));
            detailAwayNameLabel.setText(awayTeam == null ? "Equipe exterieur" : emptyToFallback(awayTeam.getNom(), "Equipe exterieur"));
            lineupHomeTeamLabel.setText(detailHomeNameLabel.getText());
            lineupAwayTeamLabel.setText(detailAwayNameLabel.getText());
            statsHomeTeamLabel.setText(detailHomeNameLabel.getText());
            statsAwayTeamLabel.setText(detailAwayNameLabel.getText());
            lineupDomicileMetaLabel.setText("Formation indisponible");
            lineupExterieurMetaLabel.setText("Formation indisponible");
            renderStoredLineups();
            renderStatistics(List.of());

            updateLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, homeTeam, "D");
            updateLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, awayTeam, "E");
            renderCachedInsights();
            loadApiFootballInsightsAsync();
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
        if (cached != null && (cached.hasLineups() || cached.hasStatistics())) {
            renderApiFootballInsights(cached);
            showApiFootballStatus("Donnees detaillees en cache affichees.", "status-success");
            return;
        }

        showApiFootballStatus("Chargement des stats et compositions...", "status-muted");
    }

    private void loadApiFootballInsightsAsync() {
        if (match == null || apiFootballInsightsService == null) {
            return;
        }

        long requestId = apiRequestSequence.incrementAndGet();
        showApiFootballStatus("Chargement des stats et compositions...", "status-muted");

        Task<ApiFootballMatchDetails> task = new Task<>() {
            @Override
            protected ApiFootballMatchDetails call() throws Exception {
                return apiFootballInsightsService.loadMatchDetails(match, homeTeam, awayTeam);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != apiRequestSequence.get()) {
                return;
            }
            ApiFootballMatchDetails details = task.getValue();
            renderApiFootballInsights(details);
            showApiFootballStatus("Donnees detaillees synchronisees pour ce match.", "status-success");
        });

        task.setOnFailed(event -> {
            if (requestId != apiRequestSequence.get()) {
                return;
            }
            Throwable throwable = task.getException();
            showApiFootballStatus(shortError(throwable), "status-warning");
        });

        API_EXECUTOR.execute(task);
    }

    private void renderApiFootballInsights(ApiFootballMatchDetails details) {
        if (details == null) {
            return;
        }

        ApiFootballLineupSide storedHomeLineup = buildStoredLineup(match == null ? null : match.getLineupDomicile(), homeTeam);
        ApiFootballLineupSide storedAwayLineup = buildStoredLineup(match == null ? null : match.getLineupExterieur(), awayTeam);
        ApiFootballLineupSide homeLineup = chooseRenderableLineup(details.homeLineup(), storedHomeLineup);
        ApiFootballLineupSide awayLineup = chooseRenderableLineup(details.awayLineup(), storedAwayLineup);

        lineupDomicileMetaLabel.setText(buildLineupMeta(homeLineup));
        renderLineupSection(homeLineupPitchContainer, homeBenchContainer, homeLineup, false);

        lineupExterieurMetaLabel.setText(buildLineupMeta(awayLineup));
        renderLineupSection(awayLineupPitchContainer, awayBenchContainer, awayLineup, true);

        renderStatistics(details.statistics());
    }

    private void renderStoredLineups() {
        ApiFootballLineupSide homeStored = chooseRenderableLineup(buildStoredLineup(match == null ? null : match.getLineupDomicile(), homeTeam), null);
        ApiFootballLineupSide awayStored = chooseRenderableLineup(buildStoredLineup(match == null ? null : match.getLineupExterieur(), awayTeam), null);
        lineupDomicileMetaLabel.setText(buildLineupMeta(homeStored));
        lineupExterieurMetaLabel.setText(buildLineupMeta(awayStored));
        renderLineupSection(homeLineupPitchContainer, homeBenchContainer, homeStored, false);
        renderLineupSection(awayLineupPitchContainer, awayBenchContainer, awayStored, true);
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

        return new ApiFootballLineupPlayer(sanitized, shirtNumber, position, null, null);
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
        ImageView photoView = new ImageView();
        photoView.setFitWidth(48);
        photoView.setFitHeight(54);
        photoView.setPreserveRatio(true);

        Label fallbackLabel = new Label(buildPlayerInitials(player == null ? null : player.playerName()));
        fallbackLabel.getStyleClass().add("pitch-player-photo-fallback");

        StackPane photoShell = new StackPane(photoView, fallbackLabel);
        photoShell.getStyleClass().add("pitch-player-photo-shell");
        bindPlayerPhoto(photoView, fallbackLabel, player);

        Label shirtLabel = new Label(emptyToFallback(player == null ? null : player.shirtNumber(), "?"));
        shirtLabel.getStyleClass().add("pitch-player-number-badge");

        Label nameLabel = new Label(emptyToFallback(player == null ? null : player.playerName(), "Joueur"));
        nameLabel.getStyleClass().add("pitch-player-name-pill");
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setMaxWidth(78);

        HBox tagRow = new HBox(4, shirtLabel, nameLabel);
        tagRow.setAlignment(Pos.CENTER);
        tagRow.getStyleClass().add("pitch-player-pill-row");

        VBox marker = new VBox(3, photoShell, tagRow);
        marker.setAlignment(Pos.CENTER);
        marker.getStyleClass().add("pitch-player-marker");
        marker.setPrefSize(PITCH_MARKER_WIDTH, PITCH_MARKER_HEIGHT);
        marker.setMinSize(PITCH_MARKER_WIDTH, PITCH_MARKER_HEIGHT);
        marker.setMaxSize(PITCH_MARKER_WIDTH, PITCH_MARKER_HEIGHT);
        return marker;
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
        if (profile == null || !profile.isLightNeutral()) {
            return converted;
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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = converted.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                double backgroundStrength = backgroundStrength(argb, profile);
                if (backgroundMask[y][x]) {
                    double fade = clamp01((backgroundStrength - 0.58) / 0.24);
                    int nextAlpha = (int) Math.round(alpha * (1.0 - fade));
                    converted.setRGB(x, y, (nextAlpha << 24) | (argb & 0x00FFFFFF));
                    continue;
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

        return converted;
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
            trySeedBackgroundPixel(image, profile, backgroundMask, queue, x, height - 1, true);
        }

        for (int y = 1; y < height - 1; y++) {
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
        double backgroundStrength = backgroundStrength(argb, profile);

        if (borderSeed) {
            if (subjectSupport >= 0.22) {
                return false;
            }
            return backgroundStrength >= 0.68;
        }

        if (subjectSupport >= 0.36) {
            return false;
        }

        if (subjectSupport >= 0.18) {
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

    private double portraitSubjectSupport(double nx, double ny) {
        double head = ellipseSupport(nx, ny, 0.50, 0.20, 0.20, 0.18, 0.10);
        double shoulders = ellipseSupport(nx, ny, 0.50, 0.44, 0.38, 0.24, 0.14);
        double torso = ellipseSupport(nx, ny, 0.50, 0.76, 0.34, 0.28, 0.12);
        double neck = verticalBandSupport(nx, ny, 0.50, 0.54, 0.22, 0.26, 0.14, 0.10);
        return clamp01(Math.max(Math.max(head, shoulders), Math.max(torso, neck)));
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
        return clamp01((brightnessScore * 0.45) + (chromaScore * 0.30) + (distanceScore * 0.25));
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
    }

    private HBox buildBenchPlayerCard(ApiFootballLineupPlayer player) {
        Label shirtLabel = new Label(emptyToFallback(player == null ? null : player.shirtNumber(), "?"));
        shirtLabel.getStyleClass().add("bench-player-number");

        Label nameLabel = new Label(emptyToFallback(player == null ? null : player.playerName(), "Joueur"));
        nameLabel.getStyleClass().add("bench-player-name");
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setMaxWidth(160);

        HBox card = new HBox(8, shirtLabel, nameLabel);
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
        double usableWidth = Math.max(120.0, width - (PITCH_HORIZONTAL_INSET * 2.0));
        double usableHeight = Math.max(180.0, height - (PITCH_VERTICAL_INSET * 2.0));

        int markerIndex = 0;
        int rowCount = rows.size();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<ApiFootballLineupPlayer> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }

            double depthRatio = rowCount == 1 ? 0.5 : (double) rowIndex / (double) (rowCount - 1);
            double xCenter = PITCH_HORIZONTAL_INSET + (usableWidth * depthRatio);
            if (awaySide) {
                xCenter = width - PITCH_HORIZONTAL_INSET - (usableWidth * depthRatio);
            }

            double laneGap = usableHeight / (row.size() + 1.0);
            for (int playerIndex = 0; playerIndex < row.size() && markerIndex < markers.size(); playerIndex++) {
                VBox marker = markers.get(markerIndex++);
                double yCenter = PITCH_VERTICAL_INSET + (laneGap * (playerIndex + 1));
                marker.resizeRelocate(
                        xCenter - (PITCH_MARKER_WIDTH / 2.0),
                        yCenter - (PITCH_MARKER_HEIGHT / 2.0),
                        PITCH_MARKER_WIDTH,
                        PITCH_MARKER_HEIGHT
                );
            }
        }
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

    private String emptyToFallback(String value, String fallback) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

