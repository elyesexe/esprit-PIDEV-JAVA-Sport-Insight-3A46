package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
import tn.esprit.services.UserService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class ProfileController {
    private static final DateTimeFormatter MEMBER_SINCE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

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

    private SidebarModuleGroup sidebarModuleGroup;
    private UserService userService;
    private User currentUser;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        configureSidebar();
        hideStatus();

        if (dateNaissancePicker != null) {
            dateNaissancePicker.setEditable(false);
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
            showStatus("Your session expired. Please sign in again.", "status-error");
            return;
        }

        try {
            userService = new UserService();
            User freshUser = userService.getById(currentUser.getId());
            if (freshUser != null) {
                currentUser = freshUser;
                AuthSession.login(freshUser);
            }
            populateProfile(currentUser);
        } catch (Exception ex) {
            saveButton.setDisable(true);
            populateProfile(currentUser);
            showStatus("The profile service is unavailable right now.", "status-error");
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
        chooser.setTitle("Select a profile picture");
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
            showStatus("No authenticated profile was found.", "status-error");
            return;
        }
        if (userService == null) {
            showStatus("The profile service is not available.", "status-error");
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
            showStatus("Email, first name, and last name are required.", "status-error");
            return;
        }
        if (telephone == null) {
            showStatus("Telephone is required.", "status-error");
            return;
        }
        if (dateNaissancePicker.getValue() == null) {
            showStatus("Birth date is required.", "status-error");
            return;
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) {
                showStatus("New passwords must contain at least 8 characters.", "status-error");
                return;
            }
            if (!Objects.equals(newPassword, confirmPassword)) {
                showStatus("Password confirmation does not match.", "status-error");
                return;
            }
        }

        try {
            if (userService.emailExists(email, currentUser.getId())) {
                showStatus("Another account already uses this email address.", "status-error");
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
            showStatus("Your profile has been updated successfully.", "status-success");
        } catch (Exception ex) {
            showStatus("The profile could not be updated. " + ex.getMessage(), "status-error");
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
            profileEmailLabel.setText(emptyIfBlank(user.getEmail(), "No email"));
        }
        if (roleValueLabel != null) {
            roleValueLabel.setText(roleDisplay);
        }
        if (statusValueLabel != null) {
            statusValueLabel.setText(statusDisplay);
        }
        if (memberSinceValueLabel != null) {
            memberSinceValueLabel.setText(user.getDateInscription() == null
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
            profileEmailLabel.setText(emptyIfBlank(emailField == null ? null : emailField.getText(), "No email"));
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
            return "Active";
        }
        if ("INACTIF".equalsIgnoreCase(value)) {
            return "Inactive";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.substring(0, 1) + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    private String buildDisplayName(String prenom, String nom, String email) {
        String fullName = ((emptyIfBlank(prenom, "") + " " + emptyIfBlank(nom, "")).trim());
        if (!fullName.isBlank()) {
            return fullName;
        }
        return emptyIfBlank(email, "Sport Insight user");
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
}
