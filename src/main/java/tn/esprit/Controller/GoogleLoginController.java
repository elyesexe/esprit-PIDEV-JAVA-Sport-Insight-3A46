package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
import tn.esprit.services.GoogleOAuthService;
import tn.esprit.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Controller for google_login.fxml
 *
 * This screen is shown while the Google OAuth flow is running.
 * It shows a spinner and status messages, then navigates to the
 * dashboard on success or shows an error with a retry button.
 *
 * The controller auto-starts the OAuth flow on initialize().
 */
public class GoogleLoginController implements Initializable {

    @FXML private Label             statusLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Button            retryBtn;
    @FXML private Button            cancelBtn;

    private GoogleOAuthService oauthService;
    private UserService        userService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try { userService = new UserService(); }
        catch (Exception e) {
            setStatus("Database unavailable.", "status-error");
            return;
        }
        oauthService = new GoogleOAuthService();
        retryBtn.setVisible(false);
        startOAuthFlow();
    }

    // ── OAuth flow ────────────────────────────────────────────────────────────

    private void startOAuthFlow() {
        spinner.setVisible(true);
        retryBtn.setVisible(false);
        setStatus("Opening browser — please sign in with Google…", "status-muted");

        oauthService.startLoginFlow(
            email -> Platform.runLater(() -> onGoogleEmail(email)),
            error -> Platform.runLater(() -> onOAuthError(error))
        );
    }

    private void onGoogleEmail(String email) {
        setStatus("Checking account for " + email + "…", "status-muted");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);

                if (user == null) {
                    // Auto-create account for new Google users
                    user = createGoogleAccount(email);
                }

                if (!user.isActiveAccount()) {
                    Platform.runLater(() -> onOAuthError("Your account is inactive or blocked."));
                    return;
                }

                User finalUser = user;
                Platform.runLater(() -> {
                    AuthSession.setCurrentUser(finalUser);
                    spinner.setVisible(false);
                    setStatus("Welcome, " + finalUser.getDisplayName() + "!", "status-success");
                    navigateToDashboard();
                });

            } catch (SQLException e) {
                Platform.runLater(() -> onOAuthError("Database error: " + e.getMessage()));
            }
        }, "google-login-db").start();
    }

    private void onOAuthError(String message) {
        spinner.setVisible(false);
        setStatus(message, "status-error");
        retryBtn.setVisible(true);
    }

    // ── Auto account creation ─────────────────────────────────────────────────

    /**
     * First-time Google login: create a local account with a random
     * unusable password (the user will always log in via Google or face).
     */
    private User createGoogleAccount(String email) throws SQLException {
        User user = new User();
        user.setEmail(email);

        // Derive a display name from the email prefix
        String prefix = email.contains("@") ? email.split("@")[0] : email;
        user.setPrenom(capitalize(prefix));
        user.setNom("");

        // Impossible-to-use random password (user logs in via Google only)
        user.setPassword(java.util.UUID.randomUUID().toString());

        user.setRoleList(java.util.List.of(UserRoles.ROLE_USER));
        user.setStatut("ACTIVE");
        user.setDateInscription(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userService.add(user);
        return user;
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onRetry() {
        startOAuthFlow();
    }

    @FXML
    private void onCancel() {
        navigateToLogin();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToDashboard() {
        SceneNavigator.switchScene(cancelBtn,
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    private void navigateToLogin() {
        SceneNavigator.switchScene(cancelBtn,
                "/tn/esprit/views/login-view.fxml",
                "/tn/esprit/styles/auth-theme.css",
                "Sport Insight | Sign in");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String msg, String styleClass) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-muted","status-info",
                                              "status-success","status-warning","status-error");
        statusLabel.getStyleClass().add(styleClass);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
