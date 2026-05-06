package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.entities.AiChecklistProgress;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.AIRecommendationService;
import tn.esprit.services.AiChecklistProgressService;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EvaluationService;
import tn.esprit.services.ExercisePlanGenerator;
import tn.esprit.services.MealPlanGenerator;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.PerformanceAnalyticsService;
import tn.esprit.services.UserService;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntrainementUserController {
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private HBox navbarRoot;
    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button matchsNavButton;
    @FXML
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button annonceNavButton;
    @FXML
    private Button entrainementNavButton;
    @FXML
    private ToggleButton themeToggleButton;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortChoiceBox;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private Label countLabel;
    @FXML
    private Label emptyStateLabel;

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
    private TextArea notesField;
    @FXML
    private Label validationLabel;
    @FXML
    private Label participationContextLabel;
    @FXML
    private ComboBox<String> participationPresenceField;
    @FXML
    private TextArea participationJustificationField;
    @FXML
    private Label evaluationContextLabel;
    @FXML
    private VBox evaluationCardsPane;
    @FXML
    private VBox managementPanel;
    @FXML
    private VBox coachOnlyNoticePanel;

    // Performance Evolution Fields
    @FXML
    private Button togglePerformanceButton;
    @FXML
    private Button foodTrackingButton;
    @FXML
    private VBox performanceSection;
    @FXML
    private Label totalEvaluationsLabel;
    @FXML
    private Label attendanceRateLabel;
    @FXML
    private Label avgOverallLabel;
    @FXML
    private VBox performanceChartContainer;
    @FXML
    private VBox weakAreasBox;
    @FXML
    private Button getAIRecommendationsButton;

    private final ObservableList<Entrainement> master = FXCollections.observableArrayList();
    private final ObservableList<Entrainement> filtered = FXCollections.observableArrayList();
    private final ObservableList<CoachOption> coachOptions = FXCollections.observableArrayList();
    private final Map<Integer, Participation> participationByTrainingId = new HashMap<>();
    private final Map<Integer, Evaluation> evaluationByTrainingId = new HashMap<>();

    private EntrainementService entrainementService;
    private EvaluationService evaluationService;
    private ParticipationService participationService;
    private UserService userService;
    private PerformanceAnalyticsService analyticsService;
    private AIRecommendationService aiService;
    private Entrainement selected;
    private Integer currentUserId;
    private boolean currentUserIsCoach;
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean performanceExpanded = false;

    @FXML
    public void initialize() {
        configureNavbar();
        ThemeManager.bindToggle(themeToggleButton);
        sortChoiceBox.setItems(FXCollections.observableArrayList("Date", "Type", "Lieu"));
        sortChoiceBox.setValue("Date");
        participationPresenceField.setItems(FXCollections.observableArrayList("Present", "Absent"));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortChoiceBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        try {
            entrainementService = new EntrainementService();
            evaluationService = new EvaluationService();
            participationService = new ParticipationService();
            userService = new UserService();
            analyticsService = new PerformanceAnalyticsService();
            aiService = new AIRecommendationService();
            loadCoaches();
            resolveCurrentUser();
            applyManagementAccess();
            refreshData();
            initializePerformanceSection();
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
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
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
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton, "/tn/esprit/views/annonce-user-view.fxml", "/tn/esprit/styles/annonce-theme.css", "Anonce | Sport Insight");
    }

    @FXML
    private void handleOpenEntrainements() {
        // Already on the training page.
    }

    @FXML
    private void handleOpenFoodTracking() {
        SceneNavigator.switchScene(foodTrackingButton, 
            "/tn/esprit/views/food-tracking-view.fxml", 
            "/tn/esprit/styles/food-tracking-theme.css", 
            "Suivi Nutritionnel | Sport Insight");
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void handleAdd() {
        if (!ensureCoachAccess()) {
            return;
        }
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
        if (!ensureCoachAccess()) {
            return;
        }
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
        if (!ensureCoachAccess()) {
            return;
        }
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
    private void handleSaveParticipation() {
        if (!ensureCoachAccess()) {
            return;
        }
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez une session avant d'enregistrer la participation.");
            return;
        }
        if (currentUserId == null) {
            showValidation("Aucun coach connecte.");
            return;
        }
        String presence = participationPresenceField.getValue();
        if (presence == null || presence.isBlank()) {
            markInvalid(participationPresenceField);
            showValidation("Presence obligatoire.");
            return;
        }
        try {
            Participation existing = findCurrentUserParticipation(selected.getId());
            if (existing == null) {
                existing = new Participation();
            }
            existing.setPresence(presence);
            existing.setJustificationAbsence(optionalText(participationJustificationField));
            existing.setEntrainementId(selected.getId());
            existing.setJoueurId(currentUserId);
            if (existing.getId() == null) {
                participationService.add(existing);
            } else {
                participationService.update(existing);
            }
            refreshParticipationAndEvaluations();
        } catch (SQLException e) {
            showError("Participation", "Erreur lors de l'enregistrement.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteParticipation() {
        if (!ensureCoachAccess()) {
            return;
        }
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez une session avant de supprimer la participation.");
            return;
        }
        if (currentUserId == null) {
            showValidation("Aucun coach connecte.");
            return;
        }
        try {
            Participation existing = findCurrentUserParticipation(selected.getId());
            if (existing == null || existing.getId() == null) {
                showValidation("Aucune participation a supprimer.");
                return;
            }
            participationService.delete(existing.getId());
            refreshParticipationAndEvaluations();
        } catch (SQLException e) {
            showError("Participation", "Erreur lors de la suppression.\n" + e.getMessage());
        }
    }


    private void refreshData() {
        try {
            master.setAll(entrainementService.getAll());
            refreshCurrentUserTracking();
            applyFilters();
<<<<<<< HEAD
            refreshPerformanceSection();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
        updateCards();
    }

    private void updateCards() {
        cardsPane.getChildren().clear();
        for (Entrainement entrainement : filtered) {
            cardsPane.getChildren().add(buildCard(entrainement));
        }
        countLabel.setText(String.valueOf(filtered.size()));
        emptyStateLabel.setVisible(filtered.isEmpty());
        emptyStateLabel.setManaged(filtered.isEmpty());
    }

    private Node buildCard(Entrainement entrainement) {
        VBox card = new VBox(10);
        card.getStyleClass().add("training-card");
        card.setUserData(entrainement.getId());
        applyParticipationStyle(card, entrainement.getId());

        VBox hero = new VBox(4);
        hero.getStyleClass().add("training-card-hero");
        Label type = new Label(emptyIfNull(entrainement.getType(), "Session"));
        type.getStyleClass().add("training-card-title");
        Label date = new Label(formatDate(entrainement.getDateEntrainement()));
        date.getStyleClass().add("training-card-subtitle");
        hero.getChildren().addAll(type, date);

        VBox body = new VBox(6);
        body.getStyleClass().add("training-card-body");
        Label time = new Label(formatTimeRange(entrainement));
        time.getStyleClass().add("training-card-line");
        Label place = new Label("Lieu: " + emptyIfNull(entrainement.getLieu(), "-"));
        place.getStyleClass().add("training-card-line");
        Label objectif = new Label("Objectif: " + emptyIfNull(entrainement.getObjectif(), "-"));
        objectif.getStyleClass().add("training-card-line");
        Label coach = new Label("Coach: " + resolveCoachLabel(entrainement.getEntraineurId()));
        coach.getStyleClass().add("training-card-line");
        Label badge = new Label(formatDurationBadge(entrainement));
        badge.getStyleClass().add("training-card-badge");

        Participation participation = participationByTrainingId.get(entrainement.getId());
        Label attendance = new Label(buildAttendanceLabel(participation));
        attendance.getStyleClass().addAll("training-card-line", "training-card-participation");

        Evaluation evaluation = evaluationByTrainingId.get(entrainement.getId());
        body.getChildren().addAll(time, place, objectif, coach, attendance, badge);
        if (evaluation != null) {
            Label score = new Label("Ma note: " + formatAverage(evaluation) + "/20");
            score.getStyleClass().add("training-card-score");
            body.getChildren().add(score);
        }

        card.getChildren().addAll(hero, body);
        card.setOnMouseClicked(event -> handleCardClick(entrainement));
        updateCardSelection(card);
        return card;
    }

    private void handleCardClick(Entrainement entrainement) {
        if (currentUserIsCoach) {
            selectEntrainement(entrainement);
            return;
        }
        openParticipationDialog(entrainement);
    }

    private void updateCardSelection(VBox card) {
        Integer id = (Integer) card.getUserData();
        boolean active = selected != null && Objects.equals(selected.getId(), id);
        card.getStyleClass().remove("training-card-selected");
        if (active) {
            card.getStyleClass().add("training-card-selected");
        }
    }

    private void applyParticipationStyle(VBox card, Integer entrainementId) {
        card.getStyleClass().removeAll("training-card-attending", "training-card-absent");
        Participation participation = entrainementId == null ? null : participationByTrainingId.get(entrainementId);
        if (isPresentParticipation(participation)) {
            card.getStyleClass().add("training-card-attending");
        } else if (isAbsentParticipation(participation)) {
            card.getStyleClass().add("training-card-absent");
        }
    }

    private void selectEntrainement(Entrainement entrainement) {
        selected = entrainement;
        populateForm(entrainement);
        formHintLabel.setText("Modification de la session #" + entrainement.getId());
        cardsPane.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                updateCardSelection(card);
            }
        });
        refreshParticipationAndEvaluations();
    }

    private void openParticipationDialog(Entrainement entrainement) {
        if (currentUserId == null) {
            showError("Participation", "Aucun utilisateur connecte.");
            return;
        }

        Participation existing = participationByTrainingId.get(entrainement.getId());
        showModernAttendanceDialog(entrainement, existing);
    }

    private void showModernAttendanceDialog(Entrainement entrainement, Participation existing) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Marquer ma présence");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.getStyleClass().add("attendance-dialog-root");
        root.setMaxWidth(500);

        // Header with gradient
        VBox header = new VBox(12);
        header.getStyleClass().add("attendance-dialog-header");
        header.setPadding(new Insets(30, 30, 30, 30));
        
        Label titleLabel = new Label("Marquer ma présence");
        titleLabel.getStyleClass().add("attendance-dialog-title");
        
        Label sessionLabel = new Label(emptyIfNull(entrainement.getType(), "Entrainement"));
        sessionLabel.getStyleClass().add("attendance-dialog-session");
        
        HBox dateTimeBox = new HBox(15);
        dateTimeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dateLabel = new Label("📅 " + formatDate(entrainement.getDateEntrainement()));
        dateLabel.getStyleClass().add("attendance-dialog-info");
        
        Label timeLabel = new Label("🕐 " + formatTime(entrainement.getHeureDebut()) + " - " + formatTime(entrainement.getHeureFin()));
        timeLabel.getStyleClass().add("attendance-dialog-info");
        
        dateTimeBox.getChildren().addAll(dateLabel, timeLabel);
        
        Label locationLabel = new Label("📍 " + emptyIfNull(entrainement.getLieu(), "Lieu non spécifié"));
        locationLabel.getStyleClass().add("attendance-dialog-info");
        
        header.getChildren().addAll(titleLabel, sessionLabel, dateTimeBox, locationLabel);

        // Content area
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 30, 30, 30));
        content.getStyleClass().add("attendance-dialog-content");

        Label questionLabel = new Label("Serez-vous présent(e) à cette session ?");
        questionLabel.getStyleClass().add("attendance-dialog-question");
        questionLabel.setWrapText(true);

        // Modern toggle buttons for Present/Absent
        HBox choiceBox = new HBox(15);
        choiceBox.setAlignment(Pos.CENTER);
        
        ToggleButton presentBtn = new ToggleButton("✓ Je serai présent(e)");
        presentBtn.getStyleClass().addAll("attendance-choice-button", "attendance-present-button");
        presentBtn.setPrefWidth(200);
        presentBtn.setPrefHeight(80);
        
        ToggleButton absentBtn = new ToggleButton("✗ Je serai absent(e)");
        absentBtn.getStyleClass().addAll("attendance-choice-button", "attendance-absent-button");
        absentBtn.setPrefWidth(200);
        absentBtn.setPrefHeight(80);

        // Make buttons mutually exclusive
        javafx.scene.control.ToggleGroup toggleGroup = new javafx.scene.control.ToggleGroup();
        presentBtn.setToggleGroup(toggleGroup);
        absentBtn.setToggleGroup(toggleGroup);

        // Set existing selection
        if (existing != null && existing.getPresence() != null) {
            if ("Present".equalsIgnoreCase(existing.getPresence())) {
                presentBtn.setSelected(true);
            } else if ("Absent".equalsIgnoreCase(existing.getPresence())) {
                absentBtn.setSelected(true);
            }
        }

        choiceBox.getChildren().addAll(presentBtn, absentBtn);

        // Justification area (only shown when absent is selected)
        VBox justificationBox = new VBox(10);
        justificationBox.setManaged(false);
        justificationBox.setVisible(false);
        
        Label justificationLabel = new Label("Motif d'absence (optionnel)");
        justificationLabel.getStyleClass().add("attendance-dialog-label");
        
        TextArea justificationArea = new TextArea();
        justificationArea.setPromptText("Indiquez la raison de votre absence...");
        justificationArea.getStyleClass().add("attendance-justification-area");
        justificationArea.setPrefRowCount(3);
        justificationArea.setWrapText(true);
        
        if (existing != null && existing.getJustificationAbsence() != null) {
            justificationArea.setText(existing.getJustificationAbsence());
        }
        
        justificationBox.getChildren().addAll(justificationLabel, justificationArea);

        // Show/hide justification based on selection
        absentBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            justificationBox.setManaged(newVal);
            justificationBox.setVisible(newVal);
        });

        content.getChildren().addAll(questionLabel, choiceBox, justificationBox);

        // Footer with action buttons
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(20, 30, 30, 30));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("attendance-dialog-footer");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().addAll("attendance-dialog-button", "attendance-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button clearBtn = new Button("Effacer");
        clearBtn.getStyleClass().addAll("attendance-dialog-button", "attendance-clear-button");
        clearBtn.setOnAction(e -> {
            try {
                if (existing != null && existing.getId() != null) {
                    participationService.delete(existing.getId());
                    refreshData();
                    dialog.close();
                }
            } catch (SQLException ex) {
                showError("Participation", "Erreur lors de la suppression.\n" + ex.getMessage());
            }
        });
        clearBtn.setDisable(existing == null || existing.getId() == null);

        Button saveBtn = new Button("Confirmer");
        saveBtn.getStyleClass().addAll("attendance-dialog-button", "attendance-save-button");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            if (toggleGroup.getSelectedToggle() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez sélectionner votre présence ou absence.");
                alert.showAndWait();
                return;
            }

            try {
                String presence = presentBtn.isSelected() ? "Present" : "Absent";
                String justification = justificationArea.getText().trim();
                justification = justification.isEmpty() ? null : justification;

                Participation participation = existing == null ? new Participation() : existing;
                participation.setPresence(presence);
                participation.setJustificationAbsence(justification);
                participation.setEntrainementId(entrainement.getId());
                participation.setJoueurId(currentUserId);
                
                if (participation.getId() == null) {
                    participationService.add(participation);
                } else {
                    participationService.update(participation);
                }
                
                refreshData();
                dialog.close();
            } catch (SQLException ex) {
                showError("Participation", "Erreur lors de l'enregistrement.\n" + ex.getMessage());
            }
        });

        footer.getChildren().addAll(clearBtn, cancelBtn, saveBtn);

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

    private void populateForm(Entrainement entrainement) {
        dateField.setValue(entrainement.getDateEntrainement());
        startField.setText(formatTime(entrainement.getHeureDebut()));
        endField.setText(formatTime(entrainement.getHeureFin()));
        typeField.setText(emptyIfNull(entrainement.getType(), ""));
        objectifField.setText(emptyIfNull(entrainement.getObjectif(), ""));
        lieuField.setText(emptyIfNull(entrainement.getLieu(), ""));
        selectCoachById(entrainement.getEntraineurId());
    }

    private void clearForm() {
        selected = null;
        dateField.setValue(null);
        startField.clear();
        endField.clear();
        typeField.clear();
        objectifField.clear();
        lieuField.clear();
        notesField.clear();
        coachField.getSelectionModel().clearSelection();
        formHintLabel.setText("Renseignez les details de la session.");
        participationPresenceField.getSelectionModel().clearSelection();
        participationJustificationField.clear();
        evaluationCardsPane.getChildren().clear();
        participationContextLabel.setText("Selectionnez une session pour marquer votre presence.");
        evaluationContextLabel.setText("Aucune evaluation chargee.");
        cardsPane.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                updateCardSelection(card);
            }
        });
        clearValidation();
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
                .filter(this::isCoachAccount)
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

    private void resolveCurrentUser() {
        User current = AuthSession.getCurrentUser();
        currentUserId = current == null ? null : current.getId();
        currentUserIsCoach = current != null && current.hasRole(UserRoles.ROLE_ENTRAINEUR);
    }

    private void refreshCurrentUserTracking() throws SQLException {
        participationByTrainingId.clear();
        evaluationByTrainingId.clear();
        if (currentUserId == null) {
            return;
        }

        for (Participation participation : participationService.getAll()) {
            if (Objects.equals(participation.getJoueurId(), currentUserId) && participation.getEntrainementId() != null) {
                participationByTrainingId.put(participation.getEntrainementId(), participation);
            }
        }

        for (Evaluation evaluation : evaluationService.getAll()) {
            if (!Objects.equals(evaluation.getJoueurId(), currentUserId) || evaluation.getEntrainementId() == null) {
                continue;
            }
            Evaluation existing = evaluationByTrainingId.get(evaluation.getEntrainementId());
            if (existing == null) {
                evaluationByTrainingId.put(evaluation.getEntrainementId(), evaluation);
                continue;
            }
            Integer currentId = evaluation.getId();
            Integer existingId = existing.getId();
            if (currentId != null && (existingId == null || currentId > existingId)) {
                evaluationByTrainingId.put(evaluation.getEntrainementId(), evaluation);
            }
        }
    }

    private void applyManagementAccess() {
        if (managementPanel != null) {
            managementPanel.setManaged(currentUserIsCoach);
            managementPanel.setVisible(currentUserIsCoach);
        }
        if (coachOnlyNoticePanel != null) {
            coachOnlyNoticePanel.setManaged(false);
            coachOnlyNoticePanel.setVisible(false);
        }
    }

    private void configureNavbar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
        if (entrainementNavButton != null && !entrainementNavButton.getStyleClass().contains("navbar-nav-button-active")) {
            entrainementNavButton.getStyleClass().add("navbar-nav-button-active");
        }
    }

    private boolean isCoachAccount(User user) {
        return user != null && user.hasRole(UserRoles.ROLE_ENTRAINEUR);
    }

    private boolean isCoach(User user) {
        String roles = user == null ? null : user.getRoles();
        if (roles == null) {
            return false;
        }
        String value = roles.toLowerCase();
        return value.contains("coach") || value.contains("entraineur") || value.contains("entraîneur");
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

    private boolean matchesKeyword(Entrainement entrainement, String keyword) {
        return contains(entrainement.getType(), keyword)
                || contains(entrainement.getLieu(), keyword)
                || contains(entrainement.getObjectif(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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
            return "Horaire non defini";
        }
        return "Horaire: " + start + " - " + end;
    }

    private String formatDurationBadge(Entrainement entrainement) {
        if (entrainement.getHeureDebut() == null || entrainement.getHeureFin() == null) {
            return "Duree inconnue";
        }
        long minutes = java.time.Duration.between(entrainement.getHeureDebut(), entrainement.getHeureFin()).toMinutes();
        return minutes > 0 ? minutes + " min" : "Session";
    }

    private String buildAttendanceLabel(Participation participation) {
        if (participation == null || participation.getPresence() == null || participation.getPresence().isBlank()) {
            return "Participation: non renseignee";
        }
        return "Participation: " + participation.getPresence();
    }

    private boolean isPresentParticipation(Participation participation) {
        return participation != null && "present".equalsIgnoreCase(participation.getPresence());
    }

    private boolean isAbsentParticipation(Participation participation) {
        return participation != null && "absent".equalsIgnoreCase(participation.getPresence());
    }

    private String emptyIfNull(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
        clearInvalid(dateField, startField, endField, typeField, objectifField, lieuField, coachField, participationPresenceField);
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshParticipationAndEvaluations() {
        if (!currentUserIsCoach) {
            return;
        }
        if (selected == null) {
            participationContextLabel.setText("Selectionnez une session pour marquer votre presence.");
            evaluationContextLabel.setText("Aucune evaluation chargee.");
            evaluationCardsPane.getChildren().clear();
            return;
        }
        participationContextLabel.setText("Session: " + buildTrainingLabel(selected));
        if (currentUserId == null) {
            evaluationContextLabel.setText("Aucun coach connecte.");
            evaluationCardsPane.getChildren().clear();
            return;
        }
        try {
            Participation participation = findCurrentUserParticipation(selected.getId());
            if (participation != null) {
                participationPresenceField.setValue(participation.getPresence());
                participationJustificationField.setText(optionalText(participation.getJustificationAbsence()));
            } else {
                participationPresenceField.getSelectionModel().clearSelection();
                participationJustificationField.clear();
            }

            List<Evaluation> evaluations = evaluationService.getByEntrainement(selected.getId()).stream()
                    .filter(evaluation -> Objects.equals(evaluation.getJoueurId(), currentUserId))
                    .collect(Collectors.toList());
            evaluationCardsPane.getChildren().clear();
            if (evaluations.isEmpty()) {
                evaluationContextLabel.setText("Aucune evaluation pour cette session.");
            } else {
                evaluationContextLabel.setText("Evaluations de la session.");
                for (Evaluation evaluation : evaluations) {
                    evaluationCardsPane.getChildren().add(buildEvaluationCard(evaluation));
                }
            }
        } catch (SQLException e) {
            showError("Evaluation", "Impossible de charger les evaluations.\n" + e.getMessage());
        }
    }

    private VBox buildEvaluationCard(Evaluation evaluation) {
        VBox card = new VBox(4);
        card.getStyleClass().add("note-card");
        Label title = new Label("Note moyenne: " + formatAverage(evaluation));
        title.getStyleClass().add("note-card-title");
        Label scores = new Label("P " + evaluation.getNotePhysique()
                + " | T " + evaluation.getNoteTechnique()
                + " | Tac " + evaluation.getNoteTactique());
        scores.getStyleClass().add("note-card-text");
        card.getChildren().addAll(title, scores);
        if (evaluation.getCommentaire() != null && !evaluation.getCommentaire().isBlank()) {
            Label comment = new Label(evaluation.getCommentaire());
            comment.getStyleClass().add("note-card-comment");
            comment.setWrapText(true);
            card.getChildren().add(comment);
        }
        return card;
    }

    private String formatAverage(Evaluation evaluation) {
        double average = (evaluation.getNotePhysique() + evaluation.getNoteTechnique() + evaluation.getNoteTactique()) / 3.0;
        return String.format("%.2f", average);
    }

    private Participation findCurrentUserParticipation(Integer entrainementId) throws SQLException {
        if (entrainementId == null || currentUserId == null) {
            return null;
        }
        return participationService.getByEntrainement(entrainementId).stream()
                .filter(participation -> Objects.equals(participation.getJoueurId(), currentUserId))
                .findFirst()
                .orElse(null);
    }

    private boolean ensureCoachAccess() {
        if (currentUserIsCoach) {
            return true;
        }
        showValidation("Seuls les comptes coach peuvent gerer les entrainements depuis cette interface.");
        return false;
    }

    private String optionalText(TextArea field) {
        if (field == null) {
            return null;
        }
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String optionalText(String value) {
        return value == null ? null : value;
    }

    // ==================== PERFORMANCE EVOLUTION METHODS ====================

    private void initializePerformanceSection() {
        if (currentUserId == null || currentUserIsCoach) {
            // Hide performance section for coaches or if no user
            if (togglePerformanceButton != null) {
                togglePerformanceButton.setVisible(false);
                togglePerformanceButton.setManaged(false);
            }
            return;
        }
        loadPerformanceData();
    }

<<<<<<< HEAD
    private void refreshPerformanceSection() {
        if (currentUserId == null || currentUserIsCoach || analyticsService == null) {
            return;
        }
        loadPerformanceData();
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    @FXML
    private void handleTogglePerformance() {
        performanceExpanded = !performanceExpanded;
        performanceSection.setVisible(performanceExpanded);
        performanceSection.setManaged(performanceExpanded);
        togglePerformanceButton.setText(performanceExpanded ? "▲ Masquer" : "▼ Afficher");
        
        if (performanceExpanded) {
            loadPerformanceData();
        }
    }

    @FXML
    private void handleGetAIRecommendations() {
        if (currentUserId == null) {
            showError("Erreur", "Utilisateur non identifié.");
            return;
        }

        try {
            List<Evaluation> evaluations = analyticsService.getPlayerEvaluationHistory(currentUserId);
            
            if (evaluations == null || evaluations.isEmpty()) {
                showInfo("Pas de données", "Vous devez avoir au moins une évaluation pour obtenir des recommandations.");
                return;
            }

            // Show loading dialog
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("Génération en cours...");
            loadingAlert.setHeaderText("🤖 L'IA analyse vos performances");
            loadingAlert.setContentText("Veuillez patienter...");
            loadingAlert.show();

            // Generate recommendations in background thread
            new Thread(() -> {
                try {
                    // Calculate stats
                    PerformanceAnalyticsService.PerformanceStats stats = analyticsService.calculateStats(evaluations);
                    double attendanceRate = analyticsService.getAttendanceRate(currentUserId);
                    
                    User currentUser = userService.getById(currentUserId);
                    String playerName = currentUser.getPrenom() + " " + currentUser.getNom();
                    
                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAIRecommendationsDialog(stats, playerName, attendanceRate);
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showError("Erreur IA", "Erreur lors de la génération des recommandations:\n" + e.getMessage());
                    });
                }
            }).start();
            
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les évaluations:\n" + e.getMessage());
        }
    }

    private void showAIRecommendationsDialog(PerformanceAnalyticsService.PerformanceStats stats, String playerName, double attendanceRate) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("🤖 Recommandations IA Personnalisées");
        dialog.setResizable(true);

        // Main ScrollPane
        javafx.scene.control.ScrollPane mainScrollPane = new javafx.scene.control.ScrollPane();
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background: #f5f7fa; -fx-background-color: #f5f7fa;");

        VBox mainContent = new VBox(30);
        mainContent.setStyle("-fx-padding: 30; -fx-background-color: #f5f7fa;");

        // Header Section with Stats
        VBox header = createHeaderWithStats(stats);
        
        // Training Section with Interactive Cards
        VBox trainingSection = createInteractiveTrainingSection(stats, playerName);
        
        // Nutrition Section with Meal Plans
        VBox nutritionSection = createInteractiveMealPlans(stats, playerName);

        mainContent.getChildren().addAll(header, trainingSection, nutritionSection);
        mainScrollPane.setContent(mainContent);

        // Footer
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Button closeButton = new Button("✓ Fermer");
        closeButton.setStyle(
            "-fx-background-color: #FF6B6B;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 15 40;" +
            "-fx-background-radius: 30;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialog.close());
        
        footer.getChildren().add(closeButton);

        VBox root = new VBox(0);
        root.getChildren().addAll(mainScrollPane, footer);
        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1200, 900);
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox createHeaderWithStats(PerformanceAnalyticsService.PerformanceStats stats) {
        VBox header = new VBox(20);
        header.setAlignment(Pos.CENTER);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #FF6B6B, #4ECDC4, #45B7D1);" +
            "-fx-padding: 40 20;" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);"
        );
        
        Label iconLabel = new Label("🤖");
        iconLabel.setStyle("-fx-font-size: 60px;");
        
        Label titleLabel = new Label("Votre Programme Personnalisé");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Stats Row
        HBox statsRow = new HBox(40);
        statsRow.setAlignment(Pos.CENTER);
        
        VBox physiqueStat = createStatBadge("💪", "Physique", String.format("%.1f/20", stats.avgPhysique()), "#FF6B6B");
        VBox techniqueStat = createStatBadge("⚽", "Technique", String.format("%.1f/20", stats.avgTechnique()), "#4ECDC4");
        VBox tactiqueStat = createStatBadge("🎯", "Tactique", String.format("%.1f/20", stats.avgTactique()), "#45B7D1");
        
        statsRow.getChildren().addAll(physiqueStat, techniqueStat, tactiqueStat);
        
        header.getChildren().addAll(iconLabel, titleLabel, statsRow);
        return header;
    }

    private VBox createStatBadge(String emoji, String label, String value, String color) {
        VBox badge = new VBox(5);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2);" +
            "-fx-padding: 15 25;" +
            "-fx-background-radius: 15;"
        );
        
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 30px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-opacity: 0.9;");
        
        badge.getChildren().addAll(emojiLabel, valueLabel, labelText);
        return badge;
    }

    private VBox createInteractiveTrainingSection(PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        VBox section = new VBox(20);
        
        Label sectionTitle = new Label("🏋️ PROGRAMME D'ENTRAÎNEMENT");
        sectionTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        GridPane exerciseGrid = new GridPane();
        exerciseGrid.setHgap(20);
        exerciseGrid.setVgap(20);
        
        // Determine focus areas based on scores
        boolean needsPhysique = stats.avgPhysique() < 14;
        boolean needsTechnique = stats.avgTechnique() < 14;
        boolean needsTactique = stats.avgTactique() < 14;
        
        // Exercise 1: Cardio (always important)
        VBox cardio = createClickableExerciseCard(
            "🏃 Cardio & Endurance",
            needsPhysique ? "PRIORITÉ HAUTE" : "Maintien",
            "#FF6B6B",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&h=300&fit=crop",
            stats, playerName, "cardio"
        );
        
        // Exercise 2: Strength
        VBox strength = createClickableExerciseCard(
            "💪 Musculation",
            needsPhysique ? "PRIORITÉ HAUTE" : "Maintien",
            "#4ECDC4",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop",
            stats, playerName, "strength"
        );
        
        // Exercise 3: Technical
        VBox technical = createClickableExerciseCard(
            "⚽ Technique",
            needsTechnique ? "PRIORITÉ HAUTE" : "Maintien",
            "#45B7D1",
            "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&h=300&fit=crop",
            stats, playerName, "technical"
        );
        
        // Exercise 4: Tactical
        VBox tactical = createClickableExerciseCard(
            "🎯 Tactique",
            needsTactique ? "PRIORITÉ HAUTE" : "Maintien",
            "#96CEB4",
            "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=400&h=300&fit=crop",
            stats, playerName, "tactical"
        );
        
        // Exercise 5: Recovery
        VBox recovery = createClickableExerciseCard(
            "🧘 Récupération",
            "Essentiel",
            "#FFEAA7",
            "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&h=300&fit=crop",
            stats, playerName, "recovery"
        );
        
        // Exercise 6: Match
        VBox match = createClickableExerciseCard(
            "🏆 Match Simulé",
            "2x par semaine",
            "#DFE6E9",
            "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=400&h=300&fit=crop",
            stats, playerName, "match"
        );
        
        exerciseGrid.add(cardio, 0, 0);
        exerciseGrid.add(strength, 1, 0);
        exerciseGrid.add(technical, 2, 0);
        exerciseGrid.add(tactical, 0, 1);
        exerciseGrid.add(recovery, 1, 1);
        exerciseGrid.add(match, 2, 1);

        section.getChildren().addAll(sectionTitle, exerciseGrid);
        return section;
    }

    private VBox createInteractiveMealPlans(PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        VBox section = new VBox(20);
        
        Label sectionTitle = new Label("🥗 PLAN NUTRITIONNEL PERSONNALISÉ");
        sectionTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        GridPane mealGrid = new GridPane();
        mealGrid.setHgap(20);
        mealGrid.setVgap(20);
        
        // Calculate calorie needs based on performance
        int baseCalories = 2500;
        int calorieAdjustment = (int)((stats.avgPhysique() - 12) * 100);
        int totalCalories = baseCalories + calorieAdjustment;
        
        // Meal 1: Breakfast
        VBox breakfast = createClickableMealCard(
            "🌅 Petit-Déjeuner",
            String.format("%d kcal", (int)(totalCalories * 0.25)),
            "#FFA07A",
            "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?w=400&h=300&fit=crop",
            stats, playerName, "breakfast", totalCalories
        );
        
        // Meal 2: Pre-Workout
        VBox preWorkout = createClickableMealCard(
            "⚡ Pré-Entraînement",
            String.format("%d kcal", (int)(totalCalories * 0.10)),
            "#98D8C8",
            "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=400&h=300&fit=crop",
            stats, playerName, "preworkout", totalCalories
        );
        
        // Meal 3: Post-Workout
        VBox postWorkout = createClickableMealCard(
            "💪 Post-Entraînement",
            String.format("%d kcal", (int)(totalCalories * 0.15)),
            "#F7DC6F",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop",
            stats, playerName, "postworkout", totalCalories
        );
        
        // Meal 4: Lunch
        VBox lunch = createClickableMealCard(
            "🍽️ Déjeuner",
            String.format("%d kcal", (int)(totalCalories * 0.30)),
            "#BB8FCE",
            "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=400&h=300&fit=crop",
            stats, playerName, "lunch", totalCalories
        );
        
        // Meal 5: Snack
        VBox snack = createClickableMealCard(
            "🍎 Collation",
            String.format("%d kcal", (int)(totalCalories * 0.10)),
            "#85C1E2",
            "https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop",
            stats, playerName, "snack", totalCalories
        );
        
        // Meal 6: Dinner
        VBox dinner = createClickableMealCard(
            "🌙 Dîner",
            String.format("%d kcal", (int)(totalCalories * 0.25)),
            "#F8B88B",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=300&fit=crop",
            stats, playerName, "dinner", totalCalories
        );
        
        mealGrid.add(breakfast, 0, 0);
        mealGrid.add(preWorkout, 1, 0);
        mealGrid.add(postWorkout, 2, 0);
        mealGrid.add(lunch, 0, 1);
        mealGrid.add(snack, 1, 1);
        mealGrid.add(dinner, 2, 1);

        section.getChildren().addAll(sectionTitle, mealGrid);
        return section;
    }

    private VBox createTrainingSection(String trainingContent) {
        VBox section = new VBox(20);
        
        // Section Title
        Label sectionTitle = new Label("🏋️ PLAN D'ENTRAÎNEMENT");
        sectionTitle.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2C3E50;"
        );

        // Exercise Cards Grid
        javafx.scene.layout.GridPane exerciseGrid = new javafx.scene.layout.GridPane();
        exerciseGrid.setHgap(20);
        exerciseGrid.setVgap(20);
        
        // Exercise 1: Running/Cardio
        VBox exercise1 = createExerciseCard(
            "🏃 Cardio & Endurance",
            "Course, HIIT, Sprint",
            "#FF6B6B",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=400&h=300&fit=crop"
        );
        
        // Exercise 2: Strength Training
        VBox exercise2 = createExerciseCard(
            "💪 Musculation",
            "Force & Puissance",
            "#4ECDC4",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop"
        );
        
        // Exercise 3: Technical Skills
        VBox exercise3 = createExerciseCard(
            "⚽ Technique",
            "Dribbles, Passes, Contrôle",
            "#45B7D1",
            "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&h=300&fit=crop"
        );
        
        // Exercise 4: Tactical Training
        VBox exercise4 = createExerciseCard(
            "🎯 Tactique",
            "Positionnement, Stratégie",
            "#96CEB4",
            "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=400&h=300&fit=crop"
        );
        
        // Exercise 5: Flexibility
        VBox exercise5 = createExerciseCard(
            "🧘 Récupération",
            "Étirements, Yoga, Mobilité",
            "#FFEAA7",
            "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&h=300&fit=crop"
        );
        
        // Exercise 6: Match Simulation
        VBox exercise6 = createExerciseCard(
            "🏆 Match Simulé",
            "Jeux réduits, Compétition",
            "#DFE6E9",
            "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=400&h=300&fit=crop"
        );
        
        exerciseGrid.add(exercise1, 0, 0);
        exerciseGrid.add(exercise2, 1, 0);
        exerciseGrid.add(exercise3, 2, 0);
        exerciseGrid.add(exercise4, 0, 1);
        exerciseGrid.add(exercise5, 1, 1);
        exerciseGrid.add(exercise6, 2, 1);

        // Training Details
        TextArea detailsArea = new TextArea(trainingContent);
        detailsArea.setWrapText(true);
        detailsArea.setEditable(false);
        detailsArea.setPrefRowCount(8);
        detailsArea.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-background-color: white;" +
            "-fx-border-color: #E0E0E0;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 20;"
        );

        section.getChildren().addAll(sectionTitle, exerciseGrid, detailsArea);
        return section;
    }

    private VBox createNutritionSection(String nutritionContent) {
        VBox section = new VBox(20);
        
        // Section Title
        Label sectionTitle = new Label("🥗 PROGRAMME NUTRITIONNEL");
        sectionTitle.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2C3E50;"
        );

        // Nutrition Cards Grid
        javafx.scene.layout.GridPane nutritionGrid = new javafx.scene.layout.GridPane();
        nutritionGrid.setHgap(20);
        nutritionGrid.setVgap(20);
        
        // Meal 1: Breakfast
        VBox meal1 = createExerciseCard(
            "🌅 Petit-Déjeuner",
            "Énergie pour la journée",
            "#FFA07A",
            "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?w=400&h=300&fit=crop"
        );
        
        // Meal 2: Pre-Workout
        VBox meal2 = createExerciseCard(
            "⚡ Pré-Entraînement",
            "Boost d'énergie",
            "#98D8C8",
            "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=400&h=300&fit=crop"
        );
        
        // Meal 3: Post-Workout
        VBox meal3 = createExerciseCard(
            "💪 Post-Entraînement",
            "Récupération musculaire",
            "#F7DC6F",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop"
        );
        
        // Meal 4: Lunch
        VBox meal4 = createExerciseCard(
            "🍽️ Déjeuner",
            "Repas équilibré",
            "#BB8FCE",
            "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=400&h=300&fit=crop"
        );
        
        // Meal 5: Snack
        VBox meal5 = createExerciseCard(
            "🍎 Collation",
            "Fruits & Noix",
            "#85C1E2",
            "https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop"
        );
        
        // Meal 6: Dinner
        VBox meal6 = createExerciseCard(
            "🌙 Dîner",
            "Protéines & Légumes",
            "#F8B88B",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=300&fit=crop"
        );
        
        nutritionGrid.add(meal1, 0, 0);
        nutritionGrid.add(meal2, 1, 0);
        nutritionGrid.add(meal3, 2, 0);
        nutritionGrid.add(meal4, 0, 1);
        nutritionGrid.add(meal5, 1, 1);
        nutritionGrid.add(meal6, 2, 1);

        // Nutrition Details
        TextArea detailsArea = new TextArea(nutritionContent);
        detailsArea.setWrapText(true);
        detailsArea.setEditable(false);
        detailsArea.setPrefRowCount(8);
        detailsArea.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-background-color: white;" +
            "-fx-border-color: #E0E0E0;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 20;"
        );

        section.getChildren().addAll(sectionTitle, nutritionGrid, detailsArea);
        return section;
    }

    private VBox createExerciseCard(String title, String subtitle, String color, String imageUrl) {
        VBox card = new VBox(0);
        card.setPrefSize(320, 240);
        card.setMaxSize(320, 240);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
            "-fx-cursor: hand;"
        );

        // Image placeholder with colored background
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(160);
        imagePane.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 15 15 0 0;"
        );
        
        // Try to load image from URL, fallback to colored background with large emoji
        try {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            javafx.scene.image.Image image = new javafx.scene.image.Image(imageUrl, true);
            imageView.setImage(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            
            // Clip to rounded corners
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(320, 160);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imageView.setClip(clip);
            
            imagePane.getChildren().add(imageView);
        } catch (Exception e) {
            // Fallback: show large emoji on colored background
            Label emojiLabel = new Label(title.substring(0, 2)); // Get emoji from title
            emojiLabel.setStyle("-fx-font-size: 60px;");
            imagePane.getChildren().add(emojiLabel);
        }

        // Card content
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2C3E50;"
        );
        
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #7F8C8D;"
        );
        
        content.getChildren().addAll(titleLabel, subtitleLabel);
        card.getChildren().addAll(imagePane, content);
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 25, 0, 0, 10);" +
            "-fx-cursor: hand;" +
            "-fx-scale-x: 1.05;" +
            "-fx-scale-y: 1.05;"
        ));
        
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
            "-fx-cursor: hand;"
        ));
        
        return card;
    }

    private void loadPerformanceData() {
        if (currentUserId == null || analyticsService == null) return;

        try {
            List<Evaluation> evaluations = analyticsService.getPlayerEvaluationHistory(currentUserId);
<<<<<<< HEAD
            double attendanceRate = analyticsService.getAttendanceRate(currentUserId);
            attendanceRateLabel.setText(String.format("%.0f%%", attendanceRate));
            totalEvaluationsLabel.setText(String.valueOf(evaluations.size()));
            
            if (evaluations.isEmpty()) {
                avgOverallLabel.setText("N/A");
                performanceChartContainer.getChildren().clear();
=======
            
            if (evaluations.isEmpty()) {
                totalEvaluationsLabel.setText("0");
                attendanceRateLabel.setText("0%");
                avgOverallLabel.setText("N/A");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                weakAreasBox.getChildren().clear();
                weakAreasBox.getChildren().add(new Label("Aucune évaluation disponible"));
                return;
            }

<<<<<<< HEAD
=======
            // Update stats
            totalEvaluationsLabel.setText(String.valueOf(evaluations.size()));
            
            double attendanceRate = analyticsService.getAttendanceRate(currentUserId);
            attendanceRateLabel.setText(String.format("%.0f%%", attendanceRate));

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            PerformanceAnalyticsService.PerformanceStats stats = analyticsService.calculateStats(evaluations);
            avgOverallLabel.setText(String.format("%.1f", stats.avgOverall()));

            // Update chart
            updatePerformanceChart(evaluations);
            
            // Analyze weak areas
            analyzeWeakAreas(stats);

        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les données de performance: " + e.getMessage());
        }
    }

    private void updatePerformanceChart(List<Evaluation> evaluations) {
        performanceChartContainer.getChildren().clear();

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Évaluation #");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis(0, 20, 2);
        yAxis.setLabel("Score");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Progression");
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(250);
        chart.setAnimated(true);

        XYChart.Series<Number, Number> physiqueSeries = new XYChart.Series<>();
        physiqueSeries.setName("💪 Physique");

        XYChart.Series<Number, Number> techniqueSeries = new XYChart.Series<>();
        techniqueSeries.setName("⚽ Technique");

        XYChart.Series<Number, Number> tactiqueSeries = new XYChart.Series<>();
        tactiqueSeries.setName("🎯 Tactique");

        for (int i = 0; i < evaluations.size(); i++) {
            Evaluation eval = evaluations.get(i);
            physiqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNotePhysique()));
            techniqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNoteTechnique()));
            tactiqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNoteTactique()));
        }

        chart.getData().addAll(physiqueSeries, techniqueSeries, tactiqueSeries);
        performanceChartContainer.getChildren().add(chart);
    }

    private void analyzeWeakAreas(PerformanceAnalyticsService.PerformanceStats stats) {
        weakAreasBox.getChildren().clear();

        double minScore = Math.min(stats.avgPhysique(), Math.min(stats.avgTechnique(), stats.avgTactique()));
        boolean hasWeakness = false;

        if (stats.avgPhysique() == minScore && stats.avgPhysique() < 15) {
            Label label = new Label(String.format("💪 Physique: %.1f/20 - À améliorer", stats.avgPhysique()));
            label.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 13px;");
            weakAreasBox.getChildren().add(label);
            hasWeakness = true;
        }
        if (stats.avgTechnique() == minScore && stats.avgTechnique() < 15) {
            Label label = new Label(String.format("⚽ Technique: %.1f/20 - À améliorer", stats.avgTechnique()));
            label.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 13px;");
            weakAreasBox.getChildren().add(label);
            hasWeakness = true;
        }
        if (stats.avgTactique() == minScore && stats.avgTactique() < 15) {
            Label label = new Label(String.format("🎯 Tactique: %.1f/20 - À améliorer", stats.avgTactique()));
            label.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 13px;");
            weakAreasBox.getChildren().add(label);
            hasWeakness = true;
        }

        if (!hasWeakness) {
            Label label = new Label("✅ Excellentes performances!");
            label.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 13px; -fx-font-weight: bold;");
            weakAreasBox.getChildren().add(label);
        }
    }

    // ==================== END PERFORMANCE METHODS ====================

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

    private VBox createClickableExerciseCard(String title, String priority, String color, String imageUrl, 
                                             PerformanceAnalyticsService.PerformanceStats stats, String playerName, String exerciseType) {
        VBox card = new VBox(0);
        card.setPrefSize(360, 260);
        card.setMaxSize(360, 260);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
            "-fx-cursor: hand;"
        );

        // Image with overlay
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(180);
        imagePane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 15 15 0 0;");
        
        try {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            javafx.scene.image.Image image = new javafx.scene.image.Image(imageUrl, true);
            imageView.setImage(image);
            imageView.setFitWidth(360);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.setOpacity(0.9);
            imagePane.getChildren().add(imageView);
        } catch (Exception e) {
            Label emojiLabel = new Label(title.substring(0, 2));
            emojiLabel.setStyle("-fx-font-size: 70px;");
            imagePane.getChildren().add(emojiLabel);
        }
        
        // Priority badge
        Label priorityBadge = new Label(priority);
        priorityBadge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 5 12;" +
            "-fx-background-radius: 15;"
        );
        StackPane.setAlignment(priorityBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priorityBadge, new Insets(10, 10, 0, 0));
        imagePane.getChildren().add(priorityBadge);

        // Card content
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        
        Label clickLabel = new Label("👆 Cliquez pour voir le plan détaillé");
        clickLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        
        content.getChildren().addAll(titleLabel, clickLabel);
        card.getChildren().addAll(imagePane, content);
        
        // Click handler - Generate AI plan
        card.setOnMouseClicked(e -> showAIExercisePlan(title, color, stats, playerName, exerciseType));
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, " + color + ", 25, 0, 0, 10);" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            );
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
                "-fx-cursor: hand;"
            );
        });
        
        return card;
    }

    private VBox createClickableMealCard(String title, String calories, String color, String imageUrl,
                                        PerformanceAnalyticsService.PerformanceStats stats, String playerName, 
                                        String mealType, int totalCalories) {
        VBox card = new VBox(0);
        card.setPrefSize(360, 260);
        card.setMaxSize(360, 260);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
            "-fx-cursor: hand;"
        );

        // Image
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(180);
        imagePane.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 15 15 0 0;");
        
        try {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            javafx.scene.image.Image image = new javafx.scene.image.Image(imageUrl, true);
            imageView.setImage(image);
            imageView.setFitWidth(360);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.setOpacity(0.9);
            imagePane.getChildren().add(imageView);
        } catch (Exception e) {
            Label emojiLabel = new Label(title.substring(0, 2));
            emojiLabel.setStyle("-fx-font-size: 70px;");
            imagePane.getChildren().add(emojiLabel);
        }
        
        // Calorie badge
        Label calorieBadge = new Label(calories);
        calorieBadge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 15;" +
            "-fx-background-radius: 20;"
        );
        StackPane.setAlignment(calorieBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(calorieBadge, new Insets(10, 10, 0, 0));
        imagePane.getChildren().add(calorieBadge);

        // Card content
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        
        Label clickLabel = new Label("👆 Cliquez pour voir le menu");
        clickLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        
        content.getChildren().addAll(titleLabel, clickLabel);
        card.getChildren().addAll(imagePane, content);
        
        // Click handler - Generate AI meal plan
        card.setOnMouseClicked(e -> showAIMealPlan(title, color, stats, playerName, mealType, totalCalories));
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, " + color + ", 25, 0, 0, 10);" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            );
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
                "-fx-cursor: hand;"
            );
        });
        
        return card;
    }

    private void showAIExercisePlan(String title, String color, PerformanceAnalyticsService.PerformanceStats stats, 
                                   String playerName, String exerciseType) {
        // Show loading
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération IA...");
        loadingAlert.setHeaderText("🤖 Création de votre plan personnalisé");
        loadingAlert.setContentText("Veuillez patienter...");
        loadingAlert.show();

        new Thread(() -> {
            try {
                // Generate SPECIFIC training plan for this exercise type using the new generator
                String specificPlan = generateSpecificExercisePlan(exerciseType, stats, playerName);
                
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showInteractiveChecklist(title, specificPlan, color);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Erreur", "Impossible de générer le plan: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showAIMealPlan(String title, String color, PerformanceAnalyticsService.PerformanceStats stats,
                               String playerName, String mealType, int totalCalories) {
        // Show loading
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération IA...");
        loadingAlert.setHeaderText("🤖 Création de votre menu personnalisé");
        loadingAlert.setContentText("Veuillez patienter...");
        loadingAlert.show();

        new Thread(() -> {
            try {
                // Generate SPECIFIC meal plan for this meal type using the new generator
                String specificPlan = generateSpecificMealPlan(mealType, totalCalories, stats, playerName);
                
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showInteractiveChecklist(title, specificPlan, color);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Erreur", "Impossible de générer le menu: " + e.getMessage());
                });
            }
        }).start();
    }

    // These methods are replaced by generateSpecificExercisePlan and generateSpecificMealPlan

    private String identifyWeakArea(PerformanceAnalyticsService.PerformanceStats stats) {
        if (stats.avgPhysique() < stats.avgTechnique() && stats.avgPhysique() < stats.avgTactique()) {
            return "Physique";
        } else if (stats.avgTechnique() < stats.avgPhysique() && stats.avgTechnique() < stats.avgTactique()) {
            return "Technique";
        } else {
            return "Tactique";
        }
    }

    private String generateSpecificExercisePlan(String exerciseType, PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
<<<<<<< HEAD
        String localPlan = ExercisePlanGenerator.generatePlan(exerciseType, stats, playerName);
        if (aiService == null || !aiService.isConfigured()) {
            return localPlan;
        }

        try {
            String aiPlan = aiService.generateTrainingRecommendations(
                    playerName,
                    stats.avgPhysique(),
                    stats.avgTechnique(),
                    stats.avgTactique(),
                    resolveCurrentAttendanceRate()
            );
            if (aiPlan == null || aiPlan.isBlank()) {
                return localPlan;
            }
            return "RECOMMANDATION IA PERSONNALISEE\n"
                    + "Focus choisi: " + humanizeExerciseType(exerciseType) + "\n\n"
                    + aiPlan.trim()
                    + "\n\nPLAN ACTION DETAILLE\n\n"
                    + localPlan;
        } catch (Exception ignored) {
            return localPlan;
        }
    }

    private String generateSpecificMealPlan(String mealType, int totalCalories, PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        String localPlan = MealPlanGenerator.generatePlan(mealType, totalCalories, stats, playerName);
        if (aiService == null || !aiService.isConfigured()) {
            return localPlan;
        }

        try {
            double trainingFrequency = Math.max(1.0, resolveCurrentAttendanceRate() / 25.0);
            String aiPlan = aiService.generateNutritionAdvice(
                    playerName,
                    stats.avgPhysique(),
                    trainingFrequency,
                    identifyWeakArea(stats)
            );
            if (aiPlan == null || aiPlan.isBlank()) {
                return localPlan;
            }
            return "RECOMMANDATION IA NUTRITIONNELLE\n"
                    + "Menu choisi: " + humanizeMealType(mealType) + "\n\n"
                    + aiPlan.trim()
                    + "\n\nPLAN ACTION DETAILLE\n\n"
                    + localPlan;
        } catch (Exception ignored) {
            return localPlan;
        }
    }

    private double resolveCurrentAttendanceRate() {
        if (analyticsService == null || currentUserId == null) {
            return 0.0;
        }
        try {
            return analyticsService.getAttendanceRate(currentUserId);
        } catch (SQLException ignored) {
            return 0.0;
        }
    }

    private String humanizeExerciseType(String exerciseType) {
        return switch (exerciseType) {
            case "cardio" -> "Cardio et endurance";
            case "strength" -> "Musculation";
            case "technical" -> "Technique";
            case "tactical" -> "Tactique";
            case "recovery" -> "Recuperation et mobilite";
            case "match" -> "Matchs simules";
            default -> "Entrainement personnalise";
        };
    }

    private String humanizeMealType(String mealType) {
        return switch (mealType) {
            case "breakfast" -> "Petit-dejeuner";
            case "preworkout" -> "Pre-entrainement";
            case "postworkout" -> "Post-entrainement";
            case "lunch" -> "Dejeuner";
            case "snack" -> "Collation";
            case "dinner" -> "Diner";
            default -> "Plan nutritionnel";
        };
=======
        return ExercisePlanGenerator.generatePlan(exerciseType, stats, playerName);
    }

    private String generateSpecificMealPlan(String mealType, int totalCalories, PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        return MealPlanGenerator.generatePlan(mealType, totalCalories, stats, playerName);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private void showInteractiveChecklist(String title, String aiContent, String color) {
        // Extract plan type and category from title
        String planType = title.contains("🏋️") || title.contains("🏃") || title.contains("💪") || 
                         title.contains("⚽") || title.contains("🎯") || title.contains("🧘") || title.contains("🏆") 
                         ? "exercise" : "meal";
        String planCategory = extractPlanCategory(title);
        
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setResizable(true);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white;");

        // Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + color + "; -fx-padding: 20;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
<<<<<<< HEAD
        Label aiLabel = new Label(aiService != null && aiService.isConfigured()
                ? "✨ Généré par IA selon vos performances"
                : "Plan généré selon vos performances");
=======
        Label aiLabel = new Label("✨ Généré par IA selon vos performances");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        aiLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-opacity: 0.9;");
        
        header.getChildren().addAll(titleLabel, aiLabel);

        // Scrollable checklist
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        scrollPane.setPrefHeight(500);

        VBox checklistContainer = new VBox(10);
        checklistContainer.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Parse AI content into checklist items
        String[] lines = aiContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("**") || line.startsWith("##") || line.startsWith("⚠️")) {
                // Section header
                if (!line.isEmpty()) {
                    Label sectionLabel = new Label(line.replaceAll("\\*\\*", "").replaceAll("##", ""));
                    sectionLabel.setStyle(
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-padding: 10 0 5 0;"
                    );
                    checklistContainer.getChildren().add(sectionLabel);
                }
            } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                // Checklist item
                HBox checkItem = createChecklistItem(line.substring(1).trim(), color, planType, planCategory);
                checklistContainer.getChildren().add(checkItem);
            } else if (line.matches("^\\d+\\..*")) {
                // Numbered item
                HBox checkItem = createChecklistItem(line.replaceFirst("^\\d+\\.", "").trim(), color, planType, planCategory);
                checklistContainer.getChildren().add(checkItem);
            } else if (line.length() > 10) {
                // Regular text
                Label textLabel = new Label(line);
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2C3E50; -fx-padding: 5 0;");
                checklistContainer.getChildren().add(textLabel);
            }
        }

        scrollPane.setContent(checklistContainer);

        // Footer
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        Button closeButton = new Button("✓ Fermer");
        closeButton.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 30;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialog.close());
        
        footer.getChildren().add(closeButton);

        root.getChildren().addAll(header, scrollPane, footer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 700, 650);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private String extractPlanCategory(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("cardio")) return "cardio";
        if (lower.contains("musculation")) return "strength";
        if (lower.contains("technique")) return "technical";
        if (lower.contains("tactique")) return "tactical";
        if (lower.contains("récupération")) return "recovery";
        if (lower.contains("match")) return "match";
        if (lower.contains("déjeuner") || lower.contains("breakfast")) return "breakfast";
        if (lower.contains("pré-entraînement") || lower.contains("preworkout")) return "preworkout";
        if (lower.contains("post-entraînement") || lower.contains("postworkout")) return "postworkout";
        if (lower.contains("lunch")) return "lunch";
        if (lower.contains("collation") || lower.contains("snack")) return "snack";
        if (lower.contains("dîner") || lower.contains("dinner")) return "dinner";
        return "general";
    }

    private HBox createChecklistItem(String text, String color, String planType, String planCategory) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
            "-fx-padding: 10;" +
            "-fx-background-color: #f8f9fa;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );

        // Checkbox
        javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox();
        checkBox.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-text-fill: " + color + ";"
        );

        // Text label
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(580);
        textLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");

        // Load saved state
        try {
            AiChecklistProgressService progressService = new AiChecklistProgressService();
            AiChecklistProgress saved = progressService.findByUserAndItem(currentUserId, planType, planCategory, text);
            if (saved != null && saved.getIsCompleted()) {
                checkBox.setSelected(true);
            }
        } catch (Exception e) {
            // Ignore errors loading saved state
        }

        // Strike-through when checked + SAVE TO DATABASE
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                textLabel.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-text-fill: #7F8C8D;" +
                    "-fx-strikethrough: true;" +
                    "-fx-opacity: 0.6;"
                );
                item.setStyle(
                    "-fx-padding: 10;" +
                    "-fx-background-color: #e8f5e9;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 8;"
                );
            } else {
                textLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");
                item.setStyle(
                    "-fx-padding: 10;" +
                    "-fx-background-color: #f8f9fa;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 8;"
                );
            }
            
            // Save to database in background
            new Thread(() -> {
                try {
                    AiChecklistProgressService progressService = new AiChecklistProgressService();
                    progressService.toggleCompletion(currentUserId, planType, planCategory, text, newVal);
                } catch (Exception e) {
                    System.err.println("Error saving checklist progress: " + e.getMessage());
                }
            }).start();
        });

        item.getChildren().addAll(checkBox, textLabel);
        return item;
    }
}
