package tn.esprit.Controller;

import com.stripe.exception.StripeException;
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
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.ProductImageResolver;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.repositories.ProductRepository;
import tn.esprit.security.AuthSession;
import tn.esprit.services.OrderPdfExportService;
import tn.esprit.services.OrderIntelligenceService;
import tn.esprit.services.OrderService;
import tn.esprit.services.OrderWorkflowNotificationService;
import tn.esprit.services.CurrencyConversionService;
import tn.esprit.services.ProductAiService;
import tn.esprit.services.ProductAnalyticsService;
import tn.esprit.services.ProductService;
import tn.esprit.services.StoreCartSession;
import tn.esprit.services.StripeCheckoutService;
import tn.esprit.services.StripeCheckoutSessionStore;

import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class StoreController {
    private static final DateTimeFormatter INVOICE_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int STRIPE_AUTO_VERIFY_MAX_ATTEMPTS = 60;
    private static final long STRIPE_AUTO_VERIFY_DELAY_MS = 2500L;
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
    @FXML private Region heroPhotoRegion;
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
    @FXML private Label searchSummaryLabel;
    @FXML private ComboBox<ProductRepository.ProductSortField> sortByComboBox;
    @FXML private ComboBox<ProductRepository.SortDirection> sortDirectionComboBox;
    @FXML private Button trendingFilterButton;

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
    @FXML private Label paymentFlowDescriptionLabel;
    @FXML private Label paymentMethodHintLabel;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea shippingAddressArea;
    @FXML private TextArea billingAddressArea;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private Button payNowButton;

    private final List<Product> visibleProducts = new ArrayList<>();
    private final List<Product> catalogProducts = new ArrayList<>();
    private final List<CartLine> cartItems = new ArrayList<>();
    private final List<Order> orderHistorySnapshot = new ArrayList<>();
    private final Map<String, Image> imageCache = new HashMap<>();

    private ProductService productService;
    private OrderService orderService;
    private OrderIntelligenceService orderIntelligenceService;
    private OrderPdfExportService orderPdfExportService;
    private ProductAiService productAiService;
    private ProductAnalyticsService productAnalyticsService;
    private OrderWorkflowNotificationService workflowNotificationService;
    private StripeCheckoutService stripeCheckoutService;
    private CurrencyConversionService currencyConversionService;
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean serviceReady;
    private StoreSection activeSection = StoreSection.CATALOG;
    private boolean trendingFilterActive;
    private final Map<Integer, ProductAnalyticsService.TrendingProductSnapshot> trendingByProductId = new HashMap<>();
    private final AtomicLong stripeHintRequestId = new AtomicLong();
    private final AtomicLong stripeAutoVerifySequence = new AtomicLong();
    private final Object stripeCompletionLock = new Object();
    private String completingStripeSessionId;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        configureNavigation();
        configureHeroImage();
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
        prefillCheckoutFromSession();
        restorePendingStripeCheckout();
        updateTrendingButtonState();
        updateSectionVisibility(StoreSection.CATALOG);
        updatePaymentMethodState();
        Platform.runLater(this::forceScrollPaneViewportTransparent);

        try {
            productService = new ProductService();
            orderService = new OrderService();
            orderIntelligenceService = new OrderIntelligenceService();
            orderPdfExportService = new OrderPdfExportService();
            productAiService = new ProductAiService();
            productAnalyticsService = new ProductAnalyticsService();
            workflowNotificationService = new OrderWorkflowNotificationService();
            stripeCheckoutService = new StripeCheckoutService();
            currencyConversionService = new CurrencyConversionService();
            serviceReady = true;
            restoreCartSession();
            refreshCatalog();
            updatePaymentMethodState();
            setStatus("Boutique prete.", "status-success");
        } catch (SQLException exception) {
            serviceReady = false;
            setStatus("Boutique indisponible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Store", exception.getMessage());
        }
    }

    private void configureHeroImage() {
        if (heroPhotoRegion == null) {
            return;
        }
        var imageUrl = getClass().getResource("/tn/esprit/images/store.png");
        if (imageUrl == null) {
            return;
        }
        BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, true);
        BackgroundImage backgroundImage = new BackgroundImage(
                new Image(imageUrl.toExternalForm()),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                backgroundSize
        );
        heroPhotoRegion.setBackground(new Background(backgroundImage));
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
        trendingFilterActive = false;
        updateTrendingButtonState();
        refreshCatalog();
    }

    @FXML
    private void handleToggleTrending() {
        trendingFilterActive = !trendingFilterActive;
        updateTrendingButtonState();
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
        CheckoutFormSnapshot checkout = validateAndCaptureCheckout();
        if (checkout == null) {
            return;
        }

        try {
            PreparedCheckout preparedCheckout = prepareCheckout(cartItems);
            if (isStripePayment(checkout.paymentMethod())) {
                handleStripeCheckout(checkout, preparedCheckout);
                return;
            }
            finalizePaidCheckout(checkout, preparedCheckout, checkout.paymentMethod());
        } catch (IllegalArgumentException | SQLException exception) {
            notifyPaymentFailed(calculateCartTotal(), exception.getMessage());
            setStatus("Paiement impossible.", "status-error");
            showValidation(exception.getMessage());
        } catch (IOException exception) {
            notifyPaymentSucceeded(cartItems.size(), calculateCartTotal(), null);
            clearStoreAfterSuccessfulPayment();
            setStatus("Commande enregistree, facture PDF indisponible.", "status-warning");
            showAlert(Alert.AlertType.WARNING, "Facture", "La commande a ete enregistree, mais la facture PDF n'a pas pu etre generee.\n" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            notifyPaymentFailed(calculateCartTotal(), "Le service Stripe n'a pas repondu.");
            setStatus("Paiement Stripe indisponible.", "status-error");
            showValidation("Le service Stripe n'a pas repondu. Reessayez dans quelques instants.");
        } catch (StripeException exception) {
            notifyPaymentFailed(calculateCartTotal(), exception.getMessage());
            setStatus("Paiement Stripe impossible.", "status-error");
            showValidation("Stripe a refuse la demande: " + emptyIfNull(trimToNull(exception.getMessage()), "erreur inconnue."));
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
        paymentMethodComboBox.valueProperty().addListener((obs, oldValue, newValue) -> handlePaymentMethodSelectionChanged(oldValue, newValue));
    }

    private void handlePaymentMethodSelectionChanged(String previousMethod, String selectedMethod) {
        if (isStripePayment(previousMethod) && !isStripePayment(selectedMethod)) {
            clearPendingStripeCheckout("Session Stripe abandonnee. Vous pouvez terminer en paiement a la livraison.", "status-muted");
        }
        updatePaymentMethodState();
        clearValidation();
    }

    private CheckoutFormSnapshot validateAndCaptureCheckout() {
        clearValidation();
        if (!serviceReady || orderService == null || productService == null) {
            showValidation("Le service store est indisponible.");
            return null;
        }
        if (cartItems.isEmpty()) {
            showValidation("Votre panier est vide.");
            updateSectionVisibility(StoreSection.CART);
            return null;
        }

        Map<String, String> errors = validateCheckoutForm();
        if (!errors.isEmpty()) {
            applyCheckoutErrors(errors);
            updateSectionVisibility(StoreSection.PAYMENT);
            return null;
        }

        User currentUser = AuthSession.getCurrentUser();
        Integer orderOwnerId = currentUser == null ? null : currentUser.getId();
        if (orderOwnerId == null || orderOwnerId <= 0) {
            showValidation("Votre session utilisateur est introuvable. Reconnectez-vous puis reessayez.");
            updateSectionVisibility(StoreSection.PAYMENT);
            return null;
        }

        return new CheckoutFormSnapshot(
                currentUser,
                orderOwnerId,
                trimToNull(emailField.getText()),
                trimToNull(phoneField.getText()),
                trimToNull(shippingAddressArea.getText()),
                trimToNull(billingAddressArea.getText()),
                paymentMethodComboBox.getValue()
        );
    }

    private PreparedCheckout prepareCheckout(List<CartLine> sourceLines) throws SQLException {
        List<PreparedCheckoutLine> lines = new ArrayList<>();
        List<OrderPdfExportService.InvoiceLine> invoiceLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartLine line : sourceLines) {
            Product freshProduct = productService.getById(line.getProduct().getId());
            if (freshProduct == null) {
                throw new IllegalArgumentException("Un produit du panier n'existe plus.");
            }
            if (freshProduct.getStock() < line.getQuantity()) {
                throw new IllegalArgumentException("Stock insuffisant pour " + freshProduct.getName() + ".");
            }

            BigDecimal unitPrice = freshProduct.getPrice() == null
                    ? BigDecimal.ZERO
                    : freshProduct.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            lines.add(new PreparedCheckoutLine(freshProduct, line.getQuantity(), subtotal));
            invoiceLines.add(new OrderPdfExportService.InvoiceLine(
                    freshProduct.getName(),
                    freshProduct.getSize(),
                    line.getQuantity(),
                    unitPrice,
                    subtotal
            ));
            total = total.add(subtotal);
        }

        return new PreparedCheckout(
                lines,
                invoiceLines,
                total.setScale(2, RoundingMode.HALF_UP),
                buildCartSignature(sourceLines)
        );
    }

    private void handleStripeCheckout(CheckoutFormSnapshot checkout, PreparedCheckout preparedCheckout)
            throws IOException, InterruptedException, StripeException, SQLException {
        if (stripeCheckoutService == null || !stripeCheckoutService.isConfigured()) {
            setStatus("Stripe n'est pas configure.", "status-error");
            showValidation("Ajoutez une cle Stripe valide dans stripe.local.properties, STRIPE_SECRET_KEY, ou STRIPE_API_KEY avant d'utiliser ce mode.");
            return;
        }

        StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout = StripeCheckoutSessionStore.get(checkout.userId());
        if (pendingCheckout != null && !Objects.equals(pendingCheckout.cartSignature(), preparedCheckout.cartSignature())) {
            clearPendingStripeCheckout("Le panier a change. Une nouvelle session Stripe est requise.", "status-warning");
            pendingCheckout = null;
        }

        if (pendingCheckout == null) {
            launchStripeCheckout(checkout, preparedCheckout);
            return;
        }

        verifyStripeCheckout(checkout, preparedCheckout, pendingCheckout);
    }

    private void launchStripeCheckout(CheckoutFormSnapshot checkout, PreparedCheckout preparedCheckout)
            throws IOException, InterruptedException, StripeException {
        StripeChargePreview chargePreview = resolveStripeChargePreview(preparedCheckout.total());
        if (chargePreview.usdAmount().compareTo(new BigDecimal("0.50")) < 0) {
            showValidation("Le montant Stripe converti est inferieur au minimum autorise par Stripe.");
            setStatus("Montant Stripe trop faible.", "status-error");
            return;
        }

        List<StripeCheckoutService.StripeCheckoutLine> stripeLines = new ArrayList<>();
        for (PreparedCheckoutLine line : preparedCheckout.lines()) {
            BigDecimal unitPriceUsd = resolveStripeChargePreview(line.product().getPrice()).usdAmount();
            stripeLines.add(new StripeCheckoutService.StripeCheckoutLine(
                    emptyIfNull(line.product().getName(), "Produit"),
                    buildStripeLineDescription(line.product()),
                    line.quantity(),
                    unitPriceUsd
            ));
        }

        StripeCheckoutService.CheckoutLaunch launch = stripeCheckoutService.createCheckoutSession(
                new StripeCheckoutService.StripeCheckoutRequest(
                        checkout.email(),
                        "store-" + checkout.userId() + "-" + System.currentTimeMillis(),
                        stripeLines
                )
        );

        StripeCheckoutSessionStore.save(checkout.userId(), new StripeCheckoutSessionStore.PendingStripeCheckout(
                launch.sessionId(),
                launch.checkoutUrl(),
                checkout.email(),
                checkout.phone(),
                checkout.shippingAddress(),
                checkout.billingAddress(),
                preparedCheckout.total(),
                "USD",
                chargePreview.usdAmount(),
                chargePreview.rateDate(),
                preparedCheckout.cartSignature(),
                LocalDateTime.now()
        ));
        updatePaymentMethodState();
        openStripeCheckoutInBrowser(launch.checkoutUrl(), chargePreview.usdAmount());
        startStripeAutoVerification(checkout, preparedCheckout, launch.sessionId());
        setStatus("Session Stripe ouverte dans le navigateur.", "status-success");
    }

    private void verifyStripeCheckout(
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout
    ) throws StripeException, IOException, SQLException {
        StripeCheckoutService.CheckoutVerification verification = stripeCheckoutService.verifyCheckoutSession(pendingCheckout.sessionId());
        switch (verification.status()) {
            case PAID -> completeVerifiedStripeCheckout(checkout, preparedCheckout, pendingCheckout.sessionId());
            case PENDING -> {
                setStatus("Paiement Stripe en attente.", "status-warning");
                showValidation("Terminez le paiement dans le navigateur Stripe. L'application verifiera automatiquement le paiement, ou vous pouvez cliquer a nouveau pour forcer la verification.");
                updatePaymentMethodState();
            }
            case FAILED -> {
                clearPendingStripeCheckout("La session Stripe a ete fermee sans paiement confirme.", "status-error");
                showValidation("Stripe indique un paiement incomplet. Relancez une nouvelle session Stripe.");
            }
            case EXPIRED -> {
                clearPendingStripeCheckout("La session Stripe a expire.", "status-warning");
                showValidation("La session Stripe a expire. Creez une nouvelle session pour payer.");
            }
        }
    }

    private void finalizePaidCheckout(
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            String paymentMethod
    ) throws SQLException, IOException {
        StripeCheckoutSessionStore.PendingStripeCheckout pendingStripeCheckout = isStripePayment(paymentMethod)
                ? StripeCheckoutSessionStore.get(checkout.userId())
                : null;
        String clientName = checkout.currentUser() == null
                ? emptyIfNull(checkout.email(), "Sport Insight client")
                : emptyIfNull(checkout.currentUser().getDisplayName(), checkout.email());
        for (PreparedCheckoutLine line : preparedCheckout.lines()) {
            Order order = new Order(
                    line.quantity(),
                    LocalDate.now(),
                    clientName,
                    null,
                    paymentMethod,
                    null,
                    line.product().getSize(),
                    checkout.email(),
                    checkout.phone(),
                    checkout.shippingAddress(),
                    checkout.billingAddress(),
                    null,
                    line.product().getId(),
                    checkout.userId()
            );
            orderService.add(order);
        }

        Path invoicePath = exportInvoiceAutomatically(
                checkout.currentUser(),
                checkout.email(),
                checkout.phone(),
                paymentMethod,
                checkout.shippingAddress(),
                checkout.billingAddress(),
                preparedCheckout.total(),
                preparedCheckout.invoiceLines(),
                buildAutomaticInvoiceQrPayload(checkout, preparedCheckout, paymentMethod, pendingStripeCheckout)
        );

        clearStoreAfterSuccessfulPayment();
        notifyPaymentSucceeded(preparedCheckout.lines().size(), preparedCheckout.total(), invoicePath);
        setStatus("Commande enregistree avec succes.", "status-success");

        String invoiceMessage = invoicePath == null
                ? ""
                : "\nFacture PDF telechargee ici:\n" + invoicePath.toAbsolutePath();
        showAlert(Alert.AlertType.INFORMATION, "Paiement", "Commande validee pour " + formatPrice(preparedCheckout.total()) + "." + invoiceMessage);
    }

    private void clearStoreAfterSuccessfulPayment() {
        cancelStripeAutoVerification();
        cartItems.clear();
        clearCartSession();
        StripeCheckoutSessionStore.clear(currentCartUserId());
        clearCheckoutForm();
        refreshCatalog();
        renderCart();
        renderPaymentSummary();
        updateSectionVisibility(StoreSection.CATALOG);
        updatePaymentMethodState();
    }

    private void startStripeAutoVerification(
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            String sessionId
    ) {
        if (stripeCheckoutService == null || trimToNull(sessionId) == null) {
            return;
        }
        long monitorId = stripeAutoVerifySequence.incrementAndGet();
        CompletableFuture.runAsync(() -> monitorStripeCheckoutCompletion(monitorId, checkout, preparedCheckout, sessionId));
    }

    private void monitorStripeCheckoutCompletion(
            long monitorId,
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            String sessionId
    ) {
        for (int attempt = 0; attempt < STRIPE_AUTO_VERIFY_MAX_ATTEMPTS; attempt++) {
            if (monitorId != stripeAutoVerifySequence.get()) {
                return;
            }
            try {
                Thread.sleep(STRIPE_AUTO_VERIFY_DELAY_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            if (monitorId != stripeAutoVerifySequence.get()) {
                return;
            }
            StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout = StripeCheckoutSessionStore.get(checkout.userId());
            if (pendingCheckout == null || !Objects.equals(sessionId, pendingCheckout.sessionId())) {
                return;
            }
            try {
                StripeCheckoutService.CheckoutVerification verification = stripeCheckoutService.verifyCheckoutSession(sessionId);
                if (monitorId != stripeAutoVerifySequence.get()) {
                    return;
                }
                switch (verification.status()) {
                    case PAID -> {
                        Platform.runLater(() -> completeVerifiedStripeCheckout(checkout, preparedCheckout, sessionId));
                        return;
                    }
                    case FAILED -> {
                        Platform.runLater(() -> {
                            StripeCheckoutSessionStore.PendingStripeCheckout latestPending = StripeCheckoutSessionStore.get(checkout.userId());
                            if (latestPending != null && Objects.equals(sessionId, latestPending.sessionId())) {
                                clearPendingStripeCheckout("La session Stripe a ete fermee sans paiement confirme.", "status-error");
                                showValidation("Stripe indique un paiement incomplet. Relancez une nouvelle session Stripe.");
                            }
                        });
                        return;
                    }
                    case EXPIRED -> {
                        Platform.runLater(() -> {
                            StripeCheckoutSessionStore.PendingStripeCheckout latestPending = StripeCheckoutSessionStore.get(checkout.userId());
                            if (latestPending != null && Objects.equals(sessionId, latestPending.sessionId())) {
                                clearPendingStripeCheckout("La session Stripe a expire.", "status-warning");
                                showValidation("La session Stripe a expire. Creez une nouvelle session pour payer.");
                            }
                        });
                        return;
                    }
                    case PENDING -> {
                    }
                }
            } catch (StripeException ignored) {
                if (attempt == STRIPE_AUTO_VERIFY_MAX_ATTEMPTS - 1) {
                    Platform.runLater(() -> setStatus("Verification Stripe en attente. Cliquez pour verifier manuellement.", "status-warning"));
                }
            }
        }
    }

    private void completeVerifiedStripeCheckout(
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            String sessionId
    ) {
        if (!beginStripeCompletion(sessionId)) {
            return;
        }
        try {
            finalizePaidCheckout(checkout, preparedCheckout, "online");
        } catch (IllegalArgumentException | SQLException exception) {
            notifyPaymentFailed(preparedCheckout.total(), exception.getMessage());
            setStatus("Paiement impossible.", "status-error");
            showValidation(exception.getMessage());
        } catch (IOException exception) {
            notifyPaymentSucceeded(preparedCheckout.lines().size(), preparedCheckout.total(), null);
            clearStoreAfterSuccessfulPayment();
            setStatus("Commande enregistree, facture PDF indisponible.", "status-warning");
            showAlert(Alert.AlertType.WARNING, "Facture", "La commande a ete enregistree, mais la facture PDF n'a pas pu etre generee.\n" + exception.getMessage());
        } finally {
            cancelStripeAutoVerification();
            endStripeCompletion(sessionId);
        }
    }

    private boolean beginStripeCompletion(String sessionId) {
        String normalized = trimToNull(sessionId);
        if (normalized == null) {
            return false;
        }
        synchronized (stripeCompletionLock) {
            if (Objects.equals(completingStripeSessionId, normalized)) {
                return false;
            }
            completingStripeSessionId = normalized;
            return true;
        }
    }

    private void endStripeCompletion(String sessionId) {
        String normalized = trimToNull(sessionId);
        synchronized (stripeCompletionLock) {
            if (Objects.equals(completingStripeSessionId, normalized)) {
                completingStripeSessionId = null;
            }
        }
    }

    private void cancelStripeAutoVerification() {
        stripeAutoVerifySequence.incrementAndGet();
    }

    private StripeChargePreview resolveStripeChargePreview(BigDecimal amount) throws IOException, InterruptedException {
        if (currencyConversionService == null) {
            throw new IOException("Currency API is unavailable.");
        }
        CurrencyConversionService.ConversionResult conversionResult = currencyConversionService.convert(amount, "TND", List.of("USD"));
        BigDecimal usdAmount = conversionResult.convertedAmounts().get("USD");
        if (usdAmount == null) {
            throw new IOException("USD conversion is unavailable.");
        }
        return new StripeChargePreview(
                usdAmount.setScale(2, RoundingMode.HALF_UP),
                conversionResult.rateDate(),
                conversionResult.provider()
        );
    }

    private void openStripeCheckoutInBrowser(String checkoutUrl, BigDecimal usdAmount) throws IOException {
        if (trimToNull(checkoutUrl) == null) {
            throw new IOException("Stripe n'a pas retourne d'URL de paiement.");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("Impossible d'ouvrir le navigateur automatiquement. Ouvrez cette URL: " + checkoutUrl);
        }
        Desktop.getDesktop().browse(URI.create(checkoutUrl));
        showAlert(
                Alert.AlertType.INFORMATION,
                "Stripe",
                "Stripe Checkout a ete ouvert dans votre navigateur.\nMontant estime: "
                        + formatCurrency(usdAmount, "USD")
                        + "\nApres paiement, revenez ici. L'application verifiera automatiquement le paiement et generera la facture PDF."
        );
    }

    private boolean clearPendingStripeCheckout(String statusMessage, String statusClass) {
        StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout = StripeCheckoutSessionStore.get(currentCartUserId());
        if (pendingCheckout == null) {
            return false;
        }
        cancelStripeAutoVerification();
        StripeCheckoutSessionStore.clear(currentCartUserId());
        if (statusMessage != null) {
            setStatus(statusMessage, statusClass);
        }
        updatePaymentMethodState();
        return true;
    }

    private boolean isStripePayment(String paymentMethod) {
        String normalized = trimToNull(paymentMethod);
        return "online".equalsIgnoreCase(normalized)
                || "STRIPE".equalsIgnoreCase(normalized)
                || "stripe_checkout".equalsIgnoreCase(normalized);
    }

    public void openPaymentFromNotification() {
        prefillCheckoutFromSession();
        restorePendingStripeCheckout();
        clearValidation();
        updateSectionVisibility(StoreSection.PAYMENT);
        updatePaymentMethodState();
        setStatus("Paiement ouvert depuis les notifications.", "status-success");
    }

    private void refreshCatalog() {
        if (!serviceReady || productService == null) {
            return;
        }

        try {
            List<Product> sortedProducts = productService.getAllSorted(
                    sortByComboBox == null ? ProductRepository.ProductSortField.NAME : sortByComboBox.getValue(),
                    sortDirectionComboBox == null ? ProductRepository.SortDirection.ASC : sortDirectionComboBox.getValue()
            );
            ProductAiService.SmartProductQuery smartQuery = productAiService == null
                    ? ProductAiService.SmartProductQuery.empty()
                    : productAiService.interpretSearch(searchField == null ? null : searchField.getText(), sortedProducts);
            List<Product> filteredProducts = resolveVisibleProducts(sortedProducts, smartQuery);
            List<Order> orderHistory = loadOrderHistorySafely();
            ProductAnalyticsService.TrendingSelection trendingSelection = productAnalyticsService == null
                    ? new ProductAnalyticsService.TrendingSelection(List.of(), List.of(), 6, "Trending unavailable.")
                    : productAnalyticsService.selectTrendingProducts(filteredProducts, orderHistory, 6);

            catalogProducts.clear();
            catalogProducts.addAll(sortedProducts);
            orderHistorySnapshot.clear();
            orderHistorySnapshot.addAll(orderHistory);
            visibleProducts.clear();
            visibleProducts.addAll(trendingFilterActive ? trendingSelection.products() : filteredProducts);
            updateTrendingRanking(trendingSelection);
            syncCartWithCatalog(sortedProducts);
            renderProducts();
            renderCart();
            renderPaymentSummary();
            updateCartMetrics();
            updateSearchSummary(searchField == null ? null : searchField.getText(), smartQuery, trendingSelection);
            setStatus(visibleProducts.size() + " produit(s) disponibles.", "status-muted");
        } catch (SQLException exception) {
            setStatus("Chargement impossible.", "status-error");
            showAlert(Alert.AlertType.ERROR, "Store", exception.getMessage());
        }
    }

    private void syncCartWithCatalog(List<Product> catalogProducts) {
        Map<Integer, Product> visibleById = catalogProducts.stream()
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
        persistCartSession();
    }

    private List<Product> resolveVisibleProducts(List<Product> sortedProducts, ProductAiService.SmartProductQuery smartQuery) throws SQLException {
        String search = searchField == null ? null : searchField.getText();
        String normalizedSearch = trimToNull(search);
        if (normalizedSearch == null) {
            return sortedProducts;
        }
        if (smartQuery == null || smartQuery.isEmpty()) {
            return productService.findProducts(
                    normalizedSearch,
                    sortByComboBox == null ? ProductRepository.ProductSortField.NAME : sortByComboBox.getValue(),
                    sortDirectionComboBox == null ? ProductRepository.SortDirection.ASC : sortDirectionComboBox.getValue()
            );
        }
        return productService.filterProducts(sortedProducts, smartQuery);
    }

    private List<Order> loadOrderHistorySafely() {
        if (orderService == null) {
            return List.of();
        }
        try {
            return orderService.getAll();
        } catch (SQLException exception) {
            return List.of();
        }
    }

    private void updateTrendingRanking(ProductAnalyticsService.TrendingSelection trendingSelection) {
        trendingByProductId.clear();
        if (trendingSelection == null || trendingSelection.ranking() == null) {
            return;
        }
        for (ProductAnalyticsService.TrendingProductSnapshot snapshot : trendingSelection.ranking()) {
            if (snapshot.productId() != null) {
                trendingByProductId.put(snapshot.productId(), snapshot);
            }
        }
    }

    private void updateSearchSummary(
            String search,
            ProductAiService.SmartProductQuery smartQuery,
            ProductAnalyticsService.TrendingSelection trendingSelection
    ) {
        if (searchSummaryLabel == null) {
            return;
        }
        String normalizedSearch = trimToNull(search);
        String baseSummary;
        if (normalizedSearch == null) {
            baseSummary = "Local search ready.";
        } else if (smartQuery == null || smartQuery.isEmpty()) {
            baseSummary = "Text search: " + normalizedSearch;
        } else {
            baseSummary = emptyIfNull(smartQuery.summary(), "Local smart filters applied.");
        }

        if (trendingFilterActive) {
            String trendSummary = trendingSelection == null
                    ? "Trending filter active."
                    : emptyIfNull(trendingSelection.summary(), "Trending filter active.");
            searchSummaryLabel.setText(baseSummary + " | " + trendSummary);
            return;
        }

        searchSummaryLabel.setText(baseSummary);
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

        ProductAnalyticsService.TrendingProductSnapshot trendingSnapshot = product == null || product.getId() == null
                ? null
                : trendingByProductId.get(product.getId());
        if (trendingSnapshot != null) {
            Label trendingBadge = new Label("Trending");
            trendingBadge.getStyleClass().addAll("store-meta-chip", "store-trending-badge");
            card.getChildren().add(trendingBadge);
        }

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

        if (trendingSnapshot != null) {
            Label trendingInsight = new Label(trendingSnapshot.summary());
            trendingInsight.getStyleClass().add("store-product-subtitle");
            trendingInsight.setWrapText(true);
            card.getChildren().addAll(title, subtitle, price, stock, trendingInsight, addButton);
        } else {
            card.getChildren().addAll(title, subtitle, price, stock, addButton);
        }
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

        List<StoreRecommendation> recommendations = buildRecommendationsForProduct(line.getProduct(), 3);
        if (!recommendations.isEmpty()) {
            VBox suggestionBox = new VBox(10);
            Label suggestionTitle = new Label("Suggestions produits");
            suggestionTitle.getStyleClass().add("section-title");
            suggestionBox.getChildren().add(suggestionTitle);
            for (StoreRecommendation recommendation : recommendations) {
                suggestionBox.getChildren().add(buildPaymentSuggestionCard(recommendation));
            }
            card.getChildren().add(suggestionBox);
        }
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

    private List<StoreRecommendation> buildRecommendationsForProduct(Product baseProduct, int limit) {
        if (orderIntelligenceService == null || baseProduct == null || catalogProducts.isEmpty()) {
            return List.of();
        }

        List<StoreRecommendation> recommendations = new ArrayList<>();
        List<OrderIntelligenceService.ProductRecommendation> matches =
                orderIntelligenceService.recommendProducts(baseProduct, catalogProducts, orderHistorySnapshot);
        for (OrderIntelligenceService.ProductRecommendation match : matches) {
            if (match == null || match.productId() == null || findCartLine(match.productId()) != null) {
                continue;
            }
            Product candidate = findProductInCatalog(match.productId());
            if (candidate == null || candidate.getStock() <= 0 || containsRecommendation(recommendations, match.productId())) {
                continue;
            }
            recommendations.add(new StoreRecommendation(candidate, formatStoreRecommendationReason(match.reason())));
            if (recommendations.size() >= limit) {
                break;
            }
        }
        return recommendations;
    }

    private boolean containsRecommendation(List<StoreRecommendation> recommendations, Integer productId) {
        for (StoreRecommendation recommendation : recommendations) {
            if (recommendation != null
                    && recommendation.product() != null
                    && Objects.equals(recommendation.product().getId(), productId)) {
                return true;
            }
        }
        return false;
    }

    private Product findProductInCatalog(Integer productId) {
        for (Product product : catalogProducts) {
            if (product != null && Objects.equals(product.getId(), productId)) {
                return product;
            }
        }
        return null;
    }

    private VBox buildPaymentSuggestionCard(StoreRecommendation recommendation) {
        Product product = recommendation.product();

        VBox card = new VBox(10);
        card.getStyleClass().addAll("panel-card", "store-cart-card");

        HBox header = new HBox(12);
        header.getChildren().add(createImageShell(product.getImage(), product.getName(), 96, 74, "store-cart-image-shell"));

        VBox textBox = new VBox(6);
        Label title = new Label(emptyIfNull(product.getName(), "Produit"));
        title.getStyleClass().add("store-cart-title");
        title.setWrapText(true);

        Label subtitle = new Label(buildProductSubtitle(product));
        subtitle.getStyleClass().add("store-product-subtitle");
        subtitle.setWrapText(true);

        Label reason = new Label(recommendation.reason());
        reason.getStyleClass().add("store-product-subtitle");
        reason.setWrapText(true);

        Label price = new Label(formatPrice(product.getPrice()));
        price.getStyleClass().add("store-product-price");
        textBox.getChildren().addAll(title, subtitle, reason, price);

        header.getChildren().add(textBox);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Label stock = new Label(product.getStock() > 0 ? product.getStock() + " in stock" : "Out of stock");
        stock.getStyleClass().addAll("store-stock-badge", resolveStockStyle(product.getStock()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button(product.getStock() > 0 ? "Add to order" : "Unavailable");
        addButton.getStyleClass().add(product.getStock() > 0 ? "primary-button" : "ghost-button");
        addButton.setDisable(product.getStock() <= 0);
        addButton.setOnAction(event -> addProductToCart(product));

        actions.getChildren().addAll(stock, spacer, addButton);
        card.getChildren().addAll(header, actions);
        return card;
    }

    private String formatStoreRecommendationReason(String reason) {
        String normalized = trimToNull(reason);
        if (normalized == null) {
            return "Recommended for this order.";
        }
        String lower = normalized.toLowerCase();
        if ("same brand cross-sell".equals(lower)) {
            return "Same brand, good complementary choice.";
        }
        if (lower.startsWith("complements ")) {
            return "Complements " + normalized.substring("complements ".length()) + ".";
        }
        if ("popular with recent orders".equals(lower)) {
            return "Popular with recent buyers.";
        }
        if ("related product".equals(lower)) {
            return "Related product for this order.";
        }
        return normalized;
    }

    private void addProductToCart(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        clearPendingStripeCheckout(null, null);
        CartLine existingLine = findCartLine(product.getId());
        if (existingLine == null) {
            cartItems.add(new CartLine(copyProduct(product), 1));
        } else if (existingLine.getQuantity() < product.getStock()) {
            existingLine.setQuantity(existingLine.getQuantity() + 1);
        }
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
        prefillCheckoutFromSession();
        persistCartSession();
        if (persistCartNotification(product)) {
            setStatus("Produit ajoute au panier. Notification envoyee.", "status-success");
        } else {
            setStatus("Produit ajoute au panier.", "status-success");
        }
    }

    private void updateCartQuantity(CartLine line, int quantity) {
        if (line == null) {
            return;
        }
        clearPendingStripeCheckout(null, null);
        if (quantity <= 0) {
            cartItems.remove(line);
        } else {
            int maxQuantity = Math.max(line.getProduct().getStock(), 1);
            line.setQuantity(Math.min(quantity, maxQuantity));
        }
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
        persistCartSession();
    }

    private void removeFromCart(CartLine line) {
        clearPendingStripeCheckout(null, null);
        cartItems.remove(line);
        renderCart();
        renderPaymentSummary();
        updateCartMetrics();
        persistCartSession();
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
        updatePaymentMethodState();
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
        clearValidation();
        prefillCheckoutFromSession();
        updatePaymentMethodState();
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

    private void updateTrendingButtonState() {
        if (trendingFilterButton == null) {
            return;
        }
        trendingFilterButton.setText(trendingFilterActive ? "Trending ON" : "Trending");
        trendingFilterButton.getStyleClass().removeAll("primary-button", "soft-button");
        trendingFilterButton.getStyleClass().add(trendingFilterActive ? "primary-button" : "soft-button");
    }

    private String buildPlaceholderLabel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "IMG";
        }
        return normalized.substring(0, Math.min(3, normalized.length())).toUpperCase();
    }

    private void restoreCartSession() {
        cartItems.clear();
        for (StoreCartSession.CartEntry entry : StoreCartSession.loadCart(currentCartUserId())) {
            if (entry == null || entry.product() == null || entry.quantity() <= 0) {
                continue;
            }
            cartItems.add(new CartLine(copyProduct(entry.product()), entry.quantity()));
        }
    }

    private void persistCartSession() {
        List<StoreCartSession.CartEntry> entries = new ArrayList<>(cartItems.size());
        for (CartLine line : cartItems) {
            if (line == null || line.getProduct() == null || line.getQuantity() <= 0) {
                continue;
            }
            entries.add(new StoreCartSession.CartEntry(copyProduct(line.getProduct()), line.getQuantity()));
        }
        StoreCartSession.saveCart(currentCartUserId(), entries);
    }

    private void clearCartSession() {
        StoreCartSession.clearCart(currentCartUserId());
    }

    private Integer currentCartUserId() {
        User currentUser = AuthSession.getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }

    private boolean persistCartNotification(Product product) {
        User currentUser = AuthSession.getCurrentUser();
        if (product == null || currentUser == null || currentUser.getId() == null || workflowNotificationService == null) {
            return false;
        }
        return workflowNotificationService.notifyCartAdded(
                currentUser.getId(),
                product,
                calculateCartQuantity(),
                calculateCartTotal()
        );
    }

    private void prefillCheckoutFromSession() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        if (trimToNull(emailField.getText()) == null && trimToNull(currentUser.getEmail()) != null) {
            emailField.setText(currentUser.getEmail().trim());
        }
        if (trimToNull(phoneField.getText()) == null && trimToNull(currentUser.getTelephone()) != null) {
            phoneField.setText(currentUser.getTelephone().trim());
        }
    }

    private void restorePendingStripeCheckout() {
        StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout = StripeCheckoutSessionStore.get(currentCartUserId());
        if (pendingCheckout == null) {
            return;
        }
        if (trimToNull(emailField.getText()) == null && pendingCheckout.email() != null) {
            emailField.setText(pendingCheckout.email());
        }
        if (trimToNull(phoneField.getText()) == null && pendingCheckout.phone() != null) {
            phoneField.setText(pendingCheckout.phone());
        }
        if (trimToNull(shippingAddressArea.getText()) == null && pendingCheckout.shippingAddress() != null) {
            shippingAddressArea.setText(pendingCheckout.shippingAddress());
        }
        if (trimToNull(billingAddressArea.getText()) == null && pendingCheckout.billingAddress() != null) {
            billingAddressArea.setText(pendingCheckout.billingAddress());
        }
        if (paymentMethodComboBox != null) {
            paymentMethodComboBox.setValue("online");
        }
    }

    private void updatePaymentMethodState() {
        if (payNowButton == null || paymentMethodComboBox == null) {
            return;
        }

        boolean stripeSelected = isStripePayment(paymentMethodComboBox.getValue());
        StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout = StripeCheckoutSessionStore.get(currentCartUserId());

        if (!stripeSelected) {
            payNowButton.setDisable(false);
            payNowButton.setText("Valider la commande");
            setPaymentFlowCopy(
                    "Paiement a la livraison et enregistrement direct de la commande.",
                    "Choisissez Stripe pour payer en ligne, ou gardez le paiement a la livraison."
            );
            return;
        }

        if (stripeCheckoutService == null || !stripeCheckoutService.isConfigured()) {
            payNowButton.setDisable(true);
            payNowButton.setText("Stripe indisponible");
            setPaymentFlowCopy(
                    "Stripe n'est pas encore configure pour cette application.",
                    "Ajoutez une cle Stripe valide dans stripe.local.properties, STRIPE_SECRET_KEY, ou STRIPE_API_KEY pour activer ce mode."
            );
            return;
        }

        payNowButton.setDisable(false);
        if (pendingCheckout != null) {
            payNowButton.setText("Verifier maintenant");
            String chargeText = pendingCheckout.chargeAmount() == null
                    ? "Montant Stripe en attente."
                    : "Session Stripe ouverte pour " + formatCurrency(pendingCheckout.chargeAmount(), emptyIfNull(pendingCheckout.chargeCurrency(), "USD")) + ".";
            String rateText = trimToNull(pendingCheckout.rateDate()) == null
                    ? chargeText
                    : chargeText + " Taux du " + pendingCheckout.rateDate() + ".";
            setPaymentFlowCopy(
                    "Le paiement Stripe a ete lance dans votre navigateur. La verification est automatique des que Stripe confirme le paiement.",
                    rateText
            );
            return;
        }

        payNowButton.setText("Payer avec Stripe");
        setPaymentFlowCopy(
                "Stripe Checkout ouvrira une page de paiement securisee dans votre navigateur. Des que le paiement est confirme, la commande et la facture PDF sont generees ici.",
                "Le montant Stripe est converti en USD via l'API de change avant creation de la session."
        );
        refreshStripeHintAsync();
    }

    private void setPaymentFlowCopy(String description, String hint) {
        if (paymentFlowDescriptionLabel != null) {
            paymentFlowDescriptionLabel.setText(description);
        }
        if (paymentMethodHintLabel != null) {
            String normalizedHint = trimToNull(hint);
            paymentMethodHintLabel.setManaged(normalizedHint != null);
            paymentMethodHintLabel.setVisible(normalizedHint != null);
            paymentMethodHintLabel.setText(emptyIfNull(normalizedHint, ""));
        }
    }

    private void refreshStripeHintAsync() {
        if (paymentMethodHintLabel == null || !isStripePayment(paymentMethodComboBox == null ? null : paymentMethodComboBox.getValue())) {
            return;
        }
        if (cartItems.isEmpty()) {
            paymentMethodHintLabel.setText("Ajoutez d'abord des produits pour calculer le montant Stripe.");
            return;
        }

        long requestId = stripeHintRequestId.incrementAndGet();
        paymentMethodHintLabel.setManaged(true);
        paymentMethodHintLabel.setVisible(true);
        paymentMethodHintLabel.setText("Calcul du montant Stripe en USD...");

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return resolveStripeChargePreview(calculateCartTotal());
                    } catch (IOException | InterruptedException exception) {
                        return null;
                    }
                })
                .thenAccept(preview -> Platform.runLater(() -> {
                    if (requestId != stripeHintRequestId.get() || paymentMethodHintLabel == null || !isStripePayment(paymentMethodComboBox.getValue())) {
                        return;
                    }
                    if (preview == null) {
                        paymentMethodHintLabel.setText("Conversion USD indisponible pour le moment. Stripe calculera a la creation de la session.");
                        return;
                    }
                    String suffix = trimToNull(preview.rateDate()) == null ? "" : " | taux du " + preview.rateDate();
                    paymentMethodHintLabel.setText("Montant estime Stripe: " + formatCurrency(preview.usdAmount(), "USD") + suffix);
                }));
    }

    private void notifyPaymentSucceeded(int orderCount, BigDecimal total, Path invoicePath) {
        Integer userId = currentCartUserId();
        if (workflowNotificationService == null || userId == null) {
            return;
        }
        workflowNotificationService.notifyPaymentSucceeded(userId, orderCount, total, invoicePath);
    }

    private void notifyPaymentFailed(BigDecimal total, String reason) {
        Integer userId = currentCartUserId();
        if (workflowNotificationService == null || userId == null) {
            return;
        }
        workflowNotificationService.notifyPaymentFailed(userId, total, reason);
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(emptyIfNull(message, "Boutique prete"));
        statusLabel.getStyleClass().setAll("status-pill", styleClass == null ? "status-muted" : styleClass);
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " DT";
    }

    private String formatCurrency(BigDecimal value, String currency) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " " + emptyIfNull(currency, "USD");
    }

    private String buildStripeLineDescription(Product product) {
        return buildProductSubtitle(product).replace(" â€¢ ", " | ");
    }

    private String buildCartSignature(List<CartLine> sourceLines) {
        return sourceLines.stream()
                .filter(line -> line != null && line.getProduct() != null && line.getProduct().getId() != null)
                .map(line -> line.getProduct().getId()
                        + ":" + line.getQuantity()
                        + ":" + (line.getProduct().getPrice() == null ? "0.00" : line.getProduct().getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()))
                .sorted()
                .collect(Collectors.joining("|"));
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

    private Path exportInvoiceAutomatically(
            User currentUser,
            String email,
            String phone,
            String paymentMethod,
            String shipping,
            String billing,
            BigDecimal total,
            List<OrderPdfExportService.InvoiceLine> invoiceLines,
            String qrPayload
    ) throws IOException {
        if (orderPdfExportService == null) {
            return null;
        }

        String customerName = currentUser == null ? email : currentUser.getDisplayName();
        Path target = resolveAutomaticInvoicePath(customerName);
        orderPdfExportService.exportInvoice(
                target,
                new OrderPdfExportService.Invoice(
                        customerName,
                        email,
                        phone,
                        paymentMethod,
                        shipping,
                        billing,
                        LocalDate.now(),
                        total,
                        invoiceLines
                ),
                qrPayload
        );
        openFileIfPossible(target);
        return target;
    }

    private String buildAutomaticInvoiceQrPayload(
            CheckoutFormSnapshot checkout,
            PreparedCheckout preparedCheckout,
            String paymentMethod,
            StripeCheckoutSessionStore.PendingStripeCheckout pendingStripeCheckout
    ) {
        String customerLabel = checkout.currentUser() == null
                ? emptyIfNull(checkout.email(), "Sport Insight client")
                : emptyIfNull(checkout.currentUser().getDisplayName(), checkout.email());
        String items = preparedCheckout.invoiceLines() == null
                ? "-"
                : preparedCheckout.invoiceLines().stream()
                .limit(5)
                .map(line -> compactInvoiceItem(line.productName(), line.quantity(), line.subtotal()))
                .collect(Collectors.joining("\n"));
        String stripeSessionId = pendingStripeCheckout == null ? "-" : emptyIfNull(pendingStripeCheckout.sessionId(), "-");
        String stripeAmount = pendingStripeCheckout == null || pendingStripeCheckout.chargeAmount() == null
                ? "-"
                : formatCurrency(pendingStripeCheckout.chargeAmount(), emptyIfNull(pendingStripeCheckout.chargeCurrency(), "USD"));

        return """
                SPORT INSIGHT INVOICE
                CUSTOMER: %s
                PAYMENT: %s
                TOTAL: %s
                DATE: %s
                ITEMS:
                %s
                STRIPE SESSION: %s
                STRIPE AMOUNT: %s
                """.formatted(
                customerLabel,
                emptyIfNull(paymentMethod, "-"),
                formatPrice(preparedCheckout.total()),
                LocalDate.now(),
                items,
                stripeSessionId,
                stripeAmount
        );
    }

    private String compactInvoiceItem(String productName, int quantity, BigDecimal subtotal) {
        String compactName = emptyIfNull(productName, "Produit")
                .replaceAll("\\s+", " ")
                .trim();
        if (compactName.length() > 28) {
            compactName = compactName.substring(0, 28).trim() + "...";
        }
        return compactName + " x" + quantity + " = " + formatPrice(subtotal);
    }

    private Path resolveAutomaticInvoicePath(String customerName) throws IOException {
        String safeName = (customerName == null || customerName.isBlank() ? "client" : customerName)
                .replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase();
        if (safeName.isBlank()) {
            safeName = "client";
        }

        Path downloadsDir = Path.of(System.getProperty("user.home"), "Downloads");
        java.nio.file.Files.createDirectories(downloadsDir);
        return downloadsDir.resolve("facture-" + safeName + "-" + INVOICE_FILE_FORMATTER.format(LocalDateTime.now()) + ".pdf");
    }

    private void openFileIfPossible(Path path) {
        try {
            if (path != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException ignored) {
        }
    }

    private enum StoreSection {
        CATALOG,
        CART,
        PAYMENT
    }

    private record CheckoutFormSnapshot(
            User currentUser,
            Integer userId,
            String email,
            String phone,
            String shippingAddress,
            String billingAddress,
            String paymentMethod
    ) {
    }

    private record PreparedCheckout(
            List<PreparedCheckoutLine> lines,
            List<OrderPdfExportService.InvoiceLine> invoiceLines,
            BigDecimal total,
            String cartSignature
    ) {
    }

    private record PreparedCheckoutLine(
            Product product,
            int quantity,
            BigDecimal subtotal
    ) {
    }

    private record StripeChargePreview(
            BigDecimal usdAmount,
            String rateDate,
            String provider
    ) {
    }

    private record StoreRecommendation(
            Product product,
            String reason
    ) {
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
