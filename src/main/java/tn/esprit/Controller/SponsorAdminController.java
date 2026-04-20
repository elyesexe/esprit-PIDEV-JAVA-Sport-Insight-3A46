package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.ContratSponsorService;
import tn.esprit.services.SponsorService;
import tn.esprit.services.SponsoringPdfService;
import tn.esprit.services.SponsoringWorkspaceService;
import tn.esprit.tools.SponsorAssets;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class SponsorAdminController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FILTER_ALL = "All";
    private static final String SPONSOR_SORT_BUDGET = "Highest budget";
    private static final String SPONSOR_SORT_NAME = "Name A-Z";
    private static final String SPONSOR_SORT_UPDATED = "Updated recently";
    private static final String CONTRACT_SORT_RECENT = "Newest";
    private static final String CONTRACT_SORT_AMOUNT = "Highest amount";
    private static final String CONTRACT_SORT_SPONSOR = "Sponsor name";

    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private ToggleButton themeToggleButton;

    @FXML private Label overviewSponsorCountLabel;
    @FXML private Label overviewContractCountLabel;
    @FXML private Label overviewActiveCountLabel;
    @FXML private Label overviewExpiredCountLabel;
    @FXML private Label overviewBudgetLabel;
    @FXML private Label overviewAverageLabel;
    @FXML private Label overviewStatusLabel;
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private PieChart paymentChart;

    @FXML private TextField sponsorSearchField;
    @FXML private ComboBox<String> sponsorSortComboBox;
    @FXML private TableView<SponsorRow> sponsorTableView;
    @FXML private TableColumn<SponsorRow, String> sponsorNameColumn;
    @FXML private TableColumn<SponsorRow, String> sponsorEmailColumn;
    @FXML private TableColumn<SponsorRow, String> sponsorPhoneColumn;
    @FXML private TableColumn<SponsorRow, String> sponsorBudgetColumn;
    @FXML private TableColumn<SponsorRow, String> sponsorContractsColumn;
    @FXML private TableColumn<SponsorRow, String> sponsorAddressColumn;
    @FXML private Label sponsorTabStatusLabel;
    @FXML private Label sponsorFormHintLabel;
    @FXML private TextField sponsorNameField;
    @FXML private TextField sponsorEmailField;
    @FXML private TextField sponsorPhoneField;
    @FXML private TextField sponsorBudgetField;
    @FXML private TextField sponsorAddressField;
    @FXML private TextField sponsorLogoField;
    @FXML private StackPane sponsorLogoPreviewBox;
    @FXML private Label sponsorValidationLabel;

    @FXML private TextField contractSearchField;
    @FXML private ComboBox<String> contractStatusFilterComboBox;
    @FXML private ComboBox<String> contractSortComboBox;
    @FXML private TableView<ContractRow> contractTableView;
    @FXML private TableColumn<ContractRow, String> contractSponsorColumn;
    @FXML private TableColumn<ContractRow, String> contractTeamColumn;
    @FXML private TableColumn<ContractRow, String> contractAmountColumn;
    @FXML private TableColumn<ContractRow, String> contractPaymentColumn;
    @FXML private TableColumn<ContractRow, String> contractPeriodColumn;
    @FXML private TableColumn<ContractRow, String> contractStatusColumn;
    @FXML private Label contractTabStatusLabel;
    @FXML private Label contractFormHintLabel;
    @FXML private DatePicker contractStartDateField;
    @FXML private DatePicker contractEndDateField;
    @FXML private TextField contractAmountField;
    @FXML private ComboBox<SponsorOption> contractSponsorField;
    @FXML private ComboBox<TeamOption> contractTeamField;
    @FXML private ComboBox<String> contractStatusField;
    @FXML private ComboBox<String> contractPaymentField;
    @FXML private CheckBox contractNotifiedCheckBox;
    @FXML private TextArea contractDescriptionArea;
    @FXML private Label contractValidationLabel;

    private final ObservableList<SponsorRow> sponsorRows = FXCollections.observableArrayList();
    private final ObservableList<ContractRow> contractRows = FXCollections.observableArrayList();
    private final ObservableList<SponsorOption> sponsorOptions = FXCollections.observableArrayList();
    private final ObservableList<TeamOption> teamOptions = FXCollections.observableArrayList();

    private SponsoringWorkspaceService workspaceService;
    private SponsoringPdfService pdfService;
    private SponsorService sponsorService;
    private ContratSponsorService contratSponsorService;
    private SponsoringWorkspaceService.SponsoringSnapshot snapshot;
    private SponsorRow selectedSponsorRow;
    private ContractRow selectedContractRow;
    private boolean darkMode;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        pdfService = new SponsoringPdfService();
        configureOverview();
        configureSponsorTable();
        configureContractTable();
        configureChoiceBoxes();
        configureListeners();

        try {
            workspaceService = new SponsoringWorkspaceService();
            sponsorService = new SponsorService();
            contratSponsorService = new ContratSponsorService();
            refreshWorkspace("Sponsoring workspace ready.");
        } catch (SQLException e) {
            showError("Sponsoring", "Could not load sponsor administration.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox,
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleRefreshOverview() {
        refreshWorkspace("Overview refreshed.");
    }

    @FXML
    private void handleExportSummaryPdf() {
        if (snapshot == null) {
            showOverviewStatus("Nothing to export.", "status-warning");
            return;
        }
        Path target = choosePdfTarget("sponsoring-summary.pdf");
        if (target == null) {
            return;
        }
        try {
            pdfService.exportSummaryPdf(target, snapshot, workspaceService);
            openFile(target);
            showOverviewStatus("Summary PDF exported.", "status-success");
        } catch (IOException e) {
            showOverviewStatus("PDF export failed.", "status-error");
            showError("PDF", "Could not export the sponsoring summary.\n" + e.getMessage());
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (budgetChart != null) {
            budgetChart.applyCss();
        }
        if (paymentChart != null) {
            paymentChart.applyCss();
        }
        Platform.runLater(() -> applyPieChartTheme(paymentChart, darkMode));
    }

    private void configureOverview() {
        if (budgetChart != null) {
            budgetChart.setLegendVisible(false);
            budgetChart.setAnimated(false);
        }
        if (paymentChart != null) {
            paymentChart.setLabelsVisible(true);
            paymentChart.setLegendVisible(true);
            paymentChart.setClockwise(true);
            Platform.runLater(() -> applyPieChartTheme(paymentChart, isDarkModeEnabled()));
        }
    }

    private void configureSponsorTable() {
        sponsorNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(fallbackText(cell.getValue().sponsor().getNom(), "Sponsor")));
        sponsorEmailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(fallbackText(cell.getValue().sponsor().getEmail(), "-")));
        sponsorPhoneColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(fallbackText(cell.getValue().sponsor().getTelephone(), "-")));
        sponsorBudgetColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatCurrency(cell.getValue().sponsor().getBudget())));
        sponsorContractsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().contractCount())));
        sponsorAddressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(fallbackText(cell.getValue().sponsor().getAdresse(), "-")));
        sponsorTableView.setItems(sponsorRows);
        sponsorTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sponsorTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedSponsorRow = newValue;
            if (newValue == null) {
                sponsorFormHintLabel.setText("Create a new sponsor or select one to edit it.");
                return;
            }
            populateSponsorForm(newValue.sponsor());
            sponsorFormHintLabel.setText("Editing sponsor #" + newValue.sponsor().getId());
        });
    }

    private void configureContractTable() {
        contractSponsorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().sponsorName()));
        contractTeamColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().teamName()));
        contractAmountColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatCurrency(cell.getValue().contrat().getMontant())));
        contractPaymentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().paymentStatus()));
        contractPeriodColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatDate(cell.getValue().contrat().getDateDebut()) + " -> " + formatDate(cell.getValue().contrat().getDateFin())
        ));
        contractStatusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status()));
        contractTableView.setItems(contractRows);
        contractTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        contractTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedContractRow = newValue;
            if (newValue == null) {
                contractFormHintLabel.setText("Select a contract or create a new one.");
                return;
            }
            populateContractForm(newValue.contrat());
            contractFormHintLabel.setText("Editing contract #" + newValue.contrat().getId());
        });
    }

    private void configureChoiceBoxes() {
        sponsorSortComboBox.setItems(FXCollections.observableArrayList(
                SPONSOR_SORT_BUDGET,
                SPONSOR_SORT_NAME,
                SPONSOR_SORT_UPDATED
        ));
        sponsorSortComboBox.setValue(SPONSOR_SORT_BUDGET);

        contractStatusFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        contractStatusFilterComboBox.setValue(FILTER_ALL);

        contractSortComboBox.setItems(FXCollections.observableArrayList(
                CONTRACT_SORT_RECENT,
                CONTRACT_SORT_AMOUNT,
                CONTRACT_SORT_SPONSOR
        ));
        contractSortComboBox.setValue(CONTRACT_SORT_RECENT);

        contractSponsorField.setItems(sponsorOptions);
        contractTeamField.setItems(teamOptions);
        contractStatusField.setItems(FXCollections.observableArrayList("ACTIVE", "RENEWED", "DRAFT", "EXPIRED"));
        contractPaymentField.setItems(FXCollections.observableArrayList("PENDING", "PAID", "PARTIAL"));
        contractStatusField.setValue("ACTIVE");
        contractPaymentField.setValue("PENDING");
    }

    private void configureListeners() {
        sponsorSearchField.textProperty().addListener((obs, oldValue, newValue) -> applySponsorFilters());
        sponsorSortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applySponsorFilters());
        contractSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyContractFilters());
        contractStatusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyContractFilters());
        contractSortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyContractFilters());
        sponsorLogoField.textProperty().addListener((obs, oldValue, newValue) -> updateSponsorPreview());
        sponsorNameField.textProperty().addListener((obs, oldValue, newValue) -> updateSponsorPreview());
    }

    @FXML
    private void handleRefreshSponsors() {
        refreshWorkspace("Sponsors refreshed.");
    }

    @FXML
    private void handleChooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose sponsor logo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File selectedFile = chooser.showOpenDialog(sponsorLogoField.getScene() == null ? null : sponsorLogoField.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }
        try {
            sponsorLogoField.setText(SponsorAssets.importLogo(selectedFile.toPath()));
            updateSponsorPreview();
            showSponsorStatus("Logo imported into the project workspace.", "status-success");
        } catch (IOException e) {
            showSponsorStatus("Logo import failed.", "status-error");
            showError("Logo", "Could not import the selected logo.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAddSponsor() {
        clearSponsorValidation();
        Sponsor sponsor = buildSponsorFromForm();
        if (sponsor == null) {
            return;
        }
        try {
            sponsorService.add(sponsor);
            refreshWorkspace("Sponsor added.");
            clearSponsorForm();
            showSponsorStatus("Sponsor added successfully.", "status-success");
        } catch (SQLException e) {
            showSponsorStatus("Sponsor add failed.", "status-error");
            showError("Sponsor", "Could not add the sponsor.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateSponsor() {
        clearSponsorValidation();
        if (selectedSponsorRow == null) {
            showSponsorValidation("Select a sponsor before updating.");
            return;
        }
        Sponsor sponsor = buildSponsorFromForm();
        if (sponsor == null) {
            return;
        }
        sponsor.setId(selectedSponsorRow.sponsor().getId());
        try {
            sponsorService.update(sponsor);
            refreshWorkspace("Sponsor updated.");
            clearSponsorForm();
            showSponsorStatus("Sponsor updated successfully.", "status-success");
        } catch (SQLException e) {
            showSponsorStatus("Sponsor update failed.", "status-error");
            showError("Sponsor", "Could not update the sponsor.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteSponsor() {
        clearSponsorValidation();
        if (selectedSponsorRow == null) {
            showSponsorValidation("Select a sponsor before deleting.");
            return;
        }
        if (!confirm("Delete this sponsor and its dependent contracts?")) {
            return;
        }
        try {
            sponsorService.delete(selectedSponsorRow.sponsor().getId());
            refreshWorkspace("Sponsor deleted.");
            clearSponsorForm();
            showSponsorStatus("Sponsor deleted successfully.", "status-success");
        } catch (SQLException e) {
            showSponsorStatus("Sponsor delete failed.", "status-error");
            showError("Sponsor", "Could not delete the sponsor.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearSponsor() {
        clearSponsorForm();
    }

    @FXML
    private void handleRefreshContracts() {
        refreshWorkspace("Contracts refreshed.");
    }

    @FXML
    private void handleAddContract() {
        clearContractValidation();
        ContratSponsor contrat = buildContractFromForm();
        if (contrat == null) {
            return;
        }
        try {
            contratSponsorService.add(contrat);
            refreshWorkspace("Contract added.");
            clearContractForm();
            showContractStatus("Contract added successfully.", "status-success");
        } catch (SQLException e) {
            showContractStatus("Contract add failed.", "status-error");
            showError("Contract", "Could not add the contract.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateContract() {
        clearContractValidation();
        if (selectedContractRow == null) {
            showContractValidation("Select a contract before updating.");
            return;
        }
        ContratSponsor contrat = buildContractFromForm();
        if (contrat == null) {
            return;
        }
        contrat.setId(selectedContractRow.contrat().getId());
        try {
            contratSponsorService.update(contrat);
            refreshWorkspace("Contract updated.");
            clearContractForm();
            showContractStatus("Contract updated successfully.", "status-success");
        } catch (SQLException e) {
            showContractStatus("Contract update failed.", "status-error");
            showError("Contract", "Could not update the contract.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteContract() {
        clearContractValidation();
        if (selectedContractRow == null) {
            showContractValidation("Select a contract before deleting.");
            return;
        }
        if (!confirm("Delete this contract?")) {
            return;
        }
        try {
            contratSponsorService.delete(selectedContractRow.contrat().getId());
            refreshWorkspace("Contract deleted.");
            clearContractForm();
            showContractStatus("Contract deleted successfully.", "status-success");
        } catch (SQLException e) {
            showContractStatus("Contract delete failed.", "status-error");
            showError("Contract", "Could not delete the contract.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearContract() {
        clearContractForm();
    }

    @FXML
    private void handleExportContractPdf() {
        if (selectedContractRow == null) {
            showContractValidation("Select a contract before exporting.");
            return;
        }
        String sponsorName = safeSlug(selectedContractRow.sponsorName().isBlank() ? "contract" : selectedContractRow.sponsorName());
        Path target = choosePdfTarget(sponsorName + "-contract.pdf");
        if (target == null) {
            return;
        }
        try {
            pdfService.exportContractPdf(
                    target,
                    selectedContractRow.contrat(),
                    snapshot.sponsorOf(selectedContractRow.contrat()),
                    snapshot.equipeOf(selectedContractRow.contrat())
            );
            openFile(target);
            showContractStatus("Contract PDF exported.", "status-success");
        } catch (IOException e) {
            showContractStatus("Contract PDF export failed.", "status-error");
            showError("PDF", "Could not export the selected contract.\n" + e.getMessage());
        }
    }

    private void refreshWorkspace(String overviewMessage) {
        if (workspaceService == null) {
            return;
        }
        try {
            snapshot = workspaceService.loadSnapshot();
            rebuildChoiceBoxData();
            updateOverview();
            applySponsorFilters();
            applyContractFilters();
            showOverviewStatus(overviewMessage, "status-success");
        } catch (SQLException e) {
            showOverviewStatus("Refresh failed.", "status-error");
            showError("Sponsoring", "Could not refresh sponsor data.\n" + e.getMessage());
        }
    }

    private void rebuildChoiceBoxData() {
        String currentFilter = contractStatusFilterComboBox.getValue();
        SponsorOption currentSponsor = contractSponsorField.getValue();
        TeamOption currentTeam = contractTeamField.getValue();

        sponsorOptions.setAll(snapshot.sponsors().stream()
                .map(sponsor -> new SponsorOption(sponsor.getId(), fallbackText(sponsor.getNom(), "Sponsor")))
                .sorted(Comparator.comparing(SponsorOption::label, String.CASE_INSENSITIVE_ORDER))
                .toList());

        teamOptions.setAll(snapshot.equipesById().values().stream()
                .map(equipe -> new TeamOption(equipe.getId(), fallbackText(equipe.getNom(), "Team")))
                .sorted(Comparator.comparing(TeamOption::label, String.CASE_INSENSITIVE_ORDER))
                .toList());

        List<String> statuses = snapshot.contrats().stream()
                .map(workspaceService::resolveContractStatus)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        contractStatusFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        contractStatusFilterComboBox.getItems().addAll(statuses);
        contractStatusFilterComboBox.setValue(statuses.contains(currentFilter) ? currentFilter : FILTER_ALL);

        if (currentSponsor != null) {
            sponsorOptions.stream()
                    .filter(option -> Objects.equals(option.id(), currentSponsor.id()))
                    .findFirst()
                    .ifPresentOrElse(contractSponsorField::setValue, () -> contractSponsorField.getSelectionModel().clearSelection());
        } else {
            contractSponsorField.getSelectionModel().clearSelection();
        }
        if (currentTeam != null) {
            teamOptions.stream()
                    .filter(option -> Objects.equals(option.id(), currentTeam.id()))
                    .findFirst()
                    .ifPresentOrElse(contractTeamField::setValue, () -> contractTeamField.getSelectionModel().clearSelection());
        } else {
            contractTeamField.getSelectionModel().clearSelection();
        }
    }

    private void updateOverview() {
        SponsoringWorkspaceService.SponsoringStats stats = snapshot.stats();
        overviewSponsorCountLabel.setText(String.valueOf(stats.totalSponsors()));
        overviewContractCountLabel.setText(String.valueOf(stats.totalContrats()));
        overviewActiveCountLabel.setText(String.valueOf(stats.activeContracts()));
        overviewExpiredCountLabel.setText(String.valueOf(stats.expiredContracts()));
        overviewBudgetLabel.setText(formatCompactCurrency(stats.totalBudget()));
        overviewAverageLabel.setText(formatCompactCurrency(stats.averageContractAmount()));

        XYChart.Series<String, Number> budgetSeries = new XYChart.Series<>();
        for (SponsoringWorkspaceService.SponsorBudgetPoint point : stats.topSponsors()) {
            budgetSeries.getData().add(new XYChart.Data<>(point.label(), point.value()));
        }
        budgetChart.getData().setAll(budgetSeries);

        ObservableList<PieChart.Data> paymentData = FXCollections.observableArrayList();
        stats.paymentBreakdown().forEach((label, value) -> paymentData.add(new PieChart.Data(label + " (" + value + ")", value)));
        paymentChart.setData(paymentData);
        Platform.runLater(() -> applyPieChartTheme(paymentChart, isDarkModeEnabled()));
    }

    private void applySponsorFilters() {
        if (snapshot == null) {
            sponsorRows.clear();
            return;
        }

        String query = normalize(sponsorSearchField.getText());
        List<SponsorRow> rows = new ArrayList<>();

        for (Sponsor sponsor : snapshot.sponsors()) {
            if (query != null
                    && !contains(sponsor.getNom(), query)
                    && !contains(sponsor.getEmail(), query)
                    && !contains(sponsor.getTelephone(), query)
                    && !contains(sponsor.getAdresse(), query)) {
                continue;
            }

            long contractCount = snapshot.contrats().stream()
                    .filter(contrat -> Objects.equals(contrat.getSponsorId(), sponsor.getId()))
                    .count();
            rows.add(new SponsorRow(sponsor, contractCount));
        }

        rows.sort(resolveSponsorComparator(sponsorSortComboBox.getValue()));
        sponsorRows.setAll(rows);
    }

    private void applyContractFilters() {
        if (snapshot == null) {
            contractRows.clear();
            return;
        }

        String query = normalize(contractSearchField.getText());
        String statusFilter = normalizeFilter(contractStatusFilterComboBox.getValue());

        List<ContractRow> rows = snapshot.contrats().stream()
                .map(contrat -> new ContractRow(
                        contrat,
                        fallbackText(snapshot.sponsorOf(contrat) == null ? null : snapshot.sponsorOf(contrat).getNom(), "Sponsor"),
                        fallbackText(snapshot.equipeOf(contrat) == null ? null : snapshot.equipeOf(contrat).getNom(), "Team"),
                        workspaceService.resolveContractStatus(contrat),
                        workspaceService.resolvePaymentStatus(contrat)
                ))
                .filter(row -> matchesContractRow(row, query, statusFilter))
                .sorted(resolveContractComparator(contractSortComboBox.getValue()))
                .toList();

        contractRows.setAll(rows);
    }

    private Comparator<SponsorRow> resolveSponsorComparator(String sortMode) {
        if (SPONSOR_SORT_NAME.equals(sortMode)) {
            return Comparator.comparing(row -> fallbackText(row.sponsor().getNom(), "Sponsor"), String.CASE_INSENSITIVE_ORDER);
        }
        if (SPONSOR_SORT_UPDATED.equals(sortMode)) {
            return Comparator.comparing((SponsorRow row) -> row.sponsor().getUpdatedAt(), Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
        }
        return Comparator.comparingDouble((SponsorRow row) -> row.sponsor().getBudget()).reversed();
    }

    private boolean matchesContractRow(ContractRow row, String query, String statusFilter) {
        boolean queryMatches = query == null
                || contains(row.sponsorName(), query)
                || contains(row.teamName(), query)
                || contains(row.contrat().getDescription(), query)
                || contains(row.status(), query)
                || contains(row.paymentStatus(), query);

        boolean statusMatches = statusFilter == null || Objects.equals(normalize(row.status()), statusFilter);
        return queryMatches && statusMatches;
    }

    private Comparator<ContractRow> resolveContractComparator(String sortMode) {
        if (CONTRACT_SORT_AMOUNT.equals(sortMode)) {
            return Comparator.comparingDouble((ContractRow row) -> row.contrat().getMontant()).reversed();
        }
        if (CONTRACT_SORT_SPONSOR.equals(sortMode)) {
            return Comparator.comparing(ContractRow::sponsorName, String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing((ContractRow row) -> row.contrat().getDateDebut(), Comparator.nullsLast(LocalDate::compareTo)).reversed();
    }

    private Sponsor buildSponsorFromForm() {
        String name = requiredText(sponsorNameField, "Sponsor name");
        String email = requiredText(sponsorEmailField, "Sponsor email");
        String phone = requiredText(sponsorPhoneField, "Sponsor phone");
        String address = requiredText(sponsorAddressField, "Sponsor address");
        Double budget = parsePositiveDouble(sponsorBudgetField, "Budget", sponsorValidationLabel);
        if (name == null || email == null || phone == null || address == null || budget == null) {
            return null;
        }
        if (!email.contains("@")) {
            markInvalid(sponsorEmailField);
            showSponsorValidation("Sponsor email is invalid.");
            return null;
        }

        Sponsor sponsor = new Sponsor();
        sponsor.setNom(name);
        sponsor.setEmail(email);
        sponsor.setTelephone(phone);
        sponsor.setBudget(budget);
        sponsor.setAdresse(address);
        sponsor.setLogoName(optionalText(sponsorLogoField.getText()));
        sponsor.setUpdatedAt(LocalDateTime.now());
        return sponsor;
    }

    private ContratSponsor buildContractFromForm() {
        LocalDate startDate = contractStartDateField.getValue();
        LocalDate endDate = contractEndDateField.getValue();
        if (startDate == null) {
            markInvalid(contractStartDateField);
            showContractValidation("Start date is required.");
            return null;
        }
        if (endDate == null) {
            markInvalid(contractEndDateField);
            showContractValidation("End date is required.");
            return null;
        }
        if (endDate.isBefore(startDate)) {
            markInvalid(contractEndDateField);
            showContractValidation("End date must be after the start date.");
            return null;
        }

        Double amount = parsePositiveDouble(contractAmountField, "Amount", contractValidationLabel);
        if (amount == null) {
            return null;
        }

        SponsorOption sponsorOption = contractSponsorField.getValue();
        TeamOption teamOption = contractTeamField.getValue();
        if (sponsorOption == null) {
            markInvalid(contractSponsorField);
            showContractValidation("Sponsor selection is required.");
            return null;
        }
        if (teamOption == null) {
            markInvalid(contractTeamField);
            showContractValidation("Team selection is required.");
            return null;
        }

        ContratSponsor contrat = new ContratSponsor();
        contrat.setDateDebut(startDate);
        contrat.setDateFin(endDate);
        contrat.setMontant(amount);
        contrat.setDescription(optionalText(contractDescriptionArea.getText()));
        contrat.setStatut(fallbackText(contractStatusField.getValue(), "ACTIVE"));
        contrat.setStatutPaiement(fallbackText(contractPaymentField.getValue(), "PENDING"));
        contrat.setSponsorId(sponsorOption.id());
        contrat.setEquipeId(teamOption.id());
        contrat.setNotified(contractNotifiedCheckBox.isSelected());
        return contrat;
    }

    private void populateSponsorForm(Sponsor sponsor) {
        sponsorNameField.setText(fallbackText(sponsor.getNom(), ""));
        sponsorEmailField.setText(fallbackText(sponsor.getEmail(), ""));
        sponsorPhoneField.setText(fallbackText(sponsor.getTelephone(), ""));
        sponsorBudgetField.setText(String.format(Locale.ENGLISH, "%.2f", sponsor.getBudget()));
        sponsorAddressField.setText(fallbackText(sponsor.getAdresse(), ""));
        sponsorLogoField.setText(fallbackText(sponsor.getLogoName(), ""));
        updateSponsorPreview();
        clearSponsorValidation();
    }

    private void populateContractForm(ContratSponsor contrat) {
        contractStartDateField.setValue(contrat.getDateDebut());
        contractEndDateField.setValue(contrat.getDateFin());
        contractAmountField.setText(String.format(Locale.ENGLISH, "%.2f", contrat.getMontant()));
        contractDescriptionArea.setText(fallbackText(contrat.getDescription(), ""));
        contractStatusField.setValue(fallbackText(contrat.getStatut(), "ACTIVE"));
        contractPaymentField.setValue(fallbackText(contrat.getStatutPaiement(), "PENDING"));
        contractNotifiedCheckBox.setSelected(contrat.isNotified());

        sponsorOptions.stream()
                .filter(option -> Objects.equals(option.id(), contrat.getSponsorId()))
                .findFirst()
                .ifPresent(contractSponsorField::setValue);
        teamOptions.stream()
                .filter(option -> Objects.equals(option.id(), contrat.getEquipeId()))
                .findFirst()
                .ifPresent(contractTeamField::setValue);

        clearContractValidation();
    }

    private void clearSponsorForm() {
        selectedSponsorRow = null;
        sponsorTableView.getSelectionModel().clearSelection();
        sponsorNameField.clear();
        sponsorEmailField.clear();
        sponsorPhoneField.clear();
        sponsorBudgetField.clear();
        sponsorAddressField.clear();
        sponsorLogoField.clear();
        sponsorLogoPreviewBox.getChildren().clear();
        sponsorFormHintLabel.setText("Create a new sponsor or select one to edit it.");
        clearSponsorValidation();
    }

    private void clearContractForm() {
        selectedContractRow = null;
        contractTableView.getSelectionModel().clearSelection();
        contractStartDateField.setValue(null);
        contractEndDateField.setValue(null);
        contractAmountField.clear();
        contractSponsorField.getSelectionModel().clearSelection();
        contractTeamField.getSelectionModel().clearSelection();
        contractStatusField.setValue("ACTIVE");
        contractPaymentField.setValue("PENDING");
        contractNotifiedCheckBox.setSelected(false);
        contractDescriptionArea.clear();
        contractFormHintLabel.setText("Select a contract or create a new one.");
        clearContractValidation();
    }

    private void updateSponsorPreview() {
        if (sponsorLogoPreviewBox == null) {
            return;
        }
        sponsorLogoPreviewBox.getChildren().setAll(buildLogoNode(
                sponsorLogoField.getText(),
                sponsorNameField.getText(),
                84
        ));
    }

    private Node buildLogoNode(String logoReference, String fallbackName, double imageSize) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("sponsor-logo-preview-shell");
        shell.setPrefSize(imageSize + 24, imageSize + 24);
        shell.setMinSize(imageSize + 24, imageSize + 24);

        String logoUrl = SponsorAssets.resolveDisplayLogoUrl(logoReference);
        if (logoUrl != null) {
            Image image = new Image(logoUrl, imageSize, imageSize, true, true, false);
            if (!image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(imageSize);
                imageView.setFitHeight(imageSize);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.getStyleClass().add("sponsor-logo-image");
                shell.getChildren().add(imageView);
                return shell;
            }
        }

        Label fallbackLabel = new Label(resolveInitial(fallbackName));
        fallbackLabel.getStyleClass().add("sponsor-logo-fallback");
        shell.getChildren().add(fallbackLabel);
        return shell;
    }

    private void showSponsorValidation(String message) {
        sponsorValidationLabel.setText(message);
        sponsorValidationLabel.setManaged(true);
        sponsorValidationLabel.setVisible(true);
    }

    private void clearSponsorValidation() {
        sponsorValidationLabel.setText("");
        sponsorValidationLabel.setManaged(false);
        sponsorValidationLabel.setVisible(false);
        clearInvalid(sponsorNameField, sponsorEmailField, sponsorPhoneField, sponsorBudgetField, sponsorAddressField);
    }

    private void showContractValidation(String message) {
        contractValidationLabel.setText(message);
        contractValidationLabel.setManaged(true);
        contractValidationLabel.setVisible(true);
    }

    private void clearContractValidation() {
        contractValidationLabel.setText("");
        contractValidationLabel.setManaged(false);
        contractValidationLabel.setVisible(false);
        clearInvalid(
                contractStartDateField,
                contractEndDateField,
                contractAmountField,
                contractSponsorField,
                contractTeamField
        );
    }

    private String requiredText(TextField field, String label) {
        String value = optionalText(field.getText());
        if (value == null) {
            markInvalid(field);
            showSponsorValidation(label + " is required.");
            return null;
        }
        return value;
    }

    private Double parsePositiveDouble(TextField field, String label, Label validationLabel) {
        String value = optionalText(field.getText());
        if (value == null) {
            markInvalid(field);
            validationLabel.setText(label + " is required.");
            validationLabel.setManaged(true);
            validationLabel.setVisible(true);
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            markInvalid(field);
            validationLabel.setText(label + " must be a positive number.");
            validationLabel.setManaged(true);
            validationLabel.setVisible(true);
            return null;
        }
    }

    private Path choosePdfTarget(String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName(suggestedName);
        File selected = chooser.showSaveDialog(
                sponsorTableView.getScene() == null ? null : sponsorTableView.getScene().getWindow()
        );
        return selected == null ? null : selected.toPath();
    }

    private void openFile(Path path) {
        if (path == null) {
            return;
        }
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(path.toFile());
                return;
            } catch (IOException ignored) {
                // Fall through to manual message.
            }
        }
        showInfo("PDF exported", "The PDF was generated here:\n" + path.toAbsolutePath());
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.ENGLISH, "%,.2f DT", amount);
    }

    private String formatCompactCurrency(double amount) {
        if (Math.abs(amount) >= 1000) {
            return String.format(Locale.ENGLISH, "%,.1fK DT", amount / 1000.0);
        }
        return String.format(Locale.ENGLISH, "%,.0f DT", amount);
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safeSlug(String value) {
        return fallbackText(value, "export")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String resolveInitial(String value) {
        String safe = fallbackText(value, "S");
        return safe.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private boolean contains(String source, String query) {
        return query != null
                && source != null
                && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFilter(String value) {
        String normalized = normalize(value);
        return normalized == null || FILTER_ALL.equalsIgnoreCase(value) ? null : normalized;
    }

    private void markInvalid(Node node) {
        if (node != null && !node.getStyleClass().contains("invalid-field")) {
            node.getStyleClass().add("invalid-field");
        }
    }

    private void clearInvalid(Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                node.getStyleClass().remove("invalid-field");
            }
        }
    }

    private void showOverviewStatus(String message, String styleClass) {
        setStatusLabel(overviewStatusLabel, message, styleClass);
    }

    private void showSponsorStatus(String message, String styleClass) {
        setStatusLabel(sponsorTabStatusLabel, message, styleClass);
    }

    private void showContractStatus(String message, String styleClass) {
        setStatusLabel(contractTabStatusLabel, message, styleClass);
    }

    private void setStatusLabel(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private void applyPieChartTheme(PieChart chart, boolean darkMode) {
        if (chart == null) {
            return;
        }
        String labelColor = darkMode ? "#eef3ff" : "#475569";
        String lineColor = darkMode ? "rgba(226, 232, 255, 0.58)" : "rgba(71, 85, 105, 0.5)";
        String legendColor = darkMode ? "#eef3ff" : "#475569";
        String legendBackground = darkMode ? "rgba(31, 38, 67, 0.96)" : "rgba(255, 255, 255, 0.82)";

        chart.applyCss();
        chart.lookupAll(".chart-pie-label").forEach(node ->
                node.setStyle("-fx-fill: " + labelColor + "; -fx-font-weight: 700;"));
        chart.lookupAll(".chart-pie-label-line").forEach(node ->
                node.setStyle("-fx-stroke: " + lineColor + ";"));
        chart.lookupAll(".chart-legend").forEach(node ->
                node.setStyle("-fx-background-color: " + legendBackground + "; -fx-background-radius: 12;"));
        chart.lookupAll(".chart-legend-item").forEach(node ->
                node.setStyle("-fx-text-fill: " + legendColor + ";"));
        chart.lookupAll(".chart-legend-item .label").forEach(node ->
                node.setStyle("-fx-text-fill: " + legendColor + ";"));
    }

    private boolean isDarkModeEnabled() {
        return darkMode || (themeToggleButton != null && themeToggleButton.isSelected());
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record SponsorRow(Sponsor sponsor, long contractCount) {
    }

    private record ContractRow(
            ContratSponsor contrat,
            String sponsorName,
            String teamName,
            String status,
            String paymentStatus
    ) {
    }

    private record SponsorOption(Integer id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record TeamOption(Integer id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
