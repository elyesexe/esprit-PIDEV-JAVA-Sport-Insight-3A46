package tn.esprit.gui;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
<<<<<<< HEAD
import tn.esprit.i18n.I18n;
import tn.esprit.security.AuthSession;
=======
import tn.esprit.security.AuthSession;
import tn.esprit.tools.MyConnection;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

public class HomeMain extends Application {
    @Override
    public void start(Stage stage) {
<<<<<<< HEAD
        I18n.getLocale();
=======
        bootstrapDatabase();

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

        AuthSession.logout();
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMinWidth(960);
        stage.setMinHeight(720);
        SceneNavigator.setScene(stage, "/tn/esprit/views/login-view.fxml", "/tn/esprit/styles/auth-theme.css", "Sport Insight | Sign in");
        stage.centerOnScreen();
        stage.show();
    }

<<<<<<< HEAD
=======
    private void bootstrapDatabase() {
        try {
            MyConnection.getInstance();
        } catch (Exception e) {
            System.err.println("Database bootstrap failed: " + e.getMessage());
        }
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    public static void main(String[] args) {
        launch(args);
    }
}
