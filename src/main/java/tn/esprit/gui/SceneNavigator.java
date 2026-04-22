package tn.esprit.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.stage.Stage;
import tn.esprit.assistant.AssistantOverlay;
import tn.esprit.assistant.AssistantService;
import tn.esprit.security.AuthSession;

import java.net.URL;
import java.util.Set;
import java.util.function.Consumer;

public final class SceneNavigator {
    private static final String LOGIN_VIEW = "/tn/esprit/views/login-view.fxml";
    private static final String SIGNUP_VIEW = "/tn/esprit/views/signup-view.fxml";
    private static final String AUTH_CSS = "/tn/esprit/styles/auth-theme.css";
    private static final String HOME_VIEW = "/tn/esprit/views/home-view.fxml";
    private static final String HOME_CSS = "/tn/esprit/styles/home-theme.css";
    private static final String HOME_TITLE = "Sport Insight | Accueil";
    private static final String ASSISTANT_CSS = "/tn/esprit/styles/assistant-theme.css";
    private static final String POPUP_DARK_CSS = "popup-dark.css";
    private static final String POPUP_LIGHT_CSS = "popup-light.css";

    private static final Set<String> PUBLIC_VIEWS = Set.of(LOGIN_VIEW, SIGNUP_VIEW);
    private static final Set<String> ADMIN_VIEWS = Set.of(
            "/tn/esprit/views/admin-shell.fxml",
            "/tn/esprit/views/admin-dashboard.fxml",
            "/tn/esprit/views/admin-users-view.fxml",
            "/tn/esprit/views/equipe-crud-view.fxml",
            "/tn/esprit/views/joueur-admin-view.fxml",
            "/tn/esprit/views/match-admin-view.fxml",
            "/tn/esprit/views/annonce-crud-view.fxml",
            "/tn/esprit/views/product-crud-view.fxml",
            "/tn/esprit/views/entrainement-admin-view.fxml",
            "/tn/esprit/views/sponsor-admin-view.fxml",
            "/tn/esprit/views/equipe-form-view.fxml",
            "/tn/esprit/views/match-form-view.fxml"
    );

    private SceneNavigator() {
    }

    public static void setScene(Stage stage, String fxmlPath, String cssPath, String title) {
        setScene(stage, fxmlPath, cssPath, title, null);
    }

    public static void setScene(Stage stage, String fxmlPath, String cssPath, String title, Consumer<Object> controllerConfigurer) {
        if (stage == null) {
            return;
        }
        if (!ensureAccess(stage, fxmlPath, null)) {
            return;
        }
        setSceneInternal(stage, fxmlPath, cssPath, title, controllerConfigurer);
    }

    public static void switchScene(Control source, String fxmlPath, String cssPath, String title) {
        switchScene((Node) source, fxmlPath, cssPath, title);
    }

    public static void switchScene(Node source, String fxmlPath, String cssPath, String title) {
        switchScene(source, fxmlPath, cssPath, title, null);
    }

    public static void switchScene(Control source, String fxmlPath, String cssPath, String title, Consumer<Object> controllerConfigurer) {
        switchScene((Node) source, fxmlPath, cssPath, title, controllerConfigurer);
    }

    public static void switchScene(Node source, String fxmlPath, String cssPath, String title, Consumer<Object> controllerConfigurer) {
        if (source == null || source.getScene() == null) {
            return;
        }

        Stage stage = (Stage) source.getScene().getWindow();
        if (!ensureAccess(stage, fxmlPath, source)) {
            return;
        }
        setSceneInternal(stage, fxmlPath, cssPath, title, controllerConfigurer);
    }

    private static void setSceneInternal(Stage stage, String fxmlPath, String cssPath, String title, Consumer<Object> controllerConfigurer) {
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1180;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 820;
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            if (controllerConfigurer != null) {
                controllerConfigurer.accept(loader.getController());
            }
            AuthSession.configureLoadedController(loader.getController());
            boolean publicView = PUBLIC_VIEWS.contains(fxmlPath);
            if (publicView) {
                AssistantService.getInstance().setWakeWordListener(null);
                AssistantService.getInstance().setPanelOpen(false);
            }
            Parent sceneRoot = publicView
                    ? root
                    : AssistantOverlay.wrap(root, stage, fxmlPath, title, loader.getController());
            URL stylesheet = SceneNavigator.class.getResource(cssPath);
            URL assistantStylesheet = SceneNavigator.class.getResource(ASSISTANT_CSS);
            String pageStylesheet = stylesheet == null ? null : stylesheet.toExternalForm();
            String assistantStylesheetUrl = publicView || assistantStylesheet == null ? null : assistantStylesheet.toExternalForm();

            Scene existingScene = stage.getScene();
            if (existingScene == null) {
                Scene scene = new Scene(sceneRoot, width, height);
                configureSceneStylesheets(scene, pageStylesheet, assistantStylesheetUrl);
                ThemeManager.registerScene(scene);
                stage.setScene(scene);
            } else {
                existingScene.setRoot(sceneRoot);
                configureSceneStylesheets(existingScene, pageStylesheet, assistantStylesheetUrl);
                ThemeManager.registerScene(existingScene);
            }
            stage.setTitle(title);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la page demandee.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private static void configureSceneStylesheets(Scene scene, String pageStylesheet, String assistantStylesheet) {
        if (scene == null) {
            return;
        }

        scene.getStylesheets().removeIf(SceneNavigator::isNavigationManagedStylesheet);
        if (pageStylesheet != null && !scene.getStylesheets().contains(pageStylesheet)) {
            scene.getStylesheets().add(pageStylesheet);
        }
        if (assistantStylesheet != null && !scene.getStylesheets().contains(assistantStylesheet)) {
            scene.getStylesheets().add(assistantStylesheet);
        }
    }

    private static boolean isNavigationManagedStylesheet(String stylesheet) {
        if (stylesheet == null) {
            return false;
        }
        if (!stylesheet.contains("/tn/esprit/styles/")) {
            return false;
        }
        return !stylesheet.endsWith(POPUP_DARK_CSS) && !stylesheet.endsWith(POPUP_LIGHT_CSS);
    }

    private static boolean ensureAccess(Stage stage, String fxmlPath, Node source) {
        if (PUBLIC_VIEWS.contains(fxmlPath)) {
            return true;
        }

        if (!AuthSession.isAuthenticated()) {
            showAccessAlert(stage, "Authentication required", "Please sign in to continue.");
            AuthSession.logout();
            setSceneInternal(stage, LOGIN_VIEW, AUTH_CSS, "Sport Insight | Sign in", null);
            return false;
        }

        if (ADMIN_VIEWS.contains(fxmlPath) && !AuthSession.isAdmin()) {
            showAccessAlert(stage, "Access denied", "Only admins can access the dashboard and moderation views.");
            setSceneInternal(stage, HOME_VIEW, HOME_CSS, HOME_TITLE, null);
            return false;
        }

        return true;
    }

    private static void showAccessAlert(Stage stage, String title, String message) {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
