package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.JoueurUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.PlayerPortraitService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class JoueurListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double PLAYER_CARD_WIDTH = 232;
    private static final double PLAYER_CARD_HEIGHT = 244;
    private static final double CARD_IMAGE_SIZE = 96;
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-list-db-worker"));
    private static final ExecutorService PORTRAIT_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-list-portrait-worker"));

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
    private Label resultCountLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label toolbarTitleLabel;
    @FXML
    private Label toolbarSubtitleLabel;
    @FXML
    private Label cardsTitleLabel;
    @FXML
    private Label cardsSubtitleLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label teamCountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Equipe> equipeFilterComboBox;
    @FXML
    private FlowPane joueurCardsPane;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Button refreshButton;

    private final ObservableList<Joueur> joueurs = FXCollections.observableArrayList();
    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();
    private final FilteredList<Joueur> filteredJoueurs = new FilteredList<>(joueurs, joueur -> true);
    private final Map<Integer, Equipe> equipeById = new HashMap<>();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final PlayerPortraitService playerPortraitService = new PlayerPortraitService();

    private JoueurService joueurService;
    private EquipeService equipeService;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean dataLoaded;
    private Integer contextTeamId;
    private String contextCompetitionCode;
    private Equipe contextTeam;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabel();
        configureEquipeFilter();
        configureSearch();
        configurePlayerList();
        updateActionAvailability();
        updateSelectionState();

        try {
            joueurService = new JoueurService();
            equipeService = new EquipeService();
            serviceReady = true;
            updateActionAvailability();
            Platform.runLater(() -> {
                refreshDataAsync("Chargement de l'effectif...");
            });
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les joueurs.\n" + e.getMessage());
        }
    }

    public void setTeamContext(Equipe equipe, String competitionCode) {
        contextTeam = equipe;
        contextTeamId = equipe == null ? null : equipe.getId();
        contextCompetitionCode = FootballDataCompetitions.normalizeCode(competitionCode);
        updatePageTexts();
        updateActionAvailability();
        if (dataLoaded) {
            refreshDataAsync("Chargement de l'effectif...");
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
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Competitions");
    }

    @FXML
    private void handleRefresh() {
        refreshDataAsync("Actualisation des joueurs...");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        if (contextTeamId == null) {
            equipeFilterComboBox.getSelectionModel().clearSelection();
        } else {
            selectEquipeInFilter(contextTeamId);
        }
        applyFilters();
        showMutedStatus("Filtres reinitialises.");
    }

    @FXML
    private void handleBack() {
        if (contextCompetitionCode != null) {
            SceneNavigator.switchScene(
                    joueurCardsPane,
                    "/tn/esprit/views/joueur-teams-view.fxml",
                    "/tn/esprit/styles/joueur-theme.css",
                    FootballDataCompetitions.labelOf(contextCompetitionCode) + " | Equipes des joueurs",
                    controller -> {
                        if (controller instanceof JoueurTeamController joueurTeamController) {
                            joueurTeamController.setCompetitionFilter(contextCompetitionCode);
                        }
                    }
            );
            return;
        }
        SceneNavigator.switchScene(joueurCardsPane, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Competitions");
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

    private void configureStatusLabel() {
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.setText("Pret");
        setStatusStyle("status-muted");
    }

    private void configureEquipeFilter() {
        equipeFilterComboBox.setItems(equipes);
        equipeFilterComboBox.setCellFactory(listView -> createEquipeCell());
        equipeFilterComboBox.setButtonCell(createEquipeCell());
        equipeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private ListCell<Equipe> createEquipeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        };
    }

    private void configureSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configurePlayerList() {
        joueurCardsPane.getChildren().clear();
    }

    private Button buildPlayerCard(Joueur joueur) {
        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("player-avatar-shell");
        avatarShell.setMinSize(CARD_IMAGE_SIZE + 24, CARD_IMAGE_SIZE + 24);
        avatarShell.setPrefSize(CARD_IMAGE_SIZE + 24, CARD_IMAGE_SIZE + 24);
        avatarShell.setMaxSize(CARD_IMAGE_SIZE + 24, CARD_IMAGE_SIZE + 24);

        Image image = JoueurUiSupport.loadJoueurImage(joueur.getImage());
        if (image != null) {
            ImageView avatarView = new ImageView(image);
            avatarView.setFitWidth(CARD_IMAGE_SIZE);
            avatarView.setFitHeight(CARD_IMAGE_SIZE);
            avatarView.setPreserveRatio(false);
            avatarView.setClip(new Circle(CARD_IMAGE_SIZE / 2, CARD_IMAGE_SIZE / 2, CARD_IMAGE_SIZE / 2));
            avatarShell.getChildren().add(avatarView);
        } else {
            Label fallback = new Label(buildInitials(joueur));
            fallback.getStyleClass().add("player-avatar-fallback");
            avatarShell.getChildren().add(fallback);
        }

        Label titleLabel = new Label(buildFullName(joueur));
        titleLabel.getStyleClass().add("player-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(PLAYER_CARD_WIDTH - 36);

        Label positionPill = new Label(emptyToFallback(joueur.getPosition(), "Poste N/A"));
        positionPill.getStyleClass().add("player-number-badge");
        positionPill.setMaxWidth(92);

        Label nationalityPill = new Label(emptyToFallback(joueur.getNationalite(), "Nationalite N/A"));
        nationalityPill.getStyleClass().add("player-card-meta-pill");
        nationalityPill.setMaxWidth(92);

        HBox metaRow = new HBox(8, positionPill, nationalityPill);
        metaRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(14, avatarShell, titleLabel, metaRow);
        content.setAlignment(Pos.CENTER);

        Button cardButton = new Button();
        cardButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        cardButton.setGraphic(content);
        cardButton.getStyleClass().addAll("competition-card-button", "player-grid-card");
        cardButton.setPrefSize(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);
        cardButton.setMinSize(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);
        cardButton.setMaxSize(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);
        cardButton.setOnAction(event -> openJoueurDetail(joueur));
        return cardButton;
    }

    private void refreshData() {
        refreshDataAsync("Chargement de l'effectif...");
    }

    private void refreshDataAsync(String loadingMessage) {
        if (joueurService == null || equipeService == null) {
            return;
        }

        Integer selectedFilterEquipeId = getSelectedFilterEquipeId();
        Integer teamId = contextTeamId;
        Equipe teamContext = contextTeam;
        long requestId = refreshSequence.incrementAndGet();
        loadingData = true;
        updateActionAvailability();
        if (loadingMessage != null) {
            showMutedStatus(loadingMessage);
        }

        Task<PlayerData> loadTask = new Task<>() {
            @Override
            protected PlayerData call() throws Exception {
                EquipeService backgroundEquipeService = new EquipeService();
                JoueurService backgroundJoueurService = new JoueurService();

                List<Equipe> loadedEquipes;
                Equipe resolvedContextTeam = teamContext;
                if (teamId == null) {
                    loadedEquipes = new ArrayList<>(backgroundEquipeService.getAll());
                } else {
                    if (resolvedContextTeam == null) {
                        resolvedContextTeam = backgroundEquipeService.getById(teamId);
                    }
                    loadedEquipes = resolvedContextTeam == null ? new ArrayList<>() : new ArrayList<>(List.of(resolvedContextTeam));
                }
                loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

                List<Joueur> loadedJoueurs = teamId == null
                        ? new ArrayList<>(backgroundJoueurService.getAll())
                        : new ArrayList<>(backgroundJoueurService.getByEquipeId(teamId));
                return new PlayerData(loadedEquipes, loadedJoueurs, resolvedContextTeam);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            PlayerData data = loadTask.getValue();
            contextTeam = data.contextTeam();
            equipeById.clear();
            for (Equipe equipe : data.equipes()) {
                if (equipe.getId() != null) {
                    equipeById.put(equipe.getId(), equipe);
                }
            }
            equipes.setAll(data.equipes());
            selectEquipeInFilter(teamId == null ? selectedFilterEquipeId : teamId);

            List<Joueur> loadedJoueurs = new ArrayList<>(data.joueurs());
            loadedJoueurs.sort(Comparator
                    .comparing(this::getEquipeNameForSort, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Joueur::getNumero)
                    .thenComparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(Joueur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

            joueurs.setAll(loadedJoueurs);
            dataLoaded = true;
            loadingData = false;
            applyFilters();
            updatePageTexts();
            updateActionAvailability();
            showSuccessStatus("Effectif charge.");
            importMissingPortraitsAsync(loadedJoueurs, new HashMap<>(equipeById), requestId);
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            loadingData = false;
            updateActionAvailability();
            showErrorStatus("Erreur pendant le chargement.");
            Throwable throwable = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des joueurs.\n"
                    + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void importMissingPortraitsAsync(List<Joueur> loadedJoueurs, Map<Integer, Equipe> teamsById, long requestId) {
        List<Joueur> targets = loadedJoueurs.stream()
                .filter(playerPortraitService::shouldRefreshPortrait)
                .limit(60)
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        Equipe fallbackTeam = contextTeam;
        PORTRAIT_EXECUTOR.execute(() -> {
            Map<Integer, String> importedPortraits = new LinkedHashMap<>();
            try {
                JoueurService backgroundJoueurService = new JoueurService();
                PlayerPortraitService backgroundPortraitService = new PlayerPortraitService();
                for (Joueur target : targets) {
                    if (requestId != refreshSequence.get()) {
                        return;
                    }
                    if (target.getId() == null) {
                        continue;
                    }
                    Equipe team = target.getEquipeId() == null ? fallbackTeam : teamsById.get(target.getEquipeId());
                    String portrait = backgroundPortraitService.resolvePortrait(target, team);
                    if (portrait == null || portrait.isBlank() || Objects.equals(portrait.trim(), target.getImage())) {
                        continue;
                    }

                    backgroundJoueurService.updateImage(target.getId(), portrait.trim());
                    importedPortraits.put(target.getId(), portrait.trim());
                }
            } catch (Exception e) {
                System.err.println("Player portrait background import failed: " + e.getMessage());
            }

            int imported = importedPortraits.size();
            if (imported > 0 && requestId == refreshSequence.get()) {
                Platform.runLater(() -> {
                    importedPortraits.forEach(this::updateLoadedJoueurImage);
                    JoueurUiSupport.clearImageCache();
                    applyFilters();
                    showMutedStatus(imported + " photo(s) joueur importee(s).");
                });
            }
        });
    }

    private void updateLoadedJoueurImage(Integer joueurId, String imagePath) {
        if (joueurId == null || imagePath == null || imagePath.isBlank()) {
            return;
        }
        for (Joueur current : joueurs) {
            if (Objects.equals(current.getId(), joueurId)) {
                current.setImage(imagePath);
                return;
            }
        }
    }

    private void loadEquipes() throws SQLException {
        Integer selectedFilterEquipeId = getSelectedFilterEquipeId();

        List<Equipe> loadedEquipes;
        if (contextTeamId == null) {
            loadedEquipes = new ArrayList<>(equipeService.getAll());
        } else {
            Equipe team = contextTeam != null ? contextTeam : equipeService.getById(contextTeamId);
            loadedEquipes = team == null ? new ArrayList<>() : new ArrayList<>(List.of(team));
            contextTeam = team;
        }
        loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        equipeById.clear();
        for (Equipe equipe : loadedEquipes) {
            if (equipe.getId() != null) {
                equipeById.put(equipe.getId(), equipe);
            }
        }

        equipes.setAll(loadedEquipes);
        selectEquipeInFilter(contextTeamId == null ? selectedFilterEquipeId : contextTeamId);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        Integer filterEquipeId = contextTeamId == null ? getSelectedFilterEquipeId() : contextTeamId;

        filteredJoueurs.setPredicate(joueur -> {
            boolean matchesQuery = query == null
                    || containsNormalized(buildFullName(joueur), query)
                    || containsNormalized(getEquipeName(joueur.getEquipeId()), query)
                    || containsNormalized(joueur.getPosition(), query)
                    || containsNormalized(joueur.getNationalite(), query)
                    || containsNormalized(joueur.getNom(), query)
                    || containsNormalized(joueur.getPrenom(), query);

            boolean matchesEquipe = filterEquipeId == null || Objects.equals(joueur.getEquipeId(), filterEquipeId);
            return matchesQuery && matchesEquipe;
        });

        joueurCardsPane.getChildren().setAll(filteredJoueurs.stream()
                .map(this::buildPlayerCard)
                .toList());
        updateCounters();
        updateEmptyState();
    }

    private void updateCounters() {
        int joueursCount = filteredJoueurs.size();
        long equipesCount = filteredJoueurs.stream()
                .map(Joueur::getEquipeId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        resultCountLabel.setText(joueursCount + " joueur(s)");
        resultsMetaLabel.setText(joueursCount + " carte(s)");
        teamCountLabel.setText(equipesCount + " equipe(s)");
        updateSelectionState();
    }

    private void updateSelectionState() {
        selectionStateLabel.setText("Cliquez sur une carte pour ouvrir la fiche");
    }

    private void updateEmptyState() {
        boolean empty = filteredJoueurs.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
    }

    private void openJoueurDetail(Joueur joueur) {
        if (joueur == null) {
            return;
        }

        SceneNavigator.switchScene(
                joueurCardsPane,
                "/tn/esprit/views/joueur-detail-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Fiche joueur",
                controller -> {
                    if (controller instanceof JoueurDetailController joueurDetailController) {
                        joueurDetailController.setJoueurContext(joueur, contextTeam, contextCompetitionCode);
                    }
                }
        );
    }

    private void updateActionAvailability() {
        boolean disabled = !serviceReady || loadingData;
        refreshButton.setDisable(disabled);
        searchField.setDisable(disabled);
        equipeFilterComboBox.setDisable(disabled || contextTeamId != null);
        joueurCardsPane.setDisable(disabled);
    }

    private Integer getSelectedFilterEquipeId() {
        Equipe selectedEquipe = equipeFilterComboBox.getValue();
        return selectedEquipe == null ? null : selectedEquipe.getId();
    }

    private void selectEquipeInFilter(Integer equipeId) {
        if (equipeId == null) {
            equipeFilterComboBox.getSelectionModel().clearSelection();
            return;
        }

        for (Equipe equipe : equipes) {
            if (Objects.equals(equipe.getId(), equipeId)) {
                equipeFilterComboBox.getSelectionModel().select(equipe);
                return;
            }
        }

        equipeFilterComboBox.getSelectionModel().clearSelection();
    }

    private String getEquipeName(Integer equipeId) {
        Equipe equipe = equipeId == null ? null : equipeById.get(equipeId);
        return equipe == null ? "-" : emptyIfNull(equipe.getNom());
    }

    private String getEquipeNameForSort(Joueur joueur) {
        String equipeName = getEquipeName(joueur.getEquipeId());
        return "-".equals(equipeName) ? "zzzz" : equipeName;
    }

    private String buildFullName(Joueur joueur) {
        String prenom = emptyIfNull(joueur.getPrenom()).trim();
        String nom = emptyIfNull(joueur.getNom()).trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Joueur" : fullName;
    }

    private String buildInitials(Joueur joueur) {
        return JoueurUiSupport.buildInitials(joueur.getPrenom(), joueur.getNom(), "J");
    }

    private String buildPlayerBadge(Joueur joueur) {
        if (joueur.getNumero() > 0) {
            return "#" + joueur.getNumero();
        }
        String position = emptyToNull(joueur.getPosition());
        return position == null ? "API" : position;
    }

    private String buildPlayerSecondaryLine(Joueur joueur) {
        String equipeName = sanitizeDash(getEquipeName(joueur.getEquipeId()));
        String position = emptyToNull(joueur.getPosition());
        String nationalite = emptyToNull(joueur.getNationalite());

        List<String> parts = new ArrayList<>();
        if (equipeName != null) {
            parts.add(equipeName);
        }
        if (position != null) {
            parts.add(position);
        }
        if (nationalite != null) {
            parts.add(nationalite);
        }

        return parts.isEmpty() ? "Profil sans equipe" : String.join(" | ", parts);
    }

    private void updatePageTexts() {
        String teamName = contextTeam == null ? null : emptyToNull(contextTeam.getNom());
        String competitionLabel = FootballDataCompetitions.labelOf(contextCompetitionCode);
        if (teamName == null) {
            setTextIfPresent(pageTitleLabel, "Parcourez les joueurs");
            setTextIfPresent(pageSubtitleLabel, "Retrouvez les profils par nom ou par equipe, puis ouvrez chaque joueur sur une page detail dediee.");
            setTextIfPresent(toolbarTitleLabel, "Joueurs");
            setTextIfPresent(toolbarSubtitleLabel, "Vue publique en lecture seule : liste, filtres et fiche detail sur une autre page.");
            setTextIfPresent(cardsTitleLabel, "Cartes des joueurs");
            setTextIfPresent(cardsSubtitleLabel, "Chaque carte ouvre directement une page detail. Aucun formulaire ni action CRUD ici.");
            return;
        }

        setTextIfPresent(pageTitleLabel, teamName);
        setTextIfPresent(pageSubtitleLabel, "Effectif " + teamName + " | " + competitionLabel + ". Cliquez sur un joueur pour ouvrir sa fiche et ses statistiques de saison.");
        setTextIfPresent(toolbarTitleLabel, "Joueurs de " + teamName);
        setTextIfPresent(toolbarSubtitleLabel, "Liste limitee a l'equipe selectionnee pour garder la page rapide et lisible.");
        setTextIfPresent(cardsTitleLabel, "Effectif");
        setTextIfPresent(cardsSubtitleLabel, "Chaque carte ouvre la fiche detaillee du joueur avec ses informations et statistiques disponibles.");
    }

    private void setTextIfPresent(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private String buildPlayerBirthLine(Joueur joueur) {
        return joueur.getDateNaissance() == null
                ? "Date de naissance indisponible"
                : "Ne le " + formatDate(joueur.getDateNaissance());
    }

    private String buildPlayerMetaPill(Joueur joueur) {
        String nationalite = emptyToNull(joueur.getNationalite());
        return nationalite == null ? formatAge(joueur.getDateNaissance()) : nationalite;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatAge(LocalDate date) {
        if (date == null) {
            return "Age indisponible";
        }
        return Period.between(date, LocalDate.now()).getYears() + " ans";
    }

    private void showMutedStatus(String message) {
        setStatus(message, "status-muted");
    }

    private void showSuccessStatus(String message) {
        setStatus(message, "status-success");
    }

    private void showErrorStatus(String message) {
        setStatus(message, "status-error");
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        setStatusStyle(styleClass);
    }

    private void setStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String sanitizeDash(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }
        return value;
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

    private record PlayerData(List<Equipe> equipes, List<Joueur> joueurs, Equipe contextTeam) {
    }
}

