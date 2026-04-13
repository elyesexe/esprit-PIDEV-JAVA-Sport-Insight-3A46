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
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Sponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.services.ContratSponsorService;
import tn.esprit.services.SponsorService;
import tn.esprit.services.EquipeService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ContratSponsorController {

    @FXML
    private TextField searchField;
    @FXML
    private javafx.scene.control.DatePicker dateDebutMinPicker;
    @FXML
    private javafx.scene.control.DatePicker dateDebutMaxPicker;
    @FXML
    private TextField minMontantField;
    @FXML
    private TextField maxMontantField;
    @FXML
    private TableView<ContratSponsor> contratTable;
    @FXML
    private TableColumn<ContratSponsor, Integer> idColumn;
    @FXML
    private TableColumn<ContratSponsor, LocalDate> dateDebutColumn;
    @FXML
    private TableColumn<ContratSponsor, LocalDate> dateFinColumn;
    @FXML
    private TableColumn<ContratSponsor, Double> montantColumn;
    @FXML
    private TableColumn<ContratSponsor, String> descriptionColumn;
    @FXML
    private TableColumn<ContratSponsor, String> statutColumn;
    @FXML
    private TableColumn<ContratSponsor, String> sponsorColumn;
    @FXML
    private TableColumn<ContratSponsor, String> equipeColumn;
    @FXML
    private TableColumn<ContratSponsor, Void> actionsColumn;

    private ContratSponsorService contratService = new ContratSponsorService();
    private SponsorService sponsorService = new SponsorService();
    private EquipeService equipeService = new EquipeService();
    private ObservableList<ContratSponsor> contratList = FXCollections.observableArrayList();
    private Map<Integer, String> sponsorNames = new HashMap<>();
    private Map<Integer, String> equipeNames = new HashMap<>();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        dateFinColumn.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        montantColumn.setCellValueFactory(new PropertyValueFactory<>("montant"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Custom cell factories for sponsor and equipe names
        sponsorColumn.setCellValueFactory(cellData -> {
            Integer sponsorId = cellData.getValue().getSponsorId();
            return new javafx.beans.property.SimpleStringProperty(getSponsorName(sponsorId));
        });
        equipeColumn.setCellValueFactory(cellData -> {
            Integer equipeId = cellData.getValue().getEquipeId();
            return new javafx.beans.property.SimpleStringProperty(getEquipeName(equipeId));
        });

        // Actions column
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                editButton.setOnAction(event -> {
                    ContratSponsor contrat = getTableView().getItems().get(getIndex());
                    showEditContratDialog(contrat);
                });
                deleteButton.setOnAction(event -> {
                    ContratSponsor contrat = getTableView().getItems().get(getIndex());
                    deleteContrat(contrat);
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

        contratTable.setItems(contratList);
        try {
            loadNames();
            loadAllContrats();
        } catch (Exception e) {
            showAlert("Erreur de connexion", "Impossible de se connecter à la base de données. Vérifiez que MySQL est démarré.");
        }
    }

    private void loadNames() {
        try {
            List<Sponsor> sponsors = sponsorService.getAll();
            for (Sponsor s : sponsors) {
                sponsorNames.put(s.getId(), s.getNom());
            }
            List<Equipe> equipes = equipeService.getAll();
            for (Equipe e : equipes) {
                equipeNames.put(e.getId(), e.getNom());
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des noms: " + e.getMessage());
        }
    }

    private String getSponsorName(Integer id) {
        return sponsorNames.getOrDefault(id, "Inconnu");
    }

    private String getEquipeName(Integer id) {
        return equipeNames.getOrDefault(id, "Inconnu");
    }

    @FXML
    private void loadAllContrats() {
        try {
            List<ContratSponsor> contrats = contratService.getAll();
            contratList.setAll(contrats);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des contrats: " + e.getMessage());
        }
    }

    @FXML
    private void searchContrats() {
        try {
            // Recherche par plage de date de début
            if (dateDebutMinPicker.getValue() == null || dateDebutMaxPicker.getValue() == null) {
                showAlert("Avertissement", "Veuillez sélectionner les deux dates pour la recherche.");
                return;
            }
            
            LocalDate dateMin = dateDebutMinPicker.getValue();
            LocalDate dateMax = dateDebutMaxPicker.getValue();
            
            if (dateMin.isAfter(dateMax)) {
                showAlert("Erreur", "La date de début doit être antérieure à la date de fin.");
                return;
            }
            
            List<ContratSponsor> contrats = contratService.searchByDateDebutRange(dateMin, dateMax);
            contratList.setAll(contrats);
            showAlert("Résultat", "Trouvé " + contrats.size() + " contrat(s).");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage());
        }
    }
    
    @FXML
    private void resetSearch() {
        dateDebutMinPicker.setValue(null);
        dateDebutMaxPicker.setValue(null);
        loadAllContrats();
    }

    @FXML
    private void searchByMontant() {
        try {
            double minMontant = Double.parseDouble(minMontantField.getText().trim());
            double maxMontant = Double.parseDouble(maxMontantField.getText().trim());
            List<ContratSponsor> contrats = contratService.searchByMontantRange(minMontant, maxMontant);
            contratList.setAll(contrats);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des valeurs numériques valides pour le montant.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @FXML
    private void showActiveContrats() {
        try {
            List<ContratSponsor> allContrats = contratService.getAll();
            List<ContratSponsor> activeContrats = tn.esprit.utils.StatisticsCalculator.filterActiveContrats(allContrats);
            contratList.setAll(activeContrats);
            showAlert("Filtrage", "Affichage de " + activeContrats.size() + " contrats actifs.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage());
        }
    }

    @FXML
    private void showExpiredContrats() {
        try {
            List<ContratSponsor> allContrats = contratService.getAll();
            List<ContratSponsor> expiredContrats = tn.esprit.utils.StatisticsCalculator.getExpiredContrats(allContrats);
            contratList.setAll(expiredContrats);
            showAlert("Filtrage", "Affichage de " + expiredContrats.size() + " contrats expirés.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage());
        }
    }

    @FXML
    private void exportToPDF() {
        try {
            if (contratList.isEmpty()) {
                showAlert("Avertissement", "Aucun contrat à exporter.");
                return;
            }
            
            // Essayer le dossier Téléchargements d'abord
            String downloadsPath = System.getProperty("user.home") + java.io.File.separator + "Downloads";
            java.io.File downloadsDir = new java.io.File(downloadsPath);
            
            // Si le dossier Downloads n'existe pas, utiliser Desktop
            if (!downloadsDir.exists()) {
                downloadsPath = System.getProperty("user.home") + java.io.File.separator + "Desktop";
                downloadsDir = new java.io.File(downloadsPath);
            }
            
            // Si aucun des deux n'existe, utiliser le répertoire utilisateur
            if (!downloadsDir.exists()) {
                downloadsDir = new java.io.File(System.getProperty("user.home"));
            }
            
            String filePath = downloadsDir.getAbsolutePath() + java.io.File.separator + "contrats_sponsors_" + System.currentTimeMillis() + ".pdf";
            tn.esprit.utils.PDFExporter.exportContratsToPDF(contratList, filePath);
            
            // Verify file was created
            java.io.File pdfFile = new java.io.File(filePath);
            if (pdfFile.exists()) {
                // Open PDF automatically
                try {
                    java.awt.Desktop.getDesktop().open(pdfFile);
                } catch (Exception ex) {
                    // If Desktop is not supported, just show path
                }
                showAlert("Succès ✅", "PDF exporté avec succès !\n\n📁 Dossier: " + downloadsDir.getAbsolutePath() + "\n\n📄 Fichier: " + pdfFile.getName() + "\n\n✓ Le fichier s'ouvrira automatiquement.");
            } else {
                showAlert("Erreur", "Le fichier PDF n'a pas pu être créé.");
            }
        } catch (Exception e) {
            showAlert("Erreur ❌", "Erreur lors de l'export PDF: " + e.getMessage());
        }
    }

    @FXML
    private void showStatistics() {
        showAlert("Statistiques", 
            "📊 STATISTIQUES CONTRATS\n\n" +
            "Total Contrats: " + contratList.size() + "\n" +
            "Contrats Actifs: " + tn.esprit.utils.StatisticsCalculator.countActiveContrats(contratList) + "\n" +
            "Montant Total: " + String.format("%.2f DT", tn.esprit.utils.StatisticsCalculator.calculateTotalMontant(contratList)) + "\n" +
            "Montant Moyen: " + String.format("%.2f DT", tn.esprit.utils.StatisticsCalculator.calculateAverageMontant(contratList)));
    }

    @FXML
    private void showAddContratDialog() {
        showContratDialog(null);
    }

    @FXML
    private void showEditContratDialog() {
        ContratSponsor selected = contratTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Avertissement", "Veuillez sélectionner un contrat à modifier.");
            return;
        }
        showEditContratDialog(selected);
    }

    private void showEditContratDialog(ContratSponsor contrat) {
        showContratDialog(contrat);
    }

    private void showContratDialog(ContratSponsor contrat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ContratSponsorForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(contrat == null ? "Ajouter Contrat" : "Modifier Contrat");
            stage.initModality(Modality.APPLICATION_MODAL);

            ContratSponsorFormController controller = loader.getController();
            controller.setContrat(contrat);
            controller.setParentController(this);

            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors de l'ouverture du formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void deleteContrat() {
        ContratSponsor selected = contratTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Avertissement", "Veuillez sélectionner un contrat à supprimer.");
            return;
        }
        deleteContrat(selected);
    }

    private void deleteContrat(ContratSponsor contrat) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le contrat");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce contrat?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    contratService.delete(contrat.getId());
                    contratList.remove(contrat);
                    showAlert("Succès", "Contrat supprimé avec succès.");
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    public void refreshTable() {
        loadNames();
        loadAllContrats();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
