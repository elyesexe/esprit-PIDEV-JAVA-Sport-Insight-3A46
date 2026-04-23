package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.EmailService;
import tn.esprit.services.PasswordResetService;
import tn.esprit.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for forgot_password.fxml
 *
 * Three steps shown in a single screen — only one VBox visible at a time:
 *
 *   Step 1 — stepEmailPane:   user enters their email → OTP is sent
 *   Step 2 — stepOtpPane:     user types the 6-digit code from their inbox
 *   Step 3 — stepNewPassPane: user enters and confirms new password
 */
public class ForgotPasswordController implements Initializable {

    // ── Step panes ────────────────────────────────────────────────────────────
    @FXML private VBox stepEmailPane;
    @FXML private VBox stepOtpPane;
    @FXML private VBox stepNewPassPane;

    // ── Step 1 ────────────────────────────────────────────────────────────────
    @FXML private TextField    emailField;
    @FXML private Button       sendOtpBtn;
    @FXML private Label        emailStatusLabel;

    // ── Step 2 ────────────────────────────────────────────────────────────────
    @FXML private TextField    otpField;
    @FXML private Label        otpHintLabel;
    @FXML private Button       verifyOtpBtn;
    @FXML private Button       resendOtpBtn;
    @FXML private Label        otpStatusLabel;

    // ── Step 3 ────────────────────────────────────────────────────────────────
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button        savePasswordBtn;
    @FXML private Label         passStatusLabel;

    // ── Back to login ─────────────────────────────────────────────────────────
    @FXML private Button backToLoginBtn;

    // ── Services ──────────────────────────────────────────────────────────────
    private UserService          userService;
    private PasswordResetService resetService;
    private EmailService         emailService;

    // ── State ─────────────────────────────────────────────────────────────────
    private String pendingEmail;   // email confirmed in step 1
    private String pendingOtp;     // OTP verified in step 2

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            userService   = new UserService();
            resetService  = new PasswordResetService();
            emailService  = new EmailService();
            resetService.purgeExpired();
        } catch (SQLException e) {
            setStatus(emailStatusLabel, "Database unavailable — try again later", "status-error");
        }
        showStep(1);
    }

    // ── Step 1: send OTP ──────────────────────────────────────────────────────

    @FXML
    private void onSendOtp() {
        String email = emailField.getText().trim();
        if (email.isBlank()) {
            setStatus(emailStatusLabel, "Please enter your email address.", "status-error");
            return;
        }

        sendOtpBtn.setDisable(true);
        setStatus(emailStatusLabel, "Sending…", "status-muted");

        // Run DB + SMTP off the FX thread
        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);
                if (user == null) {
                    // Don't reveal whether the email exists — security best practice
                    // We still show "check your inbox" so the UI is identical
                    fxRun(() -> {
                        pendingEmail = email;   // let step 2 show even for unknowns
                        showStep(2);
                        otpHintLabel.setText("If " + email + " is registered, a code was sent.");
                    });
                    return;
                }

                if (!user.isActiveAccount()) {
                    fxRun(() -> {
                        setStatus(emailStatusLabel, "This account is inactive.", "status-error");
                        sendOtpBtn.setDisable(false);
                    });
                    return;
                }

                String otp = resetService.createToken(user.getId());
                emailService.sendPasswordResetOtp(email, user.getPrenom(), otp);

                fxRun(() -> {
                    pendingEmail = email;
                    showStep(2);
                    otpHintLabel.setText("Code sent to " + email + " — check your inbox (also spam).");
                });

            } catch (Exception e) {
                fxRun(() -> {
                    setStatus(emailStatusLabel, "Error: " + e.getMessage(), "status-error");
                    sendOtpBtn.setDisable(false);
                });
            }
        }, "otp-send").start();
    }

    // ── Step 2: verify OTP ────────────────────────────────────────────────────

    @FXML
    private void onVerifyOtp() {
        String code = otpField.getText().trim();
        if (code.length() != 6 || !code.matches("\\d{6}")) {
            setStatus(otpStatusLabel, "Enter the 6-digit code from your email.", "status-error");
            return;
        }

        verifyOtpBtn.setDisable(true);
        setStatus(otpStatusLabel, "Verifying…", "status-muted");

        new Thread(() -> {
            try {
                int userId = resetService.validateToken(pendingEmail, code);
                if (userId < 0) {
                    fxRun(() -> {
                        setStatus(otpStatusLabel, "Invalid or expired code. Try again or resend.", "status-error");
                        verifyOtpBtn.setDisable(false);
                    });
                } else {
                    fxRun(() -> {
                        pendingOtp = code;
                        showStep(3);
                    });
                }
            } catch (SQLException e) {
                fxRun(() -> {
                    setStatus(otpStatusLabel, "Database error: " + e.getMessage(), "status-error");
                    verifyOtpBtn.setDisable(false);
                });
            }
        }, "otp-verify").start();
    }

    @FXML
    private void onResendOtp() {
        // Reset to step 1 with the email pre-filled
        emailField.setText(pendingEmail == null ? "" : pendingEmail);
        otpField.clear();
        sendOtpBtn.setDisable(false);
        showStep(1);
        setStatus(emailStatusLabel, "Enter your email to request a new code.", "status-muted");
    }

    // ── Step 3: save new password ─────────────────────────────────────────────

    @FXML
    private void onSavePassword() {
        String newPass  = newPasswordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (newPass.length() < 8) {
            setStatus(passStatusLabel, "Password must be at least 8 characters.", "status-error");
            return;
        }
        if (!newPass.equals(confirm)) {
            setStatus(passStatusLabel, "Passwords do not match.", "status-error");
            return;
        }

        savePasswordBtn.setDisable(true);
        setStatus(passStatusLabel, "Saving…", "status-muted");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(pendingEmail);
                if (user == null) throw new IllegalStateException("User not found");

                user.setPassword(newPass);
                userService.update(user);
                resetService.markUsed(pendingEmail, pendingOtp);

                // Back on the UI Thread
                fxRun(() -> {
                    setStatus(passStatusLabel, "Password updated! Redirecting to login…", "status-success");

                    // Use PauseTransition instead of a new Thread + Sleep
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                    pause.setOnFinished(e -> goToLogin());
                    pause.play();
                });

            } catch (Exception e) {
                fxRun(() -> {
                    setStatus(passStatusLabel, "Error: " + e.getMessage(), "status-error");
                    savePasswordBtn.setDisable(false);
                });
            }
        }, "pass-save").start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onBackToLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login-view.fxml"));
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            setStatus(emailStatusLabel, "Navigation error: " + e.getMessage(), "status-error");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showStep(int step) {
        stepEmailPane.setVisible(step == 1);   stepEmailPane.setManaged(step == 1);
        stepOtpPane.setVisible(step == 2);     stepOtpPane.setManaged(step == 2);
        stepNewPassPane.setVisible(step == 3); stepNewPassPane.setManaged(step == 3);
    }

    private void setStatus(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().removeAll("status-muted", "status-info",
                                        "status-success", "status-warning", "status-error");
        label.getStyleClass().add(styleClass);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void fxRun(Runnable r) {
        Platform.runLater(r);
    }
}
