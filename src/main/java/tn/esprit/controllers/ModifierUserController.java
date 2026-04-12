package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.time.LocalDateTime;

public class ModifierUserController {

    @FXML private TextField        nomField;
    @FXML private TextField        prenomField;
    @FXML private TextField        emailField;
    @FXML private TextField        telephoneField;
    @FXML private DatePicker       dateNaissancePicker;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<String> rolesCombo;
    @FXML private Label            errorLabel;
    @FXML private Button           saveBtn;
    @FXML private Button           cancelBtn;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("actif", "inactif");
        rolesCombo.getItems().addAll("[\"ROLE_USER\"]", "[\"ROLE_ADMIN\"]");
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    public void initUser(User u) {
        this.currentUser = u;
        nomField.setText(u.getNom() != null ? u.getNom() : "");
        prenomField.setText(u.getPrenom() != null ? u.getPrenom() : "");
        emailField.setText(u.getEmail() != null ? u.getEmail() : "");
        telephoneField.setText(u.getTelephone() != null ? u.getTelephone() : "");
        dateNaissancePicker.setValue(u.getDateNaissance());
        statutCombo.setValue(u.getStatut() != null ? u.getStatut() : "actif");
        rolesCombo.setValue(u.getRoles() != null ? u.getRoles() : "[\"ROLE_USER\"]");
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (currentUser == null) { showError("Aucun utilisateur chargé."); return; }

        String nom       = nomField.getText().trim();
        String prenom    = prenomField.getText().trim();
        String email     = emailField.getText().trim();
        String telephone = telephoneField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty()) {
            showError("Veuillez remplir tous les champs."); return;
        }
        if (!email.contains("@")) { showError("Adresse email invalide."); return; }

        // ── Seuls les champs modifiables sont mis à jour ───────────────────
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setTelephone(telephone);
        currentUser.setDateNaissance(dateNaissancePicker.getValue());
        currentUser.setStatut(statutCombo.getValue());
        currentUser.setRoles(rolesCombo.getValue());
        currentUser.setUpdatedAt(LocalDateTime.now());

        // ── Champs non modifiables — garder les valeurs originales ─────────
        // password : déjà dans currentUser, on ne touche pas
        if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
            currentUser.setPassword("default123");
        }

        // date_inscription : ne jamais écraser, garder l'original
        if (currentUser.getDateInscription() == null) {
            currentUser.setDateInscription(LocalDateTime.now());
        }

        // updated_at : déjà setté ci-dessus
        if (currentUser.getUpdatedAt() == null) {
            currentUser.setUpdatedAt(LocalDateTime.now());
        }

        userService.updateUser(currentUser);
        System.out.println("✅ Modification effectuée : id=" + currentUser.getId());
        closeWindow();
    }

    @FXML
    void handleCancel(ActionEvent event) { closeWindow(); }

    private void closeWindow() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠  " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            new Alert(Alert.AlertType.ERROR, msg).showAndWait();
        }
    }
}