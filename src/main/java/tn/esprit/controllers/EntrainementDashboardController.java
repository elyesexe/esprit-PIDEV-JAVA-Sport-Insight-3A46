package tn.esprit.controllers;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EvaluationService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntrainementDashboardController {

    @FXML private Label statusLabel;
    @FXML private TabPane contentTabs;
    @FXML private Tab entrainementTab;
    @FXML private Tab evaluationTab;

    @FXML private DatePicker entrainementDateField;
    @FXML private TextField entrainementStartField;
    @FXML private TextField entrainementEndField;
    @FXML private TextField entrainementTypeField;
    @FXML private TextField entrainementObjectifField;
    @FXML private TextField entrainementLieuField;
    @FXML private ComboBox<UserOption> entrainementCoachField;
    @FXML private TextField entrainementSearchField;
    @FXML private ComboBox<String> entrainementSortBox;
    @FXML private FlowPane availableCoachPane;
    @FXML private Label availableCoachCountLabel;
    @FXML private FlowPane entrainementCardsPane;
    @FXML private Label entrainementCountLabel;
    @FXML private Label connectedUserLabel;
    @FXML private Label participationTrainingContextLabel;
    @FXML private FlowPane participationCardsPane;
    @FXML private FlowPane evaluationCardsPane;
    @FXML private Label evaluationCardsContextLabel;

    @FXML private TextField evaluationPhysiqueField;
    @FXML private TextField evaluationTechniqueField;
    @FXML private TextField evaluationTactiqueField;
    @FXML private TextArea evaluationCommentaireField;
    @FXML private ComboBox<PlayerOption> evaluationJoueurField;
    @FXML private TextField evaluationSearchField;
    @FXML private ComboBox<String> evaluationSortBox;
    @FXML private Label evaluationTrainingContextLabel;
    @FXML private FlowPane evaluationListPane;

    @FXML private ComboBox<String> participationPresenceField;
    @FXML private TextArea participationJustificationField;

    private EntrainementService entrainementService;
    private EvaluationService evaluationService;
    private ParticipationService participationService;
    private UserService userService;
    private ObservableList<UserOption> availableCoachOptions = FXCollections.observableArrayList();
    private ObservableList<PlayerOption> playerOptions = FXCollections.observableArrayList();
    private Entrainement selectedEntrainement;
    private Evaluation selectedEvaluation;
    private Integer focusedEntrainementId;
    private Integer currentUserId;

    @FXML
    public void initialize() {
        try {
            entrainementService = new EntrainementService();
            evaluationService = new EvaluationService();
            participationService = new ParticipationService();
            userService = new UserService();
        } catch (SQLException e) {
            showError("Service initialization failed", e.getMessage());
            return;
        }

        initializeTables();
        initializeFilters();
        initializeSelections();
        initializeUserContext();
        loadAvailableCoaches();
        loadPlayers();
        refreshAllTables();
        setStatus("Interface loaded.");
    }

    private void initializeTables() {
    }

    private void initializeFilters() {
        entrainementSortBox.setItems(FXCollections.observableArrayList("Date", "Type", "Lieu"));
        evaluationSortBox.setItems(FXCollections.observableArrayList("Moyenne", "Note physique", "Note technique", "Note tactique"));
        participationPresenceField.setItems(FXCollections.observableArrayList("Present", "Absent"));
        entrainementCoachField.setCellFactory(listView -> new UserOptionListCell());
        entrainementCoachField.setButtonCell(new UserOptionListCell());
        evaluationJoueurField.setCellFactory(listView -> new PlayerOptionListCell());
        evaluationJoueurField.setButtonCell(new PlayerOptionListCell());
    }

    private void initializeSelections() {
        entrainementCoachField.valueProperty().addListener((obs, oldValue, selected) -> refreshCoachCardsSelection());
    }

    private void initializeUserContext() {
        currentUserId = resolveCurrentUserId();
        refreshConnectedUserLabel();
    }

    @FXML
    private void handleSaveEntrainement() {
        try {
            Entrainement selected = selectedEntrainement;
            Entrainement entrainement = selected == null ? new Entrainement() : selected;
            entrainement.setDateEntrainement(requiredDate(entrainementDateField, "Date"));
            entrainement.setHeureDebut(parseTime(entrainementStartField.getText(), "Heure debut"));
            entrainement.setHeureFin(parseTime(entrainementEndField.getText(), "Heure fin"));
            entrainement.setType(requiredText(entrainementTypeField, "Type"));
            entrainement.setObjectif(requiredText(entrainementObjectifField, "Objectif"));
            entrainement.setLieu(requiredText(entrainementLieuField, "Lieu"));
            entrainement.setEntraineurId(requiredCoachSelection().id());

            if (selected == null) {
                entrainementService.add(entrainement);
                setStatus("Entrainement added.");
            } else {
                entrainementService.update(entrainement);
                setStatus("Entrainement updated.");
            }
            refreshEntrainements();
            clearEntrainementForm();
        } catch (Exception e) {
            showError("Unable to save entrainement", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteEntrainement() {
        Entrainement selected = requireEntrainementSelection();
        if (selected == null || !confirmDelete()) {
            return;
        }
        try {
            deleteLinkedEvaluations(selected.getId());
            deleteLinkedParticipations(selected.getId());
            entrainementService.delete(selected.getId());
            refreshEntrainements();
            refreshEvaluations();
            refreshParticipations();
            clearEntrainementForm();
            setStatus("Entrainement and related participation/evaluation deleted.");
        } catch (Exception e) {
            showError("Unable to delete entrainement", e.getMessage());
        }
    }

    @FXML
    private void handleSearchEntrainement() {
        try {
            String keyword = entrainementSearchField.getText().trim();
            List<Entrainement> results = keyword.isEmpty() ? entrainementService.getAll() : entrainementService.search(keyword);
            populateEntrainementCards(results);
            setStatus(results.size() + " entrainement(s) loaded.");
        } catch (Exception e) {
            showError("Unable to search entrainements", e.getMessage());
        }
    }

    @FXML
    private void handleSortEntrainement() {
        try {
            String sort = entrainementSortBox.getValue();
            List<Entrainement> results = switch (sort == null ? "" : sort) {
                case "Date" -> entrainementService.sortByDate();
                case "Type" -> entrainementService.sortByType();
                case "Lieu" -> entrainementService.sortByLieu();
                default -> entrainementService.getAll();
            };
            populateEntrainementCards(results);
            setStatus("Entrainements sorted.");
        } catch (Exception e) {
            showError("Unable to sort entrainements", e.getMessage());
        }
    }

    @FXML
    private void handleResetEntrainement() {
        clearEntrainementForm();
        refreshEntrainements();
    }

    @FXML
    private void handleSaveEvaluation() {
        try {
            Evaluation selected = selectedEvaluation;
            Evaluation evaluation = selected == null ? new Evaluation() : selected;
            evaluation.setNotePhysique(parseDouble(evaluationPhysiqueField.getText(), "Note physique"));
            evaluation.setNoteTechnique(parseDouble(evaluationTechniqueField.getText(), "Note technique"));
            evaluation.setNoteTactique(parseDouble(evaluationTactiqueField.getText(), "Note tactique"));
            evaluation.setCommentaire(requiredText(evaluationCommentaireField, "Commentaire"));
            evaluation.setEntrainementId(requireFocusedEntrainement());
            evaluation.setJoueurId(requiredPlayerSelection().id());

            if (selected == null) {
                evaluationService.add(evaluation);
                setStatus("Evaluation added.");
            } else {
                evaluationService.update(evaluation);
                setStatus("Evaluation updated.");
            }
            refreshEvaluations();
            clearEvaluationForm();
        } catch (Exception e) {
            showError("Unable to save evaluation", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteEvaluation() {
        Evaluation selected = requireEvaluationSelection();
        if (selected == null || !confirmDelete()) {
            return;
        }
        try {
            evaluationService.delete(selected.getId());
            refreshEvaluations();
            clearEvaluationForm();
            setStatus("Evaluation deleted.");
        } catch (Exception e) {
            showError("Unable to delete evaluation", e.getMessage());
        }
    }

    @FXML
    private void handleSearchEvaluation() {
        try {
            String keyword = evaluationSearchField.getText().trim();
            List<Evaluation> results = keyword.isEmpty() ? getEvaluationSource() : evaluationService.search(keyword);
            if (focusedEntrainementId != null) {
                results = results.stream()
                        .filter(evaluation -> focusedEntrainementId.equals(evaluation.getEntrainementId()))
                        .collect(Collectors.toList());
            }
            populateEvaluationListCards(results);
            setStatus(results.size() + " evaluation(s) loaded.");
        } catch (Exception e) {
            showError("Unable to search evaluations", e.getMessage());
        }
    }

    @FXML
    private void handleSortEvaluation() {
        try {
            String sort = evaluationSortBox.getValue();
            List<Evaluation> results = switch (sort == null ? "" : sort) {
                case "Moyenne" -> evaluationService.sortByMoyenne();
                case "Note physique" -> evaluationService.sortByNotePhysique();
                case "Note technique" -> evaluationService.sortByNoteTechnique();
                case "Note tactique" -> evaluationService.sortByNoteTactique();
                default -> evaluationService.getAll();
            };
            if (focusedEntrainementId != null) {
                results = results.stream()
                        .filter(evaluation -> focusedEntrainementId.equals(evaluation.getEntrainementId()))
                        .collect(Collectors.toList());
            }
            populateEvaluationListCards(results);
            setStatus("Evaluations sorted.");
        } catch (Exception e) {
            showError("Unable to sort evaluations", e.getMessage());
        }
    }

    @FXML
    private void handleResetEvaluation() {
        clearEvaluationForm();
        focusedEntrainementId = null;
        evaluationTrainingContextLabel.setText("All evaluations");
        evaluationCardsContextLabel.setText("No training selected");
        refreshEvaluations();
    }

    @FXML
    private void handleSaveParticipation() {
        try {
            Participation participation = findCurrentUserParticipation(requireFocusedEntrainement());
            boolean isNew = participation == null;
            if (participation == null) {
                participation = new Participation();
            }
            participation.setPresence(requiredCombo(participationPresenceField, "Presence"));
            participation.setJustificationAbsence(optionalText(participationJustificationField));
            participation.setEntrainementId(requireFocusedEntrainement());
            participation.setJoueurId(requireCurrentUserId());

            if (isNew) {
                participationService.add(participation);
                setStatus("Participation added.");
            } else {
                participationService.update(participation);
                setStatus("Participation updated.");
            }
            refreshParticipations();
            refreshEvaluations();
        } catch (Exception e) {
            showError("Unable to save participation", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteParticipation() {
        Participation selected;
        try {
            selected = findCurrentUserParticipation(requireFocusedEntrainement());
        } catch (SQLException e) {
            showError("Unable to load participation", e.getMessage());
            return;
        }
        if (selected == null || !confirmDelete()) {
            return;
        }
        try {
            participationService.delete(selected.getId());
            refreshParticipations();
            clearParticipationForm();
            setStatus("Participation deleted.");
        } catch (Exception e) {
            showError("Unable to delete participation", e.getMessage());
        }
    }

    @FXML
    private void handleSearchParticipation() {
        refreshParticipations();
    }

    @FXML
    private void handleSortParticipation() {
        refreshParticipations();
    }

    @FXML
    private void handleResetParticipation() {
        clearParticipationForm();
        refreshParticipations();
    }

    private void refreshAllTables() {
        refreshEntrainements();
        refreshEvaluations();
        refreshParticipations();
    }

    private void refreshEntrainements() {
        try {
            populateEntrainementCards(entrainementService.getAll());
            refreshCoachCardsSelection();
        } catch (SQLException e) {
            showError("Unable to load entrainements", e.getMessage());
        }
    }

    private void refreshEvaluations() {
        try {
            populateEvaluationListCards(getEvaluationSource());
        } catch (SQLException e) {
            showError("Unable to load evaluations", e.getMessage());
        }
    }

    private void refreshParticipations() {
        try {
            populateParticipationCards();
        } catch (SQLException e) {
            showError("Unable to load participations", e.getMessage());
        }
    }

    private void populateEntrainementForm(Entrainement entrainement) {
        entrainementDateField.setValue(entrainement.getDateEntrainement());
        entrainementStartField.setText(String.valueOf(entrainement.getHeureDebut()));
        entrainementEndField.setText(String.valueOf(entrainement.getHeureFin()));
        entrainementTypeField.setText(entrainement.getType());
        entrainementObjectifField.setText(entrainement.getObjectif());
        entrainementLieuField.setText(entrainement.getLieu());
        selectCoachById(entrainement.getEntraineurId());
    }

    private void populateEvaluationForm(Evaluation evaluation) {
        selectedEvaluation = evaluation;
        evaluationPhysiqueField.setText(String.valueOf(evaluation.getNotePhysique()));
        evaluationTechniqueField.setText(String.valueOf(evaluation.getNoteTechnique()));
        evaluationTactiqueField.setText(String.valueOf(evaluation.getNoteTactique()));
        evaluationCommentaireField.setText(evaluation.getCommentaire());
        selectPlayerById(evaluation.getJoueurId());
    }

    private void clearEntrainementForm() {
        selectedEntrainement = null;
        entrainementDateField.setValue(null);
        entrainementStartField.clear();
        entrainementEndField.clear();
        entrainementTypeField.clear();
        entrainementObjectifField.clear();
        entrainementLieuField.clear();
        entrainementCoachField.getSelectionModel().clearSelection();
        entrainementSearchField.clear();
        entrainementSortBox.getSelectionModel().clearSelection();
        refreshCoachCardsSelection();
        refreshEntrainementCardSelection();
    }

    private void loadPlayers() {
        try {
            List<User> users = userService.getAll();
            List<User> playerUsers = users.stream()
                    .filter(this::isPlayerUser)
                    .collect(Collectors.toList());

            if (playerUsers.isEmpty()) {
                playerUsers = users;
            }

            List<PlayerOption> options = playerUsers.stream()
                    .sorted(Comparator.comparing(User::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(User::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .map(PlayerOption::fromUser)
                    .collect(Collectors.toList());

            playerOptions.setAll(options);
            evaluationJoueurField.setItems(playerOptions);

            if (currentUserId == null && !options.isEmpty()) {
                currentUserId = options.get(0).id();
            }
            refreshConnectedUserLabel();
        } catch (SQLException e) {
            showError("Unable to load players", e.getMessage());
        }
    }

    private void loadAvailableCoaches() {
        try {
            List<UserOption> options = userService.getAll().stream()
                    .filter(this::isCoachUser)
                    .filter(this::isAvailableCoach)
                    .sorted(Comparator.comparing(User::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(User::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .map(UserOption::fromUser)
                    .collect(Collectors.toList());

            availableCoachOptions.setAll(options);
            entrainementCoachField.setItems(availableCoachOptions);
            availableCoachCountLabel.setText(options.size() + " disponible(s)");
            populateCoachCards();
        } catch (SQLException e) {
            availableCoachCountLabel.setText("Unavailable");
            showError("Unable to load available coaches", e.getMessage());
        }
    }

    private boolean isCoachUser(User user) {
        String roles = user.getRoles();
        if (roles == null || roles.isBlank()) {
            return false;
        }

        String value = roles.toLowerCase();
        return value.contains("entraineur")
                || value.contains("entra\u00eeneur")
                || value.contains("coach")
                || value.contains("trainer");
    }

    private boolean isPlayerUser(User user) {
        String roles = user.getRoles();
        if (roles == null || roles.isBlank()) {
            return false;
        }

        String value = roles.toLowerCase();
        return value.contains("player") || value.contains("joueur");
    }

    private boolean isAvailableCoach(User user) {
        String statut = user.getStatut();
        if (statut == null || statut.isBlank()) {
            return true;
        }

        String value = statut.trim().toLowerCase();
        return !value.contains("indisponible")
                && !value.contains("inactive")
                && !value.contains("bloque")
                && !value.contains("blocked")
                && !value.contains("disabled");
    }

    private void populateEntrainementCards(List<Entrainement> entrainements) {
        entrainementCardsPane.getChildren().clear();
        entrainementCountLabel.setText(entrainements.size() + " sessions");

        for (Entrainement entrainement : entrainements) {
            VBox card = new VBox(12);
            card.getStyleClass().add("training-card");
            card.setUserData(entrainement.getId());

            StackPane hero = new StackPane();
            hero.getStyleClass().add("training-card-hero");

            Label heroIcon = new Label(trainingIcon(entrainement.getType()));
            heroIcon.getStyleClass().add("training-card-icon");

            Label heroType = new Label(safeText(entrainement.getType(), "Session"));
            heroType.getStyleClass().add("training-card-type");

            hero.getChildren().addAll(heroIcon, heroType);
            StackPane.setAlignment(heroIcon, Pos.CENTER_LEFT);
            StackPane.setAlignment(heroType, Pos.BOTTOM_LEFT);

            VBox body = new VBox(8);

            Label title = new Label(safeText(entrainement.getObjectif(), "No objectif"));
            title.getStyleClass().add("training-card-title");
            title.setWrapText(true);

            Label schedule = new Label(formatSchedule(entrainement));
            schedule.getStyleClass().add("training-card-schedule");

            Label location = new Label("Lieu: " + safeText(entrainement.getLieu(), "-"));
            location.getStyleClass().add("training-card-meta");

            Label coach = new Label("Coach: " + resolveCoachLabel(entrainement.getEntraineurId()));
            coach.getStyleClass().add("training-card-meta");

            Label idBadge = new Label("Session #" + entrainement.getId());
            idBadge.getStyleClass().add("training-card-badge");

            HBox actions = new HBox(8);
            actions.getStyleClass().add("training-card-actions");

            Button presentButton = new Button("Present");
            presentButton.getStyleClass().addAll("success-button", "training-card-action");
            presentButton.setOnAction(event -> confirmAttendanceForEntrainement(entrainement, "Present"));

            Button absentButton = new Button("Absent");
            absentButton.getStyleClass().addAll("danger-button", "training-card-action");
            absentButton.setOnAction(event -> confirmAttendanceForEntrainement(entrainement, "Absent"));

            Button evaluateButton = new Button("Evaluate");
            evaluateButton.getStyleClass().addAll("primary-button", "training-card-action");
            evaluateButton.setOnAction(event -> openEvaluationForEntrainement(entrainement));

            actions.getChildren().addAll(presentButton, absentButton, evaluateButton);

            body.getChildren().addAll(title, schedule, location, coach, idBadge, actions);
            card.getChildren().addAll(hero, body);
            card.setOnMouseClicked(event -> {
                selectEntrainement(entrainement, false);
            });
            entrainementCardsPane.getChildren().add(card);
        }

        refreshEntrainementCardSelection();
    }

    private void refreshEntrainementCardSelection() {
        Integer selectedId = selectedEntrainement == null ? null : selectedEntrainement.getId();
        entrainementCardsPane.getChildren().forEach(node -> {
            node.getStyleClass().remove("training-card-selected");
            if (selectedId != null && selectedId.equals(node.getUserData())) {
                node.getStyleClass().add("training-card-selected");
            }
        });
    }

    private Entrainement requireEntrainementSelection() {
        if (selectedEntrainement == null) {
            showWarning("Select an entrainement card to delete.");
        }
        return selectedEntrainement;
    }

    private String formatSchedule(Entrainement entrainement) {
        return String.valueOf(entrainement.getDateEntrainement()) + "  |  "
                + String.valueOf(entrainement.getHeureDebut()) + " - " + String.valueOf(entrainement.getHeureFin());
    }

    private String trainingIcon(String type) {
        if (type == null) {
            return "\u25A3";
        }
        String value = type.toLowerCase();
        if (value.contains("tech")) {
            return "\u26BD";
        }
        if (value.contains("phys")) {
            return "\u26A1";
        }
        if (value.contains("tact")) {
            return "\u265F";
        }
        return "\u25A3";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void populateCoachCards() {
        availableCoachPane.getChildren().clear();
        for (UserOption option : availableCoachOptions) {
            VBox card = new VBox(6);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("person-card");
            card.setPadding(new Insets(14));

            Label avatar = new Label(option.initials());
            avatar.getStyleClass().add("person-avatar");

            Label name = new Label(option.fullName());
            name.getStyleClass().add("person-name");

            Label role = new Label("Coach disponible");
            role.getStyleClass().add("person-role");

            Label badge = new Label(option.statusLabel());
            badge.getStyleClass().add("person-badge");

            card.getChildren().addAll(avatar, name, role, badge);
            card.setOnMouseClicked(event -> {
                entrainementCoachField.setValue(option);
                refreshCoachCardsSelection();
            });
            card.setUserData(option.id());
            availableCoachPane.getChildren().add(card);
        }
        refreshCoachCardsSelection();
    }

    private void selectEntrainement(Entrainement entrainement, boolean openEvaluationTab) {
        selectedEntrainement = entrainement;
        populateEntrainementForm(entrainement);
        openParticipationFlowForEntrainement(entrainement, openEvaluationTab);
        refreshEntrainementCardSelection();
    }

    private void confirmAttendanceForEntrainement(Entrainement entrainement, String presence) {
        selectEntrainement(entrainement, false);
        saveQuickParticipation(presence);
    }

    private void openEvaluationForEntrainement(Entrainement entrainement) {
        selectEntrainement(entrainement, true);
        clearEvaluationForm();
    }

    private void openParticipationFlowForEntrainement(Entrainement entrainement, boolean openEvaluationTab) {
        focusedEntrainementId = entrainement.getId();
        String context = "Training #" + entrainement.getId() + " - " + safeText(entrainement.getType(), "Session");

        participationTrainingContextLabel.setText(context);
        evaluationTrainingContextLabel.setText(context);
        evaluationCardsContextLabel.setText(context);

        refreshParticipations();
        refreshEvaluations();

        if (openEvaluationTab) {
            contentTabs.getSelectionModel().select(evaluationTab);
            setStatus("Training selected. Use the form to evaluate players for this training.");
        } else {
            setStatus("Training selected.");
        }
    }

    private void populateParticipationCards() throws SQLException {
        participationCardsPane.getChildren().clear();
        evaluationCardsPane.getChildren().clear();

        if (focusedEntrainementId == null) {
            participationCardsPane.getChildren().add(createInfoCard("Select a training card to mark your attendance.", "neutral-card"));
            evaluationCardsPane.getChildren().add(createInfoCard("Your notes will appear here after selecting a training.", "neutral-card"));
            return;
        }

        if (currentUserId == null) {
            participationCardsPane.getChildren().add(createInfoCard("No connected player found. Launch with `-Dapp.currentUserId=USER_ID` or add a user with ROLE_PLAYER.", "warning-card"));
            evaluationCardsPane.getChildren().add(createInfoCard("Your evaluation needs the connected user ID.", "neutral-card"));
            return;
        }

        Participation participation = findCurrentUserParticipation(focusedEntrainementId);
        if (participation == null) {
            participationCardsPane.getChildren().add(createAttendanceActionCard());
        } else {
            participationPresenceField.setValue(participation.getPresence());
            participationJustificationField.setText(participation.getJustificationAbsence());
            participationCardsPane.getChildren().add(createParticipationStatusCard(participation));
        }

        populateEvaluationCards();
    }

    private Participation findCurrentUserParticipation(Integer entrainementId) throws SQLException {
        if (entrainementId == null || currentUserId == null) {
            return null;
        }
        return participationService.getByEntrainement(entrainementId).stream()
                .filter(participation -> currentUserId.equals(participation.getJoueurId()))
                .findFirst()
                .orElse(null);
    }

    private VBox createAttendanceActionCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("status-card", "neutral-card");

        Label title = new Label("My attendance");
        title.getStyleClass().add("status-card-title");

        Label text = new Label("Choose if you will attend this training.");
        text.getStyleClass().add("status-card-text");
        text.setWrapText(true);

        Button presentButton = new Button("I attend");
        presentButton.getStyleClass().add("success-button");
        presentButton.setOnAction(event -> saveQuickParticipation("Present"));

        Button absentButton = new Button("I can't attend");
        absentButton.getStyleClass().add("danger-button");
        absentButton.setOnAction(event -> saveQuickParticipation("Absent"));

        card.getChildren().addAll(title, text, presentButton, absentButton);
        return card;
    }

    private VBox createParticipationStatusCard(Participation participation) {
        boolean present = participation.getPresence() != null && participation.getPresence().equalsIgnoreCase("Present");
        VBox card = new VBox(10);
        card.getStyleClass().addAll("status-card", present ? "present-card" : "absent-card");

        Label title = new Label(present ? "You are present" : "You are absent");
        title.getStyleClass().add("status-card-title");

        Label text = new Label(present
                ? "Your attendance is confirmed for this training."
                : "Your absence is recorded for this training.");
        text.getStyleClass().add("status-card-text");
        text.setWrapText(true);
        card.getChildren().addAll(title, text);

        if (participation.getJustificationAbsence() != null && !participation.getJustificationAbsence().isBlank()) {
            Label justification = new Label("Justification: " + participation.getJustificationAbsence());
            justification.getStyleClass().add("status-card-text");
            justification.setWrapText(true);
            card.getChildren().add(justification);
        }

        return card;
    }

    private void populateEvaluationCards() throws SQLException {
        evaluationCardsPane.getChildren().clear();
        List<Evaluation> evaluations = evaluationService.getByEntrainement(focusedEntrainementId).stream()
                .filter(evaluation -> currentUserId != null && currentUserId.equals(evaluation.getJoueurId()))
                .collect(Collectors.toList());

        if (evaluations.isEmpty()) {
            evaluationCardsPane.getChildren().add(createInfoCard("No note available yet for this training.", "neutral-card"));
            return;
        }

        for (Evaluation evaluation : evaluations) {
            VBox card = new VBox(8);
            card.getStyleClass().add("note-card");

            Label title = new Label("My evaluation");
            title.getStyleClass().add("note-card-title");

            Label physique = new Label("Physique: " + evaluation.getNotePhysique());
            Label technique = new Label("Technique: " + evaluation.getNoteTechnique());
            Label tactique = new Label("Tactique: " + evaluation.getNoteTactique());
            physique.getStyleClass().add("note-card-text");
            technique.getStyleClass().add("note-card-text");
            tactique.getStyleClass().add("note-card-text");

            card.getChildren().addAll(title, physique, technique, tactique);
            if (evaluation.getCommentaire() != null && !evaluation.getCommentaire().isBlank()) {
                Label commentaire = new Label(evaluation.getCommentaire());
                commentaire.getStyleClass().add("note-card-comment");
                commentaire.setWrapText(true);
                card.getChildren().add(commentaire);
            }

            evaluationCardsPane.getChildren().add(card);
        }
    }

    private List<Evaluation> getEvaluationSource() throws SQLException {
        return focusedEntrainementId != null
                ? evaluationService.getByEntrainement(focusedEntrainementId)
                : evaluationService.getAll();
    }

    private void populateEvaluationListCards(List<Evaluation> evaluations) {
        evaluationListPane.getChildren().clear();
        for (Evaluation evaluation : evaluations) {
            VBox card = new VBox(8);
            card.getStyleClass().add("note-card");
            card.setUserData(evaluation.getId());

            Label title = new Label(resolvePlayerLabel(evaluation.getJoueurId()));
            title.getStyleClass().add("note-card-title");

            Label moyenne = new Label("Moyenne: " + formatAverage(evaluation));
            moyenne.getStyleClass().add("note-card-text");

            Label scores = new Label("P " + evaluation.getNotePhysique()
                    + "  |  T " + evaluation.getNoteTechnique()
                    + "  |  Tac " + evaluation.getNoteTactique());
            scores.getStyleClass().add("note-card-text");

            card.getChildren().addAll(title, moyenne, scores);
            if (evaluation.getCommentaire() != null && !evaluation.getCommentaire().isBlank()) {
                Label commentaire = new Label(evaluation.getCommentaire());
                commentaire.getStyleClass().add("note-card-comment");
                commentaire.setWrapText(true);
                card.getChildren().add(commentaire);
            }

            card.setOnMouseClicked(event -> populateEvaluationForm(evaluation));
            evaluationListPane.getChildren().add(card);
        }
    }

    private String formatAverage(Evaluation evaluation) {
        double average = (evaluation.getNotePhysique() + evaluation.getNoteTechnique() + evaluation.getNoteTactique()) / 3.0;
        return String.format("%.2f", average);
    }

    private VBox createInfoCard(String message, String styleClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("status-card", styleClass);
        Label label = new Label(message);
        label.getStyleClass().add("status-card-text");
        label.setWrapText(true);
        card.getChildren().add(label);
        return card;
    }

    private void saveQuickParticipation(String presence) {
        participationPresenceField.setValue(presence);
        if ("Present".equalsIgnoreCase(presence)) {
            participationJustificationField.clear();
        }
        handleSaveParticipation();
    }

    private void refreshCoachCardsSelection() {
        Integer selectedCoachId = entrainementCoachField.getValue() == null ? null : entrainementCoachField.getValue().id();
        availableCoachPane.getChildren().forEach(node -> {
            node.getStyleClass().remove("person-card-selected");
            if (selectedCoachId != null && selectedCoachId.equals(node.getUserData())) {
                node.getStyleClass().add("person-card-selected");
            }
        });
    }

    private void selectCoachById(Integer coachId) {
        if (coachId == null) {
            entrainementCoachField.getSelectionModel().clearSelection();
        } else {
            availableCoachOptions.stream()
                    .filter(option -> option.id().equals(coachId))
                    .findFirst()
                    .ifPresentOrElse(entrainementCoachField::setValue, () -> entrainementCoachField.getSelectionModel().clearSelection());
        }
        refreshCoachCardsSelection();
    }

    private String resolveCoachLabel(Integer coachId) {
        if (coachId == null) {
            return "";
        }
        return availableCoachOptions.stream()
                .filter(option -> option.id().equals(coachId))
                .findFirst()
                .map(UserOption::fullName)
                .orElse("Coach #" + coachId);
    }

    private String resolvePlayerLabel(Integer playerId) {
        if (playerId == null) {
            return "Unknown player";
        }
        return playerOptions.stream()
                .filter(option -> option.id().equals(playerId))
                .findFirst()
                .map(PlayerOption::fullName)
                .orElse("Player #" + playerId);
    }

    private UserOption requiredCoachSelection() {
        UserOption selected = entrainementCoachField.getValue();
        if (selected == null) {
            throw new IllegalArgumentException("Coach selection is required.");
        }
        return selected;
    }

    private PlayerOption requiredPlayerSelection() {
        PlayerOption selected = evaluationJoueurField.getValue();
        if (selected == null) {
            throw new IllegalArgumentException("Player selection is required.");
        }
        return selected;
    }

    private Evaluation requireEvaluationSelection() {
        if (selectedEvaluation == null) {
            showWarning("Select an evaluation card first.");
        }
        return selectedEvaluation;
    }

    private void selectPlayerById(Integer playerId) {
        if (playerId == null) {
            evaluationJoueurField.getSelectionModel().clearSelection();
            return;
        }
        playerOptions.stream()
                .filter(option -> option.id().equals(playerId))
                .findFirst()
                .ifPresentOrElse(evaluationJoueurField::setValue, () -> evaluationJoueurField.getSelectionModel().clearSelection());
    }

    private Integer requireFocusedEntrainement() {
        if (focusedEntrainementId == null) {
            throw new IllegalArgumentException("Select a training first.");
        }
        return focusedEntrainementId;
    }

    private Integer requireCurrentUserId() {
        if (currentUserId == null) {
            throw new IllegalArgumentException("No connected player ID found.");
        }
        return currentUserId;
    }

    private void clearEvaluationForm() {
        selectedEvaluation = null;
        evaluationPhysiqueField.clear();
        evaluationTechniqueField.clear();
        evaluationTactiqueField.clear();
        evaluationCommentaireField.clear();
        evaluationJoueurField.getSelectionModel().clearSelection();
        evaluationSearchField.clear();
        evaluationSortBox.getSelectionModel().clearSelection();
    }

    private void clearParticipationForm() {
        participationPresenceField.getSelectionModel().clearSelection();
        participationJustificationField.clear();
    }

    private void deleteLinkedEvaluations(int entrainementId) throws SQLException {
        for (Evaluation evaluation : evaluationService.getByEntrainement(entrainementId)) {
            evaluationService.delete(evaluation.getId());
        }
    }

    private void deleteLinkedParticipations(int entrainementId) throws SQLException {
        for (Participation participation : participationService.getByEntrainement(entrainementId)) {
            participationService.delete(participation.getId());
        }
    }

    private <T> ObservableList<T> toObservableList(List<T> items) {
        return FXCollections.observableArrayList(items);
    }

    private Integer resolveCurrentUserId() {
        return parseRuntimeInteger(System.getProperty("app.currentUserId"), System.getenv("APP_CURRENT_USER_ID"));
    }

    private void refreshConnectedUserLabel() {
        if (currentUserId == null) {
            connectedUserLabel.setText("Connected player: not set");
            return;
        }

        playerOptions.stream()
                .filter(option -> option.id().equals(currentUserId))
                .findFirst()
                .ifPresentOrElse(
                        option -> connectedUserLabel.setText("Connected player: " + option.fullName() + " (#" + option.id() + ")"),
                        () -> connectedUserLabel.setText("Connected player ID: " + currentUserId)
                );
    }

    private Integer parseRuntimeInteger(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private LocalTime parseTime(String value, String label) {
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " must use HH:mm format.");
        }
    }

    private Integer parseInt(String value, String label) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid integer.");
        }
    }

    private Integer parseOptionalInt(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return parseInt(value, label);
    }

    private double parseDouble(String value, String label) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }

    private String requiredText(TextField field, String label) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String requiredText(TextArea field, String label) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String optionalText(TextArea field) {
        String value = field.getText();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String requiredCombo(ComboBox<String> comboBox, String label) {
        String value = comboBox.getValue();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private LocalDate requiredDate(DatePicker picker, String label) {
        if (picker.getValue() == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return picker.getValue();
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete the selected item?", ButtonType.YES, ButtonType.CANCEL);
        alert.setHeaderText("Confirm delete");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Validation");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record UserOption(Integer id, String firstName, String lastName, String status) {
        private static UserOption fromUser(User user) {
            return new UserOption(
                    user.getId(),
                    safeValue(user.getPrenom()),
                    safeValue(user.getNom()),
                    safeValue(user.getStatut())
            );
        }

        private String fullName() {
            return (firstName + " " + lastName).trim();
        }

        private String initials() {
            String firstInitial = firstName.isBlank() ? "?" : firstName.substring(0, 1).toUpperCase();
            String lastInitial = lastName.isBlank() ? "" : lastName.substring(0, 1).toUpperCase();
            return firstInitial + lastInitial;
        }

        private String statusLabel() {
            return status.isBlank() ? "Unknown" : status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }

        @Override
        public String toString() {
            return fullName();
        }

        private static String safeValue(String value) {
            return value == null ? "" : value.trim();
        }
    }

    private record PlayerOption(Integer id, String firstName, String lastName) {
        private static PlayerOption fromUser(User user) {
            return new PlayerOption(
                    user.getId(),
                    user.getPrenom() == null ? "" : user.getPrenom().trim(),
                    user.getNom() == null ? "" : user.getNom().trim()
            );
        }

        private String fullName() {
            return (firstName + " " + lastName).trim();
        }
    }

    private static class UserOptionListCell extends javafx.scene.control.ListCell<UserOption> {
        @Override
        protected void updateItem(UserOption item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label name = new Label(item.fullName());
            name.getStyleClass().add("combo-person-name");

            Label meta = new Label(item.statusLabel());
            meta.getStyleClass().add("combo-person-meta");

            VBox content = new VBox(2, name, meta);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(content);
        }
    }

    private static class PlayerOptionListCell extends javafx.scene.control.ListCell<PlayerOption> {
        @Override
        protected void updateItem(PlayerOption item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item.fullName());
        }
    }
}
