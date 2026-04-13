package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML
    private TabPane tabPane;

    @FXML
    private void exitApplication() {
        Platform.exit();
    }
}
