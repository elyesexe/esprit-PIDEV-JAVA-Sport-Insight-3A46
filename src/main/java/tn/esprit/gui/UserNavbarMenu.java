package tn.esprit.gui;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.User;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;

import java.lang.reflect.Field;

public final class UserNavbarMenu {
    private static final String SETTINGS_MENU_KEY = "sportInsight.settingsMenuInjected";
    private static final String PROFILE_VIEW = "/tn/esprit/views/profile-view.fxml";
    private static final String PROFILE_CSS = "/tn/esprit/styles/profile-theme.css";
    private static final String LOGIN_VIEW = "/tn/esprit/views/login-view.fxml";
    private static final String AUTH_CSS = "/tn/esprit/styles/auth-theme.css";
    private static final String TRAINING_VIEW = "/tn/esprit/views/entrainement-user-view.fxml";
    private static final String TRAINING_CSS = "/tn/esprit/styles/entrainement-theme.css";
    private static final String SPONSOR_VIEW = "/tn/esprit/views/sponsor-user-view.fxml";
    private static final String SPONSOR_CSS = "/tn/esprit/styles/sponsor-theme.css";
    private static final double SETTINGS_MENU_CONTENT_WIDTH = 218;
    private static final double SETTINGS_MENU_ACTION_WIDTH = 112;

    private UserNavbarMenu() {
    }

    public static void configureLoadedController(Object controller) {
        if (controller == null || !AuthSession.isAuthenticated()) {
            return;
        }

        HBox navbarRoot = getFieldValue(controller, "navbarRoot", HBox.class);
        Button adminNavButton = getFieldValue(controller, "adminNavButton", Button.class);
        ToggleButton themeToggleButton = getFieldValue(controller, "themeToggleButton", ToggleButton.class);

        if (navbarRoot == null) {
            return;
        }
        if (navbarRoot != null && Boolean.TRUE.equals(navbarRoot.getProperties().get(SETTINGS_MENU_KEY))) {
            hideNode(adminNavButton);
            hideNode(themeToggleButton);
            return;
        }

        hideNode(adminNavButton);
        hideNode(themeToggleButton);
        ensureSponsorNavButton(navbarRoot);
        ensureTrainingNavButton(navbarRoot);

        Button settingsButton = createSettingsButton();
        ContextMenu settingsMenu = createSettingsMenu(settingsButton);

        settingsButton.setOnAction(event -> {
            if (settingsMenu.isShowing()) {
                settingsMenu.hide();
                return;
            }
            settingsMenu.show(settingsButton, Side.BOTTOM, 0, 8);
        });

        Pane targetPane = themeToggleButton != null && themeToggleButton.getParent() instanceof Pane pane
                ? pane
                : navbarRoot;
        if (targetPane == null) {
            return;
        }

        if (targetPane.getChildren().stream().anyMatch(node -> node.getStyleClass().contains("navbar-settings-button"))) {
            return;
        }

        targetPane.getChildren().add(settingsButton);
        if (navbarRoot != null) {
            navbarRoot.getProperties().put(SETTINGS_MENU_KEY, Boolean.TRUE);
        }
    }

    private static Button createSettingsButton() {
        Button settingsButton = new Button("Settings");
        settingsButton.setMnemonicParsing(false);
        settingsButton.setContentDisplay(ContentDisplay.LEFT);
        settingsButton.setFocusTraversable(false);
        settingsButton.getStyleClass().add("navbar-settings-button");

        Label icon = new Label("\u2699");
        icon.getStyleClass().add("navbar-settings-icon");
        settingsButton.setGraphic(icon);
        return settingsButton;
    }

