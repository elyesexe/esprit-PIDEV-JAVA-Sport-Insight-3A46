package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.Parent;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.i18n.I18n;
import tn.esprit.i18n.UiTextLocalizer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * Admin layout: left sidebar (dashboard + CRUD + return to user UI), center hosts panels.
 * CRUD panels reuse existing FXML; the user navbar strip is removed so only the workspace shows.
 */
public class AdminShellController {
    private static final String ADMIN_LIGHT_CLASS = "admin-light";
    private static final String ADMIN_DARK_CLASS = "admin-dark";
    private static final String ADMIN_PERFORMANCE_CLASS = "admin-performance-mode";
    private static final String ADMIN_EMBEDDED_PAGE_CLASS = "admin-embedded-page";
    private static final String ADMIN_DARK_BACKGROUND_STYLE =
            "-fx-background-color: radial-gradient(center 12% 12%, radius 34%, rgba(16, 185, 129, 0.12) 0%, rgba(16, 185, 129, 0) 100%), " +
                    "radial-gradient(center 86% 14%, radius 30%, rgba(59, 130, 246, 0.10) 0%, rgba(59, 130, 246, 0) 100%), " +
                    "linear-gradient(from 0% 0% to 100% 100%, #071019 0%, #0f172a 48%, #111827 100%); " +
                    "-fx-background-insets: 0; " +
                    "-fx-background-radius: 0; " +
                    "-fx-border-color: transparent;";
    private static final String ADMIN_DARK_TRANSPARENT_STYLE =
            "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;";
    private static final String ADMIN_DARK_CARD_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.92); "
                    + "-fx-border-color: rgba(71, 85, 105, 0.34); "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_FIELD_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.96); "
                    + "-fx-control-inner-background: rgba(15, 23, 42, 0.96); "
                    + "-fx-border-color: rgba(71, 85, 105, 0.42); "
                    + "-fx-text-fill: #e2e8f0; "
                    + "-fx-prompt-text-fill: rgba(148, 163, 184, 0.82);";
    private static final String ADMIN_DARK_SECONDARY_BUTTON_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.88); "
                    + "-fx-border-color: rgba(71, 85, 105, 0.36); "
                    + "-fx-text-fill: #e2e8f0; "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_ACTIVE_BUTTON_STYLE =
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #064e3b 0%, #0f766e 100%); "
                    + "-fx-border-color: rgba(52, 211, 153, 0.48); "
                    + "-fx-text-fill: #ffffff; "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_PRIMARY_BUTTON_STYLE =
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #10b981 0%, #34d399 100%); "
                    + "-fx-border-color: rgba(16, 185, 129, 0.42); "
                    + "-fx-text-fill: #ffffff; "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_DANGER_BUTTON_STYLE =
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #991b1b 0%, #dc2626 100%); "
                    + "-fx-border-color: rgba(248, 113, 113, 0.34); "
                    + "-fx-text-fill: #fff7f7; "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_CHIP_STYLE =
            "-fx-background-color: rgba(6, 78, 59, 0.36); "
                    + "-fx-border-color: rgba(16, 185, 129, 0.32); "
                    + "-fx-text-fill: #a7f3d0; "
                    + "-fx-effect: none;";
    private static final String ADMIN_DARK_SIDEBAR_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.94); "
                    + "-fx-border-color: rgba(71, 85, 105, 0.36); "
                    + "-fx-effect: none;";
    private static final Set<String> TRANSPARENT_DARK_SURFACE_CLASSES = Set.of(
            "page-scroll", "page-shell-wrap", "page-shell", "content-shell", "side-column",
            "product-content-shell", "sponsor-admin-shell", "admin-users-shell", "toolbar-row",
            "competition-filter-bar", "sponsor-overview-grid", "product-chart-grid", "training-cards",
            "annonce-feed", "annonce-comment-stack"
    );
    private static final Set<String> DARK_CARD_CLASSES = Set.of(
            "panel-card", "toolbar-panel", "results-panel", "detail-card", "form-card", "match-toolbar",
            "sync-toolbar-row", "toolbar-summary-card", "admin-kpi-card", "admin-hint-card", "admin-chart-card",
            "training-card", "training-card-hero", "training-card-body", "note-card", "player-list-card",
            "fixture-card", "fixture-team-logo-shell", "match-detail-logo-shell", "fixture-score-shell",
            "match-detail-score-shell", "match-detail-board", "match-stat-row", "competition-card-button",
            "competition-card-logo-shell", "annonce-post-card", "annonce-comment-card",
            "annonce-user-description-box", "annonce-metric-card", "product-header-card",
            "product-toolbar-panel", "product-detail-card", "product-form-card", "product-metric-card",
            "product-chart-card", "product-thumbnail-shell", "product-detail-image-shell", "product-info-pill",
            "sponsor-card", "sponsor-contract-card", "sponsor-empty-card", "sponsor-chart-card",
            "sponsor-metric-card", "sponsor-admin-metric-card", "sponsor-logo-shell",
            "sponsor-logo-preview-shell", "detail-info-card", "squad-card", "team-top-scorer-row",
            "card-logo-shell", "detail-logo-shell", "stat-chip", "empty-state-box", "flashscore-score-card",
            "flashscore-lineup-card", "flashscore-stats-card", "flashscore-scoreboard", "flashscore-fact-chip",
            "flashscore-logo-shell", "bench-column", "flashscore-stat-card", "bench-player-chip",
            "bench-placeholder-chip", "flashscore-summary-card", "timeline-event-box", "pitch-player-identity-pill"
    );
    private static final Set<String> DARK_FIELD_CLASSES = Set.of(
            "form-text-field", "search-field", "form-field", "form-area", "annonce-text-area", "glass-combo",
            "form-combo", "form-date-picker", "match-date-picker", "match-search-field"
    );
    private static final Set<String> DARK_CHIP_CLASSES = Set.of(
            "toolbar-chip", "toolbar-chip-soft", "status-muted", "status-pill", "fixture-status",
            "fixture-meta-chip", "fixture-link-chip", "player-card-meta-pill", "annonce-user-meta-chip",
            "sponsor-meta-chip", "product-stock-chip", "team-card-competition-badge", "team-top-scorer-pill",
            "flashscore-inline-badge", "timeline-minute-chip", "timeline-score-chip", "pitch-side-badge"
    );

    private static final String DASHBOARD = "/tn/esprit/views/admin-dashboard.fxml";
    private static final String EQUIPE_CRUD = "/tn/esprit/views/equipe-crud-view.fxml";
    private static final String JOUEUR_CRUD = "/tn/esprit/views/joueur-admin-view.fxml";
    private static final String MATCH_CRUD = "/tn/esprit/views/match-admin-view.fxml";
    private static final String ANNONCE_CRUD = "/tn/esprit/views/annonce-crud-view.fxml";
    private static final String PRODUCT_CRUD = "/tn/esprit/views/product-crud-view.fxml";
    private static final String ORDER_CRUD = "/tn/esprit/views/order-crud-view.fxml";
    private static final String ENTRAINEMENT_CRUD = "/tn/esprit/views/entrainement-admin-view.fxml";
    private static final String SPONSOR_CRUD = "/tn/esprit/views/sponsor-admin-view.fxml";
    private static final String USER_MODERATION = "/tn/esprit/views/admin-users-view.fxml";
    private static final double SIDEBAR_EXPANDED_WIDTH = 286;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 104;

    @FXML
    private BorderPane adminRoot;
    @FXML
    private ScrollPane adminSidebarScroll;
    @FXML
    private VBox adminSidebarRoot;
    @FXML
    private Label adminSidebarTitleLabel;
    @FXML
    private Region sidebarHeaderSpacer;
    @FXML
    private Button sidebarToggleButton;
    @FXML
    private StackPane contentStack;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Button userUiButton;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button annoncesNavButton;
    @FXML
    private Button productsNavButton;
    @FXML
    private Button ordersNavButton;
    @FXML
    private Button entrainementsNavButton;
    @FXML
    private Button sponsorsNavButton;
    @FXML
    private Button usersNavButton;

    private Object activeContentController;
    private boolean sidebarCollapsed;
    private final Map<String, LoadedWorkspace> workspaceCache = new HashMap<>();

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        themeToggleButton.selectedProperty().addListener((observable, oldValue, selected) -> applyAdminModeStyles(selected));
        enableAdminPerformanceMode();
        configureSidebarCollapse();
        applyAdminModeStyles(themeToggleButton.isSelected());
        showDashboard();
    }

    @FXML
    private void handleDashboard() {
        showDashboard();
    }

    @FXML
    private void handleEquipes() {
        loadStrippedCrud(EQUIPE_CRUD);
        highlightNav(equipesNavButton);
    }

    @FXML
    private void handleJoueurs() {
        loadStrippedCrud(JOUEUR_CRUD);
        highlightNav(joueursNavButton);
    }

    @FXML
    private void handleMatchs() {
        loadStrippedCrud(MATCH_CRUD);
        highlightNav(matchsNavButton);
    }

    @FXML
    private void handleAnnonces() {
        loadStrippedCrud(ANNONCE_CRUD);
        highlightNav(annoncesNavButton);
    }

    @FXML
    private void handleProducts() {
        loadStrippedCrud(PRODUCT_CRUD);
        highlightNav(productsNavButton);
    }

    @FXML
    private void handleOrders() {
        loadStrippedCrud(ORDER_CRUD);
        highlightNav(ordersNavButton);
    }

    @FXML
    private void handleEntrainements() {
        loadStrippedCrud(ENTRAINEMENT_CRUD);
        highlightNav(entrainementsNavButton);
    }

    @FXML
    private void handleSponsors() {
        loadStrippedCrud(SPONSOR_CRUD);
        highlightNav(sponsorsNavButton);
    }

    @FXML
    private void handleUsers() {
        loadStrippedCrud(USER_MODERATION);
        highlightNav(usersNavButton);
    }

    @FXML
    private void handleOpenUser() {
        SceneNavigator.switchScene(
                userUiButton,
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil"
        );
    }

    @FXML
    private void handleToggleSidebar() {
        setSidebarCollapsed(!sidebarCollapsed);
    }

    private void showDashboard() {
        LoadedWorkspace workspace = getOrLoadWorkspace(DASHBOARD, false);
        if (workspace != null) {
            showWorkspace(workspace);
        }
        highlightNav(dashboardNavButton);
    }

    private void loadStrippedCrud(String resourcePath) {
        LoadedWorkspace workspace = getOrLoadWorkspace(resourcePath, true);
        if (workspace != null) {
            showWorkspace(workspace);
        }
    }

    private LoadedWorkspace getOrLoadWorkspace(String resourcePath, boolean stripCrudChrome) {
        LoadedWorkspace cachedWorkspace = workspaceCache.get(resourcePath);
        if (cachedWorkspace != null) {
            return cachedWorkspace;
        }

        URL url = AdminShellController.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(url, I18n.getBundle());
            Parent root = loader.load();
            UiTextLocalizer.install(root);
            if (stripCrudChrome && root instanceof BorderPane borderPane) {
                borderPane.setTop(null);
            }
            Object controller = loader.getController();
            if (!root.getStyleClass().contains("admin-workspace-root")) {
                root.getStyleClass().add("admin-workspace-root");
            }
            if (stripCrudChrome) {
                addStyleClass(root, ADMIN_EMBEDDED_PAGE_CLASS);
            }
            addStyleClass(root, ADMIN_PERFORMANCE_CLASS);
            if (stripCrudChrome) {
                stripNodesByStyleClass(root, "hero-shell");
                stripNodesByStyleClass(root, "home-hero-shell");
            }
            LoadedWorkspace workspace = new LoadedWorkspace(root, controller, stripCrudChrome);
            workspaceCache.put(resourcePath, workspace);
            return workspace;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showWorkspace(LoadedWorkspace workspace) {
        if (workspace == null || contentStack == null) {
            return;
        }
        activeContentController = workspace.controller();
        applyWorkspaceModeStyles(workspace.root(), themeToggleButton != null && themeToggleButton.isSelected(), workspace.embeddedPage());
        applyControllerModeStyles(activeContentController);
        contentStack.getChildren().setAll(Collections.singletonList(workspace.root()));
    }

    private void applyAdminModeStyles(boolean darkMode) {
        if (adminRoot == null) {
            return;
        }
        adminRoot.getStyleClass().removeAll("theme-light", "theme-dark");
        adminRoot.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
        adminRoot.getStyleClass().removeAll(ADMIN_LIGHT_CLASS, ADMIN_DARK_CLASS);
        adminRoot.getStyleClass().add(darkMode ? ADMIN_DARK_CLASS : ADMIN_LIGHT_CLASS);
        addStyleClass(adminRoot, ADMIN_PERFORMANCE_CLASS);
        adminRoot.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
        applySidebarPalette(darkMode);
        if (contentStack != null) {
            addStyleClass(contentStack, ADMIN_PERFORMANCE_CLASS);
            contentStack.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
            for (Node child : contentStack.getChildren()) {
                applyWorkspaceModeStyles(child, darkMode);
            }
        }
        applyControllerModeStyles(activeContentController, darkMode);
    }

    private void applyWorkspaceModeStyles(Node node) {
        applyWorkspaceModeStyles(node, themeToggleButton != null && themeToggleButton.isSelected());
    }

    private void applyWorkspaceModeStyles(Node node, boolean darkMode) {
        applyWorkspaceModeStyles(node, darkMode, node != null && node.getStyleClass().contains(ADMIN_EMBEDDED_PAGE_CLASS));
    }

    private void applyWorkspaceModeStyles(Node node, boolean darkMode, boolean embeddedPage) {
        if (node == null) {
            return;
        }
        node.getStyleClass().removeAll("theme-light", "theme-dark");
        node.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
        node.getStyleClass().removeAll(ADMIN_LIGHT_CLASS, ADMIN_DARK_CLASS);
        node.getStyleClass().add(darkMode ? ADMIN_DARK_CLASS : ADMIN_LIGHT_CLASS);
        addStyleClass(node, ADMIN_PERFORMANCE_CLASS);
        node.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
        if (embeddedPage) {
            addStyleClass(node, ADMIN_EMBEDDED_PAGE_CLASS);
            forceEmbeddedPagePalette(node, darkMode, true);
        }
    }

    private void forceEmbeddedPagePalette(Node node, boolean darkMode, boolean rootNode) {
        if (node == null) {
            return;
        }

        if (!darkMode) {
            if (rootNode
                    || hasAnyStyleClass(node, TRANSPARENT_DARK_SURFACE_CLASSES)
                    || hasAnyStyleClass(node, DARK_CARD_CLASSES)
                    || hasAnyStyleClass(node, DARK_FIELD_CLASSES)
                    || hasAnyStyleClass(node, DARK_CHIP_CLASSES)
                    || node instanceof Button) {
                node.setStyle("");
            }
        } else if (rootNode) {
            node.setStyle(ADMIN_DARK_BACKGROUND_STYLE);
        } else if (hasAnyStyleClass(node, TRANSPARENT_DARK_SURFACE_CLASSES)) {
            node.setStyle(ADMIN_DARK_TRANSPARENT_STYLE);
        } else if (hasAnyStyleClass(node, DARK_FIELD_CLASSES)) {
            node.setStyle(ADMIN_DARK_FIELD_STYLE);
        } else if (hasAnyStyleClass(node, DARK_CARD_CLASSES)) {
            node.setStyle(ADMIN_DARK_CARD_STYLE);
        } else if (hasAnyStyleClass(node, DARK_CHIP_CLASSES)) {
            node.setStyle(ADMIN_DARK_CHIP_STYLE);
        } else if (node instanceof Button) {
            applyEmbeddedButtonPalette(node);
        }

        if (node instanceof ScrollPane scrollPane) {
            forceEmbeddedPagePalette(scrollPane.getContent(), darkMode, false);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                forceEmbeddedPagePalette(child, darkMode, false);
            }
        }
    }

    private void applyEmbeddedButtonPalette(Node node) {
        if (hasAnyStyleClass(node, Set.of("danger-button", "table-row-danger-button"))) {
            node.setStyle(ADMIN_DARK_DANGER_BUTTON_STYLE);
        } else if (hasAnyStyleClass(node, Set.of("primary-button", "sidebar-open-button", "navbar-settings-button"))) {
            node.setStyle(ADMIN_DARK_PRIMARY_BUTTON_STYLE);
        } else if (hasAnyStyleClass(node, Set.of("competition-card-button-active", "detail-tab-button-active"))) {
            node.setStyle(ADMIN_DARK_ACTIVE_BUTTON_STYLE);
        } else {
            node.setStyle(ADMIN_DARK_SECONDARY_BUTTON_STYLE);
        }
    }

    private boolean hasAnyStyleClass(Node node, Set<String> styleClasses) {
        if (node == null || styleClasses == null || styleClasses.isEmpty()) {
            return false;
        }
        for (String styleClass : styleClasses) {
            if (node.getStyleClass().contains(styleClass)) {
                return true;
            }
        }
        return false;
    }

    private void stripNodesByStyleClass(Parent root, String styleClass) {
        List<Node> targets = new ArrayList<>();
        collectNodesByStyleClass(root, styleClass, targets);
        for (Node target : targets) {
            Parent parent = target.getParent();
            if (parent instanceof Pane pane) {
                pane.getChildren().remove(target);
            } else if (parent instanceof Group group) {
                group.getChildren().remove(target);
            } else if (parent instanceof ScrollPane scrollPane && scrollPane.getContent() == target) {
                scrollPane.setContent(null);
            }
        }
    }

    private void collectNodesByStyleClass(Parent parent, String styleClass, List<Node> targets) {
        if (parent instanceof ScrollPane scrollPane) {
            Node content = scrollPane.getContent();
            if (content != null) {
                if (content.getStyleClass().contains(styleClass)) {
                    targets.add(content);
                }
                if (content instanceof Parent contentParent) {
                    collectNodesByStyleClass(contentParent, styleClass, targets);
                }
            }
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains(styleClass)) {
                targets.add(child);
            }
            if (child instanceof Parent childParent) {
                collectNodesByStyleClass(childParent, styleClass, targets);
            }
        }
    }

    private void highlightNav(Button active) {
        for (Button b : getSidebarNavButtons()) {
            if (b == null) {
                continue;
            }
            b.getStyleClass().remove("admin-nav-button-active");
        }
        if (active != null && !active.getStyleClass().contains("admin-nav-button-active")) {
            active.getStyleClass().add("admin-nav-button-active");
        }
        applySidebarPalette(themeToggleButton != null && themeToggleButton.isSelected());
    }

    private void applySidebarPalette(boolean darkMode) {
        if (adminSidebarScroll != null) {
            adminSidebarScroll.setStyle(darkMode ? ADMIN_DARK_SIDEBAR_STYLE : "");
        }
        if (adminSidebarRoot != null) {
            adminSidebarRoot.setStyle(darkMode ? ADMIN_DARK_TRANSPARENT_STYLE : "");
        }
        if (sidebarToggleButton != null) {
            sidebarToggleButton.setStyle(darkMode ? ADMIN_DARK_SECONDARY_BUTTON_STYLE : "");
        }
        for (Button button : getSidebarNavButtons()) {
            if (button == null) {
                continue;
            }
            if (!darkMode) {
                button.setStyle("");
            } else if (button.getStyleClass().contains("admin-nav-button-active")) {
                button.setStyle(ADMIN_DARK_ACTIVE_BUTTON_STYLE);
            } else {
                button.setStyle(ADMIN_DARK_SECONDARY_BUTTON_STYLE);
            }
        }
        if (userUiButton != null) {
            userUiButton.setStyle(darkMode ? ADMIN_DARK_PRIMARY_BUTTON_STYLE : "");
        }
    }

    private Button[] getSidebarNavButtons() {
        return new Button[] {
                dashboardNavButton,
                equipesNavButton,
                joueursNavButton,
                matchsNavButton,
                annoncesNavButton,
                productsNavButton,
                ordersNavButton,
                entrainementsNavButton,
                sponsorsNavButton,
                usersNavButton
        };
    }

    private void configureSidebarCollapse() {
        if (adminSidebarScroll == null) {
            return;
        }

        adminSidebarScroll.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        adminSidebarScroll.setMaxWidth(SIDEBAR_EXPANDED_WIDTH);
        adminSidebarScroll.setPrefWidth(SIDEBAR_EXPANDED_WIDTH);
        updateSidebarToggleGlyph();
    }

    private void setSidebarCollapsed(boolean collapsed) {
        if (adminSidebarScroll == null || adminRoot == null) {
            return;
        }

        sidebarCollapsed = collapsed;
        List<Node> textNodes = collectSidebarTextNodes();

        if (collapsed) {
            adminSidebarScroll.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
            adminSidebarScroll.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);
            adminSidebarScroll.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
            if (sidebarHeaderSpacer != null) {
                sidebarHeaderSpacer.setManaged(false);
                sidebarHeaderSpacer.setVisible(false);
            }
            textNodes.forEach(node -> {
                node.setManaged(false);
                node.setVisible(false);
                node.setOpacity(0);
            });
            addStyleClass(adminRoot, "admin-shell-collapsed");
            updateSidebarToggleGlyph();
            return;
        }

        adminRoot.getStyleClass().remove("admin-shell-collapsed");
        adminSidebarScroll.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        adminSidebarScroll.setMaxWidth(SIDEBAR_EXPANDED_WIDTH);
        adminSidebarScroll.setPrefWidth(SIDEBAR_EXPANDED_WIDTH);
        if (sidebarHeaderSpacer != null) {
            sidebarHeaderSpacer.setManaged(true);
            sidebarHeaderSpacer.setVisible(true);
        }
        textNodes.forEach(node -> {
            node.setManaged(true);
            node.setVisible(true);
            node.setOpacity(1);
        });
        updateSidebarToggleGlyph();
    }

    private List<Node> collectSidebarTextNodes() {
        if (adminSidebarRoot == null) {
            return List.of();
        }

        List<Node> nodes = new ArrayList<>();
        collectNodesByStyleClasses(adminSidebarRoot,
                Set.of("admin-nav-text", "admin-sidebar-section", "admin-sidebar-theme-label", "admin-sidebar-title"),
                nodes);
        return nodes;
    }

    private void collectNodesByStyleClasses(Parent parent, Set<String> styleClasses, List<Node> targets) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            for (String styleClass : styleClasses) {
                if (child.getStyleClass().contains(styleClass)) {
                    targets.add(child);
                    break;
                }
            }
            if (child instanceof Parent childParent) {
                collectNodesByStyleClasses(childParent, styleClasses, targets);
            }
        }
    }

    private void updateSidebarToggleGlyph() {
        if (sidebarToggleButton != null) {
            sidebarToggleButton.setText(sidebarCollapsed ? ">" : "<");
        }
    }

    private void enableAdminPerformanceMode() {
        addStyleClass(adminRoot, ADMIN_PERFORMANCE_CLASS);
        addStyleClass(contentStack, ADMIN_PERFORMANCE_CLASS);
        addStyleClass(adminSidebarRoot, ADMIN_PERFORMANCE_CLASS);
        if (adminSidebarRoot != null) {
            adminSidebarRoot.setCache(true);
            adminSidebarRoot.setCacheHint(CacheHint.SPEED);
        }
    }

    private void addStyleClass(Node node, String styleClass) {
        if (node != null && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void applyControllerModeStyles(Object controller) {
        applyControllerModeStyles(controller, themeToggleButton != null && themeToggleButton.isSelected());
    }

    private void applyControllerModeStyles(Object controller, boolean darkMode) {
        if (controller instanceof AdminDashboardController adminDashboardController) {
            adminDashboardController.setDarkMode(darkMode);
        } else if (controller instanceof EquipeController equipeController) {
            equipeController.setDarkMode(darkMode);
        } else if (controller instanceof JoueurController joueurController) {
            joueurController.setDarkMode(darkMode);
        } else if (controller instanceof AdminUserModerationController adminUserModerationController) {
            adminUserModerationController.setDarkMode(darkMode);
        } else if (controller instanceof SponsorAdminController sponsorAdminController) {
            sponsorAdminController.setDarkMode(darkMode);
        } else if (controller instanceof ProductController productController) {
            productController.setDarkMode(darkMode);
        } else if (controller instanceof OrderController orderController) {
            orderController.setDarkMode(darkMode);
        }
    }

    private record LoadedWorkspace(Parent root, Object controller, boolean embeddedPage) {
    }
}
