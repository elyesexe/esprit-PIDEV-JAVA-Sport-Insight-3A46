package tn.esprit.Controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javafx.util.Duration;
/**
 * Admin layout: left sidebar (dashboard + CRUD + return to user UI), center hosts panels.
 * CRUD panels reuse existing FXML; the user navbar strip is removed so only the workspace shows.
 */
public class AdminShellController {
    private static final String ADMIN_LIGHT_CLASS = "admin-light";
    private static final String ADMIN_DARK_CLASS = "admin-dark";
    private static final String ADMIN_WORKSPACE_STYLESHEET = "/tn/esprit/styles/admin-theme-fixed.css";
    private static final String ADMIN_DARK_BACKGROUND_STYLE =
            "-fx-background-color: " +
                    "radial-gradient(center 14% 10%, radius 46%, rgba(221, 110, 255, 0.30) 0%, rgba(221, 110, 255, 0.10) 48%, transparent 49%), " +
                    "radial-gradient(center 88% 14%, radius 34%, rgba(87, 213, 255, 0.18) 0%, rgba(87, 213, 255, 0.05) 46%, transparent 47%), " +
                    "linear-gradient(from 0% 0% to 100% 100%, #1a1246 0%, #24175b 48%, #2c1d70 100%); " +
                    "-fx-background-insets: 0; " +
                    "-fx-background-radius: 0; " +
                    "-fx-border-color: transparent;";

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
    private static final Duration SIDEBAR_ANIMATION_DURATION = Duration.millis(220);

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
    private Timeline sidebarTimeline;
    private boolean sidebarCollapsed;

    @FXML
    public void initialize() {
        ThemeManager.bindToggle(themeToggleButton);
        themeToggleButton.selectedProperty().addListener((observable, oldValue, selected) -> applyAdminModeStyles(selected));
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
        URL url = AdminShellController.class.getResource(DASHBOARD);
        if (url == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent panel = loader.load();
            activeContentController = loader.getController();
            panel.getStyleClass().add("admin-workspace-root");
            attachWorkspaceStylesheet(panel);
            applyWorkspaceModeStyles(panel);
            applyControllerModeStyles(activeContentController);
            contentStack.getChildren().setAll(Collections.singletonList(panel));
        } catch (IOException e) {
            e.printStackTrace();
        }
        highlightNav(dashboardNavButton);
    }

