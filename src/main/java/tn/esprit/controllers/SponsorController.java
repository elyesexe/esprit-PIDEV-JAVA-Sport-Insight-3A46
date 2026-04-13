package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Sponsor;
import tn.esprit.services.SponsorService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SponsorController {

    @FXML
    private TextField searchField;
    @FXML
    private TextField minBudgetField;
    @FXML
    private TextField maxBudgetField;
    @FXML
    private TableView<Sponsor> sponsorTable;
    @FXML
    private TableColumn<Sponsor, Integer> idColumn;
    @FXML
    private TableColumn<Sponsor, String> nomColumn;
    @FXML
    private TableColumn<Sponsor, String> emailColumn;
    @FXML
    private TableColumn<Sponsor, String> telephoneColumn;
    @FXML
    private TableColumn<Sponsor, Double> budgetColumn;
    @FXML
    private TableColumn<Sponsor, String> adresseColumn;
    @FXML
    private TableColumn<Sponsor, Void> actionsColumn;

    private SponsorService sponsorService = new SponsorService();
    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        budgetColumn.setCellValueFactory(new PropertyValueFactory<>("budget"));
        adresseColumn.setCellValueFactory(new PropertyValueFactory<>("adresse"));

        // Actions column with buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                editButton.setOnAction(event -> {
                    Sponsor sponsor = getTableView().getItems().get(getIndex());
                    showEditSponsorDialog(sponsor);
                });
                deleteButton.setOnAction(event -> {
                    Sponsor sponsor = getTableView().getItems().get(getIndex());
                    deleteSponsor(sponsor);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(editButton, deleteButton);
                    hbox.setSpacing(5);
                    setGraphic(hbox);
                }
            }
        });

        sponsorTable.setItems(sponsorList);
        try {
            loadAllSponsors();
        } catch (Exception e) {
            showAlert("Erreur de connexion", "Impossible de se connecter à la base de données. Vérifiez que MySQL est démarré.");
        }
    }

    @FXML
    private void loadAllSponsors() {
        try {
            List<Sponsor> sponsors = sponsorService.getAll();
            sponsorList.setAll(sponsors);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des sponsors: " + e.getMessage());
        }
    }

    @FXML
    private void searchSponsors() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllSponsors();
            return;
        }
        try {
            List<Sponsor> sponsors = sponsorService.searchByName(keyword);
            sponsorList.setAll(sponsors);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @FXML
    private void searchByBudget() {
        try {
            double minBudget = Double.parseDouble(minBudgetField.getText().trim());
            double maxBudget = Double.parseDouble(maxBudgetField.getText().trim());
            List<Sponsor> sponsors = sponsorService.searchByBudgetRange(minBudget, maxBudget);
            sponsorList.setAll(sponsors);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des valeurs numériques valides pour le budget.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @FXML
    private void sortByName() {
        try {
            List<Sponsor> sponsors = sponsorService.getAll();
            tn.esprit.utils.StatisticsCalculator.sortSponsorsByName(sponsors);
            sponsorList.setAll(tn.esprit.utils.StatisticsCalculator.sortSponsorsByName(sponsors));
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du tri: " + e.getMessage());
        }
    }

    @FXML
    private void sortByBudget() {
        try {
            List<Sponsor> sponsors = sponsorService.getAll();
            sponsorList.setAll(tn.esprit.utils.StatisticsCalculator.sortSponsorsByBudget(sponsors, false));
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du tri: " + e.getMessage());
        }
    }

    @FXML
    private void showAddSponsorDialog() {
        showSponsorDialog(null);
    }

    @FXML
    private void showEditSponsorDialog() {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Avertissement", "Veuillez sélectionner un sponsor à modifier.");
            return;
        }
        showEditSponsorDialog(selected);
    }

    private void showEditSponsorDialog(Sponsor sponsor) {
        showSponsorDialog(sponsor);
    }

    private void showSponsorDialog(Sponsor sponsor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SponsorForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(sponsor == null ? "Ajouter Sponsor" : "Modifier Sponsor");
            stage.initModality(Modality.APPLICATION_MODAL);

            SponsorFormController controller = loader.getController();
            controller.setSponsor(sponsor);
            controller.setParentController(this);

            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void deleteSponsor() {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Avertissement", "Veuillez sélectionner un sponsor à supprimer.");
            return;
        }
        deleteSponsor(selected);
    }

    private void deleteSponsor(Sponsor sponsor) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le sponsor");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer " + sponsor.getNom() + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sponsorService.delete(sponsor.getId());
                    sponsorList.remove(sponsor);
                    showAlert("Succès", "Sponsor supprimé avec succès.");
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    public void refreshTable() {
        loadAllSponsors();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
