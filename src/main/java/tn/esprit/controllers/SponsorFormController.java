package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import tn.esprit.entities.Sponsor;
import tn.esprit.services.SponsorService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SponsorFormController {

    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField budgetField;
    @FXML
    private TextField adresseField;
    @FXML
    private TextField logoNameField;
    @FXML
    private Button uploadLogoButton;

    private Sponsor sponsor;
    private SponsorController parentController;
    private SponsorService sponsorService = new SponsorService();
    private String selectedLogoPath = null;

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
        if (sponsor != null) {
            nomField.setText(sponsor.getNom());
            emailField.setText(sponsor.getEmail());
            telephoneField.setText(sponsor.getTelephone());
            budgetField.setText(String.valueOf(sponsor.getBudget()));
            adresseField.setText(sponsor.getAdresse());
            logoNameField.setText(sponsor.getLogoName());
        }
    }

    public void setParentController(SponsorController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void saveSponsor() {
        try {
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim();
            String telephone = telephoneField.getText().trim();
            double budget = Double.parseDouble(budgetField.getText().trim());
            String adresse = adresseField.getText().trim();
            String logoName = logoNameField.getText().trim();

            if (nom.isEmpty() || email.isEmpty()) {
                showAlert("Erreur", "Nom et email sont obligatoires.");
                return;
            }

            if (sponsor == null) {
                // Add new
                sponsor = new Sponsor(nom, email, telephone, budget, logoName, LocalDateTime.now(), adresse);
                sponsorService.add(sponsor);
            } else {
                // Update
                sponsor.setNom(nom);
                sponsor.setEmail(email);
                sponsor.setTelephone(telephone);
                sponsor.setBudget(budget);
                sponsor.setAdresse(adresse);
                sponsor.setLogoName(logoName);
                sponsorService.update(sponsor);
            }

            parentController.refreshTable();
            closeWindow();
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Budget doit être un nombre valide.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    @FXML
    private void uploadLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le logo du sponsor");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog((Stage) logoNameField.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Create logos directory if it doesn't exist
                String logosDir = System.getProperty("user.home") + File.separator + "SportInsightLogos";
                File logosDirectory = new File(logosDir);
                if (!logosDirectory.exists()) {
                    logosDirectory.mkdirs();
                }

                // Copy file to logos directory
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                File destFile = new File(logosDir, fileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Update field
                logoNameField.setText(fileName);
                selectedLogoPath = destFile.getAbsolutePath();
                showAlert("Succès", "Logo téléchargé avec succès:\n" + fileName);
            } catch (IOException e) {
                showAlert("Erreur", "Erreur lors du téléchargement du logo: " + e.getMessage());
            }
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
