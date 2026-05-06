package tn.esprit.Controller;

<<<<<<< HEAD
=======
import javafx.application.Platform;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
<<<<<<< HEAD
import javafx.scene.shape.Circle;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.assistant.AssistantPlayerProfileProvider;
import tn.esprit.assistant.AssistantPlayerProfileSnapshot;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.JoueurUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
<<<<<<< HEAD
import tn.esprit.services.PlayerPortraitService;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballPlayerSeasonStats;
import tn.esprit.services.football.FootballDataCompetitions;
=======
import tn.esprit.services.wikidata.WikidataPlayerImageService;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
<<<<<<< HEAD
import java.util.Optional;
=======
import java.util.Objects;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

<<<<<<< HEAD
public class JoueurDetailController implements AssistantContextProvider, AssistantPlayerProfileProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ExecutorService IMAGE_IMPORT_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-detail-image-import"));
    private static final ExecutorService STATS_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-detail-stats"));
=======
public class JoueurDetailController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ExecutorService IMAGE_IMPORT_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-detail-image-import"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

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
    private ImageView detailImageView;
    @FXML
    private Label detailImageFallbackLabel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailEquipeValueLabel;
    @FXML
    private Label detailNumeroValueLabel;
    @FXML
    private Label detailDateNaissanceValueLabel;
    @FXML
    private Label detailAgeValueLabel;
    @FXML
    private Label detailPositionValueLabel;
    @FXML
    private Label detailNationaliteValueLabel;
    @FXML
    private Label detailSourceValueLabel;
<<<<<<< HEAD
    @FXML
    private Label detailStatsStatusLabel;
    @FXML
    private Label detailAppearancesValueLabel;
    @FXML
    private Label detailGoalsValueLabel;
    @FXML
    private Label detailAssistsValueLabel;
    @FXML
    private Label detailYellowCardsValueLabel;
    @FXML
    private Label detailRedCardsValueLabel;
    @FXML
    private Label detailMinutesValueLabel;

    private JoueurService joueurService;
    private EquipeService equipeService;
    private ApiFootballInsightsService apiFootballInsightsService;
    private PlayerPortraitService playerPortraitService;
    private Joueur joueur;
    private Equipe contextTeam;
    private String contextCompetitionCode;
=======

    private JoueurService joueurService;
    private EquipeService equipeService;
    private Joueur joueur;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean imageImportInProgress;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);

        try {
            joueurService = new JoueurService();
            equipeService = new EquipeService();
<<<<<<< HEAD
            apiFootballInsightsService = new ApiFootballInsightsService();
            playerPortraitService = new PlayerPortraitService();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer la fiche joueur.\n" + e.getMessage());
        }

        if (joueur != null) {
            renderJoueur();
        }
    }

    public void setJoueurContext(Joueur joueur) {
<<<<<<< HEAD
        setJoueurContext(joueur, null, null);
    }

    public void setJoueurContext(Joueur joueur, Equipe equipe, String competitionCode) {
        this.joueur = joueur;
        this.contextTeam = equipe;
        this.contextCompetitionCode = FootballDataCompetitions.normalizeCode(competitionCode);
=======
        this.joueur = joueur;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (detailNameLabel != null) {
            renderJoueur();
        }
    }

