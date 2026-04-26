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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Order;
import tn.esprit.security.UserRoles;
import tn.esprit.services.OrderPdfExportService;
import tn.esprit.services.OrderService;
import tn.esprit.services.ProductService;
import tn.esprit.services.UserService;

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

    @FXML private BorderPane pageRoot;
    @FXML private ScrollPane pageScroll;
    @FXML private Label resultCountLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label statusLabel;
    @FXML private Label visibleOrdersMetricLabel;
    @FXML private Label confirmedOrdersMetricLabel;
    @FXML private Label pendingPaymentsMetricLabel;
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
    @FXML private Label detailIdValueLabel;
    @FXML private Label detailProductValueLabel;
    @FXML private Label detailCoachValueLabel;
    @FXML private Label detailDateValueLabel;
    @FXML private Label detailQuantityValueLabel;
    @FXML private Label detailEmailValueLabel;
    @FXML private Label detailPhoneValueLabel;
    @FXML private Label detailShippingValueLabel;
    @FXML private Label detailBillingValueLabel;
    @FXML private Label formModeLabel;
    @FXML private Label formHintLabel;
    @FXML private Label validationLabel;
    @FXML private ComboBox<ChoiceItem> productComboBox;
    @FXML private ComboBox<ChoiceItem> coachComboBox;
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
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button detailEditButton;
    @FXML private Button detailDeleteButton;
    @FXML private Button exportPdfButton;

    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ObservableList<ChoiceItem> productChoices = FXCollections.observableArrayList();
    private final ObservableList<ChoiceItem> coachChoices = FXCollections.observableArrayList();

    private OrderService orderService;
    private ProductService productService;
    private UserService userService;
    private OrderPdfExportService orderPdfExportService;
    private Order selectedOrder;
    private boolean serviceReady;
    private boolean darkMode;
    private final AtomicLong refreshSequence = new AtomicLong();

    @FXML
    public void initialize() {
        configureControls();
        configureTable();
        configureCharts();
        updateDetailPanel();
        updateActionAvailability();
        Platform.runLater(this::applyWorkspaceSurface);

        try {
            orderService = new OrderService();
            productService = new ProductService();
            userService = new UserService();
            orderPdfExportService = new OrderPdfExportService();
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
        updateActionAvailability();
        formModeLabel.setText("Ajouter une commande");
        formHintLabel.setText("Renseignez les informations ci-dessous pour enregistrer une nouvelle commande.");
        selectionStateLabel.setText("Selectionnez une commande");
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
            refreshOrders(null, "Commande ajoutee avec succes.", "status-success");
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
        populateForm(selectedOrder);
    }

    @FXML
    private void handleClearForm() {
        handleNewOrder();
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
                    id -> resolveChoiceLabel(productChoices, id, "Produit"),
                    id -> resolveChoiceLabel(coachChoices, id, "Coach")
            );
            setStatus("Liste des commandes exportee en PDF.", "status-success");
        } catch (IOException exception) {
            setStatus("Export PDF impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Commandes", exception.getMessage());
        }
    }

    private void configureControls() {
        productComboBox.setItems(productChoices);
        coachComboBox.setItems(coachChoices);
        sortByComboBox.setItems(FXCollections.observableArrayList("Date", "Montant", "Statut", "Quantite"));
        sortDirectionComboBox.setItems(FXCollections.observableArrayList("Asc", "Desc"));
        sortByComboBox.setValue("Date");
        sortDirectionComboBox.setValue("Desc");
        statusComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedOrderStatuses()));
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedPaymentMethods()));
        paymentStatusComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedPaymentStatuses()));
        statusComboBox.setValue("PENDING");
        paymentMethodComboBox.setValue("CARD");
        paymentStatusComboBox.setValue("UNPAID");
        orderDatePicker.setValue(LocalDate.now());
        quantityField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,5}") ? change : null));
        totalAmountField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,8}([\\.,]\\d{0,2})?") ? change : null));
    }

    private void configureTable() {
        orderTableView.setItems(orders);
        orderIdColumn.setCellValueFactory(cell -> new SimpleStringProperty("#" + cell.getValue().getId()));
        orderProductColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveChoiceLabel(productChoices, cell.getValue().getProductId(), "Produit")));
        orderCoachColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveChoiceLabel(coachChoices, cell.getValue().getEntraineurId(), "Coach")));
        orderQuantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        orderTotalColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getTotalAmount())));
        orderDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getOrderDate())));
        orderStatusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        orderStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : createStatusChip(item.getStatus(), false));
            }
        });
        orderPaymentColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        orderPaymentColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : createStatusChip(item.getPaymentStatus(), true));
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
                editButton.setOnAction(event -> {
                    selectOrder(item);
                    populateForm(item);
                });
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
                .map(product -> new ChoiceItem(product.getId(), product.getName() + " • stock " + product.getStock()))
                .sorted(Comparator.comparing(ChoiceItem::toString, String.CASE_INSENSITIVE_ORDER))
                .toList());
        coachChoices.setAll(userService.getAll().stream()
                .filter(user -> user.hasRole(UserRoles.ROLE_ENTRAINEUR))
                .map(user -> new ChoiceItem(user.getId(), user.getDisplayName() + " • " + user.getEmail()))
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
                List<ChoiceItem> loadedProductChoices = productService.getAll().stream()
                        .map(product -> new ChoiceItem(product.getId(), product.getName() + " • stock " + product.getStock()))
                        .sorted(Comparator.comparing(ChoiceItem::toString, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                List<ChoiceItem> loadedCoachChoices = userService.getAll().stream()
                        .filter(user -> user.hasRole(UserRoles.ROLE_ENTRAINEUR))
                        .map(user -> new ChoiceItem(user.getId(), user.getDisplayName() + " • " + user.getEmail()))
                        .sorted(Comparator.comparing(ChoiceItem::toString, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                List<Order> filteredOrders = filterAndSort(
                        orderService.getAll(),
                        loadedProductChoices,
                        loadedCoachChoices,
                        keyword,
                        sortBy,
                        sortDirection
                );
                return new RefreshPayload(loadedProductChoices, loadedCoachChoices, filteredOrders);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshSequence.get()) {
                return;
            }
            RefreshPayload payload = loadTask.getValue();
            productChoices.setAll(payload.productChoices());
            coachChoices.setAll(payload.coachChoices());
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
            List<ChoiceItem> coachChoiceSnapshot,
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
                .filter(order -> matchesKeyword(order, keyword, productChoiceSnapshot, coachChoiceSnapshot))
                .sorted(comparator.thenComparing(Order::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean matchesKeyword(Order order, String keyword, List<ChoiceItem> productChoiceSnapshot, List<ChoiceItem> coachChoiceSnapshot) {
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(order.getId(), normalized)
                || contains(order.getStatus(), normalized)
                || contains(order.getPaymentMethod(), normalized)
                || contains(order.getPaymentStatus(), normalized)
                || contains(order.getContactEmail(), normalized)
                || contains(resolveChoiceLabel(productChoiceSnapshot, order.getProductId(), "Produit"), normalized)
                || contains(resolveChoiceLabel(coachChoiceSnapshot, order.getEntraineurId(), "Coach"), normalized);
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Order buildOrderFromForm(boolean updateMode) {
        clearValidation();
        ChoiceItem product = productComboBox.getValue();
        ChoiceItem coach = coachComboBox.getValue();
        if (product == null) {
            markFieldInvalid(productComboBox);
            showValidation("Selectionnez un produit.");
            return null;
        }
        if (coach == null) {
            markFieldInvalid(coachComboBox);
            showValidation("Selectionnez un coach.");
            return null;
        }

        Order order = new Order();
        if (updateMode && selectedOrder != null) {
            order.setId(selectedOrder.getId());
        }
        order.setProductId(product.id);
        order.setEntraineurId(coach.id);
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
        selectChoice(coachComboBox, coachChoices, order.getEntraineurId());
        orderDatePicker.setValue(order.getOrderDate());
        statusComboBox.setValue(emptyIfNull(order.getStatus(), "PENDING"));
        paymentMethodComboBox.setValue(emptyIfNull(order.getPaymentMethod(), "CARD"));
        paymentStatusComboBox.setValue(emptyIfNull(order.getPaymentStatus(), "UNPAID"));
        quantityField.setText(String.valueOf(order.getQuantity()));
        sizeField.setText(emptyIfNull(order.getSize(), ""));
        contactEmailField.setText(emptyIfNull(order.getContactEmail(), ""));
        contactPhoneField.setText(emptyIfNull(order.getContactPhone(), ""));
        totalAmountField.setText(order.getTotalAmount() == null ? "" : order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        shippingAddressField.setText(emptyIfNull(order.getShippingAddress(), ""));
        billingAddressField.setText(emptyIfNull(order.getBillingAddress(), ""));
        formModeLabel.setText("Modifier la commande");
        formHintLabel.setText("Les modifications seront appliquees a la commande selectionnee.");
    }

    private void confirmAndDelete(Order order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la commande #" + order.getId() + " ?", ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            orderService.delete(order.getId());
            selectedOrder = null;
            clearFormFields();
            refreshOrders(null, "Commande supprimee avec succes.", "status-success");
        } catch (SQLException | IllegalArgumentException exception) {
            showAlert(Alert.AlertType.ERROR, "Suppression", resolveMessage(exception));
        }
    }

    private void selectOrder(Order order) {
        selectedOrder = order;
        updateDetailPanel();
        updateActionAvailability();
        selectionStateLabel.setText(order == null ? "Selectionnez une commande" : "Commande #" + order.getId());
    }

    private void selectOrderById(Integer id) {
        if (id == null) {
            return;
        }
        orders.stream().filter(order -> id.equals(order.getId())).findFirst().ifPresent(this::selectOrder);
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
    }

    private void updateCharts() {
        Map<String, Long> statusCounts = orders.stream().collect(Collectors.groupingBy(order -> emptyIfNull(order.getStatus(), "PENDING"), Collectors.counting()));
        Map<String, Long> paymentCounts = orders.stream().collect(Collectors.groupingBy(order -> emptyIfNull(order.getPaymentStatus(), "UNKNOWN"), Collectors.counting()));

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

        statusChartSummaryLabel.setText(orders.size() + " visible order(s) | " + statusCounts);
        paymentChartSummaryLabel.setText(paymentCounts.isEmpty() ? "Aucune donnee de paiement." : paymentCounts.toString());
        Platform.runLater(() -> applyPieChartTheme(paymentChart));
    }

    private void updateDetailPanel() {
        if (selectedOrder == null) {
            detailBadgeLabel.setText("Apercu");
            detailTitleLabel.setText("Aucune commande selectionnee");
            detailSubtitleLabel.setText("Selectionnez une commande");
            detailTotalLabel.setText("0.00 DT");
            detailPaymentChipLabel.setText("-");
            detailIdValueLabel.setText("Auto");
            detailProductValueLabel.setText("-");
            detailCoachValueLabel.setText("-");
            detailDateValueLabel.setText("-");
            detailQuantityValueLabel.setText("-");
            detailEmailValueLabel.setText("-");
            detailPhoneValueLabel.setText("-");
            detailShippingValueLabel.setText("-");
            detailBillingValueLabel.setText("-");
            return;
        }

        detailBadgeLabel.setText(emptyIfNull(selectedOrder.getStatus()));
        detailTitleLabel.setText(resolveChoiceLabel(productChoices, selectedOrder.getProductId(), "Produit"));
        detailSubtitleLabel.setText(resolveChoiceLabel(coachChoices, selectedOrder.getEntraineurId(), "Coach") + " • " + formatDate(selectedOrder.getOrderDate()));
        detailTotalLabel.setText(formatPrice(selectedOrder.getTotalAmount()));
        detailPaymentChipLabel.setText(emptyIfNull(selectedOrder.getPaymentStatus()));
        applyStatusStyle(detailPaymentChipLabel, selectedOrder.getPaymentStatus(), true);
        detailIdValueLabel.setText(String.valueOf(selectedOrder.getId()));
        detailProductValueLabel.setText(resolveChoiceLabel(productChoices, selectedOrder.getProductId(), "Produit"));
        detailCoachValueLabel.setText(resolveChoiceLabel(coachChoices, selectedOrder.getEntraineurId(), "Coach"));
        detailDateValueLabel.setText(formatDate(selectedOrder.getOrderDate()));
        detailQuantityValueLabel.setText(String.valueOf(selectedOrder.getQuantity()));
        detailEmailValueLabel.setText(emptyIfNull(selectedOrder.getContactEmail()));
        detailPhoneValueLabel.setText(emptyIfNull(selectedOrder.getContactPhone()));
        detailShippingValueLabel.setText(emptyIfNull(selectedOrder.getShippingAddress()));
        detailBillingValueLabel.setText(emptyIfNull(selectedOrder.getBillingAddress()));
    }

    private Label createStatusChip(String value, boolean paymentMode) {
        Label label = new Label(emptyIfNull(value));
        applyStatusStyle(label, value, paymentMode);
        return label;
    }

    private void applyStatusStyle(Label label, String value, boolean paymentMode) {
        label.getStyleClass().setAll("status-pill", "product-stock-chip");
        String normalized = emptyIfNull(value, "").toUpperCase(Locale.ROOT);
        if (paymentMode) {
            if ("PAID".equals(normalized)) {
                label.getStyleClass().add("product-stock-good");
            } else if ("FAILED".equals(normalized) || "REFUNDED".equals(normalized)) {
                label.getStyleClass().add("product-stock-out");
            } else {
                label.getStyleClass().add("product-stock-low");
            }
            return;
        }
        if ("DELIVERED".equals(normalized) || "CONFIRMED".equals(normalized)) {
            label.getStyleClass().add("product-stock-good");
        } else if ("CANCELLED".equals(normalized)) {
            label.getStyleClass().add("product-stock-out");
        } else {
            label.getStyleClass().add("product-stock-low");
        }
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
        coachComboBox.getSelectionModel().clearSelection();
        orderDatePicker.setValue(LocalDate.now());
        statusComboBox.setValue("PENDING");
        paymentMethodComboBox.setValue("CARD");
        paymentStatusComboBox.setValue("UNPAID");
        quantityField.clear();
        sizeField.clear();
        contactEmailField.clear();
        contactPhoneField.clear();
        totalAmountField.clear();
        shippingAddressField.clear();
        billingAddressField.clear();
    }

    private void updateActionAvailability() {
        boolean hasSelection = selectedOrder != null;
        updateButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        detailEditButton.setDisable(!hasSelection);
        detailDeleteButton.setDisable(!hasSelection);
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
        for (Control control : List.of(productComboBox, coachComboBox, quantityField, totalAmountField, contactEmailField, contactPhoneField, shippingAddressField, billingAddressField)) {
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

    private Integer getSelectedOrderId() {
        return selectedOrder == null ? null : selectedOrder.getId();
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

    private record RefreshPayload(List<ChoiceItem> productChoices, List<ChoiceItem> coachChoices, List<Order> orders) {
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
