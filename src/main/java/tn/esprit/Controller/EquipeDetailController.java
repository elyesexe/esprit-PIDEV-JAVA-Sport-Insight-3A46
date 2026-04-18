package tn.esprit.Controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.football.ApiFootballInsightsService;
import tn.esprit.services.football.ApiFootballScorerEntry;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class EquipeDetailController {
    private static final ExecutorService API_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("equipe-detail-api-worker"));
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
    private Label detailTitleLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private ImageView detailLogoView;
    @FXML
    private Label detailLogoFallbackLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailCoachValueLabel;
    @FXML
    private Label detailCompetitionValueLabel;
    @FXML
    private Label detailPlayerCountValueLabel;
    @FXML
    private Label detailAddressValueLabel;
    @FXML
    private Label detailPhoneValueLabel;
    @FXML
    private Label detailEmailValueLabel;
    @FXML
    private Label detailSourceValueLabel;
    @FXML
    private VBox squadContainer;
    @FXML
    private Label squadEmptyLabel;
    @FXML
    private Label topScorersStatusLabel;
    @FXML
    private VBox topScorersContainer;
    @FXML
    private Label topScorersEmptyLabel;

    private EquipeService equipeService;
    private JoueurService joueurService;
    private ApiFootballInsightsService apiFootballInsightsService;
    private Equipe equipe;
    private SidebarModuleGroup sidebarModuleGroup;
    private final AtomicLong scorersRequestSequence = new AtomicLong();

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);

        try {
            equipeService = new EquipeService();
            joueurService = new JoueurService();
            apiFootballInsightsService = new ApiFootballInsightsService();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer la fiche equipe.\n" + e.getMessage());
        }

        if (equipe != null) {
            renderEquipe();
        }
    }

    public void setEquipeContext(Equipe equipe) {
        this.equipe = equipe;
        if (detailTitleLabel != null) {
            renderEquipe();
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
        openCompetitionSelector();
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
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleBack() {
        openEquipeList();
    }

    @FXML
    private void handleEdit() {
        if (equipe == null) {
            return;
        }

        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/equipe-form-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Modifier une equipe",
                controller -> {
                    if (controller instanceof EquipeFormController equipeFormController) {
                        equipeFormController.configureForUpdate(equipe);
                    }
                }
        );
    }

    @FXML
    private void handleDelete() {
        if (equipe == null || equipe.getId() == null || equipeService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer l'equipe \"" + emptyToFallback(equipe.getNom(), "Equipe") + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            equipeService.delete(equipe.getId());
            EquipeUiSupport.clearImageCache();
            openEquipeList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Suppression", "Impossible de supprimer l'equipe.\n" + e.getMessage());
        }
    }

    private void renderEquipe() {
        if (equipe == null || equipeService == null || joueurService == null) {
            return;
        }

        try {
            if (equipe.getId() != null) {
                Equipe refreshed = equipeService.getById(equipe.getId());
                if (refreshed != null) {
                    equipe = refreshed;
                }
            }

            String competitionLabel = resolveCompetitionLabel(equipe.getCompetitionCode());
            List<Joueur> squad = joueurService.getAll().stream()
                    .filter(joueur -> joueur.getEquipeId() != null && joueur.getEquipeId().equals(equipe.getId()))
                    .sorted(Comparator
                            .comparingInt(Joueur::getNumero)
                            .thenComparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Joueur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .collect(Collectors.toList());

            competitionBadgeLabel.setText(competitionLabel);
            detailTitleLabel.setText(emptyToFallback(equipe.getNom(), "Equipe"));
            detailSubtitleLabel.setText(buildSubtitle(competitionLabel, equipe));
            detailIdValueLabel.setText(equipe.getId() == null ? "Nouveau" : "#" + equipe.getId());
            detailCoachValueLabel.setText(emptyToFallback(equipe.getCoach(), "Non renseigne"));
            detailCompetitionValueLabel.setText(competitionLabel);
            detailPlayerCountValueLabel.setText(String.valueOf(squad.size()));
            detailAddressValueLabel.setText(emptyToFallback(equipe.getAdresse(), "Non renseignee"));
            detailPhoneValueLabel.setText(emptyToFallback(equipe.getTelephone(), "Non renseigne"));
            detailEmailValueLabel.setText(emptyToFallback(equipe.getEmail(), "Non renseignee"));
            detailSourceValueLabel.setText(emptyToFallback(equipe.getExternalSource(), "Manuel"));

            updateLogo();
            renderSquad(squad);
            loadTopScorersAsync();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les informations de l'equipe.\n" + e.getMessage());
        }
    }

    private void renderSquad(List<Joueur> squad) {
        squadContainer.getChildren().clear();
        boolean hasPlayers = squad != null && !squad.isEmpty();
        squadEmptyLabel.setManaged(!hasPlayers);
        squadEmptyLabel.setVisible(!hasPlayers);
        if (!hasPlayers) {
            return;
        }

        for (Joueur joueur : squad) {
            squadContainer.getChildren().add(createPlayerCard(joueur));
        }
    }

    private HBox createPlayerCard(Joueur joueur) {
        Label numberLabel = new Label(joueur.getNumero() <= 0 ? "--" : String.valueOf(joueur.getNumero()));
        numberLabel.getStyleClass().add("squad-number");

        Label nameLabel = new Label(buildPlayerName(joueur));
        nameLabel.getStyleClass().add("squad-name");
        nameLabel.setWrapText(true);

        String detailText = emptyToFallback(joueur.getPosition(), "Poste non renseigne");
        if (joueur.getNationalite() != null && !joueur.getNationalite().isBlank()) {
            detailText += " | " + joueur.getNationalite();
        }

        Label metaLabel = new Label(detailText);
        metaLabel.getStyleClass().add("squad-meta");
        metaLabel.setWrapText(true);

        VBox textBox = new VBox(4, nameLabel, metaLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(14, numberLabel, textBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("squad-card");
        return card;
    }

    private void loadTopScorersAsync() {
        if (equipe == null || apiFootballInsightsService == null) {
            showTopScorersStatus("status-warning", "Les sources de buteurs sont indisponibles.");
            renderTopScorers(List.of());
            return;
        }

        long requestId = scorersRequestSequence.incrementAndGet();
        showTopScorersStatus("status-muted", "Chargement des meilleurs buteurs...");

        Task<List<ApiFootballScorerEntry>> task = new Task<>() {
            @Override
            protected List<ApiFootballScorerEntry> call() throws Exception {
                return apiFootballInsightsService.loadTeamTopScorers(equipe);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != scorersRequestSequence.get()) {
                return;
            }

            renderTopScorers(task.getValue());
            showTopScorersStatus("status-success", "Meilleurs buteurs actualises.");
        });

        task.setOnFailed(event -> {
            if (requestId != scorersRequestSequence.get()) {
                return;
            }

            renderTopScorers(List.of());
            showTopScorersStatus("status-warning", shortError(task.getException()));
        });

        API_EXECUTOR.execute(task);
    }

    private void renderTopScorers(List<ApiFootballScorerEntry> scorers) {
        topScorersContainer.getChildren().clear();
        boolean hasScorers = scorers != null && !scorers.isEmpty();
        topScorersEmptyLabel.setManaged(!hasScorers);
        topScorersEmptyLabel.setVisible(!hasScorers);
        if (!hasScorers) {
            topScorersEmptyLabel.setText("Aucun meilleur buteur disponible pour cette equipe.");
            return;
        }

        for (ApiFootballScorerEntry scorer : scorers) {
            Label rankLabel = new Label(scorer.rank() + ".");
            rankLabel.getStyleClass().add("team-top-scorer-rank");

            Label nameLabel = new Label(emptyToFallback(scorer.playerName(), "Joueur"));
            nameLabel.getStyleClass().add("team-top-scorer-name");
            nameLabel.setWrapText(true);

            Label metaLabel = new Label(buildScorerMeta(scorer));
            metaLabel.getStyleClass().add("team-top-scorer-meta");

            VBox textBox = new VBox(3, nameLabel, metaLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

            Label goalsLabel = new Label((scorer.goals() == null ? "-" : scorer.goals()) + " buts");
            goalsLabel.getStyleClass().add("team-top-scorer-pill");

            HBox row = new HBox(14, rankLabel, textBox, goalsLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("team-top-scorer-row");
            topScorersContainer.getChildren().add(row);
        }
    }

    private String buildScorerMeta(ApiFootballScorerEntry scorer) {
        return "Assists " + (scorer.assists() == null ? "-" : scorer.assists())
                + " | Matchs " + (scorer.appearances() == null ? "-" : scorer.appearances())
                + " | Minutes " + (scorer.minutes() == null ? "-" : scorer.minutes());
    }

    private void showTopScorersStatus(String styleClass, String message) {
        topScorersStatusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!topScorersStatusLabel.getStyleClass().contains(styleClass)) {
            topScorersStatusLabel.getStyleClass().add(styleClass);
        }
        topScorersStatusLabel.setText(message);
    }

    private String shortError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Meilleurs buteurs indisponibles.";
        }

        String message = throwable.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() <= 140 ? message : message.substring(0, 140) + "...";
    }

    private void updateLogo() {
        Image image = EquipeUiSupport.loadEquipeImage(equipe.getImage());
        boolean hasImage = image != null;
        detailLogoView.setImage(image);
        detailLogoView.setManaged(hasImage);
        detailLogoView.setVisible(hasImage);
        detailLogoFallbackLabel.setManaged(!hasImage);
        detailLogoFallbackLabel.setVisible(!hasImage);
        detailLogoFallbackLabel.setText(EquipeUiSupport.buildInitials(equipe.getNom(), "SI"));
    }

    private void openEquipeList() {
        String competitionCode = equipe == null ? null : equipe.getCompetitionCode();
        if (competitionCode == null || !FootballDataCompetitions.isTeamCompetition(competitionCode)) {
            openCompetitionSelector();
            return;
        }

        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/equipe-list-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                resolveCompetitionLabel(competitionCode) + " | Equipes",
                controller -> {
                    if (controller instanceof EquipeListController equipeListController) {
                        equipeListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

    private void openCompetitionSelector() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.EQUIPES);
    }

    private String buildSubtitle(String competitionLabel, Equipe equipe) {
        String coach = emptyToFallback(equipe.getCoach(), "Coach non renseigne");
        return competitionLabel + " | " + coach;
    }

    private String resolveCompetitionLabel(String competitionCode) {
        if (!FootballDataCompetitions.isTeamCompetition(competitionCode)) {
            return "Competition non renseignee";
        }
        return FootballDataCompetitions.labelOf(competitionCode);
    }

    private String buildPlayerName(Joueur joueur) {
        String prenom = joueur.getPrenom() == null ? "" : joueur.getPrenom().trim();
        String nom = joueur.getNom() == null ? "" : joueur.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? "Joueur" : fullName;
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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

