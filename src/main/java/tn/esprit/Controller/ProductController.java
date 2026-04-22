package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Product;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.ProductImageResolver;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.repositories.ProductRepository;
import tn.esprit.services.ProductPdfExportService;
import tn.esprit.services.ProductService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ProductController {
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("product-db"));

    @FXML private BorderPane pageRoot;
    @FXML private ScrollPane pageScroll;
    @FXML private HBox navbarRoot;
    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private Button equipesNavButton;
    @FXML private Button matchsNavButton;
    @FXML private Button annonceNavButton;
    @FXML private HBox sidebarModuleChildrenBox;
    @FXML private Button leaguesNavButton;
    @FXML private Button joueursNavButton;
    @FXML private Button productNavButton;
    @FXML private ToggleButton themeToggleButton;

    @FXML private Label resultCountLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label statusLabel;
    @FXML private Label visibleProductsMetricLabel;
    @FXML private Label lowStockMetricLabel;
    @FXML private Label outOfStockMetricLabel;
    @FXML private Label stockChartSummaryLabel;
    @FXML private Label categoryChartSummaryLabel;
    @FXML private BarChart<String, Number> stockStatusChart;
    @FXML private PieChart categoryDistributionChart;

    @FXML private TextField searchField;
    @FXML private ComboBox<ProductRepository.ProductSortField> sortByComboBox;
    @FXML private ComboBox<ProductRepository.SortDirection> sortDirectionComboBox;

    @FXML private TableView<Product> productTableView;
    @FXML private TableColumn<Product, Product> productImageColumn;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> productCategoryColumn;
    @FXML private TableColumn<Product, String> productPriceColumn;
    @FXML private TableColumn<Product, Product> productStockColumn;
    @FXML private TableColumn<Product, String> productBrandColumn;
    @FXML private TableColumn<Product, Product> productActionsColumn;

    @FXML private Label detailBadgeLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label detailPriceLabel;
    @FXML private Label detailStockChipLabel;
    @FXML private Label detailIdValueLabel;
    @FXML private Label detailCategoryValueLabel;
    @FXML private Label detailBrandValueLabel;
    @FXML private Label detailSizeValueLabel;
    @FXML private Label detailImagePathLabel;
    @FXML private ImageView detailImageView;
    @FXML private Label detailImagePlaceholderLabel;

    @FXML private Label formModeLabel;
    @FXML private Label formHintLabel;
    @FXML private Label validationLabel;
    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField brandField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField sizeField;
    @FXML private TextField imageField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button detailEditButton;
    @FXML private Button detailDeleteButton;
    @FXML private Button exportPdfButton;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final Map<String, Image> imageCache = new HashMap<>();

    private ProductService productService;
    private ProductPdfExportService productPdfExportService;
    private Product selectedProduct;
    private boolean serviceReady;
    private boolean darkMode;
    private SidebarModuleGroup sidebarModuleGroup;
    private final AtomicLong refreshSequence = new AtomicLong();

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        configureNavigation();
        configureSortControls();
        configureFormatters();
        configureTable();
        configureCharts();
        updateDetailPanel();
        updateActionAvailability();
        if (pageScroll != null) {
            pageScroll.skinProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::applyWorkspaceSurface));
            pageScroll.sceneProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::applyWorkspaceSurface));
        }
        Platform.runLater(this::applyWorkspaceSurface);

        try {
            productService = new ProductService();
            productPdfExportService = new ProductPdfExportService();
            serviceReady = true;
            setStatus("Module produits pret.", "status-success");
            refreshProducts(null, "Chargement des produits...", "status-muted");
        } catch (SQLException exception) {
            serviceReady = false;
            setStatus("Module produits indisponible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Produit", resolveSqlMessage(exception));
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        Platform.runLater(this::applyWorkspaceSurface);
        if (productTableView != null) {
            productTableView.refresh();
        }
        if (stockStatusChart != null) {
            stockStatusChart.applyCss();
        }
        if (categoryDistributionChart != null) {
            categoryDistributionChart.applyCss();
        }
        updateCharts();
        Platform.runLater(() -> applyPieChartTheme(categoryDistributionChart, darkMode));
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(resolveNavigationSource(sidebarBrandBox, navbarRoot),
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(resolveNavigationSource(adminNavButton, navbarRoot));
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesNavButton, navbarRoot),
                "/tn/esprit/views/equipe-competitions-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs(ActionEvent event) {
        if (event != null
                && event.getSource() == matchsNavButton
                && sidebarModuleGroup != null
                && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(resolveNavigationSource(matchsNavButton, navbarRoot),
                "/tn/esprit/views/match-competitions-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(resolveNavigationSource(leaguesNavButton, navbarRoot),
                "/tn/esprit/views/league-competitions-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursNavButton, navbarRoot),
                "/tn/esprit/views/joueur-crud-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(resolveNavigationSource(annonceNavButton, navbarRoot),
                "/tn/esprit/views/annonce-user-view.fxml",
                "/tn/esprit/styles/annonce-theme.css",
                "Annonce | Sport Insight");
    }

    @FXML
    private void handleOpenProducts() {
        refreshProducts(getSelectedProductId(), null, null);
    }

    @FXML
    private void handleNewProduct() {
        selectedProduct = null;
        productTableView.getSelectionModel().clearSelection();
        clearFormFields();
        clearValidation();
        updateDetailPanel();
        updateActionAvailability();
        formModeLabel.setText("Ajouter un produit");
        formHintLabel.setText("Renseignez les champs ci-dessous pour ajouter un nouveau produit.");
        selectionStateLabel.setText("Selectionnez un produit");
    }

    @FXML
    private void handleApplyFilters() {
        refreshProducts(getSelectedProductId(), "Filtres appliques.", "status-muted");
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (sortByComboBox != null) {
            sortByComboBox.setValue(ProductRepository.ProductSortField.NAME);
        }
        if (sortDirectionComboBox != null) {
            sortDirectionComboBox.setValue(ProductRepository.SortDirection.ASC);
        }
        refreshProducts(getSelectedProductId(), "Filtres reinitialises.", "status-muted");
    }

    @FXML
    private void handleExportPdf() {
        if (!serviceReady || productPdfExportService == null) {
            showValidation("Le service d'export PDF n'est pas disponible.");
            return;
        }

        List<Product> productsToExport = new ArrayList<>(products);
        if (productsToExport.isEmpty()) {
            showValidation("Il n'y a aucun produit a exporter.");
            return;
        }

        Path target = choosePdfTarget();
        if (target == null) {
            setStatus("Export PDF annule.", "status-muted");
            return;
        }

        try {
            productPdfExportService.export(target, productsToExport);
            setStatus("Liste des produits exportee en PDF.", "status-success");
        } catch (IOException exception) {
            setStatus("Export PDF impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Produit", exception.getMessage());
        }
    }

    @FXML
    private void handleAddProduct() {
        Product product = buildProductFromForm(false);
        if (product == null || productService == null) {
            return;
        }

        try {
            productService.add(product);
            refreshProducts(product.getId(), "Produit ajoute avec succes.", "status-success");
            clearValidation();
        } catch (IllegalArgumentException | SQLException exception) {
            setStatus("Ajout impossible.", "status-error");
            showValidation(resolvePersistenceMessage(exception));
        }
    }

    @FXML
    private void handleUpdateProduct() {
        if (selectedProduct == null) {
            showValidation("Selectionnez un produit a modifier.");
            return;
        }

        Product product = buildProductFromForm(true);
        if (product == null || productService == null) {
            return;
        }

        try {
            productService.update(product);
            refreshProducts(product.getId(), "Produit modifie avec succes.", "status-success");
            clearValidation();
        } catch (IllegalArgumentException | SQLException exception) {
            setStatus("Modification impossible.", "status-error");
            showValidation(resolvePersistenceMessage(exception));
        }
    }

    @FXML
    private void handleDeleteSelectedProduct() {
        if (selectedProduct == null) {
            showValidation("Selectionnez un produit a supprimer.");
            return;
        }
        confirmAndDelete(selectedProduct);
    }

    @FXML
    private void handleClearForm() {
        handleNewProduct();
    }

    @FXML
    private void handleEditSelectedProduct() {
        if (selectedProduct == null) {
            showValidation("Selectionnez un produit a modifier.");
            return;
        }
        populateForm(selectedProduct);
    }

    private void configureNavigation() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
        if (productNavButton != null && !productNavButton.getStyleClass().contains("navbar-nav-button-active")) {
            productNavButton.getStyleClass().add("navbar-nav-button-active");
        }
    }

    private void configureSortControls() {
        sortByComboBox.setItems(FXCollections.observableArrayList(ProductRepository.ProductSortField.values()));
        sortDirectionComboBox.setItems(FXCollections.observableArrayList(ProductRepository.SortDirection.values()));
        sortByComboBox.setValue(ProductRepository.ProductSortField.NAME);
        sortDirectionComboBox.setValue(ProductRepository.SortDirection.ASC);
    }

    private void configureFormatters() {
        priceField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}([\\.,]\\d{0,2})?") ? change : null));
        stockField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}") ? change : null));
    }

    private void configureTable() {
        productTableView.setItems(products);
        productImageColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        productImageColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : createThumbnailNode(item.getImage(), item.getName(), 56));
            }
        });

        productNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getName())));
        productCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getCategory())));
        productPriceColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getPrice())));
        productStockColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        productStockColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(createStockChip(item.getStock()));
            }
        });

        productBrandColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getBrand())));
        productActionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        productActionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Button viewButton = createTableActionButton("Voir", "soft-button");
                viewButton.setOnAction(event -> viewProduct(item));

                Button editButton = createTableActionButton("Modifier", "ghost-button");
                editButton.setOnAction(event -> editProduct(item));

                Button deleteItemButton = createTableActionButton("Supprimer", "danger-button");
                deleteItemButton.setOnAction(event -> confirmAndDelete(item));

                HBox box = new HBox(8, viewButton, editButton, deleteItemButton);
                box.getStyleClass().add("product-table-actions");
                setGraphic(box);
            }
        });

        productTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> selectProduct(newValue));
    }

    private void configureCharts() {
        if (stockStatusChart != null) {
            stockStatusChart.setLegendVisible(false);
            stockStatusChart.setAnimated(false);
        }
        if (categoryDistributionChart != null) {
            categoryDistributionChart.setLegendVisible(true);
            categoryDistributionChart.setLabelsVisible(true);
            categoryDistributionChart.setAnimated(false);
            categoryDistributionChart.setClockwise(true);
            Platform.runLater(() -> applyPieChartTheme(categoryDistributionChart, isDarkModeEnabled()));
        }
    }

    private void refreshProducts(Integer selectedId, String successMessage, String statusStyle) {
        if (!serviceReady || productService == null) {
            return;
        }

        long requestId = refreshSequence.incrementAndGet();
        String search = searchField == null ? null : searchField.getText();
        ProductRepository.ProductSortField sortField =
                sortByComboBox == null || sortByComboBox.getValue() == null
                        ? ProductRepository.ProductSortField.NAME
                        : sortByComboBox.getValue();
        ProductRepository.SortDirection sortDirection =
                sortDirectionComboBox == null || sortDirectionComboBox.getValue() == null
                        ? ProductRepository.SortDirection.ASC
                        : sortDirectionComboBox.getValue();

        setStatus(successMessage == null ? "Chargement des produits..." : successMessage,
                statusStyle == null ? "status-muted" : statusStyle);

        Task<List<Product>> loadTask = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productService.findProducts(search, sortField, sortDirection);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }

            List<Product> foundProducts = loadTask.getValue();
            products.setAll(foundProducts);
            updateMetrics();
            updateCharts();

            if (selectedId != null) {
                selectProductById(selectedId);
            } else if (selectedProduct != null && selectedProduct.getId() != null) {
                selectProductById(selectedProduct.getId());
            } else {
                updateDetailPanel();
                updateActionAvailability();
            }

            if (successMessage != null) {
                setStatus(successMessage, statusStyle);
            } else if (products.isEmpty()) {
                setStatus("Aucun produit trouve.", "status-muted");
            } else {
                setStatus(products.size() + " produit(s) charges.", "status-muted");
            }
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            setStatus("Chargement impossible.", "status-error");
            Throwable exception = loadTask.getException();
            if (exception instanceof SQLException sqlException) {
                showAlert(Alert.AlertType.ERROR, "Produit", resolveSqlMessage(sqlException));
            } else if (exception != null) {
                showAlert(Alert.AlertType.ERROR, "Produit", resolvePersistenceMessage(new Exception(exception)));
            }
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private void populateForm(Product product) {
        if (product == null) {
            return;
        }
        selectedProduct = product;
        nameField.setText(emptyIfNull(product.getName(), ""));
        categoryField.setText(emptyIfNull(product.getCategory(), ""));
        brandField.setText(emptyIfNull(product.getBrand(), ""));
        priceField.setText(product.getPrice() == null ? "" : product.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
        stockField.setText(String.valueOf(product.getStock()));
        sizeField.setText(emptyIfNull(product.getSize(), ""));
        imageField.setText(emptyIfNull(product.getImage(), ""));
        formModeLabel.setText("Modifier le produit");
        formHintLabel.setText("Les modifications seront appliquees au produit selectionne.");
        clearValidation();
        updateActionAvailability();
    }

    private Product buildProductFromForm(boolean updateMode) {
        clearValidation();

        BigDecimal price;
        Integer stock;
        try {
            price = parsePrice();
            stock = parseStock();
        } catch (IllegalArgumentException exception) {
            showValidation(exception.getMessage());
            return null;
        }

        Product product = new Product(
                updateMode && selectedProduct != null ? selectedProduct.getId() : null,
                trimToNull(nameField.getText()),
                trimToNull(categoryField.getText()),
                price,
                stock,
                trimToNull(sizeField.getText()),
                trimToNull(brandField.getText()),
                trimToNull(imageField.getText())
        );

        Map<String, String> errors = productService.validate(product);
        if (!errors.isEmpty()) {
            applyValidationErrors(errors);
            return null;
        }

        return product;
    }

    private void confirmAndDelete(Product product) {
        if (product == null || productService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer \"" + emptyIfNull(product.getName(), "ce produit") + "\" ?");
        alert.setContentText("Cette action est definitive.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            productService.delete(product.getId());
            selectedProduct = null;
            clearFormFields();
            refreshProducts(null, "Produit supprime avec succes.", "status-success");
            updateDetailPanel();
            updateActionAvailability();
        } catch (IllegalArgumentException | SQLException exception) {
            setStatus("Suppression impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Suppression impossible", resolveDeleteMessage(exception));
        }
    }

    private void viewProduct(Product product) {
        selectProduct(product);
    }

    private void editProduct(Product product) {
        selectProduct(product);
        populateForm(product);
    }

    private void selectProduct(Product product) {
        selectedProduct = product;
        updateDetailPanel();
        updateActionAvailability();
        selectionStateLabel.setText(product == null
                ? "Selectionnez un produit"
                : emptyIfNull(product.getName(), "Produit selectionne"));
    }

    private void selectProductById(Integer productId) {
        if (productId == null) {
            return;
        }
        for (Product product : products) {
            if (productId.equals(product.getId())) {
                productTableView.getSelectionModel().select(product);
                productTableView.scrollTo(product);
                selectProduct(product);
                return;
            }
        }
        productTableView.getSelectionModel().clearSelection();
        selectedProduct = null;
        updateDetailPanel();
        updateActionAvailability();
    }

    private void updateMetrics() {
        long lowStock = products.stream().filter(product -> product.getStock() > 0 && product.getStock() <= LOW_STOCK_THRESHOLD).count();
        long outOfStock = products.stream().filter(product -> product.getStock() <= 0).count();

        resultCountLabel.setText(products.size() + " produit(s)");
        visibleProductsMetricLabel.setText(String.valueOf(products.size()));
        lowStockMetricLabel.setText(String.valueOf(lowStock));
        outOfStockMetricLabel.setText(String.valueOf(outOfStock));
    }

    private void updateCharts() {
        long healthyStock = products.stream().filter(product -> product.getStock() > LOW_STOCK_THRESHOLD).count();
        long lowStock = products.stream().filter(product -> product.getStock() > 0 && product.getStock() <= LOW_STOCK_THRESHOLD).count();
        long outOfStock = products.stream().filter(product -> product.getStock() <= 0).count();

        if (stockStatusChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            XYChart.Data<String, Number> healthyData = new XYChart.Data<>("Healthy", healthyStock);
            XYChart.Data<String, Number> lowData = new XYChart.Data<>("Low", lowStock);
            XYChart.Data<String, Number> outData = new XYChart.Data<>("Out", outOfStock);
            series.getData().addAll(healthyData, lowData, outData);
            stockStatusChart.getData().setAll(series);
            if (isDarkModeEnabled()) {
                applyBarColor(healthyData, "#9d71ff");
                applyBarColor(lowData, "#57d5ff");
                applyBarColor(outData, "#ff63d0");
            } else {
                applyBarColor(healthyData, "#22c55e");
                applyBarColor(lowData, "#f59e0b");
                applyBarColor(outData, "#ef4444");
            }
        }

        Map<String, Long> categories = products.stream()
                .collect(Collectors.groupingBy(
                        product -> emptyIfNull(product.getCategory(), "Uncategorized"),
                        Collectors.counting()
                ));

        if (categoryDistributionChart != null) {
            ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
            categories.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                    .limit(5)
                    .forEach(entry -> chartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue())));
            if (chartData.isEmpty()) {
                chartData.add(new PieChart.Data("No products", 1));
            }
            categoryDistributionChart.setData(chartData);
            Platform.runLater(() -> applyPieChartTheme(categoryDistributionChart, isDarkModeEnabled()));
        }

        if (stockChartSummaryLabel != null) {
            stockChartSummaryLabel.setText(products.size() + " visible product(s) | " + healthyStock + " healthy | " + lowStock + " low | " + outOfStock + " out");
        }
        if (categoryChartSummaryLabel != null) {
            String topCategory = categories.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                    .map(entry -> entry.getKey() + " leads with " + entry.getValue())
                    .findFirst()
                    .orElse("No category data available.");
            categoryChartSummaryLabel.setText(topCategory);
        }
    }

    private void updateDetailPanel() {
        if (selectedProduct == null) {
            detailBadgeLabel.setText("Apercu");
            detailTitleLabel.setText("Aucun produit selectionne");
            detailSubtitleLabel.setText("Selectionnez un produit");
            detailPriceLabel.setText("0.00 DT");
            detailIdValueLabel.setText("Auto");
            detailCategoryValueLabel.setText("-");
            detailBrandValueLabel.setText("-");
            detailSizeValueLabel.setText("-");
            detailImagePathLabel.setText("Aucune image");
            detailStockChipLabel.setText("-");
            detailStockChipLabel.getStyleClass().setAll("status-pill", "product-stock-chip", "product-stock-good");
            detailImageView.setImage(null);
            detailImageView.setVisible(false);
            detailImageView.setManaged(false);
            detailImagePlaceholderLabel.setVisible(true);
            detailImagePlaceholderLabel.setManaged(true);
            return;
        }

        detailBadgeLabel.setText(selectedProduct.getStock() > 0 ? "Disponible" : "Rupture");
        detailTitleLabel.setText(emptyIfNull(selectedProduct.getName(), "Produit"));
        detailSubtitleLabel.setText(buildDetailSubtitle(
                selectedProduct.getCategory(),
                selectedProduct.getBrand(),
                selectedProduct.getSize(),
                selectedProduct.getStock() > 0
        ));
        detailPriceLabel.setText(formatPrice(selectedProduct.getPrice()));
        detailIdValueLabel.setText(String.valueOf(selectedProduct.getId()));
        detailCategoryValueLabel.setText(emptyIfNull(selectedProduct.getCategory()));
        detailBrandValueLabel.setText(emptyIfNull(selectedProduct.getBrand()));
        detailSizeValueLabel.setText(emptyIfNull(selectedProduct.getSize()));
        detailImagePathLabel.setText(emptyIfNull(selectedProduct.getImage(), "Aucune image"));
        detailStockChipLabel.setText(selectedProduct.getStock() + " en stock");
        applyStockStyle(detailStockChipLabel, selectedProduct.getStock());
        updateDetailImage(selectedProduct.getImage(), selectedProduct.getName());
    }

    private String buildDetailSubtitle(String category, String brand, String size, boolean available) {
        StringBuilder builder = new StringBuilder();
        appendDetailPart(builder, category);
        appendDetailPart(builder, brand);
        appendDetailPart(builder, size);
        appendDetailPart(builder, available ? "Disponible" : "Rupture");
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private void appendDetailPart(StringBuilder builder, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" • ");
        }
        builder.append(normalized);
    }

    private void updateDetailImage(String path, String name) {
        Image image = loadImage(path);
        if (image == null) {
            detailImageView.setImage(null);
            detailImageView.setVisible(false);
            detailImageView.setManaged(false);
            detailImagePlaceholderLabel.setText(buildPlaceholderLabel(name));
            detailImagePlaceholderLabel.setVisible(true);
            detailImagePlaceholderLabel.setManaged(true);
            return;
        }

        detailImageView.setImage(image);
        detailImageView.setVisible(true);
        detailImageView.setManaged(true);
        detailImagePlaceholderLabel.setVisible(false);
        detailImagePlaceholderLabel.setManaged(false);
    }

    private StackPane createThumbnailNode(String imagePath, String productName, double size) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("product-thumbnail-shell");
        shell.setMinSize(size, size);
        shell.setPrefSize(size, size);
        shell.setMaxSize(size, size);

        Image image = loadImage(imagePath);
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(size - 10);
            view.setFitHeight(size - 10);
            view.setPreserveRatio(true);
            shell.getChildren().add(view);
        } else {
            Label placeholder = new Label(buildPlaceholderLabel(productName));
            placeholder.getStyleClass().add("product-thumbnail-placeholder");
            shell.getChildren().add(placeholder);
        }
        return shell;
    }

    private Label createStockChip(int stock) {
        Label chip = new Label(stock <= 0 ? "Rupture" : stock + " stock");
        applyStockStyle(chip, stock);
        return chip;
    }

    private void applyStockStyle(Label label, int stock) {
        label.getStyleClass().setAll("status-pill", "product-stock-chip");
        if (stock <= 0) {
            label.getStyleClass().add("product-stock-out");
        } else if (stock <= LOW_STOCK_THRESHOLD) {
            label.getStyleClass().add("product-stock-low");
        } else {
            label.getStyleClass().add("product-stock-good");
        }
    }

    private Button createTableActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClass, "product-table-action");
        return button;
    }

    private Image loadImage(String path) {
        String normalized = trimToNull(path);
        String cacheKey = normalized == null ? "__default__" : normalized;
        return imageCache.computeIfAbsent(cacheKey, ignored -> resolveImage(normalized));
    }

    private Image resolveImage(String path) {
        return ProductImageResolver.loadImage(getClass(), path);
    }

    private void applyValidationErrors(Map<String, String> errors) {
        if (errors.isEmpty()) {
            return;
        }

        clearFieldError(nameField);
        clearFieldError(categoryField);
        clearFieldError(brandField);
        clearFieldError(priceField);
        clearFieldError(stockField);
        clearFieldError(sizeField);
        clearFieldError(imageField);

        if (errors.containsKey("name")) {
            markFieldInvalid(nameField);
        }
        if (errors.containsKey("category")) {
            markFieldInvalid(categoryField);
        }
        if (errors.containsKey("brand")) {
            markFieldInvalid(brandField);
        }
        if (errors.containsKey("price")) {
            markFieldInvalid(priceField);
        }
        if (errors.containsKey("stock")) {
            markFieldInvalid(stockField);
        }
        if (errors.containsKey("size")) {
            markFieldInvalid(sizeField);
        }
        if (errors.containsKey("image")) {
            markFieldInvalid(imageField);
        }

        showValidation(errors.values().stream().collect(Collectors.joining("\n")));
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedProduct != null;
        updateButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        detailEditButton.setDisable(!hasSelection);
        detailDeleteButton.setDisable(!hasSelection);
    }

    private void clearFormFields() {
        nameField.clear();
        categoryField.clear();
        brandField.clear();
        priceField.clear();
        stockField.clear();
        sizeField.clear();
        imageField.clear();
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
        setStatus("Controle de saisie requis.", "status-error");
    }

    private void clearValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
        clearFieldError(nameField);
        clearFieldError(categoryField);
        clearFieldError(brandField);
        clearFieldError(priceField);
        clearFieldError(stockField);
        clearFieldError(sizeField);
        clearFieldError(imageField);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private BigDecimal parsePrice() {
        String value = trimToNull(priceField.getText());
        if (value == null) {
            throw new IllegalArgumentException("Le prix est obligatoire.");
        }
        try {
            return new BigDecimal(value.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Saisissez un prix valide.");
        }
    }

    private Integer parseStock() {
        String value = trimToNull(stockField.getText());
        if (value == null) {
            throw new IllegalArgumentException("Le stock est obligatoire.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Saisissez un stock valide.");
        }
    }

    private Integer getSelectedProductId() {
        return selectedProduct == null ? null : selectedProduct.getId();
    }

    private Path choosePdfTarget() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les produits en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("products-export-" + java.time.LocalDate.now() + ".pdf");
        Window owner = exportPdfButton == null || exportPdfButton.getScene() == null
                ? (pageRoot == null || pageRoot.getScene() == null ? null : pageRoot.getScene().getWindow())
                : exportPdfButton.getScene().getWindow();
        java.io.File targetFile = chooser.showSaveDialog(owner);
        return targetFile == null ? null : targetFile.toPath();
    }

    private String resolveDeleteMessage(Exception exception) {
        String message = resolvePersistenceMessage(exception);
        if (message != null && message.toLowerCase().contains("commande")) {
            return "Ce produit est deja utilise dans une commande et ne peut pas etre supprime.";
        }
        return message;
    }

    private String resolveSqlMessage(SQLException exception) {
        String message = exception == null ? null : trimToNull(exception.getMessage());
        return message == null ? "Une erreur base de donnees est survenue." : message;
    }

    private String resolvePersistenceMessage(Exception exception) {
        if (exception instanceof SQLException sqlException) {
            return resolveSqlMessage(sqlException);
        }
        String message = exception == null ? null : trimToNull(exception.getMessage());
        return message == null ? "Une erreur est survenue." : message;
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(emptyIfNull(message, "Pret"));
        statusLabel.getStyleClass().setAll("status-pill", styleClass == null ? "status-muted" : styleClass);
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safeValue.toPlainString() + " DT";
    }

    private String buildPlaceholderLabel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "IMG";
        }
        return normalized.substring(0, Math.min(3, normalized.length())).toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String emptyIfNull(String value) {
        return emptyIfNull(value, "-");
    }

    private String emptyIfNull(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private void applyWorkspaceSurface() {
        if (pageScroll == null) {
            return;
        }
        forceTransparent(pageScroll);
        forceTransparent(pageScroll.lookup(".viewport"));
        forceTransparent(pageScroll.lookup(".content"));
    }

    private void forceTransparent(Node node) {
        if (node == null) {
            return;
        }
        node.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyBarColor(XYChart.Data<String, Number> data, String color) {
        if (data == null) {
            return;
        }
        Runnable styler = () -> {
            Node node = data.getNode();
            if (node != null) {
                node.setStyle("-fx-bar-fill: " + color + ";");
            }
        };
        if (data.getNode() != null) {
            styler.run();
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> styler.run());
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

    private Node resolveNavigationSource(Node preferred, Node fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
