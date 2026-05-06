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
<<<<<<< HEAD
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.util.StringConverter;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EvaluationNotificationService;
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
    private EvaluationNotificationService evaluationNotificationService;
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

<<<<<<< HEAD
        // Make entrainement table editable
        tableView.setEditable(true);
        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getDateEntrainement())));
        
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getType(), "-")));
        typeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        typeColumn.setOnEditCommit(event -> {
            Entrainement entrainement = event.getRowValue();
            entrainement.setType(event.getNewValue());
            updateEntrainementInDatabase(entrainement);
        });
        
        lieuColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getLieu(), "-")));
        lieuColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        lieuColumn.setOnEditCommit(event -> {
            Entrainement entrainement = event.getRowValue();
            entrainement.setLieu(event.getNewValue());
            updateEntrainementInDatabase(entrainement);
        });
        
=======
        // Make entrainement table editable with double-click
        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getDateEntrainement())));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getType(), "-")));
        lieuColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getLieu(), "-")));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        horaireColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTimeRange(cell.getValue())));
        coachColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveCoachLabel(cell.getValue().getEntraineurId())));
        tableView.setItems(filtered);
        
<<<<<<< HEAD
        // Double-click to edit entrainement in dialog for complex fields
=======
        // Double-click to edit entrainement
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Entrainement selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openEntrainementEditDialog(selected);
                }
            }
        });
        
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selected = newValue;
            if (newValue != null) {
                populateForm(newValue);
<<<<<<< HEAD
                formHintLabel.setText("Click on Type or Location to edit directly, or double-click for full edit");
            } else {
                formHintLabel.setText("Click on Type or Location to edit directly, or double-click for full edit");
=======
                formHintLabel.setText("Double-cliquez sur une ligne pour modifier");
            } else {
                formHintLabel.setText("Double-cliquez sur une ligne pour modifier");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
            clearValidation();
        });

<<<<<<< HEAD
        // Make evaluation table editable
        evaluationTableView.setEditable(true);
        evaluationSortBox.setItems(FXCollections.observableArrayList("Average", "Physical Score", "Technical Score", "Tactical Score"));
=======
        evaluationSortBox.setItems(FXCollections.observableArrayList("Moyenne", "Note physique", "Note technique", "Note tactique"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        evaluationSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyEvaluationFilters());
        evaluationSortBox.valueProperty().addListener((obs, oldValue, newValue) -> applyEvaluationFilters());
        evaluationEntrColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveTrainingLabel(cell.getValue().getEntrainementId())));
        evaluationPlayerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolvePlayerLabel(cell.getValue().getJoueurId())));
<<<<<<< HEAD
        
        // Make score columns editable
        evaluationScoreColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(buildScoreLabel(cell.getValue())));
        evaluationAvgColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatAverage(cell.getValue())));
        
        // Add editable comment column if needed - for now keep double-click for full edit
        
        // Double-click to edit evaluation with all fields
=======
        evaluationScoreColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(buildScoreLabel(cell.getValue())));
        evaluationAvgColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatAverage(cell.getValue())));
        
        // Double-click to edit evaluation
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        evaluationTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Evaluation selected = evaluationTableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openEvaluationEditDialog(selected);
                }
            }
        });
        
        evaluationTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedEvaluation = newValue;
            if (newValue != null) {
                populateEvaluationForm(newValue);
<<<<<<< HEAD
                evaluationHintLabel.setText("Double-click to edit evaluation details");
            } else {
                evaluationHintLabel.setText("Double-click to edit evaluation details");
=======
                evaluationHintLabel.setText("Double-cliquez sur une ligne pour modifier");
            } else {
                evaluationHintLabel.setText("Double-cliquez sur une ligne pour modifier");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
            clearEvaluationValidation();
        });

<<<<<<< HEAD
        // Make participation table editable
        participationTableView.setEditable(true);
        participationSortBox.setItems(FXCollections.observableArrayList("Presence", "Player", "Training"));
=======
        participationSortBox.setItems(FXCollections.observableArrayList("Presence", "Joueur", "Entrainement"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        participationPresenceFormField.setItems(FXCollections.observableArrayList("Present", "Absent"));
        participationSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyParticipationFilters());
        participationSortBox.valueProperty().addListener((obs, oldValue, newValue) -> applyParticipationFilters());
        participationEntrColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolveTrainingLabel(cell.getValue().getEntrainementId())));
        participationPlayerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(resolvePlayerLabel(cell.getValue().getJoueurId())));
<<<<<<< HEAD
        
        // Make presence editable with dropdown
        participationPresenceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getPresence(), "-")));
        participationPresenceColumn.setCellFactory(ComboBoxTableCell.forTableColumn("Present", "Absent"));
        participationPresenceColumn.setOnEditCommit(event -> {
            Participation participation = event.getRowValue();
            participation.setPresence(event.getNewValue());
            updateParticipationInDatabase(participation);
        });
        
        // Make justification editable
        participationJustifColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getJustificationAbsence(), "-")));
        participationJustifColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        participationJustifColumn.setOnEditCommit(event -> {
            Participation participation = event.getRowValue();
            participation.setJustificationAbsence(event.getNewValue());
            updateParticipationInDatabase(participation);
        });
