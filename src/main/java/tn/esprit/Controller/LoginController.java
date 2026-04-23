package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.services.UserService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;

public class LoginController {

    @FXML private StackPane     authRoot;
    @FXML private ScrollPane    authScrollPane;
    @FXML private ToggleButton  themeToggleButton;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         feedbackLabel;
    @FXML private Button        signInButton;
    @FXML private Button        faceLoginButton;

    private volatile UserService userService;
    private volatile boolean     userServiceLoading;

    // ── Initialise ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, o, selected) -> applyAuthThemeChrome());
        }
        if (authRoot != null) {
            authRoot.sceneProperty().addListener((obs, o, newScene) -> applyAuthThemeChrome());
        }
        hideFeedback();
        Platform.runLater(this::applyAuthThemeChrome);
        userServiceLoading = true;
        showFeedback("Preparing the authentication service...", "auth-feedback-muted");
        signInButton.setDisable(true);
        loadUserServiceAsync();
    }

    // ── Sign in ───────────────────────────────────────────────────────────────

    @FXML
    private void handleSignIn() {
        hideFeedback();
        String email    = clean(emailField.getText());
        String password = passwordField.getText();

        if (email == null) {
            showFeedback("Email is required.", "auth-feedback-error"); return;
        }
        if (password == null || password.isBlank()) {
            showFeedback("Password is required.", "auth-feedback-error"); return;
        }
        if (userService == null) {
            showFeedback(userServiceLoading
                            ? "Authentication is still starting. Please wait a moment and try again."
                            : "The authentication service is unavailable. Please check your database setup.",
                    "auth-feedback-error");
            return;
        }

        try {
            User user = userService.authenticate(email, password);
            if (user == null) {
                showFeedback("Invalid credentials or inactive account.", "auth-feedback-error");
                return;
            }
            AuthSession.login(user);
            SceneNavigator.switchScene(signInButton,
                    "/tn/esprit/views/home-view.fxml",
                    "/tn/esprit/styles/home-theme.css",
                    "Sport Insight | Accueil");
        } catch (SQLException ex) {
            showFeedback("Sign in failed: " + ex.getMessage(), "auth-feedback-error");
        }
    }

    @FXML
    private void handleOpenSignup() {
        SceneNavigator.switchScene(signInButton,
                "/tn/esprit/views/signup-view.fxml",
                "/tn/esprit/styles/auth-theme.css",
                "Sport Insight | Create account");
    }

    // ── Face login ────────────────────────────────────────────────────────────

    @FXML
    private void onFaceLogin() {
        navigateToView("face_login.fxml", "Face Login");
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    @FXML
    private void onForgotPassword() {
        navigateToView("forgot_password.fxml", "Forgot Password");
    }

    // ── Google login ──────────────────────────────────────────────────────────

    @FXML
    private void onGoogleLogin() {
        navigateToView("google_login.fxml", "Google Login");
    }

    // ── Shared navigation helper ──────────────────────────────────────────────

    /**
     * Looks for the FXML in the same folder as your existing views:
     *   tn/esprit/views/{fileName}
     * Make sure face_login.fxml, forgot_password.fxml, google_login.fxml
     * are placed in src/main/resources/tn/esprit/views/
     */
    private void navigateToView(String fileName, String title) {
        try {
            // Try the views folder first (matches your existing structure)
            URL url = getClass().getResource("/tn/esprit/views/" + fileName);

            // Fallback: try /fxml/ in case you placed them there
            if (url == null) {
                url = getClass().getResource("/fxml/" + fileName);
            }

            if (url == null) {
                showFeedback(fileName + " not found. Place it in src/main/resources/tn/esprit/views/",
                        "auth-feedback-error");
                return;
            }

            Parent root  = FXMLLoader.load(url);
            Stage  stage = (Stage) signInButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sport Insight | " + title);
            stage.show();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            showFeedback("Error opening " + fileName + ": " + e.getMessage(), "auth-feedback-error");
        }
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    public void prefillEmail(String email) {
        if (emailField != null && email != null) emailField.setText(email);
    }

    public void showSuccessMessage(String message) {
        showFeedback(message, "auth-feedback-success");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void showFeedback(String message, String styleClass) {
        feedbackLabel.setText(message);
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
        feedbackLabel.getStyleClass().removeAll(
                "auth-feedback-error", "auth-feedback-success", "auth-feedback-muted");
        if (!feedbackLabel.getStyleClass().contains("auth-feedback"))
            feedbackLabel.getStyleClass().add("auth-feedback");
        if (!feedbackLabel.getStyleClass().contains(styleClass))
            feedbackLabel.getStyleClass().add(styleClass);
    }

    private void hideFeedback() {
        feedbackLabel.setText("");
        feedbackLabel.setManaged(false);
        feedbackLabel.setVisible(false);
        feedbackLabel.getStyleClass().removeAll(
                "auth-feedback-error", "auth-feedback-success", "auth-feedback-muted");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void applyAuthThemeChrome() {
        if (authScrollPane != null)
            authScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        if (authRoot != null && authRoot.getScene() != null) {
            String fill = themeToggleButton != null && themeToggleButton.isSelected() ? "#030712" : "#eff6ff";
            authRoot.getScene().setFill(Paint.valueOf(fill));
        }
    }

    private void loadUserServiceAsync() {
        Thread t = new Thread(() -> {
            try {
                UserService service = new UserService();
                Platform.runLater(() -> {
                    userServiceLoading = false;
                    userService = service;
                    if (signInButton != null) signInButton.setDisable(false);
                    hideFeedback();
                });
            } catch (IllegalStateException ex) {
                Platform.runLater(() -> {
                    userServiceLoading = false;
                    if (signInButton != null) signInButton.setDisable(true);
                    String reason = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showFeedback("Database connection failed: " + reason, "auth-feedback-error");
                });
            }
        }, "login-user-service-loader");
        t.setDaemon(true);
        t.start();
    }
}