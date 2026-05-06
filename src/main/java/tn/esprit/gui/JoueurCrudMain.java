package tn.esprit.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
<<<<<<< HEAD
import tn.esprit.i18n.I18n;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class JoueurCrudMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
<<<<<<< HEAD
        FXMLLoader loader = new FXMLLoader(JoueurCrudMain.class.getResource("/tn/esprit/views/joueur-crud-view.fxml"), I18n.getBundle());
=======
        FXMLLoader loader = new FXMLLoader(JoueurCrudMain.class.getResource("/tn/esprit/views/joueur-crud-view.fxml"));
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
        URL stylesheet = JoueurCrudMain.class.getResource("/tn/esprit/styles/joueur-theme.css");
        scene.getStylesheets().add(Objects.requireNonNull(stylesheet, "Joueur theme stylesheet is missing").toExternalForm());

        ThemeManager.registerScene(scene);
<<<<<<< HEAD
        stage.setTitle(I18n.getOrDefault("scene.title.joueur-crud-view", "Joueurs | Sport Insight"));
=======
        stage.setTitle("Joueurs | Sport Insight");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
