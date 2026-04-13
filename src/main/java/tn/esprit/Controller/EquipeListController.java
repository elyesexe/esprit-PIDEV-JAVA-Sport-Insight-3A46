package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.FootballDataSyncService;
import tn.esprit.services.FootballDataSyncSummary;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class EquipeListController {
    private static final double CARD_LOGO_SIZE = 82;
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("equipe-list-db-worker"));

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
    private Label competitionChipLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortChoiceBox;
    @FXML
    private Button sortOrderButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button addButton;
    @FXML
    private ListView<Equipe> equipeListView;
    @FXML
    private VBox emptyStateBox;

    private final ObservableList<Equipe> masterEquipes = FXCollections.observableArrayList();
    private final ObservableList<Equipe> displayedEquipes = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();

    private EquipeService equipeService;
    private FootballDataSyncService footballDataSyncService;
    private String competitionFilterCode;
    private String lastAutoSyncedCompetitionCode;
    private boolean sortDescending;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean syncingData;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureToolbar();
        configureListView();
        updateCompetitionTexts();
        updateSortOrderButtonText();
        updateToolbarState();

        try {
            equipeService = new EquipeService();
            serviceReady = true;
            refreshTableAsync("Chargement des equipes...");
        } catch (SQLException e) {
            serviceReady = false;
            updateToolbarState();
            showStatus("status-error", "Connexion a la base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les equipes.\n" + e.getMessage());
        }
    }

    public void setCompetitionFilter(String competitionCode) {
        competitionFilterCode = competitionCode;
        updateCompetitionTexts();
        applyFiltersAndSort();
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
    private void handleRefresh() {
        if (normalizeCode(competitionFilterCode) != null) {
            syncCompetitionAsync(normalizeCode(competitionFilterCode), false);
            return;
        }
        refreshTableAsync("Actualisation des equipes...");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortChoiceBox.setValue("Nom");
        sortDescending = false;
        updateSortOrderButtonText();
        applyFiltersAndSort();
    }

    @FXML
    private void handleToggleSortOrder() {
        sortDescending = !sortDescending;
        updateSortOrderButtonText();
        applyFiltersAndSort();
    }

    @FXML
    private void handleBack() {
        openCompetitionSelector();
    }

    @FXML
    private void handleAddEquipe() {
        SceneNavigator.switchScene(
                addButton,
                "/tn/esprit/views/equipe-form-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Ajouter une equipe",
                controller -> {
                    if (controller instanceof EquipeFormController equipeFormController) {
                        equipeFormController.configureForCreate(competitionFilterCode);
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.EQUIPES);
    }

    private void configureToolbar() {
        sortChoiceBox.setItems(FXCollections.observableArrayList("Nom", "Coach", "Id"));
        sortChoiceBox.setValue("Nom");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort());
        sortChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort());
    }

    private void configureListView() {
        equipeListView.setItems(displayedEquipes);
        equipeListView.setCellFactory(listView -> createEquipeCell());
    }

    private void refreshTableAsync(String loadingMessage) {
        refreshTableAsync(loadingMessage, "Liste des equipes actualisee.");
    }

    private void refreshTableAsync(String loadingMessage, String successMessage) {
        if (equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        loadingData = true;
        updateToolbarState();
        if (loadingMessage != null) {
            showStatus("status-muted", loadingMessage);
        }

        Task<List<Equipe>> loadTask = new Task<>() {
            @Override
            protected List<Equipe> call() throws Exception {
                return equipeService.getAll();
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            masterEquipes.setAll(loadTask.getValue());
            applyFiltersAndSort();
            loadingData = false;
            updateToolbarState();
            if (shouldAutoSyncSelectedCompetition()) {
                syncCompetitionAsync(normalizeCode(competitionFilterCode), true);
                return;
            }
            showStatus("status-success", successMessage);
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            updateToolbarState();
            showStatus("status-error", "Erreur lors du chargement des equipes.");
            Throwable throwable = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les equipes.\n"
                    + (throwable == null ? "Erreur inconnue." : throwable.getMessage()));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void syncCompetitionAsync(String competitionCode, boolean automatic) {
        FootballDataSyncService syncService = ensureSyncService();
        if (syncService == null) {
            return;
        }

        String normalizedCode = normalizeCode(competitionCode);
        if (normalizedCode == null) {
            refreshTableAsync("Actualisation des equipes...");
            return;
        }

        if (automatic) {
            lastAutoSyncedCompetitionCode = normalizedCode;
        }

        syncingData = true;
        updateToolbarState();
        showStatus(
                "status-muted",
                automatic
                        ? "Aucune equipe locale pour " + resolveCompetitionLabel(normalizedCode)
                        + ". Import via football-data.org..."
                        : "Synchronisation " + resolveCompetitionLabel(normalizedCode) + " en cours..."
        );

        Task<FootballDataSyncSummary> syncTask = new Task<>() {
            @Override
            protected FootballDataSyncSummary call() throws Exception {
                updateMessage("Import des clubs et effectifs " + resolveCompetitionLabel(normalizedCode) + "...");
                return syncService.syncTeamsAndPlayers(List.of(normalizedCode), this::updateMessage);
            }
        };

        syncTask.messageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }
            showStatus("status-muted", newValue);
        });

        syncTask.setOnSucceeded(event -> {
            syncingData = false;
            updateToolbarState();

            FootballDataSyncSummary summary = syncTask.getValue();
            String summaryMessage = summary == null
                    ? "Synchronisation terminee."
                    : summary.toHumanMessage(true, false);
            refreshTableAsync(null, summaryMessage);
        });

        syncTask.setOnFailed(event -> {
            syncingData = false;
            updateToolbarState();
            Throwable throwable = syncTask.getException();
            String errorMessage = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showStatus("status-error", "Synchronisation impossible.");
            showAlert(Alert.AlertType.ERROR, "football-data.org",
                    "Impossible de synchroniser " + resolveCompetitionLabel(normalizedCode) + ".\n" + errorMessage);
        });

        DB_EXECUTOR.execute(syncTask);
    }

    private void applyFiltersAndSort() {
        List<Equipe> filtered = new ArrayList<>(masterEquipes);

        filtered.removeIf(equipe -> !matchesCompetition(equipe));

        String keyword = normalize(searchField.getText());
        if (!keyword.isEmpty()) {
            filtered.removeIf(equipe -> !matchesSearch(equipe, keyword));
        }

        filtered.sort(buildComparator());
        displayedEquipes.setAll(filtered);

        boolean isEmpty = filtered.isEmpty();
        emptyStateBox.setManaged(isEmpty);
        emptyStateBox.setVisible(isEmpty);

        resultCountLabel.setText(filtered.size() + " equipe(s)");
        resultsMetaLabel.setText(filtered.size() + " resultat(s)");
    }

    private boolean matchesCompetition(Equipe equipe) {
        String equipeCompetition = equipe == null ? null : normalizeCode(equipe.getCompetitionCode());
        if (competitionFilterCode != null) {
            return Objects.equals(normalizeCode(competitionFilterCode), equipeCompetition);
        }
        return FootballDataCompetitions.isTeamCompetition(equipeCompetition);
    }

    private boolean shouldAutoSyncSelectedCompetition() {
        String normalizedCompetitionCode = normalizeCode(competitionFilterCode);
        return normalizedCompetitionCode != null
                && displayedEquipes.isEmpty()
                && !syncingData
                && !Objects.equals(lastAutoSyncedCompetitionCode, normalizedCompetitionCode);
    }

    private boolean matchesSearch(Equipe equipe, String keyword) {
        return normalize(equipe.getNom()).contains(keyword) || normalize(equipe.getCoach()).contains(keyword);
    }

    private Comparator<Equipe> buildComparator() {
        Comparator<Equipe> comparator;
        String selectedSort = sortChoiceBox.getValue();
        if ("Id".equals(selectedSort)) {
            comparator = Comparator.comparing(Equipe::getId, Comparator.nullsLast(Integer::compareTo));
        } else if ("Coach".equals(selectedSort)) {
            comparator = Comparator.comparing(Equipe::getCoach, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else {
            comparator = Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        return sortDescending ? comparator.reversed() : comparator;
    }

    private ListCell<Equipe> createEquipeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Equipe equipe, boolean empty) {
                super.updateItem(equipe, empty);
                if (empty || equipe == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                StackPane logoPane = createLogoPane(equipe);

                Label nameLabel = new Label(emptyIfNull(equipe.getNom()));
                nameLabel.getStyleClass().add("card-title");
                nameLabel.setWrapText(true);

                String coach = emptyToNull(equipe.getCoach());
                Label coachLabel = new Label(coach == null ? "Coach non renseigne" : "Coach : " + coach);
                coachLabel.getStyleClass().add(coach == null ? "card-subtitle-muted" : "card-subtitle");
                coachLabel.setWrapText(true);

                Label competitionLabel = new Label(resolveCompetitionLabel(equipe.getCompetitionCode()));
                competitionLabel.getStyleClass().add("team-card-competition-badge");

                String logoState = emptyToNull(equipe.getImage()) == null ? "Sans logo" : "Logo disponible";
                Label metaLabel = new Label("#" + equipe.getId() + "  |  " + logoState);
                metaLabel.getStyleClass().add("card-meta");

                Label ctaLabel = new Label("Ouvrir la fiche");
                ctaLabel.getStyleClass().add("card-link");

                VBox textBox = new VBox(6, competitionLabel, nameLabel, coachLabel, metaLabel, ctaLabel);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox cardContent = new HBox(16, logoPane, textBox);
                cardContent.setAlignment(Pos.CENTER_LEFT);

                StackPane cardButton = new StackPane(cardContent);
                cardButton.getStyleClass().addAll("team-list-card", "team-list-card-clickable");
                cardButton.setOnMouseClicked(event -> openEquipeDetail(equipe));

                setText(null);
                setGraphic(cardButton);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private StackPane createLogoPane(Equipe equipe) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CARD_LOGO_SIZE);
        imageView.setFitHeight(CARD_LOGO_SIZE);
        imageView.setPreserveRatio(true);

        Image image = EquipeUiSupport.loadEquipeImage(equipe.getImage());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(equipe.getNom(), "SI"));
        fallbackLabel.getStyleClass().add("card-logo-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.setPrefSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.setMaxSize(CARD_LOGO_SIZE, CARD_LOGO_SIZE);
        logoPane.getStyleClass().add("card-logo-shell");
        return logoPane;
    }

    private void openEquipeDetail(Equipe equipe) {
        SceneNavigator.switchScene(
                equipeListView,
                "/tn/esprit/views/equipe-detail-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                emptyIfNull(equipe.getNom()) + " | Equipe",
                controller -> {
                    if (controller instanceof EquipeDetailController equipeDetailController) {
                        equipeDetailController.setEquipeContext(equipe);
                    }
                }
        );
    }

    private void openCompetitionSelector() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    private void updateCompetitionTexts() {
        String competitionLabel = resolveCompetitionLabel(competitionFilterCode);
        competitionChipLabel.setText(competitionFilterCode == null ? "Competitions" : competitionLabel);
        pageTitleLabel.setText(competitionFilterCode == null ? "Equipes par competition" : competitionLabel);
        pageSubtitleLabel.setText(
                competitionFilterCode == null
                        ? "Choisissez une competition pour voir ses clubs sur une page dediee."
                        : "Tous les clubs rattaches a " + competitionLabel + " sont listes ici."
        );
    }

    private void updateSortOrderButtonText() {
        sortOrderButton.setText(sortDescending ? "Decroissant" : "Croissant");
    }

    private void updateToolbarState() {
        boolean disabled = !serviceReady || loadingData || syncingData;
        refreshButton.setDisable(disabled);
        if (addButton != null) {
            addButton.setDisable(disabled);
        }
        sortOrderButton.setDisable(disabled);
        sortChoiceBox.setDisable(disabled);
        searchField.setDisable(disabled);
    }

    private void showStatus(String styleClass, String message) {
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private String resolveCompetitionLabel(String competitionCode) {
        String normalizedCode = normalizeCode(competitionCode);
        if (normalizedCode == null) {
            return "Competition non renseignee";
        }
        return FootballDataCompetitions.labelOf(normalizedCode);
    }

    private FootballDataSyncService ensureSyncService() {
        if (footballDataSyncService != null) {
            return footballDataSyncService;
        }

        try {
            footballDataSyncService = new FootballDataSyncService();
            return footballDataSyncService;
        } catch (Exception e) {
            showStatus("status-error", "Synchronisation football-data.org indisponible.");
            showAlert(Alert.AlertType.ERROR, "football-data.org",
                    "Impossible de preparer la synchronisation.\n" + e.getMessage());
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeCode(String value) {
        return FootballDataCompetitions.normalizeCode(value);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

