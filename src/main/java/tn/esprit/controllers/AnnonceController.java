package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.Annonce;
import tn.esprit.services.AnnonceService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class AnnonceController {
    @FXML private TableView<Annonce> annonceTable;
    @FXML private TableColumn<Annonce, Integer> idColumn;
    @FXML private TableColumn<Annonce, String> titreColumn;
    @FXML private TableColumn<Annonce, String> posteColumn;
    @FXML private TableColumn<Annonce, LocalDate> dateColumn;
    @FXML private TableColumn<Annonce, String> statutColumn;

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField posteField;
    @FXML private TextField niveauField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField entraineurIdField;
    @FXML private Label commentCountLabel;
    @FXML private CheckBox commentsEnabledCheck;
    @FXML private CheckBox urgentCheck;

    @FXML private TextField searchTitreField;
    @FXML private DatePicker searchDatePicker;
    @FXML private TextField searchPosteField;

    @FXML private Label messageLabel;

    private AnnonceService annonceService;
    private tn.esprit.services.CommentaireService commentaireService;
    private ObservableList<Annonce> annonces;
    // cache: nombre de commentaires par annonce
    private java.util.Map<Integer, Integer> commentCounts = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        try {
            annonceService = new AnnonceService();
            commentaireService = new tn.esprit.services.CommentaireService();

            // Configure les colonnes
            idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
            titreColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTitre()));
            posteColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPosteRecherche()));
            dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDatePublication()));
            statutColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStatut()));
            // Le nombre de commentaires est affiché dans le panneau détail (commentCountLabel)

            // Configure le statut combo (valeurs utilisées en base)
            // Affiche en français mais utilise des valeurs simples sans accents pour la compatibilité
            statutCombo.setItems(FXCollections.observableArrayList("ACTIVE", "EXPIREE", "EN_ATTENTE"));
            statutCombo.setPromptText("Sélectionner...");

            // Charge les annonces
            rafraichirTable();

            // Ajoute un listener pour afficher les détails quand on sélectionne une annonce
            annonceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    afficherAnnonce(newVal);
                }
            });
        } catch (SQLException e) {
            afficherErreur("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterAnnonce() {
        if (!validerChamps()) return;

        try {
            // Vérification explicite du statut
            String statut = statutCombo.getValue();
            if (statut == null || statut.trim().isEmpty()) {
                afficherErreur("Veuillez sélectionner un statut valide.");
                return;
            }
            // Vérification des valeurs autorisées
            if (!statut.equals("ACTIVE") && !statut.equals("EXPIREE") && !statut.equals("EN_ATTENTE")) {
                afficherErreur("Statut invalide. Choisissez parmi: ACTIVE, EXPIREE, EN_ATTENTE.");
                return;
            }

            // Création de l'annonce
            Annonce annonce = new Annonce(
                titreField.getText(),
                descriptionArea.getText(),
                posteField.getText(),
                niveauField.getText(),
                datePicker.getValue(),
                statut,
                Integer.parseInt(entraineurIdField.getText()),
                commentsEnabledCheck != null ? commentsEnabledCheck.isSelected() : true,
                urgentCheck != null ? urgentCheck.isSelected() : false
            );

            annonceService.add(annonce);
            afficherSucces("✅ Annonce ajoutée avec succès!");
            rafraichirTable();
            nettoyer();
        } catch (SQLException e) {
            afficherErreur("Erreur SQL: " + e.getMessage());
        } catch (Exception e) {
            afficherErreur("Erreur inattendue: " + e.getMessage());
        }
    }

    @FXML
    private void modifierAnnonce() {
        Annonce selected = annonceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherErreur("Veuillez sélectionner une annonce");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setTitre(titreField.getText());
            selected.setDescription(descriptionArea.getText());
            selected.setPosteRecherche(posteField.getText());
            selected.setNiveauRequis(niveauField.getText());
            selected.setDatePublication(datePicker.getValue());
            selected.setStatut(statutCombo.getValue());
            selected.setEntraineurId(Integer.parseInt(entraineurIdField.getText()));
                selected.setCommentsEnabled(commentsEnabledCheck != null ? commentsEnabledCheck.isSelected() : true);
                selected.setUrgent(urgentCheck != null ? urgentCheck.isSelected() : false);

            annonceService.update(selected);
            afficherSucces("✅ Annonce modifiée avec succès!");
            rafraichirTable();
            nettoyer();
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerAnnonce() {
        Annonce selected = annonceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherErreur("Veuillez sélectionner une annonce");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'annonce?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer l'annonce '" + selected.getTitre() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                annonceService.delete(selected.getId());
                afficherSucces("✅ Annonce supprimée!");
                rafraichirTable();
                nettoyer();
            } catch (SQLException e) {
                afficherErreur("Erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    private void rechercherParTitre() {
        String titre = searchTitreField.getText();
        if (titre.trim().isEmpty()) {
            rafraichirTable();
            return;
        }

        try {
            List<Annonce> resultats = annonceService.searchByTitre(titre);
            afficherResultats(resultats);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherParDate() {
        LocalDate date = searchDatePicker.getValue();
        if (date == null) {
            rafraichirTable();
            return;
        }

        try {
            List<Annonce> resultats = annonceService.searchByDatePublication(date);
            afficherResultats(resultats);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherParPoste() {
        String poste = searchPosteField.getText();
        if (poste.trim().isEmpty()) {
            rafraichirTable();
            return;
        }

        try {
            List<Annonce> resultats = annonceService.getAnnoncesByPoste(poste);
            afficherResultats(resultats);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void rafraichirRecherche() {
        rafraichirTable();
        searchTitreField.clear();
        searchDatePicker.setValue(null);
        searchPosteField.clear();
    }

    @FXML
    private void nettoyer() {
        titreField.clear();
        descriptionArea.clear();
        posteField.clear();
        niveauField.clear();
        datePicker.setValue(null);
        entraineurIdField.clear();
        messageLabel.setText("");
        if (commentCountLabel != null) commentCountLabel.setText("0");
        if (commentsEnabledCheck != null) commentsEnabledCheck.setSelected(true);
        if (urgentCheck != null) urgentCheck.setSelected(false);
    }

    @FXML
    private void exporterPdf() {
        try {
            List<Annonce> liste = annonceService.getAll();
            File outFile = new File("annonces_list.pdf");
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);

                PDPageContentStream cs = new PDPageContentStream(doc, page);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText("Liste des annonces");
                cs.newLineAtOffset(0, -20);
                cs.setFont(PDType1Font.HELVETICA, 11);

                float yPosition = 730;
                for (Annonce a : liste) {
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
                    int nb = 0;
                    try {
                        nb = commentaireService.countCommentairesByAnnonce(a.getId());
                    } catch (Exception ex) {
                        // ignore
                    }
                    String urgentMark = (a.getUrgent() != null && a.getUrgent()) ? " (URGENT)" : "";
                    String line = String.format("%d - %s%s | %s | %s | %s | comments: %d", a.getId(), a.getTitre(), urgentMark, a.getPosteRecherche(), a.getDatePublication(), a.getStatut(), nb);
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                    yPosition -= 15;
                }

                cs.endText();
                cs.close();
                doc.save(outFile);
            }

            afficherSucces("PDF généré: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            afficherErreur("Erreur export PDF: " + e.getMessage());
        }
    }

    private void rafraichirTable() {
        try {
            List<Annonce> liste = annonceService.getAll();
            // Mettre à jour le cache des comptes de commentaires en une seule requête
            try {
                commentCounts = commentaireService.countCommentairesGroupByAnnonce();
            } catch (Exception e) {
                commentCounts = new java.util.HashMap<>();
            }

            annonces = FXCollections.observableArrayList(liste);
            annonceTable.setItems(annonces);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
    }

    private void afficherResultats(List<Annonce> resultats) {
        annonces = FXCollections.observableArrayList(resultats);
        annonceTable.setItems(annonces);
        afficherSucces("✅ " + resultats.size() + " résultat(s) trouvé(s)");
    }

    private void afficherAnnonce(Annonce annonce) {
        titreField.setText(annonce.getTitre());
        descriptionArea.setText(annonce.getDescription());
        posteField.setText(annonce.getPosteRecherche());
        niveauField.setText(annonce.getNiveauRequis());
        datePicker.setValue(annonce.getDatePublication());
        statutCombo.setValue(annonce.getStatut());
        entraineurIdField.setText(annonce.getEntraineurId() != null ? annonce.getEntraineurId().toString() : "");
        // Mettre à jour les checkboxes
        if (commentsEnabledCheck != null) {
            commentsEnabledCheck.setSelected(annonce.getCommentsEnabled() == null ? true : annonce.getCommentsEnabled());
        }
        if (urgentCheck != null) {
            urgentCheck.setSelected(annonce.getUrgent() == null ? false : annonce.getUrgent());
        }
        // Mettre à jour le label du nombre de commentaires pour cette annonce
        int cnt = 0;
        if (annonce.getId() != null) {
            if (commentCounts != null && commentCounts.containsKey(annonce.getId())) {
                cnt = commentCounts.get(annonce.getId());
            } else {
                try {
                    cnt = commentaireService.countCommentairesByAnnonce(annonce.getId());
                } catch (Exception e) {
                    cnt = 0;
                }
            }
        }
        if (commentCountLabel != null) {
            commentCountLabel.setText(String.valueOf(cnt));
        }
    }

    private boolean validerChamps() {
        if (titreField.getText().trim().isEmpty() ||
            descriptionArea.getText().trim().isEmpty() ||
            posteField.getText().trim().isEmpty() ||
            niveauField.getText().trim().isEmpty() ||
            datePicker.getValue() == null ||
            statutCombo.getValue() == null ||
            entraineurIdField.getText().trim().isEmpty()) {
            afficherErreur("Tous les champs sont requis!");
            return false;
        }

        try {
            Integer.parseInt(entraineurIdField.getText());
        } catch (NumberFormatException e) {
            afficherErreur("L'ID entraîneur doit être un nombre");
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
