package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Equipe;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;

import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class EquipeController {
    private static final double CARD_LOGO_SIZE = 78;
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("equipe-db-worker"));

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Label sidebarSectionLabel;
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
    private TextField searchField;
    @FXML
    private ComboBox<String> sortChoiceBox;
    @FXML
    private Button sortOrderButton;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Equipe> equipeListView;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Label detailBadgeLabel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailCoachValueLabel;
    @FXML
    private Label detailStatusValueLabel;
    @FXML
    private ImageView detailLogoView;
    @FXML
    private Label detailLogoFallbackLabel;
    @FXML
    private Label formHintLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private TextField nomField;
    @FXML
    private TextField coachField;
    @FXML
    private TextField imageField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button clearButton;

    private final ObservableList<Equipe> masterEquipes = FXCollections.observableArrayList();
    private final ObservableList<Equipe> displayedEquipes = FXCollections.observableArrayList();
    private final AtomicLong refreshSequence = new AtomicLong();
    private final Map<String, Optional<Image>> imageCache = new ConcurrentHashMap<>();

    private EquipeService equipeService;
    private File lastImageDirectory;
    private boolean sortDescending;
    private boolean serviceReady;
    private boolean sidebarVisible;
    private boolean loadingData;
    private boolean mutatingData;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureToolbar();
        configureListView();
        bindFormPreview();
        updateSortOrderButtonText();
        updateFormMode();
        updateDetailCard();
        updateActionAvailability();

        try {
            equipeService = new EquipeService();
            serviceReady = true;
            refreshTableAsync(null, "Chargement des equipes...", "status-muted", "Connexion etablie. Module Equipe pret.");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showStatus("status-error", "Connexion a la base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les equipes.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        clearValidation();

        Equipe equipe = buildEquipeFromForm(false);
        if (equipe == null || equipeService == null) {
            return;
        }

        runMutation(
                () -> equipeService.add(equipe),
                null,
                true,
                "Equipe ajoutee avec succes.",
                "Ajout",
                "Erreur lors de l'ajout :",
                "Erreur lors de l'ajout de l'equipe."
        );
    }

    @FXML
    private void handleUpdate() {
        clearValidation();

        Equipe selectedEquipe = equipeListView.getSelectionModel().getSelectedItem();
        if (selectedEquipe == null) {
            showValidation("Selectionnez une equipe avant de lancer une modification.");
            return;
        }

        Equipe equipe = buildEquipeFromForm(true);
        if (equipe == null || equipeService == null) {
            return;
        }

        equipe.setId(selectedEquipe.getId());

        runMutation(
                () -> equipeService.update(equipe),
                selectedEquipe.getId(),
                false,
                "Equipe modifiee avec succes.",
                "Modification",
                "Erreur lors de la modification :",
                "Erreur lors de la modification de l'equipe."
        );
    }

    @FXML
    private void handleDelete() {
        clearValidation();

        Equipe selectedEquipe = equipeListView.getSelectionModel().getSelectedItem();
        if (selectedEquipe == null) {
            showValidation("Selectionnez une equipe a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer l'equipe \"" + emptyIfNull(selectedEquipe.getNom()) + "\" ?");
        alert.setContentText("Cette action est definitive et ne pourra pas etre annulee.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        runMutation(
                () -> equipeService.delete(selectedEquipe.getId()),
                null,
                true,
                "Equipe supprimee avec succes.",
                "Suppression",
                "Erreur lors de la suppression :",
                "Erreur lors de la suppression de l'equipe."
        );
    }

    @FXML
    private void handleRefresh() {
        refreshTableAsync(
                getSelectedEquipeId(),
                "Actualisation des equipes...",
                "status-muted",
                "Liste actualisee depuis la base de donnees."
        );
    }

    @FXML
    private void handleClear() {
        clearForm();
        showStatus("status-muted", "Mode creation active.");
    }

    @FXML
    private void handleToggleSortOrder() {
        sortDescending = !sortDescending;
        updateSortOrderButtonText();
        applyFiltersAndSort(getSelectedEquipeId());
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortChoiceBox.setValue("Nom");
        sortDescending = false;
        updateSortOrderButtonText();
        applyFiltersAndSort(getSelectedEquipeId());
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
    private void handleToggleSidebar() {
        hideSidebar();
    }

    @FXML
    private void handleOpenSidebar() {
        showSidebar();
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenEquipes() {
        showStatus("status-muted", "Module Equipes deja actif.");
    }

    @FXML
    private void handleOpenMatchsSoon() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    @FXML
    private void handleOpenJoueursSoon() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    private void configureSidebar() {
        sidebarVisible = true;
        if (!equipesNavButton.getStyleClass().contains("sidebar-nav-button-active")) {
            equipesNavButton.getStyleClass().add("sidebar-nav-button-active");
        }
        applySidebarState();
    }

    private void configureToolbar() {
        sortChoiceBox.setItems(FXCollections.observableArrayList("Nom", "Coach", "Id"));
        sortChoiceBox.setValue("Nom");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort(getSelectedEquipeId()));
        sortChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort(getSelectedEquipeId()));
    }

    private void configureListView() {
        equipeListView.setItems(displayedEquipes);
        equipeListView.setCellFactory(listView -> createEquipeCardCell());
        equipeListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateForm(newValue);
            } else if (!hasDraftContent()) {
                clearFormFieldsOnly();
            }

            clearValidation();
            updateFormMode();
            updateDetailCard();
            updateActionAvailability();
        });
    }

    private void bindFormPreview() {
        nomField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(nomField);
            updateDetailCard();
        });
        coachField.textProperty().addListener((observable, oldValue, newValue) -> updateDetailCard());
        imageField.textProperty().addListener((observable, oldValue, newValue) -> updateDetailCard());
    }

    private void refreshTableAsync(
            Integer preferredSelectionId,
            String loadingMessage,
            String successStyleClass,
            String successMessage
    ) {
        if (equipeService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        loadingData = true;
        updateActionAvailability();
        if (loadingMessage != null) {
            showStatus("status-muted", loadingMessage);
        }

        Task<List<Equipe>> loadTask = new Task<>() {
            @Override
            protected List<Equipe> call() throws Exception {
                return equipeService.getAll();
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            masterEquipes.setAll(loadTask.getValue());
            applyFiltersAndSort(preferredSelectionId);
            loadingData = false;
            updateActionAvailability();

            if (successMessage != null) {
                showStatus(successStyleClass == null ? "status-muted" : successStyleClass, successMessage);
            }
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            loadingData = false;
            updateActionAvailability();
            showStatus("status-error", "Erreur lors du chargement des equipes.");
            Throwable throwable = loadTask.getException();
            String details = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showAlert(Alert.AlertType.ERROR, "Chargement", "Erreur lors du chargement des equipes :\n" + details);
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void applyFiltersAndSort(Integer preferredSelectionId) {
        List<Equipe> filteredEquipes = new ArrayList<>(masterEquipes);

        String keyword = normalize(searchField.getText());
        if (!keyword.isEmpty()) {
            filteredEquipes.removeIf(equipe -> !matchesSearch(equipe, keyword));
        }

        filteredEquipes.sort(buildComparator());
        displayedEquipes.setAll(filteredEquipes);

        boolean isEmpty = filteredEquipes.isEmpty();
        emptyStateBox.setManaged(isEmpty);
        emptyStateBox.setVisible(isEmpty);

        resultCountLabel.setText(filteredEquipes.size() + " equipe(s)");
        resultsMetaLabel.setText(filteredEquipes.size() + " resultat(s)");

        restoreSelection(preferredSelectionId);
        updateDetailCard();
    }

    private boolean matchesSearch(Equipe equipe, String keyword) {
        return normalize(equipe.getNom()).contains(keyword) || normalize(equipe.getCoach()).contains(keyword);
    }

    private Comparator<Equipe> buildComparator() {
        Comparator<Equipe> comparator;
        String selectedSort = sortChoiceBox.getValue();

        if ("Id".equals(selectedSort)) {
            comparator = Comparator.comparing(Equipe::getId, Comparator.nullsLast(Integer::compareTo));
        } else if ("Coach".equals(selectedSort)) {
            comparator = Comparator.comparing(Equipe::getCoach, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else {
            comparator = Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return sortDescending ? comparator.reversed() : comparator;
    }

    private void restoreSelection(Integer preferredSelectionId) {
        if (preferredSelectionId == null) {
            Equipe currentlySelected = equipeListView.getSelectionModel().getSelectedItem();
            if (currentlySelected != null && displayedEquipes.stream().noneMatch(equipe -> Objects.equals(equipe.getId(), currentlySelected.getId()))) {
                equipeListView.getSelectionModel().clearSelection();
            }
            return;
        }

        for (Equipe equipe : displayedEquipes) {
            if (Objects.equals(equipe.getId(), preferredSelectionId)) {
                equipeListView.getSelectionModel().select(equipe);
                equipeListView.scrollTo(equipe);
                return;
            }
        }

        equipeListView.getSelectionModel().clearSelection();
    }

    private void populateForm(Equipe equipe) {
        nomField.setText(emptyIfNull(equipe.getNom()));
        coachField.setText(emptyIfNull(equipe.getCoach()));
        imageField.setText(emptyIfNull(equipe.getImage()));
    }

    private Equipe buildEquipeFromForm(boolean updateMode) {
        String nom = emptyToNull(nomField.getText());
        String coach = emptyToNull(coachField.getText());
        String image = emptyToNull(imageField.getText());

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

        if (updateMode && equipeListView.getSelectionModel().getSelectedItem() == null) {
            showValidation("Selectionnez une equipe avant de lancer une modification.");
            return null;
        }

        return new Equipe(nom, coach, null, null, null, image);
    }

    private void clearForm() {
        equipeListView.getSelectionModel().clearSelection();
        clearFormFieldsOnly();
        clearValidation();
        updateFormMode();
        updateDetailCard();
        updateActionAvailability();
    }

    private void clearFormFieldsOnly() {
        nomField.clear();
        coachField.clear();
        imageField.clear();
    }

    private void updateFormMode() {
        Equipe selectedEquipe = equipeListView.getSelectionModel().getSelectedItem();
        if (selectedEquipe == null) {
            selectionStateLabel.setText(hasDraftContent() ? "Brouillon en cours" : "Mode creation");
            formHintLabel.setText("Composez une nouvelle fiche equipe avec le meme esprit que le front-office Symfony.");
            detailBadgeLabel.setText(hasDraftContent() ? "Brouillon" : "Apercu");
        } else {
            selectionStateLabel.setText("Selection : " + emptyIfNull(selectedEquipe.getNom()));
            formHintLabel.setText("Modification en cours de la fiche #" + selectedEquipe.getId() + ".");
            detailBadgeLabel.setText("Equipe selectionnee");
        }
    }

    private void updateDetailCard() {
        Equipe selectedEquipe = equipeListView.getSelectionModel().getSelectedItem();
        String draftName = emptyToNull(nomField.getText());
        String draftCoach = emptyToNull(coachField.getText());
        String draftImage = emptyToNull(imageField.getText());

        if (selectedEquipe == null && !hasDraftContent()) {
            detailNameLabel.setText("Aucune equipe selectionnee");
            detailSubtitleLabel.setText("Selectionnez une carte ou commencez une nouvelle creation pour afficher la fiche detail.");
            detailIdValueLabel.setText("Nouveau");
            detailCoachValueLabel.setText("Non renseigne");
            detailStatusValueLabel.setText("Sans logo");
            updateDetailLogo(null, "SI");
            return;
        }

        String effectiveName = draftName != null ? draftName : selectedEquipe != null ? emptyToNull(selectedEquipe.getNom()) : "Nouvelle equipe";
        String effectiveCoach = draftCoach != null ? draftCoach : selectedEquipe != null ? emptyToNull(selectedEquipe.getCoach()) : null;
        String effectiveImage = draftImage != null ? draftImage : selectedEquipe != null ? emptyToNull(selectedEquipe.getImage()) : null;

        detailNameLabel.setText(effectiveName == null ? "Nouvelle equipe" : effectiveName);
        detailSubtitleLabel.setText(
                effectiveCoach == null
                        ? "Coach non renseigne. Vous pouvez enregistrer la fiche avec uniquement le nom."
                        : "Coach principal : " + effectiveCoach
        );
        detailIdValueLabel.setText(selectedEquipe == null ? "Nouveau" : "#" + selectedEquipe.getId());
        detailCoachValueLabel.setText(effectiveCoach == null ? "Non renseigne" : effectiveCoach);
        detailStatusValueLabel.setText(effectiveImage == null ? "Sans logo" : "Logo pret");
        updateDetailLogo(effectiveImage, effectiveName);
    }

    private void updateDetailLogo(String imagePath, String teamName) {
        Image image = loadImage(imagePath);
        boolean hasImage = image != null;

        detailLogoView.setImage(image);
        detailLogoView.setManaged(hasImage);
        detailLogoView.setVisible(hasImage);
        detailLogoFallbackLabel.setManaged(!hasImage);
        detailLogoFallbackLabel.setVisible(!hasImage);
        detailLogoFallbackLabel.setText(buildInitials(teamName));
    }

    private ListCell<Equipe> createEquipeCardCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Equipe equipe, boolean empty) {
                super.updateItem(equipe, empty);

                if (empty || equipe == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                StackPane logoPane = createLogoPane(equipe.getImage(), equipe.getNom(), CARD_LOGO_SIZE);

                Label nameLabel = new Label(emptyIfNull(equipe.getNom()));
                nameLabel.getStyleClass().add("card-title");
                nameLabel.setWrapText(true);

                String coach = emptyToNull(equipe.getCoach());
                Label coachLabel = new Label(coach == null ? "Coach non renseigne" : "Coach : " + coach);
                coachLabel.getStyleClass().add(coach == null ? "card-subtitle-muted" : "card-subtitle");
                coachLabel.setWrapText(true);

                String logoState = emptyToNull(equipe.getImage()) == null ? "Sans logo" : "Logo disponible";
                Label metaLabel = new Label("#" + equipe.getId() + "  |  " + logoState);
                metaLabel.getStyleClass().add("card-meta");

                Label ctaLabel = new Label("Ouvrir la fiche");
                ctaLabel.getStyleClass().add("card-link");

                VBox textBox = new VBox(6, nameLabel, coachLabel, metaLabel, ctaLabel);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox card = new HBox(16, logoPane, textBox);
                card.setAlignment(Pos.CENTER_LEFT);
                card.getStyleClass().add("team-list-card");

                setText(null);
                setGraphic(card);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private StackPane createLogoPane(String imagePath, String teamName, double size) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("card-logo-image");

        Label fallbackLabel = new Label(buildInitials(teamName));
        fallbackLabel.getStyleClass().add("card-logo-fallback");

        Image image = loadImage(imagePath);
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setVisible(hasImage);
        imageView.setManaged(hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setManaged(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(size, size);
        logoPane.setPrefSize(size, size);
        logoPane.setMaxSize(size, size);
        logoPane.getStyleClass().add("card-logo-shell");
        return logoPane;
    }

    private void updateActionAvailability() {
        boolean hasSelection = equipeListView.getSelectionModel().getSelectedItem() != null;
        boolean busy = loadingData || mutatingData;

        addButton.setDisable(!serviceReady || busy);
        updateButton.setDisable(!serviceReady || !hasSelection || busy);
        deleteButton.setDisable(!serviceReady || !hasSelection || busy);
        refreshButton.setDisable(!serviceReady || busy);
        clearButton.setDisable(!serviceReady || busy);
    }

    private boolean hasDraftContent() {
        return emptyToNull(nomField.getText()) != null
                || emptyToNull(coachField.getText()) != null
                || emptyToNull(imageField.getText()) != null;
    }

    private Integer getSelectedEquipeId() {
        Equipe selectedEquipe = equipeListView.getSelectionModel().getSelectedItem();
        return selectedEquipe == null ? null : selectedEquipe.getId();
    }

    private void updateSortOrderButtonText() {
        sortOrderButton.setText(sortDescending ? "Decroissant" : "Croissant");
    }

    private void applySidebarState() {
        sidebarRoot.setManaged(sidebarVisible);
        sidebarRoot.setVisible(sidebarVisible);
        sidebarSectionLabel.setManaged(sidebarVisible);
        sidebarSectionLabel.setVisible(sidebarVisible);
        sidebarOpenButton.setManaged(!sidebarVisible);
        sidebarOpenButton.setVisible(!sidebarVisible);

        sidebarBrandBox.setAlignment(Pos.CENTER_LEFT);
        sidebarToggleButton.setText("<");

        equipesNavButton.setText("Equipes");
        matchsNavButton.setText("Matchs");
        joueursNavButton.setText("Joueurs");

        sidebarRoot.setMinWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);

        updateSidebarStyleClass(sidebarVisible);
    }

    private void updateSidebarStyleClass(boolean visible) {
        sidebarRoot.getStyleClass().removeAll("sidebar-visible", "sidebar-hidden");
        sidebarRoot.getStyleClass().add(visible ? "sidebar-visible" : "sidebar-hidden");
    }

    private void showSidebar() {
        sidebarVisible = true;
        applySidebarState();
    }

    private void hideSidebar() {
        sidebarVisible = false;
        applySidebarState();
    }

    private void showStatus(String styleClass, String message) {
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning", "status-muted");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
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
        clearFieldError(coachField);
    }

    private void markFieldInvalid(TextField field) {
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(TextField field) {
        field.getStyleClass().remove("invalid-field");
    }

    private String buildInitials(String teamName) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return "SI";
        }

        String[] parts = normalizedName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }

        if (initials.length() == 0) {
            return "SI";
        }

        return initials.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
        Optional<Image> cachedImage = imageCache.get(normalizedPath);
        if (cachedImage != null) {
            return cachedImage.orElse(null);
        }

        Image resolvedImage = resolveImage(normalizedPath);
        imageCache.put(normalizedPath, Optional.ofNullable(resolvedImage));
        return resolvedImage;
    }

    private Image resolveImage(String normalizedPath) {
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

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }

    private void runMutation(
            SqlRunnable mutation,
            Integer preferredSelectionId,
            boolean clearFormOnSuccess,
            String successMessage,
            String errorTitle,
            String errorMessagePrefix,
            String errorStatusMessage
    ) {
        if (equipeService == null) {
            return;
        }

        mutatingData = true;
        updateActionAvailability();
        showStatus("status-muted", "Operation en cours...");

        Task<Void> mutationTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                mutation.run();
                return null;
            }
        };

        mutationTask.setOnSucceeded(event -> {
            mutatingData = false;
            updateActionAvailability();
            imageCache.clear();
            if (clearFormOnSuccess) {
                clearForm();
            }
            refreshTableAsync(preferredSelectionId, null, "status-success", successMessage);
        });

        mutationTask.setOnFailed(event -> {
            mutatingData = false;
            updateActionAvailability();
            showStatus("status-error", errorStatusMessage);
            Throwable throwable = mutationTask.getException();
            String details = throwable == null ? "Erreur inconnue." : throwable.getMessage();
            showAlert(Alert.AlertType.ERROR, errorTitle, errorMessagePrefix + "\n" + details);
        });

        DB_EXECUTOR.execute(mutationTask);
    }

    private URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/equipes/" + imagePath,
                "/uploads/equipes/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = EquipeController.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit", "images"), imagePath);
        appendCandidate(candidates, SYMFONY_UPLOADS_DIRECTORY, imagePath);
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

        return createImage(file.toURI().toString());
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ThreadFactory daemonFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

