package tn.esprit.Controller;

import javafx.application.Platform;
<<<<<<< HEAD
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
=======
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
<<<<<<< HEAD
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.i18n.I18n;
import tn.esprit.gui.LiveMatchNotificationRuntime;
=======
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.User;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
<<<<<<< HEAD
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchFollowTargetService;
import tn.esprit.services.MatchsService;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import tn.esprit.services.UserService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
<<<<<<< HEAD
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProfileController {
    private static final int MAX_MATCH_SUGGESTIONS = 6;
=======
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class ProfileController {
    private static final DateTimeFormatter MEMBER_SINCE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

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
    private ToggleButton themeToggleButton;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private Label roleValueLabel;
    @FXML
    private Label statusValueLabel;
    @FXML
    private Label memberSinceValueLabel;
    @FXML
    private Label profileInitialsLabel;
    @FXML
    private ImageView profileImageView;
    @FXML
    private StackPane profileImageFallback;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField emailField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField telephoneField;
    @FXML
    private DatePicker dateNaissancePicker;
    @FXML
    private TextField photoPathField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button saveButton;
<<<<<<< HEAD
    @FXML
    private ComboBox<String> favouriteLeagueComboBox;
    @FXML
    private Button addFavouriteLeagueButton;
    @FXML
    private FlowPane favouriteLeagueChipPane;
    @FXML
    private ComboBox<EquipeSelectionItem> favouriteTeamComboBox;
    @FXML
    private Button addFavouriteTeamButton;
    @FXML
    private FlowPane favouriteTeamChipPane;
    @FXML
    private FlowPane favouriteMatchChipPane;
    @FXML
    private VBox matchSuggestionContainer;
    @FXML
    private Label favouritesSummaryLabel;

    private SidebarModuleGroup sidebarModuleGroup;
    private UserService userService;
    private MatchFollowTargetService matchFollowTargetService;
    private EquipeService equipeService;
    private MatchsService matchsService;
    private User currentUser;
    private final ObservableList<EquipeSelectionItem> teamOptions = FXCollections.observableArrayList();
    private final Map<Integer, EquipeSelectionItem> teamOptionsById = new LinkedHashMap<>();
=======

    private SidebarModuleGroup sidebarModuleGroup;
    private UserService userService;
    private User currentUser;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        configureSidebar();
        hideStatus();
        applyCircularImageClip(profileImageView);

        if (dateNaissancePicker != null) {
            dateNaissancePicker.setEditable(false);
            Platform.runLater(this::refreshBirthDatePickerSkin);
        }
        if (photoPathField != null) {
            photoPathField.textProperty().addListener((obs, oldValue, newValue) ->
                    refreshPhotoPreview(newValue, buildDisplayName(prenomField == null ? null : prenomField.getText(),
                             nomField == null ? null : nomField.getText(),
                             emailField == null ? null : emailField.getText()))
            );
        }
        if (prenomField != null) {
            prenomField.textProperty().addListener((obs, oldValue, newValue) -> updatePreviewIdentity());
        }
        if (nomField != null) {
            nomField.textProperty().addListener((obs, oldValue, newValue) -> updatePreviewIdentity());
        }
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldValue, newValue) -> updatePreviewIdentity());
        }

        currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            saveButton.setDisable(true);
<<<<<<< HEAD
            configureFavouriteControlsDisabled();
            showStatus(I18n.get("profile.status.sessionExpired"), "status-error");
