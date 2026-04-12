package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.util.List;

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
    @FXML private Button                    ajouterBtn;
    @FXML private Button                    modifierBtn;
    @FXML private Button                    supprimerBtn;
    @FXML private Button                    rafraichirBtn;

    private final UserService userService = new UserService();
    private final ObservableList<User> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));

        // ── Lier la liste observable à la table une seule fois ─────────────
        tableView.setItems(data);
        loadData();
    }

    // ── Vide et recharge depuis la DB ──────────────────────────────────────
    private void loadData() {
        try {
            List<User> list = userService.getAllUsers();
            data.clear();                    // vide la liste observable
            data.addAll(list);               // recharge depuis DB
            tableView.refresh();             // force le rendu visuel
            hideError();
            System.out.println("✅ " + list.size() + " utilisateur(s) chargé(s).");
        } catch (SQLException e) {
            showError("Erreur DB : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRafraichir() {
        loadData();
    }

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/register.fxml")
            );
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 860, 580));
            stage.setTitle("Ajouter un utilisateur");
            stage.showAndWait();
            loadData();     // rafraîchit après fermeture
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        User selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez sélectionner un utilisateur à modifier.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/modifier_user.fxml")
            );
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 500, 520));
            stage.setTitle("Modifier — " + selected.getNom() + " " + selected.getPrenom());

            ModifierUserController ctrl = loader.getController();
            ctrl.initUser(selected);

            stage.showAndWait();
            loadData();     // ← recharge depuis DB après fermeture de la fenêtre
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        User selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez sélectionner un utilisateur à supprimer.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + selected.getNom() + " " + selected.getPrenom() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.deleteUser(selected.getId());
                loadData();
            }
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠  " + msg);
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