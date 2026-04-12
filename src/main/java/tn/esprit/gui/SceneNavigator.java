package tn.esprit.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.stage.Stage;

import java.net.URL;
import java.util.function.Consumer;

public final class SceneNavigator {
    private SceneNavigator() {
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
        double width = stage.getWidth();
        double height = stage.getHeight();

        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            if (controllerConfigurer != null) {
                controllerConfigurer.accept(loader.getController());
            }
            Scene scene = new Scene(root, width, height);

            URL stylesheet = SceneNavigator.class.getResource(cssPath);
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }

            ThemeManager.registerScene(scene);
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la page demandee.\n" + e.getMessage());
            alert.showAndWait();
        }
    }
}
