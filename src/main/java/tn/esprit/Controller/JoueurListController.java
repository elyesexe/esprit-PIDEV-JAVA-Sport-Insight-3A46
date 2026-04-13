package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.JoueurUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JoueurListController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double CARD_IMAGE_SIZE = 82;

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
    private ListView<Joueur> joueurListView;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Button refreshButton;

    private final ObservableList<Joueur> joueurs = FXCollections.observableArrayList();
    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();
    private final FilteredList<Joueur> filteredJoueurs = new FilteredList<>(joueurs, joueur -> true);
    private final Map<Integer, Equipe> equipeById = new HashMap<>();

    private JoueurService joueurService;
    private EquipeService equipeService;
    private boolean serviceReady;
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
            refreshData();
            showSuccessStatus("Effectif charge.");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les joueurs.\n" + e.getMessage());
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
        showMutedStatus("Vous etes deja dans le module Joueurs.");
    }

    @FXML
    private void handleRefresh() {
        refreshData();
        showMutedStatus("Liste des joueurs actualisee.");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        equipeFilterComboBox.getSelectionModel().clearSelection();
        applyFilters();
        showMutedStatus("Filtres reinitialises.");
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
        joueurListView.setItems(filteredJoueurs);
        joueurListView.setPlaceholder(new Label(""));
        joueurListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Joueur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox card = buildPlayerCard(item);
                card.prefWidthProperty().bind(listView.widthProperty().subtract(26));
                card.setOnMouseClicked(event -> openJoueurDetail(item));
                setText(null);
                setGraphic(card);
            }
        });
    }

    private VBox buildPlayerCard(Joueur joueur) {
        HBox root = new HBox(16);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().addAll("player-list-card", "team-list-card-clickable");

        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("player-avatar-shell");
        avatarShell.setMinSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
        avatarShell.setPrefSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
        avatarShell.setMaxSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);

        Image image = JoueurUiSupport.loadJoueurImage(joueur.getImage());
        if (image != null) {
            ImageView avatarView = new ImageView(image);
            avatarView.setFitWidth(CARD_IMAGE_SIZE - 12);
            avatarView.setFitHeight(CARD_IMAGE_SIZE - 12);
            avatarView.setPreserveRatio(true);
            avatarShell.getChildren().add(avatarView);
        } else {
            Label fallback = new Label(buildInitials(joueur));
            fallback.getStyleClass().add("player-avatar-fallback");
            avatarShell.getChildren().add(fallback);
        }

        VBox content = new VBox(7);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(buildFullName(joueur));
        titleLabel.getStyleClass().add("player-card-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label numberLabel = new Label(buildPlayerBadge(joueur));
        numberLabel.getStyleClass().add("player-number-badge");

        titleRow.getChildren().addAll(titleLabel, numberLabel);

        Label teamLabel = new Label(buildPlayerSecondaryLine(joueur));
        teamLabel.getStyleClass().add("player-card-team");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label birthLabel = new Label(buildPlayerBirthLine(joueur));
        birthLabel.getStyleClass().add("player-card-meta");

        Label infoPill = new Label(buildPlayerMetaPill(joueur));
        infoPill.getStyleClass().add("player-card-meta-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        metaRow.getChildren().addAll(birthLabel, spacer, infoPill);
        content.getChildren().addAll(titleRow, teamLabel, metaRow);

        root.getChildren().addAll(avatarShell, content);
        root.setMaxWidth(Double.MAX_VALUE);

        VBox wrapper = new VBox(root);
        wrapper.setFillWidth(true);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    private void refreshData() {
        if (joueurService == null || equipeService == null) {
            return;
        }

        try {
            loadEquipes();

            List<Joueur> loadedJoueurs = new ArrayList<>(joueurService.getAll());
            loadedJoueurs.sort(Comparator
                    .comparing(this::getEquipeNameForSort, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Joueur::getNumero)
                    .thenComparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(Joueur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

            JoueurUiSupport.clearImageCache();
            joueurs.setAll(loadedJoueurs);
            applyFilters();
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant le chargement.");
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des joueurs.\n" + e.getMessage());
        }
    }

    private void loadEquipes() throws SQLException {
        Integer selectedFilterEquipeId = getSelectedFilterEquipeId();

        List<Equipe> loadedEquipes = new ArrayList<>(equipeService.getAll());
        loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        equipeById.clear();
        for (Equipe equipe : loadedEquipes) {
            if (equipe.getId() != null) {
                equipeById.put(equipe.getId(), equipe);
            }
        }

        equipes.setAll(loadedEquipes);
        selectEquipeInFilter(selectedFilterEquipeId);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        Integer filterEquipeId = getSelectedFilterEquipeId();

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
                joueurListView,
                "/tn/esprit/views/joueur-detail-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Fiche joueur",
                controller -> {
                    if (controller instanceof JoueurDetailController joueurDetailController) {
                        joueurDetailController.setJoueurContext(joueur);
                    }
                }
        );
    }

    private void updateActionAvailability() {
        boolean disabled = !serviceReady;
        refreshButton.setDisable(disabled);
        searchField.setDisable(disabled);
        equipeFilterComboBox.setDisable(disabled);
        joueurListView.setDisable(disabled);
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
}

