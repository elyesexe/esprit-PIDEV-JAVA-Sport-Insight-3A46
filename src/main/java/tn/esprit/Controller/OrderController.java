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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.services.CurrencyConversionService;
import tn.esprit.security.AuthSession;
import tn.esprit.services.OrderIntelligenceService;
import tn.esprit.services.OrderNotificationService;
import tn.esprit.services.OrderPdfExportService;
import tn.esprit.services.OrderService;
import tn.esprit.services.OrderWorkflowNotificationService;
import tn.esprit.services.ProductService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class OrderController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ExecutorService DB_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("order-db"));
    private static final ExecutorService IO_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("order-io"));

    @FXML private BorderPane pageRoot;
    @FXML private ScrollPane pageScroll;
    @FXML private Label resultCountLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label statusLabel;
    @FXML private Label visibleOrdersMetricLabel;
    @FXML private Label confirmedOrdersMetricLabel;
    @FXML private Label pendingPaymentsMetricLabel;
    @FXML private Label anomalyOrdersMetricLabel;
    @FXML private Label statusChartSummaryLabel;
    @FXML private Label paymentChartSummaryLabel;
    @FXML private BarChart<String, Number> statusChart;
    @FXML private PieChart paymentChart;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortByComboBox;
    @FXML private ComboBox<String> sortDirectionComboBox;
    @FXML private TableView<Order> orderTableView;
    @FXML private TableColumn<Order, String> orderIdColumn;
    @FXML private TableColumn<Order, String> orderProductColumn;
    @FXML private TableColumn<Order, String> orderCoachColumn;
    @FXML private TableColumn<Order, String> orderQuantityColumn;
    @FXML private TableColumn<Order, String> orderTotalColumn;
    @FXML private TableColumn<Order, Order> orderStatusColumn;
    @FXML private TableColumn<Order, Order> orderPaymentColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;
    @FXML private TableColumn<Order, Order> orderActionsColumn;
    @FXML private Label detailBadgeLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label detailTotalLabel;
    @FXML private Label detailPaymentChipLabel;
    @FXML private Label detailAnomalyChipLabel;
    @FXML private Label detailIdValueLabel;
    @FXML private Label detailProductValueLabel;
    @FXML private Label detailCoachValueLabel;
    @FXML private Label detailDateValueLabel;
    @FXML private Label detailQuantityValueLabel;
    @FXML private Label detailEmailValueLabel;
    @FXML private Label detailPhoneValueLabel;
    @FXML private Label detailShippingValueLabel;
    @FXML private Label detailBillingValueLabel;
    @FXML private Label detailLiveFxValueLabel;
    @FXML private Label detailAnomalySummaryLabel;
    @FXML private Label detailRecommendationValueLabel;
    @FXML private VBox emptyStateCard;
    @FXML private VBox detailCard;
    @FXML private VBox formCard;
    @FXML private Label formModeLabel;
    @FXML private Label formHintLabel;
    @FXML private Label validationLabel;
    @FXML private ComboBox<ChoiceItem> productComboBox;
    @FXML private TextField clientNameField;
    @FXML private DatePicker orderDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private ComboBox<String> paymentStatusComboBox;
    @FXML private TextField quantityField;
    @FXML private TextField sizeField;
    @FXML private TextField contactEmailField;
    @FXML private TextField contactPhoneField;
    @FXML private TextField totalAmountField;
    @FXML private TextField shippingAddressField;
    @FXML private TextField billingAddressField;
    @FXML private Button createOrderButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button cancelFormButton;
    @FXML private Button detailEditButton;
    @FXML private Button detailDeleteButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportInvoiceButton;
    @FXML private Button sendNotificationButton;

    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ObservableList<ChoiceItem> productChoices = FXCollections.observableArrayList();

    private OrderService orderService;
    private ProductService productService;
    private OrderPdfExportService orderPdfExportService;
    private OrderIntelligenceService orderIntelligenceService;
    private OrderNotificationService orderNotificationService;
    private CurrencyConversionService currencyConversionService;
    private OrderWorkflowNotificationService workflowNotificationService;
    private Order selectedOrder;
    private PanelMode panelMode = PanelMode.EMPTY;
    private boolean serviceReady;
    private boolean darkMode;
    private final AtomicLong refreshSequence = new AtomicLong();
    private final AtomicLong liveFxSequence = new AtomicLong();
    private List<Product> productSnapshot = List.of();
    private OrderIntelligenceService.OrderAnomalyAssessment currentAnomalyAssessment =
            OrderIntelligenceService.OrderAnomalyAssessment.empty();

    @FXML
    public void initialize() {
        configureControls();
        configureTable();
        configureCharts();
        updateDetailPanel();
        setPanelMode(PanelMode.EMPTY);
        Platform.runLater(this::applyWorkspaceSurface);

        try {
            orderService = new OrderService();
            productService = new ProductService();
            orderPdfExportService = new OrderPdfExportService();
            orderIntelligenceService = new OrderIntelligenceService();
            orderNotificationService = new OrderNotificationService();
            currencyConversionService = new CurrencyConversionService();
            workflowNotificationService = new OrderWorkflowNotificationService();
            serviceReady = true;
            refreshOrders(null, "Chargement des commandes...", "status-muted");
        } catch (SQLException | IllegalStateException exception) {
            serviceReady = false;
            setStatus("Module commandes indisponible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Commandes", resolveMessage(exception));
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (orderTableView != null) {
            orderTableView.refresh();
        }
        if (statusChart != null) {
            statusChart.applyCss();
        }
        updateCharts();
        Platform.runLater(() -> applyPieChartTheme(paymentChart));
    }

    @FXML
    private void handleNewOrder() {
        selectedOrder = null;
        orderTableView.getSelectionModel().clearSelection();
        clearFormFields();
        clearValidation();
        updateDetailPanel();
        selectionStateLabel.setText("Creation d'une commande");
        setPanelMode(PanelMode.CREATE);
    }

    @FXML
    private void handleApplyFilters() {
        refreshOrders(getSelectedOrderId(), "Filtres appliques.", "status-muted");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortByComboBox.setValue("Date");
        sortDirectionComboBox.setValue("Desc");
        refreshOrders(getSelectedOrderId(), "Filtres reinitialises.", "status-muted");
    }

    @FXML
    private void handleAddOrder() {
        Order order = buildOrderFromForm(false);
        if (order == null) {
            return;
        }
        try {
            orderService.add(order);
            triggerOrderNotification(order, false);
            notifyOrderCreated(order);
            refreshOrders(order.getId(), "Commande ajoutee avec succes.", "status-success");
            clearValidation();
        } catch (SQLException | IllegalArgumentException exception) {
            showValidation(resolveMessage(exception));
        }
    }

    @FXML
    private void handleUpdateOrder() {
        if (selectedOrder == null) {
            showValidation("Selectionnez une commande a modifier.");
            return;
        }
        Order order = buildOrderFromForm(true);
        if (order == null) {
            return;
        }
        try {
            orderService.update(order);
            triggerOrderNotification(order, true);
            notifyOrderUpdated(order);
            refreshOrders(order.getId(), "Commande modifiee avec succes.", "status-success");
            clearValidation();
        } catch (SQLException | IllegalArgumentException exception) {
            showValidation(resolveMessage(exception));
        }
    }

    @FXML
    private void handleDeleteSelectedOrder() {
        if (selectedOrder == null) {
            showValidation("Selectionnez une commande a supprimer.");
            return;
        }
        confirmAndDelete(selectedOrder);
    }

    @FXML
    private void handleEditSelectedOrder() {
        if (selectedOrder == null) {
            showValidation("Selectionnez une commande a modifier.");
            return;
        }
        openEditForm(selectedOrder);
    }

    @FXML
    private void handleClearForm() {
        clearValidation();
        if (panelMode == PanelMode.EDIT && selectedOrder != null) {
            updateDetailPanel();
            selectionStateLabel.setText("Commande #" + selectedOrder.getId());
            setPanelMode(PanelMode.DETAIL);
            return;
        }
        selectedOrder = null;
        orderTableView.getSelectionModel().clearSelection();
        clearFormFields();
        updateDetailPanel();
        selectionStateLabel.setText("Selectionnez une commande");
        setPanelMode(PanelMode.EMPTY);
    }

    @FXML
    private void handleExportPdf() {
        if (!serviceReady || orderPdfExportService == null) {
            showValidation("Le service d'export PDF n'est pas disponible.");
            return;
        }

        List<Order> ordersToExport = new ArrayList<>(orders);
        if (ordersToExport.isEmpty()) {
            showValidation("Il n'y a aucune commande a exporter.");
            return;
        }

        Path target = choosePdfTarget();
        if (target == null) {
            setStatus("Export PDF annule.", "status-muted");
            return;
        }

        try {
            orderPdfExportService.exportOrders(
                    target,
                    ordersToExport,
                    id -> resolveChoiceLabel(productChoices, id, "Produit")
            );
            notifyOrdersExported(ordersToExport.size(), target);
            setStatus("Liste des commandes exportee en PDF.", "status-success");
        } catch (IOException exception) {
            setStatus("Export PDF impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Commandes", exception.getMessage());
        }
    }

    @FXML
    private void handleExportInvoice() {
        if (selectedOrder == null || orderPdfExportService == null) {
            showValidation("Selectionnez une commande pour exporter la facture.");
            return;
        }

        Path target = chooseInvoiceTarget(selectedOrder.getId());
        if (target == null) {
            setStatus("Export facture annule.", "status-muted");
            return;
        }

        try {
            Product product = resolveProduct(selectedOrder.getProductId());
            OrderPdfExportService.Invoice invoice = buildInvoice(selectedOrder, product);
            orderPdfExportService.exportInvoice(target, invoice, buildInvoiceQrPayload(selectedOrder, product));
            notifyInvoiceExported(selectedOrder, target);
            setStatus("Facture exportee avec QR code: " + target.getFileName(), "status-success");
        } catch (IOException | SQLException exception) {
            setStatus("Export facture impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Facture", resolveMessage(exception instanceof SQLException sqlException ? sqlException : new Exception(exception)));
        }
    }

    @FXML
    private void handleSendNotification() {
        if (selectedOrder == null) {
            showValidation("Selectionnez une commande pour envoyer une notification.");
            return;
        }
        triggerOrderNotification(selectedOrder, true);
    }

    @FXML
    private void handleRefreshLiveFx() {
        Order referenceOrder = selectedOrder != null ? selectedOrder : buildDraftOrderFromForm();
        if (referenceOrder == null) {
            showValidation("Selectionnez une commande ou renseignez le formulaire pour calculer le taux live.");
            return;
        }
        refreshLiveFx(referenceOrder);
    }

    private void configureControls() {
        productComboBox.setItems(productChoices);
        sortByComboBox.setItems(FXCollections.observableArrayList("Date", "Montant", "Statut", "Quantite"));
        sortDirectionComboBox.setItems(FXCollections.observableArrayList("Asc", "Desc"));
        sortByComboBox.setValue("Date");
        sortDirectionComboBox.setValue("Desc");
        statusComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedOrderStatuses()));
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedPaymentMethods()));
        paymentStatusComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedPaymentStatuses()));
        statusComboBox.setValue("PENDING");
        String defaultPaymentMethod = defaultPaymentMethod();
        paymentMethodComboBox.setValue(defaultPaymentMethod);
        paymentStatusComboBox.setValue(defaultPaymentStatus(defaultPaymentMethod));
        orderDatePicker.setValue(LocalDate.now());
        quantityField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,5}") ? change : null));
        totalAmountField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,8}([\\.,]\\d{0,2})?") ? change : null));
        registerDraftPreviewListeners();
    }

    private void configureTable() {
        orderTableView.setItems(orders);
        orderIdColumn.setCellValueFactory(cell -> new SimpleStringProperty("#" + cell.getValue().getId()));
        orderProductColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveChoiceLabel(productChoices, cell.getValue().getProductId(), "Produit")));
        orderCoachColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveClientName(cell.getValue())));
        orderQuantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        orderTotalColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getTotalAmount())));
        orderDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getOrderDate())));
        orderStatusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        orderStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : createEditableStatusChip(item, false));
            }
        });
        orderPaymentColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        orderPaymentColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : createEditableStatusChip(item, true));
            }
        });
        orderActionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        orderActionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button viewButton = createTableActionButton("Voir", "soft-button");
                Button editButton = createTableActionButton("Modifier", "ghost-button");
                Button deleteItemButton = createTableActionButton("Supprimer", "danger-button");
                viewButton.setOnAction(event -> selectOrder(item));
                editButton.setOnAction(event -> openEditForm(item));
                deleteItemButton.setOnAction(event -> confirmAndDelete(item));
                HBox box = new HBox(8, viewButton, editButton, deleteItemButton);
                box.getStyleClass().add("product-table-actions");
                setGraphic(box);
            }
        });
        orderTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> selectOrder(newValue));
    }

    private void configureCharts() {
        statusChart.setLegendVisible(false);
        statusChart.setAnimated(false);
        paymentChart.setLegendVisible(true);
        paymentChart.setLabelsVisible(true);
        paymentChart.setAnimated(false);
    }

    private void loadReferenceData() throws SQLException {
        productChoices.setAll(productService.getAll().stream()
                .map(product -> new ChoiceItem(product.getId(), product.getName() + " | stock " + product.getStock()))
                .sorted(Comparator.comparing(ChoiceItem::toString, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    private void refreshOrders(Integer selectedId, String successMessage, String styleClass) {
        if (!serviceReady) {
            return;
        }
        long requestId = refreshSequence.incrementAndGet();
        String keyword = trimToNull(searchField.getText());
        String sortBy = sortByComboBox.getValue();
        String sortDirection = sortDirectionComboBox.getValue();

        setStatus(successMessage == null ? "Chargement des commandes..." : successMessage,
                styleClass == null ? "status-muted" : styleClass);

        Task<RefreshPayload> loadTask = new Task<>() {
            @Override
            protected RefreshPayload call() throws Exception {
                List<Product> loadedProducts = productService.getAll();
                List<ChoiceItem> loadedProductChoices = loadedProducts.stream()
                        .map(product -> new ChoiceItem(product.getId(), product.getName() + " | stock " + product.getStock()))
                        .sorted(Comparator.comparing(ChoiceItem::toString, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                List<Order> filteredOrders = filterAndSort(
                        orderService.getAll(),
                        loadedProductChoices,
                        keyword,
                        sortBy,
                        sortDirection
                );
                return new RefreshPayload(loadedProducts, loadedProductChoices, filteredOrders);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            RefreshPayload payload = loadTask.getValue();
            productSnapshot = payload.products();
            productChoices.setAll(payload.productChoices());
            orders.setAll(payload.orders());
            updateMetrics();
            updateCharts();
            if (selectedId != null) {
                selectOrderById(selectedId);
            } else {
                updateDetailPanel();
                updateActionAvailability();
            }
            if (successMessage != null) {
                setStatus(successMessage, styleClass);
            } else if (orders.isEmpty()) {
                setStatus("Aucune commande trouvee.", "status-muted");
            } else {
                setStatus(orders.size() + " commande(s) chargees.", "status-muted");
            }
        });

        loadTask.setOnFailed(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            Throwable exception = loadTask.getException();
            showAlert(Alert.AlertType.ERROR, "Commandes", resolveMessage(exception instanceof Exception e ? e : new Exception(exception)));
        });

        DB_EXECUTOR.execute(loadTask);
    }

    private List<Order> filterAndSort(
            List<Order> source,
            List<ChoiceItem> productChoiceSnapshot,
            String keyword,
            String sortBy,
            String sortDirection
    ) {
        Comparator<Order> comparator = switch (sortBy == null ? "Date" : sortBy) {
            case "Montant" -> Comparator.comparing(Order::getTotalAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Statut" -> Comparator.comparing(order -> emptyIfNull(order.getStatus(), ""), String.CASE_INSENSITIVE_ORDER);
            case "Quantite" -> Comparator.comparing(Order::getQuantity, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if ("Desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        return source.stream()
                .filter(order -> matchesKeyword(order, keyword, productChoiceSnapshot))
                .sorted(comparator.thenComparing(Order::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean matchesKeyword(Order order, String keyword, List<ChoiceItem> productChoiceSnapshot) {
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(order.getId(), normalized)
                || contains(order.getStatus(), normalized)
                || contains(order.getPaymentMethod(), normalized)
                || contains(order.getPaymentStatus(), normalized)
                || contains(resolveClientName(order), normalized)
                || contains(order.getContactEmail(), normalized)
                || contains(resolveChoiceLabel(productChoiceSnapshot, order.getProductId(), "Produit"), normalized);
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Order buildOrderFromForm(boolean updateMode) {
        clearValidation();
        ChoiceItem product = productComboBox.getValue();
        String clientName = trimToNull(clientNameField.getText());
        if (product == null) {
            markFieldInvalid(productComboBox);
            showValidation("Selectionnez un produit.");
            return null;
        }
        if (clientName == null) {
            markFieldInvalid(clientNameField);
            showValidation("Saisissez le nom du client.");
            return null;
        }

        Order order = new Order();
        if (updateMode && selectedOrder != null) {
            order.setId(selectedOrder.getId());
        }
        order.setProductId(product.id);
        order.setClientName(clientName);
        order.setEntraineurId(selectedOrder == null ? null : selectedOrder.getEntraineurId());
        order.setOrderDate(orderDatePicker.getValue() == null ? LocalDate.now() : orderDatePicker.getValue());
        order.setStatus(statusComboBox.getValue());
        order.setPaymentMethod(paymentMethodComboBox.getValue());
        order.setPaymentStatus(paymentStatusComboBox.getValue());
        order.setQuantity(parseQuantity());
        order.setSize(trimToNull(sizeField.getText()));
        order.setContactEmail(trimToNull(contactEmailField.getText()));
        order.setContactPhone(trimToNull(contactPhoneField.getText()));
        order.setTotalAmount(parseTotalAmount());
        order.setShippingAddress(trimToNull(shippingAddressField.getText()));
        order.setBillingAddress(trimToNull(billingAddressField.getText()));
        return order;
    }

    private int parseQuantity() {
        String value = trimToNull(quantityField.getText());
        if (value == null) {
            markFieldInvalid(quantityField);
            throw new IllegalArgumentException("La quantite est obligatoire.");
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("La quantite doit etre superieure a zero.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            markFieldInvalid(quantityField);
            throw new IllegalArgumentException("Saisissez une quantite valide.");
        }
    }

    private BigDecimal parseTotalAmount() {
        String value = trimToNull(totalAmountField.getText());
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            markFieldInvalid(totalAmountField);
            throw new IllegalArgumentException("Saisissez un montant valide.");
        }
    }

    private void populateForm(Order order) {
        selectedOrder = order;
        selectChoice(productComboBox, productChoices, order.getProductId());
        clientNameField.setText(emptyIfNull(order.getClientName(), ""));
        orderDatePicker.setValue(order.getOrderDate());
        statusComboBox.setValue(emptyIfNull(order.getStatus(), "PENDING"));
        String paymentMethod = resolveEditablePaymentMethod(order.getPaymentMethod());
        paymentMethodComboBox.setValue(paymentMethod);
        paymentStatusComboBox.setValue(emptyIfNull(order.getPaymentStatus(), defaultPaymentStatus(paymentMethod)));
        quantityField.setText(String.valueOf(order.getQuantity()));
        sizeField.setText(emptyIfNull(order.getSize(), ""));
        contactEmailField.setText(emptyIfNull(order.getContactEmail(), ""));
        contactPhoneField.setText(emptyIfNull(order.getContactPhone(), ""));
        totalAmountField.setText(order.getTotalAmount() == null ? "" : order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        shippingAddressField.setText(emptyIfNull(order.getShippingAddress(), ""));
        billingAddressField.setText(emptyIfNull(order.getBillingAddress(), ""));
    }

    private void openEditForm(Order order) {
        if (order == null) {
            return;
        }
        clearValidation();
        populateForm(order);
        selectionStateLabel.setText("Edition commande #" + order.getId());
        setPanelMode(PanelMode.EDIT);
    }

    private void confirmAndDelete(Order order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la commande #" + order.getId() + " ?", ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            List<Integer> recipients = orderRecipients(order);
            String productLabel = resolveChoiceLabel(productChoices, order.getProductId(), "Produit");
            orderService.delete(order.getId());
            selectedOrder = null;
            clearFormFields();
            updateDetailPanel();
            selectionStateLabel.setText("Selectionnez une commande");
            setPanelMode(PanelMode.EMPTY);
            notifyOrderDeleted(recipients, order.getId(), productLabel);
            refreshOrders(null, "Commande supprimee avec succes.", "status-success");
        } catch (SQLException | IllegalArgumentException exception) {
            showAlert(Alert.AlertType.ERROR, "Suppression", resolveMessage(exception));
        }
    }

    private void selectOrder(Order order) {
        selectedOrder = order;
        updateDetailPanel();
        if (order == null) {
            selectionStateLabel.setText("Selectionnez une commande");
            if (panelMode != PanelMode.CREATE) {
                setPanelMode(PanelMode.EMPTY);
            } else {
                updateActionAvailability();
            }
            return;
        }
        selectionStateLabel.setText("Commande #" + order.getId());
        setPanelMode(PanelMode.DETAIL);
    }

    private void selectOrderById(Integer id) {
        if (id == null) {
            return;
        }
        orders.stream()
                .filter(order -> id.equals(order.getId()))
                .findFirst()
                .ifPresentOrElse(this::selectOrder, () -> {
                    orderTableView.getSelectionModel().clearSelection();
                    selectedOrder = null;
                    updateDetailPanel();
                    selectionStateLabel.setText("Selectionnez une commande");
                    setPanelMode(PanelMode.EMPTY);
                });
    }

    private void updateMetrics() {
        long confirmed = orders.stream().filter(order -> {
            String status = emptyIfNull(order.getStatus(), "").toUpperCase(Locale.ROOT);
            return "CONFIRMED".equals(status) || "DELIVERED".equals(status);
        }).count();
        long pendingPayment = orders.stream().filter(order -> {
            String status = emptyIfNull(order.getPaymentStatus(), "").toUpperCase(Locale.ROOT);
            return "PENDING".equals(status) || "UNPAID".equals(status);
        }).count();
        resultCountLabel.setText(orders.size() + " commande(s)");
        visibleOrdersMetricLabel.setText(String.valueOf(orders.size()));
        confirmedOrdersMetricLabel.setText(String.valueOf(confirmed));
        pendingPaymentsMetricLabel.setText(String.valueOf(pendingPayment));
        if (anomalyOrdersMetricLabel != null) {
            anomalyOrdersMetricLabel.setText(String.valueOf(countFlaggedOrders()));
        }
    }

    private void updateCharts() {
        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> normalizeChartStatus(order == null ? null : order.getStatus(), false),
                        java.util.LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> paymentCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> normalizeChartStatus(order == null ? null : order.getPaymentStatus(), true),
                        java.util.LinkedHashMap::new,
                        Collectors.counting()
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (String status : List.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(status, statusCounts.getOrDefault(status, 0L));
            series.getData().add(data);
            applyBarColor(data, colorForStatus(status));
        }
        statusChart.getData().setAll(series);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        paymentCounts.forEach((status, count) -> pieData.add(new PieChart.Data(status + " (" + count + ")", count)));
        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("No payments", 1));
        }
        paymentChart.setData(pieData);

        statusChartSummaryLabel.setText(orders.size() + " visible order(s) | " + formatCountSummary(statusCounts));
        paymentChartSummaryLabel.setText(paymentCounts.isEmpty() ? "Aucune donnee de paiement." : formatCountSummary(paymentCounts));
        Platform.runLater(() -> applyPieChartTheme(paymentChart));
    }

    private String normalizeChartStatus(String value, boolean paymentMode) {
        String normalized = emptyIfNull(value, paymentMode ? "UNKNOWN" : "PENDING")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (paymentMode) {
            return switch (normalized) {
                case "PAID", "PENDING", "UNPAID", "FAILED", "REFUNDED" -> normalized;
                default -> "UNKNOWN";
            };
        }
        return switch (normalized) {
            case "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED" -> normalized;
            default -> "PENDING";
        };
    }

    private String formatCountSummary(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private void updateDetailPanel() {
        if (selectedOrder == null) {
            Order draftOrder = buildDraftOrderFromForm();
            if (draftOrder != null && (panelMode == PanelMode.CREATE || panelMode == PanelMode.EDIT)) {
                Product draftProduct = resolveProductFromSnapshot(draftOrder.getProductId());
                currentAnomalyAssessment = orderIntelligenceService == null
                        ? OrderIntelligenceService.OrderAnomalyAssessment.empty()
                        : orderIntelligenceService.assessAnomaly(draftOrder, draftProduct, orders);
                List<OrderIntelligenceService.ProductRecommendation> recommendations = orderIntelligenceService == null
                        ? List.of()
                        : orderIntelligenceService.recommendProducts(draftProduct, productSnapshot, orders);

                detailBadgeLabel.setText("Brouillon");
                detailTitleLabel.setText(resolveChoiceLabel(productChoices, draftOrder.getProductId(), "Produit"));
                detailSubtitleLabel.setText(resolveClientName(draftOrder) + " | " + formatDate(draftOrder.getOrderDate()));
                detailTotalLabel.setText(formatPrice(draftOrder.getTotalAmount()));
                detailPaymentChipLabel.setText(emptyIfNull(draftOrder.getPaymentStatus()));
                applyStatusStyle(detailPaymentChipLabel, draftOrder.getPaymentStatus(), true);
                detailAnomalyChipLabel.setText(formatAnomalyLevel(currentAnomalyAssessment.level()));
                applyStatusStyle(detailAnomalyChipLabel, currentAnomalyAssessment.level(), false);
                detailIdValueLabel.setText("Auto");
                detailProductValueLabel.setText(resolveChoiceLabel(productChoices, draftOrder.getProductId(), "Produit"));
                detailCoachValueLabel.setText(resolveClientName(draftOrder));
                detailDateValueLabel.setText(formatDate(draftOrder.getOrderDate()));
                detailQuantityValueLabel.setText(draftOrder.getQuantity() == null ? "-" : String.valueOf(draftOrder.getQuantity()));
                detailEmailValueLabel.setText(emptyIfNull(draftOrder.getContactEmail()));
                detailPhoneValueLabel.setText(emptyIfNull(draftOrder.getContactPhone()));
                detailShippingValueLabel.setText(emptyIfNull(draftOrder.getShippingAddress()));
                detailBillingValueLabel.setText(emptyIfNull(draftOrder.getBillingAddress()));
                detailAnomalySummaryLabel.setText(formatAnomalySummary(currentAnomalyAssessment));
                detailRecommendationValueLabel.setText(formatRecommendations(recommendations));
                refreshLiveFx(draftOrder);
                return;
            }

            detailBadgeLabel.setText("Apercu");
            detailTitleLabel.setText("Aucune commande selectionnee");
            detailSubtitleLabel.setText(panelMode == PanelMode.CREATE
                    ? "L'analyse de risque et les recommandations apparaitront ici pendant la saisie."
                    : "Choisissez une commande pour afficher ses informations et ses actions.");
            detailTotalLabel.setText("0.00 DT");
            detailPaymentChipLabel.setText("-");
            detailAnomalyChipLabel.setText(formatAnomalyLevel("LOW"));
            detailIdValueLabel.setText("Auto");
            detailProductValueLabel.setText("-");
            detailCoachValueLabel.setText("-");
            detailDateValueLabel.setText("-");
            detailQuantityValueLabel.setText("-");
            detailEmailValueLabel.setText("-");
            detailPhoneValueLabel.setText("-");
            detailShippingValueLabel.setText("-");
            detailBillingValueLabel.setText("-");
            detailLiveFxValueLabel.setText("Selectionnez une commande pour charger la conversion live.");
            detailAnomalySummaryLabel.setText("Score 0/100\nAucun signal de risque detecte pour le moment.");
            detailRecommendationValueLabel.setText("Selectionnez une commande pour afficher des suggestions produits.");
            applyStatusStyle(detailAnomalyChipLabel, "LOW", false);
            return;
        }

        Product selectedProductRef = resolveProductFromSnapshot(selectedOrder.getProductId());
        currentAnomalyAssessment = orderIntelligenceService == null
                ? OrderIntelligenceService.OrderAnomalyAssessment.empty()
                : orderIntelligenceService.assessAnomaly(selectedOrder, selectedProductRef, orders);
        List<OrderIntelligenceService.ProductRecommendation> recommendations = orderIntelligenceService == null
                ? List.of()
                : orderIntelligenceService.recommendProducts(selectedProductRef, productSnapshot, orders);

        detailBadgeLabel.setText(emptyIfNull(selectedOrder.getStatus()));
        detailTitleLabel.setText(resolveChoiceLabel(productChoices, selectedOrder.getProductId(), "Produit"));
        detailSubtitleLabel.setText(resolveClientName(selectedOrder) + " | " + formatDate(selectedOrder.getOrderDate()));
        detailTotalLabel.setText(formatPrice(selectedOrder.getTotalAmount()));
        detailPaymentChipLabel.setText(emptyIfNull(selectedOrder.getPaymentStatus()));
        applyStatusStyle(detailPaymentChipLabel, selectedOrder.getPaymentStatus(), true);
        detailAnomalyChipLabel.setText(formatAnomalyLevel(currentAnomalyAssessment.level()));
        applyStatusStyle(detailAnomalyChipLabel, currentAnomalyAssessment.level(), false);
        detailIdValueLabel.setText(String.valueOf(selectedOrder.getId()));
        detailProductValueLabel.setText(resolveChoiceLabel(productChoices, selectedOrder.getProductId(), "Produit"));
        detailCoachValueLabel.setText(resolveClientName(selectedOrder));
        detailDateValueLabel.setText(formatDate(selectedOrder.getOrderDate()));
        detailQuantityValueLabel.setText(String.valueOf(selectedOrder.getQuantity()));
        detailEmailValueLabel.setText(emptyIfNull(selectedOrder.getContactEmail()));
        detailPhoneValueLabel.setText(emptyIfNull(selectedOrder.getContactPhone()));
        detailShippingValueLabel.setText(emptyIfNull(selectedOrder.getShippingAddress()));
        detailBillingValueLabel.setText(emptyIfNull(selectedOrder.getBillingAddress()));
        detailAnomalySummaryLabel.setText(formatAnomalySummary(currentAnomalyAssessment));
        detailRecommendationValueLabel.setText(formatRecommendations(recommendations));
        refreshLiveFx(selectedOrder);
    }

    private Label createStatusChip(String value, boolean paymentMode) {
        Label label = new Label(emptyIfNull(value));
        applyStatusStyle(label, value, paymentMode);
        return label;
    }

    private MenuButton createEditableStatusChip(Order order, boolean paymentMode) {
        String currentValue = paymentMode ? order.getPaymentStatus() : order.getStatus();
        MenuButton menuButton = new MenuButton(emptyIfNull(currentValue));
        menuButton.setFocusTraversable(false);
        menuButton.getStyleClass().setAll("status-pill", "product-stock-chip", "order-status-menu");
        applyStatusStyle(menuButton, currentValue, paymentMode);

        List<String> allowedValues = paymentMode
                ? OrderService.allowedPaymentStatuses()
                : OrderService.allowedOrderStatuses();

        for (String candidate : allowedValues) {
            MenuItem item = new MenuItem(candidate);
            item.setOnAction(event -> handleInlineStatusUpdate(order, candidate, paymentMode));
            menuButton.getItems().add(item);
        }
        return menuButton;
    }

    private void applyStatusStyle(Control control, String value, boolean paymentMode) {
        control.getStyleClass().removeAll("product-stock-good", "product-stock-low", "product-stock-out");
        String normalized = emptyIfNull(value, "").toUpperCase(Locale.ROOT);
        if (paymentMode) {
            if ("PAID".equals(normalized)) {
                control.getStyleClass().add("product-stock-good");
            } else if ("FAILED".equals(normalized) || "REFUNDED".equals(normalized)) {
                control.getStyleClass().add("product-stock-out");
            } else {
                control.getStyleClass().add("product-stock-low");
            }
            return;
        }
        if ("HIGH".equals(normalized)) {
            control.getStyleClass().add("product-stock-out");
            return;
        }
        if ("MEDIUM".equals(normalized)) {
            control.getStyleClass().add("product-stock-low");
            return;
        }
        if ("LOW".equals(normalized)) {
            control.getStyleClass().add("product-stock-good");
            return;
        }
        if ("DELIVERED".equals(normalized) || "CONFIRMED".equals(normalized)) {
            control.getStyleClass().add("product-stock-good");
        } else if ("CANCELLED".equals(normalized)) {
            control.getStyleClass().add("product-stock-out");
        } else {
            control.getStyleClass().add("product-stock-low");
        }
    }

    private void handleInlineStatusUpdate(Order sourceOrder, String newValue, boolean paymentMode) {
        if (sourceOrder == null || orderService == null) {
            return;
        }
        String currentValue = paymentMode ? sourceOrder.getPaymentStatus() : sourceOrder.getStatus();
        if (emptyIfNull(currentValue).equalsIgnoreCase(emptyIfNull(newValue))) {
            return;
        }

        Order updatedOrder = copyOrder(sourceOrder);
        if (paymentMode) {
            updatedOrder.setPaymentStatus(newValue);
        } else {
            updatedOrder.setStatus(newValue);
        }

        String fieldLabel = paymentMode ? "paiement" : "statut";
        setStatus("Mise a jour du " + fieldLabel + " en cours...", "status-muted");

        Task<Order> task = new Task<>() {
            @Override
            protected Order call() throws Exception {
                orderService.update(updatedOrder);
                return updatedOrder;
            }
        };

        task.setOnSucceeded(event -> {
            Order persistedOrder = task.getValue();
            triggerOrderNotification(persistedOrder, true);
            refreshOrders(
                    persistedOrder.getId(),
                    "Commande #" + persistedOrder.getId() + " mise a jour: " + fieldLabel + " " + newValue + ".",
                    "status-success"
            );
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            setStatus("Mise a jour impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Commande", resolveMessage(exception instanceof Exception e ? e : new Exception(exception)));
            orderTableView.refresh();
        });

        DB_EXECUTOR.execute(task);
    }

    private Order copyOrder(Order source) {
        if (source == null) {
            return null;
        }
        return new Order(
                source.getId(),
                source.getQuantity(),
                source.getOrderDate(),
                source.getClientName(),
                source.getStatus(),
                source.getPaymentMethod(),
                source.getPaymentStatus(),
                source.getSize(),
                source.getContactEmail(),
                source.getContactPhone(),
                source.getShippingAddress(),
                source.getBillingAddress(),
                source.getTotalAmount(),
                source.getProductId(),
                source.getEntraineurId()
        );
    }

    private String colorForStatus(String status) {
        if (darkMode) {
            return switch (status) {
                case "CONFIRMED" -> "#9d71ff";
                case "SHIPPED" -> "#7c84ff";
                case "DELIVERED" -> "#57d5ff";
                case "CANCELLED" -> "#ff63d0";
                default -> "#c084fc";
            };
        }
        return switch (status) {
            case "CONFIRMED" -> "#0ea5e9";
            case "SHIPPED" -> "#6366f1";
            case "DELIVERED" -> "#22c55e";
            case "CANCELLED" -> "#ef4444";
            default -> "#f59e0b";
        };
    }

    private void clearFormFields() {
        productComboBox.getSelectionModel().clearSelection();
        clientNameField.clear();
        orderDatePicker.setValue(LocalDate.now());
        statusComboBox.setValue("PENDING");
        String defaultPaymentMethod = defaultPaymentMethod();
        paymentMethodComboBox.setValue(defaultPaymentMethod);
        paymentStatusComboBox.setValue(defaultPaymentStatus(defaultPaymentMethod));
        quantityField.clear();
        sizeField.clear();
        contactEmailField.clear();
        contactPhoneField.clear();
        totalAmountField.clear();
        shippingAddressField.clear();
        billingAddressField.clear();
        currentAnomalyAssessment = OrderIntelligenceService.OrderAnomalyAssessment.empty();
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedOrder != null;
        boolean createMode = panelMode == PanelMode.CREATE;
        boolean editMode = panelMode == PanelMode.EDIT;
        if (createOrderButton != null) {
            createOrderButton.setDisable(!createMode);
        }
        updateButton.setDisable(!editMode || !hasSelection);
        deleteButton.setDisable(!editMode || !hasSelection);
        detailEditButton.setDisable(!hasSelection);
        detailDeleteButton.setDisable(!hasSelection);
        if (exportInvoiceButton != null) {
            exportInvoiceButton.setDisable(!hasSelection);
        }
        if (sendNotificationButton != null) {
            sendNotificationButton.setDisable(!hasSelection);
        }
        if (cancelFormButton != null) {
            cancelFormButton.setDisable(!(createMode || editMode));
        }
    }

    private String defaultPaymentMethod() {
        if (OrderService.allowedPaymentMethods().contains("CASH_ON_DELIVERY")) {
            return "CASH_ON_DELIVERY";
        }
        return OrderService.allowedPaymentMethods().isEmpty() ? null : OrderService.allowedPaymentMethods().get(0);
    }

    private String resolveEditablePaymentMethod(String paymentMethod) {
        String normalized = emptyIfNull(paymentMethod, "");
        return OrderService.allowedPaymentMethods().contains(normalized) ? normalized : defaultPaymentMethod();
    }

    private String defaultPaymentStatus(String paymentMethod) {
        return "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod) ? "PENDING" : "UNPAID";
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
        for (Control control : List.of(productComboBox, clientNameField, quantityField, totalAmountField, contactEmailField, contactPhoneField, shippingAddressField, billingAddressField)) {
            control.getStyleClass().remove("invalid-field");
        }
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void selectChoice(ComboBox<ChoiceItem> comboBox, List<ChoiceItem> choices, Integer id) {
        choices.stream().filter(choice -> choice.id.equals(id)).findFirst().ifPresent(comboBox::setValue);
    }

    private String resolveChoiceLabel(List<ChoiceItem> choices, Integer id, String fallbackPrefix) {
        if (id == null) {
            return "-";
        }
        return choices.stream().filter(choice -> choice.id.equals(id)).map(ChoiceItem::toString).findFirst().orElse(fallbackPrefix + " #" + id);
    }

    private String resolveClientName(Order order) {
        if (order == null) {
            return "-";
        }
        String clientName = trimToNull(order.getClientName());
        if (clientName != null) {
            return clientName;
        }
        return buildCustomerLabel(order);
    }

    private Order buildDraftOrderFromForm() {
        ChoiceItem product = productComboBox == null ? null : productComboBox.getValue();
        if (product == null) {
            return null;
        }

        Order order = new Order();
        order.setProductId(product.id);
        order.setClientName(trimToNull(clientNameField == null ? null : clientNameField.getText()));
        order.setOrderDate(orderDatePicker == null || orderDatePicker.getValue() == null ? LocalDate.now() : orderDatePicker.getValue());
        order.setStatus(statusComboBox == null ? null : statusComboBox.getValue());
        order.setPaymentMethod(paymentMethodComboBox == null ? null : paymentMethodComboBox.getValue());
        order.setPaymentStatus(paymentStatusComboBox == null ? null : paymentStatusComboBox.getValue());
        order.setQuantity(parseOptionalInteger(quantityField == null ? null : quantityField.getText()));
        order.setSize(trimToNull(sizeField == null ? null : sizeField.getText()));
        order.setContactEmail(trimToNull(contactEmailField == null ? null : contactEmailField.getText()));
        order.setContactPhone(trimToNull(contactPhoneField == null ? null : contactPhoneField.getText()));
        order.setTotalAmount(parseOptionalAmount(totalAmountField == null ? null : totalAmountField.getText()));
        order.setShippingAddress(trimToNull(shippingAddressField == null ? null : shippingAddressField.getText()));
        order.setBillingAddress(trimToNull(billingAddressField == null ? null : billingAddressField.getText()));
        return order;
    }

    private Integer parseOptionalInteger(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal parseOptionalAmount(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void registerDraftPreviewListeners() {
        productComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        clientNameField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        orderDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        paymentMethodComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        paymentStatusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        quantityField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        sizeField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        contactEmailField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        contactPhoneField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        totalAmountField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        shippingAddressField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
        billingAddressField.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftPreviewIfNeeded());
    }

    private void refreshDraftPreviewIfNeeded() {
        if (panelMode == PanelMode.CREATE || panelMode == PanelMode.EDIT) {
            updateDetailPanel();
        }
    }

    private Integer getSelectedOrderId() {
        return selectedOrder == null ? null : selectedOrder.getId();
    }

    private long countFlaggedOrders() {
        if (orderIntelligenceService == null) {
            return 0;
        }
        return orders.stream()
                .filter(order -> {
                    Product product = resolveProductFromSnapshot(order.getProductId());
                    OrderIntelligenceService.OrderAnomalyAssessment assessment =
                            orderIntelligenceService.assessAnomaly(order, product, orders);
                    return "HIGH".equalsIgnoreCase(assessment.level()) || "MEDIUM".equalsIgnoreCase(assessment.level());
                })
                .count();
    }

    private void refreshLiveFx(Order order) {
        if (detailLiveFxValueLabel == null) {
            return;
        }
        if (currencyConversionService == null) {
            detailLiveFxValueLabel.setText("API devise indisponible.");
            return;
        }
        BigDecimal totalAmount = order == null ? null : order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            detailLiveFxValueLabel.setText("Montant total requis pour la conversion live.");
            return;
        }

        long requestId = liveFxSequence.incrementAndGet();
        detailLiveFxValueLabel.setText("Chargement du taux live...");
        Task<CurrencyConversionService.ConversionResult> task = new Task<>() {
            @Override
            protected CurrencyConversionService.ConversionResult call() throws Exception {
                return currencyConversionService.convert(totalAmount, "TND", List.of("USD", "EUR", "GBP"));
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != liveFxSequence.get() || order != selectedOrder) {
                return;
            }
            detailLiveFxValueLabel.setText(formatLiveFx(task.getValue()));
        });

        task.setOnFailed(event -> {
            if (requestId != liveFxSequence.get() || order != selectedOrder) {
                return;
            }
            detailLiveFxValueLabel.setText("API devise indisponible pour le moment.");
        });

        IO_EXECUTOR.execute(task);
    }

    private String formatLiveFx(CurrencyConversionService.ConversionResult result) {
        if (result == null || result.convertedAmounts() == null || result.convertedAmounts().isEmpty()) {
            return "Aucune conversion live disponible.";
        }
        String amounts = result.convertedAmounts().entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .collect(Collectors.joining(" | "));
        String rateDate = emptyIfNull(result.rateDate(), "unknown");
        return amounts + " | source " + rateDate;
    }

    private Product resolveProduct(Integer productId) throws SQLException {
        Product product = resolveProductFromSnapshot(productId);
        if (product != null) {
            return product;
        }
        return productId == null || productService == null ? null : productService.getById(productId);
    }

    private Product resolveProductFromSnapshot(Integer productId) {
        if (productId == null) {
            return null;
        }
        return productSnapshot.stream()
                .filter(product -> productId.equals(product.getId()))
                .findFirst()
                .orElse(null);
    }

    private String formatRecommendations(List<OrderIntelligenceService.ProductRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Aucune suggestion immediate.\nLa commande actuelle reste l'option la plus coherente pour ce produit.";
        }
        return recommendations.stream()
                .map(recommendation -> "- " + recommendation.productName() + " - " + translateRecommendationReason(recommendation.reason()))
                .collect(Collectors.joining("\n"));
    }

    private String formatAnomalySummary(OrderIntelligenceService.OrderAnomalyAssessment assessment) {
        if (assessment == null || assessment.reasons() == null || assessment.reasons().isEmpty()) {
            return "Score 0/100\nAucun signal de risque detecte pour cette commande.";
        }
        String intro = switch (emptyIfNull(assessment.level(), "").toUpperCase(Locale.ROOT)) {
            case "HIGH" -> "Verification conseillee avant validation :";
            case "MEDIUM" -> "Quelques points meritent un controle :";
            default -> "Commande globalement saine, avec un leger point d'attention :";
        };
        String reasons = assessment.reasons().stream()
                .map(this::translateAnomalyReason)
                .map(reason -> "- " + reason)
                .collect(Collectors.joining("\n"));
        return "Score " + assessment.score() + "/100\n" + intro + "\n" + reasons;
    }

    private String formatAnomalyLevel(String level) {
        return switch (emptyIfNull(level, "").toUpperCase(Locale.ROOT)) {
            case "HIGH" -> "Risque eleve";
            case "MEDIUM" -> "A surveiller";
            default -> "Stable";
        };
    }

    private String translateAnomalyReason(String reason) {
        return switch (emptyIfNull(reason, "").toLowerCase(Locale.ROOT)) {
            case "large quantity" -> "quantite tres elevee";
            case "above normal quantity" -> "quantite au-dessus du volume habituel";
            case "well above product average" -> "niveau nettement au-dessus de la moyenne du produit";
            case "high order amount" -> "montant bien superieur aux commandes habituelles";
            case "many recent orders from same customer" -> "plusieurs commandes recentes du meme client";
            case "repeat customer in a short window" -> "nouvelle commande du meme client sur une courte periode";
            case "consumes most of remaining stock" -> "commande qui absorbe une grande partie du stock restant";
            default -> reason;
        };
    }

    private String translateRecommendationReason(String reason) {
        String normalized = emptyIfNull(reason, "").toLowerCase(Locale.ROOT);
        if ("same brand cross-sell".equals(normalized)) {
            return "meme marque, bon produit complementaire";
        }
        if (normalized.startsWith("complements ")) {
            return "complete bien " + normalized.substring("complements ".length());
        }
        if ("popular with recent orders".equals(normalized)) {
            return "souvent choisi dans les commandes recentes";
        }
        if ("related product".equals(normalized)) {
            return "alternative proche pour enrichir la commande";
        }
        return reason;
    }

    private void setPanelMode(PanelMode mode) {
        panelMode = mode == null ? PanelMode.EMPTY : mode;
        setVisibleManaged(emptyStateCard, panelMode == PanelMode.EMPTY);
        setVisibleManaged(detailCard, panelMode == PanelMode.DETAIL || panelMode == PanelMode.CREATE || panelMode == PanelMode.EDIT);
        setVisibleManaged(formCard, panelMode == PanelMode.CREATE || panelMode == PanelMode.EDIT);

        if (panelMode == PanelMode.EDIT) {
            formModeLabel.setText("Modifier la commande");
            formHintLabel.setText("Ajustez les champs ci-dessous puis appliquez les changements sur la commande selectionnee.");
            if (cancelFormButton != null) {
                cancelFormButton.setText("Retour au detail");
            }
        } else if (panelMode == PanelMode.CREATE) {
            formModeLabel.setText("Ajouter une commande");
            formHintLabel.setText("Renseignez les informations ci-dessous pour enregistrer une nouvelle commande.");
            if (cancelFormButton != null) {
                cancelFormButton.setText("Annuler");
            }
        }

        if (createOrderButton != null) {
            setVisibleManaged(createOrderButton, panelMode == PanelMode.CREATE);
        }
        setVisibleManaged(updateButton, panelMode == PanelMode.EDIT);
        setVisibleManaged(deleteButton, panelMode == PanelMode.EDIT);
        updateActionAvailability();
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void triggerOrderNotification(Order order, boolean updateMode) {
        if (orderNotificationService == null || order == null) {
            return;
        }

        Task<OrderNotificationService.DeliveryResult> task = new Task<>() {
            @Override
            protected OrderNotificationService.DeliveryResult call() throws Exception {
                Product product = resolveProduct(order.getProductId());
                return orderNotificationService.sendOrderNotification(order, product, resolveClientName(order), updateMode);
            }
        };

        task.setOnSucceeded(event -> {
            OrderNotificationService.DeliveryResult result = task.getValue();
            if (result == null) {
                return;
            }
            notifyOrderEmailResult(order, result);
            if (result.delivered()) {
                setStatus(result.message(), "status-success");
            } else if (result.previewPath() != null) {
                setStatus(result.message() + " " + result.previewPath(), "status-muted");
            } else {
                setStatus(result.message(), "status-muted");
            }
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            notifyOrderEmailFailure(order);
            setStatus("Notification commande non envoyee.", "status-error");
            if (exception != null) {
                showAlert(Alert.AlertType.ERROR, "Notification", resolveMessage(exception instanceof Exception e ? e : new Exception(exception)));
            }
        });

        IO_EXECUTOR.execute(task);
    }

    private OrderPdfExportService.Invoice buildInvoice(Order order, Product product) {
        BigDecimal totalAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        int quantity = order.getQuantity() == null || order.getQuantity() <= 0 ? 1 : order.getQuantity();
        BigDecimal unitPrice = totalAmount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        OrderPdfExportService.InvoiceLine line = new OrderPdfExportService.InvoiceLine(
                product == null ? resolveChoiceLabel(productChoices, order.getProductId(), "Produit") : emptyIfNull(product.getName()),
                emptyIfNull(order.getSize(), product == null ? "-" : product.getSize()),
                quantity,
                unitPrice,
                totalAmount
        );
        return new OrderPdfExportService.Invoice(
                buildCustomerLabel(order),
                order.getContactEmail(),
                order.getContactPhone(),
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getOrderDate() == null ? LocalDate.now() : order.getOrderDate(),
                totalAmount,
                List.of(line)
        );
    }

    private String buildCustomerLabel(Order order) {
        String clientName = trimToNull(order == null ? null : order.getClientName());
        if (clientName != null) {
            return clientName;
        }
        String email = trimToNull(order == null ? null : order.getContactEmail());
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        String phone = trimToNull(order == null ? null : order.getContactPhone());
        if (phone != null) {
            return "Client " + phone;
        }
        return "Client Sport Insight";
    }

    private void notifyOrderCreated(Order order) {
        if (workflowNotificationService == null || order == null) {
            return;
        }
        Product product = resolveProductFromSnapshot(order.getProductId());
        workflowNotificationService.notifyOrderCreated(orderRecipients(order), order, product);
    }

    private void notifyOrderUpdated(Order order) {
        if (workflowNotificationService == null || order == null) {
            return;
        }
        Product product = resolveProductFromSnapshot(order.getProductId());
        workflowNotificationService.notifyOrderUpdated(orderRecipients(order), order, product);
    }

    private void notifyOrderDeleted(List<Integer> recipients, Integer orderId, String productLabel) {
        if (workflowNotificationService == null) {
            return;
        }
        workflowNotificationService.notifyOrderDeleted(recipients, orderId, productLabel);
    }

    private void notifyInvoiceExported(Order order, Path target) {
        if (workflowNotificationService == null || order == null) {
            return;
        }
        workflowNotificationService.notifyInvoiceExported(orderRecipients(order), order.getId(), target);
    }

    private void notifyOrdersExported(int count, Path target) {
        Integer userId = currentUserId();
        if (workflowNotificationService == null || userId == null) {
            return;
        }
        workflowNotificationService.notifyOrdersExported(userId, count, target);
    }

    private void notifyOrderEmailResult(Order order, OrderNotificationService.DeliveryResult result) {
        if (workflowNotificationService == null || order == null || result == null) {
            return;
        }
        workflowNotificationService.notifyOrderEmailResult(orderRecipients(order), order.getId(), result);
    }

    private void notifyOrderEmailFailure(Order order) {
        if (workflowNotificationService == null || order == null) {
            return;
        }
        OrderNotificationService.DeliveryResult failure = new OrderNotificationService.DeliveryResult(
                false,
                "Notification email non envoyee.",
                null,
                null,
                null
        );
        workflowNotificationService.notifyOrderEmailResult(orderRecipients(order), order.getId(), failure);
    }

    private List<Integer> orderRecipients(Order order) {
        List<Integer> recipients = new ArrayList<>();
        Integer currentUserId = currentUserId();
        if (currentUserId != null && !recipients.contains(currentUserId)) {
            recipients.add(currentUserId);
        }
        Integer ownerId = order == null ? null : order.getEntraineurId();
        if (ownerId != null && ownerId > 0 && !recipients.contains(ownerId)) {
            recipients.add(ownerId);
        }
        return recipients;
    }

    private Integer currentUserId() {
        return AuthSession.getCurrentUser() == null ? null : AuthSession.getCurrentUser().getId();
    }

    private String buildInvoiceQrPayload(Order order, Product product) {
        return """
                ORDER:%s
                PRODUCT:%s
                QTY:%s
                TOTAL:%s
                DATE:%s
                STATUS:%s
                """.formatted(
                order.getId(),
                product == null ? resolveChoiceLabel(productChoices, order.getProductId(), "Produit") : emptyIfNull(product.getName()),
                order.getQuantity() == null ? 0 : order.getQuantity(),
                formatPrice(order.getTotalAmount()),
                formatDate(order.getOrderDate()),
                emptyIfNull(order.getStatus())
        );
    }

    private Path choosePdfTarget() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les commandes en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("orders-export-" + LocalDate.now() + ".pdf");
        Window owner = exportPdfButton == null || exportPdfButton.getScene() == null
                ? (pageRoot == null || pageRoot.getScene() == null ? null : pageRoot.getScene().getWindow())
                : exportPdfButton.getScene().getWindow();
        java.io.File targetFile = chooser.showSaveDialog(owner);
        return targetFile == null ? null : targetFile.toPath();
    }

    private Path chooseInvoiceTarget(Integer orderId) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter la facture");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("invoice-order-" + (orderId == null ? "draft" : orderId) + ".pdf");
        Window owner = exportInvoiceButton == null || exportInvoiceButton.getScene() == null
                ? (pageRoot == null || pageRoot.getScene() == null ? null : pageRoot.getScene().getWindow())
                : exportInvoiceButton.getScene().getWindow();
        java.io.File targetFile = chooser.showSaveDialog(owner);
        return targetFile == null ? null : targetFile.toPath();
    }

    private Button createTableActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClass, "product-table-action");
        return button;
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(emptyIfNull(message, "Pret"));
        statusLabel.getStyleClass().setAll("status-pill", styleClass == null ? "status-muted" : styleClass);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " DT";
    }

    private String resolveMessage(Exception exception) {
        String message = exception == null ? null : trimToNull(exception.getMessage());
        return message == null ? "Une erreur est survenue." : message;
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        if (node != null) {
            node.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        }
    }

    private void applyBarColor(XYChart.Data<String, Number> data, String color) {
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

    private void applyPieChartTheme(PieChart chart) {
        if (chart == null) {
            return;
        }
        String labelColor = darkMode ? "#eef3ff" : "#475569";
        String lineColor = darkMode ? "rgba(226, 232, 255, 0.58)" : "rgba(71, 85, 105, 0.5)";
        String legendColor = darkMode ? "#eef3ff" : "#475569";
        String legendBackground = darkMode ? "rgba(31, 38, 67, 0.96)" : "rgba(255, 255, 255, 0.82)";
        chart.applyCss();
        chart.lookupAll(".chart-pie-label").forEach(node -> node.setStyle("-fx-fill: " + labelColor + "; -fx-font-weight: 700;"));
        chart.lookupAll(".chart-pie-label-line").forEach(node -> node.setStyle("-fx-stroke: " + lineColor + ";"));
        chart.lookupAll(".chart-legend").forEach(node -> node.setStyle("-fx-background-color: " + legendBackground + "; -fx-background-radius: 12;"));
        chart.lookupAll(".chart-legend-item").forEach(node -> node.setStyle("-fx-text-fill: " + legendColor + ";"));
        chart.lookupAll(".chart-legend-item .label").forEach(node -> node.setStyle("-fx-text-fill: " + legendColor + ";"));
    }

    private static final class ChoiceItem {
        private final Integer id;
        private final String label;

        private ChoiceItem(Integer id, String label) {
            this.id = id;
            this.label = label == null || label.isBlank() ? "-" : label.trim();
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record RefreshPayload(List<Product> products, List<ChoiceItem> productChoices, List<Order> orders) {
    }

    private enum PanelMode {
        EMPTY,
        DETAIL,
        CREATE,
        EDIT
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
