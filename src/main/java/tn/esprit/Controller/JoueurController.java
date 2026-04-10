package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JoueurController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Path SYMFONY_JOUEURS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "joueurs");
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;
    private static final double CARD_IMAGE_SIZE = 82;

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button sidebarToggleButton;
    @FXML
    private Button sidebarOpenButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label teamCountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Equipe> equipeFilterComboBox;
    @FXML
    private ListView<Joueur> joueurListView;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Label detailBadgeLabel;
    @FXML
    private ImageView detailImageView;
    @FXML
    private Label detailImageFallbackLabel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailEquipeValueLabel;
    @FXML
    private Label detailNumeroValueLabel;
    @FXML
    private Label formHintLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private DatePicker dateNaissancePicker;
    @FXML
    private TextField numeroField;
    @FXML
    private ComboBox<Equipe> equipeComboBox;
    @FXML
    private TextField imageField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button refreshButton;

    private final ObservableList<Joueur> joueurs = FXCollections.observableArrayList();
    private final ObservableList<Equipe> equipes = FXCollections.observableArrayList();
    private final FilteredList<Joueur> filteredJoueurs = new FilteredList<>(joueurs, joueur -> true);
    private final Map<Integer, Equipe> equipeById = new HashMap<>();

    private JoueurService joueurService;
    private EquipeService equipeService;
    private Joueur selectedJoueur;
    private File lastImageDirectory;
    private boolean serviceReady;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureNavigationState();
        configureStatusLabel();
        configureEquipeComboBoxes();
        configureNumeroField();
        configurePlayerList();
        bindUiState();
        updateActionAvailability();
        updateDetailPanel();

        try {
            joueurService = new JoueurService();
            equipeService = new EquipeService();
            serviceReady = true;
            refreshData(null);
            showSuccessStatus("Module Joueur pret.");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Connexion base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les joueurs.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        clearValidation();

        Joueur joueur = buildJoueurFromForm(false);
        if (joueur == null || joueurService == null) {
            return;
        }

        try {
            joueurService.add(joueur);
            refreshData(null);
            clearForm();
            showSuccessStatus("Joueur ajoute avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant l'ajout.");
            showAlert(Alert.AlertType.ERROR, "Ajout", "Erreur lors de l'ajout du joueur.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        clearValidation();

        if (selectedJoueur == null) {
            showValidation("Selectionnez un joueur a modifier.");
            return;
        }

        Joueur joueur = buildJoueurFromForm(true);
        if (joueur == null || joueurService == null) {
            return;
        }

        joueur.setId(selectedJoueur.getId());

        try {
            joueurService.update(joueur);
            refreshData(selectedJoueur.getId());
            showSuccessStatus("Joueur modifie avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la modification.");
            showAlert(Alert.AlertType.ERROR, "Modification", "Erreur lors de la modification du joueur.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        clearValidation();

        if (selectedJoueur == null) {
            showValidation("Selectionnez un joueur a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le joueur \"" + buildFullName(selectedJoueur) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            joueurService.delete(selectedJoueur.getId());
            refreshData(null);
            clearForm();
            showSuccessStatus("Joueur supprime avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la suppression.");
            showAlert(Alert.AlertType.ERROR, "Suppression", "Erreur lors de la suppression du joueur.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshData(getSelectedJoueurId());
        showMutedStatus("Liste des joueurs actualisee.");
    }

    @FXML
    private void handleClear() {
        clearForm();
        showMutedStatus("Formulaire vide.");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        equipeFilterComboBox.getSelectionModel().clearSelection();
        applyFilters();
        showMutedStatus("Filtres reinitialises.");
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
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
        clearFieldError(imageField);
        updateDetailPanel();
    }

    @FXML
    private void handleOpenSidebar() {
        applySidebarState(true);
    }

    @FXML
    private void handleToggleSidebar() {
        applySidebarState(false);
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/equipe-crud-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Sport Insight");
    }

    @FXML
    private void handleOpenMatchsSoon() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    @FXML
    private void handleOpenJoueurs() {
        showMutedStatus("Vous etes deja dans le module Joueurs.");
    }

    private void configureSidebar() {
        applySidebarState(true);
    }

    private void configureNavigationState() {
        joueursNavButton.getStyleClass().remove("sidebar-nav-button-active");
        joueursNavButton.getStyleClass().add("sidebar-nav-button-active");
    }

    private void configureStatusLabel() {
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.setText("Pret");
        setStatusStyle("status-muted");
    }

    private void configureEquipeComboBoxes() {
        equipeComboBox.setItems(equipes);
        equipeFilterComboBox.setItems(equipes);
        equipeComboBox.setCellFactory(listView -> createEquipeCell());
        equipeComboBox.setButtonCell(createEquipeCell());
        equipeFilterComboBox.setCellFactory(listView -> createEquipeCell());
        equipeFilterComboBox.setButtonCell(createEquipeCell());
    }

    private ListCell<Equipe> createEquipeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNom());
            }
        };
    }

    private void configureNumeroField() {
        numeroField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d{0,2}") ? change : null;
        }));
    }

    private void configurePlayerList() {
        joueurListView.setItems(filteredJoueurs);
        joueurListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Joueur item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(buildPlayerCard(item));
            }
        });

        joueurListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedJoueur = newValue;

            if (newValue != null) {
                populateForm(newValue);
            } else if (!hasDraftContent()) {
                clearFormFieldsOnly();
            }

            clearValidation();
            updateActionAvailability();
            updateDetailPanel();
        });
    }

    private void bindUiState() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        equipeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        nomField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(nomField);
            updateDetailPanel();
        });
        prenomField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(prenomField);
            updateDetailPanel();
        });
        dateNaissancePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(dateNaissancePicker);
            updateDetailPanel();
        });
        numeroField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(numeroField);
            updateDetailPanel();
        });
        equipeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(equipeComboBox);
            updateDetailPanel();
        });
        imageField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(imageField);
            updateDetailPanel();
        });
    }

    private VBox buildPlayerCard(Joueur joueur) {
        HBox root = new HBox(16);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("player-list-card");

        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("player-avatar-shell");
        avatarShell.setMinSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
        avatarShell.setPrefSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);
        avatarShell.setMaxSize(CARD_IMAGE_SIZE, CARD_IMAGE_SIZE);

        Image image = loadImage(joueur.getImage());
        if (image != null) {
            ImageView avatarView = new ImageView(image);
            avatarView.setFitWidth(CARD_IMAGE_SIZE - 12);
            avatarView.setFitHeight(CARD_IMAGE_SIZE - 12);
            avatarView.setPreserveRatio(true);
            avatarShell.getChildren().add(avatarView);
        } else {
            Label fallback = new Label(buildInitials(joueur));
            fallback.getStyleClass().add("player-avatar-fallback");
            avatarShell.getChildren().add(fallback);
        }

        VBox content = new VBox(7);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(buildFullName(joueur));
        titleLabel.getStyleClass().add("player-card-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label numberLabel = new Label("#" + joueur.getNumero());
        numberLabel.getStyleClass().add("player-number-badge");

        titleRow.getChildren().addAll(titleLabel, numberLabel);

        Label teamLabel = new Label(getEquipeName(joueur.getEquipeId()));
        teamLabel.getStyleClass().add("player-card-team");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label birthLabel = new Label("Ne le " + formatDate(joueur.getDateNaissance()));
        birthLabel.getStyleClass().add("player-card-meta");

        Label ageLabel = new Label(formatAge(joueur.getDateNaissance()));
        ageLabel.getStyleClass().add("player-card-meta-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        metaRow.getChildren().addAll(birthLabel, spacer, ageLabel);
        content.getChildren().addAll(titleRow, teamLabel, metaRow);

        root.getChildren().addAll(avatarShell, content);
        return new VBox(root);
    }

    private void refreshData(Integer preferredSelectionId) {
        if (joueurService == null || equipeService == null) {
            return;
        }

        try {
            loadEquipes();
            List<Joueur> loadedJoueurs = new ArrayList<>(joueurService.getAll());
            loadedJoueurs.sort(Comparator
                    .comparing(this::getEquipeNameForSort, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Joueur::getNumero)
                    .thenComparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(Joueur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

            joueurs.setAll(loadedJoueurs);
            applyFilters();
            restoreSelection(preferredSelectionId);
            updateDetailPanel();
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant le chargement.");
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des joueurs.\n" + e.getMessage());
        }
    }

    private void loadEquipes() throws SQLException {
        Integer selectedFormEquipeId = getSelectedFormEquipeId();
        Integer selectedFilterEquipeId = getSelectedFilterEquipeId();

        List<Equipe> loadedEquipes = new ArrayList<>(equipeService.getAll());
        loadedEquipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        equipeById.clear();
        for (Equipe equipe : loadedEquipes) {
            if (equipe.getId() != null) {
                equipeById.put(equipe.getId(), equipe);
            }
        }

        equipes.setAll(loadedEquipes);
        selectEquipeInForm(selectedFormEquipeId);
        selectEquipeInFilter(selectedFilterEquipeId);
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        Integer filterEquipeId = getSelectedFilterEquipeId();

        filteredJoueurs.setPredicate(joueur -> {
            boolean matchesQuery = query == null
                    || normalize(buildFullName(joueur)).contains(query)
                    || normalize(getEquipeName(joueur.getEquipeId())).contains(query)
                    || normalize(emptyIfNull(joueur.getNom())).contains(query)
                    || normalize(emptyIfNull(joueur.getPrenom())).contains(query);

            boolean matchesEquipe = filterEquipeId == null || Objects.equals(joueur.getEquipeId(), filterEquipeId);
            return matchesQuery && matchesEquipe;
        });

        updateCounters();
        updateEmptyState();

        if (selectedJoueur != null && !filteredJoueurs.contains(selectedJoueur)) {
            joueurListView.getSelectionModel().clearSelection();
        }
    }

    private void updateCounters() {
        int joueursCount = filteredJoueurs.size();
        long equipesCount = filteredJoueurs.stream()
                .map(Joueur::getEquipeId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        resultCountLabel.setText(joueursCount + " joueur(s)");
        resultsMetaLabel.setText(joueursCount + " resultat(s)");
        teamCountLabel.setText(equipesCount + " equipe(s)");
    }

    private void updateEmptyState() {
        boolean empty = filteredJoueurs.isEmpty();
        emptyStateBox.setManaged(empty);
        emptyStateBox.setVisible(empty);
    }

    private void restoreSelection(Integer preferredSelectionId) {
        if (preferredSelectionId == null) {
            selectedJoueur = null;
            joueurListView.getSelectionModel().clearSelection();
            updateActionAvailability();
            return;
        }

        for (Joueur joueur : filteredJoueurs) {
            if (Objects.equals(joueur.getId(), preferredSelectionId)) {
                joueurListView.getSelectionModel().select(joueur);
                joueurListView.scrollTo(joueur);
                selectedJoueur = joueur;
                updateActionAvailability();
                return;
            }
        }

        selectedJoueur = null;
        joueurListView.getSelectionModel().clearSelection();
        updateActionAvailability();
    }

    private void populateForm(Joueur joueur) {
        nomField.setText(emptyIfNull(joueur.getNom()));
        prenomField.setText(emptyIfNull(joueur.getPrenom()));
        dateNaissancePicker.setValue(joueur.getDateNaissance());
        numeroField.setText(joueur.getNumero() == 0 ? "" : String.valueOf(joueur.getNumero()));
        imageField.setText(emptyIfNull(joueur.getImage()));
        selectEquipeInForm(joueur.getEquipeId());
    }

    private Joueur buildJoueurFromForm(boolean updateMode) {
        String nom = emptyToNull(nomField.getText());
        String prenom = emptyToNull(prenomField.getText());
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String numeroText = emptyToNull(numeroField.getText());
        String image = emptyToNull(imageField.getText());
        Equipe equipe = equipeComboBox.getValue();

        if (nom == null) {
            markFieldInvalid(nomField);
            showValidation("Le nom est obligatoire.");
            return null;
        }

        if (prenom == null) {
            markFieldInvalid(prenomField);
            showValidation("Le prenom est obligatoire.");
            return null;
        }

        if (dateNaissance == null) {
            markFieldInvalid(dateNaissancePicker);
            showValidation("La date de naissance est obligatoire.");
            return null;
        }

        if (numeroText == null) {
            markFieldInvalid(numeroField);
            showValidation("Le numero est obligatoire.");
            return null;
        }

        int numero;
        try {
            numero = Integer.parseInt(numeroText);
        } catch (NumberFormatException e) {
            markFieldInvalid(numeroField);
            showValidation("Le numero doit etre un nombre.");
            return null;
        }

        if (numero < 1 || numero > 99) {
            markFieldInvalid(numeroField);
            showValidation("Le numero doit etre entre 1 et 99.");
            return null;
        }

        if (equipe == null || equipe.getId() == null) {
            markFieldInvalid(equipeComboBox);
            showValidation("L'equipe est obligatoire.");
            return null;
        }

        if (nom.length() > 100) {
            markFieldInvalid(nomField);
            showValidation("Le nom ne peut pas depasser 100 caracteres.");
            return null;
        }

        if (prenom.length() > 100) {
            markFieldInvalid(prenomField);
            showValidation("Le prenom ne peut pas depasser 100 caracteres.");
            return null;
        }

        if (updateMode && selectedJoueur == null) {
            showValidation("Selectionnez un joueur avant de modifier.");
            return null;
        }

        return new Joueur(nom, prenom, dateNaissance, numero, image, equipe.getId());
    }

    private void clearForm() {
        joueurListView.getSelectionModel().clearSelection();
        selectedJoueur = null;
        clearFormFieldsOnly();
        clearValidation();
        updateActionAvailability();
        updateDetailPanel();
    }

    private void clearFormFieldsOnly() {
        nomField.clear();
        prenomField.clear();
        dateNaissancePicker.setValue(null);
        numeroField.clear();
        imageField.clear();
        equipeComboBox.getSelectionModel().clearSelection();
    }

    private void updateDetailPanel() {
        String fullName = buildDraftFullName();
        if (fullName == null && selectedJoueur != null) {
            fullName = buildFullName(selectedJoueur);
        }

        Equipe selectedEquipe = equipeComboBox.getValue();
        String equipeName = selectedEquipe == null ? null : emptyToNull(selectedEquipe.getNom());
        if (equipeName == null && selectedJoueur != null) {
            equipeName = sanitizeDash(getEquipeName(selectedJoueur.getEquipeId()));
        }

        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String numeroValue = emptyToNull(numeroField.getText());
        String imagePath = emptyToNull(imageField.getText());

        boolean editing = selectedJoueur != null;
        boolean drafting = editing || hasDraftContent();

        detailBadgeLabel.setText(editing ? "Edition" : drafting ? "Creation" : "Apercu");
        selectionStateLabel.setText(editing ? "Mode edition" : "Mode creation");
        formHintLabel.setText(editing
                ? "Modifiez la fiche selectionnee puis enregistrez vos changements."
                : "Composez une nouvelle fiche joueur et visualisez-la a droite.");

        detailNameLabel.setText(fullName == null ? "Aucun joueur selectionne" : fullName);
        detailSubtitleLabel.setText(buildDetailSubtitle(equipeName, dateNaissance, numeroValue, drafting));
        detailIdValueLabel.setText(editing && selectedJoueur.getId() != null ? "#" + selectedJoueur.getId() : "Nouveau");
        detailEquipeValueLabel.setText(equipeName == null ? "Aucune" : equipeName);
        detailNumeroValueLabel.setText(numeroValue == null ? "Non defini" : "#" + numeroValue);

        Image image = loadImage(imagePath);
        boolean hasImage = image != null;
        detailImageView.setImage(image);
        detailImageView.setManaged(hasImage);
        detailImageView.setVisible(hasImage);
        detailImageFallbackLabel.setManaged(!hasImage);
        detailImageFallbackLabel.setVisible(!hasImage);
        detailImageFallbackLabel.setText(buildDraftInitials());
    }

    private String buildDetailSubtitle(String equipeName, LocalDate dateNaissance, String numeroValue, boolean drafting) {
        if (!drafting) {
            return "Selectionnez une carte ou commencez une nouvelle creation pour afficher la fiche detail.";
        }

        List<String> parts = new ArrayList<>();
        if (equipeName != null) {
            parts.add(equipeName);
        }
        if (dateNaissance != null) {
            parts.add("Ne le " + formatDate(dateNaissance));
        }
        if (numeroValue != null) {
            parts.add("Maillot #" + numeroValue);
        }

        return parts.isEmpty()
                ? "Commencez par saisir les informations principales du joueur."
                : String.join(" | ", parts);
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedJoueur != null;

        addButton.setDisable(!serviceReady);
        updateButton.setDisable(!serviceReady || !hasSelection);
        deleteButton.setDisable(!serviceReady || !hasSelection);
        clearButton.setDisable(!serviceReady);
        refreshButton.setDisable(!serviceReady);
    }

    private boolean hasDraftContent() {
        return emptyToNull(nomField.getText()) != null
                || emptyToNull(prenomField.getText()) != null
                || dateNaissancePicker.getValue() != null
                || emptyToNull(numeroField.getText()) != null
                || emptyToNull(imageField.getText()) != null
                || equipeComboBox.getValue() != null;
    }

    private Integer getSelectedJoueurId() {
        return selectedJoueur == null ? null : selectedJoueur.getId();
    }

    private Integer getSelectedFormEquipeId() {
        Equipe selectedEquipe = equipeComboBox.getValue();
        return selectedEquipe == null ? null : selectedEquipe.getId();
    }

    private Integer getSelectedFilterEquipeId() {
        Equipe selectedEquipe = equipeFilterComboBox.getValue();
        return selectedEquipe == null ? null : selectedEquipe.getId();
    }

    private void selectEquipeInForm(Integer equipeId) {
        selectEquipeInComboBox(equipeComboBox, equipeId);
    }

    private void selectEquipeInFilter(Integer equipeId) {
        selectEquipeInComboBox(equipeFilterComboBox, equipeId);
    }

    private void selectEquipeInComboBox(ComboBox<Equipe> comboBox, Integer equipeId) {
        if (equipeId == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }

        for (Equipe equipe : equipes) {
            if (Objects.equals(equipe.getId(), equipeId)) {
                comboBox.getSelectionModel().select(equipe);
                return;
            }
        }

        comboBox.getSelectionModel().clearSelection();
    }

    private String getEquipeName(Integer equipeId) {
        Equipe equipe = equipeId == null ? null : equipeById.get(equipeId);
        return equipe == null ? "-" : emptyIfNull(equipe.getNom());
    }

    private String getEquipeNameForSort(Joueur joueur) {
        String equipeName = getEquipeName(joueur.getEquipeId());
        return "-".equals(equipeName) ? "zzzz" : equipeName;
    }

    private String buildFullName(Joueur joueur) {
        String prenom = emptyIfNull(joueur.getPrenom()).trim();
        String nom = emptyIfNull(joueur.getNom()).trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Joueur" : fullName;
    }

    private String buildDraftFullName() {
        String prenom = emptyToNull(prenomField.getText());
        String nom = emptyToNull(nomField.getText());
        String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private String buildInitials(Joueur joueur) {
        return buildInitials(new String[]{emptyToNull(joueur.getPrenom()), emptyToNull(joueur.getNom())}, "J");
    }

    private String buildDraftInitials() {
        return buildInitials(new String[]{emptyToNull(prenomField.getText()), emptyToNull(nomField.getText())}, "J");
    }

    private String buildInitials(String[] values, String fallback) {
        StringBuilder initials = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                initials.append(Character.toUpperCase(value.charAt(0)));
            }
        }
        return initials.isEmpty() ? fallback : initials.toString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatAge(LocalDate date) {
        if (date == null) {
            return "Age indisponible";
        }
        int years = Period.between(date, LocalDate.now()).getYears();
        return years + " ans";
    }

    private void showMutedStatus(String message) {
        setStatus(message, "status-muted");
    }

    private void showSuccessStatus(String message) {
        setStatus(message, "status-success");
    }

    private void showErrorStatus(String message) {
        setStatus(message, "status-error");
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
        setStatusStyle(styleClass);
    }

    private void setStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
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

        clearFieldError(nomField);
        clearFieldError(prenomField);
        clearFieldError(dateNaissancePicker);
        clearFieldError(numeroField);
        clearFieldError(imageField);
        clearFieldError(equipeComboBox);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private void applySidebarState(boolean visible) {
        sidebarRoot.setManaged(visible);
        sidebarRoot.setVisible(visible);
        sidebarRoot.setMinWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(visible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarOpenButton.setManaged(!visible);
        sidebarOpenButton.setVisible(!visible);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String sanitizeDash(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }
        return value;
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();

        Image image = loadImageFromUri(normalizedPath);
        if (image != null) {
            return image;
        }

        Path directPath = toPathIfValid(normalizedPath);
        if (directPath != null && directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        URL resource = resolveResource(normalizedPath);
        if (resource != null) {
            return createImage(resource.toExternalForm());
        }

        if (directPath != null && !directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        for (Path candidate : buildRelativeCandidates(normalizedPath)) {
            image = loadImageFromFile(candidate);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    private URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/joueurs/" + imagePath,
                "/uploads/joueurs/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = JoueurController.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "joueurs"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "joueurs"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit", "images"), imagePath);
        appendCandidate(candidates, SYMFONY_JOUEURS_DIRECTORY, imagePath);
        return candidates;
    }

    private void appendCandidate(List<Path> candidates, Path base, String imagePath) {
        Path childPath = toPathIfValid(imagePath);
        if (childPath == null || childPath.isAbsolute()) {
            return;
        }

        candidates.add(base.resolve(childPath));
    }

    private Image loadImageFromUri(String imagePath) {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
            return createImage(imagePath);
        }
        return null;
    }

    private Path toPathIfValid(String pathValue) {
        try {
            return Path.of(pathValue);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private Image loadImageFromFile(Path path) {
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        Image image = createImage(file.toURI().toString());
        if (image != null) {
            return image;
        }

        return loadImageWithImageIo(file);
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Image loadImageWithImageIo(File file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                return null;
            }
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
