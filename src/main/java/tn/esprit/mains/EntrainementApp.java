package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EntrainementApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/entrainement-dashboard.fxml"));
        Scene scene = new Scene(loader.load(), 1440, 920);
        scene.getStylesheets().add(getClass().getResource("/styles/entrainement-dashboard.css").toExternalForm());

        stage.setTitle("Sport Insight - Entrainement");
        stage.setMinWidth(1200);
        stage.setMinHeight(780);
        stage.setScene(scene);
        stage.show();
    }
}
