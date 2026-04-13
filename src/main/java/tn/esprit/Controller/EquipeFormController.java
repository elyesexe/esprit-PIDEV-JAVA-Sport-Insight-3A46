package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Equipe;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EquipeFormController {
    private static final Map<String, String> COMPETITION_LABELS = buildCompetitionLabels();
    private static final Map<String, String> COMPETITION_CODES_BY_LABEL = buildCompetitionCodesByLabel();

    @FXML
    private HBox navbarRoot;
    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button annonceNavButton;
    @FXML
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label formModeChipLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private ComboBox<String> competitionComboBox;
    @FXML
    private TextField nomField;
    @FXML
    private TextField coachField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField imageField;
    @FXML
    private Button saveButton;

    private EquipeService equipeService;
    private Equipe editingEquipe;
    private String returnCompetitionCode;
    private boolean updateMode;
    private File lastImageDirectory;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureCompetitionChoices();

        try {
            equipeService = new EquipeService();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer le formulaire equipe.\n" + e.getMessage());
        }

        applyModeTexts();
    }

    public void configureForCreate(String initialCompetitionCode) {
        updateMode = false;
        editingEquipe = null;
        returnCompetitionCode = initialCompetitionCode;
        clearForm();
        if (initialCompetitionCode != null) {
            competitionComboBox.getSelectionModel().select(resolveCompetitionLabel(initialCompetitionCode));
        }
        applyModeTexts();
    }

    public void configureForUpdate(Equipe equipe) {
        updateMode = true;
        editingEquipe = equipe;
        returnCompetitionCode = equipe == null ? null : equipe.getCompetitionCode();
        populateForm(equipe);
        applyModeTexts();
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        openCompetitionSelector();
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un logo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        if (lastImageDirectory != null && lastImageDirectory.exists()) {
            fileChooser.setInitialDirectory(lastImageDirectory);
        } else {
            File picturesDirectory = new File(System.getProperty("user.home"), "Pictures");
            if (picturesDirectory.exists()) {
                fileChooser.setInitialDirectory(picturesDirectory);
            }
        }

        Window window = imageField.getScene() == null ? null : imageField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(window);
        if (selectedFile == null) {
            return;
        }

        lastImageDirectory = selectedFile.getParentFile();
        imageField.setText(selectedFile.getAbsolutePath());
        clearValidation();
    }

    @FXML
    private void handleSave() {
        clearValidation();

        Equipe equipe = buildEquipeFromForm();
        if (equipe == null || equipeService == null) {
            return;
        }

        try {
            if (updateMode && editingEquipe != null) {
                equipe.setId(editingEquipe.getId());
                equipe.setExternalApiId(editingEquipe.getExternalApiId());
                equipe.setExternalSource(editingEquipe.getExternalSource());
                equipeService.update(equipe);
                EquipeUiSupport.clearImageCache();
                openDetail(equipe);
                return;
            }

            equipeService.add(equipe);
            EquipeUiSupport.clearImageCache();
            openEquipeList(equipe.getCompetitionCode());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, updateMode ? "Modification" : "Ajout",
                    "Impossible d'enregistrer l'equipe.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (updateMode && editingEquipe != null) {
            openDetail(editingEquipe);
            return;
        }

        if (returnCompetitionCode != null) {
            openEquipeList(returnCompetitionCode);
            return;
        }

        openCompetitionSelector();
    }


    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton != null ? annonceNavButton : matchsNavButton, "/tn/esprit/views/annonce-user-view.fxml", "/tn/esprit/styles/annonce-theme.css", "Anonce | Sport Insight");
    }
    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.EQUIPES);
    }

    private void configureCompetitionChoices() {
        competitionComboBox.setItems(FXCollections.observableArrayList(COMPETITION_LABELS.values()));
    }

    private void applyModeTexts() {
        formModeChipLabel.setText(updateMode ? "Modification" : "Ajout");
        pageTitleLabel.setText(updateMode ? "Modifier l'equipe" : "Ajouter une equipe");
        pageSubtitleLabel.setText(updateMode
                ? "Mettez a jour les informations du club puis validez."
                : "Renseignez le formulaire pour creer un club dans l'une des 5 competitions.");
        saveButton.setText(updateMode ? "Enregistrer" : "Ajouter");
    }

    private void populateForm(Equipe equipe) {
        clearForm();
        if (equipe == null) {
            return;
        }

        competitionComboBox.getSelectionModel().select(resolveCompetitionLabel(equipe.getCompetitionCode()));
        nomField.setText(emptyIfNull(equipe.getNom()));
        coachField.setText(emptyIfNull(equipe.getCoach()));
        addressField.setText(emptyIfNull(equipe.getAdresse()));
        telephoneField.setText(emptyIfNull(equipe.getTelephone()));
        emailField.setText(emptyIfNull(equipe.getEmail()));
        imageField.setText(emptyIfNull(equipe.getImage()));
    }

    private void clearForm() {
        competitionComboBox.getSelectionModel().clearSelection();
        nomField.clear();
        coachField.clear();
        addressField.clear();
        telephoneField.clear();
        emailField.clear();
        imageField.clear();
        clearValidation();
    }

    private Equipe buildEquipeFromForm() {
        String competitionCode = resolveCompetitionCode(competitionComboBox.getValue());
        String nom = emptyToNull(nomField.getText());
        String coach = emptyToNull(coachField.getText());
        String address = emptyToNull(addressField.getText());
        String telephone = emptyToNull(telephoneField.getText());
        String email = emptyToNull(emailField.getText());
        String image = emptyToNull(imageField.getText());

        if (competitionCode == null) {
            markFieldInvalid(competitionComboBox);
            showValidation("Selectionnez une competition.");
            return null;
        }

        if (nom == null) {
            markFieldInvalid(nomField);
            showValidation("Le nom de l'equipe est obligatoire.");
            return null;
        }

        if (nom.length() > 100) {
            markFieldInvalid(nomField);
            showValidation("Le nom de l'equipe ne peut pas depasser 100 caracteres.");
            return null;
        }

        if (coach != null && coach.length() > 100) {
            markFieldInvalid(coachField);
            showValidation("Le nom du coach ne peut pas depasser 100 caracteres.");
            return null;
        }

        if (email != null && !email.contains("@")) {
            markFieldInvalid(emailField);
            showValidation("Renseignez une adresse email valide.");
            return null;
        }

        Equipe equipe = new Equipe(nom, coach, address, telephone, email, image);
        equipe.setCompetitionCode(competitionCode);
        return equipe;
    }

    private void openCompetitionSelector() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    private void openEquipeList(String competitionCode) {
        if (competitionCode == null) {
            openCompetitionSelector();
            return;
        }

        SceneNavigator.switchScene(
                saveButton,
                "/tn/esprit/views/equipe-list-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                resolveCompetitionLabel(competitionCode) + " | Equipes",
                controller -> {
                    if (controller instanceof EquipeListController equipeListController) {
                        equipeListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

    private void openDetail(Equipe equipe) {
        SceneNavigator.switchScene(
                saveButton,
                "/tn/esprit/views/equipe-detail-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                emptyIfNull(equipe.getNom()) + " | Equipe",
                controller -> {
                    if (controller instanceof EquipeDetailController equipeDetailController) {
                        equipeDetailController.setEquipeContext(equipe);
                    }
                }
        );
    }

    private String resolveCompetitionLabel(String competitionCode) {
        if (!FootballDataCompetitions.isTeamCompetition(competitionCode)) {
            return null;
        }
        return FootballDataCompetitions.labelOf(competitionCode);
    }

    private String resolveCompetitionCode(String competitionLabel) {
        return competitionLabel == null ? null : COMPETITION_CODES_BY_LABEL.get(competitionLabel);
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void clearValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        clearFieldError(competitionComboBox);
        clearFieldError(nomField);
        clearFieldError(coachField);
        clearFieldError(emailField);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Map<String, String> buildCompetitionLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String code : FootballDataCompetitions.TEAM_CODES) {
            labels.put(code, FootballDataCompetitions.labelOf(code));
        }
        return labels;
    }

    private static Map<String, String> buildCompetitionCodesByLabel() {
        Map<String, String> codesByLabel = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : COMPETITION_LABELS.entrySet()) {
            codesByLabel.put(entry.getValue(), entry.getKey());
        }
        return codesByLabel;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