<<<<<<< HEAD
    @Override
    public String assistantContextSummary() {
        AssistantPlayerProfileSnapshot profile = assistantPlayerProfileSnapshot();
        return """
                Current player detail screen.
                Player: %s.
                Subtitle: %s.
                Team: %s. Number: %s.
                Birth date: %s. Age: %s.
                Position: %s. Nationality: %s.
                Source: %s.
                Season stats context: %s.
                Season stats: appearances %s, goals %s, assists %s, yellow cards %s, red cards %s, minutes %s.
                Recent form: %s.
                """.formatted(
                emptyToFallback(profile.playerName(), "Player"),
                emptyToFallback(profile.subtitle(), "No subtitle"),
                emptyToFallback(profile.clubName(), "Unknown"),
                emptyToFallback(profile.numberLabel(), "Unknown"),
                emptyToFallback(profile.birthDateLabel(), "Unknown"),
                emptyToFallback(profile.ageLabel(), "Unknown"),
                emptyToFallback(profile.positionLabel(), "Unknown"),
                emptyToFallback(profile.nationalityLabel(), "Unknown"),
                emptyToFallback(profile.sourceLabel(), "Unknown"),
                emptyToFallback(profile.statsStatusLabel(), "Unknown"),
                emptyToFallback(profile.appearancesLabel(), "Unknown"),
                emptyToFallback(profile.goalsLabel(), "Unknown"),
                emptyToFallback(profile.assistsLabel(), "Unknown"),
                emptyToFallback(profile.yellowCardsLabel(), "Unknown"),
                emptyToFallback(profile.redCardsLabel(), "Unknown"),
                emptyToFallback(profile.minutesLabel(), "Unknown"),
                emptyToFallback(profile.recentFormLabel(), "Not visible on this screen")
        );
    }

    @Override
    public AssistantPlayerProfileSnapshot assistantPlayerProfileSnapshot() {
        return new AssistantPlayerProfileSnapshot(
                textOf(detailNameLabel),
                textOf(detailSubtitleLabel),
                textOf(detailEquipeValueLabel),
                textOf(detailNumeroValueLabel),
                textOf(detailDateNaissanceValueLabel),
                textOf(detailAgeValueLabel),
                textOf(detailPositionValueLabel),
                textOf(detailNationaliteValueLabel),
                textOf(detailSourceValueLabel),
                textOf(detailStatsStatusLabel),
                textOf(detailAppearancesValueLabel),
                textOf(detailGoalsValueLabel),
                textOf(detailAssistsValueLabel),
                textOf(detailYellowCardsValueLabel),
                textOf(detailRedCardsValueLabel),
                textOf(detailMinutesValueLabel),
                null
        );
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
<<<<<<< HEAD
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Competitions");
=======
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    @FXML
    private void handleBack() {
<<<<<<< HEAD
        if (contextTeam != null && contextTeam.getId() != null) {
            SceneNavigator.switchScene(
                    detailNameLabel,
                    "/tn/esprit/views/joueur-list-view.fxml",
                    "/tn/esprit/styles/joueur-theme.css",
                    emptyToFallback(contextTeam.getNom(), "Equipe") + " | Joueurs",
                    controller -> {
                        if (controller instanceof JoueurListController joueurListController) {
                            joueurListController.setTeamContext(contextTeam, contextCompetitionCode);
                        }
                    }
            );
            return;
        }
        SceneNavigator.switchScene(detailNameLabel, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Competitions");
=======
        SceneNavigator.switchScene(detailNameLabel, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.JOUEURS);
    }

    private void renderJoueur() {
        if (joueur == null || joueurService == null || equipeService == null) {
            return;
        }

        try {
            if (joueur.getId() != null) {
                Joueur refreshed = joueurService.getById(joueur.getId());
                if (refreshed != null) {
                    joueur = refreshed;
                }
            }

<<<<<<< HEAD
            Equipe equipe = contextTeam != null ? contextTeam : resolveEquipe(joueur.getEquipeId());
            contextTeam = equipe;
            if (contextCompetitionCode == null && equipe != null) {
                contextCompetitionCode = FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode());
            }
=======
            Equipe equipe = resolveEquipe(joueur.getEquipeId());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            String equipeName = equipe == null ? "Sans equipe" : emptyToFallback(equipe.getNom(), "Sans equipe");
            String position = emptyToFallback(joueur.getPosition(), "Non renseigne");
            String nationalite = emptyToFallback(joueur.getNationalite(), "Non renseignee");
            String source = emptyToFallback(joueur.getExternalSource(), "Manuel");

            detailNameLabel.setText(buildFullName(joueur));
            detailSubtitleLabel.setText(buildSubtitle(equipeName, joueur.getDateNaissance(), position, nationalite));
<<<<<<< HEAD
            detailIdValueLabel.setText("Joueur");
=======
            detailIdValueLabel.setText(joueur.getId() == null ? "-" : "#" + joueur.getId());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            detailEquipeValueLabel.setText(equipeName);
            detailNumeroValueLabel.setText(joueur.getNumero() > 0 ? "#" + joueur.getNumero() : "Non defini");
            detailDateNaissanceValueLabel.setText(formatDate(joueur.getDateNaissance()));
            detailAgeValueLabel.setText(formatAge(joueur.getDateNaissance()));
            detailPositionValueLabel.setText(position);
            detailNationaliteValueLabel.setText(nationalite);
            detailSourceValueLabel.setText(source);

            JoueurUiSupport.clearImageCache();
            updatePhoto();
<<<<<<< HEAD
            loadSeasonStatsAsync(equipe);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            triggerLazyImageImport();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les informations du joueur.\n" + e.getMessage());
        }
    }

    private Equipe resolveEquipe(Integer equipeId) throws SQLException {
        return equipeId == null ? null : equipeService.getById(equipeId);
    }

    private void updatePhoto() {
        Image image = JoueurUiSupport.loadJoueurImage(joueur.getImage());
<<<<<<< HEAD
        boolean hasImage = image != null && image.getProgress() >= 1.0 && !image.isError();
        detailImageView.setPreserveRatio(false);
        detailImageView.setSmooth(true);
        detailImageView.setClip(new Circle(95, 95, 95));
        detailImageView.setImage(image);
        setPhotoVisibility(hasImage);
        detailImageFallbackLabel.setText(JoueurUiSupport.buildInitials(joueur.getPrenom(), joueur.getNom(), "J"));

        if (image != null && !hasImage) {
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() >= 1.0 && !image.isError()) {
                    setPhotoVisibility(true);
                }
            });
            image.errorProperty().addListener((observable, oldValue, hasError) -> {
                if (Boolean.TRUE.equals(hasError)) {
                    setPhotoVisibility(false);
                    triggerLazyImageImport(true);
                }
            });
        }
    }

    private void setPhotoVisibility(boolean hasImage) {
=======
        boolean hasImage = image != null;
        detailImageView.setImage(image);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        detailImageView.setManaged(hasImage);
        detailImageView.setVisible(hasImage);
        detailImageFallbackLabel.setManaged(!hasImage);
        detailImageFallbackLabel.setVisible(!hasImage);
<<<<<<< HEAD
    }

    private void triggerLazyImageImport() {
        triggerLazyImageImport(false);
    }

    private void triggerLazyImageImport(boolean force) {
        if (imageImportInProgress || joueur == null || joueur.getId() == null || !force && !needsLazyImageImport(joueur)) {
=======
        detailImageFallbackLabel.setText(JoueurUiSupport.buildInitials(joueur.getPrenom(), joueur.getNom(), "J"));
    }

    private void triggerLazyImageImport() {
        if (imageImportInProgress || joueur == null || joueur.getId() == null || !needsLazyImageImport(joueur)) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }

        imageImportInProgress = true;
<<<<<<< HEAD
        detailSourceValueLabel.setText("Import photo...");
=======
        detailSourceValueLabel.setText("Importing photo...");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        Integer joueurId = joueur.getId();

        Task<Joueur> imageImportTask = new Task<>() {
            @Override
            protected Joueur call() throws Exception {
                JoueurService backgroundService = new JoueurService();
                Joueur fresh = backgroundService.getById(joueurId);
                if (fresh == null || !needsLazyImageImport(fresh)) {
                    return null;
                }

<<<<<<< HEAD
                PlayerPortraitService portraitService = playerPortraitService == null
                        ? new PlayerPortraitService()
                        : playerPortraitService;
                String imagePath = portraitService.resolvePortrait(fresh, contextTeam);
=======
                WikidataPlayerImageService imageService = new WikidataPlayerImageService();
                String imagePath = imageService.resolvePlayerImagePath(fresh);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                if (imagePath == null || imagePath.isBlank()) {
                    return null;
                }

                backgroundService.updateImage(joueurId, imagePath);
                fresh.setImage(imagePath);
                return fresh;
            }
        };

        imageImportTask.setOnSucceeded(event -> {
            imageImportInProgress = false;
            Joueur imported = imageImportTask.getValue();
            if (imported == null) {
                detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel"));
                return;
            }

            joueur = imported;
            JoueurUiSupport.clearImageCache();
            updatePhoto();
<<<<<<< HEAD
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel") + " | Photo importee");
=======
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel") + " | Photo imported");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        });

        imageImportTask.setOnFailed(event -> {
            imageImportInProgress = false;
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel"));
            Throwable throwable = imageImportTask.getException();
            if (throwable != null) {
                System.err.println("Detail lazy player image import failed: " + throwable.getMessage());
            }
        });

        IMAGE_IMPORT_EXECUTOR.execute(imageImportTask);
    }

<<<<<<< HEAD
    private void loadSeasonStatsAsync(Equipe equipe) {
        resetSeasonStats("Chargement des statistiques de saison...");
        if (apiFootballInsightsService == null || joueur == null || equipe == null) {
            showStatsStatus("status-warning", "Statistiques de saison indisponibles.");
            return;
        }

        Task<Optional<ApiFootballPlayerSeasonStats>> statsTask = new Task<>() {
            @Override
            protected Optional<ApiFootballPlayerSeasonStats> call() throws Exception {
                return apiFootballInsightsService.loadPlayerSeasonStats(joueur, equipe);
            }
        };

        statsTask.setOnSucceeded(event -> {
            Optional<ApiFootballPlayerSeasonStats> stats = statsTask.getValue();
            if (stats == null || stats.isEmpty()) {
                resetSeasonStats("Aucune statistique de saison disponible pour ce joueur.");
                return;
            }
            renderSeasonStats(stats.get());
        });

        statsTask.setOnFailed(event -> {
            Throwable throwable = statsTask.getException();
            resetSeasonStats(shortError(throwable));
        });

        STATS_EXECUTOR.execute(statsTask);
    }

    private void renderSeasonStats(ApiFootballPlayerSeasonStats stats) {
        detailAppearancesValueLabel.setText(formatStat(stats.appearances()));
        detailGoalsValueLabel.setText(formatStat(stats.goals()));
        detailAssistsValueLabel.setText(formatStat(stats.assists()));
        detailYellowCardsValueLabel.setText(formatStat(stats.yellowCards()));
        detailRedCardsValueLabel.setText(formatStat(stats.redCards()));
        detailMinutesValueLabel.setText(formatStat(stats.minutes()));
        applyStatsPhotoIfUseful(stats);
        String team = emptyToFallback(stats.teamName(), emptyToFallback(contextTeam == null ? null : contextTeam.getNom(), "Equipe"));
        String competition = emptyToFallback(stats.competitionName(), FootballDataCompetitions.labelOf(contextCompetitionCode));
        String season = stats.seasonYear() == null ? "saison actuelle" : "saison " + stats.seasonYear() + "/" + (stats.seasonYear() + 1);
        boolean partialStats = stats.yellowCards() == null || stats.redCards() == null || stats.minutes() == null;
        showStatsStatus(
                partialStats ? "status-warning" : "status-success",
                team + " | " + competition + " | " + season + (partialStats ? " | stats partielles" : "")
        );
    }

    private void applyStatsPhotoIfUseful(ApiFootballPlayerSeasonStats stats) {
        if (stats == null || joueur == null || joueur.getId() == null || !needsLazyImageImport(joueur)) {
            return;
        }
        String photoUrl = stats.photoUrl();
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        try {
            joueurService.updateImage(joueur.getId(), photoUrl.trim());
            joueur.setImage(photoUrl.trim());
            JoueurUiSupport.clearImageCache();
            updatePhoto();
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel") + " | Photo API-Football");
        } catch (SQLException e) {
            System.err.println("Could not persist API-Football player photo: " + e.getMessage());
        }
    }

    private void resetSeasonStats(String statusMessage) {
        setStatPlaceholder(detailAppearancesValueLabel);
        setStatPlaceholder(detailGoalsValueLabel);
        setStatPlaceholder(detailAssistsValueLabel);
        setStatPlaceholder(detailYellowCardsValueLabel);
        setStatPlaceholder(detailRedCardsValueLabel);
        setStatPlaceholder(detailMinutesValueLabel);
        showStatsStatus("status-muted", statusMessage);
    }

    private void setStatPlaceholder(Label label) {
        if (label != null) {
            label.setText("-");
        }
    }

    private String formatStat(Integer value) {
        return value == null ? "N/A" : String.valueOf(value);
    }

    private void showStatsStatus(String styleClass, String message) {
        if (detailStatsStatusLabel == null) {
            return;
        }
        detailStatsStatusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!detailStatsStatusLabel.getStyleClass().contains(styleClass)) {
            detailStatsStatusLabel.getStyleClass().add(styleClass);
        }
        detailStatsStatusLabel.setText(message);
    }

    private String shortError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Statistiques de saison indisponibles.";
        }
        String message = throwable.getMessage().replace('\r', ' ').replace('\n', ' ').trim();
        if (message.length() > 120) {
            message = message.substring(0, 120) + "...";
        }
        return message;
    }

    private boolean needsLazyImageImport(Joueur currentJoueur) {
        PlayerPortraitService portraitService = playerPortraitService == null
                ? new PlayerPortraitService()
                : playerPortraitService;
        return portraitService.shouldRefreshPortrait(currentJoueur);
=======
    private boolean needsLazyImageImport(Joueur currentJoueur) {
        if (currentJoueur == null) {
            return false;
        }
        String image = currentJoueur.getImage();
        if (image == null || image.isBlank()) {
            return true;
        }
        String normalized = image.replace('\\', '/').toLowerCase();
        return normalized.contains("fd-player-");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private String buildSubtitle(String equipeName, LocalDate dateNaissance, String position, String nationalite) {
        List<String> parts = new ArrayList<>();
        parts.add(equipeName);
        if (dateNaissance != null) {
            parts.add("Ne le " + formatDate(dateNaissance));
        }
        parts.add(position);
        parts.add(nationalite);
        return String.join(" | ", parts);
    }

    private String buildFullName(Joueur joueur) {
        String prenom = joueur.getPrenom() == null ? "" : joueur.getPrenom().trim();
        String nom = joueur.getNom() == null ? "" : joueur.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? "Joueur" : fullName;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Non renseignee" : DATE_FORMATTER.format(date);
    }

    private String formatAge(LocalDate date) {
        if (date == null) {
            return "Age indisponible";
        }
        return Period.between(date, LocalDate.now()).getYears() + " ans";
    }

<<<<<<< HEAD
    private String textOf(Label label) {
        return label == null ? null : label.getText();
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
<<<<<<< HEAD

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
}