=======
        participationPresenceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getPresence(), "-")));
        participationJustifColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(emptyIfNull(cell.getValue().getJustificationAbsence(), "-")));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        
        // Double-click to edit participation or evaluate if present
        participationTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Participation selected = participationTableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // If present, open evaluation dialog; otherwise edit participation
                    if ("Present".equalsIgnoreCase(selected.getPresence())) {
                        openEvaluationDialog(selected);
                    } else {
                        openParticipationEditDialog(selected);
                    }
                }
            }
        });
        
        participationTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedParticipation = newValue;
            if (newValue != null) {
                populateParticipationForm(newValue);
<<<<<<< HEAD
                participationHintLabel.setText("Click Presence or Justification to edit directly");
            } else {
                participationHintLabel.setText("Click Presence or Justification to edit directly");
=======
                participationHintLabel.setText("Double-cliquez sur une ligne pour modifier");
            } else {
                participationHintLabel.setText("Double-cliquez sur une ligne pour modifier");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
            clearParticipationValidation();
        });

        try {
            entrainementService = new EntrainementService();
            evaluationService = new EvaluationService();
            evaluationNotificationService = new EvaluationNotificationService();
            participationService = new ParticipationService();
            userService = new UserService();
            loadCoaches();
            loadTrainingOptions();
            loadPlayerOptions();
            refreshData();
            refreshEvaluations();
            refreshParticipations();
        } catch (SQLException e) {
<<<<<<< HEAD
            showError("Loading Error", "Unable to load training sessions.\n" + e.getMessage());
=======
            showError("Chargement", "Impossible de charger les entrainements.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleOpenHome() {
<<<<<<< HEAD
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Home");
=======
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Add Error", "Failed to add training session.\n" + e.getMessage());
=======
            showError("Ajout", "Erreur lors de l'ajout.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleUpdate() {
        clearValidation();
        if (selected == null) {
<<<<<<< HEAD
            showValidation("Please select a training session.");
=======
            showValidation("Selectionnez un entrainement.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Update Error", "Failed to update training session.\n" + e.getMessage());
=======
            showError("Modification", "Erreur lors de la modification.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleDelete() {
        clearValidation();
        if (selected == null) {
<<<<<<< HEAD
            showValidation("Please select a training session.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Training");
        alert.setHeaderText("Delete this training session?");
        alert.setContentText("This action cannot be undone.");
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(150);
=======
            showValidation("Selectionnez un entrainement.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer cet entrainement ?");
        alert.setContentText("Cette action est definitive.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            entrainementService.delete(selected.getId());
            refreshData();
            clearForm();
        } catch (SQLException e) {
<<<<<<< HEAD
            showError("Delete Error", "Failed to delete training session.\n" + e.getMessage());
=======
            showError("Suppression", "Erreur lors de la suppression.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
            String emailStatus = notifyPlayerAboutEvaluation(evaluation, false);
            refreshEvaluations();
            clearEvaluationForm();
<<<<<<< HEAD
            showSuccess("Evaluation", "Evaluation added successfully!\n" + emailStatus);
        } catch (SQLException e) {
            showError("Evaluation", "Failed to add evaluation.\n" + e.getMessage());
=======
            showInfo("Evaluation", "Evaluation ajoutee avec succes!\n" + emailStatus);
        } catch (SQLException e) {
            showError("Evaluation", "Erreur lors de l'ajout.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
            String emailStatus = notifyPlayerAboutEvaluation(evaluation, true);
            refreshEvaluations();
            clearEvaluationForm();
<<<<<<< HEAD
            showSuccess("Evaluation", "Evaluation updated successfully!\n" + emailStatus);
        } catch (SQLException e) {
            showError("Evaluation", "Failed to update evaluation.\n" + e.getMessage());
=======
            showInfo("Evaluation", "Evaluation mise a jour avec succes!\n" + emailStatus);
        } catch (SQLException e) {
            showError("Evaluation", "Erreur lors de la modification.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleDeleteEvaluation() {
        clearEvaluationValidation();
        if (selectedEvaluation == null) {
            showEvaluationValidation("Selectionnez une evaluation.");
            return;
        }
<<<<<<< HEAD
        if (!confirmDelete("Delete this evaluation?")) {
=======
        if (!confirmDelete("Supprimer cette evaluation ?")) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }
        try {
            evaluationService.delete(selectedEvaluation.getId());
            refreshEvaluations();
            clearEvaluationForm();
        } catch (SQLException e) {
<<<<<<< HEAD
            showError("Evaluation", "Failed to delete evaluation.\n" + e.getMessage());
=======
            showError("Evaluation", "Erreur lors de la suppression.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleClearEvaluation() {
        clearEvaluationForm();
    }

    private String notifyPlayerAboutEvaluation(Evaluation evaluation, boolean updated) {
        if (evaluationNotificationService == null || userService == null || entrainementService == null) {
<<<<<<< HEAD
            return "Email notification unavailable.";
=======
            return "Notification email indisponible.";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
        try {
            User player = userService.getById(evaluation.getJoueurId());
            if (player == null || player.getEmail() == null || player.getEmail().isBlank()) {
<<<<<<< HEAD
                return "Evaluation saved, but no valid email address found for this player.";
=======
                return "Evaluation enregistree, mais aucune adresse e-mail valide n'a ete trouvee pour ce joueur.";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }

            Entrainement training = entrainementService.getById(evaluation.getEntrainementId());
            EvaluationNotificationService.DeliveryResult result =
                    evaluationNotificationService.sendEvaluationNotification(player, training, evaluation, updated);
            return result.message();
        } catch (SQLException e) {
<<<<<<< HEAD
            return "Evaluation saved, but email could not be prepared: " + e.getMessage();
=======
            return "Evaluation enregistree, mais l'e-mail n'a pas pu etre prepare: " + e.getMessage();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
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
<<<<<<< HEAD
            showError("Participation", "Failed to add participation.\n" + e.getMessage());
=======
            showError("Participation", "Erreur lors de l'ajout.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Participation", "Failed to update participation.\n" + e.getMessage());
=======
            showError("Participation", "Erreur lors de la modification.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    @FXML
    private void handleDeleteParticipation() {
        clearParticipationValidation();
        if (selectedParticipation == null) {
            showParticipationValidation("Selectionnez une participation.");
            return;
        }
<<<<<<< HEAD
        if (!confirmDelete("Delete this participation?")) {
=======
        if (!confirmDelete("Supprimer cette participation ?")) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }
        try {
            participationService.delete(selectedParticipation.getId());
            refreshParticipations();
            clearParticipationForm();
        } catch (SQLException e) {
<<<<<<< HEAD
            showError("Participation", "Failed to delete participation.\n" + e.getMessage());
=======
            showError("Participation", "Erreur lors de la suppression.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Loading Error", "Unable to load training sessions.\n" + e.getMessage());
=======
            showError("Chargement", "Impossible de charger les entrainements.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Evaluation", "Unable to load evaluations.\n" + e.getMessage());
=======
            showError("Evaluation", "Impossible de charger les evaluations.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            case "Physical Score" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNotePhysique).reversed())
                    .collect(Collectors.toList());
            case "Technical Score" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNoteTechnique).reversed())
                    .collect(Collectors.toList());
            case "Tactical Score" -> items.stream()
=======
            case "Note physique" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNotePhysique).reversed())
                    .collect(Collectors.toList());
            case "Note technique" -> items.stream()
                    .sorted(Comparator.comparingDouble(Evaluation::getNoteTechnique).reversed())
                    .collect(Collectors.toList());
            case "Note tactique" -> items.stream()
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showError("Participation", "Unable to load participations.\n" + e.getMessage());
=======
            showError("Participation", "Impossible de charger les participations.\n" + e.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            case "Player" -> items.stream()
                    .sorted(Comparator.comparing(p -> p.getJoueurId() == null ? 0 : p.getJoueurId()))
                    .collect(Collectors.toList());
            case "Training" -> items.stream()
=======
            case "Joueur" -> items.stream()
                    .sorted(Comparator.comparing(p -> p.getJoueurId() == null ? 0 : p.getJoueurId()))
                    .collect(Collectors.toList());
            case "Entrainement" -> items.stream()
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        formHintLabel.setText("Click on Type or Location to edit directly, or add new training session");
=======
        formHintLabel.setText("Selectionnez une ligne ou saisissez une nouvelle session.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        evaluationHintLabel.setText("Double-click to edit or create new evaluation");
=======
        evaluationHintLabel.setText("Selectionnez une evaluation ou creez-en une.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        clearEvaluationValidation();
    }

    private void clearParticipationForm() {
        selectedParticipation = null;
        participationTrainingField.getSelectionModel().clearSelection();
        participationPlayerField.getSelectionModel().clearSelection();
        participationPresenceFormField.getSelectionModel().clearSelection();
        participationJustifField.clear();
<<<<<<< HEAD
        participationHintLabel.setText("Click Presence or Justification to edit, or create new");
=======
        participationHintLabel.setText("Selectionnez une participation ou creez-en une.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        clearParticipationValidation();
    }

    private Entrainement buildFromForm(boolean updateMode) {
        LocalDate date = dateField.getValue();
        if (date == null) {
            markInvalid(dateField);
<<<<<<< HEAD
            showValidation("Date is required.");
=======
            showValidation("La date est obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        if (date.isBefore(LocalDate.now())) {
            markInvalid(dateField);
<<<<<<< HEAD
            showValidation("Date must be today or in the future.");
            return null;
        }

        LocalTime start = parseTime(startField, "Start time");
        if (start == null) {
            return null;
        }
        LocalTime end = parseTime(endField, "End time");
=======
            showValidation("La date doit etre aujourd'hui ou dans le futur.");
            return null;
        }

        LocalTime start = parseTime(startField, "Heure debut");
        if (start == null) {
            return null;
        }
        LocalTime end = parseTime(endField, "Heure fin");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (end == null) {
            return null;
        }
        if (!end.isAfter(start)) {
            markInvalid(endField);
<<<<<<< HEAD
            showValidation("End time must be after start time.");
=======
            showValidation("L'heure de fin doit etre apres l'heure de debut.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showValidation("Coach is required.");
=======
            showValidation("Le coach est obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }

        Entrainement entrainement = new Entrainement(date, start, end, type, objectif, lieu, coach.id());
        if (updateMode && selected == null) {
<<<<<<< HEAD
            showValidation("Please select a training session to update.");
=======
            showValidation("Selectionnez un entrainement a modifier.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        return entrainement;
    }

    private Evaluation buildEvaluationFromForm(boolean updateMode) {
        TrainingOption training = evaluationTrainingField.getValue();
        UserOption player = evaluationPlayerField.getValue();
        if (training == null) {
            markInvalid(evaluationTrainingField);
<<<<<<< HEAD
            showEvaluationValidation("Training session is required.");
=======
            showEvaluationValidation("Entrainement obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        if (player == null) {
            markInvalid(evaluationPlayerField);
<<<<<<< HEAD
            showEvaluationValidation("Player is required.");
            return null;
        }
        Double phys = parseDouble(evaluationPhysField, "Physical score");
        Double tech = parseDouble(evaluationTechField, "Technical score");
        Double tact = parseDouble(evaluationTactField, "Tactical score");
=======
            showEvaluationValidation("Joueur obligatoire.");
            return null;
        }
        Double phys = parseDouble(evaluationPhysField, "Note physique");
        Double tech = parseDouble(evaluationTechField, "Note technique");
        Double tact = parseDouble(evaluationTactField, "Note tactique");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (phys == null || tech == null || tact == null) {
            return null;
        }
        String comment = evaluationCommentField.getText();
        if (comment == null || comment.isBlank()) {
            markInvalid(evaluationCommentField);
<<<<<<< HEAD
            showEvaluationValidation("Comment is required.");
=======
            showEvaluationValidation("Commentaire obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        Evaluation evaluation = new Evaluation(phys, tech, tact, comment.trim(), training.id(), player.id());
        if (updateMode && selectedEvaluation == null) {
<<<<<<< HEAD
            showEvaluationValidation("Please select an evaluation to update.");
=======
            showEvaluationValidation("Selectionnez une evaluation a modifier.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showParticipationValidation("Training session is required.");
=======
            showParticipationValidation("Entrainement obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        if (player == null) {
            markInvalid(participationPlayerField);
<<<<<<< HEAD
            showParticipationValidation("Player is required.");
=======
            showParticipationValidation("Joueur obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        if (presence == null || presence.isBlank()) {
            markInvalid(participationPresenceFormField);
<<<<<<< HEAD
            showParticipationValidation("Presence status is required.");
=======
            showParticipationValidation("Presence obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        Participation participation = new Participation(presence, emptyIfNull(participationJustifField.getText(), null), training.id(), player.id());
        if (updateMode && selectedParticipation == null) {
<<<<<<< HEAD
            showParticipationValidation("Please select a participation to update.");
=======
            showParticipationValidation("Selectionnez une participation a modifier.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        return participation;
    }

    private LocalTime parseTime(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            markInvalid(field);
<<<<<<< HEAD
            showValidation(label + " is required.");
=======
            showValidation(label + " est obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
            showValidation(label + " is required.");
=======
            showValidation(label + " est obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        return value.contains("coach") || value.contains("entraineur") || value.contains("entraîneur") || value.contains("trainer");
=======
        return value.contains("coach") || value.contains("entraineur") || value.contains("entraîneur");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
                .orElseGet(() -> resolveUserDisplayName(coachId, "Coach"));
    }

    private String resolveTrainingLabel(Integer trainingId) {
        if (trainingId == null) {
            return "-";
        }
        return trainingOptions.stream()
                .filter(option -> option.id().equals(trainingId))
                .findFirst()
                .map(TrainingOption::label)
                .orElseGet(() -> resolveTrainingDisplayName(trainingId));
    }

    private String resolvePlayerLabel(Integer playerId) {
        if (playerId == null) {
            return "-";
        }
        return playerOptions.stream()
                .filter(option -> option.id().equals(playerId))
                .findFirst()
                .map(UserOption::fullName)
                .orElseGet(() -> resolveUserDisplayName(playerId, "Joueur"));
    }

    private String resolveUserDisplayName(Integer userId, String fallbackLabel) {
        if (userId == null || userService == null) {
            return fallbackLabel;
        }
        try {
            User user = userService.getById(userId);
            return user == null ? fallbackLabel : user.getDisplayName();
        } catch (SQLException e) {
            return fallbackLabel;
        }
    }

    private String resolveTrainingDisplayName(Integer trainingId) {
        if (trainingId == null || entrainementService == null) {
            return "Session";
        }
        try {
            Entrainement entrainement = entrainementService.getById(trainingId);
            if (entrainement == null) {
                return "Session";
            }
            return buildTrainingLabel(entrainement);
        } catch (SQLException e) {
            return "Session";
        }
    }

    private String buildTrainingLabel(Entrainement entrainement) {
        if (entrainement == null) {
            return "Session";
        }
        String type = emptyIfNull(entrainement.getType(), "Session");
        String date = formatDate(entrainement.getDateEntrainement());
        if ("-".equals(date)) {
            return type;
        }
        return type + " - " + date;
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
<<<<<<< HEAD
            showEvaluationValidation(label + " is required.");
=======
            showEvaluationValidation(label + " obligatoire.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            markInvalid(field);
<<<<<<< HEAD
            showEvaluationValidation(label + " is invalid.");
=======
            showEvaluationValidation(label + " invalide.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
<<<<<<< HEAD
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(150);
        alert.getDialogPane().getStyleClass().add("custom-alert");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
<<<<<<< HEAD
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(150);
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✓ Success");
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(150);
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.getDialogPane().getStyleClass().add("success-alert");
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(150);
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.showAndWait();
    }
    
    // Inline editing helper methods
    private void updateEntrainementInDatabase(Entrainement entrainement) {
        try {
            entrainementService.update(entrainement);
            showSuccess("Updated", "Training session updated successfully!");
            refreshData();
        } catch (SQLException e) {
            showError("Update Error", "Failed to update training session.\n" + e.getMessage());
            refreshData(); // Refresh to revert changes in UI
        }
    }
    
    private void updateEvaluationInDatabase(Evaluation evaluation) {
        try {
            evaluationService.update(evaluation);
            showSuccess("Updated", "Evaluation updated successfully!");
            refreshEvaluations();
        } catch (SQLException e) {
            showError("Update Error", "Failed to update evaluation.\n" + e.getMessage());
            refreshEvaluations(); // Refresh to revert changes in UI
        }
    }
    
    private void updateParticipationInDatabase(Participation participation) {
        try {
            participationService.update(participation);
            showSuccess("Updated", "Participation updated successfully!");
            refreshParticipations();
        } catch (SQLException e) {
            showError("Update Error", "Failed to update participation.\n" + e.getMessage());
            refreshParticipations(); // Refresh to revert changes in UI
        }
    }
=======
        alert.showAndWait();
    }
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    private void openEvaluationDialog(Participation participation) {
        if (participation == null || participation.getJoueurId() == null || participation.getEntrainementId() == null) {
            return;
        }

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
<<<<<<< HEAD
        dialog.setTitle("Evaluate Performance");
=======
        dialog.setTitle("Évaluer la performance");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        dialog.setResizable(false);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.getStyleClass().add("evaluation-dialog-root");
        root.setMaxWidth(550);

        // Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(12);
        header.getStyleClass().add("evaluation-dialog-header");
        header.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        
<<<<<<< HEAD
        Label titleLabel = new Label("Evaluate Performance");
=======
        Label titleLabel = new Label("Évaluer la performance");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        titleLabel.getStyleClass().add("evaluation-dialog-title");
        
        Label playerLabel = new Label("Joueur: " + resolvePlayerLabel(participation.getJoueurId()));
        playerLabel.getStyleClass().add("evaluation-dialog-player");
        
        Label sessionLabel = new Label("Session: " + resolveTrainingLabel(participation.getEntrainementId()));
        sessionLabel.getStyleClass().add("evaluation-dialog-session");
        
        header.getChildren().addAll(titleLabel, playerLabel, sessionLabel);

        // Content
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(20);
        content.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        content.getStyleClass().add("evaluation-dialog-content");

<<<<<<< HEAD
        Label instructionLabel = new Label("Rate the player's performance out of 20 points");
=======
        Label instructionLabel = new Label("Évaluez les performances du joueur sur 20 points");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        instructionLabel.getStyleClass().add("evaluation-dialog-instruction");
        instructionLabel.setWrapText(true);

        // Physical score
        javafx.scene.layout.VBox physBox = new javafx.scene.layout.VBox(8);
        Label physLabel = new Label("💪 Note Physique");
        physLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox physSliderBox = new javafx.scene.layout.HBox(15);
        physSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider physSlider = new javafx.scene.control.Slider(0, 20, 10);
        physSlider.setShowTickLabels(true);
        physSlider.setShowTickMarks(true);
        physSlider.setMajorTickUnit(5);
        physSlider.setMinorTickCount(4);
        physSlider.setBlockIncrement(1);
        physSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(physSlider, javafx.scene.layout.Priority.ALWAYS);
        Label physValueLabel = new Label("10.0");
        physValueLabel.getStyleClass().add("evaluation-value-label");
        physValueLabel.setMinWidth(50);
        physSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            physValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        physSliderBox.getChildren().addAll(physSlider, physValueLabel);
        physBox.getChildren().addAll(physLabel, physSliderBox);

        // Technical score
        javafx.scene.layout.VBox techBox = new javafx.scene.layout.VBox(8);
        Label techLabel = new Label("⚽ Note Technique");
        techLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox techSliderBox = new javafx.scene.layout.HBox(15);
        techSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider techSlider = new javafx.scene.control.Slider(0, 20, 10);
        techSlider.setShowTickLabels(true);
        techSlider.setShowTickMarks(true);
        techSlider.setMajorTickUnit(5);
        techSlider.setMinorTickCount(4);
        techSlider.setBlockIncrement(1);
        techSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(techSlider, javafx.scene.layout.Priority.ALWAYS);
        Label techValueLabel = new Label("10.0");
        techValueLabel.getStyleClass().add("evaluation-value-label");
        techValueLabel.setMinWidth(50);
        techSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            techValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        techSliderBox.getChildren().addAll(techSlider, techValueLabel);
        techBox.getChildren().addAll(techLabel, techSliderBox);

        // Tactical score
        javafx.scene.layout.VBox tactBox = new javafx.scene.layout.VBox(8);
        Label tactLabel = new Label("🎯 Note Tactique");
        tactLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox tactSliderBox = new javafx.scene.layout.HBox(15);
        tactSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider tactSlider = new javafx.scene.control.Slider(0, 20, 10);
        tactSlider.setShowTickLabels(true);
        tactSlider.setShowTickMarks(true);
        tactSlider.setMajorTickUnit(5);
        tactSlider.setMinorTickCount(4);
        tactSlider.setBlockIncrement(1);
        tactSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(tactSlider, javafx.scene.layout.Priority.ALWAYS);
        Label tactValueLabel = new Label("10.0");
        tactValueLabel.getStyleClass().add("evaluation-value-label");
        tactValueLabel.setMinWidth(50);
        tactSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            tactValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        tactSliderBox.getChildren().addAll(tactSlider, tactValueLabel);
        tactBox.getChildren().addAll(tactLabel, tactSliderBox);

        // Average display
        javafx.scene.layout.HBox avgBox = new javafx.scene.layout.HBox(10);
        avgBox.setAlignment(javafx.geometry.Pos.CENTER);
        avgBox.getStyleClass().add("evaluation-average-box");
        avgBox.setPadding(new javafx.geometry.Insets(15, 20, 15, 20));
        Label avgTitleLabel = new Label("Moyenne:");
        avgTitleLabel.getStyleClass().add("evaluation-average-title");
        Label avgValueLabel = new Label("10.0");
        avgValueLabel.getStyleClass().add("evaluation-average-value");
        avgBox.getChildren().addAll(avgTitleLabel, avgValueLabel);

        // Update average when sliders change
        javafx.beans.value.ChangeListener<Number> avgListener = (obs, oldVal, newVal) -> {
            double avg = (physSlider.getValue() + techSlider.getValue() + tactSlider.getValue()) / 3.0;
            avgValueLabel.setText(String.format("%.2f / 20", avg));
        };
        physSlider.valueProperty().addListener(avgListener);
        techSlider.valueProperty().addListener(avgListener);
        tactSlider.valueProperty().addListener(avgListener);

        // Comment
        javafx.scene.layout.VBox commentBox = new javafx.scene.layout.VBox(8);
        Label commentLabel = new Label("💬 Commentaire (optionnel)");
        commentLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.control.TextArea commentArea = new javafx.scene.control.TextArea();
        commentArea.setPromptText("Ajoutez vos observations sur la performance...");
        commentArea.getStyleClass().add("evaluation-comment-area");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        commentBox.getChildren().addAll(commentLabel, commentArea);

        content.getChildren().addAll(instructionLabel, physBox, techBox, tactBox, avgBox, commentBox);

        // Check if evaluation already exists
        try {
            List<Evaluation> existing = evaluationService.getByEntrainement(participation.getEntrainementId())
                .stream()
                .filter(e -> Objects.equals(e.getJoueurId(), participation.getJoueurId()))
                .collect(Collectors.toList());
            
            if (!existing.isEmpty()) {
                Evaluation eval = existing.get(0);
                physSlider.setValue(eval.getNotePhysique());
                techSlider.setValue(eval.getNoteTechnique());
                tactSlider.setValue(eval.getNoteTactique());
                if (eval.getCommentaire() != null) {
                    commentArea.setText(eval.getCommentaire());
                }
            }
        } catch (SQLException e) {
            // Ignore, use default values
        }

        // Footer
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(15);
        footer.setPadding(new javafx.geometry.Insets(20, 30, 30, 30));
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getStyleClass().add("evaluation-dialog-footer");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-save-button");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                // Check if evaluation exists
                List<Evaluation> existingList = evaluationService.getByEntrainement(participation.getEntrainementId())
                    .stream()
                    .filter(ev -> Objects.equals(ev.getJoueurId(), participation.getJoueurId()))
                    .collect(Collectors.toList());

                Evaluation evaluation;
                boolean isUpdate = false;
                
                if (!existingList.isEmpty()) {
                    evaluation = existingList.get(0);
                    isUpdate = true;
                } else {
                    evaluation = new Evaluation();
                    evaluation.setEntrainementId(participation.getEntrainementId());
                    evaluation.setJoueurId(participation.getJoueurId());
                }

                evaluation.setNotePhysique(physSlider.getValue());
                evaluation.setNoteTechnique(techSlider.getValue());
                evaluation.setNoteTactique(tactSlider.getValue());
                String comment = commentArea.getText().trim();
                evaluation.setCommentaire(comment.isEmpty() ? null : comment);

                if (isUpdate) {
                    evaluationService.update(evaluation);
                } else {
                    evaluationService.add(evaluation);
                }

                String emailStatus = notifyPlayerAboutEvaluation(evaluation, isUpdate);

                refreshEvaluations();
                dialog.close();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
<<<<<<< HEAD
                success.setTitle("Success");
                success.setHeaderText(null);
                success.setContentText("Evaluation saved successfully!\n" + emailStatus);
                success.showAndWait();
                
            } catch (SQLException ex) {
                showError("Evaluation", "Failed to save evaluation.\n" + ex.getMessage());
=======
                success.setTitle("Succès");
                success.setHeaderText(null);
                success.setContentText("Évaluation enregistrée avec succès!\n" + emailStatus);
                success.showAndWait();
                
            } catch (SQLException ex) {
                showError("Évaluation", "Erreur lors de l'enregistrement.\n" + ex.getMessage());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, content, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/tn/esprit/styles/entrainement-theme.css").toExternalForm());
<<<<<<< HEAD
        ThemeManager.registerScene(scene);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openEntrainementEditDialog(Entrainement entrainement) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifier l'entrainement");
        dialog.setResizable(false);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.getStyleClass().add("evaluation-dialog-root");
        root.setMaxWidth(600);

        // Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(8);
        header.getStyleClass().add("evaluation-dialog-header");
        header.setPadding(new javafx.geometry.Insets(25, 30, 25, 30));
        
        Label titleLabel = new Label("Modifier l'entrainement #" + entrainement.getId());
        titleLabel.getStyleClass().add("evaluation-dialog-title");
        header.getChildren().add(titleLabel);

        // Content
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(18);
        content.setPadding(new javafx.geometry.Insets(25, 30, 25, 30));
        content.getStyleClass().add("evaluation-dialog-content");

        // Date
        javafx.scene.layout.VBox dateBox = new javafx.scene.layout.VBox(6);
        Label dateLabel = new Label("📅 Date");
        dateLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(entrainement.getDateEntrainement());
        datePicker.getStyleClass().add("evaluation-comment-area");
        dateBox.getChildren().addAll(dateLabel, datePicker);

        // Time range
        javafx.scene.layout.HBox timeBox = new javafx.scene.layout.HBox(15);
        javafx.scene.layout.VBox startBox = new javafx.scene.layout.VBox(6);
<<<<<<< HEAD
        Label startLabel = new Label("🕐 Start time");
=======
        Label startLabel = new Label("🕐 Heure début");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        startLabel.getStyleClass().add("evaluation-dialog-label");
        TextField startField = new TextField(entrainement.getHeureDebut() != null ? entrainement.getHeureDebut().toString() : "");
        startField.setPromptText("HH:MM");
        startField.getStyleClass().add("evaluation-comment-area");
        startBox.getChildren().addAll(startLabel, startField);
        
        javafx.scene.layout.VBox endBox = new javafx.scene.layout.VBox(6);
<<<<<<< HEAD
        Label endLabel = new Label("🕐 End time");
=======
        Label endLabel = new Label("🕐 Heure fin");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        endLabel.getStyleClass().add("evaluation-dialog-label");
        TextField endField = new TextField(entrainement.getHeureFin() != null ? entrainement.getHeureFin().toString() : "");
        endField.setPromptText("HH:MM");
        endField.getStyleClass().add("evaluation-comment-area");
        endBox.getChildren().addAll(endLabel, endField);
        
        javafx.scene.layout.HBox.setHgrow(startBox, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(endBox, javafx.scene.layout.Priority.ALWAYS);
        timeBox.getChildren().addAll(startBox, endBox);

        // Location
        javafx.scene.layout.VBox locBox = new javafx.scene.layout.VBox(6);
        Label locLabel = new Label("📍 Lieu");
        locLabel.getStyleClass().add("evaluation-dialog-label");
        TextField locField = new TextField(entrainement.getLieu() != null ? entrainement.getLieu() : "");
        locField.setPromptText("Lieu de l'entrainement");
        locField.getStyleClass().add("evaluation-comment-area");
        locBox.getChildren().addAll(locLabel, locField);

        // Type
        javafx.scene.layout.VBox typeBox = new javafx.scene.layout.VBox(6);
        Label typeLabel = new Label("⚽ Type");
        typeLabel.getStyleClass().add("evaluation-dialog-label");
        TextField typeField = new TextField(entrainement.getType() != null ? entrainement.getType() : "");
        typeField.setPromptText("Type d'entrainement");
        typeField.getStyleClass().add("evaluation-comment-area");
        typeBox.getChildren().addAll(typeLabel, typeField);

        // Coach
        javafx.scene.layout.VBox coachBox = new javafx.scene.layout.VBox(6);
        Label coachLabel = new Label("👤 Entraineur");
        coachLabel.getStyleClass().add("evaluation-dialog-label");
        ComboBox<CoachOption> coachCombo = new ComboBox<>();
        coachCombo.setItems(coachField.getItems());
        coachCombo.getStyleClass().add("evaluation-comment-area");
        selectCoachById(entrainement.getEntraineurId());
        coachCombo.setValue(coachField.getValue());
        coachBox.getChildren().addAll(coachLabel, coachCombo);

        content.getChildren().addAll(dateBox, timeBox, locBox, typeBox, coachBox);

        // Footer
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(15);
        footer.setPadding(new javafx.geometry.Insets(20, 30, 30, 30));
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getStyleClass().add("evaluation-dialog-footer");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-save-button");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                if (datePicker.getValue() == null) {
<<<<<<< HEAD
                    showError("Validation", "Date is required");
                    return;
                }
                if (locField.getText().trim().isEmpty()) {
                    showError("Validation", "Location is required");
                    return;
                }
                if (coachCombo.getValue() == null) {
                    showError("Validation", "Coach is required");
=======
                    showError("Validation", "La date est obligatoire");
                    return;
                }
                if (locField.getText().trim().isEmpty()) {
                    showError("Validation", "Le lieu est obligatoire");
                    return;
                }
                if (coachCombo.getValue() == null) {
                    showError("Validation", "L'entraineur est obligatoire");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                    return;
                }

                LocalTime startTime = null;
                LocalTime endTime = null;
                try {
                    if (!startField.getText().trim().isEmpty()) {
                        startTime = LocalTime.parse(startField.getText().trim());
                    }
                    if (!endField.getText().trim().isEmpty()) {
                        endTime = LocalTime.parse(endField.getText().trim());
                    }
                } catch (Exception ex) {
                    showError("Validation", "Format d'heure invalide (utilisez HH:MM)");
                    return;
                }

                entrainement.setDateEntrainement(datePicker.getValue());
                entrainement.setHeureDebut(startTime);
                entrainement.setHeureFin(endTime);
                entrainement.setLieu(locField.getText().trim());
                entrainement.setType(typeField.getText().trim().isEmpty() ? null : typeField.getText().trim());
                entrainement.setEntraineurId(coachCombo.getValue().id());

                entrainementService.update(entrainement);
                refreshData();
                dialog.close();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText(null);
                success.setContentText("Entrainement modifié avec succès!");
                success.showAndWait();
                
            } catch (SQLException ex) {
                showError("Modification", "Erreur lors de la modification.\n" + ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, content, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/tn/esprit/styles/entrainement-theme.css").toExternalForm());
<<<<<<< HEAD
        ThemeManager.registerScene(scene);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openEvaluationEditDialog(Evaluation evaluation) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifier l'évaluation");
        dialog.setResizable(false);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.getStyleClass().add("evaluation-dialog-root");
        root.setMaxWidth(550);

        // Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(12);
        header.getStyleClass().add("evaluation-dialog-header");
        header.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        
        Label titleLabel = new Label("Modifier l'évaluation #" + evaluation.getId());
        titleLabel.getStyleClass().add("evaluation-dialog-title");
        
        Label playerLabel = new Label("Joueur: " + resolvePlayerLabel(evaluation.getJoueurId()));
        playerLabel.getStyleClass().add("evaluation-dialog-player");
        
        Label sessionLabel = new Label("Session: " + resolveTrainingLabel(evaluation.getEntrainementId()));
        sessionLabel.getStyleClass().add("evaluation-dialog-session");
        
        header.getChildren().addAll(titleLabel, playerLabel, sessionLabel);

        // Content
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(20);
        content.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        content.getStyleClass().add("evaluation-dialog-content");

        // Physical score
        javafx.scene.layout.VBox physBox = new javafx.scene.layout.VBox(8);
        Label physLabel = new Label("💪 Note Physique");
        physLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox physSliderBox = new javafx.scene.layout.HBox(15);
        physSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider physSlider = new javafx.scene.control.Slider(0, 20, evaluation.getNotePhysique());
        physSlider.setShowTickLabels(true);
        physSlider.setShowTickMarks(true);
        physSlider.setMajorTickUnit(5);
        physSlider.setMinorTickCount(4);
        physSlider.setBlockIncrement(1);
        physSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(physSlider, javafx.scene.layout.Priority.ALWAYS);
        Label physValueLabel = new Label(String.format("%.1f", evaluation.getNotePhysique()));
        physValueLabel.getStyleClass().add("evaluation-value-label");
        physValueLabel.setMinWidth(50);
        physSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            physValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        physSliderBox.getChildren().addAll(physSlider, physValueLabel);
        physBox.getChildren().addAll(physLabel, physSliderBox);

        // Technical score
        javafx.scene.layout.VBox techBox = new javafx.scene.layout.VBox(8);
        Label techLabel = new Label("⚽ Note Technique");
        techLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox techSliderBox = new javafx.scene.layout.HBox(15);
        techSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider techSlider = new javafx.scene.control.Slider(0, 20, evaluation.getNoteTechnique());
        techSlider.setShowTickLabels(true);
        techSlider.setShowTickMarks(true);
        techSlider.setMajorTickUnit(5);
        techSlider.setMinorTickCount(4);
        techSlider.setBlockIncrement(1);
        techSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(techSlider, javafx.scene.layout.Priority.ALWAYS);
        Label techValueLabel = new Label(String.format("%.1f", evaluation.getNoteTechnique()));
        techValueLabel.getStyleClass().add("evaluation-value-label");
        techValueLabel.setMinWidth(50);
        techSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            techValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        techSliderBox.getChildren().addAll(techSlider, techValueLabel);
        techBox.getChildren().addAll(techLabel, techSliderBox);

        // Tactical score
        javafx.scene.layout.VBox tactBox = new javafx.scene.layout.VBox(8);
        Label tactLabel = new Label("🎯 Note Tactique");
        tactLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.layout.HBox tactSliderBox = new javafx.scene.layout.HBox(15);
        tactSliderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Slider tactSlider = new javafx.scene.control.Slider(0, 20, evaluation.getNoteTactique());
        tactSlider.setShowTickLabels(true);
        tactSlider.setShowTickMarks(true);
        tactSlider.setMajorTickUnit(5);
        tactSlider.setMinorTickCount(4);
        tactSlider.setBlockIncrement(1);
        tactSlider.getStyleClass().add("evaluation-slider");
        javafx.scene.layout.HBox.setHgrow(tactSlider, javafx.scene.layout.Priority.ALWAYS);
        Label tactValueLabel = new Label(String.format("%.1f", evaluation.getNoteTactique()));
        tactValueLabel.getStyleClass().add("evaluation-value-label");
        tactValueLabel.setMinWidth(50);
        tactSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            tactValueLabel.setText(String.format("%.1f", newVal.doubleValue()))
        );
        tactSliderBox.getChildren().addAll(tactSlider, tactValueLabel);
        tactBox.getChildren().addAll(tactLabel, tactSliderBox);

        // Average display
        javafx.scene.layout.HBox avgBox = new javafx.scene.layout.HBox(10);
        avgBox.setAlignment(javafx.geometry.Pos.CENTER);
        avgBox.getStyleClass().add("evaluation-average-box");
        avgBox.setPadding(new javafx.geometry.Insets(15, 20, 15, 20));
        Label avgTitleLabel = new Label("Moyenne:");
        avgTitleLabel.getStyleClass().add("evaluation-average-title");
        double initialAvg = (evaluation.getNotePhysique() + evaluation.getNoteTechnique() + evaluation.getNoteTactique()) / 3.0;
        Label avgValueLabel = new Label(String.format("%.2f / 20", initialAvg));
        avgValueLabel.getStyleClass().add("evaluation-average-value");
        avgBox.getChildren().addAll(avgTitleLabel, avgValueLabel);

        // Update average when sliders change
        javafx.beans.value.ChangeListener<Number> avgListener = (obs, oldVal, newVal) -> {
            double avg = (physSlider.getValue() + techSlider.getValue() + tactSlider.getValue()) / 3.0;
            avgValueLabel.setText(String.format("%.2f / 20", avg));
        };
        physSlider.valueProperty().addListener(avgListener);
        techSlider.valueProperty().addListener(avgListener);
        tactSlider.valueProperty().addListener(avgListener);

        // Comment
        javafx.scene.layout.VBox commentBox = new javafx.scene.layout.VBox(8);
        Label commentLabel = new Label("💬 Commentaire (optionnel)");
        commentLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.control.TextArea commentArea = new javafx.scene.control.TextArea();
        commentArea.setPromptText("Ajoutez vos observations sur la performance...");
        commentArea.getStyleClass().add("evaluation-comment-area");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        if (evaluation.getCommentaire() != null) {
            commentArea.setText(evaluation.getCommentaire());
        }
        commentBox.getChildren().addAll(commentLabel, commentArea);

        content.getChildren().addAll(physBox, techBox, tactBox, avgBox, commentBox);

        // Footer
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(15);
        footer.setPadding(new javafx.geometry.Insets(20, 30, 30, 30));
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getStyleClass().add("evaluation-dialog-footer");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-save-button");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                evaluation.setNotePhysique(physSlider.getValue());
                evaluation.setNoteTechnique(techSlider.getValue());
                evaluation.setNoteTactique(tactSlider.getValue());
                String comment = commentArea.getText().trim();
                evaluation.setCommentaire(comment.isEmpty() ? null : comment);

                evaluationService.update(evaluation);
                String emailStatus = notifyPlayerAboutEvaluation(evaluation, true);
                refreshEvaluations();
                dialog.close();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText(null);
                success.setContentText("Évaluation modifiée avec succès!\n" + emailStatus);
                success.showAndWait();
                
            } catch (SQLException ex) {
                showError("Modification", "Erreur lors de la modification.\n" + ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, content, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/tn/esprit/styles/entrainement-theme.css").toExternalForm());
<<<<<<< HEAD
        ThemeManager.registerScene(scene);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openParticipationEditDialog(Participation participation) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifier la participation");
        dialog.setResizable(false);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.getStyleClass().add("evaluation-dialog-root");
        root.setMaxWidth(500);

        // Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(12);
        header.getStyleClass().add("evaluation-dialog-header");
        header.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        
        Label titleLabel = new Label("Modifier la participation #" + participation.getId());
        titleLabel.getStyleClass().add("evaluation-dialog-title");
        
        Label playerLabel = new Label("Joueur: " + resolvePlayerLabel(participation.getJoueurId()));
        playerLabel.getStyleClass().add("evaluation-dialog-player");
        
        Label sessionLabel = new Label("Session: " + resolveTrainingLabel(participation.getEntrainementId()));
        sessionLabel.getStyleClass().add("evaluation-dialog-session");
        
        header.getChildren().addAll(titleLabel, playerLabel, sessionLabel);

        // Content
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(25);
        content.setPadding(new javafx.geometry.Insets(30, 30, 30, 30));
        content.getStyleClass().add("evaluation-dialog-content");

        Label instructionLabel = new Label("Modifier le statut de présence");
        instructionLabel.getStyleClass().add("evaluation-dialog-instruction");

        // Presence toggle buttons
        javafx.scene.layout.HBox toggleBox = new javafx.scene.layout.HBox(15);
        toggleBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        javafx.scene.control.ToggleGroup presenceGroup = new javafx.scene.control.ToggleGroup();
        
        javafx.scene.control.ToggleButton presentBtn = new javafx.scene.control.ToggleButton("✓ Présent");
        presentBtn.getStyleClass().addAll("attendance-toggle-button", "present-button");
        presentBtn.setToggleGroup(presenceGroup);
        presentBtn.setPrefWidth(150);
        presentBtn.setPrefHeight(60);
        
        javafx.scene.control.ToggleButton absentBtn = new javafx.scene.control.ToggleButton("✗ Absent");
        absentBtn.getStyleClass().addAll("attendance-toggle-button", "absent-button");
        absentBtn.setToggleGroup(presenceGroup);
        absentBtn.setPrefWidth(150);
        absentBtn.setPrefHeight(60);
        
        // Set initial selection
        if ("Present".equalsIgnoreCase(participation.getPresence())) {
            presentBtn.setSelected(true);
        } else {
            absentBtn.setSelected(true);
        }
        
        toggleBox.getChildren().addAll(presentBtn, absentBtn);

        // Justification field (only for absent)
        javafx.scene.layout.VBox justifBox = new javafx.scene.layout.VBox(8);
        Label justifLabel = new Label("📝 Justification d'absence (optionnel)");
        justifLabel.getStyleClass().add("evaluation-dialog-label");
        javafx.scene.control.TextArea justifArea = new javafx.scene.control.TextArea();
        justifArea.setPromptText("Raison de l'absence...");
        justifArea.getStyleClass().add("evaluation-comment-area");
        justifArea.setPrefRowCount(3);
        justifArea.setWrapText(true);
        if (participation.getJustificationAbsence() != null) {
            justifArea.setText(participation.getJustificationAbsence());
        }
        justifBox.getChildren().addAll(justifLabel, justifArea);
        justifBox.setVisible("Absent".equalsIgnoreCase(participation.getPresence()));
        justifBox.setManaged("Absent".equalsIgnoreCase(participation.getPresence()));

        // Show/hide justification based on selection
        presenceGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAbsent = newVal == absentBtn;
            justifBox.setVisible(isAbsent);
            justifBox.setManaged(isAbsent);
            if (!isAbsent) {
                justifArea.clear();
            }
        });

        content.getChildren().addAll(instructionLabel, toggleBox, justifBox);

        // Footer
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(15);
        footer.setPadding(new javafx.geometry.Insets(20, 30, 30, 30));
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getStyleClass().add("evaluation-dialog-footer");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("evaluation-dialog-button", "evaluation-save-button");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                if (presenceGroup.getSelectedToggle() == null) {
                    showError("Validation", "Veuillez sélectionner le statut de présence");
                    return;
                }

                String presence = presenceGroup.getSelectedToggle() == presentBtn ? "Present" : "Absent";
                participation.setPresence(presence);
                
                String justif = justifArea.getText().trim();
                participation.setJustificationAbsence(justif.isEmpty() ? null : justif);

                participationService.update(participation);
                refreshParticipations();
                dialog.close();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText(null);
                success.setContentText("Participation modifiée avec succès!");
                success.showAndWait();
                
            } catch (SQLException ex) {
                showError("Modification", "Erreur lors de la modification.\n" + ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, content, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/tn/esprit/styles/entrainement-theme.css").toExternalForm());
<<<<<<< HEAD
        ThemeManager.registerScene(scene);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        
        dialog.setScene(scene);
        dialog.showAndWait();
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
            return (firstName + " " + (lastName == null ? "" : lastName)).trim();
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

        @Override
        public String toString() {
            return label;
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
