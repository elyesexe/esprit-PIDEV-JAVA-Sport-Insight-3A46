package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchFormController {
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final Map<String, String> COMPETITION_CODES_BY_LABEL = COMPETITION_LABELS.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private static final String STATUS_PROGRAMME = "Programme";
    private static final String STATUS_EN_DIRECT = "En direct";
    private static final String STATUS_FINI = "Fini";
    private static final String STATUS_REPORTE = "Reporte";
    private static final String STATUS_ANNULE = "Annule";

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
    private Label formModeChipLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private ComboBox<String> typeMatchComboBox;
    @FXML
    private DatePicker dateMatchPicker;
    @FXML
    private TextField heureDebutField;
    @FXML
    private TextField lieuField;
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
    private TextArea lineupDomicileArea;
    @FXML
    private TextArea lineupExterieurArea;
    @FXML
    private Button saveButton;

    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();

    private MatchsService matchsService;
    private EquipeService equipeService;
    private Map<Integer, Equipe> equipeById = Map.of();
    private Matchs editingMatch;
    private String returnCompetitionCode;
    private boolean updateMode;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureCompetitionChoices();
        configureStatusChoices();
        configureTeamComboBoxes();
        configureFormatters();
        configureLineupAreas();

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
            loadEquipes();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer le formulaire match.\n" + e.getMessage());
        }

        applyModeTexts();
    }

    public void configureForCreate(String initialCompetitionCode) {
        updateMode = false;
        editingMatch = null;
        returnCompetitionCode = initialCompetitionCode;
        clearForm();

        if (initialCompetitionCode != null) {
            typeMatchComboBox.getSelectionModel().select(resolveCompetitionLabel(initialCompetitionCode));
        }

        applyModeTexts();
    }

    public void configureForUpdate(Matchs match) {
        updateMode = true;
        editingMatch = match;
        returnCompetitionCode = match == null ? null : match.getCompetitionCode();
        populateForm(match);
        applyModeTexts();
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
        openCompetitionSelector();
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
    private void handleSave() {
        clearValidation();

        Matchs match = buildMatchFromForm();
        if (match == null || matchsService == null) {
            return;
        }

        try {
            if (updateMode && editingMatch != null) {
                match.setId(editingMatch.getId());
                match.setIdMatch(editingMatch.getIdMatch());
                matchsService.update(match);
                openDetail(match);
                return;
            }

            matchsService.add(match);
            openMatchList(match.getCompetitionCode());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, updateMode ? "Modification" : "Ajout",
                    "Impossible d'enregistrer le match.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (updateMode && editingMatch != null) {
            openDetail(editingMatch);
            return;
        }

        if (returnCompetitionCode != null) {
            openMatchList(returnCompetitionCode);
            return;
        }

        openCompetitionSelector();
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

    private void configureCompetitionChoices() {
        typeMatchComboBox.setItems(FXCollections.observableArrayList(COMPETITION_LABELS.values()));
    }

    private void configureStatusChoices() {
        statutComboBox.setItems(FXCollections.observableArrayList(
                STATUS_PROGRAMME,
                STATUS_EN_DIRECT,
                STATUS_FINI,
                STATUS_REPORTE,
                STATUS_ANNULE
        ));
        statutComboBox.getSelectionModel().select(STATUS_PROGRAMME);
        statutComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateScoreFieldsForStatus(true));
        updateScoreFieldsForStatus(true);
    }

    private void configureTeamComboBoxes() {
        equipeDomicileComboBox.setItems(equipes);
        equipeExterieurComboBox.setItems(equipes);
        equipeDomicileComboBox.setCellFactory(listView -> createEquipeCell());
        equipeDomicileComboBox.setButtonCell(createEquipeCell());
        equipeExterieurComboBox.setCellFactory(listView -> createEquipeCell());
        equipeExterieurComboBox.setButtonCell(createEquipeCell());
    }

    private void configureFormatters() {
        scoreDomicileField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        scoreExterieurField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        heureDebutField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9:]{0,5}") ? change : null));
    }

    private void configureLineupAreas() {
        lineupDomicileArea.setWrapText(true);
        lineupExterieurArea.setWrapText(true);
    }

    private void loadEquipes() throws SQLException {
        var loadedEquipes = equipeService.getAll();
        loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        equipes.setAll(loadedEquipes);
        equipeById = loadedEquipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(Equipe::getId, Function.identity(), (left, right) -> left));
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

    private void applyModeTexts() {
        if (formModeChipLabel == null) {
            return;
        }

        formModeChipLabel.setText(updateMode ? "Modification" : "Ajout");
        pageTitleLabel.setText(updateMode ? "Modifier le match" : "Ajouter un match");
        pageSubtitleLabel.setText(updateMode
                ? "Mettez a jour les informations du match puis validez."
                : "Renseignez le formulaire pour creer un nouveau match dans l'une des 6 competitions.");
        saveButton.setText(updateMode ? "Enregistrer" : "Ajouter");
    }

    private void populateForm(Matchs match) {
        clearForm();
        if (match == null) {
            return;
        }

        String competitionLabel = resolveCompetitionLabel(match.getCompetitionCode());
        if (competitionLabel == null && match.getType() != null && COMPETITION_CODES_BY_LABEL.containsKey(match.getType())) {
            competitionLabel = match.getType();
        }
        typeMatchComboBox.getSelectionModel().select(competitionLabel);
        dateMatchPicker.setValue(match.getDateMatch());
        heureDebutField.setText(match.getHeureDebut() == null ? "" : match.getHeureDebut().toString());
        lieuField.setText(emptyIfNull(match.getLieu()));
        statutComboBox.setValue(normalizeMatchStatus(match.getStatut()) == null ? STATUS_PROGRAMME : normalizeMatchStatus(match.getStatut()));
        selectEquipe(equipeDomicileComboBox, match.getEquipeDomicileId());
        selectEquipe(equipeExterieurComboBox, match.getEquipeExterieurId());
        scoreDomicileField.setText(match.getScoreEquipeDomicile() == null ? "" : String.valueOf(match.getScoreEquipeDomicile()));
        scoreExterieurField.setText(match.getScoreEquipeExterieur() == null ? "" : String.valueOf(match.getScoreEquipeExterieur()));
        lineupDomicileArea.setText(emptyIfNull(match.getLineupDomicile()));
        lineupExterieurArea.setText(emptyIfNull(match.getLineupExterieur()));
        updateScoreFieldsForStatus(false);
    }

    private void clearForm() {
        typeMatchComboBox.getSelectionModel().clearSelection();
        dateMatchPicker.setValue(null);
        heureDebutField.clear();
        lieuField.clear();
        statutComboBox.setValue(STATUS_PROGRAMME);
        equipeDomicileComboBox.getSelectionModel().clearSelection();
        equipeExterieurComboBox.getSelectionModel().clearSelection();
        scoreDomicileField.clear();
        scoreExterieurField.clear();
        lineupDomicileArea.clear();
        lineupExterieurArea.clear();
        updateScoreFieldsForStatus(true);
        clearValidation();
    }

    private Matchs buildMatchFromForm() {
        String competitionLabel = typeMatchComboBox.getValue();
        String competitionCode = resolveCompetitionCode(competitionLabel);
        LocalDate dateMatch = dateMatchPicker.getValue();
        String heureText = emptyToNull(heureDebutField.getText());
        String lieu = emptyToNull(lieuField.getText());
        String statut = normalizeMatchStatus(statutComboBox.getValue());
        Equipe equipeDomicile = equipeDomicileComboBox.getValue();
        Equipe equipeExterieur = equipeExterieurComboBox.getValue();
        String scoreDomicileText = emptyToNull(scoreDomicileField.getText());
        String scoreExterieurText = emptyToNull(scoreExterieurField.getText());
        String lineupDomicile = emptyToNull(lineupDomicileArea.getText());
        String lineupExterieur = emptyToNull(lineupExterieurArea.getText());

        if (competitionCode == null) {
            markFieldInvalid(typeMatchComboBox);
            showValidation("Choisissez le type de match parmi les 6 competitions.");
            return null;
        }

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

        if (statut == null) {
            markFieldInvalid(statutComboBox);
            showValidation("Choisissez un statut valide.");
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
        if (isScoreLockedStatus(statut)) {
            if (scoreDomicileText != null || scoreExterieurText != null) {
                markFieldInvalid(scoreDomicileField);
                markFieldInvalid(scoreExterieurField);
                showValidation("Ce statut ne permet pas de score.");
                return null;
            }
        } else {
            if (requiresFinalScores(statut) && (scoreDomicileText == null || scoreExterieurText == null)) {
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
            if (scoreDomicileText != null && scoreDomicile == null) {
                return null;
            }

            scoreExterieur = parseScore(scoreExterieurText, scoreExterieurField);
            if (scoreExterieurText != null && scoreExterieur == null) {
                return null;
            }
        }

        String idMatch = updateMode && editingMatch != null && editingMatch.getIdMatch() != null
                ? editingMatch.getIdMatch()
                : generateMatchReference();

        Matchs match = new Matchs(
                idMatch,
                dateMatch,
                heureDebut,
                lieu,
                competitionLabel,
                statut,
                lineupDomicile,
                lineupExterieur,
                scoreDomicile,
                scoreExterieur,
                equipeDomicile.getId(),
                equipeExterieur.getId()
        );
        match.setCompetitionCode(competitionCode);
        return match;
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

    private void selectEquipe(ComboBox<Equipe> comboBox, Integer equipeId) {
        if (equipeId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        Equipe equipe = equipeById.get(equipeId);
        if (equipe != null) {
            comboBox.getSelectionModel().select(equipe);
        }
    }

    private void updateScoreFieldsForStatus(boolean clearWhenLocked) {
        String status = normalizeMatchStatus(statutComboBox.getValue());
        boolean locked = isScoreLockedStatus(status);
        scoreDomicileField.setDisable(locked);
        scoreExterieurField.setDisable(locked);
        if (locked && clearWhenLocked) {
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
        if (normalized.contains("direct") || normalized.contains("cours") || normalized.contains("live")) {
            return STATUS_EN_DIRECT;
        }
        if (normalized.startsWith("fini") || normalized.contains("term")) {
            return STATUS_FINI;
        }
        if (normalized.contains("report") || normalized.contains("postpon") || normalized.contains("suspend")) {
            return STATUS_REPORTE;
        }
        if (normalized.contains("annul") || normalized.contains("cancel")) {
            return STATUS_ANNULE;
        }
        return null;
    }

    private boolean isScoreLockedStatus(String status) {
        String normalizedStatus = normalizeMatchStatus(status);
        return normalizedStatus == null
                || STATUS_PROGRAMME.equals(normalizedStatus)
                || STATUS_REPORTE.equals(normalizedStatus)
                || STATUS_ANNULE.equals(normalizedStatus);
    }

    private boolean requiresFinalScores(String status) {
        return STATUS_FINI.equals(normalizeMatchStatus(status));
    }

    private String resolveCompetitionCode(String label) {
        return label == null ? null : COMPETITION_CODES_BY_LABEL.get(label);
    }

    private String resolveCompetitionLabel(String competitionCode) {
        return competitionCode == null ? null : COMPETITION_LABELS.getOrDefault(competitionCode, competitionCode);
    }

    private void openCompetitionSelector() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    private void openMatchList(String competitionCode) {
        SceneNavigator.switchScene(
                saveButton,
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

    private void openDetail(Matchs match) {
        SceneNavigator.switchScene(
                saveButton,
                "/tn/esprit/views/match-detail-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Fiche match",
                controller -> {
                    if (controller instanceof MatchDetailController matchDetailController) {
                        matchDetailController.setMatchContext(match);
                    }
                }
        );
    }

    private String generateMatchReference() {
        return "MATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
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
        clearFieldError(typeMatchComboBox);
        clearFieldError(dateMatchPicker);
        clearFieldError(heureDebutField);
        clearFieldError(lieuField);
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

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