    private static ContextMenu createSettingsMenu(Button ownerButton) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("settings-context-menu");

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser != null) {
            VBox accountBox = new VBox(2);
            accountBox.getStyleClass().add("settings-menu-panel");
            accountBox.setAlignment(Pos.CENTER);
            accountBox.setMinWidth(SETTINGS_MENU_CONTENT_WIDTH);
            accountBox.setPrefWidth(SETTINGS_MENU_CONTENT_WIDTH);
            accountBox.setMaxWidth(SETTINGS_MENU_CONTENT_WIDTH);

            Label nameLabel = new Label(currentUser.getDisplayName());
            nameLabel.getStyleClass().add("settings-menu-user");

            Label roleLabel = new Label(UserRoles.displayName(currentUser.getPrimaryRole()));
            roleLabel.getStyleClass().add("settings-menu-role");

            accountBox.getChildren().addAll(nameLabel, roleLabel);
            contextMenu.getItems().add(wrapNode(accountBox, false));
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        ToggleButton menuThemeToggle = createThemeToggle();
        ThemeManager.bindToggle(menuThemeToggle);

        Label lightLabel = new Label("Light");
        lightLabel.getStyleClass().add("settings-menu-mode-label");

        Label darkLabel = new Label("Dark");
        darkLabel.getStyleClass().add("settings-menu-mode-label");

        HBox themeRow = new HBox(12, lightLabel, menuThemeToggle, darkLabel);
        themeRow.setAlignment(Pos.CENTER);
        themeRow.getStyleClass().add("settings-menu-row");
        themeRow.setMinWidth(SETTINGS_MENU_CONTENT_WIDTH);
        themeRow.setPrefWidth(SETTINGS_MENU_CONTENT_WIDTH);
        themeRow.setMaxWidth(SETTINGS_MENU_CONTENT_WIDTH);
        contextMenu.getItems().add(wrapNode(themeRow, false));

        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(wrapActionButton(createActionButton("Profile", false, () -> {
            contextMenu.hide();
            String title = AuthSession.isAdmin() ? "Sport Insight | Admin profile" : "Sport Insight | Profile";
            SceneNavigator.switchScene(ownerButton, PROFILE_VIEW, PROFILE_CSS, title);
        }), true));

        if (AuthSession.isAdmin()) {
            contextMenu.getItems().add(wrapActionButton(createActionButton("Admin", false, () -> {
                contextMenu.hide();
                AdminNavigation.openAdmin(ownerButton);
            }), true));
        }

        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(wrapActionButton(createActionButton("Logout", true, () -> {
            contextMenu.hide();
            AuthSession.logout();
            SceneNavigator.switchScene(ownerButton, LOGIN_VIEW, AUTH_CSS, "Sport Insight | Sign in");
        }), true));

        return contextMenu;
    }

    private static Button createActionButton(String text, boolean danger, Runnable action) {
        Button actionButton = new Button(text);
        actionButton.setMnemonicParsing(false);
        actionButton.setFocusTraversable(false);
        actionButton.setMinWidth(SETTINGS_MENU_ACTION_WIDTH);
        actionButton.setPrefWidth(SETTINGS_MENU_ACTION_WIDTH);
        actionButton.setMaxWidth(SETTINGS_MENU_ACTION_WIDTH);
        actionButton.setAlignment(Pos.CENTER);
        actionButton.getStyleClass().add("settings-menu-action");
        if (danger) {
            actionButton.getStyleClass().add("settings-menu-danger");
        }
        actionButton.setOnAction(event -> action.run());
        return actionButton;
    }

    private static CustomMenuItem wrapActionButton(Button button, boolean hideOnClick) {
        HBox wrapper = new HBox(button);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.getStyleClass().add("settings-menu-action-row");
        wrapper.setMinWidth(SETTINGS_MENU_CONTENT_WIDTH);
        wrapper.setPrefWidth(SETTINGS_MENU_CONTENT_WIDTH);
        wrapper.setMaxWidth(SETTINGS_MENU_CONTENT_WIDTH);
        return wrapNode(wrapper, hideOnClick);
    }

    private static CustomMenuItem wrapNode(Node node, boolean hideOnClick) {
        CustomMenuItem item = new CustomMenuItem(node, hideOnClick);
        item.getStyleClass().add("settings-menu-item");
        item.setHideOnClick(hideOnClick);
        return item;
    }

    private static ToggleButton createThemeToggle() {
        ToggleButton toggleButton = new ToggleButton();
        toggleButton.setMnemonicParsing(false);
        toggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggleButton.setFocusTraversable(false);
        toggleButton.getStyleClass().add("theme-toggle");
        toggleButton.getStyleClass().add("settings-theme-toggle");

        StackPane iconShell = new StackPane();
        iconShell.getStyleClass().add("theme-toggle-icon-shell");

        Label sunLabel = new Label("\u263c");
        sunLabel.getStyleClass().addAll("theme-toggle-icon", "theme-toggle-sun");

        Label moonLabel = new Label("\u263e");
        moonLabel.getStyleClass().addAll("theme-toggle-icon", "theme-toggle-moon");

        iconShell.getChildren().addAll(sunLabel, moonLabel);

        StackPane thumb = new StackPane(iconShell);
        thumb.getStyleClass().add("theme-toggle-thumb");

        StackPane track = new StackPane(thumb);
        track.setAlignment(Pos.CENTER_LEFT);
        track.getStyleClass().add("theme-toggle-track");

        toggleButton.setGraphic(track);
        return toggleButton;
    }

    private static void hideNode(Node node) {
        if (node == null) {
            return;
        }
        node.setManaged(false);
        node.setVisible(false);
    }

    private static void ensureTrainingNavButton(HBox navbarRoot) {
        HBox modules = findModulesContainer(navbarRoot);
        if (modules == null) {
            return;
        }

        boolean exists = modules.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(button -> "Entrainements".equalsIgnoreCase(button.getText()));
        if (exists) {
            return;
        }

        Button trainingButton = new Button("Entrainements");
        trainingButton.setMnemonicParsing(false);
        trainingButton.getStyleClass().add("navbar-nav-button");
        trainingButton.setOnAction(event ->
                SceneNavigator.switchScene(trainingButton, TRAINING_VIEW, TRAINING_CSS, "Entrainements | Sport Insight"));

        int insertIndex = modules.getChildren().size();
        for (int i = 0; i < modules.getChildren().size(); i++) {
            Node node = modules.getChildren().get(i);
            if (node instanceof Button button) {
                String label = button.getText() == null ? "" : button.getText().toLowerCase();
                if (label.contains("annonc")) {
                    insertIndex = i;
                    break;
                }
            }
        }
        modules.getChildren().add(insertIndex, trainingButton);
    }

    private static void ensureSponsorNavButton(HBox navbarRoot) {
        HBox modules = findModulesContainer(navbarRoot);
        if (modules == null) {
            return;
        }

        boolean exists = modules.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(button -> "Sponsors".equalsIgnoreCase(button.getText()) || "Sponsoring".equalsIgnoreCase(button.getText()));
        if (exists) {
            return;
        }

        Button sponsorButton = new Button("Sponsors");
        sponsorButton.setMnemonicParsing(false);
        sponsorButton.getStyleClass().add("navbar-nav-button");
        sponsorButton.setOnAction(event ->
                SceneNavigator.switchScene(sponsorButton, SPONSOR_VIEW, SPONSOR_CSS, "Sponsors | Sport Insight"));

        int insertIndex = modules.getChildren().size();
        for (int i = 0; i < modules.getChildren().size(); i++) {
            Node node = modules.getChildren().get(i);
            if (node instanceof Button button) {
                String label = button.getText() == null ? "" : button.getText().toLowerCase();
                if (label.contains("annonc") || label.contains("entrain")) {
                    insertIndex = i;
                    break;
                }
            }
        }
        modules.getChildren().add(insertIndex, sponsorButton);
    }

    private static HBox findModulesContainer(HBox navbarRoot) {
        if (navbarRoot == null) {
            return null;
        }
        if (navbarRoot.getStyleClass().contains("navbar-modules")) {
            return navbarRoot;
        }
        for (Node child : navbarRoot.getChildren()) {
            HBox found = findModulesContainer(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static HBox findModulesContainer(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof HBox hBox && hBox.getStyleClass().contains("navbar-modules")) {
            return hBox;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findModulesContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T getFieldValue(Object controller, String fieldName, Class<T> type) {
        Field field = findField(controller.getClass(), fieldName);
        if (field == null || !type.isAssignableFrom(field.getType())) {
            return null;
        }

        try {
            field.setAccessible(true);
            Object candidate = field.get(controller);
            return type.isInstance(candidate) ? type.cast(candidate) : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
