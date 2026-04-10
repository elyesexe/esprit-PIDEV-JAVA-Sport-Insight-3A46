package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;
    private static final double CARD_LOGO_SIZE = 68;
    private static final String STATUS_PROGRAMME = "Programme";
    private static final String STATUS_FINI = "Fini";

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button sidebarOpenButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<Matchs> matchListView;
    @FXML
    private Label detailBadgeLabel;
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
    private Label validationLabel;
    @FXML
    private DatePicker dateMatchPicker;
    @FXML
    private TextField heureDebutField;
    @FXML
    private TextField lieuField;
    @FXML
    private TextField typeField;
    @FXML
    private ComboBox<String> statutComboBox;
    @FXML
    private ComboBox<Equipe> equipeDomicileComboBox;
    @FXML
    private ComboBox<Equipe> equipeExterieurComboBox;
    @FXML
    private TextField scoreDomicileField;
    @FXML
    private TextField scoreExterieurField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button refreshButton;

    private final ObservableList<Matchs> matchs = FXCollections.observableArrayList();
    private final FilteredList<Matchs> filteredMatchs = new FilteredList<>(matchs, match -> true);
    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();

    private MatchsService matchsService;
    private EquipeService equipeService;
    private Map<Integer, Equipe> equipeById = Map.of();
    private Matchs selectedMatch;
    private boolean serviceReady;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabel();
        configureSearch();
        configureTeamComboBoxes();
        configureStatusChoices();
        configureFormatters();
        configureMatchList();
        bindFormState();
        updateActionAvailability();
        updateCounters();
        updateDetailPanel();

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            serviceReady = true;
            refreshData(null);
            showSuccessStatus("Module Match pret.");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les matchs.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        clearValidation();
        Matchs match = buildMatchFromForm(false);
        if (match == null || matchsService == null) {
            return;
        }

        try {
            matchsService.add(match);
            refreshData(null);
            clearForm();
            showSuccessStatus("Match ajoute avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant l'ajout.");
            showAlert(Alert.AlertType.ERROR, "Ajout", "Erreur lors de l'ajout du match.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        clearValidation();
        if (selectedMatch == null) {
            showValidation("Selectionnez un match a modifier.");
            return;
        }

        Matchs match = buildMatchFromForm(true);
        if (match == null || matchsService == null) {
            return;
        }

        match.setId(selectedMatch.getId());
        match.setIdMatch(selectedMatch.getIdMatch());

        try {
            matchsService.update(match);
            refreshData(selectedMatch.getId());
            showSuccessStatus("Match modifie avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la modification.");
            showAlert(Alert.AlertType.ERROR, "Modification", "Erreur lors de la modification du match.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        clearValidation();
        if (selectedMatch == null) {
            showValidation("Selectionnez un match a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le match \"" + buildMatchLabel(selectedMatch) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            matchsService.delete(selectedMatch.getId());
            refreshData(null);
            clearForm();
            showSuccessStatus("Match supprime avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la suppression.");
            showAlert(Alert.AlertType.ERROR, "Suppression", "Erreur lors de la suppression du match.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshData(getSelectedMatchId());
        showMutedStatus("Liste des matchs actualisee.");
    }

    @FXML
    private void handleClear() {
        clearForm();
        showMutedStatus("Formulaire vide.");
    }

    @FXML
    private void handleOpenSidebar() {
        applySidebarState(true);
    }

    @FXML
    private void handleToggleSidebar() {
        applySidebarState(false);
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/equipe-crud-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Sport Insight");
    }

    @FXML
    private void handleOpenMatchs() {
        showMutedStatus("Vous etes deja dans le module Matchs.");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    private void configureSidebar() {
        applySidebarState(true);
        if (!matchsNavButton.getStyleClass().contains("sidebar-nav-button-active")) {
            matchsNavButton.getStyleClass().add("sidebar-nav-button-active");
        }
    }

    private void configureStatusLabel() {
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        setStatusStyle("status-muted");
        statusLabel.setText("Pret");
    }

    private void configureSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureTeamComboBoxes() {
        equipeDomicileComboBox.setItems(equipes);
        equipeExterieurComboBox.setItems(equipes);
        equipeDomicileComboBox.setCellFactory(listView -> createEquipeCell());
        equipeDomicileComboBox.setButtonCell(createEquipeCell());
        equipeExterieurComboBox.setCellFactory(listView -> createEquipeCell());
        equipeExterieurComboBox.setButtonCell(createEquipeCell());
    }

    private void configureStatusChoices() {
        statutComboBox.setItems(FXCollections.observableArrayList(STATUS_PROGRAMME, STATUS_FINI));
        statutComboBox.getSelectionModel().select(STATUS_PROGRAMME);
        updateScoreFieldsForStatus(true);
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

    private void configureFormatters() {
        scoreDomicileField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        scoreExterieurField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        heureDebutField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9:]{0,5}") ? change : null));
    }

    private void configureMatchList() {
        matchListView.setItems(filteredMatchs);
        matchListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Matchs item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(buildMatchCard(item));
            }
        });

        matchListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedMatch = newValue;
            if (newValue != null) {
                populateForm(newValue);
            } else if (!hasDraftContent()) {
                clearFormFieldsOnly();
            }

            clearValidation();
            updateActionAvailability();
            updateSelectionState();
            updateDetailPanel();
        });
    }

    private VBox buildMatchCard(Matchs match) {
        Label statusChip = new Label(resolveStatus(match));
        statusChip.getStyleClass().add("fixture-status");
        applyFixtureStatusStyle(statusChip, match.getStatut());

        Label dateLabel = new Label(formatDate(match.getDateMatch()) + "  |  " + formatTime(match.getHeureDebut()));
        dateLabel.getStyleClass().add("fixture-date");

        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);

        Label idLabel = new Label(match.getIdMatch() == null ? "#" + match.getId() : match.getIdMatch());
        idLabel.getStyleClass().add("fixture-id");

        HBox head = new HBox(10, statusChip, headSpacer, dateLabel, idLabel);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("fixture-card-head");

        Equipe homeTeam = getEquipe(match.getEquipeDomicileId());
        Equipe awayTeam = getEquipe(match.getEquipeExterieurId());

        VBox homeBox = buildTeamPreview(homeTeam, "Domicile");
        VBox awayBox = buildTeamPreview(awayTeam, "Exterieur");

        Label scoreLabel = new Label(buildScore(match));
        scoreLabel.getStyleClass().add("fixture-score-value");

        Label versusLabel = new Label("VS");
        versusLabel.getStyleClass().add("fixture-score-caption");

        VBox scoreBox = new VBox(2, scoreLabel, versusLabel);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.getStyleClass().add("fixture-score-shell");

        HBox teamsRow = new HBox(16, homeBox, scoreBox, awayBox);
        teamsRow.setAlignment(Pos.CENTER);
        teamsRow.getStyleClass().add("fixture-teams-row");
        HBox.setHgrow(homeBox, Priority.ALWAYS);
        HBox.setHgrow(awayBox, Priority.ALWAYS);

        Label locationChip = new Label(emptyToNull(match.getLieu()) == null ? "Lieu non renseigne" : match.getLieu());
        locationChip.getStyleClass().add("fixture-meta-chip");

        Label typeChip = new Label(emptyToNull(match.getType()) == null ? "Type non renseigne" : match.getType());
        typeChip.getStyleClass().add("fixture-meta-chip");

        Label detailChip = new Label("Voir les details");
        detailChip.getStyleClass().add("fixture-link-chip");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        HBox metaRow = new HBox(10, locationChip, typeChip, metaSpacer, detailChip);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("fixture-meta-row");

        VBox card = new VBox(14, head, teamsRow, metaRow);
        card.getStyleClass().add("fixture-card");
        return card;
    }

    private VBox buildTeamPreview(Equipe equipe, String fallbackRole) {
        String teamName = equipe == null ? "Equipe " + fallbackRole.toLowerCase() : emptyIfNull(equipe.getNom());
        StackPane logoPane = createLogoPane(equipe == null ? null : equipe.getImage(), teamName, CARD_LOGO_SIZE, "fixture-team-logo-shell", "fixture-team-fallback");

        Label nameLabel = new Label(teamName);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("fixture-team-name");

        Label roleLabel = new Label(fallbackRole);
        roleLabel.getStyleClass().add("fixture-team-role");

        VBox box = new VBox(10, logoPane, nameLabel, roleLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("fixture-team-box");
        return box;
    }

    private void bindFormState() {
        dateMatchPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(dateMatchPicker);
            updateSelectionState();
            updateDetailPanel();
        });
        heureDebutField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(heureDebutField);
            updateSelectionState();
            updateDetailPanel();
        });
        lieuField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(lieuField);
            updateSelectionState();
            updateDetailPanel();
        });
        typeField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(typeField);
            updateSelectionState();
            updateDetailPanel();
        });
        statutComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(statutComboBox);
            updateScoreFieldsForStatus(true);
            updateSelectionState();
            updateDetailPanel();
        });
        equipeDomicileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(equipeDomicileComboBox);
            updateSelectionState();
            updateDetailPanel();
        });
        equipeExterieurComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(equipeExterieurComboBox);
            updateSelectionState();
            updateDetailPanel();
        });
        scoreDomicileField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(scoreDomicileField);
            updateDetailPanel();
        });
        scoreExterieurField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(scoreExterieurField);
            updateDetailPanel();
        });
    }

    private void refreshData(Integer preferredSelectionId) {
        if (matchsService == null || equipeService == null) {
            return;
        }

        try {
            loadEquipes();
            List<Matchs> loadedMatchs = new ArrayList<>(matchsService.getAll());
            loadedMatchs.sort(Comparator
                    .comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(Matchs::getHeureDebut, Comparator.nullsLast(LocalTime::compareTo))
                    .reversed());
            matchs.setAll(loadedMatchs);
            applyFilters();
            restoreSelection(preferredSelectionId);
            matchListView.refresh();
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant le chargement.");
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des matchs.\n" + e.getMessage());
        }
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        filteredMatchs.setPredicate(match -> query == null || matchesQuery(match, query));
        updateCounters();

        if (selectedMatch != null && !filteredMatchs.contains(selectedMatch)) {
            selectedMatch = null;
            matchListView.getSelectionModel().clearSelection();
            updateActionAvailability();
        }
    }

    private boolean matchesQuery(Matchs match, String query) {
        return containsNormalized(buildMatchLabel(match), query)
                || containsNormalized(getEquipeName(match.getEquipeDomicileId()), query)
                || containsNormalized(getEquipeName(match.getEquipeExterieurId()), query)
                || containsNormalized(match.getLieu(), query)
                || containsNormalized(match.getType(), query)
                || containsNormalized(match.getStatut(), query)
                || containsNormalized(formatDate(match.getDateMatch()), query);
    }

    private void updateCounters() {
        int count = filteredMatchs.size();
        resultCountLabel.setText(count + " match(s)");
        resultsMetaLabel.setText(count + " rencontre(s) affichee(s)");
        updateSelectionState();
    }

    private void updateSelectionState() {
        if (selectedMatch != null) {
            selectionStateLabel.setText("Selection : " + buildMatchLabel(selectedMatch));
            return;
        }

        selectionStateLabel.setText(hasDraftContent() ? "Brouillon en cours" : "Aucune selection");
    }

    private void loadEquipes() throws SQLException {
        Integer selectedDomicileId = selectedEquipeId(equipeDomicileComboBox);
        Integer selectedExterieurId = selectedEquipeId(equipeExterieurComboBox);

        List<Equipe> loadedEquipes = new ArrayList<>(equipeService.getAll());
        loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        equipes.setAll(loadedEquipes);
        equipeById = loadedEquipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, Function.identity(), (left, right) -> left));

        selectEquipe(equipeDomicileComboBox, selectedDomicileId);
        selectEquipe(equipeExterieurComboBox, selectedExterieurId);
    }

    private Matchs buildMatchFromForm(boolean updateMode) {
        LocalDate dateMatch = dateMatchPicker.getValue();
        String heureText = emptyToNull(heureDebutField.getText());
        String lieu = emptyToNull(lieuField.getText());
        String type = emptyToNull(typeField.getText());
        String statut = normalizeMatchStatus(statutComboBox.getValue());
        Equipe equipeDomicile = equipeDomicileComboBox.getValue();
        Equipe equipeExterieur = equipeExterieurComboBox.getValue();
        String scoreDomicileText = emptyToNull(scoreDomicileField.getText());
        String scoreExterieurText = emptyToNull(scoreExterieurField.getText());

        if (dateMatch == null) {
            markFieldInvalid(dateMatchPicker);
            showValidation("La date du match est obligatoire.");
            return null;
        }

        if (heureText == null) {
            markFieldInvalid(heureDebutField);
            showValidation("L'heure de debut est obligatoire.");
            return null;
        }

        LocalTime heureDebut;
        try {
            heureDebut = LocalTime.parse(heureText);
        } catch (DateTimeParseException e) {
            markFieldInvalid(heureDebutField);
            showValidation("L'heure doit etre au format HH:mm.");
            return null;
        }

        if (lieu == null) {
            markFieldInvalid(lieuField);
            showValidation("Le lieu est obligatoire.");
            return null;
        }

        if (type == null) {
            markFieldInvalid(typeField);
            showValidation("Le type est obligatoire.");
            return null;
        }

        if (statut == null) {
            markFieldInvalid(statutComboBox);
            showValidation("Choisissez un statut: Programme ou Fini.");
            return null;
        }

        if (equipeDomicile == null || equipeDomicile.getId() == null) {
            markFieldInvalid(equipeDomicileComboBox);
            showValidation("L'equipe domicile est obligatoire.");
            return null;
        }

        if (equipeExterieur == null || equipeExterieur.getId() == null) {
            markFieldInvalid(equipeExterieurComboBox);
            showValidation("L'equipe exterieur est obligatoire.");
            return null;
        }

        if (Objects.equals(equipeDomicile.getId(), equipeExterieur.getId())) {
            markFieldInvalid(equipeDomicileComboBox);
            markFieldInvalid(equipeExterieurComboBox);
            showValidation("Les equipes domicile et exterieur doivent etre differentes.");
            return null;
        }

        Integer scoreDomicile = null;
        Integer scoreExterieur = null;
        if (STATUS_PROGRAMME.equals(statut)) {
            if (scoreDomicileText != null || scoreExterieurText != null) {
                markFieldInvalid(scoreDomicileField);
                markFieldInvalid(scoreExterieurField);
                showValidation("Un match programme ne peut pas avoir de score.");
                return null;
            }
        } else {
            if (scoreDomicileText == null || scoreExterieurText == null) {
                if (scoreDomicileText == null) {
                    markFieldInvalid(scoreDomicileField);
                }
                if (scoreExterieurText == null) {
                    markFieldInvalid(scoreExterieurField);
                }
                showValidation("Pour un match fini, les deux scores sont obligatoires.");
                return null;
            }

            scoreDomicile = parseScore(scoreDomicileText, scoreDomicileField);
            if (scoreDomicile == null) {
                return null;
            }

            scoreExterieur = parseScore(scoreExterieurText, scoreExterieurField);
            if (scoreExterieur == null) {
                return null;
            }
        }

        String idMatch = updateMode && selectedMatch != null && selectedMatch.getIdMatch() != null
                ? selectedMatch.getIdMatch()
                : generateMatchReference();

        return new Matchs(
                idMatch,
                dateMatch,
                heureDebut,
                lieu,
                type,
                statut,
                "",
                "",
                scoreDomicile,
                scoreExterieur,
                equipeDomicile.getId(),
                equipeExterieur.getId()
        );
    }

    private Integer parseScore(String scoreText, Control field) {
        if (scoreText == null) {
            return null;
        }

        try {
            int value = Integer.parseInt(scoreText);
            if (value < 0) {
                markFieldInvalid(field);
                showValidation("Les scores doivent etre positifs.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            markFieldInvalid(field);
            showValidation("Les scores doivent etre numeriques.");
            return null;
        }
    }

    private void populateForm(Matchs match) {
        dateMatchPicker.setValue(match.getDateMatch());
        heureDebutField.setText(match.getHeureDebut() == null ? "" : match.getHeureDebut().format(TIME_FORMATTER));
        lieuField.setText(emptyIfNull(match.getLieu()));
        typeField.setText(emptyIfNull(match.getType()));
        String status = normalizeMatchStatus(match.getStatut());
        statutComboBox.setValue(status == null ? STATUS_PROGRAMME : status);
        scoreDomicileField.setText(match.getScoreEquipeDomicile() == null ? "" : String.valueOf(match.getScoreEquipeDomicile()));
        scoreExterieurField.setText(match.getScoreEquipeExterieur() == null ? "" : String.valueOf(match.getScoreEquipeExterieur()));
        selectEquipe(equipeDomicileComboBox, match.getEquipeDomicileId());
        selectEquipe(equipeExterieurComboBox, match.getEquipeExterieurId());
        updateScoreFieldsForStatus(true);
    }

    private void restoreSelection(Integer preferredSelectionId) {
        if (preferredSelectionId == null) {
            selectedMatch = null;
            matchListView.getSelectionModel().clearSelection();
            updateSelectionState();
            return;
        }

        for (Matchs match : filteredMatchs) {
            if (Objects.equals(match.getId(), preferredSelectionId)) {
                matchListView.getSelectionModel().select(match);
                matchListView.scrollTo(match);
                selectedMatch = match;
                updateSelectionState();
                return;
            }
        }

        selectedMatch = null;
        matchListView.getSelectionModel().clearSelection();
        updateSelectionState();
    }

    private void updateDetailPanel() {
        Matchs effectiveMatch = selectedMatch;
        Equipe homeTeam = resolveEquipe(equipeDomicileComboBox.getValue(), effectiveMatch == null ? null : effectiveMatch.getEquipeDomicileId());
        Equipe awayTeam = resolveEquipe(equipeExterieurComboBox.getValue(), effectiveMatch == null ? null : effectiveMatch.getEquipeExterieurId());
        String draftLabel = buildDraftLabel(homeTeam, awayTeam);

        if (effectiveMatch == null && !hasDraftContent()) {
            detailBadgeLabel.setText("Apercu");
            detailStatusChipLabel.setText("A venir");
            applyDetailStatusStyle(detailStatusChipLabel, null);
            detailTitleLabel.setText("Aucun match selectionne");
            detailSubtitleLabel.setText("Selectionnez une rencontre ou commencez une nouvelle creation pour afficher la fiche detail.");
            detailScoreValueLabel.setText("-  :  -");
            detailDateValueLabel.setText("-");
            detailHeureValueLabel.setText("-");
            detailLieuValueLabel.setText("Non renseigne");
            detailTypeValueLabel.setText("Non renseigne");
            detailStatutValueLabel.setText("Programme");
            detailIdValueLabel.setText("Nouveau");
            detailHomeNameLabel.setText("Equipe domicile");
            detailAwayNameLabel.setText("Equipe exterieur");
            updateDetailLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, null, "D");
            updateDetailLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, null, "E");
            return;
        }

        String status = resolveFieldValue(
                normalizeMatchStatus(statutComboBox.getValue()),
                normalizeMatchStatus(effectiveMatch == null ? null : effectiveMatch.getStatut()),
                STATUS_PROGRAMME
        );
        String title = draftLabel == null ? buildMatchLabel(effectiveMatch) : draftLabel;

        detailBadgeLabel.setText(selectedMatch == null ? "Brouillon" : "Fiche match");
        detailStatusChipLabel.setText(status);
        applyDetailStatusStyle(detailStatusChipLabel, status);
        detailTitleLabel.setText(title);
        detailSubtitleLabel.setText(buildDetailSubtitle(homeTeam, awayTeam));
        detailScoreValueLabel.setText(buildDraftScore(effectiveMatch));
        detailDateValueLabel.setText(formatDate(dateMatchPicker.getValue() != null ? dateMatchPicker.getValue() : effectiveMatch == null ? null : effectiveMatch.getDateMatch()));
        detailHeureValueLabel.setText(formatTime(parseTimeSafely(heureDebutField.getText(), effectiveMatch == null ? null : effectiveMatch.getHeureDebut())));
        detailLieuValueLabel.setText(resolveFieldValue(lieuField.getText(), effectiveMatch == null ? null : effectiveMatch.getLieu(), "Non renseigne"));
        detailTypeValueLabel.setText(resolveFieldValue(typeField.getText(), effectiveMatch == null ? null : effectiveMatch.getType(), "Non renseigne"));
        detailStatutValueLabel.setText(status);
        detailIdValueLabel.setText(selectedMatch == null ? "Nouveau" : (effectiveMatch.getIdMatch() == null ? "#" + effectiveMatch.getId() : effectiveMatch.getIdMatch()));
        detailHomeNameLabel.setText(homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom()));
        detailAwayNameLabel.setText(awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom()));
        updateDetailLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, homeTeam, "D");
        updateDetailLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, awayTeam, "E");
    }

    private String buildDetailSubtitle(Equipe homeTeam, Equipe awayTeam) {
        String home = homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom());
        String away = awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom());
        return home + " recoit " + away + " dans une presentation inspiree du front-office Symfony.";
    }

    private void updateDetailLogo(ImageView imageView, Label fallbackLabel, Equipe equipe, String defaultLetter) {
        Image image = equipe == null ? null : loadImage(equipe.getImage());
        boolean hasImage = image != null;

        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setText(equipe == null ? defaultLetter : buildInitials(equipe.getNom(), defaultLetter));
    }

    private void clearForm() {
        matchListView.getSelectionModel().clearSelection();
        selectedMatch = null;
        clearFormFieldsOnly();
        clearValidation();
        updateActionAvailability();
        updateSelectionState();
        updateDetailPanel();
    }

    private void clearFormFieldsOnly() {
        dateMatchPicker.setValue(null);
        heureDebutField.clear();
        lieuField.clear();
        typeField.clear();
        statutComboBox.setValue(STATUS_PROGRAMME);
        scoreDomicileField.clear();
        scoreExterieurField.clear();
        equipeDomicileComboBox.getSelectionModel().clearSelection();
        equipeExterieurComboBox.getSelectionModel().clearSelection();
        updateScoreFieldsForStatus(true);
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedMatch != null;
        addButton.setDisable(!serviceReady);
        updateButton.setDisable(!serviceReady || !hasSelection);
        deleteButton.setDisable(!serviceReady || !hasSelection);
        clearButton.setDisable(!serviceReady);
        refreshButton.setDisable(!serviceReady);
    }

    private boolean hasDraftContent() {
        return dateMatchPicker.getValue() != null
                || emptyToNull(heureDebutField.getText()) != null
                || emptyToNull(lieuField.getText()) != null
                || emptyToNull(typeField.getText()) != null
                || (normalizeMatchStatus(statutComboBox.getValue()) != null
                && !STATUS_PROGRAMME.equals(normalizeMatchStatus(statutComboBox.getValue())))
                || emptyToNull(scoreDomicileField.getText()) != null
                || emptyToNull(scoreExterieurField.getText()) != null
                || equipeDomicileComboBox.getValue() != null
                || equipeExterieurComboBox.getValue() != null;
    }

    private Integer getSelectedMatchId() {
        return selectedMatch == null ? null : selectedMatch.getId();
    }

    private Integer selectedEquipeId(ComboBox<Equipe> comboBox) {
        Equipe equipe = comboBox.getValue();
        return equipe == null ? null : equipe.getId();
    }

    private void selectEquipe(ComboBox<Equipe> comboBox, Integer equipeId) {
        if (equipeId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        for (Equipe equipe : equipes) {
            if (Objects.equals(equipe.getId(), equipeId)) {
                comboBox.getSelectionModel().select(equipe);
                return;
            }
        }
        comboBox.getSelectionModel().clearSelection();
    }

    private String buildMatchLabel(Matchs match) {
        if (match == null) {
            return "Nouveau match";
        }
        return getEquipeName(match.getEquipeDomicileId()) + " vs " + getEquipeName(match.getEquipeExterieurId());
    }

    private String buildDraftLabel(Equipe homeTeam, Equipe awayTeam) {
        if (homeTeam == null && awayTeam == null) {
            return null;
        }
        String home = homeTeam == null ? "Equipe domicile" : emptyIfNull(homeTeam.getNom());
        String away = awayTeam == null ? "Equipe exterieur" : emptyIfNull(awayTeam.getNom());
        return home + " vs " + away;
    }

    private String getEquipeName(Integer equipeId) {
        Equipe equipe = getEquipe(equipeId);
        return equipe == null ? "Equipe inconnue" : emptyIfNull(equipe.getNom());
    }

    private Equipe getEquipe(Integer equipeId) {
        return equipeId == null ? null : equipeById.get(equipeId);
    }

    private Equipe resolveEquipe(Equipe selectedEquipe, Integer fallbackId) {
        if (selectedEquipe != null) {
            return selectedEquipe;
        }
        return getEquipe(fallbackId);
    }

    private String buildDraftScore(Matchs effectiveMatch) {
        String status = resolveFieldValue(
                normalizeMatchStatus(statutComboBox.getValue()),
                normalizeMatchStatus(effectiveMatch == null ? null : effectiveMatch.getStatut()),
                STATUS_PROGRAMME
        );
        if (STATUS_PROGRAMME.equals(status)) {
            return "-  :  -";
        }

        String home = emptyToNull(scoreDomicileField.getText());
        String away = emptyToNull(scoreExterieurField.getText());
        if (home == null && effectiveMatch != null && effectiveMatch.getScoreEquipeDomicile() != null) {
            home = String.valueOf(effectiveMatch.getScoreEquipeDomicile());
        }
        if (away == null && effectiveMatch != null && effectiveMatch.getScoreEquipeExterieur() != null) {
            away = String.valueOf(effectiveMatch.getScoreEquipeExterieur());
        }
        return (home == null ? "-" : home) + "  :  " + (away == null ? "-" : away);
    }

    private String buildScore(Matchs match) {
        return (match.getScoreEquipeDomicile() == null ? "-" : match.getScoreEquipeDomicile())
                + " : "
                + (match.getScoreEquipeExterieur() == null ? "-" : match.getScoreEquipeExterieur());
    }

    private String resolveFieldValue(String draftValue, String fallbackValue, String emptyValue) {
        String cleanedDraft = emptyToNull(draftValue);
        if (cleanedDraft != null) {
            return cleanedDraft;
        }
        String cleanedFallback = emptyToNull(fallbackValue);
        return cleanedFallback == null ? emptyValue : cleanedFallback;
    }

    private String resolveStatus(Matchs match) {
        String status = match == null ? null : emptyToNull(match.getStatut());
        return status == null ? "Programme" : status;
    }

    private LocalTime parseTimeSafely(String text, LocalTime fallback) {
        String value = emptyToNull(text);
        if (value == null) {
            return fallback;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private String generateMatchReference() {
        return "MATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
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
        setStatusStyle(styleClass);
    }

    private void setStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void clearValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        clearFieldError(dateMatchPicker);
        clearFieldError(heureDebutField);
        clearFieldError(lieuField);
        clearFieldError(typeField);
        clearFieldError(statutComboBox);
        clearFieldError(equipeDomicileComboBox);
        clearFieldError(equipeExterieurComboBox);
        clearFieldError(scoreDomicileField);
        clearFieldError(scoreExterieurField);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private void applySidebarState(boolean visible) {
        sidebarRoot.setManaged(visible);
        sidebarRoot.setVisible(visible);
        sidebarRoot.setMinWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarOpenButton.setManaged(!visible);
        sidebarOpenButton.setVisible(!visible);
    }

    private void applyFixtureStatusStyle(Label label, String status) {
        label.getStyleClass().removeAll("fixture-status-scheduled", "fixture-status-live", "fixture-status-finished", "fixture-status-cancelled");
        label.getStyleClass().add(resolveFixtureStatusClass(status));
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
        if (normalized.contains("prog")) {
            return "fixture-status-scheduled";
        }
        return "fixture-status-scheduled";
    }

    private void updateScoreFieldsForStatus(boolean clearWhenProgramme) {
        String status = normalizeMatchStatus(statutComboBox.getValue());
        boolean programme = status == null || STATUS_PROGRAMME.equals(status);
        scoreDomicileField.setDisable(programme);
        scoreExterieurField.setDisable(programme);
        if (programme && clearWhenProgramme) {
            scoreDomicileField.clear();
            scoreExterieurField.clear();
        }
    }

    private String normalizeMatchStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("prog")) {
            return STATUS_PROGRAMME;
        }
        if (normalized.startsWith("fini") || normalized.contains("term")) {
            return STATUS_FINI;
        }
        return null;
    }

    private StackPane createLogoPane(String imagePath, String teamName, double size, String shellStyle, String fallbackStyle) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("match-logo-image");

        Label fallbackLabel = new Label(buildInitials(teamName, "SI"));
        fallbackLabel.getStyleClass().add(fallbackStyle);

        Image image = loadImage(imagePath);
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setVisible(hasImage);
        imageView.setManaged(hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setManaged(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(size, size);
        logoPane.setPrefSize(size, size);
        logoPane.setMaxSize(size, size);
        logoPane.getStyleClass().add(shellStyle);
        return logoPane;
    }

    private String buildInitials(String teamName, String fallback) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return fallback;
        }

        String[] parts = normalizedName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }

        return initials.isEmpty() ? fallback : initials.toString();
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

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
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
            return createImage(resource.toExternalForm());
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
            URL resource = MatchController.class.getResource(candidate);
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

        return loadImageWithImageIo(file);
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Image loadImageWithImageIo(File file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                return null;
            }
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
