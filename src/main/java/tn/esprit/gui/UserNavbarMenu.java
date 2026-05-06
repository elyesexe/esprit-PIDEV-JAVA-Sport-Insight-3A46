package tn.esprit.gui;

<<<<<<< HEAD
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
<<<<<<< HEAD
import javafx.scene.control.ToggleGroup;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
<<<<<<< HEAD
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import tn.esprit.entities.User;
import tn.esprit.i18n.I18n;
import tn.esprit.security.AuthSession;
=======
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.esprit.entities.User;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
<<<<<<< HEAD
import javafx.util.Duration;

public final class UserNavbarMenu {
    private static final String SETTINGS_MENU_KEY = "sportInsight.settingsMenuInjected";
    private static final String EXTRA_NAV_KEY = "sportInsight.extraNavKey";
=======

public final class UserNavbarMenu {
    private static final String SETTINGS_MENU_KEY = "sportInsight.settingsMenuInjected";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static final String PROFILE_VIEW = "/tn/esprit/views/profile-view.fxml";
    private static final String PROFILE_CSS = "/tn/esprit/styles/profile-theme.css";
    private static final String LOGIN_VIEW = "/tn/esprit/views/login-view.fxml";
    private static final String AUTH_CSS = "/tn/esprit/styles/auth-theme.css";
    private static final String TRAINING_VIEW = "/tn/esprit/views/entrainement-user-view.fxml";
    private static final String TRAINING_CSS = "/tn/esprit/styles/entrainement-theme.css";
    private static final String STORE_VIEW = "/tn/esprit/views/store-view.fxml";
    private static final String STORE_CSS = "/tn/esprit/styles/store-theme.css";
    private static final String SPONSOR_VIEW = "/tn/esprit/views/sponsor-user-view.fxml";
    private static final String SPONSOR_CSS = "/tn/esprit/styles/sponsor-theme.css";
<<<<<<< HEAD
    private static final String FOOTBALL_NEWS_VIEW = "/tn/esprit/views/football-news-view.fxml";
    private static final String FOOTBALL_NEWS_CSS = "/tn/esprit/styles/football-news-theme.css";
    private static final double SETTINGS_MENU_CONTENT_WIDTH = 218;
    private static final double SETTINGS_MENU_ACTION_WIDTH = 112;
    private static final double LANGUAGE_BUTTON_WIDTH = 76;
    private static final double LANGUAGE_BUTTON_SPACING = 8;
    private static final double LANGUAGE_INDICATOR_OFFSET = (LANGUAGE_BUTTON_WIDTH + LANGUAGE_BUTTON_SPACING) / 2.0;
=======
    private static final double SETTINGS_MENU_CONTENT_WIDTH = 218;
    private static final double SETTINGS_MENU_ACTION_WIDTH = 112;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

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
        ensureStoreNavButton(navbarRoot);
        ensureSponsorNavButton(navbarRoot);
<<<<<<< HEAD
        ensureFootballNewsNavButton(navbarRoot);
        ensureTrainingNavButton(navbarRoot);

        NavbarNotificationCenter notificationCenter = new NavbarNotificationCenter();
        Button alertsButton = notificationCenter.getButton();
=======
        ensureTrainingNavButton(navbarRoot);

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        Button settingsButton = createSettingsButton();
        Button profileAvatarButton = createProfileAvatarButton();
        ContextMenu settingsMenu = createSettingsMenu(settingsButton);

        settingsButton.setOnAction(event -> {
            if (settingsMenu.isShowing()) {
                settingsMenu.hide();
                return;
            }
            settingsMenu.show(settingsButton, Side.BOTTOM, 0, 8);
        });
        profileAvatarButton.setOnAction(event -> {
            String title = AuthSession.isAdmin() ? "Sport Insight | Admin profile" : "Sport Insight | Profile";
            SceneNavigator.switchScene(profileAvatarButton, PROFILE_VIEW, PROFILE_CSS, title);
        });

        Pane targetPane = themeToggleButton != null && themeToggleButton.getParent() instanceof Pane pane
                ? pane
                : navbarRoot;
        if (targetPane == null) {
            return;
        }

        if (targetPane.getChildren().stream().anyMatch(node ->
                node.getStyleClass().contains("navbar-settings-button")
                        || node.getStyleClass().contains("navbar-user-actions"))) {
            return;
        }

<<<<<<< HEAD
        HBox actionsBox = new HBox(10, alertsButton, settingsButton, profileAvatarButton);
=======
        HBox actionsBox = new HBox(10, settingsButton, profileAvatarButton);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.getStyleClass().add("navbar-user-actions");
        targetPane.getChildren().add(actionsBox);
        if (navbarRoot != null) {
            navbarRoot.getProperties().put(SETTINGS_MENU_KEY, Boolean.TRUE);
        }
    }

