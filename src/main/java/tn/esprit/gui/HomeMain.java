package tn.esprit.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class HomeMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(HomeMain.class.getResource("/tn/esprit/views/home-view.fxml"));
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1420, visualBounds.getWidth() - 40);
        double height = Math.min(900, visualBounds.getHeight() - 40);

        if (width < 820) {
            width = Math.max(640, visualBounds.getWidth() - 20);
        }
        if (height < 620) {
            height = Math.max(520, visualBounds.getHeight() - 20);
        }

        Scene scene = new Scene(loader.load(), width, height);
        URL stylesheet = HomeMain.class.getResource("/tn/esprit/styles/home-theme.css");
        scene.getStylesheets().add(Objects.requireNonNull(stylesheet, "Home theme stylesheet is missing").toExternalForm());

        ThemeManager.registerScene(scene);
        stage.setTitle("Sport Insight | Accueil");
        stage.setMinWidth(940);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
