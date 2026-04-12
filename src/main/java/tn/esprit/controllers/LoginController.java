package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginBtn;
    @FXML private Button        signUpBtn;

    // ── Sign In → afficher_users ──────────────────────────────────────────
    @FXML
    void handleLogin(ActionEvent event) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/afficher_users.fxml")
            );
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1000, 650));
            stage.setTitle("Sport Insight — Gestion des utilisateurs");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    // ── Sign Up → register ────────────────────────────────────────────────
    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/register.fxml")
            );
            Stage stage = (Stage) signUpBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 860, 580));
            stage.setTitle("Sport Insight — Register");
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }
}