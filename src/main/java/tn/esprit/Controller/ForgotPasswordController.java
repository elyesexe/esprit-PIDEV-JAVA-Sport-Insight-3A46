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

public class ForgotPasswordController implements Initializable {

    @FXML private VBox stepEmailPane;
    @FXML private VBox stepOtpPane;
    @FXML private VBox stepNewPassPane;

    @FXML private TextField    emailField;
    @FXML private Button       sendOtpBtn;
    @FXML private Label        emailStatusLabel;

    @FXML private TextField    otpField;
    @FXML private Label        otpHintLabel;
    @FXML private Button       verifyOtpBtn;
    @FXML private Button       resendOtpBtn;
    @FXML private Label        otpStatusLabel;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button        savePasswordBtn;
    @FXML private Label         passStatusLabel;

    @FXML private Button backToLoginBtn;

    private UserService          userService;
    private PasswordResetService resetService;
    private EmailService         emailService;

    private String pendingEmail;
    private String pendingOtp;

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

        // Limit OTP field to 6 digits only
        otpField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) otpField.setText(newVal.replaceAll("[^\\d]", ""));
            if (newVal.length() > 6)    otpField.setText(newVal.substring(0, 6));
        });

        showStep(1);
    }

    @FXML
    private void onSendOtp() {
        String email = emailField.getText().trim();
        if (email.isBlank()) {
            setStatus(emailStatusLabel, "Please enter your email address.", "status-error");
            return;
        }

        sendOtpBtn.setDisable(true);
        setStatus(emailStatusLabel, "Sending…", "status-muted");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);
                if (user == null) {
                    fxRun(() -> {
                        pendingEmail = email;
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
                    fxRun(() -> { pendingOtp = code; showStep(3); });
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
        emailField.setText(pendingEmail == null ? "" : pendingEmail);
        otpField.clear();
        sendOtpBtn.setDisable(false);
        showStep(1);
        setStatus(emailStatusLabel, "Enter your email to request a new code.", "status-muted");
    }

    @FXML
    private void onSavePassword() {
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (newPass.length() < 8) {
            setStatus(passStatusLabel, "Password must be at least 8 characters.", "status-error"); return;
        }
        if (!newPass.equals(confirm)) {
            setStatus(passStatusLabel, "Passwords do not match.", "status-error"); return;
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
                fxRun(() -> {
                    setStatus(passStatusLabel, "Password updated! Redirecting to login…", "status-success");
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
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

    @FXML
    private void onBackToLogin() { goToLogin(); }

    private void goToLogin() {
        try {
            // ── FIXED: correct path matching your project structure ──────────
            Parent root = FXMLLoader.load(
                    getClass().getResource("/tn/esprit/views/login-view.fxml"));
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            setStatus(emailStatusLabel, "Navigation error: " + e.getMessage(), "status-error");
        }
    }

    private void showStep(int step) {
        stepEmailPane.setVisible(step == 1);   stepEmailPane.setManaged(step == 1);
        stepOtpPane.setVisible(step == 2);     stepOtpPane.setManaged(step == 2);
        stepNewPassPane.setVisible(step == 3); stepNewPassPane.setManaged(step == 3);
    }

    private void setStatus(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().removeAll(
                "status-muted","status-info","status-success","status-warning","status-error");
        label.getStyleClass().add(styleClass);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void fxRun(Runnable r) { Platform.runLater(r); }
}