package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.AdminTableButtons;
import tn.esprit.gui.AdminTableScrollSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.AdminExcelExportService;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class EquipeController implements AssistantContextProvider {
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();
    private static final String ALL_COMPETITIONS_LABEL = "Toutes competitions";
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory("equipe-db-worker"));
    private static final Pattern TEAM_NAME_INPUT_PATTERN = Pattern.compile("[\\p{L}0-9 .&'()/_-]{0,100}");
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("[\\p{L}0-9 .&'()/_-]{2,100}");
    private static final Pattern COACH_NAME_INPUT_PATTERN = Pattern.compile("[\\p{L} .'-]{0,100}");
    private static final Pattern COACH_NAME_PATTERN = Pattern.compile("[\\p{L} .'-]{2,100}");
    private static final Pattern IMAGE_REFERENCE_PATTERN = Pattern.compile("(?i).+\\.(png|jpe?g|gif|bmp|webp)$");

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
    private TextField searchField;
    @FXML
    private ComboBox<String> sortChoiceBox;
    @FXML
    private Button sortOrderButton;
    @FXML
    private HBox competitionFilterBar;
    @FXML
    private TextField statsTeamField;
    @FXML
    private Label teamStatsSummaryLabel;
    @FXML
    private BarChart<String, Number> teamRateChart;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<Equipe> equipeTableView;
    @FXML
    private TableColumn<Equipe, Integer> idColumn;
    @FXML
    private TableColumn<Equipe, String> nomColumn;
    @FXML
    private TableColumn<Equipe, String> coachColumn;
    @FXML
    private TableColumn<Equipe, String> competitionColumn;
    @FXML
    private TableColumn<Equipe, String> sourceColumn;
    @FXML
    private TableColumn<Equipe, String> logoColumn;
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
    private MatchsService matchsService;
    private JoueurService joueurService;
    private AdminExcelExportService excelExportService;
    private File lastImageDirectory;
    private boolean sortDescending;
    private boolean serviceReady;
    private boolean loadingData;
    private boolean mutatingData;
    private boolean darkMode;
    private SidebarModuleGroup sidebarModuleGroup;
    private String selectedCompetitionCode;
    private final Map<String, Button> competitionFilterButtons = new java.util.LinkedHashMap<>();
    private List<Matchs> teamStatsMatchs = List.of();
    private List<Joueur> teamStatsJoueurs = List.of();
    private TableColumn<Equipe, Void> actionsColumn;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureToolbar();
        configureFieldRestrictions();
        configureTableView();
        configureCreateOnlyForm();
        bindFormPreview();
        configureStatsSection();
        updateSortOrderButtonText();
        updateFormMode();
        updateDetailCard();
        updateActionAvailability();

        try {
            equipeService = new EquipeService();
            matchsService = new MatchsService();
            joueurService = new JoueurService();
            excelExportService = new AdminExcelExportService();
            serviceReady = true;
            refreshTableAsync(null, "Chargement des equipes...", "status-muted", "Connexion etablie. Module Equipe pret.");
        } catch (SQLException e) {
            serviceReady = false;
            updateActionAvailability();
            showStatus("status-error", "Connexion a la base impossible.");
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de charger les equipes.\n" + e.getMessage());
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (teamRateChart != null) {
            teamRateChart.applyCss();
        }
        Platform.runLater(() -> updateTeamRateChart(resolveTeamStatsSelection()));
    }

    @Override
    public String assistantContextSummary() {
        Equipe selectedEquipe = equipeTableView == null ? null : equipeTableView.getSelectionModel().getSelectedItem();
        String competitionFilter = selectedCompetitionCode == null ? "All competitions" : FootballDataCompetitions.labelOf(selectedCompetitionCode);
        String selectedTeamName = selectedEquipe == null ? null : emptyToNull(selectedEquipe.getNom());

        return """
                Current team management screen.
                Visible teams: %s. Results meta: %s.
                Search query: %s. Competition filter: %s.
                Selection state: %s.
                Selected or preview team: %s.
                Team detail subtitle: %s.
                Coach: %s. Status: %s.
                Team statistics summary: %s.
                Toolbar status: %s.
                """.formatted(
                emptyIfNull(resultCountLabel == null ? null : resultCountLabel.getText()),
                emptyIfNull(resultsMetaLabel == null ? null : resultsMetaLabel.getText()),
                emptyIfNull(searchField == null ? null : searchField.getText()),
                emptyIfNull(competitionFilter),
                emptyIfNull(selectionStateLabel == null ? null : selectionStateLabel.getText()),
                emptyIfNull(selectedTeamName != null ? selectedTeamName : detailNameLabel == null ? null : detailNameLabel.getText()),
                emptyIfNull(detailSubtitleLabel == null ? null : detailSubtitleLabel.getText()),
                emptyIfNull(detailCoachValueLabel == null ? null : detailCoachValueLabel.getText()),
                emptyIfNull(detailStatusValueLabel == null ? null : detailStatusValueLabel.getText()),
                emptyIfNull(teamStatsSummaryLabel == null ? null : teamStatsSummaryLabel.getText()),
                emptyIfNull(statusLabel == null ? null : statusLabel.getText())
        );
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

        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();
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

        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();
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
    private void handleExportExcel() {
        if (excelExportService == null) {
            showAlert(Alert.AlertType.ERROR, "Export", "Le service Excel n'est pas disponible.");
            return;
        }
        Path target = chooseExcelTarget("equipes-export.xlsx");
        if (target == null) {
            return;
        }
        try {
            excelExportService.exportEquipes(target, new ArrayList<>(displayedEquipes), this::resolveCompetitionLabel);
            openFile(target);
            showStatus("status-success", "Export Excel des equipes termine.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export", "Erreur lors de l'export Excel des equipes.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleShowTeamStats() {
        updateTeamRateChart(resolveTeamStatsSelection());
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
        selectedCompetitionCode = null;
        sortDescending = false;
        updateCompetitionFilterButtonState();
        updateSortOrderButtonText();
        applyFiltersAndSort(getSelectedEquipeId());
    }

    @FXML
    private void handleBrowseImage() {
        if (equipeTableView != null && equipeTableView.getSelectionModel().getSelectedItem() != null) {
            showStatus("status-muted", "Le formulaire sert uniquement a l'ajout. Modifiez l'equipe depuis le tableau.");
            return;
        }

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
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        showStatus("status-muted", "Module Equipes deja actif.");
    }

    @FXML
    private void handleOpenMatchsSoon() {
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
    private void handleOpenJoueursSoon() {
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.EQUIPES);
    }

    private void configureToolbar() {
        sortChoiceBox.setItems(FXCollections.observableArrayList("Nom", "Coach"));
        sortChoiceBox.setValue("Nom");
        configureCompetitionFilterBar();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort(getSelectedEquipeId()));
        sortChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFiltersAndSort(getSelectedEquipeId()));
    }

    private void configureCompetitionFilterBar() {
        if (competitionFilterBar == null) {
            return;
        }
        competitionFilterBar.getChildren().clear();
        competitionFilterButtons.clear();

        addCompetitionFilterButton(ALL_COMPETITIONS_LABEL, null);
        COMPETITION_LABELS.forEach((code, label) -> addCompetitionFilterButton(label, code));
        updateCompetitionFilterButtonState();
    }

    private void configureStatsSection() {
        if (teamRateChart != null) {
            teamRateChart.setAnimated(false);
            teamRateChart.setLegendVisible(false);
        }
        if (statsTeamField != null) {
            statsTeamField.textProperty().addListener((observable, oldValue, newValue) -> updateTeamRateChart(resolveTeamStatsSelection()));
        }
        if (teamStatsSummaryLabel != null) {
            teamStatsSummaryLabel.setText("Ecrivez le nom d'une equipe pour voir ses statistiques.");
        }
    }

    private void addCompetitionFilterButton(String label, String competitionCode) {
        Button button = new Button(label);
        button.getStyleClass().addAll("soft-button", "competition-filter-button");
        button.setOnAction(event -> selectCompetitionFilter(competitionCode));
        competitionFilterButtons.put(label, button);
        competitionFilterBar.getChildren().add(button);
    }

    private void selectCompetitionFilter(String competitionCode) {
        selectedCompetitionCode = FootballDataCompetitions.normalizeCode(competitionCode);
        updateCompetitionFilterButtonState();
        applyFiltersAndSort(getSelectedEquipeId());
    }

    private void configureFieldRestrictions() {
        nomField.setTextFormatter(createPatternFormatter(TEAM_NAME_INPUT_PATTERN));
        coachField.setTextFormatter(createPatternFormatter(COACH_NAME_INPUT_PATTERN));
    }

    private TextFormatter<String> createPatternFormatter(Pattern pattern) {
        return new TextFormatter<>(change -> pattern.matcher(change.getControlNewText()).matches() ? change : null);
    }

    private void configureTableView() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        equipeTableView.getColumns().remove(idColumn);
        nomColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getNom())));
        coachColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getCoach(), "Non renseigne")));
        competitionColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveCompetitionLabel(cell.getValue())));
        sourceColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveSourceLabel(cell.getValue())));
        logoColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveLogoState(cell.getValue())));

        equipeTableView.setItems(displayedEquipes);
        equipeTableView.setEditable(true);
        equipeTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        equipeTableView.setTableMenuButtonVisible(true);
        AdminTableScrollSupport.enable(equipeTableView);
        equipeTableView.setRowFactory(tableView -> {
            TableRow<Equipe> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    startInlineEquipeEdit(row.getItem());
                }
            });
            return row;
        });
        equipeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                clearFormFieldsOnly();
            } else if (!hasDraftContent()) {
                clearFormFieldsOnly();
            }

            clearValidation();
            updateFormMode();
            updateDetailCard();
            updateActionAvailability();
        });

        configureEditableTableColumns();
        ensureActionsColumn();
        configureReadableTableLayout();
    }

    private void configureCreateOnlyForm() {
        if (updateButton != null) {
            updateButton.setManaged(false);
            updateButton.setVisible(false);
        }
        if (deleteButton != null) {
            deleteButton.setManaged(false);
            deleteButton.setVisible(false);
        }
    }

    private void configureEditableTableColumns() {
        nomColumn.setEditable(true);
        nomColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nomColumn.setOnEditCommit(event ->
                handleInlineEquipeEdit(event.getRowValue(), equipe -> equipe.setNom(emptyToNull(event.getNewValue()))));

        coachColumn.setEditable(true);
        coachColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        coachColumn.setOnEditCommit(event -> {
            String value = emptyToNull(event.getNewValue());
            handleInlineEquipeEdit(event.getRowValue(), equipe -> equipe.setCoach(value));
        });
    }

    private void ensureActionsColumn() {
        if (actionsColumn != null) {
            return;
        }

        actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setResizable(false);
        actionsColumn.setMinWidth(84);
        actionsColumn.setPrefWidth(84);
        actionsColumn.setMaxWidth(84);
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteRowButton = AdminTableButtons.createTrashButton();
            private final HBox actionsBox = new HBox(deleteRowButton);

            {
                actionsBox.getStyleClass().add("table-inline-actions");

                deleteRowButton.setOnAction(event -> {
                    Equipe equipe = getTableRow() == null ? null : getTableRow().getItem();
                    deleteEquipeFromTable(equipe);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                boolean rowEmpty = empty || getTableRow() == null || getTableRow().getItem() == null;
                setGraphic(rowEmpty ? null : actionsBox);
                setText(null);
            }
        });
        equipeTableView.getColumns().add(actionsColumn);
    }

    private void configureReadableTableLayout() {
        idColumn.setPrefWidth(80);
        nomColumn.setPrefWidth(280);
        coachColumn.setPrefWidth(220);
        competitionColumn.setPrefWidth(190);
        sourceColumn.setPrefWidth(150);
        logoColumn.setPrefWidth(100);
    }

    private void startInlineEquipeEdit(Equipe equipe) {
        if (equipe == null || equipeTableView == null) {
            return;
        }

        clearValidation();
        clearFormFieldsOnly();
        equipeTableView.getSelectionModel().select(equipe);
        int rowIndex = displayedEquipes.indexOf(equipe);
        if (rowIndex >= 0) {
            equipeTableView.scrollTo(rowIndex);
            equipeTableView.edit(rowIndex, nomColumn);
        }
        showStatus("status-muted", "Modifiez directement la ligne puis validez avec Entree.");
    }

    private void handleInlineEquipeEdit(Equipe original, Consumer<Equipe> updater) {
        clearValidation();

        if (original == null || original.getId() == null || equipeService == null) {
            equipeTableView.refresh();
            return;
        }

        Equipe candidate = copyEquipe(original);
        updater.accept(candidate);

        String validationMessage = validateInlineEquipe(candidate);
        if (validationMessage != null) {
            equipeTableView.refresh();
            showValidation(validationMessage);
            return;
        }

        runMutation(
                () -> equipeService.update(candidate),
                original.getId(),
                false,
                "Equipe modifiee depuis le tableau.",
                "Modification",
                "Erreur lors de la modification :",
                "Erreur lors de la mise a jour en ligne."
        );
    }

    private void deleteEquipeFromTable(Equipe equipe) {
        clearValidation();

        if (equipe == null || equipe.getId() == null || equipeService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer l'equipe \"" + emptyIfNull(equipe.getNom()) + "\" ?");
        alert.setContentText("Cette action est definitive et ne pourra pas etre annulee.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        runMutation(
                () -> equipeService.delete(equipe.getId()),
                null,
                true,
                "Equipe supprimee avec succes.",
                "Suppression",
                "Erreur lors de la suppression :",
                "Erreur lors de la suppression de l'equipe."
        );
    }

    private Equipe copyEquipe(Equipe source) {
        Equipe copy = new Equipe(
                source.getId(),
                source.getNom(),
                source.getCoach(),
                source.getAdresse(),
                source.getTelephone(),
                source.getEmail(),
                source.getImage()
        );
        copy.setExternalApiId(source.getExternalApiId());
        copy.setExternalSource(source.getExternalSource());
        copy.setCompetitionCode(source.getCompetitionCode());
        copy.setApiFootballId(source.getApiFootballId());
        return copy;
    }

    private String validateInlineEquipe(Equipe equipe) {
        String nom = emptyToNull(equipe == null ? null : equipe.getNom());
        String coach = emptyToNull(equipe == null ? null : equipe.getCoach());
        String image = emptyToNull(equipe == null ? null : equipe.getImage());

        if (nom == null) {
            return "Le nom de l'equipe est obligatoire.";
        }

        if (!TEAM_NAME_PATTERN.matcher(nom).matches()) {
            return "Le nom de l'equipe doit contenir entre 2 et 100 caracteres valides.";
        }

        if (coach != null && !COACH_NAME_PATTERN.matcher(coach).matches()) {
            return "Le nom du coach doit contenir entre 2 et 100 lettres maximum.";
        }

        if (isDuplicateTeamName(nom, equipe.getId())) {
            return "Une equipe avec ce nom existe deja.";
        }

        if (image != null && !isValidImageReference(image)) {
            return "Le logo doit pointer vers une image valide (.png, .jpg, .jpeg, .gif, .bmp, .webp).";
        }

        return null;
    }

    private void bindFormPreview() {
        nomField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(nomField);
            updateDetailCard();
        });
        coachField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(coachField);
            updateDetailCard();
        });
        imageField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(imageField);
            updateDetailCard();
        });
    }

    private void refreshTeamStatistics() throws SQLException {
        if (matchsService == null || joueurService == null) {
            return;
        }
        teamStatsMatchs = new ArrayList<>(matchsService.getAll());
        teamStatsJoueurs = new ArrayList<>(joueurService.getAll());
        updateTeamRateChart(resolveTeamStatsSelection());
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
            try {
                refreshTeamStatistics();
            } catch (SQLException sqlException) {
                teamStatsMatchs = List.of();
                teamStatsJoueurs = List.of();
                if (teamStatsSummaryLabel != null) {
                    teamStatsSummaryLabel.setText("Statistiques equipe indisponibles.");
                }
            }
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
        if (selectedCompetitionCode != null) {
            filteredEquipes.removeIf(equipe -> !Objects.equals(
                    FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode()),
                    selectedCompetitionCode
            ));
        }

        filteredEquipes.sort(buildComparator());
        displayedEquipes.setAll(filteredEquipes);

        boolean isEmpty = filteredEquipes.isEmpty();
        emptyStateBox.setManaged(isEmpty);
        emptyStateBox.setVisible(isEmpty);

        resultCountLabel.setText(filteredEquipes.size() + " equipe(s)");
        resultsMetaLabel.setText(buildResultsMeta(filteredEquipes.size()));

        restoreSelection(preferredSelectionId);
        updateDetailCard();
    }

    private String buildResultsMeta(int count) {
        StringBuilder builder = new StringBuilder(count + " ligne(s)");
        if (selectedCompetitionCode != null) {
            builder.append(" | ").append(FootballDataCompetitions.labelOf(selectedCompetitionCode));
        }
        return builder.toString();
    }

    private Equipe resolveTeamStatsSelection() {
        String query = normalize(statsTeamField == null ? null : statsTeamField.getText());
        if (query == null || query.isBlank()) {
            return equipeTableView == null ? null : equipeTableView.getSelectionModel().getSelectedItem();
        }
        return masterEquipes.stream()
                .filter(equipe -> containsNormalized(equipe.getNom(), query))
                .findFirst()
                .orElse(null);
    }

    private void updateTeamRateChart(Equipe equipe) {
        if (teamRateChart == null) {
            return;
        }

        teamRateChart.getData().clear();
        if (equipe == null || equipe.getId() == null) {
            if (teamStatsSummaryLabel != null) {
                teamStatsSummaryLabel.setText("Ecrivez le nom d'une equipe pour voir ses statistiques.");
            }
            return;
        }

        List<Matchs> teamMatches = teamStatsMatchs.stream()
                .filter(match -> Objects.equals(equipe.getId(), match.getEquipeDomicileId())
                        || Objects.equals(equipe.getId(), match.getEquipeExterieurId()))
                .toList();
        List<Matchs> completedMatches = teamMatches.stream()
                .filter(this::hasFinalScore)
                .toList();

        long wins = completedMatches.stream().filter(match -> resolveResult(match, equipe.getId()) == MatchResult.WIN).count();
        long draws = completedMatches.stream().filter(match -> resolveResult(match, equipe.getId()) == MatchResult.DRAW).count();
        long losses = completedMatches.stream().filter(match -> resolveResult(match, equipe.getId()) == MatchResult.LOSS).count();
        int playerCount = (int) teamStatsJoueurs.stream().filter(joueur -> Objects.equals(equipe.getId(), joueur.getEquipeId())).count();
        int goalsFor = completedMatches.stream().mapToInt(match -> goalsFor(match, equipe.getId())).sum();
        int goalsAgainst = completedMatches.stream().mapToInt(match -> goalsAgainst(match, equipe.getId())).sum();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (completedMatches.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Victoires", 0));
            series.getData().add(new XYChart.Data<>("Nuls", 0));
            series.getData().add(new XYChart.Data<>("Defaites", 0));
        } else {
            double total = completedMatches.size();
            series.getData().add(new XYChart.Data<>("Victoires", roundRate(wins, total)));
            series.getData().add(new XYChart.Data<>("Nuls", roundRate(draws, total)));
            series.getData().add(new XYChart.Data<>("Defaites", roundRate(losses, total)));
        }
        teamRateChart.getData().add(series);
        applyBarColors(series, darkMode
                ? List.of("#22c55e", "#f59e0b", "#ef4444")
                : List.of("#16a34a", "#f59e0b", "#dc2626"));

        if (teamStatsSummaryLabel != null) {
            long pendingMatches = teamMatches.size() - completedMatches.size();
            teamStatsSummaryLabel.setText(emptyIfNull(equipe.getNom()) + " | "
                    + playerCount + " joueurs | "
                    + completedMatches.size() + " matchs joues | "
                    + goalsFor + " buts marques / " + goalsAgainst + " encaisses"
                    + (pendingMatches > 0 ? " | " + pendingMatches + " en attente" : ""));
        }
    }

    private boolean matchesSearch(Equipe equipe, String keyword) {
        return normalize(equipe.getNom()).contains(keyword) || normalize(equipe.getCoach()).contains(keyword);
    }

    private Comparator<Equipe> buildComparator() {
        Comparator<Equipe> comparator;
        String selectedSort = sortChoiceBox.getValue();

        if ("Coach".equals(selectedSort)) {
            comparator = Comparator.comparing(Equipe::getCoach, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else {
            comparator = Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return sortDescending ? comparator.reversed() : comparator;
    }

    private void restoreSelection(Integer preferredSelectionId) {
        if (preferredSelectionId == null) {
            Equipe currentlySelected = equipeTableView.getSelectionModel().getSelectedItem();
            if (currentlySelected != null && displayedEquipes.stream().noneMatch(equipe -> Objects.equals(equipe.getId(), currentlySelected.getId()))) {
                equipeTableView.getSelectionModel().clearSelection();
            }
            return;
        }

        for (Equipe equipe : displayedEquipes) {
            if (Objects.equals(equipe.getId(), preferredSelectionId)) {
                equipeTableView.getSelectionModel().select(equipe);
                equipeTableView.scrollTo(equipe);
                return;
            }
        }

        equipeTableView.getSelectionModel().clearSelection();
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
        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();

        if (nom == null) {
            markFieldInvalid(nomField);
            showValidation("Le nom de l'equipe est obligatoire.");
            return null;
        }

        if (!TEAM_NAME_PATTERN.matcher(nom).matches()) {
            markFieldInvalid(nomField);
            showValidation("Le nom de l'equipe doit contenir entre 2 et 100 caracteres valides.");
            return null;
        }

        if (coach != null && !COACH_NAME_PATTERN.matcher(coach).matches()) {
            markFieldInvalid(coachField);
            showValidation("Le nom du coach doit contenir entre 2 et 100 lettres maximum.");
            return null;
        }

        if (isDuplicateTeamName(nom, selectedEquipe == null ? null : selectedEquipe.getId())) {
            markFieldInvalid(nomField);
            showValidation("Une equipe avec ce nom existe deja.");
            return null;
        }

        if (image != null && !isValidImageReference(image)) {
            markFieldInvalid(imageField);
            showValidation("Le logo doit pointer vers une image valide (.png, .jpg, .jpeg, .gif, .bmp, .webp).");
            return null;
        }

        if (updateMode && selectedEquipe == null) {
            showValidation("Selectionnez une equipe avant de lancer une modification.");
            return null;
        }

        return new Equipe(nom, coach, null, null, null, image);
    }

    private void clearForm() {
        equipeTableView.getSelectionModel().clearSelection();
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
        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();
        if (selectedEquipe == null) {
            selectionStateLabel.setText(hasDraftContent() ? "Brouillon en cours" : "Mode creation");
            formHintLabel.setText("Renseignez les champs pour creer une nouvelle fiche equipe.");
            detailBadgeLabel.setText(hasDraftContent() ? "Brouillon" : "Apercu");
        } else {
            selectionStateLabel.setText("Selection : " + emptyIfNull(selectedEquipe.getNom()));
            formHintLabel.setText("Double-cliquez la ligne pour modifier. Utilisez l'icone corbeille pour supprimer.");
            detailBadgeLabel.setText("Edition en ligne");
        }
    }

    private void updateDetailCard() {
        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();
        String draftName = emptyToNull(nomField.getText());
        String draftCoach = emptyToNull(coachField.getText());
        String draftImage = emptyToNull(imageField.getText());

        if (selectedEquipe == null && !hasDraftContent()) {
            detailNameLabel.setText("Aucune equipe selectionnee");
            detailSubtitleLabel.setText("Selectionnez une ligne du tableau ou creez une nouvelle fiche pour afficher le detail.");
            detailIdValueLabel.setText("Creation");
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
                        ? "Coach non renseigne. Le nom seul suffit pour enregistrer la fiche."
                        : "Coach principal : " + effectiveCoach
        );
        detailIdValueLabel.setText(selectedEquipe == null ? "Creation" : "Selection");
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

    private void updateActionAvailability() {
        boolean hasSelection = equipeTableView.getSelectionModel().getSelectedItem() != null;
        boolean busy = loadingData || mutatingData;
        boolean createMode = serviceReady && !busy && !hasSelection;

        addButton.setDisable(!createMode);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        refreshButton.setDisable(!serviceReady || busy);
        clearButton.setDisable(!serviceReady || busy);
        searchField.setDisable(!serviceReady || busy);
        sortChoiceBox.setDisable(!serviceReady || busy);
        sortOrderButton.setDisable(!serviceReady || busy);
        competitionFilterButtons.values().forEach(button -> button.setDisable(!serviceReady || busy));
        equipeTableView.setDisable(!serviceReady || busy);
        nomField.setDisable(!createMode);
        coachField.setDisable(!createMode);
        imageField.setDisable(!createMode);
    }

    private void updateCompetitionFilterButtonState() {
        competitionFilterButtons.forEach((label, button) -> {
            boolean active = selectedCompetitionCode == null
                    ? ALL_COMPETITIONS_LABEL.equals(label)
                    : Objects.equals(label, FootballDataCompetitions.labelOf(selectedCompetitionCode));
            button.getStyleClass().removeAll("primary-button", "soft-button", "competition-filter-button-active");
            button.getStyleClass().add(active ? "primary-button" : "soft-button");
            button.getStyleClass().add("competition-filter-button");
            if (active) {
                button.getStyleClass().add("competition-filter-button-active");
            }
        });
    }

    private boolean hasDraftContent() {
        return emptyToNull(nomField.getText()) != null
                || emptyToNull(coachField.getText()) != null
                || emptyToNull(imageField.getText()) != null;
    }

    private Integer getSelectedEquipeId() {
        Equipe selectedEquipe = equipeTableView.getSelectionModel().getSelectedItem();
        return selectedEquipe == null ? null : selectedEquipe.getId();
    }

    private void updateSortOrderButtonText() {
        sortOrderButton.setText(sortDescending ? "Decroissant" : "Croissant");
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
        clearFieldError(imageField);
    }

    private void markFieldInvalid(TextField field) {
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(TextField field) {
        field.getStyleClass().remove("invalid-field");
    }

    private boolean isDuplicateTeamName(String teamName, Integer ignoredId) {
        String normalizedTeamName = normalizeIdentity(teamName);
        return masterEquipes.stream()
                .filter(equipe -> ignoredId == null || !Objects.equals(equipe.getId(), ignoredId))
                .map(Equipe::getNom)
                .map(this::normalizeIdentity)
                .anyMatch(normalizedTeamName::equals);
    }

    private String normalizeIdentity(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private boolean isValidImageReference(String imagePath) {
        String normalizedPath = emptyToNull(imagePath);
        if (normalizedPath == null) {
            return true;
        }

        Path path = toPathIfValid(normalizedPath);
        String fileName = path == null ? normalizedPath : path.getFileName().toString();
        return IMAGE_REFERENCE_PATTERN.matcher(fileName).matches();
    }

    private String resolveCompetitionLabel(Equipe equipe) {
        String competitionCode = equipe == null ? null : FootballDataCompetitions.normalizeCode(equipe.getCompetitionCode());
        if (competitionCode == null) {
            return "Locale";
        }
        return FootballDataCompetitions.labelOf(competitionCode);
    }

    private boolean hasFinalScore(Matchs match) {
        return match != null && match.getScoreEquipeDomicile() != null && match.getScoreEquipeExterieur() != null;
    }

    private MatchResult resolveResult(Matchs match, Integer teamId) {
        int goalsFor = goalsFor(match, teamId);
        int goalsAgainst = goalsAgainst(match, teamId);
        if (goalsFor > goalsAgainst) {
            return MatchResult.WIN;
        }
        if (goalsFor < goalsAgainst) {
            return MatchResult.LOSS;
        }
        return MatchResult.DRAW;
    }

    private int goalsFor(Matchs match, Integer teamId) {
        if (match == null || teamId == null) {
            return 0;
        }
        if (Objects.equals(teamId, match.getEquipeDomicileId())) {
            return match.getScoreEquipeDomicile() == null ? 0 : match.getScoreEquipeDomicile();
        }
        return match.getScoreEquipeExterieur() == null ? 0 : match.getScoreEquipeExterieur();
    }

    private int goalsAgainst(Matchs match, Integer teamId) {
        if (match == null || teamId == null) {
            return 0;
        }
        if (Objects.equals(teamId, match.getEquipeDomicileId())) {
            return match.getScoreEquipeExterieur() == null ? 0 : match.getScoreEquipeExterieur();
        }
        return match.getScoreEquipeDomicile() == null ? 0 : match.getScoreEquipeDomicile();
    }

    private double roundRate(long count, double total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((count * 1000.0) / total) / 10.0;
    }

    private String resolveSourceLabel(Equipe equipe) {
        String source = equipe == null ? null : emptyToNull(equipe.getExternalSource());
        return source == null ? "Base locale" : source;
    }

    private String resolveLogoState(Equipe equipe) {
        return equipe == null || emptyToNull(equipe.getImage()) == null ? "Non" : "Oui";
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

    private boolean containsNormalized(String source, String query) {
        String normalizedSource = normalize(source);
        return query != null && !query.isBlank() && normalizedSource.contains(query);
    }

    private void applyBarColors(XYChart.Series<String, Number> series, List<String> colors) {
        Platform.runLater(() -> {
            for (int index = 0; index < series.getData().size(); index++) {
                XYChart.Data<String, Number> data = series.getData().get(index);
                String color = colors.get(index % colors.size());
                applyBarColor(data, color);
                data.nodeProperty().addListener((observable, oldNode, newNode) -> applyBarColor(data, color));
            }
        });
    }

    private void applyBarColor(XYChart.Data<String, Number> data, String color) {
        if (data == null) {
            return;
        }
        Node node = data.getNode();
        if (node != null) {
            node.setStyle("-fx-bar-fill: " + color + ";");
        }
    }

    private Path chooseExcelTarget(String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter vers Excel");
        chooser.getExtensionFilters().add(new ExtensionFilter("Excel files", "*.xlsx"));
        chooser.setInitialFileName(suggestedName);
        Window owner = refreshButton == null || refreshButton.getScene() == null ? null : refreshButton.getScene().getWindow();
        File selected = chooser.showSaveDialog(owner);
        return selected == null ? null : selected.toPath();
    }

    private void openFile(Path path) {
        if (path == null) {
            return;
        }
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException ignored) {
                // Ignore when desktop integration is unavailable.
            }
        }
    }

    private String emptyIfNull(String value) {
        return emptyIfNull(value, "");
    }

    private String emptyIfNull(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private enum MatchResult {
        WIN,
        DRAW,
        LOSS
    }
}


