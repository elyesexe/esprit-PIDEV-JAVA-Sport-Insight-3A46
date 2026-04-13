package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EvaluationService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.UserService;
import tn.esprit.security.AuthSession;

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

    private final ObservableList<Entrainement> master = FXCollections.observableArrayList();
    private final ObservableList<Entrainement> filtered = FXCollections.observableArrayList();
    private final ObservableList<CoachOption> coachOptions = FXCollections.observableArrayList();

    private EntrainementService entrainementService;
    private EvaluationService evaluationService;
    private ParticipationService participationService;
    private UserService userService;
    private Entrainement selected;
    private Integer currentUserId;
    private SidebarModuleGroup sidebarModuleGroup;

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
            loadCoaches();
            resolveCurrentUser();
            refreshData();
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
    private void handleSaveParticipation() {
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez une session avant d'enregistrer la participation.");
            return;
        }
        if (currentUserId == null) {
            showValidation("Aucun joueur connecte.");
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
        clearValidation();
        if (selected == null) {
            showValidation("Selectionnez une session avant de supprimer la participation.");
            return;
        }
        if (currentUserId == null) {
            showValidation("Aucun joueur connecte.");
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
        body.getChildren().addAll(time, place, objectif, coach, badge);

        card.getChildren().addAll(hero, body);
        card.setOnMouseClicked(event -> selectEntrainement(entrainement));
        updateCardSelection(card);
        return card;
    }

    private void updateCardSelection(VBox card) {
        Integer id = (Integer) card.getUserData();
        boolean active = selected != null && Objects.equals(selected.getId(), id);
        card.getStyleClass().remove("training-card-selected");
        if (active) {
            card.getStyleClass().add("training-card-selected");
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

    private void resolveCurrentUser() {
        User current = AuthSession.getCurrentUser();
        currentUserId = current == null ? null : current.getId();
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
                .orElse("Coach #" + coachId);
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

    private void refreshParticipationAndEvaluations() {
        if (selected == null) {
            participationContextLabel.setText("Selectionnez une session pour marquer votre presence.");
            evaluationContextLabel.setText("Aucune evaluation chargee.");
            evaluationCardsPane.getChildren().clear();
            return;
        }
        participationContextLabel.setText("Session #" + selected.getId());
        if (currentUserId == null) {
            evaluationContextLabel.setText("Aucun joueur connecte.");
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
