package tn.esprit.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class SportInsightApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Crée le conteneur principal avec TabPane
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            // Charge les fichiers FXML
            FXMLLoader annonceLoader = new FXMLLoader(getClass().getResource("/annonce_view.fxml"));
            BorderPane annonceView = annonceLoader.load();

            FXMLLoader commentaireLoader = new FXMLLoader(getClass().getResource("/commentaire_view.fxml"));
            BorderPane commentaireView = commentaireLoader.load();

            // Crée les onglets
            Tab annonceTab = new Tab("📋 Annonces", annonceView);
            Tab commentaireTab = new Tab("💬 Commentaires", commentaireView);

            // Ajoute les onglets au TabPane
            tabPane.getTabs().addAll(annonceTab, commentaireTab);

            // Crée la scène et configure le stage
            Scene scene = new Scene(tabPane, 1400, 800);

            // Ajoute le CSS
            String stylesheet = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);

            primaryStage.setTitle("🏆 Sport Insight - Gestion des Annonces et Commentaires");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> System.out.println("👋 Application fermée"));
            primaryStage.show();

            // Affiche un message de bienvenue
            System.out.println("✅ Application démarrée avec succès!");
            System.out.println("📊 Interface chargée avec succès");

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement des fichiers FXML:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

