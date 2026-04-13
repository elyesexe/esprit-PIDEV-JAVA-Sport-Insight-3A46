package tn.esprit.controllers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AfficherUsersController {

    @FXML private TableView<User>           tableView;
    @FXML private TableColumn<User,Integer> colId;
    @FXML private TableColumn<User,String>  colNom;
    @FXML private TableColumn<User,String>  colPrenom;
    @FXML private TableColumn<User,String>  colEmail;
    @FXML private TableColumn<User,String>  colTelephone;
    @FXML private TableColumn<User,String>  colStatut;
    @FXML private TableColumn<User,String>  colRoles;
    @FXML private Label                     errorLabel;
    @FXML private Label                     countLabel;
    @FXML private Button                    ajouterBtn;
    @FXML private Button                    modifierBtn;
    @FXML private Button                    supprimerBtn;
    @FXML private Button                    rafraichirBtn;
    @FXML private TextField                 searchIdField;
    @FXML private TextField                 searchNomField;

    private final UserService userService = new UserService();
    private final ObservableList<User> data = FXCollections.observableArrayList();
    private List<User> allUsers;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        tableView.setItems(data);
        loadData();
    }

    private void loadData() {
        try {
            allUsers = userService.getAllUsers();
            refresh(allUsers);
            hideError();
        } catch (SQLException e) {
            showError("Erreur DB : " + e.getMessage());
        }
    }

    private void refresh(List<User> list) {
        data.clear();
        data.addAll(list);
        tableView.refresh();
        if (countLabel != null)
            countLabel.setText(list.size() + " utilisateur(s)");
    }

    // ── Recherche par ID ──────────────────────────────────────────────────
    @FXML
    private void handleSearchById() {
        String txt = searchIdField.getText().trim();
        if (txt.isEmpty()) { showError("Entrez un ID."); return; }
        try {
            int id = Integer.parseInt(txt);
            List<User> result = allUsers.stream()
                    .filter(u -> u.getId() == id)
                    .collect(Collectors.toList());
            if (result.isEmpty()) showError("Aucun utilisateur avec id=" + id);
            else { hideError(); refresh(result); }
        } catch (NumberFormatException e) {
            showError("L'ID doit etre un nombre entier.");
        }
    }

    // ── Recherche par nom ─────────────────────────────────────────────────
    @FXML
    private void handleSearchByNom() {
        String txt = searchNomField.getText().trim().toLowerCase();
        if (txt.isEmpty()) { showError("Entrez un nom."); return; }
        List<User> result = allUsers.stream()
                .filter(u -> (u.getNom()    != null && u.getNom().toLowerCase().contains(txt))
                        || (u.getPrenom() != null && u.getPrenom().toLowerCase().contains(txt)))
                .collect(Collectors.toList());
        if (result.isEmpty()) showError("Aucun utilisateur trouve pour \"" + txt + "\"");
        else { hideError(); refresh(result); }
    }

    // ── Réinitialiser ─────────────────────────────────────────────────────
    @FXML
    private void handleReset() {
        searchIdField.clear();
        searchNomField.clear();
        hideError();
        refresh(allUsers);
    }

    // ── Tri alphabétique ──────────────────────────────────────────────────
    @FXML
    private void handleTriAlpha() {
        List<User> sorted = data.stream()
                .sorted(Comparator.comparing(
                        u -> u.getNom() != null ? u.getNom().toLowerCase() : ""))
                .collect(Collectors.toList());
        data.clear();
        data.addAll(sorted);
        tableView.refresh();
    }

    @FXML
    private void handleRafraichir() { loadData(); }

    // ── Ajouter ───────────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/register.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 860, 580));
            stage.setTitle("Ajouter un utilisateur");
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Modifier ──────────────────────────────────────────────────────────
    @FXML
    private void handleModifier() {
        User selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez selectionner un utilisateur.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/modifier_user.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 500, 520));
            stage.setTitle("Modifier — " + selected.getNom());
            ModifierUserController ctrl = loader.getController();
            ctrl.initUser(selected);
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Supprimer ─────────────────────────────────────────────────────────
    @FXML
    private void handleSupprimer() {
        User selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez selectionner un utilisateur.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + selected.getNom() + " " + selected.getPrenom() + " ?");
        confirm.setContentText("Cette action est irreversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                userService.deleteUser(selected.getId());
                loadData();
            }
        });
    }

    // ── Export PDF ────────────────────────────────────────────────────────
    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("utilisateurs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(tableView.getScene().getWindow());
        if (file == null) return;

        try {
            exportToPdf(file, data);
            new Alert(Alert.AlertType.INFORMATION,
                    "PDF exporte avec succes !\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            showError("Erreur export PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportToPdf(File file, List<User> users) throws Exception {
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,
                new BaseColor(26, 58, 92));
        Paragraph title = new Paragraph("Liste des Utilisateurs - Sport Insight", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        doc.add(title);

        Font subFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
        Paragraph sub = new Paragraph("Genere le : " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(16);
        doc.add(sub);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 1.5f, 1.5f, 2.5f, 1.5f, 1f, 1.5f});

        String[] headers = {"ID", "Nom", "Prenom", "Email", "Telephone", "Statut", "Role"};
        Font hFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(26, 58, 92);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(7);
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);
        boolean odd = true;
        for (User u : users) {
            BaseColor rowColor = odd ? new BaseColor(240, 245, 250) : BaseColor.WHITE;
            String[] values = {
                    String.valueOf(u.getId()),
                    u.getNom()       != null ? u.getNom()       : "",
                    u.getPrenom()    != null ? u.getPrenom()    : "",
                    u.getEmail()     != null ? u.getEmail()     : "",
                    u.getTelephone() != null ? u.getTelephone() : "",
                    u.getStatut()    != null ? u.getStatut()    : "",
                    u.getRoles()     != null ? u.getRoles()     : ""
            };
            for (String v : values) {
                PdfPCell cell = new PdfPCell(new Phrase(v, cellFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(5);
                table.addCell(cell);
            }
            odd = !odd;
        }
        doc.add(table);

        Paragraph footer = new Paragraph("\nTotal : " + users.size() + " utilisateur(s)", subFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        doc.add(footer);

        doc.close();
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("  " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}