package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.ProductImageResolver;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.repositories.ProductRepository;
import tn.esprit.services.OrderService;
import tn.esprit.services.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StoreController {
    private static final String LIGHT_PAGE_BACKGROUND = "-fx-background-color:"
            + " radial-gradient(center 12% 12%, radius 34%, rgba(16, 185, 129, 0.14) 0%, rgba(16, 185, 129, 0) 100%),"
            + " radial-gradient(center 86% 14%, radius 30%, rgba(59, 130, 246, 0.12) 0%, rgba(59, 130, 246, 0) 100%),"
            + " linear-gradient(from 0% 0% to 100% 100%, #f8fffb 0%, #f0fdf4 38%, #f8fafc 100%);";
    private static final String DARK_PAGE_BACKGROUND = "-fx-background-color:"
            + " radial-gradient(center 12% 12%, radius 34%, rgba(16, 185, 129, 0.12) 0%, rgba(16, 185, 129, 0) 100%),"
            + " radial-gradient(center 86% 14%, radius 30%, rgba(59, 130, 246, 0.10) 0%, rgba(59, 130, 246, 0) 100%),"
            + " linear-gradient(from 0% 0% to 100% 100%, #071019 0%, #0f172a 48%, #111827 100%);";
    private static final int LOW_STOCK_THRESHOLD = 5;

    @FXML private BorderPane pageRoot;
    @FXML private ScrollPane pageScroll;
    @FXML private StackPane pageShellWrap;
    @FXML private VBox pageShell;
    @FXML private HBox navbarRoot;
    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private Button matchsNavButton;
    @FXML private HBox sidebarModuleChildrenBox;
    @FXML private Button equipesNavButton;
    @FXML private Button leaguesNavButton;
    @FXML private Button joueursNavButton;
    @FXML private Button annonceNavButton;
    @FXML private Button productNavButton;
    @FXML private ToggleButton themeToggleButton;

    @FXML private Label catalogMetricLabel;
    @FXML private Label cartMetricLabel;
    @FXML private Label checkoutMetricLabel;
    @FXML private Label resultCountLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;
    @FXML private Label statusLabel;
    @FXML private Label selectionStateLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<ProductRepository.ProductSortField> sortByComboBox;
    @FXML private ComboBox<ProductRepository.SortDirection> sortDirectionComboBox;

    @FXML private Button catalogSectionButton;
    @FXML private Button cartSectionButton;
    @FXML private Button paymentSectionButton;
    @FXML private VBox catalogSection;
    @FXML private VBox cartSection;
    @FXML private VBox paymentSection;
    @FXML private FlowPane productsPane;
    @FXML private VBox cartItemsPane;
    @FXML private VBox paymentItemsPane;
    @FXML private Label paymentTotalLabel;
    @FXML private Label validationLabel;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea shippingAddressArea;
    @FXML private TextArea billingAddressArea;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private Button payNowButton;

    private final List<Product> visibleProducts = new ArrayList<>();
    private final List<CartLine> cartItems = new ArrayList<>();
    private final Map<String, Image> imageCache = new HashMap<>();

    private ProductService productService;
    private OrderService orderService;
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean serviceReady;
    private StoreSection activeSection = StoreSection.CATALOG;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        configureNavigation();
        applyThemeState(themeToggleButton != null && themeToggleButton.isSelected());
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, oldValue, selected) -> applyThemeState(selected));
        }
        if (pageScroll != null) {
            pageScroll.skinProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
            pageScroll.sceneProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
        }
        configureFilters();
        configurePaymentMethods();
        updateSectionVisibility(StoreSection.CATALOG);
        Platform.runLater(this::forceScrollPaneViewportTransparent);

        try {
            productService = new ProductService();
            orderService = new OrderService();
            serviceReady = true;
            refreshCatalog();
            setStatus("Boutique prete.", "status-success");
        } catch (SQLException exception) {
            serviceReady = false;
            setStatus("Boutique indisponible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Store", exception.getMessage());
        }
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
        refreshCatalog();
    }

    @FXML
    private void handleRefreshCatalog() {
        refreshCatalog();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        sortByComboBox.setValue(ProductRepository.ProductSortField.NAME);
        sortDirectionComboBox.setValue(ProductRepository.SortDirection.ASC);
        refreshCatalog();
    }

    @FXML
    private void handleShowCatalog() {
        updateSectionVisibility(StoreSection.CATALOG);
    }

    @FXML
    private void handleShowCart() {
        updateSectionVisibility(StoreSection.CART);
    }

    @FXML
    private void handleShowPayment() {
        updateSectionVisibility(StoreSection.PAYMENT);
    }

    @FXML
    private void handleCopyShippingToBilling() {
        billingAddressArea.setText(shippingAddressArea.getText());
    }

    @FXML
    private void handlePayNow() {
        clearValidation();
        if (!serviceReady || orderService == null || productService == null) {
            showValidation("Le service store est indisponible.");
            return;
        }
        if (cartItems.isEmpty()) {
            showValidation("Votre panier est vide.");
            updateSectionVisibility(StoreSection.CART);
            return;
        }

        Map<String, String> errors = validateCheckoutForm();
        if (!errors.isEmpty()) {
            applyCheckoutErrors(errors);
            updateSectionVisibility(StoreSection.PAYMENT);
            return;
        }

        String email = trimToNull(emailField.getText());
        String phone = trimToNull(phoneField.getText());
        String shipping = trimToNull(shippingAddressArea.getText());
        String billing = trimToNull(billingAddressArea.getText());
        String paymentMethod = paymentMethodComboBox.getValue();

        try {
            Map<Integer, Product> latestProducts = new HashMap<>();
            for (CartLine line : cartItems) {
                Product freshProduct = productService.getById(line.getProduct().getId());
                if (freshProduct == null) {
                    throw new IllegalArgumentException("Un produit du panier n'existe plus.");
                }
                if (freshProduct.getStock() < line.getQuantity()) {
                    throw new IllegalArgumentException("Stock insuffisant pour " + freshProduct.getName() + ".");
                }
                latestProducts.put(freshProduct.getId(), freshProduct);
            }

            for (CartLine line : cartItems) {
                Product freshProduct = latestProducts.get(line.getProduct().getId());
                Order order = new Order(
                        line.getQuantity(),
                        LocalDate.now(),
                        null,
                        paymentMethod,
                        null,
                        freshProduct.getSize(),
                        email,
                        phone,
                        shipping,
                        billing,
                        null,
                        freshProduct.getId(),
                        null
                );
                orderService.add(order);
            }

            BigDecimal total = calculateCartTotal();
            cartItems.clear();
            clearCheckoutForm();
            refreshCatalog();
            renderCart();
            renderPaymentSummary();
            updateSectionVisibility(StoreSection.CATALOG);
            setStatus("Commande enregistree avec succes.", "status-success");
            showAlert(Alert.AlertType.INFORMATION, "Paiement", "Commande validee pour " + formatPrice(total) + ".");
        } catch (IllegalArgumentException | SQLException exception) {
            setStatus("Paiement impossible.", "status-error");
            showValidation(exception.getMessage());
        }
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

    private void applyThemeState(boolean darkMode) {
        if (pageRoot != null) {
            pageRoot.getStyleClass().removeAll("store-user-dark", "store-user-light");
            pageRoot.getStyleClass().add(darkMode ? "store-user-dark" : "store-user-light");
            pageRoot.setStyle(darkMode ? DARK_PAGE_BACKGROUND : LIGHT_PAGE_BACKGROUND);
        }
        forceTransparentShell(pageScroll);
        forceTransparentShell(pageShellWrap);
        forceTransparentShell(pageShell);
        Platform.runLater(this::forceScrollPaneViewportTransparent);
    }

    private void configureFilters() {
        sortByComboBox.setItems(FXCollections.observableArrayList(ProductRepository.ProductSortField.values()));
        sortDirectionComboBox.setItems(FXCollections.observableArrayList(ProductRepository.SortDirection.values()));
        sortByComboBox.setValue(ProductRepository.ProductSortField.NAME);
        sortDirectionComboBox.setValue(ProductRepository.SortDirection.ASC);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCatalog());
        sortByComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshCatalog());
        sortDirectionComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshCatalog());
    }

    private void configurePaymentMethods() {
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(OrderService.allowedPaymentMethods()));
        if (!paymentMethodComboBox.getItems().isEmpty()) {
            paymentMethodComboBox.setValue(paymentMethodComboBox.getItems().get(0));
        }
    }

    private void refreshCatalog() {
        if (!serviceReady || productService == null) {
            return;
        }

        try {
            visibleProducts.clear();
            visibleProducts.addAll(productService.findProducts(
                    searchField == null ? null : searchField.getText(),
                    sortByComboBox.getValue(),
                    sortDirectionComboBox.getValue()
            ));
            syncCartWithCatalog();
            renderProducts();
            renderCart();
            renderPaymentSummary();
            updateCartMetrics();
            setStatus(visibleProducts.size() + " produit(s) disponibles.", "status-muted");
        } catch (SQLException exception) {
            setStatus("Chargement impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Store", exception.getMessage());
        }
    }

    private void syncCartWithCatalog() {
        Map<Integer, Product> visibleById = visibleProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, this::copyProduct, (left, right) -> right));

        cartItems.removeIf(line -> {
            Product product = visibleById.get(line.getProduct().getId());
            if (product == null) {
                return true;
            }
            line.setProduct(product);
            if (line.getQuantity() > product.getStock()) {
                line.setQuantity(Math.max(product.getStock(), 0));
            }
            return line.getQuantity() <= 0;
        });
    }

    private void renderProducts() {
        productsPane.getChildren().clear();
        if (visibleProducts.isEmpty()) {
            productsPane.getChildren().add(buildEmptyCard("Aucun produit", "Aucun produit ne correspond aux filtres actuels."));
            return;
        }

        for (Product product : visibleProducts) {
            productsPane.getChildren().add(buildProductCard(product));
        }
    }

    private VBox buildProductCard(Product product) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("panel-card", "store-product-card");
        card.setPrefWidth(280);

        card.getChildren().add(createImageShell(product.getImage(), product.getName(), 240, 170, "store-product-image-shell"));

        Label title = new Label(emptyIfNull(product.getName(), "Produit"));
        title.getStyleClass().add("store-product-title");
        title.setWrapText(true);

        Label subtitle = new Label(buildProductSubtitle(product));
        subtitle.getStyleClass().add("store-product-subtitle");
        subtitle.setWrapText(true);

        Label price = new Label(formatPrice(product.getPrice()));
        price.getStyleClass().add("store-product-price");

        Label stock = new Label(product.getStock() > 0 ? product.getStock() + " en stock" : "Rupture de stock");
        stock.getStyleClass().addAll("store-stock-badge", resolveStockStyle(product.getStock()));

        Button addButton = new Button(product.getStock() > 0 ? "Ajouter au panier" : "Indisponible");
        addButton.getStyleClass().add(product.getStock() > 0 ? "primary-button" : "ghost-button");
        addButton.setDisable(product.getStock() <= 0);
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> addProductToCart(product));

        card.getChildren().addAll(title, subtitle, price, stock, addButton);
        VBox.setVgrow(addButton, Priority.NEVER);
        return card;
    }

    private void renderCart() {
        cartItemsPane.getChildren().clear();
        if (cartItems.isEmpty()) {
            cartItemsPane.getChildren().add(buildEmptyCard("Panier vide", "Ajoutez des produits depuis le catalogue pour commencer."));
            return;
        }

        for (CartLine line : cartItems) {
            cartItemsPane.getChildren().add(buildCartCard(line));
        }
    }

    private VBox buildCartCard(CartLine line) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("panel-card", "store-cart-card");

        HBox header = new HBox(12);
        header.getChildren().add(createImageShell(line.getProduct().getImage(), line.getProduct().getName(), 110, 86, "store-cart-image-shell"));

        VBox textBox = new VBox(6);
        Label title = new Label(emptyIfNull(line.getProduct().getName(), "Produit"));
        title.getStyleClass().add("store-cart-title");
        Label subtitle = new Label(buildProductSubtitle(line.getProduct()));
        subtitle.getStyleClass().add("store-product-subtitle");
        subtitle.setWrapText(true);
        Label subtotal = new Label(formatPrice(line.getSubtotal()));
        subtotal.getStyleClass().add("store-product-price");
        textBox.getChildren().addAll(title, subtitle, subtotal);

        header.getChildren().add(textBox);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button minusButton = new Button("-");
        minusButton.getStyleClass().add("soft-button");
        minusButton.setOnAction(event -> updateCartQuantity(line, line.getQuantity() - 1));

        Label quantityLabel = new Label(String.valueOf(line.getQuantity()));
        quantityLabel.getStyleClass().addAll("store-meta-chip", "store-quantity-label");

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("soft-button");
        plusButton.setDisable(line.getQuantity() >= line.getProduct().getStock());
        plusButton.setOnAction(event -> updateCartQuantity(line, line.getQuantity() + 1));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeButton = new Button("Retirer");
        removeButton.getStyleClass().add("danger-button");
        removeButton.setOnAction(event -> removeFromCart(line));

        actions.getChildren().addAll(minusButton, quantityLabel, plusButton, spacer, removeButton);
        card.getChildren().addAll(header, actions);
        return card;
    }

    private void renderPaymentSummary() {
        paymentItemsPane.getChildren().clear();
        if (cartItems.isEmpty()) {
            paymentItemsPane.getChildren().add(buildEmptyCard("Aucun article", "Le resume du paiement apparaitra ici."));
        } else {
            for (CartLine line : cartItems) {
                HBox row = new HBox(10);
                row.getStyleClass().add("store-payment-item-row");
                Label title = new Label(emptyIfNull(line.getProduct().getName(), "Produit") + " x" + line.getQuantity());
                title.getStyleClass().add("store-payment-item-title");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label amount = new Label(formatPrice(line.getSubtotal()));
                amount.getStyleClass().add("store-payment-item-amount");
                row.getChildren().addAll(title, spacer, amount);
                paymentItemsPane.getChildren().add(row);
            }
        }
        paymentTotalLabel.setText(formatPrice(calculateCartTotal()));
    }

    private void addProductToCart(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        CartLine existingLine = findCartLine(product.getId());
        if (existingLine == null) {
            cartItems.add(new CartLine(copyProduct(product), 1));
        } else if (existingLine.getQuantity() < product.getStock()) {
            existingLine.setQuantity(existingLine.getQuantity() + 1);
        }
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
        setStatus("Produit ajoute au panier.", "status-success");
    }

    private void updateCartQuantity(CartLine line, int quantity) {
        if (line == null) {
            return;
        }
        if (quantity <= 0) {
            cartItems.remove(line);
        } else {
            int maxQuantity = Math.max(line.getProduct().getStock(), 1);
            line.setQuantity(Math.min(quantity, maxQuantity));
        }
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
    }

    private void removeFromCart(CartLine line) {
        cartItems.remove(line);
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
        setStatus("Produit retire du panier.", "status-muted");
    }

    private void updateCartMetrics() {
        int quantity = calculateCartQuantity();
        String total = formatPrice(calculateCartTotal());

        catalogMetricLabel.setText(String.valueOf(visibleProducts.size()));
        cartMetricLabel.setText(String.valueOf(quantity));
        checkoutMetricLabel.setText(total);
        resultCountLabel.setText(visibleProducts.size() + " produit(s)");
        cartCountLabel.setText(quantity + " article(s)");
        cartTotalLabel.setText(total);
        paymentTotalLabel.setText(total);
        payNowButton.setDisable(cartItems.isEmpty());
    }

    private int calculateCartQuantity() {
        return cartItems.stream().mapToInt(CartLine::getQuantity).sum();
    }

    private BigDecimal calculateCartTotal() {
        return cartItems.stream()
                .map(CartLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void updateSectionVisibility(StoreSection section) {
        activeSection = section;
        setSectionState(catalogSection, section == StoreSection.CATALOG);
        setSectionState(cartSection, section == StoreSection.CART);
        setSectionState(paymentSection, section == StoreSection.PAYMENT);

        setSectionButtonState(catalogSectionButton, section == StoreSection.CATALOG);
        setSectionButtonState(cartSectionButton, section == StoreSection.CART);
        setSectionButtonState(paymentSectionButton, section == StoreSection.PAYMENT);

        selectionStateLabel.setText(switch (section) {
            case CATALOG -> "Mode catalogue";
            case CART -> "Mode panier";
            case PAYMENT -> "Mode paiement";
        });
    }

    private void setSectionState(VBox section, boolean active) {
        section.setVisible(active);
        section.setManaged(active);
    }

    private void setSectionButtonState(Button button, boolean active) {
        button.getStyleClass().remove("store-section-button-active");
        if (active && !button.getStyleClass().contains("store-section-button-active")) {
            button.getStyleClass().add("store-section-button-active");
        }
    }

    private void clearCheckoutForm() {
        emailField.clear();
        phoneField.clear();
        shippingAddressArea.clear();
        billingAddressArea.clear();
        if (!paymentMethodComboBox.getItems().isEmpty()) {
            paymentMethodComboBox.setValue(paymentMethodComboBox.getItems().get(0));
        }
    }

    private Map<String, String> validateCheckoutForm() {
        Map<String, String> errors = new HashMap<>();
        String email = trimToNull(emailField.getText());
        String phone = trimToNull(phoneField.getText());
        String shipping = trimToNull(shippingAddressArea.getText());
        String billing = trimToNull(billingAddressArea.getText());

        if (email == null) {
            errors.put("email", "L'email est obligatoire.");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.put("email", "L'email n'est pas valide.");
        }

        if (phone == null) {
            errors.put("phone", "Le telephone est obligatoire.");
        } else if (!phone.replaceAll("[\\s().-]", "").matches("^\\+?[0-9]{8,20}$")) {
            errors.put("phone", "Le telephone doit contenir entre 8 et 20 chiffres.");
        }

        if (shipping == null || shipping.length() < 10) {
            errors.put("shipping", "L'adresse de livraison doit contenir au moins 10 caracteres.");
        }

        if (billing == null || billing.length() < 10) {
            errors.put("billing", "L'adresse de facturation doit contenir au moins 10 caracteres.");
        }

        if (paymentMethodComboBox.getValue() == null) {
            errors.put("paymentMethod", "Le mode de paiement est obligatoire.");
        }

        return errors;
    }

    private void applyCheckoutErrors(Map<String, String> errors) {
        clearFieldError(emailField);
        clearFieldError(phoneField);
        clearFieldError(shippingAddressArea);
        clearFieldError(billingAddressArea);
        clearFieldError(paymentMethodComboBox);

        if (errors.containsKey("email")) {
            markFieldInvalid(emailField);
        }
        if (errors.containsKey("phone")) {
            markFieldInvalid(phoneField);
        }
        if (errors.containsKey("shipping")) {
            markFieldInvalid(shippingAddressArea);
        }
        if (errors.containsKey("billing")) {
            markFieldInvalid(billingAddressArea);
        }
        if (errors.containsKey("paymentMethod")) {
            markFieldInvalid(paymentMethodComboBox);
        }

        showValidation(errors.values().stream().collect(Collectors.joining("\n")));
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
        clearFieldError(emailField);
        clearFieldError(phoneField);
        clearFieldError(shippingAddressArea);
        clearFieldError(billingAddressArea);
        clearFieldError(paymentMethodComboBox);
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private VBox buildEmptyCard(String titleText, String bodyText) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("panel-card", "store-empty-card");
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Label body = new Label(bodyText);
        body.getStyleClass().add("section-subtitle");
        body.setWrapText(true);
        box.getChildren().addAll(title, body);
        return box;
    }

    private StackPane createImageShell(String imagePath, String label, double width, double height, String styleClass) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add(styleClass);
        shell.setMinSize(width, height);
        shell.setPrefSize(width, height);
        shell.setMaxSize(width, height);

        Image image = loadImage(imagePath);
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(width - 10);
            view.setFitHeight(height - 10);
            view.setPreserveRatio(true);
            shell.getChildren().add(view);
        } else {
            Label placeholder = new Label(buildPlaceholderLabel(label));
            placeholder.getStyleClass().add("store-image-placeholder");
            shell.getChildren().add(placeholder);
        }
        return shell;
    }

    private Image loadImage(String imagePath) {
        String normalized = trimToNull(imagePath);
        String cacheKey = normalized == null ? "__default__" : normalized;
        return imageCache.computeIfAbsent(cacheKey, ignored -> resolveImage(normalized));
    }

    private Image resolveImage(String imagePath) {
        return ProductImageResolver.loadImage(getClass(), imagePath);
    }

    private String buildProductSubtitle(Product product) {
        List<String> parts = new ArrayList<>();
        if (trimToNull(product.getCategory()) != null) {
            parts.add(product.getCategory().trim());
        }
        if (trimToNull(product.getBrand()) != null) {
            parts.add(product.getBrand().trim());
        }
        if (trimToNull(product.getSize()) != null) {
            parts.add("Taille " + product.getSize().trim());
        }
        return parts.isEmpty() ? "Produit store" : String.join(" • ", parts);
    }

    private Product copyProduct(Product product) {
        return new Product(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getSize(),
                product.getBrand(),
                product.getImage()
        );
    }

    private CartLine findCartLine(Integer productId) {
        for (CartLine line : cartItems) {
            if (productId != null && productId.equals(line.getProduct().getId())) {
                return line;
            }
        }
        return null;
    }

    private String buildPlaceholderLabel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "IMG";
        }
        return normalized.substring(0, Math.min(3, normalized.length())).toUpperCase();
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(emptyIfNull(message, "Boutique prete"));
        statusLabel.getStyleClass().setAll("status-pill", styleClass == null ? "status-muted" : styleClass);
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " DT";
    }

    private String resolveStockStyle(int stock) {
        if (stock <= 0) {
            return "store-stock-badge-out";
        }
        if (stock <= LOW_STOCK_THRESHOLD) {
            return "store-stock-badge-low";
        }
        return "store-stock-badge-good";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String emptyIfNull(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private void forceTransparentShell(Node node) {
        if (node == null) {
            return;
        }
        node.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }

    private void forceScrollPaneViewportTransparent() {
        if (pageScroll == null) {
            return;
        }
        forceTransparentShell(pageScroll.lookup(".viewport"));
        forceTransparentShell(pageScroll.lookup(".content"));
    }

    private Node resolveNavigationSource(Node preferred, Node fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private enum StoreSection {
        CATALOG,
        CART,
        PAYMENT
    }

    private static final class CartLine {
        private Product product;
        private int quantity;

        private CartLine(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
