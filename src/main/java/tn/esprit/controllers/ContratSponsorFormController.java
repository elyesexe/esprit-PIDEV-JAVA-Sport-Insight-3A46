package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Sponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.services.ContratSponsorService;
import tn.esprit.services.SponsorService;
import tn.esprit.services.EquipeService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ContratSponsorFormController {

    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private TextField montantField;
    @FXML
    private TextField descriptionField;
    @FXML
    private ComboBox<String> statutCombo;
    @FXML
    private ComboBox<Sponsor> sponsorCombo;
    @FXML
    private ComboBox<Equipe> equipeCombo;
    @FXML
    private ComboBox<String> statutPaiementCombo;
    @FXML
    private CheckBox notifiedCheck;

    private ContratSponsor contrat;
    private ContratSponsorController parentController;
    private ContratSponsorService contratService = new ContratSponsorService();
    private SponsorService sponsorService = new SponsorService();
    private EquipeService equipeService = new EquipeService();

    @FXML
    public void initialize() {
        loadSponsors();
        loadEquipes();
    }

    private void loadSponsors() {
        try {
            List<Sponsor> sponsors = sponsorService.getAll();
            ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList(sponsors);
            sponsorCombo.setItems(sponsorList);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des sponsors: " + e.getMessage());
        }
    }

    private void loadEquipes() {
        try {
            List<Equipe> equipes = equipeService.getAll();
            ObservableList<Equipe> equipeList = FXCollections.observableArrayList(equipes);
            equipeCombo.setItems(equipeList);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des équipes: " + e.getMessage());
        }
    }

    public void setContrat(ContratSponsor contrat) {
        this.contrat = contrat;
        if (contrat != null) {
            dateDebutPicker.setValue(contrat.getDateDebut());
            dateFinPicker.setValue(contrat.getDateFin());
            montantField.setText(String.valueOf(contrat.getMontant()));
            descriptionField.setText(contrat.getDescription());
            statutCombo.setValue(contrat.getStatut());
            statutPaiementCombo.setValue(contrat.getStatutPaiement());
            notifiedCheck.setSelected(contrat.isNotified());

            // Set selected sponsor and equipe
            for (Sponsor s : sponsorCombo.getItems()) {
                if (s.getId().equals(contrat.getSponsorId())) {
                    sponsorCombo.setValue(s);
                    break;
                }
            }
            for (Equipe e : equipeCombo.getItems()) {
                if (e.getId().equals(contrat.getEquipeId())) {
                    equipeCombo.setValue(e);
                    break;
                }
            }
        }
    }

    public void setParentController(ContratSponsorController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void saveContrat() {
        try {
            LocalDate dateDebut = dateDebutPicker.getValue();
            LocalDate dateFin = dateFinPicker.getValue();
            double montant = Double.parseDouble(montantField.getText().trim());
            String description = descriptionField.getText().trim();
            String statut = statutCombo.getValue();
            String statutPaiement = statutPaiementCombo.getValue();
            boolean notified = notifiedCheck.isSelected();
            Sponsor selectedSponsor = sponsorCombo.getValue();
            Equipe selectedEquipe = equipeCombo.getValue();

            if (dateDebut == null || dateFin == null || description.isEmpty() || statut == null || selectedSponsor == null || selectedEquipe == null) {
                showAlert("Erreur", "Tous les champs sont obligatoires.");
                return;
            }

            if (dateDebut.isAfter(dateFin)) {
                showAlert("Erreur", "La date de début doit être avant la date de fin.");
                return;
            }

            if (contrat == null) {
                // Add new
                contrat = new ContratSponsor(dateDebut, dateFin, montant, description, statut, notified, statutPaiement, selectedSponsor.getId(), selectedEquipe.getId());
                contratService.add(contrat);
            } else {
                // Update
                contrat.setDateDebut(dateDebut);
                contrat.setDateFin(dateFin);
                contrat.setMontant(montant);
                contrat.setDescription(description);
                contrat.setStatut(statut);
                contrat.setNotified(notified);
                contrat.setStatutPaiement(statutPaiement);
                contrat.setSponsorId(selectedSponsor.getId());
                contrat.setEquipeId(selectedEquipe.getId());
                contratService.update(contrat);
            }

            parentController.refreshTable();
            closeWindow();
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Montant doit être un nombre valide.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) dateDebutPicker.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
