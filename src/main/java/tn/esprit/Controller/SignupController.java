package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.UserRoles;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SignupController {
    @FXML
    private StackPane authRoot;
    @FXML
    private ScrollPane authScrollPane;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private DatePicker dateNaissancePicker;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button createAccountButton;

    private UserService userService;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((observable, oldValue, selected) -> applyAuthThemeChrome());
        }
        if (authRoot != null) {
            authRoot.sceneProperty().addListener((observable, oldScene, newScene) -> applyAuthThemeChrome());
        }
        hideFeedback();
        Platform.runLater(this::applyAuthThemeChrome);
        if (dateNaissancePicker != null) {
            dateNaissancePicker.setEditable(false);
        }

        try {
            userService = new UserService();
        } catch (IllegalStateException ex) {
            createAccountButton.setDisable(true);
            showFeedback("The database connection is unavailable. Please check your local setup and try again.", "auth-feedback-error");
        }
    }

    @FXML
    private void handleCreateAccount() {
        hideFeedback();

        String prenom = clean(prenomField.getText());
        String nom = clean(nomField.getText());
        String email = clean(emailField.getText());
        String telephone = clean(telephoneField.getText());
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String password = passwordField.getText();
        String passwordConfirmation = confirmPasswordField.getText();

        if (prenom == null || nom == null) {
            showFeedback("First name and last name are required.", "auth-feedback-error");
            return;
        }
        if (email == null) {
            showFeedback("Email is required.", "auth-feedback-error");
            return;
        }
        if (telephone == null) {
            showFeedback("Telephone is required.", "auth-feedback-error");
            return;
        }
        if (dateNaissance == null) {
            showFeedback("Birth date is required.", "auth-feedback-error");
            return;
        }
        if (password == null || password.length() < 8) {
            showFeedback("Password must contain at least 8 characters.", "auth-feedback-error");
            return;
        }
        if (!password.equals(passwordConfirmation)) {
            showFeedback("The password confirmation does not match.", "auth-feedback-error");
            return;
        }
        if (userService == null) {
            showFeedback("The registration service is not ready yet.", "auth-feedback-error");
            return;
        }

        try {
            if (userService.emailExists(email, null)) {
                showFeedback("An account already exists for this email address.", "auth-feedback-error");
                return;
            }

            User user = new User();
            user.setPrenom(prenom);
            user.setNom(nom);
            user.setEmail(email);
            user.setTelephone(telephone);
            user.setDateNaissance(dateNaissance);
            user.setPassword(password);
            user.setRoleList(List.of(UserRoles.ROLE_USER));
            user.setStatut("ACTIVE");
            user.setPhoto(null);
            user.setCvName(null);
            user.setDateInscription(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userService.add(user);

            SceneNavigator.switchScene(createAccountButton,
                    "/tn/esprit/views/login-view.fxml",
                    "/tn/esprit/styles/auth-theme.css",
                    "Sport Insight | Sign in",
                    controller -> {
                        if (controller instanceof LoginController loginController) {
                            loginController.prefillEmail(email);
                            loginController.showSuccessMessage("Your account has been created. You can sign in now.");
                        }
                    });
        } catch (SQLException ex) {
            showFeedback("Account creation failed because the user record could not be saved.", "auth-feedback-error");
        } catch (IllegalArgumentException ex) {
            showFeedback(ex.getMessage(), "auth-feedback-error");
        }
    }

    @FXML
    private void handleOpenLogin() {
        SceneNavigator.switchScene(createAccountButton,
                "/tn/esprit/views/login-view.fxml",
                "/tn/esprit/styles/auth-theme.css",
                "Sport Insight | Sign in");
    }

    private void showFeedback(String message, String styleClass) {
        feedbackLabel.setText(message);
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
        feedbackLabel.getStyleClass().removeAll("auth-feedback-error", "auth-feedback-success", "auth-feedback-muted");
        if (!feedbackLabel.getStyleClass().contains("auth-feedback")) {
            feedbackLabel.getStyleClass().add("auth-feedback");
        }
        if (!feedbackLabel.getStyleClass().contains(styleClass)) {
            feedbackLabel.getStyleClass().add(styleClass);
        }
    }

    private void hideFeedback() {
        feedbackLabel.setText("");
        feedbackLabel.setManaged(false);
        feedbackLabel.setVisible(false);
        feedbackLabel.getStyleClass().removeAll("auth-feedback-error", "auth-feedback-success", "auth-feedback-muted");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void applyAuthThemeChrome() {
        if (authScrollPane != null) {
            authScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        }
        if (authRoot != null && authRoot.getScene() != null) {
            String fill = themeToggleButton != null && themeToggleButton.isSelected() ? "#030712" : "#eff6ff";
            authRoot.getScene().setFill(Paint.valueOf(fill));
        }
    }
}