    private static Button createSettingsButton() {
<<<<<<< HEAD
        Button settingsButton = new Button(I18n.get("settings.title"));
=======
        Button settingsButton = new Button("Settings");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        settingsButton.setMnemonicParsing(false);
        settingsButton.setContentDisplay(ContentDisplay.LEFT);
        settingsButton.setFocusTraversable(false);
        settingsButton.getStyleClass().add("navbar-settings-button");

        Label icon = new Label("\u2699");
        icon.getStyleClass().add("navbar-settings-icon");
        settingsButton.setGraphic(icon);
        return settingsButton;
    }

    private static Button createProfileAvatarButton() {
        Button avatarButton = new Button();
        avatarButton.setMnemonicParsing(false);
        avatarButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        avatarButton.setFocusTraversable(false);
        avatarButton.getStyleClass().add("navbar-profile-avatar-button");

        User currentUser = AuthSession.getCurrentUser();
        String displayName = currentUser == null ? "Sport Insight user" : currentUser.getDisplayName();

        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("navbar-profile-avatar-shell");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(42);
        imageView.setFitHeight(42);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("navbar-profile-avatar-image");
        applyCircularClip(imageView);

        Label initialsLabel = new Label(buildInitials(displayName));
        initialsLabel.getStyleClass().add("navbar-profile-avatar-fallback");

        Image image = loadProfileImage(currentUser == null ? null : currentUser.getPhoto());
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);
        initialsLabel.setManaged(!hasImage);
        initialsLabel.setVisible(!hasImage);

        avatarShell.getChildren().addAll(imageView, initialsLabel);
        avatarButton.setGraphic(avatarShell);
<<<<<<< HEAD
        avatarButton.setAccessibleText(I18n.get("profile.open"));
=======
        avatarButton.setAccessibleText("Open profile");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        return avatarButton;
    }

    private static ContextMenu createSettingsMenu(Button ownerButton) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("settings-context-menu");

<<<<<<< HEAD
        ToggleButton menuThemeToggle = createThemeToggle();
        ThemeManager.bindToggle(menuThemeToggle);

        Label lightLabel = new Label(I18n.get("theme.light"));
        lightLabel.getStyleClass().add("settings-menu-mode-label");

        Label darkLabel = new Label(I18n.get("theme.dark"));
=======
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
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        darkLabel.getStyleClass().add("settings-menu-mode-label");

        HBox themeRow = new HBox(12, lightLabel, menuThemeToggle, darkLabel);
        themeRow.setAlignment(Pos.CENTER);
        themeRow.getStyleClass().add("settings-menu-row");
        themeRow.setMinWidth(SETTINGS_MENU_CONTENT_WIDTH);
        themeRow.setPrefWidth(SETTINGS_MENU_CONTENT_WIDTH);
        themeRow.setMaxWidth(SETTINGS_MENU_CONTENT_WIDTH);
        contextMenu.getItems().add(wrapNode(themeRow, false));
