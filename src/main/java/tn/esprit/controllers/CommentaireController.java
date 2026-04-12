package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.entities.Commentaire;
import tn.esprit.services.CommentaireService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentaireController {
    @FXML private TableView<Commentaire> commentaireTable;
    @FXML private TableColumn<Commentaire, Integer> idColumn;
    @FXML private TableColumn<Commentaire, String> auteurColumn;
    @FXML private TableColumn<Commentaire, String> contenuColumn;
    @FXML private TableColumn<Commentaire, LocalDate> dateColumn;
    @FXML private TableColumn<Commentaire, Integer> likesColumn;

    @FXML private TextArea contenuArea;
    @FXML private DatePicker datePicker;
    @FXML private TextField joueurIdField;
    @FXML private TextField annonceIdField;
    @FXML private TextField auteurField;
    @FXML private TextField likesField;
    @FXML private ComboBox<String> moderationCombo;
    @FXML private TextArea moderationReasonArea;

    @FXML private TextField searchJoueurField;
    @FXML private TextField searchAnnonceField;
    @FXML private DatePicker searchPublicationDatePicker;

    @FXML private Label messageLabel;

    private CommentaireService commentaireService;
    private ObservableList<Commentaire> commentaires;

    @FXML
    public void initialize() {
        try {
            commentaireService = new CommentaireService();

            // Configure les colonnes
            idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
            auteurColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAuteurAnonyme()));
            contenuColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(
                cellData.getValue().getContenu().length() > 50 ?
                cellData.getValue().getContenu().substring(0, 50) + "..." :
                cellData.getValue().getContenu()
            ));
            dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDateCommentaire()));
            likesColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getNbLikes()));

            // Configure les combos
            moderationCombo.setItems(FXCollections.observableArrayList("APPROVED", "PENDING", "REJECTED"));

            // Charge les commentaires
            rafraichirTable();

            // Ajoute un listener
            commentaireTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    afficherCommentaire(newVal);
                }
            });
        } catch (SQLException e) {
            afficherErreur("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterCommentaire() {
        if (!validerChamps()) return;

        try {
            Commentaire commentaire = new Commentaire(
                contenuArea.getText(),
                datePicker.getValue(),
                Integer.parseInt(joueurIdField.getText()),
                Integer.parseInt(annonceIdField.getText()),
                auteurField.getText(),
                Integer.parseInt(likesField.getText()),
                moderationCombo.getValue(),
                moderationReasonArea.getText().isEmpty() ? null : moderationReasonArea.getText()
            );

            commentaireService.add(commentaire);
            afficherSucces("✅ Commentaire ajouté avec succès!");
            rafraichirTable();
            nettoyer();
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void modifierCommentaire() {
        Commentaire selected = commentaireTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherErreur("Veuillez sélectionner un commentaire");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setContenu(contenuArea.getText());
            selected.setDateCommentaire(datePicker.getValue());
            selected.setJoueurId(Integer.parseInt(joueurIdField.getText()));
            selected.setAnnonceId(Integer.parseInt(annonceIdField.getText()));
            selected.setAuteurAnonyme(auteurField.getText());
            selected.setNbLikes(Integer.parseInt(likesField.getText()));
            selected.setModerationStatus(moderationCombo.getValue());
            selected.setModerationReason(moderationReasonArea.getText().isEmpty() ? null : moderationReasonArea.getText());

            commentaireService.update(selected);
            afficherSucces("✅ Commentaire modifié avec succès!");
            rafraichirTable();
            nettoyer();
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerCommentaire() {
        Commentaire selected = commentaireTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherErreur("Veuillez sélectionner un commentaire");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le commentaire?");
        alert.setContentText("Êtes-vous sûr?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentaireService.delete(selected.getId());
                afficherSucces("✅ Commentaire supprimé!");
                rafraichirTable();
                nettoyer();
            } catch (SQLException e) {
                afficherErreur("Erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    private void rechercherParAnnonce() {
        String annonceId = searchAnnonceField.getText();
        if (annonceId.trim().isEmpty()) {
            rafraichirTable();
            return;
        }

        try {
            List<Commentaire> resultats = commentaireService.getCommentairesByAnnonce(Integer.parseInt(annonceId));
            afficherResultats(resultats);
        } catch (SQLException | NumberFormatException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherParJoueur() {
        String joueurId = searchJoueurField.getText();
        if (joueurId.trim().isEmpty()) {
            rafraichirTable();
            return;
        }

        try {
            List<Commentaire> resultats = commentaireService.getCommentairesByJoueur(Integer.parseInt(joueurId));
            afficherResultats(resultats);
        } catch (SQLException | NumberFormatException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherAvance() {
        String joueurIdText = searchJoueurField.getText() != null ? searchJoueurField.getText().trim() : "";
        LocalDate publicationDate = searchPublicationDatePicker.getValue();

        if (joueurIdText.isEmpty() && publicationDate == null) {
            rafraichirTable();
            return;
        }

        try {
            Integer joueurId = joueurIdText.isEmpty() ? null : Integer.parseInt(joueurIdText);
            List<Commentaire> resultats = commentaireService.searchAdvanced(publicationDate, joueurId);
            afficherResultats(resultats);
        } catch (SQLException | NumberFormatException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rafraichirRecherche() {
        rafraichirTable();
        searchJoueurField.clear();
        searchAnnonceField.clear();
        if (searchPublicationDatePicker != null) {
            searchPublicationDatePicker.setValue(null);
        }
    }

    @FXML
    private void nettoyer() {
        contenuArea.clear();
        datePicker.setValue(null);
        joueurIdField.clear();
        annonceIdField.clear();
        auteurField.clear();
        likesField.clear();
        moderationReasonArea.clear();
        messageLabel.setText("");
    }

    @FXML
    private void exporterPdf() {
        try {
            List<Commentaire> liste = commentaireTable.getItems() != null && !commentaireTable.getItems().isEmpty()
                    ? new ArrayList<>(commentaireTable.getItems())
                    : commentaireService.getAll();

            File outFile = new File("commentaires_list.pdf");
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);

                PDPageContentStream cs = new PDPageContentStream(doc, page);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText(safePdfText("Liste des commentaires", 80));
                cs.newLineAtOffset(0, -20);
                cs.setFont(PDType1Font.HELVETICA, 11);

                float yPosition = 730;
                for (Commentaire commentaire : liste) {
                    if (yPosition < 60) {
                        cs.endText();
                        cs.close();
                        PDPage newPage = new PDPage();
                        doc.addPage(newPage);
                        cs = new PDPageContentStream(doc, newPage);
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 11);
                        cs.newLineAtOffset(50, 750);
                        yPosition = 730;
                    }

                    String line = String.format(
                            "%d | joueur=%s | annonce=%s | %s | likes=%d | %s",
                            commentaire.getId(),
                            commentaire.getJoueurId() != null ? commentaire.getJoueurId() : "-",
                            commentaire.getAnnonceId() != null ? commentaire.getAnnonceId() : "-",
                            commentaire.getDateCommentaire(),
                            commentaire.getNbLikes(),
                            safePdfText(commentaire.getContenu(), 60)
                    );
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                    yPosition -= 15;
                }

                cs.endText();
                cs.close();
                doc.save(outFile);
            }

            afficherSucces("PDF genere: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            afficherErreur("Erreur export PDF: " + e.getMessage());
        }
    }

    private String safePdfText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("[^\\x20-\\x7E]", "?");
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength - 3) + "...";
    }

    private void rafraichirTable() {
        try {
            List<Commentaire> liste = commentaireService.getAll();
            commentaires = FXCollections.observableArrayList(liste);
            commentaireTable.setItems(commentaires);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    private void afficherResultats(List<Commentaire> resultats) {
        commentaires = FXCollections.observableArrayList(resultats);
        commentaireTable.setItems(commentaires);
        afficherSucces("✅ " + resultats.size() + " résultat(s) trouvé(s)");
    }

    private void afficherCommentaire(Commentaire commentaire) {
        contenuArea.setText(commentaire.getContenu());
        datePicker.setValue(commentaire.getDateCommentaire());
        joueurIdField.setText(commentaire.getJoueurId() != null ? commentaire.getJoueurId().toString() : "");
        annonceIdField.setText(commentaire.getAnnonceId() != null ? commentaire.getAnnonceId().toString() : "");
        auteurField.setText(commentaire.getAuteurAnonyme());
        likesField.setText(String.valueOf(commentaire.getNbLikes()));
        moderationCombo.setValue(commentaire.getModerationStatus());
        moderationReasonArea.setText(commentaire.getModerationReason() != null ? commentaire.getModerationReason() : "");
    }

    private boolean validerChamps() {
        if (contenuArea.getText().trim().isEmpty() ||
            datePicker.getValue() == null ||
            joueurIdField.getText().trim().isEmpty() ||
            annonceIdField.getText().trim().isEmpty() ||
            auteurField.getText().trim().isEmpty() ||
            likesField.getText().trim().isEmpty() ||
            moderationCombo.getValue() == null) {
            afficherErreur("Tous les champs requis doivent être remplis!");
            return false;
        }

        try {
            Integer.parseInt(joueurIdField.getText());
            Integer.parseInt(annonceIdField.getText());
            Integer.parseInt(likesField.getText());
        } catch (NumberFormatException e) {
            afficherErreur("Les ID et les likes doivent être des nombres");
            return false;
        }

        return true;
    }

    private void afficherSucces(String message) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void afficherErreur(String message) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        messageLabel.setText("❌ " + message);
    }
}

