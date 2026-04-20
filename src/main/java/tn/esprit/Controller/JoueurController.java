package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.AdminExcelExportService;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.wikidata.WikidataPlayerImageService;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.util.StringConverter;

public class JoueurController implements AssistantContextProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Path SYMFONY_JOUEURS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "joueurs");
    private static final double CARD_IMAGE_SIZE = 82;
    private static final ExecutorService IMAGE_IMPORT_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-image-import"));
    private static final Pattern PERSON_NAME_INPUT_PATTERN = Pattern.compile("[\\p{L} .'-]{0,100}");
    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile("[\\p{L} .'-]{2,100}");
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
    private Label resultCountLabel;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private Label teamCountLabel;
    @FXML
    private Label playerChartSummaryLabel;
    @FXML
    private BarChart<String, Number> playerDistributionChart;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Equipe> equipeFilterComboBox;
    @FXML
    private ListView<Joueur> joueurListView;
    @FXML
    private TableView<Joueur> joueurTableView;
    @FXML
    private TableColumn<Joueur, Integer> joueurIdColumn;
    @FXML
    private TableColumn<Joueur, String> joueurNomColumn;
    @FXML
    private TableColumn<Joueur, String> joueurPrenomColumn;
    @FXML
    private TableColumn<Joueur, String> joueurEquipeColumn;
    @FXML
    private TableColumn<Joueur, Integer> joueurNumeroColumn;
    @FXML
    private TableColumn<Joueur, String> joueurNaissanceColumn;
    @FXML
    private TableColumn<Joueur, String> joueurPositionColumn;
    @FXML
    private TableColumn<Joueur, String> joueurNationaliteColumn;
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
    private AdminExcelExportService excelExportService;
    private Joueur selectedJoueur;
    private File lastImageDirectory;
    private boolean serviceReady;
    private boolean darkMode;
    private SidebarModuleGroup sidebarModuleGroup;
    private final Set<Integer> loadingImageIds = new HashSet<>();
    private TableColumn<Joueur, Void> actionsColumn;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
        configureStatusLabel();
        configureEquipeComboBoxes();
        configureNumeroField();
        configureFieldRestrictions();
        configurePlayerList();
        configureCreateOnlyForm();
        configureStatsSection();
        bindUiState();
        updateActionAvailability();
        updateDetailPanel();

        try {
            joueurService = new JoueurService();
            equipeService = new EquipeService();
            excelExportService = new AdminExcelExportService();
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

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (playerDistributionChart != null) {
            playerDistributionChart.applyCss();
        }
        Platform.runLater(this::updatePlayerDistributionChart);
    }

    @Override
    public String assistantContextSummary() {
        String filterTeam = equipeFilterComboBox != null && equipeFilterComboBox.getValue() != null
                ? emptyIfNull(equipeFilterComboBox.getValue().getNom())
                : "All teams";
        String selectedName = selectedJoueur == null ? null : buildFullName(selectedJoueur);

        return """
                Current player management screen.
                Visible players: %s. Results meta: %s. Team count: %s.
                Search query: %s. Team filter: %s.
                Selection state: %s.
                Selected or preview player: %s.
                Player subtitle: %s.
                Team: %s. Number: %s.
                Player distribution summary: %s.
                Toolbar status: %s.
                """.formatted(
                emptyIfNull(resultCountLabel == null ? null : resultCountLabel.getText()),
                emptyIfNull(resultsMetaLabel == null ? null : resultsMetaLabel.getText()),
                emptyIfNull(teamCountLabel == null ? null : teamCountLabel.getText()),
                emptyIfNull(searchField == null ? null : searchField.getText()),
                emptyIfNull(filterTeam),
                emptyIfNull(selectionStateLabel == null ? null : selectionStateLabel.getText()),
                emptyIfNull(selectedName != null ? selectedName : detailNameLabel == null ? null : detailNameLabel.getText()),
                emptyIfNull(detailSubtitleLabel == null ? null : detailSubtitleLabel.getText()),
                emptyIfNull(detailEquipeValueLabel == null ? null : detailEquipeValueLabel.getText()),
                emptyIfNull(detailNumeroValueLabel == null ? null : detailNumeroValueLabel.getText()),
                emptyIfNull(playerChartSummaryLabel == null ? null : playerChartSummaryLabel.getText()),
                emptyIfNull(statusLabel == null ? null : statusLabel.getText())
        );
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
    private void handleExportExcel() {
        if (excelExportService == null) {
            showAlert(Alert.AlertType.ERROR, "Export", "Le service Excel n'est pas disponible.");
            return;
        }
        Path target = chooseExcelTarget("joueurs-export.xlsx");
        if (target == null) {
            return;
        }
        try {
            excelExportService.exportJoueurs(target, new ArrayList<>(filteredJoueurs), this::getEquipeName);
            openFile(target);
            showSuccessStatus("Export Excel des joueurs termine.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export", "Erreur lors de l'export Excel des joueurs.\n" + e.getMessage());
        }
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
        if (selectedJoueur != null) {
            showMutedStatus("Le formulaire sert uniquement a l'ajout. Modifiez le joueur depuis le tableau.");
            return;
        }

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
    private void handleOpenMatchsSoon() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        showMutedStatus("Vous etes deja dans le module Joueurs.");
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.JOUEURS);
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

    private void configureStatsSection() {
        if (playerDistributionChart != null) {
            playerDistributionChart.setAnimated(false);
            playerDistributionChart.setLegendVisible(false);
        }
        if (playerChartSummaryLabel != null) {
            playerChartSummaryLabel.setText("Chargement des statistiques joueurs...");
        }
    }

    private void configureNumeroField() {
        numeroField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d{0,2}") ? change : null;
        }));
    }

    private void configureFieldRestrictions() {
        nomField.setTextFormatter(createPatternFormatter(PERSON_NAME_INPUT_PATTERN));
        prenomField.setTextFormatter(createPatternFormatter(PERSON_NAME_INPUT_PATTERN));
        dateNaissancePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setDisable(false);
                    return;
                }
                LocalDate today = LocalDate.now();
                setDisable(item.isAfter(today) || item.isBefore(today.minusYears(100)));
            }
        });
    }

    private TextFormatter<String> createPatternFormatter(Pattern pattern) {
        return new TextFormatter<>(change -> pattern.matcher(change.getControlNewText()).matches() ? change : null);
    }

    private void configurePlayerList() {
        if (joueurTableView != null) {
            joueurIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            joueurNomColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getNom())));
            joueurPrenomColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getPrenom())));
            joueurEquipeColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveEquipeLabel(cell.getValue())));
            joueurNumeroColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getNumero()));
            joueurNaissanceColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveBirthDateLabel(cell.getValue())));
            joueurPositionColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolvePlayerPositionLabel(cell.getValue())));
            joueurNationaliteColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolvePlayerNationalityLabel(cell.getValue())));

            joueurTableView.setItems(filteredJoueurs);
            joueurTableView.setEditable(true);
            joueurTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            joueurTableView.setTableMenuButtonVisible(true);
            joueurTableView.setPlaceholder(new Label(""));
            joueurTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    handleSelectedJoueurChange(newValue));
            configureEditableTableColumns();
            ensureActionsColumn();
            configureReadableTableLayout();
        }

        if (joueurListView != null) {
            joueurListView.setItems(filteredJoueurs);
            joueurListView.setPlaceholder(new Label(""));
            joueurListView.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(Joueur item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setText(null);
                    VBox card = buildPlayerCard(item);
                    card.prefWidthProperty().bind(listView.widthProperty().subtract(26));
                    setGraphic(card);
                }
            });
            joueurListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    handleSelectedJoueurChange(newValue));
        }
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
        joueurNomColumn.setEditable(true);
        joueurNomColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        joueurNomColumn.setOnEditCommit(event ->
                handleInlineJoueurEdit(event.getRowValue(), joueur -> joueur.setNom(emptyToNull(event.getNewValue()))));

        joueurPrenomColumn.setEditable(true);
        joueurPrenomColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        joueurPrenomColumn.setOnEditCommit(event ->
                handleInlineJoueurEdit(event.getRowValue(), joueur -> joueur.setPrenom(emptyToNull(event.getNewValue()))));

        joueurEquipeColumn.setEditable(true);
        joueurEquipeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        joueurEquipeColumn.setOnEditCommit(event -> {
            String equipeName = emptyToNull(event.getNewValue());
            handleInlineJoueurEdit(event.getRowValue(), joueur -> {
                Equipe equipe = findEquipeByName(equipeName);
                joueur.setEquipeId(equipe == null ? null : equipe.getId());
            });
        });

        joueurNumeroColumn.setEditable(true);
        joueurNumeroColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.valueOf(value);
            }

            @Override
            public Integer fromString(String value) {
                String trimmed = emptyToNull(value);
                if (trimmed == null) {
                    return null;
                }
                try {
                    return Integer.parseInt(trimmed);
                } catch (NumberFormatException exception) {
                    return Integer.MIN_VALUE;
                }
            }
        }));
        joueurNumeroColumn.setOnEditCommit(event ->
                handleInlineJoueurEdit(event.getRowValue(), joueur -> joueur.setNumero(event.getNewValue() == null ? 0 : event.getNewValue())));

        joueurNaissanceColumn.setEditable(true);
        joueurNaissanceColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        joueurNaissanceColumn.setOnEditCommit(event ->
                handleInlineJoueurEdit(event.getRowValue(), joueur -> joueur.setDateNaissance(parseInlineBirthDate(event.getNewValue()))));
    }

    private void ensureActionsColumn() {
        if (actionsColumn != null || joueurTableView == null) {
            return;
        }

        actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setResizable(false);
        actionsColumn.setMinWidth(190);
        actionsColumn.setPrefWidth(190);
        actionsColumn.setMaxWidth(190);
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteRowButton = new Button("Supprimer");
            private final HBox actionsBox = new HBox(8, editButton, deleteRowButton);

            {
                actionsBox.getStyleClass().add("table-inline-actions");
                editButton.getStyleClass().add("soft-button");
                editButton.getStyleClass().add("table-row-action-button");
                deleteRowButton.getStyleClass().add("danger-button");
                deleteRowButton.getStyleClass().add("table-row-danger-button");
                editButton.setPrefWidth(84);
                deleteRowButton.setPrefWidth(100);

                editButton.setOnAction(event -> {
                    Joueur joueur = getTableRow() == null ? null : getTableRow().getItem();
                    startInlineJoueurEdit(joueur);
                });
                deleteRowButton.setOnAction(event -> {
                    Joueur joueur = getTableRow() == null ? null : getTableRow().getItem();
                    deleteJoueurFromTable(joueur);
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
        joueurTableView.getColumns().add(actionsColumn);
    }

    private void configureReadableTableLayout() {
        joueurIdColumn.setPrefWidth(72);
        joueurNomColumn.setPrefWidth(140);
        joueurPrenomColumn.setPrefWidth(140);
        joueurEquipeColumn.setPrefWidth(240);
        joueurNumeroColumn.setPrefWidth(90);
        joueurNaissanceColumn.setPrefWidth(130);
        joueurPositionColumn.setPrefWidth(145);
        joueurNationaliteColumn.setPrefWidth(145);
    }

    private void handleSelectedJoueurChange(Joueur newValue) {
        selectedJoueur = newValue;

        if (newValue != null) {
            clearFormFieldsOnly();
        } else if (!hasDraftContent()) {
            clearFormFieldsOnly();
        }

        clearValidation();
        updateActionAvailability();
        updateDetailPanel();
        triggerLazyImageImport(newValue);
    }

    private void startInlineJoueurEdit(Joueur joueur) {
        if (joueur == null || joueurTableView == null) {
            return;
        }

        clearValidation();
        clearFormFieldsOnly();
        joueurTableView.getSelectionModel().select(joueur);
        int rowIndex = filteredJoueurs.indexOf(joueur);
        if (rowIndex >= 0) {
            joueurTableView.scrollTo(rowIndex);
            joueurTableView.edit(rowIndex, joueurNomColumn);
        }
        showMutedStatus("Modifiez directement la ligne puis validez avec Entree.");
    }

    private void handleInlineJoueurEdit(Joueur original, Consumer<Joueur> updater) {
        clearValidation();

        if (original == null || original.getId() == null || joueurService == null) {
            if (joueurTableView != null) {
                joueurTableView.refresh();
            }
            return;
        }

        Joueur candidate = copyJoueur(original);
        updater.accept(candidate);

        String validationMessage = validateInlineJoueur(candidate);
        if (validationMessage != null) {
            if (joueurTableView != null) {
                joueurTableView.refresh();
            }
            showValidation(validationMessage);
            return;
        }

        try {
            joueurService.update(candidate);
            refreshData(original.getId());
            showSuccessStatus("Joueur modifie depuis le tableau.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la modification.");
            showAlert(Alert.AlertType.ERROR, "Modification", "Erreur lors de la modification du joueur.\n" + e.getMessage());
        }
    }

    private void deleteJoueurFromTable(Joueur joueur) {
        clearValidation();

        if (joueur == null || joueur.getId() == null || joueurService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le joueur \"" + buildFullName(joueur) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            joueurService.delete(joueur.getId());
            refreshData(null);
            clearForm();
            showSuccessStatus("Joueur supprime avec succes.");
        } catch (SQLException e) {
            showErrorStatus("Erreur pendant la suppression.");
            showAlert(Alert.AlertType.ERROR, "Suppression", "Erreur lors de la suppression du joueur.\n" + e.getMessage());
        }
    }

    private Joueur copyJoueur(Joueur source) {
        Joueur copy = new Joueur(
                source.getId(),
                source.getNom(),
                source.getPrenom(),
                source.getDateNaissance(),
                source.getNumero(),
                source.getImage(),
                source.getEquipeId()
        );
        copy.setExternalApiId(source.getExternalApiId());
        copy.setExternalSource(source.getExternalSource());
        copy.setPosition(source.getPosition());
        copy.setNationalite(source.getNationalite());
        return copy;
    }

    private String validateInlineJoueur(Joueur joueur) {
        String nom = emptyToNull(joueur == null ? null : joueur.getNom());
        String prenom = emptyToNull(joueur == null ? null : joueur.getPrenom());
        LocalDate dateNaissance = joueur == null ? null : joueur.getDateNaissance();
        Integer equipeId = joueur == null ? null : joueur.getEquipeId();
        int numero = joueur == null ? 0 : joueur.getNumero();
        String image = emptyToNull(joueur == null ? null : joueur.getImage());

        if (nom == null) {
            return "Le nom est obligatoire.";
        }
        if (!PERSON_NAME_PATTERN.matcher(nom).matches()) {
            return "Le nom doit contenir entre 2 et 100 lettres maximum.";
        }
        if (prenom == null) {
            return "Le prenom est obligatoire.";
        }
        if (!PERSON_NAME_PATTERN.matcher(prenom).matches()) {
            return "Le prenom doit contenir entre 2 et 100 lettres maximum.";
        }
        if (dateNaissance == null) {
            return "La date de naissance est obligatoire.";
        }
        if (dateNaissance.isAfter(LocalDate.now())) {
            return "La date de naissance ne peut pas etre dans le futur.";
        }
        if (dateNaissance.isBefore(LocalDate.now().minusYears(100))) {
            return "La date de naissance semble invalide.";
        }
        if (numero == Integer.MIN_VALUE) {
            return "Le numero doit etre un nombre.";
        }
        if (numero < 1 || numero > 99) {
            return "Le numero doit etre entre 1 et 99.";
        }
        if (equipeId == null || !equipeById.containsKey(equipeId)) {
            return "Selectionnez une equipe existante dans le tableau.";
        }
        if (isDuplicateJerseyNumber(equipeId, numero, joueur.getId())) {
            return "Ce numero est deja attribue dans l'equipe selectionnee.";
        }
        if (image != null && !isValidImageReference(image)) {
            return "L'image du joueur doit etre une image valide (.png, .jpg, .jpeg, .gif, .bmp, .webp).";
        }
        return null;
    }

    private Equipe findEquipeByName(String equipeName) {
        String normalizedName = normalize(equipeName);
        if (normalizedName == null) {
            return null;
        }

        return equipes.stream()
                .filter(equipe -> Objects.equals(normalize(equipe.getNom()), normalizedName))
                .findFirst()
                .orElse(null);
    }

    private LocalDate parseInlineBirthDate(String rawValue) {
        String value = emptyToNull(rawValue);
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(value);
            } catch (Exception secondIgnored) {
                return null;
            }
        }
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

        Label numberLabel = new Label(buildPlayerBadge(joueur));
        numberLabel.getStyleClass().add("player-number-badge");

        titleRow.getChildren().addAll(titleLabel, numberLabel);

        Label teamLabel = new Label(buildPlayerSecondaryLine(joueur));
        teamLabel.getStyleClass().add("player-card-team");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label birthLabel = new Label(buildPlayerBirthLine(joueur));
        birthLabel.getStyleClass().add("player-card-meta");

        Label ageLabel = new Label(buildPlayerMetaPill(joueur));
        ageLabel.getStyleClass().add("player-card-meta-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        metaRow.getChildren().addAll(birthLabel, spacer, ageLabel);
        content.getChildren().addAll(titleRow, teamLabel, metaRow);

        root.getChildren().addAll(avatarShell, content);
        root.setMaxWidth(Double.MAX_VALUE);

        VBox wrapper = new VBox(root);
        wrapper.setFillWidth(true);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
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
            updatePlayerDistributionChart();
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
                    || containsNormalized(buildFullName(joueur), query)
                    || containsNormalized(getEquipeName(joueur.getEquipeId()), query)
                    || containsNormalized(joueur.getPosition(), query)
                    || containsNormalized(joueur.getNationalite(), query)
                    || containsNormalized(joueur.getNom(), query)
                    || containsNormalized(joueur.getPrenom(), query);

            boolean matchesEquipe = filterEquipeId == null || Objects.equals(joueur.getEquipeId(), filterEquipeId);
            return matchesQuery && matchesEquipe;
        });

        updateCounters();
        updateEmptyState();
        updatePlayerDistributionChart();

        if (selectedJoueur != null && !filteredJoueurs.contains(selectedJoueur)) {
            clearPlayerSelection();
        }
    }

    private void updatePlayerDistributionChart() {
        if (playerDistributionChart == null) {
            return;
        }
        playerDistributionChart.getData().clear();

        if (filteredJoueurs.isEmpty()) {
            if (playerChartSummaryLabel != null) {
                playerChartSummaryLabel.setText("Aucune statistique joueur disponible.");
            }
            return;
        }

        Map<String, Long> counts = filteredJoueurs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        joueur -> {
                            String equipeName = sanitizeDash(getEquipeName(joueur.getEquipeId()));
                            return equipeName == null ? "Sans equipe" : equipeName;
                        },
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(6)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        playerDistributionChart.getData().add(series);
        applyBarColors(series, darkMode
                ? List.of("#8b5cf6", "#a855f7", "#c084fc", "#60a5fa", "#38bdf8", "#f472b6")
                : List.of("#38bdf8", "#34d399", "#f59e0b", "#f97316", "#a78bfa", "#f43f5e"));

        long withoutTeam = filteredJoueurs.stream().filter(joueur -> joueur.getEquipeId() == null).count();
        double averageAge = filteredJoueurs.stream()
                .map(Joueur::getDateNaissance)
                .filter(Objects::nonNull)
                .mapToInt(date -> Period.between(date, LocalDate.now()).getYears())
                .average()
                .orElse(0);
        if (playerChartSummaryLabel != null) {
            playerChartSummaryLabel.setText(filteredJoueurs.size() + " joueurs | age moyen "
                    + (averageAge <= 0 ? "-" : String.format("%.1f ans", averageAge))
                    + " | " + withoutTeam + " sans equipe");
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
        resultsMetaLabel.setText(joueursCount + (isCardLayout() ? " carte(s)" : " ligne(s)"));
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
            clearPlayerSelection();
            updateActionAvailability();
            return;
        }

        for (Joueur joueur : filteredJoueurs) {
            if (Objects.equals(joueur.getId(), preferredSelectionId)) {
                selectPlayer(joueur);
                selectedJoueur = joueur;
                updateActionAvailability();
                return;
            }
        }

        selectedJoueur = null;
        clearPlayerSelection();
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

        if (dateNaissance.isAfter(LocalDate.now())) {
            markFieldInvalid(dateNaissancePicker);
            showValidation("La date de naissance ne peut pas etre dans le futur.");
            return null;
        }

        if (dateNaissance.isBefore(LocalDate.now().minusYears(100))) {
            markFieldInvalid(dateNaissancePicker);
            showValidation("La date de naissance semble invalide.");
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

        if (!PERSON_NAME_PATTERN.matcher(nom).matches()) {
            markFieldInvalid(nomField);
            showValidation("Le nom doit contenir entre 2 et 100 lettres maximum.");
            return null;
        }

        if (!PERSON_NAME_PATTERN.matcher(prenom).matches()) {
            markFieldInvalid(prenomField);
            showValidation("Le prenom doit contenir entre 2 et 100 lettres maximum.");
            return null;
        }

        if (isDuplicateJerseyNumber(equipe.getId(), numero, updateMode && selectedJoueur != null ? selectedJoueur.getId() : null)) {
            markFieldInvalid(numeroField);
            markFieldInvalid(equipeComboBox);
            showValidation("Ce numero est deja attribue dans l'equipe selectionnee.");
            return null;
        }

        if (image != null && !isValidImageReference(image)) {
            markFieldInvalid(imageField);
            showValidation("L'image du joueur doit etre une image valide (.png, .jpg, .jpeg, .gif, .bmp, .webp).");
            return null;
        }

        if (updateMode && selectedJoueur == null) {
            showValidation("Selectionnez un joueur avant de modifier.");
            return null;
        }

        return new Joueur(nom, prenom, dateNaissance, numero, image, equipe.getId());
    }

    private void clearForm() {
        clearPlayerSelection();
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
        if (dateNaissance == null && selectedJoueur != null) {
            dateNaissance = selectedJoueur.getDateNaissance();
        }

        String numeroValue = emptyToNull(numeroField.getText());
        if (numeroValue == null && selectedJoueur != null && selectedJoueur.getNumero() > 0) {
            numeroValue = String.valueOf(selectedJoueur.getNumero());
        }

        String imagePath = emptyToNull(imageField.getText());
        if (imagePath == null && selectedJoueur != null) {
            imagePath = emptyToNull(selectedJoueur.getImage());
        }

        boolean editing = selectedJoueur != null;
        boolean drafting = editing || hasDraftContent();

        detailBadgeLabel.setText(editing ? "Edition en ligne" : drafting ? "Creation" : "Apercu");
        selectionStateLabel.setText(editing ? "Selection active" : "Mode creation");
        formHintLabel.setText(editing
                ? "Les modifications se font directement dans le tableau via les boutons de la ligne."
                : "Composez une nouvelle fiche joueur et visualisez-la a droite.");

        String positionValue = selectedJoueur == null ? null : emptyToNull(selectedJoueur.getPosition());
        String nationaliteValue = selectedJoueur == null ? null : emptyToNull(selectedJoueur.getNationalite());

        detailNameLabel.setText(fullName == null ? "Aucun joueur selectionne" : fullName);
        detailSubtitleLabel.setText(buildDetailSubtitle(equipeName, dateNaissance, numeroValue, drafting, positionValue, nationaliteValue));
        detailIdValueLabel.setText(editing && selectedJoueur.getId() != null ? "#" + selectedJoueur.getId() : "Nouveau");
        detailEquipeValueLabel.setText(equipeName == null ? "Aucune" : equipeName);
        detailNumeroValueLabel.setText(hasDefinedNumber(numeroValue) ? "#" + numeroValue : "Non defini");

        Image image = loadImage(imagePath);
        boolean hasImage = image != null;
        detailImageView.setImage(image);
        detailImageView.setManaged(hasImage);
        detailImageView.setVisible(hasImage);
        detailImageFallbackLabel.setManaged(!hasImage);
        detailImageFallbackLabel.setVisible(!hasImage);
        detailImageFallbackLabel.setText(buildDraftInitials());
    }

    private void triggerLazyImageImport(Joueur joueur) {
        if (!serviceReady || joueur == null || joueur.getId() == null || !needsLazyImageImport(joueur)) {
            return;
        }
        if (!loadingImageIds.add(joueur.getId())) {
            return;
        }

        showMutedStatus("Importing player photo for " + buildFullName(joueur) + "...");

        Task<String> imageImportTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                JoueurService backgroundService = new JoueurService();
                Joueur fresh = backgroundService.getById(joueur.getId());
                if (fresh == null || !needsLazyImageImport(fresh)) {
                    return null;
                }

                WikidataPlayerImageService imageService = new WikidataPlayerImageService();
                String imagePath = imageService.resolvePlayerImagePath(fresh);
                if (imagePath == null || imagePath.isBlank()) {
                    return null;
                }

                backgroundService.updateImage(fresh.getId(), imagePath);
                return imagePath;
            }
        };

        imageImportTask.setOnSucceeded(event -> {
            loadingImageIds.remove(joueur.getId());
            String imagePath = imageImportTask.getValue();
            if (imagePath == null || imagePath.isBlank()) {
                showMutedStatus("No online photo found for " + buildFullName(joueur) + ".");
                return;
            }

            joueur.setImage(imagePath);
            if (selectedJoueur != null && Objects.equals(selectedJoueur.getId(), joueur.getId())) {
                selectedJoueur.setImage(imagePath);
                imageField.setText(imagePath);
                updateDetailPanel();
            }
            if (joueurListView != null) {
                joueurListView.refresh();
            }
            if (joueurTableView != null) {
                joueurTableView.refresh();
            }
            showSuccessStatus("Player photo imported for " + buildFullName(joueur) + ".");
        });

        imageImportTask.setOnFailed(event -> {
            loadingImageIds.remove(joueur.getId());
            Throwable throwable = imageImportTask.getException();
            showErrorStatus("Could not import photo for " + buildFullName(joueur) + ".");
            if (throwable != null) {
                System.err.println("Lazy player image import failed: " + throwable.getMessage());
            }
        });

        IMAGE_IMPORT_EXECUTOR.execute(imageImportTask);
    }

    private boolean needsLazyImageImport(Joueur joueur) {
        if (joueur == null) {
            return false;
        }
        String image = emptyToNull(joueur.getImage());
        if (image == null) {
            return true;
        }
        String normalized = image.replace('\\', '/').toLowerCase();
        return normalized.contains("fd-player-");
    }

    private String buildDetailSubtitle(
            String equipeName,
            LocalDate dateNaissance,
            String numeroValue,
            boolean drafting,
            String positionValue,
            String nationaliteValue
    ) {
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
        if (hasDefinedNumber(numeroValue)) {
            parts.add("Maillot #" + numeroValue);
        }
        if (positionValue != null) {
            parts.add(positionValue);
        }
        if (nationaliteValue != null) {
            parts.add(nationaliteValue);
        }

        return parts.isEmpty()
                ? "Commencez par saisir les informations principales du joueur."
                : String.join(" | ", parts);
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedJoueur != null;
        boolean createMode = serviceReady && !hasSelection;

        addButton.setDisable(!createMode);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        clearButton.setDisable(!serviceReady);
        refreshButton.setDisable(!serviceReady);
        nomField.setDisable(!createMode);
        prenomField.setDisable(!createMode);
        dateNaissancePicker.setDisable(!createMode);
        numeroField.setDisable(!createMode);
        equipeComboBox.setDisable(!createMode);
        imageField.setDisable(!createMode);
        if (joueurTableView != null) {
            joueurTableView.setDisable(!serviceReady);
        }
        if (joueurListView != null) {
            joueurListView.setDisable(!serviceReady);
        }
    }

    private boolean isCardLayout() {
        return joueurListView != null;
    }

    private void clearPlayerSelection() {
        if (joueurTableView != null) {
            joueurTableView.getSelectionModel().clearSelection();
        }
        if (joueurListView != null) {
            joueurListView.getSelectionModel().clearSelection();
        }
    }

    private void selectPlayer(Joueur joueur) {
        if (joueurTableView != null) {
            joueurTableView.getSelectionModel().select(joueur);
            joueurTableView.scrollTo(joueur);
        }
        if (joueurListView != null) {
            joueurListView.getSelectionModel().select(joueur);
            joueurListView.scrollTo(joueur);
        }
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

    private String buildPlayerBadge(Joueur joueur) {
        if (joueur.getNumero() > 0) {
            return "#" + joueur.getNumero();
        }
        String position = emptyToNull(joueur.getPosition());
        return position == null ? "API" : position;
    }

    private String buildPlayerSecondaryLine(Joueur joueur) {
        String equipeName = sanitizeDash(getEquipeName(joueur.getEquipeId()));
        String position = emptyToNull(joueur.getPosition());
        String nationalite = emptyToNull(joueur.getNationalite());

        List<String> parts = new ArrayList<>();
        if (equipeName != null) {
            parts.add(equipeName);
        }
        if (position != null) {
            parts.add(position);
        }
        if (nationalite != null) {
            parts.add(nationalite);
        }

        return parts.isEmpty() ? "Profil sans equipe" : String.join(" | ", parts);
    }

    private String buildPlayerBirthLine(Joueur joueur) {
        return joueur.getDateNaissance() == null
                ? "Date de naissance indisponible"
                : "Ne le " + formatDate(joueur.getDateNaissance());
    }

    private String buildPlayerMetaPill(Joueur joueur) {
        String nationalite = emptyToNull(joueur.getNationalite());
        return nationalite == null ? formatAge(joueur.getDateNaissance()) : nationalite;
    }

    private String resolveEquipeLabel(Joueur joueur) {
        String equipeName = sanitizeDash(getEquipeName(joueur.getEquipeId()));
        return equipeName == null ? "Sans equipe" : equipeName;
    }

    private String resolveBirthDateLabel(Joueur joueur) {
        return joueur.getDateNaissance() == null ? "-" : formatDate(joueur.getDateNaissance());
    }

    private String resolvePlayerPositionLabel(Joueur joueur) {
        String position = emptyToNull(joueur.getPosition());
        return position == null ? "-" : position;
    }

    private String resolvePlayerNationalityLabel(Joueur joueur) {
        String nationalite = emptyToNull(joueur.getNationalite());
        return nationalite == null ? "-" : nationalite;
    }

    private boolean hasDefinedNumber(String numeroValue) {
        return numeroValue != null && !"0".equals(numeroValue);
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

    private boolean isDuplicateJerseyNumber(Integer equipeId, int numero, Integer ignoredId) {
        return joueurs.stream()
                .filter(joueur -> ignoredId == null || !Objects.equals(joueur.getId(), ignoredId))
                .anyMatch(joueur -> Objects.equals(joueur.getEquipeId(), equipeId) && joueur.getNumero() == numero);
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

    private boolean containsNormalized(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
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

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
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

