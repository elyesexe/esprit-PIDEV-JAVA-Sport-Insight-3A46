package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TabPane;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EvaluationService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.UserService;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntrainementAdminController {
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private ToggleButton themeToggleButton;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortChoiceBox;
    @FXML
    private TableView<Entrainement> tableView;
    @FXML
    private TableColumn<Entrainement, String> dateColumn;
    @FXML
    private TableColumn<Entrainement, String> typeColumn;
    @FXML
    private TableColumn<Entrainement, String> lieuColumn;
    @FXML
    private TableColumn<Entrainement, String> horaireColumn;
    @FXML
    private TableColumn<Entrainement, String> coachColumn;

    @FXML
    private Label formHintLabel;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField startField;
    @FXML
    private TextField endField;
    @FXML
    private TextField typeField;
    @FXML
    private TextField objectifField;
    @FXML
    private TextField lieuField;
    @FXML
    private ComboBox<CoachOption> coachField;
    @FXML
    private Label validationLabel;

    @FXML
    private TextField evaluationSearchField;
    @FXML
    private ComboBox<String> evaluationSortBox;
    @FXML
    private TableView<Evaluation> evaluationTableView;
    @FXML
    private TableColumn<Evaluation, String> evaluationEntrColumn;
    @FXML
    private TableColumn<Evaluation, String> evaluationPlayerColumn;
    @FXML
    private TableColumn<Evaluation, String> evaluationScoreColumn;
    @FXML
    private TableColumn<Evaluation, String> evaluationAvgColumn;
    @FXML
    private Label evaluationHintLabel;
    @FXML
    private ComboBox<TrainingOption> evaluationTrainingField;
    @FXML
    private ComboBox<UserOption> evaluationPlayerField;
    @FXML
    private TextField evaluationPhysField;
    @FXML
    private TextField evaluationTechField;
    @FXML
    private TextField evaluationTactField;
    @FXML
    private TextField evaluationCommentField;
    @FXML
    private Label evaluationValidationLabel;

    @FXML
    private TextField participationSearchField;
    @FXML
    private ComboBox<String> participationSortBox;
    @FXML
    private TableView<Participation> participationTableView;
    @FXML
    private TableColumn<Participation, String> participationEntrColumn;
    @FXML
    private TableColumn<Participation, String> participationPlayerColumn;
    @FXML
    private TableColumn<Participation, String> participationPresenceColumn;
    @FXML
    private TableColumn<Participation, String> participationJustifColumn;
    @FXML
    private Label participationHintLabel;
    @FXML
    private ComboBox<TrainingOption> participationTrainingField;
    @FXML
    private ComboBox<UserOption> participationPlayerField;
    @FXML
    private ComboBox<String> participationPresenceFormField;
    @FXML
    private TextField participationJustifField;
    @FXML
    private Label participationValidationLabel;

    private final ObservableList<Entrainement> master = FXCollections.observableArrayList();
    private final ObservableList<Entrainement> filtered = FXCollections.observableArrayList();
    private final ObservableList<CoachOption> coachOptions = FXCollections.observableArrayList();

    private EntrainementService entrainementService;
    private EvaluationService evaluationService;
    private ParticipationService participationService;
    private UserService userService;
    private Entrainement selected;

    private Evaluation selectedEvaluation;
    private Participation selectedParticipation;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        sortChoiceBox.setItems(FXCollections.observableArrayList("Date", "Type", "Lieu"));
        sortChoiceBox.setValue("Date");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortChoiceBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getDateEntrainement())));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getType(), "-")));
        lieuColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getLieu(), "-")));
        horaireColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTimeRange(cell.getValue())));
        coachColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveCoachLabel(cell.getValue().getEntraineurId())));
        tableView.setItems(filtered);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selected = newValue;
            if (newValue != null) {
                populateForm(newValue);
                formHintLabel.setText("Modification de la session #" + newValue.getId());
            } else {
                formHintLabel.setText("Selectionnez une ligne ou saisissez une nouvelle session.");
            }
            clearValidation();
        });

        evaluationSortBox.setItems(FXCollections.observableArrayList("Moyenne", "Note physique", "Note technique", "Note tactique"));
        evaluationSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyEvaluationFilters());
        evaluationSortBox.valueProperty().addListener((obs, oldValue, newValue) -> applyEvaluationFilters());
        evaluationEntrColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveTrainingLabel(cell.getValue().getEntrainementId())));
        evaluationPlayerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolvePlayerLabel(cell.getValue().getJoueurId())));
        evaluationScoreColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(buildScoreLabel(cell.getValue())));
        evaluationAvgColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatAverage(cell.getValue())));
        evaluationTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedEvaluation = newValue;
            if (newValue != null) {
                populateEvaluationForm(newValue);
                evaluationHintLabel.setText("Modification de l'evaluation #" + newValue.getId());
            } else {
                evaluationHintLabel.setText("Selectionnez une evaluation ou creez-en une.");
            }
            clearEvaluationValidation();
        });

        participationSortBox.setItems(FXCollections.observableArrayList("Presence", "Joueur", "Entrainement"));
        participationPresenceFormField.setItems(FXCollections.observableArrayList("Present", "Absent"));
        participationSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyParticipationFilters());
        participationSortBox.valueProperty().addListener((obs, oldValue, newValue) -> applyParticipationFilters());
        participationEntrColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveTrainingLabel(cell.getValue().getEntrainementId())));
        participationPlayerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolvePlayerLabel(cell.getValue().getJoueurId())));
        participationPresenceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getPresence(), "-")));
        participationJustifColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getJustificationAbsence(), "-")));
        participationTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedParticipation = newValue;
            if (newValue != null) {
                populateParticipationForm(newValue);
                participationHintLabel.setText("Modification de la participation #" + newValue.getId());
            } else {
                participationHintLabel.setText("Selectionnez une participation ou creez-en une.");
            }
            clearParticipationValidation();
        });

        try {
            entrainementService = new EntrainementService();
            evaluationService = new EvaluationService();
            participationService = new ParticipationService();
            userService = new UserService();
            loadCoaches();
            loadTrainingOptions();
            loadPlayerOptions();
            refreshData();
            refreshEvaluations();
            refreshParticipations();
        } catch (SQLException e) {
            showError("Chargement", "Impossible de charger les entrainements.\n" + e.getMessage());
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
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void handleAdd() {
        clearValidation();
        Entrainement entrainement = buildFromForm(false);
        if (entrainement == null) {
            return;
        }
        try {
            entrainementService.add(entrainement);
            refreshData();
            clearForm();
        } catch (SQLException e) {
            showError("Ajout", "Erreur lors de l'ajout.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez un entrainement.");
            return;
        }
        Entrainement entrainement = buildFromForm(true);
        if (entrainement == null) {
            return;
        }
        entrainement.setId(selected.getId());
        try {
            entrainementService.update(entrainement);
            refreshData();
            clearForm();
        } catch (SQLException e) {
            showError("Modification", "Erreur lors de la modification.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez un entrainement.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer cet entrainement ?");
        alert.setContentText("Cette action est definitive.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            entrainementService.delete(selected.getId());
            refreshData();
            clearForm();
        } catch (SQLException e) {
            showError("Suppression", "Erreur lors de la suppression.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleRefreshEvaluations() {
        refreshEvaluations();
    }

    @FXML
    private void handleAddEvaluation() {
        clearEvaluationValidation();
        Evaluation evaluation = buildEvaluationFromForm(false);
        if (evaluation == null) {
            return;
        }
        try {
            evaluationService.add(evaluation);
            refreshEvaluations();
            clearEvaluationForm();
        } catch (SQLException e) {
            showError("Evaluation", "Erreur lors de l'ajout.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEvaluation() {
        clearEvaluationValidation();
        if (selectedEvaluation == null) {
            showEvaluationValidation("Selectionnez une evaluation.");
            return;
        }
        Evaluation evaluation = buildEvaluationFromForm(true);
        if (evaluation == null) {
            return;
        }
        evaluation.setId(selectedEvaluation.getId());
        try {
            evaluationService.update(evaluation);
            refreshEvaluations();
            clearEvaluationForm();
        } catch (SQLException e) {
            showError("Evaluation", "Erreur lors de la modification.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteEvaluation() {
        clearEvaluationValidation();
        if (selectedEvaluation == null) {
            showEvaluationValidation("Selectionnez une evaluation.");
            return;
        }
        if (!confirmDelete("Supprimer cette evaluation ?")) {
            return;
        }
        try {
            evaluationService.delete(selectedEvaluation.getId());
            refreshEvaluations();
            clearEvaluationForm();
        } catch (SQLException e) {
            showError("Evaluation", "Erreur lors de la suppression.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearEvaluation() {
        clearEvaluationForm();
    }

    @FXML
    private void handleRefreshParticipations() {
        refreshParticipations();
    }

    @FXML
    private void handleAddParticipation() {
        clearParticipationValidation();
        Participation participation = buildParticipationFromForm(false);
        if (participation == null) {
            return;
        }
        try {
            participationService.add(participation);
            refreshParticipations();
            clearParticipationForm();
        } catch (SQLException e) {
            showError("Participation", "Erreur lors de l'ajout.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateParticipation() {
        clearParticipationValidation();
        if (selectedParticipation == null) {
            showParticipationValidation("Selectionnez une participation.");
            return;
        }
        Participation participation = buildParticipationFromForm(true);
        if (participation == null) {
            return;
        }
        participation.setId(selectedParticipation.getId());
        try {
            participationService.update(participation);
            refreshParticipations();
            clearParticipationForm();
        } catch (SQLException e) {
            showError("Participation", "Erreur lors de la modification.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteParticipation() {
        clearParticipationValidation();
        if (selectedParticipation == null) {
            showParticipationValidation("Selectionnez une participation.");
            return;
        }
        if (!confirmDelete("Supprimer cette participation ?")) {
            return;
        }
        try {
            participationService.delete(selectedParticipation.getId());
            refreshParticipations();
            clearParticipationForm();
        } catch (SQLException e) {
            showError("Participation", "Erreur lors de la suppression.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearParticipation() {
        clearParticipationForm();
    }

    private void refreshData() {
        try {
            master.setAll(entrainementService.getAll());
            loadTrainingOptions();
            applyFilters();
        } catch (SQLException e) {
            showError("Chargement", "Impossible de charger les entrainements.\n" + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = normalize(searchField.getText());
        List<Entrainement> items = new ArrayList<>(master);
        if (!keyword.isEmpty()) {
            items = items.stream()
                    .filter(e -> matchesKeyword(e, keyword))
                    .collect(Collectors.toList());
        }

        Comparator<Entrainement> comparator = switch (sortChoiceBox.getValue() == null ? "" : sortChoiceBox.getValue()) {
            case "Type" -> Comparator.comparing(Entrainement::getType, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "Lieu" -> Comparator.comparing(Entrainement::getLieu, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(Entrainement::getDateEntrainement, Comparator.nullsLast(LocalDate::compareTo));
        };
        items.sort(comparator);
        filtered.setAll(items);
    }

    private void refreshEvaluations() {
        try {
            evaluationMaster.setAll(evaluationService.getAll());
            applyEvaluationFilters();
        } catch (SQLException e) {
            showError("Evaluation", "Impossible de charger les evaluations.\n" + e.getMessage());
        }
    }

    private void applyEvaluationFilters() {
        String keyword = normalize(evaluationSearchField.getText());
        List<Evaluation> items = new ArrayList<>(evaluationMaster);
        if (!keyword.isEmpty()) {
            items = items.stream()
                    .filter(e -> contains(e.getCommentaire(), keyword))
                    .collect(Collectors.toList());
        }
        List<Evaluation> sorted = switch (evaluationSortBox.getValue() == null ? "" : evaluationSortBox.getValue()) {
            case "Note physique" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNotePhysique).reversed())
                    .collect(Collectors.toList());
            case "Note technique" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNoteTechnique).reversed())
                    .collect(Collectors.toList());
            case "Note tactique" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNoteTactique).reversed())
                    .collect(Collectors.toList());
            default -> items.stream()
                    .sorted(Comparator.comparingDouble(e -> -average(e)))
                    .collect(Collectors.toList());
        };
        evaluationFiltered.setAll(sorted);
        evaluationTableView.setItems(evaluationFiltered);
    }

    private void refreshParticipations() {
        try {
            participationMaster.setAll(participationService.getAll());
            applyParticipationFilters();
        } catch (SQLException e) {
            showError("Participation", "Impossible de charger les participations.\n" + e.getMessage());
        }
    }

    private void applyParticipationFilters() {
        String keyword = normalize(participationSearchField.getText());
        List<Participation> items = new ArrayList<>(participationMaster);
        if (!keyword.isEmpty()) {
            items = items.stream()
                    .filter(p -> contains(p.getPresence(), keyword) || contains(p.getJustificationAbsence(), keyword))
                    .collect(Collectors.toList());
        }
        List<Participation> sorted = switch (participationSortBox.getValue() == null ? "" : participationSortBox.getValue()) {
            case "Joueur" -> items.stream()
                    .sorted(Comparator.comparing(p -> p.getJoueurId() == null ? 0 : p.getJoueurId()))
                    .collect(Collectors.toList());
            case "Entrainement" -> items.stream()
                    .sorted(Comparator.comparing(p -> p.getEntrainementId() == null ? 0 : p.getEntrainementId()))
                    .collect(Collectors.toList());
            default -> items.stream()
                    .sorted(Comparator.comparing(p -> emptyIfNull(p.getPresence(), "")))
                    .collect(Collectors.toList());
        };
        participationFiltered.setAll(sorted);
        participationTableView.setItems(participationFiltered);
    }

    private boolean matchesKeyword(Entrainement entrainement, String keyword) {
        return contains(entrainement.getType(), keyword)
                || contains(entrainement.getLieu(), keyword)
                || contains(entrainement.getObjectif(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void populateForm(Entrainement entrainement) {
        dateField.setValue(entrainement.getDateEntrainement());
        startField.setText(formatTime(entrainement.getHeureDebut()));
        endField.setText(formatTime(entrainement.getHeureFin()));
        typeField.setText(emptyIfNull(entrainement.getType(), ""));
        objectifField.setText(emptyIfNull(entrainement.getObjectif(), ""));
        lieuField.setText(emptyIfNull(entrainement.getLieu(), ""));
        selectCoachById(entrainement.getEntraineurId());
    }

    private void populateEvaluationForm(Evaluation evaluation) {
        selectTrainingById(evaluationTrainingField, evaluation.getEntrainementId());
        selectUserById(evaluationPlayerField, evaluation.getJoueurId());
        evaluationPhysField.setText(String.valueOf(evaluation.getNotePhysique()));
        evaluationTechField.setText(String.valueOf(evaluation.getNoteTechnique()));
        evaluationTactField.setText(String.valueOf(evaluation.getNoteTactique()));
        evaluationCommentField.setText(emptyIfNull(evaluation.getCommentaire(), ""));
    }

    private void populateParticipationForm(Participation participation) {
        selectTrainingById(participationTrainingField, participation.getEntrainementId());
        selectUserById(participationPlayerField, participation.getJoueurId());
        participationPresenceFormField.setValue(participation.getPresence());
        participationJustifField.setText(emptyIfNull(participation.getJustificationAbsence(), ""));
    }

    private void clearForm() {
        selected = null;
        dateField.setValue(null);
        startField.clear();
        endField.clear();
        typeField.clear();
        objectifField.clear();
        lieuField.clear();
        coachField.getSelectionModel().clearSelection();
        formHintLabel.setText("Selectionnez une ligne ou saisissez une nouvelle session.");
        clearValidation();
    }

    private void clearEvaluationForm() {
        selectedEvaluation = null;
        evaluationTrainingField.getSelectionModel().clearSelection();
        evaluationPlayerField.getSelectionModel().clearSelection();
        evaluationPhysField.clear();
        evaluationTechField.clear();
        evaluationTactField.clear();
        evaluationCommentField.clear();
        evaluationHintLabel.setText("Selectionnez une evaluation ou creez-en une.");
        clearEvaluationValidation();
    }

    private void clearParticipationForm() {
        selectedParticipation = null;
        participationTrainingField.getSelectionModel().clearSelection();
        participationPlayerField.getSelectionModel().clearSelection();
        participationPresenceFormField.getSelectionModel().clearSelection();
        participationJustifField.clear();
        participationHintLabel.setText("Selectionnez une participation ou creez-en une.");
        clearParticipationValidation();
    }

    private Entrainement buildFromForm(boolean updateMode) {
        LocalDate date = dateField.getValue();
        if (date == null) {
            markInvalid(dateField);
            showValidation("La date est obligatoire.");
            return null;
        }
        if (date.isBefore(LocalDate.now())) {
            markInvalid(dateField);
            showValidation("La date doit etre aujourd'hui ou dans le futur.");
            return null;
        }

        LocalTime start = parseTime(startField, "Heure debut");
        if (start == null) {
            return null;
        }
        LocalTime end = parseTime(endField, "Heure fin");
        if (end == null) {
            return null;
        }
        if (!end.isAfter(start)) {
            markInvalid(endField);
            showValidation("L'heure de fin doit etre apres l'heure de debut.");
            return null;
        }

        String type = requiredText(typeField, "Type");
        if (type == null) {
            return null;
        }
        String objectif = requiredText(objectifField, "Objectif");
        if (objectif == null) {
            return null;
        }
        String lieu = requiredText(lieuField, "Lieu");
        if (lieu == null) {
            return null;
        }

        CoachOption coach = coachField.getValue();
        if (coach == null) {
            markInvalid(coachField);
            showValidation("Le coach est obligatoire.");
            return null;
        }

        Entrainement entrainement = new Entrainement(date, start, end, type, objectif, lieu, coach.id());
        if (updateMode && selected == null) {
            showValidation("Selectionnez un entrainement a modifier.");
            return null;
        }
        return entrainement;
    }

    private Evaluation buildEvaluationFromForm(boolean updateMode) {
        TrainingOption training = evaluationTrainingField.getValue();
        UserOption player = evaluationPlayerField.getValue();
        if (training == null) {
            markInvalid(evaluationTrainingField);
            showEvaluationValidation("Entrainement obligatoire.");
            return null;
        }
        if (player == null) {
            markInvalid(evaluationPlayerField);
            showEvaluationValidation("Joueur obligatoire.");
            return null;
        }
        Double phys = parseDouble(evaluationPhysField, "Note physique");
        Double tech = parseDouble(evaluationTechField, "Note technique");
        Double tact = parseDouble(evaluationTactField, "Note tactique");
        if (phys == null || tech == null || tact == null) {
            return null;
        }
        String comment = evaluationCommentField.getText();
        if (comment == null || comment.isBlank()) {
            markInvalid(evaluationCommentField);
            showEvaluationValidation("Commentaire obligatoire.");
            return null;
        }
        Evaluation evaluation = new Evaluation(phys, tech, tact, comment.trim(), training.id(), player.id());
        if (updateMode && selectedEvaluation == null) {
            showEvaluationValidation("Selectionnez une evaluation a modifier.");
            return null;
        }
        return evaluation;
    }

    private Participation buildParticipationFromForm(boolean updateMode) {
        TrainingOption training = participationTrainingField.getValue();
        UserOption player = participationPlayerField.getValue();
        String presence = participationPresenceFormField.getValue();
        if (training == null) {
            markInvalid(participationTrainingField);
            showParticipationValidation("Entrainement obligatoire.");
            return null;
        }
        if (player == null) {
            markInvalid(participationPlayerField);
            showParticipationValidation("Joueur obligatoire.");
            return null;
        }
        if (presence == null || presence.isBlank()) {
            markInvalid(participationPresenceFormField);
            showParticipationValidation("Presence obligatoire.");
            return null;
        }
        Participation participation = new Participation(presence, emptyIfNull(participationJustifField.getText(), null), training.id(), player.id());
        if (updateMode && selectedParticipation == null) {
            showParticipationValidation("Selectionnez une participation a modifier.");
            return null;
        }
        return participation;
    }

    private LocalTime parseTime(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            markInvalid(field);
            showValidation(label + " est obligatoire.");
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            markInvalid(field);
            showValidation(label + " doit etre au format HH:mm.");
            return null;
        }
    }

    private String requiredText(TextField field, String label) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            markInvalid(field);
            showValidation(label + " est obligatoire.");
            return null;
        }
        return value.trim();
    }

    private void loadCoaches() throws SQLException {
        if (userService == null) {
            return;
        }
        List<User> users = userService.getAll();
        List<CoachOption> options = users.stream()
                .filter(this::isCoach)
                .map(CoachOption::fromUser)
                .sorted(Comparator.comparing(CoachOption::fullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        if (options.isEmpty()) {
            options = users.stream()
                    .map(CoachOption::fromUser)
                    .sorted(Comparator.comparing(CoachOption::fullName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        coachOptions.setAll(options);
        coachField.setItems(coachOptions);
    }

    private void loadPlayerOptions() throws SQLException {
        List<User> users = userService.getAll();
        List<UserOption> options = users.stream()
                .filter(this::isPlayer)
                .map(UserOption::fromUser)
                .sorted(Comparator.comparing(UserOption::fullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        if (options.isEmpty()) {
            options = users.stream()
                    .map(UserOption::fromUser)
                    .sorted(Comparator.comparing(UserOption::fullName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        playerOptions.setAll(options);
        evaluationPlayerField.setItems(playerOptions);
        participationPlayerField.setItems(playerOptions);
    }

    private void loadTrainingOptions() throws SQLException {
        List<TrainingOption> options = entrainementService.getAll().stream()
                .map(TrainingOption::fromTraining)
                .sorted(Comparator.comparing(TrainingOption::label, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        trainingOptions.setAll(options);
        evaluationTrainingField.setItems(trainingOptions);
        participationTrainingField.setItems(trainingOptions);
    }

    private boolean isCoach(User user) {
        String roles = user == null ? null : user.getRoles();
        if (roles == null) {
            return false;
        }
        String value = roles.toLowerCase();
        return value.contains("coach") || value.contains("entraineur") || value.contains("entraîneur");
    }

    private boolean isPlayer(User user) {
        String roles = user == null ? null : user.getRoles();
        if (roles == null) {
            return false;
        }
        String value = roles.toLowerCase();
        return value.contains("player") || value.contains("joueur") || value.contains("athlete");
    }

    private void selectCoachById(Integer coachId) {
        if (coachId == null) {
            coachField.getSelectionModel().clearSelection();
            return;
        }
        coachOptions.stream()
                .filter(option -> option.id().equals(coachId))
                .findFirst()
                .ifPresentOrElse(coachField::setValue, () -> coachField.getSelectionModel().clearSelection());
    }

    private void selectTrainingById(ComboBox<TrainingOption> comboBox, Integer trainingId) {
        if (trainingId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        trainingOptions.stream()
                .filter(option -> option.id().equals(trainingId))
                .findFirst()
                .ifPresentOrElse(comboBox::setValue, () -> comboBox.getSelectionModel().clearSelection());
    }

    private void selectUserById(ComboBox<UserOption> comboBox, Integer userId) {
        if (userId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        playerOptions.stream()
                .filter(option -> option.id().equals(userId))
                .findFirst()
                .ifPresentOrElse(comboBox::setValue, () -> comboBox.getSelectionModel().clearSelection());
    }

    private String resolveCoachLabel(Integer coachId) {
        if (coachId == null) {
            return "-";
        }
        return coachOptions.stream()
                .filter(option -> option.id().equals(coachId))
                .findFirst()
                .map(CoachOption::fullName)
                .orElse("Coach #" + coachId);
    }

    private String resolveTrainingLabel(Integer trainingId) {
        if (trainingId == null) {
            return "-";
        }
        return trainingOptions.stream()
                .filter(option -> option.id().equals(trainingId))
                .findFirst()
                .map(TrainingOption::label)
                .orElse("#" + trainingId);
    }

    private String resolvePlayerLabel(Integer playerId) {
        if (playerId == null) {
            return "-";
        }
        return playerOptions.stream()
                .filter(option -> option.id().equals(playerId))
                .findFirst()
                .map(UserOption::fullName)
                .orElse("Joueur #" + playerId);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_LABEL.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "" : TIME_LABEL.format(time);
    }

    private String formatTimeRange(Entrainement entrainement) {
        String start = formatTime(entrainement.getHeureDebut());
        String end = formatTime(entrainement.getHeureFin());
        if (start.isEmpty() && end.isEmpty()) {
            return "-";
        }
        return start + " - " + end;
    }

    private String emptyIfNull(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildScoreLabel(Evaluation evaluation) {
        return "P " + evaluation.getNotePhysique()
                + " | T " + evaluation.getNoteTechnique()
                + " | Tac " + evaluation.getNoteTactique();
    }

    private double average(Evaluation evaluation) {
        return (evaluation.getNotePhysique() + evaluation.getNoteTechnique() + evaluation.getNoteTactique()) / 3.0;
    }

    private String formatAverage(Evaluation evaluation) {
        return String.format("%.2f", average(evaluation));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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
        clearInvalid(dateField, startField, endField, typeField, objectifField, lieuField, coachField);
    }

    private void showEvaluationValidation(String message) {
        evaluationValidationLabel.setText(message);
        evaluationValidationLabel.setManaged(true);
        evaluationValidationLabel.setVisible(true);
    }

    private void clearEvaluationValidation() {
        evaluationValidationLabel.setText("");
        evaluationValidationLabel.setManaged(false);
        evaluationValidationLabel.setVisible(false);
        clearInvalid(evaluationTrainingField, evaluationPlayerField, evaluationPhysField, evaluationTechField, evaluationTactField, evaluationCommentField);
    }

    private void showParticipationValidation(String message) {
        participationValidationLabel.setText(message);
        participationValidationLabel.setManaged(true);
        participationValidationLabel.setVisible(true);
    }

    private void clearParticipationValidation() {
        participationValidationLabel.setText("");
        participationValidationLabel.setManaged(false);
        participationValidationLabel.setVisible(false);
        clearInvalid(participationTrainingField, participationPlayerField, participationPresenceFormField, participationJustifField);
    }

    private void markInvalid(Node node) {
        if (node == null) {
            return;
        }
        if (!node.getStyleClass().contains("invalid-field")) {
            node.getStyleClass().add("invalid-field");
        }
    }

    private void clearInvalid(Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                node.getStyleClass().remove("invalid-field");
            }
        }
    }

    private Double parseDouble(TextField field, String label) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            markInvalid(field);
            showEvaluationValidation(label + " obligatoire.");
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            markInvalid(field);
            showEvaluationValidation(label + " invalide.");
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirmDelete(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private record UserOption(Integer id, String firstName, String lastName) {
        static UserOption fromUser(User user) {
            return new UserOption(
                    user.getId(),
                    user.getPrenom() == null ? "" : user.getPrenom().trim(),
                    user.getNom() == null ? "" : user.getNom().trim()
            );
        }

        String fullName() {
            return (firstName + " lastName").replace("lastName", lastName == null ? "" : lastName).trim();
        }

        @Override
        public String toString() {
            return fullName();
        }
    }

    private record TrainingOption(Integer id, String label) {
        static TrainingOption fromTraining(Entrainement entrainement) {
            String label = entrainement.getType() + " - " + formatDateStatic(entrainement.getDateEntrainement());
            return new TrainingOption(entrainement.getId(), label);
        }
    }

    private static String formatDateStatic(LocalDate date) {
        return date == null ? "-" : DATE_LABEL.format(date);
    }

    private final ObservableList<Evaluation> evaluationMaster = FXCollections.observableArrayList();
    private final ObservableList<Evaluation> evaluationFiltered = FXCollections.observableArrayList();
    private final ObservableList<Participation> participationMaster = FXCollections.observableArrayList();
    private final ObservableList<Participation> participationFiltered = FXCollections.observableArrayList();
    private final ObservableList<UserOption> playerOptions = FXCollections.observableArrayList();
    private final ObservableList<TrainingOption> trainingOptions = FXCollections.observableArrayList();

    private record CoachOption(Integer id, String firstName, String lastName) {
        static CoachOption fromUser(User user) {
            return new CoachOption(
                    user.getId(),
                    user.getPrenom() == null ? "" : user.getPrenom().trim(),
                    user.getNom() == null ? "" : user.getNom().trim()
            );
        }

        String fullName() {
            return (firstName + " " + lastName).trim();
        }

        @Override
        public String toString() {
            return fullName();
        }
    }
}