=======
            showStatus("Your session expired. Please sign in again.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }

        try {
            userService = new UserService();
<<<<<<< HEAD
            matchFollowTargetService = new MatchFollowTargetService();
            equipeService = new EquipeService();
            matchsService = new MatchsService();
            configureFavouriteControls();
            loadFavouriteTeamOptions();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            User freshUser = userService.getById(currentUser.getId());
            if (freshUser != null) {
                currentUser = freshUser;
                AuthSession.login(freshUser);
            }
            populateProfile(currentUser);
<<<<<<< HEAD
            refreshFavouriteChips();
        } catch (Exception ex) {
            saveButton.setDisable(true);
            configureFavouriteControlsDisabled();
            populateProfile(currentUser);
            showStatus(I18n.get("profile.status.serviceUnavailable"), "status-error");
=======
        } catch (Exception ex) {
            saveButton.setDisable(true);
            populateProfile(currentUser);
            showStatus("The profile service is unavailable right now.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private void refreshBirthDatePickerSkin() {
        if (dateNaissancePicker == null) {
            return;
        }

        String wrapperStyle = "-fx-background-color: #12233f;"
                + "-fx-control-inner-background: #12233f;"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: rgba(96, 165, 250, 0.22);"
                + "-fx-border-radius: 16;"
                + "-fx-border-width: 1;";
        String displayStyle = "-fx-background-color: #12233f;"
                + "-fx-control-inner-background: #12233f;"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 16 0 0 16;"
                + "-fx-text-fill: #f8fafc;"
                + "-fx-prompt-text-fill: #94a3b8;";
        String arrowButtonStyle = "-fx-background-color: #0b1730;"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 0 16 16 0;";
        String arrowStyle = "-fx-background-color: #60a5fa;";

        dateNaissancePicker.setStyle(wrapperStyle);

        Node displayNode = dateNaissancePicker.lookup(".date-picker-display-node");
        if (displayNode != null) {
            displayNode.setStyle(displayStyle);
        }

        Node textFieldNode = dateNaissancePicker.lookup(".text-field");
        if (textFieldNode != null) {
            textFieldNode.setStyle(displayStyle);
        }

        Node arrowButtonNode = dateNaissancePicker.lookup(".arrow-button");
        if (arrowButtonNode != null) {
            arrowButtonNode.setStyle(arrowButtonStyle);
        }

        Node arrowNode = dateNaissancePicker.lookup(".arrow");
        if (arrowNode != null) {
            arrowNode.setStyle(arrowStyle);
        }
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox,
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton,
                "/tn/esprit/views/equipe-competitions-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton,
                "/tn/esprit/views/match-competitions-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton,
                "/tn/esprit/views/league-competitions-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton,
                "/tn/esprit/views/joueur-crud-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton,
                "/tn/esprit/views/annonce-user-view.fxml",
                "/tn/esprit/styles/annonce-theme.css",
                "Anonce | Sport Insight");
    }

    @FXML
    private void handleBrowsePhoto() {
        FileChooser chooser = new FileChooser();
<<<<<<< HEAD
        chooser.setTitle(I18n.get("profile.photo.dialogTitle"));
=======
        chooser.setTitle("Select a profile picture");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );

        Window window = saveButton != null && saveButton.getScene() != null ? saveButton.getScene().getWindow() : null;
        File selectedFile = chooser.showOpenDialog(window);
        if (selectedFile == null) {
            return;
        }

        photoPathField.setText(selectedFile.getAbsolutePath());
    }

    @FXML
    private void handleSaveProfile() {
        hideStatus();

        if (currentUser == null) {
<<<<<<< HEAD
            showStatus(I18n.get("profile.status.noAuthenticatedProfile"), "status-error");
            return;
        }
        if (userService == null) {
            showStatus(I18n.get("profile.status.serviceUnavailable"), "status-error");
=======
            showStatus("No authenticated profile was found.", "status-error");
            return;
        }
        if (userService == null) {
            showStatus("The profile service is not available.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }

        String email = clean(emailField.getText());
        String prenom = clean(prenomField.getText());
        String nom = clean(nomField.getText());
        String telephone = clean(telephoneField.getText());
        String photoPath = clean(photoPathField.getText());
        String newPassword = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email == null || prenom == null || nom == null) {
<<<<<<< HEAD
            showStatus(I18n.get("profile.status.emailNameRequired"), "status-error");
            return;
        }
        if (telephone == null) {
            showStatus(I18n.get("common.validation.phoneRequired"), "status-error");
            return;
        }
        if (dateNaissancePicker.getValue() == null) {
            showStatus(I18n.get("common.validation.birthDateRequired"), "status-error");
=======
            showStatus("Email, first name, and last name are required.", "status-error");
            return;
        }
        if (telephone == null) {
            showStatus("Telephone is required.", "status-error");
            return;
        }
        if (dateNaissancePicker.getValue() == null) {
            showStatus("Birth date is required.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            return;
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) {
<<<<<<< HEAD
                showStatus(I18n.get("profile.status.passwordTooShort"), "status-error");
                return;
            }
            if (!Objects.equals(newPassword, confirmPassword)) {
                showStatus(I18n.get("common.validation.passwordMismatch"), "status-error");
=======
                showStatus("New passwords must contain at least 8 characters.", "status-error");
                return;
            }
            if (!Objects.equals(newPassword, confirmPassword)) {
                showStatus("Password confirmation does not match.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                return;
            }
        }

        try {
            if (userService.emailExists(email, currentUser.getId())) {
<<<<<<< HEAD
                showStatus(I18n.get("profile.status.emailInUse"), "status-error");
=======
                showStatus("Another account already uses this email address.", "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                return;
            }

            currentUser.setEmail(email);
            currentUser.setPrenom(prenom);
            currentUser.setNom(nom);
            currentUser.setTelephone(telephone);
            currentUser.setDateNaissance(dateNaissancePicker.getValue());
            currentUser.setPhoto(photoPath);
            currentUser.setUpdatedAt(LocalDateTime.now());
            if (newPassword != null && !newPassword.isBlank()) {
                currentUser.setPassword(newPassword);
            }

            userService.update(currentUser);
            User refreshedUser = userService.getById(currentUser.getId());
            if (refreshedUser != null) {
                currentUser = refreshedUser;
                AuthSession.login(refreshedUser);
            } else {
                AuthSession.login(currentUser);
            }

            passwordField.clear();
            confirmPasswordField.clear();
            populateProfile(currentUser);
<<<<<<< HEAD
            showStatus(I18n.get("profile.status.updated"), "status-success");
        } catch (Exception ex) {
            showStatus(I18n.format("profile.status.updateFailed", ex.getMessage()), "status-error");
        }
    }

    @FXML
    private void handleAddFavouriteLeague() {
        hideStatus();

        if (currentUser == null || currentUser.getId() == null || matchFollowTargetService == null) {
            showStatus(I18n.get("profile.alerts.statusUnavailable"), "status-error");
            return;
        }

        String selectedLabel = favouriteLeagueComboBox == null ? null : favouriteLeagueComboBox.getValue();
        String competitionCode = resolveCompetitionCode(selectedLabel);
        if (competitionCode == null) {
            showStatus(I18n.get("profile.alerts.chooseLeague"), "status-error");
            return;
        }

        try {
            boolean added = matchFollowTargetService.addCompetitionFavorite(currentUser.getId(), competitionCode);
            refreshFavouriteChips();
            LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            showStatus(added
                    ? I18n.format("profile.alerts.leagueAdded", selectedLabel)
                    : I18n.format("profile.alerts.leagueAlreadyAdded", selectedLabel),
                    added ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.leagueAddFailed", e.getMessage()), "status-error");
        }
    }

    @FXML
    private void handleAddFavouriteTeam() {
        hideStatus();

        if (currentUser == null || currentUser.getId() == null || matchFollowTargetService == null) {
            showStatus(I18n.get("profile.alerts.statusUnavailable"), "status-error");
            return;
        }

        EquipeSelectionItem selectedTeam = favouriteTeamComboBox == null ? null : favouriteTeamComboBox.getValue();
        if (selectedTeam == null || selectedTeam.teamId() == null) {
            showStatus(I18n.get("profile.alerts.chooseTeam"), "status-error");
            return;
        }

        try {
            boolean added = matchFollowTargetService.addTeamFavorite(currentUser.getId(), selectedTeam.teamId());
            refreshFavouriteChips();
            LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            showStatus(added
                    ? I18n.format("profile.alerts.teamAdded", selectedTeam.label())
                    : I18n.format("profile.alerts.teamAlreadyAdded", selectedTeam.label()),
                    added ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.teamAddFailed", e.getMessage()), "status-error");
=======
            showStatus("Your profile has been updated successfully.", "status-success");
        } catch (Exception ex) {
            showStatus("The profile could not be updated. " + ex.getMessage(), "status-error");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
    }

<<<<<<< HEAD
    private void configureFavouriteControls() {
        if (favouriteLeagueComboBox != null) {
            favouriteLeagueComboBox.setItems(FXCollections.observableArrayList(
                    tn.esprit.services.football.FootballDataCompetitions.DEFAULT_CODES.stream()
                            .map(tn.esprit.services.football.FootballDataCompetitions::labelOf)
                            .toList()
            ));
        }

        if (favouriteTeamComboBox != null) {
            favouriteTeamComboBox.setItems(teamOptions);
            favouriteTeamComboBox.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(EquipeSelectionItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.displayLabel());
                }
            });
            favouriteTeamComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(EquipeSelectionItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.displayLabel());
                }
            });
        }

        if (favouritesSummaryLabel != null) {
            favouritesSummaryLabel.setText(I18n.get("profile.alerts.summary"));
        }
    }

    private void configureFavouriteControlsDisabled() {
        if (favouriteLeagueComboBox != null) {
            favouriteLeagueComboBox.setDisable(true);
        }
        if (addFavouriteLeagueButton != null) {
            addFavouriteLeagueButton.setDisable(true);
        }
        if (favouriteTeamComboBox != null) {
            favouriteTeamComboBox.setDisable(true);
        }
        if (addFavouriteTeamButton != null) {
            addFavouriteTeamButton.setDisable(true);
        }
        if (favouriteMatchChipPane != null) {
            favouriteMatchChipPane.getChildren().setAll(buildEmptyFavouriteChip(I18n.get("profile.alerts.emptyMatches")));
        }
        if (matchSuggestionContainer != null) {
            matchSuggestionContainer.getChildren().setAll(buildSuggestionEmptyLabel(I18n.get("profile.alerts.emptySuggestionsUnavailable")));
        }
        if (favouritesSummaryLabel != null) {
            favouritesSummaryLabel.setText(I18n.get("profile.alerts.disabledSummary"));
        }
    }

    private void loadFavouriteTeamOptions() throws Exception {
        if (equipeService == null) {
            return;
        }

        teamOptions.clear();
        teamOptionsById.clear();
        equipeService.getAll().stream()
                .filter(team -> team.getId() != null && team.getNom() != null && !team.getNom().isBlank())
                .sorted((left, right) -> left.getNom().compareToIgnoreCase(right.getNom()))
                .forEach(team -> {
                    EquipeSelectionItem item = new EquipeSelectionItem(
                            team.getId(),
                            team.getNom().trim(),
                            tn.esprit.services.football.FootballDataCompetitions.labelOf(team.getCompetitionCode())
                    );
                    teamOptions.add(item);
                    teamOptionsById.put(item.teamId(), item);
                });
    }

    private void refreshFavouriteChips() throws Exception {
        if (currentUser == null || currentUser.getId() == null || matchFollowTargetService == null) {
            configureFavouriteControlsDisabled();
            return;
        }

        Set<String> competitionCodes = matchFollowTargetService.getFollowedCompetitionCodes(currentUser.getId());
        Set<Integer> teamIds = matchFollowTargetService.getFollowedTeamIds(currentUser.getId());
        Set<Integer> matchIds = matchFollowTargetService.getFollowedMatchIds(currentUser.getId());

        renderFavouriteLeagueChips(competitionCodes);
        renderFavouriteTeamChips(teamIds);
        renderFavouriteMatchChips(matchIds);
        renderSuggestedMatchCards(teamIds, competitionCodes, matchIds);
        updateFavouriteSummary();
    }

    private void renderFavouriteLeagueChips(java.util.Set<String> competitionCodes) {
        if (favouriteLeagueChipPane == null) {
            return;
        }

        favouriteLeagueChipPane.getChildren().clear();
        if (competitionCodes == null || competitionCodes.isEmpty()) {
            favouriteLeagueChipPane.getChildren().add(buildEmptyFavouriteChip("No followed leagues yet."));
            return;
        }

        for (String competitionCode : competitionCodes) {
            String label = tn.esprit.services.football.FootballDataCompetitions.labelOf(competitionCode);
            favouriteLeagueChipPane.getChildren().add(buildFavouriteChip(label, () -> removeFavouriteLeague(competitionCode, label)));
        }
    }

    private void renderFavouriteTeamChips(java.util.Set<Integer> teamIds) {
        if (favouriteTeamChipPane == null) {
            return;
        }

        favouriteTeamChipPane.getChildren().clear();
        if (teamIds == null || teamIds.isEmpty()) {
            favouriteTeamChipPane.getChildren().add(buildEmptyFavouriteChip(I18n.get("profile.alerts.emptyTeams")));
            return;
        }

        for (Integer teamId : teamIds) {
            EquipeSelectionItem item = teamOptionsById.get(teamId);
            String label = item == null ? "Team #" + teamId : item.label();
            favouriteTeamChipPane.getChildren().add(buildFavouriteChip(label, () -> removeFavouriteTeam(teamId, label)));
        }
    }

    private void renderFavouriteMatchChips(Set<Integer> matchIds) throws Exception {
        if (favouriteMatchChipPane == null) {
            return;
        }

        favouriteMatchChipPane.getChildren().clear();
        if (matchIds == null || matchIds.isEmpty()) {
            favouriteMatchChipPane.getChildren().add(buildEmptyFavouriteChip(I18n.get("profile.alerts.emptyMatches")));
            return;
        }

        Map<Integer, Matchs> matchesById = loadMatchesById();
        for (Integer matchId : matchIds) {
            Matchs match = matchesById.get(matchId);
            String label = match == null ? "Match #" + matchId : labelForMatch(match);
            favouriteMatchChipPane.getChildren().add(buildFavouriteChip(label, () -> removeFavouriteMatch(matchId, label)));
        }
    }

    private void renderSuggestedMatchCards(Set<Integer> teamIds, Set<String> competitionCodes, Set<Integer> followedMatchIds) throws Exception {
        if (matchSuggestionContainer == null) {
            return;
        }

        matchSuggestionContainer.getChildren().clear();
        if ((teamIds == null || teamIds.isEmpty()) && (competitionCodes == null || competitionCodes.isEmpty())) {
            matchSuggestionContainer.getChildren().add(buildSuggestionEmptyLabel(I18n.get("profile.alerts.emptySuggestionsFollow")));
            return;
        }
        if (matchsService == null) {
            matchSuggestionContainer.getChildren().add(buildSuggestionEmptyLabel(I18n.get("profile.alerts.emptySuggestionsUnavailable")));
            return;
        }

        List<Matchs> suggestions = matchsService.getAll().stream()
                .filter(match -> match != null && match.getId() != null)
                .filter(match -> followedMatchIds == null || !followedMatchIds.contains(match.getId()))
                .filter(this::isUpcomingMatch)
                .filter(match -> isSuggestedByFavorites(match, teamIds, competitionCodes))
                .sorted(Comparator
                        .comparing(this::kickoffOf, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Matchs::getId, Comparator.nullsLast(Integer::compareTo)))
                .limit(MAX_MATCH_SUGGESTIONS)
                .toList();

        if (suggestions.isEmpty()) {
            matchSuggestionContainer.getChildren().add(buildSuggestionEmptyLabel(I18n.get("profile.alerts.emptySuggestionsNone")));
            return;
        }

        for (Matchs suggestion : suggestions) {
            matchSuggestionContainer.getChildren().add(buildSuggestedMatchRow(suggestion));
        }
    }

    private VBox buildFavouriteChip(String label, Runnable removeAction) {
        Label textLabel = new Label(label);
        textLabel.getStyleClass().add("favorite-chip-label");

        Button removeButton = new Button(I18n.get("profile.action.remove"));
        removeButton.getStyleClass().add("favorite-chip-remove");
        removeButton.setOnAction(event -> removeAction.run());

        HBox row = new HBox(8, textLabel, removeButton);
        row.getStyleClass().add("favorite-chip");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    private Label buildEmptyFavouriteChip(String text) {
        Label emptyChip = new Label(text);
        emptyChip.getStyleClass().add("favorite-empty-chip");
        return emptyChip;
    }

    private Label buildSuggestionEmptyLabel(String text) {
        Label emptyLabel = new Label(text);
        emptyLabel.setWrapText(true);
        emptyLabel.getStyleClass().add("match-suggestion-empty");
        return emptyLabel;
    }

    private HBox buildSuggestedMatchRow(Matchs match) {
        String label = labelForMatch(match);

        Label titleLabel = new Label(teamLabel(match.getEquipeDomicileId(), I18n.get("profile.match.home")) + " vs "
                + teamLabel(match.getEquipeExterieurId(), I18n.get("profile.match.away")));
        titleLabel.getStyleClass().add("match-suggestion-title");
        titleLabel.setWrapText(true);

        Label metaLabel = new Label(formatKickoff(match) + " | " + tn.esprit.services.football.FootballDataCompetitions.labelOf(match.getCompetitionCode()));
        metaLabel.getStyleClass().add("match-suggestion-meta");
        metaLabel.setWrapText(true);

        VBox textBox = new VBox(3, titleLabel, metaLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button addButton = new Button(I18n.get("profile.action.notify"));
        addButton.getStyleClass().add("favorite-chip-remove");
        addButton.setOnAction(event -> addSuggestedMatch(match.getId(), label));

        HBox row = new HBox(10, textBox, addButton);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("match-suggestion-row");
        return row;
    }

    private void removeFavouriteLeague(String competitionCode, String label) {
        try {
            boolean removed = matchFollowTargetService.removeCompetitionFavorite(currentUser.getId(), competitionCode);
            refreshFavouriteChips();
            showStatus(removed
                    ? I18n.format("profile.alerts.leagueRemoved", label)
                    : I18n.format("profile.alerts.leagueMissing", label),
                    removed ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.leagueRemoveFailed", e.getMessage()), "status-error");
        }
    }

    private void removeFavouriteTeam(Integer teamId, String label) {
        try {
            boolean removed = matchFollowTargetService.removeTeamFavorite(currentUser.getId(), teamId);
            refreshFavouriteChips();
            showStatus(removed
                    ? I18n.format("profile.alerts.teamRemoved", label)
                    : I18n.format("profile.alerts.teamMissing", label),
                    removed ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.teamRemoveFailed", e.getMessage()), "status-error");
        }
    }

    private void removeFavouriteMatch(Integer matchId, String label) {
        try {
            boolean removed = matchFollowTargetService.removeMatchFavorite(currentUser.getId(), matchId);
            refreshFavouriteChips();
            showStatus(removed
                    ? I18n.format("profile.alerts.matchRemoved", label)
                    : I18n.format("profile.alerts.matchMissing", label),
                    removed ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.matchRemoveFailed", e.getMessage()), "status-error");
        }
    }

    private void addSuggestedMatch(Integer matchId, String label) {
        try {
            boolean added = matchFollowTargetService.addMatchFavorite(currentUser.getId(), matchId);
            refreshFavouriteChips();
            LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            showStatus(added
                    ? I18n.format("profile.alerts.matchAdded", label)
                    : I18n.format("profile.alerts.matchAlreadyAdded", label),
                    added ? "status-success" : "status-muted");
        } catch (Exception e) {
            showStatus(I18n.format("profile.alerts.matchAddFailed", e.getMessage()), "status-error");
        }
    }

    private void updateFavouriteSummary() {
        if (favouritesSummaryLabel == null) {
            return;
        }

        int leagueCount = favouriteLeagueChipPane == null ? 0 : (int) favouriteLeagueChipPane.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .count();
        int teamCount = favouriteTeamChipPane == null ? 0 : (int) favouriteTeamChipPane.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .count();
        int matchCount = favouriteMatchChipPane == null ? 0 : (int) favouriteMatchChipPane.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .count();
        favouritesSummaryLabel.setText(I18n.format("profile.alerts.summaryDynamic", leagueCount, teamCount, matchCount));
    }

    private String resolveCompetitionCode(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        for (String competitionCode : tn.esprit.services.football.FootballDataCompetitions.DEFAULT_CODES) {
            if (label.equalsIgnoreCase(tn.esprit.services.football.FootballDataCompetitions.labelOf(competitionCode))) {
                return competitionCode;
            }
        }
        return null;
    }

    private Map<Integer, Matchs> loadMatchesById() throws Exception {
        Map<Integer, Matchs> matchesById = new LinkedHashMap<>();
        if (matchsService == null) {
            return matchesById;
        }
        for (Matchs match : matchsService.getAll()) {
            if (match != null && match.getId() != null) {
                matchesById.put(match.getId(), match);
            }
        }
        return matchesById;
    }

    private boolean isUpcomingMatch(Matchs match) {
        LocalDateTime kickoff = kickoffOf(match);
        return kickoff != null
                && !kickoff.isBefore(LocalDateTime.now())
                && !isFinishedStatus(match.getStatut());
    }

    private boolean isSuggestedByFavorites(Matchs match, Set<Integer> teamIds, Set<String> competitionCodes) {
        if (match == null) {
            return false;
        }
        if (teamIds != null) {
            if (match.getEquipeDomicileId() != null && teamIds.contains(match.getEquipeDomicileId())) {
                return true;
            }
            if (match.getEquipeExterieurId() != null && teamIds.contains(match.getEquipeExterieurId())) {
                return true;
            }
        }

        String competitionCode = tn.esprit.services.football.FootballDataCompetitions.normalizeCode(match.getCompetitionCode());
        return competitionCode != null && competitionCodes != null && competitionCodes.contains(competitionCode);
    }

    private LocalDateTime kickoffOf(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        LocalTime time = match.getHeureDebut() == null ? LocalTime.MIDNIGHT : match.getHeureDebut();
        return match.getDateMatch().atTime(time);
    }

    private String labelForMatch(Matchs match) {
        if (match == null) {
            return I18n.get("profile.match.label");
        }
        return teamLabel(match.getEquipeDomicileId(), I18n.get("profile.match.home"))
                + " vs "
                + teamLabel(match.getEquipeExterieurId(), I18n.get("profile.match.away"))
                + " | "
                + formatKickoff(match);
    }

    private String teamLabel(Integer teamId, String fallback) {
        if (teamId == null) {
            return fallback;
        }
        EquipeSelectionItem item = teamOptionsById.get(teamId);
        return item == null ? fallback + " #" + teamId : item.label();
    }

    private String formatKickoff(Matchs match) {
        LocalDateTime kickoff = kickoffOf(match);
        return kickoff == null
                ? I18n.get("profile.match.dateTbc")
                : DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", I18n.getLocale()).format(kickoff);
    }

    private boolean isFinishedStatus(String status) {
        return status != null && status.toLowerCase(Locale.ROOT).contains("fini");
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private void populateProfile(User user) {
        if (user == null) {
            return;
        }

        String displayName = user.getDisplayName();
        String roleDisplay = UserRoles.displayName(user.getPrimaryRole());
        String statusDisplay = normalizeStatus(user.getStatut());

        if (profileNameLabel != null) {
            profileNameLabel.setText(displayName);
        }
        if (profileEmailLabel != null) {
<<<<<<< HEAD
            profileEmailLabel.setText(emptyIfBlank(user.getEmail(), I18n.get("profile.value.noEmail")));
=======
            profileEmailLabel.setText(emptyIfBlank(user.getEmail(), "No email"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
        if (roleValueLabel != null) {
            roleValueLabel.setText(roleDisplay);
        }
        if (statusValueLabel != null) {
            statusValueLabel.setText(statusDisplay);
        }
        if (memberSinceValueLabel != null) {
            memberSinceValueLabel.setText(user.getDateInscription() == null
<<<<<<< HEAD
                    ? I18n.get("common.value.unknown")
                    : DateTimeFormatter.ofPattern("dd MMM yyyy", I18n.getLocale()).format(user.getDateInscription()));
        }
        if (formTitleLabel != null) {
            formTitleLabel.setText(AuthSession.isAdmin() ? I18n.get("profile.form.titleAdmin") : I18n.get("profile.form.title"));
        }
        if (formSubtitleLabel != null) {
            formSubtitleLabel.setText(AuthSession.isAdmin()
                    ? I18n.get("profile.form.subtitleAdmin")
                    : I18n.get("profile.form.subtitleUser"));
=======
                    ? "Unknown"
                    : MEMBER_SINCE_FORMATTER.format(user.getDateInscription()));
        }
        if (formTitleLabel != null) {
            formTitleLabel.setText(AuthSession.isAdmin() ? "Admin profile" : "My profile");
        }
        if (formSubtitleLabel != null) {
            formSubtitleLabel.setText(AuthSession.isAdmin()
                    ? "Review your admin account, update your picture, and keep your contact details current."
                    : "Review your account, update your picture, and keep your contact details current.");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }

        emailField.setText(emptyIfBlank(user.getEmail(), ""));
        prenomField.setText(emptyIfBlank(user.getPrenom(), ""));
        nomField.setText(emptyIfBlank(user.getNom(), ""));
        telephoneField.setText(emptyIfBlank(user.getTelephone(), ""));
        dateNaissancePicker.setValue(user.getDateNaissance());
        photoPathField.setText(emptyIfBlank(user.getPhoto(), ""));
        passwordField.clear();
        confirmPasswordField.clear();

        updatePreviewIdentity();
        refreshPhotoPreview(user.getPhoto(), displayName);
    }

    private void updatePreviewIdentity() {
        String displayName = buildDisplayName(prenomField == null ? null : prenomField.getText(),
                nomField == null ? null : nomField.getText(),
                emailField == null ? null : emailField.getText());

        if (profileNameLabel != null) {
            profileNameLabel.setText(displayName);
        }
        if (profileEmailLabel != null) {
<<<<<<< HEAD
            profileEmailLabel.setText(emptyIfBlank(emailField == null ? null : emailField.getText(), I18n.get("profile.value.noEmail")));
=======
            profileEmailLabel.setText(emptyIfBlank(emailField == null ? null : emailField.getText(), "No email"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
        if (profileInitialsLabel != null) {
            profileInitialsLabel.setText(buildInitials(displayName));
        }
    }

    private void refreshPhotoPreview(String rawPath, String displayName) {
        Image image = loadProfileImage(rawPath);
        boolean showImage = image != null;

        if (profileImageView != null) {
            profileImageView.setImage(image);
            profileImageView.setManaged(showImage);
            profileImageView.setVisible(showImage);
        }
        if (profileImageFallback != null) {
            profileImageFallback.setManaged(!showImage);
            profileImageFallback.setVisible(!showImage);
        }
        if (!showImage && profileInitialsLabel != null) {
            profileInitialsLabel.setText(buildInitials(displayName));
        }
    }

    private void applyCircularImageClip(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        Circle clip = new Circle();
        clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));
        imageView.setClip(clip);
    }

    private Image loadProfileImage(String rawPath) {
        String candidate = clean(rawPath);
        if (candidate == null) {
            return null;
        }

        try {
            if (candidate.startsWith("http://") || candidate.startsWith("https://") || candidate.startsWith("file:")) {
                Image image = new Image(candidate, false);
                return image.isError() ? null : image;
            }

            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                Image image = new Image(path.toUri().toString(), false);
                return image.isError() ? null : image;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void showStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains("status-pill")) {
            statusLabel.getStyleClass().add("status-pill");
        }
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
    }

    private String normalizeStatus(String status) {
        String value = emptyIfBlank(status, "ACTIVE");
        if ("ACTIF".equalsIgnoreCase(value)) {
<<<<<<< HEAD
            return I18n.get("common.status.active");
        }
        if ("INACTIF".equalsIgnoreCase(value)) {
            return I18n.get("common.status.inactive");
=======
            return "Active";
        }
        if ("INACTIF".equalsIgnoreCase(value)) {
            return "Inactive";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.substring(0, 1) + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    private String buildDisplayName(String prenom, String nom, String email) {
        String fullName = ((emptyIfBlank(prenom, "") + " " + emptyIfBlank(nom, "")).trim());
        if (!fullName.isBlank()) {
            return fullName;
        }
<<<<<<< HEAD
        return emptyIfBlank(email, I18n.get("profile.user.fallback"));
=======
        return emptyIfBlank(email, "Sport Insight user");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private String buildInitials(String displayName) {
        String safeName = emptyIfBlank(displayName, "Sport Insight");
        String[] parts = safeName.trim().split("\\s+");
        if (parts.length == 1) {
            return safeName.substring(0, Math.min(2, safeName.length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String emptyIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
<<<<<<< HEAD

    private record EquipeSelectionItem(Integer teamId, String label, String competitionLabel) {
        private String displayLabel() {
            return competitionLabel == null || competitionLabel.isBlank()
                    ? label
                    : label + "  |  " + competitionLabel;
        }
    }
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
}
