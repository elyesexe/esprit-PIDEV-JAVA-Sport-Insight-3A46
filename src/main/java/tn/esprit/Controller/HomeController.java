package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.gui.SceneNavigator;

public class HomeController {
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Label sidebarSectionLabel;
    @FXML
    private Button sidebarToggleButton;
    @FXML
    private Button sidebarOpenButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button equipesButton;
    @FXML
    private Button joueursButton;
    @FXML
    private Button matchsButton;

    private boolean sidebarVisible;

    @FXML
    public void initialize() {
        sidebarVisible = true;
        applySidebarState();
    }

    @FXML
    private void handleOpenHome() {
        // Already on the homepage.
    }

    @FXML
    private void handleOpenSidebar() {
        sidebarVisible = true;
        applySidebarState();
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarVisible = false;
        applySidebarState();
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesButton, equipesNavButton), "/tn/esprit/views/equipe-crud-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Sport Insight");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursButton, joueursNavButton), "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenMatchs() {
        SceneNavigator.switchScene(resolveNavigationSource(matchsButton, matchsNavButton), "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    private Button resolveNavigationSource(Button primary, Button fallback) {
        return primary != null ? primary : fallback;
    }

    private void applySidebarState() {
        sidebarRoot.setManaged(sidebarVisible);
        sidebarRoot.setVisible(sidebarVisible);
        sidebarSectionLabel.setManaged(sidebarVisible);
        sidebarSectionLabel.setVisible(sidebarVisible);
        sidebarOpenButton.setManaged(!sidebarVisible);
        sidebarOpenButton.setVisible(!sidebarVisible);

        sidebarBrandBox.setVisible(sidebarVisible);
        sidebarBrandBox.setManaged(sidebarVisible);
        sidebarToggleButton.setText("<");

        sidebarRoot.setMinWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
    }
}