<<<<<<< HEAD
        contextMenu.getItems().add(wrapNode(createLanguageRow(ownerButton, contextMenu), false));

        if (AuthSession.isAdmin()) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(wrapActionButton(createActionButton(I18n.get("admin.title"), false, () -> {
=======

        if (AuthSession.isAdmin()) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(wrapActionButton(createActionButton("Admin", false, () -> {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                contextMenu.hide();
                AdminNavigation.openAdmin(ownerButton);
            }), true));
        }

        contextMenu.getItems().add(new SeparatorMenuItem());
<<<<<<< HEAD
        contextMenu.getItems().add(wrapActionButton(createActionButton(I18n.get("auth.logout"), true, () -> {
=======
        contextMenu.getItems().add(wrapActionButton(createActionButton("Logout", true, () -> {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            contextMenu.hide();
            AuthSession.logout();
            SceneNavigator.switchScene(ownerButton, LOGIN_VIEW, AUTH_CSS, "Sport Insight | Sign in");
        }), true));

        return contextMenu;
    }

<<<<<<< HEAD
    private static HBox createLanguageRow(Button ownerButton, ContextMenu contextMenu) {
        ToggleGroup languageGroup = new ToggleGroup();
        ToggleButton frenchButton = createLanguageButton(I18n.get("settings.language.french"), languageGroup);
        ToggleButton englishButton = createLanguageButton(I18n.get("settings.language.english"), languageGroup);
        Region selectionIndicator = new Region();
        selectionIndicator.getStyleClass().add("settings-language-indicator");

        Locale currentLocale = I18n.getLocale();
        if (Locale.ENGLISH.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
            englishButton.setSelected(true);
        } else {
            frenchButton.setSelected(true);
        }

        frenchButton.setOnAction(event -> switchLanguage(Locale.FRENCH, ownerButton, contextMenu));
        englishButton.setOnAction(event -> switchLanguage(Locale.ENGLISH, ownerButton, contextMenu));

        HBox languageButtons = new HBox(8, frenchButton, englishButton);
        languageButtons.setAlignment(Pos.CENTER);
        HBox.setHgrow(languageButtons, Priority.ALWAYS);
        languageButtons.getStyleClass().add("settings-language-switch");

        StackPane languageSwitchShell = new StackPane(selectionIndicator, languageButtons);
        languageSwitchShell.setAlignment(Pos.CENTER);
        languageSwitchShell.getStyleClass().add("settings-language-switch-shell");

        languageGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) ->
                updateLanguageSelection(selectionIndicator, frenchButton, englishButton, newToggle == englishButton));
        Platform.runLater(() ->
                updateLanguageSelection(selectionIndicator, frenchButton, englishButton, englishButton.isSelected()));

        HBox languageRow = new HBox(languageSwitchShell);
        languageRow.setAlignment(Pos.CENTER);
        languageRow.getStyleClass().add("settings-menu-row");
        languageRow.setMinWidth(SETTINGS_MENU_CONTENT_WIDTH);
        languageRow.setPrefWidth(SETTINGS_MENU_CONTENT_WIDTH);
        languageRow.setMaxWidth(SETTINGS_MENU_CONTENT_WIDTH);
        return languageRow;
    }

    private static ToggleButton createLanguageButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setMnemonicParsing(false);
        button.setFocusTraversable(false);
        button.setMinWidth(LANGUAGE_BUTTON_WIDTH);
        button.setPrefWidth(LANGUAGE_BUTTON_WIDTH);
        button.setMaxWidth(LANGUAGE_BUTTON_WIDTH);
        button.getStyleClass().add("settings-menu-action");
        button.getStyleClass().add("settings-language-toggle");
        return button;
    }

    private static void updateLanguageSelection(Region selectionIndicator,
                                                ToggleButton frenchButton,
                                                ToggleButton englishButton,
                                                boolean englishSelected) {
        if (selectionIndicator == null || frenchButton == null || englishButton == null) {
            return;
        }

        double targetTranslateX = englishSelected ? LANGUAGE_INDICATOR_OFFSET : -LANGUAGE_INDICATOR_OFFSET;

        TranslateTransition slide = new TranslateTransition(Duration.millis(190), selectionIndicator);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        slide.setToX(targetTranslateX);
        slide.play();

        applyLanguageButtonState(frenchButton, !englishSelected);
        applyLanguageButtonState(englishButton, englishSelected);
    }

    private static void applyLanguageButtonState(ToggleButton button, boolean active) {
        if (button == null) {
            return;
        }

        if (active) {
            if (!button.getStyleClass().contains("settings-language-active")) {
                button.getStyleClass().add("settings-language-active");
            }
        } else {
            button.getStyleClass().remove("settings-language-active");
        }

        ScaleTransition scale = new ScaleTransition(Duration.millis(170), button);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        scale.setToX(active ? 1.04 : 1.0);
        scale.setToY(active ? 1.04 : 1.0);
        scale.play();
    }

    private static void switchLanguage(Locale locale, Button ownerButton, ContextMenu contextMenu) {
        Locale normalizedLocale = I18n.normalize(locale);
        if (normalizedLocale.equals(I18n.getLocale())) {
            return;
        }

        I18n.setLocale(normalizedLocale);
        contextMenu.hide();
        if (ownerButton != null && ownerButton.getScene() != null && ownerButton.getScene().getWindow() instanceof javafx.stage.Stage stage) {
            SceneNavigator.reloadCurrentScene(stage);
        }
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
                .anyMatch(button -> matchesButton(button, "training", "entrainements", "training"));
=======
                .anyMatch(button -> "Entrainements".equalsIgnoreCase(button.getText()));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (exists) {
            return;
        }

<<<<<<< HEAD
        Button trainingButton = new Button(I18n.get("nav.training"));
        trainingButton.setMnemonicParsing(false);
        trainingButton.getStyleClass().add("navbar-nav-button");
        trainingButton.getProperties().put(EXTRA_NAV_KEY, "training");
=======
        Button trainingButton = new Button("Entrainements");
        trainingButton.setMnemonicParsing(false);
        trainingButton.getStyleClass().add("navbar-nav-button");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

    private static void ensureStoreNavButton(HBox navbarRoot) {
        HBox modules = findModulesContainer(navbarRoot);
        if (modules == null) {
            return;
        }

        boolean exists = modules.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
<<<<<<< HEAD
                .anyMatch(button -> matchesButton(button, "store", "store", "products", "product", "boutique"));
=======
                .anyMatch(button -> {
                    String label = button.getText() == null ? "" : button.getText().trim().toLowerCase();
                    return "store".equals(label) || "product".equals(label) || "products".equals(label);
                });
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (exists) {
            return;
        }

<<<<<<< HEAD
        Button storeButton = new Button(I18n.get("nav.store"));
        storeButton.setMnemonicParsing(false);
        storeButton.getStyleClass().add("navbar-nav-button");
        storeButton.getProperties().put(EXTRA_NAV_KEY, "store");
=======
        Button storeButton = new Button("Store");
        storeButton.setMnemonicParsing(false);
        storeButton.getStyleClass().add("navbar-nav-button");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        storeButton.setOnAction(event ->
                SceneNavigator.switchScene(storeButton, STORE_VIEW, STORE_CSS, "Store | Sport Insight"));

        int insertIndex = modules.getChildren().size();
        for (int i = 0; i < modules.getChildren().size(); i++) {
            Node node = modules.getChildren().get(i);
            if (node instanceof Button button) {
                String label = button.getText() == null ? "" : button.getText().toLowerCase();
                if (label.contains("annonc") || label.contains("sponsor") || label.contains("entrain")) {
                    insertIndex = i;
                    break;
                }
            }
        }
        modules.getChildren().add(insertIndex, storeButton);
    }

    private static void ensureSponsorNavButton(HBox navbarRoot) {
        HBox modules = findModulesContainer(navbarRoot);
        if (modules == null) {
            return;
        }

        boolean exists = modules.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
<<<<<<< HEAD
                .anyMatch(button -> matchesButton(button, "sponsor", "sponsors", "sponsoring"));
=======
                .anyMatch(button -> "Sponsors".equalsIgnoreCase(button.getText()) || "Sponsoring".equalsIgnoreCase(button.getText()));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (exists) {
            return;
        }

<<<<<<< HEAD
        Button sponsorButton = new Button(I18n.get("nav.sponsors"));
        sponsorButton.setMnemonicParsing(false);
        sponsorButton.getStyleClass().add("navbar-nav-button");
        sponsorButton.getProperties().put(EXTRA_NAV_KEY, "sponsor");
=======
        Button sponsorButton = new Button("Sponsors");
        sponsorButton.setMnemonicParsing(false);
        sponsorButton.getStyleClass().add("navbar-nav-button");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

<<<<<<< HEAD
    private static void ensureFootballNewsNavButton(HBox navbarRoot) {
        HBox modules = findModulesContainer(navbarRoot);
        if (modules == null) {
            return;
        }

        boolean exists = modules.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(button -> matchesButton(button, "news", "sport insight news", "football news", "news", "actualites"));
        if (exists) {
            return;
        }

        Button newsButton = new Button(I18n.get("nav.news"));
        newsButton.setMnemonicParsing(false);
        newsButton.getStyleClass().add("navbar-nav-button");
        newsButton.getProperties().put(EXTRA_NAV_KEY, "news");
        newsButton.setOnAction(event ->
                SceneNavigator.switchScene(newsButton, FOOTBALL_NEWS_VIEW, FOOTBALL_NEWS_CSS, "Sport Insight News | Sport Insight"));

        int insertIndex = modules.getChildren().size();
        for (int i = 0; i < modules.getChildren().size(); i++) {
            Node node = modules.getChildren().get(i);
            if (node instanceof Button button) {
                String label = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (label.contains("annonc") || label.contains("anonce") || label.contains("entrain")) {
                    insertIndex = i;
                    break;
                }
            }
        }
        modules.getChildren().add(insertIndex, newsButton);
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

<<<<<<< HEAD
    private static boolean matchesButton(Button button, String extraNavKey, String... labels) {
        if (button == null) {
            return false;
        }
        Object propertyValue = button.getProperties().get(EXTRA_NAV_KEY);
        if (extraNavKey.equals(propertyValue)) {
            return true;
        }
        String normalizedText = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
        for (String label : labels) {
            if (normalizedText.equals(label.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

    private static void applyCircularClip(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        Circle clip = new Circle();
        clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));
        imageView.setClip(clip);
    }

    private static Image loadProfileImage(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            String candidate = rawPath.trim();
            if (candidate.startsWith("http://") || candidate.startsWith("https://") || candidate.startsWith("file:")) {
                Image image = new Image(candidate, false);
                return image.isError() ? null : image;
            }
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                Image image = new Image(path.toUri().toString(), false);
                return image.isError() ? null : image;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String buildInitials(String displayName) {
        String safeName = displayName == null || displayName.isBlank() ? "Sport Insight" : displayName.trim();
        String[] parts = safeName.split("\\s+");
        if (parts.length == 1) {
            return safeName.substring(0, Math.min(2, safeName.length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
