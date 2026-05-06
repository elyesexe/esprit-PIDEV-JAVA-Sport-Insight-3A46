package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.embed.swing.SwingFXUtils;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.ContractQrCodeService;
import tn.esprit.services.SponsorMapViewService;
import tn.esprit.services.SponsoringWorkspaceService;
import tn.esprit.tools.SponsorAssets;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SponsorUserController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter UPDATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String FILTER_ALL = "All";
    private static final String SORT_RECENT = "Most recent";
    private static final String SORT_AMOUNT = "Highest amount";
    private static final String SORT_NAME = "Sponsor name";
    private static final int USER_SPONSOR_PAGE_SIZE = 6;
    private static final int USER_CONTRACT_PAGE_SIZE = 4;
    private static final String LIGHT_PAGE_BACKGROUND = "-fx-background-color:"
            + " radial-gradient(center 16% 14%, radius 34%, rgba(16, 185, 129, 0.16) 0%, rgba(16, 185, 129, 0) 100%),"
            + " radial-gradient(center 86% 18%, radius 28%, rgba(245, 158, 11, 0.14) 0%, rgba(245, 158, 11, 0) 100%),"
            + " linear-gradient(from 0% 0% to 100% 100%, #fbfffd 0%, #f3faf7 38%, #f8fafc 100%);";
    private static final String DARK_PAGE_BACKGROUND = "-fx-background-color:"
            + " radial-gradient(center 18% 14%, radius 36%, rgba(16, 185, 129, 0.16) 0%, rgba(16, 185, 129, 0) 100%),"
            + " radial-gradient(center 84% 14%, radius 28%, rgba(245, 158, 11, 0.12) 0%, rgba(245, 158, 11, 0) 100%),"
            + " linear-gradient(from 0% 0% to 100% 100%, #071019 0%, #0f172a 48%, #111827 100%);";

    @FXML
    private BorderPane pageRoot;
    @FXML
    private ScrollPane pageScroll;
    @FXML
    private StackPane pageShellWrap;
    @FXML
    private VBox pageShell;
    @FXML
    private HBox navbarRoot;
    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button matchsNavButton;
    @FXML
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button annonceNavButton;
    @FXML
    private Button sponsorNavButton;
    @FXML
    private ToggleButton themeToggleButton;

    @FXML
    private Label totalSponsorsMetricLabel;
    @FXML
    private Label totalContractsMetricLabel;
    @FXML
    private Label totalBudgetMetricLabel;
    @FXML
    private Label averageContractMetricLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> teamFilterComboBox;
    @FXML
    private ComboBox<String> paymentFilterComboBox;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Label sponsorCountLabel;
    @FXML
    private Label contractCountLabel;
    @FXML
    private Button sponsorPreviousPageButton;
    @FXML
    private Button sponsorNextPageButton;
    @FXML
    private Label sponsorPaginationLabel;
    @FXML
    private Button contractPreviousPageButton;
    @FXML
    private Button contractNextPageButton;
    @FXML
    private Label contractPaginationLabel;
    @FXML
    private Label toolbarSummaryLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private FlowPane sponsorCardsPane;
    @FXML
    private VBox contractCardsPane;

    private SidebarModuleGroup sidebarModuleGroup;
    private SponsoringWorkspaceService workspaceService;
    private ContractQrCodeService qrCodeService;
    private SponsorMapViewService sponsorMapViewService;
    private SponsoringWorkspaceService.SponsoringSnapshot snapshot;
    private List<Sponsor> filteredSponsors = List.of();
    private List<ContractViewModel> filteredContracts = List.of();
    private int sponsorPageIndex;
    private int contractPageIndex;

    @FXML
    public void initialize() {
        configureNavbar();
        ThemeManager.bindToggle(themeToggleButton);
        applyThemeState(themeToggleButton != null && themeToggleButton.isSelected());
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, oldValue, selected) -> applyThemeState(selected));
        }
        if (pageScroll != null) {
            pageScroll.skinProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
            pageScroll.sceneProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
        }
        configureFilters();

        try {
            workspaceService = new SponsoringWorkspaceService();
            qrCodeService = new ContractQrCodeService();
            sponsorMapViewService = new SponsorMapViewService();
            refreshData("Sponsor feed ready.");
        } catch (SQLException e) {
            showErrorStatus("Sponsor feed unavailable.");
            showAlert(Alert.AlertType.ERROR, "Sponsors", "Could not load sponsors.\n" + e.getMessage());
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
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton,
                "/tn/esprit/views/match-competitions-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Matchs | Competitions");
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton,
                "/tn/esprit/views/equipe-competitions-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Equipes | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton,
                "/tn/esprit/views/league-competitions-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton,
                "/tn/esprit/views/joueur-crud-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton,
                "/tn/esprit/views/annonce-user-view.fxml",
                "/tn/esprit/styles/annonce-theme.css",
                "Annonce | Sport Insight");
    }

    @FXML
    private void handleOpenSponsors() {
        applyFilters();
    }

    @FXML
    private void handleRefresh() {
        refreshData("Sponsor feed refreshed.");
    }

    @FXML
    private void handleSponsorPreviousPage() {
        if (sponsorPageIndex > 0) {
            sponsorPageIndex--;
            updateSponsorPage();
        }
    }

    @FXML
    private void handleSponsorNextPage() {
        if (sponsorPageIndex + 1 < computeTotalPages(filteredSponsors.size(), USER_SPONSOR_PAGE_SIZE)) {
            sponsorPageIndex++;
            updateSponsorPage();
        }
    }

    @FXML
    private void handleContractPreviousPage() {
        if (contractPageIndex > 0) {
            contractPageIndex--;
            updateContractPage();
        }
    }

    @FXML
    private void handleContractNextPage() {
        if (contractPageIndex + 1 < computeTotalPages(filteredContracts.size(), USER_CONTRACT_PAGE_SIZE)) {
            contractPageIndex++;
            updateContractPage();
        }
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        teamFilterComboBox.setValue(FILTER_ALL);
        paymentFilterComboBox.setValue(FILTER_ALL);
        sortComboBox.setValue(SORT_RECENT);
        applyFilters();
        showMutedStatus("Filters reset.");
    }

    private void configureNavbar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
        if (sponsorNavButton != null && !sponsorNavButton.getStyleClass().contains("navbar-nav-button-active")) {
            sponsorNavButton.getStyleClass().add("navbar-nav-button-active");
        }
    }

    private void applyThemeState(boolean darkMode) {
        if (pageRoot != null) {
            pageRoot.getStyleClass().removeAll("sponsor-user-dark", "sponsor-user-light");
            pageRoot.getStyleClass().add(darkMode ? "sponsor-user-dark" : "sponsor-user-light");
            pageRoot.setStyle(darkMode ? DARK_PAGE_BACKGROUND : LIGHT_PAGE_BACKGROUND);
        }

        forceTransparentShell(pageScroll);
        forceTransparentShell(pageShellWrap);
        forceTransparentShell(pageShell);
        Platform.runLater(this::forceScrollPaneViewportTransparent);
    }

    private void forceTransparentShell(Node node) {
        if (node == null) {
            return;
        }
        String style = node.getStyle();
        String transparentStyle = "-fx-background-color: transparent; -fx-background: transparent;";
        if (style == null || style.isBlank()) {
            node.setStyle(transparentStyle);
            return;
        }
        if (!style.contains("-fx-background-color: transparent")) {
            node.setStyle(style + (style.trim().endsWith(";") ? " " : "; ") + transparentStyle);
        }
    }

    private void forceScrollPaneViewportTransparent() {
        if (pageScroll == null) {
            return;
        }
        forceTransparentShell(pageScroll.lookup(".viewport"));
        forceTransparentShell(pageScroll.lookup(".content"));
    }

    private void configureFilters() {
        teamFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        teamFilterComboBox.setValue(FILTER_ALL);

        paymentFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        paymentFilterComboBox.setValue(FILTER_ALL);

        sortComboBox.setItems(FXCollections.observableArrayList(SORT_RECENT, SORT_AMOUNT, SORT_NAME));
        sortComboBox.setValue(SORT_RECENT);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        teamFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        paymentFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void refreshData(String successMessage) {
        if (workspaceService == null) {
            return;
        }

        try {
            snapshot = workspaceService.loadSnapshot();
            loadFilterChoices();
            updateMetrics();
            applyFilters();
            showSuccessStatus(successMessage);
        } catch (SQLException e) {
            showErrorStatus("Refresh failed.");
            showAlert(Alert.AlertType.ERROR, "Sponsors", "Could not refresh sponsorship data.\n" + e.getMessage());
        }
    }

    private void loadFilterChoices() {
        String selectedTeam = teamFilterComboBox.getValue();
        String selectedPayment = paymentFilterComboBox.getValue();

        List<String> teams = snapshot.contrats().stream()
                .map(snapshot::equipeOf)
                .filter(Objects::nonNull)
                .map(Equipe::getNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> payments = snapshot.contrats().stream()
                .map(workspaceService::resolvePaymentStatus)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        teamFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        teamFilterComboBox.getItems().addAll(teams);
        paymentFilterComboBox.setItems(FXCollections.observableArrayList(FILTER_ALL));
        paymentFilterComboBox.getItems().addAll(payments);

        teamFilterComboBox.setValue(teams.contains(selectedTeam) ? selectedTeam : FILTER_ALL);
        paymentFilterComboBox.setValue(payments.contains(selectedPayment) ? selectedPayment : FILTER_ALL);
    }

    private void updateMetrics() {
        SponsoringWorkspaceService.SponsoringStats stats = snapshot.stats();
        totalSponsorsMetricLabel.setText(String.valueOf(stats.totalSponsors()));
        totalContractsMetricLabel.setText(String.valueOf(stats.totalContrats()));
        totalBudgetMetricLabel.setText(formatCompactCurrency(stats.totalBudget()));
        averageContractMetricLabel.setText(formatCompactCurrency(stats.averageContractAmount()));
    }

    private void applyFilters() {
        if (snapshot == null) {
            filteredSponsors = List.of();
            filteredContracts = List.of();
            sponsorCardsPane.getChildren().clear();
            contractCardsPane.getChildren().clear();
            updateSponsorPaginationControls();
            updateContractPaginationControls();
            return;
        }

        String query = normalize(searchField.getText());
        String selectedTeam = normalizeFilter(teamFilterComboBox.getValue());
        String selectedPayment = normalizeFilter(paymentFilterComboBox.getValue());
        String sortMode = sortComboBox.getValue();

        List<ContractViewModel> visibleContracts = snapshot.contrats().stream()
                .map(contrat -> new ContractViewModel(contrat, snapshot.sponsorOf(contrat), snapshot.equipeOf(contrat)))
                .filter(model -> matchesContract(model, query, selectedTeam, selectedPayment))
                .sorted(resolveContractComparator(sortMode))
                .toList();

        Set<Integer> contractSponsorIds = visibleContracts.stream()
                .map(model -> model.sponsor() == null ? null : model.sponsor().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Sponsor> visibleSponsors = snapshot.sponsors().stream()
                .filter(sponsor -> matchesSponsor(sponsor, query, contractSponsorIds, selectedTeam, selectedPayment))
                .sorted(Comparator.comparingDouble(Sponsor::getBudget).reversed()
                        .thenComparing(sponsor -> emptyIfNull(sponsor.getNom()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        filteredSponsors = visibleSponsors;
        filteredContracts = visibleContracts;
        sponsorPageIndex = 0;
        contractPageIndex = 0;
        updateSponsorPage();
        updateContractPage();

        sponsorCountLabel.setText(visibleSponsors.size() + " visible");
        contractCountLabel.setText(visibleContracts.size() + " visible");
        toolbarSummaryLabel.setText(snapshot.stats().totalSponsors() + " sponsors / " + snapshot.stats().totalContrats() + " contracts");
    }

    private void updateSponsorPage() {
        int totalPages = computeTotalPages(filteredSponsors.size(), USER_SPONSOR_PAGE_SIZE);
        sponsorPageIndex = clampPageIndex(sponsorPageIndex, totalPages);
        renderSponsorCards(slicePage(filteredSponsors, sponsorPageIndex, USER_SPONSOR_PAGE_SIZE));
        updateSponsorPaginationControls();
    }

    private void updateContractPage() {
        int totalPages = computeTotalPages(filteredContracts.size(), USER_CONTRACT_PAGE_SIZE);
        contractPageIndex = clampPageIndex(contractPageIndex, totalPages);
        renderContractCards(slicePage(filteredContracts, contractPageIndex, USER_CONTRACT_PAGE_SIZE));
        updateContractPaginationControls();
    }

    private void updateSponsorPaginationControls() {
        int totalPages = computeTotalPages(filteredSponsors.size(), USER_SPONSOR_PAGE_SIZE);
        if (sponsorPaginationLabel != null) {
            sponsorPaginationLabel.setText(buildPaginationLabel(sponsorPageIndex, totalPages, filteredSponsors.size()));
        }
        if (sponsorPreviousPageButton != null) {
            sponsorPreviousPageButton.setDisable(sponsorPageIndex <= 0);
        }
        if (sponsorNextPageButton != null) {
            sponsorNextPageButton.setDisable(sponsorPageIndex + 1 >= totalPages);
        }
    }

    private void updateContractPaginationControls() {
        int totalPages = computeTotalPages(filteredContracts.size(), USER_CONTRACT_PAGE_SIZE);
        if (contractPaginationLabel != null) {
            contractPaginationLabel.setText(buildPaginationLabel(contractPageIndex, totalPages, filteredContracts.size()));
        }
        if (contractPreviousPageButton != null) {
            contractPreviousPageButton.setDisable(contractPageIndex <= 0);
        }
        if (contractNextPageButton != null) {
            contractNextPageButton.setDisable(contractPageIndex + 1 >= totalPages);
        }
    }

    private Comparator<ContractViewModel> resolveContractComparator(String sortMode) {
        if (SORT_AMOUNT.equals(sortMode)) {
            return Comparator.comparingDouble((ContractViewModel model) -> model.contrat().getMontant()).reversed();
        }
        if (SORT_NAME.equals(sortMode)) {
            return Comparator.comparing(
                    (ContractViewModel model) -> emptyIfNull(model.sponsor() == null ? null : model.sponsor().getNom()),
                    String.CASE_INSENSITIVE_ORDER
            );
        }
        return Comparator.comparing((ContractViewModel model) -> model.contrat().getDateDebut(), Comparator.nullsLast(LocalDate::compareTo))
                .reversed();
    }

    private boolean matchesSponsor(
            Sponsor sponsor,
            String query,
            Set<Integer> contractSponsorIds,
            String selectedTeam,
            String selectedPayment
    ) {
        boolean queryMatches = query == null
                || contains(sponsor.getNom(), query)
                || contains(sponsor.getEmail(), query)
                || contains(sponsor.getTelephone(), query)
                || contains(sponsor.getAdresse(), query);

        boolean contractScoped = selectedTeam != null || selectedPayment != null;
        if (!contractScoped) {
            return queryMatches;
        }
        return queryMatches && sponsor.getId() != null && contractSponsorIds.contains(sponsor.getId());
    }

    private boolean matchesContract(ContractViewModel model, String query, String selectedTeam, String selectedPayment) {
        Sponsor sponsor = model.sponsor();
        Equipe equipe = model.equipe();

        boolean queryMatches = query == null
                || contains(model.contrat().getDescription(), query)
                || contains(model.contrat().getStatut(), query)
                || contains(model.contrat().getStatutPaiement(), query)
                || contains(sponsor == null ? null : sponsor.getNom(), query)
                || contains(sponsor == null ? null : sponsor.getAdresse(), query)
                || contains(equipe == null ? null : equipe.getNom(), query);

        boolean teamMatches = selectedTeam == null
                || Objects.equals(normalize(equipe == null ? null : equipe.getNom()), selectedTeam);

        boolean paymentMatches = selectedPayment == null
                || Objects.equals(normalize(workspaceService.resolvePaymentStatus(model.contrat())), selectedPayment);

        return queryMatches && teamMatches && paymentMatches;
    }

    private void renderSponsorCards(List<Sponsor> sponsors) {
        sponsorCardsPane.getChildren().clear();
        if (sponsors.isEmpty()) {
            sponsorCardsPane.getChildren().add(buildEmptyCard(
                    "No sponsors found",
                    "Try another search or remove the team/payment filters."
            ));
            return;
        }

        for (Sponsor sponsor : sponsors) {
            long contractCount = snapshot.contrats().stream()
                    .filter(contrat -> Objects.equals(contrat.getSponsorId(), sponsor.getId()))
                    .count();
            sponsorCardsPane.getChildren().add(buildSponsorCard(sponsor, contractCount));
        }
    }

    private void renderContractCards(List<ContractViewModel> contracts) {
        contractCardsPane.getChildren().clear();
        if (contracts.isEmpty()) {
            contractCardsPane.getChildren().add(buildEmptyCard(
                    "No contracts found",
                    "There are no sponsor contracts that match the current filters."
            ));
            return;
        }

        for (ContractViewModel contract : contracts) {
            contractCardsPane.getChildren().add(buildContractCard(contract));
        }
    }

    private VBox buildSponsorCard(Sponsor sponsor, long contractCount) {
        VBox card = new VBox(14);
        card.getStyleClass().add("sponsor-card");
        card.setPrefWidth(300);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Node logo = buildLogoNode(sponsor == null ? null : sponsor.getLogoName(), sponsor == null ? null : sponsor.getNom(), 58);

        VBox titleBox = new VBox(4);
        Label nameLabel = new Label(fallbackText(sponsor == null ? null : sponsor.getNom(), "Sponsor"));
        nameLabel.getStyleClass().add("sponsor-card-title");

        Label subtitleLabel = new Label("Updated " + formatUpdatedAt(sponsor == null ? null : sponsor.getUpdatedAt()));
        subtitleLabel.getStyleClass().add("sponsor-card-subtitle");
        titleBox.getChildren().addAll(nameLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label contractsChip = new Label(contractCount + " contract(s)");
        contractsChip.getStyleClass().addAll("sponsor-meta-chip", "status-muted");

        header.getChildren().addAll(logo, titleBox, spacer, contractsChip);

        Label budgetLabel = new Label(formatCurrency(sponsor == null ? 0.0 : sponsor.getBudget()));
        budgetLabel.getStyleClass().add("sponsor-card-value");

        VBox body = new VBox(8);
        body.getChildren().addAll(
                buildInfoLine("Email", fallbackText(sponsor == null ? null : sponsor.getEmail(), "-")),
                buildInfoLine("Phone", fallbackText(sponsor == null ? null : sponsor.getTelephone(), "-")),
                buildInfoLine("Address", fallbackText(sponsor == null ? null : sponsor.getAdresse(), "-"))
        );

        HBox actionRow = new HBox(10);
        Button mapButton = new Button("View map");
        mapButton.getStyleClass().add("primary-button");
        mapButton.setOnAction(event -> sponsorMapViewService.showMap(
                pageRoot == null || pageRoot.getScene() == null ? null : pageRoot.getScene().getWindow(),
                sponsor == null ? null : sponsor.getNom(),
                sponsor == null ? null : sponsor.getAdresse()
        ));
        actionRow.getChildren().add(mapButton);

        card.getChildren().addAll(header, budgetLabel, body, actionRow);
        return card;
    }

    private VBox buildContractCard(ContractViewModel model) {
        VBox card = new VBox(14);
        card.getStyleClass().add("sponsor-contract-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Node logo = buildLogoNode(
                model.sponsor() == null ? null : model.sponsor().getLogoName(),
                model.sponsor() == null ? null : model.sponsor().getNom(),
                52
        );

        VBox titleBox = new VBox(4);
        Label titleLabel = new Label(
                fallbackText(model.sponsor() == null ? null : model.sponsor().getNom(), "Sponsor")
                        + " x "
                        + fallbackText(model.equipe() == null ? null : model.equipe().getNom(), "Team")
        );
        titleLabel.getStyleClass().add("sponsor-contract-title");

        Label subtitleLabel = new Label(
                formatDate(model.contrat().getDateDebut())
                        + " to "
                        + formatDate(model.contrat().getDateFin())
        );
        subtitleLabel.getStyleClass().add("sponsor-contract-subtitle");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label amountLabel = new Label(formatCurrency(model.contrat().getMontant()));
        amountLabel.getStyleClass().add("sponsor-contract-amount");

        HBox chips = new HBox(8,
                buildStatusChip(workspaceService.resolveContractStatus(model.contrat()), resolveContractStatusStyle(model.contrat())),
                buildStatusChip(workspaceService.resolvePaymentStatus(model.contrat()), resolvePaymentStatusStyle(model.contrat()))
        );
        chips.setAlignment(Pos.CENTER_RIGHT);

        rightBox.getChildren().addAll(amountLabel, chips);
        header.getChildren().addAll(logo, titleBox, spacer, rightBox);

        FlowPane infoFlow = new FlowPane();
        infoFlow.setHgap(8);
        infoFlow.setVgap(8);
        infoFlow.getChildren().addAll(
                buildMetaChip("Sponsor email: " + fallbackText(model.sponsor() == null ? null : model.sponsor().getEmail(), "-")),
                buildMetaChip("Address: " + fallbackText(model.sponsor() == null ? null : model.sponsor().getAdresse(), "-"))
        );

        Label descriptionLabel = new Label(fallbackText(model.contrat().getDescription(), "No description provided."));
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("sponsor-contract-description");

        HBox qrSection = buildQrSection(model);
        HBox mapSection = buildContractMapSection(model);

        card.getChildren().addAll(header, infoFlow, descriptionLabel, qrSection, mapSection);
        return card;
    }

    private HBox buildContractMapSection(ContractViewModel model) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Button mapButton = new Button("View sponsor map");
        mapButton.getStyleClass().add("ghost-button");
        mapButton.setOnAction(event -> sponsorMapViewService.showMap(
                pageRoot == null || pageRoot.getScene() == null ? null : pageRoot.getScene().getWindow(),
                model.sponsor() == null ? null : model.sponsor().getNom(),
                model.sponsor() == null ? null : model.sponsor().getAdresse()
        ));

        row.getChildren().add(mapButton);
        return row;
    }

    private HBox buildQrSection(ContractViewModel model) {
        HBox container = new HBox(14);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("sponsor-qr-row");

        VBox textBox = new VBox(4);
        Label title = new Label("Contract QR");
        title.getStyleClass().add("sponsor-line-label");
        Label subtitle = new Label("Scan this code to retrieve the sponsor name.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("sponsor-contract-subtitle");
        textBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Node qrNode = buildQrNode(model);
        container.getChildren().addAll(textBox, spacer, qrNode);
        return container;
    }

    private Node buildQrNode(ContractViewModel model) {
        StackPane shell = new StackPane();
        shell.setPrefSize(132, 132);
        shell.setMinSize(132, 132);
        shell.getStyleClass().add("sponsor-qr-shell");

        try {
            BufferedImage qrImage = qrCodeService.generateQrImage(model.contrat(), model.sponsor(), model.equipe());
            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(qrImage, null));
            imageView.setFitWidth(112);
            imageView.setFitHeight(112);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.getStyleClass().add("sponsor-qr-image");
            shell.getChildren().add(imageView);
            return shell;
        } catch (Exception e) {
            Label fallback = new Label("QR");
            fallback.getStyleClass().add("sponsor-logo-fallback");
            shell.getChildren().add(fallback);
            return shell;
        }
    }

    private VBox buildEmptyCard(String title, String subtitle) {
        VBox emptyCard = new VBox(8);
        emptyCard.setAlignment(Pos.CENTER_LEFT);
        emptyCard.getStyleClass().add("sponsor-empty-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("sponsor-empty-text");

        emptyCard.getChildren().addAll(titleLabel, subtitleLabel);
        return emptyCard;
    }

    private Node buildLogoNode(String logoReference, String fallbackName, double imageSize) {
        StackPane shell = new StackPane();
        shell.setPrefSize(imageSize + 18, imageSize + 18);
        shell.setMinSize(imageSize + 18, imageSize + 18);
        shell.getStyleClass().add("sponsor-logo-shell");

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

    private HBox buildInfoLine(String label, String value) {
        HBox line = new HBox(8);
        line.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().add("sponsor-line-label");

        Label valueNode = new Label(value);
        valueNode.setWrapText(true);
        valueNode.getStyleClass().add("sponsor-line-value");

        line.getChildren().addAll(labelNode, valueNode);
        return line;
    }

    private Label buildMetaChip(String text) {
        Label chip = new Label(text);
        chip.getStyleClass().add("sponsor-meta-chip");
        return chip;
    }

    private Label buildStatusChip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().add(styleClass);
        return chip;
    }

    private String resolveContractStatusStyle(ContratSponsor contrat) {
        if (workspaceService.isExpired(contrat)) {
            return "status-error";
        }
        String status = normalize(contrat.getStatut());
        if (status == null || status.isBlank() || status.contains("active")) {
            return "status-success";
        }
        if (status.contains("pending") || status.contains("draft")) {
            return "status-warning";
        }
        return "status-muted";
    }

    private String resolvePaymentStatusStyle(ContratSponsor contrat) {
        String paymentStatus = normalize(workspaceService.resolvePaymentStatus(contrat));
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return "status-warning";
        }
        if (paymentStatus.contains("paid")) {
            return "status-success";
        }
        if (paymentStatus.contains("partial")) {
            return "status-warning";
        }
        return "status-error";
    }

    private String resolveInitial(String value) {
        String safe = fallbackText(value, "S").trim();
        return safe.isEmpty() ? "S" : safe.substring(0, 1).toUpperCase(Locale.ROOT);
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

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatUpdatedAt(LocalDateTime updatedAt) {
        return updatedAt == null ? "-" : UPDATE_FORMATTER.format(updatedAt);
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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

    private String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    private <T> List<T> slicePage(List<T> items, int pageIndex, int pageSize) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, pageIndex * pageSize);
        if (fromIndex >= items.size()) {
            return List.of();
        }
        int toIndex = Math.min(items.size(), fromIndex + pageSize);
        return items.subList(fromIndex, toIndex);
    }

    private int computeTotalPages(int totalItems, int pageSize) {
        return Math.max(1, (int) Math.ceil((double) Math.max(0, totalItems) / Math.max(1, pageSize)));
    }

    private int clampPageIndex(int pageIndex, int totalPages) {
        return Math.max(0, Math.min(pageIndex, Math.max(0, totalPages - 1)));
    }

    private String buildPaginationLabel(int pageIndex, int totalPages, int totalItems) {
        if (totalItems <= 0) {
            return "Page 0/0 | 0 item";
        }
        return "Page " + (pageIndex + 1) + "/" + totalPages + " | " + totalItems + " items";
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
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ContractViewModel(ContratSponsor contrat, Sponsor sponsor, Equipe equipe) {
    }
}