    private void loadStrippedCrud(String resourcePath) {
        URL url = AdminShellController.class.getResource(resourcePath);
        if (url == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            BorderPane root = loader.load();
            activeContentController = loader.getController();
            root.setTop(null);
            if (!root.getStyleClass().contains("admin-workspace-root")) {
                root.getStyleClass().add("admin-workspace-root");
            }
            attachWorkspaceStylesheet(root);
            stripNodesByStyleClass(root, "hero-shell");
            stripNodesByStyleClass(root, "home-hero-shell");
            applyWorkspaceModeStyles(root);
            applyControllerModeStyles(activeContentController);
            contentStack.getChildren().setAll(Collections.singletonList(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyAdminModeStyles(boolean darkMode) {
        if (adminRoot == null) {
            return;
        }
        adminRoot.getStyleClass().removeAll("theme-light", "theme-dark");
        adminRoot.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
        adminRoot.getStyleClass().removeAll(ADMIN_LIGHT_CLASS, ADMIN_DARK_CLASS);
        adminRoot.getStyleClass().add(darkMode ? ADMIN_DARK_CLASS : ADMIN_LIGHT_CLASS);
        adminRoot.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
        if (contentStack != null) {
            contentStack.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
        }
        for (Node child : contentStack.getChildren()) {
            applyWorkspaceModeStyles(child, darkMode);
        }
        applyControllerModeStyles(activeContentController, darkMode);
    }

    private void applyWorkspaceModeStyles(Node node) {
        applyWorkspaceModeStyles(node, themeToggleButton != null && themeToggleButton.isSelected());
    }

    private void applyWorkspaceModeStyles(Node node, boolean darkMode) {
        if (node == null) {
            return;
        }
        node.getStyleClass().removeAll("theme-light", "theme-dark");
        node.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
        node.getStyleClass().removeAll(ADMIN_LIGHT_CLASS, ADMIN_DARK_CLASS);
        node.getStyleClass().add(darkMode ? ADMIN_DARK_CLASS : ADMIN_LIGHT_CLASS);
        node.setStyle(darkMode ? ADMIN_DARK_BACKGROUND_STYLE : "");
    }

    private void attachWorkspaceStylesheet(Parent root) {
        if (root == null) {
            return;
        }
        URL stylesheet = AdminShellController.class.getResource(ADMIN_WORKSPACE_STYLESHEET);
        if (stylesheet == null) {
            return;
        }
        String stylesheetUrl = stylesheet.toExternalForm();
        root.getStylesheets().remove(stylesheetUrl);
        root.getStylesheets().add(stylesheetUrl);
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
        for (Button b : new Button[] {
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
        }) {
            if (b == null) {
                continue;
            }
            b.getStyleClass().remove("admin-nav-button-active");
        }
        if (active != null && !active.getStyleClass().contains("admin-nav-button-active")) {
            active.getStyleClass().add("admin-nav-button-active");
        }
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

        if (sidebarTimeline != null) {
            sidebarTimeline.stop();
        }

        sidebarCollapsed = collapsed;
        List<Node> textNodes = collectSidebarTextNodes();

        if (collapsed) {
            sidebarTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(adminSidebarScroll.prefWidthProperty(), adminSidebarScroll.getWidth() > 0 ? adminSidebarScroll.getWidth() : SIDEBAR_EXPANDED_WIDTH, Interpolator.EASE_BOTH)
                    ),
                    new KeyFrame(SIDEBAR_ANIMATION_DURATION,
                            new KeyValue(adminSidebarScroll.prefWidthProperty(), SIDEBAR_COLLAPSED_WIDTH, Interpolator.EASE_BOTH)
                    )
            );
            textNodes.forEach(node -> node.setOpacity(1));
            sidebarTimeline.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                double progress = Math.min(1.0, newValue.toMillis() / SIDEBAR_ANIMATION_DURATION.toMillis());
                double opacity = 1.0 - progress;
                textNodes.forEach(node -> node.setOpacity(opacity));
            });
            sidebarTimeline.setOnFinished(event -> {
                if (sidebarHeaderSpacer != null) {
                    sidebarHeaderSpacer.setManaged(false);
                    sidebarHeaderSpacer.setVisible(false);
                }
                textNodes.forEach(node -> {
                    node.setManaged(false);
                    node.setVisible(false);
                    node.setOpacity(0);
                });
                if (!adminRoot.getStyleClass().contains("admin-shell-collapsed")) {
                    adminRoot.getStyleClass().add("admin-shell-collapsed");
                }
                updateSidebarToggleGlyph();
            });
            sidebarTimeline.play();
            return;
        }

        adminRoot.getStyleClass().remove("admin-shell-collapsed");
        if (sidebarHeaderSpacer != null) {
            sidebarHeaderSpacer.setManaged(true);
            sidebarHeaderSpacer.setVisible(true);
        }
        textNodes.forEach(node -> {
            node.setManaged(true);
            node.setVisible(true);
            node.setOpacity(0);
        });
        sidebarTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(adminSidebarScroll.prefWidthProperty(), adminSidebarScroll.getWidth() > 0 ? adminSidebarScroll.getWidth() : SIDEBAR_COLLAPSED_WIDTH, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(SIDEBAR_ANIMATION_DURATION,
                        new KeyValue(adminSidebarScroll.prefWidthProperty(), SIDEBAR_EXPANDED_WIDTH, Interpolator.EASE_BOTH)
                )
        );
        sidebarTimeline.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            double progress = Math.min(1.0, newValue.toMillis() / SIDEBAR_ANIMATION_DURATION.toMillis());
            textNodes.forEach(node -> node.setOpacity(progress));
        });
        sidebarTimeline.setOnFinished(event -> {
            textNodes.forEach(node -> node.setOpacity(1));
            updateSidebarToggleGlyph();
        });
        sidebarTimeline.play();
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
            sidebarToggleButton.setText(sidebarCollapsed ? "›" : "‹");
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
}
