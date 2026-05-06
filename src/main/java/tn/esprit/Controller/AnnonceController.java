package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.AnnoncePdfExportService;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.CommentCvStorageService;
import tn.esprit.services.CommentaireService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class AnnonceController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> ANNONCE_STATUTS = List.of("ACTIVE", "EN_ATTENTE", "EXPIREE", "CLOSED");
    private static final List<String> COMMENT_STATUTS = List.of("PENDING", "APPROVED", "REJECTED");
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("annonce-admin-db"));

    @FXML private HBox navbarRoot;
    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private Button equipesNavButton;
    @FXML private Button matchsNavButton;
    @FXML private Button annonceNavButton;
    @FXML private HBox sidebarModuleChildrenBox;
    @FXML private Button leaguesNavButton;
    @FXML private Button joueursNavButton;
    @FXML private ToggleButton themeToggleButton;
    @FXML private Label resultCountLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label totalAnnoncesMetricLabel;
    @FXML private Label activeAnnoncesMetricLabel;
    @FXML private Label urgentAnnoncesMetricLabel;
    @FXML private Label pendingCommentsMetricLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> posteFilterComboBox;
    @FXML private ComboBox<String> statutFilterComboBox;
    @FXML private CheckBox urgentOnlyCheck;
    @FXML private Label resultsMetaLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Annonce> annonceTableView;
    @FXML private TableColumn<Annonce, Integer> annonceIdColumn;
    @FXML private TableColumn<Annonce, String> annonceTitreColumn;
    @FXML private TableColumn<Annonce, String> annoncePosteColumn;
    @FXML private TableColumn<Annonce, String> annonceNiveauColumn;
    @FXML private TableColumn<Annonce, String> annonceDateColumn;
    @FXML private TableColumn<Annonce, String> annonceStatutColumn;
    @FXML private TableColumn<Annonce, String> annonceUrgentColumn;
    @FXML private TableColumn<Annonce, Integer> annonceCommentairesColumn;
    @FXML private Label detailBadgeLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailCommentsValueLabel;
    @FXML private Label detailOwnerValueLabel;
    @FXML private Label detailUrgentValueLabel;
    @FXML private Label formHintLabel;
    @FXML private Label validationLabel;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField posteField;
    @FXML private TextField niveauField;
    @FXML private DatePicker datePublicationPicker;
    @FXML private ComboBox<String> statutComboBox;
    @FXML private TextField entraineurIdField;
    @FXML private CheckBox commentsEnabledCheck;
    @FXML private CheckBox urgentCheck;
    @FXML private Label selectedAnnonceCommentsLabel;
    @FXML private Label commentResultsMetaLabel;
    @FXML private Label commentStatusLabel;
    @FXML private TableView<Commentaire> commentaireTableView;
    @FXML private TableColumn<Commentaire, Integer> commentaireIdColumn;
    @FXML private TableColumn<Commentaire, String> commentaireAuteurColumn;
    @FXML private TableColumn<Commentaire, String> commentaireContenuColumn;
    @FXML private TableColumn<Commentaire, String> commentaireDateColumn;
    @FXML private TableColumn<Commentaire, Integer> commentaireLikesColumn;
    @FXML private TableColumn<Commentaire, String> commentaireModerationColumn;
    @FXML private Label commentFormHintLabel;
    @FXML private Label commentValidationLabel;
    @FXML private TextField auteurField;
    @FXML private TextField joueurIdField;
    @FXML private DatePicker commentDatePicker;
    @FXML private TextField likesField;
    @FXML private ComboBox<String> moderationComboBox;
    @FXML private TextArea moderationReasonArea;
    @FXML private TextArea commentaireArea;
    @FXML private Button addAnnonceButton;
    @FXML private Button updateAnnonceButton;
    @FXML private Button deleteAnnonceButton;
    @FXML private Button clearAnnonceButton;
    @FXML private Button exportPdfButton;
    @FXML private Button addCommentButton;
    @FXML private Button updateCommentButton;
    @FXML private Button deleteCommentButton;
    @FXML private Button clearCommentButton;

    private final ObservableList<Annonce> annonces = FXCollections.observableArrayList();
    private final ObservableList<Commentaire> commentaires = FXCollections.observableArrayList();
    private final ObservableList<Commentaire> visibleCommentaires = FXCollections.observableArrayList();
    private final FilteredList<Annonce> filteredAnnonces = new FilteredList<>(annonces, annonce -> true);
    private final java.util.Map<Integer, Integer> commentCounts = new java.util.HashMap<>();

    private SidebarModuleGroup sidebarModuleGroup;
    private AnnonceService annonceService;
    private CommentaireService commentaireService;
    private CommentCvStorageService commentCvStorageService;
    private AnnoncePdfExportService annoncePdfExportService;
    private Annonce selectedAnnonce;
    private Commentaire selectedCommentaire;
    private boolean serviceReady;
    private final AtomicLong refreshSequence = new AtomicLong();

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabels();
        configureInputs();
        configureTables();
        bindUi();

        try {
            annonceService = new AnnonceService();
            commentaireService = new CommentaireService();
            commentCvStorageService = new CommentCvStorageService();
            annoncePdfExportService = new AnnoncePdfExportService();
            serviceReady = true;
            refreshData(null, null, "Loading announcement workspace...", "status-muted");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showErrorStatus("Database connection unavailable.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Could not load announcements.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshData(getSelectedAnnonceId(), getSelectedCommentaireId(), "Lists refreshed.", "status-muted");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        posteFilterComboBox.getSelectionModel().clearSelection();
        statutFilterComboBox.getSelectionModel().clearSelection();
        urgentOnlyCheck.setSelected(false);
        applyAnnonceFilters();
        showMutedStatus("Filters reset.");
    }

    @FXML
    private void handleAddAnnonce() {
        clearAnnonceValidation();
        Annonce annonce = buildAnnonceFromForm(false);
        if (annonce == null || annonceService == null) {
            return;
        }

        try {
            annonceService.add(annonce);
            refreshData(null, null, "Announcement added successfully.", "status-success");
            handleClearAnnonce();
        } catch (SQLException e) {
            showErrorStatus("Could not add the announcement.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Add failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateAnnonce() {
        clearAnnonceValidation();
        if (selectedAnnonce == null) {
            showAnnonceValidation("Select an announcement to update.");
            return;
        }

        Annonce annonce = buildAnnonceFromForm(true);
        if (annonce == null || annonceService == null) {
            return;
        }

        annonce.setId(selectedAnnonce.getId());

        try {
            annonceService.update(annonce);
            refreshData(annonce.getId(), null, "Announcement updated successfully.", "status-success");
        } catch (SQLException e) {
            showErrorStatus("Could not update the announcement.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Update failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteAnnonce() {
        clearAnnonceValidation();
        if (selectedAnnonce == null || annonceService == null) {
            showAnnonceValidation("Select an announcement to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete announcement");
        alert.setHeaderText("Delete \"" + selectedAnnonce.getTitre() + "\"?");
        alert.setContentText("This also removes its linked comments.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            annonceService.delete(selectedAnnonce.getId());
            refreshData(null, null, "Announcement deleted.", "status-success");
            handleClearAnnonce();
        } catch (SQLException e) {
            showErrorStatus("Could not delete the announcement.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Delete failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearAnnonce() {
        clearAnnonceSelection();
        clearAnnonceFormFields();
        clearAnnonceValidation();
        updateDetailPanel();
        updateActionAvailability();
        showMutedStatus("Announcement form cleared.");
    }

    @FXML
    private void handleExportPdf() {
        clearAnnonceValidation();
        if (!serviceReady || annoncePdfExportService == null) {
            showAnnonceValidation("The PDF export service is not ready.");
            return;
        }

        List<Annonce> annoncesToExport = new ArrayList<>(filteredAnnonces);
        if (annoncesToExport.isEmpty()) {
            showAnnonceValidation("There are no announcements to export with the current filters.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export announcements to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("annonces-export-" + LocalDate.now() + ".pdf");

        Window owner = exportPdfButton == null || exportPdfButton.getScene() == null
                ? null
                : exportPdfButton.getScene().getWindow();
        File targetFile = chooser.showSaveDialog(owner);
        if (targetFile == null) {
            showMutedStatus("PDF export cancelled.");
            return;
        }

        try {
            annoncePdfExportService.export(targetFile.toPath(), annoncesToExport, commentaires);
            showSuccessStatus("PDF exported to " + targetFile.getName() + ".");
        } catch (Exception e) {
            showErrorStatus("Could not export the PDF.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "PDF export failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAddCommentaire() {
        clearCommentValidation();
        showCommentValidation("Admin moderation can update or delete existing player comments. Creating new comments stays on the user interface.");
    }

    @FXML
    private void handleUpdateCommentaire() {
        clearCommentValidation();
        if (selectedCommentaire == null) {
            showCommentValidation("Select a comment to update.");
            return;
        }

        Commentaire commentaire = buildCommentaireFromForm(true);
        if (commentaire == null || commentaireService == null) {
            return;
        }

        commentaire.setId(selectedCommentaire.getId());

        try {
            commentaireService.update(commentaire);
            refreshData(getSelectedAnnonceId(), commentaire.getId(), "Comment updated successfully.", "status-success");
            showCommentSuccessStatus("Comment updated successfully.");
        } catch (SQLException e) {
            showCommentErrorStatus("Could not update the comment.");
            showAlert(Alert.AlertType.ERROR, "Comments", "Update failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteCommentaire() {
        clearCommentValidation();
        if (selectedCommentaire == null || commentaireService == null) {
            showCommentValidation("Select a comment to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete comment");
        alert.setHeaderText("Delete the selected comment?");
        alert.setContentText(selectedCommentaire.getAuteurAnonyme());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            if (commentCvStorageService != null) {
                commentCvStorageService.deleteQuietly(selectedCommentaire.getCvName());
            }
            commentaireService.delete(selectedCommentaire.getId());
            refreshData(getSelectedAnnonceId(), null, "Comment deleted.", "status-success");
            clearCommentFormFields();
            showCommentSuccessStatus("Comment deleted.");
        } catch (SQLException e) {
            showCommentErrorStatus("Could not delete the comment.");
            showAlert(Alert.AlertType.ERROR, "Comments", "Delete failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearCommentaire() {
        clearCommentSelection();
        clearCommentFormFields();
        clearCommentValidation();
        updateActionAvailability();
        updateCommentSection();
        showCommentMutedStatus(selectedAnnonce == null
                ? "All comments are visible. Select one to moderate it."
                : "Comment moderation selection cleared.");
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
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
    }

    private void configureStatusLabels() {
        setStatus(statusLabel, "Ready", "status-muted");
        setStatus(commentStatusLabel, "Select an announcement to manage comments.", "status-muted");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        commentValidationLabel.setManaged(false);
        commentValidationLabel.setVisible(false);
        detailStatusLabel.getStyleClass().add("status-pill");
    }

    private void configureInputs() {
        posteFilterComboBox.setPromptText("All roles");
        statutFilterComboBox.setPromptText("All statuses");
        statutFilterComboBox.setItems(FXCollections.observableArrayList(ANNONCE_STATUTS));
        statutComboBox.setItems(FXCollections.observableArrayList(ANNONCE_STATUTS));
        moderationComboBox.setItems(FXCollections.observableArrayList(COMMENT_STATUTS));

        configureNumericField(entraineurIdField);
        configureNumericField(joueurIdField);
        configureNumericField(likesField);

        clearAnnonceFormFields();
        clearCommentFormFields();
    }

    private void configureTables() {
        annonceIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        annonceTitreColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getTitre())));
        annoncePosteColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getPosteRecherche())));
        annonceNiveauColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getNiveauRequis())));
        annonceDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDatePublication())));
        annonceStatutColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getStatut())));
        annonceUrgentColumn.setCellValueFactory(cell -> new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getUrgent()) ? "Urgent" : "Normal"));
        annonceCommentairesColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(commentCounts.getOrDefault(cell.getValue().getId(), 0)));
        annonceTableView.setItems(filteredAnnonces);
        annonceTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        annonceTableView.setPlaceholder(new Label("No announcements found."));
        annonceTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> handleSelectedAnnonceChange(newValue));

        commentaireIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        commentaireAuteurColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getAuteurAnonyme())));
        commentaireContenuColumn.setCellValueFactory(cell -> new SimpleStringProperty(shorten(cell.getValue().getContenu(), 56)));
        commentaireDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDateCommentaire())));
        commentaireLikesColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getNbLikes()));
        commentaireModerationColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getModerationStatus())));
        commentaireTableView.setItems(visibleCommentaires);
        commentaireTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        commentaireTableView.setPlaceholder(new Label("No comments found."));
        commentaireTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> handleSelectedCommentaireChange(newValue));
    }

    private void bindUi() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyAnnonceFilters());
        posteFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyAnnonceFilters());
        statutFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyAnnonceFilters());
        urgentOnlyCheck.selectedProperty().addListener((obs, oldValue, newValue) -> applyAnnonceFilters());

        titreField.textProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(titreField);
            updateDetailPanel();
        });
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(descriptionArea);
            updateDetailPanel();
        });
        posteField.textProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(posteField);
            updateDetailPanel();
        });
        niveauField.textProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(niveauField);
            updateDetailPanel();
        });
        datePublicationPicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(datePublicationPicker);
            updateDetailPanel();
        });
        statutComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(statutComboBox);
            updateDetailPanel();
        });
        entraineurIdField.textProperty().addListener((obs, oldValue, newValue) -> {
            clearFieldError(entraineurIdField);
            updateDetailPanel();
        });
        commentsEnabledCheck.selectedProperty().addListener((obs, oldValue, newValue) -> updateDetailPanel());
        urgentCheck.selectedProperty().addListener((obs, oldValue, newValue) -> updateDetailPanel());

        auteurField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(auteurField));
        joueurIdField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(joueurIdField));
        commentDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(commentDatePicker));
        likesField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(likesField));
        moderationComboBox.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(moderationComboBox));
        commentaireArea.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(commentaireArea));
    }

    private void refreshData(Integer annonceIdToSelect, Integer commentaireIdToSelect, String statusMessage, String statusStyle) {
        if (!serviceReady || annonceService == null || commentaireService == null) {
            updateActionAvailability();
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        showMutedStatus(statusMessage == null ? "Loading announcements..." : statusMessage);

        Task<RefreshPayload> loadTask = new Task<>() {
            @Override
            protected RefreshPayload call() throws Exception {
                return new RefreshPayload(
                        new ArrayList<>(annonceService.getAll()),
                        new ArrayList<>(commentaireService.getAll())
                );
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            RefreshPayload payload = loadTask.getValue();
            annonces.setAll(payload.annonces());
            commentaires.setAll(payload.commentaires());
            rebuildCommentCounts();
            refreshPosteFilterValues();
            applyAnnonceFilters();
            updateMetrics();

            annonceTableView.getSelectionModel().clearSelection();
            selectedAnnonce = null;
            if (annonceIdToSelect != null) {
                selectAnnonceById(annonceIdToSelect);
            } else {
                applyCommentFilter();
                updateDetailPanel();
            }

            if (commentaireIdToSelect != null) {
                selectCommentaireById(commentaireIdToSelect);
            } else {
                commentaireTableView.getSelectionModel().clearSelection();
                selectedCommentaire = null;
                updateCommentSection();
            }

            updateActionAvailability();
            setStatus(statusLabel,
                    statusMessage == null ? "Announcement workspace ready." : statusMessage,
                    statusStyle == null ? "status-muted" : statusStyle);
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            showErrorStatus("Could not refresh the announcements.");
            Throwable exception = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Announcements", "Refresh failed.\n" + (exception == null ? "" : exception.getMessage()));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void rebuildCommentCounts() {
        commentCounts.clear();
        for (Commentaire commentaire : commentaires) {
            if (commentaire.getAnnonceId() == null) {
                continue;
            }
            commentCounts.merge(commentaire.getAnnonceId(), 1, Integer::sum);
        }
        annonceTableView.refresh();
    }

    private void refreshPosteFilterValues() {
        String currentValue = posteFilterComboBox.getValue();
        LinkedHashSet<String> postes = new LinkedHashSet<>();
        for (Annonce annonce : annonces) {
            String poste = emptyToNull(annonce.getPosteRecherche());
            if (poste != null) {
                postes.add(poste);
            }
        }
        posteFilterComboBox.setItems(FXCollections.observableArrayList(postes));
        if (currentValue != null && postes.contains(currentValue)) {
            posteFilterComboBox.setValue(currentValue);
        } else if (currentValue != null) {
            posteFilterComboBox.getSelectionModel().clearSelection();
        }
    }

    private void applyAnnonceFilters() {
        String query = normalize(searchField.getText());
        String poste = emptyToNull(posteFilterComboBox.getValue());
        String statut = emptyToNull(statutFilterComboBox.getValue());
        boolean urgentOnly = urgentOnlyCheck.isSelected();

        filteredAnnonces.setPredicate(annonce -> {
            boolean matchesQuery = query == null
                    || containsNormalized(annonce.getTitre(), query)
                    || containsNormalized(annonce.getDescription(), query)
                    || containsNormalized(annonce.getPosteRecherche(), query)
                    || containsNormalized(annonce.getNiveauRequis(), query);
            boolean matchesPoste = poste == null || poste.equalsIgnoreCase(emptyIfNull(annonce.getPosteRecherche()));
            boolean matchesStatut = statut == null || statut.equalsIgnoreCase(emptyIfNull(annonce.getStatut()));
            boolean matchesUrgent = !urgentOnly || Boolean.TRUE.equals(annonce.getUrgent());
            return matchesQuery && matchesPoste && matchesStatut && matchesUrgent;
        });

        resultsMetaLabel.setText(filteredAnnonces.size() + " announcement(s) visible");
        resultCountLabel.setText(annonces.size() + " total announcement(s)");

        if (selectedAnnonce != null && filteredAnnonces.stream().noneMatch(annonce -> Objects.equals(annonce.getId(), selectedAnnonce.getId()))) {
            clearAnnonceSelection();
            updateDetailPanel();
            applyCommentFilter();
        }

        updateActionAvailability();
    }

    private void handleSelectedAnnonceChange(Annonce annonce) {
        selectedAnnonce = annonce;
        selectedCommentaire = null;
        commentaireTableView.getSelectionModel().clearSelection();

        if (annonce != null) {
            populateAnnonceForm(annonce);
        } else if (!hasAnnonceDraft()) {
            clearAnnonceFormFields();
        }

        clearAnnonceValidation();
        applyCommentFilter();
        updateDetailPanel();
        updateActionAvailability();
    }

    private void handleSelectedCommentaireChange(Commentaire commentaire) {
        selectedCommentaire = commentaire;
        if (commentaire != null) {
            populateCommentForm(commentaire);
        } else if (!hasCommentDraft()) {
            clearCommentFormFields();
        }

        clearCommentValidation();
        updateCommentSection();
        updateActionAvailability();
    }

    private void populateAnnonceForm(Annonce annonce) {
        titreField.setText(emptyIfNull(annonce.getTitre()));
        descriptionArea.setText(emptyIfNull(annonce.getDescription()));
        posteField.setText(emptyIfNull(annonce.getPosteRecherche()));
        niveauField.setText(emptyIfNull(annonce.getNiveauRequis()));
        datePublicationPicker.setValue(annonce.getDatePublication() != null ? annonce.getDatePublication() : LocalDate.now());
        statutComboBox.setValue(emptyToNull(annonce.getStatut()));
        entraineurIdField.setText(annonce.getEntraineurId() == null ? "" : String.valueOf(annonce.getEntraineurId()));
        commentsEnabledCheck.setSelected(annonce.getCommentsEnabled() == null || annonce.getCommentsEnabled());
        urgentCheck.setSelected(Boolean.TRUE.equals(annonce.getUrgent()));
        updateDetailPanel();
    }

    private void populateCommentForm(Commentaire commentaire) {
        auteurField.setText(emptyIfNull(commentaire.getAuteurAnonyme()));
        joueurIdField.setText(commentaire.getJoueurId() == null ? "" : String.valueOf(commentaire.getJoueurId()));
        commentDatePicker.setValue(commentaire.getDateCommentaire() != null ? commentaire.getDateCommentaire() : LocalDate.now());
        likesField.setText(String.valueOf(commentaire.getNbLikes()));
        moderationComboBox.setValue(emptyToNull(commentaire.getModerationStatus()));
        moderationReasonArea.setText(emptyIfNull(commentaire.getModerationReason()));
        commentaireArea.setText(emptyIfNull(commentaire.getContenu()));
    }

    private void applyCommentFilter() {
        visibleCommentaires.clear();

        if (selectedAnnonce == null) {
            List<Commentaire> allComments = new ArrayList<>(commentaires);
            allComments.sort(Comparator
                    .comparing(Commentaire::getDateCommentaire, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Commentaire::getId, Comparator.nullsLast(Comparator.reverseOrder())));
            visibleCommentaires.setAll(allComments);
            selectedAnnonceCommentsLabel.setText("All comments");
            commentResultsMetaLabel.setText(allComments.size() + " comment(s)");
            updateCommentSection();
            return;
        }

        List<Commentaire> filtered = new ArrayList<>();
        for (Commentaire commentaire : commentaires) {
            if (Objects.equals(commentaire.getAnnonceId(), selectedAnnonce.getId())) {
                filtered.add(commentaire);
            }
        }
        filtered.sort(Comparator
                .comparing(Commentaire::getDateCommentaire, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Commentaire::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        visibleCommentaires.setAll(filtered);
        selectedAnnonceCommentsLabel.setText("Comments for #" + selectedAnnonce.getId() + " - " + emptyIfNull(selectedAnnonce.getTitre()));
        commentResultsMetaLabel.setText(filtered.size() + " comment(s)");
        updateCommentSection();
    }

    private Annonce buildAnnonceFromForm(boolean updateMode) {
        String titre = emptyToNull(titreField.getText());
        String description = emptyToNull(descriptionArea.getText());
        String poste = emptyToNull(posteField.getText());
        String niveau = emptyToNull(niveauField.getText());
        LocalDate datePublication = datePublicationPicker.getValue();
        String statut = emptyToNull(statutComboBox.getValue());
        Integer entraineurId = parseOptionalInteger(entraineurIdField);

        if (titre == null) {
            markFieldInvalid(titreField);
            showAnnonceValidation("Title is required.");
            return null;
        }
        if (description == null) {
            markFieldInvalid(descriptionArea);
            showAnnonceValidation("Description is required.");
            return null;
        }
        if (poste == null) {
            markFieldInvalid(posteField);
            showAnnonceValidation("Role is required.");
            return null;
        }
        if (niveau == null) {
            markFieldInvalid(niveauField);
            showAnnonceValidation("Level is required.");
            return null;
        }
        if (datePublication == null) {
            markFieldInvalid(datePublicationPicker);
            showAnnonceValidation("Publication date is required.");
            return null;
        }
        if (statut == null) {
            markFieldInvalid(statutComboBox);
            showAnnonceValidation("Status is required.");
            return null;
        }
        if (entraineurId == Integer.MIN_VALUE) {
            showAnnonceValidation("Coach ID must be numeric.");
            return null;
        }
        if (updateMode && selectedAnnonce == null) {
            showAnnonceValidation("Select an announcement before updating it.");
            return null;
        }

        return new Annonce(
                titre,
                description,
                poste,
                niveau,
                datePublication,
                statut,
                entraineurId,
                commentsEnabledCheck.isSelected(),
                urgentCheck.isSelected()
        );
    }

    private Commentaire buildCommentaireFromForm(boolean updateMode) {
        Integer annonceId = resolveCommentAnnonceId();
        if (annonceId == null) {
            showCommentValidation("Select a comment or announcement first.");
            return null;
        }

        String auteur = emptyToNull(auteurField.getText());
        Integer joueurId = parseOptionalInteger(joueurIdField);
        LocalDate commentDate = commentDatePicker.getValue();
        Integer likes = parseOptionalInteger(likesField);
        String moderationStatus = emptyToNull(moderationComboBox.getValue());
        String moderationReason = emptyToNull(moderationReasonArea.getText());
        String contenu = emptyToNull(commentaireArea.getText());

        if (auteur == null) {
            markFieldInvalid(auteurField);
            showCommentValidation("Author name is required.");
            return null;
        }
        if (joueurId == Integer.MIN_VALUE) {
            showCommentValidation("Player ID must be numeric.");
            return null;
        }
        if (commentDate == null) {
            markFieldInvalid(commentDatePicker);
            showCommentValidation("Comment date is required.");
            return null;
        }
        if (likes == Integer.MIN_VALUE) {
            showCommentValidation("Likes must be numeric.");
            return null;
        }
        if (likes != null && likes < 0) {
            markFieldInvalid(likesField);
            showCommentValidation("Likes cannot be negative.");
            return null;
        }
        if (moderationStatus == null) {
            markFieldInvalid(moderationComboBox);
            showCommentValidation("Moderation status is required.");
            return null;
        }
        if (contenu == null) {
            markFieldInvalid(commentaireArea);
            showCommentValidation("Comment content is required.");
            return null;
        }
        if (updateMode && selectedCommentaire == null) {
            showCommentValidation("Select a comment before updating it.");
            return null;
        }

        Commentaire commentaire = new Commentaire(
                contenu,
                commentDate,
                joueurId,
                annonceId,
                auteur,
                updateMode && selectedCommentaire != null ? selectedCommentaire.getCvName() : null,
                updateMode && selectedCommentaire != null ? selectedCommentaire.getCvTitle() : null,
                likes == null ? 0 : likes,
                moderationStatus,
                moderationReason,
                updateMode && selectedCommentaire != null ? selectedCommentaire.getAuthorUserId() : joueurId,
                updateMode && selectedCommentaire != null ? selectedCommentaire.getAuthorRole() : null
        );
        if (updateMode && selectedCommentaire != null) {
            commentaire.setNbDislikes(selectedCommentaire.getNbDislikes());
        }
        return commentaire;
    }

    private void updateMetrics() {
        int active = 0;
        int urgent = 0;
        int pendingComments = 0;
        for (Annonce annonce : annonces) {
            if ("ACTIVE".equalsIgnoreCase(annonce.getStatut())) {
                active++;
            }
            if (Boolean.TRUE.equals(annonce.getUrgent())) {
                urgent++;
            }
        }
        for (Commentaire commentaire : commentaires) {
            if ("PENDING".equalsIgnoreCase(commentaire.getModerationStatus())) {
                pendingComments++;
            }
        }

        totalAnnoncesMetricLabel.setText(String.valueOf(annonces.size()));
        activeAnnoncesMetricLabel.setText(String.valueOf(active));
        urgentAnnoncesMetricLabel.setText(String.valueOf(urgent));
        pendingCommentsMetricLabel.setText(String.valueOf(pendingComments));
    }

    private void updateDetailPanel() {
        boolean editing = selectedAnnonce != null;
        boolean drafting = editing || hasAnnonceDraft();

        String titre = emptyToNull(titreField.getText());
        String poste = emptyToNull(posteField.getText());
        String niveau = emptyToNull(niveauField.getText());
        LocalDate datePublication = datePublicationPicker.getValue();
        String statut = emptyToNull(statutComboBox.getValue());
        String coachLabel = emptyToNull(entraineurIdField.getText());

        if (editing && titre == null) {
            titre = selectedAnnonce.getTitre();
        }
        if (editing && poste == null) {
            poste = selectedAnnonce.getPosteRecherche();
        }
        if (editing && niveau == null) {
            niveau = selectedAnnonce.getNiveauRequis();
        }
        if (editing && datePublication == null) {
            datePublication = selectedAnnonce.getDatePublication();
        }
        if (editing && statut == null) {
            statut = selectedAnnonce.getStatut();
        }
        if (editing && coachLabel == null && selectedAnnonce.getEntraineurId() != null) {
            coachLabel = String.valueOf(selectedAnnonce.getEntraineurId());
        }

        detailBadgeLabel.setText(editing ? "Editing" : drafting ? "Creating" : "Preview");
        selectionStateLabel.setText(editing
                ? "Editing #" + selectedAnnonce.getId()
                : drafting ? "Creating a new announcement" : "Select an announcement");
        formHintLabel.setText(editing
                ? "Update the selected announcement and save the changes."
                : "Create a new announcement with the same visual language as the local modules.");

        detailTitleLabel.setText(titre == null ? "No announcement selected" : titre);
        detailSubtitleLabel.setText(buildDetailSubtitle(poste, niveau, datePublication, drafting));
        setStatus(detailStatusLabel, statut == null ? "Draft" : statut, resolveAnnonceStatusStyle(statut));
        detailCommentsValueLabel.setText(String.valueOf(resolveCommentCount()));
        detailOwnerValueLabel.setText(coachLabel == null ? "No coach ID" : "Coach #" + coachLabel);
        detailUrgentValueLabel.setText(isUrgentDraft() ? "Urgent" : "Standard");
    }

    private String buildDetailSubtitle(String poste, String niveau, LocalDate datePublication, boolean drafting) {
        if (!drafting) {
            return "Choose an announcement from the list or start a new one.";
        }

        List<String> parts = new ArrayList<>();
        if (poste != null) {
            parts.add(poste);
        }
        if (niveau != null) {
            parts.add(niveau);
        }
        if (datePublication != null) {
            parts.add(formatDate(datePublication));
        }
        return parts.isEmpty() ? "Fill in the main fields to preview the announcement here." : String.join(" | ", parts);
    }

    private int resolveCommentCount() {
        if (selectedAnnonce != null && selectedAnnonce.getId() != null) {
            return commentCounts.getOrDefault(selectedAnnonce.getId(), 0);
        }
        return 0;
    }

    private boolean isUrgentDraft() {
        if (selectedAnnonce != null) {
            return urgentCheck.isSelected() || Boolean.TRUE.equals(selectedAnnonce.getUrgent());
        }
        return urgentCheck.isSelected();
    }

    private void updateCommentSection() {
        if (selectedAnnonce == null) {
            commentFormHintLabel.setText("All posted comments are shown here. Select an announcement if you want to filter the moderation table to a single post.");
            setStatus(commentStatusLabel, "Showing all comments", "status-success");
        } else if (!isCommentsEnabled(selectedAnnonce)) {
            commentFormHintLabel.setText("Comments are locked for the selected announcement. Admin can still delete existing comments if needed.");
            setStatus(commentStatusLabel, "Comments locked", "status-warning");
        } else if (selectedCommentaire != null) {
            commentFormHintLabel.setText("Admin can review, update, or delete the selected player comment for moderation purposes.");
            setStatus(commentStatusLabel, "Comment selected", resolveCommentStatusStyle(selectedCommentaire.getModerationStatus()));
        } else {
            commentFormHintLabel.setText("Read comments here and select one to update or delete it as part of moderation.");
            setStatus(commentStatusLabel, "Moderation ready", "status-success");
        }
    }

    private void updateActionAvailability() {
        boolean hasAnnonceSelection = selectedAnnonce != null;
        boolean hasCommentSelection = selectedCommentaire != null;

        addAnnonceButton.setDisable(!serviceReady);
        updateAnnonceButton.setDisable(!serviceReady || !hasAnnonceSelection);
        deleteAnnonceButton.setDisable(!serviceReady || !hasAnnonceSelection);
        clearAnnonceButton.setDisable(!serviceReady);
        exportPdfButton.setDisable(!serviceReady || filteredAnnonces.isEmpty());
        annonceTableView.setDisable(!serviceReady);

        addCommentButton.setDisable(true);
        updateCommentButton.setDisable(!serviceReady || !hasCommentSelection);
        deleteCommentButton.setDisable(!serviceReady || !hasCommentSelection);
        clearCommentButton.setDisable(!serviceReady);
        commentaireTableView.setDisable(!serviceReady);

        boolean commentFieldsEnabled = serviceReady && hasCommentSelection;
        for (Control control : List.of(
                auteurField,
                joueurIdField,
                commentDatePicker,
                likesField,
                moderationComboBox,
                moderationReasonArea,
                commentaireArea
        )) {
            if (control != null) {
                control.setDisable(!commentFieldsEnabled);
            }
        }
    }

    private void clearAnnonceFormFields() {
        titreField.clear();
        descriptionArea.clear();
        posteField.clear();
        niveauField.clear();
        datePublicationPicker.setValue(LocalDate.now());
        statutComboBox.setValue("ACTIVE");
        entraineurIdField.clear();
        commentsEnabledCheck.setSelected(true);
        urgentCheck.setSelected(false);
    }

    private void clearCommentFormFields() {
        auteurField.clear();
        joueurIdField.clear();
        commentDatePicker.setValue(LocalDate.now());
        likesField.setText("0");
        moderationComboBox.setValue("PENDING");
        moderationReasonArea.clear();
        commentaireArea.clear();
    }

    private void clearAnnonceSelection() {
        annonceTableView.getSelectionModel().clearSelection();
        selectedAnnonce = null;
    }

    private void clearCommentSelection() {
        commentaireTableView.getSelectionModel().clearSelection();
        selectedCommentaire = null;
    }

    private void selectAnnonceById(Integer annonceId) {
        if (annonceId == null) {
            return;
        }
        for (Annonce annonce : filteredAnnonces) {
            if (Objects.equals(annonce.getId(), annonceId)) {
                annonceTableView.getSelectionModel().select(annonce);
                annonceTableView.scrollTo(annonce);
                return;
            }
        }
    }

    private void selectCommentaireById(Integer commentaireId) {
        if (commentaireId == null) {
            return;
        }
        for (Commentaire commentaire : visibleCommentaires) {
            if (Objects.equals(commentaire.getId(), commentaireId)) {
                commentaireTableView.getSelectionModel().select(commentaire);
                commentaireTableView.scrollTo(commentaire);
                return;
            }
        }
    }

    private Integer getSelectedAnnonceId() {
        return selectedAnnonce == null ? null : selectedAnnonce.getId();
    }

    private Integer getSelectedCommentaireId() {
        return selectedCommentaire == null ? null : selectedCommentaire.getId();
    }

    private boolean hasAnnonceDraft() {
        return emptyToNull(titreField.getText()) != null
                || emptyToNull(descriptionArea.getText()) != null
                || emptyToNull(posteField.getText()) != null
                || emptyToNull(niveauField.getText()) != null
                || emptyToNull(entraineurIdField.getText()) != null;
    }

    private boolean hasCommentDraft() {
        return emptyToNull(auteurField.getText()) != null
                || emptyToNull(joueurIdField.getText()) != null
                || emptyToNull(commentaireArea.getText()) != null
                || emptyToNull(moderationReasonArea.getText()) != null;
    }

    private Integer resolveCommentAnnonceId() {
        if (selectedAnnonce != null) {
            return selectedAnnonce.getId();
        }
        if (selectedCommentaire != null) {
            return selectedCommentaire.getAnnonceId();
        }
        return null;
    }

    private boolean isCommentsEnabled(Annonce annonce) {
        return annonce.getCommentsEnabled() == null || annonce.getCommentsEnabled();
    }

    private void configureNumericField(TextField textField) {
        textField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
        ));
    }

    private Integer parseOptionalInteger(TextField field) {
        String value = emptyToNull(field.getText());
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            markFieldInvalid(field);
            return Integer.MIN_VALUE;
        }
    }

    private void showAnnonceValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void clearAnnonceValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        clearFieldError(titreField);
        clearFieldError(descriptionArea);
        clearFieldError(posteField);
        clearFieldError(niveauField);
        clearFieldError(datePublicationPicker);
        clearFieldError(statutComboBox);
        clearFieldError(entraineurIdField);
    }

    private void showCommentValidation(String message) {
        commentValidationLabel.setText(message);
        commentValidationLabel.setManaged(true);
        commentValidationLabel.setVisible(true);
    }

    private void clearCommentValidation() {
        commentValidationLabel.setText("");
        commentValidationLabel.setManaged(false);
        commentValidationLabel.setVisible(false);
        clearFieldError(auteurField);
        clearFieldError(joueurIdField);
        clearFieldError(commentDatePicker);
        clearFieldError(likesField);
        clearFieldError(moderationComboBox);
        clearFieldError(commentaireArea);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private void showMutedStatus(String message) {
        setStatus(statusLabel, message, "status-muted");
    }

    private void showSuccessStatus(String message) {
        setStatus(statusLabel, message, "status-success");
    }

    private void showErrorStatus(String message) {
        setStatus(statusLabel, message, "status-error");
    }

    private void showCommentMutedStatus(String message) {
        setStatus(commentStatusLabel, message, "status-muted");
    }

    private void showCommentSuccessStatus(String message) {
        setStatus(commentStatusLabel, message, "status-success");
    }

    private void showCommentErrorStatus(String message) {
        setStatus(commentStatusLabel, message, "status-error");
    }

    private void setStatus(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private String resolveAnnonceStatusStyle(String statut) {
        if (statut == null) {
            return "status-muted";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "status-success";
            case "EN_ATTENTE" -> "status-warning";
            case "EXPIREE", "CLOSED" -> "status-error";
            default -> "status-muted";
        };
    }

    private String resolveCommentStatusStyle(String statut) {
        if (statut == null) {
            return "status-muted";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> "status-success";
            case "PENDING" -> "status-warning";
            case "REJECTED" -> "status-error";
            default -> "status-muted";
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean containsNormalized(String source, String query) {
        String normalizedSource = normalize(source);
        return normalizedSource != null && normalizedSource.contains(query);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RefreshPayload(List<Annonce> annonces, List<Commentaire> commentaires) {
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
