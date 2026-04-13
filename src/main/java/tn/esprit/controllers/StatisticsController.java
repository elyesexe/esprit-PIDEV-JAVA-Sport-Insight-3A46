package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.entities.Sponsor;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.services.SponsorService;
import tn.esprit.services.ContratSponsorService;
import tn.esprit.utils.StatisticsCalculator;

import java.sql.SQLException;
import java.util.List;

public class StatisticsController {

    @FXML
    private Label sponsorCountLabel;
    @FXML
    private Label totalBudgetLabel;
    @FXML
    private Label averageBudgetLabel;
    @FXML
    private Label topSponsorLabel;
    @FXML
    private Label contratCountLabel;
    @FXML
    private Label activeContratLabel;
    @FXML
    private Label totalMontantLabel;
    @FXML
    private Label averageMontantLabel;

    private SponsorService sponsorService = new SponsorService();
    private ContratSponsorService contratService = new ContratSponsorService();

    @FXML
    public void initialize() {
        refreshStatistics();
    }

    @FXML
    private void refreshStatistics() {
        try {
            // Get all sponsors and contrats
            List<Sponsor> sponsors = sponsorService.getAll();
            List<ContratSponsor> contrats = contratService.getAll();

            // Update sponsor statistics
            sponsorCountLabel.setText(String.valueOf(sponsors.size()));
            totalBudgetLabel.setText(String.format("%.2f DT", StatisticsCalculator.calculateTotalBudget(sponsors)));
            averageBudgetLabel.setText(String.format("%.2f DT", StatisticsCalculator.calculateAverageBudget(sponsors)));
            
            Sponsor topSponsor = StatisticsCalculator.getTopBudgetSponsor(sponsors);
            if (topSponsor != null) {
                topSponsorLabel.setText(topSponsor.getNom() + " (" + String.format("%.2f", topSponsor.getBudget()) + " DT)");
            } else {
                topSponsorLabel.setText("N/A");
            }

            // Update contrat statistics
            contratCountLabel.setText(String.valueOf(contrats.size()));
            activeContratLabel.setText(String.valueOf(StatisticsCalculator.countActiveContrats(contrats)));
            totalMontantLabel.setText(String.format("%.2f DT", StatisticsCalculator.calculateTotalMontant(contrats)));
            averageMontantLabel.setText(String.format("%.2f DT", StatisticsCalculator.calculateAverageMontant(contrats)));

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des statistiques: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

