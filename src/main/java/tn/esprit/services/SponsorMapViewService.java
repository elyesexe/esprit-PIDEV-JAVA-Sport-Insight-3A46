package tn.esprit.services;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SponsorMapViewService {
    public void showMap(Window owner, String sponsorName, String address) {
        String safeAddress = normalizeAddress(address);
        if (safeAddress == null) {
            showMissingAddressAlert();
            return;
        }

        String title = sponsorName == null || sponsorName.isBlank()
                ? "Sponsor map"
                : "Sponsor map - " + sponsorName.trim();

        WebView webView = new WebView();
        webView.setPrefSize(960, 620);
        webView.getEngine().load(buildSearchUrl(safeAddress));

        Label addressLabel = new Label(safeAddress);
        addressLabel.setWrapText(true);
        addressLabel.getStyleClass().add("section-subtitle");

        Button browserButton = new Button("Open in browser");
        browserButton.getStyleClass().add("ghost-button");
        browserButton.setOnAction(event -> openInBrowser(safeAddress));

        HBox actionRow = new HBox(10, browserButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(8, new Label(title), addressLabel, actionRow);
        header.setPadding(new Insets(16, 16, 12, 16));
        header.getStyleClass().add("panel-card");
        header.getChildren().get(0).getStyleClass().add("section-title");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(webView);

        Scene scene = new Scene(root, 980, 760);
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.NONE);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    public void openInBrowser(String address) {
        String safeAddress = normalizeAddress(address);
        if (safeAddress == null) {
            showMissingAddressAlert();
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            showError("Map", "Desktop browsing is not supported on this machine.");
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(buildSearchUrl(safeAddress)));
        } catch (IOException e) {
            showError("Map", "Could not open the map in the browser.\n" + e.getMessage());
        }
    }

    private String buildSearchUrl(String address) {
        return "https://www.openstreetmap.org/search?query="
                + URLEncoder.encode(address, StandardCharsets.UTF_8);
    }

    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        return address.trim();
    }

    private void showMissingAddressAlert() {
        showError("Map", "This sponsor does not have a valid address yet.");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
