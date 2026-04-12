package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegisterController {

    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     emailField;
    @FXML private TextField     telephoneField;
    @FXML private DatePicker    dateNaissancePicker;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;
    @FXML private Button        registerBtn;
    @FXML private Button        goLoginBtn;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // ── appelé par le bouton "Create Account" (onAction="#handleRegister") ─
    @FXML
    private void handleRegister() {
        String nom              = nomField.getText().trim();
        String prenom           = prenomField.getText().trim();
        String email            = emailField.getText().trim();
        String telephone        = telephoneField.getText().trim();
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String password         = passwordField.getText().trim();
        String confirmPassword  = confirmPasswordField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()
                || telephone.isEmpty() || password.isEmpty()
                || confirmPassword.isEmpty() || dateNaissance == null) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (!email.contains("@")) {
            showError("Adresse email invalide.");
            return;
        }
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            confirmPasswordField.clear();
            return;
        }

        User newUser = new User(
                email, "[\"ROLE_USER\"]", password,
                nom, prenom, telephone,
                dateNaissance, "default.png", "actif",
                LocalDateTime.now(), null, LocalDateTime.now()
        );

        try {
            userService.addUser(newUser);   // ← appel addUser
            showSuccess();
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/views/login.fxml")
            );
            Stage stage = (Stage) goLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 860, 480));
            stage.setTitle("Sport Insight — Sign In");
            stage.show();
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess() {
        errorLabel.setStyle("-fx-text-fill: #6bffb8; -fx-font-size: 12px;");
        errorLabel.setText("✔  Compte créé avec succès !");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        nomField.clear(); prenomField.clear(); emailField.clear();
        telephoneField.clear(); dateNaissancePicker.setValue(null);
        passwordField.clear(); confirmPasswordField.clear();
        new Thread(() -> {
            try { Thread.sleep(2000); }
            catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::handleGoLogin);
        }).start();
    }
}